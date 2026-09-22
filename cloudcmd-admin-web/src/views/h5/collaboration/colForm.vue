<template>
  <div>
    <el-dialog
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
        <el-form-item label="协同岗名称" prop="postName">
          <el-input
            v-model="formData.postName"
            maxlength="20"
            show-word-limit
            placeholder="请输入协同岗名称"
          />
        </el-form-item>
        <el-form-item label="协同岗类型" prop="type">
          <el-select
            v-if="dialogVisible"
            v-model="formData.type"
            style="width: 100%;"
            reserve-keyword
            placeholder="请选择协同岗类型"
            :disabled="formType === 'update'"
            @change="changeType"
          >
            <el-option
              v-for="item in collarationArr"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="图标" prop="iconUrl" class="avatar-item">
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
            <div v-if="showAuthImg || imageUrl" class="delete">
              <i class="el-icon-delete" @click.stop="handleRemove"></i>
            </div>
          </el-upload>
        </el-form-item>
        <el-form-item label="归属组织" prop="orgName" style="width: 100%;">
          <DepartmentSelect
            v-model="formData.orgName"
            :department-code="departmentCode"
            @change="handleChangeOrg"
            @clear="clearOrganizationType"
          />
        </el-form-item>
        <el-form-item label="关联人员" prop="relatedUserIds">
          <RelatedUserSelect
            :orgId="formData.orgId"
            :type="formData.type"
            :multipleCollaboration="multipleCollaboration"
            v-model="formData.relatedUserIds"
            :labels.sync="formData.relatedUserNames"
            :initUserIds="initUserIds"
            :disabled="isRelatedUserIdsDisabled"
          />
        </el-form-item>
        <!-- 人员核查类型不显示该字段 -->
        <el-form-item v-if="formData.type !== 1" label="警单类型" prop="typeIds">
          <el-select
            v-if="dialogVisible"
            ref="typeSelectRef"
            v-model="formData.typeIds"
            style="width: 100%;"
            multiple
            reserve-keyword
            placeholder="请选择警单类型"
            :remote-method="remoteMethodType"
            :loading="loadingType"
            @change="handleChangeType"
            @visible-change="handleTypeSelectVisibleChange"
          >
            <el-option
              v-for="item in typeList"
              :key="item.id"
              :label="item.tag"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeDialog">取 消</el-button>
        <el-button type="primary" :disabled="formLoading" @click="handleSubmit">
          确 定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  createCollaboration,
  updateCollaboration,
  uploadColTmp
} from '@/api/h5/collaboration'
import authImg from '@/components/AuthImg'
import { deepCopy } from '@/utils'
import { getPolicetickettypes } from '@/api/policeReport/dock'
import { getGlobalsList } from '@/api/dictionary/globals'
import _ from 'lodash'
import RelatedUserSelect from "./components/RelatedUserSelect/index.vue"
import DepartmentSelect from './components/DepartmentSelect/index.vue'

const form = {
  postName: undefined,
  iconUrl: '',
  fileId: '',
  orgId: undefined,
  orgCode: undefined,
  orgName: undefined,
  relatedUserIds: [],
  relatedUserNames: [],
  recordType: undefined,
  type: 0,
  typeIds: []
}

