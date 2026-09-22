<template>
  <el-card class="app-container">
    <el-tabs v-model="activeTab" type="border-card" class="menuBody">
      <el-tab-pane
        v-for="item in applicationOptions"
        :key="item.id"
        :label="item.name"
        :name="item.id"
      >
        <el-container style="height: 100%">
          <el-aside style="height:700px;overflow-y:auto;">
            <br />
            <br />
            <aside-tree
              :draggable="false"
              :tree-data="treeData"
              :top-add-visible="false"
              @showDetail="showDetail"
            />
          </el-aside>
          <div
            style="margin-top: 10px;margin-bottom: 10px;width: 1px;background-color: #99a9bf"
          ></div>
          <el-main style="height:700px;overflow-y:auto;">
            <el-button
              v-if="
                actionParam.menuId !== '' && hasPerm('/admin/action/create')
              "
              type="primary"
              style="margin-left: 10px"
              icon="el-icon-circle-plus-outline"
              size="small"
              @click="handleCreate"
            >
              {{ $t('index.operations.Added') }}
            </el-button>
            <br />
            <br />
            <el-table
              v-if="actionList.length > 0"
              v-loading="listLoading"
              :data="actionList"
              stripe
              border
              fit
              highlight-current-row
              style="width: 100%;"
            >
              <el-table-column
                :label="$t('index.list.functionName')"
                :show-overflow-tooltip="true"
                min-width="80"
                align="center"
              >
                <template slot-scope="scope">
                  <span>{{ scope.row.name }}</span>
                </template>
              </el-table-column>
              <el-table-column
                :label="$t('index.authority.permissionToIdentify')"
                :show-overflow-tooltip="true"
                min-width="80"
                align="center"
              >
                <template slot-scope="scope">
                  <span>{{ scope.row.action }}</span>
                </template>
              </el-table-column>
              <el-table-column
                :label="$t('index.list.condition')"
                class-name="status-col"
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
                class-name="small-padding fixed-width"
                header-align="center"
                align="center"
                min-width="160"
                :label="$t('index.operations.operation')"
              >
                <template slot-scope="{ row }">
                  <el-button
                    v-if="hasPerm('/admin/action/update')"
                    type="primary"
                    icon="el-icon-edit"
                    size="small"
                    @click="handleUpdate(row)"
                  >
                    {{ $t('index.operations.redact') }}
                  </el-button>
                  <el-button
                    v-if="hasPerm('/admin/action/delete')"
                    type="danger"
                    icon="el-icon-delete"
                    size="small"
                    @click="handleDelete(row)"
                  >
                    {{ $t('index.delete') }}
                  </el-button>
                  <el-button
                    v-if="row.status === 0 && hasPerm('/admin/action/update')"
                    type="danger"
                    icon="el-icon-goods"
                    size="small"
                    @click="clickWarn(row)"
                  >
                    {{ $t('index.list.forbidden') }}
                  </el-button>
                  <el-button
                    v-else-if="
                      row.status !== 0 && hasPerm('/admin/action/update')
                    "
                    type="warning"
                    icon="el-icon-star-on"
                    size="small"
                    @click="clickResume(row)"
                  >
                    {{ $t('index.operations.enabled') }}
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-main>
        </el-container>
      </el-tab-pane>
    </el-tabs>

    <el-dialog
      :visible.sync="dialogFormVisible"
      :title="dialogStatus"
      width="650px"
      custom-class="adaptive-dialog"
      @close="closeDialog"
    >
      <el-form
        ref="tempForm"
        :model="temp"
        :rules="rules"
        label-position="left"
        label-width="250px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.list.functionName')" prop="name">
          <el-input v-model.trim="temp.name" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.functionIdentificationPlate')"
          prop="action"
        >
          <el-input v-model.trim="temp.action" class="edit-input" />
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
          v-if="dialogStatus === $t('index.operations.newFeature')"
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
  </el-card>
</template>

<script>
import AsideTree from '../common/asideTree'
import { getMenuList } from '@/api/permission/menu'
import { getApplicationList } from '@/api/permission/application'
import {
  createAction,
  deleteAction,
  getActionList,
  updateAction
} from '@/api/permission/action'

