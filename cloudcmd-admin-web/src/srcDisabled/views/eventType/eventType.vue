<template>
  <div class="app-container">
    <el-form :inline="true">
      <el-form-item>
        <el-button
          v-if="hasPerm('/admin/eventType/create')"
          type="primary"
          @click="handleCreate()"
        >
          {{ $t('index.operations.Added') }}
        </el-button>
      </el-form-item>
    </el-form>
    <el-table :data="dataList" row-key="id" border style="width: 100%;">
      <el-table-column
        prop="name"
        header-align="center"
        min-width="120"
        :label="$t('index.list.alarmTypeName')"
      />
      <el-table-column
        prop="category"
        header-align="center"
        align="center"
        min-width="120"
        :label="$t('index.list.alarmTypeSerial')"
      />
      <el-table-column
        fixed="right"
        header-align="center"
        align="center"
        min-width="120"
        :label="$t('index.operations.operation')"
      >
        <template slot-scope="scope">
          <el-button
            v-if="hasPerm('/admin/eventType/update')"
            type="primary"
            icon="el-icon-document"
            size="small"
            @click="handleUpdate(scope.row)"
          >
            {{ $t('index.operations.change') }}
          </el-button>
          <el-button
            v-if="hasPerm('/admin/eventType/delete')"
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

    <el-dialog
      :visible="dialogFormVisible"
      :title="dialogStatus"
      :before-close="closeDialog"
    >
      <el-form
        ref="tempForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="200px"
        style="width: 600px; margin-left:60px"
      >
        <el-form-item :label="$t('index.list.alarmTypeName')" prop="name">
          <el-input v-model="form.name" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.list.alarmTypeSerial')" prop="category">
          <el-input v-model="form.category" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.alarmTypeDescription')"
          prop="remark"
        >
          <el-input v-model="form.remark" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.superiorAlarmTypeName')"
          prop="parentName"
        >
          <el-popover
            ref="listPopover"
            placement="bottom-start"
            trigger="click"
          >
            <el-tree
              ref="ListTree"
              class="tree-style"
              :data="dataList"
              :props="listTreeProps"
              node-key="id"
              :default-expand-all="true"
              :highlight-current="true"
              :expand-on-click-node="false"
              @current-change="parentCurrentChange"
            />
          </el-popover>
          <el-input
            v-model="form.parentName"
            v-popover:listPopover
            :placeholder="$t('index.list.selectSuperiorAlarmTypeLevel')"
            clearable
            :readonly="true"
            class="edit-input"
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="closeDialog">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="
            dialogStatus === this.$t('index.list.newSuperiorAlarmTypeLevel')
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
  createEventType,
  deleteEventType,
  getEventTypeById,
  getEventTypeList,
  updateEventType
} from '@/api/eventType/eventType'
import { deepCopy } from '@/utils'

const form = {
  id: '',
  name: '',
  category: '',
  parentId: -1,
  parentName: '',
  parentCategory: '',
  remark: '',
  fullPathName: ''
}

export default {
  name: 'EventType',
  data() {
    const validateName = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('index.list.alarmTypeNameCannotNull')))
      } else {
        callback()
      }
    }
    const validateCategory = (rule, value, callback) => {
      if (value === '') {
        callback(new Error(this.$t('index.list.alarmTypeSerialCannotNull')))
      } else if (parseFloat(value).toString() === 'NaN') {
        callback(new Error(this.$t('index.list.alarmTypeSerialOnlyNumber')))
      } else {
        callback()
      }
    }
    return {
      dataList: [],
      parentList: [],
      dataListLoading: false,
      dialogFormVisible: false,
      dialogStatus: this.$t('index.operations.redact'),
      rules: {
        name: [
          {
            required: true,
            message: this.$t('index.list.alarmTypeSerialCannotNull'),
            trigger: 'blur'
          },
          { validator: validateName, trigger: 'blur' }
        ],
        category: [
          {
            required: true,
            message: this.$t('index.list.alarmTypeSerialCannotNull'),
            trigger: 'blur'
          },
          { validator: validateCategory, trigger: 'blur' }
        ]
      },
      listTreeProps: {
        label: 'name',
        children: 'children'
      },
      form: deepCopy(form)
    }
  },
  created() {
    this.getDataList()
  },
  activated() {
    this.getDataList()
  },
  methods: {
    // 获取数据列表
    getDataList() {
      getEventTypeList().then(({ data }) => {
        this.dataList = data
        this.dataListLoading = false
      })
    },
    handleCreate() {
      this.dialogStatus = this.$t('index.list.newSuperiorAlarmTypeLevel')
      this.dialogFormVisible = true
    },
    handleUpdate({ id }) {
      getEventTypeById(id).then(({ data }) => {
        this.form = data
        this.form.id = id
        this.dialogStatus = this.$t('index.operations.redact')
        this.dialogFormVisible = true
      })
    },
    handleDelete({ id }) {
      this.$confirm(
        this.$t('index.operations.affirmDeleted') +
          this.$t('index.operations.affirmDeletedIncludesChildrenType'),
        {
          confirmButtonText: this.$t('index.determine'),
          cancelButtonText: this.$t('index.cancel'),
          type: 'info'
        }
      )
        .then(() => {
          deleteEventType(id).then(result => {
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
            this.getDataList()
          })
        })
        .catch(() => {})
    },
    async create() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          createEventType(this.form)
            .then(res => {
              if (res.code === 0) {
                this.$message({
                  message: this.$t('index.statusTitle.createSuccess'),
                  type: 'success'
                })
                this.getDataList()
                this.closeDialog()
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
          updateEventType(this.form)
            .then(res => {
              if (res.code === 0) {
                this.$message({
                  message: this.$t('index.statusTitle.changeSuccess'),
                  type: 'success'
                })
                this.getDataList()
                this.closeDialog()
              } else {
                this.$message({
                  message: this.$t('index.statusTitle.changeFail'),
                  type: 'error'
                })
              }
            })
            .catch(() => {})
        }
      })
    },
    closeDialog() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.dialogFormVisible = false
    },
    closeDetailDialog() {
      this.detailDialogFormVisible = false
    },
    parentCurrentChange(data, node) {
      if (this.form.id === data.id) {
        this.$message({
          message: this.$t('index.list.superiorCannotOneself'),
          type: 'warning'
        })
        return
      }
      this.form.parentId = data.id
      this.form.parentName = data.name
      this.form.parentCategory = data.category
      this.$refs.listPopover.doClose()
    }
  }
}
</script>

<style scoped>
.edit-input {
  padding-right: 50px;
  width: 400px;
}
.tree-style {
  width: 320px;
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
.el-radio-group {
  width: 200px;
}
</style>
