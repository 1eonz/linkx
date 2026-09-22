<template>
  <div class="app-container">
    <div class="menu-body">
      <el-card shadow="always" class="card">
        <aside-tree
          :tree-data="treeData"
          :append-top-add-visible="
            hasPerm('/admin/organization/create') && isAdmin
          "
          :append-add-visible="
            hasPerm('/admin/organization/create') && hasChild
          "
          :append-delete-visible="hasPerm('/admin/organization/delete')"
          :append-resume-visible="hasPerm('/admin/organization/update')"
          :append-edit-visible="hasPerm('/admin/organization/update')"
          @clickEdit="handleUpdate"
          @moveNode="moveNode"
          @clickRemove="handleDelete"
          @clickResume="resuming"
          @showDetail="showDetail"
          @clickAdd="clickAdd"
          @clickTopAdd="clickTopAdd"
        />
      </el-card>
    </div>

    <!-- 恢复 -->
    <el-dialog
      title="提示"
      :visible.sync="dialogVisible"
      width="30%"
      :before-close="handleClose"
    >
      <el-radio v-model="hasChildren" label="1">{{
        $t('index.messageText.restoreTissueAndItsSubTissues')
      }}</el-radio>
      <el-radio v-model="hasChildren" label="0">{{
        $t('index.messageText.restoreTissue')
      }}</el-radio>

      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button type="primary" @click="handelResume">
          {{ $t('index.determine') }}
        </el-button>
      </span>
    </el-dialog>

    <!-- 详情 -->
    <organization-details ref="details" />

    <!-- 编辑 -->
    <organization-edit ref="edit" @success="getOrganizationTree" />
  </div>
</template>

<script>
import asideTree from '../common/asideTree'
import {
  deleteOrganization,
  getOrganizationById,
  getOrganizationList,
  moveOrganizationNode,
  updateOrganization
} from '@/api/resource/organization'
import organizationDetails from './components/organizationDetails'
import organizationEdit from './components/organizationEdit'

export default {
  name: 'Organization',
  components: { asideTree, organizationDetails, organizationEdit },
  data() {
    return {
      treeData: [],
      nodeTemp: {
        currentId: '',
        currentParentId: '',
        currentSort: '',
        targetId: '',
        targetParentId: '',
        targetSort: '',
        targetLevel: '',
        location: ''
      },
      hasChildOrgPriv: 0,
      dialogVisible: false,
      upDataData: {},
      hasChildren: '1'
    }
  },
  computed: {
    isAdmin() {
      return this.$store.state.user.type === 0
    },
    hasChild() {
      return this.$store.state.user.hasChildOrgPriv === 0
    }
  },
  created() {
    this.getOrganizationTree()
  },
  activated() {
    this.getOrganizationTree()
  },
  methods: {
    getOrganizationTree() {
      getOrganizationList(1).then(({ data }) => {
        this.treeData = data
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
      moveOrganizationNode(this.nodeTemp).then(({ code, msg }) => {
        if (code !== 0) {
          this.$message({
            message: msg,
            type: 'error'
          })
        }
        this.getOrganizationTree()
      })
    },

    async clickTopAdd(node, data) {
      this.$refs.edit.topAdd()
    },

    async clickAdd(node, data) {
      this.$refs.edit.add(data)
    },

    handleUpdate(node, data) {
      this.$refs.edit.modify(data)
    },
    handleClose() {
      this.dialogVisible = false
    },

    handleDelete(node, { id }) {
      this.$confirm(
        this.$t('index.operations.affirmDeleted') +
          this.$t('index.operations.affirmDeletedIncludesOrganizations'),
        {
          confirmButtonText: this.$t('index.determine'),
          cancelButtonText: this.$t('index.cancel'),
          type: 'info'
        }
      ).then(() => {
        deleteOrganization(id).then(result => {
          if (result.code === 0) {
            this.$message({
              message: this.$t('index.statusTitle.successfullyDelete'),
              type: 'success'
            })
            this.eventBus.$emit('clearFilterText')
            this.getOrganizationTree()
          } else {
            this.$message({
              message: result.msg,
              type: 'error'
            })
          }
        })
      })
    },

    showDetail({ id }) {
      getOrganizationById(id).then(res => {
        if (res.code === 0) {
          this.$refs.details.setData(res.data)
        }
      })
    },

    resuming(node, data) {
      this.upDataData = data
      this.dialogVisible = true
    },

    handelResume() {
      const param = {
        ...this.upDataData,
        status: 0,
        hasChildren: this.hasChildren === '1'
      }
      updateOrganization(param).then(result => {
        if (result.code === 0) {
          this.$message({
            message: this.$t('index.statusTitle.restoreSuccess'),
            type: 'success'
          })
          this.eventBus.$emit('clearFilterText')
          this.getOrganizationTree()
        } else {
          const msg =
            result.code === 568
              ? result.msg
              : this.$t('index.statusTitle.restoreFailure')
          this.$message({
            message: msg,
            type: 'error'
          })
        }
      })

      this.dialogVisible = false
    }
  }
}
</script>

<style lang="scss" scoped>
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

.menu-body {
  position: absolute;
  left: 20px;
  right: 20px;
  top: 20px;
  bottom: 20px;
  background-color: #ebeef5;
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
