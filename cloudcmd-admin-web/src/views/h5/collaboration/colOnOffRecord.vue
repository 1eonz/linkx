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
        label="协同岗名称"
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
        label="协同岗人员"
        show-overflow-tooltip
        align="center"
        prop="personName"
      />
      <el-table-column label="上下岗类型" align="center" prop="type" />
      <el-table-column label="剩余在岗人数" align="center" prop="lastPeopleNum">
        <template slot-scope="scope">
          {{ scope.row.lastPeopleNum ? scope.row.lastPeopleNum : '0' }}
        </template>
      </el-table-column>
      <el-table-column
        label="剩余在岗人员"
        show-overflow-tooltip
        align="center"
        prop="lastPeople"
      >
        <template slot-scope="scope">
          {{ scope.row.lastPeople ? scope.row.lastPeople : '-' }}
        </template>
      </el-table-column>
      <el-table-column label="数据来源" align="center" prop="switchType">
        <template slot-scope="scope">
          {{ getSwitchTypeName(scope.row.switchType) }}
        </template>
      </el-table-column>
      <el-table-column label="上下岗时间" align="center" prop="createTime" />
    </el-table>
    <!-- 分页 -->
    <pagination
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="changePage"
    />
  </div>
</template>
<script>
import { getAttendancePage, exportAttendance } from '@/api/h5/collaboration'
import pagination from '@/components/Pagination'
import Search from './search.vue'
export default {
  name: 'ColOnOffRecord',
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
      }
    }
  },
  async mounted() {
    this.handleQuery()
  },
  methods: {
    getSwitchTypeName(type) {
      // 切换类型(type)：0手工切换（default），1IM状态变化切换，2值班自动上下岗,3其他
      if (!type && type !== 0) {
        return ''
      } else if (+type === 0) {
        return '手工切换'
      } else if (+type === 1) {
        return 'IM状态变化'
      } else if (+type === 2) {
        return '值班自动上下岗'
      } else if (+type === 3) {
        return '管理员操作下岗'
      } else {
        return '其他'
      }
    },
    changePage() {
      this.$refs.searchRef?.handleQuery()
    },
    async getList(query) {
      this.loading = true
      const params = { ...query, ...this.queryParams }
      params.personName = params.relatedUserNames
      try {
        const res = await getAttendancePage(params)
        this.list = res.records
        this.total = res.total
      } finally {
        this.loading = false
      }
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
      this.$refs.searchRef?.handleQuery()
    },

    /** 导出 */
    async exportData(params) {
      params.personName = params.relatedUserNames
      const data = await exportAttendance(params)
      if (!data) {
        return
      }
      const url = window.URL.createObjectURL(new Blob([data]))
      const link = document.createElement('a')
      link.style.display = 'none'
      link.href = url
      link.setAttribute('download', '协同岗上下岗记录.xlsx')
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
