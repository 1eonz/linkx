<template>
  <div>
    <el-dialog :visible.sync="dialogVisible" :title="dialogTitle" @close="closeDialog" class="edit-dock" width="800px">
      <div class="content-box">

        <!-- 第一步：基础表单 -->
        <div v-show="currentStep === 0" class="step-content">
          <el-form ref="formRef" :model="formData" :rules="formRules" label-position="left" label-width="200px"
            style="width: 100%; padding:0 30px;box-sizing:border-box">
            <el-form-item label="名称" prop="name">
              <el-input v-model="formData.name" maxlength="100" show-word-limit placeholder="请输入" />
            </el-form-item>
            <el-form-item label="所属系统" prop="systemName">
              <el-input v-model="formData.systemName" maxlength="50" show-word-limit placeholder="请输入" />
            </el-form-item>
            <el-form-item label="系统编码" prop="systemCode">
              <el-input disabled v-model="formData.systemCode" maxlength="50" show-word-limit placeholder="自动生成" />
            </el-form-item>
            <el-form-item label="唯一标识字段" prop="uniqueId">
              <el-input v-model="formData.uniqueId" maxlength="50" show-word-limit />
            </el-form-item>
            <el-form-item label="应用类型" prop="type">
              <el-select v-model.number="formData.type" style="width:100%;" @change="changeType">
                <el-option v-for="item in interfaceTypeList" :key="item.value" :label="item.label"
                  :value="item.value"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="展示范围" prop="scope">
              <el-select v-model.number="formData.scope" style="width:100%;">
                <el-option v-for="item in scopeList" :key="item.value" :label="item.label"
                  :value="item.value"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item v-if="formData.type === 1" label="访问协议" prop="protocol">
              <el-select filterable v-model="formData.protocol" placeholder="示例：http" style="width:100%;">
                <el-option v-for="item in protocolList" :key="item.value" :label="item.value"
                  :value="item.value"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="访问IP" prop="ip">
              <el-input v-model="formData.ip" maxlength="50" show-word-limit placeholder="示例：127.0.0.0" />
            </el-form-item>
            <el-form-item label="端口" prop="port">
              <el-input v-model="formData.port" maxlength="10" show-word-limit
                :placeholder="`示例：${protocolDefaultPort}`" />
            </el-form-item>
            <el-form-item v-if="formData.type === 1" label="接口URI" prop="uri">
              <el-input v-model="formData.uri" maxlength="200" show-word-limit placeholder="示例：/getList/task" />
            </el-form-item>
            <el-form-item v-if="formData.type === 1" label="请求方式" prop="method">
              <el-select filterable v-model="formData.method" placeholder="示例：GET" style="width:100%;">
                <el-option v-for="item in methodList" :key="item.value" :label="item.value"
                  :value="item.value"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item v-if="formData.type === 1" prop="dataStartTime">
              <template #label>
                第一条数据开始时间
                <el-tooltip content="三方数据库的第一条数据开始时间" placement="top">
                  <i class="el-icon-warning-outline" style="color:#909399;cursor:pointer;" />
                </el-tooltip>
              </template>
              <el-date-picker v-model="formData.dataStartTime" type="datetime" placeholder="请选择日期时间" value-format="yyyy-MM-dd HH:mm:ss" style="width:100%;" />
            </el-form-item>
            <el-form-item v-if="formData.type === 1" label="时间字段标识" prop="dateTimeSign">
              <el-input v-model="formData.dateTimeSign" maxlength="50" show-word-limit placeholder="示例：createTime" />
            </el-form-item>
            <el-form-item v-if="formData.type === 1" label="请求Header" prop="reqHeader">
              <el-input v-model="formData.reqHeader" type="textarea" :rows="3" maxlength="2000" show-word-limit
                placeholder='示例：{"Content-Type": "application/json"}...，无则填{}' />
            </el-form-item>
            <el-form-item v-if="formData.type === 1" label="请求Body" prop="reqBody">
              <el-input v-model="formData.reqBody" type="textarea" :rows="3" maxlength="2000" show-word-limit
                placeholder='示例：{"id": "123456"}...，无则填{}' />
            </el-form-item>
            <el-form-item v-if="formData.type === 1" label="请求Params" prop="reqParam">
              <el-input v-model="formData.reqParam" type="textarea" :rows="3" maxlength="2000" show-word-limit
                placeholder='示例：{"id": "123456"}...，无则填{}' />
            </el-form-item>
            <el-form-item v-if="formData.type === 1" label="是否分页" prop="pagenation">
              <el-radio-group v-model="formData.pagenation" @change="changePagenation">
                <el-radio :label="0">不支持分页</el-radio>
                <el-radio :label="1">分页</el-radio>
              </el-radio-group>
            </el-form-item>
            <!-- 分页类型 -->
            <el-form-item v-if="formData.type === 1 && formData.pagenation === 1" label="分页类型" prop="pagenationType">
              <el-select clearable v-model="formData.pagenationType" placeholder="请选择分页类型" style="width:100%;">
                <el-option :label="'页码模式'" :value="1"></el-option>
                <el-option :label="'偏移模式'" :value="2"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item v-if="formData.type === 1 && formData.pagenation === 1" label="分页参数类型" prop="pagenation">
              <el-radio-group v-model="formData.pageParamLocation">
                <el-radio :label="0">Query参数</el-radio>
                <el-radio :label="1">Body参数</el-radio>
              </el-radio-group>
            </el-form-item>
            <!-- 页码的参数名称 -->
            <el-form-item v-if="formData.type === 1 && formData.pagenation === 1" label="页码字段" prop="pageFieldName">
              <el-input v-model="formData.pageFieldName" placeholder="示例：pageNum" />
            </el-form-item>
            <!-- 每页条目数的参数名称 -->
            <el-form-item v-if="formData.type === 1 && formData.pagenation === 1" label="每页条数字段" prop="pageSizeFieldName">
              <el-input v-model="formData.pageSizeFieldName" placeholder="示例：pageSize" />
            </el-form-item>
            <!-- 响应数据路径 -->
            <el-form-item v-if="formData.type === 1 && formData.pagenation === 1" label="响应数据路径" prop="responseDataPath">
              <el-input v-model="formData.responseDataPath" placeholder="示例：data.list" />
            </el-form-item>
            <el-form-item v-if="formData.type === 2" label="表名/视图名" prop="dataName">
              <el-input clearable v-model="formData.dataName" placeholder="示例：data_name"></el-input>
            </el-form-item>
            <el-form-item v-if="formData.type === 2" label="数据库名" prop="databaseName">
              <el-input clearable v-model="formData.databaseName" placeholder="示例：database_name"></el-input>
            </el-form-item>
            <el-form-item v-if="formData.type === 2" label="数据库类型" prop="dbType">
              <el-select v-model.number="formData.dbType" style="width:100%;">
                <el-option v-for="item in dbTypeList" :key="item.value" :label="item.label"
                  :value="item.value"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item v-if="formData.type === 2" label="数据库账号" prop="account">
              <el-input clearable v-model="formData.account" placeholder="示例：account@1234"></el-input>
            </el-form-item>
            <el-form-item v-if="formData.type === 2" label="数据库密码" prop="password">
              <el-input clearable show-password type="password" v-model="formData.password"
                placeholder="示例：Aa@123456"></el-input>
            </el-form-item>
            <el-form-item label="数据刷新周期" prop="period">
              <div class="cycle-list">
                <div class="cycle-item" :class="{ 'active-cycle': item.id === formData.period }"
                  v-for="(item, index) in periodList" :key="index" @click="clickPeriod(item)">
                  {{ item.name }}
                </div>
              </div>
            </el-form-item>
          </el-form>
        </div>

        <!-- 第二步：字段映射配置 -->
        <div v-show="currentStep === 1" class="step-content">
          <div class="mapper-container">
            <div class="mapper-header">
              <el-button type="primary" size="small" @click="handleAddMapper">
                <i class="el-icon-plus"></i>
                添加映射
              </el-button>
              <el-button size="small" @click="handleResetMapper" :disabled="mapperList.length === 0">
                <i class="el-icon-delete"></i>
                清空所有
              </el-button>
              <span class="mapper-tip">提示：字段名(key)用于展示字段，字段含义(value)用于展示字段的列标题</span>
            </div>

            <div class="mapper-list" v-if="mapperList.length > 0">
              <div v-for="(item, index) in mapperList" :key="index" class="mapper-item">
                <div class="mapper-item-fields">
                  <el-input v-model="item.key" placeholder="请输入" size="medium" :maxlength="100" show-word-limit
                    @blur="handleKeyBlur(index)">
                    <template slot="prepend">字段名</template>
                  </el-input>

                  <i class="el-icon-arrow-right arrow-icon"></i>

                  <el-input v-model="item.value" placeholder="请输入" size="medium" :maxlength="100" show-word-limit
                    @blur="handleValueBlur(index)">
                    <template slot="prepend">字段含义</template>
                  </el-input>
                </div>

                <div class="mapper-item-actions">
                  <el-button plain type="primary" size="small" icon="el-icon-top" circle @click="handleMoveUp(index)"
                    :disabled="index === 0" />
                  <el-button plain type="primary" size="small" icon="el-icon-bottom" circle
                    @click="handleMoveDown(index)" :disabled="index === mapperList.length - 1" />
                  <el-button plain type="danger" size="small" icon="el-icon-delete" circle
                    @click="handleRemoveMapper(index)" />
                </div>
              </div>
            </div>

            <el-empty v-if="mapperList.length === 0" description="暂无映射配置，请点击添加映射" :image-size="80"
              class="mapper-empty" />
          </div>
        </div>
      </div>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDialog">取 消</el-button>
          <el-button v-if="currentStep > 0" @click="prevStep">上一步</el-button>
          <el-button v-if="currentStep === 0" type="primary" @click="nextStep">下一步</el-button>
          <el-button v-if="currentStep === 1" type="primary" :disabled="formLoading" @click="handleSubmit">
            确 定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { deepCopy } from '@/utils'
