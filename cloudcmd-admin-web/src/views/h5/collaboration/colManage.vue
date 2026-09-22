<template>
  <div class="table-container">
    <Search
      ref="searchRef"
      :is-main="true"
      :is-admin="isAdmin"
      :org-ids="orgId"
      :department-code="departmentCode"
      :hidden-sync="hiddenSync"
      :sync-loading="syncLoading"
      :department-sync-sign="departmentSyncSign"
      @query="getList"
      @reset="resetQuery"
      @open="openForm('create')"
      @sync="syncPostFromImFunc"
      @del-batch="delBatch"
    />
    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
      class="flex-table"
      @selection-change="handleSelect"
    >
      <el-table-column type="selection" width="55" />
      <el-table-column
        label="协同岗名称"
        show-overflow-tooltip
        align="center"
        prop="postName"
      />
      <el-table-column label="图标" align="center" width="100">
        <template #default="{ row }">
          <authImg
            v-if="row.iconUrl"
            class="head-shot"
            :auth-src="row.iconUrl"
          />
        </template>
      </el-table-column>
      <el-table-column label="协同岗类型" align="center" prop="operationType">
        <template #default="{ row }">
          {{ row.type === 1 ? '人员核查协同岗' : '普通协同岗' }}
        </template>
      </el-table-column>
      <el-table-column
        label="警单类型"
        align="center"
        prop="ticketTypeNames"
        show-overflow-tooltip
      >
      </el-table-column>

      <el-table-column
        label="所属组织"
        show-overflow-tooltip
        align="center"
        prop="orgName"
      />
      <el-table-column
        label="关联人员"
        show-overflow-tooltip
        align="center"
        prop="relatedUserNames"
      />
      <el-table-column label="关联人数" align="center">
        <template #default="{ row }">
          {{ getRelatedUserCount(row.relatedUserIds) }}
        </template>
      </el-table-column>
      <el-table-column label="操作人员" align="center" prop="operatorName" />
      <el-table-column label="操作类型" align="center" prop="operationType">
        <template #default="{ row }">
          {{ row.operationType === 1 ? '创建' : '修改' }}
        </template>
      </el-table-column>
      <el-table-column label="数据来源" align="center" prop="source">
        <template #default="{ row }">
          {{ row.source === 1 ? '历史同步' : '新建数据' }}
        </template>
      </el-table-column>
      <el-table-column
        label="操作时间"
        align="center"
        prop="updateTime"
        sortable
      />
      <el-table-column label="操作" align="center" min-width="300px">
        <template #default="scope" align="center">
          <el-button
            type="primary"
            icon="el-icon-edit"
            size="small"
            @click="openForm('update', scope.row)"
          >
            {{ $t('index.operations.change') }}
          </el-button>
          <el-button
            type="danger"
            icon="el-icon-delete"
            size="small"
            @click="handleDelete(scope.row.id)"
          >
            {{ $t('index.delete') }}
          </el-button>
          <el-button
            type="warning"
            size="small"
            :loading="listOffDutyLoadingMap[scope.row.id]"
            :disabled="isAnyOffDutyLoading || offDutyDialogVisible"
            icon="el-icon-switch-button"
            @click="handleOffDuty(scope.row)"
          >
            {{ $t('index.collaboration.offDuty') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
    <!-- 分页 -->
    <pagination
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="changePage"
    />

    <!-- 表单弹窗：添加/修改 -->
    <col-form
      ref="formRef"
      :is-admin="isAdmin"
      :department-code="departmentCode"
      @success="handleQuery"
    />

    <!-- 下岗弹窗 -->
    <OffDutyDialog
      :visible.sync="offDutyDialogVisible"
      :post-info="currentOffDutyPost"
      @success="handleQuery"
    />
  </div>
</template>
<script>
import {
  getCollaborationPage,
  deleteCollaboration,
  delBatchCollaboration,
  syncPostFromIm,
  getImSyncStatus,
  getOnDutyUsersByPostId
} from '@/api/h5/collaboration'
import pagination from '@/components/Pagination'
import authImg from '@/components/AuthImg'
import colForm from './colForm.vue'
import Search from './search.vue'
import OffDutyDialog from './components/OffDutyDialog.vue'
import { pageLoadingUtils } from '@/utils/pageLoading'
export default {
  name: 'ColManage',
  components: { pagination, authImg, colForm, Search, OffDutyDialog },
  props: {
    isAdmin: {
      type: Boolean,
      default: false
    },
    orgId: {
      type: String,
      default: ''
    },
    departmentCode: {
      type: String,
      default: ''
    },
    departmentSyncSign: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      list: [],
      loading: true,
      total: 0,
      hiddenSync: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10
      },
      multipleSelection: '',
      filterQuery: {},
      syncLoading: false,
      // 下岗功能相关
      offDutyDialogVisible: false, // 下岗弹窗显示状态
      currentOffDutyPost: null, // 当前操作的协同岗
      listOffDutyLoadingMap: {} // 列表下岗按钮的loading状态
    }
  },
  computed: {
    /** 是否有任何下岗按钮正在loading */
    isAnyOffDutyLoading() {
      return Object.values(this.listOffDutyLoadingMap).some(v => v)
    }
  },
  beforeDestroy() {
    // 关闭轮询
    pageLoadingUtils.closePageLoading()
  },
  async mounted() {
    this.getImSyncStatusFunc()
    this.handleQuery()
  },
  methods: {
    changePage() {
      this.$refs.searchRef?.handleQuery()
    },
    async getList(query) {
      if (query) {
        this.filterQuery = query
      }
      const params = { ...this.filterQuery, ...this.queryParams }
      this.loading = true
      try {
        const res = await getCollaborationPage(params)
        const arr = res.records || []
        arr.map(item => {
          item.ticketTypeNames = item.policeTicketTypes
            .map(child => child.tag)
            .join(',')
          item.typeIds = item.policeTicketTypes.map(child => child.id)
          return item
        })
        this.list = res.records
        this.total = res.total
      } finally {
        this.loading = false
      }
    },

    getRelatedUserCount(ids) {
      if (Array.isArray(ids)) {
        return ids.length
      } else {
        return ids?.split(',').length
      }
    },
    async getImSyncStatusFunc() {
      const { code, data } = await getImSyncStatus({})
      if (code === 0) {
        this.hiddenSync = data
      }
    },
    // 同步老的协同岗数据
    async syncPostFromImFunc() {
      if (this.syncLoading) return // 防止极端情况下的并发
      this.syncLoading = true
      try {
        const { code } = await syncPostFromIm({})
        if (code === 0) {
          pageLoadingUtils.openPageLoading(this.handleQuery)
          this.hiddenSync = true
          this.$refs.manageRef.resetQuery()
        }
      } finally {
        this.syncLoading = false
      }
    },
    /** 添加/修改操作 */
    openForm(type, row) {
      this.$refs.formRef.open(type, row)
    },

    /** 多选 */
    handleSelect(val) {
      this.multipleSelection = val.map(item => item.id)
    },

    /** 删除按钮操作 */
    handleDelete(id) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteCollaboration(id).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
              this.$refs.searchRef?.handleQuery()
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

    /** 删除按钮操作 */
    async delBatch() {
      if (this.multipleSelection.length === 0) {
        this.$message({
          message: '未勾选协同岗数据',
          type: 'error'
        })
        return
      }
      this.$confirm('确定批量删除吗？', {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          delBatchCollaboration(this.multipleSelection).then(result => {
            if (result.code === 0) {
              this.$message({
                message: '批量删除成功',
                type: 'success'
              })
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.$refs.searchRef?.handleQuery()
          })
        })
        .catch(() => {})
    },

    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.$refs.searchRef?.handleQuery()
    },

    /** 重置按钮操作 */
    resetQuery() {
      this.queryParams.pageNum = 1
      this.queryParams.pageSize = 10
      this.filterQuery = {}
      this.$refs.searchRef?.handleQuery()
    },

    /** 下岗按钮操作 - 查询在岗人员 */
    async handleOffDuty(row) {
      // 设置按钮loading状态
      this.$set(this.listOffDutyLoadingMap, row.id, true)
      try {
        const res = await getOnDutyUsersByPostId(row.id)
        if (res.code === 0) {
          const users = res.data || []
          if (users.length === 0) {
            this.$message({
              message: this.$t('index.collaboration.noOnDutyUserTip'),
              type: 'warning'
            })
          } else {
            this.currentOffDutyPost = row
            this.offDutyDialogVisible = true
          }
        } else {
          this.$message({
            message: res.msg || this.$t('index.collaboration.queryOnDutyFailed'),
            type: 'error'
          })
        }
      } catch (error) {
        this.$message({
          message: this.$t('index.collaboration.queryOnDutyFailed'),
          type: 'error'
        })
      } finally {
        this.$set(this.listOffDutyLoadingMap, row.id, false)
      }
    }
  }
}
</script>
<style lang="scss" scoped>
.table-container {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
}

.flex-table {
  flex: 1;
  overflow: auto;
  min-height: 200px; /* 确保表格在没有数据时也有最小高度 */
}

.head-shot {
  display: inline-block;
  height: 40px;
  width: 40px;
}
</style>