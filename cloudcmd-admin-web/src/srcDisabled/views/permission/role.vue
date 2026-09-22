<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQuery.name"
          :placeholder="$t('index.list.roleName')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-select
          v-model="listQuery.type"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.roleType')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in roleTypeList"
            :key="item.code"
            :label="item.name"
            :value="item.code"
          />
        </el-select>
        <el-select
          v-model="listQuery.applicationId"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.applicationOfAffiliation')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in applicationList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
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
          v-if="hasPerm('/admin/role/create')"
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
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
        <el-table-column
          :label="$t('index.list.roleName')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.roleType')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.typeName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.applicationOfAffiliation')"
          :show-overflow-tooltip="true"
          min-width="100"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.applicationName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.condition')"
          :show-overflow-tooltip="true"
          min-width="60"
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
          min-width="200"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              v-if="hasPerm('/admin-api/role/listByPage')"
              type="info"
              icon="el-icon-document"
              size="small"
              @click="getDetails(scope.row)"
            >
              {{ $t('index.operations.particulars') }}
            </el-button>
            <template v-if="scope.row.type !== 505001">
              <el-button
                v-if="hasPerm('/admin/role/update')"
                type="primary"
                icon="el-icon-edit"
                size="small"
                @click="handleUpdate(scope.row)"
              >
                {{ $t('index.operations.change') }}
              </el-button>
              <el-button
                v-if="
                  showPermissionButton(scope.row) &&
                    hasPerm('/admin/permission/configPermission')
                "
                icon="el-icon-document"
                type="success"
                size="small"
                @click="managePermission(scope.row)"
              >
                {{ $t('index.authority.setPermission') }}
              </el-button>
              <el-button
                v-if="hasPerm('/admin/role/delete')"
                type="danger"
                icon="el-icon-delete"
                size="small"
                @click="handleDelete(scope.row)"
              >
                {{ $t('index.delete') }}
              </el-button>
              <el-button
                v-if="scope.row.status === 0 && hasPerm('/admin/role/update')"
                type="danger"
                icon="el-icon-goods"
                size="small"
                @click="handleStatus(scope.row, 1)"
              >
                {{ $t('index.list.forbidden') }}
              </el-button>
              <el-button
                v-else-if="
                  scope.row.status !== 0 && hasPerm('/admin/role/update')
                "
                type="warning"
                icon="el-icon-star-on"
                size="small"
                @click="handleStatus(scope.row, 0)"
              >
                {{ $t('index.operations.enabled') }}
              </el-button>
            </template>
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

    <!-- 权限设置 -->
    <permission-dialog ref="permissionDialog" />

    <!-- 详情 -->
    <role-details ref="details" />

    <!-- 新增修改 -->
    <el-dialog
      :visible.sync="dialogFormVisible"
      :destroy-on-close="true"
      :close-on-click-modal="false"
      :title="dialogStatus"
      width="650px"
      custom-class="adaptive-dialog"
      @close="closeDialog"
    >
      <el-form
        ref="tempForm"
        :model="form"
        label-position="left"
        :rules="rules"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.list.roleName')" prop="name">
          <el-input v-model.trim="form.name" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.roleType')" prop="type">
          <el-select
            v-model="form.type"
            :disabled="dialogStatus !== $t('index.operations.newsRole')"
            collapse-tags
            value-key="key"
            :placeholder="$t('index.operations.selectRoleType')"
            clearable
            class="edit-select"
          >
            <el-option
              v-for="item in roleTypeList"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="$t('index.list.applicationOfAffiliation')"
          prop="applicationId"
        >
          <el-select
            v-model="form.applicationId"
            :disabled="dialogStatus !== $t('index.operations.newsRole')"
            collapse-tags
            value-key="key"
            :placeholder="$t('index.list.selectingApplication')"
            clearable
            class="edit-select"
          >
            <el-option
              v-for="item in applicationList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="this.$t('index.list.remarks')" prop="code">
          <el-input v-model="form.remark" class="edit-input" />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="dialogStatus === $t('index.operations.newsRole')"
          type="primary"
          @click="handleEdit(true)"
        >
          {{ $t('index.create') }}
        </el-button>
        <el-button v-else type="primary" @click="handleEdit(false)">
          {{ $t('index.operations.alter') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import pagination from '@/components/Pagination'
import {
  createRole,
  deleteRole,
  getRoleListByPage,
  getRoleTypeList,
  updateRole
} from '@/api/permission/role'
import { getApplicationList } from '@/api/permission/application'
import permissionDialog from './components/permissionDialog'
import roleDetails from './components/roleDetails'
import { deepCopy } from '@/utils'

const form = {
  id: '',
  name: '',
  type: '',
  typeName: '',
  applicationId: '',
  applicationName: '',
  applicationKey: '',
  remark: '',
  status: 0
}

export default {
  name: 'Role',
  filters: {
    statusFilter(status) {
      const statusMap = {
        0: 'success',
        1: 'danger'
      }
      return statusMap[status]
    }
  },
  components: { pagination, permissionDialog, roleDetails },
  data() {
    const rules = {
      name: [
        {
          required: true,
          message: this.$t('index.messageText.roleNameCannotBeEmpty'),
          trigger: 'blur'
        }
      ],
      type: [
        {
          required: true,
          message: this.$t('index.messageText.roleTypeCannotBeEmpty'),
          trigger: 'change'
        }
      ],
      applicationId: [
        {
          required: true,
          message: this.$t('index.list.roleOwningApplicationCannotNull'),
          trigger: 'change'
        }
      ]
    }
    return {
      list: [],
      roleTypeList: [],
      applicationList: [],
      listLoading: false,
      dialogFormVisible: false,
      dialogStatus: '',
      total: 0,
      listQuery: {
        page: 1,
        limit: 10,
        name: '',
        type: '',
        applicationId: ''
      },
      rules: Object.freeze(rules),
      form: deepCopy(form)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      getRoleListByPage(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
      getRoleTypeList().then(({ data }) => {
        this.roleTypeList = data
      })
      getApplicationList().then(({ data }) => {
        this.applicationList = data
      })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.getList()
    },
    handleCreate() {
      this.dialogStatus = this.$t('index.operations.newsRole')
      this.dialogFormVisible = true
      this.resetTemp()
    },
    handleEdit(isAdd) {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const param = deepCopy(this.form)
          const api = isAdd ? createRole : updateRole
          api(param).then(result => {
            if (result.code === 0) {
              this.$message({
                message: isAdd
                  ? this.$t('index.statusTitle.createSuccess')
                  : this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
              this.getList()
              this.closeDialog()
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        } else {
          this.$message({
            message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
            type: 'warning'
          })
        }
      })
    },
    handleUpdate(row) {
      this.form = deepCopy(row)
      this.dialogStatus = this.$t('index.operations.editRole')
      this.dialogFormVisible = true
    },
    handleDelete({ id }) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteRole(id).then(result => {
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
        .catch(() => {})
    },
    getDetails(row) {
      this.$refs.details.setData(row)
    },
    closeDialog() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.dialogFormVisible = false
    },
    handleStatus(row, status) {
      const param = {
        ...deepCopy(row),
        status
      }
      updateRole(param).then(result => {
        if (result.code === 0) {
          this.getList()
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
      })
    },

    showPermissionButton(row) {
      if (row.type !== 505001) {
        return true
      }
    },

    managePermission(row) {
      this.$refs.permissionDialog.init(row)
    },
    resetTemp() {
      const form = this.form
      for (const key in form) {
        this.form[key] = ''
      }
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
