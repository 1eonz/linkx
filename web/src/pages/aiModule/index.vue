<template>
  <div class="ai-module" :class="{ 'session-mode': isSessionMode }">
    <!-- 会话模式：左右布局 -->
    <template v-if="isSessionMode">
      <SessionSidebar
        :history-list="historyList"
        :current-session-id="currentSessionId"
        :is-hidden="sidebarHidden"
        @new-chat="handleNewChat"
        @agent-click="handleAgentClick"
        @history-click="handleHistoryClick"
        @delete-history="handleDeleteHistory"
        @toggle-hide="toggleSidebar"
      />
      <div class="session-main" :class="{ 'sidebar-hidden': sidebarHidden }">
        <div class="session-content">
          <!-- 智能体名称标题栏 -->
          <div v-if="!showAgentStore && currentAgentName" class="agent-title-bar">
            <span class="agent-name">{{ currentAgentName }}</span>
          </div>
          <!-- 显示智能体超市 -->
          <div v-if="showAgentStore" class="agent-store-wrapper">
            <ModulesList
              v-model:selectedModules="selectedModules"
              v-model:activeModuleId="activeModuleId"
              @module-click="handleModuleClick"
              :show-as-content="true"
            />
          </div>
          <!-- 显示聊天界面 -->
          <template v-else>
            <!-- ChatList 始终渲染以确保 ref 可用，但根据状态控制显示 -->
            <div class="all-wrap" v-show="!showEmptyHeader">
              <ChatList
                ref="chatListRef"
                v-model:inputValue="inputValue"
                v-model:activeModuleId="activeModuleId"
                :session-mode="true"
                :session-id="currentSessionId"
                :agent-id="currentAgentId"
                :agent-pic-url="currentAgentPicUrl"
                :wsSessionId="wsSessionId"
                @reach-top="handleReachTop"
                @stream-poll="handleStreamPoll"
                @stream-pause="handleStreamPause"
                @stream-complete="handleStreamComplete"
                :webSocketMessage="webSocketMessage"
              />
            </div>
            <!-- 没有消息时：提示文字+输入框居中显示 -->
            <div v-if="showEmptyHeader" class="empty-center-wrapper">
              <div class="empty-tip">👋嗨，今天有什么我可以帮你的吗？</div>
              <div class="footer-input">
                <MessageSender
                  :is-loading="messageLoading"
                  :is-paused="isPaused"
                  :is-streaming="isStreaming"
                  :is-receiving="isReceiving"
                  :is-displaying="isDisplaying"
                  :user-id="userId"
                  :agent-id="currentAgentId"
                  :session-id="currentSessionId"
                  @send="handleSend"
                  @pause="pausedMessage"
                />
              </div>
            </div>
            <!-- 有消息时：底部输入框 -->
            <div v-else class="footer-wrap">
              <MessageSender
                :is-loading="messageLoading"
                :is-paused="isPaused"
                :is-streaming="isStreaming"
                :is-receiving="isReceiving"
                :is-displaying="isDisplaying"
                :user-id="userId"
                :agent-id="currentAgentId"
                @send="handleSend"
                @pause="pausedMessage"
              />
            </div>
          </template>
        </div>
      </div>
    </template>
    <!-- 综合模式：原有布局 -->
    <template v-else>
      <div class="ai-comprehensive">
        <div class="all-wrap">
          <div v-if="showEmptyHeader" class="empty-header">
            <img :src="currentTheme === 'light' ? AiNav : AiNavDark" class="header-img" />
          </div>
          <ChatList
            v-show="!showEmptyHeader"
            ref="chatListRef"
            v-model:inputValue="inputValue"
            v-model:activeModuleId="activeModuleId"
            :wsSessionId="wsSessionId"
            @reach-top="handleReachTop"
            @stream-poll="handleStreamPoll"
            @stream-pause="handleStreamPause"
            @stream-complete="handleStreamComplete"
            :webSocketMessage="webSocketMessage"
          />
        </div>
        <div class="footer-wrap">
          <ModulesList
            v-model:selectedModules="selectedModules"
            v-model:activeModuleId="activeModuleId"
            @module-click="handleModuleClick"
          />
          <div class="footer-input">
            <MessageSender
          :is-loading="messageLoading"
          :is-paused="isPaused"
          :is-streaming="isStreaming"
          :is-receiving="isReceiving"
          :is-displaying="isDisplaying"
          :user-id="userId"
          :agent-id="activeModuleId"
          :session-id="currentSessionId"
          @send="handleSend"
          @pause="pausedMessage"
        />
        </div>
      </div>
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { themeService } from '../../data/useTheme';
import ModulesList from './modulesList.vue';
import ChatList from './chatList.vue';
import SessionSidebar from './SessionSidebar.vue';
import { usePIMStore } from '@/store/modules/pim';
import { generateUUID, checkIsSessionMode, getGlobalsConfigByKey } from '@/utils';
import AiNav from '@/assets/images/ai/ai_nav.png';
import AiNavDark from '@/assets/images/ai/ai_nav_dark.png';
// import Send from '@/assets/images/ai/send.png';
// import SendDark from '@/assets/images/ai/send_dark.png';
// import SendDisabled from '@/assets/images/ai/send_disabled.png';
// import SendDisabledDark from '@/assets/images/ai/send_disabled_dark.png';
// import AiPause from '@/assets/svg/aiPause.svg';
// import AiPauseDisabled from '@/assets/svg/aiPauseDisabled.svg';
// import AiSendPause from '@/assets/svg/ai_send_pause.svg';
import {getAiHistory, getAiHistoryDetail, getDeployConfig, deleteAiHistory, updateApprovalWsSession} from "@/api/ai";
import { close, sendCustomCard, isWebView2Env } from '@/bridge/post.js';
import { getUserInfoByIdCard } from '@/api/ai';
import { ElMessage } from 'element-plus';
import { getCurrentOrganization } from '@/api/statics';
import MessageSender from './MessageSender.vue';
import AiAgentChatPausePosition from './index.js';
import { useLocaAgentId } from "./hooks/useLocaAgentId"
import dateUtil from '@/utils/dateUtil';

const { getLocaAgentId, setLocaAgentId, removeLocaAgentId } = useLocaAgentId()


const pimStore = usePIMStore();
console.log('pimStore.user', pimStore.user);
const userInfo = computed(() => pimStore.user || {});
const userId = computed(() => pimStore.user?.userid || '');

const activeModuleId = ref('');
const selectedModules = ref(<any>[]);
const inputValue = ref('');
const currentTheme = ref('');
const chatListRef = ref();

