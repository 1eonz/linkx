<template>
  <div class="edit-section" v-loading="detailLoading">
    <el-form :model="form" ref="form" :rules="rules" label-width="100px">
      <!-- 公共表单项 -->
      <el-form-item label="板块名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入板块名称" />
      </el-form-item>
      <el-form-item label="板块类型" prop="type">
        <el-select v-model="form.type" placeholder="请选择板块类型" @change="handleTypeChange" style="width: 100%;">
          <el-option v-for="item in sectionTypeList" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      
      <!-- 根据类型动态渲染自定义表单项 -->
      <template v-if="form.type">
        <!-- 常用应用 -->
        <template v-if="form.type === 2">
          <el-form-item label="跳转地址" prop="url">
            <el-input disabled v-model="form.url" placeholder="" clearable />
          </el-form-item>
          <el-form-item label="展示行数" prop="custom">
            <el-input-number v-model="form.custom.rowCount" placeholder="请输入展示行数" :min="1" :max="10" style="width: 100%;" />
          </el-form-item>
        </template>
        
        <!-- 协同群组 -->
        <template v-if="form.type === 3">
          <!-- 按钮配置已移至公共设置tab -->
        </template>
        
        <!-- 三方网页 -->
        <template v-if="form.type === 4">
          <el-form-item label="跳转地址" prop="url">
            <el-input v-model="form.url" placeholder="请输入跳转地址" clearable />
          </el-form-item>
          <el-form-item label="页面地址" prop="custom">
            <el-input v-model="form.custom.iframePageUrl" placeholder="请输入页面地址" clearable />
          </el-form-item>
          <el-form-item label="网页宽高" prop="custom">
            <el-input disabled v-model="form.custom.iframeSize" placeholder="请输入网页宽高，格式：宽*高" />
          </el-form-item>
        </template>
        
        <!-- 分割条 -->
        <template v-if="form.type === 5">
          <el-form-item label="内容类型">
            <el-radio-group v-model="form.custom.contentType" @change="handleContentTypeChange">
              <el-radio label="text">文字/图片URL</el-radio>
              <el-radio label="upload">上传图片</el-radio>
            </el-radio-group>
          </el-form-item>
          <!-- 文字/图片URL 输入 -->
          <el-form-item v-if="form.custom.contentType === 'text'" label="分割内容" prop="custom">
            <el-input v-model="form.custom.lineContent" placeholder="请输入文字/图片URL" />
          </el-form-item>
          <!-- 上传图片 -->
          <el-form-item v-if="form.custom.contentType === 'upload'" label="分割图片" prop="custom">
            <el-upload
              ref="dividerUpload"
              class="divider-uploader"
              action="#"
              :file-list="dividerFileList"
              :show-file-list="false"
              :auto-upload="false"
              :on-change="handleDividerChange"
              :before-upload="beforeDividerUpload"
              accept=".jpg,.png,.gif,.JPG,.PNG,.GIF"
            >
              <authImg v-if="showDividerAuthImg" class="divider-avatar" :auth-src="dividerImageUrl" />
              <i v-else-if="!dividerImageUrl" class="el-icon-plus divider-uploader-icon"></i>
              <img v-else :src="dividerImageUrl" class="divider-avatar" />
            </el-upload>
          </el-form-item>
        </template>
      </template>
      
      <!-- 公共表单项 -->
      <el-form-item label="排列顺序" prop="sort">
        <el-input-number v-model="form.sort" :min="1" placeholder="请输入排列顺序" style="width: 100%;" />
      </el-form-item>
      <!-- 编辑模式有该字段 -->
      <el-form-item label="是否显示" prop="show">
        <el-switch v-model="form.show" :active-value="1" :inactive-value="0" />
      </el-form-item>
    </el-form>
    <div class="section-footer">
      <el-button @click="closeDialog">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </div>
  </div>
</template>

<script>
import { getSectionDetail } from '@/api/h5/layoutConfig'
import { uploadTmp } from '@/api/resource/person'
import authImg from '@/components/AuthImg'

