<template>
  <div class="detail-dialog glass-light">
    <div class="dialog-header">
      <div>
        <span class="blue-block"></span>
        <span class="dialog-title">详情</span>
      </div>
      <span class="dialog-close" @click="handleClose">✕</span>
    </div>
    
    <div class="detail-content">
      <div class="descriptions-wrapper">
        <el-descriptions :column="2" border label-width="120px" layout="fixed">
          <el-descriptions-item 
            v-for="col in columns" 
            :key="col.prop"
            :label="col.label"
          >
            {{ formatDisplayText(row[col.prop]) }}
          </el-descriptions-item>
        </el-descriptions>
      </div>
    </div>
  </div>
</template>

<script setup>
import { defineProps } from 'vue'
import { Dialog } from '@/components/Dialog/src/helper'
import { formatDisplayText } from '@/utils/formatter'

const props = defineProps({
  row: {
    type: Object,
    default: () => ({})
  },
  columns: {
    type: Array,
    default: () => []
  }
})

const handleClose = () => {
  Dialog('DetailDialog')?.close()
}
</script>

<style scoped lang="less">
.detail-dialog {
  display: flex;
  flex-direction: column;
  width: 800px;
  height: 600px;
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

  .detail-content {
    flex: 1;
    overflow-y: auto;
    padding: 20px 24px;

    .descriptions-wrapper {
      max-height: 100%;
      overflow-y: auto;
      overflow-x: hidden;
      padding-right: 10px;
      
      &::-webkit-scrollbar {
        width: 6px;
      }
      &::-webkit-scrollbar-thumb {
        background-color: #dcdfe6;
        border-radius: 3px;
      }
      &::-webkit-scrollbar-track {
        background-color: #f5f7fa;
      }
    }

    .el-descriptions {
      margin-bottom: 20px;
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

  ::v-deep .el-descriptions__table {
    table-layout: fixed !important;
  }
}
</style>