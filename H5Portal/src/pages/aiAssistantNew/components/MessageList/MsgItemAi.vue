<template>
  <view class="ai-message">
    <!-- 头像 -->
    <img
      :width="adaptationSize.radioSize"
      :height="adaptationSize.radioSize"
      :src="agentAvatarUrl"
      style="border-radius: 50%"
    />
   <view class="ai-content-box">
    <!-- 名称 + 时间 -->
    <view class="name-time-row">
      <text class="agent-name">{{ agentName }}</text>
      <text class="msg-time">{{ msg.answerTime }}</text>
    </view>
    <!-- 气泡 + 继续按钮 并排 -->
    <view class="bubble-row">
      <!-- 气泡 -->
      <view
        ref="bubbleRef"
        class="message-content ai-bubble"
        @touchstart="handleTouchStart"
        @touchend="handleTouchEnd"
        @touchmove="handleTouchMove"
        @contextmenu.prevent
      >
        <!-- 回答内容 -->
        <view v-if="renderedContent" v-html="renderedContent" class="rendered-content"></view>
        <!-- 流式输出中的loading -->
        <text v-if="msg.isStreaming && !renderedContent" class="loading-text">正在查询...</text>
        <!-- 等待审批提示 -->
        <text v-if="!msg.isStreaming && content === '等待审批...'" class="waiting-text">等待审批...</text>

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

      <!-- 继续按钮 - 在气泡右边，和气泡同一层级 -->
      <view v-if="msg.isPaused" class="resume-btn-wrapper">
        <view class="resume-btn" @click.stop="handleResume">
          <text class="resume-text">继续生成</text>
        </view>
      </view>
    </view>
   </view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { marked } from 'marked'
import { useDeviceAdapter } from "@/stores/useDeviceAdapter.js";
import { useTextSelection } from "../../composables/useTextSelection.js";
import { showCustomToast } from "@/utils/toast.js";
import { getBaseUrl } from '@/common/config.js'
import aiAvatarDefault from "@/static/ai/ai_avatar.png";

const { adaptationSize } = useDeviceAdapter();

const bubbleRef = ref(null);

const props = defineProps({
  content: { type: String, default: '' },
  msg: {
    type: Object,
    default: () => ({})
  },
  index: {
    type: Number,
    default: -1
  },
  agents: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['longpress', 'resume'])

function getFullAvatarUrl(avatar) {
  console.log('MsgItemAi----------getFullAvatarUrl: avatar-----1111111', avatar)
  if (!avatar) return aiAvatarDefault;
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) return avatar;
  const baseUrl = getBaseUrl() + '/XA-ics-agent'
  const path = avatar.startsWith('/') ? avatar : `/${avatar}`;
  return `${baseUrl}${path}`;
}

const agentAvatarUrl = computed(() => {
  console.log('MsgItemAi----------getFullAvatarUrl: avatar-----2222222', props.msg)
  const agentId = props.msg?.agentConfigId;
  if (!agentId) return aiAvatarDefault;
  const matchedAgent = props.agents.find(a => String(a.index) === String(agentId));
  console.log('MsgItemAi----------getFullAvatarUrl: avatar-----3333333', matchedAgent)
  if (matchedAgent?.picUrl) return getFullAvatarUrl(matchedAgent.picUrl);
  return aiAvatarDefault;
})

// 智能体名称：优先使用接口返回的 agentName，其次使用智能体列表匹配的名称
const agentName = computed(() => {
  if (props.msg?.agentName) return props.msg.agentName
  const agentId = props.msg?.agentId
  if (!agentId) return 'AI助手'
  const matchedAgent = props.agents.find(a => String(a.index) === String(agentId))
  return matchedAgent?.name || 'AI助手'
})

const handleResume = () => {
  emit('resume', props.index)
}

const renderedContent = computed(() => {
  if (!props.content) return ''
  if (props.msg.isStreaming && props.content === '正在查询...') return ''
  if (props.content === '等待审批...') return ''
  if (props.content === '该问题审批未通过，无法获取答案。') return ''
  return marked.parse(props.content)
})

// ====== 长按选区 + 游标 + 操作菜单 ======
// 流式输出中禁用选择，避免与打字效果冲突；暂停状态下允许选择复制
const selectionEnabled = () => !props.msg.isStreaming || props.msg.isPaused

const {
  showMenu,
  menuPosition,
  selectedText,
  handleTouchStart,
  handleTouchEnd,
  handleTouchMove,
  copySelection,
  copyAll,
  closeSelection,
  setup: setupTextSelection,
} = useTextSelection(bubbleRef, { showToast: showCustomToast, enabled: selectionEnabled })

onMounted(() => {
  setupTextSelection()
})

// 菜单项按下反馈：touchstart 加 active，touchend 立即移除
function onItemTouchStart(e) {
  e.currentTarget.classList.add('active')
}
function onItemTouchEnd(e) {
  e.currentTarget.classList.remove('active')
}
</script>

<style scoped lang="scss">
.ai-message {
  display: flex;
  margin-bottom: 16px;
  padding: 0 12px;
  .ai-content-box {
    flex: 1;
    display: flex;
    flex-direction: column;

    .name-time-row {
      display: flex;
      align-items: center;
      margin: 0 10px 6px 10px;
    }

    .agent-name {
      font-size: 12px;
      line-height: 150%;
      color: rgba(91, 96, 114, 1);
      max-width: 7em;
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
      margin-right: 8px;
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

    .bubble-row {
      display: flex;
    }

    .message-content {
        max-width: 70%;
        margin: 0 10px;
        padding: 10px 14px;
        border-radius: 12px;
        border-top-left-radius: 4px;
        word-break: break-all;
        border: 0.1px solid #eee;
        position: relative; /* 添加相对定位，使子元素绝对定位相对于此容器 */
        overflow: visible;  /* 允许工具栏菜单溢出显示 */
      }

      .resume-btn-wrapper {
        flex-shrink: 0;
        height: 100%;
        display: flex;
        flex-direction: column;
        justify-content: flex-end;
      }
  }
}


.ai-bubble {
  background-color: #fff;
  -webkit-user-select: none;
  -moz-user-select: none;
  -ms-user-select: none;
  user-select: none;
  -webkit-touch-callout: none;
}

.loading-text {
  color: #999;
  font-size: 14px;
}

.waiting-text {
  color: #666;
  font-size: 14px;
}

.streaming-cursor {
  color: #1E52F2;
  animation: blink 1s infinite;
}

@keyframes blink {
  0%, 50% { opacity: 1; }
  51%, 100% { opacity: 0; }
}



.resume-btn {
  padding: 6px 16px;
  background-color: #fff;
  border: 1px solid #ddd;
  border-radius: 16px;
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

.rendered-content {
  overflow: hidden;
  word-break: break-word;

  :deep(pre) {
    overflow-x: auto;
    max-width: 100%;
    margin: 8px 0;
    padding: 8px;
    background: #f5f5f5;
    border-radius: 4px;
    font-size: 13px;
  }

  :deep(code) {
    word-break: break-all;
    font-size: 13px;
  }

  :deep(pre code) {
    white-space: pre-wrap;
    word-break: break-all;
  }

  :deep(table) {
    display: block;
    overflow-x: auto;
    max-width: 100%;
    border-collapse: collapse;
  }

  :deep(img) {
    max-width: 100%;
    height: auto;
  }

  :deep(p) {
    margin: 4px 0;
  }

  :deep(a) {
    word-break: break-all;
  }
}
</style>