export default {
  name: 'EditSectionModal',
  components: { authImg },
  props: {
    sectionId: null,
  },
  data() {
    return {
      form: {
        name: '',
        type: '',
        url: '',
        custom: '',
        sort: 1,
        show: 1,
      },
      rules: {
        name: [{ required: true, message: '请输入板块名称', trigger: 'blur' }],
        type: [{ required: true, message: '请选择板块类型', trigger: 'change' }],
        sort: [{ required: true, message: '请输入排列顺序', trigger: 'blur' }]
      },
      detailLoading: false,
      // 分割条图片上传相关
      dividerImageUrl: '',
      dividerFileList: [],
      showDividerAuthImg: false,
      dividerChangeImg: false,
      dividerFile: null,
    }
  },
  computed: {
    sectionTypeList() {
      return [
        { value: 1, label: '轮播图' },
        { value: 2, label: '常用应用' },
        { value: 3, label: '协同群组' },
        { value: 4, label: '三方网页' },
        { value: 5, label: '分割条' },
        { value: 6, label: '消息列表' },
      ]
    }
  },
  mounted() {
    this.sectionId && this.getLayoutSectionDetail(this.sectionId)
  },

  methods: {
     // 获取板块详情
    async getLayoutSectionDetail(sectionId) {
      try {
        this.detailLoading = true;
        const res = await getSectionDetail(sectionId)
        const result = res.data || {}
        let parsedCustom = result.custom ? JSON.parse(result.custom) : null
        
        // 处理分割条图片回显
        if (result.type === 5 && parsedCustom) {
          // 兼容旧数据：如果没有 contentType，根据 lineContent 判断
          if (!parsedCustom.contentType) {
            parsedCustom.contentType = 'text'
          }
          if (parsedCustom.contentType === 'upload' && parsedCustom.uploadedImageUrl) {
            this.dividerImageUrl = parsedCustom.uploadedImageUrl
            this.showDividerAuthImg = true
          }
        }
        
        this.form.name = result.name || ''
        this.form.type = result.type || ''
        this.form.url = result.url || ''
        // 使用 $set 确保 custom 对象是响应式的
        this.$set(this.form, 'custom', parsedCustom || {})
        this.form.sort = result.sort == undefined ? 1 : result.sort
        this.form.show = result.show == undefined ? 1 : result.show
      } finally {
        this.detailLoading = false;
      }
    },
    handleTypeChange(value) {
      const defaultCustom = {
        2: { rowCount: 2 }, 
        3: {}, // 协同群组：按钮配置已移至公共设置tab
        4: { iframeSize: '400*300', iframePageUrl: '' }, 
        5: { contentType: 'text', lineContent: '', uploadedImageUrl: '' }
      }
      
      // 使用 $set 确保 custom 对象是响应式的
      this.$set(this.form, 'custom', defaultCustom[value] || {})
      this.form.url = ''
      this.form.sort = 1
      this.form.show = 1
      
      // 重置分割条图片上传相关状态
      this.dividerImageUrl = ''
      this.dividerFileList = []
      this.showDividerAuthImg = false
      this.dividerChangeImg = false
      this.dividerFile = null
    },
    // 分割条内容类型切换
    handleContentTypeChange() {
      const newType = this.form.custom.contentType
      if (newType === 'upload') {
        // 切换到上传图片，恢复已上传图片的显示
        if (this.form.custom.uploadedImageUrl) {
          this.dividerImageUrl = this.form.custom.uploadedImageUrl
          this.showDividerAuthImg = true
        } else {
          this.dividerImageUrl = ''
          this.showDividerAuthImg = false
        }
        this.dividerChangeImg = false
        this.dividerFile = null
      }
      // 切换到 text 时，lineContent 已绑定在 form.custom 上，无需额外处理
    },
    // 提交表单
    async handleSubmit() {
      this.$refs.form.validate(async (valid) => {
        if(!valid) return this.$message('表单校验失败')
        
        // 如果是分割条且选择了上传图片且有图片变更，先上传图片
        if (this.form.type === 5 && this.form.custom.contentType === 'upload' && this.dividerChangeImg) {
          await this.uploadDividerFile()
        } else {
          this.$emit('ok', this.form)
        }
      })
    },
    // 分割条图片上传
    async uploadDividerFile() {
      if (!this.dividerFile) {
        this.$emit('ok', this.form)
        return
      }
      try {
        const formData = new FormData()
        formData.append('file', this.dividerFile.raw)
        const result = await uploadTmp(formData)
        const { code, data } = result
        if (code === 0) {
          const uploadedUrl = Array.isArray(data) && data?.length > 0 ? data[0] : data
          this.form.custom.uploadedImageUrl = uploadedUrl
          this.$emit('ok', this.form)
        } else {
          this.$message.error('分割图片上传失败')
        }
      } catch (e) {
        this.$message.error('分割图片上传失败')
      }
    },
    // 分割条图片选择
    handleDividerChange(file) {
      const flag = this.beforeDividerUpload(file)
      if (!flag) return
      this.dividerFile = file
      this.dividerChangeImg = true
      this.showDividerAuthImg = false
      this.dividerImageUrl = URL.createObjectURL(file.raw)
    },
    // 分割条图片上传前校验
    beforeDividerUpload(file) {
      const { name, size } = file
      const index = name.lastIndexOf('.')
      const type = name.substring(index + 1).toLowerCase()
      const typeData = ['jpg', 'png', 'gif']
      const isJPG = typeData.includes(type)
      const isLt5M = size / 1024 / 1024 < 5
      if (!isJPG) {
        this.$message.error('图片只能是jpg/png/gif格式!')
        return false
      }
      if (!isLt5M) {
        this.$message.error('图片大小不能超过5M!')
        return false
      }
      return true
    },
    // 关闭弹窗
    closeDialog() {
      this.$emit('cancel')
    }
  }
}
</script>

<style scoped>
.edit-section {
  padding: 10px;
}
.section-footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}
/* 分割条图片上传样式 */
.divider-uploader {
  display: inline-block;
}
.divider-uploader ::v-deep .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  width: 200px;
  height: 100px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  position: relative;
  overflow: hidden;
}
.divider-uploader ::v-deep .el-upload:hover {
  border-color: #409eff;
}
.divider-uploader-icon {
  font-size: 28px;
  color: #8c939d;
}
.divider-avatar {
  width: 200px;
  height: 100px;
  display: block;
  object-fit: contain;
}
</style>