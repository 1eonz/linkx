<template>
  <view class="user-message">
    <!-- 头像 -->
    <img
      :width="adaptationSize.radioSize"
      :height="adaptationSize.radioSize"
      :src="userAvatar"
      style="border-radius: 50%"
    />
    <view class="message-content-wrap">
      <!-- 名称 + 时间 -->
      <view class="name-time-row">
        <text class="msg-time">{{ msg.queryTime }}</text>
        <text class="user-name">{{ msg.userName }}</text>
      </view>
      <!-- 文件图标 -->
      <Attachement v-if="msg.attachement_path" :file-url="msg.attachement_path" />
      <view
        v-if="content"
        ref="bubbleRef"
        class="message-content user-bubble"
        :class="{ 'is-remind-deal': isRemindDeal }"
        @touchstart="handleTouchStart"
        @touchend="handleTouchEnd"
        @touchmove="handleTouchMove"
        @contextmenu.prevent
      >
        <text class="user-text">{{ content }}</text>
        <!-- 选区操作菜单：复制选中 / 复制全部 -->
        <view
          v-show="showMenu"
          class="action-menu"
          :style="{ top: menuPosition.y + 'px', left: menuPosition.x + 'px' }"
          @touchstart.stop
          @click.stop
        >
          <view
            class="menu-item"
            @touchstart="onItemTouchStart"
            @touchend="onItemTouchEnd"
            @click="copySelection"
          >复制选中</view>
          <view class="menu-divider"></view>
          <view
            class="menu-item"
            @touchstart="onItemTouchStart"
            @touchend="onItemTouchEnd"
            @click="copyAll"
          >复制全部</view>
        </view>
      </view>
      <!-- 审批状态组件 -->
      <ApprovalStatus
        v-if="msg.approvalStatus"
        :msg="msg"
        :approval-status="msg.approvalStatus"
        :approve-result="msg.approveResult"
        :approve-no="msg.approveNo"
        :approve-url="msg.approveUrl"
        :approve-detail-url="msg.approveDetailUrl"
        :to-leader-url="msg.toLeaderUrl"
        :approve-user="msg.approveUser"
        :user-id="props.userInfo?.userid"
        :ws-session-id="msg.wsSessionId"
        :task-id="msg.id"
        :user-info="props.userInfo"
        @remind-deal="handleUrgeApproval"
        @open-detail="handleOpenDetail"
      />
    </view>
  </view>
</template>

<script setup>
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import ApprovalStatus from '../ApprovalStatus/index.vue';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { ref, computed, onMounted } from 'vue';
  import Attachement from "./Attachement/index.vue"
  import { useTextSelection } from "../../composables/useTextSelection.js";
  import { showCustomToast } from "@/utils/toast.js";

  const communicationStore = useCommunicationStore();

  const { adaptationSize } = useDeviceAdapter();

  const props = defineProps({
    content: '',
    msg: {
      type: Object,
      default: () => ({}),
    },
    userAvatar: {
      type: String,
      default: '',
    },
    userInfo: {
      type: Object,
      default: () => ({}),
    },
  });

  const emit = defineEmits(['remind-deal', 'openDetail']);

  const isRemindDeal = ref(false)
  const handleUrgeApproval = (approveNo) => {
    isRemindDeal.value = true
    emit('remind-deal', approveNo);
  };

  const handleOpenDetail = (url) => {
    const appId = Math.random().toString().slice(2, 34).padEnd(32, '0')
    const detailUrl = url ? (url + (url.includes('?') ? '&' : '?') + 'needHiddenBack=true') : ''
    communicationStore.openLocalUrlApp(detailUrl, {}, appId, '', '审批详情');
  };

  // ====== 长按选区 + 游标 + 操作菜单 ======
  // 用户问题没有流式输出，始终允许选择复制
  const bubbleRef = ref(null);
  const {
    showMenu,
    menuPosition,
    handleTouchStart,
    handleTouchEnd,
    handleTouchMove,
    copySelection,
    copyAll,
    setup: setupTextSelection,
  } = useTextSelection(bubbleRef, { showToast: showCustomToast, enabled: () => true });

  onMounted(() => {
    setupTextSelection();
  });

  // 菜单项按下反馈：touchstart 加 active，touchend 立即移除
  function onItemTouchStart(e) {
    e.currentTarget.classList.add('active')
  }
  function onItemTouchEnd(e) {
    e.currentTarget.classList.remove('active')
  }
</script>

<style scoped lang="scss">
  .user-message {
    display: flex;
    align-items: flex-start;
    flex-direction: row-reverse;
    margin-bottom: 16px;
    padding: 0 12px;
  }
  .message-content-wrap {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    margin-left: auto;
    max-width: 70%;
    padding: 0;
    margin: 0 10px;
    border-radius: 12px;
    border-top-right-radius: 4px;
  }

  .name-time-row {
    display: flex;
    align-items: center;
    margin-bottom: 6px;
  }

  .user-name {
    font-size: 12px;
    line-height: 150%;
    color: rgba(91, 96, 114, 1);
    max-width: 7em;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
    margin-left: 8px;
    -webkit-user-select: none;
    user-select: none;
    -webkit-touch-callout: none;
  }

  .msg-time {
    font-size: 12px;
    line-height: 150%;
    color: rgba(91, 96, 114, 1);
    -webkit-user-select: none;
    user-select: none;
    -webkit-touch-callout: none;
  }
  .message-content {
    padding: 10px 14px;
    border-radius: 12px;
    border-top-right-radius: 4px;
    word-break: break-all;
    position: relative; /* 添加相对定位，使子元素绝对定位相对于此容器 */
    overflow: visible;  /* 允许工具栏菜单溢出显示 */
    &.user-bubble {
      background-color: #4363e6;
      color: #fff;
      -webkit-user-select: none;
      -moz-user-select: none;
      -ms-user-select: none;
      user-select: none;
      -webkit-touch-callout: none;
    }

    &.is-remind-deal {
       background-color: rgba(162, 179, 235, 1);
       color: rgba(255, 255, 255, 1);
    }
  }

  .user-text {
    font-size: 14px;
    line-height: 1.5;
  }

  .action-menu {
    position: absolute;
    z-index: 1000;
    background: #fff;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 8px;
    box-shadow: rgba(0, 0, 0, 0.02) 0px 2px 4px 0px,
                rgba(0, 0, 0, 0.04) 0px 4px 16px 0px,
                rgba(0, 0, 0, 0.08) 0px 8px 32px 0px;
    padding: 2px;
    font-size: 14px;
    line-height: 18px;
    transform: translateX(-50%);
    display: flex;
    align-items: center;
    white-space: nowrap;
    user-select: none;

    .menu-item {
      padding: 6px 8px;
      text-align: center;
      line-height: 18px;
      color: #000;
      flex-shrink: 0;
      border-radius: 4px;
      transition: background-color 0.15s ease;
      &.active {
        background-color: rgba(0, 0, 0, 0.06);
      }
    }

    .menu-divider {
      width: 1px;
      min-width: 1px;
      height: 14px;
      background-color: #eeeeee;
      flex-shrink: 0;
      margin: 0 2px;
    }
  }
  /* 文件指示器 */
.file-indicator {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
  padding: 4px 8px;
  background:#d3e3fd;
  border-radius: 4px;
  font-size: 14px;
  
  .file-icon {
    margin-right: 4px;
  }
  .file-text{
    color: rgba(0, 0, 0, 0.5);
  }
}
</style>
