<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.executorCode"
          :placeholder="$t('index.list.policeNumber')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQuery.executorName"
          :placeholder="$t('index.list.policeName')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-select
          v-model="listQuery.type"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.category')"
          clearable
          style="width: 150px"
          class="filter-item"
        >
          <el-option
            v-for="item in queryTypeOptions"
            :key="item.code"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
        <el-select
          v-model="listQuery.queryResult"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.condition')"
          clearable
          style="width: 150px"
          class="filter-item"
        >
          <el-option
            v-for="item in queryResultOptions"
            :key="item.code"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
        <el-date-picker
          v-model="listQuery.startTime"
          class="filter-item"
          type="datetime"
          :placeholder="$t('index.list.startTime')"
          value-format="yyyy-MM-dd HH:mm:ss"
        />
        <el-date-picker
          v-model="listQuery.endTime"
          class="filter-item"
          type="datetime"
          :placeholder="$t('index.list.endTime')"
          value-format="yyyy-MM-dd HH:mm:ss"
        />
        <el-button
          v-waves
          class="filter-item"
          type="primary"
          icon="el-icon-search"
          @click="handleFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          v-waves
          class="filter-item"
          type="primary"
          icon="el-icon-download"
          :loading="loading"
          @click="handleDownload"
        >
          {{ $t('index.operations.export') }}
        </el-button>
      </div>
      <el-table
        v-loading="listLoading"
        :data="list"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('index.list.policeNumber')"
          :show-overflow-tooltip="true"
          min-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.executorCode }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.policeName')"
          :show-overflow-tooltip="true"
          min-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.executorName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.category')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>
              {{
                scope.row.type === 0
                  ? $t('index.list.person')
                  : $t('index.list.vehicle')
              }}
            </span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.searchContent')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.queryContent }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.searchTime')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.queryTime }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.searchResult')"
          class-name="queryResult-col"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            {{
              scope.row.queryResult
                ? $t('index.statusTitle.succeed')
                : $t('index.statusTitle.fail')
            }}
          </template>
        </el-table-column>
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="listQuery.pageNum"
        :limit.sync="listQuery.pageSize"
        @pagination="getList"
      />
    </el-card>
  </div>
</template>

<script>
import Pagination from '@/components/Pagination'
import waves from '@/directive/waves'
import { selectPageCappQuery, exportCappQuery } from '@/api/auth/auth'
export default {
  name: 'EquipmentLog',
  components: { Pagination },
  directives: { waves },
  data() {
    return {
      listLoading: false,
      listQuery: {
        pageSize: 10,
        pageNum: 1,
        executorCode: '',
        executorName: '',
        queryResult: '',
        type: '',
        startTime: '',
        endDate: '',
      },
      list: [],
      total: 0,
      queryTypeOptions: [
        { name: this.$t('index.list.person'), code: 0 },
        { name: this.$t('index.list.vehicle'), code: 1 },
      ],
      queryResultOptions: [
        { name: this.$t('index.statusTitle.succeed'), code: true },
        { name: this.$t('index.statusTitle.fail'), code: false },
      ],
      value1: [new Date(2000, 10, 10, 10, 10), new Date(2000, 10, 11, 10, 10)],
      multipleSelection: [],
      loading: false,
    }
  },
  created() {
    this.getList()
  },
  methods: {
    async getList() {
      await selectPageCappQuery(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total * 1
        this.listLoading = false
      })
    },
    async handleFilter() {
      this.listQuery.pageNum = 1
      await this.getList()
    },
    handleSelection(val) {
      this.multipleSelection = val
    },
    async handleDownload() {
      this.loading = true
      const param = {
        ids: this.multipleSelection.map((item) => item.id),
      }
      if (this.multipleSelection.length === 0) {
        Object.assign(param, this.listQuery)
      }
      const data = await exportCappQuery(param)
      this.loading = false
      if (!data) {
        return
      }
      const url = window.URL.createObjectURL(new Blob([data]))
      const link = document.createElement('a')
      link.style.display = 'none'
      link.href = url
      link.setAttribute('download', 'log.xlsx')
      document.body.appendChild(link)
      link.click()
    },
  },
}
</script>

<style scoped lang="scss">
.card {
  height: 100%;
  overflow-y: auto;
}
.filter-container {
  padding-bottom: 10px;
  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }
}
</style>
