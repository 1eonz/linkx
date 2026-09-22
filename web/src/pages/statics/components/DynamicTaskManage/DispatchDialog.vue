<template>
  <div class="dispatch-dialog glass-light">
    <div class="dialog-header">
      <span class="dialog-close" @click="handleClose">✕</span>
    </div>

    <div class="dispatch-content" v-loading="loading">
      <!-- 左侧：派发表单 -->
      <div class="form-panel">
        <h3 class="form-title">
          <span class="blue-block"></span>
          任务派发
        </h3>

        <div class="form-body">
          <el-form
            class="dispatch-form"
            ref="formRef"
            :model="form"
            :rules="rules"
            label-position="top"
          >
            <!-- 上部：所属系统 | 任务名称 -->
            <div class="form-section form-section-top">
              <el-row :gutter="32">
              <el-col :span="12">
                <el-form-item label="所属系统" prop="taskSystem">
                  <el-input v-model="form.taskSystem" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="任务名称" prop="name">
                  <el-input v-model="form.name" placeholder="请输入任务名称" />
                </el-form-item>
              </el-col>
            </el-row>
            </div>

            <!-- 中部：任务内容 -->
            <div class="form-section form-section-middle">
              <el-form-item label="任务内容" prop="content">
                <el-input
                  v-model="form.content"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入任务内容"
                />
              </el-form-item>
            </div>

            <!-- 下部：业务类型/执行人 + 时间 + 等级/紧急 -->
            <div class="form-section form-section-bottom">
              <el-row :gutter="32">
              <el-col :span="12">
                <el-form-item label="业务类型" prop="businessType">
                  <el-input v-model="form.businessType" disabled />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="执行人" prop="executor">
                    <!-- value 绑定 user 对象：change 时数据随事件到手，不回查 userList，避免搜索替换列表后选中项脱钩（LINKXSPC176B005） -->
                    <el-select
                      v-model="form.executor"
                      placeholder="请选择执行人"
                      filterable
                      clearable
                      value-key="id"
                      fit-input-width
                      :filter-method="handleFilter"
                      :loading="userListLoading"
                      loading-text="搜索中..."
                      popper-class="executor-select-popper"
                      style="width: 100%"
                    >
                      <!-- 请求在途时隐藏旧选项：避免用户点到即将被替换的旧列表（点选瞬间列表刷新导致落点偏移） -->
                      <el-option
                        v-if="!userListLoading"
                        v-for="user in userList"
                        :key="user.id"
                        :label="user.name"
                        :value="user"
                      >
                        <span style="float: left">{{ user.name }}</span>
                      </el-option>
                      <el-option
                        v-if="hasMoreUsers"
                        key="__load-more__"
                        :label="userLoading ? '加载中...' : '已加载 ' + userList.length + '/' + userTotal + '，滚动到底部加载更多'"
                        value="__load-more__"
                        disabled
                      />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="32">
              <el-col :span="12">
                <el-form-item label="任务开始时间" prop="startTime">
                  <CustomDatePicker
                    v-model="form.startTime"
                    type="datetime"
                    placeholder="请选择开始时间"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="任务结束时间" prop="endTime">
                  <CustomDatePicker
                    v-model="form.endTime"
                    type="datetime"
                    placeholder="请选择结束时间"
                    value-format="YYYY-MM-DD HH:mm:ss"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="32">
              <el-col :span="12">
                <el-form-item label="任务等级" prop="level">
                  <CustomSelect
                    v-model="form.level"
                    :options="taskLevelList"
                    placeholder="请选择任务等级"
                    @change="changeTaskLevel"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="是否紧急" prop="urgent">
                  <el-switch
                    v-model="form.urgent"
                    :active-color="'#264ed1'"
                    :inactive-color="'#c0c4cc'"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            </div>
          </el-form>
        </div>

        <div class="dialog-footer">
          <el-button class="btn btn-cancel" @click="handleClose">取消</el-button>
          <el-button class="btn btn-confirm" type="primary" @click="handleSubmit" :loading="loading">
            派发
          </el-button>
        </div>
      </div>

      <!-- 右侧：详情展示 -->
      <div class="detail-panel" v-if="props.columns && props.columns.length > 0">
        <h3 class="section-title">详情</h3>
        <div class="detail-list">
          <div class="detail-item" v-for="col in props.columns" :key="col.prop">
            <span class="detail-label">{{ col.label }}</span>
            <span class="detail-value">{{ formatDisplayText(props.row[col.prop]) }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted, defineProps } from 'vue'
