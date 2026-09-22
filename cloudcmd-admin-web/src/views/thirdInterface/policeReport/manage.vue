<template>
  <div class="dock-container">
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="68px"
    >
      <el-form-item label="" prop="code">
        <el-input
          v-model="queryParams.code"
          placeholder="警单编号"
          clearable
          style="width: 180px;"
          class="filter-item"
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="" prop="name">
        <el-input
          v-model="queryParams.name"
          placeholder="警单名称"
          clearable
          style="width: 180px;"
          class="filter-item"
          @keyup.enter.native="handleQuery"
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
          @click="handleReset"
        >
          重置
        </el-button>
      </el-form-item>
    </el-form>

    <div class="table-card">
      <el-table
        border
        v-loading="loading"
        :data="list"
        :show-overflow-tooltip="true"
      >
        <el-table-column
          label="警单编号"
          show-overflow-tooltip
          align="center"
          prop="code"
        />
        <el-table-column
          label="警单名称"
          show-overflow-tooltip
          align="center"
          prop="name"
        />
        <el-table-column
          label="警单内容"
          show-overflow-tooltip
          align="center"
          prop="content"
        />
        <el-table-column label="三方警单类型" align="center" prop="tag" />
        <el-table-column
          label="警单来源"
          show-overflow-tooltip
          align="center"
          prop="source"
        />
        <el-table-column
          label="操作时间"
          align="center"
          prop="createTime"
          width="200px"
        />
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="queryParams.current"
        :limit.sync="queryParams.size"
        @pagination="changePage"
      />
    </div>
    <!-- 表单弹窗：添加/修改 -->
    <manage-detail ref="formRef" :detail-obj="detailObj" />
  </div>
</template>

<script>
import manageDetail from './manageDetail.vue'
import { policeticketPage } from '@/api/policeReport/dock'
export default {
  components: { manageDetail },
  data() {
    return {
      queryParams: {
        code: '',
        name: '',
        current: 1,
        size: 10
      },
      list: [],
      loading: false,
      total: 0,
      isAdmin: false,
      detailObj: {}
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    changePage() {
      this.getList()
    },
    handleReset() {
      this.$refs['queryFormRef'].resetFields()
      this.handleQuery()
    },
    handleQuery() {
      this.queryParams.current = 1
      this.getList()
    },
    async getList() {
      this.loading = true
      try {
        const { code, data } = await policeticketPage({ ...this.queryParams })
        if (code === 0) {
          this.list = data.records
          this.total = Number(data.total || 0)
        }
      } finally {
        this.loading = false
      }
    },
    async openForm(row) {
      this.detailObj = row
      this.$refs.formRef.open(row)
    },
    async handleDelete(id) {},
    handleOpen(type) {
      this.$refs.formRef.open(type)
    },
    dataAsync() {}
  }
}
</script>
<style lang="scss" scoped>
.dock-container {
  width: 100%;
  height: 100%;
  flex: 1;
  min-height: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  padding: 20px;
}

.table-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

::v-deep .el-table {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;

  .el-table__body-wrapper {
    flex: 1;
    overflow-y: auto;
  }

  .el-table__header th {
    background-color: #f5f7fa;
    color: #606266;
  }
}
</style>