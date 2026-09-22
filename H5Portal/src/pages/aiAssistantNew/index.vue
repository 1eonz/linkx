<template>
  <view class="ai-assistant-page">
    <!-- 头部导航栏 -->
    <view class="header">
      <NavBar
        :title="navBarTitle"
        @back="gotoBack"
      >
        <template #right>
          <view
            v-if="isSessionMode"
            style="color: rgba(30, 82, 242, 1); padding: 0 2vw"
            @click="gotoHistory"
            >历史</view
          >
        </template>
      </NavBar>
    </view>

    <!-- 主要内容区域 -->
    <view class="main">
      <view v-if="pageLoading" class="page-loading">
        <view class="loading-spinner"></view>
      </view>
      <MessageWelcom v-else-if="isInit" />
      <MessageList
        v-else
        ref="messageList"
        :curr-agent="currPageAgent"
        :is-session-mode="isSessionMode"
        :init-msg-list="messages"
        @state-change="handleStateChange"
      />
    </view>

    <view v-if="!pageLoading" class="footer">
      <!-- 综合模式可使用多个。等模式就绪后再挂载，避免 session 模式第一次加载用错缓存 -->
      <AgentSupermarket
        v-if="modeReady"
        :is-session-mode="isSessionMode"
        :curr-agent="currPageAgent"
        @before-change="handleAgentChange"
        @select="handleAgentSelect"
      />
      <MessageSender
        :is-loading="isLoading"
        :is-paused="isPaused"
        :is-streaming="isStreaming"
        :is-receiving="isReceiving"
        :is-displaying="isDisplaying"
        :userId="userInfo?.userid"
        :agentId="agentId"
        @send="sendMessage"
        @pause="handlePause"
        @focus="handleInputFocus"
        />
    </view>
  </view>
</template>

<script setup>
  import { ref, useTemplateRef, nextTick, onMounted, onUnmounted, computed, watch } from 'vue';
  import { getGlobalsConfigByKey } from '@/common/utils';
  import NavBar from './components/NavBar/index.vue';
  import MessageWelcom from './components/MessageWelcom/index.vue';
  import MessageList from './components/MessageList/index.vue';
  import AgentSupermarket from './components/AgentSupermarket/index.vue';
  import MessageSender from './components/MessageSender/index.vue';
  import { useRouter, useRoute } from 'vue-router';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useAiStore } from '@/stores/ai.js';
  import { getRecordsByAgent , getAgents} from '@/common/api/ai.js';
  import { getSelectedAgent } from '@/pages/aiAssistantNew/composables/useAgentCache';
  import AiAgentChatPausePosition from './composables/AiAgentChatPausePosition.js';
  import { formatMsgTime } from '@/utils/aiAssistantUtils.js';

  // 历史记录分页参数
  const HISTORY_PAGE_SIZE = 20;
  const lastId = ref('');
  const hasMoreHistory = ref(true);

  // 路由
  const router = useRouter();
  const route = useRoute();

  // 消息列表组件的ref
  const messageListRef = useTemplateRef('messageList');

// 用户信息
const communicationStore = useCommunicationStore();
const aiStore = useAiStore();
const userInfo = ref({
  username: "",
  userid: null,
  idCard: null,
  thumbAvatar: "",
});

  // 本地消息（用于初始化历史传给 MessageList）。WebSocket 由 MessageList 独占管理。
  const messages = ref([]);

  // 流式输出状态 - 从 MessageList 组件获取
  const isLoading = ref(false);
  const isReceiving = ref(false);
  const isPaused = ref(false);
  const isStreaming = ref(false);
  const isDisplaying = ref(false);
  
  // 默认智能体名称
  const defaultAgent = ref();
  const pageLoading = ref(true);

  // 监听 MessageList 组件的状态变化
  function handleStateChange(state) {
    console.log('handleStateChange:', state);
    isStreaming.value = state.isStreaming;
    isPaused.value = state.isPaused;
    isDisplaying.value = state.isDisplaying;
    isLoading.value = state.isLoading;
    isReceiving.value = state.isReceiving;
  }

  // 消息页面是否是初始化状态
  const isInit = ref(true);

  // 页面当前智能体
  const currPageAgent = ref(null);

  const navBarTitle = computed(() => {
    if (isSessionMode.value) {
      return currPageAgent.value?.name || defaultAgent.name
    }

    // 综合模式显示默认智能体名称
    return defaultAgent.name
  });

  // 获取当前智能体id,默认海智1
  const agentId = computed(() => currPageAgent.value?.index || 1);

  // 是否session模式
  const isSessionMode = ref(false);

  // 交互模式是否就绪（getSessionMode 完成后置 true），用于延迟挂载 AgentSupermarket，
  // 避免子组件以默认 false 的模式先加载一遍 general 缓存再切 session
  const modeReady = ref(false);

