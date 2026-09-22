<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          :placeholder="$t('index.list.roleName')"
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
          :label="$t('index.list.roleName')"
          :show-overflow-tooltip="true"
          min-width="90"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.condition')"
          :show-overflow-tooltip="true"
          min-width="100"
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
          fixed="right"
          header-align="center"
          align="center"
          min-width="90"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="bindUser(scope.row)"
            >
              绑定用户
            </el-button>
            <el-button
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/role/delete') && isShow(scope.row)"
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
            >
              {{ $t('index.delete') }}
            </el-button>
            <el-button
              v-if="
                scope.row.status === 0 &&
                  hasPerm('/admin/role/update') &&
                  isShow(scope.row)
              "
              type="danger"
              icon="el-icon-goods"
              size="small"
              @click="handleStatus(scope.row, 1)"
            >
              {{ $t('index.list.forbidden') }}
            </el-button>
            <el-button
              v-else-if="
                scope.row.status !== 0 &&
                  hasPerm('/admin/role/update') &&
                  isShow(scope.row)
              "
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

    <!-- 新增/修改角色 -->
    <edit-role ref="editRole" @success="getList" />
    <bindUser ref="bindUser" @success="getList" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import editRole from './components/editRole.vue'
import bindUser from './components/bindUser.vue'
import { getRoleList, updateRole, deleteRole } from '@/api/resource/role'
let allList = []
export default {
  name: 'ImRole',
  filters: {
    statusFilter(status) {
      const statusMap = {
        0: 'success',
        1: 'danger'
      }
      return statusMap[status]
    }
  },
  components: { pagination, editRole, bindUser },
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
    isShow(row) {
      return +row.id !== 2 && +row.id !== 6
    },
    // 获取
    getList() {
      this.listLoading = true
      getRoleList(this.listQuery).then(({ data }) => {
        allList = data.records
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    },
    // 搜索
    handleFilter() {
      this.listQuery.pageNum = 1
      // const { name } = this.listQuery
      // if (name) {
      //   this.list = allList.filter(item => {
      //     return item.name.indexOf(name) !== -1
      //   })
      //   this.total = this.list.length
      // } else {
      //   this.getList()
      // }
      this.getList()
    },
    // 新增角色权限
    handleCreate() {
      this.$refs.editRole.init()
    },
    // 重置按钮
    handleReset() {
      this.listQuery.name = ''
      this.getList()
    },
    // 修改角色权限
    handleUpdate(row) {
      this.$refs.editRole.init(row)
    },
    // 删除角色
    handleDelete({ id, name }) {
      let confirmMsg = this.$t('index.operations.affirmDeleted')
      if (confirmMsg.includes('确认删除')) {
        confirmMsg = confirmMsg.replace('确认删除', '确认删除' + name)
      }
      this.$confirm(confirmMsg, {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'warning'
      })
        .then(() => {
          deleteRole([id]).then(result => {
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
    // 禁用/启用
    handleStatus(row, status) {
      const param = {
        id: row.id,
        status
      }
      updateRole(param).then(result => {
        if (result.code === 0) {
          this.getList()
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
      })
    },
    // 绑定用户
    bindUser(row) {
      this.$refs.bindUser.init(row)
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
