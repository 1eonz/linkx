<template>
  <view class="message-list">
    <view class="section-header">
      <text class="section-title">{{ sectionTitle }}</text>
      <ArrowIcon @click="handleHeaderClick" />
    </view>
    <!-- loading 状态 -->
    <view v-if="loading" class="loading-wrapper">
      <van-loading size="24px" color="#1989fa" />
    </view>
    <!-- 空数据状态 -->
    <view v-else-if="!messageList.length" class="swiper-empty">
      <img class="empty-img" src="@/static/task/indexTypeEmpty.png" />
      <view class="empty-text">暂无数据</view>
    </view>
    <!-- 内容区域：左侧消息列表 + 右侧图表 -->
    <view v-else class="content-wrapper">
      <!-- 左侧消息列表（最多显示4条） -->
      <view class="message-grid" :class="{ 'grid-3-col': columnNum === 3 }">
        <view
          v-for="(item, index) in displayList"
          :key="item.title"
          class="message-item-wrapper"
          @click="handleMessageClick(item)"
        >
          <view class="message-item" :style="{ borderColor: getColorByIndex(index)?.borderColor }">
            <view class="message-left">
              <view class="message-unread">
                <text class="unread-count" :style="{ color: getColorByIndex(index)?.color }">{{ formatUnread(item.unread) }}</text>
              </view>
              <text class="message-title">{{ item.title }}</text>
            </view>
          </view>
        </view>
      </view>
      <!-- 右侧图表 -->
      <view class="chart-wrapper">
        <RoundedPieChart
          :data="chartData"
          :inner-text="chartInnerText"
          :colors="chartColors"
          :border-radius="6"
          :radius="['70%', '100%']"
          :width="chartSize + 'px'"
          :height="chartSize + 'px'"
        />
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';

import DC from '@/common/network/DC.js';
import { groupApi } from '@/common/api/index.js';
import RoundedPieChart from '@/components/Charts/RoundedPieChart.vue';
import { getColorByIndex } from '@/config/messageColors.js';
import { useEmitter } from '@/hooks/useEmitter.js';
import { useCommunicationStore } from '@/stores/communication.js';
import { usePageUrlStore } from '@/stores/pageUrl.js';
import ArrowIcon from '@/components/ArrowIcon/index.vue';


const props = defineProps({
  // license权限
  licensePermissions: {
    type: Object,
    default: () => ({}),
  },
  // 用户信息
  userInfo: {
    type: Object,
    default: () => ({}),
  },
  // accessToken
  accessToken: {
    type: String,
    default: '',
  },
  // 配置对象
  section: {
    type: Object,
    default: () => ({}),
  },
  // custom配置对象（从section.custom解析）
  custom: {
    type: Object,
    default: () => ({}),
  },
});

const emitter = useEmitter();
const pageUrlStore = usePageUrlStore();
const communicationStore = useCommunicationStore();

// 组件标题
const sectionTitle = computed(() => props.section?.name || '消息列表提醒');

// 每行显示数量：1条数据占满，偶数显示2列，其他显示3列
const columnNum = computed(() => {
  const count = displayList.value.length;
  if (count === 1) return 1;
  return count % 2 === 0 ? 2 : 3;
});

// 图标大小：3列时25px，2列时34px
const iconSize = computed(() => {
  return columnNum.value === 3 ? '25px' : '34px';
});

// 图表大小：偶数列(2列)时100px，3列时75px
const chartSize = computed(() => {
  return columnNum.value === 2 ? 100 : 75;
});

// 图表中间文字
const chartInnerText = computed(() => {
  const total = messageList.value.reduce((sum, item) => sum + (+item.unread || 0), 0);
  return {
    value: total,
    label: '统计占比',
    valueColor: '#333',
    valueSize: chartSize.value === 100 ? 20 : 16,
    labelColor: '#999',
    labelSize: chartSize.value === 100 ? 12 : 10,
  };
});

// 图表数据：超过4条时显示前4+其他（剩余所有数据未读数总和）
const chartData = computed(() => {
  const list = messageList.value;
  if (list.length <= 4) {
    return list.map((item) => ({
      name: item.title,
      value: +item.unread || 0,
    }));
  }
  // 前4条 + 其他
  const top4 = list.slice(0, 4).map((item) => ({
    name: item.title,
    value: +item.unread || 0,
  }));
  const otherTotal = list.slice(4).reduce((sum, item) => sum + (+item.unread || 0), 0);
  top4.push({ name: '其他', value: otherTotal });
  return top4;
});

