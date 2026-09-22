<template>
  <!-- <view style="height:200px;border: 1px solid red ;" @click="delAgentPhsh">11111111</view> -->
  <component
      :is="CurrentComponent"
      :messages="messages"
      :functions="functions"
      :isLoading="isLoading"
      :isReceiving="isReceiving"
      :isPaused="streamState.isPaused"
      :isStreaming="streamState.isStreaming"
      :isDisplaying="!!(streamState.pendingContent || streamState.timer)"
      :currentAgentIndex="currentAgentIndex"
      :currentSessionId="currentSessionId"
      :userAvatar="userInfo.thumbAvatar"
      :hideFunctions="hideFunctions"
      :isSessionMode="sessionMode"
      :showSessionFunctions="showSessionFunctions"
      :isSessionChatRoute="isSessionChatRoute"
      :titleText="titleText"
      :agents="agents"
      @handleFunction="handleFunction"
      @copyText="copyText"
      @sendMessage="sendMessage"
      @pauseStream="pauseStream"
      @resumeStream="resumeStream"
      @deleteSession="deleteSession"
      @back="handleBackFromChild"
  />
</template>

<script setup>
import { marked } from "marked";
import { showToast } from "vant";
import { ref, unref, onMounted, nextTick, onUnmounted, computed, shallowRef, watch } from "vue";

import aiAssistantPhone from "./aiAssistantPhone.vue";
import aiAssistantWeb from './aiAssistantWeb.vue';

import { aiApi } from "@/common/api/index.js";
import { useSocketManage } from '@/common/network/ws.js';
import { mockAiApi } from '@/mock/mockAiApi';
import { useAiStore } from "@/stores/ai.js";
import { useCommunicationStore } from "@/stores/communication.js";
import avatarCache from "@/utils/avatarCache.js";
import { sendNotification } from '@/utils/glassUtils.js';
import { useRoute, useRouter } from "vue-router";
import {
  createSessionId,
  getDeepSeekAgentIndex,
  getSessionSelectedAgentKey,
  getSummaryKey,
  getChatKey,
  stripHtml,
} from "@/utils/aiAssistantUtils.js";

const communicationStore = useCommunicationStore();
const aiStore = useAiStore();
const route = useRoute();
const router = useRouter();
const sessionMode = ref(false); // 是否会话模式
const showSessionFunctions = ref(false); // 会话模式下功能列表是否展示（历史会话列表）
const sessionHistoryList = ref([]); // 会话模式下的历史会话列表（按会话ID分桶）
const currentSessionId = ref(null); // 当前会话ID（uuid）
const storageKeys = []; // 记录本组件注册的存储监听，便于卸载

// 会话模式下：是否处于“聊天态”（URL 中带 chat=1）
const isSessionChatRoute = computed(() => {
  if (!sessionMode.value) return false;
  return String(route.query?.chat ?? "") === "1";
});

// 路由同步锁：避免我们自己 push/replace 时触发 watcher 做额外 restore/reset
const routeSyncLock = ref(false);
let hasInitializedRouteSync = false;

function buildSessionChatQuery({ sessionId, agentIndex }) {
  return {
    ...route.query,
    // 只在聊天态追加这些 query；默认态保持现有路由不变
    chat: "1",
    sessionId: sessionId || undefined,
    agentIndex: agentIndex || undefined,
  };
}

function buildSessionHistoryQuery() {
  // 移除 chat/sessionId/agentIndex，回到“默认态”（历史列表/空提示）
  const q = { ...route.query };
  delete q.chat;
  delete q.sessionId;
  delete q.agentIndex;
  return q;
}

async function pushSessionChatRoute({ sessionId, agentIndex }) {
  if (!sessionMode.value) return;
  if (isSessionChatRoute.value) return;
  routeSyncLock.value = true;
  try {
    await router.push({
      path: route.path,
      query: buildSessionChatQuery({ sessionId, agentIndex }),
    });
  } finally {
    // nextTick 确保 route 已更新，避免 watcher 同步时序问题
    await nextTick();
    routeSyncLock.value = false;
  }
}

async function replaceToSessionHistoryRoute() {
  if (!sessionMode.value) return;
  routeSyncLock.value = true;
  try {
    await router.replace({ path: route.path, query: buildSessionHistoryQuery() });
  } finally {
    await nextTick();
    routeSyncLock.value = false;
  }
}

async function enterChatFromRouteQuery() {
  if (!sessionMode.value) return;
  const q = route.query || {};
  const sessionId = typeof q.sessionId === "string" ? q.sessionId : null;
  const agentIndex = q.agentIndex != null ? String(q.agentIndex) : null;
  if (!sessionId) return;

  // 同步状态（仅用于浏览器前进/后退触发的路由变化）
  currentSessionId.value = sessionId;
  currentAgentIndex.value = agentIndex || getDeepSeekAgentIndex(agents.value);
  showSessionFunctions.value = false;
  await restoreConversation();
}

// 从聊天态退回默认态时，用于恢复“历史列表样式”并清理当前会话状态
async function resetToSessionHistoryState() {
  if (!sessionMode.value) return;
  resetStreamState();
  messages.value = [];
  isLoading.value = false;
  isReceiving.value = false;
  currentTaskId.value = null;
  currentAgentIndex.value = null;
  currentSessionId.value = null;

  // 清除存储的 currentAgentIndex_userid
  if (userInfo.value?.userid) {
    try {
      await window.WeSpaceSDK.setStorage(
        `currentAgentIndex_${userInfo.value.userid}`,
        unescape(encodeURIComponent(JSON.stringify(null)))
      );
      console.log("已清除 currentAgentIndex 存储");
    } catch (error) {
      console.error("清除 currentAgentIndex 存储失败:", error);
    }
  }

  // 返回列表时刷新会话历史，确保“任务已完成”在查看后能即时消失
  await loadSessionHistories();
  setShowSessionFunctions();
}

// 组件自适应
const CurrentComponent = shallowRef();
function updateComponentByScreenSize() {
  const width = window.innerWidth;
  CurrentComponent.value = aiAssistantPhone;
}

const agents = ref([]);
async function delAgentPhsh() {
  const val = {
    index: '2009187294442309634',
  };
  //val为删除的智能体
  const agent = agents.value.find((item) => item?.index === val?.index);
  if (agent?.selectedTime) {
    agent.selectedTime = null;
    if (aiStore.currentAgentIndex === val.index) {
      aiStore.setCurrentAgent(null);
    }
    functions.value = await aiStore.getCurrentFunc(agents.value);
  }
}
const messages = ref([]);
const functions = ref([]);
const currentAgentIndex = ref(null);
const isLoading = ref(false);
const isReceiving = ref(false); // 正在回复
const currentTaskId = ref(null);
const hideFunctions = ref(false); // 是否隐藏功能按钮区
const DEFAULT_TITLE = "雄安AI助手";
const userInfo = ref({
  username: "",
  userid: null,
  idCard: null,
  thumbAvatar: "",
});
const titleText = computed(() => {
  if (sessionMode.value && currentAgentIndex.value != null) {
    const agent = agents.value.find(
        (item) => item.index === currentAgentIndex.value
    );
    const name = agent?.name || DEFAULT_TITLE;
    return name?.toLowerCase?.() === "deepseek" ? DEFAULT_TITLE : name;
  }
  return DEFAULT_TITLE;
});

// 用户滚动检测
const userScrolled = ref(false);
let lastScrollTime = 0;
const SCROLL_THRESHOLD = 5; // 滚动阈值，超过这个值认为是用户手动滚动
const TIME_THRESHOLD = 100; // 时间阈值，防止频繁触发

