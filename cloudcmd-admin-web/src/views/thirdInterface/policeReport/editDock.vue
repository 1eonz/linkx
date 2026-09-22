<template>
  <div>
    <el-dialog
      :visible.sync="dialogVisible"
      :title="dialogTitle"
      @close="closeDialog"
      class="edit-dock"
    >
      <div class="content-box">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-position="left"
          label-width="200px"
          style="width: 100%; padding:0 30px;box-sizing:border-box"
        >
          <el-form-item label="名称" prop="name">
            <el-input
              v-model="formData.name"
              maxlength="100"
              show-word-limit
              placeholder="请输入"
            />
          </el-form-item>
          <el-form-item label="所属系统" prop="systemName">
            <el-input
              v-model="formData.systemName"
              maxlength="50"
              show-word-limit
              placeholder="请输入"
            />
          </el-form-item>
          <el-form-item label="系统编码" prop="systemCode">
            <el-input
              v-model="formData.systemCode"
              maxlength="50"
              show-word-limit
              placeholder="请输入"
            />
          </el-form-item>
          <el-form-item label="访问协议" prop="schema">
            <el-input
              v-model="formData.schema"
              maxlength="10"
              show-word-limit
              placeholder="示例：http..."
            />
          </el-form-item>
          <el-form-item label="访问IP" prop="ip">
            <el-input
              v-model="formData.ip"
              maxlength="50"
              show-word-limit
              placeholder="示例：127.0.0.0..."
            />
          </el-form-item>
          <el-form-item label="端口" prop="port">
            <el-input
              v-model="formData.port"
              maxlength="10"
              show-word-limit
              placeholder="示例：8000..."
            />
          </el-form-item>
          <el-form-item label="接口名称" prop="path">
            <el-input
              v-model="formData.path"
              maxlength="200"
              show-word-limit
              placeholder="示例：/getList/task..."
            />
          </el-form-item>
          <el-form-item label="请求方式" prop="method">
            <el-input
              v-model="formData.method"
              maxlength="200"
              show-word-limit
              placeholder="示例：GET..."
            />
          </el-form-item>
          <el-form-item label="请求Header" prop="headers">
            <el-input
              v-model="formData.headers"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              placeholder='示例：{"Content-Type": "application/json"}...，无则填{}'
            />
          </el-form-item>
          <el-form-item label="请求Body" prop="body">
            <el-input
              v-model="formData.body"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              placeholder='示例：{"id": "123456"}...，无则填{}'
            />
          </el-form-item>
          <el-form-item label="请求Params" prop="params">
            <el-input
              v-model="formData.params"
              type="textarea"
              :rows="3"
              maxlength="2000"
              show-word-limit
              placeholder='示例：{"id": "123456"}...，无则填{}'
            />
          </el-form-item>
          <el-form-item label="表单映射脚本" prop="script">
            <div class="script-container">
              <div class="script-actions">
                <el-button
                  type="text"
                  size="small"
                  @click="downloadExample"
                  icon="el-icon-download"
                >
                  下载示例
                </el-button>
              </div>
              <el-input
                v-model="formData.script"
                type="textarea"
                :rows="8"
                maxlength="2000"
                show-word-limit
                placeholder="请输入"
              />
            </div>
          </el-form-item>
          <el-form-item label="数据刷新周期" prop="executePeriod">
            <div class="cycle-list">
              <div
                class="cycle-item"
                :class="{ 'active-cycle': item.id === formData.executePeriod }"
                v-for="(item, index) in cycleArr"
                :key="index"
                @click="clickCycle(item)"
              >
                {{ item.name }}
              </div>
            </div>
          </el-form-item>
        </el-form>
      </div>
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
import { deepCopy } from '@/utils'
import { saveDock, updateDock } from '@/api/policeReport/dock'
const form = {
  name: '',
  systemName: '',
  systemCode: '',
  schema: '',
  ip: '',
  port: '',
  path: '',
  method: '',
  headers: '',
  body: '',
  params: '',
  script: '',
  executePeriod: '1800000',
  status: 0
}
export default {
  name: 'EditDock',
  props: {
    isAdmin: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      dialogVisible: false,
      formData: deepCopy(form),
      formType: '',
      formLoading: false,
      dialogTitle: '',
      cycleArr: [
        { id: '1800000', name: '30min' },
        { id: '3600000', name: '1h' },
        { id: '21600000', name: '6h' },
        { id: '43200000', name: '12h' },
        { id: '86400000', name: '24h' }
      ],
      formRules: {
        name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
        systemName: [
          { required: true, message: '所属系统不能为空', trigger: 'blur' }
        ],
        systemCode: [
          { required: true, message: '系统编码不能为空', trigger: 'blur' }
        ],
        schema: [
          { required: true, message: '访问协议不能为空', trigger: 'blur' }
        ],
        ip: [{ required: true, message: '访问IP不能为空', trigger: 'blur' }],
        port: [{ required: true, message: '端口不能为空', trigger: 'blur' }],
        path: [
          { required: true, message: '接口名称不能为空', trigger: 'blur' }
        ],
        method: [
          { required: true, message: '请求方式不能为空', trigger: 'blur' }
        ],
        script: [
          { required: true, message: '表单映射脚本不能为空', trigger: 'blur' }
        ],
        headers: [
          { required: true, message: '请求头不能为空', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              this.validateJson(rule, value, callback, '请求头', false)
            },
            trigger: 'blur'
          }
        ],
        body: [
          { required: true, message: '请求体不能为空', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              this.validateJson(rule, value, callback, '请求体', false)
            },
            trigger: 'blur'
          }
        ],
        params: [
          { required: true, message: '请求参数不能为空', trigger: 'blur' },
          {
            validator: (rule, value, callback) => {
              this.validateJson(rule, value, callback, '请求参数', false)
            },
            trigger: 'blur'
          }
        ],
        executePeriod: [
          { required: true, message: '数据刷新周期不能为空', trigger: 'blur' }
        ]
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
  methods: {
    validateJson(rule, value, callback, fieldName, required = false) {
      if (value && value.trim()) {
        try {
          JSON.parse(value)
          callback()
        } catch (e) {
          callback(new Error(`${fieldName}必须是有效的JSON格式`))
        }
      } else if (required) {
        callback(new Error(`${fieldName}不能为空`))
      } else {
        callback()
      }
    },
    clickCycle(item) {
      this.formData.executePeriod = item.id
    },
    async open(type, row) {
      this.dialogTitle = type === 'create' ? '新增' : '修改'
      this.formType = type
      this.formLoading = true
      this.resetForm()
      if (row) {
        this.formData = { ...row }
        this.formData.id = row.id
      } else {
        this.formData = deepCopy(form)
      }
      this.formLoading = false
      this.dialogVisible = true
    },
    resetForm() {
      this.formData = deepCopy(form)
    },
    closeDialog() {
      this.dialogVisible = false
    },
    handleSubmit() {
      this.$refs['formRef'].validate(valid => {
        if (valid) {
          if (this.ensureJsonFields()) {
            this.handleRequest()
          }
        } else {
          return false
        }
      })
    },
    ensureJsonFields() {
      const fields = [
        { key: 'headers', name: '请求头' },
        { key: 'body', name: '请求体' },
        { key: 'params', name: '请求参数' }
      ]
      for (const { key, name } of fields) {
        const val = this.formData[key]
        if (!val || !val.trim()) {
          this.$message.error(`${name}不能为空`)
          return false
        }
        try {
          JSON.parse(val)
        } catch (e) {
          this.$message.error(`${name}必须是有效的JSON格式`)
          return false
        }
      }
      return true
    },
    async handleRequest() {
      let res
      if (this.formType === 'create') {
        res = await saveDock({ ...this.formData })
      } else {
        res = await updateDock({ ...this.formData })
      }
      const { code, msg } = res
      this.handleVisible(code)
      if (code === 0) {
        this.$message({
          message: this.formType === 'create' ? '新增成功' : '修改成功',
          type: 'success'
        })
        return
      }
      const title =
        this.formType === 'create'
          ? '新增失败，请稍后重试'
          : '修改失败，请稍后重试'
      this.$message({
        message: msg || title,
        type: 'error'
      })
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
    downloadExample() {
      const exampleScript = `var Result = function(param, conf) {
    var json = JSON.parse(param)
    var conf_json = JSON.parse(conf)
    console.log(JSON.stringify(json))

    if (json.code != 0) {
        console.log("error:" + JSON.stringify(json))
        return null
    }

    return {
        id: json.data.id,
        tag: json.data.tag,
        origin: json,
        name: json.data.name,
        code: json.data.code,
        content: json.data.content,
        createTime: json.data.createTime,
        dispatcher: json.data.dispatcher,
        source: conf_json.name,
        systemCode: conf_json.systemCode,
        systemName: conf_json.systemName
    }
}(Params, Config)`

      const blob = new Blob([exampleScript], { type: 'application/javascript' })
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = 'form-mapping-example.js'
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
    }
  }
}
</script>

<style lang="scss" scoped>
::v-deep .el-form-item__label {
  width: 120px !important;
}
::v-deep .el-form-item__content {
  margin-left: 120px !important;
}
.edit-dock {
  .content-box {
    width: 100%;
    height: 100%;
    overflow-y: auto;
    max-height: 60vh;
  }
  .script-container {
    .script-actions {
      display: flex;
      justify-content: flex-end;
    }
  }
}
.cycle-list {
  display: flex;
  align-items: center;
  margin-top: 9px;
  .cycle-item {
    padding: 0 12px;
    height: 22px;
    line-height: 22px;
    text-align: center;
    font-size: 14px;
    font-weight: 400;
    border: 1px solid rgba(217, 217, 217, 1);
    cursor: pointer;
    margin-right: 8px;
  }
  .active-cycle {
    color: rgba(38, 78, 209, 1);
    border-color: rgba(38, 78, 209, 1);
  }
}
</style>