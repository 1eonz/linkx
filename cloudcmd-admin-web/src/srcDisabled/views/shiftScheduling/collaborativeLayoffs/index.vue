<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          placeholder="名称"
          style="width: 200px;"
          class="filter-item"
          clearable
          @keyup.enter.native="handleFilter"
        />
        <el-date-picker
          v-model="listQuery.date"
          type="date"
          placeholder="请选择查询时间"
          class="filter-item"
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
          icon="el-icon-refresh"
          @click="resetQuery"
        >
          重置
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
        <el-table-column label="智能体名称" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请人" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.sqName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审批人" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.spName }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审批状态" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.status }}</span>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.sqDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="审批时间" align="center">
          <template slot-scope="scope">
            <span>{{ scope.row.spDate }}</span>
          </template>
        </el-table-column>
        <el-table-column align="center" />
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="viewDetail(scope.row)"
            >
              详情
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
    <!-- 新增/修改位置 -->
    <detail ref="detailDialog" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import detail from './components/detail.vue'
export default {
  name: 'Approval',
  components: { pagination, detail },
  data() {
    return {
      listLoading: false,
      list: [],
      clearable: true,
      total: 0,
      listQuery: {
        date: '',
        name: '',
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
    getList() {
      this.listLoading = true
      this.list = [
        {
          name: '测试',
          sqName: '张三',
          spName: '李四',
          status: '同意',
          sqDate: '2025-11-12 12:13:11',
          spDate: '2025-11-12 12:13:11'
        }
      ]
      //   getLocationList(this.listQuery).then(data => {
      //     this.list = data.list
      //     this.total = data.total
      //     this.listLoading = false
      //   })
      this.listLoading = false
    },
    // 搜索
    handleFilter() {
      this.listQuery.pageNum = 1
      this.getList(this.listQuery)
    },
    // 重置
    resetQuery() {
      this.listQuery = {
        pageNum: 1,
        pageSize: 10,
        name: '',
        date: ''
      }
      this.getList()
    },
    // 查看详情
    viewDetail(row) {
      this.$refs.detailDialog.init(row)
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
