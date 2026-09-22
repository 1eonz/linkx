<template>
  <div class="mytree">
    <div class="search-box">
      <el-input
        v-model="filterText"
        :placeholder="$t('index.operations.inputContent')"
      >
        <i slot="prefix" class="el-input__icon el-icon-search"></i>
      </el-input>
      <el-button
        v-if="syncVisible"
        icon="el-icon-upload"
        size="medium"
        type="success"
        @click="synchronizationDataFnc"
      >
        {{ $t('index.operations.synchronousData') }}
      </el-button>
      <el-button
        v-if="importExport"
        type="info"
        icon="el-icon-download"
        style="margin-right:10px"
        @click="downloadTheTemplate"
      >
        {{ $t('index.operations.downloadTheTemplate') }}
      </el-button>
      <el-upload
        v-if="importExport"
        ref="upload"
        class="upload-demo"
        action="#"
        accept=".json"
        :http-request="uploadFileBtn"
        :show-file-list="false"
        :on-success="onSuccess"
        :on-error="onError"
      >
        <el-button type="primary">
          {{ $t('index.operations.import') }}
        </el-button>
      </el-upload>
      <el-button
        v-if="importExport"
        type="primary"
        style="margin-left:10px"
        @click="exportlist"
      >
        {{ $t('index.operations.export') }}
      </el-button>
    </div>

    <span v-if="title !== null && title !== ''" class="titleClass">
      {{ title }}
    </span>

    <el-tree
      ref="tree"
      class="filter-tree"
      :draggable="draggable"
      :default-expand-all="defaultExpandAll"
      node-key="id"
      :data="treeData"
      :expand-on-click-node="false"
      :props="defaultProps"
      :highlight-current="true"
      :indent="0"
      :filter-node-method="filterNode"
      @node-click="showDetail"
      @node-drop="moveNode"
    >
      <span
        slot-scope="{ node, data }"
        class="custom-tree-node"
        @click="clickData(data)"
      >
        <el-tooltip
          effect="Light"
          :open-delay="openDelay"
          :visible-arrow="false"
          placement="right"
        >
          <span slot="content" style="font-size: 18px">
            <template v-if="data.status !== 1">
              <a
                v-show="appendAddVisible"
                class="el-icon-circle-plus-outline"
                :title="$t('index.operations.Added')"
                style="color:#409EFF;size: auto"
                @click="clickAdd(node, data)"
              >
              </a>
              &nbsp;
              <a
                v-show="appendEditVisible"
                class="el-icon-edit"
                :title="$t('index.operations.redact')"
                style="color:#409E00"
                @click="clickEdit(node, data)"
              >
              </a>
              &nbsp;
            </template>
            <template v-if="appendManageOrganizationVisible">
              <a
                class="el-icon-setting"
                :title="$t('index.list.organizationManager')"
                style="color:#8A2BE2"
                @click="clickManageOrganization(node, data)"
              >
              </a>
              &nbsp;
            </template>
            <a
              v-show="appendManageExecutorVisible"
              class="el-icon-user"
              :title="$t('index.list.executiveManager')"
              style="color:#B4BCCC"
              @click="clickManageExecutor(node, data)"
            >
            </a>
            <a
              v-show="syncVisible"
              class="el-icon-upload"
              :title="$t('index.operations.synchronousData')"
              style="color:green;margin-right: 7px;"
              @click="clickSync(node, data)"
            >
            </a>
            <a
              v-show="appendDeleteVisible && data.status === 0"
              class="el-icon-delete"
              :title="$t('index.delete')"
              style="color:red"
              @click="clickRemove(node, data)"
            >
            </a>
            <a
              v-show="appendPhyDeleteVisible"
              class="el-icon-delete"
              :title="$t('index.delete')"
              style="color:red"
              @click="clickRemove(node, data)"
            >
            </a>
            <a
              v-show="
                (appendResumeVisible && data.status === 1) || data.status === 2
              "
              class="el-icon-star-on"
              :title="$t('index.operations.restore')"
              style="color:orange"
              @click="clickResume(node, data)"
            >
            </a>
            <a
              v-show="appendWarnVisible && data.status === 0"
              class="el-icon-star-off"
              :title="$t('index.list.forbidden')"
              style="color:red"
              @click="clickWarn(node, data)"
            >
            </a>
          </span>
          <span
            :title="data.name"
            class="content"
            :style="data.status === 0 ? 'color:black' : 'color:#CDC8B1'"
          >
            {{ data.name }}
            <span v-if="data.status === 1">
              {{ `(${$t('index.operations.deleted')})` }}
            </span>
            <span v-if="data.status === 2">
              {{ `(${$t('index.operations.disabled')})` }}
            </span>
          </span>
        </el-tooltip>
      </span>
    </el-tree>

    <el-main v-show="topAddVisible">
      <span
        v-show="appendTopAddVisible"
        class="add-body-class"
        @click="clickTopAdd()"
      >
        <a
          class="el-icon-circle-plus-outline"
          :title="$t('index.operations.Added')"
          style="color:#409EFF;size: auto"
        ></a>
        {{ $t('index.operations.Added') }}
      </span>
    </el-main>
    <el-footer />
    <!-- 同步页面 -->
    <regionSync ref="sync" @success="getList" />
  </div>
