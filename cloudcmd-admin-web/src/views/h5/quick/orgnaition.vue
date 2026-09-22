<template>
  <div class="select-tree">
    <!-- 已选项以 Tag 方式回显，可点击进行二次筛选，可关闭移除 -->
    <div
      class="selected-tags"
      v-popover:treePopover
      @click="togglePopover"
      :class="{ 'is-focus': popoverVisible }"
    >
      <div class="tags-container" @click="togglePopover">
        <el-tag
          v-for="tag in checkedNodes"
          :key="tag.id"
          size="small"
          :type="activeTagId === tag.id ? 'info' : ''"
          :effect="activeTagId === tag.id ? 'light' : 'plain'"
          :disable-transitions="true"
          closable
          @close="removeTag(tag)"
          @click.native.stop="focusTag(tag)"
          class="org-tag"
          :class="{ 'active-org': activeTagId === tag.id }"
        >
          {{ tag.name }}
        </el-tag>
        <span v-if="checkedNodes.length === 0" class="placeholder"
          >请选择关联部门</span
        >
      </div>
      <div class="actions">
        <!-- <i
          v-if="checkedNodes.length"
          class="el-icon-close clear-icon"
          @click.stop="clearVal"
        /> -->
        <i
          class="el-icon-arrow-down dropdown-icon"
          :class="{ 'is-reverse': popoverVisible }"
          @click.stop="togglePopover"
        />
      </div>
    </div>

    <el-popover
      ref="treePopover"
      placement="bottom-start"
      trigger="click"
      popper-class="select-tree-popper"
      @show="onPopoverShow"
      @hide="onPopoverHide"
    >
      <el-tree
        ref="treeRef"
        class="popover-tree"
        node-key="id"
        :data="orgList"
        :props="treeProps"
        :default-expand-all="false"
        :highlight-current="false"
        :expand-on-click-node="false"
        :lazy="true"
        :load="loadNode"
        :show-checkbox="multiple"
        :check-strictly="checkStrictly"
        @check="handleCheck"
        @current-change="currentChange"
      />
    </el-popover>
  </div>
</template>