onMounted(async () => {
  console.log("打开ai助手界面");
  updateComponentByScreenSize();
  window.addEventListener("resize", updateComponentByScreenSize);
  await getUserInfo();
  
  // 等待DOM渲染完成后添加滚动事件监听器
  await nextTick();
  const chatContainer = document.querySelector(".chat-history");
  if (chatContainer) {
    chatContainer.addEventListener("scroll", handleScroll);
    // 同时添加触摸事件监听器，以支持移动端触摸滚动
    chatContainer.addEventListener("touchstart", handleTouchStart);
    chatContainer.addEventListener("touchmove", handleTouchMove);
    chatContainer.addEventListener("touchend", handleTouchEnd);
  } else {
    console.warn("chat-history元素未找到，无法添加滚动事件监听器");
  }
  // 读取模式与基础配置
  sessionMode.value =
      route.query.sessionMode === "true" || route.query.sessionMode === "1";
  const hideFunctionsParam = route.query.hideFunctions;

  // 资源预加载
  preloadImages();

  // 智能体列表
  agents.value = await aiStore.getAgentsList();

  if (sessionMode.value) {
    // 会话模式：不使用底部快捷区，初始不选智能体
    hideFunctions.value = true;
    currentAgentIndex.value = null;
    currentSessionId.value = null;
    messages.value = [];
    resetStreamState();
    await loadSessionHistories(); // 会话模式按会话ID加载历史列表

    // 会话模式：进入页面时只显示历史消息列表，不自动恢复任何会话
    // 用户需要点击历史消息才会恢复对应的会话
    setShowSessionFunctions();

    // 如果直接以 chat=1 打开（或浏览器前进进入），同步 URL -> 状态
    // 注意：此时 userInfo/agents 已就绪，restoreConversation 才是安全的
    hasInitializedRouteSync = true;
    if (isSessionChatRoute.value) {
      await enterChatFromRouteQuery();
    }
  } else {
    // 综合模式：保持现有逻辑
    hideFunctions.value =
        hideFunctionsParam === "true" || hideFunctionsParam === true;
    functions.value = await aiStore.getCurrentFunc(agents.value);
    currentAgentIndex.value = await aiStore.cachedCurrentAgentIndex();
    await restoreConversation(); // 综合模式使用原键名
  }

  //开启websocket
  const socketManage = await useSocketManage();
  await socketManage.initWebSocket();
  //开启websocket
  // 存储变更监听（仅影响综合模式缓存）
  registerStorageListener(
      `functionsList_${userInfo.value.userid}`,
      async (val) => {
        console.log(
            "onStorageChange数据解码处理functionsList---------",
            JSON.stringify(JSON.parse(decodeURIComponent(escape(val))), null, 2)
        );
        if (!sessionMode.value) {
          functions.value = JSON.parse(decodeURIComponent(escape(val)));
        }
      }
  );
  registerStorageListener(
      `currentAgentIndex_${userInfo.value.userid}`,
      async (val) => {
        console.log(
            "onStorageChange数据解码处理currentAgentIndex---------",
            JSON.stringify(JSON.parse(decodeURIComponent(escape(val))), null, 2)
        );
        if (!sessionMode.value) {
          currentAgentIndex.value = JSON.parse(decodeURIComponent(escape(val)));
        }
      }
  );
  // 会话模式：从智能体列表页回传选中智能体
  registerStorageListener(
      getSessionSelectedAgentKey(userInfo.value?.userid),
      async (val) => {
        if (!sessionMode.value) return;
        try {
          const parsed = val ? JSON.parse(decodeURIComponent(escape(val))) : null;
          if (parsed == null) return;
          // 从智能体超市选择智能体时，创建新会话，不恢复历史会话
          currentAgentIndex.value =
              typeof parsed === "object" ? parsed?.agentIndex : parsed;
          // 使用传入的 sessionId 或创建新的 sessionId
          currentSessionId.value =
              typeof parsed === "object" ? parsed?.sessionId : createSessionId();
          // 确保进入聊天页面：隐藏历史列表，清空消息，重置状态
          showSessionFunctions.value = false;
          messages.value = [];
          isLoading.value = false;
          isReceiving.value = false;
          currentTaskId.value = null;
          resetStreamState();

          // 进入聊天态：压栈一条 chat=1 路由（新会话不恢复历史消息）
          await pushSessionChatRoute({
            sessionId: currentSessionId.value,
            agentIndex: currentAgentIndex.value,
          });
          // 清除 currentAgentIndex 缓存，因为会话模式下使用 sessionId 来管理会话
          if (userInfo.value?.userid) {
            try {
              await window.WeSpaceSDK.setStorage(
                  `currentAgentIndex_${userInfo.value.userid}`,
                  unescape(encodeURIComponent(JSON.stringify(null)))
              );
              console.log("已清除 currentAgentIndex 存储（进入新会话时）");
            } catch (error) {
              console.error("清除 currentAgentIndex 存储失败:", error);
            }
          }
          // 清除存储，避免重复处理
          if (userInfo.value?.userid) {
            try {
              await window.WeSpaceSDK.setStorage(
                  getSessionSelectedAgentKey(userInfo.value.userid),
                  unescape(encodeURIComponent(JSON.stringify(null)))
              );
              console.log("已清除 sessionSelectedAgentIndex 存储（进入新会话时）");
            } catch (error) {
              console.error("清除 sessionSelectedAgentIndex 存储失败:", error);
            }
          }
          // 不调用 restoreConversation()，直接创建新会话
          // 新会话会在用户发送第一条消息后开始保存
          // 使用 nextTick 确保 Vue 响应式更新完成，界面正确显示
          await nextTick();
          console.log("从智能体超市选择智能体，创建新会话", {
            agentIndex: currentAgentIndex.value,
            sessionId: currentSessionId.value,
            showSessionFunctions: showSessionFunctions.value,
            messagesLength: messages.value.length
          });
        } catch (e) {
          console.log("解析selectedSessionAgent失败", e);
        }
      }
  );
  window.WeSpaceSDK.onClose(() => {
    saveConversationState();
  });
});

// 会话模式：浏览器后退/前进时，根据 chat=1 的存在与否切换“聊天态/默认态”
watch(
  () => [route.query?.chat, route.query?.sessionId, route.query?.agentIndex],
  async ([newChat, newSessionId], [oldChat]) => {
    if (!sessionMode.value) return;
    if (!hasInitializedRouteSync) return;
    if (routeSyncLock.value) return;

    const wasChat = String(oldChat ?? "") === "1";
    const isChat = String(newChat ?? "") === "1";

    if (wasChat && !isChat) {
      // 从聊天态后退回默认态：恢复历史列表 UI
      await resetToSessionHistoryState();
      return;
    }

    if (!wasChat && isChat) {
      // 从默认态前进到聊天态：按 URL 恢复会话
      await enterChatFromRouteQuery();
      return;
    }

    // chat=1 保持不变，但 sessionId 变化（浏览器前进/后退跨会话）
    if (isChat && typeof newSessionId === "string" && newSessionId !== currentSessionId.value) {
      await enterChatFromRouteQuery();
    }
  }
);

// 滚动事件处理函数
function handleScroll(event) {
  const now = Date.now();
  // 防止频繁触发
  if (now - lastScrollTime < TIME_THRESHOLD) {
    return;
  }
  lastScrollTime = now;
  
  const chatContainer = event.target;
  const { scrollTop, scrollHeight, clientHeight } = chatContainer;
  const distanceToBottom = scrollHeight - scrollTop - clientHeight;
  
  // 如果距离底部超过阈值，认为用户手动向上滚动
  if (distanceToBottom > SCROLL_THRESHOLD) {
    userScrolled.value = true;
  }
}

// 触摸事件处理函数
let isTouchScrolling = false;
let touchStartY = 0;

function handleTouchStart(event) {
  touchStartY = event.touches[0].clientY;
  isTouchScrolling = true;
}

function handleTouchMove(event) {
  if (!isTouchScrolling) return;
  
  const touchY = event.touches[0].clientY;
  const scrollDirection = touchStartY - touchY;
  
  // 向上滚动且有一定距离
  if (scrollDirection > SCROLL_THRESHOLD) {
    const chatContainer = event.target;
    const { scrollTop, scrollHeight, clientHeight } = chatContainer;
    const distanceToBottom = scrollHeight - scrollTop - clientHeight;
    
    // 如果距离底部超过阈值，认为用户手动向上滚动
    if (distanceToBottom > SCROLL_THRESHOLD) {
      userScrolled.value = true;
    }
  }
}

function handleTouchEnd() {
  isTouchScrolling = false;
}