// 跳转返回
function gotoBack() {
  // 先保存暂停状态再关闭
  if (messageListRef.value && messageListRef.value.streamState) {
    const streamState = messageListRef.value.streamState; // streamState 本身就是 ref
    if (streamState.taskId && (streamState.isStreaming || streamState.isPaused || streamState.pendingContent)) {
      const replyPosition = streamState.displayedContent.length;
      // 这里只保存真正的手动暂停状态，意外离开时 replyPaused 设为 false
      const replyPaused = streamState.isPaused;
      AiAgentChatPausePosition.savePosition(streamState.taskId, { replyPaused, replyPosition, agentId: currPageAgent.value?.index });
      console.log('gotoBack: 保存暂停状态', streamState.taskId, replyPosition, replyPaused);
    }
  }
  communicationStore.close();
}

// 跳转历史
function gotoHistory() {
  // 先保存暂停状态再跳转
  if (messageListRef.value && messageListRef.value.streamState) {
    const streamState = messageListRef.value.streamState; // streamState 本身就是 ref
    if (streamState.taskId && (streamState.isStreaming || streamState.isPaused || streamState.pendingContent)) {
      const replyPosition = streamState.displayedContent.length;
      // 这里只保存真正的手动暂停状态，意外离开时 replyPaused 设为 false
      const replyPaused = streamState.isPaused;
      AiAgentChatPausePosition.savePosition(streamState.taskId, { replyPaused, replyPosition, agentId: currPageAgent.value?.index });
      console.log('gotoHistory: 保存暂停状态', streamState.taskId, replyPosition, replyPaused);
    }
  }
  router.push('/pages/aiAssistantNew/sessionHistorys');
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
        userDepartments: res.userDepartments || [],
      };
    }
  } catch (error) {
    console.error(`获取用户信息错误: ${error.message}`);
  }
}

// 加载历史消息：type='' 首次加载，'loadMore' 上滑加载更早历史
async function loadHistoryMessages(type = '') {
  if (type !== 'loadMore') {
    lastId.value = '';
    hasMoreHistory.value = true;
  }
  if (!hasMoreHistory.value && type === 'loadMore') {
    messageListRef.value?.resetReachTop?.();
    return;
  }
  try {
    const agentId = isSessionMode.value ? currPageAgent.value?.index || '' : '';
    const res = await getRecordsByAgent({
      userId: userInfo?.value?.idCard || '',
      pageNo: 1,
      pageSize: HISTORY_PAGE_SIZE,
      agentId,
      lastId: type === 'loadMore' ? lastId.value : '',
    });
    const historyMessages = (res?.list || [])?.toReversed();
    const rawList = res?.list || [];

    if (rawList.length < HISTORY_PAGE_SIZE) {
      hasMoreHistory.value = false;
    }
    if (rawList.length > 0) {
      lastId.value = rawList[rawList.length - 1].id;
    }

    if (type === 'loadMore') {
      // 上滑加载更多：prepend 到列表前面
      // historyMessages 已通过 toReversed() 转为旧→新顺序，直接 map 即可，不要再 unshift 反转
      const newItems = historyMessages.map((record) => buildHistoryMessageItem(record, false));
      messageListRef.value?.prependMessages?.(newItems);
    } else {
      processHistoryMessages(historyMessages);
      isInit.value = historyMessages.length === 0;
      messageListRef.value?.setHasMoreHistory?.(hasMoreHistory.value);
    }
  } catch (error) {
    console.error('加载历史消息失败:', error)
    if (type === 'loadMore') {
      messageListRef.value?.resetReachTop?.();
    } else {
      isInit.value = true
    }
  }
}

