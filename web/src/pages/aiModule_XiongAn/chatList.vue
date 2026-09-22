<template>
  <div class="chat-list">
    <div class="chat-container">
      <div class="chat-messages">
        <div
          v-for="message in messages"
          :key="message.id"
          class="message-wrapper"
          :class="{ 'user-message': message.type === 'user', 'ai-message': message.type === 'ai' }"
          @contextmenu.prevent="onMessageContextMenu($event, message)"
        >
          <!-- AI消息在左侧 -->
          <div class="message-content ai-content" v-if="message.type === 'ai'">
            <div class="avatar ai-avatar">
              <!-- 会话模式下：如果有智能体头像，优先显示智能体头像；否则使用默认头像 -->
              <AgentAuthImg
                v-if="props.sessionMode && props.agentPicUrl"
                :pic-url="props.agentPicUrl"
                img-class="ai-avatar-img"
              />
              <img v-else :src="aiAvatarSrc" class="ai-avatar-img" />
            </div>
            <div class="message-bubble ai-bubble">
              <div class="message-text" v-html="message.content"></div>
            </div>
          </div>
          <!-- 继续按钮，只在暂停状态且是最新的AI消息时显示 -->
          <div v-if="streamState.isPaused && message.type === 'ai' && message === messages[messages.length - 1]" class="resume-btn-wrapper" >
            <el-button class="resume-btn" type="primary" plain round @click="resumeStream" >继续生成</el-button>
          </div>

          <!-- 用户消息在右侧 -->
          <div v-if="message.type === 'user'" class="message-content user-content">
            <div class="message-bubble user-bubble">
              <div class="message-text">{{ message.content }}</div>
            </div>
            <div class="avatar user-avatar">
              <img :src="userInfo.avatar" class="avatar-img" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右键菜单 -->
    <div
      v-if="contextMenu.visible"
      class="context-menu"
      :style="{ top: contextMenu.position.y + 'px', left: contextMenu.position.x + 'px' }"
      @click.stop
    >
      <div class="context-menu-item" @click="copyCurrentMessage">复制</div>
    </div>
  </div>
</template>

