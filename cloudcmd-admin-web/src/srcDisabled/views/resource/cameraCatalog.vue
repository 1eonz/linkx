<template>
  <div class="app-container">
    <div class="menuBody">
      <el-card shadow="always" class="card">
        <aside-tree
          :tree-data="dataList"
          :default-expand-all="false"
          :append-top-add-visible="hasPerm('/admin/facilityCatalog/create')"
          :append-add-visible="hasPerm('/admin/facilityCatalog/create')"
          :append-edit-visible="hasPerm('/admin/facilityCatalog/update')"
          :append-delete-visible="hasPerm('/admin/facilityCatalog/delete')"
          :append-resume-visible="hasPerm('/admin/facilityCatalog/update')"
          :append-manage-organization-visible="true"
          @clickEdit="handleUpdate"
          @moveNode="moveNode"
          @clickRemove="handleDelete"
          @clickResume="resuming"
          @showDetail="getDetails"
          @clickAdd="handleCreate"
          @clickTopAdd="clickTopAdd"
          @clickManageOrganization="handleManageOrg"
        />
      </el-card>
    </div>

    <el-dialog
      :visible.sync="detailDialogFormVisible"
      :title="$t('index.list.cameraLevel') + $t('index.operations.particulars')"
      @close="closeDetailDialog"
    >
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.cameraLevelSerial') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.code }}</span>
          </template>
        </el-col>
      </el-row>
      <el-row>
        <el-col :span="8">
          <template>
            <span>{{ $t('index.list.cameraLevelName') }}</span>
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
            <span>{{ $t('index.list.cameraCameraLevel') }}</span>
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
            <span>{{ $t('index.list.cameraFullName') }}</span>
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
            <span>{{ $t('index.list.upperLevelName') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.parentName }}</span>
          </template>
        </el-col>
      </el-row>
      <!-- <el-row>
        <el-col :span="8">
          <template><span>是否收藏</span></template>
        </el-col>
        <el-col :span="12">
          <template><span style="color: blue">{{ temp.isFavorite === 1 ? '收藏' : '不收藏' }}</span></template>
        </el-col>
      </el-row> -->
      <el-row v-if="temp.creatorId !== '0' && temp.creatorId !== undefined">
        <el-col :span="8">
          <template>
            <span>{{ $t('index.creator') }}</span>
          </template>
        </el-col>
        <el-col :span="12">
          <template>
            <span style="color: blue">{{ temp.creatorId }}</span>
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
        label-width="220px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.list.cameraLevelName')" prop="name">
          <el-input v-model.trim="temp.name" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="
            `${$t('index.list.cameraLevelSerial')}(${$t('index.list.only')})`
          "
          prop="code"
        >
          <el-input v-model.trim="temp.code" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.cameraCameraLevel')"
          prop="shortName"
        >
          <el-input v-model.trim="temp.shortName" class="edit-input" />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.cameraLevelIntroduction')"
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
          v-if="dialogStatus === $t('index.operations.newCameraLevel')"
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

    <!-- 组织树 -->
    <camera-organization ref="org" />
  </div>
</template>