// 会话模式相关状态
const isSessionMode = ref(false);
const sidebarHidden = ref(false);
const showAgentStore = ref(false);
const currentSessionId = ref<string | null>(null);
const currentAgentId = ref<string>(getLocaAgentId());
const historyList = ref<any[]>([]);
const wsSessionId = ref<string>('');
let groupAiHost: any = ref('');
// 分页相关状态：使用 lastId 锚点分页，避免 pageNo 在新增对话后错位
const lastId = ref<string | number>('');
// 是否还有更早的历史可加载
const hasMoreHistory = ref(true);
/** 历史记录分页每页条数 */
const HISTORY_PAGE_SIZE = 100;
const currentItem = ref<any>(getLocaAgentId() === '' ? null : { agentConfigId: getLocaAgentId(), agentId:  getLocaAgentId() } ); //分页查询记录需要查询的当前这条记录
const historyDetailArr = ref<any[]>([]);
const historyTotal = ref<any>(0);
const webSocketMessage = ref<any>(null)

// 格式化消息时间
function formatMsgTime(val: any): string {
  if (val === null || val === undefined || val === '') return '';
  if (typeof val === 'string' && /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(val)) return val;
  const result = dateUtil.format(val);
  return result || '';
}

// 当前智能体头像（会话模式下，优先取选中智能体，其次取当前会话摘要里的头像）
const currentAgentPicUrl = computed(() => {
  if (!isSessionMode.value) return '';
  // 优先根据当前智能体 ID 在已选智能体列表中查找
  if (currentAgentId.value && currentAgentId.value !== '-1') {
    const fromSelected =
      selectedModules.value.find((m: any) => String(m.index) === currentAgentId.value) || null;
    if (fromSelected?.picUrl) return fromSelected.picUrl;
  }
  // 兜底：根据当前会话 ID 在历史摘要中查找头像
  if (!currentSessionId.value) return '';
  const historyItem = historyList.value.find(
    (h: any) => h.sessionId === currentSessionId.value,
  );
  return historyItem?.agentPicUrl || '';
});

const showEmptyHeader = computed(() => {
  return !chatListRef.value?.messages?.length;
});
const messageLoading = computed(() => {
  return chatListRef.value?.isLoading || false;
});
// 是否暂停，是否正在流，是否正在显示
const isPaused = computed(() => chatListRef.value?.streamState?.isPaused || false);
const isStreaming = computed(() => chatListRef.value?.streamState?.isStreaming || false);
const isDisplaying = computed(() => chatListRef.value?.streamState?.pendingContent || chatListRef.value?.streamState?.timer);
const isReceiving = computed(() => chatListRef.value?.streamState?.isReceiving || false);
// 会话模式下按钮是否禁用（输入为空或正在加载）
// const isSendDisabled = computed(() => {
//   if (isSessionMode.value) {
//     return !inputValue.value?.trim() || messageLoading.value;
//   }
//   return messageLoading.value;
// });

// 获取当前智能体名称
const currentAgentName = computed(() => {
  if (!currentAgentId.value || currentAgentId.value === '-1') {
    return '公安AI助手';
  }
  // 先从 selectedModules 中查找
  const agent = selectedModules.value.find((m: any) => String(m.index) === String(currentAgentId.value));
  if (agent && agent.name) {
    return agent.name;
  }
  // 如果没找到，从历史记录中查找
  const historyItem = historyList.value.find((h: any) => String(h.agentId) === String(currentAgentId.value));
  if (historyItem && historyItem.agentName) {
    return historyItem.agentName;
  }
  return '公安AI助手';
});


// 缓存 WebSocket 连接 URL
let cachedWsUrl = ''
let approvalWebSocket : WebSocket | null = null
let pingTimer: ReturnType<typeof setInterval> | null = null

// 发送 ping 心跳
function sendPing() {
  if (approvalWebSocket && approvalWebSocket.readyState === WebSocket.OPEN) {
    approvalWebSocket.send('ping');
    console.log('send ping');
  }
}

