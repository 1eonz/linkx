<template>
  <view class="ai-assistant-page">
    <!-- 顶部区域 -->
    <view :style="{ width: '100%', height: paddingTop + 'px' }"></view>
    <view class="top-nav-bar">
      <van-icon
        name="arrow-left"
        :size="adaptationSize.iconSize"
        color="#333"
        @click="handleBack"
      />
      <text class="title">{{ titleText }}</text>
      <view class="nav-actions">
        <text class="history-text" v-if="isSessionMode" @click="toHistory">历史</text>
      </view>
    </view>

    <!-- 对话历史区域 -->
    <view class="chat-history">
    <view
      v-if="messages.length === 0"
        class="init-robot"
      >
        <text class="ai-title">👏你好，我是你的AI助手</text>
        <text class="ai-desc1">我可以帮你搜索、答疑、写作，请把你的任务</text>
        <text class="ai-desc2">交给我吧～</text>
      </view>
      <template v-else>
        <view
          class="message"
          v-for="(msg, index) in messages"
          :key="index"
          :class="msg.type === 'user' ? 'user-message' : 'ai-message'"
        >
          <img
            v-if="msg?.type === 'user'"
            :width="adaptationSize.radioSize"
            :height="adaptationSize.radioSize"
            :src="userAvatar || userAvatarDefault"
            style="border-radius: 50%"
          />
          <!-- AI 消息头像：优先使用消息对应的智能体头像，其次使用当前智能体头像，最后使用默认 AI 头像 -->
          <AgentAuthImg
            v-else-if="getAgentAvatarByMessage(msg)"
            :width="adaptationSize.radioSize"
            :height="adaptationSize.radioSize"
            :pic-url="getAgentAvatarByMessage(msg)"
            style="border-radius: 50%"
          />
          <AgentAuthImg
            v-else-if="currentAgentIndex != null && currentAgentAvatar"
            :width="adaptationSize.radioSize"
            :height="adaptationSize.radioSize"
            :pic-url="currentAgentAvatar"
            style="border-radius: 50%"
          />
          <img
            v-else
            :width="adaptationSize.radioSize"
            :height="adaptationSize.radioSize"
            :src="aiAvatarDefault"
            style="border-radius: 50%"
          />
          <view
            class="message-content"
            @touchstart="(e) => handleTouchStart(e, msg.content)"
            @touchend="handleTouchEnd"
            @touchmove="handleTouchMove"
          >
            <text v-if="msg.type === 'user'" class="user-text">{{ msg.content }}</text>
            <view v-else v-html="msg.content"></view>
          </view>

          <!-- 继续按钮，只在暂停状态且是最新的AI消息时显示 -->
          <view v-if="msg.type === 'ai' && msg === messages[messages.length - 1] && isPaused" class="resume-btn-wrapper">
            <view class="resume-btn" @click="handleResumeStream">
              <text class="resume-text">继续生成</text>
            </view>
          </view>

          <view
            v-show="showMenu"
            class="action-menu"
            :style="{ top: menuTop + 'px', left: menuLeft + 'px', zIndex: 1 }"
            @click.stop="onMenuClick"
          >
            <view @click="handleAction">复制</view>
          </view>
        </view>
      </template>

      <!-- 加载状态 -->
      <view v-show="isLoading && !isReceiving" class="message ai-message">
        <img
          :width="adaptationSize.radioSize"
          :height="adaptationSize.radioSize"
          src="@/static/ai/ai_loading.png"
          style="border-radius: 50%"
        />
        <view class="message-content">
          <text>正在查询...</text>
        </view>
      </view>
    </view>

    <!-- 功能按钮区 -->
    <view class="function-btns" >
      <view class="agent-supermarket-btn" @click="toAgent()">
        <img
          :width="36"
          :height="36"
          src="@/assets/svg/agent_supermarket.svg"
        />
      </view>
      <view
        v-if="!hideFunctions"
        class="func-btn"
        :class="{ 'current-agent': currentAgentIndex === func.index }"
        v-for="func in functions"
        :key="func.index"
        @click="handleFunction(func)"
      >
      <AuthImg :picUrl="func.picUrl" class="func-icon"></AuthImg>
        {{ func.name }}
      </view>
    </view>

    <!-- 输入区域 -->
    <view class="input-area">
      <input
        ref="inputRef"
        class="chat-input"
        v-model="inputValue"
        placeholder="发消息..."
        :maxlength="9999"
        @focus="handleInputFocus"
        @confirm="sendMessage"
        @click="handleClick"
      />

      <!-- 暂停按钮 -->
      <img v-if="(isStreaming || isReceiving || isDisplaying) && !isPaused"
        class="send-btn clickable" src="@/static/ai/pause.png" alt="暂停" 
        :width="adaptationSize.groupIconWidth"
        :height="adaptationSize.groupIconHeight"
        @click="handlePauseStream"/>

      <!-- 发送按钮 -->
      <img v-else 
         class="send-btn clickable" src="@/static/ai/send_app.png"
        :class="[{ 'send-btn-disabled': isLoading || !inputValue.trim() }]"
        :width="adaptationSize.groupIconWidth"
        :height="adaptationSize.groupIconHeight"
        @click="sendMessage"
      />
    </view>

    <!-- 智能体超市底部弹窗 -->
    <AgentPopup ref="agentPopupRef" />
  </view>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, nextTick, computed } from "vue";
