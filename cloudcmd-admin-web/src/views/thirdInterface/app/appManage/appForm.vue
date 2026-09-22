<template>
  <el-dialog
    v-if="dialogVisible"
    :visible.sync="dialogVisible"
    :title="dialogTitle"
    width="750px"
    custom-class="adaptive-dialog"
    @close="closeDialog"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="formRules"
      label-position="left"
      label-width="200px"
      class="dialog-form"
    >
      <el-form-item label="应用图标" prop="icon" class="avatar-item">
        <el-upload
          ref="upload"
          class="avatar-uploader"
          action="#"
          :show-file-list="false"
          :auto-upload="false"
          :http-request="uploadFile"
          :on-change="handleChange"
          :before-upload="beforeUpload"
          accept=".jpg,.png,.gif,.JPG,.PNG,.GIF"
        >
          <authImg v-if="showAuthImg" class="avatar" :auth-src="imageUrl" />
          <i
            v-else-if="!imageUrl"
            class="el-icon-plus avatar-uploader-icon"
          ></i>
          <img v-else :src="imageUrl" class="avatar" />
        </el-upload>
      </el-form-item>
      <el-form-item label="应用名称" prop="name">
        <el-input
          v-model="formData.name"
          maxlength="8"
          :disabled="disableName"
          placeholder="请输入应用名称"
        />
      </el-form-item>
      <el-form-item label="应用类型" prop="type" @change="changeAppType">
        <el-select
          v-model="formData.type"
          placeholder="请选择应用类型"
          style="width: 100%;"
        >
          <el-option
            v-for="item in appTypeList"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      
      <el-form-item label="应用展示范围" prop="scope">
        <el-checkbox-group v-model="formData.scope">
          <el-checkbox
            v-for="item in scopeOptions"
            :key="item.value"
            :label="item.value"
          >{{ item.label }}</el-checkbox>
        </el-checkbox-group>
      </el-form-item>
      
      <el-form-item v-if="formData.type !== 3" label="业务区域" prop="zone">
        <el-select
          v-model="formData.zone"
          placeholder="请选择业务区域"
          style="width: 100%;"
          @change="changeZone"
        >
          <el-option
            v-for="item in appZoneList"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item v-if="isShowPreApp" label="前置应用" prop="prerequisite">
        <el-select
          v-model="formData.prerequisite"
          clearable
          placeholder="请选择前置应用"
          style="width: 100%;"
        >
          <el-option
            v-for="app in preAppList"
            :key="app.id"
            :label="app.name"
            :value="app.id"
          />
        </el-select>
      </el-form-item>
      <el-tooltip
        :content="
          `提示: ${urlLabel}在Mobile，Tablet，PC 上显示的链接使用;符号隔开`
        "
        placement="top"
      >
        <el-form-item v-if="formData.type === 0" :label="urlLabel" prop="url">
          <el-input
            v-model="formData.url"
            maxlength="500"
            :disabled="formData.official === 1"
            :placeholder="
              `请输入${urlLabel},多个${urlLabel}在Mobile，Tablet，PC 上显示的链接使用;符号隔开`
            "
          />
        </el-form-item>
      </el-tooltip>
      <el-form-item
      v-if="formData.type !== 0"
        label="android应用包名"
        prop="packageAndroid"
      >
        <el-input
          v-model="formData.packageAndroid"
          maxlength="500"
          :placeholder="`请输入android应用包名`"
        />
      </el-form-item>
      <el-form-item
      v-if="formData.type !== 0"
        label="HarmonyOS应用包名"
        prop="packageHm"
      >
        <el-input
          v-model="formData.packageHm"
          maxlength="500"
          :placeholder="`请输入HarmonyOS应用包名`"
          @input="onPairFieldChange"
        />
      </el-form-item>
      <el-form-item v-if="formData.type !== 0" label="Ability名称" prop="activity">
        <el-input
          v-model="formData.activity"
          maxlength="500"
          :placeholder="`请输入Ability名称`"
          @input="onPairFieldChange"
        />
      </el-form-item>
      <el-form-item label="应用ID" prop="appId">
        <el-input
          v-model="formData.appId"
          maxlength="64"
          placeholder="请输入应用ID"
        />
      </el-form-item>
      <el-form-item label="跳转参数" prop="params">
        <el-input
          v-model="formData.params"
          type="textarea"
          placeholder="请输入跳转参数（格式：name=李四&id=123456）"
        />
      </el-form-item>
      <el-form-item label="排序值" prop="sort">
        <el-input-number
          v-model="formData.sort"
          :min="0"
          placeholder="请输入排序值"
          controls-position="right"
        />
      </el-form-item>
      <el-form-item label="上架" prop="status">
        <el-radio-group v-model="formData.status">
          <el-radio
            v-for="item in appStatusList"
            :key="item.value"
            :label="item.value"
            >{{ item.label }}</el-radio>
        </el-radio-group>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取 消</el-button>
      <el-button
        type="primary"
      :disabled="formLoading"
      @click="handleSubmit"
      >确 定</el-button>
    </template>
  </el-dialog>
