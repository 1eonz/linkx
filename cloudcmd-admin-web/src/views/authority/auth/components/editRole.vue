<template>
  <!-- 新增/修改角色 -->
  <el-dialog
    :title="
      $t(this.isAdd ? 'index.operations.newsRole' : 'index.operations.editRole')
    "
    :visible.sync="dialogVisible"
    :close-on-click-modal="false"
    @close="closeDialog"
    @opened="handleDialogOpened"
  >
    <el-form
      ref="roleForm"
      :model="roleForm"
      :rules="rules"
      label-width="100px"
      class="roleForm"
      label-position="left"
    >
      <el-form-item :label="$t('index.list.roleName')" prop="name">
        <el-input v-model="roleForm.name" :disabled="!isAdd" />
      </el-form-item>
    </el-form>
    <el-tabs
      v-model="activeTab"
      style="min-height: 400px;"
      @tab-click="handleTabClick"
    >
      <el-tab-pane
        key="RoleAuth"
        :label="$t('index.RoleAuth')"
        name="RoleAuth"
        style="max-height: 400px;overflow-y:auto;"
      >
        <div class="my-tab-pane-btn">
          <div class="my-tab-btn">
            <el-button
              :type="activeButton === 'client' ? 'primary' : ''"
              plain
              size="small"
              @click="activeButton = 'client'"
              >{{ $t('index.ICC', { title: systemTitle }) }}</el-button
            >
            <el-button
              :type="activeButton === 'admin' ? 'primary' : ''"
              plain
              size="small"
              @click="activeButton = 'admin'"
              >{{ $t('index.ICS', { title: systemTitle }) }}</el-button
            >
            <el-button
              :type="activeButton === 'h5' ? 'primary' : ''"
              plain
              size="small"
              @click="activeButton = 'h5'"
              >{{ $t('index.Capp', { title: systemTitle }) }}</el-button
            >
          </div>
          <div class="my-tab-content">
            <div v-show="activeButton === 'client'">
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
            </div>
            <div v-show="activeButton === 'admin'">
              <div class="admin-tree">
                <el-tree
                  ref="roleAuthAdminTree"
                  node-key="id"
                  show-checkbox
                  default-expand-all
                  highlight-current
                  :check-strictly="true"
                  :props="treeProps"
                  :data="menuTree[1]"
                  :default-checked-keys="roleForm.adminPrivJson"
                  @check-change="changeRoleAuthAdminCheckKeys"
                />
              </div>
            </div>
            <div v-show="activeButton === 'h5'">
              <el-tree
                ref="roleAuthH5Tree"
                node-key="id"
                show-checkbox
                default-expand-all
                highlight-current
                :props="treeProps"
                :data="menuTree[2]"
                :default-checked-keys="roleForm.cappPrivJson"
                @check-change="changeRoleAuthH5CheckKeys"
              />
            </div>
          </div>
        </div>
      </el-tab-pane>
      <el-tab-pane
        key="DataAuth"
        :label="$t('index.DataAuth')"
        name="DataAuth"
      >
        <!-- 数据权限穿梭树 -->
        <div class="data-auth-wrapper">
          <DataPermissionTree
            ref="dataAuthTree"
            :check-strictly="true"
            :default-checked-keys="roleForm.dataAuthTreecheckedKeys"
            :show-node-select-all="true"
            :show-node-cancel-all="false"
            height="100%"
            left-title="部门列表"
            right-title="已选择"
            filter-placeholder="搜索部门"
            @change="changeDataAuthCheckKeys"
          />
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-tabs
      v-if="false"
      v-model="activeTab"
      style="min-height: 400px;"
      class="generalAdminTabs"
      @tab-click="handleTabClick"
    >
      <el-tab-pane
        key="ICC"
        :label="$t('index.ICC')"
        name="ICC"
        style="max-height: 400px;overflow-y:auto;"
        class="icc-tab-pane"
      >
        <el-tree
          ref="iccTree"
          node-key="id"
          show-checkbox
          default-expand-all
          highlight-current
          :props="treeProps"
          :data="menuTree[0]"
          :default-checked-keys="roleForm.iccPrivJson"
          @check-change="changeIccCheckKeys"
        />
      </el-tab-pane>

      <el-tab-pane
        key="admin"
        :label="$t('index.ICS', { title: systemTitle })"
        name="admin"
        style="max-height: 400px;overflow-y:auto;"
      >
        <el-tree
          ref="adminTree"
          node-key="id"
          show-checkbox
          default-expand-all
          highlight-current
          :check-strictly="true"
          :props="treeProps"
          :data="menuTree[1]"
          :default-checked-keys="roleForm.adminPrivJson"
          @check-change="changeAdminCheckKeys"
        />
      </el-tab-pane>
      <el-tab-pane
        key="CAPP"
        :label="$t('index.Capp')"
        name="CAPP"
        style="max-height: 400px;overflow-y:auto;"
        class="capp-tab-pane"
      >
        <el-tree
          ref="cappTree"
          node-key="id"
          show-checkbox
          default-expand-all
          highlight-current
          :props="treeProps"
          :data="menuTree[2]"
          :default-checked-keys="roleForm.cappPrivJson"
          @check-change="changeCappCheckKeys"
        />
      </el-tab-pane>
    </el-tabs>
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
import { getMenuList } from '@/api/permission/menu'
import { deepCopy } from '@/utils'
import SelectTree from '@/components/SelectTree'
import selectTreeLazy from '@/components/SelectTreeLazy'
import DataPermissionTree from '../../components/DataPermissionTree.vue'
export default {
  name: 'EditRole',
  components: { SelectTree, selectTreeLazy, DataPermissionTree },
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
      roleForm: {
        name: '',
        iccPrivJson: [],
        adminPrivJson: [],
        cappPrivJson: [],
        orgPrivList: [],
        dataAuthTreecheckedKeys: []
      },
      menuTree: [],
      rules: Object.freeze(rules),
      activeButton: 'client',
      DEPARTMENT_SYNC_SIGN: false
    }
  },
  computed: {
    systemTitle() {
      return this.$store.state.settings.systemName
    }
  },
  created() {
    this.resolveDepartmentSyncSign()
    this.getMenuTree()
  },
  mounted() {},
  methods: {
    // 弹窗打开后恢复数据权限选中状态
    handleDialogOpened() {
      if (this.$refs.dataAuthTree) {
        const normalized = this.$refs.dataAuthTree.syncFromDetail({
          orgList: this.roleForm.orgPrivList
        })
        if (normalized && normalized.length) {
          this.roleForm.orgPrivList = normalized
        }
      }
    },
    // 解析部门同步标识
    resolveDepartmentSyncSign() {
      try {
        const globalConfig = JSON.parse(localStorage.getItem('globalConfig'))
        if (globalConfig) {
          this.DEPARTMENT_SYNC_SIGN =
            globalConfig.DEPARTMENT_SYNC_SIGN === 'true' ||
            globalConfig.DEPARTMENT_SYNC_SIGN === true
        }
      } catch (e) {
        this.DEPARTMENT_SYNC_SIGN = false
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
          orgPrivList
        } = row
        this.roleForm.name = name
        this.roleForm.iccPrivJson = iccPrivJson || []
        this.roleForm.adminPrivJson = adminPrivJson || []
        this.roleForm.cappPrivJson = cappPrivJson || []
        this.roleForm.orgPrivList = orgPrivList || []
        this.roleForm.dataAuthTreecheckedKeys = this.getAllIds(orgPrivList)
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
          param.orgPrivList = this.roleForm.orgPrivList || []
          // 新格式（扁平数组 [{id, name, path}]）不需要填充 children
          const isNewFormat = param.orgPrivList.length > 0 && param.orgPrivList.some(item => item.path !== undefined)
          if (!this.DEPARTMENT_SYNC_SIGN && !isNewFormat) {
            const allRoleNode = this.$refs.dataAuthTree?.getAllRoleNode?.() || []
            await this.fillEmptyChildren(param.orgPrivList, allRoleNode)
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
                disabled: item.status === 2
              }
            })
            console.log(JSON.parse(JSON.stringify(this.menuTree[index])), index)
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
    changeDataAuthCheckKeys(checkedDetail) {
      console.log(checkedDetail, 'checkedDetail')
      this.roleForm.orgPrivList = checkedDetail
      return
      const checkedKeys = checkedDetail.map(item => item.id)
      const tree = this.$refs.dataAuthTree

      if (this.DEPARTMENT_SYNC_SIGN) {
        this.roleForm.orgPrivList = this.getSelectedOrgTreeFromSync(
          tree.actualTreeData,
          checkedKeys
        )
      } else {
        this.roleForm.orgPrivList = this.filterOrgData(
          tree.allRoleNode,
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
      this.$refs.dataAuthTree && this.$refs.dataAuthTree.reset()
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
      this.roleForm.orgPrivList = []
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
    }
  }
}
</script>
<style lang="scss" scoped>
.data-auth-wrapper {
  height: 400px;
  overflow: hidden;
}
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
