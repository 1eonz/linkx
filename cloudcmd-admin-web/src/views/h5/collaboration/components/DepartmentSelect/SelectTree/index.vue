<template>
  <div class="select-tree">
    <el-input
      v-model="inputText"
      v-popover:treePopover
      clearable
      :placeholder="placeholder"
      :disabled="isDisabled"
      @input="handleInput"
      @clear="clearVal"
    />

    <el-popover ref="treePopover" placement="bottom-start" trigger="click">
      <el-tree
        ref="treeRef"
        class="popover-tree"
        node-key="id"
        :data="data"
        :props="treeProps"
        :default-expand-all="true"
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
import { getIdCardNum } from '@/utils/auth'
import { getOrgPrivTree } from '@/api/h5/collaboration'
export default {
  name: 'SelectTree',
  props: {
    // data: {
    //   type: Array,
    //   default: () => []
    // },
    value: {
      type: String,
      default: ''
    },
    placeholder: {
      type: String,
      default: ''
    },
    isDisabled: {
      type: Boolean,
      default: false
    },
    isInitValue: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      inputText: '',
      filterKeyword: '',
      treeProps: {
        label: 'name',
        children: 'children'
      },
      data: [],
      idCardNum: getIdCardNum(),
    }
  },
  watch: {
    value(val) {
      this.inputText = val
    }
  },
  mounted() {
    if (this.isInitValue) {
      this.inputText = this.value
    }
    this.getTreeData()
  },
  methods: {
    // 判断节点是否被禁用
    isNodeDisabled(data) {
      return data?.hasPermission === false
    },
    async getTreeData() {
      const { code, data } = await getOrgPrivTree({})
      if (code === 0) {
        this.data = data
      }
    },
    handleInput(val) {
      this.filterKeyword = val
      this.$refs.treeRef && this.$refs.treeRef.filter(val)
    },
    currentChange(data) {
      // 禁用节点拦截：不更新值、不关闭弹窗
      if (this.isNodeDisabled(data)) {
        return
      }
      this.inputText = data.name
      this.filterKeyword = ''
      this.$nextTick(() => {
        this.$refs.treeRef && this.$refs.treeRef.filter('')
      })
      this.$emit('current-change', data)
      this.$refs.treePopover.doClose()
    },
    clearVal() {
      this.inputText = ''
      this.filterKeyword = ''
      this.$nextTick(() => {
        this.$refs.treeRef && this.$refs.treeRef.filter('')
      })
      this.$emit('clear-val')
    },
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
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
