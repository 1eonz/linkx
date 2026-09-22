<template>
  <view class="history-wrap">
    <!-- 头部导航栏 -->
    <view class="header">
      <NavBar title="历史对话"  @back="gotoBack" />
    </view>

    <view class="main">
      <view class="history-item"
        v-for="history in sessionHistoryList"
        :key="history.agentId"
        @click="gotoIndex(history)"
        @touchstart="(e) => touchStartHistory(e, history)"
        @touchend="touchEndHistory"
        @touchmove="touchMoveHistory">
        <view class="history-content">
          <text class="content-text">{{ history.latestQueryContent }}</text>
        </view>
        <view class="history-agent">
          <view class="agent-info">
            <AuthImg :picUrl="getFullAvatarUrl(history.avatar)" class="agent-icon"/>
            <text class="agent-name">{{ history.agentName }}</text>
            <text class="agent-time">{{ formatLastTime(history.latestTime) }}</text>
          </view>
          <!-- 状态标签 -->
          <view v-if="getStatusInfo(history).label" class="status-tag" :class="getStatusInfo(history).type">
            <view v-if="getStatusInfo(history).type === 'processing'" class="status-icon loading-icon"></view>
            <view v-if="getStatusInfo(history).type === 'completed'" class="status-icon completed-icon"></view>
            <text class="status-text">{{ getStatusInfo(history).label }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>

  <van-dialog 
    v-model:show="deleteModalVisible" 
    class="delete-dialog" 
    confirm-button-text="删除" 
    :show-cancel-button="false"
    :close-on-click-overlay="true" 
    @confirm="confirmDelete">
    <view style="padding: 16px 16px 8px 16px; color: rgba(142, 145, 157, 1); font-size: 16px; font-weight: 400; text-align: center;">
      确认删除？
    </view>
  </van-dialog>
</template>

<script setup>
import { ref, onMounted } from "vue";
import AuthImg from "@/components/AuthImg/index.vue";
import NavBar from './components/NavBar/index.vue';
import { useRouter } from "vue-router";

import { getUsedAgents, deleteUsedAgent } from "@/common/api/ai.js";
import { getBaseUrl } from '@/common/config.js';
import { saveSelectedAgent } from '@/pages/aiAssistantNew/composables/useAgentCache';
import AiAgentChatPausePosition from '@/pages/aiAssistantNew/composables/AiAgentChatPausePosition.js';
import { useCommunicationStore } from "@/stores/communication.js";

// 用户信息
const communicationStore = useCommunicationStore();
const userInfo = ref({
  username: "",
  userid: null,
  idCard: null,
  thumbAvatar: "",
});

// 路由
const router = useRouter();

const emit = defineEmits(["clickHistory", "deleteSession"]);
// 长按删除
const sessionPressTimer = ref(null);
const deleteModalVisible = ref(false);
const toBeDelHistory = ref(null);
const sessionStartX = ref(0);
const sessionStartY = ref(0);
const sessionIsMoving = ref(false);

// 会话记录列表
const sessionHistoryList = ref([]);

// 获取会话状态信息（后端 latestReplyPaused 已废弃，改用本地缓存的暂停状态判断）
function getStatusInfo(item) {
  const pauseData = AiAgentChatPausePosition.getPositionByAgent(item.agentId);
  if (pauseData && pauseData.replyPosition !== undefined) {
    return { label: '进行中', type: 'processing' };
  }
  return { label: '', type: '' };
}

// 时间格式化函数
function formatLastTime(timeSource) {
  if (!timeSource) return "";
  const date = new Date(timeSource);
  if (Number.isNaN(date.getTime())) return "";

  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  const yesterday = new Date();
  yesterday.setDate(now.getDate() - 1);
  const isYesterday = date.toDateString() === yesterday.toDateString();

  if (isToday) {
    const hours = String(date.getHours()).padStart(2, "0");
    const minutes = String(date.getMinutes()).padStart(2, "0");
    return `${hours}:${minutes}`;
  }
  if (isYesterday) return "昨天";
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, "0");
  const d = String(date.getDate()).padStart(2, "0");
  return `${y}-${m}-${d}`;
}

function getFullAvatarUrl(avatar) {
  if (!avatar) return '';
  if (avatar.startsWith('http://') || avatar.startsWith('https://')) {
    return avatar;
  }
  const baseUrl = getBaseUrl() + '/XA-ics-agent'
  const path = avatar.startsWith('/') ? avatar : `/${avatar}`;
  return `${baseUrl}${path}`;
}

function touchStartHistory(e, history) {
  toBeDelHistory.value = history;
  sessionStartX.value = e.touches[0].clientX;
  sessionStartY.value = e.touches[0].clientY;
  sessionIsMoving.value = false;
  clearTimeout(sessionPressTimer.value);
  sessionPressTimer.value = setTimeout(() => {
    if (!sessionIsMoving.value) {
      deleteModalVisible.value = true;
    }
  }, 500);
}

