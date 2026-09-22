<template>
  <div class="chat-list">
    <div class="chat-container">
      <div class="chat-messages">
        <div
          v-for="(message, index) in messages"
          :key="message.id"
          :id="'msg-' + index"
          class="message-wrapper"
          :class="{ 'user-message': message.type === 'user', 'ai-message': message.type === 'ai' }"
        >
          <!-- AI消息在左侧 -->
          <div class="message-content ai-content" v-if="message.type === 'ai' && message.content">

            <AiAvatar :agentConfigId="message.agentConfigId" :agentList="agentList" :item="message" />

            <div class="ai-content-main">
              <!-- 名称 + 时间 -->
              <div class="name-time-row">
                <span class="agent-name">{{ message.agentName }}</span>
                <span class="msg-time">{{ message.answerTime }}</span>
              </div>
              <div class="message-bubble ai-bubble">
                <ReplyContent :model="message.content" />
              </div>
            </div>

            <!-- 继续按钮，只在暂停状态且是最新的AI消息时显示  -->
            <div v-if="streamState.isPaused && message.type === 'ai' && message === messages[messages.length - 1]" class="resume-btn-wrapper" >
              <el-button class="resume-btn" type="primary" plain round @click="() => resumeStream()" >继续生成</el-button>
            </div>
          </div>

          <!-- 用户消息在右侧 -->
          <div v-if="message.type === 'user'" class="message-content user-content">
            <div class="user-bubble-wrap">
              <!-- 名称 + 时间 -->
              <div class="name-time-row">
                <span class="msg-time">{{ message.queryTime }}</span>
                <span class="user-name">{{ message.userName }}</span>
              </div>
              <!-- 消息附件 -->
              <Attachement v-if="message.attachement_path" :file-url="message.attachement_path" />
              <div v-if="message.content" class="message-bubble user-bubble" :class="{ 'is-remind-deal': message.isUrge }">
                <div class="message-text">{{ message.content }}</div>
              </div>
              <div class="approval-status-wrap">
                <!-- 待建单 -->
                <div class="approval-status create" v-if="message.approvalStatus === '3'" @click="handleApprovalClick(message, 'create')">
                  <img :src="approvePendingIcon" class="status-icon" />
                  <span>{{ message.approveStatusName }}</span>
                </div>
                <!-- 审批中/催办 -->
                <div class="approval-status-row"  v-else-if="message.approvalStatus === '1'">
                  <div class="approval-status pending" @click="handleApprovalClick(message, 'pending')">
                    <img :src="approvePendingIcon" class="status-icon" />
                    <span>{{ message.approveStatusName }}</span>
                  </div>
                  <div class="approval-status urge" @click="handleUrge(message)">催办</div>
                </div>
                <!-- 审批通过 -->
                <div class="approval-status success" v-else-if="message.approvalStatus === '2' && message.approveResult == 0"  @click="handleApprovalClick(message, 'success')">
                  <img :src="approveSuccessIcon" class="status-icon" />
                  <span>{{ message.approveStatusName }}</span>
                </div>
                <!-- 审批拒绝 -->
                <div class="approval-status reject"  v-else-if="message.approvalStatus === '2' && message.approveResult == 1" @click="handleApprovalClick(message, 'reject')">
                  <img :src="approveRejectedIcon" class="status-icon" />
                  <span>{{ message.approveStatusName }}</span>
                </div>
              </div>
            </div>
            <div class="avatar user-avatar">
              <img :src="userInfo.avatar" class="avatar-img" />
            </div>
          </div>
        </div>

        <!-- 选中文本工具栏（放在滚动容器内部，随内容滚动并被裁切） -->
        <SelectionToolbar
          target-selector=".chat-messages"
          bubble-selector=".ai-bubble, .user-bubble"
          :get-all-text="getAiAllTextByElement"
        />
      </div>
    </div>

    <MsgAnchor
      target=".chat-messages"
      :scroll-to="pendingApprovalIndex >= 0 ? `#msg-${pendingApprovalIndex}` : ''"
      :visible="anchorVisible"
      @click="handleAnchorClick"
      @scroll-end="handleAnchorScrollEnd"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onUnmounted, onMounted, ref, watch, nextTick } from 'vue';
import { usePIMStore } from '@/store/modules/pim';
import { useAiModuleStore } from '@/store/modules/aiModule';
import { newTask, getAnswer } from '@/api/ai';
import { getGlobalsConfigByKey } from '@/utils';
import { useAutoScrollToBottom } from '@/hooks';
import { useReachTop } from './hooks/useReachTop';
import MsgAnchor from './MsgAnchor.vue';
import { getDeployConfig } from '@/api/ai';
import axios from 'axios';
import { openUrl, isWebView2Env, sendCustomCard } from '@/bridge/post.js';
import { ElMessage } from 'element-plus';
import { getUserInfoByIdCard } from '@/api/ai';
import { getCurrentOrganization } from '@/api/statics';
import approveSuccessIcon from '@/assets/svg/approve-success.svg';
import approveRejectedIcon from '@/assets/svg/approve-rejected.svg';
import approvePendingIcon from '@/assets/svg/approve-pending.svg';
import AiAvatar from './AiAvatar.vue';
import { useAgents } from './hooks/useAgents';

import AiAgentChatPausePosition from './index.js';
import ReplyContent from "./ReplyContent.vue";
import SelectionToolbar from "./SelectionToolbar.vue";

import Attachement from "./Attachement/index.vue"
import { marked } from 'marked';
import dateUtil from '@/utils/dateUtil';

// 格式化消息的时间
function formatMsgTime(val: any): string {
  if (val === null || val === undefined || val === '') return '';
  // 已是目标格式直接返回，避免二次解析
  if (typeof val === 'string' && /^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/.test(val)) return val;
  const result = dateUtil.format(val);
  return result || '';
}

const props = defineProps<{
  activeModuleId: string;
  sessionMode?: boolean;
  sessionId?: string | null;
  agentId?: string;
  agentPicUrl?: string;
  wsSessionId?: string;
  webSocketMessage?: object
}>();

const emit = defineEmits<{
  'update:inputValue': [value: string];
  'reach-bottom': [];
  'reach-top': [payload: { prevScrollHeight: number; prevScrollTop: number }];
  'stream-poll': [];
  'stream-pause': [];
  'stream-complete': [];
}>();

const pimStore = usePIMStore();
const aiModuleStore = useAiModuleStore();

const { agentList } = useAgents()

// 内部状态：追踪最新的 sessionId 和 agentId，确保保存到正确的 key
// 解决 props 更新延迟导致 saveConversationState 保存到错误 key 的问题
const internalSessionId = ref<string | null>(null);
const internalAgentId = ref<string>('');
const currentWebsocketMsg = ref<any>(null)
const anchorVisible = ref(false)
const pendingApprovalIndex = ref(-1)

// 监听 props 变化，同步到内部状态
watch(
  () => props.sessionId,
  (newVal) => {
    internalSessionId.value = newVal || null;
  },
  { immediate: true }
);

watch(
  () => props.agentId,
  (newVal) => {
    internalAgentId.value = newVal || '';
  },
  { immediate: true }
);

