<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          placeholder="名称"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-button
          v-waves
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-search"
          @click="handleFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          v-waves
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-refresh"
          @click="handleReset"
        >
          {{ $t('index.operations.reset') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-plus"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
      </div>
      <el-table
        v-loading="listLoading"
        :data="list"
        border
        fit
        highlight-current-row
        style="width: 100%;"
        :header-cell-style="{ background: '#F9FAFC', color: '#272828' }"
      >
        <el-table-column
          label="名称"
          :show-overflow-tooltip="true"
          min-width="90"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.idCard }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.condition')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag :type="scope.row.status | statusFilter">
              {{
                scope.row.status === 0
                  ? $t('index.list.normal')
                  : $t('index.list.forbidden')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          label="创建时间"
          :show-overflow-tooltip="true"
          min-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.gmtCreated }}</span>
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          min-width="200"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button
              type="warning"
              icon="el-icon-key"
              size="small"
              @click="handleResetPassword(scope.row)"
            >
              重置密码
            </el-button>
            <el-button
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
            >
              {{ $t('index.delete') }}
            </el-button>
            <el-button
              v-if="scope.row.status === 0"
              type="danger"
              icon="el-icon-goods"
              size="small"
              @click="handleStatus(scope.row, 1)"
            >
              {{ $t('index.list.forbidden') }}
            </el-button>
            <el-button
              v-else
              type="warning"
              icon="el-icon-star-on"
              size="small"
              @click="handleStatus(scope.row, 0)"
            >
              {{ $t('index.operations.enabled') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="listQuery.pageNum"
        :limit.sync="listQuery.pageSize"
        @pagination="getList"
      />
    </el-card>

    <!-- 新增/编辑管理员 -->
    <edit-user ref="editUser" @success="getList" />
    <!-- 密码 -->
    <user-password ref="password" @success="getList" />
  </div>
</template>

<script>
import userPassword from '@/views/permission/components/userPassword'
import pagination from '@/components/Pagination'
import editUser from './components/editUser.vue'
import {
  getUserListByPage,
  deleteUser,
  updateUser
} from '@/api/resource/person'
export default {
  name: 'UserManage',
  filters: {
    statusFilter(status) {
      const statusMap = {
        0: 'success',
        1: 'danger'
      }
      return statusMap[status]
    }
  },
  components: { pagination, editUser, userPassword },
  data() {
    return {
      listLoading: false,
      list: [],
      total: 0,
      listQuery: {
        name: '',
        pageSize: 10,
        pageNum: 1
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    // 获取列表
    getList() {
      this.listLoading = true
      getUserListByPage(this.listQuery)
        .then(({ data }) => {
          this.list = data.records
          this.total = data.total
          this.listLoading = false
        })
        .catch(() => {
          this.listLoading = false
        })
    },
    // 搜索
    handleFilter() {
      this.listQuery.pageNum = 1
      this.getList()
    },
    // 重置按钮
    handleReset() {
      this.listQuery.name = ''
      this.getList()
    },
    // 新增管理员
    handleCreate() {
      this.$refs.editUser.init()
    },
    // 编辑管理员
    handleUpdate(row) {
      this.$refs.editUser.init(row)
    },
    // 删除
    handleDelete({ id, name }) {
      this.$confirm(`确认删除用户"${name}"？`, {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'warning'
      })
        .then(() => {
          deleteUser(id).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    },
    // 重置密码
    handleResetPassword(row) {
      this.$refs.password.setData(row, false)
    },
    // 禁用/启用
    handleStatus(row, status) {
      const param = {
        id: row.id,
        status
      }
      updateUser(param).then(result => {
        if (result.code === 0) {
          this.$message({
            message: result.msg || '操作成功',
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
    }
  }
}
</script>

<style scoped lang="scss">
.card {
  height: 100%;
  overflow-y: auto;
}
.filter-container {
  padding-bottom: 10px;
  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }
}
.edit-input {
  padding-right: 50px;
  width: 360px;
}
.edit-select {
  padding-right: 50px;
  width: 360px;
}

.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
</style>
