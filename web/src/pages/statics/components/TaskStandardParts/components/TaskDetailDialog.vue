<!--
  任务详情弹窗组件（系统内部生成任务 type=1）
  - UI 风格布局参考 DynamicTaskManage/ProgressDialog.vue（el-descriptions column=2 + border + label-width=120px + layout=fixed）
  - 字段：任务名称、任务内容、所属系统、创建人、创建部门、开始时间、结束时间、任务状态、附件
  - 详情数据从列表行 row 直接透传（:data），无需 API 调用
  - 备注：UI 展示出来，内容暂为空（显示 '--'），后续从其他接口获取后填充 remark
-->
<template>
  <el-dialog
    :model-value="visible"
    width="800px"
    append-to-body
    destroy-on-close
    :show-close="false"
    class="task-detail-dialog"
    @update:model-value="(v) => emit('update:visible', v)"
  >
    <template #header>
      <div class="dialog-header">
        <div></div>
        <span class="dialog-close" @click="emit('update:visible', false)">✕</span>
      </div>
    </template>
    <div class="task-detail">
      <div class="task-base-detail">
        <div class="info-header">
          <div>
            <span class="blue-block"></span>
            <span class="info-title">详情</span>
            <span v-if="data.number" class="info-subtitle">({{ data.number }})</span>
          </div>
        </div>
        <el-descriptions :column="2" border label-width="120px" layout="fixed">
          <el-descriptions-item label="任务名称" :span="2">{{ data.name || '--' }}</el-descriptions-item>
          <el-descriptions-item label="任务内容" :span="2">{{ data.content || '--' }}</el-descriptions-item>
          <el-descriptions-item label="所属系统">{{ data.system || '--' }}</el-descriptions-item>
          <el-descriptions-item label="创建人">{{ data.creator?.name || '--' }}</el-descriptions-item>
          <el-descriptions-item label="创建人部门">{{ data.creator?.department || '--' }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ data.startTime || '--' }}</el-descriptions-item>
          <el-descriptions-item label="结束时间">{{ data.endTime || '--' }}</el-descriptions-item>
          <el-descriptions-item label="任务状态">
            <StatusTag :status="data.status" />
          </el-descriptions-item>
          <el-descriptions-item label="附件" :span="2">
            <div class="attach-list">
              <el-empty
                v-if="!data.attachments || data.attachments.length === 0"
                :image-size="30"
                description="暂无附件"
              />
              <div
                v-for="(file, index) in data.attachments"
                :key="file.id || index"
                class="file-card"
                :title="file.fileName || ''"
                @click="handleDownload(file)"
              >
                <div class="file-card-icon">
                  <FileTypeIcon :type="getFileTypeKey(file)" :size="32" />
                </div>
                <div class="file-card-info">
                  <div class="file-card-name">{{ file.fileName || '--' }}</div>
                  <div class="file-card-meta">
                    <span v-if="file.fileSize" class="meta-size">{{ formatFileSize(file.fileSize) }}</span>
                    <span v-if="file.gmtCreated" class="meta-time">{{ file.gmtCreated }}上传</span>
                  </div>
                </div>
              </div>
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </div>

      <!-- 任务处置记录（侧边栏） -->
      <div class="task-side-detail">
        <ProcessRecord :task-number="data.number" />
      </div>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import FileTypeIcon from '@/components/FileTypeIcon';
import ProcessRecord from './ProcessRecord.vue';
import StatusTag from './StatusTag.vue';
import { formatFileSize, getFileTypeKey, handleDownload } from '@/utils/fileType';
import type { TasksVO } from '../types';

defineProps<{
  visible: boolean;
  data: TasksVO;
}>();

const emit = defineEmits<{
  (e: 'update:visible', v: boolean): void;
}>();
</script>

<style scoped lang="less">
// 弹窗标题样式对齐 ProgressDialog.vue
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;

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

.task-detail {
  display: flex;
  min-height: 400px;
  max-height: 65vh;
  border-top: 1px solid var(--border-color, #ebeef5);
  .task-base-detail {
    flex: 1;
    min-width: 0;
    overflow-y: auto;
  }

  // 基本信息标题样式（参照 ProcessRecord 的 record-header）
  .info-header {
    display: flex;
    align-items: center;
    padding: 16px 0;
    flex-shrink: 0;

    & > div {
      display: flex;
      align-items: center;
    }

    .blue-block {
      display: inline-block;
      width: 3px;
      height: 14px;
      margin-right: 4px;
      background: #264ed1;
      flex-shrink: 0;
    }

    .info-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-color, #1d2129);
      line-height: 1;
    }

    .info-subtitle {
      margin-left: 4px;
      font-size: 14px;
      font-weight: normal;
      line-height: 1;
      color: var(--text-color, #1d2129);
    }
  }

  .task-side-detail {
    width: 280px;
    flex-shrink: 0;
    border-left: 1px solid var(--border-color, #ebeef5);
    padding-left: 16px;
    overflow-y: auto;
    background: var(--bg-color, #fafafa);
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
      gap: 10px;
      width: 100%;
      box-sizing: border-box;
      padding: 10px 12px;
      border-radius: 4px;
      background: var(--bg-color, #fafafa);
      cursor: pointer;
      transition: all 0.15s;

      &:hover {
        background: var(--hover-color, #f0f4ff);
      }

      .file-card-icon {
        position: relative;
        flex-shrink: 0;
        width: 32px;
        height: 32px;
        display: flex;
        align-items: center;
        justify-content: center;

        :deep(.file-type-icon) {
          display: block;
          width: 32px !important;
          height: 32px !important;
        }
      }

      .file-card-info {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;
        gap: 2px;

        .file-card-name {
          font-size: 13px;
          line-height: 1.4;
          color: var(--text-color, #1d2129);
          overflow: hidden;
          white-space: nowrap;
          text-overflow: ellipsis;
        }

        .file-card-meta {
          display: flex;
          align-items: center;
          gap: 8px;
          font-size: 11px;
          line-height: 1.4;
          color: var(--text-color-secondary, #86909c);

          .meta-size {
            flex-shrink: 0;
          }

          .meta-time {
            flex-shrink: 0;
            overflow: hidden;
            white-space: nowrap;
            text-overflow: ellipsis;
          }
        }
      }
    }

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

// 描述列宽度对齐 ProgressDialog.vue
:deep(.el-descriptions__label) {
  font-weight: normal !important;
  width: 120px !important;
  min-width: 120px !important;
  max-width: 120px !important;
}

:deep(.el-descriptions__table) {
  table-layout: fixed !important;
}

:deep(.el-descriptions__cell) {
  vertical-align: middle !important;
}

:deep(.el-descriptions__table) {
  table-layout: fixed !important;
}
</style>
