<template>
  <div class="app-container">
    <div class="menuBody">
      <el-card shadow="always" class="card">
        <aside-tree
          :tree-data="dataList"
          :append-add-visible="true"
          :append-edit-visible="true"
          :append-manage-organization-visible="true"
          :append-manage-executor-visible="true"
          @clickEdit="handleUpdate"
          @moveNode="moveNode"
          @showDetail="getDetails"
          @clickAdd="handleCreate"
          @clickTopAdd="clickTopAdd"
          @clickManageExecutor="manageExecutor"
          @clickManageOrganization="manageOrganization"
        />
      </el-card>
    </div>

    <el-dialog
      :visible.sync="detailDialogFormVisible"
      :title="$t('index.commandCenter') + $t('index.operations.particulars')"
      @close="closeDetailDialog"
    >
      <el-row>
        <el-col :span="8">
          <template>
            <span>
              {{ $t('index.commandCenter') + $t('index.list.name') }}
            </span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.name }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>
              {{ $t('index.commandCenter') + $t('index.list.shortName') }}
            </span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.shortName }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>
              {{ $t('index.commandCenter') + $t('index.list.fullName') }}
            </span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.fullPathName }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row v-if="temp.parentName !== '' && temp.parentName !== undefined">
        <el-col :span="8">
          <template>
            <span>{{ $t('index.superiorCommandCenter') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.parentName }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.organization') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">
              {{ temp.organizationName }}
            </span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ this.$t('index.createTime') }}</span>
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

    <el-dialog
      id="orgDialog"
      :visible.sync="dialogFormVisible"
      :title="dialogStatus"
      width="550px"
      custom-class="adaptive-dialog"
      @close="closeDialog"
    >
      <el-form
        ref="tempForm"
        :model="temp"
        label-position="left"
        label-width="200px"
        :rules="rules"
        class="dialog-form"
      >
        <el-form-item
          :label="`${$t('index.commandCenterName')($t('index.list.only'))}`"
          prop="name"
        >
          <el-input v-model.trim="temp.name" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="$t('index.commandCenterNameAbbreviation')"
          prop="shortName"
        >
          <el-input v-model.trim="temp.shortName" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.organization')"
          prop="organizationName"
        >
          <el-popover
            ref="orglistPopover"
            placement="bottom-start"
            trigger="click"
          >
            <el-tree
              ref="ListTree"
              class="tree-style"
              :data="orgList"
              :props="orglistTreeProps"
              node-key="id"
              :default-expand-all="true"
              :highlight-current="true"
              :expand-on-click-node="false"
              @current-change="orgCurrentChange"
            />
          </el-popover>
          <el-input
            v-model="temp.organizationName"
            v-popover:orglistPopover
            :readonly="true"
            class="edit-input"
            :placeholder="$t('index.list.addressType')"
          />
        </el-form-item>
        <el-form-item
          :label="$t('index.commandCenterNameSynopsis')"
          prop="remark"
        >
          <el-input v-model="temp.remark" class="edit-input" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogFormVisible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="dialogStatus === $t('index.newCommandCenter')"
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
      :visible.sync="commandCenterOrgFormVisible"
      :title="commandCenterOrgDialogTitle"
      @close="closeCommandCenterOrgDialog"
    >
      <div style="overflow-y: auto;height:360px">
        <el-tree
          ref="orgTree"
          :data="commandCenterOrgList"
          show-checkbox
          :default-checked-keys="checkedOrgIds"
          node-key="id"
          :default-expand-all="true"
          :props="orglistTreeProps"
        />
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="updateManageOrganization">
          {{ $t('index.operations.alter') }}
        </el-button>
        <el-button @click="closeCommandCenterOrgDialog()">
          {{ $t('index.operations.close') }}
        </el-button>
      </div>
    </el-dialog>
    <el-dialog
      :visible.sync="commandCenterExecutorFormVisible"
      :title="commandCenterExecutorDialogTitle"
      @close="closeCommandCenterExecutorDialog"
    >
      <div class="filter-container">
        <el-input
          v-model="listQueryHelper.name"
          :placeholder="$t('index.list.handName')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-input
          v-model="listQueryHelper.code"
          :placeholder="$t('index.list.serialNumber')"
          style="width: 200px;"
          class="filter-item"
          @keyup.enter.native="handleFilter"
        />
        <el-popover
          ref="orgListPopover"
          placement="bottom-start"
          trigger="click"
        >
          <el-tree
            ref="ListTree"
            class="tree-style"
            :data="orgList"
            :props="listTreeProps"
            node-key="id"
            :default-expand-all="true"
            :highlight-current="true"
            :expand-on-click-node="false"
            @current-change="organizationCurrentChange"
          />
        </el-popover>
        <el-input
          v-model="listQueryHelper.organizationName"
          v-popover:orgListPopover
          :readonly="true"
          style="width: 200px;"
          class="filter-item"
          :placeholder="$t('index.list.addressType')"
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
      </div>
      <br />
      <el-table
        ref="commandCenterExecutorTable"
        :data="commandCenterExecutorList"
        border
        style="width: 100%;"
        @selection-change="handleSelection"
      >
        <el-table-column type="selection" width="40" />
        <el-table-column
          prop="name"
          align="center"
          min-width="120"
          :label="$t('index.list.nameOfExecutor')"
        />
        <el-table-column
          prop="code"
          align="center"
          min-width="120"
          :label="$t('index.list.SerialOfExecutor')"
        />
        <el-table-column
          prop="typeName"
          align="center"
          min-width="120"
          :label="$t('index.list.typeOfExecutor')"
        />
        <el-table-column
          prop="organizationName"
          align="center"
          min-width="160"
          :label="$t('index.list.organization')"
        />
      </el-table>
      <pagination
        v-show="total > 0"
        :total="total"
        :page.sync="listQuery.page"
        :limit.sync="listQuery.limit"
        @pagination="getExecutorList"
      />
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="updateManageExecutor">
          {{ $t('index.operations.alter') }}
        </el-button>
        <el-button @click="closeCommandCenterExecutorDialog()">
          {{ $t('index.operations.close') }}
        </el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  createCommandCenter,
  deleteCommandCenter,
  getCommandCenterById,
  getCommandCenterList,
  updateCommandCenter,
  getOrganizationListByCommandCenterId,
  updateManageOrganizationList,
  updateManageExecutorList,
  getExecutorListByCommandCenterId,
  getCommandCenterChildren,
  moveCommandCenterNode
} from '@/api/resource/commandCenter'
import Pagination from '@/components/Pagination'
import AsideTree from '../common/asideTree'
import waves from '@/directive/waves'
import { getOrganizationList } from '@/api/resource/organization'
import { getPersonList } from '@/api/resource/person'
export default {
  name: 'CommandCenter',
  components: { Pagination, AsideTree },
  directives: { waves },
  data() {
    return {
      dataList: [],
      orgList: [],
      parentList: [],
      checkedOrgIds: [],
      checkedExecutorIds: [],
      dataListLoading: false,
      dialogFormVisible: false,
      detailDialogFormVisible: false,
      commandCenterExecutorFormVisible: false,
      commandCenterOrgFormVisible: false,
      dialogStatus: this.$t('index.operations.redact'),
      commandCenterOrgDialogTitle: this.$t('index.list.organizationManaged'),
      commandCenterExecutorDialogTitle: this.$t(
        'index.list.executorUnderManagement'
      ),
      listTreeProps: {
        label: 'name',
        children: 'children'
      },
      total: 0,
      orglistTreeProps: {
        label: 'name',
        children: 'children'
      },
      commandCenterOrgList: [],
      commandCenterExecutorList: [],
      tempAllCommandCenterExecutorIdList: [],
      listQueryHelper: {
        name: '',
        organizationId: '',
        organizationName: '',
        code: ''
      },
      listQuery: {
        name: '',
        code: '',
        organizationId: '',
        page: 1,
        limit: 5
      },
      rules: {
        name: [
          {
            required: true,
            message: this.$t('index.commandCenterNameCannotBeEmpty'),
            trigger: 'blur'
          }
        ],
        shortName: [
          {
            required: true,
            message: this.$t('index.commandCenterAbbreviationCannotBeEmpty'),
            trigger: 'blur'
          }
        ],
        organizationName: [
          {
            required: true,
            message: this.$t('index.list.organizationCannotNull'),
            trigger: 'change'
          }
        ]
      },
      temp: {
        id: '',
        name: '',
        parentId: 0,
        parentName: '',
        remark: '',
        fullPathName: '',
        shortName: '',
        gmtCreated: undefined,
        gmtModified: undefined,
        organizationName: '',
        organizationId: '',
        orgIds: [],
        executorIds: []
      },
      nodeTemp: {
        currentId: '',
        currentParentId: '',
        currentSort: '',
        targetId: '',
        targetParentId: '',
        targetSort: '',
        targetLevel: '',
        location: ''
      }
    }
  },
  created() {
    this.getDataList()
    this.getOrgList()
  },
  activated() {
    this.getDataList()
  },
  methods: {
    // 获取数据列表
    getDataList() {
      getCommandCenterList().then(({ data }) => {
        this.dataList = data
        this.dataListLoading = false
      })
    },
    getOrgList() {
      getOrganizationList().then(({ data }) => {
        this.orgList = data
      })
    },
    async handleCreate(node, data) {
      this.resetTemp()
      this.temp.parentName = data.name
      this.temp.parentId = data.id
      this.dialogStatus = this.$t('index.newCommandCenter')
      let sort = 1
      if (data.children !== null) {
        const id = data.id
        const response = await getCommandCenterChildren(id)
        const children = response.data
        if (children.length > 0) {
          sort = Math.max.apply(
            Math,
            children.map(function(o) {
              return o.sort
            })
          )
        }
        this.temp.sort = sort + 1
      } else {
        this.temp.sort = sort
      }
      this.dialogFormVisible = true
    },
    handleUpdate(node, data) {
      this.resetTemp()
      this.temp = Object.assign({}, data)
      const id = this.temp.id
      getCommandCenterById(id).then(({ data }) => {
        this.temp = data
        this.temp.id = id
        this.dialogStatus = this.$t('index.operations.redact')
        this.dialogFormVisible = true
      })
      getOrganizationList().then(({ data }) => {
        this.orgList = data
      })
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
          deleteCommandCenter(this.temp.id).then(result => {
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
    manageOrganization(node, data) {
      this.resetTemp()
      this.temp = Object.assign({}, data)
      getOrganizationListByCommandCenterId(this.temp.id).then(({ data }) => {
        const ids = []
        data.forEach(org => {
          ids.push(org.id)
        })
        this.checkedOrgIds = ids
      })
      getOrganizationList().then(({ data }) => {
        this.commandCenterOrgList = data
        this.commandCenterOrgFormVisible = true
      })
    },
    manageExecutor(node, data) {
      this.resetTemp()
      this.checkedExecutorIds = []
      this.temp = Object.assign({}, data)
      getOrganizationList().then(({ data }) => {
        this.orgList = data
      })
      getExecutorListByCommandCenterId(this.temp.id).then(({ data }) => {
        const ids = []
        data.forEach(executor => {
          ids.push(executor.id)
        })
        this.checkedExecutorIds = ids
        // 得到所有的执行者
        this.getExecutorList()
        this.commandCenterExecutorFormVisible = true
      })
    },
    handleFilter() {
      this.listQuery.page = 1
      this.listQuery.name = this.listQueryHelper.name
      this.listQuery.code = this.listQueryHelper.code
      this.listQuery.organizationId = this.listQueryHelper.organizationId
      console.log(this.listQuery)
      this.getExecutorList()
    },
    getExecutorList() {
      getPersonList(this.listQuery).then(({ data }) => {
        this.commandCenterExecutorList = data.records
        this.tempAllCommandCenterExecutorIdList = []
        this.$nextTick(() => {
          this.commandCenterExecutorList.forEach(executor => {
            this.tempAllCommandCenterExecutorIdList.push(executor.id)
            if (this.checkedExecutorIds.includes(executor.id)) {
              this.$refs.commandCenterExecutorTable.toggleRowSelection(
                executor,
                true
              )
            }
          })
        })
        this.total = data.total
      })
    },
    organizationCurrentChange(data, node) {
      this.listQueryHelper.organizationId = data.id
      this.listQueryHelper.organizationName = data.name
      this.$refs.orgListPopover.doClose()
    },
    getDetails(data) {
      this.temp = Object.assign({}, data)
      getCommandCenterById(this.temp.id).then(({ data }) => {
        this.temp = data
        this.detailDialogFormVisible = true
      })
    },
    moveNode(node, targetNode, location, event) {
      console.log(node)
      this.nodeTemp.currentId = node.data.id
      this.nodeTemp.currentParentId = node.data.parentId
      this.nodeTemp.currentSort = node.data.sort
      this.nodeTemp.targetId = targetNode.data.id
      this.nodeTemp.targetParentId = targetNode.data.parentId
      this.nodeTemp.targetSort = targetNode.data.sort
      this.nodeTemp.targetLevel = targetNode.data.level
      this.nodeTemp.location = location
      console.log(this.nodeTemp)
      moveCommandCenterNode(this.nodeTemp).then(({ data }) => {
        this.getOrganizationTree()
      })
    },
    async clickTopAdd(node, data) {
      this.resetTemp()
      this.temp.parentId = -1
      this.temp.level = 1
      let sort = 1
      const response = await getCommandCenterChildren(-1)
      const children = response.data
      if (children.length > 0) {
        sort = Math.max.apply(
          Math,
          children.map(function(o) {
            return o.sort
          })
        )
      }
      this.dialogStatus = this.$t('index.newCommandCenter')
      this.temp.sort = sort + 1
      this.dialogFormVisible = true
    },
    async create() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          createCommandCenter(this.temp).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.createSuccess'),
                type: 'success'
              })
              this.getDataList()
              this.dialogFormVisible = false
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
            type: 'error'
          })
        }
      })
    },
    async update() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          updateCommandCenter(this.temp).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
              this.getDataList()
              this.dialogFormVisible = false
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
            type: 'error'
          })
        }
      })
    },
    closeDialog() {
      this.dialogFormVisible = false
    },
    closeDetailDialog() {
      this.detailDialogFormVisible = false
    },
    closeCommandCenterExecutorDialog() {
      this.commandCenterExecutorFormVisible = false
    },
    closeCommandCenterOrgDialog() {
      this.commandCenterOrgFormVisible = false
    },
    parentCurrentChange(data, node) {
      if (this.temp.id === data.id) {
        this.$message({
          message: this.$t('index.list.superiorCannotOneself'),
          type: 'warning'
        })
        return
      }
      this.temp.parentId = data.id
      this.temp.parentName = data.name
      this.$refs.listPopover.doClose()
    },
    orgCurrentChange(data, node) {
      this.temp.organizationId = data.id
      this.temp.organizationName = data.name
      this.$refs.orglistPopover.doClose()
    },
    handleSelection(val) {
      const curCheckedIdList = []
      if (val.length > 0) {
        val.forEach(executor => {
          curCheckedIdList.push(executor.id)
          if (this.checkedExecutorIds.indexOf(executor.id) === -1) {
            this.checkedExecutorIds.push(executor.id)
          }
        })
      }
      this.tempAllCommandCenterExecutorIdList.forEach(exeId => {
        if (
          curCheckedIdList.indexOf(exeId) === -1 &&
          this.checkedExecutorIds.indexOf(exeId) !== -1
        ) {
          const index = this.checkedExecutorIds.indexOf(exeId)
          this.checkedExecutorIds.splice(index, 1)
        }
        if (
          curCheckedIdList.indexOf(exeId) !== -1 &&
          this.checkedExecutorIds.indexOf(exeId) === -1
        ) {
          this.checkedExecutorIds.push(exeId)
        }
      })
    },
    updateManageOrganization() {
      this.temp.orgIds = this.$refs.orgTree.getCheckedKeys()
      delete this.temp.children
      updateManageOrganizationList(this.temp).then(({ data }) => {
        if (data === 'success') {
          this.$message({
            message: this.$t('index.statusTitle.changeSuccess'),
            type: 'success'
          })
        } else {
          this.$message({
            message: this.$t('index.statusTitle.changeFail'),
            type: 'error'
          })
        }
        this.commandCenterOrgFormVisible = false
      })
    },
    updateManageExecutor() {
      if (this.checkedExecutorIds.length > 0) {
        this.temp.executorIds = this.checkedExecutorIds
        updateManageExecutorList(this.temp).then(({ data }) => {
          if (data === 'success') {
            this.$message({
              message: this.$t('index.statusTitle.changeSuccess'),
              type: 'success'
            })
          } else {
            this.$message({
              message: this.$t('index.statusTitle.changeFail'),
              type: 'error'
            })
          }
          this.commandCenterExecutorFormVisible = false
        })
      } else {
        console.error(this.$t('index.messageText.transmissionTypeError'))
      }
    },
    resuming(row) {
      this.temp = Object.assign({}, row)
      this.temp.status = 0
      this.$confirm(this.$t('index.RestoreItAndItsSubCommandCenter'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(() => {
        updateCommandCenter(this.temp).then(({ data }) => {
          if (data === 'success') {
            this.$message({
              message: this.$t('index.statusTitle.restoreSuccess'),
              type: 'success'
            })
            this.getDataList()
          } else {
            this.$message({
              message: this.$t('index.statusTitle.restoreFailure'),
              type: 'error'
            })
          }
        })
      })
    },
    resetTemp() {
      this.temp.id = ''
      this.temp.name = ''
      this.temp.parentId = 0
      this.temp.parentName = ''
      this.temp.gmtCreated = undefined
      this.temp.gmtModified = undefined
      this.temp.remark = ''
      this.temp.fullPathName = ''
      this.temp.organizationName = ''
      this.temp.shortName = ''
      this.temp.orgIds = []
    }
  }
}
</script>

<style scoped>
.app-container {
  padding: 20px;
  position: absolute;
  background-color: #ebeef5;
  height: 100%;
  width: 100%;
  overflow-y: auto;
}

.card {
  height: 100%;
  overflow-y: auto;
  border-color: black;
  border-width: 1px;
}

.cardInner {
  position: absolute;
  left: 18px;
  right: 18px;
  top: 25px;
  bottom: 25px;
}

.menuBody {
  position: absolute;
  left: 20px;
  right: 20px;
  top: 20px;
  bottom: 20px;
  background-color: #ebeef5;
}
.edit-input {
  padding-right: 50px;
  width: 250px;
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
.el-radio-group {
  width: 200px;
}
</style>
