<template>
  <div class="agent-manage">
    <div class="header">
      <div class="header-left">
        <el-input v-model="searchForm.name" clearable placeholder="请输入名称" style="width: 200px" @keyup.enter.native="handleSearch"/>
        <el-select v-model="searchForm.categoryId" filterable clearable placeholder="请选择分类" style="width: 200px">
          <el-option v-for="item in typeList" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
        <el-button type="primary" icon="el-icon-refresh" @click="handleReset">重置</el-button>
        <el-button type="primary" icon="el-icon-price-tag" @click="showTypeDialog = true">创建分类</el-button>
        <el-button type="primary" icon="el-icon-circle-plus-outline" @click="handleAdd">创建智能体</el-button>
      </div>
    </div>
    <div class="table-wrapper">
      <el-table v-loading="loading" border :data="tableData" style="width: 100%">
        <el-table-column label="序号" type="index" width="60" align="center" />
        <el-table-column label="名称" prop="name" width="180" />
        <el-table-column label="说明" prop="desc" show-overflow-tooltip />
        <el-table-column label="图标" prop="avatar" width="80" align="center">
          <template #default="scope">
            <el-image fit="cover" :src="scope.row.avatarUrl || defaultImg" style="width: 48px; height: 50px" />
          </template>
        </el-table-column>
        <el-table-column label="三方智能体对接地址" prop="url" show-overflow-tooltip />
        <el-table-column label="Token" prop="token" show-overflow-tooltip />
        <el-table-column label="分类" prop="categoryName" />
        <el-table-column label="优先级" prop="priority" width="80" align="center">
          <template #default="scope">
            {{ priorityLabels[scope.row.priority] || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="是否涉密" prop="isRestricted" width="80" align="center">
          <template #default="scope">
            {{ scope.row.isRestricted === 1 ? '是' : '否' }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" align="center">
          <template #default="scope">
            <!-- <el-button size="mini" type="primary" @click="handleBind(scope.row)">绑定用户</el-button> -->
            <el-button size="mini" type="primary" @click="handleEdit(scope.row)">修改</el-button>
            <el-button size="mini" type="danger" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <div class="bottom">
      <el-pagination :current-page.sync="currentPage" :page-size.sync="pageSize"
        layout="total, sizes, prev, pager, next, jumper" :page-sizes="[10, 20, 50, 100]" :total="total"
        @current-change="handleCurrentChange" @size-change="handleSizeChange" />
    </div>

    <AgentManageEditModal :visible.sync="dialogVisible" :operate="operate" :type-list="typeList" :edit-data="editData"
      @success="fetchData" @refresh-category="queryType" />

    <AgentTypeEditModal :visible.sync="showTypeDialog" :type-list="typeList" @success="queryType" />

    <AgentBindVirtualUser :visible.sync="showVirtualUser" :agentId="agentId" />
  </div>
</template>

<script>
import {
  getAiagentPage,
  deleteAiagent,
  queryCategory
} from '@/api/thirdInterface/agentInterface.js'
import AgentManageEditModal from './AgentManageEditModal.vue'
import AgentTypeEditModal from './AgentTypeEditModal.vue'
import AgentBindVirtualUser from './AgentBindVirtualUser.vue'

export default {
  name: 'AgentManage',
  components: {
    AgentManageEditModal,
    AgentTypeEditModal,
    AgentBindVirtualUser
  },
  data() {
    return {
      loading: false,
      tableData: [],
      currentPage: 1,
      pageSize: 20,
      total: 0,
      searchForm: {
        name: '',
        categoryId: ''
      },
      priorityLabels: ['高', '中', '低'],
      defaultImg: 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHdpZHRoPSI0OCIgaGVpZ2h0PSI1MCI+PHJlY3Qgd2lkdGg9IjQ4IiBoZWlnaHQ9IjUwIiBmaWxsPSIjZGRkIi8+PC9zdmc+',
      dialogVisible: false,
      operate: 'add',
      editData: {},
      typeList: [],
      showTypeDialog: false,
      showVirtualUser: false,
      agentId: null,
    }
  },
  mounted() {
    this.fetchData()
    this.queryType()
  },
  methods: {
    async fetchData() {
      this.loading = true
      try {
        const params = {
          pageNo: this.currentPage,
          pageSize: this.pageSize
        }
        if (this.searchForm.name) params.name = this.searchForm.name
        if (this.searchForm.categoryId) params.categoryId = this.searchForm.categoryId
        const res = await getAiagentPage(params)
        if (res.code === 0) {
          const list = res.data.list || []
          const baseUrl = window.location.origin + '/linkx/admin/XA-ics-agent'
          // 处理 avatar 相对路径
          this.tableData = list.map(item => {
            if (item.avatar) {
              item.avatarUrl = baseUrl + item.avatar
            }
            return item
          })
          this.total = res.data.total || 0
        }
      } catch (error) {
        this.tableData = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    async queryType() {
      try {
        const res = await queryCategory()
        if (res.code === 0) {
          this.typeList = res.data || []
        }
      } catch (error) {
        console.error('获取分类失败:', error)
        this.typeList = []
      } finally {
        this.loading = false
      }
    },
    handleAdd() {
      this.operate = 'add'
      this.editData = {}
      this.dialogVisible = true
    },
    handleEdit(row) {
      this.operate = 'edit'
      this.editData = { ...row }
      this.dialogVisible = true
    },
    handleBind(row) {
      this.agentId = row.id
      this.showVirtualUser = true
    },
    handleDelete(row) {
      this.$confirm('确认删除该智能体吗？', '请确认', {
        confirmButtonText: '确认',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          const res = await deleteAiagent(row.id)
          if (res.code === 0) {
            this.$message.success(res.msg || '删除成功')
            this.fetchData()
          } else {
            this.$message.error(res.msg || '删除失败')
          }
        } catch (error) {
          this.$message.error('删除失败')
        }
      }).catch(() => {})
    },
    handleSearch() {
      console.log('触发搜索')
      this.currentPage = 1
      this.fetchData()
    },
    handleReset() {
      this.searchForm = { name: '', categoryId: '' }
      this.currentPage = 1
      this.fetchData()
    },
    handleCurrentChange(page) {
      this.currentPage = page
      this.fetchData()
    },
    handleSizeChange(size) {
      this.pageSize = size
      this.fetchData()
    }
  }
}
</script>

<style scoped>
.agent-manage {
  flex: 1;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 16px;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  gap: 8px;
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

.bottom {
  display: flex;
  justify-content: flex-end;
  padding-top: 16px;
  flex-shrink: 0;
}

.filter-item {
  margin-left: 8px;
}
</style>
