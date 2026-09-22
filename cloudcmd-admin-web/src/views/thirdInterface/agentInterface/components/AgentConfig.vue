<template>
  <div class="agent-config">
    <el-form :model="deployForm" :rules="deployRules" ref="deployFormRef" label-width="160px">
      <div class="section-title">Agent部署开关</div>
      <div class="section-content">
        <el-row :gutter="100">
          <el-col :span="12">
            <el-form-item label="分离部署">
              <el-switch v-model="deployForm.separatedDeploy" @change="handleDeployModeChange" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="100">
          <el-col :span="8">
            <el-form-item label="服务器地址" prop="groupAiHost">
              <el-input v-model="deployForm.groupAiHost" placeholder="请输入服务器地址" clearable />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="智能体管理页面地址" prop="groupAiFrontendHost">
              <el-input v-model="deployForm.groupAiFrontendHost" placeholder="请输入页面跳转地址" clearable :disabled="!deployForm.separatedDeploy" />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="btns-wrap">
          <el-button type="primary" @click="saveDeployConfig" :loading="loading">更新部署配置</el-button>
        </div>
      </div>
    </el-form>
    <el-form :model="approvalForm" :rules="approvalRules" ref="approvalFormRef" label-width="160px">
      <div class="section-title">审批模式</div>
      <div class="section-content">
        <el-row :gutter="100">
          <el-col :span="8">
            <el-form-item class="approval-item" label="是否开启审批">
              <el-radio-group v-model="approvalForm.approvalEnabled" class="radio-group">
                <el-radio :label="true">是（一问一审批）</el-radio>
                <el-radio :label="false">否</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item class="approval-item" label="审批子模式" prop="approvalSubMode">
              <el-radio-group v-model="approvalForm.approvalSubMode" class="radio-group" :disabled="!approvalForm.approvalEnabled">
                <el-radio :label="0">先问后审</el-radio>
                <el-radio :label="1">先审后答</el-radio>
              </el-radio-group>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="100">
          <el-col :span="16">
            <el-form-item label="审批系统地址" :required="approvalForm.approvalEnabled" prop="approvalSystemUrl">
              <el-input v-model="approvalForm.approvalSystemUrl" placeholder="请输入审批系统地址" clearable />
            </el-form-item>
          </el-col>
        </el-row>
        <div class="btns-wrap">
          <el-button type="primary" @click="saveApprovalConfig" :loading="loading">更新审批配置</el-button>
        </div>
      </div>
    </el-form>
    <div class="section-title">智能体导入</div>
    <div class="section-content import-section">
      <el-button type="primary" icon="el-icon-download" @click="handleExport">模板下载</el-button>
      <el-upload ref="upload" class="filter-item" action="#" accept=".xlsx,.xls,.csv" :http-request="uploadFileBtn" :show-file-list="false">
        <el-button type="primary" icon="el-icon-upload2">导入</el-button>
      </el-upload>
    </div>
  </div>
</template>

<script>
import { 
  getAiagentSettings, 
  updateAiagentSettings, 
  getDeploySettings, 
  updateDeploySettings,
  importAiagent,
  exportAiagentTemplate
} from '@/api/thirdInterface/agentInterface.js'