watch(
  () => props.webSocketMessage,
  (newVal) => {
    console.log('[ChatList-WS] ===== 收到 webSocketMessage 变化 =====')
    console.log('[ChatList-WS] newVal:', JSON.stringify(newVal, null, 2))
    console.log('[ChatList-WS] 当前消息总数:', messages.value.length)

    currentWebsocketMsg.value = newVal;
    if(currentWebsocketMsg.value){
      const recordId = currentWebsocketMsg.value.recordId
      console.log('[ChatList-WS] 查找 recordId:', recordId, '类型:', typeof recordId)

      messages.value.forEach((m: any, i: number) => {
        console.log(`[ChatList-WS]   消息[${i}] taskId:`, m.taskId, '类型:', typeof m.taskId, '是否匹配:', String(m.taskId) === String(recordId))
      })

      const index = messages.value.findIndex((msg: any) => msg.taskId === currentWebsocketMsg.value.recordId);
      console.log('[ChatList-WS] findIndex 结果:', index)

      if (index !== -1) {
        console.log('[ChatList-WS] 找到匹配消息，索引:', index)
        console.log('[ChatList-WS] 更新前消息状态:', {
          approvalStatus: messages.value[index].approvalStatus,
          approveResult: messages.value[index].approveResult,
          approveDetailUrl: messages.value[index].approveDetailUrl,
        })

        // 更新审批字段（保留原始值）
        messages.value[index].approvalStatus = currentWebsocketMsg.value.approvalStatus || '';
        messages.value[index].approveResult = currentWebsocketMsg.value.approveResult;
        messages.value[index].approveNo = currentWebsocketMsg.value.approveNo || '';
        messages.value[index].approveDetailUrl = currentWebsocketMsg.value.approveDetailUrl || '';
        messages.value[index].toLeaderUrl = currentWebsocketMsg.value.toLeaderUrl || '';
        messages.value[index].approveUser = currentWebsocketMsg.value.approveUser || '';

        // 更新审批状态中文名
        const approvalStatus = currentWebsocketMsg.value.approvalStatus
        const approveResult = currentWebsocketMsg.value.approveResult
        console.log('[ChatList-WS] approvalStatus:', approvalStatus, 'approveResult:', approveResult)

        if(approvalStatus == '0'){
          messages.value[index].approveStatusName = '无需审核';
          console.log('[ChatList-WS] 设置为: 无需审核')
        } else if(approvalStatus == '1') {
          messages.value[index].approveStatusName = '待审批';
          console.log('[ChatList-WS] 设置为: 待审批')
        } else if(approvalStatus == '3') {
          messages.value[index].approveStatusName = '待建单';
          console.log('[ChatList-WS] 设置为: 待建单')
        } else if(approvalStatus == '2'){
          if(approveResult == 0){
            messages.value[index].approveStatusName = '审批通过';
            // 先问后审模式：审批完成不需要再回答
            if (messages.value[index].approvalSubMode == '0') {
              console.log('[ChatList-WS] 先问后审：AI已输出回答，审批通过后不需要重新输出')
            } else if (messages.value[index + 1]?.type === 'ai' && messages.value[index + 1]?.content) {
              console.log('[ChatList-WS] 该消息已有AI回答，审批通过后不需要重新输出')
            } else {
              console.log('[ChatList-WS] 设置为: 审批通过，显示锚点')
              const taskId = messages.value[index].taskId || currentWebsocketMsg.value.recordId
              if (taskId) {
                pendingApprovalIndex.value = index
                anchorVisible.value = true
              }
            }
          } else {
            messages.value[index].approveStatusName = '审批拒绝';
            console.log('[ChatList-WS] 设置为: 审批拒绝')
          }
        }

        console.log('[ChatList-WS] 更新后消息状态:', {
          approvalStatus: messages.value[index].approvalStatus,
          approveResult: messages.value[index].approveResult,
          approveStatusName: messages.value[index].approveStatusName,
          approveDetailUrl: messages.value[index].approveDetailUrl,
        })
      } else {
        console.warn('[ChatList-WS] 未找到匹配的消息，recordId:', recordId)
        console.log('[ChatList-WS] 当前所有消息的 taskId:', messages.value.map((m: any) => m.taskId))
      }
    }
    console.log('[ChatList-WS] ===== webSocketMessage 处理完成 =====')
  },
  { deep: true }
);

const isLoading = ref(false);
const isReceiving = ref(false);
const currentTaskId = ref('');
const messages = ref(<any>[]);
const ERROR_MESSAGE = '业务繁忙，请稍后再试';
const ERROR_TIMEOUT = '查询超时';
const SEARCHING_SHOW = '正在查询...';

// 智能体响应超时时间（ms）：读取全局配置 AI_AGENT_RESPONSE_TIMEOUT
// 默认 10s；-1 表示不过期一直等待；> -1 按取值（秒）作为超时时间
async function getAgentResponseTimeout(): Promise<number> {
  const val = Number(await getGlobalsConfigByKey('AI_AGENT_RESPONSE_TIMEOUT'));
  if (Number.isNaN(val)) return 10000;
  return val === -1 ? Infinity : val * 1000;
}

// 流式输出的状态管理
const streamState = ref({
  isStreaming: false, // 是否正在流式输出
  isPaused: false, // 是否暂停
  serverDone: false, // 服务端是否已返回结束（[end]）；可能本地仍在逐字显示 pendingContent
  displayedContent: '', // 当前显示内容
  timer: null as NodeJS.Timeout | null, // 定时器引用
  rawContent: '', // 本地保存的完整内容
  pendingContent: '', // 待显示的内容
  taskId: null as string | null, // 任务标识
});

/**
 * 用于中止 startStreamOutput 的轮询循环：
 * - 每次开始新轮询会生成一个 token
 * - 当需要“终止上一轮生成”（例如暂停后发送新问题）时，自增 token，使旧循环自动退出
 */
const streamLoopToken = ref(0);

const streamMessageIndex = ref(-1);

function getStreamingMessageIndex(): number {
  return streamMessageIndex.value >= 0 ? streamMessageIndex.value : messages.value.length - 1;
}

function cancelStreamLoop() {
  streamLoopToken.value += 1;
}

function findLastAiMessageIndex(): number {
  for (let i = messages.value.length - 1; i >= 0; i -= 1) {
    if (messages.value[i]?.type === 'ai') return i;
  }
  return -1;
}

/**
 * 方案 B：当上一轮处于“暂停生成”时，用户发送新问题前，先就地结束上一轮生成，
 * 避免 saveConversationState 的“兜底补 AI 消息”把暂停内容再次 push 一条。
 */
function finalizePausedStreamBeforeSend() {
  // 1) 中止轮询循环
  cancelStreamLoop();

  // 2) 停止逐字显示定时器
  if (streamState.value.timer) {
    clearInterval(streamState.value.timer);
    streamState.value.timer = null;
  }

  // 3) 将当前已显示内容固化到最后一条 AI 消息，并标记为完成
  const finalText = (streamState.value.displayedContent || streamState.value.rawContent || '').trim();
  if (finalText) {
    const streamIdx = getStreamingMessageIndex();
    if (streamIdx >= 0 && messages.value[streamIdx]?.type === 'ai') {
      messages.value[streamIdx].content = marked.parse(finalText);
      messages.value[streamIdx].isStreaming = false;
    } else {
      const lastAiIndex = findLastAiMessageIndex();
      if (lastAiIndex >= 0) {
        messages.value[lastAiIndex].content = marked.parse(finalText);
        messages.value[lastAiIndex].isStreaming = false;
      }
    }
  }

  // 4) 收尾 streamState（保留 displayedContent 作为已完成结果；清掉 pending）
  streamState.value.isPaused = false;
  streamState.value.isStreaming = false;
  streamState.value.serverDone = true; // 语义：本地已终止，不再继续拉取
  streamState.value.rawContent = streamState.value.displayedContent || streamState.value.rawContent || '';
  streamState.value.pendingContent = '';
  streamState.value.taskId = null;
  isReceiving.value = false;

  // 5) 落盘，保证刷新后也是“已暂停的最终态”
  saveConversationState();
}

// 保存状态节流
let saveThrottleTimer: NodeJS.Timeout | null = null;

// 使用自动滚动 hooks
const { resetReachBottom, forceScrollToBottom } = useAutoScrollToBottom('.chat-messages', () => {
  // 滚动到底部时，向父组件发送事件
  emit('reach-bottom');
});

// 使用"上滑到顶部"hooks，用于触发"加载更早历史"
const { resetReachTop, setLoading: setReachTopLoading } = useReachTop('.chat-messages', (payload) => {
  // 向父组件发送事件，附带当前滚动高度信息，便于加载后保持滚动位置
  emit('reach-top', payload);
});

const userInfo = computed(() => pimStore.user || {});
const userId = computed(() => pimStore.user?.userid || '');

