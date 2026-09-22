<template>
  <div class="app-container">
    <el-button
      v-if="hasPerm('/admin/extendInfoProperties/create')"
      type="primary"
      @click="handleCreate()"
    >
      {{ $t('index.operations.Added') }}
    </el-button>
    <br />
    <br />
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
        :label="$t('index.list.extensionNumber')"
        :show-overflow-tooltip="true"
        min-width="100"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.code }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.expandTheName')"
        :show-overflow-tooltip="true"
        min-width="100"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.name }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.extensionTags')"
        :show-overflow-tooltip="true"
        min-width="100"
        align="center"
      >
        <template slot-scope="scope">
          <span>{{ scope.row.label }}</span>
        </template>
      </el-table-column>
      <el-table-column
        :label="$t('index.list.condition')"
        class-name="status-col"
        min-width="100"
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
        min-width="120"
        :label="$t('index.operations.operation')"
      >
        <template slot-scope="scope">
          <el-button
            v-if="hasPerm('/admin/extendInfoProperties/update')"
            type="primary"
            icon="el-icon-edit"
            size="small"
            @click="handleUpdate(scope.row)"
          >
            {{ $t('index.operations.redact') }}
          </el-button>
          <el-button
            v-if="
              scope.row.status === 0 &&
                hasPerm('/admin/extendInfoProperties/delete')
            "
            type="danger"
            icon="el-icon-delete"
            size="small"
            @click="handleDelete(scope.row)"
          >
            {{ $t('index.delete') }}
          </el-button>
          <el-button
            v-else-if="
              scope.row.status !== 0 &&
                hasPerm('/admin/extendInfoProperties/update')
            "
            type="warning"
            icon="el-icon-star-on"
            size="small"
            @click="resuming(scope.row)"
          >
            {{ $t('index.operations.restore') }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog
      :visible.sync="dialogFormVisible"
      :destroy-on-close="true"
      :close-on-click-modal="false"
      :title="dialogStatus"
      @close="closeDialog"
    >
      <el-form
        ref="tempForm"
        :model="temp"
        :rules="rules"
        label-width="200px"
        label-position="left"
        style="width: 600px; margin-left:30px"
      >
        <el-form-item :label="$t('index.list.extensionNumber')" prop="code">
          <el-select
            v-model="temp.code"
            clearable
            class="filter-item"
            style="width: 300px;"
            :placeholder="$t('index.list.selectNumber')"
          >
            <el-option
              v-for="item in codeOptions"
              :key="item"
              :label="item"
              :value="item"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('index.list.expandTheName')" prop="name">
          <el-input v-model="temp.name" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.extensionTags')" prop="label">
          <el-input v-model="temp.label" class="edit-input" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="
            dialogStatus ===
              $t('index.operations.addedExtensionConfigurationItems')
          "
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
  createExtendInfoProperties,
  deleteExtendInfoProperties,
  getExtendInfoPropertiesList,
  updateExtendInfoProperties
} from '@/api/dictionary/extendInfoProperties'

export default {
  name: 'ExtendInfoProperties',
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
    const validateCode = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('index.list.extensionNumberCannotNull')))
      } else {
        callback()
      }
    }
    const validateName = (rule, value, callback) => {
      if (value === '') {
        callback(
          new Error(this.$t('index.messageText.extendNameCannotBeEmpty'))
        )
      } else {
        callback()
      }
    }
    const validateLabel = (rule, value, callback) => {
      if (value === '') {
        callback(
          new Error(this.$t('index.messageText.extensionLabelCannotBeEmpty'))
        )
      } else {
        callback()
      }
    }
    return {
      list: [],
      listLoading: false,
      dialogFormVisible: false,
      dialogStatus: '',
      typeCode: 300,
      rules: {
        code: [
          {
            required: true,
            message: this.$t('index.list.alarmTypeSerialCannotNull'),
            trigger: 'blur'
          },
          { validator: validateCode, trigger: 'blur' }
        ],
        name: [
          {
            required: true,
            message: this.$t('index.list.alarmTypeSerialCannotNull'),
            trigger: 'blur'
          },
          { validator: validateName, trigger: 'blur' }
        ],
        label: [
          {
            required: true,
            message: this.$t('index.list.alarmTypeSerialCannotNull'),
            trigger: 'blur'
          },
          { validator: validateLabel, trigger: 'blur' }
        ]
      },
      codeOptions: [
        'PERSON',
        'VEHICLE',
        'DEVICE',
        'SEAT',
        'CEMERA',
        'POLICEBOX',
        'HYDRANT'
      ],
      temp: {
        id: '',
        code: '',
        name: '',
        label: '',
        sort: '',
        status: 0
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      getExtendInfoPropertiesList().then(({ data }) => {
        this.temp.typeCode = this.typeCode
        this.list = data
        this.listLoading = false
      })
    },
    async create() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          createExtendInfoProperties(this.temp)
            .then(res => {
              if (res.code === 0) {
                this.$message({
                  message: this.$t('index.statusTitle.createSuccess'),
                  type: 'success'
                })
                this.dialogFormVisible = false
                this.getList()
              } else {
                this.$message({
                  message: this.$t('index.statusTitle.createFail'),
                  type: 'error'
                })
              }
            })
            .catch(() => {})
        }
      })
    },
    async update() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          updateExtendInfoProperties(this.temp)
            .then(res => {
              if (res.code === 0) {
                this.$message({
                  message: this.$t('index.statusTitle.createSuccess'),
                  type: 'success'
                })
                this.dialogFormVisible = false
                this.getList()
              } else {
                this.$message({
                  message: this.$t('index.statusTitle.createFail'),
                  type: 'error'
                })
              }
            })
            .catch(() => {})
        }
      })
    },
    closeDialog() {
      this.dialogFormVisible = false
    },
    handleCreate() {
      this.resetTemp()
      this.dialogStatus = this.$t(
        'index.operations.addedExtensionConfigurationItems'
      )
      this.dialogFormVisible = true
    },
    handleUpdate(row) {
      this.resetTemp()
      this.temp = Object.assign({}, row)
      this.dialogStatus = this.$t(
        'index.operations.editExtensionConfigurationItems'
      )
      this.dialogFormVisible = true
    },
    handleDelete(row) {
      this.resetTemp()
      this.temp = Object.assign({}, row)
      console.log(this.temp)
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteExtendInfoProperties(this.temp.id).then(result => {
            if (result.data === 'success') {
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
    resuming(row) {
      this.temp = Object.assign({}, row)
      console.log(this.temp)
      this.temp.status = 0
      updateExtendInfoProperties(this.temp).then(({ data }) => {
        if (data === 'success') {
          this.getList()
        } else {
          this.$message({
            message: this.$t('index.statusTitle.enabledSucceed'),
            type: 'error'
          })
        }
      })
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

<style scoped>
.edit-input {
  width: 300px;
}
</style>
