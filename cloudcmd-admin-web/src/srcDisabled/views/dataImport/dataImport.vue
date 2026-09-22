<template>
  <div class="div-box">
    <div class="sub-div-box">
      <el-upload
        ref="upload"
        class="upload-demo"
        accept=".xls,.xlsx"
        action="string"
        multiple
        :limit="1"
        :before-upload="beforeUpload"
        :http-request="getSheets"
        :on-exceed="handleExceed"
        :on-preview="handlePreview"
        :on-error="handleError"
      >
        <el-button
          v-if="hasPerm('/admin/data/getSheets')"
          slot="trigger"
          icon="el-icon-folder-opened"
          size="medium"
          type="primary"
        >
          {{ $t('index.operations.gettingTheImportList') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/data/import')"
          icon="el-icon-upload"
          size="medium"
          type="success"
          @click="submitUpload($t('index.operations.importData'))"
        >
          {{ $t('index.operations.importData') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/data/export')"
          icon="el-icon-download"
          size="medium"
          type="info"
          :loading="tempLoading"
          @click="templateDownload"
        >
          {{ $t('index.operations.downloadTheTemplate') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/data/export/data')"
          icon="el-icon-download"
          size="medium"
          type="info"
          :loading="exportLoading"
          @click="handleExport"
        >
          {{ $t('index.operations.dataExport') }}
        </el-button>
        <el-button
          v-if="hasPerm('/admin/resource/synchronization/data')"
          v-show="false"
          icon="el-icon-upload"
          size="medium"
          type="success"
          :loading="syncLoading"
          @click="
            synchronizationDataFnc($t('index.operations.synchronousData'))
          "
        >
          {{ $t('index.operations.synchronousData') }}
        </el-button>
        <el-select
          v-model="remote"
          :placeholder="$t('index.operations.selects')"
          size="medium"
          style="width: 120px"
        >
          <el-option
            v-for="item in remotes"
            :key="item"
            :label="item"
            :value="item"
          />
        </el-select>
        <el-button
          :disabled="!remote"
          size="medium"
          type="success"
          @click="handleFullTableSynchronization"
        >
          {{ $t('index.operations.fullTableSynchronization') }}
        </el-button>

        <div slot="tip" class="el-upload__tip">
          <span style="display:inline-block;width: 150px;">
            {{ $t('index.operations.onlyExcelFilesCanBeUploaded') }}
          </span>
          <el-button size="medium" type="text" @click="downloadResult">
            {{ $t('index.operations.importedLastTime') }}
          </el-button>
        </div>
      </el-upload>
    </div>

    <div class="proClass">
      <el-progress
        v-if="showProgress"
        :percentage="percentage"
        :text-inside="true"
        :stroke-width="12"
        status="success"
        :color="customColors"
      />
    </div>

    <div v-show="sheets.length !== 0" class="checkbox-box">
      <el-checkbox
        v-model="checkAll"
        :indeterminate="isIndeterminate"
        @change="handleCheckAllChange"
      >
        {{ $t('index.operations.checkAll') }}
      </el-checkbox>
      <el-checkbox-group
        v-model="uploadData.checkedList"
        @change="handleCheckedSheetChange"
      >
        <el-checkbox
          v-for="(sheet, index) in sheets"
          :key="index"
          :label="sheet"
        />
      </el-checkbox-group>
    </div>

    <el-dialog
      :title="dataProcessingMethod"
      :visible.sync="dialogVisible"
      width="500px"
      center
    >
      <div style="text-align:left">
        <span>{{ result }}</span>
      </div>
      <el-button
        v-if="dataProcessingMethod !== $t('index.operations.synchronousData')"
        size="medium"
        type="text"
        @click="downloadResult"
      >
        {{ $t('index.operations.objectInformation') }}
      </el-button>
    </el-dialog>
  </div>
</template>

<script>
import {
  downloadModal,
  exportData,
  downloadResult,
  getSheets,
  importData,
  importGetProgress,
  synchronizationData,
  synchronizationGetProcess,
  querydataRemotes,
  dataCopy,
  dataGetCopyProgress
} from '@/api/dataImport/dataImport'

export default {
  name: 'DataImport',
  data() {
    return {
      dataProcessingMethod: '',
      uploadData: {
        checkedList: []
      },
      customColors: [
        { color: '#f56c6c', percentage: 20 },
        { color: '#FFA07A', percentage: 40 },
        { color: '#FFFF00', percentage: 60 },
        { color: '#00FF00', percentage: 80 },
        { color: '#008000', percentage: 100 }
      ],
      timer: null,
      showProgress: false,
      loadingObj: '',
      percentage: 0,
      result: null,
      checkAll: false,
      dialogVisible: false,
      isIndeterminate: false,
      tableData: [],
      sheets: [],
      syncLoading: false,
      exportLoading: false,
      tempLoading: false,
      remotes: [],
      remote: ''
    }
  },
  mounted() {
    this.getDataRemotes()
    this.checkProcess()
  },
  methods: {
    checkProcess() {
      importGetProgress().then(({ data }) => {
        if (data.percent < 100) {
          this.waitLoading()
          this.showProgress = true
          this.timer = setInterval(this.getUploadProgress, 2000)
          this.dialogVisible = false
        }
      })
      synchronizationGetProcess().then(({ data }) => {
        if (data.percent < 100) {
          this.waitLoading('sync')
          this.dataProcessingMethod = this.$t(
            'index.operations.synchronousData'
          )
          this.syncLoading = true
          this.timer = setInterval(this.getSynchronizedProgress, 3000)
        }
      })
    },
    handleCheckAllChange(val) {
      if (val) {
        this.uploadData.checkedList = this.sheets
      } else {
        this.uploadData.checkedList = []
      }
      this.isIndeterminate = false
    },
    handleCheckedSheetChange(value) {
      const checkedCount = value.length
      this.checkAll = checkedCount === this.sheets.length
      this.isIndeterminate =
        checkedCount > 0 && checkedCount < this.sheets.length
    },
    // 模板下载
    async templateDownload() {
      this.tempLoading = true
      const data = await downloadModal()
      this.tempLoading = false
      if (!data) {
        return
      }
      const url = window.URL.createObjectURL(new Blob([data]))
      const link = document.createElement('a')
      link.style.display = 'none'
      link.href = url
      link.setAttribute('download', 'model.xlsx')
      document.body.appendChild(link)
      link.click()
    },
    // 数据导出
    async handleExport() {
      this.exportLoading = true
      const data = await exportData()
      this.exportLoading = false
      if (!data) {
        return
      }
      const url = window.URL.createObjectURL(new Blob([data]))
      const link = document.createElement('a')
      link.style.display = 'none'
      link.href = url
      link.setAttribute('download', 'data.xlsx')
      document.body.appendChild(link)
      link.click()
    },
    // 同步数据
    synchronizationDataFnc(text) {
      this.$confirm(this.$t('index.operations.confirmSynchronization'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          this.handleSynchronizationData(text)
        })
        .catch(() => {})
    },
    async handleSynchronizationData(text) {
      this.waitLoading('sync')
      this.dataProcessingMethod = text
      this.syncLoading = true
      this.timer = setInterval(this.getSynchronizedProgress, 3000)
      const { code, data } = await synchronizationData()
      if (code !== 0) {
        this.$message({
          message: data,
          type: 'error'
        })
      }
      // clearInterval(this.timer)
      this.getSynchronizedProgress()
    },
    // 获取同步数据进度
    async getSynchronizedProgress() {
      const { code, data } = await synchronizationGetProcess()
      if (code === 0 && data.percent === 100) {
        this.loadingObj.close()
        this.syncLoading = false
        clearInterval(this.timer)
        this.$set(this, 'result', data.result)
        this.dialogVisible = true
      }
    },
    // 导入进度
    getUploadProgress(text) {
      importGetProgress()
        .then(({ data }) => {
          this.percentage = parseInt(data.percent)
          if (data.percent >= 100) {
            this.$set(this, 'result', data.result)
            this.clearImportStatus(text)
          }
        })
        .catch(e => {
          clearInterval(this.timer)
        })
    },
    updateLastRequestTime() {
      global.lastRequestTime = new Date().getTime()
    },
    // 导入数据
    submitUpload(text) {
      if (this.sheets.length === 0) {
        this.$message.warning(
          this.$t('index.messageText.selectTheFileToImport')
        )
        return
      }
      if (this.uploadData.checkedList.length === 0) {
        this.$message.warning(
          this.$t('index.messageText.selectTheTypeOfDataToImport')
        )
        return
      }
      this.$confirm(this.$t('index.determineImport'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          // 清空进度条
          this.percentage = 0
          this.waitLoading()
          this.showProgress = true
          this.timer = setInterval(this.getUploadProgress, 2000)
          this.requestTimer = setInterval(this.updateLastRequestTime, 30000)
          importData(this.uploadData.checkedList).then(res => {
            if (!res) return
            const { code, msg } = res
            if (code === 2) {
              this.$message.error(msg)
              this.clearImportStatus(text)
              this.dialogVisible = false
            }
            if (code !== 0 && code !== 2) {
              this.$message.error(this.$t('index.messageText.importFailure'))
              this.clearImportStatus(text)
            }
          })
        })
        .catch(() => {})
    },
    // 清空导入状态
    clearImportStatus(text) {
      this.dialogVisible = true
      this.dataProcessingMethod = text
      this.loadingObj.close()
      this.sheets.length = 0
      this.choseProgress()
      clearInterval(this.timer)
      clearInterval(this.requestTimer)
      this.isIndeterminate = false
      this.checkAll = false
      this.uploadData.checkedList = []
    },
    handleExceed(result, fileList) {
      this.$message.warning(
        this.$t('index.messageText.onlyOneFileCanBeUploaded')
      )
    },
    waitLoading(type) {
      const text =
        type === 'sync'
          ? this.$t('index.messageText.dataSynchronization')
          : this.$t('index.messageText.dataImport')
      this.loadingObj = this.$loading({
        lock: true,
        text,
        spinner: 'el-icon-loading',
        background: 'rgba(0, 0, 0, 0.7)'
      })
    },
    beforeUpload(file) {
      if (file.size > 1024 * 1024 * 10) {
        this.$message.error(
          this.$t('index.messageText.fileSizeCannotExceed10MB')
        )
        return false
      }
      const permitType = '.xls|.xlsx|'
      const extName = file.name
        .substring(file.name.lastIndexOf('.'))
        .toLowerCase()
      if (permitType.indexOf(extName + '|') === -1) {
        this.$message.error(this.$t('index.messageText.fileFormatIsIncorrect'))
        return false
      }
      return true
    },
    getSheets(param) {
      const formData = new FormData()
      formData.append('file', param.file)
      this.$refs.upload.clearFiles()
      this.sheets = []
      getSheets(formData).then(result => {
        if (result.code === 0) {
          this.sheets = result.data
          this.$message.success(this.$t('index.statusTitle.acquireSuccess'))
        } else {
          this.$message.error(result.msg)
        }
      })
    },
    handleError(result, fileList) {
      this.$message.error(this.$t('index.statusTitle.acquireFail'))
    },
    handlePreview(file) {
      this.sheets = []
    },
    choseProgress() {
      this.showProgress = false
    },
    downloadResult() {
      downloadResult().then(data => {
        if (!data) {
          return
        }
        const url = window.URL.createObjectURL(new Blob([data]))
        const link = document.createElement('a')
        link.style.display = 'none'
        link.href = url
        link.setAttribute('download', 'result.xlsx')
        document.body.appendChild(link)
        link.click()
      })
    },
    async getDataRemotes() {
      const { code, data } = await querydataRemotes()
      if (code === 0) {
        this.remotes = data
      }
    },
    async handleFullTableSynchronization() {
      this.$confirm(this.$t('index.operations.confirmSynchronization'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      })
        .then(() => {
          this.fullTableSynchronization()
        })
        .catch(() => {})
    },
    async fullTableSynchronization() {
      this.percentage = 0
      this.waitLoading('sync')
      this.showProgress = true
      this.timer = setInterval(this.getDataGetCopyProgress, 2000)
      this.requestTimer = setInterval(this.updateLastRequestTime, 30000)

      const { code, msg } = await dataCopy(this.remote)
      if (code === 0) {
        this.$message.success(msg)
      } else {
        this.$message.error(msg)
      }
    },
    // 同步进度
    getDataGetCopyProgress() {
      dataGetCopyProgress()
        .then(({ data }) => {
          this.percentage = parseInt(data.percent)
          if (data.percent >= 100) {
            this.$set(this, 'result', data.result)
            this.clearImportStatus(this.$t('index.operations.synchronousData'))
          }
        })
        .catch(e => {
          clearInterval(this.timer)
        })
    }
  }
}
</script>

<style scoped lang="scss">
.div-box {
  margin-top: 5%;
  display: flex;
  flex-direction: column;
}
.sub-div-box {
  display: flex;
  justify-content: center;
}
.file_record_nodata {
  width: 100%;
  display: none;
}
.proClass {
  margin: auto;
  width: 60%;
}
.checkbox-box {
  margin: 20px auto;
}
</style>
