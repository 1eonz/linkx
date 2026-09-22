<template>
  <view class="chat-historys-page">
    <!-- 顶部区域 -->
    <view :style="{ width: '100%', height: paddingTop + 'px' }"></view>
    <view class="top-nav-bar">
      <van-icon name="arrow-left" :size="adaptationSize.iconSize" color="#333" @click="handleBack" />
      <text class="title">历史会话</text>
      <view style="width: 24px"></view>
    </view>

    <!-- 会话列表 -->
    <view class="history-list">
      <view v-if="functions.length === 0" class="empty-state">
        <text class="empty-text">暂无历史会话</text>
      </view>

      <view v-else class="session-func-item" v-for="func in functions" :key="func.sessionId || func.index"
        @click="handleFunction(func)" @touchstart="(e) => handleSessionTouchStart(e, func)"
        @touchend="handleSessionTouchEnd" @touchmove="handleSessionTouchMove">
        <view class="session-func-first-row">
          <text class="session-func-lastmsg">{{ getLastMessageText(func) }}</text>
        </view>
        <view class="session-func-second-row">
          <view class="session-func-meta">
            <AuthImg :picUrl="func.picUrl || aiDefaultIcon" class="func-icon"></AuthImg>
            <text class="session-func-name">{{
              func.displayName || func.name
              }}</text>
            <text class="session-func-time">{{ formatLastTime(func) }}</text>
          </view>
          <view v-if="getStatusInfo(func).label" class="status-tag" :class="getStatusInfo(func).type">
            <img v-if="getStatusInfo(func).type === 'processing'" src="@/assets/svg/loading.svg" class="status-tag-icon"
              alt="任务进行中" />
            <img v-if="getStatusInfo(func).type === 'completed'" src="@/assets/svg/circle.svg" class="status-tag-icon"
              alt="任务已完成" />
            {{ getStatusInfo(func).label }}
          </view>
        </view>
      </view>
    </view>

    <!-- 会话删除确认弹窗 -->
    <van-dialog v-model:show="showDeleteDialog" class="delete-dialog" :show-cancel-button="false"
      confirm-button-text="删除" :close-on-click-overlay="true" @confirm="confirmDeleteSession">
      <view style="
          padding: 16px 16px 8px 16px;
          color: rgba(142, 145, 157, 1);
          font-size: 12px;
          font-weight: 400;
        ">
        确认删除？
      </view>
    </van-dialog>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from "vue";
import AuthImg from "@/components/AuthImg/index.vue";
import { useDeviceAdapter } from "@/stores/useDeviceAdapter.js";
import { useCommunicationStore } from "@/stores/communication.js";
import { usePageUrlStore } from "@/stores/pageUrl.js";
import { useAiStore } from "@/stores/ai.js";
import aiDefaultIcon from "@/assets/svg/ai-icon.svg";
import { 
  getSummaryKey, 
  getDeepSeekAgentIndex, 
  stripHtml as stripHtmlUtil,
  linkxLog
} from "@/utils/aiAssistantUtils.js";

const { adaptationSize } = useDeviceAdapter();
const communicationStore = useCommunicationStore();
const pageUrlStore = usePageUrlStore();
const aiStore = useAiStore();

const paddingTop = ref(0);

// 长按删除相关
const sessionPressTimer = ref(null);
const showDeleteDialog = ref(false);
const deletingFunc = ref(null);
const sessionStartX = ref(0);
const sessionStartY = ref(0);
const sessionIsMoving = ref(false);

// 会话模式历史列表（从 chatSummary_${userId}_session 读取）
const sessionHistoryList = ref([]);
const agentsList = ref([]);

// 从会话摘要读取历史列表
const functions = computed(() => sessionHistoryList.value);

// 读取会话摘要
async function loadSummary(modeName) {
  linkxLog('aiAssistant/ChatHistorys.vue', 'loadSummary', "开始调用");
  if (!aiStore.userid) return {};
  try {
    const summaryKey = getSummaryKey(aiStore.userid, modeName);
    const data = await window.WeSpaceSDK.getStorage(summaryKey);
    return data ? JSON.parse(decodeURIComponent(escape(data))) : {};
  } catch (error) {
    linkxLog('aiAssistant/ChatHistorys.vue', 'loadSummary', "调用异常");
    return {};
  }
}