<script>
import { queryDepartment, queryUserByIdCard } from '@/api/h5/collaboration'
import { getIsAdmin, getIdCardNum } from '@/utils/auth'
export default {
  name: 'SelectTreeLoading',
  props: {
    value: {
      type: [String, Array],
      default: () => []
    },
    placeholder: {
      type: String,
      default: ''
    },
    isInitValue: {
      type: Boolean,
      default: false
    },
    multiple: {
      type: Boolean,
      default: true
    },
    checkStrictly: {
      type: Boolean,
      default: true
    }
  },
  data() {
    return {
      displayText: '',
      checkedNodes: [],
      checkedKeys: [],
      activeTagId: null,
      popoverVisible: false,
      departmentCode: null,
      idCardNum: getIdCardNum(),
      isAdmin: getIsAdmin(),
      treeProps: {
        label: 'name',
        children: 'children'
      },
      orgList: [],
      // 存储所有组织节点的完整信息，用于计算path
      allOrgNodes: new Map()
    }
  },
  watch: {
    value: {
      handler(val) {
        if (this.multiple) {
          if (Array.isArray(val)) {
            this.checkedKeys = val
            this.updateDisplayText()
          }
        } else {
          this.displayText = val
        }
      },
      immediate: true
    }
  },
  mounted() {
    if (this.isAdmin) {
      this.getOrgList()
    } else {
      this.getUserOrgNameByIdCardNum()
    }
    if (this.isInitValue) {
      if (this.multiple && Array.isArray(this.value)) {
        this.checkedKeys = this.value
        this.updateDisplayText()
      } else {
        this.displayText = this.value
      }
    }
  },
  methods: {
    togglePopover() {
      if (this.popoverVisible) {
        this.$refs.treePopover.doClose && this.$refs.treePopover.doClose()
      } else {
        this.$refs.treePopover.doShow && this.$refs.treePopover.doShow()
      }
    },
    onPopoverShow() {
      this.popoverVisible = true
    },
    onPopoverHide() {
      this.popoverVisible = false
    },
    focusTag(tag) {
      this.activeTagId = tag.id
      // 触发激活组织变化事件
      this.$emit('active-org-change', tag.id)
    },
    removeTag(tag) {
      // 从已选中中移除，并同步到树勾选
      const idxNode = this.checkedNodes.findIndex(
        n => Number(n.id) === Number(tag.id)
      )
      if (idxNode > -1) this.checkedNodes.splice(idxNode, 1)
      const idxKey = this.checkedKeys.findIndex(
        k => Number(k) === Number(tag.id)
      )
      if (idxKey > -1) this.checkedKeys.splice(idxKey, 1)

      // 如果移除的是当前激活的组织，设置 activeTagId 为剩余组织的第一个
      if (this.activeTagId === tag.id) {
        if (this.checkedNodes.length > 0) {
          // 设置为剩余已选组织的第一个
          this.activeTagId = this.checkedNodes[0].id
          this.$emit('active-org-change', this.activeTagId)
        } else {
          // 如果没有剩余组织，设置为 null
          this.activeTagId = null
          this.$emit('active-org-change', null)
        }
      }

      // 同步更新树的勾选状态，使用 $nextTick 确保DOM更新完成
      this.syncTreeCheckedKeys()
      this.$emit('input', this.checkedKeys)
    },
    currentChange(data) {
      if (!this.multiple) {
        // 单选模式
        this.displayText = data.name
        this.$emit('input', data.id)
        this.$emit('current-change', data)
        this.$refs.treePopover.doClose()
      }
    },
    handleCheck(data, checked) {
      if (this.multiple) {
        // 检查当前激活的组织是否还在已选列表中
        const isActiveOrgStillSelected = checked.checkedNodes.some(
          node => node.id === this.activeTagId
        )

        // 为每个选中的组织节点计算并添加path字段
        const nodesWithPath = checked.checkedNodes.map(node => ({
          ...node,
          path: this.calculateOrgPath(node.id)
        }))

        this.checkedNodes = nodesWithPath
        this.checkedKeys = checked.checkedKeys
        this.updateDisplayText()

        // 如果当前激活的组织被取消勾选，设置 activeTagId 为剩余组织的第一个
        if (this.activeTagId && !isActiveOrgStillSelected) {
          if (checked.checkedNodes.length > 0) {
            // 设置为剩余已选组织的第一个
            this.activeTagId = checked.checkedNodes[0].id
            this.$emit('active-org-change', this.activeTagId)
          } else {
            // 如果没有剩余组织，设置为 null
            this.activeTagId = null
            this.$emit('active-org-change', null)
          }
        }
        // 如果是新增的组织且当前没有激活组织，自动激活第一个组织
        else if (checked.checkedNodes.length > 0 && !this.activeTagId) {
          this.activeTagId = checked.checkedNodes[0].id
          this.$emit('active-org-change', this.activeTagId)
        }

        this.$emit('input', this.checkedKeys)
        this.$emit('check-change', data, checked)
      }
    },
    clearVal() {
      if (this.multiple) {
        this.checkedNodes = []
        this.checkedKeys = []
        this.displayText = ''
        this.activeTagId = null // 清空激活组织
        this.$emit('input', [])
        this.$emit('active-org-change', null)
        // 同步清空树的勾选状态
        this.syncTreeCheckedKeys()
      } else {
        this.displayText = ''
        this.$emit('input', '')
      }
      this.$emit('clear-val')
    },
    updateDisplayText() {
      if (this.multiple && this.checkedNodes.length > 0) {
        // 不再使用输入框展示，保留变量以兼容外部逻辑
        this.displayText = this.checkedNodes.map(node => node.name).join('、')
      } else {
        this.displayText = ''
      }
    },

    loadNode(node, resolve) {
      const { data } = node
      if (!data || !data.id) return
      this.load(data, resolve)
    },
    async load(node, resolve) {
      const params = {}
      if (node.id) {
        params.parentId = node.id
      }
      const { code, data } = await queryDepartment(params)
      if (code === 0) {
        // 存储所有组织节点信息，用于计算path
        this.storeOrgNodes(data, node.id)
        resolve(data)
      } else {
        resolve([])
      }
    },
    async getOrgList() {
      this.orgList = []
      const params = {}
      if (!this.isAdmin && this.departmentCode) {
        params.parentCode = this.departmentCode
      }
      const { code, data } = await queryDepartment(params)
      if (code === 0) {
        this.orgList = data
        // 存储根级组织节点信息
        this.storeOrgNodes(data, null)
      }
    },

    async getUserOrgNameByIdCardNum() {
      try {
        // 首先使用身份证号查询所人员departmentCode
        const userRes = await queryUserByIdCard({ idCard: this.idCardNum })
        this.userInfo = userRes?.data
        if (
          userRes &&
          userRes.data &&
          userRes.data.userDepartments &&
          userRes.data.userDepartments.length > 0
        ) {
          // 存储根级组织节点信息
          const arr = [...userRes.data.userDepartments]
          arr.map(item => {
            item.id = item.departmentId
            item.name = item.departmentName // 确保有name字段用于显示
          })

          // 设置orgList用于树形组件显示
          this.orgList = arr
          this.storeOrgNodes(arr, null)
          this.departmentCode = arr[0]?.departmentCode
        }
      } catch (error) {
        console.log(error)
      }
    },
    getAllIds(data, result = []) {
      if (!data) return ''
      for (const item of data) {
        if (item.id) {
          result.push(item.id)
        }
        if (item.children) {
          this.getAllIds(item.children, result)
        }
      }
      return result.join(',')
    },
    // 同步树形组件的勾选状态
    syncTreeCheckedKeys() {
      this.$nextTick(() => {
        if (this.$refs.treeRef && this.$refs.treeRef.setCheckedKeys) {
          try {
            this.$refs.treeRef.setCheckedKeys(this.checkedKeys)
          } catch (error) {
            console.warn('同步树形组件勾选状态失败:', error)
          }
        }
      })
    },

    // 存储组织节点信息
    storeOrgNodes(orgData, parentId) {
      if (!orgData || !Array.isArray(orgData)) return

      orgData.forEach(org => {
        if (org.id) {
          // 存储组织节点信息，包含父级ID
          this.allOrgNodes.set(org.id, {
            ...org,
            parentId: parentId
          })
        }
      })
    },

    // 计算组织的path（父级、祖父级id组成的字符串）
    calculateOrgPath(orgId) {
      const path = []
      let currentId = orgId

      // 从当前组织向上追溯，直到根节点
      while (currentId && this.allOrgNodes.has(currentId)) {
        const org = this.allOrgNodes.get(currentId)
        if (org.parentId) {
          path.unshift(org.parentId) // 将父级ID添加到路径开头
        }
        currentId = org.parentId
      }

      const result = path.join(',')
      return result
    },

    // 展开指定的组织节点（供外部调用）
    async expandNodes(nodeIds) {
      if (
        !this.$refs.treeRef ||
        !Array.isArray(nodeIds) ||
        nodeIds.length === 0
      ) {
        console.log('expandNodes 条件不满足:', {
          hasTreeRef: !!this.$refs.treeRef,
          nodeIds: nodeIds
        })
        return
      }

      const treeRef = this.$refs.treeRef

      // 等待节点加载完成（包含懒加载完子节点或确认无子节点）
      const waitForNodeLoaded = async node => {
        let attempts = 0
        const maxAttempts = 40 // ~2秒
        // Element-UI Tree 节点上有 loading 标记，childNodes 加载完成后更新
        while (attempts < maxAttempts) {
          const n = treeRef.getNode(node.key)
          const loading = n && n.loading
          const hasChildrenField = n && Array.isArray(n.childNodes)
          const finished = hasChildrenField && !loading
          // 如果已经没有在loading，且 childNodes 已可用，即认为完成
          if (finished) return
          attempts++
          await new Promise(resolve => setTimeout(resolve, 50))
        }
      }

      // 获取某节点到根的祖先链（不包含自身）
      const getAncestors = id => {
        const result = []
        let current = this.allOrgNodes.get(id)
        const guard = new Set()
        while (current && current.parentId && !guard.has(current.parentId)) {
          guard.add(current.parentId)
          result.unshift(current.parentId)
          current = this.allOrgNodes.get(current.parentId)
        }
        return result
      }

      // 获取节点深度（根为0）
      const getDepth = id => getAncestors(id).length

      // 先确保父链按层级顺序展开，再展开目标节点
      const expandAndWait = async id => {
        // 如果节点在树中不存在，先尝试展开父节点
        let vnode = treeRef.getNode(id)
        if (!vnode) {
          const parent = this.findParentNode(id)
          if (parent && parent.id !== undefined) {
            await expandAndWait(parent.id)
            vnode = treeRef.getNode(id)
          }
        }
        if (vnode && !vnode.expanded) {
          vnode.expand()
          await waitForNodeLoaded(vnode)
        }
      }

      // 组装需要展开的节点集合：所有目标节点及其祖先
      const idsToExpandSet = new Set()
      for (const id of nodeIds) {
        getAncestors(id).forEach(a => idsToExpandSet.add(a))
        idsToExpandSet.add(id)
      }
      const idsToExpand = Array.from(idsToExpandSet)
      // 按深度从小到大排序，确保先展开父，再展开子
      idsToExpand.sort((a, b) => getDepth(a) - getDepth(b))

      for (const id of idsToExpand) {
        await expandAndWait(id)
      }

      // 展开完成后，确保勾选状态正确
      await this.$nextTick()
      if (this.checkedKeys && this.checkedKeys.length > 0) {
        this.syncTreeCheckedKeys()
      }
    },

    // 查找父节点（用于懒加载场景）
    findParentNode(nodeId) {
      // 从已存储的组织节点中查找父节点
      const org = this.allOrgNodes.get(nodeId)
      if (org && org.parentId) {
        return this.allOrgNodes.get(org.parentId)
      }
      return null
    }
  }
}
</script>

