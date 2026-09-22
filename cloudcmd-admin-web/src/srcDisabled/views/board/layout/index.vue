<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          :placeholder="$t('index.list.layoutName')"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-select
          v-model="listQuery.status"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.releaseStatus')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in options"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
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
          :label="$t('index.list.layoutName')"
          :show-overflow-tooltip="true"
          align="center"
          prop="dashboardName"
        />
        <el-table-column
          prop="type"
          :label="$t('index.list.type')"
          align="center"
        >
        <template slot-scope="scope">
          <!-- <div v-if="scope.row.isDefault===0">{{ $t('index.list.custom') }}</div> -->
          <div class="wrap-grid">
            <grid-box
              :key="+new Date()"
              :nums="Number(scope.row.type)"
              is-hide-num
              class="box-item"
              :style-object="getTableGridStyleObj(scope.row)"
            />
          </div>
        </template>
      </el-table-column>
        <el-table-column
        :label="$t('index.list.createOrg')"
          :show-overflow-tooltip="true"
          align="center"
          prop="orgName"
        />
        <el-table-column
          :label="$t('index.list.remarks')"
          :show-overflow-tooltip="true"
          align="center"
          prop="remark"
        />
        <el-table-column
         :label="$t('index.createTime')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
          prop="gmtCreated"
        />
        <el-table-column
          prop="status"
           :label="$t('index.list.publish')"
          width="120"
          align="center"
      >
        <template slot-scope="scope">
          <el-switch
            :value="scope.row.status===1"
            active-color="#13ce66"
            inactive-color="#ff4949"
            @change="v => setReleased(v, scope.row)"
          />
        </template>
      </el-table-column>
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
              @click="handleLook(scope.row)"
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
              type="primary"
              icon="el-icon-s-grid"
              size="small"
              @click="handleLayout(scope.row)"
            >
           {{ $t('index.arrange') }}
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
    <layout-edit ref="editBoard" @success="getList" />

    <!-- 编排 -->
    <layout-preview
      :drawer.sync="drawer"
      :size2="500"
      :nums="setLayoutNums"
      :from-info="previewInfo"
      :is-look="previewModel === 'look'"
      @initTable="getList"
    />

    <!--预览  -->
    <layout-look
      :dialog-visible.sync="lookDialogVisible"
      :nums="nums"
      :style-object="gridBoxStyle"
      :show-chart-list="showChartList"
      :is-look="true"
    />
  </div>
</template>

<script>
import { deepCopy } from '@/utils'

import pagination from '@/components/Pagination'
import layoutEdit from './layoutEdit.vue'
import gridBox from './gridBox.vue'
import layoutPreview from './layoutPreview.vue'
import layoutLook from './layoutLook.vue'
import handlerChartUrl from '@/mixins/handler-chart-url.js'
import { lookDashboardsPage, lookDashboardsById, updateDashboards, delDashboards } from '@/api/board/layout.js'

export default {
  name: 'Chart',
  components: { pagination, layoutEdit, gridBox, layoutPreview, layoutLook },
  mixins: [handlerChartUrl],
  data() {
    return {
      layouts: [
        { px: '1920X1080', types: [100, 400, 600, 601, 701, 800, 900] },
        { px: '3840X1080', types: [700, 1200, 1300, 1500] }
      ],
      listLoading: false,
      list: [],
      total: 0,
      listQuery: {
        chartName: '',
        pageSize: 10,
        pageNum: 1
      },
      options: [
        { label: this.$t('index.list.published'), value: 1 },
        { label: this.$t('index.list.unpublished'), value: 0 }
      ],
      drawer: false,
      setLayoutNums: 0,
      previewInfo: null,
      previewModel: 'edit',
      nums: 0,
      lookDialogVisible: false,
      gridBoxStyle: {
        height: '100%',
        width: '100%'
      },
      showChartList: []
    }
  },
  computed: {
    columnMaxWidth() {
      return this.list.reduce((max, currrent) => {
        return Math.max(
          max,
          this.layouts[1].types.includes(currrent.type) ? 106 : 63
        )
      }, 0)
    }
  },
  created() {
    window.$chart = deepCopy(this.layouts)
  },
  mounted() {
    this.getList()
  },
  methods: {
    getTableGridStyleObj(obj) {
      let width = 0
      const styleObj = { width: '0' }
      this.layouts[1].types.includes(obj.type)
        ? (width = 85.32)
        : (width = 42.66)
      styleObj.width = width + 'px'
      return styleObj
    },
    toGridNum(n) {
      return Math.floor(n / 100)
    },
    toGridType(n) {
      return n % 100
    },
    // 获取
    async getList() {
      this.listLoading = true
      const { code, data } = await lookDashboardsPage(this.listQuery)
      if (code === 0) {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      }
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
      this.$refs.editBoard.init()
    },
    // 修改
    handleUpdate(row) {
      this.$refs.editBoard.init(row)
    },
    // 编排
    handleLayout(row) {
      const { status, type } = row
      if (status) {
        this.$message.error(this.$t('index.list.unableRelease'))
        return
      }
      this.previewInfo = row
      this.setLayoutNums = type - 0
      this.previewModel = 'edit'
      this.drawer = true
    },
    // 详情
    async handleLook(row) {
      const { status, type, id } = row
      if (status) {
        this.nums = type
        const { data } = await lookDashboardsById(id)
        this.showChartList = data.dashboardChartList
        this.handlerChartUrl('showChartList')
        this.previewModel = 'edit'
        this.lookDialogVisible = true
      } else {
        this.previewInfo = row
        this.setLayoutNums = type - 0
        this.previewModel = 'look'
        this.drawer = true
      }
    },
    // 删除
    handleDelete({ id }) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          delDashboards(id).then(result => {
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

    async setReleased(v, obj) {
      const { id, type } = obj
      const { data } = await lookDashboardsById(id)
      if (data.dashboardChartList.length < this.toGridNum(type - 0)) {
        this.$message.error(this.$t('index.list.fillFirst'))
        obj.status = false
        return
      }

      const params = {
        status: Number(v),
        dashboardChartList: data.dashboardChartList
      }
      const { code } = await updateDashboards(params, id)
      if (code === 0) {
        if (v) {
          this.$message.success(this.$t('index.list.releaseSuccess'))
        } else {
          this.$message.success(this.$t('index.list.cancelSuccessfully'))
        }
        this.getList()
      }
    }
  }
}
</script>

<style scoped lang="scss">
.card {
  height: 100%;
  overflow-y: auto;
  .wrap-grid {
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 2px;
    .grid-wrap {
      height: 24px;
    }
  }
}
.filter-container {
  padding-bottom: 10px;
  .filter-item {
    display: inline-block;
    vertical-align: middle;
    margin-bottom: 10px;
  }
}

.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
</style>