// 图表颜色：前4条用配置颜色，第5项"其他"用灰色
const chartColors = computed(() => {
  const list = messageList.value;
  const colors = list.slice(0, 4).map((item, index) => getColorByIndex(index).color);
  if (list.length > 4) {
    colors.push('rgba(200, 201, 204, 1)'); // 其他-灰色 #C8C9CC
  }
  return colors;
});

// 消息列表数据
const messageList = ref([]);
// loading 状态
const loading = ref(true);

// 左侧显示的列表：最多4条
const displayList = computed(() => {
  return messageList.value.slice(0, 4);
});

// 格式化未读数
const formatUnread = (count) => {
  return count > 99 ? '99+' : count;
};

// 跳转页面
const gotoGroupList = async() => {
  const url = pageUrlStore.getFullPageUrl('/pages/myGroup');
  await communicationStore.openUrl(url, null, 'noTitleStyle');
};

// 标题栏点击
const handleHeaderClick = () => {
  gotoGroupList();
};

// 消息项点击
const handleMessageClick = (item) => {
  // 目前暂不跳转，后续根据需求再调整
  return;
  gotoGroupList();
};

// 获取任务类型统计
const getTaskTypeTotal = async () => {
  loading.value = true;
    try {
      // 从接口获取任务通知模块列表
      const modulesRes = await groupApi.getNotificationModules();
      if (modulesRes && Array.isArray(modulesRes)) {
        // 适配后端返回格式：纯字符串数组 ["值班管理","警单处理"] 或对象数组 [{id, moduleName}]
        const list = modulesRes.map((item) => {
          if (typeof item === 'string') {
            return { moduleName: item, title: item, unread: 0 };
          }
          return {
            moduleName: item.moduleName,
            title: item.moduleName,
            unread: 0,
          };
        });
        // 异步获取每个模块的未读数（操作临时数组，避免中间状态触发渲染闪烁）
        await fetchUnreadCount(list);
        // 数据完全准备好后一次性赋值
        messageList.value = list;
      } else {
        messageList.value = [];
      }
    } catch (err) {
      console.error('获取任务通知模块列表失败:', err);
      messageList.value = [];
    } finally {
      // 数据排序完成才移除loading，避免闪烁
      loading.value = false;
    }
};

// 异步获取每个模块的未读数（操作传入的数组，不直接修改messageList）
const fetchUnreadCount = async (list) => {
  if (!list || list.length === 0) return;
  // 并行请求所有模块的未读数
  const promises = list.map(async (item) => {
    try {
      const res = await groupApi.getNotificationModuleCount(item.moduleName);
      if (res) {
        item.unread = Number(res.unread || res.unReadCount || res.count || 0);
      }
    } catch (error) {
      console.error(`获取模块 ${item.moduleName} 未读数失败:`, error);
    }
  });
  await Promise.allSettled(promises);
  // 过滤掉未读数为0的模块
  for (let i = list.length - 1; i >= 0; i--) {
    if (!list[i].unread || list[i].unread === 0) {
      list.splice(i, 1);
    }
  }
  // 按未读数降序排序
  list.sort((a, b) => (+b.unread) - (+a.unread));
};

// 异步获取每个模块的未读数（用于定时刷新，直接修改messageList）
const updateModulesUnreadCount = async () => {
  if (messageList.value.length === 0) return;
  // 并行请求所有模块的未读数
  const promises = messageList.value.map(async (item) => {
    try {
      const res = await groupApi.getNotificationModuleCount(item.moduleName);
      if (res) {
        item.unread = Number(res.unread || res.unReadCount || res.count || 0);
      }
    } catch (error) {
      console.error(`获取模块 ${item.moduleName} 未读数失败:`, error);
    }
  });
  await Promise.allSettled(promises);
  // 过滤掉未读数为0的模块
  const filteredList = messageList.value.filter((item) => item.unread && item.unread > 0);
  messageList.value = filteredList;
  // 按未读数降序排序
  messageList.value.sort((a, b) => (+b.unread) - (+a.unread));
};

// 定时器ID
let timerId = null;

// 定时更新未读消息（定时逻辑和更新逻辑组合）
const updateIntervalUnreadCount = () => {
  // 先更新一次
  updateModulesUnreadCount();
  
  // 停止已有定时器
  if (timerId) {
    clearInterval(timerId);
    timerId = null;
  }
  
  // 开启新定时器（每分钟）
  timerId = setInterval(() => {
    updateModulesUnreadCount();
  }, 60000);
};

// 停止定时更新
const stopIntervalUnreadCount = () => {
  if (timerId) {
    clearInterval(timerId);
    timerId = null;
  }
};