// 页面可见性变化处理
function handleVisibilityChange() {
  // 当页面变为可见时，如果有暂停的流式输出，自动恢复
  if (document.hidden) {
    console.log('离开页面')
    // 页面不可见，保存暂停状态
    if (streamState.value.taskId && (streamState.value.isStreaming || streamState.value.pendingContent)) {
      const replyPosition = streamState.value.displayedContent.length;
      const replyPaused = streamState.value.isPaused || streamState.value.pendingContent.length > 0;
      savePauseState(streamState.value.taskId, replyPaused, replyPosition);
      
      // 更新消息对象的暂停状态和位置信息
      const lastIndex = getStreamingMessageIndex();
      if (lastIndex >= 0 && messages.value[lastIndex]) {
        messages.value[lastIndex].isPaused = replyPaused;
        messages.value[lastIndex].taskId = streamState.value.taskId;
        messages.value[lastIndex].replyPosition = replyPosition;
      }
      
      console.log('离开前保存暂停位置')
      // 保存完整会话状态
      saveConversationState();
    }
  } else {
    // 页面变为可见，检查是否有暂停的消息需要恢复
    const lastMsg = messages.value[messages.value.length - 1];

    // 判断最后一条暂停消息如果是手动暂停的 则不需要恢复
    const replyPaused = AiAgentChatPausePosition.getPosition(lastMsg?.taskId)?.replyPaused
    console.log('恢复前',{
      'lastMsg?.taskId':lastMsg?.taskId,
      'AiAgentChatPausePosition.getPosition(lastMsg?.taskId)': AiAgentChatPausePosition.getPosition(lastMsg?.taskId),
      replyPaused
    })

    if(replyPaused) return;

    if (lastMsg?.type === 'ai' && lastMsg?.isPaused) {
      // 找到最后一条暂停的AI消息并恢复
      resumeStream();
    } else if (streamState.value.isPaused) {
      // 如果streamState标记为暂停但消息没有isPaused标记，也尝试恢复
      resumeStream();
    }
  }
}

onMounted(() => {
  loadUserByIdCard();
  // 预热智能体响应超时配置缓存，避免首次提问时阻塞流式输出
  getAgentResponseTimeout();
  // 会话模式下，只在有明确 sessionId 时才自动恢复
  // 父组件会在 handleHistoryClick 中显式调用 restoreConversation
  if (!props.sessionMode || props.sessionId) {
    restoreConversation();
  }

  // 监听页面可见性变化
  document.addEventListener('visibilitychange', handleVisibilityChange);
});

onUnmounted(() => {
  // 清理节流定时器，但先立即保存最新状态
  if (saveThrottleTimer) {
    clearTimeout(saveThrottleTimer);
    saveThrottleTimer = null;
  }
  
  // 页面卸载时保存暂停状态到后端
  if (streamState.value.taskId && (streamState.value.isStreaming || streamState.value.isPaused || streamState.value.pendingContent)) {
    const replyPosition = streamState.value.displayedContent.length;
    const replyPaused = streamState.value.isPaused || streamState.value.pendingContent.length > 0;
    savePauseState(streamState.value.taskId, replyPaused, replyPosition);
  }
  
  // 立即保存当前状态（包括最新的 displayedContent）
  saveConversationState();

  document.removeEventListener('visibilitychange', handleVisibilityChange);
});

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

// 发送消息
function sendMessageFunc(data) {

  let { content, fileIds } = data ?? {}
  if (isLoading.value) return;
  if (content||fileIds) {
    // 会话模式下：确保内部状态与 props 同步
    // 解决发送新消息时创建新 sessionId 后内部状态未同步的问题
    if (props.sessionMode && props.sessionId) {
      internalSessionId.value = props.sessionId;
    }
    if (props.agentId) {
      internalAgentId.value = props.agentId;
    }

    // 如果上一轮在暂停中，先就地终止上一轮生成，再开始新问题
    if (props.sessionMode && streamState.value.isStreaming && streamState.value.isPaused) {
      finalizePausedStreamBeforeSend();
    }

    // 关键修复：在添加用户消息前，先清理旧的流式状态
    // 避免 saveConversationState 时因为 streamState.isStreaming=true 而错误地添加旧 AI 消息
    if (streamState.value.isStreaming) {
      // 如果上一轮还在进行（非暂停状态），先终止它
      cancelStreamLoop();
      if (streamState.value.timer) {
        clearInterval(streamState.value.timer);
        streamState.value.timer = null;
      }
      streamState.value.isStreaming = false;
      streamState.value.isPaused = false;
      streamState.value.serverDone = true;
    }

    // // 添加用户消息
    // messages.value.push({
    //   type: 'user',
    //   content: inputValue,
    //   timestamp: Date.now(),
    //   userid: userId.value,
    //   approveResult: ''
    // });
    saveConversationState();
    callAiApi(data);
    // emit('update:inputValue', '');
  }
}

// 调用 AI API
async function callAiApi(sendData) {
  let { content, fileIds, files } = sendData
  try {
    isLoading.value = true;
    isReceiving.value = false;
    console.info('[CallAiApi] ===== 调用 AI API =====')
    console.info('[CallAiApi] props.activeModuleId:', props.activeModuleId, '| props.sessionId:', props.sessionId, '| props.agentId:', props.agentId)
    console.info('[CallAiApi] internalSessionId:', internalSessionId.value, '| internalAgentId:', internalAgentId.value)
    console.log(props.wsSessionId,'oooooooooosssss22222')
    const params: any = {
      // content: message,
      // agent: props.activeModuleId,
      // userName: userInfo.value.username,
      // userID: userInfo.value.idCard, // 传递身份证号码

      userName: userInfo.value.username,
      userID: userInfo.value.idCard,
      content: content,
      sessionId: fileIds,
      agent: props.activeModuleId || '',  //测试数据6666
      approver: "",
      wsSessionId: props.wsSessionId
    };
    console.info('[CallAiApi] 请求参数 agent:', params.agent, '| 完整参数:', JSON.parse(JSON.stringify(params)))
    if(userInfo.value?.department){
      params.departmentId = userInfo.value.department?.departmentId;
      params.departmentCode = userInfo.value.department?.departmentCode;
      params.departmentName = userInfo.value.department?.departmentName;
    }

    // 清空之前的流式状态
    resetStreamState();

    console.log(params,'oooooooooosssss11111')
    const { code, data } = await newTask(params);
    isLoading.value = false;

    if (code === 0) {
      currentTaskId.value = data.id;

      console.log('[ChatList-API] ===== newTask 响应 =====')
      console.log('[ChatList-API] taskId:', data.id)
      console.log('[ChatList-API] approvalStatus:', data.approvalStatus, '(0-无需审核 1-待审批 2-审核完成 3-待建单)')
      console.log('[ChatList-API] approveResult:', data.approveResult, '(0-审核通过 1-审批拒绝)')
      console.log('[ChatList-API] wsSessionId:', props.wsSessionId)

      // 添加用户消息
      let currentMessageObj: any = {
        type: 'user',
        content: content,
        timestamp: Date.now(),
        userid: userId.value,
        taskId: data.id,
        approvalStatus: data.approvalStatus || '',
        approveResult: data.approveResult,
        approveStatusName: '',
        approveNo: data.approveNo || '',
        approveUrl: data.approveUrl || '',
        approveDetailUrl: data.approveDetailUrl || '',
        toLeaderUrl: data.toLeaderUrl || '',
        sessionId: fileIds,
        approveUser: data.approveUser || '',
        attachement_path: files?.[0]?.attachement_path,
        approvalSubMode: data.approvalSubMode,
        agentConfigId: props.activeModuleId,
        userName: data.userName || '',
        queryTime: formatMsgTime(data.queryTime),
      };
      if(data.approvalStatus == '0'){
        currentMessageObj.approveStatusName = '无需审核';
        console.log('[ChatList-API] 设置审批状态: 无需审核')
      } else if(data.approvalStatus == '1') {
        currentMessageObj.approveStatusName = '待审批';
        console.log('[ChatList-API] 设置审批状态: 待审批 → 等待 WebSocket 推送审批结果')
      } else if(data.approvalStatus == '3') {
        currentMessageObj.approveStatusName = '待建单';
        console.log('[ChatList-API] 设置审批状态: 待建单')
      } else if(data.approvalStatus == '2'){
        if(data.approveResult == 0){
          currentMessageObj.approveStatusName = '审批通过';
          console.log('[ChatList-API] 设置审批状态: 审批通过')
        } else {
          currentMessageObj.approveStatusName = '审批拒绝';
          console.log('[ChatList-API] 设置审批状态: 审批拒绝')
        }
      }
      messages.value.push(currentMessageObj);

      /**
       * 满足 审批通过 || 不需要审批 || 审批模式为先问后审 直接请求回复
       */
      if(data.approvalStatus == '0' || data.approveResult == 0 || data.approvalSubMode == 0){
        // 使用流式输出
        console.log('[ChatList-API] 开始流式输出, taskId:', currentTaskId.value)
        await startStreamOutput(currentTaskId.value);
      } else {
        console.log('[ChatList-API] 需要审批，等待 WebSocket 推送审批结果')
        streamState.value.isStreaming = false;
      }
    } else {
      // API调用失败，重置流式状态
      streamState.value.isStreaming = false;
      streamState.value.isPaused = false;
    }
    isLoading.value = false;
    isReceiving.value = false;
  } catch (error) {
    console.error('调用 AI API 失败:', error);
    handleError(ERROR_MESSAGE);
    isLoading.value = false;
  }
}

