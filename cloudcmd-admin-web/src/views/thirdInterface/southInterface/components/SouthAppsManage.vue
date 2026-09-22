<template>
  <div class="dock-container">
    <el-form ref="queryFormRef" class="-mb-15px" :model="queryParams" :inline="true" label-width="68px" @submit.native.prevent>
      <el-form-item label="" prop="name">
        <el-input v-model="queryParams.name" placeholder="名称" clearable style="width: 180px;" class="filter-item" @keyup.enter.native="searchList" />
      </el-form-item>
      <el-form-item>
        <el-button v-waves class="filter-item" style="margin-left: 10px;" type="primary" icon="el-icon-search" @click="searchList">
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="el-icon-refresh" @click="handleReset">
          重置
        </el-button>
        <el-button class="filter-item" style="margin-left: 10px;" type="primary" icon="el-icon-circle-plus-outline" @click="openForm('create')">
          {{ $t('index.operations.Added') }}
        </el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <el-table border v-loading="loading" :data="list" :show-overflow-tooltip="true">
        <el-table-column label="名称" show-overflow-tooltip align="center" prop="name" />
        <el-table-column label="所属系统" show-overflow-tooltip align="center" prop="systemName" />
        <el-table-column label="系统编码" show-overflow-tooltip align="center" prop="systemCode" />                        
        <el-table-column label="唯一标识" show-overflow-tooltip align="center" prop="uniqueId" />                        
        <el-table-column label="应用类型" align="center" prop="type">
          <template #default="scope">
            {{ getTypeLabel(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column label="展示范围" align="center" prop="scope">
          <template #default="scope">
            {{ getScopeLabel(scope.row.scope) }}
          </template>
        </el-table-column>
        <el-table-column label="访问IP" align="center" prop="ip" />
        <el-table-column label="端口" align="center" prop="port" />
        <el-table-column label="执行周期" align="center" prop="period">
          <template #default="scope">
            {{ formatPeriod(scope.row.period) }}
          </template>
        </el-table-column>
        <el-table-column label="创建时间" show-overflow-tooltip align="center" prop="gmtCreated" width="200px" />
        <el-table-column label="操作" align="center" width="350px">
          <template #default="scope">
            <div class="btns">
              <div class="action-edit" @click="openForm('update', scope.row)">编辑</div>
              <div class="action-mapper" @click="openMapperForm('update', scope.row)">映射字段</div>
              <div class="action-detail" @click="handleDetail(scope.row)">详情</div>
              <div class="action-delete" @click="handleDelete(scope.row.id)">删除</div>
              <div class="action-config" @click="openTaskConfig(scope.row)">关联任务标准件</div>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.page"
        :limit.sync="queryParams.pageSize"
        @pagination="handleQuery"
      />
    </div>
    <AppsManageEditModal ref="formRef" :is-admin="isAdmin" @success="searchList" />
    <AppsManageMapperModal ref="mapperModalRef" @save="handleMapperSave" />
    <AppsManageDetailModal ref="detailModalRef" />
    <TaskConfigModal ref="taskConfigModalRef"/>
  </div>
</template>

<script>
import AppsManageEditModal from './AppsManageEditModal.vue'
import AppsManageMapperModal from './AppsManageMapperModal.vue'
import AppsManageDetailModal from './AppsManageDetailModal.vue'
import TaskConfigModal from './TaskConfigModal.vue'
import {
  getCallableAppList,
  deleteCallableApp,
  getCallableAppDetail,
  updateCallableAppMapper
} from '@/api/thirdInterface/southInterface'

const createDefaultQueryParams = () => ({
  name: '',
  page: 1,
  pageSize: 10
})

export default {
  name: 'SouthAppsManage',
  components: { AppsManageEditModal, AppsManageMapperModal, AppsManageDetailModal, TaskConfigModal },
  data() {
    return {
      queryParams: createDefaultQueryParams(),
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
    getTypeLabel(type) {
      const typeMap = { 1: 'RESTful接口',  2: '数据库' }
      return typeMap[type] || '未知'
    },

    getScopeLabel(scope) {
      const scopeMap = { 0: '全部', 1: 'PC端', 2: '移动端' }
      return scopeMap[scope] || '未知'
    },

    formatPeriod(period) {
      if (!period) return '-'
      const minutes = Number(period)
      if (minutes >= 60) {
        const hours = Math.floor(minutes / 60)
        return `${hours}小时`
      }
      return `${minutes}分钟`
    },

    searchList() {
      this.queryParams.page = 1
      this.handleQuery()
    },

    handleReset() {
      this.$refs.queryFormRef.resetFields()
      this.queryParams = createDefaultQueryParams()
      this.handleQuery()
    },

    async handleQuery() {
      this.loading = true
      try {
        const { code, data } = await getCallableAppList({ ...this.queryParams })
        if (code === 0) {
          const { total, records } = data
          this.list = records || []
          this.total = Number(total) || 0
        }
      } finally {
        this.loading = false
      }
    },

    openForm(type, row) {
      this.$refs.formRef.open(type, row)
    },

    async handleDelete(id) {
      try {
        await this.$confirm(this.$t('index.operations.affirmDeleted'), {
          confirmButtonText: this.$t('index.determine'),
          cancelButtonText: this.$t('index.cancel'),
          type: 'info'
        })
        const result = await deleteCallableApp(id)
        if (result.code === 0) {
          this.$message.success(this.$t('index.statusTitle.successfullyDelete'))
          this.searchList()
        } else {
          this.$message.error(result.msg)
        }
      } catch {
        // 用户取消
      }
    },

    async handleDetail(row) {
      try {
        this.$refs.detailModalRef.open(row);
      } catch (error) {
        console.log(error)
        this.$message.error('获取详情失败')
      }
    },

    getDbTypeLabel(dbType) {
      const dbTypeMap = { 1: 'MySQL', 2: 'Oracle', 3: 'SQLServer' }
      return dbTypeMap[dbType] || '未知'
    },

    openMapperForm(type, row) {
      this.$refs.mapperModalRef.open(type, row)
    },

    async handleMapperSave(data) {
      if (!data.id) {
        this.$message.error('缺少应用ID')
        return
      }
      try {
        const { code } = await updateCallableAppMapper(data.id, { mapper: data.mapper })
        if (code === 0) {
          this.$message.success('映射配置保存成功')
          this.searchList()
        }
      } catch (error) {
        this.$message.error('保存映射配置失败')
      }
    },

    openTaskConfig(row) {
      this.$refs.taskConfigModalRef.open(row)
    },
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
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;

  ::v-deep .el-card__body {
    flex: 1;
    height: 100%;
    display: flex;
    flex-direction: column;
    overflow: hidden;
  }
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

  .action-edit {
    color: rgba(38, 99, 255, 1);
  }

  .action-mapper {
    color: rgba(38, 99, 255, 1);
  }

  .action-detail {
    color: rgba(38, 99, 255, 1);
  }

  .action-delete {
    color: rgba(245, 83, 83, 1);
  }

  .action-config {
    color: rgba(38, 99, 255, 1);
  }
}
</style>

<style lang="scss">
.detail-dialog {
  .detail-content {
    p {
      margin: 8px 0;
      line-height: 1.6;
      
      strong {
        display: inline-block;
        min-width: 80px;
        color: #606266;
      }
    }
  }
}
</style>
