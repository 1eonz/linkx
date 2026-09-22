<template>
  <div class="warningRelation">
    <div class="warningRelation-content">
      <el-tabs v-model="activeTab" type="border-card">
        <!-- 任务逾期 Tab -->
        <el-tab-pane label="任务逾期" name="taskOverdue">
          <el-form :model="queryParams" :inline="true" class="tab-header-form">
            <el-form-item>
              <el-input v-model="queryParams.businessName" placeholder="组织名称" clearable style="width: 200px;" @keyup.enter.native="searchQuery"/>
            </el-form-item>
            <el-form-item>
              <el-select v-model="queryParams.targetType" placeholder="通知对象类型" clearable style="width: 150px;" >
                <el-option v-for="item in targetTypeList" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input v-model="queryParams.targetName" placeholder="预警通知对象" clearable style="width: 200px;" @keyup.enter.native="searchQuery"/>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="searchQuery">
                搜索
              </el-button>
              <el-button type="primary" icon="el-icon-refresh" @click="resetQuery" style="margin-left: 10px;">
                重置
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-circle-plus-outline" @click="addTaskDue">新增</el-button>
              <el-button type="danger" icon="el-icon-delete" @click="deleteSelectedRows" style="margin-left: 10px;">批量删除</el-button>
            </el-form-item>
          </el-form>
          <el-table v-loading="loading" :data="taskOverdueList" style="width: 100%" border @selection-change="selectionChangeTaskDue">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column prop="businessName" label="组织名称" align="center" />
            <el-table-column prop="orgName" label="父组织" align="center" />
            <el-table-column label="通知对象类型" align="center" width="150px">
              <template slot-scope="scope">
                {{ scope.row.targetType === 1 ? '用户' : scope.row.targetType === 2 ? '群组' : '' }}
              </template>
            </el-table-column>
            <el-table-column prop="targetName" label="预警通知对象" align="center">
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right" align="center">
              <template slot-scope="scope">
                <el-button type="primary" size="small" @click="editTaskDue(scope.row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
          <!-- 分页组件 -->
          <div class="pagination-container">
            <el-pagination :current-page="pagination.pageNum" :page-sizes="[10, 20, 50, 100]"
              :page-size="pagination.pageSize" layout="total, sizes, prev, pager, next"
              :total="pagination.total" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
          </div>
        </el-tab-pane>

        <!-- 无人值守 Tab -->
        <el-tab-pane label="无人值守" name="unattended">
          <el-form :model="queryParams" :inline="true" class="tab-header-form">
            <el-form-item>
              <el-input v-model="queryParams.businessName" placeholder="协同岗名称" clearable style="width: 200px;" @keyup.enter.native="searchQuery"/>
            </el-form-item>
            <el-form-item>
              <el-select v-model="queryParams.targetType" placeholder="通知对象类型" clearable style="width: 150px;" >
                <el-option v-for="item in targetTypeList" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input v-model="queryParams.targetName" placeholder="预警通知对象" clearable style="width: 200px;" @keyup.enter.native="searchQuery" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="searchQuery" >
                搜索
              </el-button>
              <el-button type="primary" icon="el-icon-refresh" @click="resetQuery" style="margin-left: 10px;" >
                重置
              </el-button>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-circle-plus-outline" @click="addUnattended">新增</el-button>
              <el-button type="danger" icon="el-icon-delete" @click="deleteSelectedRows" style="margin-left: 10px;">批量删除</el-button>
            </el-form-item>
          </el-form>
          <el-table v-loading="loading" :data="unattendedList" style="width: 100%" border @selection-change="selectionChangeUnattended">
            <el-table-column type="selection" width="55" align="center" />
            <el-table-column prop="businessName" label="协同岗名称" align="center"/>
            <el-table-column prop="orgName" label="所属组织" align="center" />
            <el-table-column label="通知对象类型" align="center" width="150px">
              <template slot-scope="scope">
                {{ scope.row.targetType === 1 ? '用户' : scope.row.targetType === 2 ? '群组' : '' }}
              </template>
            </el-table-column>
            <el-table-column prop="targetName" label="预警通知对象" align="center"></el-table-column>
            <el-table-column label="操作" width="150" fixed="right" align="center">
              <template slot-scope="scope">
                <el-button type="primary" size="small" @click="editUnattended(scope.row)">编辑</el-button>
              </template>
            </el-table-column>
          </el-table>
          <!-- 分页组件 -->
          <div class="pagination-container">
            <el-pagination :current-page="pagination.pageNum" :page-sizes="[10, 20, 50, 100]"
              :page-size="pagination.pageSize" layout="total, sizes, prev, pager, next"
              :total="pagination.total" @size-change="handleSizeChange" @current-change="handleCurrentChange" />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script>