import { useCommunicationStore } from "@/stores/communication.js";
import { usePageUrlStore } from "@/stores/pageUrl.js";
import AuthImg from "@/components/AuthImg/index.vue";
import AgentAuthImg from "@/components/AgentAuthImg/index.vue";
import { getGlobalsConfigByKey } from "@/common/utils";
import { useDeviceAdapter } from "@/stores/useDeviceAdapter.js";
import userAvatarDefault from "@/static/ai/user_avatar.png";
import aiAvatarDefault from "@/static/ai/ai_avatar.png";
import aiLoading from "@/static/ai/ai_loading.png";
import avatarCache from "@/utils/avatarCache.js";
import AgentPopup from './AgentPopup.vue';
import { linkxLog } from "@/utils/aiAssistantUtils.js";
// 使用设备适配
const { adaptationSize } = useDeviceAdapter();

const communicationStore = useCommunicationStore();
const pageUrlStore = usePageUrlStore();

const inputRef = ref(null);
const agentPopupRef = ref(null);

// 接收父组件传递的props
const props = defineProps({
  messages: {
    type: Array,
    required: true,
    default: () => [],
  },
  functions: {
    type: Array,
    required: true,
    default: () => [],
  },
  isLoading: {
    type: Boolean,
    required: true,
    default: false,
  },
  isReceiving: {
    type: Boolean,
    required: true,
    default: false,
  },
  isPaused: {
    type: Boolean,
    required: true,
    default: false,
  },
  isStreaming: {
    type: Boolean,
    required: true,
    default: false,
  },
  isDisplaying: {
    type: Boolean,
    required: true,
    default: false,
  },
  currentAgentIndex: {
    type: [Number, null],
    required: true,
    default: null,
  },
  userAvatar: {
    type: [String, null],
    required: true,
    default: null,
  },
  hideFunctions: {
    type: Boolean,
    required: false,
    default: false,
  },
  isSessionMode: {
    type: Boolean,
    required: false,
    default: false,
  },
  showSessionFunctions: {
    type: Boolean,
    required: false,
    default: false,
  },
  // 会话模式下：是否处于“聊天态”（URL 中 chat=1）
  // 由父组件根据路由计算并传入，避免子组件用状态推断返回行为
  isSessionChatRoute: {
    type: Boolean,
    required: false,
    default: false,
  },
  titleText: {
    type: String,
    required: false,
    default: "公安AI助手",
  },
  currentSessionId: {
    type: [String, null],
    required: false,
    default: null,
  },
  agents: {
    type: Array,
    required: false,
    default: () => [],
  },
});
// 发送父组件定义的事件
const emit = defineEmits([
  "sendMessage",
  "handleFunction",
  "copyText",
  "deleteSession",
]);

const paddingTop = ref(0);

// 聊天相关状态
const inputValue = ref("");
// 当前智能体头像（会话模式 + 已选择智能体时生效）
const currentAgentAvatar = computed(() => {
if (!props.isSessionMode || props.currentAgentIndex == null) return "";
  // 从智能体列表中查找当前智能体，而不是从 functions（历史会话列表）中查找
  const agentsList = Array.isArray(props.agents) ? props.agents : [];
  const currentAgent = agentsList.find((agent) => agent.index === props.currentAgentIndex);
  return currentAgent?.picUrl || "";
});