</template>

<script>
import {
  getInfo,
  createInfo,
  updateInfo,
  getPrerequisiteList
} from '@/api/h5/app'
import { uploadTmp } from '@/api/resource/person'
import authImg from '@/components/AuthImg'
import { cloneDeep } from 'lodash'

const form = {
  id: undefined,
  appId: undefined,
  name: undefined,
  url: undefined,
  icon: undefined,
  sort: undefined,
  status: 0,
  type: 0,
  zone: 2,
  prerequisite: undefined,
  packageAndroid: undefined,
  packageHm: undefined,
  activity: undefined,
  scope: [1, 2, 4, 8]
}

export default {
  name: 'AppInfoForm',
  components: { authImg },
  data() {
    return {
      dialogVisible: false,
      dialogTitle: this.$t('index.operations.Added'),
      formLoading: false,
      formType: '',
      formData: cloneDeep(form),
      imageUrl: '',
      imageValid: true,
      showAuthImg: false,
      changeImg: false,
      appTypeList: [
        { label: 'H5 应用', value: 0 },
        { label: 'App 应用', value: 1 },
        { label: '前置应用', value: 3 }
      ],
      appZoneList: [
        { label: '一类区', value: 1 },
        { label: '二类区', value: 2 },
        { label: '三类区', value: 3 }
      ],
      appStatusList: [{ label: '上架', value: 0 }, { label: '下架', value: 1 }],
      preAppList: [],
      scopeOptions: [
        { label: '鸿蒙移动端', value: 1 },
        { label: '安卓移动端', value: 2 },
        { label: 'PC 浏览器', value: 4 },
        { label: 'PC 桌面端', value: 8 }
      ]
    }
  },
  computed: {
    disableName() {
      return (
        this.formData.name === '设备调度' || this.formData.name === '位置共享'
      )
    },
    isShowPreApp() {
      // 显示条件： [业务区域：三类区] && [应用类型：非前置应用]
      return this.formData.zone === 3 && this.formData.type !== 3
    },
    urlLabel() {
      // 根据应用类型动态生成label文本
      return { 0: '应用链接', 1: '包名', 3: '包名' }[this.formData.type]
    },
    formRules() {
      return {
        name: [{ required: true, message: '应用名称不能为空', trigger: 'blur' }],
        url: [
          {
            required: !this.disableName,
            trigger: 'blur',
            validator: (_, value, callback) => {
              if (!value) {
                callback(new Error(this.urlLabel + '不能为空'))
              } else if (this.formData.type !== 1) {
                const semicolonCount = (value.match(/;/g) || []).length
                if (semicolonCount === 0) {
                  callback()
                } else if (semicolonCount === 2) {
                  const urls = value.split(';')
                  // 前两个url必填（移动端、平板），第三个可选（PC）
                  if (!urls[0].trim() || !urls[1].trim()) {
                    callback(new Error('url格式错误：移动端和平板url不能为空'))
                  } else {
                    callback()
                  }
                } else if (semicolonCount === 1) {
                  callback(
                    new Error('url格式错误：分号数量不正确，应为0个或2个')
                  )
                } else {
                  callback(
                    new Error('url格式错误：分号数量超出限制，最多支持2个分号（3个url）')
                  )
                }
              } else {
                callback()
              }
            }
          }
        ],
        scope: [{ required: true, type: 'array', message: '应用展示范围不能为空', trigger: 'change' }],
        icon: [{ required: true, message: '应用图标不能为空', trigger: 'change' }],
        sort: [{ required: true, message: '排序值不能为空', trigger: 'blur' }],
        type: [{ required: true, message: '应用类型不能为空', trigger: 'change' }],
        zone: [{ required: true, message: '业务区域不能为空', trigger: 'change' }],
        packageAndroid: [{ required: true, message: 'android应用包名不能为空', trigger: 'blur' }],
        packageHm: [
          {
            required: false,
            trigger: 'blur',
            validator: (_, value, callback) => {
              const hasPackage = value && value.trim() !== ''
              const hasActivity = this.formData.activity && this.formData.activity.trim() !== ''
              if (hasPackage && !hasActivity) {
                callback(new Error('填写了应用包名，请同时填写Activity名称'))
              } else if (!hasPackage && hasActivity) {
                callback(new Error('请填写应用包名'))
              } else {
                callback()
              }
            }
          }
        ],
        activity: [
          {
            required: false,
            trigger: 'blur',
            validator: (_, value, callback) => {
              const hasPackage = this.formData.packageHm && this.formData.packageHm.trim() !== ''
              const hasActivity = value && value.trim() !== ''
              if (hasActivity && !hasPackage) {
                callback(new Error('填写了Activity名称，请同时填写应用包名'))
              } else if (!hasActivity && hasPackage) {
                callback(new Error('请填写Activity名称'))
              } else {
                callback()
              }
            }
          }
        ]
      }
    }
  },
  watch: {
    dialogVisible() {
      if (!this.dialogVisible) {
        // 关闭弹窗时同步清空校验状态与数据，避免下次打开残留错误提示
        this.$nextTick(() => {
          if (this.$refs.formRef) {
            this.$refs.formRef.resetFields()
            this.$refs.formRef.clearValidate()
          }
          this.resetForm()
        })
      }
    },
    'formData.name'() {
      if (
        this.formData.name === '设备调度' ||
        this.formData.name === '位置共享'
      ) {
        this.formRules.url = undefined
      } else {
        this.formRules.url = [
          {
            required: true,
            trigger: 'blur',
            validator: (_, value, callback) => {
              if (!value) {
                callback(new Error(this.urlLabel + '不能为空'))
              } else if (this.formData.type !== 1) {
                const semicolonCount = (value.match(/;/g) || []).length
                if (semicolonCount === 0) {
                  callback()
                } else if (semicolonCount === 2) {
                  const urls = value.split(';')
                  // 前两个url必填（移动端、平板），第三个可选（PC）
                  if (!urls[0].trim() || !urls[1].trim()) {
                    callback(new Error('url格式错误：移动端和平板url不能为空'))
                  } else {
                    callback()
                  }
                } else if (semicolonCount === 1) {
                  callback(
                    new Error('url格式错误：分号数量不正确，应为0个或2个')
                  )
                } else {
                  callback(
                    new Error('url格式错误：分号数量超出限制，最多支持2个分号（3个url）')
                  )
                }
              } else {
                callback()
              }
            }
          }
        ]
      }
    }
  },
  mounted() {
    this.getPrerequisiteApps()
  },
  methods: {
    // 校验鸿蒙包名
    onPairFieldChange() {
      if (!this.$refs.formRef) return
    this.$refs.formRef?.validateField(['activity','packageHm'])
    },
    // 获取前置应用列表
    async getPrerequisiteApps() {
      try {
        const res = await getPrerequisiteList({})
        this.preAppList = res?.data?.records || []
      } catch (error) {
        console.error('获取前置应用列表失败:', error)
      }
    },
    // 选应用类型：值为前置应用，清空前置应用，重置业务区域
    changeAppType(type) {
      if (type === 3) {
        this.formData.prerequisite = ''
        this.formData.zone = ''
      }
    },
    // 选业务区域：值为非三类区，清空前置应用
    changeZone(zone) {
      if (zone !== 3) {
        this.formData.prerequisite = ''
      }
    },
    // 打开弹窗
    async open(type, id) {
      this.dialogVisible = true
      this.dialogTitle = type === 'create' ? '新增' : '修改'
      this.formType = type
      this.resetForm()
      // 每次打开弹窗时都调用getPrerequisiteApps获取最新的前置应用列表
      this.getPrerequisiteApps()
      if (id) {
        this.formLoading = true
        try {
          const { data } = await getInfo(id)
          console.log('data', data)
          this.formData = data || {}
          this.$set(this.formData, 'scope', data.scopeList || [])
          this.showAuthImg = true
          this.imageUrl = data?.icon
        } finally {
          this.formLoading = false
        }
      } else {
        this.formLoading = false
      }
    },

    // 提交请求
    async handleRequest() {
      this.formLoading = true
      try {
        const data = this.formData
        if (this.formType === 'create') {
          const { code, msg } = await createInfo(data)
          this.$message({
            message: code === 0 ? '新增成功' : msg,
            type: code === 0 ? 'success' : 'error'
          })
        } else {
          const { code, msg } = await updateInfo(data)
          this.$message({
            message: code === 0 ? '修改成功' : msg,
            type: code === 0 ? 'success' : 'error'
          })
        }
        this.dialogVisible = false
        this.$emit('success')
      } finally {
        this.dialogVisible = false
      }
    },

    // 点击确定
    async handleSubmit() {
      this.submitForm()
    },

    // 校验表单
    async submitForm() {
      if (!this.imageValid) {
        return
      }
      this.$refs['formRef'].validate(valid => {
        if (valid) {
          this.handleRequest()
        } else {
          this.$message({
            message: this.$t('index.statusTitle.requiredFieldIsEmpty'),
            type: 'error'
          })
          return false
        }
      })
    },

    closeDialog() {
      this.dialogVisible = false
    },

    /** 重置表单 */
    resetForm() {
      this.formData = cloneDeep(form)
      this.imageUrl = ''
      this.imageValid = true
      this.showAuthImg = false
      this.changeImg = false
    },
    // 图片上传
    async uploadFile(file) {
      const formData = new FormData()
      formData.append('file', file.file)
      await uploadTmp(formData)
        .then(result => {
          const { code, data } = result
          if (code === 0) {
            this.formData.icon =
              Array.isArray(data) && data?.length > 0 ? data[0] : data
            this.$refs.formRef.validateField('icon')
          } else {
            this.$message({
              message: '图标上传失败',
              type: 'error'
            })
          }
        })
        .catch(() => {
          this.$message({
            message: '图标上传失败，图片资源错误',
            type: 'error'
          })
        })
    },
    handleChange(file) {
      this.changeImg = true
      if (file.raw) {
        this.showAuthImg = false
      }
      this.imageUrl = URL.createObjectURL(file.raw)
      this.formLoading = false
      this.$refs.upload.submit()
    },

    beforeUpload(file) {
      const { name, size } = file
      const index = name.lastIndexOf('.')
      const type = name.substring(index + 1).toLowerCase()
      const typeData = ['jpg', 'png', 'gif']
      const isJPG = typeData.includes(type)
      const isLt5M = size / 1024 / 1024 < 5
      if (!isJPG) {
        this.$message.error('图片只能是jpg/png/gif格式!')
        this.formLoading = true
        return false
      }
      if (!isLt5M) {
        this.$message.error('图片大小不能超过5M!')
        this.formLoading = true
        return false
      }
      this.formLoading = false
      return true
    }
  }
}
</script>

<style scoped lang="scss">
.avatar-item {
  margin-top: 20px;
  position: relative;

  ::v-deep.el-form-item__content {
    margin-left: 0 !important;
  }
  ::v-deep .el-form-item__error {
    position: absolute;
    left: 200px;
  }
}

::v-deep .el-dialog{
  display: flex;
  flex-direction: column;
  margin:0 !important;
  position:absolute;
  top:50%;
  left:50%;
  transform:translate(-50%,-50%);
  max-height:calc(100% - 120px);
  max-width:calc(100% - 30px);
}
::v-deep  .el-dialog .el-dialog__body{
  flex:1;
  overflow: auto;
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

::v-deep.el-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

::v-deep.el-checkbox {
  margin-right: 0;
}
</style>