import { createCallableApp, updateCallableApp, getCallableAppDetail } from '@/api/thirdInterface/southInterface'
const DEFAULT_FORM = {
  name: '',
  systemName: '',
  systemCode: '',
  uniqueId: '',
  type: 1,
  scope: '',
  ip: '',
  port: '',
  period: '30',
  mapper: '',

  protocol: '',
  uri: '',
  method: '',
  reqHeader: '',
  reqBody: '',
  reqParam: '',
  status: 0,
  pagenation: 0,
  pageParamLocation: 0,
  pageFieldName: '',
  pageSizeFieldName: '',
  responseDataPath: '',
  pagenationType: null,

  dateTimeSign: '',
  dataStartTime: '',
  appCallableId: null,
  dbType: 1,
  account: '',
  password: '',
  dataName: '',
  databaseName: ''
}

const IP_REGEX = /^(1\d{2}|2[0-4]\d|25[0-5]|[1-9]\d|[1-9])\.(1\d{2}|2[0-4]\d|25[0-5]|[1-9]\d|\d)\.(1\d{2}|2[0-4]\d|25[0-5]|[1-9]\d|\d)\.(1\d{2}|2[0-4]\d|25[0-5]|[1-9]\d|\d)$/

const PORT_VALIDATOR = (_, val, cb) => {
  const num = Number(val)
  return (num < 1 || num > 65535) ? cb(new Error('端口号范围是 1-65535')) : cb()
}