// 根据消息的 agentIndex 获取对应的智能体头像
function getAgentAvatarByMessage(msg) {
  linkxLog('aiAssistant/aiAssistantPhone.vue', 'getAgentAvatarByMessage', "开始调用");
  if (msg?.type !== "ai" || !msg?.agentIndex) return "";
  const agentsList = Array.isArray(props.agents) ? props.agents : [];
  // 类型转换：确保比较时类型一致（agent.index 可能是字符串或数字）
  const msgAgentIndex = String(msg.agentIndex);
  const agent = agentsList.find((a) => String(a.index) === msgAgentIndex);
  const avatar = agent?.picUrl || "";
  console.log("getAgentAvatarByMessage:", { 
    msgAgentIndex, 
    msgAgentIndexType: typeof msg.agentIndex,
    foundAgent: agent?.name, 
    foundAgentIndex: agent?.index,
    foundAgentIndexType: typeof agent?.index,
    avatar,
    agentsCount: agentsList.length,
    agentsListSample: agentsList.slice(0, 3).map(a => ({ index: a.index, name: a.name, indexType: typeof a.index }))
  });
  linkxLog('aiAssistant/aiAssistantPhone.vue', 'getAgentAvatarByMessage', "调用结束");
  return avatar;
}

// 跳转
const handleBack = async () => {
  linkxLog('aiAssistant/aiAssistantPhone.vue', 'handleBack', "开始调用");
  await communicationStore.setStorage(
    "aiBaseUrl",
    unescape(encodeURIComponent(JSON.stringify("")))
  );
  linkxLog('aiAssistant/aiAssistantPhone.vue', 'handleBack', "close开始调用");
  await communicationStore.close();
  linkxLog('aiAssistant/aiAssistantPhone.vue', 'handleBack', "close调用结束");
};
async function toAgent() {
  linkxLog('aiAssistant/aiAssistantPhone.vue', 'toAgent', "开始调用");
  if (agentPopupRef.value) {
    await agentPopupRef.value.show({
      sessionMode: props.isSessionMode
    });
  }
  await getAiBaseUrl();
  linkxLog('aiAssistant/aiAssistantPhone.vue', 'toAgent', "调用结束");
}
async function toHistory() {
  linkxLog('aiAssistant/aiAssistantPhone.vue', 'toHistory', "开始调用");
  const url = pageUrlStore.getFullPageUrl('/pages/aiAssistant/chatHistorys');
  await communicationStore.openUrl(url, null, 'noTitleStyle');
  linkxLog('aiAssistant/aiAssistantPhone.vue', 'toHistory', "调用结束");
}
// 获取最新的aiBaseUrl
async function getAiBaseUrl() {
  try {
    linkxLog('aiAssistant/aiAssistantPhone.vue', 'getAiBaseUrl', "开始调用");
    const aiBaseUrl = (await getGlobalsConfigByKey("ai"))?.replace(/\/$/, "");
    await communicationStore.setStorage(
      "aiBaseUrl",
      unescape(encodeURIComponent(JSON.stringify(aiBaseUrl)))
    );
    linkxLog('aiAssistant/aiAssistantPhone.vue', 'getAiBaseUrl', "调用结束");
  } catch (e) {
    linkxLog('aiAssistant/aiAssistantPhone.vue', 'getAiBaseUrl', "调用异常");
    console.log(e);
  }
}

// 选择智能体
function handleFunction(func) {
  emit("handleFunction", func);
}
// 发送消息
function sendMessage() {
  if (inputValue.value) {
    emit("sendMessage", inputValue.value);
    inputValue.value = "";
  }
}

// 监听暂停状态变化，滚动到底部
watch(
  () => props.isPaused,
  async (newValue) => newValue && (await scrollToBottom())
);

// 暂停流式输出
function handlePauseStream() {
  emit("pauseStream");
}

// 继续流式输出
function handleResumeStream() {
  emit("resumeStream");
}

function handleClick() {
  setTimeout(() => {
    if (inputRef.value.$el) {
      inputRef.value.$el.focus();
    }
  }, 100);
}
  function handleInputFocus() {
    setTimeout(() => {
      scrollToBottom();
    }, 500);
  }