onUnmounted(() => {
  console.log("aiAssistant组件onUnmountedt");
  // 移除滚动事件监听器
  const chatContainer = document.querySelector(".chat-history");
  if (chatContainer) {
    chatContainer.removeEventListener("scroll", handleScroll);
    chatContainer.removeEventListener("touchstart", handleTouchStart);
    chatContainer.removeEventListener("touchmove", handleTouchMove);
    chatContainer.removeEventListener("touchend", handleTouchEnd);
  }
  window.removeEventListener("resize", updateComponentByScreenSize);
  storageKeys.forEach((key) => communicationStore.removeStorageChange(key));
});

// 预加载图片
async function preloadImages() {
  const imageUrls = [
    "/static/ai/user_avatar.png",
    "/static/ai/ai_avatar.png",
    "/static/ai/ai_loading.png",
  ];

  if (userInfo.value?.thumbAvatar) {
    imageUrls.push(userInfo.value.thumbAvatar);
  }

  // 使用缓存机制预加载
  await avatarCache.preloadAvatars(imageUrls);
}

// 会话模式：加载历史会话列表（按会话ID分桶）
async function loadSessionHistories() {
  const summaryMap = await loadSummary("session");
  sessionHistoryList.value = buildSessionHistoryList(summaryMap);
  functions.value = sessionHistoryList.value;
  console.log("会话模式历史会话列表", sessionHistoryList.value?.length);
}

// 会话模式：删除某个会话（顶部列表长按触发，按会话ID删除）
async function deleteSession(func) {
  if (!sessionMode.value) return;
  const sessionId = func?.sessionId;
  const agentIndex = func?.agentIndex;
  if (!sessionId || !userInfo.value?.userid) return;

  const keyBase = `chatHistory_${userInfo.value.userid}`;
  const deepseekIndex = getDeepSeekAgentIndex(agents.value);
  const agentKey = agentIndex ?? deepseekIndex ?? "deepseek";
  const chatKey = `${keyBase}_${sessionId}_${agentKey}`;

  try {
    // 1) 删除该会话的对话缓存（设置为null，而不是空字符串，避免key残留）
    await window.WeSpaceSDK.setStorage(
        chatKey,
        unescape(encodeURIComponent(JSON.stringify(null)))
    );

    // 2) 删除会话摘要中的对应项
    const summaryKey = getSummaryKey(userInfo.value.userid, "session");
    const originSummary = await window.WeSpaceSDK.getStorage(summaryKey);
    const summaryMap = originSummary
        ? JSON.parse(decodeURIComponent(escape(originSummary)))
        : {};
    if (summaryMap && summaryMap[sessionId] != null) {
      delete summaryMap[sessionId];
      await window.WeSpaceSDK.setStorage(
          summaryKey,
          unescape(encodeURIComponent(JSON.stringify(summaryMap)))
      );
    }

    // 3) 刷新历史会话列表
    await loadSessionHistories();
    setShowSessionFunctions();
    if (currentSessionId.value === sessionId) {
      // 如果删除的是当前会话，清空当前视图
      currentSessionId.value = null;
      currentAgentIndex.value = null;
      messages.value = [];
      resetStreamState();
    }
    showToast("删除成功");
  } catch (e) {
    console.error("删除会话失败:", e);
    showToast("删除失败，请稍后再试");
  }
}

// 会话模式：决定是否展示功能列表
function setShowSessionFunctions() {
  const canShow =
      sessionMode.value &&
      !isSessionChatRoute.value &&
      sessionHistoryList.value.length > 0 &&
      !currentAgentIndex.value &&
      messages.value.length === 0;
  showSessionFunctions.value = canShow;
}

// 选择智能体
async function handleFunction(func) {
  if (sessionMode.value) {
    // func 代表"历史会话条目"，包含 sessionId 与 agentIndex
    await saveConversationState();
    currentSessionId.value = func?.sessionId ?? null;
    currentAgentIndex.value = func?.agentIndex ?? getDeepSeekAgentIndex(agents.value);
    showSessionFunctions.value = false;

    // 进入聊天态：压栈一条 chat=1 路由，确保浏览器后退能回到默认态（历史列表）
    await pushSessionChatRoute({
      sessionId: currentSessionId.value,
      agentIndex: currentAgentIndex.value,
    });

    // 标记该会话“已查看完成结果”：用于会话列表的“任务已完成”标签查看后消失
    // 仅影响“已完成”标签；“任务进行中”仍按 isStreaming 展示
    try {
      const aiTime =
        func?.lastAiMessage?.time ||
        (func?.lastMessage?.type === "ai" ? func?.lastMessage?.time : null);
      if (currentSessionId.value && aiTime) {
        await saveSummary("session", currentSessionId.value, {
          lastViewedAiTime: aiTime,
        });
      }
    } catch (e) {
      console.warn("标记会话已查看失败（可忽略）", e);
    }
    // 清除 currentAgentIndex 缓存，因为会话模式下使用 sessionId 来管理会话
    if (userInfo.value?.userid) {
      try {
        await window.WeSpaceSDK.setStorage(
            `currentAgentIndex_${userInfo.value.userid}`,
            unescape(encodeURIComponent(JSON.stringify(null)))
        );
        console.log("已清除 currentAgentIndex 存储（选择历史会话时）");
      } catch (error) {
        console.error("清除 currentAgentIndex 存储失败:", error);
      }
    }
    await restoreConversation(); // 按会话ID恢复
    return;
  }
  if (currentAgentIndex.value === func.index) {
    const targetItem = agents.value.find(
        (item) => item.name?.toLowerCase() === "deepseek"
    );
    currentAgentIndex.value = targetItem?.index;
  } else {
    currentAgentIndex.value = func?.index;
  }
  aiStore.setCurrentAgent(currentAgentIndex.value);
}

// 使用工具函数：getDeepSeekAgentIndex 和 createSessionId 已从 utils.js 导入
// 注意：getDeepSeekAgentIndex 需要传入 agents.value 作为参数

// 滚动到底部
async function scrollToBottom() {
  // 如果用户手动滚动过，不执行自动滚动
  if (userScrolled.value) {
    return;
  }
  
  await nextTick();
  const chatContainer = document.querySelector(".chat-history");
  if (chatContainer) {
    chatContainer.scrollTop = chatContainer.scrollHeight;
  }
}

// 获取用户信息
async function getUserInfo() {
  try {
    const res = await communicationStore.getUserInfo();
    if (res) {
      userInfo.value = res;
      console.log("打印用户信息", JSON.stringify(userInfo.value, null, 2));
    } else {
      console.error("获取用户信息失败或WeSpaceSDK不可用");
    }
  } catch (error) {
    console.error(`获取用户信息错误: ${error.message}`);
  }
}

// 发送消息
async function sendMessage(message) {
  if (isLoading.value) return;
  const msg = message.trim();
  if (!msg) return;

  // 会话模式：如果当前处于默认态（未 chat=1），首次发送前先压栈进入聊天态
  // 这样浏览器后退可 100% 回到历史列表/空提示页
  if (sessionMode.value && !isSessionChatRoute.value) {
    if (currentAgentIndex.value == null) {
      currentAgentIndex.value = getDeepSeekAgentIndex(agents.value);
    }
    if (!currentSessionId.value) {
      currentSessionId.value = createSessionId();
    }
    await pushSessionChatRoute({
      sessionId: currentSessionId.value,
      agentIndex: currentAgentIndex.value,
    });
  }
  // 会话模式：用户未选择智能体时，用 DeepSeek 作为默认会话智能体
  // 否则 getChatKeyLocal() 会返回 null，导致流式过程中不写入任何缓存（看起来就像"没有 ai 类型数据"）
  if (sessionMode.value && currentAgentIndex.value == null) {
    currentAgentIndex.value = getDeepSeekAgentIndex(agents.value);
  }
  // 会话模式：首次发送前创建会话ID
  if (sessionMode.value && !currentSessionId.value) {
    currentSessionId.value = createSessionId();
  }
  // 重置用户滚动状态，确保新消息发送后会自动滚动到底部
  userScrolled.value = false;

    // 添加用户消息
  messages.value.push({
    type: "user",
    content: msg,
    timestamp: Date.now(),
    userid: userInfo.value.userid,
  });
  // 会话模式下发送后收起功能列表
  if (sessionMode.value) {
    showSessionFunctions.value = false;
  }
  saveConversationState();
  callAiApi(msg);
  scrollToBottom();
}

