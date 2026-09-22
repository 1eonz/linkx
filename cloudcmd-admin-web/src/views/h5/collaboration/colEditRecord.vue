<template>
  <div class="table-container">
    <Search
      ref="searchRef"
      :is-main="false"
      :is-admin="isAdmin"
      :org-ids="orgId"
      :department-code="departmentCode"
      :department-sync-sign="departmentSyncSign"
      @query="getList"
      @reset="resetQuery"
      @export="exportData"
    />
    <el-table
      v-loading="loading"
      :data="list"
      :stripe="true"
      :show-overflow-tooltip="true"
      class="flex-table"
    >
      <el-table-column
        label="名称"
        show-overflow-tooltip
        align="center"
        prop="postName"
      />
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
      <el-table-column label="操作人员" align="center" prop="operatorName" />
      <el-table-column
        label="操作类型"
        align="center"
        prop="operationTypeName"
      />
      <el-table-column label="操作内容" align="center" prop="content" />
      <el-table-column
        label="操作时间"
        align="center"
        prop="operateTime"
        sortable
      />
    </el-table>
    <!-- 分页 -->
    <pagination
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getListPag"
    />
  </div>
</template>
<script>
// import { getInfoPage, deleteInfo } from '@/api/h5/app'
import {
  getCollaborationEditPage,
  exportCollaborationEditPage
} from '@/api/h5/collaboration'
import pagination from '@/components/Pagination'
import Search from './search.vue'
export default {
  name: 'ColEditRecord',
  components: { pagination, Search },
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
      queryParams: {
        pageNum: 1,
        pageSize: 10
      },
      searchParams: {}, // 保存搜索条件
      multipleSelection: []
    }
  },
  mounted() {
    this.handleQuery()
  },

  methods: {
    getListPag() {
      this.$refs.searchRef?.handleQuery()
    },
    getOperationTypeName(operationType) {
      if (!operationType && operationType !== 0) return ''
      const typeArr = ['新增', '修改', '删除']
      return typeArr[operationType]
    },
    getoperationTypeName(operationType) {
      if (operationType === 0) {
        return
      }
    },
    async getList(query) {
      this.loading = true
      // 如果有新的查询参数，更新保存的搜索条件
      if (query) {
        this.searchParams = { ...query }
      }
      // 合并搜索条件和分页参数
      const params = { ...this.searchParams, ...this.queryParams }
      try {
        const data = await getCollaborationEditPage(params)
        this.list = data.data?.records?.map(item => ({
          ...item,
          operationTypeName: this.getOperationTypeName(item.operationType)
        }))
        this.total = data.data?.total
      } finally {
        this.loading = false
      }
    },

    /** 删除按钮操作 */
    // async handleDelete(id) {
    //   this.$confirm(this.$t('index.operations.affirmDeleted'), {
    //     confirmButtonText: this.$t('index.determine'),
    //     cancelButtonText: this.$t('index.cancel'),
    //     type: 'info'
    //   })
    //     .then(() => {
    //       deleteInfo(id).then(result => {
    //         if (result.code === 0) {
    //           this.$message({
    //             message: this.$t('index.statusTitle.successfullyDelete'),
    //             type: 'success'
    //           })
    //         } else {
    //           this.$message({
    //             message: result.msg,
    //             type: 'error'
    //           })
    //         }
    //         this.getList()
    //       })
    //     })
    //     .catch(() => {})
    // },

    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.$refs.searchRef?.handleQuery()
    },

    /** 重置按钮操作 */
    resetQuery() {
      this.queryParams.pageNum = 1
      this.queryParams.pageSize = 10
      this.searchParams = {}
      this.$refs.searchRef?.handleQuery()
    },

    /** 导出 */
    async exportData(params) {
      const data = await exportCollaborationEditPage(params)
      if (!data) {
        return
      }
      const url = window.URL.createObjectURL(new Blob([data]))
      const link = document.createElement('a')
      link.style.display = 'none'
      link.href = url
      link.setAttribute('download', '协同岗编辑记录.xlsx')
      document.body.appendChild(link)
      link.click()
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
</style>
