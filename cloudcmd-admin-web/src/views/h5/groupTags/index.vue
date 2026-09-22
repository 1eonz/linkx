<template>
  <div class="app-container">
    <!-- 搜索工作栏 -->
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="68px"
      @submit.native.prevent="handleQuery"
      >
      <el-form-item label="" prop="name">
      <el-input
        v-model="queryParams.name"
        placeholder="标签名称"
        clearable
        style="width: 300px;"
        class="filter-item"
        @keyup.enter="handleQuery"
        />
      </el-form-item>
      <el-form-item>
        <el-button
        v-waves
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-search"
        @click="handleQuery"
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
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="openForm('create')"
          >
          {{ $t('index.operations.Added') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="danger"
          icon="el-icon-delete"
          @click="batchDeleteLabels"
          >
          批量删除
        </el-button>
      </el-form-item>
    </el-form>

    <!-- 列表 -->
    <el-card>
      <el-table v-loading="loading" :data="list" :stripe="true" :show-overflow-tooltip="true" @selection-change="selectionChangeLabelTable">
        <el-table-column type="selection" width="55" />
        <el-table-column label="序号" align="center" type="index" />
        <el-table-column label="图标" align="center" prop="icon">
          <template #default="scope">
            <i :class="scope.row.icon" :style="`font-size: 20px;color:${scope.row.color}`"></i>
          </template>
        </el-table-column>
        <el-table-column label="标签名称" align="center" prop="name" show-overflow-tooltip />
        <!-- <el-table-column label="颜色" align="center" prop="color" /> -->
        <el-table-column label="操作" align="center" min-width="120px" width="200">
          <template #default="scope">
            <el-button type="primary" icon="el-icon-edit" size="small" @click="openForm('update', scope.row.id)">
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button type="danger" icon="el-icon-delete" size="small" @click="handleDelete(scope.row.id)">
              {{ $t('index.delete') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页 -->
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.pageNum"
        :limit.sync="queryParams.pageSize"
        @pagination="getList"
        />
    </el-card>

    <!-- 表单弹窗：添加/修改 -->
    <app-form ref="formRef" @success="getList" />
  </div>
</template>

<script>
import { getInfoPage, deleteInfo, tagsBatchDelete } from '@/api/h5/groupTags'
import pagination from '@/components/Pagination'
import authImg from '@/components/AuthImg'
import appForm from './appForm'
export default {
  name: 'AppInfo',
  components: { pagination, appForm, authImg },
  data() {
    return {
      list: [],
      loading: true,
      total: 0,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        name: undefined,
      },
      // 选中的行数据
      multipleSelection: []
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    async getList() {
      this.loading = true
      try {
        const { data } = await getInfoPage(this.queryParams)
        this.list = data.records
        this.total = data.total
      } finally {
        this.loading = false
      }
    },

    /** 添加/修改操作 */
    openForm(type, id) {
      this.$refs.formRef.open(type, id)
    },

    /** 删除按钮操作 */
    async handleDelete(id) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteInfo(id).then(result => {
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
        .catch(() => { })
    },

    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },

    /** 重置按钮操作 */
    resetQuery() {
      this.$refs['queryFormRef'].resetFields()
      this.handleQuery()
    },
    
    // 处理选择变化
    selectionChangeLabelTable(selection) {
      this.multipleSelection = selection
    },
    
    // 批量删除
    batchDeleteLabels() {
      if (this.multipleSelection.length === 0) {
        this.$message.warning('请先选择要删除的标签')
        return
      }
      
      // 二次确认
      this.$confirm('确定要删除选中的标签吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          // 调用批量删除接口
          const ids = this.multipleSelection.map(row => row.id)
          const { code, msg } = await tagsBatchDelete(ids)
          if (code === 0) {
            this.$message.success('删除成功')
            this.getList()
            this.multipleSelection = []
          } else {
            this.$message.error(msg || '删除失败，请重试')
          }
        } catch (error) {
          this.$message.error('删除失败，请重试')
        }
      })
    },
  }
}

</script>
<style lang="scss" scoped>
.card {
  height: 100%;
  overflow-y: auto;
}
.head-shot {
  display: inline-block;
  height: 40px;
  width: 40px;
}
</style>