// 初始化 WebSocket 连接（用于接收审批状态变更）
async function initApprovalWebSocket() {
  console.log('[ApprovalWS-web] ===== initApprovalWebSocket 开始 =====')
  try {
    if (!cachedWsUrl) {
      // 这里 idCard 是用户的身份证号，用于唯一标识用户
      // /transfer/ws?idCard=xxxxxxx
      const groupAiHost = window.location.origin + '/linkx/desktop/XA-ics-agent'
      const urlObj = new URL(groupAiHost)
      const protocol = urlObj.protocol === 'https:' ? 'wss:' : 'ws:'
      const wsPath = urlObj.pathname.replace(/\/$/, '') + '/transfer/ws'
      const idCard = userInfo.value?.idCard || ''
      const query = `?idCard=${encodeURIComponent(idCard)}`
      cachedWsUrl = `${protocol}//${urlObj.host}${wsPath}${query}`
      console.log('[ApprovalWS-web] 构建 WebSocket URL:', cachedWsUrl)
    }

    if (approvalWebSocket) {
      const state = approvalWebSocket.readyState
      const stateMap = { 0: 'CONNECTING', 1: 'OPEN', 2: 'CLOSING', 3: 'CLOSED' }
      console.log('[ApprovalWS-web] 已有 WebSocket 实例，readyState:', stateMap[state] || state)
      if (state === WebSocket.OPEN) {
        console.log('[ApprovalWS-web] WebSocket 已连接，无需重新创建')
        return
      }
      console.log('[ApprovalWS-web] WebSocket 未处于 OPEN 状态，将重新创建连接')
      approvalWebSocket = null
    }

    console.log('[ApprovalWS-web] 创建 WebSocket 连接:', cachedWsUrl)
    approvalWebSocket = new WebSocket(cachedWsUrl);

    approvalWebSocket.onopen = () => {
      console.log('[ApprovalWS-web] ===== WebSocket 连接已打开 =====')
      if (pingTimer) {
        clearInterval(pingTimer);
      }
      console.log('[ApprovalWS-web] 启动心跳定时器（10s间隔）')
      pingTimer = setInterval(sendPing, 10000);
    };

    approvalWebSocket.onmessage = async (event) => {
      console.log('[ApprovalWS-web] ===== 收到 WebSocket 消息 =====')
      console.log('[ApprovalWS-web] 原始数据:', event?.data)

      if (event.data === 'pong') {
        console.log('[ApprovalWS-web] 收到 pong 响应')
        return;
      }

      try {
        const data = JSON.parse(event.data || '{}')
        console.log('[ApprovalWS-web] 解析后的 data:', JSON.stringify(data, null, 2))

        const type = data?.type || ''
        console.log('[ApprovalWS-web] 消息类型:', type)

        let content: any = {}
        try {
          content = typeof data?.content === 'string' ? JSON.parse(data.content) : (data?.content || {})
        } catch(e) {
          console.warn('[ApprovalWS-web] 解析 content 失败:', e)
          content = {}
        }
        console.log('[ApprovalWS-web] 解析后的 content:', JSON.stringify(content, null, 2))

        if (type === 'ai-agent-ws-connected') {
          console.log('[ApprovalWS-web] 消息类型: 初始化连接')
          wsSessionId.value = content?.wsSessionId || ''
          console.log('[ApprovalWS-web] wsSessionId 设置为:', wsSessionId.value)
          syncApprovalWsSession()
        }

        if (type === 'ai-agent-approval-changed') {
          console.log('[ApprovalWS-web] 消息类型: 审批状态变更')
          console.log('[ApprovalWS-web] 事件类型:', content?.event)

          if (content.event === 'approval_created') {
            console.log('[ApprovalWS-web] 审批单创建成功，发送通知卡片给领导')
            close()
            const toLeaderUrl = content?.toLeaderUrl || '';
            const approveUser = content?.approveUser || '';
            console.log('[ApprovalWS-web] toLeaderUrl:', toLeaderUrl)
            console.log('[ApprovalWS-web] approveUser:', approveUser)
            console.log('[ApprovalWS-web] recordId:', content?.recordId)
            console.log('[ApprovalWS-web] wsSessionId:', wsSessionId.value)
            if (approveUser && toLeaderUrl) {
              try {
                const userRes = await getUserInfoByIdCard(approveUser)
                const assignUserId = userRes?.data?.id || ''
                if (!assignUserId) {
                  ElMessage.error('未获取到审批人信息，无法发送审批通知')
                  return
                }
                const url = toLeaderUrl ? (toLeaderUrl + (toLeaderUrl.includes('?') ? '&' : '?') + 'needHiddenBack=true') : ''
                const appId = Math.random().toString().slice(2, 34).padEnd(32, '0')
                const cardParams: any = {
                  level: 'blue',
                  title: 'AI助手使用申请',
                  describe: '您有一个问答审批待处理，请尽快处理',
                  jumpType: '4',
                  url: url,
                  appUrl: url,
                  thumb: '',
                  type: '0',
                  isAssignMembers: '1',
                  assignMembers: `${assignUserId}`,
                  id: appId,
                }
                // bspc 环境需要 sessionId 和 sessionType
                if (!isWebView2Env()) {
                  cardParams.sessionId = assignUserId
                  cardParams.sessionType = 1
                  cardParams.thumb = 'data:image/jpeg;base64,/9j/4AAQSkZJRgABAQAAAQABAAD/2wCEAAkGBwgHBgkIBwgKCgkLDRYPDQwMDRsUFRAWIB0iIiAdHx8kKDQsJCYxJx8fLT0tMTU3Ojo6Iys/RD84QzQ5OjcBCgoKDQwNGg8PGjclHyU3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3Nzc3N//AABEIAJIAmAMBIgACEQEDEQH/xAAVAAEBAAAAAAAAAAAAAAAAAAAAB//EABQQAQAAAAAAAAAAAAAAAAAAAAD/xAAUAQEAAAAAAAAAAAAAAAAAAAAA/8QAFBEBAAAAAAAAAAAAAAAAAAAAAP/aAAwDAQACEQMRAD8AuIAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP/Z'
                }
                console.log('[ApprovalWS-web] 发送审批卡片参数:', JSON.stringify(cardParams, null, 2))
                const appResult = await sendCustomCard(cardParams)
                console.log('[ApprovalWS-web] 发送审批卡片结果:appResult', appResult)
                // bspc 返回 errorCode，cspc 返回 code
                const isSuccess = isWebView2Env() ? (appResult && appResult.code === 0) : (appResult && appResult.errorCode === 0)
                if (isSuccess) {
                  ElMessage.success('审批通知发送成功')
                } else {
                  const errorMsg = isWebView2Env() ? (appResult?.msg || '审批通知发送失败') : (appResult?.errorMessage || '审批通知发送失败')
                  ElMessage.error(errorMsg)
                }
              } catch (e) {
                ElMessage.error('审批通知发送失败')
              }
            } else {
              console.warn('[ApprovalWS-web] 缺少必要参数: approveUser或toLeaderUrl为空')
            }
          } else if (content.event === 'approval_changed') {
            console.log('[ApprovalWS-web] 审批状态改变')
            console.log('[ApprovalWS-web] recordId:', content?.recordId)
            console.log('[ApprovalWS-web] approvalStatus:', content?.approvalStatus)
            console.log('[ApprovalWS-web] approveResult:', content?.approveResult)
            console.log('[ApprovalWS-web] approveDetailUrl:', content?.approveDetailUrl)
          }
        }

        console.log('[ApprovalWS-web] 将 content 传递给 ChatList 组件')
        webSocketMessage.value = content;
      } catch(e) {
        console.error('[ApprovalWS-web] 解析 WebSocket 消息失败:', e)
      }

      console.log('[ApprovalWS-web] ===== WebSocket 消息处理完成 =====')
    };

    approvalWebSocket.onerror = (error) => {
      console.error('[ApprovalWS-web] ===== WebSocket 连接错误 =====', error);
      if (pingTimer) {
        clearInterval(pingTimer);
        pingTimer = null;
      }
    };

    approvalWebSocket.onclose = (event) => {
      console.log('[ApprovalWS-web] ===== WebSocket 连接关闭 =====')
      console.log('[ApprovalWS-web] close code:', event?.code, 'reason:', event?.reason)
      approvalWebSocket = null
      if (pingTimer) {
        clearInterval(pingTimer);
        pingTimer = null;
      }
    };
  } catch (error) {
    console.error('[ApprovalWS-web] 初始化失败:', error);
  }
}
// 更新websocket的 wsSessionID
async function syncApprovalWsSession() {
  console.log('[ApprovalWS-web] ===== syncApprovalWsSession 开始 =====')
  if (!wsSessionId.value || !userInfo.value?.idCard) return
  const hasMessages = chatListRef.value?.messages?.length > 0
  if (!hasMessages) return
  try {
    const params: any = {
      userID: userInfo.value.idCard,
      wsSessionId: wsSessionId.value,
    }
    if (isSessionMode.value) {
      const agentId = currentAgentId.value
      params.agentId = (agentId && agentId !== '-1') ? agentId : 1
      params.agentConfigId = (agentId && agentId !== '-1') ? agentId : 1
    }
    await updateApprovalWsSession(params)
  } catch (error) {
    console.error('[ApprovalWS-web] 更新审批wsSessionId失败:', error)
  }
}

async function loadUserByIdCard() {
  try {
    const userRes = await getCurrentOrganization({ idCard: userInfo.value?.idCard || '' })
    if (userRes?.data){
      userInfo.value.directLeaderId = userRes.data.directLeaderId || ''
    }
  } catch (error) {
    console.log(error)
  }
}

function handleWsVisibilityChange() {
  if (document.visibilityState !== 'visible') return
  if (approvalWebSocket && approvalWebSocket.readyState === WebSocket.OPEN) {
    console.log('[ApprovalWS-web] WebSocket 已连接，无需重新创建')
    syncApprovalWsSession()
  } else {
    initApprovalWebSocket()
  }
}