// 构造单条历史消息对象
function buildHistoryMessageItem(record, isLastMessage) {
  let replyPosition = record.replyPosition;
  let isPaused = false;
  const frontendPauseData = AiAgentChatPausePosition.getPosition(record.id);
  if (frontendPauseData && frontendPauseData.replyPosition !== undefined) {
    replyPosition = frontendPauseData.replyPosition;
    isPaused = frontendPauseData.replyPaused !== false;
  } else {
    isPaused = !(
      record.replyPosition == -1
      || record.replyPaused == false
      || record.responseContent.includes('[end]')
    );
  }

  let fullResponseContent = record.responseContent.replace(/\[end\]/g, '');
  let isErrorContent = false;
  if (fullResponseContent.trim() === '[ERROR]') {
    fullResponseContent = '业务繁忙，请稍后再试';
    isErrorContent = true;
  }
  let responseContent = fullResponseContent;
  if (!isErrorContent && replyPosition > 0 && responseContent.length > replyPosition) {
    responseContent = responseContent.substring(0, replyPosition);
  }

  return {
    taskId: record.id,
    queryContent: record.queryContent,
    responseContent: responseContent,
    fullResponseContent: fullResponseContent,
    timestamp: record.latestTime ? new Date(record.latestTime).getTime() : Date.now(),
    replyPosition: replyPosition,
    isPaused: isLastMessage && isPaused,
    approvalRequired: record.approvalRequired,
    approveUrl: record.approveUrl,
    approveDetailUrl: record.approveDetailUrl,
    approvalStatus: record.approvalStatus,
    approveResult: record.approveResult,
    approvalSubMode: record.approvalSubMode,
    agentId: record.agentId,
    approveNo: '',
    toLeaderUrl: record.toLeaderUrl || '',
    approveUser: record.approveUser || '',
    isStreaming: false,
    needAutoContinue: false,
    attachement: record.attachement || '',
    attachement_path: record.attachement_path,
    agentName: record.agentName || '',
    userName: record.userName || '',
    answerTime: formatMsgTime(record.answerTime),
    queryTime: formatMsgTime(record.queryTime),
  };
}

// 上滑到顶部回调
async function handleReachTop() {
  if (!hasMoreHistory.value) {
    messageListRef.value?.resetReachTop?.();
    return;
  }
  await loadHistoryMessages('loadMore');
}