function touchEndHistory() {
  clearTimeout(sessionPressTimer.value);
}

function touchMoveHistory(e) {
  const moveX = Math.abs(e.touches[0].clientX - sessionStartX.value);
  const moveY = Math.abs(e.touches[0].clientY - sessionStartY.value);
  if (moveX > 5 || moveY > 5) {
    sessionIsMoving.value = true;
    clearTimeout(sessionPressTimer.value);
  }
}

async function confirmDelete() {
  deleteModalVisible.value = false;
  if (toBeDelHistory.value) {
    try {
      // 调用后端删除接口
      console.log('deleteUsedAgent', userInfo.value.idCard, {
        agentConfigId: toBeDelHistory.value.agentConfigId,
      })
      await deleteUsedAgent(userInfo.value.idCard, {
        agentConfigId: toBeDelHistory.value.agentConfigId,
      });
      // 删除成功后刷新列表
      await getsessionHistoryList();
    } catch (error) {
      console.error('删除历史对话失败:', error);
    }
  }
  toBeDelHistory.value = null;
}

async function getsessionHistoryList() {
  const res = await getUsedAgents({userId: userInfo?.value?.idCard || ''});
  console.log('getUsedAgents', res);
  // 先缓存一下list，做判空处理
  const list = res || [];
  // 这里处理如果是默认的海智助手（判断agentId是1），则显示为公安AI助手
  sessionHistoryList.value = list.map(item => ({
    ...item,
    // agentName: item.agentId === 1 ? '公安AI助手' : item.agentName,
  }))
}

// 返回上一页
function gotoBack() {
  router.back();
}

// 跳转首页根据智能体信息
function gotoIndex(history) {
  // 缓存+路由跳转
  const agent = { index: history.agentId, name: history.agentName, picUrl: history.avatar };
  saveSelectedAgent(agent, 'session');
  router.push({ 
    path: '/pages/aiAssistantNew/index', 
    query: { 
      index: history.agentId,  
      name: history.agentName,  
      picUrl: history.avatar,  
    }
  });
}

// 获取用户信息
async function getUserInfo() {
  try {
    const res = await communicationStore.getUserInfo();
    if (res) {
      userInfo.value = {
        username: res.username || "",
        userid: res.userid || null,
        idCard: res.idCard || null,
        thumbAvatar: res.thumbAvatar || "",
      };
    }
  } catch (error) {
    console.error(`获取用户信息错误: ${error.message}`);
  }
}

onMounted(async () => {
  await getUserInfo();
  getsessionHistoryList();

});
</script>

<style scoped lang="scss">
.history-wrap {
  overflow: hidden;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg,
      rgba(204, 220, 249, 1) 0%,
      rgb(237, 240, 243) 40%);

  .main {
    flex: 1;
    overflow-y: auto;
    padding: 0 12px;

    .history-item {
      padding: 14px 16px;
      margin-bottom: 8px;
      background-color: #fff;
      border-radius: 8px;

      .history-content {
        margin-bottom: 8px;
        display: flex;
        align-items: flex-start;

        .content-text {
          flex: 1;
          font-size: 16px;
          color: #333;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          display: block;
        }
      }

      .history-agent {
        display: flex;
        align-items: center;
        justify-content: space-between;

        .agent-info {
          display: flex;
          align-items: center;
          flex: 1;
          overflow: hidden;

          .agent-icon {
            width: 20px;
            height: 20px;
            border-radius: 8px;
            margin-right: 8px;
          }

          .agent-name {
            font-size: 13px;
            color: rgba(142, 145, 157, 1);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }

          .agent-time {
            font-size: 13px;
            color: rgba(142, 145, 157, 1);
            margin-left: 8px;
          }
        }

        .status-tag {
          display: inline-flex;
          align-items: center;
          padding: 2px 6px;
          font-size: 12px;
          border-radius: 4px;
          vertical-align: middle;
          flex-shrink: 0;
          margin-left: 8px;

          .status-icon {
            width: 12px;
            height: 12px;
            margin-right: 4px;
          }

          .loading-icon {
            background: url('@/assets/svg/loading.svg') no-repeat center;
            background-size: contain;
            animation: rotate 1s linear infinite;
          }

          .completed-icon {
            background: url('@/assets/svg/circle2.svg') no-repeat center;
            background-size: contain;
            transform: scaleX(-1);
          }

          .status-text {
            font-size: 12px;
          }

          &.processing {
            color: rgba(255, 255, 255, 1);
            background: rgba(38, 78, 209, 1);
          }

          &.paused {
            color: rgba(250, 120, 27, 1);
            background: rgba(250, 120, 27, 0.1);
          }

          &.completed {
            color: rgba(67, 207, 124, 1);
            background: rgba(67, 207, 124, 0.1);
          }
        }

        @keyframes rotate {
          from {
            transform: rotate(0deg);
          }
          to {
            transform: rotate(360deg);
          }
        }
      }
    }
  }
}
</style>