export default {
  name: 'AgentConfig',
  data() {
    return {
      deployForm: {
        separatedDeploy: false,
        groupAiHost: '',
        groupAiFrontendHost: ''
      },
      approvalForm: {
        approvalEnabled: false,
        approvalSubMode: '',
        approvalSystemUrl: ''
      },
      loading: false
    }
  },
  computed: {
    deployRules() {
      return {
        groupAiHost: [{ required: true, message: '请输入服务器地址', trigger: 'blur' }],
        groupAiFrontendHost: [
           {
            required: this.deployForm.separatedDeploy ? true : false, message: '分离部署时必须填写智能体管理页面地址',trigger: 'blur'
          },
        ]
      }
    },
    approvalRules() {
      return {
        approvalSystemUrl: [
          {
            required: this.approvalForm.approvalEnabled ? true : false, message: '开启审批时必须填写审批系统地址', trigger: ['blur', 'change']
          }
        ]
      }
    }
  },
  watch: {
    'deployForm.separatedDeploy'(newVal) {
      if (!newVal) {
        this.$nextTick(() => {
          this.$refs.deployFormRef?.clearValidate(['groupAiHost', 'groupAiFrontendHost'])
        })
      }
    },
    'approvalForm.approvalEnabled': function(newVal) {
      if (newVal) {
        if (this.approvalForm.approvalSubMode === '') {
          this.approvalForm.approvalSubMode = 0
        }
      } else {
        this.approvalForm.approvalSubMode = ''
        this.$nextTick(() => {
          this.$refs.approvalFormRef?.clearValidate(['approvalSystemUrl'])
        })
      }
    },
  },
  mounted() {
    this.loadDeployConfig()
    this.loadApprovalConfig()
  },
  methods: {
    // 加载部署配置
    async loadDeployConfig() {
      try {
        console.log(122222)
        const res = await getDeploySettings()
        console.log(133333, res)
        if (res.code === 0) {
          console.log(144444)
          const d = res.data
          this.deployForm.separatedDeploy = d.separatedDeploy === 1
          this.deployForm.groupAiHost = d.groupAiHost || ''
          this.deployForm.groupAiFrontendHost = d.groupAiFrontendHost || ''
          console.log(122212222, {
            separatedDeploy: this.deployForm.separatedDeploy,
            groupAiFrontendHost: this.deployForm.groupAiFrontendHost
          })
          this.$emit('deploy-mode-change', {
            separatedDeploy: this.deployForm.separatedDeploy,
            groupAiFrontendHost: this.deployForm.groupAiFrontendHost
          })
        }
      } catch (error) {
        console.error('获取部署配置失败:', error)
      }
    },
    // 加载审批配置
    async loadApprovalConfig() {
      try {
        const res = await getAiagentSettings()
        if (res.code === 0) {
          const d = res.data
          const enabled = {0: false, 1: true}[d?.approvalEnabled] || false
          this.approvalForm.approvalSystemUrl = d.approvalSystemUrl || ''
          this.approvalForm.approvalEnabled = enabled
          this.$nextTick(() => {
            this.approvalForm.approvalSubMode = enabled ? (Number(d?.approvalSubMode) || 0) : ''
          })
        }
      } catch (error) {
        console.error('获取审批配置失败:', error)
      }
    },
    // 处理Agent部署开关改变
    async handleDeployModeChange(val) {
      try {
        const params = {
          separatedDeploy: this.deployForm.separatedDeploy ? 1 : 0,
          groupAiHost: this.deployForm.groupAiHost,
          groupAiFrontendHost: this.deployForm.groupAiFrontendHost
        }
        const res = await updateDeploySettings(params)
        if (res.code === 0) {
          this.$emit('deploy-mode-change', {
            separatedDeploy: this.deployForm.separatedDeploy,
            groupAiFrontendHost: this.deployForm.groupAiFrontendHost
          })
        } else {
          this.$message.error((res.data && res.msg) || '部署配置保存失败')
        }
      } catch (error) {
        this.$message.error('部署配置保存失败')
      } finally {
        this.loading = false

        this.$emit('deploy-mode-change',  {
          separatedDeploy: this.deployForm.separatedDeploy,
          groupAiFrontendHost: this.deployForm.groupAiFrontendHost
        })
      }
    },
    // 保存部署配置
    saveDeployConfig() {
      this.$refs.deployFormRef.validate(async (valid) => {
        if (!valid) return
        this.loading = true
        try {
          const params = {
            separatedDeploy: this.deployForm.separatedDeploy ? 1 : 0,
            groupAiHost: this.deployForm.groupAiHost,
            groupAiFrontendHost: this.deployForm.groupAiFrontendHost
          }
          const res = await updateDeploySettings(params)
          if (res.code === 0) {
            this.$message.success('部署配置保存成功')
            this.$emit('deploy-mode-change', {
              separatedDeploy: this.deployForm.separatedDeploy,
              groupAiFrontendHost: this.deployForm.groupAiFrontendHost
            })
          } else {
            this.$message.error((res.data && res.msg) || '部署配置保存失败')
          }
        } catch (error) {
          this.$message.error('部署配置保存失败')
        } finally {
          this.loading = false
        }
      })
    },
    // 保存审批配置
    saveApprovalConfig() {
      this.$refs.approvalFormRef.validate(async (valid) => {
        console.log('valid', valid)
        if (!valid) return
        this.loading = true
        try {
          const params = {
            approvalEnabled: this.approvalForm.approvalEnabled ? 1 : 0,
            approvalSubMode: this.approvalForm.approvalEnabled ? this.approvalForm.approvalSubMode : 0,
            approvalSystemUrl: this.approvalForm.approvalSystemUrl
          }
          console.log('审批配置保存参数:', params)
          const res = await updateAiagentSettings(params)
          console.log(res)
          if (res.code === 0) {
            this.$message.success('审批配置保存成功')
          } else {
            this.$message.error((res.data && res.msg) || '审批配置保存失败')
          }
        } catch (error) {
          this.$message.error('审批配置保存失败')
        } finally {
          this.loading = false
        }
      })
    },
    uploadFileBtn(param) {
      const formData = new FormData()
      formData.append('file', param.file)
      this.$refs.upload.clearFiles()
      importAiagent(formData).then(res => {
        if (res.code === 0) {
          if (res.data) {
            this.customAlertMsg(res?.data?.errorMap)
            if (res?.data?.successList?.length > 0) {
              this.$emit('import-success')
            }
            if (
              Object.keys(res?.data?.errorMap).length === 0 &&
              res?.data?.successList?.length > 0
            ) {
              this.$message.success('导入成功')
            }
          } else {
            this.$emit('import-success')
            this.$message.success('导入成功')
          }
        } else if (res.code === 1) {
          if (res?.data) {
            this.customAlertMsg(res?.data)
          } else if (res?.msg) {
            this.$msgbox({
              message: res?.msg,
              title: '导入失败',
              confirmButtonText: '确定',
              dangerouslyUseHTMLString: true
            })
          }
        } else {
          if (res?.data) {
            this.customAlertMsg(res.data)
          } else {
            this.$message({
              message: res.msg || '导入失败',
              type: 'error'
            })
          }
        }
      })
    },
    formatErrorMsg(errorData) {
      if (Array.isArray(errorData)) {
        return errorData.map(msg => `<div>${msg}</div>`).join('')
      }
      let errorText = ''
      for (const lineNum in errorData) {
        if (errorData.hasOwnProperty(lineNum)) {
          const errArr = errorData[lineNum]
          const errMsgStr = errArr.join('，')
          const singleLineError = `第${lineNum}行${errMsgStr}`
          errorText += singleLineError + '<br/>'
        }
      }
      return errorText
    },
    customAlertMsg(data) {
      const formatMsg = this.formatErrorMsg(data)
      if (formatMsg) {
        this.$msgbox({
          message: formatMsg,
          title: '导入失败',
          confirmButtonText: '确定',
          dangerouslyUseHTMLString: true,
          customClass: 'alert-width-800'
        })
      }
    },
    getExportFileName(headers) {
      const contentDisposition = headers['content-disposition'] || ''
      if (!contentDisposition) return 'AI智能体导入模板.xlsx'
      const reg = /filename\*=\s*utf-8''([^;]+)|filename="?([^;"]+)"?/i
      const match = contentDisposition.match(reg)
      if (!match) return 'AI智能体导入模板.xlsx'
      const raw = (match[1] || match[2] || '').trim()
      const fileName = decodeURIComponent(raw)
      return fileName || 'AI智能体导入模板.xlsx'
    },
    async handleExport() {
      try {
        const data = await exportAiagentTemplate()
        if (!data) {
          return
        }
        const fileName = this.getExportFileName(data?.headers)
        const blob = new Blob([data], {
          type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
        })
        const url = window.URL.createObjectURL(blob)
        const link = document.createElement('a')
        link.style.display = 'none'
        link.href = url
        link.setAttribute('download', fileName)
        document.body.appendChild(link)
        link.click()
        document.body.removeChild(link)
        window.URL.revokeObjectURL(url)
      } catch (error) {
        console.log(error)
      }
    }
  }
}
</script>

<style scoped>
.agent-config {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow-y: auto;
  overflow-x: hidden;
}

.section-title {
  font-size: 16px;
  font-weight: bold;
  color: #000;
  margin-bottom: 12px;
  margin-top: 32px;
  padding: 5px 0;
  border-bottom: 1px dotted #eee;
}

.section-content {
  margin-left: 80px;
}

.btns-wrap {
  padding-left: 160px;
  margin-top: 20px;
}

.import-section {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: 240px;
}

.label-hint {
  color: #666;
  margin-left: 4px;
}

.hint-icon {
  font-size: 12px;
  margin-left: 5px;
}
::deep(.el-form-item__content) {
  display: flex;
  align-items: center;
}
.radio-group {
  height: 40px;
  line-height: 40px;
  display: flex;
  gap: 16px;
  align-items: center;
}

.radio-group .el-radio {
  margin-right: 0;
}
</style>

<style>
.alert-width-800 {
  width: 600px !important;
  min-width: 600px !important;
}
.alert-width-800 .el-message-box__message {
  max-height: 400px;
  overflow-y: auto;
}
</style>