// 流式输出的状态管理
const streamState = ref({
  isStreaming: false, // 是否正在流式输出
  isPaused: false, // 是否暂停
  displayedContent: "", // 当前显示内容
  timer: null, // 定时器引用
  rawContent: "", // 本地保存的完整内容
  pendingContent: "", // 待显示的内容
  taskId: null, // 任务标识
});
const ERROR_MESSAGE = "业务繁忙，请稍后再试";
const ERROR_TIMEOUT = "查询超时";
const SEARCHING_SHOW = "正在查询...";

// 持久化节流，避免频繁写入存储
let saveTimer = null;
function scheduleSaveConversationState() {
  if (saveTimer) {
    clearTimeout(saveTimer);
  }
  saveTimer = setTimeout(() => {
    saveConversationState();
    saveTimer = null;
  }, 100); // 缩短节流时间到100毫秒，确保内容及时保存
}

// 调用 AI API
async function callAiApi(message) {
  try {
    isLoading.value = true;
    isReceiving.value = false;
    // 会话模式未选择智能体时，回退到 DeepSeek 作为默认智能体；综合模式保持原有值
    const fallbackAgentIndex =
        sessionMode.value && currentAgentIndex.value == null
            ? getDeepSeekAgentIndex(agents.value)
            : currentAgentIndex.value;
    // 会话模式下如果使用了 fallback（DeepSeek），也要落到 currentAgentIndex，保证后续按会话分桶缓存/恢复
    if (sessionMode.value && currentAgentIndex.value == null) {
      currentAgentIndex.value = fallbackAgentIndex;
    }
    // 会话模式下确保有会话ID
    if (sessionMode.value && !currentSessionId.value) {
      currentSessionId.value = createSessionId();
    }
    const params = {
      content: message,
      agent: fallbackAgentIndex,
      userName: userInfo.value.username,
      userID: userInfo.value.idCard, // 传递身份证号码
    };
    if (userInfo.value?.userDepartments?.length) {
      params.departmentId = userInfo.value.userDepartments[0].departmentId;
      params.departmentCode = userInfo.value.userDepartments[0].departmentCode;
      params.departmentName = userInfo.value.userDepartments[0].departmentName;
    }
      const newTaskData = await aiApi.newTask(params);
      if (newTaskData.code == 0) {
        currentTaskId.value = newTaskData.data;
      } else if (newTaskData.code == 6001) {
        // 找不到智能体
        showToast('当前智能体已删除，请重新选择');
        functions.value = functions.value.filter((item) => item?.index !== fallbackAgentIndex);
        console.log(functions, '新智能体列表');
        currentAgentIndex.value = null;
        aiStore.setCurrentAgent(null);
        // 7. 重置相关状态
        isLoading.value = false;
        isReceiving.value = false;
        currentTaskId.value = null;
        return;
      } else {
        showToast(newTaskData.msg);
      }
    // 清空之前的流式状态
    resetStreamState();

    isLoading.value = false;

    // 使用流式输出
    await startStreamOutput(currentTaskId.value);
    isReceiving.value = false;
    scrollToBottom();
  } catch (error) {
    console.error("调用 AI API 失败:", error);
    handleError(ERROR_MESSAGE);
    isLoading.value = false;
    scrollToBottom();
  }
}

// 重置流式输出状态
function resetStreamState() {
  if (streamState.value.timer) {
    clearInterval(streamState.value.timer);
  }
  streamState.value.isStreaming = false;
  streamState.value.isPaused = false;
  streamState.value.displayedContent = "";
  streamState.value.timer = null;
  streamState.value.rawContent = "";
  streamState.value.pendingContent = "";
}

// 开始流式输出
async function startStreamOutput(taskId, isHistory = false) {
  // bugfix: 暂停，输入新问题，每次问答后，while循环里面第2/3次调用的getUnswer 传入taskId是上一次问答的
  // 在循环开始前，currentLoopTaskId 捕获当前的 taskId 到局部变量，用于后续比对
  const currentLoopTaskId = taskId;
  const INTERVAL = 1000;
  const END_TAG = "[end]";
  const MAX_TIMEOUT = 5000; // 最长等待有效响应时间
  let lastResponseTime = Date.now(); // 上次收到有效响应的时间

  streamState.value.taskId = taskId;
  streamState.value.isStreaming = true;
  if (!isHistory) {
    // 非退出重进的历史加载
    // 创建临时消息
    const tempMessage = {
      type: "ai",
      content: SEARCHING_SHOW,
      timestamp: Date.now(),
      userid: userInfo.value.userid,
      isStreaming: true,
    };
    messages.value.push(tempMessage);
    // 立即保存一次，确保taskId被记录（不使用节流，避免刷新太快导致taskId丢失）
    // 特别是在会话模式下，第一条消息的taskId必须立即保存，否则刷新后无法继续查询
    await saveConversationState();
  } else {
    // 恢复历史流式输出时，确保已显示内容正确更新到消息中
    if (streamState.value.displayedContent) {
      const lastIndex = messages.value.length - 1;
      if (lastIndex >= 0 && messages.value[lastIndex]?.type === "ai") {
        messages.value[lastIndex].content = marked.parse(streamState.value.displayedContent);
        messages.value[lastIndex].isStreaming = streamState.value.isStreaming;
      }
    }
    
    // 如果有待显示内容，启动显示定时器
    if (streamState.value.pendingContent) {
      startDisplayTimer();
    }
  }

  isReceiving.value = true;
  while (true) {
    try {
      // bugfix: 暂停，输入新问题，每次问答后，while循环里面第2/3次调用的getUnswer 传入taskId是上一次问答的
      // 如果发现全局 streamState 里的 taskId 和当前taskId 不匹配，立刻退出
      if (streamState.value.taskId !== currentLoopTaskId) {
        console.log('检测到新任务启动，旧任务自动停止:', currentLoopTaskId);
        break;
      }
      if (streamState.value.isPaused) {
        await new Promise((resolve) => setTimeout(resolve, INTERVAL));
        continue;
      }
      const res = await aiApi.getAnswer({ id: unref(taskId) });
      console.log("API响应:", JSON.stringify(res, null, 2));

      // null 继续轮询
      if (!res) {
        // 检查无响应时间是否超时
        if (Date.now() - lastResponseTime > MAX_TIMEOUT) {
          console.warn("长时间无有效响应，终止轮询");
          handleError(ERROR_TIMEOUT);
          return;
        }
        continue;
      }
      // 收到有效响应，更新最后响应时间
      lastResponseTime = Date.now();

      // 检查错误响应
      if (res.includes("ERROR")) {
        handleError(ERROR_MESSAGE);
        return;
      }

      // 提取增量内容
      let newContent = res;
      let isEnd = false;

      // 检查是否包含结束标记
      if (res.includes(END_TAG)) {
        isEnd = true;
        newContent = res.replace(END_TAG, "");
      }

      // 更新原始内容
      const oldContent = streamState.value.rawContent;

      if (newContent.startsWith(oldContent)) {
        const increment = newContent.substring(oldContent.length);
        if (increment) {
          streamState.value.rawContent = newContent;
          streamState.value.pendingContent += increment;

          // 如果没有活动的定时器，开始显示内容
          if (!streamState.value.timer) {
            startDisplayTimer();
          }
          
          // 定期保存状态，确保内容不会丢失
          scheduleSaveConversationState();
        }
      } else if (!oldContent.startsWith(newContent)) {
        console.log("新内容与旧内容不连续，直接拼接:", newContent);

        streamState.value.rawContent =
            streamState.value.displayedContent + newContent;
        streamState.value.pendingContent += newContent;

        // 如果没有活动的定时器，开始显示内容
        if (!streamState.value.timer) {
          startDisplayTimer();
        }
        
        // 定期保存状态，确保内容不会丢失
        scheduleSaveConversationState();
      }
      // 如果是结束响应，更新状态
      if (isEnd) {
        isReceiving.value = false;

        // 确保所有内容都显示出来
        if (!streamState.value.timer && streamState.value.pendingContent) {
          streamState.value.displayedContent +=
              streamState.value.pendingContent;
          streamState.value.pendingContent = "";
          updateMessageContent();
        }
          const finalContent = streamState.value.rawContent || streamState.value.displayedContent;
          if (finalContent) {
            // 先转换为 HTML，再去除 HTML 标签
            const htmlContent = marked.parse(finalContent);
            const plainText = stripHtml(htmlContent);
            console.log('[AI智能体响应数据]通知：上app通知栏，消息内容为：', plainText);
            sendNotification({ text: plainText });
        }

        // 关键修复：只有在没有pendingContent且没有定时器运行时，才设置isStreaming为false
        // 如果还有pendingContent待显示，应该保持isStreaming为true，以便恢复时能继续显示
        if (!streamState.value.pendingContent && !streamState.value.timer) {
          streamState.value.isStreaming = false;
        }
        scheduleSaveConversationState();

        break;
      }
    } catch (err) {
      console.error("轮询错误:", err);
      handleError(ERROR_MESSAGE);
      return;
    }

    await new Promise((resolve) => setTimeout(resolve, INTERVAL));
  }
}