// 监听页面显示/隐藏（使用SDK事件）
const handleVisibilityChange = (val) => {
  console.log('消息列表页面可见性变化', val);
  if (val === true || val === 'true') {
    // 定时更新未读消息数量
    updateIntervalUnreadCount();
  } else {
    // 页面隐藏时停止定时更新
    stopIntervalUnreadCount();
  }
};

// 初始化数据
const initData = async () => {
  await getTaskTypeTotal();
};

// 刷新数据
const refresh = async () => {
  await getTaskTypeTotal();
};

// 刷新任务数据
const tasksUpdate = () => {
  getTaskTypeTotal();
};

// 监听事件
useEmitter('INDEX_INIT', initData);
useEmitter('INDEX_REFRESH', refresh);
useEmitter('TASKS_UPDATE', tasksUpdate);
useEmitter('SDK_VISIBLE_CHANGE', handleVisibilityChange);

onMounted(() => {
  initData();
  DC.on('TASKS_UPDATE', 'CREATE', tasksUpdate);
  DC.on('TASKS_UPDATE', 'STATUS_CHANGE', tasksUpdate);
});

onUnmounted(() => {
  // 清理定时更新
  stopIntervalUnreadCount();
  window?.WeSpaceSDK?.offVisibleChange(handleVisibilityChange);
});

// 暴露方法
defineExpose({
  initData,
  refresh,
  tasksUpdate,
});
</script>

<style lang="scss" scoped>
.message-list {
  background-color: #fff;
  margin: 8px 16px 0;
  border-radius: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px;

  .section-title {
    font-size: 14px;
    font-weight: 500;
    line-height: 22px;
    height: 22px;
  }

  @media screen and (min-height: 1200px) {
    .section-title {
      font-size: 0.8rem;
    }
  }

  @media screen and (min-height: 2001px) {
    .section-title {
      font-size: 0.6rem;
    }
  }
}

@media screen and (min-height: 1200px) {
  .section-header {
    margin: 30px 0;
  }
}

// loading 状态：与空数据态保持同高，避免 loading↔空数据 切换时高度跳动
.loading-wrapper {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 150px;
}

// 空数据状态：与 loading 态保持同高
.swiper-empty {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  min-height: 150px;

  .empty-img {
    width: 80px;
    height: 80px;
  }

  .empty-text {
    font-size: 12px;
    font-weight: 400;
    color: rgba(90, 99, 131, 1);
    margin-top: 8px;
  }
}

// 内容区域：左右布局
.content-wrapper {
  display: flex;
  align-items: center;
  padding: 0 8px 12px;
}

.message-grid {
  display: flex;
  flex-wrap: wrap;
  flex: 1;

  // 3列布局
  &.grid-3-col {
    .message-item-wrapper {
      width: 33.33%;
    }
    .message-item {
      padding: 5px 10px;
    }

    .message-title {
      line-height: 21px;
    }
    .message-left {
      gap: 10px;
    }
  }
}

// 单列布局（只有一条数据时）
.message-grid:not(.grid-3-col) .message-item-wrapper:only-child {
  width: 100%;
}

.message-item-wrapper {
  width: 50%;
  padding: 6px;
  box-sizing: border-box;
}

.message-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f7f8fa;
  border-radius: 10px;
  padding: 4.33px 8.66px 4.33px 8.66px;
  width: 100%;
  box-sizing: border-box;
  background: rgba(245, 245, 245, 1);
  border: 1px solid rgba(206, 206, 206, 1);
}

.message-left {
  display: flex;
  flex-direction: column;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.message-unread {
  display: flex;
  align-items: baseline;
  margin-bottom: 4px;
}

.unread-count {
  color: #1e52f2;
  font-size: 18px;
  font-weight: 700;
  line-height: 21px;

  &.zero {
    color: #c8c9cc;
  }
}

.unread-label {
  font-size: 10px;
  color: #969799;
  margin-left: 4px;
}

.message-title {
  font-size: 12px;
  font-weight: 500;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  max-width: 100%;
  line-height: 19px;
  color: rgba(102, 102, 102, 1);
}

.message-right {
  flex-shrink: 0;
  margin-left: 8px;
}

// 图表容器
.chart-wrapper {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media screen and (min-height: 1200px) {
  .message-item {
    padding: 20px 24px;
  }

  .message-item-wrapper {
    padding: 12px;
  }

  .unread-count {
    font-size: 1.2rem;
  }

  .unread-label {
    font-size: 0.8rem;
  }

  .message-title {
    font-size: 0.9rem;
  }
}

@media screen and (min-height: 2001px) {
  .unread-count {
    font-size: 1rem;
  }

  .unread-label {
    font-size: 0.6rem;
  }

  .message-title {
    font-size: 0.7rem;
  }
}
</style>