// 提取纯文本
function getPlainTextFromMessage(message: any): string {
  if (message?.type === 'ai') {
    const div = document.createElement('div');
    div.innerHTML = message?.content || '';
    return (div.textContent || div.innerText || '').trim();
  }
  return (message?.content || '').toString();
}

// 根据触发元素查找对应AI消息的纯文本（供 SelectionToolbar 组件"复制全部"使用）
function getAiAllTextByElement(el: HTMLElement): string {
  const wrapper = el.closest('.message-wrapper') as HTMLElement | null;
  if (!wrapper) return '';
  const id = wrapper.id || '';
  const index = parseInt(id.replace('msg-', ''), 10);
  if (Number.isNaN(index) || !messages.value[index]) return '';
  return getPlainTextFromMessage(messages.value[index]);
}

function savePauseState(taskId: string, replyPaused: boolean, replyPosition: number) {
  // if (!streamState.value.timer) return;
  AiAgentChatPausePosition.savePosition(taskId, { replyPaused, replyPosition });
}

// 暂停流式输出
function pauseStream() {
  if (streamState.value.timer) {
    clearInterval(streamState.value.timer);
    streamState.value.timer = null;
  }
  streamState.value.isPaused = true;
  isReceiving.value = false;
  
  // 记录暂停状态到缓存
  const replyPosition = streamState.value.displayedContent.length;
  const taskId = streamState.value.taskId;
  if (taskId) {
    savePauseState(taskId, true, replyPosition);
  }
  
  // 更新消息对象的 isPaused 标志
  const lastIndex = getStreamingMessageIndex();
  if (lastIndex >= 0 && messages.value[lastIndex]) {
    messages.value[lastIndex].isPaused = true;
    messages.value[lastIndex].taskId = taskId;
    messages.value[lastIndex].replyPosition = replyPosition;
  }
  
  // 保存会话状态
  saveConversationState();
  
  emit('stream-pause');
}

// 继续流式输出：如果有待显示的内容，重新启动显示定时器
function resumeStream(messageIndex: number | null = null) {
  streamState.value.isPaused = false;
  
  
  // 确定目标消息索引
  const targetIndex = messageIndex !== null && messageIndex !== undefined
    ? messageIndex
    : messages.value.length - 1;

  // 获取消息对象，检查是否有历史恢复的暂停状态
  const msg = messages.value[targetIndex];

  AiAgentChatPausePosition.clearPosition(streamState.value.taskId);
  
  if (msg?.isPaused) {
    msg.isPaused = false;

    if (msg.taskId) {
      streamState.value.taskId = msg.taskId;
    }

    if (streamState.value.rawContent && streamState.value.pendingContent) {
      streamState.value.isStreaming = true;
      streamState.value.isPaused = false;
      aiModuleStore.setStreamingTaskId(msg.taskId || null);

      if (streamState.value.pendingContent.length > 0 && !streamState.value.timer) {
        startDisplayTimer();
      } else if (msg.taskId) {
        isReceiving.value = true;
        startStreamOutput(msg.taskId, true);
      }
    } else if (streamState.value.pendingContent && !streamState.value.timer) {
      startDisplayTimer();
    }
  } else if (streamState.value.pendingContent && !streamState.value.timer) {
    startDisplayTimer();
  }
}

// 重置流式输出状态
function resetStreamState() {
  // 取消上一轮轮询循环（如果存在）
  cancelStreamLoop();
  if (streamState.value.timer) {
    clearInterval(streamState.value.timer);
  }
  if (saveThrottleTimer) {
    clearTimeout(saveThrottleTimer);
  }
  streamState.value.isStreaming = false;
  streamState.value.isPaused = false;
  aiModuleStore.clearStreamingTaskId();
  streamState.value.serverDone = false;
  streamState.value.displayedContent = '';
  streamState.value.timer = null;
  streamState.value.rawContent = '';
  streamState.value.pendingContent = '';
  streamState.value.taskId = null;
  streamMessageIndex.value = -1;
}

const END_TAG = '[end]';
const ERROR_TAG = '[Error]';

