<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <!-- 查询条件 -->
      <el-form
        ref="queryFormRef"
        class="-mb-15px"
        :model="searchForm"
        :inline="true"
        label-width="68px"
        @submit.native.prevent="handleSearch"
      >
        <el-form-item>
          <el-input
            v-model="searchForm.userName"
            placeholder="用户名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">
            查询
          </el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">
            重置
          </el-button>
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">
            新建
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table
        border
        :data="tableData"
        stripe
        fit
        row-key="id"
        highlight-current-row
        style="width: 100%"
      >
        <el-table-column label="序号" type="index" width="80" />
        <el-table-column label="用户名称" prop="userName" width="150" />
        <el-table-column label="通讯号码" prop="contactNumber" width="150" />
        <el-table-column label="应用ID" prop="appId" width="200" />
        <el-table-column label="应用密钥" prop="appSecret" width="200">
          <template slot-scope="scope">
            <span>{{ maskSecret(scope.row.appSecret) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="是否自动入群" prop="defaultUser" width="120">
          <template slot-scope="scope">
            <span>{{ scope.row.defaultUser ? '是' : '否'}}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" show-overflow-tooltip />
        <el-table-column label="创建时间" prop="createdAt" width="180" />
        <el-table-column label="操作" width="150" fixed="right">
          <template slot-scope="scope">
            <el-button size="small" type="text" @click="handleEdit(scope.row)">
              编辑
            </el-button>
            <el-button
              size="small"
              type="text"
              style="color: red"
              @click="handleDelete(scope.row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 编辑弹窗 -->
      <Edit ref="editRef" @update="fetchData" />
    </el-card>
  </div>
</template>

<script>
import {
  getVirtualUserList,
  deleteVirtualUser
} from '@/api/policeExtend/virtualUser'
import Edit from './edit.vue'
import dayjs from 'dayjs'
export default {
  name: 'VirtualUser',
  components: { Edit },
  data() {
    return {
      searchForm: {
        userName: ''
      },
      tableData: []
    }
  },
  mounted() {
    this.fetchData()
  },
  methods: {
    // 获取列表数据
    async fetchData() {
      const params = {
        ...this.searchForm
      }

      const { code, data } = await getVirtualUserList(params)
      if (code === 0) {
        const records = data || []
        this.tableData = records.map(item => {
          item.createdAt = item.createdAt
            ? dayjs(item.createdAt).format('YYYY-MM-DD HH:mm:ss')
            : ''
            return item
        })
      }
    },

    // 查询
    handleSearch() {
      this.currentPage = 1
      this.fetchData()
    },

    // 重置
    handleReset() {
      this.searchForm = { userName: '' }
      this.currentPage = 1
      this.fetchData()
    },

    // 新建
    handleAdd() {
      this.$refs.editRef.open('add')
    },

    // 编辑
    handleEdit(row) {
      this.$refs.editRef.open('edit', row)
    },

    // 删除
    handleDelete(row) {
      this.$confirm('删除后该账号绑定的智能体将无法在群里收发消息！', '提示', {
        type: 'warning'
      })
        .then(async () => {
          const res = await deleteVirtualUser(row.id)
          if (res.code === 0) {
            this.$message.success('删除成功')
            this.fetchData()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        })
        .catch(() => {})
    },

    // 密钥脱敏显示
    maskSecret(secret) {
      if (!secret) return '-'
      if (secret.length <= 8) return '********'
      return (
        secret.substring(0, 4) + '****' + secret.substring(secret.length - 4)
      )
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
