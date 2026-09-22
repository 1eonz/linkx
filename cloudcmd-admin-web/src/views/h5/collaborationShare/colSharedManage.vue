<template>
  <div class="table-container">
    <div class="search-bar">
      <el-input
        v-model="queryParams.coopUserName"
        placeholder="协同岗名称"
        clearable
        style="width: 180px;"
        class="filter-item"
        @keyup.enter.native="handleQuery"
      />
      <el-button
        type="primary"
        icon="el-icon-search"
        class="filter-item"
        @click="handleQuery"
      >
        搜索
      </el-button>
      <el-button
        type="primary"
        icon="el-icon-refresh"
        class="filter-item"
        @click="handleReset"
      >
        重置
      </el-button>
    </div>

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
        prop="coopUserName"
      />
      <el-table-column
        label="来源节点"
        show-overflow-tooltip
        align="center"
        prop="originPeerName"
      />
      <el-table-column
        label="所属组织"
        show-overflow-tooltip
        align="center"
        prop="orgName"
      />
      <el-table-column
        label="接收时间"
        align="center"
        prop="receivedTime"
        sortable
      />
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
import { getCoopUsersPage } from '@/api/h5/collaboration'
import pagination from '@/components/Pagination'
import shareForm from './shareForm.vue'
import unshareForm from './unshareForm.vue'
import shareLogDialog from './shareLogDialog.vue'

export default {
  name: 'ColSharedManage',
  components: { pagination, shareForm, unshareForm, shareLogDialog },
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
    version: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      list: [],
      loading: false,
      total: 0,
      queryParams: {
        coopUserName: '',
        pageNum: 1,
        pageSize: 10
      }
    }
  },
  mounted() {
    this.handleQuery()
  },
  methods: {
    changePage() {
      this.getList()
    },
    async getList() {
      this.loading = true
      try {
        const params = {
          type: 2,
          ...this.queryParams
        }
        const { data: res } = await getCoopUsersPage(params)
        this.list = res.records || []
        this.total = res.total || 0
      } finally {
        this.loading = false
      }
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    handleReset() {
      this.queryParams.coopUserName = ''
      this.handleQuery()
    },
    // 分享协同岗
    openShare(row) {
      const data = {
        ...row,
        postName : row.coopUserName,
        id : row.coopUserId,
      }
      this.$refs.shareFormRef.open(data)
    },
    // 取消分享
    cancelShare(row) {
      const data = {
        ...row,
        postName : row.coopUserName,
        id : row.coopUserId,
      }
      this.$refs.unshareFormRef.open(data)
    },
    /** 分享记录 */
    shareLog(row) {
      const data = {
        ...row,
        postName : row.coopUserName,
        id : row.coopUserId,
      }
      this.$refs.shareLogDialogRef.open(data)
    },
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

.search-bar {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 12px 0 0 12px;
  flex-shrink: 0;
}

.filter-item {
  margin-bottom: 12px;
}

.flex-table {
  flex: 1;
  overflow: auto;
  min-height: 200px;
}
</style>
