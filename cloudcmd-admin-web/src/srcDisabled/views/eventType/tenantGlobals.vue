<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.organizationName"
          :placeholder="$t('index.list.organizationName')"
          style="width: 200px"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-button
          v-waves
          class="filter-item"
          type="primary"
          icon="el-icon-search"
          @click="handleFilter"
        >
          {{ $t('index.operations.search') }}
        </el-button>
        <el-button
          class="filter-item"
          style="margin-left: 10px"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
        <el-button
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
        :data="list"
        stripe
        border
        fit
        highlight-current-row
        style="width: 100%"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('index.list.organizationName')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.organizationSerial')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.code }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.organizationalProfile')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.remark }}</span>
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
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.change') }}
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
        :page.sync="listQuery.page"
        :limit.sync="listQuery.limit"
        @pagination="getList"
      />
    </el-card>
    <!-- 租户添加 -->
    <tenantGlobalsEdit ref="edit" :org-list="orgList" @success="getList" />
    <!-- 租户参数编辑 -->
    <tenantGlobalsDetail ref="detail" @success="getList" />
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import tenantGlobalsDetail from './components/tenantGlobalsDetail.vue'
import tenantGlobalsEdit from './components/tenantGlobalsEdit.vue'
import { getOrganizationList } from '@/api/resource/organization'
import {
  getTenantOrganizatio,
  deleteTenant,
} from '@/api/dictionary/tenantGlobals'

export default {
  name: 'TenantGlobals',
  components: { pagination, tenantGlobalsEdit, tenantGlobalsDetail },
  data() {
    return {
      list: [],
      orgList: [],
      tempLoading: false,
      total: 0,
      listLoading: false,
      multipleSelection: [],
      listQuery: {
        page: 1,
        limit: 10,
        organizationName: '',
      },
    }
  },
  created() {
    this.getList()
    this.getOrgList()
  },
  activated() {
    this.getList()
  },
  methods: {
    getOrgList() {
      getOrganizationList().then(({ data }) => {
        this.orgList = data
      })
    },
    handleSelectionChange(val) {
      this.multipleSelection = val
    },

    handleCreate() {
      this.$refs.edit.add()
    },
    handleUpdate(row) {
      this.$refs.detail.modify(row)
    },

    handleDelete(data) {
      if (Array.isArray(data)) {
        if (data.length < 1) {
          this.$message({
            message: this.$t('index.messageText.pleaseCheckData'),
            type: 'error',
          })
          return
        } else {
          const array = []
          for (const person of data) {
            array.push(person.id)
          }
          this.delete(array)
        }
      } else {
        this.delete(Array.of(data.id))
      }
    },

    delete(array) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info',
      })
        .then(() => {
          deleteTenant(array).then((result) => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success',
              })
            } else {
              this.$message({
                message: result.msg,
                type: 'error',
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    },

    handleSelection(val) {
      this.multipleSelection = val
    },

    getList() {
      getTenantOrganizatio(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    },

    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
  },
}
</script>

<style lang="scss" scoped>
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
</style>