// 构建历史会话列表
function buildSessionHistoryList(summaryMap = {}) {
  linkxLog('aiAssistant/ChatHistorys.vue', 'buildSessionHistoryList', "开始调用");
  const deepseekIndex = getDeepSeekAgentIndex(agentsList.value);
  const list = Object.entries(summaryMap || {}).map(([sessionId, summary]) => {
    const agentIndex = summary?.agentIndex ?? deepseekIndex;
    const agent = agentsList.value.find((item) => item.index === agentIndex);
    const rawName = summary?.agentName || agent?.name || "智能助手";
    const displayName =
        rawName?.toLowerCase?.() === "deepseek" ? "智能助手" : rawName;
    return {
      sessionId,
      agentIndex,
      name: rawName,
      displayName,
      picUrl: summary?.agentPicUrl || agent?.picUrl || null,
      description: summary?.agentDesc || agent?.desc,
      lastMessage: summary?.lastMessage,
      lastAiMessage: summary?.lastAiMessage,
      lastUserMessage: summary?.lastUserMessage,
      lastViewedAiTime: summary?.lastViewedAiTime,
      lastMsgTime:
          summary?.lastMessage?.time || summary?.lastMsgTime || summary?.time,
    };
  });
  linkxLog('aiAssistant/ChatHistorys.vue', 'buildSessionHistoryList', "调用结束");
  // 按最后消息时间倒序
  return list.sort((a, b) => (b.lastMsgTime || 0) - (a.lastMsgTime || 0));
}

// 页面加载时刷新历史会话列表
async function refreshFunctionsList() { 
  try {
    linkxLog('aiAssistant/ChatHistorys.vue', 'refreshFunctionsList', "getUserInfo开始调用");
    await aiStore.getUserInfo();
    linkxLog('aiAssistant/ChatHistorys.vue', 'refreshFunctionsList', "getUserInfo调用结束");
    linkxLog('aiAssistant/ChatHistorys.vue', 'refreshFunctionsList', "getAgentsList开始调用");
    agentsList.value = await aiStore.getAgentsList();
    linkxLog('aiAssistant/ChatHistorys.vue', 'refreshFunctionsList', "getAgentsList调用结束");
    linkxLog('aiAssistant/ChatHistorys.vue', 'refreshFunctionsList', "loadSummary开始调用");
    const summaryMap = await loadSummary("session");
    linkxLog('aiAssistant/ChatHistorys.vue', 'refreshFunctionsList', "loadSummary调用结束");
    sessionHistoryList.value = buildSessionHistoryList(summaryMap);
    linkxLog('aiAssistant/ChatHistorys.vue', 'refreshFunctionsList', "buildSessionHistoryList调用结束");
    console.log("ChatHistorys 加载历史会话列表", sessionHistoryList.value?.length);
  } catch (error) {
    console.error('刷新历史会话列表失败:', error);
  }
}

function handleBack() {
  communicationStore.close();
}

function handleFunction(func) {
  // 通过存储传递选中的会话信息（跨页面通信）
  linkxLog('aiAssistant/ChatHistorys.vue', 'handleFunction', "开始调用");
  if (aiStore.userid) {
    const selectedSessionKey = `selectedSession_${aiStore.userid}`;
    window.WeSpaceSDK.setStorage(
        selectedSessionKey,
        unescape(encodeURIComponent(JSON.stringify({
          sessionId: func?.sessionId,
          agentIndex: func?.agentIndex,
        })))
    );
    console.log("已选中会话并存储", func);
  }
  // 返回上一页
  linkxLog('aiAssistant/ChatHistorys.vue', 'handleFunction', "close开始调用");
  communicationStore.close();
  linkxLog('aiAssistant/ChatHistorys.vue', 'handleFunction', "close调用结束");
}

function handleSessionTouchStart(e, func) {
  deletingFunc.value = func;
  sessionStartX.value = e.touches[0].clientX;
  sessionStartY.value = e.touches[0].clientY;
  sessionIsMoving.value = false;
  clearTimeout(sessionPressTimer.value);
  sessionPressTimer.value = setTimeout(() => {
    if (!sessionIsMoving.value) {
      showDeleteDialog.value = true;
    }
  }, 500);
}

function handleSessionTouchEnd() {
  clearTimeout(sessionPressTimer.value);
}

function handleSessionTouchMove(e) {
  const moveX = Math.abs(e.touches[0].clientX - sessionStartX.value);
  const moveY = Math.abs(e.touches[0].clientY - sessionStartY.value);
  if (moveX > 5 || moveY > 5) {
    sessionIsMoving.value = true;
    clearTimeout(sessionPressTimer.value);
  }
}

function confirmDeleteSession() {
  showDeleteDialog.value = false;
  if (deletingFunc.value) {
    deleteSession(deletingFunc.value);
  }
  deletingFunc.value = null;
}

