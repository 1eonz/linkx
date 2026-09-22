<template>
  <div class="agent-history">
    <div class="header">
      <el-form :inline="true" :model="query" class="search-form">
        <el-form-item>
          <el-input v-model="query.userName" clearable placeholder="请输入查询人" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="query.identityCardNumber" clearable placeholder="请输入身份证" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="query.content" clearable placeholder="请输入问题" style="width: 160px" />
        </el-form-item>
        <el-form-item>
          <el-date-picker v-model="time" type="datetimerange" range-separator="-" start-placeholder="开始时间"
            end-placeholder="结束时间" :default-time="['00:00:00', '23:59:59']" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button type="primary" icon="el-icon-refresh" @click="handleReset">重置</el-button>
          <el-button type="primary" icon="el-icon-bottom" @click="handleExport">导出</el-button>
        </el-form-item>
      </el-form>
    </div>
    <div class="table-wrapper">
      <el-table v-loading="loading" border :data="tableData" style="width: 100%"
        @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="55" align="center" />
        <el-table-column label="查询人" prop="userName" width="180" />
        <el-table-column label="查询人身份证号" prop="identityCardNumber" show-overflow-tooltip />
        <el-table-column label="查询智能体" prop="agentName" />
        <el-table-column label="所属部门" prop="departmentName" show-overflow-tooltip />
        <el-table-column label="查询内容" prop="queryContent" show-overflow-tooltip />
        <el-table-column label="查询时间" prop="time" width="180">
          <template #default="scope">
            {{ formatTime(scope.row.time) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="scope">
            <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="bottom">
      <el-pagination :current-page.sync="query.pageNo" :page-size.sync="query.pageSize"
        layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]" :total="total"
        @current-change="handleCurrentChange" @size-change="handleSizeChange" />
    </div>

    <el-dialog title="查询详情" :visible.sync="detailVisible" width="800px" append-to-body>
      <el-descriptions :column="1" border>
        <el-descriptions-item label="查询人">{{ detailData.userName }}</el-descriptions-item>
        <el-descriptions-item label="查询人身份证号">{{ detailData.identityCardNumber }}</el-descriptions-item>
        <el-descriptions-item label="查询智能体">{{ detailData.agentName }}</el-descriptions-item>
        <el-descriptions-item label="所属部门">{{ detailData.departmentName }}</el-descriptions-item>
        <el-descriptions-item label="查询时间">{{ formatTime(detailData.time) }}</el-descriptions-item>
        <el-descriptions-item label="查询内容">
          <div class="content-box">{{ detailData.queryContent }}</div>
        </el-descriptions-item>
        <el-descriptions-item label="响应内容">
          <div class="content-box">{{ detailData.responseContent }}</div>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script>
import {
  getAiagentRecordPage,
  deleteAiagentRecord,
  exportAiagentRecord
} from '@/api/thirdInterface/agentInterface.js'

export default {
  name: 'AgentHistory',
  data() {
    return {
      loading: false,
      tableData: [],
      query: {
        userName: '',
        identityCardNumber: '',
        content: '',
        pageNo: 1,
        pageSize: 20
      },
      time: [],
      total: 0,
      selection: [],
      detailVisible: false,
      detailData: {}
    }
  },
  mounted() {
    this.handleSearch()
  },
  methods: {
    getParams() {
      const [start, end] = this.time || []
      return {
        ...this.query,
        minTime: start ? this.formatTime(start) : '',
        maxTime: end ? this.formatTime(end) : ''
      }
    },
    async handleSearch() {
      this.loading = true
      try {
        const res = await getAiagentRecordPage(this.getParams())
        if (res.code === 0) {
          this.tableData = res.data.list || []
          this.total = res.data.total || 0
        } else {
          this.$message.error('查询失败')
        }
      } catch (error) {
        this.tableData = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    handleReset() {
      this.query = {
        userName: '',
        identityCardNumber: '',
        content: '',
        pageNo: 1,
        pageSize: 20
      }
      this.time = []
      this.handleSearch()
    },
    async handleExport() {
      try {
        const params = {
          ...this.getParams(),
          ids: this.selection.map(item => item.id).join(',') || undefined
        }
        const res = await exportAiagentRecord(params)
        const blob = new Blob([res], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
        const link = document.createElement('a')
        link.href = URL.createObjectURL(blob)
        link.download = '查询统计.xlsx'
        link.click()
        URL.revokeObjectURL(link.href)
      } catch (error) {
        console.error(error)
        this.$message.error('导出失败')
      }
    },
    handleDelete(row) {
      this.$confirm('确认删除该查询记录吗？', '请确认', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await deleteAiagentRecord(row.id)
          if (res.code === 0) {
            this.$message.success(res.msg || '删除成功')
            this.handleSearch()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        } catch (error) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },
    handleCurrentChange(page) {
      this.query.pageNo = page
      this.handleSearch()
    },
    handleSizeChange(size) {
      this.query.pageSize = size
      this.handleSearch()
    },
    handleSelectionChange(data) {
      this.selection = data
    },
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const pad = (n) => String(n).padStart(2, '0')
      return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
    }
  }
}
</script>

<style scoped>
.agent-history {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-shrink: 0;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
}

.search-form /deep/ .el-form-item {
  margin-bottom: 16px;
}

.table-wrapper {
  flex: 1;
  overflow: hidden;
  min-height: 0;
}

.table-wrapper /deep/ .el-table {
  height: 100% !important;
  display: flex;
  flex-direction: column;
}

.table-wrapper /deep/ .el-table .el-table__body-wrapper {
  flex: 1;
  overflow-y: auto;
}

.table-wrapper /deep/ .el-table th {
  background-color: #f5f7fa;
}

.table-wrapper /deep/ .el-table .el-table-column--selection .cell,
.table-wrapper /deep/ .el-table .el-table__fixed .el-table-column--selection .cell {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 0;
}

.bottom {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  flex-shrink: 0;
}

.content-box {
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>