onMounted(async () => {

  loadUserByIdCard()

  const response = await getDeployConfig();


  groupAiHost.value = response.data?.groupAiHost;
  initApprovalWebSocket()

  document.addEventListener('visibilitychange', handleWsVisibilityChange)

  getTheme();
  await checkSessionMode();
  if (isSessionMode.value) {
    loadHistoryList();
  } else {
    queryAiHistoryFromServe('');
  }
});

onUnmounted(() => {
  console.log('[ApprovalWS-web] ===== onUnmounted 组件卸载 =====')
  if (pingTimer) {
    clearInterval(pingTimer);
    pingTimer = null;
    console.log('[ApprovalWS-web] 心跳定时器已清除')
  }
  if (approvalWebSocket) {
    console.log('[ApprovalWS-web] 关闭 WebSocket 连接')
    approvalWebSocket.close();
    approvalWebSocket = null;
    cachedWsUrl = '';
  }
  document.removeEventListener('visibilitychange', handleWsVisibilityChange)
})

// userId 就绪后：综合模式从缓存恢复上次选中的智能体（用于重登/刷新后保持选中态）
watch(
  () => userId.value,
  (uid) => {
    if (!uid) return;
    if (isSessionMode.value) return;
    try {
      const cachedActive = getLocaAgentId();
      const cachedSelectedStr = localStorage.getItem(`selectedModules-${uid}`) || '';
      if (cachedSelectedStr && selectedModules.value.length === 0) {
        const cachedSelected = JSON.parse(cachedSelectedStr);
        // bufix: 修复selectedModules重复项问题
        if (Array.isArray(cachedSelected) && cachedSelected.length > 0) {
          const deduped: any[] = [];
          const seen = new Set();
          for (const item of cachedSelected) {
            const key = String(item.index);
            if (!seen.has(key)) {
              seen.add(key);
              deduped.push(item);
            }
          }
          selectedModules.value = deduped.slice(0, 3);
        }
      }
      if (!activeModuleId.value && cachedActive) {
        activeModuleId.value = cachedActive;
      }
    } catch (e) {
      console.warn('综合模式恢复智能体缓存失败（可忽略）', e);
    }
  },
  { immediate: true },
);

watch(
  () => isStreaming.value,
  (newVal, oldVal) => {
    if (isSessionMode.value && oldVal === true && newVal === false) {
      loadHistoryList();
    }
  },
);

// 检查是否为会话模式
async function checkSessionMode() {
  try {
    const data = await getGlobalsConfigByKey();
    console.log(data, 'data-getGlobalsList')
    if (data) {
      isSessionMode.value = checkIsSessionMode(data);
    } else {
      isSessionMode.value = false;
    }
  } catch (error) {
    console.error('获取全局配置失败:', error);
    isSessionMode.value = false;
  }
}

// 加载历史对话列表
async function loadHistoryList() {
  const prefix = '/linkx/desktop/XA-ics-agent';
  const params: any = {
    userId: userInfo.value.idCard || ''
  }
  if (isSessionMode.value && currentAgentId.value) {
    params.agentId = currentAgentId.value;
  }
  const responseData = await getAiHistory(params);

  if(responseData.code == 0){
    const historyData = responseData.data;
    console.log(historyData,'xixixixixixixixi1111')

    // 转换为历史列表格式
    historyList.value = historyData.map((item) => {
      return {
        sessionId: item.latestRecordId,
        agentIndex: item.agentIndex || '',
        agentName: item.agentId === 1 ? '公安AI助手' : item.agentName,
        agentPicUrl: item.avatar.startsWith(prefix) ? item.avatar : item.avatar ? `${prefix}${item.avatar}` : '',
        agentDesc: item.desc || '',
        lastMessage: item.latestQueryContent || '暂无消息',
        lastMsgTime: item.latestTime || Date.now(),
        // 添加状态标签所需的信息
        lastAiMessage: item.latestResponseContent,
        lastViewedAiTime: item.latestTime || 0,
        agentId: item.agentId,
        agentConfigId: item.agentConfigId,
        latestReplyPaused: item.latestReplyPaused,
        latestReplyPosition: item.latestReplyPosition,
      };
    });
  } else {
    console.log('加载历史对话列表失败:', responseData.msg);
  }









  // try {
  //   const summaryKey = `chatSummary_${userId.value}_session`;
  //   const summaryData = localStorage.getItem(summaryKey);
  //   if (summaryData) {
  //     const summary = JSON.parse(decodeURIComponent(escape(summaryData)));
  //     // 迁移/补齐：老数据可能没有 lastUserMessage，这里尝试从 chatHistory 中推导一次并写回 summary
  //     let summaryChanged = false;
  //     const tryGetLastUserFromHistory = (sessionId: string, agentIndex?: string) => {
  //       try {
  //         const agentId = agentIndex || '-1';
  //         const historyKey = `chatHistory_${sessionId}_${userId.value}_${agentId}`;
  //         const originData = localStorage.getItem(historyKey);
  //         if (!originData) return null;
  //         const parsed = JSON.parse(decodeURIComponent(escape(originData)));
  //         const msgs = parsed?.messages || [];
  //         for (let i = msgs.length - 1; i >= 0; i -= 1) {
  //           const m = msgs[i];
  //           if (m?.type === 'user' && m?.content) return m;
  //         }
  //       } catch (e) {
  //         // ignore
  //       }
  //       return null;
  //     };
  //     // 转换为历史列表格式
  //     historyList.value = Object.keys(summary).map((key) => {
  //       const item = summary[key];
  //       // sidebar 预览：优先展示最后一条 user 提问
  //       const lastUserMessage =
  //         item.lastUserMessage ||
  //         (item.lastMessage?.type === 'user' ? item.lastMessage : null) ||
  //         tryGetLastUserFromHistory(key, item.agentIndex);
  //       if (lastUserMessage && !item.lastUserMessage) {
  //         item.lastUserMessage = lastUserMessage;
  //         summaryChanged = true;
  //       }
  //
  //       // 尝试从 chatHistory 中获取最新的 AI 消息状态（用于判断 isStreaming）
  //       const tryGetLatestAiMessageFromHistory = (sessionId: string, agentIndex?: string) => {
  //         try {
  //           const agentId = agentIndex || '-1';
  //           const historyKey = `chatHistory_${sessionId}_${userId.value}_${agentId}`;
  //           const originData = localStorage.getItem(historyKey);
  //           if (!originData) return null;
  //           const parsed = JSON.parse(decodeURIComponent(escape(originData)));
  //           const msgs = parsed?.messages || [];
  //           const streamState = parsed?.streamState || {};
  //           // 查找最后一条 AI 消息
  //           for (let i = msgs.length - 1; i >= 0; i -= 1) {
  //             const m = msgs[i];
  //             if (m?.type === 'ai') {
  //               // 优先信任 chatHistory 里保存的 message.isStreaming（它会在真正完成时被置为 false）
  //               // 只有在 message 缺失 isStreaming 时，才兜底使用 streamState.isStreaming
  //               const msgIsStreaming =
  //                 m.isStreaming === undefined ? !!streamState.isStreaming : !!m.isStreaming;
  //               return {
  //                 ...m,
  //                 isStreaming: msgIsStreaming,
  //                 timestamp: m.timestamp || m.time,
  //               };
  //             }
  //           }
  //         } catch (e) {
  //           // ignore
  //         }
  //         return null;
  //       };
  //
  //       // 优先使用 chatHistory 中的最新状态，如果没有则使用摘要中的
  //       const latestAiMessage = tryGetLatestAiMessageFromHistory(key, item.agentIndex) || item.lastAiMessage;
  //
  //       return {
  //         sessionId: key,
  //         agentIndex: item.agentIndex || '',
  //         agentName: item.agentName || '',
  //         agentPicUrl: item.agentPicUrl || '',
  //         agentDesc: item.agentDesc || '',
  //         lastMessage: lastUserMessage?.content || '暂无消息',
  //         lastMsgTime: item.lastMsgTime || Date.now(),
  //         // 添加状态标签所需的信息
  //         lastAiMessage: latestAiMessage,
  //         lastViewedAiTime: item.lastViewedAiTime || 0,
  //       };
  //     });
  //     // 按时间倒序排列
  //     historyList.value.sort((a, b) => b.lastMsgTime - a.lastMsgTime);
  //     // 写回迁移后的 summary（只在本次确实补齐字段时写一次）
  //     if (summaryChanged) {
  //       const escaped = unescape(encodeURIComponent(JSON.stringify(summary)));
  //       localStorage.setItem(summaryKey, escaped);
  //     }
  //   }
  // } catch (error) {
  //   console.error('加载历史对话列表失败:', error);
  // }
}