// 删除会话（按会话ID删除摘要和聊天历史）
async function deleteSession(func) {
  linkxLog('aiAssistant/ChatHistorys.vue', 'deleteSession', "开始调用");
  const sessionId = func?.sessionId;
  const agentIndex = func?.agentIndex;
  if (!sessionId || !aiStore.userid) return;

  const keyBase = `chatHistory_${aiStore.userid}`;
  const deepseekIndex = getDeepSeekAgentIndex(agentsList.value);
  const agentKey = agentIndex ?? deepseekIndex ?? "deepseek";
  const chatKey = `${keyBase}_${sessionId}_${agentKey}`;

  try {
    // 1) 删除该会话的对话缓存
    await window.WeSpaceSDK.setStorage(
        chatKey,
        unescape(encodeURIComponent(JSON.stringify(null)))
    );

    // 2) 删除会话摘要中的对应项
    const summaryKey = getSummaryKey(aiStore.userid, "session");
    const originSummary = await window.WeSpaceSDK.getStorage(summaryKey);
    const summaryMap = originSummary
        ? JSON.parse(decodeURIComponent(escape(originSummary)))
        : {};

    if (summaryMap[sessionId]) {
      delete summaryMap[sessionId];
      await window.WeSpaceSDK.setStorage(
          summaryKey,
          unescape(encodeURIComponent(JSON.stringify(summaryMap)))
      );
      console.log("已删除会话摘要", sessionId);
    }
    // 3) 刷新列表
    linkxLog('aiAssistant/ChatHistorys.vue', 'deleteSession', "refreshFunctionsList开始调用");
    await refreshFunctionsList();
    linkxLog('aiAssistant/ChatHistorys.vue', 'deleteSession', "refreshFunctionsList刷新列表调用结束");
  } catch (error) {
    console.error("删除会话失败", error);
  }
}

// 辅助函数
function stripHtml(val) {
  return stripHtmlUtil(val);
}

function getLastMessageText(func) {
  linkxLog('aiAssistant/ChatHistorys.vue', 'getLastMessageText', "开始调用");
  const summaryUserMsg = func?.lastUserMessage?.content;
  if (summaryUserMsg) return stripHtml(summaryUserMsg);

  const summaryMsg =
    func?.lastMessage?.content || func?.lastMsg || func?.lastChat;
  if (summaryMsg) return stripHtml(summaryMsg);
  linkxLog('aiAssistant/ChatHistorys.vue', 'getLastMessageText', "调用结束");
  return func?.description || "暂无聊天记录";
}

function formatLastTime(func) {
  const timeSource =
    func?.lastMessage?.time ||
    func?.lastMessageTime ||
    func?.lastMsgTime ||
    func?.updateTime ||
    func?.createTime ||
    func?.createdAt;
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

function getStatusInfo(func) {
  const aiMsg = func?.lastAiMessage || (func?.lastMessage?.type === "ai" ? func.lastMessage : null);

  if (!aiMsg || aiMsg.type !== "ai") {
    return { label: "", type: "" };
  }

  const isStreaming =
    aiMsg.isStreaming === undefined ? false : aiMsg.isStreaming;
  if (isStreaming === true) {
    return { label: "任务进行中", type: "processing" };
  }

  return { label: "", type: "" };
}

onMounted(async () => {
  await pageUrlStore.initPageUrl();
  paddingTop.value = await communicationStore.fetchStatusBarHeight();
  // 刷新历史会话列表
  await refreshFunctionsList();
});
</script>

<style scoped lang="scss">
.chat-historys-page {
  overflow: hidden;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: linear-gradient(180deg,
      rgba(204, 220, 249, 1) 0%,
      rgb(247, 247, 247) 100%);
}

.top-nav-bar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2vw 2.5vh 1.5vw;
  flex-shrink: 0;

  .title {
    color: rgba(3, 8, 26, 1);
    font-size: 18px;
    height: 44px;
    line-height: 44px;
  }

  /* 平板适配 */
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
}

.history-list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
  -webkit-overflow-scrolling: touch;
}

.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 80px 0;

  .empty-text {
    font-size: 14px;
    color: rgba(134, 139, 152, 1);
  }
}

.session-func-item {
  background: rgba(255, 255, 255, 1);
  border-radius: 12px;
  padding: 12px;
  margin-bottom: 12px;

  .session-func-first-row {
    margin-bottom: 8px;

    .session-func-lastmsg {
      font-size: 14px;
      color: rgba(3, 8, 26, 1);
      display: -webkit-box;
      -webkit-line-clamp: 1;
      -webkit-box-orient: vertical;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }

  .session-func-second-row {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .session-func-meta {
      display: flex;
      align-items: center;
      flex: 1;
      overflow: hidden;

      .func-icon {
        width: 24px;
        height: 24px;
        border-radius: 8px;
        margin-right: 8px;
        flex-shrink: 0;
      }

      .session-func-name {
        font-size: 12px;
        color: rgba(134, 139, 152, 1);
        margin-right: 8px;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }

      .session-func-time {
        font-size: 12px;
        color: rgba(134, 139, 152, 1);
        flex-shrink: 0;
      }
    }

    .status-tag {
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 2px 8px;
      border-radius: 4px;
      font-size: 12px;
      flex-shrink: 0;
      margin-left: 8px;

      .status-tag-icon {
        width: 12px;
        height: 12px;
      }

      &.processing {
        background: rgba(59, 114, 255, 1);
        color: #ffffff;
      }

      &.completed {
        background: rgba(9, 172, 66, 0.1);
        color: rgba(9, 172, 66, 1);
      }
    }
  }
}

.delete-dialog {
  :deep(.van-dialog__content) {
    padding: 16px;
  }
}
</style>