import { ElMessage } from 'element-plus'
import { debounce } from 'lodash-es'
import { dispatchTask, getUserList } from '@/api/taskManage'
import { Dialog } from '@/components/Dialog/src/helper'
import { getUserInfo } from '@/bridge/post.js'
import { formatDisplayText } from '@/utils/formatter'
import CustomSelect from '@/customComponents/CustomSelect/index.vue'
import CustomDatePicker from '@/customComponents/CustomDatePicker/index.vue'
const props = defineProps({
  row: {
    type: Object,
    default: () => ({})
  },
  columns: {
    type: Array,
    default: () => []
  },
  callableId: {
    type: String,
    default: ''
  },
  tableName: {
    type: String,
    default: ''
  },
  onSuccess: {
    type: Function,
    default: null
  },
  taskTab: {
    type: Object,
    default: () => ({})
  },
  // 任务派发模板配置（JSON 串，由父组件切换 tab 时调用接口获取）
  // 解析后为数组，每项形如 { id, name, type, value: { type, ... } }
  // id 与 form 字段名一致：name / content / startTime / endTime / level / urgent
  taskAutoFillConfig: {
    type: String,
    default: ''
  }
})

// 任务等级列表
const taskLevelList = ref([
  { label: '一般', value: '一般' },
  { label: '紧急', value: '紧急' },
])

const formRef = ref()
const loading = ref(false)

// 表单数据（字段名与后端 taskAutoFillConfig 的 id 对齐：name/content/level/urgent/startTime/endTime）
const form = reactive({
  taskNumber: '',
  name: '',
  content: '',
  taskSystem: '',
  businessType: '',
  level: '',
  urgent: false,
  executor: null, // 执行人：存选中的 user 对象（含 id/name/idCard/primaryDepartment），提交时实时派生 executors
  startTime: '',
  endTime: '',
  extendedDesc: '',
})
// 获取当前用户信息
const userInfo = ref({})

// 用户列表
const userList = ref([])
// 用户分页状态：滚动到底部自动加载下一页
const userPageNum = ref(1)
const userTotal = ref(0)
const userLoading = ref(false)
// 当前列表实际使用的搜索关键词（可能与 searchKeyword 短暂不一致：输入后防抖搜索触发前）
const userQuery = ref('')
const hasMoreUsers = computed(() => userList.value.length < userTotal.value)
// 用户列表远程搜索加载中
const userListLoading = ref(false)
// 搜索关键词
const searchKeyword = ref('')
// 搜索请求 AbortController
let searchAbortController = null

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入任务名称', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入任务内容', trigger: 'blur' }
  ],
  executor: [
    { required: true, message: '请选择执行人', trigger: 'change' }
  ],
  startTime: [
    { required: true, message: '请选择任务开始时间', trigger: 'change' }
  ],
  endTime: [
    { required: true, message: '请选择任务结束时间', trigger: 'change' }
  ],
  level: [
    { required: true, message: '请选择任务等级', trigger: 'change' }
  ],
  urgent: [
    { required: true, message: '请选择任务是否紧急', trigger: 'change' }
  ],
  extendedDesc: [
    { required: true, message: '任务扩展描述不能为空', trigger: 'change' }
  ],
}

// 模板配置项的 id 与 form 字段名一一对应（name/content/level/urgent/startTime/endTime），
// 因此无需额外映射表，直接以 item.id 作为 form 字段名写入即可。

// 解析 template 类型：将 {{fieldName}} 替换为 row[fieldName]
const resolveTemplate = (template, row) => {
  return template.replace(/\{\{(\w+)\}\}/g, (_, field) => {
    return row[field] !== undefined && row[field] !== null ? String(row[field]) : ''
  })
}

