<!-- seleteDepartmentTree.vue -->
<template>
  <div class="select-department-container">
    <!-- 左侧：部门树 -->
    <div class="left-panel">
      <div class="panel-header">
        <span class="panel-title">部门列表</span>
      </div>
      <div class="panel-content">
        <DepartmentTree ref="departmentTree" @checkChange="handleCheckChange" />
      </div>
    </div>
    <div class="remove-btn">
      <el-button
        circle
        icon="el-icon-arrow-left"
        :disabled="checkList.length === 0"
        @click="removeSelected"
      />
    </div>
    <!-- 右侧：已选部门 -->
    <div class="right-panel">
      <div class="panel-header">
        <el-checkbox v-model="checkAllFlag" @change="checkAll" />
        <span class="panel-title">已选择</span>
      </div>
      <div class="panel-content">
        <el-checkbox-group v-model="checkList">
          <el-checkbox
            v-for="node in selectedNodes"
            :key="node.id"
            class="selected-checkbox"
            :label="node.id"
            >{{ node.name }}</el-checkbox
          >
        </el-checkbox-group>
      </div>
    </div>
  </div>
</template>

<script>
import DepartmentTree from './DepartmentTree.vue'

export default {
  name: 'SelectDepartmentTree',
  components: { DepartmentTree },
  props: {
    // 已选中的部门ID列表
    value: {
      type: Array,
      default: () => []
    },
    // 是否显示搜索框
    showFilter: {
      type: Boolean,
      default: true
    },
    // 最大选择数量
    maxSelect: {
      type: Number,
      default: 0 // 0 表示不限制
    }
  },
  data() {
    return {
      checkAllFlag: false, // 全选状态
      checkList: [], // 勾选的数据
      selectedNodes: []
    }
  },
  methods: {
    /**
     * 处理树节点选中变化
     */
    handleCheckChange(data) {
      this.checkList = []
      this.checkAllFlag = false
      // 更新已选列表
      this.selectedNodes = data.map(item => {
        return {
          id: item.id,
          name: item.name
        }
      })
      this.$emit('change', this.getSelectedKeys())
    },

    /**
     * 移除选中的节点
     */
    removeSelected() {
      this.$refs.departmentTree.removeSelected(this.checkList)
      // 触发更新
      this.$emit('change', this.getSelectedKeys())
    },

    /**
     * 选中所有
     */
    checkAll(val) {
      this.checkList = val ? this.selectedNodes.map(node => node.id) : []
    },

    /**
     * 获取选中的节点 ID 列表
     */
    getSelectedKeys() {
      return this.selectedNodes.map(node => node.id)
    },

    /**
     * 设置选中的节点
     */
    setSelectedNodes(nodes) {
      const keys = nodes.map(node => node.id)
      const tree = this.$refs.departmentTree?.$refs?.dataAuthTree
      if (tree) {
        tree.setCheckedKeys(keys)
        this.handleCheckChange()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.select-department-container {
  display: flex;
  height: 350px;
  border-radius: 4px;
  overflow: hidden;
}

.left-panel,
.right-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border: 1px solid #dcdfe6;
}
.remove-btn {
  align-self: center;
  margin: 0 14px;
}
.panel-header {
  display: flex;
  align-items: center;
  padding: 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #dcdfe6;
  gap: 8px;
}

.panel-title {
  font-size: 14px;
  font-weight: 500;
  color: #303133;
}

.selected-count {
  font-size: 12px;
  color: #909399;
}

.filter-input {
  flex: 1;
  margin-left: 12px;
}

.panel-content {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  .selected-checkbox {
    display: flex;
    align-items: center;
    margin-bottom: 8px;
    ::v-deep .el-checkbox__label {
      max-width: 300px;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
  }
}

// 滚动条样式
.panel-content::-webkit-scrollbar {
  width: 6px;
}

.panel-content::-webkit-scrollbar-thumb {
  background: #dcdfe6;
  border-radius: 3px;

  &:hover {
    background: #c0c4cc;
  }
}
</style>
