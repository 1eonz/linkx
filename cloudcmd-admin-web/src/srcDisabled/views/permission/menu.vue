<template>
  <el-card class="app-container">
    <el-tabs v-model="activeTab" type="border-card" class="menuBody">
      <el-tab-pane
        v-for="item in applicationOptions"
        :key="item.id"
        :label="item.name"
        :name="item.id"
        style="height:700px;overflow-y:auto;"
      >
        <aside-tree
          :tree-data="treeData"
          :append-top-add-visible="hasPerm('/admin/menu/create')"
          :append-add-visible="hasPerm('/admin/menu/create')"
          :append-phy-delete-visible="hasPerm('/admin/menu/delete')"
          :append-edit-visible="hasPerm('/admin/menu/update')"
          :append-resume-visible="hasPerm('/admin/menu/update')"
          :append-warn-visible="hasPerm('/admin/menu/update')"
          @moveNode="moveNode"
          @showDetail="showDetail"
          @clickRemove="clickRemove"
          @clickResume="(node, data) => handleStatus(data, 0)"
          @clickWarn="(node, data) => handleStatus(data, 2)"
          @clickAdd="clickAdd"
          @clickEdit="handleUpdate"
          @clickTopAdd="clickTopAdd(item)"
        />
      </el-tab-pane>
    </el-tabs>

    <!-- 详情 -->
    <menu-details ref="details" />

    <!-- 添加 -->
    <el-dialog
      :visible.sync="dialogFormVisible"
      :title="dialogStatus"
      :before-close="beforeClose"
      width="550px"
      custom-class="adaptive-dialog"
    >
      <el-form
        ref="tempForm"
        :model="temp"
        :rules="rules"
        label-position="left"
        label-width="200px"
        class="dialog-form"
      >
        <el-form-item :label="$t('index.menuBar.menuLevelName')" prop="name">
          <el-input v-model.trim="temp.name" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.menuBar.menuLinks')">
          <el-input v-model="temp.url" class="edit-input" />
        </el-form-item>
        <el-form-item :label="$t('index.menuBar.menuPictureAddress')">
          <el-input v-model="temp.imgurl" class="edit-input" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="beforeClose">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="dialogStatus === $t('index.operations.newMenu')"
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
import asideTree from '../common/asideTree'
import {
  createMenu,
  deleteMenu,
  getMenuById,
  getMenuChildren,
  getMenuList,
  moveNode,
  updateMenu
} from '@/api/permission/menu'
import { getApplicationList } from '@/api/permission/application'
import menuDetails from './components/menuDetails'
import { deepCopy } from '@/utils'

const temp = {
  id: '',
  name: '',
  url: '',
  imgurl: '',
  parentId: 0,
  parentName: '',
  applicationId: '',
  applicationName: '',
  isleaf: '',
  level: 0,
  sort: '',
  status: '',
  gmtCreated: undefined,
  gmtModified: undefined
}

export default {
  name: 'Menu',
  components: { asideTree, menuDetails },
  data() {
    return {
      treeData: [],
      menuParam: {
        applicationId: '',
        applicationName: ''
      },
      activeTab: '',
      applicationOptions: [],
      dialogStatus: this.$t('index.operations.newMenu'),
      rules: {
        name: [
          {
            required: true,
            message: this.$t('index.menuBar.menuNameCannotBeEmpty'),
            trigger: 'change'
          }
        ]
      },
      dialogFormVisible: false,
      temp: deepCopy(temp)
    }
  },
  watch: {
    activeTab() {
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
        this.treeData = data
      })
    },

    getApplicationOptions() {
      getApplicationList().then(({ data }) => {
        if (data.length > 0) {
          const { id, name } = data[0]
          this.menuParam.applicationId = id
          this.activeTab = id
          this.menuParam.applicationName = name
        }
        this.applicationOptions = data

        this.getMenuTree()
      })
    },

    moveNode(node, targetNode, location, event) {
      const param = {
        currentId: node.data.id,
        currentParentId: node.data.parentId,
        currentSort: node.data.sort,
        targetId: targetNode.data.id,
        targetParentId: targetNode.data.parentId,
        targetSort: targetNode.data.sort,
        targetLevel: targetNode.data.level,
        location
      }
      moveNode(param).then(res => {
        if (res.code === 0) {
          this.getMenuTree()
        }
      })
    },

    create() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const param = deepCopy(this.temp)
          createMenu(param).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.createSuccess'),
                type: 'success'
              })
              this.getMenuTree()
              this.beforeClose()
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

    async showDetail({ id }) {
      const res = await getMenuById(id)
      if (res.code === 0) {
        this.$refs.details.setData(res.data)
      }
    },

    // 主菜单添加
    async clickTopAdd({ id }) {
      const response = await getMenuChildren({
        id: '-1',
        applicationId: id
      })
      const children = response.data
      let sort = 0

      if (children.length > 0) {
        sort = Math.max.apply(Math, children.map(o => o.sort))
      }

      this.temp.sort = sort + 1
      this.temp.parentId = -1
      this.temp.level = 1
      this.temp.isleaf = 1
      this.temp.applicationId = id

      this.dialogStatus = this.$t('index.operations.newMenu')
      this.dialogFormVisible = true
    },

    // 叶子节点添加
    async clickAdd(node, { id, applicationId, level }) {
      const response = await getMenuChildren({ id, applicationId })
      const children = response.data
      let sort = 0

      if (children.length > 0) {
        sort = Math.max.apply(Math, children.map(o => o.sort))
      }

      this.temp.sort = sort + 1
      this.temp.parentId = id
      this.temp.level = level + 1
      this.temp.isleaf = 1
      this.temp.applicationId = applicationId

      this.dialogStatus = this.$t('index.operations.newMenu')
      this.dialogFormVisible = true
    },

    // 修改
    handleUpdate(node, { id }) {
      getMenuById(id).then(({ data }) => {
        this.temp = data
        this.temp.id = id
        this.dialogStatus = this.$t('index.operations.editMenu')
        this.dialogFormVisible = true
      })
    },

    update() {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          const param = deepCopy(this.temp)
          updateMenu(param).then(result => {
            if (result.code === 0) {
              this.$message({
                message: this.$t('index.statusTitle.changeSuccess'),
                type: 'success'
              })
              this.beforeClose()
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

    clickRemove(node, { id }) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(() => {
        deleteMenu(id).then(result => {
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
              message: this.$t('index.statusTitle.failToDelete')
            })
          }
        })
        this.getMenuTree()
      })
    },

    handleStatus(data, status) {
      const param = {
        ...deepCopy(data),
        status
      }
      updateMenu(param).then(res => {
        if (res.code === 0) {
          this.$message({
            message:
              status === 0
                ? this.$t('index.statusTitle.restoreSuccess')
                : this.$t('index.statusTitle.forbiddenSucceed'),
            type: 'success'
          })
          this.getMenuTree()
        } else {
          this.$message({
            message:
              status === 0
                ? this.$t('index.statusTitle.restoreFailure')
                : this.$t('index.statusTitle.forbiddenFail'),
            type: 'error'
          })
        }
      })
    },

    beforeClose() {
      this.temp = deepCopy(temp)
      this.$refs['tempForm'].resetFields()
      this.dialogFormVisible = false
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
</style>