const MS_PER_HOUR = 3600000;

// 将 Date 格式化为指定格式字符串
const formatDate = (target, format) => {
  const fmt = format || 'YYYY-MM-DD HH:mm:ss'
  const pad = (n) => String(n).padStart(2, '0')
  const map = {
    'YYYY': target.getFullYear(),
    'MM': pad(target.getMonth() + 1),
    'DD': pad(target.getDate()),
    'HH': pad(target.getHours()),
    'mm': pad(target.getMinutes()),
    'ss': pad(target.getSeconds()),
  }
  return fmt.replace(/YYYY|MM|DD|HH|mm|ss/g, (token) => map[token])
}

// 解析 expression 类型：时间字段表达式
// 支持两类：
//   1) {{字段名}} 模板格式（如 {{startTime}}，与任务名称/内容字段格式一致），
//      以及存量配置的裸字段名（如 DisTime、LastEditTime）：从 row 中取对应字段值
//      字段名大小写不敏感匹配，兼容毫秒时间戳 / 秒时间戳 / 日期字符串
//   2) 字段名在 row 中不存在或表达式为空：返回空字符串，由调用方决定兜底
const resolveExpression = (expression, format, row) => {
  let expr = (expression || '').trim()
  const fmt = format || 'YYYY-MM-DD HH:mm:ss'

  // 空表达式：不回填，由调用方兜底
  if (!expr) return ''

  // 兼容 {{字段名}} 模板格式：提取其中的字段名（存量裸字段名格式继续兼容）
  const templateMatch = expr.match(/^\{\{([^{}\s}][^{}]*)\}\}$/)
  if (templateMatch) expr = templateMatch[1].trim()

  // 字段名：大小写不敏感匹配
  if (row && typeof row === 'object') {
    // 精确匹配优先
    let raw = row[expr]
    // 精确匹配失败 → 大小写不敏感匹配
    if (raw === undefined || raw === null || raw === '') {
      const lowerExpr = expr.toLowerCase()
      const key = Object.keys(row).find(k => k.toLowerCase() === lowerExpr)
      if (key) raw = row[key]
    }

    if (raw !== undefined && raw !== null && raw !== '') {
      // 兼容毫秒时间戳 / 秒时间戳 / 日期字符串
      let date
      const rawStr = String(raw).trim()
      if (typeof raw === 'number' || /^\d{10,13}$/.test(rawStr)) {
        // 数字或纯数字字符串（如三方返回的 "1720000000000"）：根据数量级判断是毫秒还是秒
        const num = Number(rawStr)
        date = num > 1e12 ? new Date(num) : new Date(num * 1000)
      } else {
        // 字符串：先把 ISO 的 T 分隔符替换为空格，再把 - 替换为 / 兼容 iOS Safari
        // 兼容 2026-07-07T20:18:57 / 2026-07-07 20:18:57 / 2026-07-07 等格式
        const normalized = rawStr.replace(/T/i, ' ').replace(/-/g, '/')
        date = new Date(normalized)
      }
      if (isNaN(date.getTime())) {
        // 无法解析为日期 → 直接返回原始字符串（让用户看到值，自行修改）
        return String(raw)
      }
      return formatDate(date, fmt)
    }
  }

  // 字段名在 row 中不存在：返回空字符串，由调用方兜底
  return ''
}

// 解析 variable 类型：直接取 admin 任务标准件配置的默认值
// （admin 仅支持配置 default，任务等级/是否紧急不从行数据取字段值）
const resolveVariable = (config) => {
  return config.default !== undefined ? config.default : ''
}

// 已配置时间表达式的字段集合：这些字段即使三方数据取不到值，也不用默认时间兜底
const expressionConfiguredFields = new Set()
// 模板配置允许回填的表单字段白名单。
// executor 必须由用户手动选择（value 为 user 对象），禁止模板直接回填字符串导致与 executors 脱钩。
const TEMPLATE_FILLABLE_FIELDS = ['name', 'content', 'startTime', 'endTime', 'level', 'urgent']