import { createDialog } from '@/utils/createDialog'
import TaskDueAddForm from './components/TaskDueAddForm'
import TaskDueEditForm from './components/TaskDueEditForm'
import UnattendedAddForm from './components/UnattendedAddForm'
import UnattendedEditForm from './components/UnattendedEditForm'
// 导入API函数
import { getWarningRelationList, createWarningRelation, updateWarningRelation, deleteWarningRelation } from '@/api/notification/alertPush'

export default {
  name: 'WarningRelation',
  data() {
    return {
      loading: false,
      // 弹窗函数
      taskDueAddDialog: null,
      taskDueEditDialog: null,
      unattendedAddDialog: null,
      unattendedEditDialog: null,
      activeTab: 'taskOverdue',
      // 任务逾期数据
      taskOverdueList: [],
      // 无人值守数据
      unattendedList: [],
      // 分页参数
      pagination: {
        pageNum: 1,
        pageSize: 10,
        total: 0
      },
      // 查询参数
      queryParams: {
        classify: 1,
        businessName: '',      
        targetType: '',
        targetName: '' 
      },
      // 多选
      multipleSelectionTaskDue: [],   // 任务逾期选中行
      multipleSelectionUnattended: [], // 无人值守选中行
      targetTypeList: [
        { label: '用户', value: 1 },
        { label: '群组', value: 2 }
      ]
    }
  },
  watch: {
    // 监听tab切换，重置查询参数并重新查询
    activeTab(newVal) {
      // 重置分页参数
      this.pagination.pageNum = 1
      this.pagination.total = 0
      // 更新分类参数
      this.queryParams.classify = newVal === 'taskOverdue' ? 1 : 2
      // 清空对应的选中行数组
      if (newVal === 'taskOverdue') {
        this.multipleSelectionUnattended = []
      } else if (newVal === 'unattended') {
        this.multipleSelectionTaskDue = []
      }
      // 清空所有搜索字段
      this.queryParams.businessName = ''
      this.queryParams.targetType = ''
      this.queryParams.targetName = ''
      // 重新查询数据
      this.getWarningList()
    }
  },
  created() {
    this.taskDueAddDialog = createDialog(TaskDueAddForm, { title: '添加任务逾期项' })
    this.taskDueEditDialog = createDialog(TaskDueEditForm, { title: '编辑任务逾期项' })
    this.unattendedAddDialog = createDialog(UnattendedAddForm, { title: '添加无人值守项' })
    this.unattendedEditDialog = createDialog(UnattendedEditForm, { title: '编辑无人值守项' })
  },
  mounted() {
    // 组件挂载时初始化数据
    this.getWarningList()
  },
  methods: {
    // 搜索方法
    searchQuery() {
      this.pagination.pageNum = 1
      this.getWarningList()
    },
    // 重置查询条件
    resetQuery() {
      this.queryParams.businessName = ''
      this.queryParams.targetType = ''
      this.queryParams.targetName = ''
      this.pagination.pageNum = 1
      this.getWarningList()
    },
    // 获取数据
    async getWarningList() {
      const params = {
        ...this.queryParams,
        pageNum: this.pagination.pageNum,
        pageSize: this.pagination.pageSize
      }
      try {
        this.loading = true
        const { data = {} } = await getWarningRelationList(params)

        switch (this.queryParams.classify) {
          case 1: this.taskOverdueList = data.records || []; break
          case 2: this.unattendedList = data.records || []; break
          default: break
        }

        this.pagination.total = Number(data?.total || 0)
      } catch (error) {
        console.error('获取数据失败:', error)
      } finally {
        this.loading = false
      }
    },
    // 切换每页条数
    handleSizeChange(pageSize) {
      this.pagination.pageSize = pageSize
      this.pagination.pageNum = 1
      this.getWarningList()
    },
    // 切换页码
    handleCurrentChange(pageNum) {
      this.pagination.pageNum = pageNum
      this.getWarningList()
    },
    // 任务逾期 - 新增
    async addTaskDue() {
      try {
        const result = await this.taskDueAddDialog()
        const params = {
          classify: 1,
          businessId: result.orgId,
          businessName: result.orgName,
          orgName: result.parentOrgName,
          targetType: result.targetType,
          targetIds: Array.isArray(result.targetId) ? result.targetId.join(',') : result.targetId,
          targetNames: result.targetName || [],
          idCard: Array.isArray(result.idCard) ? result.idCard.join(',') : result.idCard,
        }
        
        const { code, msg } = await createWarningRelation(params);
        if (code !== 0) return this.$message.error(msg);

        this.$message.success('新增成功');
        this.getWarningList();
      } catch (error) {
        console.error('addTaskDue catch error:', error)
      }
    },
    // 任务逾期 - 编辑
    async editTaskDue(row) {
      try {
        const result = await this.taskDueEditDialog({ props: { initialData: row } })
        const params = {
          classify: 1,
          id: result.id,
          businessId: result.orgId,
          businessName: result.orgName,
          orgName: result.parentOrgName,
          targetType: result.targetType,
          targetId: result.targetId,
          targetName: result.targetName,
          idCard: result.idCard,
        }

        const {code, msg} = await updateWarningRelation(params);
        if (code !== 0) return this.$message.error(msg);
        
        this.$message.success('更新成功')
        this.getWarningList();
      } catch (error) {
        // 用户取消对话框，不做任何处理
        if (error.type === 'cancel' || error.type === 'close') {
          return
        }
        console.error('editTaskDue error:', error)
      }
    },
    // 无人值守 - 新增
    async addUnattended() {
      try {
        const result = await this.unattendedAddDialog();
        const params = {
          classify: 2,
          businessId: result.cooperId,
          businessName: result.cooperName,
          orgName: result.belongOrgName,
          targetType: result.targetType,
          targetIds: Array.isArray(result.targetId) ? result.targetId.join(',') : result.targetId,
          targetNames: result.targetName || [],
          idCard: Array.isArray(result.idCard) ? result.idCard.join(',') : result.idCard,
        }

        const { code, msg } = await createWarningRelation(params);
        if (code !== 0) return this.$message.error(msg);

        this.$message.success('新增成功');
        this.getWarningList();
      } catch (error) {
        // 用户取消对话框，不做任何处理
        if (error.type === 'cancel' || error.type === 'close') {
          return
        }
        console.error('addUnattended error:', error)
      }
    },
    // 无人值守 - 编辑
    async editUnattended(row) {
      try {
        const result = await this.unattendedEditDialog({ props: { initialData: row } })
        const params = {
          classify: 2,
          id: result.id,
          businessId: result.cooperId,
          businessName: result.cooperName,
          orgName: result.belongOrgName,
          targetType: result.targetType,
          targetId: result.targetId,
          targetName: result.targetName,
          idCard: result.idCard,
        }

        const {code, msg} = await updateWarningRelation(params);
        if (code !== 0) return this.$message.error(msg);

        this.$message.success('更新成功');
        this.getWarningList();
      } catch (error) {
        // 用户取消对话框，不做任何处理
        if (error.type === 'cancel' || error.type === 'close') {
          return
        }
        console.error('editUnattended error:', error)
      }
    },
      // 任务逾期行多选
    selectionChangeTaskDue(selection) {
      this.multipleSelectionTaskDue = selection
    },
    // 无人值守行多选
    selectionChangeUnattended(selection) {
      this.multipleSelectionUnattended = selection
    },
    // 删除选中的行
    async deleteSelectedRows() {
      // 根据当前激活的tab获取对应的选中行数组
      const selectedRows = this.activeTab === 'taskOverdue' ? this.multipleSelectionTaskDue : this.multipleSelectionUnattended
      if (selectedRows.length === 0) return this.$message.warning('请先勾选行')
      
      try {
        await this.$confirm('确定要删除选中的数据吗？', '删除确认', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }).then(async () => {
          const ids = selectedRows.map(row => row.id);
          const { code, msg } = await deleteWarningRelation(ids);

          if (code !== 0) return this.$message.error(msg);
          this.$message.success('删除成功');
          this.getWarningList();

          this.activeTab === 'taskOverdue' 
            ? this.multipleSelectionTaskDue = []
            : this.multipleSelectionUnattended = []
        })
      } catch (error) {
        console.error('deleteWarningRelation catch error:', error)
      }
    },
  }
}
</script>

<style scoped>
.warningRelation {
  height: 100%;
}

.warningRelation-content {
  height: 100%;
  padding: 20px;
}

.el-tabs--border-card {
  margin: 10px;
}

.tab-header {
  margin-bottom: 20px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