const JSON_FIELDS = [
  { key: 'reqHeader', name: '请求头' },
  { key: 'reqBody', name: '请求体' },
  { key: 'reqParam', name: '请求参数' }
]

const KEY_REGEX = /^[a-zA-Z_][a-zA-Z0-9_]*$/
const createMapperItem = (key = '', value = '') => ({ key, value })

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
      formData: deepCopy(DEFAULT_FORM),
      formType: '',
      formLoading: false,
      dialogTitle: '',
      currentStep: 0,
      mapperList: [],
      periodList: [
        { id: '5', name: '5min' },
        { id: '10', name: '10min' },
        { id: '20', name: '20min' },
        { id: '30', name: '30min' },
        { id: '60', name: '1h' },
        { id: '360', name: '6h' },
        { id: '720', name: '12h' },
        { id: '1440', name: '24h' }
      ],
      formRules: {
        name: [{ required: true, message: '名称不能为空', trigger: 'blur' }],
        systemName: [{ required: true, message: '所属系统不能为空', trigger: 'blur' }],
        systemCode: [{ required: true, message: '系统编码不能为空', trigger: 'blur' }],
        type: [{ required: true, message: '应用类型不能为空', trigger: 'blur' }],
        scope: [{ required: true, message: '展示范围不能为空', trigger: 'blur' }],
        protocol: [{ required: true, message: '访问协议不能为空', trigger: 'blur' }],
        ip: [
          { required: true, message: '访问IP不能为空', trigger: 'blur' },
          { pattern: IP_REGEX, message: '请输入正确的IP地址', trigger: 'blur' }
        ],
        port: [
          { required: true, message: '端口不能为空', trigger: 'blur' },
          { pattern: /^\d+$/, message: '端口必须是正整数', trigger: 'blur' },
          { validator: PORT_VALIDATOR, trigger: 'blur' }
        ],
        uri: [{ required: true, message: '接口名称不能为空', trigger: 'blur' }],
        method: [{ required: true, message: '请求方式不能为空', trigger: 'blur' }],
        reqHeader: [
          { required: true, message: '请求头不能为空', trigger: 'blur' },
          { validator: (_, val, cb) => this.validateJson(val, '请求头', cb), trigger: 'blur' }
        ],
        reqBody: [
          { required: true, message: '请求体不能为空', trigger: 'blur' },
          { validator: (_, val, cb) => this.validateJson(val, '请求体', cb), trigger: 'blur' }
        ],
        reqParam: [
          { required: true, message: '请求参数不能为空', trigger: 'blur' },
          { validator: (_, val, cb) => this.validateJson(val, '请求参数', cb), trigger: 'blur' }
        ],
        dateTimeSign: [{ required: true, message: '时间字段标识不能为空', trigger: 'blur' }],
        dataStartTime: [{ required: true, message: '第一条数据开始时间不能为空', trigger: 'blur' }],
        uniqueId: [{ required: true, message: '唯一标识字段不能为空', trigger: 'blur' }],
        pagenation: [{ required: true, message: '是否分页不能为空', trigger: 'blur' }],
        pageParamLocation: [{ required: true, message: '分页参数类型不能为空', trigger: 'blur' }],
        pageFieldName: [{ required: true, message: '页码字段不能为空', trigger: 'blur' }],
        pageSizeFieldName: [{ required: true, message: '每页条数字段不能为空', trigger: 'blur' }],
        dataName: [{ required: true, message: '表名/视图名不能为空', trigger: 'blur' }],
        dbType: [{ required: true, message: '数据库类型不能为空', trigger: 'blur' }],
        databaseName: [{ required: true, message: '数据库名不能为空', trigger: 'blur' }],
        account: [{ required: true, message: '数据库账号不能为空', trigger: 'blur' }],
        password: [{ required: true, message: '数据库密码不能为空', trigger: 'blur' }],
        period: [{ required: true, message: '数据刷新周期不能为空', trigger: 'blur' }]
      },
      protocolList: [
        { value: 'http', label: 'HTTP (80)', port: 80 },
        { value: 'https', label: 'HTTPS (443)', port: 443 },
      ],
      methodList: [
        { value: 'GET', label: 'GET' },
        { value: 'POST', label: 'POST' },
        { value: 'PUT', label: 'PUT' },
        { value: 'PATCH', label: 'PATCH' },
        { value: 'DELETE', label: 'DELETE' },
        { value: 'HEAD', label: 'HEAD' },
        { value: 'OPTIONS', label: 'OPTIONS' },
        { value: 'CONNECT', label: 'CONNECT' },
        { value: 'TRACE', label: 'TRACE' }
      ],
      interfaceTypeList: [
        { value: 1, label: 'RESTful接口' },
        { value: 2, label: '数据库' }
      ],
      scopeList: [
        { value: 1, label: 'PC端' },
      ],
      dbTypeList: [
        { value: 1, label: 'MySQL' },
      ]
    }
  },
  computed: {
    protocolDefaultPort() {
      return this.protocolList.find(item => item.value === this.formData.protocol)?.port || '8000'
    }
  },
  watch: {
    dialogVisible(val) {
      if (!val) this.$refs.formRef.resetFields()
    }
  },
  methods: {
    validateJson(value, fieldName, callback) {
      if (value && value.trim()) {
        try {
          JSON.parse(value)
          callback()
        } catch (e) {
          callback(new Error(`${fieldName}必须是有效的JSON格式`))
        }
      } else {
        callback()
      }
    },

    validateMapper() {
      if (this.mapperList.length === 0) {
        this.$message.error('请至少添加一个字段映射');
        return false;
      }

      const keysSet = new Set();
      for (let i = 0; i < this.mapperList.length; i++) {
        const item = this.mapperList[i];
        if (!item.key || !item.key.trim()) {
          this.$message.error(`第 ${i + 1} 个映射的“字段名”不能为空`);
          return false;
        }
        if (!item.value || !item.value.trim()) {
          this.$message.error(`第 ${i + 1} 个映射的“字段含义”不能为空`);
          return false;
        }

        const trimmedKey = item.key.trim();
        const KEY_REGEX = /^[a-zA-Z_][a-zA-Z0-9_]*$/;
        if (!KEY_REGEX.test(trimmedKey)) {
          this.$message.error(`字段名“${trimmedKey}”只能包含字母、数字、下划线，且不能以数字开头`);
          return false;
        }

        if (keysSet.has(trimmedKey)) {
          this.$message.error(`字段名“${trimmedKey}”重复，请使用唯一的字段名`);
          return false;
        }
        keysSet.add(trimmedKey);
      }
      return true;
    },

    clickPeriod(item) {
      this.formData.period = item.id
    },

    open(type, row) {
      this.dialogTitle = type === 'create' ? '新增' : '修改'
      this.formType = type
      this.currentStep = 0
      this.formLoading = true

      if (type === 'create') {
        this.formData = deepCopy({ ...DEFAULT_FORM, systemCode: this.generateSystemCode() })
        this.mapperList = []
        this.formLoading = false
        this.dialogVisible = true
      } else {
        this.getCallableAppDetail(row.id)
        this.formLoading = false
        this.dialogVisible = true
      }
    },

    async getCallableAppDetail(id) {
      const res = await getCallableAppDetail(id)
      const { code, msg, data } = res || {}

      if (data) {
        this.formData = {
          ...deepCopy(DEFAULT_FORM),
          id: data.id,
          name: data.name,
          systemName: data.systemName,
          systemCode: data.systemCode,
          uniqueId: data.uniqueId,
          type: data.type,
          scope: data.scope,
          ip: data.ip,
          port: data.port,
          period: String(data.period || '30')
        }

        if (data.type === 1) {
          this.formData.protocol = data.protocol || ''
          this.formData.uri = data.uri || ''
          this.formData.method = data.method || ''
          this.formData.reqHeader = data.reqHeader || '{}'
          this.formData.reqBody = data.reqBody || '{}'
          this.formData.reqParam = data.reqParam || '{}'
          this.formData.pagenation = data.pagenation || 0
          this.formData.pageParamLocation = data.pageParamLocation || 0
          this.formData.pageFieldName = data.pageFieldName
          this.formData.pageSizeFieldName = data.pageSizeFieldName
          this.formData.responseDataPath = data.responseDataPath || ''
          this.formData.pagenationType = data.pagenationType || null
          this.formData.dateTimeSign = data.dateTimeSign || ''
          this.formData.dataStartTime = data.dataStartTime || ''
        }

        if (data.type === 2) {
          this.formData.dbType = data.dbType || 1
          this.formData.account = data.account || ''
          this.formData.password = data.password || ''
          this.formData.dataName = data.dataName || ''
          this.formData.databaseName = data.databaseName || ''
        }

        let parsedMapper = []
        try { parsedMapper = JSON.parse(data.mapper || '[]') } catch (e) { }
        this.mapperList = parsedMapper
      } else {
        this.formData = deepCopy({ ...DEFAULT_FORM, systemCode: this.generateSystemCode() })
        this.mapperList = []
      }
    },

    closeDialog() {
      this.dialogVisible = false
      this.currentStep = 0
      this.mapperList = []
    },

    nextStep() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return
        if (this.ensureJsonFields()) {
          this.currentStep = 1
        }
      })
    },

    prevStep() {
      this.currentStep = 0
    },

    handleSubmit() {
      // 校验mapper
      if (!this.validateMapper()) return;

      this.formLoading = true
      const submitData = this.buildSubmitData()

      const isCreate = this.formType === 'create'
      let res
      if (isCreate) {
        res = createCallableApp(submitData)
      } else {
        res = updateCallableApp(this.formData.id, submitData)
      }

      res.then(result => {
        this.formLoading = false
        if (result.code === 0) {
          this.$emit('success')
          this.dialogVisible = false
          this.$message.success(isCreate ? '新增成功' : '修改成功')
        } else {
          this.$message.error(result.msg || (isCreate ? '新增失败，请稍后重试' : '修改失败，请稍后重试'))
        }
      }).catch(() => {
        this.formLoading = false
        this.$message.error(isCreate ? '新增失败，请稍后重试' : '修改失败，请稍后重试')
      })
    },

    ensureJsonFields() {
      if (this.formData.type !== 1) return true

      for (const { key, name } of JSON_FIELDS) {
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

    buildSubmitData() {
      const data = {
        name: this.formData.name,
        systemName: this.formData.systemName,
        systemCode: this.formData.systemCode,
        uniqueId: this.formData.uniqueId,
        type: this.formData.type,
        scope: this.formData.scope,
        ip: this.formData.ip,
        port: Number(this.formData.port),
        period: Number(this.formData.period),
        protocol: this.formData.protocol,
        uri: this.formData.uri,
        method: this.formData.method,
        reqHeader: this.formData.reqHeader,
        reqBody: this.formData.reqBody,
        reqParam: this.formData.reqParam,
        pagenation: this.formData.pagenation,
        pageParamLocation: this.formData.pageParamLocation,
        pageFieldName: this.formData.pageFieldName,
        pageSizeFieldName: this.formData.pageSizeFieldName,
        responseDataPath: this.formData.responseDataPath,
        pagenationType: this.formData.pagenationType,
        dateTimeSign: this.formData.dateTimeSign,
        dataStartTime: this.formData.dataStartTime,
        dbType: this.formData.dbType,
        account: this.formData.account,
        password: this.formData.password,
        dataName: this.formData.dataName,
        databaseName: this.formData.databaseName,
        mapper: this.mapperList.length > 0 ? JSON.stringify(this.mapperList) : ''
      }

      return data
    },

    generateSystemCode() {
      const timestamp = Math.floor(Date.now() / 1000).toString()
      const randomNum = Math.floor(Math.random() * 100000).toString().padStart(5, '0')
      return timestamp + randomNum
    },

    changeType(val) {
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate()
      })
      if (val == 1) {
        this.formData.dbType = ''
        this.formData.account = ''
        this.formData.password = ''
        this.formData.dataName = ''
        this.formData.databaseName = ''
      } else if (val == 2) {
        this.formData.protocol = ''
        this.formData.method = ''
        this.formData.reqBody = ''
        this.formData.reqHeader = ''
        this.formData.reqParam = ''
        this.formData.uri = ''
        this.formData.pagenation = 0
        this.formData.pageParamLocation = 0
        this.formData.pageFieldName = ''
        this.formData.pageSizeFieldName = ''
        this.formData.dateTimeSign = ''
        this.formData.dataStartTime = ''
      }
    },

    changePagenation(val) {
      this.formData.pageParamLocation = 0
      this.formData.pageFieldName = ''
      this.formData.pageSizeFieldName = ''
    },

    handleAddMapper() {
      this.mapperList.push(createMapperItem())
    },

    handleRemoveMapper(index) {
      this.mapperList.splice(index, 1)
    },

    handleResetMapper() {
      this.$confirm('确定要清空所有映射配置吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.mapperList = []
      }).catch(() => { })
    },

    swapItems(indexA, indexB) {
      const temp = this.mapperList[indexA]
      this.$set(this.mapperList, indexA, this.mapperList[indexB])
      this.$set(this.mapperList, indexB, temp)
    },

    handleMoveUp(index) {
      if (index > 0) this.swapItems(index, index - 1)
    },

    handleMoveDown(index) {
      if (index < this.mapperList.length - 1) this.swapItems(index, index + 1)
    },

    handleKeyBlur(index) {
      const item = this.mapperList[index]
      if (!item.key || !item.key.trim()) return
      const trimmedKey = item.key.trim()

      const isDuplicate = this.mapperList.some((other, idx) =>
        idx !== index && other.key === trimmedKey
      )
      if (isDuplicate) {
        this.$message.warning(`字段名 "${trimmedKey}" 已存在，请使用唯一的字段名`)
      }
      if (!KEY_REGEX.test(trimmedKey)) {
        this.$message.warning('字段名只能包含字母、数字、下划线，且不能以数字开头')
      }

      item.key = trimmedKey
    },

    handleValueBlur(index) {
      const item = this.mapperList[index]
      if (item.value) {
        item.value = item.value.trim()
      }
    },
  }
}
</script>

