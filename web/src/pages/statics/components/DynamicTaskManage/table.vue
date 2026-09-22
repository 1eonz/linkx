<template>
  <div class="table-container table-page-content" v-loading="loading">
    <!-- 搜索 -->
    <div class="search-form">
      <el-select v-model="searchKey" placeholder="请选择搜索字段" clearable filterable @clear="handleSearchKeyClear" style="width: 240px">
        <el-option v-for="col in columns" :key="col.prop" :label="col.label" :value="col.prop" />
      </el-select>
      <el-input v-model="searchValue" placeholder="请输入搜索值" clearable @clear="handleSearch" @keyup.enter="handleSearch" style="width: 240px" />
      <el-button class="search-btn" :icon="RefreshLeft" @click="handleReset">重置</el-button>
      <el-button class="search-btn" :icon="Search" @click="handleSearch">搜索</el-button>
    </div>
    <!-- 表格 -->
    <el-table :data="tableData" v-loading="loading" style="width: 100%" border>
      <el-table-column type="index" label="序号" width="60" />
      <!-- 动态列 -->
      <template v-if="columns.length > 0">
        <el-table-column v-for="col in columns" :key="col.prop" :prop="col.prop" :label="col.label" :min-width="120" show-overflow-tooltip />
      </template>
      <el-table-column v-else label="" min-width="200" />
      <el-table-column label="状态" width="120" align="center">
      <template #default="scope">
          <el-tag :type="scope.row.linkxStatusInfo?.taskStatus ? 'warning' : 'info'">{{ scope.row.linkxStatusInfo?.taskStatus || '待派发' }}</el-tag>
        </template>
      </el-table-column>
      <!-- 操作列 -->
      <el-table-column fixed="right" label="操作" width="150" align="center">
        <template #default="scope">
          <span :class="btn-item" class="btn-detail" link @click="handleDetail(scope.row)">
            详情
          </span>
          <!-- enableTask=0：仅显示详情，不显示派发和处置进展 -->
          <!-- enableTask=1 且未派发：显示派发；已派发：显示处置进展 -->
          <template v-if="enableTask">
            <span :class="btn-item" v-if="!scope.row.linkxStatusInfo || !scope.row.linkxStatusInfo.taskStatus" class="btn-dispatch" link @click="handleDispatch(scope.row)">
              派发
            </span>
            <span :class="btn-item" v-else class="btn-detail" link @click="handleProgress(scope.row)">
              处置进展
            </span>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination" v-if="pagination.total > 0">
      <el-pagination background layout="sizes, prev, pager, next, jumper" :total="pagination.total"
        :current-page="pagination.current" :page-size="pagination.size" :page-sizes="[10, 20, 50, 100]"
        @current-change="handlePageChange" @size-change="handleSizeChange" />
    </div>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, RefreshLeft } from '@element-plus/icons-vue'
import { getCallebleTables, getCallebleTablesData, convertToTask, getColumns, getTaskStatus, getCallableAppTaskConfig } from '@/api/taskManage'
import { Dialog } from '@/components/Dialog/src/helper'
import DetailDialog from './DetailDialog.vue'
import DispatchDialog from './DispatchDialog.vue'
import ProgressDialog from './ProgressDialog.vue'
import { usePIMStore } from '@/store/modules/pim';
const pimStore = usePIMStore()

const props = defineProps({
  callableId: {
    type: String,
    default: ''
  },
  columnsConfig: {
    type: Array,
    default: () => []
  },
  taskTab: {
    type: Object,
    default: () => ({})
  }
})
// 用户信息
const userInfo = computed(() => pimStore?.user || {});

// 状态
const tableData = ref([])
const loading = ref(false)
const columns = ref([])          // 列配置（由外部传入）
const currentTableName = ref('')
const pagination = ref({
  current: 1,
  size: 10,
  total: 0
})
const searchKey = ref('')
const searchValue = ref('')
// 任务派发配置（切换 tab 时从接口获取）
const enableTask = ref(0)
const taskAutoFillConfig = ref('')

