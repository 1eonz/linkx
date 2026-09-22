<template>
  <div class="table-container">
    <Search
      ref="searchRef"
      :is-admin="isAdmin"
      :org-ids="orgId"
      :department-code="departmentCode"
      :department-sync-sign="departmentSyncSign"
      @query="getList"
      @reset="resetQuery"
    />
    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
      class="flex-table"
    >
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
      <el-table-column label="协同岗类型" align="center">
        <template #default="{ row }">
          {{ row.type === 1 ? '人员核查协同岗' : '普通协同岗' }}
        </template>
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
      <el-table-column label="操作" align="center" min-width="200px">
        <template #default="scope">
          <el-button
            type="primary"
            icon="el-icon-share"
            size="small"
            @click="openShare(scope.row)"
          >
            分享
          </el-button>
          <template v-if="scope.row.isShared === 1">
            <el-button
              type="danger"
              icon="el-icon-refresh-left"
              size="small"
              @click="cancelShare(scope.row)"
            >
              取消分享
            </el-button>
            <el-button
              type="primary"
              icon="el-icon-time"
              size="small"
              @click="shareLog(scope.row)"
            >
              分享记录
            </el-button>
          </template>
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

    <!-- 分享弹窗 -->
    <share-form
      ref="shareFormRef"
      :is-admin="isAdmin"
      :department-code="departmentCode"
      :version="version"
      @success="handleQuery"
    />
    <!-- 取消分享弹窗 -->
    <unshare-form ref="unshareFormRef" @success="handleQuery" />
    <!-- 分享记录弹窗 -->
    <share-log-dialog ref="shareLogDialogRef" />
  </div>
</template>
<script>
import { getCollaborationPage } from '@/api/h5/collaboration'
import pagination from '@/components/Pagination'
import authImg from '@/components/AuthImg'
import Search from './search.vue'
import shareForm from './shareForm.vue'
import unshareForm from './unshareForm.vue'
import shareLogDialog from './shareLogDialog.vue'

export default {
  name: 'ColManage',
  components: { pagination, authImg, Search, shareForm, unshareForm, shareLogDialog },
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
    },
    version: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      list: [],
      loading: true,
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10
      },
      filterQuery: {}
    }
  },
  async mounted() {
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
      const params = { ...this.filterQuery, ...this.queryParams ,type:0}
      this.loading = true
      try {
        const res = await getCollaborationPage(params)
        const arr = res.records || []
        arr.map(item => {
          item.ticketTypeNames = item.policeTicketTypes
            ? item.policeTicketTypes.map(child => child.tag).join(',')
            : ''
          item.typeIds = item.policeTicketTypes
            ? item.policeTicketTypes.map(child => child.id)
            : []
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
      }
      return ids ? ids.split(',').length : 0
    },
    /** 分享协同岗 */
    openShare(row) {
      this.$refs.shareFormRef.open(row)
    },
    /** 取消分享 */
    cancelShare(row) {
      this.$refs.unshareFormRef.open(row)
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.$refs.searchRef?.handleQuery()
    },
    /** 分享记录 */
    shareLog(row) {
      this.$refs.shareLogDialogRef.open(row)
    },
    resetQuery() {
      this.queryParams.pageNum = 1
      this.queryParams.pageSize = 10
      this.filterQuery = {}
      this.$refs.searchRef?.handleQuery()
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
  min-height: 200px;
}

.head-shot {
  display: inline-block;
  height: 40px;
  width: 40px;
}
</style>