// 头像预加载
const preloadAvatars = async () => {
  const avatarUrls = [userAvatarDefault, aiAvatarDefault, aiLoading];

  // 如果用户有自定义头像，也预加载
  if (props.userAvatar) {
    avatarUrls.push(props.userAvatar);
  }

  // 使用缓存机制预加载
  await avatarCache.preloadAvatars(avatarUrls);
};

// 监听用户头像变化，重新预加载
watch(
  () => props.userAvatar,
  async (newAvatar) => {
    if (newAvatar && !avatarCache.isCached(newAvatar)) {
      await avatarCache.preloadAvatar(newAvatar);
    }
  },
  { immediate: false }
);

// 长按复制相关
onMounted(async () => {
  await pageUrlStore.initPageUrl();
  paddingTop.value = await communicationStore.fetchStatusBarHeight();
  // 预加载头像
  preloadAvatars();
  // 点击其他区域关闭菜单
  document.addEventListener("click", hideMenu);
});
onBeforeUnmount(() => {
  document.removeEventListener("click", hideMenu);
});
const onMenuClick = (e) => {
  e.stopPropagation();
};
const pressTimer = ref(null);
const showMenu = ref(false);
const menuTop = ref(0);
const menuLeft = ref(0);
const startX = ref(0);
const startY = ref(0);
const isMoving = ref(false);
const transMsg = ref(null);

function handleTouchStart(e, msg) {
  console.log("触摸开始-----------");
  transMsg.value = msg;
  startX.value = e.touches[0].clientX;
  startY.value = e.touches[0].clientY;
  isMoving.value = false;

  // 500ms后如果没有松开或移动，显示菜单
  pressTimer.value = setTimeout(() => {
    console.log("触摸超过500ms----------");
    if (!isMoving.value) {
      console.log("显示复制按钮-----------");
      showMenu.value = true;
      // 计算菜单位置
      menuTop.value = e.touches[0].clientY + 10;
      menuLeft.value = e.touches[0].clientX - 30;
    }
  }, 500);
}

function handleTouchEnd() {
  console.log("触摸结束-----------");
  clearTimeout(pressTimer.value);
}

function handleTouchMove(e) {
  const moveX = Math.abs(e.touches[0].clientX - startX.value);
  const moveY = Math.abs(e.touches[0].clientY - startY.value);

  // 判断是否移动超过阈值
  if (moveX > 5 || moveY > 5) {
    isMoving.value = true;
    showMenu.value = false;
    clearTimeout(pressTimer.value);
  }
}

function handleAction() {
  console.log("点击复制-----------");
  showMenu.value = false;
  emit("copyText", transMsg.value);
}

function hideMenu(e) {
  // 检查点击是否发生在消息内容区域
  const contentEl = e.target.closest(".message-content");
  if (!contentEl && showMenu.value) {
    console.log("隐藏复制按钮-----------");
    showMenu.value = false;
  }
}

// 滚动到底部
async function scrollToBottom() {
  await nextTick();
    const chatContainer = document.querySelector('.chat-history');
    console.log('chatContainer', chatContainer.scrollHeight);
  if (chatContainer) {
      chatContainer.scrollTo({
        top: chatContainer.scrollHeight,
        behavior: 'smooth',
      });
  }
}
// 顶部功能列表展示的辅助信息（已移至 ChatHistorys 页面）
// 以下函数已不再使用，保留注释以备参考
// function stripHtml(val) {
//   if (!val || typeof val !== "string") return "";
//   return val.replace(/<[^>]*>/g, "").trim();
// }
// 
// function getLastMessageText(func) { ... }
// function formatLastTime(func) { ... }
// function getStatusInfo(func) { ... }

