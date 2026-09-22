<template>
  <el-dialog
    :visible.sync="visible"
    width="70%"
    :title="title"
    @close="closeDialog"
  >
    <el-table
      v-loading="listLoading"
      :data="list"
      stripe
      border
      fit
      highlight-current-row
      style="width: 100%"
    >
      <el-table-column
        :label="$t('index.list.profileName')"
        :show-overflow-tooltip="true"
        min-width="120"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.operations.configurationValue')"
        :show-overflow-tooltip="true"
        min-width="120"
        align="center"
      >
        <template slot-scope="scope">
          <span v-if="currentRow.id !== scope.row.id">
            {{ scope.row.value }}
          </span>
          <el-input v-else v-model="scope.row.value" class="edit-input" />
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.remarks')"
        :show-overflow-tooltip="true"
        min-width="100"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.remark }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.condition')"
        class-name="status-col"
        min-width="50"
        align="center"
      >
        <template slot-scope="scope">
          <el-tag :type="scope.row.status | statusFilter">
            {{
              scope.row.status === 0
                ? $t('index.list.normal')
                : $t('index.list.forbidden')
            }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        max-width="120"
        :label="$t('index.operations.operation')"
      >
        <template slot-scope="scope">
          <div v-if="currentRow.id === scope.row.id">
            <el-button
              type="primary"
              size="small"
              @click="handleCommit(scope.row)"
            >
              {{ $t('index.determine') }}
            </el-button>
            <el-button size="small" @click="handleCancel()">
              {{ $t('index.cancel') }}
            </el-button>
          </div>
          <div v-else>
            <el-button
              v-if="hasPerm('/admin/globals/update')"
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.redact') }}
            </el-button>
            <el-button
              v-if="scope.row.status !== 0 && hasPerm('/admin/globals/update')"
              type="warning"
              icon="el-icon-star-on"
              size="small"
              @click="resuming(scope.row)"
            >
              {{ $t('index.operations.restore') }}
            </el-button>
          </div>
        </template>
      </el-table-column>
    </el-table>
    <div slot="footer" class="dialog-footer">
      <el-button @click="visible = false">
        {{ $t('index.cancel') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import {
  selectTenantGlobals,
  updateTenantGlobals,
} from '@/api/dictionary/tenantGlobals'

export default {
  name: 'TenantGlobalsDetail',
  filters: {
    statusFilter(status) {
      const statusMap = {
        0: 'success',
        1: 'danger',
      }
      return statusMap[status]
    },
  },
  data() {
    return {
      list: [],
      currentRow: {},
      listLoading: false,
      orgId: '',
      title: this.$t('index.operations.redact'),
      visible: false,
    }
  },
  methods: {
    getList() {
      this.listLoading = true
      this.list = []
      selectTenantGlobals({ orgId: this.orgId }).then(({ data }) => {
        this.list = data
        this.listLoading = false
      })
    },
    resuming(row) {
      const params = { ...row, status: 0 }
      updateTenantGlobals(params).then((result) => {
        if (result.code === 0) {
          this.getList()
        }
      })
    },
    handleCommit(row) {
      if (row.value === '') {
        this.$message({
          message: this.$t('index.list.configCannotEmpty'),
          type: 'error',
        })
        return
      }
      const params = { ...row }
      updateTenantGlobals(params).then((result) => {
        if (result.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.changeSuccess'),
            type: 'success',
          })
          this.getList()
        }
      })
      this.currentRow = {}
    },
    handleCancel() {
      this.currentRow = {}
      this.getList()
    },
    handleUpdate(row) {
      this.currentRow = row
    },
    modify({ id }) {
      this.title = this.$t('index.operations.redact')
      this.visible = true
      this.orgId = id
      this.getList()
    },
    closeDialog() {
      this.currentRow = {}
      this.visible = false
    },
  },
}
</script>

<style scoped>
.edit-input {
  width: 215px;
}
.tree-style {
  max-height: 240px;
  overflow: auto;
}
.el-row {
  text-align: left;
  margin-left: 10px;
}
.el-col {
  margin: 8px;
}
.el-checkbox-group {
  min-width: 500px;
}
</style>