// 处理历史消息，恢复暂停状态
function processHistoryMessages(historyMessages) {
  // 最新的一条消息索引
  const lastIndex = historyMessages.length - 1;
  
  historyMessages.forEach((record, index) => {
    // 优先从前端存储获取暂停位置
    let replyPosition = record.replyPosition;
    let isPaused = false;
    
    // 检查前端是否有存储的暂停位置
    const frontendPauseData = AiAgentChatPausePosition.getPosition(record.id);
    if (frontendPauseData && frontendPauseData.replyPosition !== undefined) {
      replyPosition = frontendPauseData.replyPosition;
      isPaused = frontendPauseData.replyPaused !== false;
      console.log('processHistoryMessages: 使用前端存储的暂停位置', record.id, replyPosition, isPaused);
    } else {
      // 使用后端返回的暂停位置
      isPaused = !(
        record.replyPosition == -1
        || record.replyPaused == false
        || record.responseContent.includes('[end]')
      );
    }
    
    // 移除 [end] 标签
    let fullResponseContent = record.responseContent.replace(/\[end\]/g, '');
    let isErrorContent = false;
    
    if (fullResponseContent.trim() === '[ERROR]') {
      fullResponseContent = '业务繁忙，请稍后再试';
      isErrorContent = true;
    }
    
    let responseContent = fullResponseContent;
    if (!isErrorContent && replyPosition > 0 && responseContent.length > replyPosition) {
      responseContent = responseContent.substring(0, replyPosition);
    }
    
    // 只有最新的一条消息才设置 isPaused
    const isLastMessage = index === lastIndex;
    
    const messageItem = {
      taskId: record.id,
      queryContent: record.queryContent,
      responseContent: responseContent, 
      fullResponseContent: fullResponseContent,
      timestamp: record.latestTime ? new Date(record.latestTime).getTime() : Date.now(),
      replyPosition: replyPosition, 
      isPaused: isLastMessage && isPaused,
      approvalRequired: record.approvalRequired,
      approveUrl: record.approveUrl,
      approveDetailUrl: record.approveDetailUrl,
      approvalStatus: record.approvalStatus,
      approveResult: record.approveResult,
      approvalSubMode: record.approvalSubMode,
      agentId: record.agentId,
      agentConfigId: record.agentConfigId,
      approveNo: '',
      toLeaderUrl: record.toLeaderUrl || '',
      approveUser: record.approveUser || '',
      isStreaming: false,
      needAutoContinue: false,
      attachement: record.attachement || '',
      attachement_path: record.attachement_path,
      agentName: record.agentName || '',
      userName: record.userName || '',
      answerTime: formatMsgTime(record.answerTime),
      queryTime: formatMsgTime(record.queryTime),
    }
    
    messages.value.push(messageItem)
    
    // 只有最新的一条消息才处理继续生成逻辑
    if (isLastMessage) {
      // 有位置信息且内容未完成 → 标记为可恢复的流式状态
      if (replyPosition > 0 && replyPosition < fullResponseContent.length) {
        messageItem.isStreaming = true
        
        // 只有不是手动暂停的（isPaused=false），才自动恢复
        if (!isPaused) {
          messageItem.needAutoContinue = true;
          console.log('processHistoryMessages: 标记最新消息需要自动恢复', index, messageItem.taskId);
        }
      }
    }
  })
}

  function sendMessage(msg, sessionIds, files) {
  console.log('sendMessage: 0000000')
    console.log('sendMessage:', sessionIds);
    // 切换欢迎页/消息页
    if (isInit.value) isInit.value = false;

    const deptObj = userInfo?.value?.userDepartments?.[0] || {};
    console.log('sendMessage: 11111111', deptObj)
    nextTick(async () => {
      //等待ws返回wsSessionId
      let wsSessionId = undefined
    try {
      console.log('sendMessage: 222222222', deptObj)
      wsSessionId = (await messageListRef?.value?.wsSessionIdPromise) || '';
    } catch(error) {
        console.error('获取 wsSessionId 失败:', error)
    } finally {
      console.log('sendMessage: 33333333',  messageListRef);
        messageListRef.value?.createQuestion({
          content: msg,
          agent: currPageAgent?.value?.index || '',
          userName: userInfo?.value?.username || '',
          userID: userInfo?.value?.idCard || '',
          departmentCode: deptObj?.departmentCode || '',
          departmentId: deptObj?.departmentId || '',
          departmentName: deptObj?.departmentName || '',
          approver: deptObj?.approver || '',
          wsSessionId,
          sessionId: sessionIds.join(',') || '',//文件id
          files,
      });
      }
  });
  }

  // 暂停
  function handlePause() {
    if (isPaused.value) {
      messageListRef.value?.resumeStream?.();
    } else {
      messageListRef.value?.pauseStream?.();
    }
  }

  function handleAgentChange() {
    messageListRef.value?.pauseStream()
    messageListRef.value?.clearMessages?.()
    messages.value = []
    isStreaming.value = false
    isPaused.value = false
    isLoading.value = false
    isReceiving.value = false
    isDisplaying.value = false
  }

  function handleAgentSelect(agent) {
    currPageAgent.value = agent;
    if (isSessionMode.value) {
      handleAgentChange()
      loadHistoryMessages().then(() => {
        nextTick(() => {
          messageListRef.value?.setReachTopHandler?.(handleReachTop);
          messageListRef.value?.setHasMoreHistory?.(hasMoreHistory.value);
          messageListRef.value?.scrollToBottom(true)
        })
      })
    }
  }

  // 获取交互模式
  async function getSessionMode() {
    try {
      const interactionMode = await getGlobalsConfigByKey('AI_AGENT_INTERACTION');
      isSessionMode.value = interactionMode === 'session';
    } catch (error) {
      console.error('获取交互模式失败:', error);
      isSessionMode.value = false;
    }
  }
  function handleInputFocus() {
    setTimeout(() => {
      messageListRef.value?.scrollToBottom(true)
    }, 500)
  }
  
  // 初始化页面
  async function initPage() {
    const { index, name, picUrl, init } = route.query;
      const agentList = await getAgents({}) || [];
        if( agentList.length) {
          defaultAgent.value = agentList[0] || {};
        }
    if (isSessionMode.value) { // session 模式
      if (!index) {
        // icon进入：有缓存 → 缓存智能体+加载消息; 无缓存 → 默认智能体+欢迎页
        const cached = await getSelectedAgent('session');
        currPageAgent.value = cached || {...defaultAgent.value};
        modeReady.value = true;
        await loadHistoryMessages()
      } else if (String(init) === 'true') {
        // 智能体超市进入：路由参数读取智能体 → 查询历史消息，无历史则显示欢迎页
        currPageAgent.value = { index, name, picUrl };
        modeReady.value = true;
        await loadHistoryMessages();
      } else {
        // 历史会话进入：路由参数读取智能体 → 加载消息列表
        currPageAgent.value = { index, name, picUrl };
        modeReady.value = true;
        await loadHistoryMessages();
      }
    } else { // 综合模式
      // 优先使用缓存中选中的智能体，保持与 AgentSupermarket 内部选中状态一致，
      // 避免 NavBar 标题与 AgentSupermarket 选中项显示不一致
      const cached = await getSelectedAgent('general');
      currPageAgent.value = cached || { ...defaultAgent };
      modeReady.value = true;
      await loadHistoryMessages();
    }

    pageLoading.value = false;
    nextTick(() => {
      messageListRef.value?.setReachTopHandler?.(handleReachTop);
      messageListRef.value?.setHasMoreHistory?.(hasMoreHistory.value);
      messageListRef.value?.scrollToBottom(true)
    })
  }

  onMounted(async () => {
    await getSessionMode();
    await getUserInfo();
    await initPage();
  });

  // 监听路由参数变化，重新初始化页面
  watch(() => ({ ...route.query }), async (newQuery, oldQuery) => {
    const { index, init, _t } = newQuery;
    const isNewSession = (String(init) === 'true') && (_t !== oldQuery?._t || index !== oldQuery?.index);
    const agentChanged = index || newQuery.agentId;

    if (isNewSession || agentChanged) {
      pageLoading.value = true;
      await initPage();
    }
  }, { deep: true });
</script>

<style scoped lang="scss">
  /* 基础样式 */
  .ai-assistant-page {
    overflow: hidden;
    height: 100vh;
    display: flex;
    flex-direction: column;
    background: linear-gradient(
      180deg,
      rgba(204, 220, 249, 1) 0%,
      rgb(250, 250, 250) 50%,
      rgb(250, 250, 250) 75%,
      rgb(255, 255, 255) 80%,
      rgb(255, 255, 255) 100%
    );
  }

  .header {
    width: 100%;
  }

  .main {
    flex: 1;
    overflow: hidden;
    position: relative;
  }

  .footer {
    width: 100%;
    padding-bottom: env(safe-area-inset-bottom);
  }

  .page-loading {
    display: flex;
    align-items: center;
    justify-content: center;
    height: 100%;
  }

  .loading-spinner {
    width: 32px;
    height: 32px;
    border: 3px solid rgba(30, 82, 242, 0.2);
    border-top-color: rgba(30, 82, 242, 1);
    border-radius: 50%;
    animation: spin 0.8s linear infinite;
  }

  @keyframes spin {
    to {
      transform: rotate(360deg);
    }
  }
</style>
