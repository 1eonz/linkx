<template>
  <div class="dock-container">
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="" prop="systemName">
        <el-input
          v-model="queryParams.systemName"
          placeholder="所属系统"
          clearable
          style="width: 180px;"
          class="filter-item"
          @keyup.enter.native="searchFunc"
        />
      </el-form-item>
      <el-form-item label="" prop="ip">
        <el-input
          v-model="queryParams.ip"
          placeholder="访问IP"
          clearable
          style="width: 180px;"
          class="filter-item"
          @keyup.enter.native="searchFunc"
        />
      </el-form-item>
      <el-form-item label="" prop="path">
        <el-input
          v-model="queryParams.path"
          placeholder="接口名称"
          clearable
          style="width: 180px;"
          class="filter-item"
          @keyup.enter.native="searchFunc"
        />
      </el-form-item>

      <el-form-item>
        <el-button
          v-waves
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-search"
          @click="searchFunc"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-refresh"
          @click="handleReset"
        >
          重置
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="openForm('create')"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <el-table
        border
        v-loading="loading"
        :data="list"
        :show-overflow-tooltip="true"
      >
        <el-table-column
          label="名称"
          show-overflow-tooltip
          align="center"
          prop="name"
        />
        <el-table-column
          label="所属系统"
          show-overflow-tooltip
          align="center"
          prop="systemName"
        />
        <el-table-column
          label="系统编码"
          show-overflow-tooltip
          align="center"
          prop="systemCode"
        />
        <el-table-column label="访问协议" align="center" prop="schema" />
        <el-table-column label="访问IP" align="center" prop="ip" />
        <el-table-column label="端口" align="center" prop="port" />
        <el-table-column
          label="接口名称"
          show-overflow-tooltip
          align="center"
          prop="path"
        />
        <el-table-column
          label="请求方式"
          show-overflow-tooltip
          align="center"
          prop="method"
        />
        <el-table-column
          label="请求头"
          show-overflow-tooltip
          align="center"
          prop="headers"
        />
        <el-table-column
          label="请求体"
          show-overflow-tooltip
          align="center"
          prop="body"
        />
        <el-table-column
          label="请求参数"
          show-overflow-tooltip
          align="center"
          prop="params"
        />
        <el-table-column
          label="操作时间"
          show-overflow-tooltip
          align="center"
          prop="gmtCreated"
          width="200px"
        />
        <el-table-column label="操作" align="center" min-width="180px">
          <template #default="scope">
            <div class="btns">
              <div
                @click="dataAsync(scope.row)"
                :class="scope.row.status === 1 ? 'action-enable' : 'action-disable'"
              >
                {{ scope.row.status === 1 ? '启用' : '禁用' }}
              </div>
              <div class="action-edit" @click="openForm('update', scope.row)">编辑</div>
              <div class="action-delete" @click="handleDelete(scope.row.id)">删除</div>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.current"
        :limit.sync="queryParams.size"
        @pagination="handleQuery"
      />
    </div>
    <!-- 表单弹窗：添加/修改 -->
    <edit-dock ref="formRef" :is-admin="isAdmin" @success="searchFunc" />
  </div>
</template>

<script>
import editDock from './editDock.vue'
import {
  getDockPage,
  deletePoliceticket,
  changeEnable
} from '@/api/policeReport/dock'
export default {
  components: { editDock },
  data() {
    return {
      defaultQueryParams: {
        systemName: '',
        ip: '',
        path: '',
        current: 1,
        size: 10
      },
      queryParams: {
        systemName: '',
        ip: '',
        path: '',
        current: 1,
        size: 10
      },
      list: [],
      loading: false,
      total: 0,
      isAdmin: false
    }
  },
  mounted() {
    this.handleQuery()
  },
  methods: {
    searchFunc() {
      this.queryParams.current = 1
      this.handleQuery()
    },
    handleReset() {
      this.$refs['queryFormRef'].resetFields()
      this.queryParams = {
        ...this.defaultQueryParams
      }
      this.handleQuery()
    },
    async handleQuery() {
      const { code, data } = await getDockPage({ ...this.queryParams })
      if (code === 0) {
        const { total, records } = data
        this.list = records
        this.total = Number(total)
      }
    },
    async openForm(type, row) {
      this.$refs.formRef.open(type, row)
    },
    handleDelete(id) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deletePoliceticket(id).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
              this.searchFunc()
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        })
        .catch(() => {})
    },
    handleOpen(type) {
      this.$refs.formRef.open(type)
    },
    dataAsync(item) {
      const title = item.status === 1 ? '启用' : '禁用'
      this.$confirm(`确认${title}吗？`, {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(async () => {
        const params = {
          id: item.id,
          status: item.status === 1 ? 0 : 1
        }
        const { code } = await changeEnable(params)

        this.$message({
          message: code === 0 ? `${title}成功` : `${title}失败`,
          type: code === 0 ? 'success' : 'error'
        })
        if (code === 0) {
          this.handleQuery()
        }
      })
    }
  }
}
</script>
<style lang="scss" scoped>
.dock-container {
  width: 100%;
  height: 100%;
  flex: 1;
  min-height: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px;
}

.table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

::v-deep .el-table {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;

  .el-table__body-wrapper {
    flex: 1;
    overflow-y: auto;
  }

  .el-table__header th {
    background-color: #f5f7fa;
    color: #606266;
  }
}

.btns {
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;

  div {
    cursor: pointer;
    padding: 0;

    &:not(:last-child)::after {
      content: '|';
      padding: 0 8px;
      color: #eeeff3;
    }
  }

  .action-enable {
    color: rgba(8, 163, 70, 0.85);
  }

  .action-disable {
    color: rgba(245, 83, 83, 1);
  }

  .action-edit {
    color: rgba(38, 99, 255, 1);
  }

  .action-delete {
    color: rgba(245, 83, 83, 1);
  }
}
</style>