// 开始流式输出
async function startStreamOutput(taskId, isHistory = false, insertAfterIndex?: number) {
  const myToken = ++streamLoopToken.value;
  const INTERVAL = 1000;
  const MAX_TIMEOUT = await getAgentResponseTimeout(); // 最长等待有效响应时间
  console.log('[startStreamOutput] 智能体响应超时时间:', MAX_TIMEOUT === Infinity ? '∞（永不超时）' : `${MAX_TIMEOUT}ms`);
  let lastResponseTime = Date.now(); // 上次收到有效响应的时间

  streamState.value.taskId = taskId;
  streamState.value.isStreaming = true;
  aiModuleStore.setStreamingTaskId(taskId);
  // 只要开始轮询，就认为服务端尚未结束（会在收到 [end] 时置 true）
  streamState.value.serverDone = false;
  if (!isHistory) {
    // 非退出重进的历史加载
    // 新一轮回复开始：清空上一轮的内容缓冲与定时器，避免多个审批回复内容串联
    if (streamState.value.timer) {
      clearInterval(streamState.value.timer);
      streamState.value.timer = null;
    }
    streamState.value.displayedContent = '';
    streamState.value.rawContent = '';
    streamState.value.pendingContent = '';
    // 创建临时消息
    const tempMessage = {
      type: 'ai',
      content: SEARCHING_SHOW,
      timestamp: Date.now(),
      userid: userId.value,
      isStreaming: true,
      agentConfigId: props.activeModuleId,
      agentName: '',
      answerTime: '',
      queryTime: '',
      userName: '',
    };
    if (insertAfterIndex !== undefined && insertAfterIndex >= 0 && insertAfterIndex < messages.value.length) {
      messages.value.splice(insertAfterIndex + 1, 0, tempMessage);
      streamMessageIndex.value = insertAfterIndex + 1;
    } else {
      messages.value.push(tempMessage);
      streamMessageIndex.value = messages.value.length - 1;
    }
    // 立即保存一次，避免"需要持续输出一段时间才写入历史"的体验问题
    // （原逻辑依赖 updateMessageContent 的 1s 节流，导致本地缓存短时间只有 user 消息）
    saveConversationState();
  } else {
    if (streamMessageIndex.value < 0) {
      for (let i = messages.value.length - 1; i >= 0; i--) {
        if (messages.value[i]?.type === 'ai' && messages.value[i]?.isStreaming) {
          streamMessageIndex.value = i;
          break;
        }
      }
    }
    if (streamState.value.pendingContent) {
      startDisplayTimer();
    }
    else if (streamState.value.displayedContent && !streamState.value.isStreaming) {
      return;
    }
  }

  isReceiving.value = true;
  // 首次调用 getAnswer 前等待 200ms，给后端预留处理时间
  await new Promise((resolve) => setTimeout(resolve, 200));
  while (true) {
    // 被取消：直接退出
    if (myToken !== streamLoopToken.value) {
      return;
    }
    // 被外部终止：直接退出
    if (!streamState.value.isStreaming) {
      return;
    }

    // 检查是否暂停，如果暂停则跳过API请求
    if (streamState.value.isPaused) {
      await new Promise((resolve) => setTimeout(resolve, INTERVAL));
      continue;
    }

    try {
      const response = await getAnswer({ id: taskId });

      // detail 接口返回智能体名称、提问时间，写入当前 AI 消息用于 UI 展示
      const detailData = response?.data;
      if (detailData && typeof detailData === 'object') {
        const streamIdx = getStreamingMessageIndex();
        const targetMsg = streamIdx >= 0 ? messages.value[streamIdx] : null;
        if (targetMsg) {
          targetMsg.agentName = detailData.agentName;
          targetMsg.answerTime = formatMsgTime(detailData.answerTime);
        }
      }

      // 提取 data 字段，兼容 null、undefined、空字符串
      let res = response?.data?.reply || '';
      const replyPosition = response?.data?.replyPosition;

      // 如果 data 是 null 或 undefined，检查 response 是否直接是字符串
      if (res === null || res === undefined) {
        if (typeof response === 'string') {
          res = response;
        } else {
          // response 是对象但 data 为空，继续轮询
          res = null;
        }
      }

      // null 或空字符串继续轮询
      if (res === null || res === undefined || res === '') {
        // 检查无响应时间是否超时
        const elapsed = Date.now() - lastResponseTime;
        if (MAX_TIMEOUT !== Infinity && elapsed > MAX_TIMEOUT) {
          console.warn(`[startStreamOutput] 超时终止：已等待 ${elapsed}ms，超过阈值 ${MAX_TIMEOUT}ms`);
          handleError(ERROR_TIMEOUT);
          return;
        }
        continue;
      }

      // 类型检查：确保 res 是字符串
      if (typeof res !== 'string') {
        console.error('响应不是字符串:', res);
        handleError(ERROR_MESSAGE);
        return;
      }

      // 收到有效响应，更新最后响应时间
      lastResponseTime = Date.now();
      emit('stream-poll');

      // 检查错误响应：只有纯 [ERROR] 才视为错误，包含其他内容则原样展示
      if (res.replace(END_TAG, '').trim() === '[ERROR]') {
        // 如果是历史恢复模式（会话模式），尝试显示已保存的内容
        if (isHistory && props.sessionMode) {
          const { rawContent, pendingContent } = streamState.value;
          if (rawContent || pendingContent) {
            // 有已保存内容，显示已保存内容
            completeWithSavedContent();
            return;
          }
        }
        // 没有已保存内容或非会话模式，显示错误
        handleError('业务繁忙，请稍后再试');
        return;
      }

      // 提取增量内容
      let newContent = res;
      let isEnd = false;

      // 检查是否包含结束标记 或者replyPosition == -1，表示结束
      if (res.includes(END_TAG) || replyPosition== '-1' || res.includes(ERROR_TAG)) {
        isEnd = true;
        // newContent = res.replace(END_TAG, '');
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
        }
      } else if (!oldContent.startsWith(newContent)) {
        console.log('新内容与旧内容不连续，直接拼接:', newContent);

        streamState.value.rawContent = streamState.value.displayedContent + newContent;
        streamState.value.pendingContent += newContent;

        // 如果没有活动的定时器，开始显示内容
        if (!streamState.value.timer) {
          startDisplayTimer();
        }
      }
      // 如果是结束响应，更新状态
      if (isEnd) {
        isReceiving.value = false;

        // 服务端已经结束：这里不要立刻把 isStreaming 置 false，否则会出现
        // “displayedContent 还没来得及推进就保存，刷新后只能从头开始显示/重新轮询”的问题。
        // 正确做法：标记 serverDone，让本地逐字显示继续把 pendingContent 消耗完；
        // 当 pendingContent 清空、timer 停止时，再由 startDisplayTimer 的收尾逻辑落盘并标记完成。
        streamState.value.serverDone = true;

        // 如果当前没有定时器（例如一次性返回且未启动显示），直接把内容同步到 displayedContent 并更新消息
        if (!streamState.value.timer && streamState.value.pendingContent) {
          streamState.value.displayedContent += streamState.value.pendingContent;
          streamState.value.pendingContent = '';
          updateMessageContent();
        }

        // 关键修复：如果 pendingContent 已空且没有定时器，说明显示已完成，直接标记 isStreaming = false
        if (!streamState.value.pendingContent && !streamState.value.timer) {
          streamState.value.isStreaming = false;
          aiModuleStore.clearStreamingTaskId();
          emit('stream-complete');
          const lastIndex = getStreamingMessageIndex();
          if (lastIndex >= 0) {
            if (streamState.value.rawContent || streamState.value.displayedContent) {
              const finalContent = streamState.value.rawContent || streamState.value.displayedContent;
              messages.value[lastIndex].content = marked.parse(finalContent);
            }
            messages.value[lastIndex].isStreaming = false;
          }
        }

        // 先保存一次：至少把 rawContent/pendingContent/serverDone/taskId 写进缓存，便于刷新后继续显示
        saveConversationState();

        console.log('[startStreamOutput] 服务端返回结束标记，正常退出轮询');
        break;
      }
    } catch (err) {
      console.error('轮询错误:', err);
      // 如果是历史恢复模式（会话模式），尝试显示已保存的内容
      if (isHistory && props.sessionMode) {
        const { rawContent, pendingContent } = streamState.value;
        if (rawContent || pendingContent) {
          // 有已保存内容，显示已保存内容
          completeWithSavedContent();
          return;
        }
      }
      // 没有已保存内容或非会话模式，显示错误
      handleError(ERROR_MESSAGE);
      return;
    }

    await new Promise((resolve) => setTimeout(resolve, INTERVAL));
  }
}

// 开始显示内容
// 暂停：由内容是否显示完 && 用户手动操作暂停继续控制
function startDisplayTimer() {
  if (streamState.value.timer) clearInterval(streamState.value.timer);
  // 立即推进一次显示，避免“刚拿到内容就刷新时 displayedContent 仍为空”的窗口期
  const step = (maxChars = 1) => {
    if (streamState.value.isPaused) return;
    if (!streamState.value.pendingContent) return;
    const chunk = streamState.value.pendingContent.slice(0, maxChars);
    streamState.value.displayedContent += chunk;
    streamState.value.pendingContent = streamState.value.pendingContent.slice(chunk.length);
    updateMessageContent();
    saveConversationState();
  };

  // 先同步显示一小段（20 字符），让 displayedContent 尽快非空并更易于断点恢复
  step(20);

  streamState.value.timer = setInterval(() => {
    if (streamState.value.isPaused) return;
    if (streamState.value.pendingContent) {
      // 每次显示一个字符
      step(1);
    } else {
      // 没有更多待显示的内容，清除定时器
      if (streamState.value.timer) {
        clearInterval(streamState.value.timer);
        AiAgentChatPausePosition.clearPosition(streamState.value.taskId)
        aiModuleStore.clearStreamingTaskId();
        // 在此处添加更新历史对话列表
        emit('stream-complete');
      }
      streamState.value.timer = null;
      streamState.value.isPaused = false;

      // 当 pendingContent 显示完成：
      // - 如果服务端已经结束（serverDone），这里才真正把会话标记为完成并落盘
      // - 如果服务端未结束，说明只是短暂停顿，等下一轮 pendingContent 进来再继续
      if (streamState.value.serverDone) {
        streamState.value.isStreaming = false;
        aiModuleStore.clearStreamingTaskId();
        const lastIndex = getStreamingMessageIndex();
        if (lastIndex >= 0 && messages.value[lastIndex].isStreaming) {
          messages.value[lastIndex].isStreaming = false;
        }
        saveConversationState();
      } else if (streamState.value.taskId) {
        isReceiving.value = true;
        startStreamOutput(streamState.value.taskId, true);
      }
    }
  }, 50);
}

// 处理错误情况
function handleError(val) {
  const savedIndex = streamMessageIndex.value;
  // 保留已有消息的 agentName/answerTime 等字段
  const existingMsg = savedIndex >= 0 ? messages.value[savedIndex] : null;
  const agentName = existingMsg?.agentName || '';
  const answerTime = existingMsg?.answerTime || '';
  const queryTime = existingMsg?.queryTime || '';
  const userName = existingMsg?.userName || '';

  resetStreamState();
  streamState.value.rawContent = val;
  streamState.value.displayedContent = val;

  let targetIndex = savedIndex >= 0 ? savedIndex : messages.value.length - 1;
  if (targetIndex >= 0 && messages.value[targetIndex] && messages.value[targetIndex].type === 'user') {
    targetIndex = targetIndex + 1;
  }
  const newMsg = {
    type: 'ai',
    content: marked.parse(val),
    timestamp: Date.now(),
    userid: userId.value,
    isStreaming: false,
    agentConfigId: props.activeModuleId,
    agentName,
    answerTime,
    queryTime,
    userName,
  };
  if (targetIndex >= 0 && targetIndex < messages.value.length) {
    messages.value[targetIndex] = newMsg;
  } else {
    messages.value.push(newMsg);
  }

  // 错误处理后保存消息状态
  saveConversationState();
}

// 更新消息内容
function updateMessageContent() {
  const lastIndex = getStreamingMessageIndex();
  if (lastIndex >= 0 && messages.value[lastIndex] && messages.value[lastIndex].isStreaming) {
    messages.value[lastIndex].content = marked.parse(streamState.value.displayedContent);

    // 节流保存状态，缩短节流时间以减少状态丢失风险
    if (saveThrottleTimer) {
      clearTimeout(saveThrottleTimer);
    }
    saveThrottleTimer = setTimeout(() => {
      saveConversationState();
      saveThrottleTimer = null;
    }, 300); // 缩短到 300ms
  }
}