// 从保存状态恢复流式输出（不重新开始API轮询，仅恢复显示状态）
async function startStreamFromSavedState(taskId) {
  // 恢复显示状态，但不重新开始API轮询
  // 这样可以避免重复回答的问题
  // 确保已显示内容正确更新到消息中
  if (streamState.value.displayedContent) {
    const lastIndex = messages.value.length - 1;
    if (lastIndex >= 0 && messages.value[lastIndex]?.type === "ai") {
      messages.value[lastIndex].content = marked.parse(streamState.value.displayedContent);
      // 关键修复：如果有pendingContent待显示，需要将isStreaming设置为true
      // 这样updateMessageContent函数才能正常工作，继续显示pendingContent
      const shouldBeStreaming = streamState.value.isStreaming || 
                                 (streamState.value.pendingContent !== "" && !streamState.value.isPaused);
      messages.value[lastIndex].isStreaming = shouldBeStreaming;
      // 同时更新streamState中的isStreaming，确保状态一致
      if (shouldBeStreaming && !streamState.value.isStreaming) {
        streamState.value.isStreaming = true;
      }
    }
  }
  
  // 如果有待显示内容且未暂停，启动显示定时器
  if (streamState.value.pendingContent !== "" && !streamState.value.isPaused) {
    startDisplayTimer();
  }
  
  // 设置当前任务ID和接收状态
  currentTaskId.value = taskId;
  isReceiving.value = streamState.value.isStreaming && !streamState.value.isPaused;
  
  // 如果仍在流式输出状态，继续轮询API以获取剩余内容
  // ⚠️ 关键修复：如果已暂停，不继续API轮询
  if (streamState.value.isStreaming && !streamState.value.isPaused) {
    // 继续未完成的流式输出，而不是重新开始
    await continueStreamOutput(taskId);
  } else if (streamState.value.isStreaming && streamState.value.isPaused) {
    isReceiving.value = false; // 暂停时不显示接收状态
  }
}

// 继续未完成的流式输出（基于保存的状态继续）
async function continueStreamOutput(taskId) {
  // bugfix: 暂停，输入新问题，每次问答后，while循环里面第2/3次调用的getUnswer 传入taskId是上一次问答的
  // 在循环开始前，currentLoopTaskId 捕获当前的 taskId 到局部变量，用于后续比对
  const currentLoopTaskId = taskId;
  const INTERVAL = 1000;
  const END_TAG = "[end]";
  const MAX_TIMEOUT = 5000; // 最长等待有效响应时间
  let lastResponseTime = Date.now(); // 上次收到有效响应的时间

  streamState.value.taskId = taskId;
  // 注意：这里不设置isStreaming为true，因为我们是从保存的状态继续
  
  isReceiving.value = true;
  
  // 继续轮询，但不创建新的临时消息
  while (streamState.value.isStreaming) {
    try {
      // bugfix: 暂停，输入新问题，每次问答后，while循环里面第2/3次调用的getUnswer 传入taskId是上一次问答的
      // 如果发现全局 streamState 里的 taskId 和当前taskId 不匹配，立刻退出
      if (streamState.value.taskId !== currentLoopTaskId) {
        console.log('检测到新任务启动，旧任务自动停止:', currentLoopTaskId);
        break;
      }
      if (streamState.value.isPaused) {
        await new Promise((resolve) => setTimeout(resolve, INTERVAL));
        continue;
      }
      
      const res = await aiApi.getAnswer({ id: unref(taskId) });

      // null 继续轮询
      if (!res) {
        // 检查无响应时间是否超时
        if (Date.now() - lastResponseTime > MAX_TIMEOUT) {
          console.warn("长时间无有效响应，终止轮询");
          handleError(ERROR_TIMEOUT);
          return;
        }
        continue;
      }
      
      // 收到有效响应，更新最后响应时间
      lastResponseTime = Date.now();

      // 检查错误响应
      if (res.includes("ERROR")) {
        handleError(ERROR_MESSAGE);
        return;
      }

      // 提取增量内容
      let newContent = res;
      let isEnd = false;

      // 检查是否包含结束标记
      if (res.includes(END_TAG)) {
        isEnd = true;
        newContent = res.replace(END_TAG, "");
      }

      // 更新原始内容
      const oldContent = streamState.value.rawContent;

      if (newContent.startsWith(oldContent)) {
        const increment = newContent.substring(oldContent.length);
        if (increment) {
          streamState.value.rawContent = newContent;
          streamState.value.pendingContent += increment;

          // 如果没有活动的定时器，开始显示内容
          if (!streamState.value.timer) {
            startDisplayTimer();
          }
          
          // 定期保存状态，确保内容不会丢失
          scheduleSaveConversationState();
        }
      } else if (!oldContent.startsWith(newContent)) {
        streamState.value.rawContent =
          streamState.value.displayedContent + newContent;
        streamState.value.pendingContent += newContent;

        // 如果没有活动的定时器，开始显示内容
        if (!streamState.value.timer) {
          startDisplayTimer();
        }
        
        // 定期保存状态，确保内容不会丢失
        scheduleSaveConversationState();
      }
      
      // 如果是结束响应，更新状态
      if (isEnd) {
        isReceiving.value = false;

        // 确保所有内容都显示出来
        if (!streamState.value.timer && streamState.value.pendingContent) {
          streamState.value.displayedContent +=
            streamState.value.pendingContent;
          streamState.value.pendingContent = "";
          updateMessageContent();
        }

        // 关键修复：只有在没有pendingContent且没有定时器运行时，才设置isStreaming为false
        // 如果还有pendingContent待显示，应该保持isStreaming为true，以便恢复时能继续显示
        if (!streamState.value.pendingContent && !streamState.value.timer) {
          streamState.value.isStreaming = false;
        }
        scheduleSaveConversationState();

        break;
      }
    } catch (err) {
      console.error("轮询错误:", err);
      handleError(ERROR_MESSAGE);
      return;
    }

    await new Promise((resolve) => setTimeout(resolve, INTERVAL));
  }
}

// 开始显示内容
function startDisplayTimer() {
  if (streamState.value.timer) {
    clearInterval(streamState.value.timer);
  }
  streamState.value.timer = setInterval(() => {
    if (streamState.value.isPaused) {
      return;
    }
    
    if (streamState.value.pendingContent) {
      // 每次显示一个字符
      const charToDisplay = streamState.value.pendingContent.charAt(0);
      streamState.value.displayedContent += charToDisplay;
      streamState.value.pendingContent =
          streamState.value.pendingContent.substring(1);
      updateMessageContent();
    } else {
      // 没有更多待显示的内容，清除定时器
      if (streamState.value.timer) {
        clearInterval(streamState.value.timer);
      }
      streamState.value.timer = null;
      streamState.value.isPaused = false;
      
      // 关键修复：当所有内容显示完毕后，将isStreaming设置为false
      // 这样按钮才能正确切换为发送按钮
      streamState.value.isStreaming = false;
      // 确保pendingContent为空
      streamState.value.pendingContent = "";
      // 确保isReceiving也为false
      isReceiving.value = false;

      // 更新消息状态：将最后一条AI消息的isStreaming设置为false
      const lastIndex = messages.value.length - 1;
      if (lastIndex >= 0 && messages.value[lastIndex]?.type === "ai") {
        messages.value[lastIndex].isStreaming = false;
      }
      
      // 保存最终状态，确保按钮状态正确
      scheduleSaveConversationState();
    }
  }, 50);
}

