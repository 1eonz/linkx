<template>
  <div class="select-tree">
    <el-select
      v-model="selectValue"
      class="edit-input"
      :placeholder="$t('index.operations.selects')"
      multiple
      collapse-tags
      clearable
      @clear="clearAll"
      @remove-tag="remove"
    >
      <el-option :value="selectValue" style="height: auto">
        <el-tree
          ref="tree"
          :data="data"
          :default-expand-all="false"
          :check-strictly="strictly"
          show-checkbox
          node-key="id"
          highlight-current
          :props="defaultProps"
          :expand-on-click-node="false"
          :default-checked-keys="modelValue"
          @check-change="handleCheckChange"
        />
      </el-option>
    </el-select>
  </div>
</template>

<script>
import _ from 'lodash'

export default {
  name: 'SelectTree',
  props: {
    data: {
      type: Array,
      default: () => []
    },
    modelValue: {
      type: Array,
      default: () => []
    },
    strictly: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      defaultProps: {
        label: 'name',
        children: 'children'
      },
      selectValue: []
    }
  },
  mounted() {
    this.selectValue = this.modelValue
    this.handleCheckChange();
  },
  methods: {
    currentChange(data) {
      this.$emit('current-change', data)
      this.$refs.treePopover.doClose()
    },
    clearAll() {
      this.$refs.tree.setCheckedKeys([])
      this.$emit('check-change', [], [])
    },
    remove(name) {
      const res = this.$refs.tree.getCheckedNodes(true, false)
      const arr = res.filter(item => item.name !== name)
      this.$refs.tree.setCheckedNodes(arr)
      const arrIds = []
      res.forEach(item => {
        arrIds.push(item.id)
      })
      this.$emit('check-change', arrIds, arr)
    },
    // 多选
    handleCheckChange: _.debounce(function() {
      // 1.是否只是叶子节点
      // 2.选择的时候不包含父节点
      const res = this.$refs.tree.getCheckedNodes(false, false)
      const arrLabel = []
      const arrIds = []
      res.forEach(item => {
        arrLabel.push(item.name)
        arrIds.push(item.id)
      })
      this.selectValue = arrLabel
      this.$emit('modelValue:update', arrLabel)
      this.$emit('check-change', arrIds, res)
    }, 500)
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