function getLastMessageText(func) {
  // 1) 优先：会话摘要中记录的“最后一条用户消息”（推荐展示的提问文案）
  const summaryUserMsg = func?.lastUserMessage?.content;
  if (summaryUserMsg) return stripHtml(summaryUserMsg);

  // 2) 兼容旧数据：使用摘要/缓存中的最后一条消息
  const summaryMsg =
    func?.lastMessage?.content || func?.lastMsg || func?.lastChat;
  if (summaryMsg) return stripHtml(summaryMsg);

  // 3) 会话模式中，如果当前智能体正在聊天，临时展示当前会话内用户的最后一条提问
  if (
    props.isSessionMode &&
    props.currentSessionId === func?.sessionId &&
    Array.isArray(props.messages)
  ) {
    const lastUserMsg = [...props.messages]
      .reverse()
      .find((item) => item?.type === "user" && item?.content);
    if (lastUserMsg?.content) return stripHtml(lastUserMsg.content);
  }

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
  console.log(func, "funcfunc");
  // 1) 会话内实时消息优先
  let aiMsg = null;
  if (
    props.isSessionMode &&
    props.currentSessionId === func?.sessionId &&
    Array.isArray(props.messages)
  ) {
    aiMsg = [...props.messages].reverse().find((item) => item?.type === "ai");
  }

  // 2) 摘要缓存：优先 lastAiMessage，兼容旧缓存只写了 lastMessage 的情况
  if (!aiMsg) {
    const summaryAi =
      func?.lastAiMessage ||
      (func?.lastMessage?.type === "ai" ? func.lastMessage : null);
    aiMsg = summaryAi;
  }
  console.log(aiMsg, "aiMsg", func?.lastAiMessage, func?.lastMessage);

  // 3) 没有 AI 消息则不显示标签
  if (!aiMsg || aiMsg.type !== "ai") {
    return { label: "", type: "" };
  }

  // 兼容旧缓存缺少 isStreaming，默认已完成
  const isStreaming =
    aiMsg.isStreaming === undefined ? false : aiMsg.isStreaming;
  if (isStreaming === true) {
    return { label: "任务进行中", type: "processing" };
  }

  // 已完成：只有“未查看”才显示标签；查看后隐藏
  // - lastViewedAiTime: 保存到摘要中的“用户最后一次查看到的AI完成消息时间”
  // - aiMsg.time / aiMsg.timestamp: 本条AI消息的时间戳（摘要用time，实时消息用timestamp）
  // const viewedTime = func?.lastViewedAiTime || 0;
  // const aiTime = aiMsg?.time || aiMsg?.timestamp || 0;
  if (isStreaming === false) {
    // 如果拿不到时间戳，为保持兼容，仍然显示“任务已完成”
    // if (!aiTime) return { label: "任务已完成", type: "completed" };
    // if (aiTime > viewedTime) return { label: "任务已完成", type: "completed" };
    return { label: "", type: "" };
  }
  return { label: "", type: "" };
}
</script>

<style scoped lang="scss">
/* 基础样式 */
.ai-assistant-page {
  overflow: hidden;
  height: 100vh;
  /* padding: 0 5vw 2.5vh 5vw; */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(
    180deg,
    rgba(204, 220, 249, 1) 0%,
    rgba(255, 255, 255, 1) 35%
  );
}

/* 头部样式 */
.top-nav-bar {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 2vw 2.5vh 1.5vw;
  .title {
    color: rgba(3, 8, 26, 1);
    font-size: 18px;
    height: 44px;
    line-height: 44px;
  }
  .nav-actions {
    display: flex;
    align-items: center;
    padding: 0 2vw;
    gap: 12px;
    .history-text {
      color: rgba(59, 114, 255, 1);
      font-size: 14px;
      cursor: pointer;
    }
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

/* 空默认显示 */
.init-robot {
  display: flex;
  flex-direction: column;
  align-items: center;
  flex: 1;
  padding-top: 16.5vh;

  .ai-title {
    font-size: clamp(14px, 4.8vw, 18px);
    font-weight: 700;
    color: rgba(51, 51, 51, 1);
  }
  .ai-desc1 {
    margin-top: 2.7vh;
    font-size: clamp(12px, 4.2vw, 16px);
    color: rgba(90, 99, 131, 1);
  }
  .ai-desc2 {
    font-size: clamp(12px, 4.2vw, 16px);
    color: rgba(90, 99, 131, 1);
  }
  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .ai-title,
    .ai-desc1,
    .ai-desc2 {
      font-size: 0.6rem;
    }
  }
}

/* 功能按钮样式 */
.function-btns {
  width: 100%;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  flex-wrap: wrap;
  /* 限制两行 */
    // max-height: calc(10vh + 3vh); /* 两行按钮的高度 + 一行的间距 */
    max-height: 110px; /* 两行按钮的高度 + 一行的间距 */
    overflow: auto; /* 超出隐藏 */
  padding: 0 2vw;
  margin-bottom: 1.5vh;
  .agent-supermarket-btn {
    border-radius: 8px;
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 1vh;
    cursor: pointer;
    flex-shrink: 0;
  }
  .func-btn {
    font-size: 14px;
    // min-width: 21vw;
    // height: 5vh;
    border-radius: 1vw;
    background: rgba(255, 255, 255, 1);
    display: flex;
    justify-content: center;
    align-items: center;
    margin-right: 1.5vh;
    padding: 10px 20px;
    border: 1px solid rgba(239, 239, 239, 1);
  }
  .func-icon {
    width: 20px;
    height: 20px;
    border-radius: 2px;
    margin-right: 4px;
  }
  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .func-btn {
      font-size: 0.5rem;
    }
    .func-icon {
      width: 32px;
      height: 32px;
    }
    img {
      width: 32px !important;
      height: 32px !important;
    }
  }
  @media screen and (min-height: 2001px) {
    .func-icon {
      width: 42px;
      height: 42px;
    }
    img {
      width: 42px !important;
      height: 42px !important;
    }
  }
  .current-agent {
    border: 1px solid rgba(30, 82, 242, 1);
  }
}

