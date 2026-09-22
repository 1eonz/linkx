<template>
  <div class="table-container">
    <el-card v-loading="loading" shadow="always" class="card">
      <div style="margin-bottom: 20px;" >
        <el-button  v-waves type="primary" icon="el-icon-circle-plus-outline" @click="handleAdd">
          新增标签
        </el-button>
        <el-button icon="el-icon-delete" type="danger" @click="batchDeleteLabels">
          批量删除
        </el-button>
      </div>
      <el-tree
        ref="tree"
        class="filter-tree"
        :data="treeData"
        :props="defaultProps"
        default-expand-all
        show-checkbox
        node-key="id"
        :check-strictly="true"
        @check-change="handleCheckChange"
        :filter-node-method="filterNode"
      >
        <span slot-scope="{ node, data }" class="custom-tree-node">
          <span>{{ node.label }}</span>
          <span class="operator">
            <i
              v-if="data.level < 3"
              class="el-icon-plus"
              @click.stop="() => handleAdd(node, data)"
            ></i>
            <i
              class="el-icon-edit"
              @click.stop="() => handleEdit(node, data)"
            ></i>
            <i
              class="el-icon-delete"
              @click.stop="() => handleDelete(node, data)"
            ></i>
          </span>
        </span>
      </el-tree>
    </el-card>
    <!-- 表单弹窗：添加/修改 -->
    <edit-from ref="formRef" @success="getList" />
  </div>
</template>

<script>
import editFrom from './editFrom'
import { labelList, labelDelete, labelBatchDelete } from '@/api/h5/quick'

export default {
  name: 'TreeLabel',
  components: { editFrom },
  data() {
    return {
      treeData: [],
      loading: true,
      defaultProps: {
        children: 'children',
        label: 'name'
      },
      // 选中的标签数组
      selectedLabels: []
    }
  },
  methods: {
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
    },
    async handleAdd(node, data) {
      if (data?.collaborationIds) {
        this.$message({
          message: '当前标签关联了协同岗无法添加子标签',
          type: 'warning'
        })
        return
      }
      this.$refs.formRef.open('create', data)
    },
    handleEdit(node, data) {
      this.$refs.formRef.open('updata', data)
    },
    handleDelete(node, { id }) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(() => {
        labelDelete(id).then(result => {
          if (result.code === 0) {
            this.$message({
              message: this.$t('index.statusTitle.successfullyDelete'),
              type: 'success'
            })
            this.getList()
          } else {
            this.$message({
              message: result.msg,
              type: 'error'
            })
          }
        })
      })
    },
    async getList(param) {
      this.loading = true
      const { code, data } = await labelList(param)
      if (code === 0) {
        this.treeData = data
        this.loading = false
      }
    },
    
    // 处理节点勾选状态变化
    handleCheckChange(data, checked, indeterminate) {
      // 点父节点时级联勾选子节点
      if (data.children && data.children.length > 0) {
        this.cascadeCheckChildren(data, checked)
      }
      // 只获取所有被完全选中的节点，不包含半选状态的节点
      // getCheckedNodes(false, false) 会返回所有被完全选中的节点，包括父节点和叶子节点
      this.selectedLabels = this.$refs.tree.getCheckedNodes(false, false)
    },
    
    // 级联勾选子节点
    cascadeCheckChildren(node, checked) {
      if (node.children && node.children.length > 0) {
        node.children.forEach(child => {
          this.$refs.tree.setChecked(child.id, checked, false)
          // 递归处理子节点的子节点
          this.cascadeCheckChildren(child, checked)
        })
      }
    },
    
    // 批量删除标签
    batchDeleteLabels() {
      if (this.selectedLabels.length === 0) {
        this.$message.warning('请先选择要删除的标签')
        return
      }
      
      // 二次确认
      this.$confirm('确定要删除选中的标签吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const ids = this.selectedLabels.map(item => item.id)
          const { code, msg } = await labelBatchDelete(ids)
          if (code !== 0) {
            return this.$message.error(msg || '删除失败，请重试')
          }
          this.$message.success('删除成功')
          this.getList()
          this.selectedLabels = []
        } catch (error) {
          this.$message.error('删除失败，请重试')
        }
      }).catch(() => {
        // 取消删除
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.filter-tree {
  max-height: calc(100vh - 350px);
  overflow-y: auto;
  margin-left:-20px;
}
.custom-tree-node {
  .operator {
    margin-left: 81px;
    opacity: 0;

    i {
      margin-right: 15px;
      color: rgba(38, 99, 255, 1);
    }

    .el-icon-delete {
      color: rgba(245, 83, 83, 1);
    }
  }

  &:hover {
    .operator {
      opacity: 1;
    }
  }
}
</style>