// 保存摘要
function saveSummary(sessionId: string, agentInfo: any, lastMessage: any, lastUserMessage?: any) {
  if (!userId.value) return;
  try {
    const summaryKey = `chatSummary_${userId.value}_session`;
    const summaryData = localStorage.getItem(summaryKey);
    const summary = summaryData ? JSON.parse(decodeURIComponent(escape(summaryData))) : {};

    summary[sessionId] = {
      ...summary[sessionId], // 保留已有字段（如 lastViewedAiTime）
      lastMessage: lastMessage,
      lastAiMessage: lastMessage?.type === 'ai' ? lastMessage : summary[sessionId]?.lastAiMessage,
      lastUserMessage:
        lastUserMessage?.type === 'user'
          ? lastUserMessage
          : summary[sessionId]?.lastUserMessage,
      lastMsgTime: lastMessage?.timestamp || Date.now(),
      agentIndex: agentInfo.index || '',
      agentName: agentInfo.name || '',
      agentPicUrl: agentInfo.picUrl || '',
      agentDesc: agentInfo.desc || '',
    };

    const escaped = unescape(encodeURIComponent(JSON.stringify(summary)));
    localStorage.setItem(summaryKey, escaped);
  } catch (error) {
    console.error('保存摘要失败:', error);
  }
}

function handleModuleClick(item) {
  console.info('[ModuleClick] ===== 选择智能体 =====')
  console.info('[ModuleClick] item 完整结构:', JSON.parse(JSON.stringify(item)))
  console.info('[ModuleClick] item.index:', item.index, '| item.agentConfigId:', item.agentConfigId, '| item.name:', item.name)
  console.info('[ModuleClick] isSessionMode:', isSessionMode.value, '| showAgentStore:', showAgentStore.value)

  activeModuleId.value = String(item.index || '');
  if (isSessionMode.value && showAgentStore.value) {
    // 会话模式下选择智能体后，准备新对话
    // 注意：会话ID统一在首次发送消息时创建，避免重复创建或丢失
    currentSessionId.value = null; // 清空会话ID，等待首次发送消息时创建
    currentAgentId.value = String(item.index || '');
    activeModuleId.value = String(item.index || '');
    showAgentStore.value = false;
    // 设置当前智能体信息，用于查询该智能体的历史记录
    // 智能体超市的 item 没有 agentConfigId，使用 item.index（后端数据中 agentId 和 agentConfigId 一致）
    currentItem.value = {
      agentId: String(item.index || ''),
      agentConfigId: String(item.index || ''),
    };
    setLocaAgentId(item.index)
    console.info('[ModuleClick] 设置后 → currentSessionId:', currentSessionId.value, '| currentAgentId:', currentAgentId.value, '| activeModuleId:', activeModuleId.value)
    console.info('[ModuleClick] currentItem:', JSON.parse(JSON.stringify(currentItem.value)))
    loadHistoryList();
    queryAiHistoryFromServe('');
  } else if (!isSessionMode.value) {
    // 综合模式下切换智能体，重新加载全部历史记录
    currentItem.value = {
      agentId: '',
      agentConfigId: '',
    };
    removeLocaAgentId()
    queryAiHistoryFromServe('');
  }
}
// 处理发送消息
function handleSend(contentOption) {

  savePosition();

  let { content, fileIds } = contentOption
  if (content || fileIds) {
    // 发送文本消息
    sendMessage(contentOption);
  }
}
// 发送
async function sendMessage(contentOption) {
  if (!chatListRef.value || !chatListRef.value.sendMessageFunc) return;

  console.info('[SendMessage] ===== 发送消息 =====')
  console.info('[SendMessage] 发送前 → currentSessionId:', currentSessionId.value, '| currentAgentId:', currentAgentId.value, '| activeModuleId:', activeModuleId.value)

  // 会话模式下，如果没有会话ID，创建新会话
  if (isSessionMode.value && !currentSessionId.value) {
    currentSessionId.value = generateUUID();
    console.info('[SendMessage] 创建新 sessionId:', currentSessionId.value)
  }
  // 会话模式下，如果没有智能体ID，设置为 -1（表示未选择智能体）
  if (isSessionMode.value && !currentAgentId.value) {
    currentAgentId.value = '-1';
  }

  console.info('[SendMessage] 发送时 → currentSessionId:', currentSessionId.value, '| currentAgentId:', currentAgentId.value, '| activeModuleId:', activeModuleId.value)

  // 等待一帧，确保 ChatList 收到最新的 sessionId / agentId props
  await nextTick();

  chatListRef.value.sendMessageFunc(contentOption);

  if (isSessionMode.value) {
    loadHistoryList();
  }
}
// 暂停
function pausedMessage() {
  // if ((isStreaming.value || isDisplaying.value) && !isPaused.value) {
  //   if (chatListRef.value && chatListRef.value.pauseStream) {
  //     chatListRef.value.pauseStream();
  //   }
  // }

  if (chatListRef.value && chatListRef.value.pauseStream) {
      chatListRef.value.pauseStream();
    }
}

