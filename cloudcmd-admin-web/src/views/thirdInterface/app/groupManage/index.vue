<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <!-- 查询条件 -->
      <el-form
        ref="queryFormRef"
        class="-mb-15px"
        :inline="true"
        label-width="68px"
        @submit.native.prevent="handleQuery"
      >
        <el-form-item>
          <el-button
            class="filter-item"
            type="primary"
            icon="el-icon-circle-plus-outline"
            @click="handleAdd"
          >
            新增分类
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table
        :data="tableData"
        stripe
        border
        fit
        row-key="id"
        highlight-current-row
        :show-overflow-tooltip="true"
      >
        <el-table-column label="序号" type="index" width="80" />
        <el-table-column label="分类名称" prop="name" width="200" />
        <el-table-column label="绑定应用" prop="appCount">
          <template slot-scope="scope">
            <el-tag
              style="margin-right: 4px;"
              type="info"
              v-for="item in scope.row.appList.slice(0, 5)"
              >{{ item.name }}</el-tag
            >
            <span v-if="scope.row.appList.length > 5"
              >+{{ scope.row.appList.length - 5 }}</span
            >
          </template>
        </el-table-column>
        <el-table-column label="排序值" prop="sort" width="100">
        </el-table-column>
        <el-table-column label="创建时间" prop="gmtCreated" width="180" />
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template slot-scope="scope">
            <el-button
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleEdit(scope.row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 编辑弹窗 -->
      <GroupForm ref="groupFormRef" @success="fetchData()" />
    </el-card>
  </div>
</template>

<script>
import { getGroupPage, deleteGroup } from '@/api/thirdInterface/app'
import GroupForm from './groupForm.vue'

export default {
  name: 'AppGroupManage',
  components: { GroupForm },
  data() {
    return {
      searchForm: {
        name: '',
        type: ''
      },
      tableData: []
    }
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    // 获取列表
    async fetchData() {
      const param = {
        type: 1
      }
      const { code, data } = await getGroupPage(param)
      if (code === 0) {
        this.tableData = data || []
      }
    },

    // 新增
    handleAdd() {
      this.$refs.groupFormRef.open('create')
    },

    // 编辑
    handleEdit(row) {
      this.$refs.groupFormRef.open('edit', row)
    },
    // 删除
    handleDelete(row) {
      this.$confirm('确认删除该分类吗？删除后不可恢复', '提示', {
        type: 'warning'
      })
        .then(async () => {
          const res = await deleteGroup(row.id)
          if (res.code === 0) {
            this.$message.success('删除成功')
            this.fetchData()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        })
        .catch(() => {})
    }
  }
}
</script>

<style scoped lang="scss">
.card {
  width: 100%;
  height: 100%;

  .search-box {
    padding: 16px 0;
    margin-bottom: 15px;
    border-bottom: 1px solid #ebeef5;
  }

  .header {
    display: flex;
    justify-content: flex-end;
    padding: 16px 0;
  }

  .bottom {
    display: flex;
    justify-content: flex-end;
    padding: 24px 0;
  }
}
</style>
