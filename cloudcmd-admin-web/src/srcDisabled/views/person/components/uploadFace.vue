<template>
  <el-dialog
    :title="$t('index.list.batchUploadFace')"
    :visible.sync="visible"
    width="500px"
    @close="closeFaceDialog"
  >
    <el-upload
      ref="upload"
      class="upload-face"
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
      :limit="100"
      accept=".jpg,.png,.gif,.JPG,.PNG,.GIF"
    >
      <i class="el-icon-folder"></i>
      <div class="el-upload__text">
        <div class="tip-words">{{ $t('index.list.clickToUpload') }}</div>
        <div class="tip-size">
          <div>
            {{ $t('index.list.uploadFormat') }}<span>&nbsp;&nbsp;</span>
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
      <el-button type="primary" @click="submitUpload">
        {{ $t('index.list.upload') }}
      </el-button>
    </div>
  </el-dialog>
</template>

<script>
import { uploadBatch } from '@/api/resource/person'
export default {
  name: 'UploadFace',
  data() {
    return {
      fileList: [],
      visible: false
    }
  },
  watch: {},
  methods: {
    async init() {
      this.visible = true
    },
    closeFaceDialog() {
      this.$refs.upload.clearFiles()
      this.fileList = []
      this.visible = false
    },

    async uploadFile(file) {},
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
      this.$message.error(this.$t('index.messageText.maxImage'))
    },
    // 校验图片规格
    beforeUpload(file) {
      const { name, size } = file
      const index = name.lastIndexOf('.')
      const fileName = name.substring(0, index)
      const type = name.substring(index + 1).toLowerCase()
      const typeData = ['jpg', 'png', 'gif']
      const isJPG = typeData.includes(type)
      const isLt5KB = size / 1024 < 500
      const isLonger = fileName.length > 64
      const hasNull = fileName.includes(' ')
      if (!isJPG) {
        this.$message.error(this.$t('index.messageText.ImageFormat'))
      }
      if (!isLt5KB) {
        this.$message.error(this.$t('index.messageText.ImageSize'))
      }
      if (isLonger) {
        this.$message.error(this.$t('index.messageText.ImageNameLength'))
      }
      if (hasNull) {
        this.$message.error(this.$t('index.messageText.ImageNameNoNull'))
      }
      return isJPG && isLt5KB && !isLonger && !hasNull
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
        formData.append('multipartFiles ', item.raw)
      })
      await uploadBatch(formData)
        .then(result => {
          const { code, msg } = result
          if (code === 0) {
            this.$message({
              message: this.$t('index.messageText.ImageUploadSuccess'),
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
            message: this.$t('index.messageText.ImageUploadFailed'),
            type: 'error'
          })
        })
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