// 根据模板配置自动填充表单默认值
// templateConfig 为数组，每项形如 { id, name, type, value: { type, ... } }
// id 与 form 字段名一致：name / content / startTime / endTime / level / urgent
const fillFormFromTemplate = (templateConfig) => {
  if (!Array.isArray(templateConfig)) return
  expressionConfiguredFields.clear()

  templateConfig.forEach(item => {
    const formField = item.id
    // 仅允许写入白名单内的字段，避免塞入意外字段（如 executor）
    if (!formField || !TEMPLATE_FILLABLE_FIELDS.includes(formField) || !item.value) return

    let value
    switch (item.value.type) {
      case 'template':
        value = resolveTemplate(item.value.template, props.row)
        // 解析后为空或仅剩分隔符，不设置值
        if (!value || value === '-') return
        break
      case 'expression':
        // 已配置字段表达式时记录该字段，即使数据对象无此属性也不用默认时间兜底
        if ((item.value.expression || '').trim()) {
          expressionConfiguredFields.add(formField)
        }
        value = resolveExpression(item.value.expression, item.value.format, props.row)
        // 解析后为空，不设置值（展示空，由 initForm 决定是否兜底）
        if (value === null || value === '') return
        break
      case 'variable':
        value = resolveVariable(item.value)
        // 解析后为空，不设置值
        if (value === undefined || value === null || value === '') return
        break
      default:
        return
    }

    // urgent 字段需归一化为布尔
    if (formField === 'urgent') {
      form.urgent = value === true || value === 1 || (typeof value === 'string' && value.toLowerCase() === 'true')
    } else {
      form[formField] = value
    }
  })
}

// 初始化表单数据
const initForm = async () => {
  // 基础字段：所属系统和业务类型
  form.taskSystem = props.taskTab.systemName
  form.businessType = props.taskTab._name

  // 从接口下发的 taskAutoFillConfig（JSON 串）解析模板配置并回填表单默认值
  let templateConfig = []
  try {
    if (props.taskAutoFillConfig) {
      const parsed = JSON.parse(props.taskAutoFillConfig)
      if (Array.isArray(parsed)) {
        templateConfig = parsed
      }
    }
  } catch (error) {
    console.warn('解析任务派发模板配置失败，不回填默认值:', error)
  }
  fillFormFromTemplate(templateConfig)

  // 兜底：仅当未配置时间表达式时才取默认时间；已配置表达式但数据取不到值的字段保持为空
  const now = new Date()
  if (!form.startTime && !expressionConfiguredFields.has('startTime')) {
    form.startTime = formatDate(now, 'YYYY-MM-DD HH:mm:ss')
  }
  if (!form.endTime && !expressionConfiguredFields.has('endTime')) {
    form.endTime = formatDate(new Date(now.getTime() + MS_PER_HOUR), 'YYYY-MM-DD HH:mm:ss')
  }

  // 将三方数据转换为JSON作为扩展描述
  const { system_name, name, linkxStatusInfo, ...otherData } = props.row
  form.extendedDesc = JSON.stringify(otherData, null, 2)

  // 加载用户列表（不阻塞弹窗展示，后台加载；输入关键词时走服务端搜索）
  loadUserList()
}

// 搜索请求序号：响应落地前对号，旧响应即使返回也不落地（防乱序双保险）
let searchSeq = 0

// 最近一次请求的关键字（含挂载时的首次加载），用于聚焦触发的空过滤去重
let requestedKeyword = null

