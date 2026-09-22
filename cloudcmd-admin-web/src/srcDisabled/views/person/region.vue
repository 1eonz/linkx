<template>
  <div class="app-container">
    <div class="menu-body">
      <el-card shadow="always" class="card">
        <aside-tree
          :tree-data="treeData"
          :append-top-add-visible="true"
          :append-add-visible="true"
          :append-delete-visible="true"
          :append-resume-visible="true"
          :append-edit-visible="true"
          :import-export="true"
          :sync-visible="true"
          @clickEdit="handleUpdate"
          @moveNode="moveNode"
          @clickRemove="handleDelete"
          @clickResume="resuming"
          @showDetail="showDetail"
          @clickAdd="clickAdd"
          @clickTopAdd="clickTopAdd"
          @getRegionTree="getOrganizationTree"
          @submitSuccess="getOrganizationTree"
          @clickSync="handleSync"
        />
      </el-card>
    </div>

    <!-- 详情 -->
    <region-details ref="details" />

    <!-- 编辑 -->
    <region-edit
      ref="edit"
      :org-list="orgList"
      @success="getOrganizationTree"
    />
    <!-- 同步页面 -->
    <regionSync ref="sync" @success="getOrganizationTree" />
  </div>
</template>

<script>
import asideTree from '../common/asideTree'
import {
  getRegionById,
  getRegionList,
  updateOrganization,
  deleteRegion
} from '@/api/resource/region'
import { getOrganizationList } from '@/api/resource/organization'
import regionDetails from './components/regionDetails'
import regionEdit from './components/regionEdit'
import regionSync from '@/views/common/regionSync.vue'

export default {
  name: 'Organization',
  components: { asideTree, regionDetails, regionEdit, regionSync },
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
      orgList: []
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
      getRegionList(1).then(({ data }) => {
        this.treeData = data
      })
      getOrganizationList(1).then(({ data }) => {
        this.orgList = data
      })
    },

    handleSync(node, data) {
      this.$refs.sync.add(data.code)
    },

    moveNode(node, targetNode, location, event) {
      this.getOrganizationTree()
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

    handleDelete(node, { id }) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(() => {
        deleteRegion(id).then(result => {
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
      getRegionById(id).then(res => {
        if (res.code === 0) {
          this.$refs.details.setData(res.data)
        }
      })
    },

    resuming(node, data) {
      this.$confirm(
        this.$t('index.messageText.restoreTissueAndItsSubTissues'),
        {
          confirmButtonText: this.$t('index.determine'),
          cancelButtonText: this.$t('index.cancel'),
          type: 'info'
        }
      ).then(() => {
        const param = {
          ...data,
          status: 0
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
            this.$message({
              message: this.$t('index.statusTitle.restoreFailure'),
              type: 'error'
            })
          }
        })
      })
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
