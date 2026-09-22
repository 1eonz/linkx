<template>
  <div>
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogVisible"
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
        <el-form-item label="轮播图" prop="pciUrl" class="avatar-item">
          <el-upload
            ref="upload"
            class="avatar-uploader"
            action="#"
            :file-list="[{ url: imageUrl }]"
            :show-file-list="false"
            :auto-upload="false"
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
        <el-form-item label="公众号" prop="officialAccountName">
          <el-select
            v-model="formData.officialAccountId"
            placeholder="请选择"
            style="width: 100%;"
            @change="changeOfficialAccount(formData.officialAccountId)"
            clearable
          >
            <el-option
              v-for="item in officialAccountList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-select
            v-if="formData.officialAccountId"
            v-model="formData.articleId"
            v-loadmore="handleScroll"
            filterable
            placeholder="请选择"
            style="width: 100%;"
            @change="changeTitle()"
          >
            <el-option
              v-for="item in titleList"
              :key="item.id"
              :label="item.title"
              :value="item.id"
            />
          </el-select>
           <el-input
            v-else
            v-model="formData.title"
            maxlength="128"
            placeholder="请输入标题"
          />
        </el-form-item>
        <el-form-item label="跳转链接" prop="url">
          <el-input
            v-model="formData.url"
            maxlength="128"
            placeholder="请输入链接地址"
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
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :disabled="formLoading" @click="handleSubmit">
          确 定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import {
  getCarousel,
  updateCarousel,
  createCarousel,
  officialAccountsSelect,
  getArticleList
} from '@/api/h5/carousel'
import { uploadTmp } from '@/api/resource/person'
import authImg from '@/components/AuthImg'
import { getGlobalsList } from '@/api/dictionary/globals'
import { deepCopy } from '@/utils'

const form = {
  id: undefined,
  title: undefined,
  pciUrl: undefined,
  officialAccountId: undefined,
  officialAccountName: undefined,
  articleId: '',
  url: undefined,
  sort: undefined
}

