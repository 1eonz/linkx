<template>
  <view class="button-box" v-if="enabled">
    <view class="create-group function-create" @click="handleCreateGroup">
      <img
        src="@/static/5110/FunctionCreateGroup.png"
        :width="adaptationSize.createGroupWidth"
        :height="adaptationSize.createGroupHeight"
        :style="{ margin: `auto ${marginLR}` }"
        style="flex-shrink: 0; pointer-events: none"
      />
      <div
        class="group-text"
        :class="title.length > 5 ? 'group-text-overflow' : ''"
        :style="{ fontSize: fontSize, }"
        style="pointer-events: none"
      >
        {{ title }}
      </div>
    </view>
  </view>
</template>

<script setup>
  import { computed } from 'vue';

  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { debounce } from '@/utils';

  const props = defineProps({
    // accessToken
    accessToken: {
      type: String,
      default: '',
    },
    // 板块自定义配置
    config: {
      type: Object,
      default: () => ({ title: '一键调度', enabled: false }),
    },
    // 字体大小
    fontSize: {
      type: String,
      default: '12px',
    },
    // 图标左侧间距
    marginLR: {
      type: String,
      default: '0px',
    },
  });

  const emit = defineEmits(['create-success']);

  const communicationStore = useCommunicationStore();
  const pageUrlStore = usePageUrlStore();
  const { adaptationSize } = useDeviceAdapter();

  // 计算属性：标题
  const title = computed(() => props.config?.title || '一键调度');

  // 计算属性：是否显示（权限判断已在父组件完成）
  const enabled = computed(() => props.config?.enabled === true);

  // 一键调度点击 - 内部管理跳转逻辑
  const _handleCreateGroup = async () => {
    try {
      // 跳转一键调度页面
      const url = pageUrlStore.getFullPageUrl('/pages/dispatchGroup');
      await communicationStore.openUrl(url, null, 'noTitleStyle');

      emit('create-success');
    } catch (error) {
      console.error('一键调度失败:', error);
    }
  };

  const handleCreateGroup = debounce(_handleCreateGroup, 300, true);

  // 暴露方法
  defineExpose({});
</script>

<style lang="scss" scoped>
  .button-box {
    display: flex;
    align-items: stretch;
    height: 50px;
    flex: 1;
    min-width: 100px;
    height: 50px;
  }
  @media (min-width: 735px) and (max-width: 1024px) {
  /* 样式 */
  .button-box {
    height: 60px;
  }
  }
  .create-group {
    background-color: #fff;
    border-radius: 12px;
    overflow: hidden;
    display: flex;
    align-items: center;
    flex: 1;
    min-width: 0;
    justify-content: center;
    box-sizing: border-box;

    .group-text {
      line-height: 22px;
      margin-left: 2px;
      color: #333;
      font-size: 12px;
      width: 100%;
    }
    .group-text-overflow {
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
</style>
