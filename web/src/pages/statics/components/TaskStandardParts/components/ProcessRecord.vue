<!--
  处置记录组件（独立组件，供 TaskDetailDialog 侧边栏使用）
  - UI 风格布局参考 DynamicTaskManage/ProgressDialog.vue 的 processList 渲染
  - 入参：
    - taskNumber：任务编号（用于拉取处置记录）
  - 数据源：getProcessListPage(taskNumber, { pageNum, pageSize })
    - 接口：GET /collaboration/v1/tasks/{taskNumber}/processes
    - 返回：R<Page<TasksProcesses>>
  - 分页：首次加载第 1 页，滚动到底部自动加载下一页，直到没有更多
-->
<template>
  <div class="process-record" v-loading="loading">
    <div class="record-header">
      <span class="blue-block"></span>
      <span class="record-title">处置记录</span>
    </div>
    <div
      ref="scrollRef"
      class="record-content"
      @scroll="handleScroll"
    >
      <el-empty
        v-if="!loading && processList.length === 0"
        :image-size="40"
        description="暂无处置记录"
      />
      <el-collapse v-else v-model="activeNames" class="record-collapse">
        <el-collapse-item
          v-for="(item, index) in processList"
          :key="item.id || index"
          :name="String(item.id || index)"
        >
          <template #title>
            <div class="collapse-title">
              {{ item.operatorName || '--' }}-{{ item.gmtCreated || '--' }}
            </div>
          </template>
          <div class="record-item">
            <div class="record-field">
              <span class="field-label">处置动作：</span>
              <el-tag size="small" type="info" class="operate-type">
                {{ getActionText(item.action) }}
              </el-tag>
            </div>
            <div class="record-field">
              <span class="field-label">任务状态：</span>
              <StatusTag :status="item.status" />
            </div>
           
            <div class="record-next" >
              <div class="field-label">下一个处理人信息：</div>
              <ul class="next-list">
                <li
                  v-for="(executor, idx) in item?.nextExecutors ?? []"
                  :key="idx"
                  class="next-item"
                >
                  {{ executor.name || '--' }}—{{ executor.department }}
                </li>
              </ul>
            </div>
            <div  class="record-field">
              <span class="field-label">操作人姓名：</span>
              <span class="field-value">{{ item.operatorName || '--' }}</span>
            </div>
            <div  class="record-field">
              <span class="field-label">备注：</span>
              <span class="field-value">{{ item.remark }}</span>
            </div>
            <div  class="record-field">
              <span class="field-label">创建时间：</span>
              <span class="field-value">{{ item.gmtCreated || '--' }}</span>
            </div>
          </div>
        </el-collapse-item>
      </el-collapse>
      
      <div v-if="loadingMore" class="load-more-tip">加载中...</div>
      <div
        v-else-if="noMore && processList.length > 0"
        class="load-more-tip no-more"
      >
        没有更多了
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick } from 'vue';
import { getProcessListPage } from '@/api/taskManage';
import StatusTag from './StatusTag.vue';
import type { TasksProcesses } from '../types';

const props = defineProps<{
  taskNumber?: string;
}>();

const loading = ref(false);
const loadingMore = ref(false);
const processList = ref<TasksProcesses[]>([]);
const pageNum = ref(1);
const pageSize = 10;
const total = ref(0);
const noMore = ref(false);
// 默认展开第一条
const activeNames = ref<string[]>([]);
// 滚动容器 ref
const scrollRef = ref<HTMLElement | null>(null);

// 处置动作映射
const ACTION_MAP: Record<number, string> = {
  1: '认领',
  2: '转发',
  3: '回退',
  4: '处置',
  5: '完成',
};

const getActionText = (action?: number) => {
  if (!action) return '--';
  return ACTION_MAP[action] || '--';
};

const fetchPage = async (page: number) => {
  if (!props.taskNumber || props.taskNumber === '--') {
    processList.value = [];
    total.value = 0;
    noMore.value = true;
    return;
  }
  if (page === 1) {
    loading.value = true;
  } else {
    loadingMore.value = true;
  }
  try {
    const res = await getProcessListPage(props.taskNumber, {
      pageNum: page,
      pageSize,
    });
    const data = res?.code === 0 ? res.data : res;
    const records: TasksProcesses[] = data?.records || [];
    total.value = data?.total || 0;
    if (page === 1) {
      processList.value = records;
      // 默认展开第一条
      activeNames.value = records[0]?.id != null ? [String(records[0].id)] : [];
    } else {
      processList.value = processList.value.concat(records);
    }
    // 是否还有更多
    noMore.value = processList.value.length >= total.value || records.length === 0;
    pageNum.value = page;
    // 第一页加载完成后，若内容未撑满容器则继续加载下一页，直到出现滚动条
    nextTick(() => {
      checkAutoLoad();
    });
  } catch (error) {
    console.error('获取处置记录失败:', error);
    if (page === 1) processList.value = [];
    noMore.value = true;
  } finally {
    loading.value = false;
    loadingMore.value = false;
  }
};