export default {
  name: 'ColForm',
  components: { authImg, RelatedUserSelect, DepartmentSelect },
  props: {
    isAdmin: {
      type: Boolean,
      default: false
    },
    departmentCode: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      dialogVisible: false,
      dialogTitle: this.$t('index.operations.Added'),
      formLoading: false,
      loading: false,
      loadingType: false,
      formType: '',
      formData: deepCopy(form),
      imageUrl: '',
      imageValid: true,
      showAuthImg: false,
      changeImg: false,
      currentFile: null, // 当前选择的文件
      isSubmitting: false, // 是否正在提交，防止重复提交
      initUserIds: [],
      typeList: [],
      orgList: [],
      formRules: {
        postName: [
          { required: true, message: '协同岗名称不能为空', trigger: 'blur' }
        ],
        type: [
          { required: true, message: '协同岗类型不能为空', trigger: 'blur' }
        ],
        orgName: [
          { required: true, message: '归属组织不能为空', trigger: 'change' }
        ],
        relatedUserIds: [
          { required: true, message: '关联人员不能为空', trigger: 'change' }
        ]
      },
      collaration: [
        { id: 0, name: '普通协同岗' },
        { id: 1, name: '人员核查协同岗' }
      ],
      globalData: {},
      // ======== 新增修复：两个select的ref引用 ========
      typeSelectRef: null,
      // 警单类型搜索备份，用于远程搜索
      originTypeList: []
    }
  },
  computed: {
    collarationArr() {
      if (this.$store.state?.user?.licenseAuth?.AICollaborationAuth) {
        return this.collaration.filter(item => item.name !== '人员核查协同岗')
      }
      return this.collaration
    },
    multipleCollaboration() {
      return this.globalData?.value === 'true'
    },
    isRelatedUserIdsDisabled() {
      return !this.formData.orgId || this.formData.orgId === ''
    }
  },
  watch: {
    dialogVisible() {
      if (!this.dialogVisible) {
        this.$refs['formRef']?.resetFields()
      }
    }
  },
  mounted() {
    this.getPolicetickettypesFunc()
    this.getGlobal()
  },
  methods: {
    async getGlobal() {
      const { code, data } = await getGlobalsList()
      if (code === 0) {
        const index = data.findIndex(
          item => item.name === 'MULTIPLE_COLLABORATION'
        )
        this.globalData = data[index] || {}
      }
    },
    changeType() {
      this.formData.relatedUserNames = undefined
      this.formData.relatedUserIds = undefined
    },
    // 获取警单列表 - 优化：备份原始全量数据用于搜索
    async getPolicetickettypesFunc() {
      const res = await getPolicetickettypes()
      if (res.code === 0) {
        this.typeList = res.data
        this.originTypeList = res.data // 备份全量警单类型数据
      }
    },
    // 打开弹窗
    async open(type, row) {
      this.dialogTitle = type === 'create' ? '新增' : '修改'
      this.formType = type
      this.formLoading = true
      this.resetForm()
      if (row) {
        this.formData = { ...row }
        const { relatedUserNames, relatedUserIds, iconUrl } = row
        if (Array.isArray(relatedUserIds)) {
          this.formData.relatedUserNames = relatedUserNames
          this.formData.relatedUserIds = relatedUserIds
          this.initUserIds = relatedUserIds
        } else {
          this.formData.relatedUserNames = relatedUserNames?.split(',')
          this.formData.relatedUserIds = relatedUserIds?.split(',')
          this.initUserIds = relatedUserIds?.split(',')
        }
        this.imageUrl = iconUrl
        this.showAuthImg = true
        this.formLoading = false
      } else {
        this.formLoading = false
      }
      this.dialogVisible = true
    },

    // 提交请求
    async handleRequest() {
      this.formLoading = true
      const params = { ...this.formData }
      const { relatedUserNames, relatedUserIds } = this.formData
      params.relatedUserNames = relatedUserNames.join(',')
      params.relatedUserIds = relatedUserIds.join(',')
      if (this.formType === 'create') {
        const { code, msg } = await createCollaboration(params)
        this.$message({
          message: code === 0 ? '新增成功' : msg,
          type: code === 0 ? 'success' : 'error'
        })
        this.handleVisible(code)
      } else {
        const { code, msg } = await updateCollaboration(params)
        this.$message({
          message: code === 0 ? '修改成功' : msg,
          type: code === 0 ? 'success' : 'error'
        })
        this.handleVisible(code)
      }
    },

    handleVisible(code) {
      if (code === 0) {
        this.dialogVisible = false
        this.$emit('success')
      } else {
        this.dialogVisible = true
      }
      this.formLoading = false
    },
    handleChangeOrg(obj) {
      const { code, name, id } = obj
      this.formData = Object.assign({}, this.formData, {
        orgCode: code,
        orgId: id,
        orgName: name
      })
      this.$refs.formRef.clearValidate()
      this.formData.relatedUserNames = undefined
      this.formData.relatedUserIds = undefined
    },

    handleChangeType(data) {
      console.log(data, '=====data')
    },

    // ======== 修复+完善：警单类型远程搜索逻辑（你原代码为空） ========
    remoteMethodType(keywords) {
      this.loadingType = true
      if (!keywords || keywords.trim() === '') {
        // 空关键词 加载全量数据
        this.typeList = this.originTypeList
        this.loadingType = false
        return
      }
      // 关键词过滤警单类型
      const filterData = this.originTypeList.filter(item => {
        return (
          item.tag && item.tag.toLowerCase().includes(keywords.toLowerCase())
        )
      })
      this.typeList = filterData
      this.loadingType = false
    },

    // 点击确定
    handleSubmit: _.debounce(function() {
      if (this.changeImg) {
        // 防止重复提交
        if (this.isSubmitting) {
          return
        }
        if (this.currentFile && this.currentFile.raw) {
          this.uploadFile({ file: this.currentFile.raw })
        } else {
          const fileList = this.$refs.upload?.fileList || []
          if (fileList.length > 0) {
            const lastFile = fileList[fileList.length - 1]
            if (lastFile && lastFile.raw) {
              this.uploadFile({ file: lastFile.raw })
            }
          }
        }
      } else {
        this.submitForm()
      }
    }, 500),

    // 校验表单
    async submitForm() {
      if (!this.imageValid) {
        return
      }
      this.$refs['formRef'].validate(valid => {
        if (valid) {
          this.handleRequest()
        } else {
          return false
        }
      })
    },

    closeDialog() {
      this.dialogVisible = false
    },

    /** 重置表单 */
    resetForm() {
      this.formData = deepCopy(form)
      this.imageUrl = ''
      this.imageValid = true
      this.showAuthImg = false
      this.changeImg = false
      this.currentFile = null
      this.isSubmitting = false
      this.initUserIds = []
      this.orgList = []
    },
    // 图片上传
    async uploadFile(file) {
      if (this.isSubmitting) {
        return
      }
      const rawFile = file?.file
      if (!rawFile) return
      // 提交前再次校验：图标大小不超过 1M
      if (rawFile.size / 1024 / 1024 > 1) {
        this.$message.error('图标大小不能超过 1M')
        return
      }
      this.isSubmitting = true
      const formData = new FormData()
      formData.append('file', rawFile)
      try {
        const result = await uploadColTmp(formData)
        const { code, data } = result
        if (code === 0) {
          this.formData = Object.assign({}, this.formData, {
            iconUrl: data.iconUrl,
            fileId: data.fileId
          })
          this.isSubmitting = false
          this.changeImg = false
          this.submitForm()
        } else {
          this.isSubmitting = false
          this.$message({
            message: '图标上传失败',
            type: 'error'
          })
        }
      } catch (error) {
        this.isSubmitting = false
        this.$message({
          message: '图标上传失败，图片资源错误',
          type: 'error'
        })
      }
    },

    handleChange(file) {
      if (!file || !file.raw) return
      // 限制图标上传图片大小不超过 1M
      const isLt1M = file.raw.size / 1024 / 1024 <= 1
      if (!isLt1M) {
        this.$message.error('图标大小不能超过 1M')
        this.$refs.upload?.clearFiles()
        this.currentFile = null
        return
      }
      const index = file.name.lastIndexOf('.')
      const type = (file.name.substring(index + 1) || '').toLowerCase()
      const typeData = ['jpg', 'png', 'gif']
      if (!typeData.includes(type)) {
        this.$message.error('图片只能是 jpg/png/gif 格式!')
        this.$refs.upload?.clearFiles()
        this.currentFile = null
        return
      }
      this.currentFile = file
      this.$nextTick(() => {
        if (this.$refs.upload && this.$refs.upload.fileList) {
          if (this.$refs.upload.fileList.length > 1) {
            this.$refs.upload.clearFiles()
            if (file && file.raw) {
              this.$refs.upload.handleStart(file.raw)
            }
          }
        }
      })
      this.changeImg = true
      this.showAuthImg = false
      this.imageUrl = URL.createObjectURL(file.raw)
      this.formLoading = false
    },
    handleRemove() {
      this.$refs.upload.clearFiles()
      this.imageUrl = ''
      this.imageValid = true
      this.showAuthImg = false
      this.formData.iconUrl = ''
      this.formData.fileId = ''
      this.formLoading = false
      this.changeImg = false
      this.currentFile = null
    },
    beforeUpload(file) {
      const { name, size } = file
      const index = name.lastIndexOf('.')
      const type = name.substring(index + 1).toLowerCase()
      const typeData = ['jpg', 'png', 'gif']
      const isJPG = typeData.includes(type)
      const isLt1M = size / 1024 / 1024 < 1
      if (!isJPG) {
        this.$message.error('图片只能是jpg/png/gif格式!')
        this.formLoading = true
        return false
      }
      if (!isLt1M) {
        this.$message.error('图片大小不能超过1M!')
        this.formLoading = true
        return false
      }
      this.formLoading = false
      return true
    },

    clearOrganizationType() {
      this.formData.orgId = undefined
      this.formData.orgCode = undefined
      this.formData.orgName = undefined
      this.formData.relatedUserNames = undefined
      this.formData.relatedUserIds = undefined
    },

    // ======== 核心修复1：警单类型select下拉展开/收起事件 ========
    async handleTypeSelectVisibleChange(isOpen) {
      if (isOpen) {
        // 下拉框打开时，获取输入框真实关键词
        const currentInputVal = this.typeSelectRef?.selectedLabel || ''
        // 关键词为空 → 主动加载全量警单类型
        if (currentInputVal.trim() === '') {
          this.remoteMethodType('')
        }
      }
    }
  }
}
</script>
<style scoped lang="scss">
.avatar-item {
  margin-top: 22px;

  ::v-deep.el-form-item__content {
    margin-left: 0 !important;
  }
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
    .delete {
      opacity: 1;
      background: rgba(127, 127, 127, 0.4);
    }
  }
}

::v-deep.avatar {
  height: 70px;
  width: 70px;
  display: block;
}
.delete {
  position: absolute;
  width: 70px;
  height: 70px;
  opacity: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  .el-icon-delete {
    color: #fff;
  }
}
</style>