// 处理错误情况
function handleError(val) {
  resetStreamState();
  streamState.value.rawContent = val;
  streamState.value.displayedContent = val;

  // 更新消息内容
  let lastIndex = messages.value.length - 1;
  if (messages.value[lastIndex].type === "user") {
    lastIndex = lastIndex + 1;
  }
  if (lastIndex >= 0) {
    messages.value[lastIndex] = {
      type: "ai",
      content: marked.parse(val),
      timestamp: Date.now(),
      userid: userInfo.value.userid,
      isStreaming: false,
    };
  } else {
    messages.value.push({
      type: "ai",
      content: marked.parse(val),
      timestamp: Date.now(),
      userid: userInfo.value.userid,
      isStreaming: false,
    });
  }

  scrollToBottom();
  console.log('[AI智能体响应数据]通知：上app通知栏，消息内容为：', val);
  sendNotification({ text: val });
  scheduleSaveConversationState();
}

// 更新消息内容
function updateMessageContent() {
  const lastIndex = messages.value.length - 1;
  
  // 关键修复：不仅检查isStreaming，还要检查是否有pendingContent或timer
  // 这样可以确保即使isStreaming为false，如果有待显示内容，也能更新消息
  const shouldUpdate = lastIndex >= 0 && 
    (messages.value[lastIndex].isStreaming || 
     streamState.value.pendingContent !== "" || 
     streamState.value.timer);
  
  if (shouldUpdate && messages.value[lastIndex]?.type === "ai") {
    messages.value[lastIndex].content = marked.parse(
        streamState.value.displayedContent
    );
    // 如果有pendingContent或timer，确保isStreaming为true
    if (streamState.value.pendingContent !== "" || streamState.value.timer) {
      messages.value[lastIndex].isStreaming = true;
    }
    scrollToBottom();
    // 立即保存对话状态，确保displayedContent被正确保存
    saveConversationState();
  }
}

// 暂停流式输出
function pauseStream() {
  if ((streamState.value.isStreaming || streamState.value.pendingContent || streamState.value.timer) && !streamState.value.isPaused) {
    streamState.value.isPaused = true;
    saveConversationState();  // 立即保存，确保状态不丢失
  }
}

// 继续流式输出
function resumeStream() {
  if (streamState.value.isPaused) {
    streamState.value.isPaused = false;
    
    // 如果有待显示内容，启动显示定时器
    if (streamState.value.pendingContent && !streamState.value.timer) {
      startDisplayTimer();
    }
    
    // ⚠️ 关键修复：如果还在流式输出状态（从API获取内容），需要继续轮询
    if (streamState.value.isStreaming && currentTaskId.value) {
      isReceiving.value = true;
      continueStreamOutput(currentTaskId.value);
    }
  }
}

// getChatKey 函数已从 utils.js 导入，但需要适配当前组件的响应式变量
// 保留本地函数以访问响应式变量
function getChatKeyLocal() {
  // 使用工具函数统一管理
  return getChatKey(
      userInfo.value.userid,
      sessionMode.value,
      currentSessionId.value,
      currentAgentIndex.value,
      agents.value
  );
}

async function loadSummary(modeName) {
  if (!userInfo.value?.userid) return {};
  try {
    const data = await window.WeSpaceSDK.getStorage(getSummaryKey(userInfo.value.userid, modeName));
    return data ? JSON.parse(decodeURIComponent(escape(data))) : {};
  } catch (error) {
    console.log("读取摘要失败", error);
    return {};
  }
}

async function saveSummary(modeName, sessionId, summary) {
  if (!userInfo.value?.userid || !sessionId) {
    console.warn("saveSummary: 缺少必要参数", { userid: userInfo.value?.userid, sessionId });
    return;
  }
  try {
    const map = await loadSummary(modeName);
    // 合并写入，避免覆盖掉会话上的其他状态字段（例如 lastViewedAiTime）
    map[sessionId] = {
      ...(map?.[sessionId] || {}),
      ...(summary || {}),
    };
    const summaryKey = getSummaryKey(userInfo.value.userid, modeName);
    await window.WeSpaceSDK.setStorage(
        summaryKey,
        unescape(encodeURIComponent(JSON.stringify(map)))
    );
    console.log("saveSummary: 保存成功", { modeName, sessionId, summaryKey, mapKeys: Object.keys(map) });
  } catch (error) {
    console.error("保存摘要失败", error);
  }
}

// 构建历史会话列表：将摘要Map转为数组并补齐展示信息
function buildSessionHistoryList(summaryMap = {}) {
  const deepseekIndex = getDeepSeekAgentIndex(agents.value);
  const list = Object.entries(summaryMap || {}).map(([sessionId, summary]) => {
    const agentIndex = summary?.agentIndex ?? deepseekIndex;
    const agent = agents.value.find((item) => item.index === agentIndex);
    const rawName = summary?.agentName || agent?.name || DEFAULT_TITLE;
    const displayName =
        rawName?.toLowerCase?.() === "deepseek" ? DEFAULT_TITLE : rawName;
    return {
      sessionId,
      agentIndex,
      name: rawName,
      displayName,
      picUrl: summary?.agentPicUrl || agent?.picUrl || null,
      description: summary?.agentDesc || agent?.desc,
      lastMessage: summary?.lastMessage,
      lastAiMessage: summary?.lastAiMessage,
      // 新增：用于展示“提问”的最后一条用户消息
      lastUserMessage: summary?.lastUserMessage,
      // 新增：用于控制“任务已完成”查看后消失
      lastViewedAiTime: summary?.lastViewedAiTime,
      lastMsgTime:
          summary?.lastMessage?.time || summary?.lastMsgTime || summary?.time,
    };
  });

  // 按最后消息时间倒序
  return list.sort((a, b) => (b.lastMsgTime || 0) - (a.lastMsgTime || 0));
}