// 加载用户列表
// append = false：加载第 1 页并替换列表（首屏加载 / 关键词搜索）
// append = true：加载下一页并追加（下拉滚动到底部触发）
const loadUserList = async (keywords = '', append = false) => {
  const seq = ++searchSeq
  // 仅搜索（非翻页）时更新请求关键词与搜索 loading：翻页追加不隐藏旧列表
  if (!append) {
    requestedKeyword = keywords || searchKeyword.value
    userListLoading.value = true
  }
  userLoading.value = true
  // 请求被更新的请求中止时，finally 不重置 loading，避免误清进行中请求的状态
  const controller = new AbortController()
  if (searchAbortController) {
    searchAbortController.abort()
  }
  searchAbortController = controller
  try {
    const pageNum = append ? userPageNum.value + 1 : 1
    const params = {
      pageNum,
      // 首屏仅加载少量人员用于展示，避免一次性拉取全量导致弹窗打开缓慢；
      // 用户输入关键词时走服务端搜索（handleFilter），由 keywords 过滤后返回
      pageSize: 50,
      keywords: keywords || searchKeyword.value,
      includeChildren: 1,
      code: userInfo.value?.department?.departmentCode || ''
    }
    const res = await getUserList(params, { abort: controller.signal })
    // 已有更新的搜索请求发出，本次响应作废
    if (seq !== searchSeq) return
    if (res.code === 0) {
      userPageNum.value = pageNum
      userTotal.value = res.data.total || 0
      userQuery.value = params.keywords
      const records = res.data.records || []
      userList.value = append ? userList.value.concat(records) : records
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    if (searchAbortController === controller) {
      userLoading.value = false
      // 仅当没有更新的请求在途时才结束搜索 loading，避免旧请求提前关闭 loading
      if (seq === searchSeq) {
        userListLoading.value = false
      }
    }
  }
}

// 用户下拉滚动到底部时自动加载下一页
// scroll 事件不冒泡，且 el-select 下拉挂载在 body 上，
// 因此在 document 捕获阶段监听，并通过 popper-class 识别执行人下拉的滚动容器
const handleUserListScroll = (e) => {
  const el = e.target
  if (!el || typeof el.closest !== 'function' || !el.closest('.executor-select-popper')) return
  if (userLoading.value || !hasMoreUsers.value) return
  // 关键词已变化但防抖搜索尚未触发时，跳过翻页，
  // 避免用新关键词的下一页拼接旧关键词的列表
  if (searchKeyword.value !== userQuery.value) return
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 20) {
    loadUserList('', true)
  }
}

// 防抖搜索 - 300ms 防抖，防止快速输入时多次请求
const debouncedLoadUserList = debounce((keyword) => {
  loadUserList(keyword)
}, 300)

// 过滤方法
const handleFilter = (keyword) => {
  // 聚焦时 EP 会触发一次空关键字过滤，与挂载时的首次加载重复；
  // 同关键字的请求在途或已完成（有数据）时直接跳过，避免重复请求和 loading 闪烁
  if (keyword === requestedKeyword && (userListLoading.value || userList.value.length > 0)) return
  searchKeyword.value = keyword
  debouncedLoadUserList(keyword)
}

// 处理任务等级变化
const changeTaskLevel = (level) => {
  form.level = level
}