// 监听消息变化，更新摘要
watch(
  () => chatListRef.value?.messages,
  (messages) => {
    if (isSessionMode.value && currentSessionId.value && messages && messages.length > 0) {
      const lastMessage = messages[messages.length - 1];
      // 取最后一条 user 消息（通常为倒数第 2 条；兜底向前找一次）
      let lastUserMessage: any = null;
      if (lastMessage?.type === 'user') {
        lastUserMessage = lastMessage;
      } else {
        const prev = messages[messages.length - 2];
        if (prev?.type === 'user') {
          lastUserMessage = prev;
        } else {
          for (let i = messages.length - 1; i >= 0; i -= 1) {
            const m = messages[i];
            if (m?.type === 'user') {
              lastUserMessage = m;
              break;
            }
          }
        }
      }
      // 获取智能体信息
      let agentInfo: any = {};
      if (currentAgentId.value && currentAgentId.value !== '-1') {
        agentInfo = selectedModules.value.find((m: any) => m.index === currentAgentId.value) || {};
      }
      // 如果没有找到，尝试从历史记录中获取
      if (!agentInfo.index && currentAgentId.value && currentAgentId.value !== '-1') {
        const historyItem = historyList.value.find((h: any) => h.agentIndex === currentAgentId.value);
        if (historyItem) {
          agentInfo = {
            index: historyItem.agentIndex,
            name: historyItem.agentName,
            picUrl: historyItem.agentPicUrl,
            desc: historyItem.agentDesc,
          };
        }
      }
      // 如果 agentId 为 -1（未选择智能体），设置默认信息
      if (currentAgentId.value === '-1' || (!agentInfo.index && currentAgentId.value === '-1')) {
        agentInfo = {
          index: '-1',
          name: '未选择智能体',
          picUrl: '',
          desc: '',
        };
      }
      // 确保 agentIndex 有值（使用 currentAgentId 作为兜底）
      if (!agentInfo.index && currentAgentId.value) {
        agentInfo.index = currentAgentId.value;
      }
      saveSummary(currentSessionId.value, agentInfo, lastMessage, lastUserMessage);
    }
  },
  { deep: true }
);

// function getImageSrc() {
//   // 会话模式下使用特殊的暂停图标
//   if (isSessionMode.value) {
//     if (isSendDisabled.value) {
//       return AiPauseDisabled;
//     }
//     return AiPause;
//   }
//   // 综合模式使用原有的发送图标
//   if (currentTheme.value === 'light') {
//     if (messageLoading.value) {
//       return SendDisabled;
//     }
//     return Send;
//   } else {
//     if (messageLoading.value) {
//       return SendDisabledDark;
//     }
//     return SendDark;
//   }
// }


function getTheme() {
  currentTheme.value = themeService.getCurrentTheme();
  themeService.onThemeChange((theme) => {
    currentTheme.value = theme;
  });
}

// 会话模式相关方法
function toggleSidebar() {
  sidebarHidden.value = !sidebarHidden.value;
}

function handleNewChat() {

  savePosition();

  // 清理当前会话的流式状态，防止旧定时器继续运行
  if (chatListRef.value?.resetStreamState) {
    chatListRef.value.resetStreamState();
  }

  currentSessionId.value = generateUUID();
  const lcalAgentId = getLocaAgentId();
  currentAgentId.value = lcalAgentId || '';
  activeModuleId.value = lcalAgentId || '';
  showAgentStore.value = false;
  if (chatListRef.value) {
    chatListRef.value.messages = [];
  }
}

function handleAgentClick() {

  savePosition();

  // 会话模式下，每次点击智能体按钮都清空之前的选择，让用户重新选择
  if (isSessionMode.value) {
    currentSessionId.value = '';
    currentAgentId.value = '';
    activeModuleId.value = '';
  }

  // 清理当前会话的流式状态
  if (chatListRef.value?.resetStreamState) {
    chatListRef.value.resetStreamState();
  }

  setTimeout(() => {
    if (chatListRef.value) {
      chatListRef.value.messages = [];
    }
    showAgentStore.value = true;
  }, 0);
}

// 保存恢复位置
function savePosition () {
  try {
    const oldStreamState = JSON.parse(JSON.stringify(chatListRef.value.streamState)) || {};
    const taskId = oldStreamState.taskId || '';
    const replyPosition = oldStreamState.displayedContent?.length;

    const isStreaming = oldStreamState.isStreaming || false;

    // 判断当前是否处于流式回复中
    if (isStreaming) {
      AiAgentChatPausePosition.savePosition(taskId, { replyPaused: false, replyPosition });
    }
  } catch (error) {
    console.error('保存恢复位置失败:', error);
  }
}

async function handleHistoryClick(item: any) {
  console.info('[HistoryClick] ===== 选择历史记录 =====')
  console.info('[HistoryClick] item 完整结构:', JSON.parse(JSON.stringify(item)))
  console.info('[HistoryClick] item.sessionId:', item.sessionId, '| item.agentIndex:', item.agentIndex, '| item.agentId:', item.agentId, '| item.agentConfigId:', item.agentConfigId, '| item.agentName:', item.agentName)

  // 使用展开运算符创建新对象，避免Proxy引用问题
  currentItem.value = { ...item };
  setLocaAgentId(item.index)
  queryAiHistoryFromServe('');

  // 切换会话时，关闭智能体超市, 切换至聊天界面
   showAgentStore.value = false;

  savePosition();



  // 清理当前历史会话的流式状态

  // 切换前先保存当前会话状态，防止数据丢失
  if (currentSessionId.value && chatListRef.value) {
    if (chatListRef.value.saveConversationState) {
      chatListRef.value.saveConversationState();
    }
  }

  // 清理当前会话的流式状态，防止定时器继续写入新会话
  if (chatListRef.value?.resetStreamState) {
    chatListRef.value.resetStreamState();
  }

  // 更新当前会话和智能体状态
  currentSessionId.value = item.sessionId;
  // 优先使用 agentId（后端返回的智能体ID），agentIndex 在当前数据中为空
  const effectiveAgentId = item.agentId || item.agentIndex || '-1';
  currentAgentId.value = String(effectiveAgentId);
  // 只有当有效智能体ID不为 -1 时，才设置 activeModuleId（发送消息时使用的智能体ID）
  activeModuleId.value = effectiveAgentId && String(effectiveAgentId) !== '-1' ? String(effectiveAgentId) : '';

  console.info('[HistoryClick] 设置后 → currentSessionId:', currentSessionId.value, '| currentAgentId:', currentAgentId.value, '| activeModuleId:', activeModuleId.value)
  console.info('[HistoryClick] currentItem:', JSON.parse(JSON.stringify(currentItem.value)))

  // 如果历史记录中有智能体信息，确保它存在于 selectedModules 中，以便显示名称
  if (effectiveAgentId && String(effectiveAgentId) !== '-1' && item.agentName) {
    const existingIndex = selectedModules.value.findIndex((m: any) => String(m.index) === String(effectiveAgentId));
    if (existingIndex === -1) {
      const agentInfo = {
        index: String(effectiveAgentId),
        name: item.agentName,
        picUrl: item.agentPicUrl || '',
        desc: item.agentDesc || '',
      };
      selectedModules.value.unshift(agentInfo);
      // 限制最多3个
      if (selectedModules.value.length > 3) {
        selectedModules.value.splice(3);
      }
    }
  }

  // 切换会话时，关闭智能体选择器
  showAgentStore.value = false;

  loadHistoryList();
  await queryAiHistoryFromServe('');
}

