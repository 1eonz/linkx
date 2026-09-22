<template>
  <div class="select-tree">
    <el-input
      v-model="displayText"
      @input="onInput"
      v-popover:treePopover
      clearable
      :placeholder="placeholder"
      @clear="clearVal"
    />

    <el-popover
      ref="treePopover"
      placement="bottom-start"
      trigger="click"
      popper-class="select-tree-popper"
    >
      <el-tree
        ref="treeRef"
        class="popover-tree"
        node-key="id"
        :data="orgList"
        :props="treeProps"
        :default-expand-all="false"
        :highlight-current="true"
        :expand-on-click-node="false"
        :filter-node-method="filterNode"
        :lazy="lazy"
        :load="loadNode"
        @current-change="currentChange"
      />
    </el-popover>
  </div>
</template>

<script>
import { queryDepartment } from '@/api/h5/collaboration'
export default {
  name: 'SelectTreeLoading',
  props: {
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
    lazy: {
      type: Boolean,
      default: false
    },
    isAdmin: {
      type: Boolean,
      default: false
    },
    departmentCode: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      displayText: '',
      filterText: '',
      treeProps: {
        label: 'name',
        children: 'children'
      },
      orgList: []
    }
  },
  watch: {
    value(val) {
      this.displayText = val
    }
  },
  mounted() {
    this.getOrgList()
    if (this.isInitValue) {
      this.displayText = this.value
    }
  },
  methods: {
    currentChange(data) {
      // 设置展示文本，但不要把名称当作过滤关键词
      this.displayText = data.name
      // 清空树过滤，避免因过滤导致展开箭头消失
      this.filterText = ''
      this.$refs.treeRef.filter('')
      this.$emit('current-change', data)
      this.$refs.treePopover.doClose()
    },
    clearVal() {
      this.displayText = ''
      this.filterText = ''
      this.$refs.treeRef && this.$refs.treeRef.filter('')
      this.$emit('clear-val')
    },
    onInput(val) {
      this.filterText = val
      this.$refs.treeRef.filter(val)
    },
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
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
      if (!this.isAdmin) {
        this.orgIds = this.getAllIds(data)
      }
      if (code === 0) {
        this.orgList = data
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

.popover-tree {
  max-height: 240px;
  overflow: auto;
}

.el-icon-close {
  margin-top: 14px;
  cursor: pointer;
}
</style>