// 对话框状态
const currentRow = ref({})

// 任务状态映射
const taskStatusMap = {
  pending: { text: '待签收', type: 'warning' },
  processing: { text: '进行中', type: 'primary' },
  completed: { text: '已完成', type: 'success' },
  rejected: { text: '已拒绝', type: 'danger' }
}

// 获取状态标签类型
const getStatusTagType = (status) => {
  return taskStatusMap[status]?.type || 'info'
}

// 获取状态文本
const getStatusText = (status) => {
  return taskStatusMap[status]?.text || '未派发'
}

// 获取数据表列表（取第一个表名）
const fetchTableName = async () => {
  if (!props.callableId) return null

  try {
    const res = await getCallebleTables(props.callableId)
    if (res.code === 0 && res.data && res.data.length > 0) {
      currentTableName.value = res.data?.[0] || ''
      return currentTableName.value
    }
  } catch (error) {
    console.error('fetchTableName获取数据表列表失败:', error)
  }
  return null
}

// 获取表格数据
const fetchTableData = async () => {
  if (!props.callableId || !currentTableName.value) return;
  loading.value = true
  try {
    const params = {
      table: currentTableName.value,
      page: pagination.value.current,
      pageSize: pagination.value.size
    }
    if (searchKey.value) {
      params.searchKey = searchKey.value
      params.searchValue = searchValue.value
    }

    const res = await getCallebleTablesData(props.callableId, { params })

    if (res.code === 0) {
      const records = res.data?.records || res.data?.data || res.data || []
      tableData.value = records
      pagination.value.total = Number(res.data?.total || records.length || 0)
      console.log(records, 'records')
    } else {
      ElMessage.error(res.msg || '获取表格数据失败')
      tableData.value = []
      pagination.value.total = 0
    }
  } catch (error) {
    console.error('获取表格数据失败:', error)
    ElMessage.error('获取数据失败')
    tableData.value = []
    pagination.value.total = 0
  } finally {
    loading.value = false
  }
}

const fetchColumns = async () => {
  if (!props.callableId || !currentTableName.value) return;
  try {
    const res = await getColumns(props.callableId, { tableName: currentTableName.value })
    if (res.code === 0) {
      const columnsData = res.data || []
      columns.value = columnsData.filter(item => {
        return item.isMapperColumn === 1 && item.columnType !== 'json'
      }).map(item => ({
        prop: item.columnName,
        label: item.mapperColumnValue,
      }))
      console.log(columns.value, 'columns.value')
    } else {
      ElMessage.error(res.msg || '获取表格列配置失败')
    }
  } catch (error) {
    console.error('获取表格列配置失败:', error)
    ElMessage.error('获取数据失败')
  }
}

// 获取任务状态
const fetchTaskStatus = async () => {
  if (!props.callableId || !currentTableName.value) return;
  try {

    const res = await getTaskStatus(props.callableId, { tableName: currentTableName.value, tableDataId: currentRow.value?.id || '' })
    if (res.code === 0) {

    } else {
      ElMessage.error(res.msg || '获取任务状态失败')
    }
  } catch (error) {
    console.error('获取任务状态失败:', error)
    ElMessage.error('获取数据失败')
  }
}

// 获取任务派发配置（enableTask 控制派发按钮显示，taskAutoFillConfig 回填表单默认值）
const fetchTaskConfig = async () => {
  if (!props.callableId) return
  try {
    const res = await getCallableAppTaskConfig(props.callableId)
    if (res.code === 0) {
      enableTask.value = res.data?.enableTask || 0
      taskAutoFillConfig.value = res.data?.taskAutoFillConfig || ''
    } else {
      enableTask.value = 0
      taskAutoFillConfig.value = ''
    }
  } catch (error) {
    console.error('获取任务派发配置失败:', error)
    enableTask.value = 0
    taskAutoFillConfig.value = ''
  }
}