// 获取用户信息
async function fetchUserInfo() {
  try {
    const res = await getUserInfo();
    if (res) {
      userInfo.value = res;
    }
  } catch (error) {
    console.error(`获取用户信息错误: ${error.message}`);
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    const valid = await formRef.value.validate()
    if (!valid) return
    
    loading.value = true
    
    // 构建符合接口要求的提交数据
    const submitData = {
      // 事务相关字段
      appCallableTableName: props.tableName,
      appCallableTableId: props.row.linkx_id,
      
      // 任务基本信息
      number: form.taskNumber, // 任务编号
      name: form.name, // 任务名称
      content: form.content, // 任务内容
      level: form.level, // 任务等级
      urgent: form.urgent ? 1 : 0, // 是否紧急任务，转换为 int
      // 创建人信息（从当前登录用户获取，这里需要后端补充）
      
      creator: {
        name: userInfo.value.username, // 需要从实际登录信息中获取
        idCard: userInfo.value.idCard, // 身份证号
        department: userInfo.value?.department?.departmentName || '', // 所属部门
        departmentId: userInfo.value?.department?.departmentId || '', // 所属部门 ID
        departmentCode: userInfo.value?.department?.departmentCode || '' // 所属部门编码
      },
      
      // 执行人信息：提交时从选中的 user 对象实时派生，与 toUserId 同源，保证两者一致
      toUserId: form.executor?.id, // 执行人 ID
      executors: form.executor ? [{
        name: form.executor.name,
        idCard: form.executor.idCard || '',
        department: form.executor.primaryDepartment?.departmentName || '',
        departmentId: form.executor.primaryDepartment?.departmentId || '',
        departmentCode: form.executor.primaryDepartment?.departmentCode || ''
      }] : [], // 执行人列表
      
      // 时间信息（转换为毫秒值）
      startTime: new Date(form.startTime).getTime(),
      endTime: new Date(form.endTime).getTime(),
      
      // 扩展描述
      extend: form.extendedDesc,
      
      // 其他字段
      taskSignType: 0 // 任务签收类型固定为 0
    }
    
    // 调用 API 派发任务
    const res = await dispatchTask(props.callableId, submitData)
    
    if (res.code === 0) {
      if (props.onSuccess) {
        props.onSuccess()
      }
      Dialog('DispatchDialog')?.close()
    } else {
      ElMessage.error(res.message || '任务派发失败')
    }
    
  } catch (error) {
    console.error('派发任务失败:', error)
    ElMessage.error('派发任务失败')
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  Dialog('DispatchDialog')?.close()
}

onMounted(async () => {
  // 捕获阶段监听用户下拉滚动，实现滚动到底部自动加载下一页
  document.addEventListener('scroll', handleUserListScroll, true)
  loading.value = true
  try {
    await fetchUserInfo()
    await initForm()
  } finally {
    loading.value = false
  }
})

onUnmounted(() => {
  // 取消防抖函数，防止组件卸载后执行
  debouncedLoadUserList.cancel()
  // 移除滚动监听
  document.removeEventListener('scroll', handleUserListScroll, true)
  // 中止进行中的人员列表请求
  if (searchAbortController) {
    searchAbortController.abort()
  }
})
</script>

<style scoped lang="less">
.dispatch-dialog {
  display: flex;
  flex-direction: column;
  width: 950px;
  height: 668px;
  border-radius: 2px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  background: var(--background-color-white, #fff);

  .dialog-header {
    display: flex;
    justify-content: flex-end;
    align-items: center;
    height: 50px;
    padding: 0 20px;
    flex-shrink: 0;
    border-bottom: 1px solid #ebeef5;

    .dialog-close {
      cursor: pointer;
      font-size: 16px;
      color: var(--text-color, #999);
      line-height: 1;
      padding: 2px 4px;
      border-radius: 3px;
      transition: all 0.15s;

      &:hover {
        background: var(--hover-color, #f5f5f5);
        color: var(--text-color, #333);
      }
    }
  }

  .dispatch-content {
    flex: 1;
    display: flex;
    overflow: hidden;

    // 左侧：表单 + 按钮
    .form-panel {
      flex: 1;
      min-width: 0;
      display: flex;
      flex-direction: column;
      padding: 0 28px 24px 28px;
      border-right: 1px solid #ebeef5;
      overflow: hidden;

      .form-title {
        display: flex;
        align-items: center;
        margin: 16px 0 20px 0;
        font-size: 16px;
        font-weight: 600;
        color: var(--text-color, #1d2129);
        flex-shrink: 0;

        .blue-block {
          display: inline-block;
          width: 3px;
          height: 14px;
          margin-right: 6px;
          background: #264ed1;
          border-radius: 2px;
        }
      }

      .form-body {
        flex: 1;
        min-height: 0;
        overflow-x: hidden;
        overflow-y: auto;

        .dispatch-form.el-form {
          ::v-deep .el-form-item__label {
            font-size: 14px;
            color: #606266 !important;
            line-height: 20px !important;
            height: auto !important;
            padding: 0 0 8px 0;
          }

          ::v-deep .el-form-item {
            margin-bottom: 20px;
          }

          ::v-deep .el-input__wrapper {
            background-color: var(--search-bg) !important;
            border: 1px solid var(--border-color) !important;
            border-radius: 4px !important;
            box-shadow: none !important;
            outline: none !important;
            padding: 0 12px;
            height: 36px;
            min-height: 36px;

            &:hover,
            &.is-focus {
              box-shadow: none !important;
              border-color: var(--border-color) !important;
            }
          }

          ::v-deep .el-form-item__content {
            overflow-x: hidden;
          }

          .form-section {
            &.form-section-top {
              flex-shrink: 0;
            }

            &.form-section-middle {
              flex: 1;
              min-height: 0;
              overflow-x: hidden;
            }

            &.form-section-bottom {
              flex-shrink: 0;
            }
          }
        }
      }

      .dialog-footer {
        display: flex;
        justify-content: flex-end;
        align-items: center;
        padding-top: 16px;
        gap: 12px;
        flex-shrink: 0;

        .btn {
          padding: 7px 24px;
          border-radius: 2px;
          font-size: 14px;
          cursor: pointer;
          outline: none;
          transition: all 0.2s;
        }

        .btn-cancel {
          background: var(--button-text-inner);
          border-color: var(--button-border-color, #d9d9d9);
          color: var(--text-color, #555);

          &:hover {
            border-color: var(--tabs-active-color, #264ed1);
            color: var(--tabs-active-color, #264ed1);
          }
        }

        .btn-confirm {
          background: var(--button-active-color, #264ed1);
          color: #fff;
        }
      }
    }

    // 右侧：详情
    .detail-panel {
      flex: 0 0 334px;
      display: flex;
      flex-direction: column;
      padding: 0 24px 20px 24px;
      background: #fafafa;
      overflow: hidden;

      * {
        user-select: text;
      }

      .section-title {
        display: flex;
        align-items: center;
        margin: 16px 0 20px 0;
        font-size: 16px;
        font-weight: 600;
        color: var(--text-color, #1d2129);
        flex-shrink: 0;

        &::before {
          content: '';
          display: inline-block;
          width: 3px;
          height: 14px;
          margin-right: 6px;
          background: #264ed1;
          border-radius: 2px;
        }
      }

      .detail-list {
        flex: 1;
        overflow-y: auto;
        padding-right: 4px;
        border: 1px solid #ebeef5;
        border-radius: 4px;

        .detail-item {
          display: flex;
          font-size: 13px;
          line-height: 1.6;
          border-bottom: 1px solid #ebeef5;

          .detail-label {
            flex-shrink: 0;
            width: 40%;
            padding: 8px 12px;
            color: #909399;
            background: #fafafa;
            border-right: 1px solid #ebeef5;
          }

          .detail-value {
            flex: 1;
            min-width: 0;
            padding: 8px 12px;
            color: var(--text-color, #1d2129);
            word-break: break-all;
          }
        }

        &::-webkit-scrollbar {
          width: 6px;
        }
        &::-webkit-scrollbar-thumb {
          background-color: #dcdfe6;
          border-radius: 3px;
        }
        &::-webkit-scrollbar-track {
          background-color: transparent;
        }
      }
    }

    .dispatch-form.el-form {
      ::v-deep .el-input__wrapper {
        box-shadow: none !important;

        &:hover,
        &.is-focus {
          box-shadow: none !important;
          border-color: var(--border-color) !important;
        }
      }

      ::v-deep .el-textarea__inner {
        background-color: var(--search-bg) !important;
        border: 1px solid var(--border-color) !important;
        border-radius: 4px !important;
        box-shadow: none !important;
        outline: none !important;
        padding: 10px 12px;
        color: var(--text-color) !important;
        resize: vertical !important;
        width: 100% !important;
        box-sizing: border-box !important;
        word-wrap: break-word !important;
        word-break: break-all !important;
        white-space: pre-wrap !important;
        overflow-x: hidden !important;

        &::placeholder {
          color: #c0c4cc !important;
        }
      }

      ::v-deep .el-input__inner {
        background-color: transparent !important;
        border: none !important;
        height: 34px;
        line-height: 34px;
        padding: 0;

        &:hover,
        &:focus {
          border: none !important;
        }

        &:not(:disabled) {
          color: var(--text-color) !important;
        }

        &::placeholder {
          color: #c0c4cc !important;
          -webkit-text-fill-color: #c0c4cc !important;
        }
      }

      ::v-deep .el-input__inner::placeholder {
        color: #c0c4cc !important;
        -webkit-text-fill-color: #c0c4cc !important;
      }

      ::v-deep .el-input.is-disabled .el-input__wrapper {
        background-color: var(--button-text-inner) !important;
        border-color: var(--border-color) !important;
      }

      ::v-deep .el-input.is-disabled .el-input__inner {
        color: var(--text-color) !important;
        -webkit-text-fill-color: var(--text-color) !important;
        cursor: not-allowed;
      }

      ::v-deep .el-select__wrapper {
        background-color: var(--search-bg) !important;
        border: 1px solid var(--border-color) !important;
        border-radius: 4px !important;
        box-shadow: none !important;
        outline: none !important;
        padding: 1px 12px;
        height: 36px;
        min-height: 36px;
      }

      ::v-deep .el-select .el-select__selected-item {
        padding: 0 !important;
        color: var(--text-color) !important;
        -webkit-text-fill-color: var(--text-color) !important;
      }

      ::v-deep .el-select__placeholder {
        color: var(--text-color) !important;
        padding: 0 !important;
        -webkit-text-fill-color: var(--text-color) !important;
      }

      ::v-deep .el-select .el-select__placeholder.is-transparent {
        color: #c0c4cc !important;
        -webkit-text-fill-color: #c0c4cc !important;
      }

      ::v-deep .el-date-editor {
        background-color: transparent !important;
        height: 36px !important;
        line-height: 36px !important;
      }

      ::v-deep .el-date-editor .el-input__wrapper {
        background-color: var(--search-bg) !important;
        box-shadow: none !important;
        padding: 0 12px;
        height: 36px !important;
        min-height: 36px !important;
      }

      ::v-deep .el-date-editor .el-input__inner {
        background-color: transparent !important;
        border: none !important;
        color: var(--text-color) !important;
        height: 34px;
        line-height: 34px;

        &::placeholder {
          color: #c0c4cc !important;
          -webkit-text-fill-color: #c0c4cc !important;
        }
      }

      ::v-deep .el-date-editor .el-input__inner::placeholder {
        color: #c0c4cc !important;
        -webkit-text-fill-color: #c0c4cc !important;
      }

      .el-popper.is-light ::v-deep  .el-button  span{
        color: #fff !important;
      }

      ::v-deep .el-switch {
        --el-switch-on-color: #264ed1;
        --el-switch-off-color: #c0c4cc;
      }

      ::v-deep .el-switch.is-checked .el-switch__core {
        background-color: #264ed1 !important;
        border-color: #264ed1 !important;
      }

      ::v-deep .el-switch:not(.is-checked) .el-switch__core {
        background-color: #c0c4cc !important;
        border-color: #c0c4cc !important;
      }
    }
  }

  // 事务详情 el-descriptions 样式（右侧单列展示）
  ::v-deep .el-descriptions__label {
    font-weight: normal !important;
    width: 120px !important;
    min-width: 120px !important;
    max-width: 120px !important;
  }

  ::v-deep .el-descriptions__content {
    width: calc(100% - 120px) !important;
    min-width: 0 !important;
  }

  ::v-deep .el-descriptions__table {
    table-layout: fixed !important;
  }
}

.el-select {
  width: 100%;
}
</style>

<style lang="less">
// 执行人下拉 popper（teleport 到 body，需全局样式，仅命中本弹窗执行人下拉，不影响其他 el-select）：
// 配合 fit-input-width 锁定宽度后，保证超长文案省略号截断、不撑开/换行；
// 固定列表最小高度，loading/无结果与有结果时高度一致，避免弹层跳动
.executor-select-popper {
  .el-select-dropdown__item {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  .el-select-dropdown__list {
    min-height: 262px; // 对齐 Element Plus 下拉默认 max-height(274px) 减去上下 padding(6px*2)
  }
}
</style>