async function queryAiHistoryFromServe(type:string, scrollRestore?: { prevScrollHeight: number; prevScrollTop: number }){
  if(type != 'loadMore'){
    lastId.value = '';
    hasMoreHistory.value = true;
  }
  const params = {
    userId: userInfo.value.idCard || '',  //测试数据6666
    pageNo: 1,
    pageSize: HISTORY_PAGE_SIZE,
    agentId: isSessionMode.value ? currentItem.value.agentId : '',
    agentConfigId: isSessionMode.value ? currentItem.value.agentConfigId : '',
    lastId: type === 'loadMore' ? lastId.value : ''
  }
  console.info('[QueryHistory] ===== 查询历史消息 =====')
  console.info('[QueryHistory] 查询参数:', JSON.parse(JSON.stringify(params)))
  console.info('[QueryHistory] isSessionMode:', isSessionMode.value, '| currentItem:', JSON.parse(JSON.stringify(currentItem.value || {})))
  const responseData = await getAiHistoryDetail(params);
  console.log(responseData,'dddddddddddddddddd')
  //恢复历史对话
  if(responseData.code == 0){
    historyTotal.value = responseData.data.total;
    if(responseData.data && responseData.data.list && responseData.data.list.length > 0){
      if(type != 'loadMore'){
        historyDetailArr.value = [];
      }
      // loadMore 时收集新加载的（原始）消息，稍后通过 prependMessages 插入到现有列表前面
      const newlyLoadedItems: any[] = [];
      responseData.data.list.forEach((item: any) => {
        let approveText: any = '';
        if(item.approvalStatus == '0'){
          approveText = '无需审核';
        } else if(item.approvalStatus == '1') {
          approveText = '待审批';
        } else if(item.approvalStatus == '3') {
          approveText = '待建单';
        } else if(item.approvalStatus == '2'){
          if(item.approveResult == 0){
            approveText = '审批通过';
          } else {
            approveText = '审批拒绝';
          }
        }
        let userObject: any = {
          type: 'user',
          content: item.queryContent,
          timestamp: item.latestTime || Date.now(),
          userid: userId.value,
          taskId: item.id,
          approvalStatus: item.approvalStatus || '',
          approveResult: item.approveResult,
          approveStatusName: approveText,
          approveNo: item.approveNo || '',
          approveUrl: item.approveUrl || '',
          approveDetailUrl: item.approveDetailUrl || '',
          toLeaderUrl: item.toLeaderUrl || '',
          approveUser: item.approveUser || '',
          attachement: item.attachement || '',
          attachement_path: item.attachement_path,
          userName: item.userName || '',
          queryTime: formatMsgTime(item.queryTime),
        }
        let aiContent = item.responseContent || '';
        let isErrorContent = false;
        // 只有纯 [ERROR] 才视为错误，包含其他内容则原样展示
        if (aiContent.trim() === '[ERROR]') {
          aiContent = '业务繁忙，请稍后再试';
          isErrorContent = true;
        }
        let aiObject: any = { //答
          type: 'ai',
          content: aiContent,
          timestamp: item.latestTime || Date.now(),
          userid: userId.value,
          isStreaming: true,
          taskId: item.id,
          isErrorContent: isErrorContent,
          agentConfigId: item.agentConfigId,
          agentName: item.agentName || '',
          answerTime: formatMsgTime(item.answerTime),
        }
        // 倒叙展示
        historyDetailArr.value.unshift(aiObject);
        historyDetailArr.value.unshift(userObject);
        // loadMore 时记录新加载的消息（按时间从旧到新排序）
        if (type === 'loadMore') {
          newlyLoadedItems.unshift(aiObject);
          newlyLoadedItems.unshift(userObject);
        }
        // 更新 lastId 为当前最旧记录的 id（list 最后一项是最旧的）
        lastId.value = item.id;
      });
      // 根据本页返回数量判断是否还有更早的历史可加载
      if (responseData.data.list.length < HISTORY_PAGE_SIZE) {
        hasMoreHistory.value = false;
      }
      if (type === 'loadMore') {
        // 上滑到顶部加载更多：仅 prepend 新加载的消息到现有列表前面，并保持滚动位置
        if (chatListRef.value?.prependMessages) {
          chatListRef.value.prependMessages(newlyLoadedItems);
        }
        // 等待渲染后恢复滚动位置，避免视觉跳动
        nextTick(() => {
          const chatMessages = document.querySelector('.chat-messages') as HTMLElement | null;
          if (chatMessages && scrollRestore) {
            const newScrollHeight = chatMessages.scrollHeight;
            chatMessages.scrollTop = newScrollHeight - scrollRestore.prevScrollHeight + scrollRestore.prevScrollTop;
          }
          // 恢复 reach-top 触发标记
          if (chatListRef.value?.resetReachTop) {
            chatListRef.value.resetReachTop();
          }
        });
      } else {
        // 等待页面渲染后滚动到底部
        nextTick(() => {
          chatListRef.value.recallConversation(historyDetailArr.value);
        })
      }
    } else {
      // 没有历史记录时，清空消息列表，防止旧消息残留
      if (chatListRef.value) {
        chatListRef.value.messages = [];
      }
    }
  }
  console.info('[QueryHistory] 完成查询历史消息')
  syncApprovalWsSession()
}

async function handleDeleteHistory(item: any) {
  if (!userId.value) return;
  try {
    // 调用后端删除接口
    const idCard = userInfo.value.idCard || '';
    await deleteAiHistory(idCard, {
      agentConfigId: item.agentConfigId,
    });

    // 删除对话历史缓存（如果 agentIndex 为空，使用 -1）
    const agentId = item.agentIndex || '-1';
    const historyKey = `chatHistory_${item.sessionId}_${userId.value}_${agentId}`;
    localStorage.removeItem(historyKey);

    // 删除摘要
    const summaryKey = `chatSummary_${userId.value}_session`;
    const summaryData = localStorage.getItem(summaryKey);
    if (summaryData) {
      const summary = JSON.parse(decodeURIComponent(escape(summaryData)));
      delete summary[item.sessionId];
      const escaped = unescape(encodeURIComponent(JSON.stringify(summary)));
      localStorage.setItem(summaryKey, escaped);
    }

    // 重新加载列表
    loadHistoryList();

    // 如果删除的是当前会话，清空当前会话
    if (currentSessionId.value === item.sessionId) {
      currentSessionId.value = null;
      currentAgentId.value = '';
      if (chatListRef.value) {
        chatListRef.value.messages = [];
      }
    }
  } catch (error) {
    console.error('删除历史对话失败:', error);
  }
}