<style lang="scss" scoped>
.select-tree {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
}

.selected-tags {
  display: flex;
  align-items: center;
  min-height: 40px;
  width: 500px;
  box-sizing: border-box;
  padding: 5px 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: #fff;
  cursor: pointer;
  transition: border-color 0.2s cubic-bezier(0.645, 0.045, 0.355, 1);

  &:hover {
    border-color: #c0c4cc;
  }
  &.is-focus {
    border-color: #409eff;
  }

  .tags-container {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 4px;
    flex: 1;

    .el-tag {
      margin: 2px 4px 2px 0;
    }

    .placeholder {
      color: #c0c4cc;
      font-size: 14px;
      line-height: 24px;
    }
  }

  .actions {
    display: flex;
    align-items: center;
    gap: 6px;

    .clear-icon {
      color: #c0c4cc;
      cursor: pointer;
    }
    .dropdown-icon {
      color: #c0c4cc;
      transition: transform 0.3s;
      &.is-reverse {
        transform: rotate(180deg);
      }
    }
  }
}

.popover-tree {
  max-height: 240px;
  overflow: auto;
}

.el-icon-close {
  margin-top: 14px;
  cursor: pointer;
}

::v-deep .el-tag--small {
  background-color: #f4f4f5 !important;
  border-color: #e9e9eb !important;
  color: #909399 !important;
  .el-tag__close {
    color: #909399 !important;
    &:hover {
      color: #fff !important;
      background: #909399 !important;
    }
  }
}
::v-deep .el-tag.el-tag--info {
  background-color: #ecf5ff !important;
  border-color: #d9ecff !important;
  color: #409eff !important;
  .el-tag__close {
    color: #409eff !important;
    &:hover {
      color: #fff !important;
      background: #409eff !important;
    }
  }
}

// 激活组织的样式
.org-tag.active-org {
  border: 1px solid #409eff !important;
}

.org-tag {
  transition: all 0.2s ease;
  cursor: pointer;

  &:hover {
    transform: translateY(-1px);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }
}
</style>