// 完成已保存内容的显示（用于会话模式历史恢复）
function completeWithSavedContent() {
  const { rawContent, pendingContent, displayedContent } = streamState.value;
  const streamIdx = getStreamingMessageIndex();
  const lastMessage = messages.value ? messages.value[streamIdx] : null;

  if (rawContent || pendingContent) {
    let finalContent = rawContent || '';

    if (displayedContent && finalContent) {
      if (finalContent.startsWith(displayedContent)) {
        const remainingContent = finalContent.substring(displayedContent.length);
        if (remainingContent) {
          streamState.value.pendingContent = remainingContent;
          streamState.value.rawContent = finalContent;
        } else {
          streamState.value.displayedContent = finalContent;
          streamState.value.pendingContent = '';
          streamState.value.isStreaming = false;
          if (lastMessage?.type === 'ai' && lastMessage?.isStreaming) {
            messages.value[streamIdx].content = marked.parse(finalContent);
            messages.value[streamIdx].isStreaming = false;
          }
          saveConversationState();
          return;
        }
      } else {
        streamState.value.pendingContent = finalContent;
        streamState.value.displayedContent = '';
      }
    } else if (finalContent && !displayedContent) {
      streamState.value.pendingContent = finalContent;
      streamState.value.rawContent = finalContent;
    } else if (pendingContent && !finalContent) {
      streamState.value.pendingContent = pendingContent;
      if (!streamState.value.rawContent) {
        streamState.value.rawContent = displayedContent + pendingContent;
      }
    }

    if (lastMessage?.type === 'ai' && lastMessage?.isStreaming) {
      if (displayedContent) {
        messages.value[streamIdx].content = marked.parse(displayedContent);
      }
      if (streamState.value.pendingContent) {
        streamState.value.isStreaming = true;
        startDisplayTimer();
      } else {
        streamState.value.isStreaming = false;
        messages.value[streamIdx].isStreaming = false;
        if (streamState.value.rawContent) {
          messages.value[streamIdx].content = marked.parse(streamState.value.rawContent);
        }
        saveConversationState();
      }
    } else if (lastMessage?.type === 'user') {
      const tempMessage = {
        type: 'ai',
        content: displayedContent ? marked.parse(displayedContent) : SEARCHING_SHOW,
        timestamp: Date.now(),
        userid: userId.value,
        isStreaming: true,
        agentConfigId: props.activeModuleId,
      };
      messages.value.push(tempMessage);
      streamMessageIndex.value = messages.value.length - 1;
      if (streamState.value.pendingContent) {
        streamState.value.isStreaming = true;
        startDisplayTimer();
      } else {
        streamState.value.isStreaming = false;
        messages.value[streamMessageIndex.value].isStreaming = false;
        if (streamState.value.rawContent) {
          messages.value[streamMessageIndex.value].content = marked.parse(streamState.value.rawContent);
        }
        saveConversationState();
      }
    }

    const checkComplete = () => {
      if (!streamState.value.pendingContent && !streamState.value.timer) {
        streamState.value.isStreaming = false;
        const idx = getStreamingMessageIndex();
        if (idx >= 0 && messages.value[idx] && messages.value[idx].isStreaming) {
          messages.value[idx].isStreaming = false;
        }
        if (streamState.value.rawContent) {
          messages.value[idx].content = marked.parse(streamState.value.rawContent);
        }
        saveConversationState();
      } else if (streamState.value.timer) {
        setTimeout(checkComplete, 100);
      }
    };
    setTimeout(checkComplete, 200);
  } else {
    handleError('查询失败');
  }
}

// 获取缓存键名
function getChatKey(sessionId?: string | null, agentId?: string): string | null {
  if (props.sessionMode) {
    // 优先使用传入参数，其次使用内部状态，最后使用 props
    const finalSessionId = sessionId ?? internalSessionId.value ?? props.sessionId;
    const finalAgentId = agentId ?? internalAgentId.value ?? props.agentId;

    if (!finalSessionId || !userId.value) {
      return null;
    }
    // 会话模式下，如果 agentId 为空或未设置，使用 -1 作为默认值
    const actualAgentId = finalAgentId || '-1';
    return `chatHistory_${finalSessionId}_${userId.value}_${actualAgentId}`;
  }
  return `chatHistory_${userId.value}`;
}

// 从本地存储恢复对话状态
async function restoreConversation(sessionId?: string | null, agentId?: string) {
  // 恢复前先完全清理流式状态，防止旧状态干扰
  if (streamState.value.timer) {
    clearInterval(streamState.value.timer);
    streamState.value.timer = null;
  }
  cancelStreamLoop();
  // 完全重置 streamState，确保不会残留旧状态
  streamState.value.isStreaming = false;
  streamState.value.isPaused = false;
  streamState.value.serverDone = false;
  streamState.value.displayedContent = '';
  streamState.value.rawContent = '';
  streamState.value.pendingContent = '';
  streamState.value.taskId = null;

  // 更新内部状态为即将恢复的会话ID
  internalSessionId.value = sessionId ?? props.sessionId ?? null;
  internalAgentId.value = agentId ?? props.agentId ?? '';

  try {
    const chatKey = getChatKey(sessionId ?? props.sessionId, agentId ?? props.agentId);
    if (!chatKey) {
      messages.value = [];
      return false;
    }
    const originData = localStorage.getItem(chatKey);
    if (!originData) return false;
    const parsedState = JSON.parse(decodeURIComponent(escape(originData)));
    if (parsedState) {
      // 恢复消息历史
      messages.value = parsedState.messages || [];

      // 如果恢复的消息为空，删除缓存
      if (!messages.value || messages.value.length === 0) {
        localStorage.removeItem(chatKey);
        return false;
      }

      // 恢复流式输出状态
      streamState.value = parsedState.streamState || {
        isStreaming: false,
        displayedContent: '',
        timer: null,
        rawContent: '',
        pendingContent: '',
        taskId: null,
        serverDone: false,
      };
      streamState.value.timer = null;
      const { isStreaming, pendingContent, taskId, displayedContent, rawContent } = streamState.value;
      const serverDone = (streamState.value as any)?.serverDone === true;

      // 如果 messages 里没有任何 AI 类型的消息，但 streamState 中已经有 rawContent / displayedContent，
      // 说明之前可能只落了 user 消息，这里做一次兜底补一条 AI 消息，避免“有答案但列表里没有 AI 消息”的情况
      const hasAiMessage = messages.value.some((m: any) => m?.type === 'ai');
      if (!hasAiMessage && (rawContent || displayedContent)) {
        const finalContent = rawContent || displayedContent || '';
        if (finalContent) {
          messages.value.push({
            type: 'ai',
            content: marked.parse(finalContent),
            timestamp: Date.now(),
            userid: userId.value,
            isStreaming: !!isStreaming,
            agentConfigId: props.activeModuleId,
          });
        }
      }

      const lastMessage = messages.value ? messages.value[messages.value.length - 1] : [];

      if (props.sessionMode && (isStreaming || pendingContent !== '' || rawContent)) {
        const streamIdx = getStreamingMessageIndex();
        if (displayedContent && lastMessage?.type === 'ai' && lastMessage?.isStreaming) {
          messages.value[streamIdx >= 0 ? streamIdx : messages.value.length - 1].content = marked.parse(displayedContent);
        }

        if (serverDone) {
          if (pendingContent) {
            streamState.value.isStreaming = true;
            startDisplayTimer();
          } else {
            streamState.value.isStreaming = false;
            const idx = streamIdx >= 0 ? streamIdx : messages.value.length - 1;
            if (idx >= 0 && messages.value[idx]?.isStreaming) {
              messages.value[idx].isStreaming = false;
            }
            saveConversationState();
          }
        }
        else if (taskId) {
          try {
            await startStreamOutput(taskId, true);
            isLoading.value = false;
          } catch (error) {
            console.warn('继续轮询失败，尝试显示已保存内容:', error);
            isLoading.value = false;
            completeWithSavedContent();
          }
        } else {
          completeWithSavedContent();
        }
      } else if (isStreaming || pendingContent !== '') {
        const streamIdx = getStreamingMessageIndex();
        if (displayedContent && lastMessage?.type === 'ai' && lastMessage?.isStreaming) {
          messages.value[streamIdx >= 0 ? streamIdx : messages.value.length - 1].content = marked.parse(displayedContent);
        }

        if (pendingContent === '') {
        }
        if (taskId) {
          await startStreamOutput(taskId, true);
          isLoading.value = false;
        }
      } else {
        const messageItem = {
          type: 'ai',
          content: '查询失败',
          timestamp: Date.now(),
          userid: userId.value,
          isStreaming: false,
          agentConfigId: props.activeModuleId,
        }
        if (lastMessage?.type === 'user') {
          messages.value.push(messageItem);
        } else if (lastMessage?.type === 'ai' && lastMessage?.content === SEARCHING_SHOW) {
          messages.value[messages.value.length - 1] = messageItem;
        }
      }

      return true;
    }
  } catch (error) {
    console.error('恢复对话状态失败:', error);
  }
  return false;
}

