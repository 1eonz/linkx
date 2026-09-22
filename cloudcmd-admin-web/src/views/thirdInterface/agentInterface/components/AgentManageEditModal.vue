<template>
  <el-dialog
    :title="operate === 'add' ? '新建智能体' : '编辑智能体'"
    :visible.sync="dialogVisible"
    width="724px"
    custom-class="agent-dialog"
    center
    @close="handleCancel"
  >
    <el-form
      ref="formRef"
      :model="form"
      :rules="rules"
      label-width="140px"
      label-position="left"
    >
      <el-form-item label="智能体名称" prop="name">
        <el-input v-model="form.name" style="width: 492px" />
      </el-form-item>
      <el-form-item label="智能体说明" prop="desc">
        <el-input v-model="form.desc" style="width: 492px" />
      </el-form-item>
      <el-form-item label="智能体图标" prop="avatar">
        <div v-if="form.avatar" class="avatar-display">
          <el-image class="agent-avatar" fit="cover" :src="avatarUrl" style="width: 50px; height: 50px;">
            <div slot="error" class="image-slot">
              <i class="el-icon-picture-outline img-close"></i>
            </div>
          </el-image>
          <i class="el-icon-circle-close" @click="form.avatar = ''" />
        </div>
        <el-upload
          v-else
          accept="image/*"
          action="#"
          :auto-upload="true"
          :before-upload="beforeUpload"
          :http-request="httpRequest"
          :limit="10"
          :show-file-list="false"
        >
          <el-button size="small" icon="el-icon-picture-outline"
            >选择图片</el-button
          >
        </el-upload>
      </el-form-item>
      <el-form-item label="请求方法" prop="httpMethod" v-if="!form.paramScript">
        <el-select v-model="form.httpMethod" style="width: 492px" clearable >
          <el-option label="GET" value="GET" />
          <el-option label="POST" value="POST" />
          <el-option label="PUT" value="PUT" />
          <el-option label="DELETE" value="DELETE" />
        </el-select>
      </el-form-item>
          <el-form-item label="关联用户" prop="virtualUserId">
        <el-select
          v-model="form.virtualUserId"
          filterable
          clearable
          placeholder="请选择用户"
          style="width: 492px"
        >
          <el-option
            v-for="item in virtualUserList"
            :key="item.id"
            :label="item.userName"
            :value="item.id"
            :disabled="item.isBound"
          >
          </el-option>
        </el-select>
      </el-form-item>
      <el-form-item label="服务地址" prop="url">
        <el-input
          v-model="form.url"
          style="width: 492px"
          placeholder="含协议、IP、端口、URI"
        />
      </el-form-item>
      <el-form-item label="认证Token" prop="token">
        <el-input
          v-model="form.token"
          style="width: 492px"
          placeholder="请输入认证Token"
        />
      </el-form-item>
      <el-form-item label="header参数" v-if="!form.paramScript">
        <el-input
          v-model="form.header"
          style="width: 492px"
          type="textarea"
          :rows="3"
          placeholder="JSON格式的Header参数"
        />
      </el-form-item>
      <el-form-item label="query参数" v-if="!form.paramScript">
        <el-input
          v-model="form.query"
          style="width: 492px"
          type="textarea"
          :rows="3"
          placeholder="JSON格式的Query参数"
        />
      </el-form-item>
      <el-form-item label="body参数类型" v-if="!form.paramScript">
        <el-select v-model="form.bodyType" style="width: 492px">
          <el-option
            v-for="item in bodyTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="body参数" v-if="!form.paramScript">
        <el-input
          v-model="form.body"
          style="width: 492px"
          type="textarea"
          :rows="3"
          :placeholder="bodyPlaceholder"
        />
      </el-form-item>
      <el-form-item label="优先级" prop="priority">
        <el-radio-group v-model="form.priority">
          <el-radio :label="0">高</el-radio>
          <el-radio :label="1">中</el-radio>
          <el-radio :label="2">低</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="智能体分类" prop="categoryIds">
        <el-select
          v-model="form.categoryIds"
          multiple
          :multiple-limit="3"
          style="width: 492px"
          placeholder="请选择分类"
        >
          <el-option
            v-for="item in typeList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="是否涉密">
        <el-radio-group v-model="form.isRestricted">
          <el-radio :label="1">是</el-radio>
          <el-radio :label="0">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="智能体作用域">
        <el-select v-model="form.scope" style="width: 492px" placeholder="请选择作用域">
          <el-option label="所有" :value="0" />
          <el-option label="仅作用于AI智能体问答" :value="1" />
          <el-option label="仅作用于IM" :value="2" />
        </el-select>
      </el-form-item>
      <el-form-item label="是否接收IM消息">
        <el-radio-group v-model="form.receiveIm">
          <el-radio :label="0">不接收</el-radio>
          <el-radio :label="1">接收</el-radio>
        </el-radio-group>
      </el-form-item>
      <!-- 音频支持能力 -->
      <el-form-item label="音频支持能力">
        <el-radio-group v-model="form.audio" @change="handleAudioChange">
          <el-radio :label="0">不支持</el-radio>
          <el-radio :label="1">支持</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.audio === 1" label="音频文件格式">
        <el-checkbox-group v-model="form.audioType">
          <el-checkbox v-for="item in audioOptions" :key="item" :label="item">
            {{ item }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <!-- 视频支持能力 -->
      <el-form-item label="视频支持能力">
        <el-radio-group v-model="form.video" @change="handleVideoChange">
          <el-radio :label="0">不支持</el-radio>
          <el-radio :label="1">支持</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.video === 1" label="视频文件格式">
        <el-checkbox-group v-model="form.videoType">
          <el-checkbox v-for="item in videoOptions" :key="item" :label="item">
            {{ item }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <!-- 图片支持能力 -->
      <el-form-item label="图片支持能力">
        <el-radio-group v-model="form.image" @change="handleImageChange">
          <el-radio :label="0">不支持</el-radio>
          <el-radio :label="1">支持</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.image === 1" label="图片文件格式">
        <el-checkbox-group v-model="form.imageType">
          <el-checkbox v-for="item in imageOptions" :key="item" :label="item">
            {{ item }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <!-- 文档支持能力 -->
      <el-form-item label="文档支持能力">
        <el-radio-group v-model="form.document" @change="handleDocumentChange">
          <el-radio :label="0">不支持</el-radio>
          <el-radio :label="1">支持</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.document === 1" label="文档文件格式">
        <el-checkbox-group v-model="form.documentType">
          <el-checkbox
            v-for="item in documentOptions"
            :key="item"
            :label="item"
          >
            {{ item }}
          </el-checkbox>
        </el-checkbox-group>
      </el-form-item>

      <!-- 文件上传接口 -->
      <el-form-item label="文件上传接口">
        <el-select
          v-model="form.fileInterfaceId"
          style="width: 492px"
          placeholder="请选择文件上传接口"
          clearable
          filterable
        >
          <el-option
            v-for="item in fileInterfaceList"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="参数脚本" v-if="!form.httpMethod && !form.header && !form.query && !form.body">
        <el-input
          v-model="form.paramScript"
          style="width: 492px"
          type="textarea"
          :rows="4"
          placeholder="智能体问题请求参数脚本"
        />
      </el-form-item>
      <el-form-item label="是否有结束标识">
        <el-radio-group v-model="form.endFlag">
          <el-radio :label="1">是</el-radio>
          <el-radio :label="0">否</el-radio>
        </el-radio-group>
      </el-form-item>
      <el-form-item label="结果脚本">
        <el-input
          v-model="form.respScript"
          style="width: 492px"
          type="textarea"
          :rows="4"
          placeholder="智能体问答响应脚本"
        />
      </el-form-item>
    </el-form>
    <span slot="footer" class="dialog-footer">
      <el-button @click="handleCancel">取消</el-button>
      <el-button type="primary" :loading="submitLoading" @click="handleConfirm"
        >确定</el-button
      >
    </span>
  </el-dialog>
</template>

<script>
import {
  addAiagent,
  updateAiagent,
  uploadFile,
  getAgentFileList,
  assistantAgentList
} from '@/api/thirdInterface/agentInterface.js'
 import { getVirtualUserList } from '@/api/policeExtend/virtualUser.js';
const DEFAULT_FORM = {
  id: '',
  name: '',
  desc: '',
  avatar: '',
  httpMethod: '',
  url: '',
  token: '',
  header: '',
  query: '',
  body: '',
  bodyType: 1,
  priority: 1,
  categoryIds: [],
  isRestricted: 0,
  receiveIm: 0,
  scope: 1,
  paramScript: '',
  respScript: '',
  endFlag: 1,
  // 文件能力配置
  audio: 0,
  audioType: [],
  video: 0,
  videoType: [],
  image: 0,
  imageType: [],
  document: 0,
  documentType: [],
  fileInterfaceId: null,
  virtualUserId: null,
}
// 文件格式选项
// Body参数类型选项
const BODY_TYPE_OPTIONS = [
  { label: 'raw-json', value: 1 },
  { label: 'raw-text', value: 2 },
  { label: 'form-data', value: 3 }
]
const AUDIO_OPTIONS = ['.mp3', '.aac', '.pcm', '.wav', '.amr', '.m4a', '.webm']
const VIDEO_OPTIONS = ['.mp4', '.mov', '.webm', '.mpeg', '.mpga']
const IMAGE_OPTIONS = ['.jpg', '.jpeg', '.gif', '.png', '.bmp', '.webp', '.svg']
const DOCUMENT_OPTIONS = [
  '.md',
  '.doc',
  '.docx',
  '.pdf',
  '.xlsx',
  '.xls',
  '.ppt',
  '.pptx',
  '.txt',
  '.html',
  '.csv',
  '.eml',
  '.xml',
  '.epub',
  '.msg',
  '.markdown'
]
export default {
  name: 'AgentManageEditModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    operate: {
      type: String,
      default: 'add'
    },
    typeList: {
      type: Array,
      default: () => []
    },
    editData: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      submitLoading: false,
      form: { ...DEFAULT_FORM },
      avatarRelativePath: '', // 保存相对路径，用于提交
      avatarUrl: '', // 用于显示的完整URL
      virtualUserList:[],
      fileInterfaceList: [],
      virtualUserList: [], //虚拟用户列表
      // 文件格式选项
      bodyTypeOptions: BODY_TYPE_OPTIONS,
      audioOptions: AUDIO_OPTIONS,
      videoOptions: VIDEO_OPTIONS,
      imageOptions: IMAGE_OPTIONS,
      documentOptions: DOCUMENT_OPTIONS,

      rules: {
        name: [{ required: true, message: '请填写名称', trigger: 'blur' }],
        desc: [{ required: true, message: '请填写智能体说明', trigger: 'blur' }],
        avatar: [{ required: true, message: '请上传图标', trigger: 'change' }],
        url: [{ required: true, message: '请填写URL', trigger: 'blur' }],
        token: [
          { required: true, message: '请填写认证Token', trigger: 'blur' }
        ],
        priority: [
          { required: true, message: '请选择优先级', trigger: 'change' }
        ],
        categoryIds: [
          { required: true, message: '请选择分类', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    dialogVisible: {
      get() {
        return this.visible
      },
      set(val) {
        this.$emit('update:visible', val)
      }
    },
    bodyPlaceholder() {
      const item = this.bodyTypeOptions.find(b => b.value === this.form.bodyType)
      return `${item ? item.label : ''}格式的Body参数`
    }
  },
  watch: {
   async visible(val) {
      if (val) {
       await this.fetchFileInterfaceList()
       await this.initForm()
       await this.getVirtualUser()
      }
    }
  },
  methods: {
    async initForm() {
      if (this.operate === 'edit' && this.editData) {
        const baseUrl = window.location.origin + '/linkx/admin/XA-ics-agent'
        const d = this.editData
        this.avatarRelativePath = d.avatar || ''
        this.avatarUrl = d.avatar ? baseUrl + d.avatar :   ''
        this.form = {
          ...DEFAULT_FORM,
          ...this.editData,
          id: d.id,
          name: d.name,
          desc: d.desc,
          avatar: d.avatar || '',
          httpMethod: d.httpMethod,
          url: d.url,
          token: d.token || '',
          header: d.header,
          query: d.query,
          body: d.body,
          bodyType: d.bodyType != null ? d.bodyType : 1,
          priority: d.priority,
          categoryIds: d.categoryIds ? d.categoryIds.split(',') : [],
          documentType: d.documentTypeList || [],
          audioType: d.audioTypeList || [],
          videoType: d.videoTypeList || [],
          imageType: d.imageTypeList || [],
          endFlag: d.endFlag != null ? d.endFlag : 1,
          receiveIm: d.receiveIm != null ? Number(d.receiveIm) : 0,
          scope: d.scope != null ? Number(d.scope) : 1,
        }
        await this.getBoundUserDetail(this.form.id)
      } else {
        this.form = {
          ...DEFAULT_FORM,
          categoryIds: this.typeList.length > 0 ? [this.typeList[0].id] : []
        }
        this.avatarRelativePath = ''
        this.avatarUrl = ''
      }
    },
    beforeUpload(file) {
      const isImage = file.type.startsWith('image/')
      if (!isImage) {
        this.$message.error('只能上传图片文件！')
        return false
      }
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isLt2M) {
        this.$message.error('图片大小不能超过2MB！')
        return false
      }
      return true
    },
    async httpRequest(options) {
      const formData = new FormData()
      formData.append('file', options.file)
      try {
        const res = await uploadFile(formData)
        if (res.code === 0) {
          this.avatarRelativePath = res.data
          this.form.avatar = res.data
          const baseUrl = window.location.origin + '/linkx/admin/XA-ics-agent'
          this.avatarUrl = res.data ? baseUrl + res.data : ''
        } else {
          this.$message.error(res.msg || '上传失败')
        }
      } catch (error) {
        this.$message.error('上传失败')
      }
    },
    // 获取文件接口列表
    async fetchFileInterfaceList() {
      try {
        const res = await getAgentFileList()
        if (res.code === 0) {
          this.fileInterfaceList = res.data || []
        }
      } catch (error) {
        console.error('获取文件接口列表失败:', error)
        this.fileInterfaceList = []
      }
    },
    // 获取虚拟用户
    async getVirtualUser() {
      try {
        const { code, data } = await getVirtualUserList()
        if (code === 0) {
          // 标记已关联用户
          this.virtualUserList = (data || []).map(item => ({
            ...item,
            // 已被其他Agent关联的用户不可选（当前已关联的除外）
            isBound: Boolean(item.agentId && item.id !== this.form.virtualUserId)
          }))
        } else {
          this.$message.error(data?.msg || '获取用户列表失败')
        }
      } catch (error) {
        console.error('获取用户列表失败:', error)
      } 
    },
      // 获取已关联用户详情
    async getBoundUserDetail(agentId) {
      if (!agentId) return

      try {
        const parmas = {
          agentId
        }
        const { code, data } = await assistantAgentList(parmas)
        if (code === 0 && data) {
          // 保存已关联用户ID
          const result = data.records || []
          if (result.length) {
            // 回显当前关联用户
            this.form.virtualUserId = result[0].virtualUserId
          }
        }
      } catch (error) {
        console.error('获取关联用户详情失败:', error)
      }
    },
    // 音频支持能力变化
    handleAudioChange(val) {
      if (val === 0) {
        this.form.audioType = []
      }
    },

    // 视频支持能力变化
    handleVideoChange(val) {
      if (val === 0) {
        this.form.videoType = []
      }
    },

    // 图片支持能力变化
    handleImageChange(val) {
      if (val === 0) {
        this.form.imageType = []
      }
    },

    // 文档支持能力变化
    handleDocumentChange(val) {
      if (val === 0) {
        this.form.documentType = []
      }
    },
    handleCancel() {
      this.dialogVisible = false
      this.$refs.formRef && this.$refs.formRef.resetFields()
    },
    async handleConfirm() {
      this.$refs.formRef.validate(async valid => {
        if (!valid) return
        // 仅当body参数类型为raw-json时校验JSON格式
        if (this.form.bodyType === 1 && this.form.body) {
          try {
            JSON.parse(this.form.body)
          } catch (e) {
            this.$message.error('body参数不是有效的JSON格式')
            return
          }
        }
        this.submitLoading = true
        try {
          const params = {
            ...this.form,
            avatar: this.avatarRelativePath,
            categoryIds: this.form.categoryIds.join(','),
          }
          const api = this.operate === 'edit' ? updateAiagent : addAiagent
          console.log(params)
          const res = await api(params)
          if (res.code === 0) {
            this.$message.success(
              this.operate === 'edit' ? '编辑成功' : '添加成功'
            )
            this.handleCancel()
            this.$emit('success')
          } else {
            this.$message.error(res.msg || '操作失败')
          }
        } catch (error) {
          if (error instanceof SyntaxError) {
            this.$message.error('请检查JSON格式是否正确')
          } else {
            this.$message.error('操作失败')
          }
        } finally {
          this.submitLoading = false
        }
      })
    }
  }
}
</script>

<style scoped>
.avatar-display {
  position: relative;
  display: inline-block;
}

.avatar-display .el-icon-circle-close {
  position: absolute;
  top: -1px;
  right: -3px;
  cursor: pointer;
  font-size: 12px;
}
.avatar-display .agent-avatar {
  background-color: #f5f7fa;
}
.avatar-display .agent-avatar ::v-deep .image-slot {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 50px;
}
::v-deep .agent-dialog {
  display: flex;
  flex-direction: column;
  max-height: 80vh;
  margin-top: 10vh !important;
}

::v-deep .agent-dialog .el-dialog__header {
  flex-shrink: 0;
}

::v-deep .agent-dialog .el-dialog__body {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
  margin: 0 20px;
  margin-left: 60px;
  min-height: 0;
}

::v-deep .agent-dialog .el-dialog__footer {
  flex-shrink: 0;
}

::v-deep
  .agent-dialog
  .el-form-item:not(.is-required)
  .el-form-item__label::before {
  content: '*';
  color: transparent;
  margin-right: 4px;
}

::v-deep .agent-dialog .el-form-item__content {
  display: flex;
  align-items: center;
  min-height: 40px;
}
</style>
