<template>
  <div class="app-container">
    <el-card shadow="always" class="card">
      <div class="filter-container">
        <el-input
          v-model="listQueryHelper.account"
          :placeholder="$t('index.list.accountNumber')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQueryHelper.alias"
          :placeholder="$t('index.list.accountAliasReceipt')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-select
          v-model="listQueryHelper.typeId"
          collapse-tags
          value-key="key"
          :placeholder="$t('index.list.accountType')"
          clearable
          class="filter-item"
        >
          <el-option
            v-for="item in accountTypeOptions"
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
          v-if="hasPerm('/admin/serviceAccount/create')"
          class="filter-item"
          style="margin-left: 10px;"
          type="primary"
          icon="el-icon-circle-plus-outline"
          @click="handleCreate"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/serviceAccount/download')"
          class="filter-item"
          style="margin-left: 10px;"
          type="info"
          icon="el-icon-download"
          @click="download"
        >
          {{ $t('index.operations.export') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/serviceAccount/delete')"
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
        style="width: 100%;"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" align="center" width="40" />
        <el-table-column
          :label="$t('index.list.account')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.account }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.alias')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.alias }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.type')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.typeName }}</span>
          </template>
        </el-table-column>
        <el-table-column
          :label="$t('index.list.associationPerformer')"
          :show-overflow-tooltip="true"
          min-width="80"
          align="center"
        >
          <template slot-scope="scope">
            <span>{{ scope.row.executorName }}</span>
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
          fixed="right"
          header-align="center"
          align="center"
          min-width="150"
          :label="$t('index.operations.operation')"
        >
          <template slot-scope="scope">
            <el-button
              v-if="hasPerm('/admin/serviceAccount/id')"
              type="info"
              icon="el-icon-document"
              size="small"
              @click="getDetails(scope.row)"
              >{{ $t('index.operations.particulars') }}
            </el-button>
            <el-button
              v-if="hasPerm('/admin/serviceAccount/update')"
              type="primary"
              icon="el-icon-edit"
              size="small"
              @click="handleUpdate(scope.row)"
            >
              {{ $t('index.operations.change') }}
            </el-button>
            <el-button
              v-if="
                scope.row.status === 0 &&
                  hasPerm('/admin/serviceAccount/delete')
              "
              type="danger"
              icon="el-icon-delete"
              size="small"
              @click="handleDelete(scope.row)"
              >{{ $t('index.delete') }}
            </el-button>
            <el-button
              v-else-if="
                scope.row.status !== 0 &&
                  hasPerm('/admin/serviceAccount/update')
              "
              type="warning"
              icon="el-icon-star-on"
              size="small"
              @click="resuming(scope.row)"
              >{{ $t('index.operations.restore') }}
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

    <el-dialog
      :visible.sync="dialogFormVisible"
      :title="dialogStatus"
      @close="closeDialog"
    >
      <el-form
        ref="tempForm"
        :model="temp"
        :rules="rules"
        label-position="left"
        label-width="200px"
        style="width: 600px; margin-left:30px"
      >
        <el-form-item
          :label="`${$t('index.list.account')}(${$t('index.list.only')})`"
          prop="account"
        >
          <el-input
            v-model.trim="temp.account"
            class="edit-input"
            :disabled="dialogStatus === $t('index.operations.redactAccount')"
          />
        </el-form-item>
        <el-form-item :label="$t('index.pass.pass')" prop="password">
          <el-input
            v-model.trim="temp.password"
            type="password"
            class="edit-input"
          />
        </el-form-item>
        <el-form-item :label="$t('index.list.accountType')" prop="typeId">
          <el-select
            v-model="temp.typeId"
            clearable
            class="filter-item"
            style="width: 300px"
            :placeholder="$t('index.list.selectAccountType')"
          >
            <el-option
              v-for="item in accountTypeOptions"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('index.list.alias')" prop="alias">
          <el-input v-model.trim="temp.alias" class="edit-input" />
        </el-form-item>
        <el-form-item :label="this.$t('index.list.remarks')" prop="remark">
          <el-input v-model="temp.remark" class="edit-input" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="dialogStatus === $t('index.operations.newAccount')"
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

    <el-dialog
      :visible.sync="detailDialogFormVisible"
      :title="$t('index.operations.detailedInformation')"
      @close="closeDetailDialog"
    >
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.account') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.account }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.accountAliasReceipt') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.alias }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.accountType') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.typeName }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ this.$t('index.list.remarks') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.remark }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span> {{ this.$t('index.createTime') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.gmtCreated }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.operations.change') + $t('index.time') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.gmtModified }}</span>
          </template>
        </el-col>
      </el-row>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="detailDialogFormVisible = false">
          {{ $t('index.determine') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Pagination from '@/components/Pagination'
import {
  createAccount,
  getAccountList,
  getAccountById,
  updateAccount,
  deleteAccount,
  getAccountTypeList,
  downloadAccounts
} from '@/api/resource/account'
import waves from '@/directive/waves'

export default {
  name: 'Account',
  components: { Pagination },
  directives: { waves },
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
    const validateIsNum = (rule, value, callback) => {
      if (value) {
        if (!/(^[0-9]*$)/.test(value)) {
          callback(new Error(this.$t('index.messageText.inputNumber')))
        } else {
          callback()
        }
      } else {
        callback()
      }
    }
    return {
      list: [],
      multipleSelection: [],
      total: 0,
      dialogStatus: this.$t('index.operations.redactAccount'),
      dialogFormVisible: false,
      detailDialogFormVisible: false,
      listLoading: false,
      listQueryHelper: {
        account: '',
        alias: '',
        typId: ''
      },
      accountTypeOptions: [],
      rules: {
        account: [
          {
            required: true,
            message: this.$t('index.messageText.accountCannotBeEmpty'),
            trigger: 'blur'
          },
          { validator: validateIsNum, trigger: 'blur' }
        ],
        password: [
          {
            required: true,
            message: this.$t('index.pass.inputPasswordNoNull'),
            trigger: 'blur'
          }
        ],
        typeId: [
          {
            required: true,
            message: this.$t('index.list.selectAccountTypeCannotNull'),
            trigger: 'change'
          }
        ],
        alias: [
          {
            required: true,
            message: this.$t('index.messageText.accountAliasCannotBeEmpty'),
            trigger: 'blur'
          }
        ]
      },
      listQuery: {
        page: 1,
        limit: 10,
        alias: '',
        typeId: '',
        account: ''
      },
      temp: {
        id: '',
        account: '',
        password: '',
        alias: '',
        typeName: '',
        executorName: '',
        typeId: '',
        remark: ''
      }
    }
  },
  watch: {
    dialogFormVisible() {
      if (!this.dialogFormVisible) {
        this.$refs['tempForm'].resetFields()
      }
    }
  },
  created() {
    this.getList()
    this.getAccountTypeList()
  },
  activated() {
    this.getList()
    this.getAccountTypeList()
  },
  methods: {
    handleSelectionChange(val) {
      this.multipleSelection = val
    },
    handleCreate() {
      this.resetTemp()
      this.dialogStatus = this.$t('index.operations.newAccount')
      getAccountTypeList().then(({ data }) => {
        this.typeList = data
      })
      this.dialogFormVisible = true
    },
    handleUpdate(row) {
      this.resetTemp()
      this.dialogStatus = this.$t('index.operations.redactAccount')
      this.temp = Object.assign({}, row)
      const id = this.temp.id
      getAccountById(id).then(({ data }) => {
        this.temp = data
      })
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
          for (const account of data) {
            array.push(account.id)
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
          deleteAccount(array).then(result => {
            if (result.data === 'success') {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
            } else {
              const list1 = result.data.slice(0, 5)
              const list2 =
                result.data.length > 10
                  ? result.data.slice(5, 10) + '...'
                  : result.data.slice(5, 10)
              this.$message({
                duration: 0,
                showClose: true,
                dangerouslyUseHTMLString: true,
                message:
                  `<div>` +
                  result.msg +
                  `</div>` +
                  `<div>` +
                  list1 +
                  `</div>` +
                  `<div>` +
                  list2 +
                  `</div>`,
                type: 'error'
              })
            }
            this.getList()
          })
        })
        .catch(() => {})
    },
    async create() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          createAccount(this.temp).then(result => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success'
              })
              this.getList()
              this.dialogFormVisible = false
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        } else {
          this.errorTips()
        }
      })
    },
    async update() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          updateAccount(this.temp).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
              this.getList()
              this.dialogFormVisible = false
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        } else {
          this.errorTips()
        }
      })
    },
    errorTips() {
      this.$message({
        message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
        type: 'error'
      })
    },
    download() {
      downloadAccounts().then(data => {
        const blob = new Blob([data], { type: 'application/xlsx' })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a') // 创建a标签
        link.href = url
        link.download = 'account.xlsx' // 重命名文件
        link.click()
        URL.revokeObjectURL(url)
      })
    },
    closeDialog() {
      this.dialogFormVisible = false
    },
    closeDetailDialog() {
      this.detailDialogFormVisible = false
    },
    handleSelection(val) {
      this.multipleSelection = val
    },
    getDetails(row) {
      this.temp = Object.assign({}, row)
      getAccountById(this.temp.id).then(({ data }) => {
        this.temp = data
        this.detailDialogFormVisible = true
      })
    },
    typeCurrentChange(data, node) {
      this.temp.typeId = data.id
      this.temp.typeName = data.name
      this.$refs.typeListPopover.doClose()
    },
    getList() {
      getAccountList(this.listQuery).then(({ data }) => {
        this.list = data.records
        this.total = data.total
        this.listLoading = false
      })
    },
    getAccountTypeList() {
      getAccountTypeList().then(({ data }) => {
        this.accountTypeOptions = data
      })
    },
    resuming(row) {
      this.temp = Object.assign({}, row)
      this.temp.status = 0
      updateAccount(this.temp).then(result => {
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
    handleFilter() {
      this.listQuery.page = 1
      this.listQuery.account = this.listQueryHelper.account
      this.listQuery.typeId = this.listQueryHelper.typeId
      this.listQuery.alias = this.listQueryHelper.alias
      this.getList()
    },
    resetTemp() {
      const temp = this.temp
      for (const key in temp) {
        this.temp[key] = ''
      }
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
