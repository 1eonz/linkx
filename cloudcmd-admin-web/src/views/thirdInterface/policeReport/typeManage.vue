<template>
  <div class="type-manage">
    <el-form
      ref="queryFormRef"
      class="-mb-15px"
      :model="queryParams"
      :inline="true"
      label-width="68px"
      @submit.native.prevent="handleQuery"
    >
      <el-form-item label="" prop="tag">
        <el-input
          v-model="queryParams.tag"
          placeholder="类型名称"
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
      <el-button
        class="filter-item"
        style="margin-left: 10px;"
        type="primary"
        icon="el-icon-circle-plus-outline"
        @click="openForm('create')"
      >
        {{ $t('index.operations.Added') }}
      </el-button>
    </el-form>
    <div class="table-card">
      <el-table
        border
        v-loading="loading"
        :data="list"
        :show-overflow-tooltip="true"
      >
        <el-table-column
          label="类型名称"
          show-overflow-tooltip
          align="center"
          prop="tag"
        />
        <el-table-column
          label="操作时间"
          show-overflow-tooltip
          align="center"
          prop="gmtCreated"
          width="200px"
        />
        <el-table-column label="操作" align="center" min-width="180px">
          <template #default="scope">
            <div class="btns">
              <div class="action-edit" @click="openForm('update', scope.row)">编辑</div>
              <div class="action-delete" @click="handleDelete(scope.row.id)">删除</div>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <!-- 分页 -->
    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pagenum"
      :limit.sync="queryParams.pagesize"
      @pagination="handleQuery"
    />
    <type-edit ref="formRef" @success="searchFunc" />
  </div>
</template>

<script>
import {
  getPolicetickettypesPage,
  deletePolicetickettypes
} from '@/api/policeReport/dock'
import typeEdit from './typeEdit.vue'
export default {
  name: 'type-manage',
  components: {
    typeEdit
  },
  data() {
    return {
      queryParams: {
        tag: '',
        pagenum: 1,
        pagesize: 10
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
    searchFunc() {
      this.queryParams.current = 1
      this.handleQuery()
    },
    changePage() {
      this.getList()
    },
    handleReset() {
      this.$refs['queryFormRef'].resetFields()
      this.handleQuery()
    },
    handleQuery() {
      this.getList()
    },
    async getList() {
      this.loading = true
      try {
        const { code, data } = await getPolicetickettypesPage({
          ...this.queryParams
        })
        if (code === 0) {
          this.list = data.records
          this.total = Number(data.total || 0)
        }
      } finally {
        this.loading = false
      }
    },
    async openForm(type, row) {
      this.detailObj = row
      this.$refs.formRef.open(type, row)
    },
    async handleDelete(id) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deletePolicetickettypes(id).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
              this.searchFunc()
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        })
        .catch(() => {})
    },
    handleOpen(type) {
      this.$refs.formRef.open(type)
    }
  }
}
</script>

<style lang="scss" scoped>
.type-manage {
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

.btns {
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;

  div {
    cursor: pointer;
    padding: 0;

    &:not(:last-child)::after {
      content: '|';
      padding: 0 8px;
      color: #eeeff3;
    }
  }

  .action-edit {
    color: rgba(38, 99, 255, 1);
  }

  .action-delete {
    color: rgba(245, 83, 83, 1);
  }
}
</style>