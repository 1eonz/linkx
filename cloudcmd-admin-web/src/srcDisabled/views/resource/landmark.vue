<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-button
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-picture"
          @click="handleUpload"
        >
          {{ $t('index.list.batchUploadFace') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/equipment/delete')"
          v-waves
          class="filter-item"
          type="danger"
          icon="el-icon-delete"
          @click="handleDelete(multipleSelection)"
        >
          {{ $t('index.operations.batchRemove') }}
        </el-button>
      </div>

      <el-table
        v-loading="listLoading"
        :data="tableData"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('index.list.face')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <authImg
              v-if="scope.row.iconInfo"
              class="head-shot"
              :auth-src="scope.row.iconInfo"
            />
          </template>
        </el-table-column>
        <el-table-column
          fixed="right"
          header-align="center"
          align="center"
          min-width="160"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
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
        :page.sync="listQuery.page"
        :limit.sync="listQuery.limit"
        @pagination="getList"
      />

      <!-- 批量上传 -->
      <upload-icon ref="uploadIcon" type="3" @success="getList" />
    </el-card>
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import { queryIconList, deleteIconBatch } from '@/api/resource/plan'
import authImg from '@/components/AuthImg'
import uploadIcon from './uploadIcon.vue'

export default {
  name: 'Landmark',
  components: { pagination, uploadIcon, authImg },
  data() {
    return {
      tableData: [],
      total: 0,
      listLoading: false,
      listQuery: {
        page: 1,
        limit: 10
      },
      multipleSelection: []
    }
  },
  mounted() {
    this.getList()
  },
  methods: {
    async getList() {
      const { page, limit } = this.listQuery
      const { code, data } = await queryIconList({
        type: 3,
        pageNum: page,
        pageSize: limit
      })
      if (code === 0) {
        this.tableData = data.records
        this.total = data.total * 1
      }
    },

    handleUpload() {
      this.$refs.uploadIcon.init()
    },
    handleSelection(val) {
      this.multipleSelection = val
    },
    handleDelete(data) {
      let param = []
      if (!Array.isArray(data)) {
        param.push(data.id)
      } else {
        param = data.map(i => i.id)
      }
      if(param.length < 1) {
        this.$message({
          message: this.$t('index.messageText.pleaseCheckData'),
          type: 'error'
        })
        return;
      }
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteIconBatch(param).then(({ code, msg }) => {
            if (code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              this.$message({
                message: msg || this.$t('index.statusTitle.failToDelete'),
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    }
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
  height: 22px;
  width: 22px;
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