</template>

<script>
import {
  regionDownload,
  uploadFile,
  regionDownloadModel
} from '@/api/resource/region'
import regionSync from './regionSync.vue'
export default {
  name: 'AsideTree',
  components: { regionSync },
  props: {
    treeData: {
      type: Array,
      default: () => []
    },
    draggable: {
      default: true,
      type: Boolean
    },
    appendTopAddVisible: {
      default: false,
      type: Boolean
    },
    appendAddVisible: {
      default: false,
      type: Boolean
    },
    appendDeleteVisible: {
      default: false,
      type: Boolean
    },
    appendPhyDeleteVisible: {
      default: false,
      type: Boolean
    },
    appendEditVisible: {
      default: false,
      type: Boolean
    },
    appendResumeVisible: {
      default: false,
      type: Boolean
    },
    appendWarnVisible: {
      default: false,
      type: Boolean
    },
    appendManageOrganizationVisible: {
      default: false,
      type: Boolean
    },
    appendManageExecutorVisible: {
      default: false,
      type: Boolean
    },
    topAddVisible: {
      default: true,
      type: Boolean
    },
    title: {
      type: String,
      default: ''
    },
    defaultExpandAll: {
      type: Boolean,
      default: true
    },
    importExport: {
      type: Boolean,
      default: false
    },
    syncVisible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dataLabel: '',
      filterText: '',
      showAdd: false,
      openDelay: 200,
      currentData: '',
      defaultProps: {
        children: 'children',
        label: 'name'
      }
    }
  },
  watch: {
    filterText(val) {
      this.$refs.tree.filter(val)
    },
    treeData(val) {
      this.$refs.tree.filter(val)
    }
  },
  mounted() {
    this.eventBus.$on('clearFilterText', () => {
      this.filterText = ''
    })
  },
  methods: {
    getList() {
      this.$emit('submitSuccess')
    },
    synchronizationDataFnc() {
      this.$refs.sync.add('')
    },
    clickAdd(node, data) {
      this.$emit('clickAdd', node, data)
    },
    clickTopAdd() {
      this.$emit('clickTopAdd')
    },
    clickEdit(node, data) {
      this.$emit('clickEdit', node, data)
    },
    clickRemove(node, data) {
      this.$emit('clickRemove', node, data)
    },
    clickSync(node, data) {
      this.$emit('clickSync', node, data)
    },
    clickResume(node, data) {
      this.$emit('clickResume', node, data)
    },
    clickWarn(node, data) {
      this.$emit('clickWarn', node, data)
    },
    clickManageExecutor(node, data) {
      this.$emit('clickManageExecutor', node, data)
    },
    clickManageOrganization(node, data) {
      this.$emit('clickManageOrganization', node, data)
    },
    showDetail(data) {
      this.$emit('showDetail', data)
    },
    clickData(data) {
      this.$emit('clickData', data)
    },
    moveNode(node, dstNode, location, event) {
      this.$emit('moveNode', node, dstNode, location, event)
    },
    filterNode(value, data) {
      if (!value) return true
      return data.name.indexOf(value) !== -1
    },
    exportlist() {
      const param = JSON.stringify(this.treeData)
      regionDownload(param).then(result => {
        const url = window.URL.createObjectURL(new Blob([result]))
        const link = document.createElement('a')
        link.style.display = 'none'
        link.href = url
        link.setAttribute('download', 'region.json')
        document.body.appendChild(link)
        link.click()
      })
    },
    downloadTheTemplate() {
      // const param = {}
      regionDownloadModel().then(result => {
        const url = window.URL.createObjectURL(new Blob([result]))
        const link = document.createElement('a')
        link.style.display = 'none'
        link.href = url
        link.setAttribute('download', this.$t('index.operations.template') + '.json')
        document.body.appendChild(link)
        link.click()
      })
    },
    onSuccess(response) {
      // this.$message.success('上传成功')
      // 处理后端返回的响应数据
    },
    onError() {
      // this.$message.success('上传成功')
      // 处理后端返回的响应数据
    },
    uploadFileBtn(param) {
      const formData = new FormData()
      formData.append('file', param.file)
      this.$refs.upload.clearFiles()
      this.sheets = []
      uploadFile(formData).then(res => {
        if (res.code === 0) {
          this.$message.success(this.$t('index.messageText.UploadSuccess'))
          this.$emit('getRegionTree')
        } else {
          this.$message({
            message: res.msg,
            type: 'error'
          })
        }
      })
    }
  }
}
</script>