// 处理上滑到顶部事件，加载更早的历史记录（使用 lastId 锚点分页）
async function handleReachTop(payload: { prevScrollHeight: number; prevScrollTop: number }) {
  console.log('滚动到顶部，加载更早的历史记录');
  // 已无更早的历史时直接重置标记，避免重复触发
  if (!hasMoreHistory.value) {
    console.log('[handleReachTop] 没有更早的历史可加载，跳过');
    if (chatListRef.value?.resetReachTop) {
      chatListRef.value.resetReachTop();
    }
    return;
  }
  // 标记加载中，避免 reach-top 重复触发
  if (chatListRef.value?.setReachTopLoading) {
    chatListRef.value.setReachTopLoading(true);
  }
  try {
    await queryAiHistoryFromServe('loadMore', payload);
  } catch (e) {
    console.error('[handleReachTop] 加载更早历史失败:', e);
    if (chatListRef.value?.resetReachTop) {
      chatListRef.value.resetReachTop();
    }
  }
}

function handleStreamPoll() {
  if (isSessionMode.value) {
    loadHistoryList();
  }
}

function handleStreamPause() {
  if (isSessionMode.value) {
    loadHistoryList();
  }
}

function handleStreamComplete() {
  if (isSessionMode.value) {
    loadHistoryList();
    console.log('更新loadHistoryList更新loadHistoryList更新loadHistoryList更新loadHistoryList更新loadHistoryList')
  }
}

</script>

<style lang="less" scoped>
.ai-module {
  width: 100%;
  height: 100%;

  // 会话模式布局
  &.session-mode {
    display: flex;
    position: relative;

    .session-main {
      flex: 1;
      height: 100%;
      display: flex;
      flex-direction: column;
      transition: margin-left 0.3s ease;
      position: relative;
      padding: 0 24px;
      overflow: hidden;

      &.sidebar-hidden {
        margin-left: 0;
        width: 100%;
      }

      .session-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        width: 100%;
        height: 100%;

        .agent-title-bar {
          display: flex;
          align-items: center;
          justify-content: center;
          padding: 10px 0;

          .agent-name {
            text-align: center;
            color: rgba(0, 0, 0, 1);
            font-size: 16px;
            font-weight: 400;
            line-height: 24px;
            letter-spacing: 0px;
          }
        }

        .agent-store-wrapper {
          flex: 1;
          overflow: hidden;
        }

        // 会话模式下的发送按钮样式
        .send-btn {
          width: 32px;
          height: 32px;
          border-radius: 50px;
        }

        // 空状态居中显示
        .empty-center-wrapper {
          flex: 1;
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
          width: 100%;
          height: 100%;

          .empty-tip {
            text-align: center;
            color: var(--text-color);
            font-size: 20px;
            font-weight: 600;
            line-height: 150%;
            margin-bottom: 40px;
          }

          .footer-input {
            width: 100%;

            .input-wrapper {
              display: flex;
              align-items: center;
                // height: 64px;
              padding: 16px 12px 16px 12px;
              box-sizing: border-box;
              border: 1px solid rgba(0, 0, 0, 0.1);
              border-radius: 10px;
              box-shadow: 0px 10px 20px 0px rgba(0, 0, 0, 0.1);
              background: rgba(255, 255, 255, 1);

              .search-input {
                flex: 1;
                padding: 0 12px 0 0;
                font-size: 16px;
                color: var(--text-color);
                background: transparent;
                border: none;
                outline: none;
                resize: none;

                &::placeholder {
                  color: var(--input-placeholder);
                }
              }

              .send-btn {
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;

                &:hover:not(.disabled) {
                  transform: scale(1.05);
                }

                &.disabled {
                  cursor: not-allowed;
                  opacity: 0.6;
                }

                img {
                  width: 100%;
                  height: 100%;
                }
              }
            }
          }
        }

        // 有消息时的布局
        .all-wrap {
          flex: 1;
          width: 100%;
          overflow: hidden;
          min-height: 0;
        }

        .footer-wrap {
          width: 100%;
          padding: 0 20px 24px 20px;
          box-sizing: border-box;

          .footer-input {
            width: 100%;

            .input-wrapper {
              display: flex;
              align-items: center;
                // height: 64px;
              padding: 16px 12px 16px 12px;
              box-sizing: border-box;
              border: 1px solid rgba(0, 0, 0, 0.1);
                border-radius: 10px;
              box-shadow: 0px 10px 20px 0px rgba(0, 0, 0, 0.1);
              background: rgba(255, 255, 255, 1);

              .search-input {
                flex: 1;
                padding: 0 12px 0 0;
                font-size: 16px;
                color: var(--text-color);
                background: transparent;
                border: none;
                outline: none;
                resize: none;

                &::placeholder {
                  color: var(--input-placeholder);
                }
              }

              .send-btn {
                display: flex;
                align-items: center;
                justify-content: center;
                cursor: pointer;

                &:hover:not(.disabled) {
                  transform: scale(1.05);
                }

                &.disabled {
                  cursor: not-allowed;
                  opacity: 0.6;
                }

                img {
                  width: 100%;
                  height: 100%;
                }
              }
            }
          }
        }
      }
    }
  }
.ai-comprehensive{
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  .all-wrap {
    width: 100%;
    // height: calc(100% - 120px);
    flex: 1;
    overflow: hidden;

    .empty-header {
      width: 100%;
      height: 223px;

      .header-img {
        width: 100%;
        height: 100%;
      }
    }
  }
}

  .footer-wrap {
    width: 100%;
    padding: 0 20px;
    box-sizing: border-box;
    .footer-input {
      width: 100%;
      .input-wrapper {
        display: flex;
        align-items: center;
          // height: 50px;
        padding: 0 12px;
        background: var(--td-input-inner-bg2);
        border: 1px solid var(--td-input-border);
          border-radius: 10px;

        .search-input {
          flex: 1;
          padding: 0 12px;
          font-size: 16px;
          color: var(--text-color);
          background: transparent;
          border: none;
          outline: none;
          resize: none;

          &::placeholder {
            color: var(--input-placeholder);
          }
        }

        .send-btn {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 50px;
          height: 33px;
          cursor: pointer;
          border-radius: 16px;

          .pause-item-wrap {
            background: #5246ff;
            width: 100%;
            height: 33px;
              border-radius: 16px;
            display: flex;
            align-items: center;
            justify-content: center;
            .pause-item {
              background: #ffffff;
              width: 13px;
              height: 13px;
                border-radius: 2px;
            }
          }

          &:hover:not(.disabled .pause-item-wrap) {
            transform: scale(1.05);
          }

          &.disabled {
            cursor: not-allowed;
            opacity: 0.6;
          }

          img {
            width: 100%;
            height: 100%;
          }
        }
      }
    }
  }
}
</style>
