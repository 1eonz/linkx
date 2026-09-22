<template>
  <div class="image-preview-dialog glass-light">
    <div class="dialog-header">
      <div>
        <span class="blue-block"></span>
        <span class="dialog-title">图片预览</span>
      </div>
      <div class="header-actions">
        <div class="preview-download" @click="handleDownload" title="下载">
          <el-icon><Download /></el-icon>
          <span>下载</span>
        </div>
        <span class="dialog-close" @click="handleClose">✕</span>
      </div>
    </div>

    <div class="preview-content">
      <div class="preview-container">
        <el-icon class="preview-arrow prev" v-if="imageList.length > 1" @click="previewPrev">
          <ArrowLeft />
        </el-icon>
        <img :src="imageList[previewIndex]?.url" class="preview-img" />
        <el-icon class="preview-arrow next" v-if="imageList.length > 1" @click="previewNext">
          <ArrowRight />
        </el-icon>
      </div>
      <div class="preview-index" v-if="imageList.length > 1">
        {{ previewIndex + 1 }} / {{ imageList.length }}
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowLeft, ArrowRight, Download } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { Dialog } from '@/components/Dialog/src/helper'
import { downloadFile } from '@/bridge/post'

const props = defineProps({
  imageList: {
    type: Array,
    default: () => []
  },
  initialIndex: {
    type: Number,
    default: 0
  }
})

const previewIndex = ref(props.initialIndex)

const previewPrev = () => {
  previewIndex.value = (previewIndex.value - 1 + props.imageList.length) % props.imageList.length
}
const previewNext = () => {
  previewIndex.value = (previewIndex.value + 1) % props.imageList.length
}

const handleClose = () => {
  Dialog('ImagePreviewDialog')?.close()
}

const handleDownload = async () => {
  const file = props.imageList[previewIndex.value]
  if (!file?.url) {
    ElMessage.warning('文件路径不存在')
    return
  }
  try {
    await downloadFile({
      url: file.url,
      fileName: file.fileName || file.name || '',
    })
  } catch (error) {
    console.error('下载失败:', error)
  }
}
</script>

<style scoped lang="less">
.image-preview-dialog {
  display: flex;
  flex-direction: column;
  width: 760px;
  height: 610px;
  border-radius: 2px;
  overflow: hidden;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  background: var(--background-color-white, #fff);

  .dialog-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
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

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .preview-download {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 12px;
      background: var(--background-color-page, #f5f7fa);
      color: var(--text-color, #1d2129);
      border-radius: 4px;
      font-size: 12px;
      cursor: pointer;
      transition: background 0.15s;

      .el-icon {
        font-size: 14px;
      }

      &:hover {
        background: var(--hover-color, #ecf5ff);
        color: var(--primary-color, #264ed1);
      }
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

  .preview-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    padding: 12px 16px;
    overflow: hidden;

    .preview-container {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: center;
      flex: 1;
      width: 100%;

      .preview-img {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
      }

      .preview-arrow {
        position: absolute;
        top: 50%;
        transform: translateY(-50%);
        font-size: 28px;
        color: #fff;
        background: rgba(0, 0, 0, 0.4);
        border-radius: 50%;
        padding: 8px;
        cursor: pointer;
        z-index: 1;

        &:hover {
          background: rgba(0, 0, 0, 0.6);
        }

        &.prev {
          left: 10px;
        }

        &.next {
          right: 10px;
        }
      }

    }

    .preview-index {
      text-align: center;
      color: #909399;
      font-size: 13px;
      margin-top: 10px;
      flex-shrink: 0;
    }
  }
}
</style>