<script>
import AsideTree from '../common/asideTree'
import CameraOrganization from './components/cameraOrganization'
import {
  createCameraCatalog,
  deleteCameraCatalog,
  getCameraCatalogById,
  getCameraCatalogChildren,
  getCameraCatalogList,
  moveCameraCatalogNode,
  updateCameraCatalog
} from '@/api/facility/cameraCatalog'
export default {
  name: 'CameraCatalog',
  components: { AsideTree, CameraOrganization },
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
      dataList: [],
      parentList: [],
      dataListLoading: false,
      dialogFormVisible: false,
      detailDialogFormVisible: false,
      dialogStatus: this.$t('index.operations.redact'),
      listTreeProps: {
        label: 'name',
        children: 'children'
      },
      rules: {
        name: [
          {
            required: true,
            message: this.$t('index.messageText.hierarchyNameCannotBeEmpty'),
            trigger: 'blur'
          }
        ],
        code: [
          {
            required: true,
            message: this.$t('index.messageText.levelNumberCannotBeEmpty'),
            trigger: 'blur'
          },
          { validator: validateIsNum, trigger: 'blur' }
        ],
        shortName: [
          {
            required: true,
            message: this.$t(
              'index.messageText.hierarchyAbbreviationCannotBeEmpty'
            ),
            trigger: 'blur'
          }
        ]
      },
      temp: {
        id: '',
        name: '',
        code: '',
        parentId: 0,
        parentName: '',
        remark: '',
        level: '',
        sort: '',
        status: '',
        category: 502001,
        isPublic: 0,
        creatorId: '',
        isFavorite: 0,
        fullPath: '',
        fullPathName: '',
        shortName: '',
        gmtCreated: '',
        gmtModified: ''
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
  },
  activated() {
    this.getDataList()
  },
  methods: {
    // 获取数据列表
    getDataList() {
      const params = {
        category: this.temp.category,
        status: 1
      }
      getCameraCatalogList(params).then(({ data }) => {
        this.dataList = data
        this.dataListLoading = false
      })
    },
    async handleCreate(node, data) {
      this.resetTemp()
      this.temp.parentName = data.name
      this.temp.parentId = data.id
      this.dialogStatus = this.$t('index.operations.newCameraLevel')
      let sort = 1
      if (data.children !== null) {
        const id = data.id
        const response = await getCameraCatalogChildren(id)
        const children = response.data
        if (children.length > 0) {
          sort = Math.max.apply(Math, children.map(o => o.sort))
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
      getCameraCatalogById(id).then(({ data }) => {
        this.temp = data
        this.temp.id = id
        this.dialogStatus = this.$t('index.operations.redact')
        this.dialogFormVisible = true
      })
    },
    moveNode(node, targetNode, location, event) {
      this.nodeTemp.currentId = node.data.id
      this.nodeTemp.currentParentId = node.data.parentId
      this.nodeTemp.currentSort = node.data.sort
      this.nodeTemp.targetId = targetNode.data.id
      this.nodeTemp.targetParentId = targetNode.data.parentId
      this.nodeTemp.targetSort = targetNode.data.sort
      this.nodeTemp.targetLevel = targetNode.data.level
      this.nodeTemp.location = location
      moveCameraCatalogNode(this.nodeTemp).then(({ data }) => {
        this.getDataList()
      })
    },
    handleDelete(node, data) {
      this.resetTemp()
      this.temp = Object.assign({}, data)
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          deleteCameraCatalog(this.temp.id).then(result => {
            if (result.data === 'success') {
              this.$message({
                message: this.$t('index.statusTitle.successfullyDelete'),
                type: 'success'
              })
              this.eventBus.$emit('clearFilterText')
              this.getDataList()
            } else {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
          })
        })
        .catch(() => {})
    },
    getDetails(row) {
      this.temp = Object.assign({}, row)
      getCameraCatalogById(this.temp.id).then(({ data }) => {
        this.temp = data
        this.detailDialogFormVisible = true
      })
    },
    async create() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          createCameraCatalog(this.temp).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.createSuccess'),
                type: 'success'
              })
              this.eventBus.$emit('clearFilterText')
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
          this.temp.fullPathName = ''
          updateCameraCatalog(this.temp).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
              this.eventBus.$emit('clearFilterText')
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
    async clickTopAdd(node, data) {
      this.resetTemp()
      this.temp.parentId = -1
      this.temp.level = 1
      let sort = 1
      const response = await getCameraCatalogChildren(-1)
      const children = response.data
      if (children.length > 0) {
        sort = Math.max.apply(Math, children.map(o => o.sort))
      }
      this.dialogStatus = this.$t('index.operations.newCameraLevel')
      this.temp.sort = sort + 1
      this.dialogFormVisible = true
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
    resuming(node, data) {
      this.temp = Object.assign({}, data)
      this.temp.status = 0
      this.$confirm(this.$t('index.messageText.restoreItAndItsSublevels'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(() => {
        this.temp.fullPathName = ''
        updateCameraCatalog(this.temp).then(result => {
          if (result.code === 0) {
            this.$message({
              message: this.$t('index.statusTitle.restoreSuccess'),
              type: 'success'
            })
            this.eventBus.$emit('clearFilterText')
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

    handleManageOrg(node) {
      this.$refs.org.setData(node.data.id)
    },
    resetTemp() {
      this.temp.id = ''
      this.temp.name = ''
      this.temp.code = ''
      this.temp.parentId = 0
      this.temp.parentName = ''
      this.temp.isPublic = 0
      this.temp.creatorId = ''
      this.temp.isFavorite = 0
      this.temp.gmtCreated = ''
      this.temp.gmtModified = ''
      this.temp.remark = ''
      this.temp.fullPathName = ''
      this.temp.typeName = ''
      this.temp.shortName = ''
      this.temp.level = ''
      this.temp.sort = ''
      this.temp.status = ''
      this.temp.fullPath = ''
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
  width: 300px;
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