<style scoped lang="scss">
.edit-dock {
  ::v-deep .el-dialog__body {
    padding: 20px;
    display: flex;
    flex-direction: column;
    max-height: 60vh;
  }

  .content-box {
    display: flex;
    flex-direction: column;
    width: 100%;
    flex: 1;
    overflow-y: auto;
  }

  .step-content {
    width: 100%;
  }

  .mapper-container {
    display: flex;
    flex-direction: column;
    height: 400px;
    padding: 0 16px;
  }

  .mapper-header {
    flex-shrink: 0;
    display: flex;
    align-items: center;
    margin-bottom: 16px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e4e7ed;
    flex-wrap: wrap;
  }

  .mapper-header .mapper-tip {
    font-size: 12px;
    color: #a9a9af;
    margin-left: auto;
    align-self: flex-end;
  }

  .mapper-list {
    flex: 1;
    overflow-y: auto;
    display: flex;
    gap: 4px;
    flex-direction: column;
    padding-right: 8px;
  }

  .mapper-list::-webkit-scrollbar {
    width: 6px;
  }

  .mapper-list::-webkit-scrollbar-thumb {
    background-color: #dcdfe6;
    border-radius: 3px;
  }

  .mapper-list::-webkit-scrollbar-thumb:hover {
    background-color: #c0c4cc;
  }

  .mapper-item {
    display: flex;
    align-items: center;
    padding: 8px;
    border-radius: 8px;
    transition: all 0.3s;
  }

  .mapper-item:hover {
    background-color: #f9fbff;
  }

  .mapper-item .mapper-item-fields {
    flex: 1;
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .mapper-item .mapper-item-fields :deep(.el-input-group__prepend) {
    width: 70px;
    justify-content: center;
  }

  .mapper-item .arrow-icon {
    color: #909399;
    font-size: 20px;
    flex-shrink: 0;
  }

  .mapper-item .mapper-item-actions {
    display: flex;
    flex-shrink: 0;
    margin-left: 16px;
  }

  .mapper-container :deep(.el-empty) {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
  }

  .mapper-empty {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}

.cycle-list {
  display: flex;
  align-items: center;
  margin-top: 9px;
  flex-wrap: wrap;

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
    margin-bottom: 8px;
  }

  .active-cycle {
    color: rgba(38, 78, 209, 1);
    border-color: rgba(38, 78, 209, 1);
  }
}
</style>