// 初始化
const init = async () => {
  if (!props.callableId) return

  pagination.value.current = 1

  await fetchTableName()
  await fetchColumns()
  await Promise.all([fetchTableData(), fetchTaskConfig()])
}

// 搜索
const handleSearch = async () => {
  pagination.value.current = 1
  await fetchTableData()
}

// 清除搜索字段时联动清除搜索值
const handleSearchKeyClear = async () => {
  searchValue.value = ''
  pagination.value.current = 1
  await fetchTableData()
}

// 重置
const handleReset = async () => {
  searchKey.value = ''
  searchValue.value = ''
  pagination.value.current = 1
  await fetchTableData()
}

// 分页变化
const handlePageChange = async (page) => {
  pagination.value.current = page
  await fetchTableData()
}

const handleSizeChange = async (size) => {
  pagination.value.size = size
  pagination.value.current = 1
  await fetchTableData()
}

// 详情
const handleDetail = (row) => {
  currentRow.value = row
  Dialog({
    cid: 'DetailDialog',
    content: DetailDialog,
    shade: true,
    data: {
      row: currentRow.value,
      columns: columns.value
    }
  })
}

// 派发
const handleDispatch = (row) => {
  currentRow.value = row
  Dialog({
    cid: 'DispatchDialog',
    content: DispatchDialog,
    shade: true,
    data: {
      row: currentRow.value,
      columns: columns.value,
      callableId: props.callableId,
      tableName: currentTableName.value,
      taskTab: props.taskTab,
      taskAutoFillConfig: taskAutoFillConfig.value,
      onSuccess: handleDispatchSuccess
    }
  })
}

// 处置进展
const handleProgress = (row) => {
  currentRow.value = row
  Dialog({
    cid: 'ProgressDialog',
    content: ProgressDialog,
    shade: true,
    data: {
      tableName: currentTableName.value,
      row: currentRow.value,
      callableId: props.callableId,
      mapper: props.columnsConfig
    }
  })
}

// 派发成功处理
const handleDispatchSuccess = () => {
  ElMessage.success('任务派发成功')
  // 更新当前行的任务状态
  if (currentRow.value) {
    currentRow.value.taskStatus = 'dispatching'
  }
  // 刷新表格数据
  fetchTableData()
}

// 转换为任务（保留原有功能，供外部调用）
const handleConvert = async (row) => {
  try {
    const res = await convertToTask(props.callableId, row)
    if (res.code === 0) {
      ElMessage.success('转换任务成功')
    } else {
      ElMessage.error('转换任务失败')
    }
  } catch (error) {
    console.error('转换任务失败:', error)
    ElMessage.error('转换任务失败')
  }
}

// 刷新
const refresh = () => {
  init()
}

// 监听 callableId 变化
watch(
  () => props.callableId,
  (newId) => {
    if (newId) {
      init()
    }
  },
  { immediate: true }
)

// 暴露方法
defineExpose({
  refresh
})
</script>

<style scoped lang="less">
@import '@/styles/tablePageStyle.less';

.table-container {
  // 操作按钮颜色
  :deep(.btn-detail) {
    color: var(--tabs-active-color, #3B72FF) !important;
    .el-button__text {
      color: var(--tabs-active-color, #3B72FF) !important;
    }
  }

  :deep(.btn-dispatch) {
    color: var(--tabs-active-color, #3B72FF) !important;
    .el-button__text {
      color: var(--tabs-active-color, #3B72FF) !important;
    }
  }

  :deep(.btn-dispatching) {
    color: #EB5255 !important;
    .el-button__text {
      color: #EB5255 !important;
    }
  }

  :deep(.btn-completed) {
    color: #09AC42 !important;
    .el-button__text {
      color: #09AC42 !important;
    }
  }
}
</style>