export default {
  name: 'CarouselForm',
  components: { authImg },
  data() {
    return {
      dialogVisible: false,
      dialogTitle: this.$t('index.operations.Added'),
      formLoading: false,
      formType: '',
      formData: deepCopy(form),
      loading: false,
      officialAccountList: [],
      list: [],
      titleList: [],
      imageUrl: '',
      imageValid: true,
      showAuthImg: false,
      changeImg: false,
      envurl: '',
      formRules: {
        pciUrl: [
          { required: true, message: '轮播图不能为空', trigger: 'change' }
        ],
        // officialAccountName: [{ required: true, message: '请选择公众号', trigger: 'blur' }],
        // title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
        // url: [{ required: true, message: '跳转链接不能为空', trigger: 'blur' }],
        sort: [{ required: true, message: '排序值不能为空', trigger: 'blur' }]
      },
      file: null,
      officialAccountParams: {
        pageNum: 1,
        pageSize: 999
      },
      titlePage: {
        pageNum: 1,
        pageSize: 20,
        totalCount: 0,
        officialAccountId: ''
      }
    }
  },
  watch: {
    dialogVisible() {
      if (!this.dialogVisible) {
        this.$refs['formRef'].resetFields()
      }
    }
  },
  mounted() {
    officialAccountsSelect(this.officialAccountParams).then(res => {
      this.officialAccountList = res.data.records
    })
    getGlobalsList().then(res => {
      this.envurl = res.data.find(item => item.name === 'IM_ADDRESS_HTTP').value
    })
  },
  methods: {
    // 打开弹窗
    async open(type, id) {
      this.dialogVisible = true
      this.dialogTitle = type === 'create' ? '新增' : '修改'
      this.formType = type
      this.resetForm()
      if (id) {
        this.formLoading = true
        try {
          const { data } = await getCarousel(id)
          this.formData = data
          this.showAuthImg = true
          this.imageUrl = data.pciUrl
          this.titlePage.officialAccountId = data.officialAccountId
          data.officialAccountId != null
            ? this.getTitleList(data.officialAccountId)
            : (this.officialAccountName = '')
        } finally {
          this.formLoading = false
        }
      } else {
        this.formLoading = false
      }
    },
    changeOfficialAccount(id) {
      this.titleList = []
      this.$set(this.formData, 'articleId', '')
      this.$set(this.formData, 'url', '')
      this.$set(this.formData, 'title', '')
      this.$set(this.formData, 'officialAccountName', '')
      id && this.getTitleList(id)
    },
    getTitleList(id, isMore = false) {
      if (!isMore) {
        this.titlePage.pageNum = 1
        this.titleList = []
        // 切换公众号时，清空标题和跳转链接的值
        this.$set(this.formData, 'articleId', '')
        this.$set(this.formData, 'title', '')
        this.$set(this.formData, 'url', '')
      }
      this.formData.officialAccountName = this.officialAccountList.find(
        item => item.id === this.formData.officialAccountId
      )?.name
      const params = {
        pageNum: this.titlePage.pageNum,
        pageSize: this.titlePage.pageSize,
        officialAccountId: id || this.formData.officialAccountId,
        isDel: false
      }
      getArticleList(params).then(res => {
        this.titlePage.totalCount = res.data.totalCount
        this.titleList = [...this.titleList, ...res.data.records]
        if (this.titleList.length > 0) {
          this.formData.articleId = this.titleList[0]?.id
          this.changeTitle()
        }
      })
    },

    handleScroll() {
      if (this.titleList.length < this.titlePage.totalCount) {
        this.titlePage.pageNum++
        if (this.formData.officialAccountId) {
          this.titlePage.officialAccountId = this.formData.officialAccountId
        }
        this.getTitleList(
          this.titlePage.officialAccountId || this.officialAccountName,
          true
        )
      }
    },
    // isSetUrl true 设置 false 清空url
    changeTitle() {
      this.formData.url =
        this.envurl +
        this.titleList.find(item => item.id === this.formData.articleId)
          ?.contentUrl
      this.$set(this.formData, 'url', this.formData.url)
      this.formData.title = this.titleList.find(
        item => item.id === this.formData.articleId
      )?.title
      this.$set(this.formData, 'title', this.formData.title)
    },

    async handleRequest() {
      // 提交请求
      this.formLoading = true
      try {
        const data = this.formData
        if (this.formType === 'create') {
          const { code, msg } = await createCarousel(data)
          this.$message({
            message: code === 0 ? '新增成功' : msg,
            type: code === 0 ? 'success' : 'error'
          })
        } else {
          const { code, msg } = await updateCarousel(data)
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
    handleSubmit() {
      if (this.changeImg) {
        this.uploadFile()
      } else {
        this.submitForm()
      }
    },

    // 校验表单
    submitForm() {
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
      this.resetForm()
    },

    // 重置表单
    resetForm() {
      this.formData = deepCopy(form)
      this.imageUrl = ''
      this.imageValid = true
      this.showAuthImg = false
      this.changeImg = false
      this.titleList = []
    },

    // 图片上传
    async uploadFile() {
      const formData = new FormData()
      formData.append('file', this.file.raw)
      await uploadTmp(formData)
        .then(result => {
          const { code, data } = result
          if (code === 0) {
            this.$set(
              this.formData,
              'pciUrl',
              Array.isArray(data) && data?.length > 0 ? data[0] : data
            )
            this.submitForm()
          } else {
            this.$message({
              message: '轮播图上传失败',
              type: 'error'
            })
          }
        })
        .catch(() => {
          this.$message({
            message: '轮播图上传失败，图片资源错误',
            type: 'error'
          })
        })
    },

    handleChange(file) {
      const flag = this.beforeUpload(file)
      if (!flag) return
      this.file = file
      this.changeImg = true
      if (file.raw) {
        this.showAuthImg = false
      }
      this.imageUrl = URL.createObjectURL(file.raw)
      // 选择图片后先让表单字段有值，并清除“轮播图不能为空”的校验提示
      // 实际的 pciUrl 会在 uploadFile() 上传成功后被真实地址覆盖
      this.$set(this.formData, 'pciUrl', this.imageUrl)
      this.$nextTick(() => {
        this.$refs?.formRef?.clearValidate?.('pciUrl')
      })
      this.formLoading = false
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

::v-deep.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  height: 120px;
  width: 343px;
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
  height: 120px;
  width: 343px;
  display: block;
}
</style>