function recallConversation(data) {


  let lastAiMsgIndex = -1;
  for (let i = data.length - 1; i >= 0; i--) {
    if (data[i].type === 'ai' && data[i].content && !data[i].isErrorContent) {
      lastAiMsgIndex = i;
      break;
    }
  }

  messages.value = data.map((msg: any, index: number) => {
    if (msg.type === 'ai' && msg.content && !msg.isErrorContent) {
      const rawContent = msg.content;
      const isComplete = rawContent.trimEnd().endsWith(END_TAG);
      const isLastAiMsg = index === lastAiMsgIndex;

      let pauseInfo: { replyPaused?: boolean; replyPosition?: number } | null = null;
      if (msg.taskId) {
        pauseInfo = AiAgentChatPausePosition.getPosition(msg.taskId) as { replyPaused?: boolean; replyPosition?: number } | null;
      }

      if (!pauseInfo || !pauseInfo.replyPosition || pauseInfo.replyPosition <= 0) {
        return {
          ...msg,
          content: marked.parse(rawContent.replace(END_TAG, '').trimEnd()),
          isPaused: false,
          replyPosition: 0,
          isStreaming: false,
        };
      }

      if (!isLastAiMsg) {
        const displayedPart = rawContent.substring(0, pauseInfo.replyPosition);
        return {
          ...msg,
          content: marked.parse(displayedPart),
          isPaused: false,
          replyPosition: pauseInfo.replyPosition,
          isStreaming: false,
        };
      }

      const displayedPart = rawContent.substring(0, pauseInfo.replyPosition);
      const pendingPart = rawContent.substring(pauseInfo.replyPosition);
      const isManualPaused = !!pauseInfo.replyPaused;

      streamState.value.displayedContent = displayedPart;
      streamState.value.rawContent = rawContent;
      streamState.value.pendingContent = pendingPart;
      streamState.value.serverDone = isComplete;
      streamState.value.taskId = msg.taskId || null;

      if (isComplete) {
        streamState.value.isStreaming = pendingPart.length > 0;
        streamState.value.isPaused = isManualPaused && pendingPart.length > 0;
        if (!isManualPaused && pendingPart.length > 0 && !streamState.value.timer) {
          nextTick(() => {
            startDisplayTimer()
          });
        } else if (!isManualPaused) {
          aiModuleStore.clearStreamingTaskId();
        }
      } else {
        streamState.value.isStreaming = true;
        streamState.value.isPaused = isManualPaused;
        if (!isManualPaused && pendingPart.length > 0 && !streamState.value.timer) {
          nextTick(() => {
            startDisplayTimer()
          });
        }
        if (!isManualPaused) {
          aiModuleStore.setStreamingTaskId(msg.taskId || null);
        }
      }

      return {
        ...msg,
        content: marked.parse(displayedPart),
        isPaused: isManualPaused,
        replyPosition: pauseInfo.replyPosition,
        isStreaming: !isComplete || pendingPart.length > 0,
      };
    }
    if (msg.type === 'ai') {
      return {
        ...msg,
        content: marked.parse(msg.content || ''),
        isStreaming: false,
      };
    }
    return msg;
  });

  // 切换会话/恢复历史时强制滚动到底部，避免 userScrolled=true 导致不滚动
  nextTick(() => {
    forceScrollToBottom();
  });
}

/**
 * 在现有消息列表前面插入更早的历史消息（用于"上滑到顶部加载更多"场景）。
 * 与 recallConversation 不同，本方法不会重置整个 messages，也不会影响当前的 streamState，
 * 仅对新插入的原始消息做 marked.parse / END_TAG 处理。
 *
 * @param newItems 原始消息数组（按时间从旧到新排序），将被 prepend 到现有 messages 之前
 */
function prependMessages(newItems: any[]) {
  if (!newItems || newItems.length === 0) return;
  const END_TAG = '[end]';
  const processed = newItems.map((msg: any) => {
    if (msg.type === 'ai' && msg.content && !msg.isErrorContent) {
      const rawContent = msg.content;
      return {
        ...msg,
        content: marked.parse(rawContent.replace(END_TAG, '').trimEnd()),
        isPaused: false,
        replyPosition: 0,
        isStreaming: false,
      };
    }
    if (msg.type === 'ai') {
      return {
        ...msg,
        content: marked.parse(msg.content || ''),
        isStreaming: false,
      };
    }
    return msg;
  });
  messages.value = [...processed, ...messages.value];
}

// 保存对话状态到本地存储
const saveConversationState = () => {
  try {
    // 优先使用内部状态，其次使用 props，确保获取到最新的 sessionId
    const sessionId = internalSessionId.value || props.sessionId;
    const agentId = internalAgentId.value || props.agentId;
    const chatKey = getChatKey(sessionId, agentId);
    if (!chatKey) return;

    // 保存前：尽量把最新的流式内容同步回 messages，避免出现“streamState 有内容但 messages 没 AI”
    if (
      props.sessionMode &&
      streamState.value?.isStreaming &&
      streamState.value?.displayedContent &&
      streamState.value.displayedContent.trim() !== ''
    ) {
      const lastIndex = getStreamingMessageIndex();
      const lastMsg = lastIndex >= 0 ? messages.value[lastIndex] : null;
      if (lastMsg?.type === 'ai' && lastMsg?.isStreaming) {
        lastMsg.content = marked.parse(streamState.value.displayedContent);
      } else if (lastMsg?.type === 'user') {
        // 边界兜底：如果还没插入占位 AI 消息（例如 props.sessionId 迟到导致前面逻辑提前 return），这里补一条
        messages.value.push({
          type: 'ai',
          content: marked.parse(streamState.value.displayedContent),
          timestamp: Date.now(),
          userid: userId.value,
          isStreaming: true,
          agentConfigId: props.activeModuleId,
        });
      }
    }

    // 只有在有消息时才保存，没有消息则删除缓存
    if (messages.value && messages.value.length > 0) {
      // 不持久化 timer（不同环境下可能是对象/句柄，影响序列化稳定性）
      const streamStateToSave = { ...streamState.value, timer: null };
      const stateToSave = {
        messages: messages.value,
        streamState: streamStateToSave,
      };
      const escaped = unescape(encodeURIComponent(JSON.stringify(stateToSave)));
      console.log('保存！！！！', streamState.value.displayedContent);
      localStorage.setItem(chatKey, escaped);
    } else {
      // 如果没有消息，删除已存在的缓存
      localStorage.removeItem(chatKey);
    }
  } catch (error) {
    console.error('保存对话状态失败:', error);
  }
};

// 处理审批点击事件
function openApprovalUrl(url: string) {
  console.log('isWebView2Env()-1111111111', isWebView2Env())
  if (isWebView2Env()) {
    openUrl(url);
  } else {
    window.open(url, '_blank');
  }
}

