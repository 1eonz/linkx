<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container"></div>
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
          :label="$t('index.list.applicationName')"
          :show-overflow-tooltip="true"
          min-width="120"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.applicationKey')"
          :show-overflow-tooltip="true"
          min-width="70"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.appKey }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.applicationType')"
          :show-overflow-tooltip="true"
          min-width="70"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{
              scope.row.type === 0
                ? $t('index.list.internalApplication')
                : $t('index.list.externalApplication')
            }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.messageText.whetherToPublishOnlineNotification')"
          :show-overflow-tooltip="true"
          min-width="70"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{
              scope.row.isPublishOnline === 1
                ? $t('index.operations.yes')
                : $t('index.operations.no')
            }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.messageText.whetherMultipleLoginsAreAllowed')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{
              scope.row.isPublishOnline === 1
                ? $t('index.operations.allow')
                : $t('index.operations.notAllow')
            }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.messageText.whetherToPublishATimeoutExit')"
          :show-overflow-tooltip="true"
          min-width="60"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{
              scope.row.isTimeoutLogout === 1
                ? $t('index.operations.yes')
                : $t('index.operations.no')
            }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.condition')"
          class-name="status-col"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <el-tag :type="scope.row.status | statusFilter">
              {{
                scope.row.status === 0
                  ? $t('index.list.normal')
                  : $t('index.delete')
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          class-name="small-padding fixed-width"
          header-align="center"
          align="center"
          min-width="120"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="{ row }">
            <el-button
              v-if="hasPerm('/admin/application/update')"
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(row)"
            >
              {{ $t('index.operations.redact') }}
            </el-button>
            <el-button
              v-if="
                row.status === 0 && hasPerm('/admin-api/application/delete')
              "
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(row)"
            >
              {{ $t('index.delete') }}
            </el-button>
            <el-button
              v-else-if="
                row.status !== 0 && hasPerm('/admin/application/update')
              "
              type="warning"
              icon="el-icon-star-on"
              size="small"
              @click="resuming(row)"
            >
              {{ $t('index.operations.enabled') }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      :visible.sync="dialogFormVisible"
      :title="dialogStatus"
      :before-close="closeDialog"
      width="750px"
      custom-class="adaptive-dialog-wide"
    >
      <el-form
        ref="tempForm"
        :model="temp"
        :rules="rules"
        label-position="left"
        label-width="300px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.list.applicationName')" prop="name">
          <el-input v-model.trim="temp.name" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.applicationKey')" prop="appKey">
          <el-input v-model.trim="temp.appKey" disabled class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.callbackURL')" prop="redirectUri">
          <el-input v-model="temp.redirectUri" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.applicationType')" prop="type">
          <el-select
            v-model="temp.type"
            collapse-tags
            value-key="key"
            :placeholder="$t('index.list.selectApplicationType')"
            clearable
            style="width:300px"
            class="filter-item"
          >
            <el-option
              v-for="item in typeOptions"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="$t('index.messageText.whetherToPublishOnlineNotification')"
          prop="isPublishOnline"
        >
          <el-select
            v-model="temp.isPublishOnline"
            collapse-tags
            value-key="key"
            :placeholder="
              $t('index.messageText.selectWhetherToPublishOnlineNotification')
            "
            clearable
            style="width:300px"
            class="filter-item"
          >
            <el-option
              v-for="item in publishOptions"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item
          :label="$t('index.messageText.whetherToExitDueToTimeout')"
          prop="isTimeoutLogout"
        >
          <el-select
            v-model="temp.isTimeoutLogout"
            collapse-tags
            value-key="key"
            :placeholder="
              $t('index.messageText.selectWhetherToExitDueToTimeout')
            "
            clearable
            style="width:300px"
            class="filter-item"
          >
            <el-option
              v-for="item in timeoutLogoutOptions"
              :key="item.code"
              :label="item.name"
              :value="item.code"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('index.list.remarks')" prop="remark">
          <el-input v-model="temp.remark" class="edit-input" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="dialogStatus === $t('index.operations.newApplication')"
          type="primary"
          @click="create()"
        >
          {{ $t('index.create') }}
        </el-button>
        <el-button v-else type="primary" @click="update()">
          {{ $t('index.operations.alter') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createApplication,
  getApplicationList,
  updateApplication,
  deleteApplication
} from '@/api/permission/application'
import { deepCopy } from '@/utils'

export default {
  name: 'Application',
  filters: {
    statusFilter(status) {
      const statusMap = {
        '0': 'success',
        '1': 'danger'
      }
      return statusMap[status]
    }
  },
  data() {
    const rules = {
      name: [
        {
          required: true,
          message: this.$t('index.list.selectApplicationNameCannotNull'),
          trigger: 'blur'
        }
      ],
      appKey: [
        {
          required: true,
          message: this.$t('index.list.selectApplicationKeyCannotNull'),
          trigger: 'blur'
        }
      ],
      type: [
        {
          required: true,
          message: this.$t('index.list.selectApplicationTypeCannotNull'),
          trigger: 'blur'
        }
      ],
      isPublishOnline: [
        {
          required: true,
          message: this.$t(
            'index.messageText.whetherToPublishOnlineNotificationCannotBeLeftBlank'
          ),
          trigger: 'blur'
        }
      ],
      isTimeoutLogout: [
        {
          required: true,
          message: this.$t(
            'index.messageText.whetherToExitDueToTimeoutCannotBeEmpty'
          ),
          trigger: 'blur'
        }
      ]
    }
    return {
      list: [],
      personList: [],
      dialogStatus: this.$t('index.operations.redact'),
      dialogFormVisible: false,
      listLoading: false,
      multipleSelection: [],
      typeOptions: [
        { code: 0, name: this.$t('index.list.internalApplication') },
        { code: 1, name: this.$t('index.list.externalApplication') }
      ],
      publishOptions: [
        { code: 0, name: this.$t('index.operations.DonItRelease') },
        { code: 1, name: this.$t('index.operations.release') }
      ],
      timeoutLogoutOptions: [
        { code: 0, name: this.$t('index.operations.no') },
        { code: 1, name: this.$t('index.operations.yes') }
      ],
      rules: Object.freeze(rules),
      temp: {
        id: '',
        name: '',
        appKey: '',
        appSecret: '',
        redirectUri: '',
        type: '',
        isPublishOnline: '',
        isTimeoutLogout: '',
        remark: '',
        status: ''
      }
    }
  },
  created() {
    this.getList()
  },
  activated() {
    this.getList()
  },
  methods: {
    handleSelectionChange(val) {
      this.multipleSelection = val
    },
    handleCreate() {
      this.dialogStatus = this.$t('index.operations.newApplication')
      this.dialogFormVisible = true
    },
    handleUpdate(row) {
      this.temp = deepCopy(row)
      this.dialogStatus = this.$t('index.operations.applicationOfEditing')
      this.dialogFormVisible = true
    },
    handleDelete(data) {
      if (Array.isArray(data)) {
        if (data.length < 1) {
          this.$message({
            message: this.$t('index.messageText.pleaseCheckData'),
            type: 'error'
          })
          return
        } else {
          const array = []
          for (const Application of data) {
            array.push(Application.id)
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
        type: 'info'
      })
        .then(() => {
          deleteApplication(array).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              this.$message({
                message: this.$t('index.statusTitle.failToDelete'),
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    },
    create() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          createApplication(this.temp)
            .then(result => {
              if (result.code === 0) {
                this.$message({
                  message: this.$t('index.statusTitle.createSuccess'),
                  type: 'success'
                })
              } else {
                this.$message({
                  message: this.$t('index.statusTitle.createFail'),
                  type: 'error'
                })
              }
              this.closeDialog()
              this.getList()
            })
            .catch(() => {
              this.$message({
                message: this.$t('index.statusTitle.createFail'),
                type: 'error'
              })
            })
        } else {
          this.$message({
            message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
            type: 'warning'
          })
        }
      })
    },
    update() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          updateApplication(this.temp).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.closeDialog()
            this.getList()
          })
        }
      })
    },
    closeDialog() {
      this.$refs['tempForm'].resetFields()
      this.dialogFormVisible = false
    },
    closeDetailDialog() {
      this.detailDialogFormVisible = false
    },
    handleSelection(val) {
      this.multipleSelection = val
    },
    getList() {
      getApplicationList().then(({ data }) => {
        this.list = data
        this.listLoading = false
      })
    },
    resuming(row) {
      this.temp = Object.assign({}, row)
      this.temp.status = 0
      updateApplication(this.temp).then(result => {
        if (result.code === 0) {
          this.getList()
        } else {
          this.$message({
            message: this.$t('index.statusTitle.enabledSucceed'),
            type: 'error'
          })
        }
      })
    }
  }
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
.edit-input {
  padding-right: 50px;
  width: 350px;
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
</style>
