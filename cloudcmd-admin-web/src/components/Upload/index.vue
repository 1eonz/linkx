<template>
  <el-dialog
    :title="title"
    :visible.sync="visible"
    width="500px"
    @close="closeFaceDialog"
  >
    <el-upload
      ref="upload"
      class="upload-content"
      drag
      action="#"
      multiple
      :http-request="uploadFile"
      :before-upload="beforeUpload"
      :on-change="handleChange"
      :on-remove="handleRemove"
      :on-success="handleSuccess"
      :on-exceed="handleExceed"
      :on-error="handleError"
      :file-list="fileList"
      :auto-upload="false"
      :limit="limit"
      :accept="accept"
    >
      <i class="el-icon-folder"></i>
      <div class="el-upload__text">
        <div class="tip-words">
          {{ tipsTitle || this.$t('index.list.clickToUpload') }}
        </div>
        <div v-if="tipsList && tipsList.length > 0" class="tip-size">
          <div v-for="(item, index) in tipsList" :key="item + index">
            {{ item }}
          </div>
        </div>
        <div v-else class="tip-size">
          <div>
            {{ $t('index.list.uploadFormat') }}
          </div>
          <div>
            {{ $t('index.list.uploadSize') }}
          </div>
          <div>
            {{ $t('index.list.uploadName') }}
          </div>
        </div>
      </div>
    </el-upload>
    <div slot="footer" class="dialog-footer">
      <el-button @click="closeFaceDialog">
        {{ $t('index.cancel') }}
      </el-button>
      <el-button type="primary" :loading="uploadLoading" @click="submitUpload">
        {{ $t('index.list.upload') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'UploadFace',
  props: {
    title: {
      type: String,
      default: ''
    },
    accept: {
      type: String,
      default: '.jpg,.png,.gif,.JPG,.PNG,.GIF'
    },
    uploadApi: {
      type: Function,
      default: () => {}
    },
    uploadField: {
      type: String,
      default: 'file'
    },
    tipsTitle: {
      type: String,
      default: ''
    },
    tipsList: {
      type: Array,
      default: () => []
    },
    limit: {
      type: Number,
      default: 1
    },
    maxSize: {
      type: Number,
      default: 0
    },
    maxSizeTips: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      fileList: [],
      visible: false,
      uploadLoading: false
    }
  },
  watch: {},
  methods: {
    init() {
      this.visible = true
    },

    closeFaceDialog() {
      this.$refs.upload.clearFiles()
      this.fileList = []
      this.visible = false
    },

    uploadFile(file) {},

    handleRemove(file, fileList) {
      this.fileList = fileList
      console.log(file, fileList, '移除')
    },

    handleChange(file, fileList) {
      this.fileList = fileList
      console.log(this.fileList, '改变')
    },

    handleSuccess(file, fileList) {
      console.log(file, fileList, '成功')
    },

    handleError(file, fileList) {
      console.log(file, fileList, '失败')
    },

    handleExceed() {
      this.$message.error('最多上传' + this.limit + '个文件')
    },

    // 校验图片规格
    beforeUpload(file) {
      const { size } = file
      if (this.maxSize) {
        if (size / 1024 > this.maxSize) {
          this.$message.error(this.maxSizeTips)
          return false
        }
      }
      return true
    },
    // 校验所有图片
    validateImage(files) {
      const filesLength = files.length
      // 空
      if (!filesLength) {
        this.$message.warning(this.$t('index.messageText.ImageNoChoose'))
        return false
      }
      // 重复
      const newFilesLength = new Set(files.map(item => item.name)).size
      if (filesLength > newFilesLength) {
        this.$message.warning(this.$t('index.messageText.ImageNoRepetition'))
        return false
      }
      // 规格
      let validate = true
      for (let i = 0; i < files.length; i++) {
        const validateItem = this.beforeUpload(files[i].raw)
        if (!validateItem) {
          validate = false
          return
        }
      }
      return validate
    },

    // 图片上传
    async submitUpload() {
      const imageValid = this.validateImage(this.fileList)
      if (!imageValid) {
        return
      }
      const formData = new FormData()
      this.fileList.forEach(item => {
        formData.append(this.uploadField, item.raw)
      })

      this.uploadLoading = true
      await this.uploadApi(formData)
        .then(result => {
          const { code, msg } = result
          if (code === 0) {
            this.$message({
              message: this.$t('index.messageText.UploadSuccess'),
              type: 'success'
            })
            this.$refs.upload.clearFiles()
            this.closeFaceDialog()
            this.$emit('success')
          } else {
            this.$message({
              message: msg,
              type: 'error'
            })
          }
        })
        .catch(() => {
          this.$message({
            message: this.$t('index.messageText.uploadFailed'),
            type: 'error'
          })
        })
      this.uploadLoading = false
    }
  }
}
</script>
<style lang="scss" scoped>
.tip-words {
  font-size: 16px;
  color: #303133;
}
.tip-size {
  font-size: 14px;
  color: #bfbdbc;
  margin-top: 6px;
}
.el-icon-folder {
  font-size: 50px;
  color: #409efe;
  margin: 30px auto 20px;
}
</style>
