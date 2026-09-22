<template>
  <div class="select-tree">
    <el-input
      :value="displayText"
      v-popover:treePopover
      clearable
      :disabled="isDisabled"
      :placeholder="placeholder"
      @input="handleInput"
      @clear="clearVal"
    />

    <el-popover ref="treePopover" placement="bottom-start" trigger="click">
      <el-tree
        ref="treeRef"
        class="popover-tree"
        node-key="id"
        :load="loadNode"
        lazy
        :props="treeProps"
        :default-expand-all="false"
        :highlight-current="true"
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        @current-change="currentChange"
      >
        <template #default="{ data }">
          <span
            class="select-tree-node"
            :class="{ 'is-disabled': isNodeDisabled(data) }"
          >{{ data.name }}</span>
        </template>
      </el-tree>
    </el-popover>
  </div>
</template>

<script>
import { getOrgPrivTree} from '@/api/h5/collaboration'
import { getIdCardNum } from '@/utils/auth'
export default {
  name: 'SelectTree',
  props: {
    data: {
      type: Array,
      default: () => []
    },
    value: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    },
    isInitValue: {
      type: Boolean,
      default: false
    },
    isDisabled: {
      type: Boolean,
      default: false
    },
    // 目标节点ID（跨节点查询部门时使用）
    peerId: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      // 显示值（选中节点名称或父组件传入值）
      displayText: '',
      // 过滤值（仅用户输入时更新，选中节点不更新此值，避免树被过滤）
      filterText: '',
      treeProps: {
        label: 'name',
        children: 'children'
      },
      idCardNum: getIdCardNum(),
    }
  },
  watch: {
    value(val) {
      this.displayText = val
    },
  },
  mounted() {
    if (this.isInitValue) {
      this.displayText = this.value
    }
  },

  methods: {
    // 判断节点是否被禁用
    isNodeDisabled(data) {
      return data?.hasPermission === false
    },
    // 用户输入时，同时更新显示值和过滤值
    handleInput(val) {
      this.displayText = val
      this.filterText = val
      this.$refs.treeRef.filter(val)
    },
    currentChange(data) {
      // 禁用节点拦截：不更新值、不关闭弹窗
      if (this.isNodeDisabled(data)) {
        return
      }
      // 选中节点时只更新显示值，并清除过滤状态，避免下次打开时节点被过滤
      this.displayText = data.name
      this.filterText = ''
      this.$refs.treeRef && this.$refs.treeRef.filter('')
      this.$emit('current-change', data)
      this.$refs.treePopover.doClose()
    },
    clearVal() {
      this.displayText = ''
      this.filterText = ''
      this.$refs.treeRef && this.$refs.treeRef.filter('')
      this.$emit('clear-val')
    },
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
    },
    async loadNode(node, resolve) {
      const res = await this.loadDepartMentList(node?.data?.code)
      return resolve(res)
    },
    async loadDepartMentList(code = '') {
      const params = {
        includeChildren: 0,
        parentCode: code,
        parentId: this.peerId
      }
      try {
        const res = await getOrgPrivTree(params)
        if (res.code === 0 && res.data && res.data.length > 0) {
          return res.data
        } else {
          return []
        }
      } catch (error) {
        return []
      }
    },
  }
}
</script>

<style lang="scss" scoped>
.select-tree {
  display: inline-block;
  vertical-align: middle;
  margin-bottom: 10px;
}

.popover-tree {
  max-height: 240px;
  overflow: auto;
}

.el-icon-close {
  margin-top: 14px;
  cursor: pointer;
}

.select-tree-node {
  display: inline-block;
  width: 100%;
  &.is-disabled {
    color: #c0c4cc;
    cursor: not-allowed;
    pointer-events: none;
  }
}
</style>
