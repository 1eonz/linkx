<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <!-- 搜索工作栏 -->
      <el-form
        ref="queryFormRef"
        class="-mb-15px"
        :model="queryParams"
        :inline="true"
        label-width="68px"
        @submit.native.prevent="handleQuery"
      >
        <el-form-item label="" prop="name">
          <el-input
            v-model="queryParams.name"
            placeholder="应用名称"
            clearable
            style="width: 300px;"
            class="filter-item"
            @keyup.enter="handleQuery"
          />
        </el-form-item>
        <el-form-item >
          <el-button
            v-waves
            class="filter-item"
            style="margin-left: 10px;"
            type="primary"
            icon="el-icon-search"
            @click="handleQuery"
          >
            {{ $t('index.operations.search') }}
          </el-button>
          <el-button
            class="filter-item"
            style="margin-left: 10px;"
            type="primary"
            icon="el-icon-refresh"
            @click="resetQuery"
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

      <!-- 列表 -->
      <el-table
        v-loading="loading"
        :data="list"
        stripe
        border
        fit
        row-key="id"
        highlight-current-row
        :show-overflow-tooltip="true"
      >
        <el-table-column
          label="序号"
          align="center"
          width="50px"
          type="index"
        />
        <el-table-column
          label="应用图标"
          align="center"
          prop="icon"
          width="100"
        >
          <template #default="{ row }">
            <authImg v-if="row.icon" class="head-shot" :auth-src="row.icon" />
          </template>
        </el-table-column>
        <el-table-column
          label="应用名称"
          prop="name"
          width="150"
          show-overflow-tooltip
        />
        <el-table-column
          label="应用链接/包名"
          prop="packageName"
          show-overflow-tooltip
        />
        <el-table-column label="跳转参数" prop="params" show-overflow-tooltip />
        <el-table-column label="应用ID" prop="appId" show-overflow-tooltip />
        <el-table-column label="应用类型" width="80px">
          <template #default="{ row }">
            <span>{{ appTypeMap[row.type] || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="应用展示范围" width="120px" align="center">
          <template #default="{ row }">
            <span>{{ formatScope(row.scopeList) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="业务区域" width="80px" align="center">
          <template #default="{ row }">
            <span>{{ row.type === 3 ? '-' : appZoneMap[row.zone] || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="配置前置应用" align="center" width="80">
          <template #default="{ row }">
            <span>{{ hasPreAppMap[Boolean(row.prerequisite)] || '' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" prop="createTime" width="180" />
        <el-table-column key="status" label="上架" width="100">
          <template #default="scope">
            <el-switch
              v-model="scope.row.status"
              :active-value="0"
              :inactive-value="1"
              @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>
        <el-table-column
          label="操作"
          align="center"
          fixed="right"
          min-width="120px"
          width="200"
        >
          <template #default="scope">
            <div style="text-align: left">
              <el-button
                type="primary"
                icon="el-icon-edit"
                size="small"
                @click="openForm('update', scope.row.id)"
              >
                {{ $t('index.operations.change') }}
              </el-button>
              <!-- 设备调度、位置共享、文件管理 协同群组 值班信息 不展示删除按钮 -->
              <el-button
                v-if="!(scope.row.official === 1)"
                type="danger"
                icon="el-icon-delete"
                size="small"
                @click="handleDelete(scope.row.id)"
              >
                {{ $t('index.delete') }}
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页 -->
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
      />

      <!-- 表单弹窗：添加/修改 -->
      <app-form ref="formRef" @success="getList" />
    </el-card>
  </div>
</template>

<script>
import { getInfoPage, deleteInfo, updateStatus } from '@/api/h5/app'
import pagination from '@/components/Pagination'
import authImg from '@/components/AuthImg'
import appForm from './appForm'

export default {
  name: 'AppInfo',
  components: { pagination, appForm, authImg },
  data() {
    // app类型，app区域，是否有前置应用的映射
    const appTypeMap = { 0: 'H5应用', 1: 'App应用', 3: '前置应用' }
    const appZoneMap = { 1: '一类区', 2: '二类区', 3: '三类区' }
    const hasPreAppMap = { true: '是', false: '否' }
    return {
      list: [],
      loading: true,
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: undefined,
        url: undefined,
        icon: undefined,
        sort: undefined
      },
      appTypeMap,
      appZoneMap,
      hasPreAppMap,
      scopeMap: {
        1: '鸿蒙移动端',
        2: '安卓移动端',
        4: 'PC浏览器',
        8: 'PC桌面端'
      }
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const { data } = await getInfoPage(this.queryParams)
        this.list = data.records.map(item => {
          return {
            ...item,
            packageName: item.type !== 0 ? item.packageAndroid : item.url
          }
        })
        this.total = data.total
      } finally {
        this.loading = false
      }
    },

    /** 添加/修改操作 */
    openForm(type, id) {
      this.$refs.formRef.open(type, id)
    },

    /** 删除按钮操作 */
    async handleDelete(id) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteInfo(id).then(result => {
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

    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    /** 重置按钮操作 */
    resetQuery() {
      this.$refs['queryFormRef'].resetFields()
      this.handleQuery()
    },
    async handleStatusChange(row) {
      const text = row.status === 0 ? '上架' : '下架'
      this.$confirm('确认要' + text + '"' + row.name + '"应用吗?', {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          updateStatus(row.id, row.status).then(result => {
            if (result.code === 0) {
              this.$message({
                message: text + '成功',
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
        .catch(() => {
          row.status = row.status === 0 ? 1 : 0
        })
    },
    // 格式化应用展示范围
    formatScope(scope) {
      if (!Array.isArray(scope) || !scope.length) {
        return ''
      }
      return scope.map(item => this.scopeMap[item]).filter(Boolean).join('、')
    }
  }
}
</script>
<style lang="scss" scoped>
.card {
  height: 100%;
  overflow-y: auto;
}
.head-shot {
  display: inline-block;
  height: 40px;
  width: 40px;
}
</style>