/* 聊天历史样式 */
.chat-history {
  width: 100%;
  flex: 1;
  overflow-y: auto;
  align-self: center;
  // display: flex;
  // flex-direction: column;
  padding: 0 2vw 2.5vh 2vw;
  .message {
    display: flex;
    flex-wrap: wrap;
    margin-bottom: 2vh;
    max-width: 100%;
  }
  
  .resume-btn-wrapper {
    flex-basis: 100%;
    margin-left: 46px;
    max-width: 80%;
  }
  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
  }
  .user-message {
    flex-direction: row-reverse; /* 反转用户消息的布局方向 */
  }

  .user-message .message-content {
    margin-right: 10px;
    background-color: rgb(73, 98, 255);
    color: #fff;
    border-radius: 12px 0px 12px 12px;
  }

  .ai-message .message-content {
    margin-left: 10px;
    border-radius: 0px 12px 12px 12px;
    background: #fff;
    border: 1px solid #f0f0f0;
  }

  .message-content {
    padding: 10px 16px;
    max-width: 80%;
    word-wrap: break-word;
    overflow-x: hidden;
  }

  /* 穿透uni-app的样式隔离 */
  .message-content :deep(pre),
  .message-content :deep(code) {
    white-space: pre-wrap !important;
  }

  .action-menu {
    position: fixed;
    z-index: 1000;
    background-color: #fff;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    padding: 8px 12px;
    font-size: 14px;
  }
  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .message-content {
      font-size: 0.6rem;
    }
  }
}

/* 输入区域样式 */
.input-area {
  width: 100%;
  display: flex;
  align-items: center;
  position: relative;
  height: 86px;
  padding: 0 2vw 14px 2vw;
  .chat-input {
    flex: 1;
    height: 56px;
    border-radius: 2vw;
    background-color: rgba(245, 245, 245, 1);
    padding-left: 2.6vw;
  }

  .send-btn {
    margin-left: 2.6vw;
  }

  .send-btn-disabled {
    opacity: 0.5 !important; /* 禁用时半透明 */
  }
}


.resume-btn-wrapper {
  width: 100%;
  display: flex;
  justify-content: flex-end;
  margin-top: 10px;
  margin-left: 0;
  padding-right: 0;
}

.resume-btn {
  width: 70px;
  height: 30px;
  background-color: #ffffff;
  border: 1px solid #dddddd;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.resume-text {
  font-size: 13px;
  color: #333333;
}

.user-text {
  word-wrap: break-word;
  overflow-wrap: break-word;
}
/* 平板适配 */
@media screen and (min-height: 1200px) {
  .input-area {
    height: 86px;
  }
  :deep(.input-placeholder) {
    font-size: 0.6rem;
    height: 2vh;
    line-height: 2vh;
  }
  :deep(.uni-input-input) {
    font-size: 0.6rem;
  }
}
</style>