const loadMore = () => {
  if (loadingMore.value || noMore.value) return;
  fetchPage(pageNum.value + 1);
};

// 检查是否需要自动加载下一页
// 当内容未撑满滚动容器（无滚动条）且还有更多数据时，继续加载下一页
const checkAutoLoad = () => {
  if (loading.value || loadingMore.value || noMore.value) return;
  if (processList.value.length === 0) return;
  const container = scrollRef.value;
  if (!container) return;
  // 内容高度 <= 容器高度，说明未撑满，没有滚动条
  if (container.scrollHeight <= container.clientHeight) {
    loadMore();
  }
};

const handleScroll = (e: Event) => {
  const target = e.target as HTMLElement;
  // 距离底部小于 50px 时加载下一页
  if (target.scrollHeight - target.scrollTop - target.clientHeight < 50) {
    loadMore();
  }
};

// 监听 taskNumber 变化重置并拉取第一页
watch(
  () => props.taskNumber,
  (val) => {
    if (val) {
      pageNum.value = 1;
      noMore.value = false;
      processList.value = [];
      fetchPage(1);
    } else {
      processList.value = [];
      total.value = 0;
      noMore.value = true;
    }
  },
  { immediate: true }
);
</script>

<style scoped lang="less">
.process-record {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 400px;

  .record-header {
    display: flex;
    align-items: center;
    padding: 16px 0;
    flex-shrink: 0;

    .blue-block {
      display: inline-block;
      width: 3px;
      height: 14px;
      margin-right: 4px;
      background: #264ed1;
      vertical-align: middle;
    }

    .record-title {
      font-size: 14px;
      font-weight: 600;
      color: var(--text-color, #1d2129);
    }
  }

  .record-content {
    flex: 1;
    overflow-y: auto;

    .record-collapse {
      border: none;

      :deep(.el-collapse-item) {
        margin-bottom: 8px;
        border-radius: 4px;
        overflow: hidden;

        .el-collapse-item__header {
          height: auto;
          min-height: 48px;
          padding-left: 12px;
          font-size: 13px;
          line-height: 1.4;
          background: #EEF2FF;
          border: 1px solid #264ED13B;
          border-radius: 4px;
          overflow: hidden;

          .el-collapse-item__arrow {
            // 把箭头放最左
            order: 0;
            margin: 0 8px 0 0;
            color: #86909c;
          }
        }

        .el-collapse-item__wrap {
          border-bottom: none;

          .el-collapse-item__content {
            padding: 10px 12px;
            font-size: 13px;
            line-height: 1.6;
          }
        }
      }

      .collapse-title {
        display: flex;
        align-items: center;
        flex-wrap: wrap;
        gap: 8px;
        width: 100%;
        font-weight: 500;
        color: var(--text-color, #1d2129);
      }
    }

    .record-item {
      .record-field {
        display: flex;
        flex-wrap: wrap;
        gap: 6px;
        font-size: 13px;
        line-height: 1.6;
        margin-bottom: 8px;
        padding-left: 4px;

        .field-label {
          color: var(--text-color-secondary, #86909c);
          flex-shrink: 0;
        }

        .field-value {
          flex: 1;
          color: var(--text-color, #1d2129);
          word-break: break-all;
        }
      }

      .record-next {
        font-size: 13px;
        line-height: 1.6;
        margin-bottom: 8px;
        padding-left: 4px;

        .field-label {
          display: block;
          font-size: 12px;
          color: var(--text-color-secondary, #86909c);
          line-height: 1.5;
          margin-bottom: 8px;
        }

        .next-list {
          list-style: none;
          padding: 0;
          margin: 0;

          .next-item {
            display: flex;
            align-items: center;
            gap: 6px;
            font-size: 12px;
            line-height: 1.8;
            background: #F7F9FC;
            padding: 4px 8px;
            border-radius: 4px;
            margin-bottom: 4px;

            .next-name {
              color: var(--text-color, #1d2129);
            }
          }
        }
      }
    }

    .load-more-tip {
      text-align: center;
      font-size: 12px;
      color: var(--text-color-secondary, #86909c);
      padding: 12px 0;

      &.no-more {
        color: var(--text-color-placeholder, #c9cdd4);
      }
    }
  }
}
</style>
