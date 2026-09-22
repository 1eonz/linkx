<template>
  <view class="chat-list-wrap">
    <view class="chat-list" ref="chatListRef" @scroll="handleScroll" @touchstart="handleTouchStart" @touchmove="handleTouchMove" @touchend="handleTouchEnd">
      <view class="message-group" v-for="(msg, index) in messages" :key="index" :id="'msg-' + index">
        <MsgItemUser
          :msg="msg"
          :content="msg.queryContent"
          :user-avatar="userAvatar"
          :user-info="userInfo"
          @remind-deal="handleRemindDeal"
        />
        <MsgItemAi
          v-if="msg.responseContent"
          :msg="msg"
          :content="msg.responseContent"
          :index="index"
          :agents="agents"
          @resume="(index) => handleResume(index)"
        />
      </view>
    </view>
    <MsgAnchor
      target=".chat-list"
      :scroll-to="pendingApprovalIndex >= 0 ? `#msg-${pendingApprovalIndex}` : ''"
      :visible="anchorVisible"
      @click="handleAnchorClick"
      @scroll-end="handleScrollEnd"
    />
  </view>
</template>

<script setup>
import { ref, unref, watch, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { showCustomToast } from '@/utils/toast'
import { getRecordsByAgent, getAgents, updateApprovalWsSession, getUserInfoByIdCard } from '@/common/api/ai.js'
import { useStreamOutput } from '../../composables/useStreamOutput.js'
import { useApprovalWebSocket } from '../../composables/useApprovalWebSocket.js'
import { useReachTop } from '../../composables/useReachTop.js'
import AiAgentChatPausePosition from '../../composables/AiAgentChatPausePosition.js'
import { useCommunicationStore } from "@/stores/communication.js";
import { queryUserByIdCard } from '@/common/api/h5.js';

import MsgItemAi from './MsgItemAi.vue'
import MsgItemUser from './MsgItemUser.vue'
import MsgAnchor from './MsgAnchor.vue'
import userAvatarDefault from "@/static/ai/user_avatar.png";
import { formatMsgTime } from '@/utils/aiAssistantUtils.js';


// 消息 = 历史消息 + 临时消息
const props = defineProps({
  currAgent: {
    type: Object,
    default: () => ({})
  },
  isSessionMode: {
    type: Boolean,
    default: false
  },
  // 初始化加载的历史消息
  initMsgList: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['stateChange'])

// 消息列表统一管理：历史消息和新消息都走同一个数组。
// 这样 useStreamOutput 操作的 messageIndex 和 UI 渲染的 index 一致。
const messages = ref(Array.isArray(props.initMsgList) ? [...props.initMsgList] : [])


const agents = ref([])
const chatListRef = ref(null)
const anchorVisible = ref(false)
const pendingApprovalIndex = ref(-1)

// 清空消息列表
function clearMessages() {
  messages.value = []
  resetStreamState()
}

// 用户滚动检测
const userScrolled = ref(false)
const SCROLL_THRESHOLD = 5 // 滚动阈值，超过这个值认为是用户手动滚动
const TIME_THRESHOLD = 100 // 时间阈值，防止频繁触发
let lastScrollTime = 0
let isAutoScrolling = false

// ===== 向上分页：使用 useReachTop composable（参考 web 端 useReachTop hooks） =====
// reach-top 回调（由父组件注入）
let onReachTopHandler = null
const {
  resetReachTop,
  setLoading: setReachTopLoading,
  setHasMore: setHasMoreHistory,
} = useReachTop('.chat-list', (payload) => {
  // 转发给父组件注入的 handler
  if (typeof onReachTopHandler === 'function') {
    return onReachTopHandler(payload)
  }
})
// 父组件注入 reach-top 回调
function setReachTopHandler(fn) { onReachTopHandler = fn }
// 在现有消息列表前面插入更早的历史消息，并保持滚动位置
function prependMessages(newItems) {
  if (!newItems || newItems.length === 0) {
    resetReachTop()
    return
  }
  const chatContainer = document.querySelector('.chat-list')
  const prevScrollHeight = chatContainer?.scrollHeight || 0
  const prevScrollTop = chatContainer?.scrollTop || 0
  messages.value = [...newItems, ...messages.value]
  nextTick(() => {
    if (chatContainer) {
      const newScrollHeight = chatContainer.scrollHeight
      chatContainer.scrollTop = newScrollHeight - prevScrollHeight + prevScrollTop
    }
    resetReachTop()
  })
}

// 用户信息
const communicationStore = useCommunicationStore();
const userInfo = ref({
  username: "",
  userid: null,
  idCard: null,
  thumbAvatar: "",
  userDepartments: [],
});

// 用户头像（优先使用用户头像，否则用默认头像）
const userAvatar = computed(() => {
  return userInfo.value?.thumbAvatar || userAvatarDefault;
})

const {
  sendQuestion,
  resumeStream,
  pauseStream,
  autoContinue,
  handleUrgeApproval,
  cleanup,
  startApprovalStream,
  streamState,
  isLoading,
  isReceiving,
  resetStreamState,
} = useStreamOutput(messages)

// ws相关事件处理
const ws = useApprovalWebSocket()
const wsSessionId = ref('')
let wsSessionIdResolve = null
const wsSessionIdPromise = new Promise(resolve => { wsSessionIdResolve = resolve })

// 监听ws事件
ws.on('ai-agent-ws-connected', handleWsConnected)
ws.on('ai-agent-approval-changed', 'approval_created', handleApprovalCreated)
ws.on('ai-agent-approval-changed', 'approval_changed', handleApprovalChanged)
ws.on('error', handleWsError)
ws.on('close', handleWsClose)

// 处理ws连接成功事件
function handleWsConnected(content) {
  console.log('审批状态连接----------------handleWsConnected', content)
  if (content.wsSessionId) {
    wsSessionId.value = content.wsSessionId
    wsSessionIdResolve?.(content.wsSessionId)
    syncApprovalWsSession()
  }
}

async function syncApprovalWsSession() {
  if (!wsSessionId.value || !userInfo.value?.idCard) return
  if (!messages.value.length) return
  try {
    const params = {
      userID: userInfo.value.idCard,
      wsSessionId: wsSessionId.value,
    }
    if (props.isSessionMode) {
      params.agentId = props.currAgent?.index || 1
      params.agentConfigId = props.currAgent?.index || 1
    }
    await updateApprovalWsSession(params)
  } catch (error) {
    console.error('[MessageList] 更新审批wsSessionId失败:', error)
  }
}

// ws连接异常
function handleWsError(error) {
  console.warn('[MessageList] WS连接异常', error)
  wsSessionIdResolve?.('')
}

// ws连接关闭
function handleWsClose() {
  console.info('[MessageList] WS连接关闭')
  wsSessionIdResolve?.('')
}

// 处理ws审批已创建
async function handleApprovalCreated(parsed) {
  console.log('审批状态创建----------------handleApprovalCreated', parsed)
  if (!parsed.recordId) return

  let messageIndex = -1
  for (let i = messages.value.length - 1; i >= 0; i--) {
    if (String(messages.value[i].taskId) === String(parsed.recordId)) {
      messageIndex = i
      break
    }
  }

  if (messageIndex > -1) {
    messages.value[messageIndex].approvalStatus = parsed.approvalStatus
    messages.value[messageIndex].approveResult = parsed.approveResult
    messages.value[messageIndex].approveNo = parsed.approveNo
    messages.value[messageIndex].approveDetailUrl = parsed.approveDetailUrl
    messages.value[messageIndex].toLeaderUrl = parsed.toLeaderUrl
    messages.value[messageIndex].approveUser = parsed.approveUser
  }

  // 发送审批卡片通知
  const approveUser = parsed.approveUser
  const toLeaderUrl = parsed.toLeaderUrl
  if (approveUser && toLeaderUrl) {
    try {
      const res = await getUserInfoByIdCard(approveUser)
      const assignUserId = res?.id || ''
      if (!assignUserId) {
        console.warn('[MessageList] 未获取到审批人信息，无法发送审批通知')
        return
      }
      const url = toLeaderUrl ? (toLeaderUrl + (toLeaderUrl.includes('?') ? '&' : '?') + 'needHiddenBack=true') : ''
      const appId = Math.random().toString().slice(2, 34).padEnd(32, '0')
      const cardParams = {
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
      console.log('[MessageList] 发送审批卡片参数:', JSON.stringify(cardParams, null, 2))
      const WeSpaceSDK = window.WeSpaceSDK
      if (!WeSpaceSDK) {
        showCustomToast('审批通知发送失败，SDK不可用')
      } else {
        const appResult = await WeSpaceSDK.sendCustomCard(cardParams)
        if (appResult && (appResult.code === 0)) {
          showCustomToast('审批通知发送成功')
        } else {
          showCustomToast('审批通知发送失败')
        }
      }
    } catch (e) {
      console.error('[MessageList] 发送审批卡片失败:', e)
      showCustomToast('审批通知发送失败')
    }
  } else {
    console.warn('[MessageList] 缺少必要参数: approveUser或toLeaderUrl为空')
  }
}

// 处理ws审批状态改变
function handleApprovalChanged(parsed) {
  console.log('审批状态改变-------------------handleApprovalChanged',parsed)
  if (parsed.recordId && (parsed.approvalStatus !== undefined)) {
    // 找到消息索引
    let messageIndex = -1
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (String(messages.value[i].taskId) === String(parsed.recordId)) {
        messageIndex = i
        break
      }
    }

    // 更新消息
    if (messageIndex > -1) {
      messages.value[messageIndex].approvalStatus = parsed.approvalStatus
      messages.value[messageIndex].approveResult = parsed.approveResult
      messages.value[messageIndex].approveNo = parsed.approveNo
      messages.value[messageIndex].approveDetailUrl = parsed.approveDetailUrl
      messages.value[messageIndex].toLeaderUrl = parsed.toLeaderUrl
      messages.value[messageIndex].approveUser = parsed.approveUser
    }

    // 审批完成
    if (parsed.approvalStatus == 2) {
      if (parsed.approveResult == 0) {
        const msg = messages.value[messageIndex]
        // 先问后审模式：审批完成不需要再回答
        if (msg && msg.approvalSubMode === '0') {
          // 先问后答模式：AI已输出回答，审批通过后不需要重新输出
        } else if (msg && msg.responseContent && !msg.isStreaming) {
          // 已有完整回答内容且不在流式中，审批通过后不需要重新输出
        } else {
          const taskId = msg?.taskId || parsed.recordId
          if (taskId) {
            pendingApprovalIndex.value = messageIndex
            anchorVisible.value = true
          }
        }
      } else if (parsed.approveResult == 1) {
        const msg = messages.value[messageIndex]
        if (msg && msg.approvalSubMode === '0') {
          // 先问后答模式：AI已输出回答，审批拒绝不影响回答展示
        } else {
          msg.responseContent = ''
          msg.isStreaming = false
        }
      }
    }
  }
}

const handleRemindDeal = async (approveNo) => {
  await handleUrgeApproval(approveNo)
}

// 获取用户信息
async function getUserInfo() {
  try {
    const res = await communicationStore.getUserInfo();
    if (res) {
      console.info('用户信息', res)
      userInfo.value = {
        ...res,
        username: res.username || "",
        userid: res.userid || null,
        idCard: res.idCard || null,
        thumbAvatar: res.thumbAvatar || "",
        userDepartments: res.userDepartments || [],
      };
    }
    console.log('getUserInfo', userInfo.value)
  } catch (error) {
    console.error(`获取用户信息错误: ${error.message}`);
  }
}

async function loadUserByIdCard() {
  console.log('loadUserByIdCard', userInfo.value)
  try {
    const userRes = await queryUserByIdCard({ idCard: userInfo.value?.idCard || '' })
    if (userRes){
      userInfo.value.directLeaderId = userRes.directLeaderId || ''
    }
  } catch (error) {
    console.log(error)
  }
}

// 加载智能体列表
async function loadAgents() {
  try {
    const agentRes = await getAgents({})
    agents.value = agentRes || []
  } catch (error) {
    console.error(`获取智能体列表错误: ${error.message}`);
  }
}

// 移除自动加载历史消息的监听，由父组件控制何时加载

watch(messages, () => {
  nextTick(() => scrollToBottom())  // 不强制滚动，尊重用户的手动滚动状态
}, { deep: true })

// 监听流式输出状态变化
watch([streamState, isLoading, isReceiving], () => {
  emit('stateChange', {
    isStreaming: streamState.value.isStreaming,
    isPaused: streamState.value.isPaused,
    isDisplaying: !!streamState.value.timer || streamState.value.pendingContent.length > 0,
    isLoading: isLoading.value,
    isReceiving: isReceiving.value,
  })
}, { deep: true })

// 最新打字的消息滚动到底部
function scrollToBottom(force = false) {
  if (userScrolled.value && !force) {
    return
  }
  nextTick(() => {
    setTimeout(() => {
      const el = document.querySelector('.chat-list')
      if (el) {
        isAutoScrolling = true
        el.scrollTop = el.scrollHeight
        setTimeout(() => {
          isAutoScrolling = false
        }, 50)
      }
    }, 100)
  })
}

// 重置用户滚动状态（在发送新消息时调用）
function resetScrollState() {
  userScrolled.value = false
}

function handleResume(index) {
  userScrolled.value = false
  resumeStream(index)
  scrollToBottom()
}

function handleAnchorClick() {
}

function handleScrollEnd() {
  anchorVisible.value = false
  const targetIndex = pendingApprovalIndex.value
  pendingApprovalIndex.value = -1

  if (targetIndex >= 0 && messages.value[targetIndex]) {
    const taskId = messages.value[targetIndex].taskId
    if (taskId) {
      startApprovalStream(taskId, targetIndex)
    }
  }
}

// 处理滚动事件
function handleScroll(event) {
  if (isAutoScrolling) return

  const now = Date.now()
  if (now - lastScrollTime < TIME_THRESHOLD) {
    return
  }
  lastScrollTime = now

  const chatContainer = event.target
  const { scrollTop, scrollHeight, clientHeight } = chatContainer
  const distanceToBottom = scrollHeight - scrollTop - clientHeight

  if (distanceToBottom > SCROLL_THRESHOLD) {
    userScrolled.value = true
  } else {
    userScrolled.value = false
  }
}

// 触摸事件处理（支持移动端）
let isTouchScrolling = false
let touchStartY = 0

function handleTouchStart(event) {
  touchStartY = event.touches[0].clientY
  isTouchScrolling = true
}

function handleTouchMove(event) {
  if (!isTouchScrolling || isAutoScrolling) return

  const touchY = event.touches[0].clientY
  const scrollDirection = touchStartY - touchY

  if (scrollDirection > SCROLL_THRESHOLD) {
    const chatContainer = event.target
    const { scrollTop, scrollHeight, clientHeight } = chatContainer
    const distanceToBottom = scrollHeight - scrollTop - clientHeight

    if (distanceToBottom > SCROLL_THRESHOLD) {
      userScrolled.value = true
    }
  }
}

function handleTouchEnd() {
  isTouchScrolling = false
}

async function getMessages(agentId) {
  try{
    const res = await getRecordsByAgent({
      userId: userInfo?.value?.idCard || '',
      pageNo: 1,
      pageSize: 20,
      agentId: agentId || '',
    })
    // 倒序号展示msgList
    const list = (res?.list || [])?.toReversed()

    messages.value = list.map((record, index) => {
      const isFinished = (
        record.replyPosition == -1
        || record.replyPaused == false
        || record.responseContent.includes('[end]')
      )
      
      // 只有最新的一条消息才设置 isPaused
      const isLastMessage = index === list.length - 1;
      
      const messageItem = {
        taskId: record.id,
        queryContent: record.queryContent,
        responseContent: record.responseContent,
        timestamp: record.latestTime ? new Date(record.latestTime).getTime() : Date.now(),
        replyPosition: record.replyPosition,
        isPaused: isLastMessage && !isFinished,
        approvalRequired: record.approvalRequired,
        approveUrl: record.approveUrl,
        approveDetailUrl: record.approveDetailUrl,
        approvalStatus: record.approvalStatus,
        approveResult: record.approveResult,
        agentId: record.agentId,
        agentConfigId: record.agentConfigId,
        approveNo: '',
        toLeaderUrl: record.toLeaderUrl || '',
        approveUser: record.approveUser || '',
        agentName: record.agentName || '',
        userName: record.userName || '',
        answerTime: formatMsgTime(record.answerTime),
        queryTime: formatMsgTime(record.queryTime),
        isStreaming: false,
      }
      
      if (messageItem.isPaused && messageItem.replyPosition > 0) {
        messageItem.isStreaming = true
      }
      
      return messageItem
    })
  } catch(e) {
    console.error('getMessages error:', e)
  } finally {
    scrollToBottom(true)
    syncApprovalWsSession()
  }
}

async function createQuestion(params) {
  console.log('createQuestion params', params)
  resetScrollState()
  await sendQuestion(params)
  scrollToBottom()
}

function handleVisibilityChange() {
  if (document.visibilityState === 'visible') {
    if (ws.isConnected()) {
      syncApprovalWsSession()
    } else {
      ws.reconnect()
    }
  }
}

onMounted(async () => {
  console.log('chatList: onMounted')
  await Promise.all([getUserInfo(), loadAgents()])
  ws.connect(userInfo.value?.idCard)
  loadUserByIdCard()

  document.addEventListener('visibilitychange', handleVisibilityChange)

  // 对父组件传入的历史消息中标记了 needAutoContinue 的条目自动续流
  nextTick(() => {
    messages.value.forEach((msg, index) => {
      if (msg.needAutoContinue && msg.taskId) {
        msg.needAutoContinue = false
        autoContinue(index, msg.taskId)
      }
    })
  })
})

onUnmounted(() => {
  // 在页面卸载时保存暂停状态
  if (streamState.value.taskId && (streamState.value.isStreaming || streamState.value.isPaused || streamState.value.pendingContent)) {
    const replyPosition = streamState.value.displayedContent.length;
    // 这里只保存真正的手动暂停状态，意外离开时 replyPaused 设为 false
    const replyPaused = streamState.value.isPaused;
    AiAgentChatPausePosition.savePosition(streamState.value.taskId, { replyPaused, replyPosition });
    console.log('MessageList onUnmounted: 保存暂停状态', streamState.value.taskId, replyPosition, replyPaused);
  }
  cleanup()
  ws.cleanup()
  document.removeEventListener('visibilitychange', handleVisibilityChange)
})

defineExpose({
  createQuestion,
  scrollToBottom,
  streamState,
  isLoading,
  isReceiving,
  resumeStream,
  pauseStream,
  getMessages,
  clearMessages,
  wsSessionId,
  wsSessionIdPromise,
  handleResume,
  messages,
  // 向上分页相关
  prependMessages,
  setReachTopHandler,
  setReachTopLoading,
  resetReachTop,
  setHasMoreHistory,
})
</script>

<style scoped lang="scss">
.chat-list-wrap {
  position: relative;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.chat-list {
  flex: 1;
  overflow-y: auto;
}
</style>