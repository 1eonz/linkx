<template>
  <!-- 新增/修改角色 -->
  <el-dialog
    :title="
      $t(this.isAdd ? 'index.operations.newsRole' : 'index.operations.editRole')
    "
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    @close="closeDialog"
  >
    <el-form
      ref="roleForm"
      :model="roleForm"
      :rules="rules"
      label-width="100px"
      class="roleForm"
      label-position="top"
    >
      <el-form-item :label="$t('index.list.roleName')" prop="name">
        <el-input v-model="roleForm.name" :disabled="!isAdd" />
      </el-form-item>
      <el-form-item :label="$t('index.list.roleName')" >
        <el-tree
          ref="roleAuthClientTree"
          node-key="id"
          show-checkbox
          default-expand-all
          highlight-current
          :props="treeProps"
          :data="menuTree[0]"
          :default-checked-keys="roleForm.iccPrivJson"
          @check-change="changeRoleAuthClientCheckKeys"
        />
      </el-form-item>
    </el-form>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" @click="handleConfirm">
        {{ $t('index.determine') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { createRole, updateRole } from '@/api/resource/role'
import { queryDepartment, queryDepartmentTree } from '@/api/h5/collaboration'
import { getMenuList } from '@/api/permission/menu'
import { deepCopy } from '@/utils'
import SelectTree from '@/components/SelectTree'
import selectTreeLazy from '@/components/SelectTreeLazy'
export default {
  name: 'EditRole',
  components: { SelectTree, selectTreeLazy },
  data() {
    const rules = {
      name: [
        {
          required: true,
          message: this.$t('index.messageText.roleNameCannotBeEmpty'),
          trigger: 'blur'
        }
      ]
    }
    return {
      treeProps: {
        children: 'children',
        label: 'name'
      },
      activeTab: 'RoleAuth',
      dialogVisible: false,
      isAdd: true,
      allRoleNode: [],
      roleForm: {
        name: '',
        iccPrivJson: [],
        adminPrivJson: [],
        cappPrivJson: [],
        imOrgPrivJson: [],
        dataAuthTreecheckedKeys: []
      },
      menuTree: [],
      rules: Object.freeze(rules),
      activeButton: 'client',
      dataAuthMenuTree: [],
      dataAuthLoading: false,
      isSelectAll: false,
      DEPARTMENT_SYNC_SIGN: false
    }
  },
  computed: {
    systemTitle() {
      return this.$store.state.settings.systemName
    }
  },
  created() {
    this.getGlobalConfig()
    this.getMenuTree()
    // this.getDataAuthMenuTree()
  },
  mounted() {},
  methods: {
    // 获取全局参数
    async getGlobalConfig() {
      const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
      if (globalConfig) {
        this.DEPARTMENT_SYNC_SIGN =
          globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
          globalConfig.DEPARTMENT_SYNC_SIGN === true
        // 新组织树一次性加载
        console.log(this.DEPARTMENT_SYNC_SIGN, '===this.DEPARTMENT_SYNC_SIGN')

        if (this.DEPARTMENT_SYNC_SIGN) {
          this.getDataAuthMenuTree()
        }
      }
    },
    // 一次性加载协同部门树（返回所有子孙节点）
    async getDataAuthMenuTree() {
      this.dataAuthLoading = true
      try {
        const res = await queryDepartmentTree({})
        if (res && res.code === 0 && res.data) {
          let treeData
          // 兼容后端返回对象或数组两种情况
          if (Array.isArray(res.data)) {
            treeData = res.data
          } else {
            treeData = [res.data]
          }
          // children 统一规范为数组
          const normalize = nodes =>
            nodes.map(node => ({
              ...node,
              children:
                node.children !== null && Array.isArray(node.children)
                  ? node.children
                  : []
            }))
          this.dataAuthMenuTree = normalize(treeData)
          // 一次性加载时，allRoleNode 直接等于整棵树，供后续筛选和提交使用
          this.allRoleNode = this.dataAuthMenuTree
        }
      } catch (e) {
        // 忽略异常，保持懒加载可用
      } finally {
        this.dataAuthLoading = false
      }
    },
    getAllIds(data) {
      const ids = []
      function traverse(nodes) {
        if (!Array.isArray(nodes)) return

        for (const node of nodes) {
          if (node && node.id) {
            ids.push(node.id)
          }
          if (node.children && Array.isArray(node.children)) {
            traverse(node.children)
          }
        }
      }
      traverse(data)
      return ids
    },
    // 初始化
    async init(row) {
      this.isAdd = !row
      this.roleForm.name = ''
      if (row) {
        const {
          id,
          name,
          iccPrivJson,
          adminPrivJson,
          cappPrivJson,
          imOrgPrivJson
        } = row
        this.roleForm.name = name
        this.roleForm.iccPrivJson = iccPrivJson || []
        this.roleForm.adminPrivJson = adminPrivJson || []
        this.roleForm.cappPrivJson = cappPrivJson || []
        this.roleForm.imOrgPrivJson = imOrgPrivJson || []
        this.roleForm.dataAuthTreecheckedKeys = this.getAllIds(imOrgPrivJson)
        this.roleForm['id'] = id
      }
      if (
        this.menuTree[0].length > 0 &&
        !this.roleForm.iccPrivJson.includes(this.menuTree[0][0].id)
      ) {
        this.roleForm.iccPrivJson = [
          this.menuTree[0][0].id,
          ...this.roleForm.iccPrivJson
        ]
      }
      if (this.menuTree[2].length > 0) {
        if (!this.roleForm.cappPrivJson.includes(this.menuTree[2][0].id)) {
          this.roleForm.cappPrivJson.push(this.menuTree[2][0].id)
        }
      }

      this.activeTab = 'RoleAuth'
      this.activeButton = 'client'
      this.dialogVisible = true
      this.isSelectAll = false
    },

    /**
     * 异步递归填充 imOrgPrivJson 中 children 为 null 或 [] 的节点
     * @param {Array} orgList - 当前处理的组织列表，通常是 imOrgPrivJson 或其子节点数组
     * @param {Array} fullTree - 完整的组织树，用于查找已有节点的 children 情况，即 allRoleNode
     */
    async fillEmptyChildren(orgList, fullTree) {
      for (const node of orgList) {
        // 如果当前节点的 children 为 null 或空数组，则尝试填充
        if (!node.children || node.children.length === 0) {
          let childrenToSet = []

          // Step 1: 先尝试从 allRoleNode 中查找当前节点，并看它有没有 children
          const fullTreeNode = this.findNodeInTree(fullTree, node.id)
          if (fullTreeNode) {
            if (fullTreeNode.children && fullTreeNode.children.length > 0) {
              // 如果 allRoleNode 中该节点有 children 数据，直接使用
              // childrenToSet = fullTreeNode.children
              return
            } else {
              // 如果 allRoleNode 中该节点的 children 为空，则调用 loadDepartMentList 加载
              try {
                console.log(`调用 loadDepartMentList，code = ${node.code}`)
                const loadedChildren = await this.loadDepartMentList(node.code)
                childrenToSet = loadedChildren || []
              } catch (error) {
                console.error(
                  `loadDepartMentList 失败，code = ${node.code}`,
                  error
                )
                childrenToSet = [] // 出错也设为空数组
              }
            }
          } else {
            // 如果在 allRoleNode 中都找不到该节点，也尝试调用加载（可选，根据需求）
            try {
              console.log(
                `在 allRoleNode 中未找到节点，尝试加载，code = ${node.code}`
              )
              const loadedChildren = await this.loadDepartMentList(node.code)
              childrenToSet = loadedChildren || []
            } catch (error) {
              console.error(
                `loadDepartMentList 失败（节点未在 allRoleNode 中找到），code = ${
                  node.code
                }`,
                error
              )
              childrenToSet = []
            }
          }

          // 设置 children
          node.children = childrenToSet

          // Step 2: 如果加载到了 children（非空），则递归处理这些子节点
          if (childrenToSet && childrenToSet.length > 0) {
            await this.fillEmptyChildren(childrenToSet, fullTree)
          }
        } else {
          // 如果当前节点已经有 children（非空），继续递归处理子节点
          await this.fillEmptyChildren(node.children, fullTree)
        }
      }
    },

    /**
     * 在树结构中根据 id 查找节点
     * @param {Array} tree - 树结构数组
     * @param {String} id - 目标节点id
     * @returns {Object|null} 找到的节点或 null
     */
    findNodeInTree(tree, id) {
      for (const node of tree) {
        if (node.id === id) {
          return node
        }
        if (node.children && node.children.length > 0) {
          const found = this.findNodeInTree(node.children, id)
          if (found) return found
        }
      }
      return null
    },

    // 点击确认
    handleConfirm() {
      this.$refs['roleForm'].validate(async valid => {
        if (valid) {
          const param = deepCopy(this.roleForm)
          param.imOrgPrivJson = this.roleForm.imOrgPrivJson || []
          if (!this.DEPARTMENT_SYNC_SIGN) {
            await this.fillEmptyChildren(param.imOrgPrivJson, this.allRoleNode)
          }
          const api = this.isAdd ? createRole : updateRole
          api(param).then(result => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success'
              })
              this.$emit('success')
              this.closeDialog()
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        }
      })
    },

    // 切换tab
    handleTabClick(tab) {},
    // getDataAuthMenuTree() {
    //   this.dataAuthLoading = true
    //   queryDepartment().then((res) => {
    //     this.dataAuthMenuTree = res.data.map(item => {
    //       if (item.status === 2) {
    //         this.roleForm.dataAuthMenuTree.push(item.id)
    //       }
    //       return {
    //         ...item,
    //         disabled: +item.status === 2
    //       }
    //     })
    //   }).catch(() => {}).finally(() => {
    //     this.dataAuthLoading = false
    //   })
    // },
    // 获取菜单
    async getMenuTree() {
      const ids = ['1289822833455460001', '', '1289822833455460002']
      ids.forEach((id, index) => {
        getMenuList({ applicationId: id }).then(res => {
          if (res.code === 0) {
            const data = this.filterData(res.data)
            this.menuTree[index] = data.map(item => {
              if (item.status === 2) {
                if (index === 0) {
                  this.roleForm.iccPrivJson.push(item.id)
                } else if (index === 1) {
                  this.roleForm.adminPrivJson.push(item.id)
                } else if (index === 2) {
                  this.roleForm.cappPrivJson.push(item.id)
                }
              }
              return {
                ...item,
                disabled: +item.status === 2
              }
            })
            // 群组协同lecense权限
            const licenseAuth = this.$store?.state?.user?.licenseAuth
            if (licenseAuth?.groupCollaborationAuth) {
              if (index === 0) {
                this.menuTree[index] = this.menuTree[index].filter(
                  item =>
                    !['1522392406668870001', '1522392406668870002'].includes(
                      item.id
                    )
                )
              }
              if (index === 1) {
                // 移除位置信息
                this.menuTree[index] = this.menuTree[index].filter(
                  item => item.id !== '1522392406668869809'
                )
                // 移除h5中协同管理 标签管理 群组标签管理
                const h5Index = this.menuTree[index].findIndex(
                  item => item.id === '1522392406668869804'
                )
                this.menuTree[index][h5Index].children = this.menuTree[index][
                  h5Index
                ].children.filter(
                  item =>
                    ![
                      '1522392406668869807',
                      '1522392406668869808',
                      '1522392406668870003'
                    ].includes(item.id)
                )
              }
              if (index === 2) {
                this.menuTree[index] = this.menuTree[index].filter(
                  item =>
                    !['1522392406668870009', '1522392406668870010'].includes(
                      item.id
                    )
                )
              }
            }
            // AI协同lecense权限
            if (licenseAuth?.AICollaborationAuth) {
              if (index === 2) {
                // 移除AI
                this.menuTree[index] = this.menuTree[index].filter(
                  item => !['1522392406668869999'].includes(item.id)
                )
              }
            }
          }
        })
      })
    },
    // 筛选数据
    filterData(arr) {
      const ret = []
      arr.map(item => {
        if (item.status !== 0 && item.status !== 2) {
          return
        }
        if (item.children) {
          item.children = this.filterData(item.children)
        }
        ret.push(item)
      })
      return ret
    },
    changeIccCheckKeys() {
      this.roleForm.iccPrivJson = this.$refs.iccTree.getCheckedKeys()
    },
    changeAdminCheckKeys() {
      this.roleForm.adminPrivJson = this.$refs.adminTree.getCheckedKeys(true)
    },
    changeCappCheckKeys() {
      this.roleForm.cappPrivJson = this.$refs.cappTree.getCheckedKeys()
    },
    // 新增tree
    // 勾选「群组归档」时，自动勾选「协同群组」 去勾选协同群组，自动去勾选群组归档
    changeRoleAuthClientCheckKeys(node, isSelect) {
      const checkedKeys = this.$refs.roleAuthClientTree.getCheckedKeys()
      this.roleForm.iccPrivJson = checkedKeys

      // 群组归档 id 为 1522392406668870002，协同群组 id 为 1522392406668870001
      if (node && node.id === '1522392406668870002' && isSelect) {
        const xtqzId = '1522392406668870001' // 协同群组 id
        if (xtqzId && !checkedKeys.includes(xtqzId)) {
          const newKeys = [xtqzId, ...checkedKeys]
          this.roleForm.iccPrivJson = newKeys
          // 同步更新树组件的勾选状态
          this.$refs.roleAuthClientTree.setCheckedKeys(newKeys)
        }
      }
      if (node && node.id === '1522392406668870001' && !isSelect) {
        const qzgdId = '1522392406668870002' // 协同群组 id
        if (qzgdId && checkedKeys.includes(qzgdId)) {
          const newKeys = checkedKeys.filter(item => item !== qzgdId)
          this.roleForm.iccPrivJson = newKeys
          // 同步更新树组件的勾选状态
          this.$refs.roleAuthClientTree.setCheckedKeys(newKeys)
        }
      }
    },
    // H5：勾选「群组归档」时，自动勾选「协同群组」
    changeRoleAuthH5CheckKeys(node, isSelect) {
      const checkedKeys = this.$refs.roleAuthH5Tree.getCheckedKeys()
      this.roleForm.cappPrivJson = checkedKeys

      // H5 群组归档 id 为 1522392406668870010，协同群组 id 为 1522392406668870009
      if (node && node.id === '1522392406668870010' && isSelect) {
        const xtqzId = '1522392406668870009' // 协同群组 id
        if (xtqzId && !checkedKeys.includes(xtqzId)) {
          const newKeys = [xtqzId, ...checkedKeys]
          this.roleForm.cappPrivJson = newKeys
          // 同步更新 H5 树组件的勾选状态
          this.$refs.roleAuthH5Tree.setCheckedKeys(newKeys)
        }
      }
      if (node && node.id === '1522392406668870009' && !isSelect) {
        const qzgdId = '1522392406668870010' // 协同群组 id
        if (qzgdId && checkedKeys.includes(qzgdId)) {
          const newKeys = checkedKeys.filter(item => item !== qzgdId)
          this.roleForm.cappPrivJson = newKeys
          // 同步更新树组件的勾选状态
          this.$refs.roleAuthH5Tree.setCheckedKeys(newKeys)
        }
      }
    },

    // findMatchingNode(data, allRoleNode) {
    //   const targetId = data.id
    //   function findInNodes(nodes) {
    //     if (!Array.isArray(nodes)) return null
    //     for (const node of nodes) {
    //       if (node && node.id === targetId) {
    //         return node // 找到匹配，直接返回该节点
    //       }
    //       // 递归查找子节点
    //       if (node && node.children) {
    //         const foundInChildren = findInNodes(node.children)
    //         if (foundInChildren) {
    //           return foundInChildren // 如果子节点中找到，提前返回
    //         }
    //       }
    //     }
    //     return null // 当前层级没找到
    //   }
    //   return findInNodes(allRoleNode)
    // },

    // handleCheck(data, checkedStatus) {
    //   const { checkedKeys } = checkedStatus
    //   const treeData = this.DEPARTMENT_SYNC_SIGN ? this.dataAuthMenuTree : this.allRoleNode
    //   const currentNode = this.findMatchingNode(data, treeData)
    //   if (!currentNode) return
    //   const isChecked = checkedKeys.includes(currentNode.id)
    //   if (isChecked) {
    //     this.checkAllChildren(currentNode, true)
    //   } else {
    //     this.checkAllChildren(currentNode, false)
    //   }
    // },
    // // 递归选中/取消选中所有子节点
    // checkAllChildren(node, isChecked) {
    //   if (node.children && node.children.length > 0) {
    //     const childKeys = this.getChildrenKeys(node)
    //     if (isChecked) {
    //       // 添加子节点到选中列表
    //       this.$refs.dataAuthTree.setCheckedKeys([
    //         ...this.$refs.dataAuthTree.getCheckedKeys(),
    //         ...childKeys
    //       ])
    //     } else {
    //       // 从选中列表中移除子节点
    //       const currentCheckedKeys = this.$refs.dataAuthTree.getCheckedKeys()
    //       const newCheckedKeys = currentCheckedKeys.filter(
    //         key => !childKeys.includes(key)
    //       )
    //       this.$refs.dataAuthTree.setCheckedKeys(newCheckedKeys)
    //     }
    //     // 递归处理子节点
    //     node.children.forEach(child => {
    //       this.checkAllChildren(child, isChecked)
    //     })
    //   }
    // },
    // // 获取所有子节点的key
    // getChildrenKeys(node) {
    //   let keys = []
    //   if (node.children && node.children.length > 0) {
    //     node.children.forEach(child => {
    //       keys.push(child.id)
    //       keys = keys.concat(this.getChildrenKeys(child))
    //     })
    //   }
    //   return keys
    // },
    changeDataAuthCheckKeys() {
      this.isSelectAll = true
      const checkedKeys = this.$refs.dataAuthTree.getCheckedKeys()

      if (this.DEPARTMENT_SYNC_SIGN) {
        this.roleForm.imOrgPrivJson = this.getSelectedOrgTreeFromSync(
          this.dataAuthMenuTree,
          checkedKeys
        )
      } else {
        this.roleForm.imOrgPrivJson = this.filterOrgData(
          this.allRoleNode,
          checkedKeys
        )
      }
    },
    changeRoleAuthAdminCheckKeys() {
      this.roleForm.adminPrivJson = this.$refs.roleAuthAdminTree.getCheckedKeys()
    },
    // 点击关闭
    closeDialog() {
      this.resetTemp()
      // this.$refs.iccTree.setCheckedKeys([])
      // this.$refs.adminTree.setCheckedKeys([])
      // this.$refs.cappTree.setCheckedKeys([])
      // 新增
      this.$refs.roleAuthClientTree &&
        this.$refs.roleAuthClientTree.setCheckedKeys([])
      this.$refs.roleAuthH5Tree && this.$refs.roleAuthH5Tree.setCheckedKeys([])
      this.$refs.dataAuthTree && this.$refs.dataAuthTree.setCheckedKeys([])
      this.$refs.roleAuthAdminTree &&
        this.$refs.roleAuthAdminTree.setCheckedKeys([])
      this.dialogVisible = false
    },
    // 重置
    resetTemp() {
      this.activeTab = 'RoleAuth'
      this.activeButton = 'client'
      this.roleForm.name = ''
      this.roleForm.iccPrivJson = []
      this.roleForm.adminPrivJson = []
      this.roleForm.cappPrivJson = []
      this.roleForm.imOrgPrivJson = []
      delete this.roleForm.id
      this.$refs.roleForm.resetFields()
    },
    filterOrgData(data, filterIds) {
      function filterRecursive(nodes) {
        const filteredNodes = []
        for (const node of nodes) {
          // 如果当前节点在筛选列表中
          if (filterIds.includes(node.id)) {
            const newNode = { ...node }
            // 递归处理子节点
            if (node.children) {
              newNode.children = filterRecursive(node.children)
            }
            filteredNodes.push(newNode)
          } else {
            // 如果当前节点不在筛选列表中，但存在子节点
            if (node.children && node.children.length > 0) {
              const filteredChildren = filterRecursive(node.children)
              // 将过滤后的子节点直接添加到结果中（等级提升）
              if (filteredChildren && filteredChildren.length > 0) {
                filteredNodes.push(...filteredChildren)
              }
            }
          }
        }
        return filteredNodes.length > 0 ? filteredNodes : null
      }
      return filterRecursive(data)
    },
    /**
     * DEPARTMENT_SYNC_SIGN 为 true 时：
     * 根据选中的 id，从一次性加载的 dataAuthMenuTree 中截取完整子树（包含全部子孙节点）
     * 规则：
     *  - 选中父节点时，直接返回父节点整棵子树（无需再勾选子节点）
     *  - 如果父节点已选中，则其子孙节点即使被勾选也不会单独再提升为根节点，避免重复
     */
    getSelectedOrgTreeFromSync(treeData, selectedIds) {
      function dfs(nodes, ancestorSelected = false) {
        if (!Array.isArray(nodes)) return []
        const result = []

        for (const node of nodes) {
          const isSelected = selectedIds.includes(node.id)

          if (isSelected && !ancestorSelected) {
            // 作为根节点返回整棵子树（深拷贝，避免后续修改影响原始树）
            result.push(JSON.parse(JSON.stringify(node)))
            // 该节点的所有子孙都已包含在返回结果中，子孙若也选中，则不再单独提升
          } else if (!ancestorSelected) {
            // 当前没有被上层选中的祖先覆盖，继续向下找是否有被选中的子孙
            if (node.children && node.children.length > 0) {
              const childrenResult = dfs(
                node.children,
                ancestorSelected || isSelected
              )
              if (childrenResult.length > 0) {
                result.push(...childrenResult)
              }
            }
          } else {
            // ancestorSelected === true 的情况下，说明上层已经作为整棵树被加入结果，
            // 本层及子孙的选中不需要再单独处理，直接跳过即可。
          }
        }

        return result
      }

      return dfs(treeData, false)
    },
    async loadDepartMentList(code = '') {
      const params = {}
      if (code) {
        params.parentCode = code
      }
      try {
        const res = await queryDepartment(params)
        if (res.code === 0 && res.data && res.data.length > 0) {
          return res.data
        } else {
          return []
        }
      } catch (error) {
        return []
      }
    },
    async loadNode(node, resolve) {
      const res = await this.loadDepartMentList(node?.data?.code)
      if (node.level === 0) {
        this.allRoleNode = this.saveAndBuildTree(this.allRoleNode, null, res)
      } else {
        if (res.length > 0) {
          this.allRoleNode = this.saveAndBuildTree(
            this.allRoleNode,
            node?.data?.code,
            res
          )
          // 新增的时候,展开能自动选中
          if (node.checked && this.isSelectAll) {
            const checkedKeys = this.$refs.dataAuthTree.getCheckedKeys()
            let flag = false // 是否有新增key标识
            res.forEach(item => {
              if (!checkedKeys.includes(item.id)) {
                checkedKeys.push(item.id)
                flag = true
              }
            })
            if (flag) {
              this.$refs.dataAuthTree.setCheckedKeys(checkedKeys)
              flag = false
              this.roleForm.imOrgPrivJson = this.filterOrgData(
                this.allRoleNode,
                checkedKeys
              )
            }
          }
        }
      }
      return resolve(res)
    },

    /**
     * 保存并构建树形结构
     * @param {Array} savedData 当前已经保存的树形数据（数组，最开始可能是 []）
     * @param {String|Number} targetCode 目标父节点的 code，新数据要放到这个节点的 children 下
     * @param {Array} newlyFetchedData 本次获取到的新数据（要作为子节点）
     * @returns {Array} 更新后的树形数据
     */
    saveAndBuildTree(savedData, targetCode, newlyFetchedData) {
      // 克隆一份 savedData，避免直接修改原数据
      let result = JSON.parse(JSON.stringify(savedData))

      // 规范化：确保所有节点的 children 是数组（原来是 null 的话，转成 []）
      result = result.map(node => ({
        ...node,
        children:
          node.children !== null && Array.isArray(node.children)
            ? node.children
            : []
      }))

      // 如果是第一次，savedData 为空，直接把新数据作为顶层节点，并确保 children 是数组
      if (result.length === 0) {
        return newlyFetchedData.map(node => ({
          ...node,
          children:
            node.children !== null && Array.isArray(node.children)
              ? node.children
              : []
        }))
      }

      // 查找是否存在目标节点（根据 targetCode）
      const targetNode = this.findNodeByCode(result, targetCode)

      if (targetNode) {
        // 找到了目标节点，确保它的 children 是数组
        targetNode.children = targetNode.children || []
        // 规范化新获取的数据的 children 字段
        const normalizedNewData = newlyFetchedData.map(node => ({
          ...node,
          children:
            node.children !== null && Array.isArray(node.children)
              ? node.children
              : []
        }))
        // 将新数据添加到目标节点的 children 中
        targetNode.children.push(...normalizedNewData)
      } else {
        // 如果没找到目标节点，目前啥也不做，也可以选择放到顶层，根据业务需求
        // 这里选择：不做任何操作，继续返回原树
        // console.warn(`未找到 code 为 "${targetCode}" 的节点，新数据未被添加。`);
      }

      return result
    },

    /**
     * 在树形结构中根据 code 查找节点（支持嵌套 children，深度优先）
     * @param {Array} treeData 树形数据
     * @param {String|Number} code 要查找的 code
     * @returns {Object|null} 找到的节点对象，没找到返回 null
     */
    findNodeByCode(treeData, code) {
      for (const node of treeData) {
        if (node.code === code) {
          // 兼容 string 和 number 比较
          return node
        }
        if (node.children && node.children.length > 0) {
          const found = this.findNodeByCode(node.children, code)
          if (found) return found
        }
      }
      return null
    }
  }
}
</script>
<style lang="scss" scoped>
.tip-words {
  font-size: 16px;
  color: #303133;
}
.tip-size {
  font-size: 14px;
  color: #bfbdbc;
  margin-top: 6px;
}
.el-icon-folder {
  font-size: 50px;
  color: #409efe;
  margin: 40px auto 20px;
}
.generalAdminTabs,
.admin-tree {
  ::v-deep .el-tree {
    .el-tree-node {
      .is-leaf + .el-checkbox .el-checkbox__inner {
        display: inline-block;
      }
      .el-checkbox .el-checkbox__inner {
        display: none;
      }
    }
  }
}

.my-tab-pane-btn {
  .my-tab-btn {
    ::v-deep .el-button--primary.is-plain:focus,
    ::v-deep .el-button--primary.is-plain:hover {
      background-color: #ecf5ff;
      color: #409eff;
    }
  }
  .my-tab-content {
    margin-top: 16px;
  }
}
.my-tab-pane-btn,
.icc-tab-pane,
.capp-tab-pane {
  ::v-deep .el-tree-node__expand-icon.is-leaf {
    display: none;
  }
  .admin-tree ::v-deep .el-tree-node__expand-icon.is-leaf {
    display: inline-block;
  }
}
.roleForm {
  ::v-deep .el-input.is-disabled .el-input__inner {
    background-color: transparent;
  }
  ::v-deep .el-form-item {
    margin-bottom: 6px;
  }
}
</style>