export default {
  name: 'Menu',
  components: { AsideTree },
  filters: {
    statusFilter(status) {
      const statusMap = {
        0: 'success',
        1: 'danger'
      }
      return statusMap[status]
    }
  },
  data() {
    return {
      listLoading: false,
      actionList: [],
      treeData: [],
      parDataList: [],
      listTreeProps: {
        label: 'name',
        children: 'children'
      },
      menuParam: {
        applicationId: '',
        applicationName: '',
        roleType: ''
      },
      rules: {
        name: [
          {
            required: true,
            message: this.$t('index.list.theFunctionNameCannotBeEmpty'),
            trigger: 'blur'
          }
        ],
        action: [
          {
            required: true,
            message: this.$t('index.list.theFunctionIdentifierCannotBeEmpty'),
            trigger: 'blur'
          }
        ]
      },
      actionParam: {
        applicationId: '',
        menuId: ''
      },
      activeTab: '',
      applicationOptions: [],
      dialogStatus: this.$t('index.operations.newFeature'),
      dialogFormVisible: false,
      temp: {
        id: '',
        name: '',
        action: '',
        menuId: '',
        applicationId: '',
        remark: '',
        status: '',
        gmtCreated: '',
        gmtModified: ''
      }
    }
  },
  watch: {
    activeTab(val) {
      this.actionParam.menuId = ''
      this.getMenuTree()
    }
  },
  created() {
    this.getApplicationOptions()
  },
  activated() {
    this.getMenuTree()
  },
  methods: {
    getMenuTree() {
      this.menuParam.applicationId = this.activeTab
      getMenuList(this.menuParam).then(({ data }) => {
        this.treeData = this.filterData(data)
        if (this.treeData.length > 0) {
          this.actionParam.applicationId = this.activeTab
          if (this.actionParam.menuId === '') {
            this.actionParam.menuId = this.treeData[0].id
          }
          getActionList(this.actionParam).then(({ data }) => {
            this.actionList = data
          })
        } else {
          this.actionList = []
          this.actionParam.menuId = ''
          this.actionParam.applicationId = ''
        }
      })
    },
    filterData(arr) {
      const newArr = arr.filter(item => item.status !== 2)
      return newArr.map(item => {
        if (item.children) {
          item.children = this.filterData(item.children)
        }
        return item
      })
    },
    getApplicationOptions() {
      getApplicationList().then(({ data }) => {
        this.applicationOptions = data
        if (this.applicationOptions.length > 0) {
          this.menuParam.applicationId = this.applicationOptions[0].id
          this.activeTab = this.applicationOptions[0].id
          this.menuParam.applicationName = this.applicationOptions[0].name
        }
        this.getMenuTree()
      })
    },
    handleCreate() {
      this.resetTemp()
      this.dialogStatus = this.$t('index.operations.newFeature')
      this.temp.applicationId = this.actionParam.applicationId
      this.temp.menuId = this.actionParam.menuId
      this.dialogFormVisible = true
    },
    create() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          createAction(this.temp).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.createSuccess'),
                type: 'success'
              })
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.getMenuTree()
            this.dialogFormVisible = false
          })
        } else {
          this.$message({
            message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
            type: 'warning'
          })
        }
      })
    },
    showDetail(data) {
      this.actionParam.applicationId = this.activeTab
      this.actionParam.menuId = data.id
      getActionList(this.actionParam).then(({ data }) => {
        this.actionList = data
      })
    },
    handleUpdate(row) {
      this.resetTemp()
      this.temp = Object.assign({}, row)
      this.dialogStatus = this.$t('index.operations.featureUpdates')
      this.dialogFormVisible = true
    },
    update() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          updateAction(this.temp).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
              this.dialogFormVisible = false
              this.getMenuTree()
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
    handleDelete(row) {
      this.resetTemp()
      this.temp = Object.assign({}, row)
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(() => {
        const array = []
        array.push(this.temp.id)
        deleteAction(array).then(result => {
          if (result.code === 0) {
            this.$message({
              title: this.$t('index.statusTitle.succeed'),
              message: this.$t('index.statusTitle.successfullyDelete'),
              type: 'success'
            })
            this.getMenuTree()
          } else {
            this.$message({
              title: this.$t('index.statusTitle.fail'),
              type: 'danger',
              message: result.msg
            })
          }
        })
        this.getMenuTree()
      })
    },
    clickWarn(row) {
      this.temp = Object.assign({}, row)
      this.temp.status = 1
      updateAction(this.temp).then(result => {
        if (result.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.forbiddenSucceed'),
            type: 'success'
          })
          this.getMenuTree()
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
      })
    },
    clickResume(row) {
      this.temp = Object.assign({}, row)
      this.temp.status = 0
      updateAction(this.temp).then(result => {
        if (result.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.enabledFail'),
            type: 'success'
          })
          this.getMenuTree()
        } else {
          this.$message({
            message: result.msg,
            type: 'error'
          })
        }
      })
    },
    closeDialog() {
      this.dialogFormVisible = false
      this.$refs['tempForm'].clearValidate()
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

<style scoped lang="scss">
.app-container {
  padding: 10px;
  position: absolute;
  background-color: #ebeef5;
  height: 100%;
  width: 100%;
}

.menuBody {
  position: absolute;
  left: 20px;
  right: 20px;
  top: 20px;
  bottom: 20px;
}
.edit-input {
  padding-right: 50px;
  width: 400px;
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
:v-deep .mytree {
  .el-tree-node__content {
    .content {
      width: 200px;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
  }
}
</style>
