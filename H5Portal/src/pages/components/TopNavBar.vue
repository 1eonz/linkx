<template>
  <view class="top-nav-bar" :style="{ 
    'padding-top': paddingTop + 'px',
    'background-color': bgColor 
  }">
    <!-- 左侧插槽 -->
    <view class="left">
      <slot name="left">
        <view class="perch"></view>
      </slot>
    </view>
    
    <!-- 标题 -->
    <slot name="title" :title="displayTitle" v-if="showTitle">
      <text class="title" :style="{ color: titleColor }" >
        {{ displayTitle }}
      </text>
    </slot>
    
    <view v-else class="perch"></view>
    
    <!-- 右侧插槽 -->
    <view class="right">
      <slot name="right">
        <view class="perch"></view>
      </slot>
    </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';

import { getGlobalsConfigByKey } from '@/common/utils';
import { useEmitter } from '@/hooks/useEmitter.js';
import { useCommunicationStore } from '@/stores/communication.js';
import { useSystemConfigStore } from '@/stores/systemConfig.js';

const props = defineProps({
  // license权限（用于判断是否显示标题）
  licensePermissions: {
    type: Object,
    default: () => ({}),
  },
  // 是否显示标题
  showTitle: {
    type: Boolean,
    default: true,
  },
  // 背景色
  bgColor: {
    type: String,
    default: '#152584',
  },
  // 标题颜色
  titleColor: {
    type: String,
    default: '#fff',
  },
  // 自定义标题（优先级最高）
  title: {
    type: String,
    default: '',
  },
});

const emitter = useEmitter();
const communicationStore = useCommunicationStore();
const systemConfigStore = useSystemConfigStore();

const paddingTop = ref(0);
const title = ref('');

// 计算属性
// 如果传了 title prop，直接显示标题（不需要判断 licensePermissions）
// 否则按原逻辑判断：showTitle && licensePermissions.LINKXBS === '1'
const showTitle = computed(() => {
  if (props.title) {
    return true;
  }
  return props.showTitle && props.licensePermissions?.LINKXBS === '1';
});
// 优先级：props.title > SYSTEM_NAME > 全局配置title > 默认值'5110'
const displayTitle = computed(() => props.title || systemConfigStore.systemName || title.value || '5110');

// 获取状态栏高度
const fetchStatusBarHeight = async () => {
  paddingTop.value = await communicationStore.fetchStatusBarHeight();
};

// 获取标题
const fetchTitle = async () => {
  title.value = await getGlobalsConfigByKey('title');
};

// 初始化
const initData = async () => {
  await fetchStatusBarHeight();
  // 获取系统配置（包含SYSTEM_NAME）
  await systemConfigStore.fetchSystemConfig();
  await fetchTitle();
};

// 刷新
const refresh = async () => {
  await systemConfigStore.fetchSystemConfig();
  await fetchTitle();
};

// 监听事件
useEmitter('INDEX_INIT', initData);
useEmitter('INDEX_REFRESH', refresh);

onMounted(() => {
  initData();
});

// 暴露方法
defineExpose({
  initData,
  refresh,
  paddingTop,
});
</script>

<style lang="scss" scoped>
.top-nav-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background-color: #152584;
  padding-left: 16px;
  padding-right: 16px;
  white-space: nowrap;
  box-sizing: border-box;
  margin-bottom: 4px;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  z-index: 1000;

  .left,
  .right {
    width: 94px;
    height: 24px;
    display: flex;
    align-items: center;
  }

  .right {
    justify-content: flex-end;
  }

  .title {
    flex: 1;
    max-width: calc(100% - 94px - 94px);
    height: 44px;
    line-height: 44px;
    color: #fff;
    font-size: 18px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    text-align: center;
  }

  @media screen and (min-height: 1200px) {
    .title {
      height: 88px;
      line-height: 88px;
      font-size: 0.8rem;
    }
  }

  @media screen and (min-height: 2001px) {
    .title {
      font-size: 0.6rem;
    }
  }

  .perch {
    width: 94px;
    height: 24px;
  }
}
</style>