<style lang="scss">
.mytree {
  .el-tree-node__content {
    line-height: 32px;
    height: 32px;
  }
  .el-tree > .el-tree-node:after {
    border-top: none;
  }
  .el-tree-node {
    position: relative;
    padding-left: 16px;
  }
  .el-tree-node__children {
    padding-left: 18px;
  }

  .el-tree-node :last-child:before {
    height: 38px;
  }

  .el-tree > .el-tree-node:before {
    border-left: none;
  }

  .el-tree > .el-tree-node:after {
    border-top: none;
  }

  .el-tree-node:before {
    content: '';
    left: -4px;
    position: absolute;
    right: auto;
    border-width: 1px;
  }

  .el-tree-node:after {
    content: '';
    left: -4px;
    position: absolute;
    right: auto;
    border-width: 1px;
  }

  .el-tree-node:before {
    border-left: 1px dashed #4386c6;
    bottom: 0;
    height: 100%;
    top: -22px;
    width: 1px;
  }

  .el-tree-node:after {
    border-top: 1px dashed #4386c6;
    height: 30px;
    top: 16px;
    width: 26px;
  }

  .el-tree .el-tree-node__expand-icon.expanded {
    -webkit-transform: rotate(0deg);
    transform: rotate(0deg);
  }
  .el-tree .el-icon-caret-right:before {
    background: url('../../assets/tree-images/folder.png') no-repeat 0 0;
    content: '';
    display: block;
    width: 18px;
    height: 18px;
    font-size: 18px;
    background-size: 18px;
  }
  .el-tree .el-tree-node__expand-icon.expanded.el-icon-caret-right:before {
    background: url('../../assets/tree-images/folder-open.png') no-repeat 0 0;
    content: '';
    display: block;
    width: 18px;
    height: 18px;
    font-size: 18px;
    background-size: 18px;
  }
  .el-tree .el-tree-node__expand-icon.is-leaf::before {
    background: url('../../assets/tree-images/document.png') no-repeat 0 0;
    content: '';
    display: block;
    width: 18px;
    height: 18px;
    font-size: 18px;
    background-size: 18px;
  }
}

.search-box {
  display: flex;

  .el-input {
    margin-right: 5px;
  }
}

.custom-tree-node {
  font-size: 16px;
  padding-right: 40px;
  line-height: 1.5;
}

.content {
  display: inline-block;
  vertical-align: top;
}

.treeCard {
  height: 100%;
  overflow-y: auto;
}

.titleClass {
  font-size: 24px;
  text-align: center;
  display: block;
  height: 40px;
  margin-top: 20px;
}

.add-body-class {
  font-size: 18px;
  padding-left: 18px;
  cursor: pointer;
}

.el-tooltip__popper[x-placement^='right'] .popper__arrow::after {
  border-left-color: #515b62;
}

.el-tooltip__popper[x-placement^='right'] .popper__arrow {
  border-left-color: #515b62;
}
</style>
