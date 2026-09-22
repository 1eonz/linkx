<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          :placeholder="$t('index.list.chartName')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-button
          v-waves
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-search"
          @click="handleFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
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
      >
      <el-table-column
          type="index"
          :label="$t('index.list.sequenceNumber')"
          width="100"
          align="center"
        />
        <el-table-column
          :label="$t('index.list.chartName')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
          prop="chartName"
        />
        <el-table-column
          :label="$t('index.list.chartAddress')"
          :show-overflow-tooltip="true"
          min-width="200"
          align="center"
          prop="chartUrl"
        />
        <el-table-column
         :label="$t('index.list.remarks')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
          prop="remark"
        />
        <el-table-column
          :label="$t('index.list.chartType')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
          prop="type"
        >
        <template slot-scope="scope">
            {{ getType(scope.row.type) }}
          </template>
        </el-table-column>
        <el-table-column
           :label="$t('index.createTime')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
          prop="gmtCreated"
        />

        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          min-width="150"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              type="info"
              icon="el-icon-document"
              size="small"
              @click="getDetails(scope.row)"
              >{{ $t('index.operations.particulars') }}
            </el-button>
            <el-button
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
            >
              {{ $t('index.delete') }}
            </el-button>
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

    <!-- 新增/修改 -->
    <chart-edit ref="editChart" @success="getList" />
    <!-- 详情 -->
    <chart-detail ref="details" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import chartEdit from './chartEdit.vue'
import chartDetail from './chatLook.vue'
import { lookChartsPage, delCharts } from '@/api/board/chart.js'
import { chartTypeEnum } from '../enum.js'
import handlerChartUrl from '@/mixins/handler-chart-url.js'

export default {
  name: 'Chart',
  components: { pagination, chartEdit, chartDetail },
  mixins: [handlerChartUrl],
  data() {
    return {
      listLoading: false,
      list: [],
      total: 0,
      listQuery: {
        chartName: '',
        pageSize: 10,
        pageNum: 1
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    // 获取
    async getList() {
      this.listLoading = true
      const { code, data } = await lookChartsPage(this.listQuery)
      if (code === 0) {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
        this.handlerChartUrl('list')
      }
    },
    getType(type) {
      return chartTypeEnum[type]
    },
    // 搜索
    handleFilter() {
      this.listQuery.pageNum = 1
      const { chartName } = this.listQuery
      if (chartName) {
        this.list = this.list.filter(item => {
          return item.chartName.indexOf(chartName) !== -1
        })
      } else {
        this.getList()
      }
    },
    // 新增
    handleCreate() {
      this.$refs.editChart.init()
    },
    // 修改
    handleUpdate(row) {
      this.$refs.editChart.init(row)
    },
    // 删除
    handleDelete({ id }) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          delCharts(id).then(result => {
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
    //
    getDetails(data) {
      this.$refs.details.setData(data)
    }
  }
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
.edit-input {
  padding-right: 50px;
  width: 360px;
}
.edit-select {
  padding-right: 50px;
  width: 360px;
}

.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
</style>