async function handleApprovalClick(message, action) {
  console.log('message', message);
  // 待建单
  if (action === 'create') {
    if (message.approveUrl) {
      openApprovalUrl(message.approveUrl);
    } else {
      try {
        const deployRes = await getDeployConfig();
        const groupAiHost = deployRes.data?.groupAiHost;
        axios.get(`${groupAiHost.replace(/\/$/, '')}/proxy/ai/v1/aiagent/management/settings`)
        .then(res => {
         if (res?.data?.data?.approvalSystemUrl) {
           openApprovalUrl(res.data.data.approvalSystemUrl);
         } else { 
           ElMessage.error('审批地址不存在');
         }
        })
      } catch (error) {
        console.error('获取审批配置失败:', error);
      }
    }

  } else if (action === 'urge') {
    const toLeaderUrl = message?.toLeaderUrl || '';
    const approveUser = message?.approveUser || '';
    if (approveUser && toLeaderUrl) {
      try {
        const userRes = await getUserInfoByIdCard(approveUser)
        const assignUserId = userRes?.data?.id || ''
        if (!assignUserId) {
          ElMessage.error('未获取到审批人信息，无法发送催办')
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
        const appResult = await sendCustomCard(cardParams)
        console.log('[ApprovalWS-web] 发送审批卡片结果:appResult', appResult)
        // bspc 返回 errorCode，cspc 返回 code
        const isSuccess = isWebView2Env() ? (appResult && appResult.code === 0) : (appResult && appResult.errorCode === 0)
        if (isSuccess) {
          ElMessage.success('催办成功')
        } else {
          const errorMsg = isWebView2Env() ? (appResult?.errMsg || '催办失败') : (appResult?.errorMessage || '催办失败')
          ElMessage.error(errorMsg)
        }
      } catch (e) {
        ElMessage.error('催办失败')
      }
    } else {
      console.warn('[ChatList] 催办缺少必要参数: approveUser或toLeaderUrl为空')
    }
  } else {
    if (message.approveDetailUrl) {
      openApprovalUrl(message.approveDetailUrl);
    } else { 
      ElMessage.error('审批详情地址不存在');
    } 
  }
}


// 催办事件 成功以后设置当前催办问题催办状态为true
async function handleUrge(message) {
  await handleApprovalClick(message, 'urge')
  console.log('催办成功')
  message.isUrge = true
}

function handleAnchorClick() {
}

function handleAnchorScrollEnd() {
  const targetIndex = pendingApprovalIndex.value
  pendingApprovalIndex.value = -1
  anchorVisible.value = false

  if (targetIndex >= 0 && messages.value[targetIndex]) {
    const taskId = messages.value[targetIndex].taskId
    if (taskId) {
      startStreamOutput(taskId, false, targetIndex)
    }
  }
}

// 兜底：当 userId 从空变为有值时，如果已经有首轮会话的 messages 和 sessionId，可立即补一次 history 落盘
watch(
  () => userId.value,
  (newUserId, oldUserId) => {
    if (!props.sessionMode) return;
    if (!newUserId || newUserId === oldUserId) return;
    if (!props.sessionId) return;
    if (!messages.value || messages.value.length === 0) return;
    const chatKey = getChatKey(props.sessionId, props.agentId);
    if (chatKey) {
      saveConversationState();
    }
  },
);

// 暴露方法供父组件调用
defineExpose({
  sendMessageFunc,
  messages,
  isLoading,
  restoreConversation,
  pauseStream,
  resumeStream,
  streamState,
  resetStreamState,
  saveConversationState,
  recallConversation,
  prependMessages,
  resetReachBottom,
  resetReachTop,
  setReachTopLoading
})
</script>

<style scoped lang="less">
@import '@/styles/markdown.less';
/* 响应式设计 */
@media (max-width: 768px) {
  .message-content {
    max-width: 85%;
  }

  .chat-container {
    padding: 10px;
  }
}

.chat-list {
  position: relative;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

.chat-container {
  box-sizing: border-box;
  width: 100%;
  height: 100%;
  padding: 20px;
}

.chat-messages {
  position: relative;
  width: 100%;
  height: 100%;
  padding: 20px 0;
  overflow-y: auto;

  /* 隐藏滚动条但保持滚动功能 */
  scrollbar-width: none; /* Firefox */
  -ms-overflow-style: none; /* IE and Edge */

  &::-webkit-scrollbar {
    display: none; /* Chrome, Safari, Opera */
  }
}

.message-wrapper {
  margin-bottom: 20px;

  &.user-message {
    display: flex;
    justify-content: flex-end;
  }

  &.ai-message {
    display: flex;
    justify-content: flex-start;
    flex-wrap: wrap;
  }

  .message-content,
  .avatar {
    flex-shrink: 0;
  }
}

.message-content {
  display: flex;
  align-items: flex-start;
  max-width: 96%;
  margin-bottom: 15px;

  // &.user-content {
  //   flex-direction: row-reverse;
  // }

  &.ai-content {
    width: 100%;
    flex-direction: row;

    .ai-content-main {
      display: flex;
      flex-direction: column;
      width: calc(100% - 80px);
    }

    .name-time-row {
      display: flex;
      align-items: center;
      margin: 0 0 6px 0;
      gap: 8px;
    }

    .agent-name {
      font-size: 14px;
      line-height: 150%;
      color: rgba(91, 96, 114, 1);
    }

    .msg-time {
      font-size: 14px;
      line-height: 150%;
      color: rgba(91, 96, 114, 1);
    }

    .resume-btn-wrapper {
      height: 100%;
      display: flex;
      justify-content: flex-end;
      flex-direction: column;
      .resume-btn {
        width: 70px;
        height: 30px;
        font-size: 13px;
        background-color: white;
        border: 1px solid #f0f0f0;
        border-radius: 16px;
        color: black;
      }
      .resume-btn:hover {
        background: white;
        border-color: #f0f0f0;
      }
    }
  }
}

.message-bubble {
  max-width: 100%;
  width: fit-content;
  padding: 12px 16px;
  line-height: 1.4;
  word-wrap: break-word;
  border-radius: 18px;

  &.user-bubble {
    margin-left: 5px;
    color: white;
    background: var(--ai-message-bg);
    border-top-right-radius: 4px;
    // 覆盖全局 reset.less 的 user-select: none，允许框选用户问题文本及其后代元素
    user-select: text;
    -webkit-user-select: text;
    * {
      user-select: text;
      -webkit-user-select: text;
    }
    ::selection {
      background: rgba(105, 187, 255, 0.5);
    }
    &.is-remind-deal {
      background: rgba(162, 179, 235, 1);
    }
  }

  &.ai-bubble {
    margin-right: 8px;
    color: var(--text-color);
    background: var(--ai-message-bg2);
    border-top-left-radius: 4px;
    box-shadow: 0 2px 8px rgb(0 0 0 / 10%);
    // 覆盖全局 reset.less 的 user-select: none，允许框选AI回答文本及其后代元素
    user-select: text;
    -webkit-user-select: text;
    * {
      user-select: text;
      -webkit-user-select: text;
    }
    // PC 端选区：与 H5 一致
    ::selection {
      background: rgba(105, 187, 255, 0.35);
    }
  }
}

.message-text {
  font-size: 14px;
  line-height: 1.5;
}

.user-bubble-wrap {
  width: 70vw;
  display: flex;
  flex-direction: column;
  align-items: flex-end;

  .name-time-row {
    display: flex;
    align-items: center;
    margin-bottom: 6px;
    gap: 8px;
  }

  .user-name {
    font-size: 14px;
    line-height: 150%;
    color: rgba(91, 96, 114, 1);
  }

  .msg-time {
    font-size: 14px;
    line-height: 150%;
    color: rgba(91, 96, 114, 1);
  }

  .approval-status-row {
    display: flex;
    align-items: center;
    gap: 8px;
  }
}

.approval-status {
  width: 100px;
  height: 40px;
  margin-top: 8px;
  font-size: 16px;
  line-height: 1.5;
  border-radius: 8px;
  font-weight: 400;
  align-self: flex-end;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 3px;
  box-sizing: border-box;

  .status-icon {
    width: 17.5px;
    height: 17.5px;
  }

  &.create {
    background-color: rgba(255, 205, 169, 0.2);
    color: rgba(250, 120, 27, 1);
  }

  &.pending {
    background-color: rgba(255, 205, 169, 0.2);
    color: rgba(250, 120, 27, 1);
  }

  &.success {
    color: rgba(57, 206, 75, 1);
    border: 1px solid rgba(57, 206, 75, 0.5);
  }

  &.reject {
    color: rgba(235, 82, 85, 1);
    border: 1px solid rgba(235, 82, 85, 0.5);
  }

  &.urge {
    background-color: #f4faff;
    color: #3772e0;
    border: 1px solid #b2cdff;
  }
}

.avatar {
  display: flex;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  width: 45px;
  height: 45px;
  flex: none;

  img {
    width: 45px;
    height: 45px;
    border-radius: 50%;
  }

  &.user-avatar {
    margin-left: 10px;

    .avatar-img {
      border-radius: 8px;
    }
  }
}


</style>
