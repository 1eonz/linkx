<template>
  <div class="capp-set">
    <el-dialog
      :visible.sync="visible"
      :title="dialogStatus"
      :before-close="handleClose"
      :close-on-click-modal="false"
      width="800px"
    >
      <el-form
        ref="ruleForm"
        :model="form"
        :rules="rules"
        label-position="left"
        label-width="150px"
        class="demo-form"
        style="height: 600px; margin-left: 30px; overflow-y: scroll;"
      >
        <el-form-item
          :label="$t('index.list.uploadFace')"
          class="avatar-item"
          prop="imageUrl"
        >
          <el-upload
            ref="uploadImg"
            class="avatar-uploader"
            action="#"
            :show-file-list="false"
            :auto-upload="false"
            :http-request="uploadFile"
            :on-change="handleChange"
            :before-upload="beforeUpload"
            accept=".jpg,.png,.gif"
          >
            <authImg v-if="showAuthImg" class="avatar" :auth-src="imageUrl" />
            <i
              v-else-if="!imageUrl"
              class="el-icon-plus avatar-uploader-icon"
            ></i>
            <img v-else :src="imageUrl" class="avatar" />
          </el-upload>
        </el-form-item>
        <el-form-item
          :label="$t('index.list.applicationName') + '：'"
          prop="name"
        >
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item
          :label="$t('index.list.applicationType') + '：'"
          prop="applicationType"
        >
          <el-select
            v-model="form.applicationType"
            clearable
            class="filter-item"
            style="width: 373px"
            :placeholder="$t('index.operations.selects')"
          >
            <el-option
              v-for="item in applicationTypeArr"
              :key="item.value"
              :label="item.name"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('index.list.jumpLink') + '：'" prop="url">
          <el-input v-model="form.url" maxlength="50" />
        </el-form-item>
        <!-- <el-form-item :label="$t('index.list.favoriteStatus') + '：'" prop="favorite" v-if="!isAdd">
          <el-radio v-model="form.favorite" :label="1">
            {{ $t('index.operations.yes') }}
          </el-radio>
          <el-radio v-model="form.favorite" :label="0">
            {{ $t('index.operations.no') }}
          </el-radio>
        </el-form-item> -->
        <el-form-item
          :label="$t('index.list.enabledStatus') + '：'"
          prop="status"
        >
          <el-radio v-model="form.status" :label="1">
            {{ $t('index.operations.yes') }}
          </el-radio>
          <el-radio v-model="form.status" :label="0">
            {{ $t('index.operations.no') }}
          </el-radio>
        </el-form-item>
        <el-form-item :label="$t('index.list.other') + '：'">
          <div v-for="(item, index) in addList" :key="index" class="other-item">
            <el-input
              v-model="item.key"
              :placeholder="$t('index.list.pleaseEnterNameForAttribute')"
              maxlength="50"
            />
            <!-- <el-input
              v-model="item.value"
              class="edit-input"
              :type="isShowPass ? 'text' : 'password'"
              :placeholder="$t('index.operations.inputContent')"
              v-if="index === 1"
            >
              <svg
                slot="suffix"
                data-v-53ff2da0=""
                data-v-ca3cd49c=""
                class="user-avatar svg-icon"
                @click="changeShowState()"
              >
                <use
                  data-v-53ff2da0=""
                  :xlink:href="isShowPass ? '#icon-eye-open' : '#icon-eye'"
                />
              </svg>
            </el-input> -->
            <el-input
              v-model="item.value"
              :placeholder="$t('index.list.pleaseEnterValueForAttribute')"
              maxlength="50"
            />
            <el-button
              type="primary"
              icon="el-icon-circle-plus-outline"
              @click="addOther"
            >
              {{ $t('index.operations.Added') }}
              <!-- <i class="el-icon-plus avatar-uploader-icon"></i -->
            </el-button>
            <el-button
              v-if="index >= 1"
              type="danger"
              icon="el-icon-delete"
              size="normal"
              @click="removeDomain(index)"
            >
              {{ $t('index.delete') }}
            </el-button>
          </div>
        </el-form-item>
        <!-- <el-form-item label="密码：">
          <el-input v-model="form.other.password"></el-input>
          <el-button @click.prevent="addOther">
            <i class="el-icon-plus avatar-uploader-icon"></i
          ></el-button>
        </el-form-item> -->
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
  </div>
</template>

<script>
import { deepCopy } from '@/utils'
import { addCapp, updateCapp } from '@/api/resource/capp'
import { uploadTmp } from '@/api/resource/person'
import authImg from '@/components/AuthImg'

