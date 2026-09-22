<template>
  <div>
    <el-dialog
      :visible.sync="visible"
      :title="dialogStatus"
      :before-close="handleClose"
      :close-on-click-modal="false"
      width="800px"
    >
      <el-form
        ref="tempForm"
        :rules="rules"
        :model="form"
        label-position="left"
        label-width="120px"
      >
        <!-- style="height: 600px; margin-left:30px; overflow-y: scroll;" -->

        <el-form-item
          :label="$t('index.list.fileImport')"
          prop="address"
          class="avatar-item"
        >
          <el-upload
            v-show="!fileObj.name"
            ref="upload"
            class="avatar-uploader"
            action="#"
            :multiple="false"
            :show-file-list="false"
            :file-list="fileList"
            :before-upload="beforeUpload"
            :http-request="uploadFile"
            :on-change="handleChange"
            accept=".html,.htm"
          >
            <i class="el-icon-plus avatar-uploader-icon"></i>
            <div class="el-upload__tip" slot="tip">
              {{ $t('index.list.importTips') }}
            </div>
          </el-upload>

          <!-- @click="getAddress(fileObj.address)" -->
          <div
            class="show-item"
            v-show="fileObj.name"
            @click="getAddress(fileObj.address)"
          >
            <div>{{ fileObj.name }}</div>
            <i class="el-icon el-icon-close" @click.stop="deleteFile"></i>
          </div>
        </el-form-item>
        <el-form-item :label="$t('index.list.headline')" prop="title">
          <el-input
            v-model.trim="form.title"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
            maxlength="50"
          />
        </el-form-item>
        <el-form-item :label="$t('index.list.explain')" prop="description">
          <el-input
            type="textarea"
            :rows="3"
            v-model="form.description"
            class="edit-input"
            :placeholder="$t('index.operations.inputContent')"
            maxlength="200"
          />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button @click="handleClose">
          {{ $t('index.cancel') }}
        </el-button>
        <el-button
          v-if="isAdd"
          type="primary"
          :loading="btnLoading"
          @click="handleSubmit(true)"
        >
          {{ $t('index.create') }}
        </el-button>
        <el-button
          v-else
          type="primary"
          :loading="btnLoading"
          @click="handleSubmit(false)"
        >
          {{ $t('index.operations.alter') }}
        </el-button>
      </div>
    </el-dialog>
    <el-dialog
      :visible.sync="visibleFile"
      :title="fileObj.name"
      :close-on-click-modal="true"
      width="800px"
      style="height: 700px; margin-left:30px;"
    >
      <div
        class="detail-content"
        style="width: 100%;height: 430px;overflow: hidden;"
      >
        <div
          v-html="reviewFile"
          ref="htmlViewer"
          style="width: 100%;height: 100%;overflow-y: scroll;"
        ></div>
      </div>
      <!-- <a :href="reviewFile">测试</a> -->
    </el-dialog>
  </div>
</template>

<script>
import { deepCopy, filterOrgList } from '@/utils'
import { getToken } from '@/utils/auth'
import { uploadGuideFile, createGuide, editGuide } from '@/api/guide/guide'

