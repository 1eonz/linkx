<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <el-table
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%;"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('index.list.userName')"
          :show-overflow-tooltip="true"
          min-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.userName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.userType')"
          :show-overflow-tooltip="true"
          min-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.clientName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.login.login')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.loginIp }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.login.loginTime')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.createTime }}</span>
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="listQuery.page"
        :limit.sync="listQuery.limit"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script>
import Pagination from '@/components/Pagination'
import waves from '@/directive/waves'
import { getAuthList } from '@/api/auth/auth'
export default {
  name: 'Cookie',
  components: { Pagination },
  directives: { waves },
  data() {
    return {
      listQuery: {
        page: 1,
        limit: 10
      },
      multipleSelection: [],
      list: [],
      listLoading: true,
      total: 0
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      getAuthList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total * 1
        this.listLoading = false
      })
    },
    handleSelection(val) {
      this.multipleSelection = val
    }
  }
}
</script>

<style scoped>
.card {
  height: 100%;
  overflow-y: auto;
}
</style>
