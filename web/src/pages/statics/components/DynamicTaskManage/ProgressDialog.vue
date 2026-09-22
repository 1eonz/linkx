<template>
  <div class="progress-dialog glass-light" v-loading="loading">
    <div class="dialog-header">
      <div>
        <span class="blue-block"></span>
        <span class="dialog-title">处置进展</span>
      </div>
      <span class="dialog-close" @click="handleClose">✕</span>
    </div>
    
    <div class="progress-content">
      <div class="task-info">
        <h3>任务基本信息</h3>
        <el-descriptions :column="2" border label-width="120px" layout="fixed">
          <el-descriptions-item label="任务编号">{{ taskInfo.number || '--' }}</el-descriptions-item>
          <el-descriptions-item label="任务名称">{{ taskInfo.name || '--' }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">
            <el-tag type="info">
              {{ taskInfo.status }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="任务等级">{{ taskInfo.level }}</el-descriptions-item>
          <el-descriptions-item label="所属系统">{{ taskInfo.system || '--' }}</el-descriptions-item>
          <el-descriptions-item label="业务类型">{{ taskInfo.businessType || '--' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ taskInfo.creatorName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="创建人部门">{{ taskInfo.creatorDepartment || '--' }}</el-descriptions-item>
          <el-descriptions-item label="执行人">{{ taskInfo.executorName || '--' }}</el-descriptions-item>
          <el-descriptions-item label="执行人部门">{{ taskInfo.executorDepartment || '--' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ taskInfo.startTime || '--' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ taskInfo.endTime || '--' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ taskInfo.completeTime || '--' }}</el-descriptions-item>
          <el-descriptions-item label="是否紧急">{{ taskInfo.urgent === 1 ? '是' : '否' }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ taskInfo.remark || '--' }}</el-descriptions-item>
          <el-descriptions-item label="任务附件">
            <div class="attach-list" v-loading="fileLoading">
              <el-empty v-if="attachList.length === 0" :image-size="30" description="暂无附件">
                <template #image>
                  <el-icon class="empty-icon"><Document /></el-icon>
                </template>
              </el-empty>
              <div
                v-else
                v-for="(file, index) in attachList"
                :key="file.id || index"
                class="file-card"
                :title="file.fileName || file.name"
                @click="handleFileClick(file)"
              >
                <div class="file-card-icon">
                  <img v-if="isImage(file)" :src="file.url" class="file-card-img" />
                  <FileTypeIcon v-else :type="getFileTypeKey(file)" :size="18" />
                </div>
                <span class="file-card-name">{{ file.fileName || file.name }}</span>
              </div>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      
      <div class="extend-info" v-if="props.mapper && props.mapper.length > 0">
        <h3>扩展信息</h3>
        <el-descriptions :column="2" border label-width="120px" layout="fixed">
          <el-descriptions-item 
            v-for="item in props.mapper" 
            :key="item.key" 
            :label="item.label"
          >
            {{ formatExtendValue(taskInfo.extendData, item.key) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, defineProps } from 'vue'
import { ElMessage } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import { getTaskStatus, getProcessList, getFileList } from '@/api/taskManage'
import { Dialog } from '@/components/Dialog/src/helper'
import ImagePreviewDialog from './ImagePreviewDialog.vue'
import FileTypeIcon from '@/components/FileTypeIcon'
import { getFileTypeKey, isImage, resolveFileUrl, handleDownload } from '@/utils/fileType'

const props = defineProps({
  tableName:{
    type: String,
    default: '',
  },
  row: {
    type: Object,
    default: () => ({})
  },
  callableId: {
    type: String,
    default: ''
  },
  mapper: {
    type: Array,
    default: () => []
  }
})

const loading = ref(false)
const processList = ref([])
const fileLoading = ref(false)
const attachList = ref([])

// 图片预览（使用项目自定义 Dialog，与处置进展弹窗一致）
const handlePreview = (file) => {
  Dialog({
    cid: 'ImagePreviewDialog',
    content: ImagePreviewDialog,
    shade: true,
    data: {
      imageList: [file],
      initialIndex: 0,
    },
  })
}

// 文件点击：图片预览，非图片下载（按后端返回顺序统一展示）
const handleFileClick = (file) => {
  if (isImage(file)) {
    handlePreview(file)
  } else {
    handleDownload(file)
  }
}

const taskStatusMap = {
  pending: { text: '待签收', type: 'warning' },
  processing: { text: '进行中', type: 'primary' },
  completed: { text: '已完成', type: 'success' },
  rejected: { text: '已拒绝', type: 'danger' }
}

const taskInfo = reactive({
  id: '',
  number: '',
  name: '',
  content: '',
  system: '',
  businessType: '',
  status: '',
  level: '',
  urgent: 0,
  creatorName: '',
  creatorDepartment: '',
  executorName: '',
  executorIdCard: '',
  executorDepartment: '',
  startTime: '',
  endTime: '',
  completeTime: '',
  remark: '', // 备注：取 processList[0].remark，与 H5 taskDeal.vue 一致
  extendData: null
})

const parseExtendData = (extendStr) => {
  if (!extendStr) return null
  try {
    return JSON.parse(extendStr)
  } catch (e) {
    return null
  }
}

const formatExtendValue = (extendData, key) => {
  if (!extendData || !key) return '--'
  const value = extendData[key]
  if (value === undefined || value === null) return '--'
  if (typeof value === 'object') return JSON.stringify(value)
  return value
}

// 获取操作记录（参考 H5Portal fetchProcessList 实现）
const fetchProcessList = async (taskNumber) => {
  if (!taskNumber || taskNumber === '--') {
    processList.value = []
    return
  }
  try {
    const res = await getProcessList(taskNumber)
    const data = res?.code === 0 ? res.data : res
    processList.value = Array.isArray(data) ? data : (data?.list || [])
    // 回显最新一条备注到 taskInfo.remark，与 H5 taskDeal.vue 一致
    const latestRemark = processList.value[0]?.remark
    if (latestRemark) {
      taskInfo.remark = latestRemark
    }
  } catch (error) {
    console.error('获取操作记录失败:', error)
    processList.value = []
  }
}

// 获取任务文件列表（图片和附件统一展示，按后端返回顺序）
const fetchFileList = async (taskNumber) => {
  if (!taskNumber || taskNumber === '--') {
    attachList.value = []
    return
  }
  fileLoading.value = true
  try {
    const res = await getFileList(taskNumber)
    const data = res?.code === 0 ? res.data : res
    const list = Array.isArray(data) ? data : (data?.list || [])
    attachList.value = list.map(f => ({ ...f, url: resolveFileUrl(f) }))
  } catch (error) {
    console.error('获取文件列表失败:', error)
    attachList.value = []
  } finally {
    fileLoading.value = false
  }
}

const initData = async () => {
  if (!props.callableId) return

  loading.value = true
  try {
    console.log(props.row)
    const res = await getTaskStatus(props.callableId, {
      tableName: props.tableName || '',
      tableDataId: props.row.linkx_id || props.row.id
    })

    if (res.code === 0 && res.data) {
      const data = res.data
      taskInfo.id = data.id || ''
      taskInfo.number = data.number || '--'
      taskInfo.name = data.name || '--'
      taskInfo.content = data.content || '--'
      taskInfo.system = data.system || '--'
      taskInfo.businessType = data.businessType || '--'
      taskInfo.status = data.status || 'pending'
      taskInfo.level = data.level || ''
      taskInfo.urgent = data.urgent || 0
      taskInfo.creatorName = data.creator?.name || '--'
      taskInfo.creatorDepartment = data.creator?.department || '--'
      taskInfo.executorName = data.executors?.[0]?.name || '--'
      taskInfo.executorIdCard = data.executors?.[0]?.idCard || '--'
      taskInfo.executorDepartment = data.executors?.[0]?.department || '--'
      taskInfo.startTime = data.startTime || '--'
      taskInfo.endTime = data.endTime || '--'
      taskInfo.completeTime = data.completeTime || '--'
      taskInfo.extendData = parseExtendData(data.extend)

      // 并行拉取操作记录和文件列表
      await Promise.allSettled([
        fetchProcessList(taskInfo.number),
        fetchFileList(taskInfo.number),
      ])
    } else {
      ElMessage.error('获取任务状态失败')
    }
  } catch (error) {
    console.error('获取任务信息失败:', error)
    ElMessage.error('获取任务信息失败')
  } finally {
    loading.value = false
  }
}

const handleClose = () => {
  Dialog('ProgressDialog')?.close()
}

onMounted(() => {
  initData()
})
</script>

<style scoped lang="less">
.progress-dialog {
  display: flex;
  flex-direction: column;
  width: 800px;
  height: 650px;
  border-radius: 2px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  background: var(--background-color-white, #fff);

  .dialog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    flex-shrink: 0;

    .blue-block {
      display: inline-block;
      width: 3px;
      height: 14px;
      margin-right: 4px;
      background: #264ed1;
      vertical-align: middle;
    }

    .dialog-title {
      font-size: 16px;
      font-weight: 600;
      color: var(--text-color, #1d2129);
    }

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

  .progress-content {
    flex: 1;
    overflow-y: auto;
    padding: 20px 24px;

    .task-info {
      margin-bottom: 30px;

      h3 {
        margin-bottom: 15px;
        color: var(--text-color, #303133);
      }

      .attach-list {
        display: flex;
        flex-wrap: wrap;
        align-items: stretch;
        gap: 8px;
        min-height: 20px;
        padding: 2px 0;

        .file-card {
          display: flex;
          align-items: center;
          gap: 8px;
          width: 100%;
          box-sizing: border-box;
          padding: 2px 0;
          cursor: pointer;
          transition: background 0.15s;

          .file-card-icon {
            position: relative;
            flex-shrink: 0;
            width: 18px;
            height: 18px;

            .file-card-img {
              display: block;
              width: 18px;
              height: 18px;
              border-radius: 2px;
              object-fit: cover;
              border: 1px solid var(--border-color, #ebeef5);
            }

            :deep(.file-type-icon) {
              display: block;
              width: 100% !important;
              height: 100% !important;
            }
          }

          .file-card-name {
            flex: 1;
            min-width: 0;
            font-size: 12px;
            line-height: 1.3;
            color: var(--text-color, #1d2129);
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
          }
        }

        // 空状态居中
        :deep(.el-empty) {
          padding: 0;
          flex: 1;
          justify-content: center;
          align-items: center;
          width: 100%;
          min-height: 40px;

          .el-empty__image {
            width: 22px;
            margin-bottom: 2px;

            .empty-icon {
              font-size: 22px;
              color: var(--text-color-secondary, #909399);
            }
          }

          .el-empty__description {
            margin-top: 0;
            p {
              font-size: 11px;
            }
          }
        }
      }
    }

    .extend-info {
      margin-bottom: 30px;

      h3 {
        margin-bottom: 15px;
        color: var(--text-color, #303133);
      }
    }
  }

  ::v-deep .el-descriptions__label {
    font-weight: normal !important;
    width: 120px !important;
    min-width: 120px !important;
    max-width: 120px !important;
  }

  ::v-deep .el-descriptions__content {
    width: calc(50% - 120px) !important;
    min-width: calc(50% - 120px) !important;
  }

  // 单元格垂直居中对齐
  ::v-deep .el-descriptions__cell {
    vertical-align: middle !important;
  }

  ::v-deep .el-descriptions__table {
    table-layout: fixed !important;
  }
}
</style>