const form = {
  title: '', //标题
  description: '', //描述
  address: '',
  id: '',
  fileName: ''
}
export default {
  name: 'GuideEdit',
  components: {},
  data() {
    const rules = {
      title: [
        {
          required: true,
          message: this.$t('index.operations.inputContent'),
          trigger: 'change'
        }
      ],
      // description: [
      //   {
      //     required: true,
      //     message: this.$t('index.operations.inputContent'),
      //     trigger: 'change'
      //   }
      // ],
      address: [
        {
          required: true,
          message: this.$t('index.messageText.uploadFile'),
          trigger: 'change'
        }
      ]
    }
    return {
      visibleFile: false,
      fileList: [],
      fileObj: {},
      visible: false,
      imageUrl: '',
      showAuthImg: false,
      rules,
      dialogStatus: '',
      isAdd: true,
      btnLoading: false,
      form: deepCopy(form),
      reviewFile: '',
      uploadFileFlag: false
    }
  },
  computed: {},
  watch: {},
  created() {},
  methods: {
    async getAddress(address) {
      if (!address) return
      const htmlViewer = this.$refs.htmlViewer
      const token = getToken()
      const url = process.env.VUE_APP_BASE_API + address
      const request = new XMLHttpRequest()
      // request.responseType = 'blob'
      request.open('get', url, true)
      request.setRequestHeader('Authorization', 'token ' + token)
      request.onreadystatechange = e => {
        if (
          request.readyState === XMLHttpRequest.DONE &&
          request.status === 200
        ) {
          this.reviewFile = request.response
          this.visibleFile = true
        }
      }
      await request.send(null)
    },
    // showFileContent() {
    //   this.visibleFile = true
    // },
    //删除已选文件
    deleteFile() {
      this.fileList = []
      this.fileObj = {}
      this.form.address = ''
      this.form.fileName = ''
    },
    beforeUpload(file) {
      const allowedTypes = ['text/htm', 'text/html']
      const isAllowed = allowedTypes.includes(file.type)
      if (!isAllowed) {
        this.$message.error(this.$t('index.list.importTips'))
      }
      return isAllowed
    },
    async handleChange(file) {
      if (this.uploadFileFlag) {
        this.uploadFileFlag = false
        return
      }
      this.fileObj = file
    },
    // 文件上传
    async uploadFile(file) {
      if (!this.fileObj.name) {
        return
      }
      const formData = new FormData()
      formData.append('file', file.file)
      await uploadGuideFile(formData)
        .then(result => {
          const { code, data } = result
          if (code === 0) {
            this.uploadFileFlag = true
            this.form.address = data
            this.form.fileName = file.file.name
            this.fileObj = {
              name: file.file.name,
              address: data
            }
          } else {
            this.$message({
              message: this.$t('index.messageText.uploadFailed'),
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
    },
    // 新增
    add() {
      this.dialogStatus = this.$t('index.operations.Added')
      this.visible = true
    },

    // 修改
    async modify(data) {
      this.dialogStatus = this.$t('index.operations.redact')
      this.isAdd = false
      Object.assign(this.form, data)
      this.fileObj = {
        name: data.fileName,
        address: data.address
      }
      this.visible = true
    },
    // 关闭
    handleClose() {
      this.form = deepCopy(form)
      this.$refs['tempForm'].resetFields()
      this.visible = false
      this.isAdd = true
      this.fileObj = {}
    },

    // 点击确定
    async handleSubmit(isAdd) {
      //this.$refs.upload.submit()
      //setTimeout(() => {
      this.handleEdit(isAdd)
      //}, 500)
    },
    async handleEdit(isAdd) {
      this.$refs['tempForm'].validate(valid => {
        if (valid) {
          if (isAdd) {
            delete this.form.id
          }
          const param = JSON.parse(JSON.stringify(this.form))
          const api = isAdd ? createGuide : editGuide
          this.btnLoading = true
          api(param).then(result => {
            if (result.code === 0) {
              this.$message({
                message: result.msg,
                type: 'success'
              })
              this.$emit('success', this.form?.id)
              this.handleClose()
            } else if (result.code) {
              this.$message({
                message: result.msg,
                type: 'error'
              })
            }
            this.btnLoading = false
          })
        }
      })
    }
  }
}
</script>

<style scoped lang="scss">
.user-avatar {
  cursor: pointer;
  width: 15px;
  height: 15px;
  margin-top: 10px;
}

.edit-input {
  padding-right: 50px;
  width: 500px;
}
.tree-style {
  max-height: 240px;
  overflow: auto;
}
.avatar-item {
  margin-top: 20px;
  ::v-deep.el-form-item__content {
    margin-left: 0 !important;
  }
}
.el-upload__tip {
  margin-left: 120px;
}
.show-item {
  display: flex;
  align-content: center;
  justify-content: space-between;
  width: 500px;
  margin-left: 120px;
  padding: 0 10px;
  cursor: pointer;
  &:hover {
    background: #d9d9d9;
  }
}
::v-deep.show-item .el-icon-close {
  margin-top: 14px !important;
}
::v-deep.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  height: 70px;
  width: 70px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  &:hover {
    border-color: #409eff;
  }
}
::v-deep.avatar {
  height: 70px;
  width: 70px;
  display: block;
}
::v-deep.el-dialog {
  width: 100% !important;
  height: 100% !important;
  overflow-y: scroll !important;
}
</style>