<script setup lang="ts">
  import { computed, onUnmounted, onMounted, ref, watch } from 'vue';
  import { usePIMStore } from '@/store/modules/pim';
  import { newTask, getAnswer } from '@/api/ai';
  import aiHeader from '@/assets/images/ai/ai_header.png';
  import { useAutoScrollToBottom } from '@/hooks';
  import AgentAuthImg from './AgentAuthImg.vue';
  // import { marked } from 'marked';

  // 简单的 HTML 转换函数
  function parseMarkdown(text: string): string {
    // 简单的 markdown 到 HTML 转换
    return text
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/\*(.*?)\*/g, '<em>$1</em>')
      .replace(/`(.*?)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br>');
  }

  const props = defineProps<{
    activeModuleId: string;
    sessionMode?: boolean;
    sessionId?: string | null;
    agentId?: string;
    agentPicUrl?: string;
  }>();

  const emit = defineEmits<{
    'update:inputValue': [value: string];
  }>();

  const pimStore = usePIMStore();

  // 内部状态：追踪最新的 sessionId 和 agentId，确保保存到正确的 key
  // 解决 props 更新延迟导致 saveConversationState 保存到错误 key 的问题
  const internalSessionId = ref<string | null>(null);
  const internalAgentId = ref<string>('');

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

  const isLoading = ref(false);
  const isReceiving = ref(false);
  const currentTaskId = ref('');
  const messages = ref(<any>[]);
  const ERROR_MESSAGE = '业务繁忙，请稍后再试';
  const ERROR_TIMEOUT = '查询超时';
  const SEARCHING_SHOW = '正在查询...';

  // 右键菜单状态
  const contextMenu = ref({
    visible: false,
    position: { x: 0, y: 0 },
    targetText: '',
  });

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
      const lastAiIndex = findLastAiMessageIndex();
      if (lastAiIndex >= 0) {
        messages.value[lastAiIndex].content = parseMarkdown(finalText);
        messages.value[lastAiIndex].isStreaming = false;
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
  useAutoScrollToBottom('.chat-messages');

  const userInfo = computed(() => pimStore.user || {});
  const userId = computed(() => pimStore.user?.userid || '');
  // 会话模式下 AI 头像：优先使用当前智能体头像，没有则使用默认头像
  const aiAvatarSrc = computed(() => {
    if (props.sessionMode && props.agentPicUrl) {
      // 有智能体头像时，模板中会优先走 AuthImg 渲染，这里返回值仅作为兜底不使用
      return aiHeader;
    }
    return aiHeader;
  });

  onMounted(() => {
    // 会话模式下，只在有明确 sessionId 时才自动恢复
    // 父组件会在 handleHistoryClick 中显式调用 restoreConversation
    if (!props.sessionMode || props.sessionId) {
      restoreConversation();
    }

    // 点击空白处关闭菜单
    document.addEventListener('click', hideContextMenu);
    window.addEventListener('scroll', hideContextMenu, true);
  });

  onUnmounted(() => {
    // 清理节流定时器，但先立即保存最新状态
    if (saveThrottleTimer) {
      clearTimeout(saveThrottleTimer);
      saveThrottleTimer = null;
    }
    // 立即保存当前状态（包括最新的 displayedContent）
    saveConversationState();

    document.removeEventListener('click', hideContextMenu);
    window.removeEventListener('scroll', hideContextMenu, true);
  });

  // 发送消息
  function sendMessageFunc(inputValue: string) {
    if (isLoading.value) return;
    if (inputValue) {
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

      // 添加用户消息
      messages.value.push({
        type: 'user',
        content: inputValue,
        timestamp: Date.now(),
        userid: userId.value,
      });
      saveConversationState();
      callAiApi(inputValue);
      emit('update:inputValue', '');
    }
  }

  // 调用 AI API
  async function callAiApi(message) {
    try {
      isLoading.value = true;
      isReceiving.value = false;
      const params: any = {
        content: message,
        agent: props.activeModuleId,
        userName: userInfo.value.username,
        userID: userInfo.value.idCard, // 传递身份证号码
      };
      if(userInfo.value?.department){
        params.departmentId = userInfo.value.department?.departmentId;
        params.departmentCode = userInfo.value.department?.departmentCode;
        params.departmentName = userInfo.value.department?.departmentName;
      }
      
      // 清空之前的流式状态并立即设置为正在流式输出
      resetStreamState();
      streamState.value.isStreaming = true; // 立即设置为流式输出状态，确保暂停按钮及时显示
      
      const { code, data } = await newTask(params);
      isLoading.value = false;
      
      if (code === 0) {
        currentTaskId.value = data;

        // 使用流式输出
        await startStreamOutput(currentTaskId.value);
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

  // 右键菜单 - 展示
  function onMessageContextMenu(event: MouseEvent, message: any) {
    event.preventDefault();
    const text = getPlainTextFromMessage(message);
    contextMenu.value.targetText = text;

    // 计算菜单位置，避免超出视窗
    const menuWidth = 120;
    const menuHeight = 40;
    let x = event.clientX;
    let y = event.clientY;
    const vw = window.innerWidth;
    const vh = window.innerHeight;
    if (x + menuWidth > vw) x = vw - menuWidth - 8;
    if (y + menuHeight > vh) y = vh - menuHeight - 8;
    contextMenu.value.position = { x, y };
    contextMenu.value.visible = true;
  }

  function hideContextMenu() {
    contextMenu.value.visible = false;
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

  // 复制到剪贴板
  async function copyCurrentMessage() {
    const text = contextMenu.value.targetText || '';
    try {
      if (navigator.clipboard && window.isSecureContext) {
        await navigator.clipboard.writeText(text);
      } else {
        legacyCopyText(text);
      }
    } catch (e) {
      console.error('复制失败:', e);
      try {
        // 本页回退
        legacyCopyText(text);
      } catch (e2) {
        console.error('回退复制也失败:', e2);
        try {
          // iframe 场景：尝试在顶层页面复制（同源可用）
          if (isInIframe() && isSameOriginTop()) {
            if (
              window.top &&
              window.top.navigator &&
              window.top.isSecureContext &&
              window.top.navigator.clipboard
            ) {
              await window.top.navigator.clipboard.writeText(text);
            } else if (window.top && window.top.document) {
              legacyCopyTextTop(text);
            }
          } else {
            throw new Error('顶层不可用或跨域');
          }
        } catch (e3) {
          console.error('顶层复制失败:', e3);
          // 最终回退：提示用户手动复制
          try {
            window.prompt('复制以下文本（Ctrl+C / ⌘+C）：', text);
          } catch (_) {
            // 忽略
          }
        }
      }
    } finally {
      hideContextMenu();
    }
  }

  function legacyCopyText(text: string) {
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.setAttribute('readonly', '');
    ta.style.position = 'fixed';
    ta.style.top = '-1000px';
    ta.style.left = '-1000px';
    ta.style.opacity = '0';
    document.body.append(ta);

    // 选择文本（兼容 iOS）
    const selection = window.getSelection();
    if (selection) {
      selection.removeAllRanges();
      const range = document.createRange();
      range.selectNodeContents(ta);
      selection.addRange(range);
    }
    ta.focus();
    ta.select();

    const successful = document.execCommand('copy');
    ta.remove();
    if (!successful) {
      throw new Error('execCommand(copy) 返回失败');
    }
  }

  function legacyCopyTextTop(text: string) {
    if (!window.top || !window.top.document) throw new Error('无顶层 document');
    const doc = window.top.document as Document;
    const ta = doc.createElement('textarea');
    ta.value = text;
    ta.setAttribute('readonly', '');
    ta.style.position = 'fixed';
    ta.style.top = '-1000px';
    ta.style.left = '-1000px';
    ta.style.opacity = '0';
    doc.body.append(ta);
    const selection = window.top.getSelection();
    if (selection) {
      selection.removeAllRanges();
      const range = doc.createRange();
      range.selectNodeContents(ta);
      selection.addRange(range);
    }
    ta.focus();
    ta.select();
    const successful = doc.execCommand('copy');
    ta.remove();
    if (!successful) {
      throw new Error('顶层 execCommand(copy) 返回失败');
    }
  }

  function isInIframe(): boolean {
    try {
      return window.self !== window.top;
    } catch {
      return true; // 跨域时访问抛错，视为在 iframe 中
    }
  }

  function isSameOriginTop(): boolean {
    try {
      // 同源才能访问 top.location.origin
      return window.top?.location?.origin === window.location.origin;
    } catch {
      return false;
    }
  }

  // 暂停流式输出
  function pauseStream() {
    if (streamState.value.timer) {
      clearInterval(streamState.value.timer);
      streamState.value.timer = null;
    }
    streamState.value.isPaused = true;
    isReceiving.value = false;
  }

  // 继续流式输出：如果有待显示的内容，重新启动显示定时器
  function resumeStream() {
    streamState.value.isPaused = false;
    if (streamState.value.pendingContent && !streamState.value.timer) {
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
    streamState.value.serverDone = false;
    streamState.value.displayedContent = '';
    streamState.value.timer = null;
    streamState.value.rawContent = '';
    streamState.value.pendingContent = '';
    streamState.value.taskId = null;
  }

  // 开始流式输出
  async function startStreamOutput(taskId, isHistory = false) {
    const myToken = ++streamLoopToken.value;
    const INTERVAL = 1000;
    const END_TAG = '[end]';
    const MAX_TIMEOUT = 5000; // 最长等待有效响应时间
    let lastResponseTime = Date.now(); // 上次收到有效响应的时间

    streamState.value.taskId = taskId;
    streamState.value.isStreaming = true;
    // 只要开始轮询，就认为服务端尚未结束（会在收到 [end] 时置 true）
    streamState.value.serverDone = false;
    if (!isHistory) {
      // 非退出重进的历史加载
      // 创建临时消息
      const tempMessage = {
        type: 'ai',
        content: SEARCHING_SHOW,
        timestamp: Date.now(),
        userid: userId.value,
        isStreaming: true,
      };
      messages.value.push(tempMessage);
      // 立即保存一次，避免“需要持续输出一段时间才写入历史”的体验问题
      // （原逻辑依赖 updateMessageContent 的 1s 节流，导致本地缓存短时间只有 user 消息）
      saveConversationState();
    } else {
      // 历史恢复模式：如果有待显示的内容，直接开始显示
      if (streamState.value.pendingContent) {
        startDisplayTimer();
      }
      // 如果已有显示内容但没有待显示内容，说明显示已完成，直接结束
      else if (streamState.value.displayedContent && !streamState.value.isStreaming) {
        return;
      }
    }

    isReceiving.value = true;
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
        
        // 提取 data 字段，兼容 null、undefined、空字符串
        let res = response?.data;
        
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
          if (Date.now() - lastResponseTime > MAX_TIMEOUT) {
            console.warn('长时间无有效响应，终止轮询');
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

        // 检查错误响应
        if (res.includes('ERROR')) {
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

        // 提取增量内容
        let newContent = res;
        let isEnd = false;

        // 检查是否包含结束标记
        if (res.includes(END_TAG)) {
          isEnd = true;
          newContent = res.replace(END_TAG, '');
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
            const lastIndex = messages.value.length - 1;
            if (lastIndex >= 0) {
              // 确保消息内容是最新的
              if (streamState.value.rawContent || streamState.value.displayedContent) {
                const finalContent = streamState.value.rawContent || streamState.value.displayedContent;
                messages.value[lastIndex].content = parseMarkdown(finalContent);
              }
              messages.value[lastIndex].isStreaming = false;
            }
          }

          // 先保存一次：至少把 rawContent/pendingContent/serverDone/taskId 写进缓存，便于刷新后继续显示
          saveConversationState();

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
        }
        streamState.value.timer = null;
        streamState.value.isPaused = false;

        // 当 pendingContent 显示完成：
        // - 如果服务端已经结束（serverDone），这里才真正把会话标记为完成并落盘
        // - 如果服务端未结束，说明只是短暂停顿，等下一轮 pendingContent 进来再继续
        if (streamState.value.serverDone) {
          streamState.value.isStreaming = false;
          const lastIndex = messages.value.length - 1;
          if (lastIndex >= 0 && messages.value[lastIndex].isStreaming) {
            messages.value[lastIndex].isStreaming = false;
          }
        saveConversationState();
        }
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
    if (messages.value[lastIndex].type === 'user') {
      lastIndex = lastIndex + 1;
    }
    if (lastIndex >= 0) {
      messages.value[lastIndex] = {
        type: 'ai',
        content: parseMarkdown(val),
        timestamp: Date.now(),
        userid: userId.value,
        isStreaming: false,
      };
    } else {
      messages.value.push({
        type: 'ai',
        content: parseMarkdown(val),
        timestamp: Date.now(),
        userid: userId.value,
        isStreaming: false,
      });
    }

    // 错误处理后保存消息状态
    saveConversationState();
  }

  // 更新消息内容
  function updateMessageContent() {
    const lastIndex = messages.value.length - 1;
    if (lastIndex >= 0 && messages.value[lastIndex].isStreaming) {
      messages.value[lastIndex].content = parseMarkdown(streamState.value.displayedContent);

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
    const lastMessage = messages.value ? messages.value[messages.value.length - 1] : null;
    
    // 如果有原始内容或待显示内容，使用它们
    if (rawContent || pendingContent) {
      // 优先使用rawContent（完整内容）
      let finalContent = rawContent || '';
      
      // 如果已有显示内容，计算剩余未显示的内容
      if (displayedContent && finalContent) {
        // 确保rawContent包含已显示的内容
        if (finalContent.startsWith(displayedContent)) {
          // 计算剩余未显示的内容
          const remainingContent = finalContent.substring(displayedContent.length);
          if (remainingContent) {
            streamState.value.pendingContent = remainingContent;
            streamState.value.rawContent = finalContent;
          } else {
            // 没有剩余内容，直接完成
            streamState.value.displayedContent = finalContent;
            streamState.value.pendingContent = '';
            streamState.value.isStreaming = false;
            if (lastMessage?.type === 'ai' && lastMessage?.isStreaming) {
              messages.value[messages.value.length - 1].content = parseMarkdown(finalContent);
              messages.value[messages.value.length - 1].isStreaming = false;
            }
            saveConversationState();
            return;
          }
        } else {
          // 内容不连续，使用rawContent作为待显示内容
          streamState.value.pendingContent = finalContent;
          streamState.value.displayedContent = '';
        }
      } else if (finalContent && !displayedContent) {
        // 如果有原始内容但没有显示内容，全部作为待显示内容
        streamState.value.pendingContent = finalContent;
        streamState.value.rawContent = finalContent;
      } else if (pendingContent && !finalContent) {
        // 只有pendingContent，使用它
        streamState.value.pendingContent = pendingContent;
        if (!streamState.value.rawContent) {
          streamState.value.rawContent = displayedContent + pendingContent;
        }
      }
      
      // 确保最后一条消息是AI消息且处于流式状态
      if (lastMessage?.type === 'ai' && lastMessage?.isStreaming) {
        // 如果有已显示内容，先更新
        if (displayedContent) {
          messages.value[messages.value.length - 1].content = parseMarkdown(displayedContent);
        }
        // 开始流式显示剩余内容（与综合模式统一）
        if (streamState.value.pendingContent) {
          streamState.value.isStreaming = true;
          startDisplayTimer();
        } else {
          // 没有待显示内容，直接完成
          streamState.value.isStreaming = false;
          messages.value[messages.value.length - 1].isStreaming = false;
          if (streamState.value.rawContent) {
            messages.value[messages.value.length - 1].content = parseMarkdown(streamState.value.rawContent);
          }
          saveConversationState();
        }
      } else if (lastMessage?.type === 'user') {
        // 如果最后一条是用户消息，创建新的AI消息
        const tempMessage = {
          type: 'ai',
          content: displayedContent ? parseMarkdown(displayedContent) : SEARCHING_SHOW,
          timestamp: Date.now(),
          userid: userId.value,
          isStreaming: true,
        };
        messages.value.push(tempMessage);
        // 开始流式显示（与综合模式统一）
        if (streamState.value.pendingContent) {
          streamState.value.isStreaming = true;
          startDisplayTimer();
        } else {
          // 没有待显示内容，直接完成
          streamState.value.isStreaming = false;
          messages.value[messages.value.length - 1].isStreaming = false;
          if (streamState.value.rawContent) {
            messages.value[messages.value.length - 1].content = parseMarkdown(streamState.value.rawContent);
          }
          saveConversationState();
        }
      }
      
      // 监听定时器完成，标记流式输出完成
      const checkComplete = () => {
        if (!streamState.value.pendingContent && !streamState.value.timer) {
          streamState.value.isStreaming = false;
          const lastIndex = messages.value.length - 1;
          if (lastIndex >= 0 && messages.value[lastIndex].isStreaming) {
            messages.value[lastIndex].isStreaming = false;
          }
          // 确保所有内容都显示
          if (streamState.value.rawContent) {
            messages.value[messages.value.length - 1].content = parseMarkdown(streamState.value.rawContent);
          }
          saveConversationState();
        } else if (streamState.value.timer) {
          // 定时器还在运行，继续检查
          setTimeout(checkComplete, 100);
        }
      };
      // 延迟检查，确保定时器已启动
      setTimeout(checkComplete, 200);
    } else {
      // 没有已保存内容，显示查询失败
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
              content: parseMarkdown(finalContent),
              timestamp: Date.now(),
              userid: userId.value,
              isStreaming: !!isStreaming,
            });
          }
        }

        const lastMessage = messages.value ? messages.value[messages.value.length - 1] : [];

        // 会话模式下：如果存在未完成的显示/轮询，尝试继续完成
        if (props.sessionMode && (isStreaming || pendingContent !== '' || rawContent)) {
          // 如果有已显示的内容，先更新最后一条消息的内容
          if (displayedContent && lastMessage?.type === 'ai' && lastMessage?.isStreaming) {
            messages.value[messages.value.length - 1].content = parseMarkdown(displayedContent);
          }

          // 如果服务端已结束（serverDone），只需要把 pendingContent 继续显示完，不要重新轮询
          if (serverDone) {
            if (pendingContent) {
              streamState.value.isStreaming = true; // 本地仍在显示中
              startDisplayTimer();
            } else {
              // 没有待显示内容：直接收尾
              streamState.value.isStreaming = false;
              const lastIndex = messages.value.length - 1;
              if (lastIndex >= 0 && messages.value[lastIndex]?.isStreaming) {
                messages.value[lastIndex].isStreaming = false;
              }
              saveConversationState();
            }
          }
          // 服务端未结束：如果有 taskId，尝试继续轮询
          else if (taskId) {
            try {
              await startStreamOutput(taskId, true);
              isLoading.value = false;
            } catch (error) {
              // 如果轮询失败，尝试显示已保存的内容
              console.warn('继续轮询失败，尝试显示已保存内容:', error);
              isLoading.value = false;
              completeWithSavedContent();
            }
          } else {
            // 没有taskId，直接显示已保存的内容
            completeWithSavedContent();
          }
        } else if (isStreaming || pendingContent !== '') {
          // 综合模式：如果正在流式输出，重新启动定时器和API轮询
          // 如果有已显示的内容，先更新最后一条消息的内容
          if (displayedContent && lastMessage?.type === 'ai' && lastMessage?.isStreaming) {
            messages.value[messages.value.length - 1].content = parseMarkdown(displayedContent);
          }

          if (pendingContent === '') {
          }
          if (taskId) {
            await startStreamOutput(taskId, true);
            isLoading.value = false;
          }
        } else if (lastMessage?.type === 'user') {
          messages.value.push({
            type: 'ai',
            content: '查询失败',
            timestamp: Date.now(),
            userid: userId.value,
            isStreaming: false,
          });
        } else if (lastMessage?.type === 'ai' && lastMessage?.content === SEARCHING_SHOW) {
          messages.value[messages.value.length - 1] = {
            type: 'ai',
            content: '查询失败',
            timestamp: Date.now(),
            userid: userId.value,
            isStreaming: false,
          };
        }
        
        return true;
      }
    } catch (error) {
      console.error('恢复对话状态失败:', error);
    }
    return false;
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
        const lastIndex = messages.value.length - 1;
        const lastMsg = lastIndex >= 0 ? messages.value[lastIndex] : null;
        if (lastMsg?.type === 'ai' && lastMsg?.isStreaming) {
          lastMsg.content = parseMarkdown(streamState.value.displayedContent);
        } else if (lastMsg?.type === 'user') {
          // 边界兜底：如果还没插入占位 AI 消息（例如 props.sessionId 迟到导致前面逻辑提前 return），这里补一条
          messages.value.push({
            type: 'ai',
            content: parseMarkdown(streamState.value.displayedContent),
            timestamp: Date.now(),
            userid: userId.value,
            isStreaming: true,
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
  });
</script>

<style scoped lang="less">
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

    .resume-btn-wrapper {
      width: 100%;
      margin-top: 10px;
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
      flex-direction: row;
    }
  }

  .message-bubble {
    max-width: calc(100% - 59px);
    padding: 12px 16px;
    line-height: 1.4;
    word-wrap: break-word;
    border-radius: 18px;

    &.user-bubble {
      margin-left: 5px;
      color: white;
      background: var(--ai-message-bg);
      border-top-right-radius: 4px;
    }

    &.ai-bubble {
      margin-right: 8px;
      color: var(--text-color);
      background: var(--ai-message-bg2);
      border-top-left-radius: 4px;
      box-shadow: 0 2px 8px rgb(0 0 0 / 10%);
    }
  }

  .message-text {
    font-size: 14px;
    line-height: 1.5;
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

    &.ai-avatar {
      margin-right: 10px;

      .ai-avatar-img {
        width: 45px;
        height: 45px;
        border-radius: 50%;
      }
    }
  }

  /* 右键菜单样式 */
  .context-menu {
    position: fixed;
    z-index: 9999;
    min-width: 120px;
    padding: 6px 0;
    color: var(--text-color);
    background: var(--ai-message-bg2);
    backdrop-filter: saturate(180%) blur(6px);
    border: 1px solid rgb(0 0 0 / 8%);
    border-radius: 8px;
    box-shadow: 0 6px 20px rgb(0 0 0 / 12%);
  }

  .context-menu-item {
    padding: 8px 14px;
    font-size: 14px;
    cursor: pointer;
    user-select: none;
  }

  .context-menu-item:hover {
    color: #fff;
    background: var(--ai-message-bg);
  }

  .resume-btn-wrapper {
    max-width: 96%;
    display: flex;
    justify-content: flex-end;
    margin-top: 10px;
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
</style>
