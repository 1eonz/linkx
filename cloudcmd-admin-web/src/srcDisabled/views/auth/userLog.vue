<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container" style="float: right">
        <el-button
          v-waves
          class="filter-item"
          type="primary"
          icon="el-icon-refresh"
          @click="getList"
        >
          {{ $t('index.operations.refresh') }}
        </el-button>
      </div>
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
          :label="$t('index.operations.operationUser')"
          :show-overflow-tooltip="true"
          min-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.userName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.operations.operationDetails')"
          :show-overflow-tooltip="true"
          max-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.content }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.operations.operationIP')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.ip }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.operations.operationTime')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.time }}</span>
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
import { getUserLogList } from '@/api/auth/auth'
export default {
  name: 'UserLog',
  components: { Pagination },
  directives: { waves },
  data() {
    return {
      listLoading: false,
      listQuery: {
        page: 1,
        limit: 10
      },
      multipleSelection: [],
      list: [],
      total: 0
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      getUserLogList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
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