// 从本地存储恢复对话状态（按模式、智能体分桶）
async function restoreConversation() {
  try {
    const chatKey = getChatKeyLocal();
    // 会话模式下未选择会话时直接返回空记录，等待用户选择历史或新会话
    if (!chatKey) {
      messages.value = [];
      resetStreamState();
      setShowSessionFunctions();
      return false;
    }

    const originData = await window.WeSpaceSDK.getStorage(chatKey);
    const parsedState = originData
        ? JSON.parse(decodeURIComponent(escape(originData)))
        : null;
    if (parsedState) {
      // 恢复消息历史
      messages.value = parsedState.messages || [];

      // 恢复流式输出状态
      streamState.value = parsedState.streamState;
      streamState.value.timer = null; // 重置定时器
      
      const {
        isStreaming,
        pendingContent,
        taskId,
        displayedContent,
        rawContent,
        isPaused
      } = streamState.value;

      // 如果正在流式输出或有暂停的内容，需要正确恢复状态
      if (isStreaming || pendingContent !== "" || isPaused) {
        // 更新已显示的内容到messages中
        if (displayedContent && displayedContent.trim() !== "") {
          const lastIndex = messages.value.length - 1;
          if (lastIndex >= 0 && messages.value[lastIndex]?.type === "ai") {
            messages.value[lastIndex].content = marked.parse(displayedContent);
            // 关键修复：如果有pendingContent待显示，需要将isStreaming设置为true
            // 这样updateMessageContent函数才能正常工作，继续显示pendingContent
            const shouldBeStreaming = isStreaming || (pendingContent !== "" && !isPaused);
            messages.value[lastIndex].isStreaming = shouldBeStreaming;
            // 同时更新streamState中的isStreaming，确保状态一致
            if (shouldBeStreaming && !isStreaming) {
              streamState.value.isStreaming = true;
            }
          }
        }
        
        // 如果有任务ID且正在流式输出，恢复流式状态但不重新开始API轮询
        // ⚠️ 关键修复：如果已暂停，不继续API轮询，只恢复显示状态
        if (taskId && isStreaming && !isPaused) {
          await startStreamFromSavedState(taskId);
        } else if (taskId && isStreaming && isPaused) {
          // 暂停状态：只恢复显示内容，不启动API轮询和显示定时器
          // 确保消息状态正确
          if (displayedContent && displayedContent.trim() !== "") {
            const lastIndex = messages.value.length - 1;
            if (lastIndex >= 0 && messages.value[lastIndex]?.type === "ai") {
              messages.value[lastIndex].content = marked.parse(displayedContent);
              messages.value[lastIndex].isStreaming = true; // 标记为流式输出中（但已暂停）
            }
          }
          currentTaskId.value = taskId;
          isReceiving.value = false; // 暂停时不显示接收状态
        } else if (pendingContent !== "" && !isPaused) {
          // 如果没有任务ID但有待显示内容，直接启动显示定时器
          startDisplayTimer();
        }
        
        isLoading.value = false;
      } else {
        // 处理非流式状态的错误情况
        const lastMessage = messages.value[messages.value.length - 1];
        if (lastMessage?.type === "user") {
          messages.value.push({
            type: "ai",
            content: "查询失败",
            timestamp: Date.now(),
            userid: userInfo.value.userid,
            isStreaming: false,
          });
        } else if (
          lastMessage?.type === "ai" &&
          lastMessage?.content === SEARCHING_SHOW
        ) {
          messages.value[messages.value.length - 1] = {
            type: "ai",
            content: "查询失败",
            timestamp: Date.now(),
            userid: userInfo.value.userid,
            isStreaming: false,
          };
        }
      }
      
      scrollToBottom();
      console.log("对话状态已从本地存储恢复", chatKey);
      setShowSessionFunctions();
      return true;
    }
  } catch (error) {
    console.error("恢复对话状态失败:", error);
  }
  
  // 默认状态：清空消息和流式状态
  messages.value = [];
  resetStreamState();
  setShowSessionFunctions();
  return false;
}
// 保存对话状态到本地存储（按模式、智能体分桶）
const saveConversationState = async () => {
  console.log("saveConversationState更新", new Date());
  try {
    const chatKey = getChatKeyLocal();
    // 未选择会话时不执行存储，等待用户明确选择
    if (!chatKey) return;

    // 在保存前，如果正在流式输出且displayedContent有内容，先同步更新messages.value
    // 这样可以确保保存的messages.value包含最新的内容
    if (
        streamState.value?.isStreaming &&
        streamState.value?.displayedContent &&
        streamState.value.displayedContent.trim() !== ""
    ) {
      const lastIndex = messages.value.length - 1;
      if (lastIndex >= 0 && messages.value[lastIndex]?.type === "ai" && messages.value[lastIndex]?.isStreaming) {
        messages.value[lastIndex].content = marked.parse(streamState.value.displayedContent);
        // 确保isStreaming属性被正确设置
        messages.value[lastIndex].isStreaming = true;
      }
    }
    
    // 特别处理：如果最后一条消息是"正在查询..."且有显示内容，更新消息内容
    const lastIndex = messages.value.length - 1;
    if (lastIndex >= 0 && 
        messages.value[lastIndex]?.type === "ai" && 
        messages.value[lastIndex]?.isStreaming === true &&
        messages.value[lastIndex]?.content === SEARCHING_SHOW &&
        streamState.value?.displayedContent && 
        streamState.value.displayedContent.trim() !== "") {
      messages.value[lastIndex].content = marked.parse(streamState.value.displayedContent);
      // 确保isStreaming属性被正确设置
      messages.value[lastIndex].isStreaming = true;
    }

    const stateToSave = {
      messages: messages.value,
      streamState: streamState.value,
    };
    const escaped = unescape(encodeURIComponent(JSON.stringify(stateToSave)));
    await window.WeSpaceSDK.setStorage(chatKey, escaped);

    // 写入摘要（最近一条"有效"消息）
    // 说明：流式开始时会先插入一条 content=SEARCHING_SHOW 的占位 AI 消息，
    // 如果立刻把它写入摘要，会导致会话列表一直显示"正在查询..."
    const lastMsg = messages.value[messages.value.length - 1];
    const lastAiMsg = [...messages.value].reverse().find((m) => m?.type === "ai");

    // 会话模式下：直接使用最后一条消息，不做isStreaming判断
    // 但如果正在流式输出，需要确保使用最新的displayedContent
    let lastEffectiveMsg, lastEffectiveAiMsg;
    if (sessionMode.value) {
      // 会话模式：如果最后一条AI消息正在流式输出，且displayedContent有内容，使用displayedContent
      if (
          lastAiMsg?.type === "ai" &&
          lastAiMsg?.isStreaming === true &&
          streamState.value?.displayedContent &&
          streamState.value.displayedContent.trim() !== ""
      ) {
        // 创建一个包含最新内容的AI消息对象用于摘要
        // 使用marked.parse保持与messages.value中的格式一致
        const parsedContent = marked.parse(streamState.value.displayedContent);
        lastEffectiveAiMsg = {
          ...lastAiMsg,
          content: parsedContent,
        };
        // 如果最后一条消息就是AI消息，也更新它
        if (lastMsg?.type === "ai" && lastMsg === lastAiMsg) {
          lastEffectiveMsg = lastEffectiveAiMsg;
        } else {
          lastEffectiveMsg = lastMsg;
        }
      } else {
        lastEffectiveMsg = lastMsg;
        lastEffectiveAiMsg = lastAiMsg;
      }
    } else {
      // 综合模式：保持原有逻辑，判断占位消息
      const isPlaceholderStreamingAi =
          lastMsg?.type === "ai" &&
          lastMsg?.isStreaming === true &&
          lastMsg?.content === SEARCHING_SHOW &&
          !streamState.value?.displayedContent;

      lastEffectiveMsg = isPlaceholderStreamingAi
          ? [...messages.value]
              .slice(0, -1)
              .reverse()
              .find((m) => m?.content)
          : lastMsg;

      lastEffectiveAiMsg =
          lastAiMsg?.type === "ai" &&
          lastAiMsg?.isStreaming === true &&
          lastAiMsg?.content === SEARCHING_SHOW &&
          !streamState.value?.displayedContent
              ? [...messages.value]
                  .slice(0, -1)
                  .reverse()
                  .find((m) => m?.type === "ai" && m?.content)
              : lastAiMsg;
    }

    // 会话模式：有用户消息时就保存摘要，即使AI还在流式输出中
    if (sessionMode.value) {
      // 检查是否有用户消息
      const hasUserMsg = messages.value.some((m) => m?.type === "user" && m?.content);

      // 检查是否有AI消息（包括流式输出中的消息）
      // 如果有流式输出中的内容，也算作有效AI消息
      const hasAiMsgInMessages = messages.value.some((m) => {
        if (m?.type !== "ai" || !m?.content) return false;
        // 排除占位消息：如果isStreaming为true且content是SEARCHING_SHOW，则不算有效消息
        if (m?.isStreaming === true && (m?.content === SEARCHING_SHOW || stripHtml(m?.content) === SEARCHING_SHOW)) {
          return false;
        }
        // 排除空内容或只有占位文本的消息
        const textContent = stripHtml(m?.content);
        return textContent && textContent.trim() !== "" && textContent !== SEARCHING_SHOW;
      });

      // 检查是否有流式输出中的内容
      const hasStreamingContent = streamState.value?.isStreaming &&
          streamState.value?.displayedContent &&
          streamState.value.displayedContent.trim() !== "";

      // 只要有用户消息就保存摘要（即使AI还在流式输出中）
      // 确保 currentSessionId 已设置
      if (hasUserMsg && currentSessionId.value && (hasAiMsgInMessages || hasStreamingContent || lastEffectiveMsg || lastEffectiveAiMsg)) {
        const agentIdx = currentAgentIndex.value ?? getDeepSeekAgentIndex(agents.value);
        const agent = agents.value.find((item) => item.index === agentIdx);

        // 如果AI消息还在流式输出中，使用流式输出中的内容
        let finalLastEffectiveMsg = lastEffectiveMsg;
        let finalLastEffectiveAiMsg = lastEffectiveAiMsg;

        // 如果流式输出中有内容，优先使用流式输出的内容
        if (hasStreamingContent && streamState.value?.displayedContent) {
          const parsedStreamingContent = marked.parse(streamState.value.displayedContent);
          // 查找最后一条AI消息（可能是占位消息）
          const lastAiMsgInList = [...messages.value].reverse().find((m) => m?.type === "ai");
          if (lastAiMsgInList) {
            finalLastEffectiveAiMsg = {
              ...lastAiMsgInList,
              content: parsedStreamingContent,
              isStreaming: true, // 标记为正在流式输出
              timestamp: lastAiMsgInList.timestamp || Date.now(), // 确保有时间戳
            };
            // 如果最后一条消息就是AI消息，也更新它
            if (lastEffectiveMsg?.type === "ai") {
              finalLastEffectiveMsg = finalLastEffectiveAiMsg;
            } else if (!finalLastEffectiveMsg) {
              // 如果没有有效的最后消息，使用用户消息作为最后消息
              const lastUserMsg = [...messages.value].reverse().find((m) => m?.type === "user");
              if (lastUserMsg) {
                finalLastEffectiveMsg = lastUserMsg;
              }
            }
          } else {
            // 如果没有AI消息，但流式输出中有内容，创建一个临时的AI消息对象
            const lastUserMsg = [...messages.value].reverse().find((m) => m?.type === "user");
            if (lastUserMsg) {
              finalLastEffectiveAiMsg = {
                type: "ai",
                content: parsedStreamingContent,
                isStreaming: true,
                timestamp: Date.now(),
              };
              finalLastEffectiveMsg = lastUserMsg;
            }
          }
        }
        
        // 新增：会话摘要中显式记录“最后一条用户消息”，用于会话列表展示提问文案
        const lastUserMsgForSummary = [...messages.value]
          .reverse()
          .find((m) => m?.type === "user" && m?.content);

        const summary = {
          // 用于列表展示文本/时间，保持兼容：仍然记录最后一条消息
          lastMessage: finalLastEffectiveMsg
              ? {
                content: finalLastEffectiveMsg.content,
                time: finalLastEffectiveMsg.timestamp,
                type: finalLastEffectiveMsg.type,
                isStreaming: finalLastEffectiveMsg.isStreaming,
              }
              : undefined,
          lastMsgTime: finalLastEffectiveMsg?.timestamp || Date.now(),
          // 额外记录最后一条 AI 消息以供状态判断
          lastAiMessage: finalLastEffectiveAiMsg
              ? {
                content: finalLastEffectiveAiMsg.content,
                time: finalLastEffectiveAiMsg.timestamp,
                type: finalLastEffectiveAiMsg.type,
                isStreaming: finalLastEffectiveAiMsg.isStreaming,
              }
            : undefined,
          // 新增：记录最后一条用户消息（用于会话列表展示“提问”文案）
          lastUserMessage: lastUserMsgForSummary
            ? {
                content: lastUserMsgForSummary.content,
                time: lastUserMsgForSummary.timestamp,
                type: lastUserMsgForSummary.type,
              }
            : undefined,
          // 新增：会话级元信息
          agentIndex: agentIdx,
          agentName:
              agent?.name?.toLowerCase?.() === "deepseek" ? DEFAULT_TITLE : agent?.name,
          agentPicUrl: agent?.picUrl,
          agentDesc: agent?.desc,
        };
        await saveSummary("session", currentSessionId.value, summary);

        // 会话模式：保存后刷新历史列表，保证顶部历史可见
        // 先读取最新的摘要数据，确保获取到刚保存的内容
        const summaryMap = await loadSummary("session");
        sessionHistoryList.value = buildSessionHistoryList(summaryMap);
        functions.value = sessionHistoryList.value;
        setShowSessionFunctions();
        console.log("会话模式：已保存并刷新历史列表", {
          sessionId: currentSessionId.value,
          summaryMapKeys: Object.keys(summaryMap),
          historyListLength: sessionHistoryList.value.length
        });
      }
    } else if (lastEffectiveMsg || lastEffectiveAiMsg) {
      // 综合模式：保持原有逻辑
      const agentIdx = currentAgentIndex.value ?? getDeepSeekAgentIndex(agents.value);
      const agent = agents.value.find((item) => item.index === agentIdx);
      const summary = {
        // 用于列表展示文本/时间，保持兼容：仍然记录最后一条消息
        lastMessage: lastEffectiveMsg
            ? {
              content: lastEffectiveMsg.content,
              time: lastEffectiveMsg.timestamp,
              type: lastEffectiveMsg.type,
              isStreaming: lastEffectiveMsg.isStreaming,
            }
            : undefined,
        lastMsgTime: lastEffectiveMsg?.timestamp,
        // 额外记录最后一条 AI 消息以供状态判断
        lastAiMessage: lastEffectiveAiMsg
            ? {
              content: lastEffectiveAiMsg.content,
              time: lastEffectiveAiMsg.timestamp,
              type: lastEffectiveAiMsg.type,
              isStreaming: lastEffectiveAiMsg.isStreaming,
            }
            : undefined,
        // 新增：会话级元信息
        agentIndex: agentIdx,
        agentName:
            agent?.name?.toLowerCase?.() === "deepseek" ? DEFAULT_TITLE : agent?.name,
        agentPicUrl: agent?.picUrl,
        agentDesc: agent?.desc,
      };
      await saveSummary("normal", "normal", summary);
    }

    console.log("对话状态已保存到本地存储", chatKey);
  } catch (error) {
    console.error("保存对话状态失败:", error);
  }
};

