<template>
  <view v-if="showGroup" class="group-statistics2" @click="handleCreateGroup">
    <view class="title-box">
      <view class="title-text">
        <img
          src="@/static/5110/create_group1.png"
          :width="adaptationSize.createMinGroupWidth"
          :height="adaptationSize.createMinGroupHeight"
          style="flex-shrink: 0; pointer-events: none"
        />
        <span>{{ title }}</span>
      </view>
      <img
        src="@/assets/svg/create_group_button.svg"
        @click="handleReview"
        width="64px"
        height="25px"
        style="flex-shrink: 0"
        class="clickable"
      />
    </view>
  </view>
  <view class="button-box" v-else-if="enabled">
    <view class="create-group custom-create" @click="handleCreateGroup">
      <img
        src="@/static/5110/create_group1.png"
        :width="adaptationSize.createGroupWidth"
        :height="adaptationSize.createGroupHeight"
        :style="{ margin: `auto ${marginLR}` }"
        style="flex-shrink: 0; pointer-events: none"
      />
      <div
        class="group-text"
        style="pointer-events: none"
        :style="{ fontSize: fontSize, }"
        :class="title.length > 5 ? 'group-text-overflow' : ''"
      >
        {{ title }}
      </div>
    </view>
  </view>
</template>

<script setup>
  import { ref, computed } from 'vue';

  import { checkConfigSwitch } from '@/common/utils';
  import { useEmitter } from '@/hooks/useEmitter.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { debounce } from '@/utils';

  const props = defineProps({
    showGroup: {
      type: Boolean,
      default: false,
    },
    // 按钮配置
    config: {
      type: Object,
      default: () => ({ title: '自定义建群', enabled: false }),
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

  const emitter = useEmitter();
  const communicationStore = useCommunicationStore();
  const pageUrlStore = usePageUrlStore();
  const { adaptationSize } = useDeviceAdapter();

  // 计算属性：标题
  const title = computed(() => props.config?.title || '自定义建群');

  // 计算属性：是否显示
  const enabled = computed(() => props.config?.enabled === true);

  // 状态
  const showCollaborationFeatures = ref(false);
  // 雄安环境
  const env = ref('');
  // 初始化
  const initData = async () => {
    showCollaborationFeatures.value = await checkConfigSwitch();
  };

  // 监听事件
  useEmitter('INDEX_INIT', initData);

  // 自定义建群点击 - 内部管理跳转逻辑
  const _handleCreateGroup = async () => {
    // 新版：走H5建群页面
    const runNewCreateGroup = async () => {
      try {
        const url = pageUrlStore.getFullPageUrl('/pages/customGroup');
        await communicationStore.openUrl(url, null, 'noTitleStyle');
      } catch (error) {
        console.error('跳转H5建群页面失败:', error);
      }
    }

    // 旧版：走警信建群页面
    const runOldCreateGroup = async () => {
      try {
        const groupParams = {
          groupName: '',
          groupType: 3,
          introduction: '',
          needSelectMember: true,
        };
        const groupDetail = await communicationStore.createGroup(groupParams);

        if (groupDetail?.groupId) {
          // 跳转聊天页面
          const chatParams = {
            id: groupDetail.groupId,
            category: 2, // 群聊
          };
          await communicationStore.sms(chatParams);

          // 切换到会话页
          setTimeout(() => communicationStore.switchTab({ appId: 'ITEM_SESSION_PAGE' }), 1000);
          emit('create-success', groupDetail);
        }
      } catch (error) {
        console.error('创建群组失败:', error);
      }
      
    }
    
    // 根据配置走不同建群逻辑
    if (showCollaborationFeatures.value) {
      runNewCreateGroup();
    } else if (env.value === 'xiongan') {
      runOldCreateGroup();
    } else {
      runOldCreateGroup();
    }
  };

  const handleCreateGroup = debounce(_handleCreateGroup, 300, true);

  // 暴露方法
  defineExpose({
    initData,
  });
</script>

<style lang="scss" scoped>
  .button-box {
    display: flex;
    align-items: stretch;
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
  .group-statistics2 {
    width: 100%;
    padding: 10px;
    background: #fff;
    border-radius: 12px;
    gap: 12px;
    .title-box {
      display: flex;
      align-items: center;
      justify-content: space-between;
      .title-text {
        display: flex;
        align-items: center;

        img {
          margin-right: 4px;
        }

        span {
          font-weight: 500;
          color: rgba(3, 11, 38, 1);
          font-size: 16px;
          line-height: 24px;
        }
      }

      .review {
        font-size: 14px;
        display: flex;
        align-items: center;
        color: #264ed1;
        cursor: pointer;
      }
    }
    .button-box {
      .arrow-right {
        width: 7px;
        height: 7px;
        border-right: 1px solid rgb(96, 98, 102);
        border-bottom: 1px solid rgb(96, 98, 102);
        transform: rotate(-45deg);
        margin-left: 4px;
      }
    }
  }
</style>