const form = {
  id: '',
  name: '',
  imageUrl: '',
  applicationType: null,
  url: '',
  favorite: 0,
  status: 0,
  other: {}
}
// const addList = [{ key: '用户名', value: '' }, { key: '密码', value: '' }]
const addList = [{ key: '', value: '' }]
export default {
  name: 'Capp',
  components: {
    authImg
  },
  data() {
    return {
      visible: false,
      showAuthImg: false,
      isAdd: true,
      dialogStatus: '',
      form: deepCopy(form),
      imageValid: true,
      btnLoading: false,
      imageUrl: '',
      isShowPass: false,
      addList: deepCopy(addList), // 新增自定义属性
      applicationTypeArr: [
        { value: 0, name: 'apk' },
        { value: 1, name: 'H5' },
        { value: 2, name: 'local' }
      ],
      selectedArr: [{ value: 0, name: '否' }, { value: 1, name: '是' }],
      rules: {
        name: [
          {
            required: true,
            message: this.$t('index.list.inputAppName'),
            trigger: 'blur'
          },
          {
            min: 1,
            max: 24,
            message: this.$t('index.list.lengthRange'),
            trigger: 'blur'
          }
        ],
        imageUrl: [
          {
            required: true,
            message: this.$t('mapConfig.uploadIcon'),
            trigger: 'blur'
          }
        ],
        applicationType: [
          {
            required: true,
            message: this.$t('index.list.selectAppType'),
            trigger: 'blur'
          }
        ],
        url: [
          {
            required: true,
            message: this.$t('index.list.inputJumpLink'),
            trigger: 'blur'
          }
        ]
      }
    }
  },
  methods: {
    changeShowState() {
      this.isShowPass = !this.isShowPass
    },
    addOther() {
      if (this.addList?.length >= 10) {
        this.$message({
          message: this.$t('index.list.maxAddOtherData'),
          type: 'warning'
        })
        return
      }
      this.addList.push({
        key: '',
        value: ''
      })
    },
    // 删除自定义属性
    removeDomain(index) {
      this.$confirm(this.$t('index.operations.affirmDeleted'), {
        confirmButtonText: this.$t('index.determine'),
        cancelButtonText: this.$t('index.cancel'),
        type: 'info'
      }).then(() => {
        if (index !== -1) {
          this.addList.splice(index, 1)
        }
      })
    },
    // 新增
    add() {
      this.dialogStatus = this.$t('index.operations.Added')
      this.visible = true
    },

    // 修改
    async modify(row) {
      this.dialogStatus = this.$t('index.operations.redact')
      this.isAdd = false
      try {
        const other = JSON.parse(row.other)
        const arr = []
        for (const i in other) {
          arr.push({
            key: i,
            value: other[i]
          })
        }
        if (arr.length > 0) {
          this.addList = arr
        }
      } catch {}
      Object.assign(this.form, row)
      // delete this.form.password
      // delete this.form.sexName
      if (row.imageUrl) {
        this.showAuthImg = true
        this.imageUrl = row.imageUrl
      }

      this.visible = true
    },
    // 关闭
    handleClose() {
      this.form = deepCopy(form)
      this.addList = deepCopy(addList)
      this.$refs['ruleForm'].resetFields()
      this.visible = false
      this.isAdd = true
      this.isShow = false
      this.showAuthImg = false
      this.imageUrl = ''
    },
    // 点击确定
    async handleSubmit(isAdd) {
      this.$refs.uploadImg.submit()

      setTimeout(() => {
        this.handleEdit(isAdd)
      }, 500)
    },
    async handleEdit(isAdd) {
      this.$refs['ruleForm'].validate(valid => {
        if (valid && this.imageValid) {
          if (isAdd) {
            delete this.form.id
          }
          const arr = {}
          this.addList.map(item => {
            arr[item.key] = item.value
          })
          this.form.other = JSON.stringify(arr)
          const param = JSON.parse(JSON.stringify(this.form))
          const api = isAdd ? addCapp : updateCapp
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
    },
    // 图片上传
    async uploadFile(file) {
      const formData = new FormData()
      formData.append('file', file.file)
      await uploadTmp(formData)
        .then(result => {
          const { code, data } = result
          if (code === 0) {
            this.form.imageUrl = Array.isArray(data) && data?.length > 0 ? data[0] : data
          } else {
            this.$message({
              message: this.$t('index.messageText.uploadFailed'),
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
    },
    handleChange(file) {
      if (file.raw) {
        this.showAuthImg = false
      }
      this.imageUrl = URL.createObjectURL(file.raw)
    },

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
      this.imageValid = isJPG && isLt5KB && !isLonger && !hasNull
      return this.imageValid
    }
  }
}
</script>

<style lang="scss" scoped>
.capp-set {
  width: 100%;
  height: 100%;
  padding: 20px;
  box-sizing: border-box;
}
.user-avatar {
  cursor: pointer;
  width: 15px;
  height: 15px;
  margin-top: 10px;
}
.avatar-item {
  margin-top: 20px;
  ::v-deep.el-form-item__content {
    margin-left: 0 !important;
  }
}
.other-item {
  margin-bottom: 20px;
}
::v-deep.avatar-uploader-icon {
  padding: 0 7px !important;
}
::v-deep.el-button {
  margin-left: 10px;
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
::v-deep.el-form-item {
  margin-right: 20px;
}
::v-deep.el-input {
  width: calc(100% - 170px) !important;
}
::v-deep.other-item > .el-input {
  &:first-child {
    margin-right: 10px;
  }
  width: calc(100% - 363px) !important;
}
::v-deep.other-item > .el-button {
  padding: 11px 9px !important;
}
</style>