// 从会话模式的子组件返回：退出当前智能体视图，回到初始列表样式
async function handleBackFromChild() {
  if (!sessionMode.value) return;

  // 会话模式：如果当前是聊天态（chat=1），优先通过浏览器历史后退回默认态
  // 这样浏览器后退按钮与左上角返回行为一致
  if (isSessionChatRoute.value) {
    if (window.history.length > 1) {
      router.back();
      return;
    }
    // 极端场景：没有可后退历史（例如直接打开 chat=1 链接）
    await replaceToSessionHistoryRoute();
    await resetToSessionHistoryState();
    return;
  }

  // 已在默认态：确保回到“历史列表样式”
  await resetToSessionHistoryState();
}

function copyText(msg){
  const textToCopy = extractText(msg)
  // ✅ 推荐使用现代浏览器的 Clipboard API
  if (navigator.clipboard && window.isSecureContext) {
    // ✅ 只有在 HTTPS 或 localhost 下，navigator.clipboard 才可用
    navigator.clipboard
        .writeText(textToCopy)
        .then(() => {
          console.log('✅ 剪贴板复制成功 (Clipboard API)')
          showToast('复制成功') // Vant Toast 提示
        })
        .catch((err) => {
          console.error('❌ 剪贴板复制失败 (Clipboard API)', err)
          // 如果 Clipboard API 失败，使用降级方案
          fallbackCopyTextToClipboard(textToCopy)
        })
  } else {
    // 如果不支持 Clipboard API，直接使用降级方案
    console.log('🚀 使用降级复制方案 (execCommand)')
    fallbackCopyTextToClipboard(textToCopy)
  }
}

function extractText(html) {
  if (typeof html !== "string") return "";

  if (html.includes("<") && html.includes(">")) {
    return html.replace(/<[^>]*>/g, "");
  }

  return html;
}
function fallbackCopyTextToClipboard(text) {
  const textArea = document.createElement("textarea");
  textArea.value = text;
  textArea.style.position = "fixed";
  textArea.style.top = "-9999px";
  textArea.style.left = "-9999px";
  document.body.appendChild(textArea);
  // 选中文本
  textArea.focus();
  textArea.select();

  try {
    const successful = document.execCommand("copy");
    if (!successful) {
      console.error(
          'document.execCommand("copy")失败',
          document.execCommand("copy")
      );
      throw new Error("复制命令执行失败");
    }
    showToast("复制成功")
  } catch (err) {
    showToast("复制失败，请稍后再试")
    console.error("document.execCommand复制失败:", err);
  } finally {
    document.body.removeChild(textArea);
  }
}

function registerStorageListener(key, handler) {
  if (!key) return;
  storageKeys.push(key);
  window.WeSpaceSDK.onStorageChange(key, handler);
}

// stripHtml 函数已从 utils.js 导入
</script>
<style scoped></style>
