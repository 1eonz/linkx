<script setup>
  import { nextTick, onMounted, onUnmounted, ref } from 'vue';

  import { aiApi } from '@/common/api/index.js';
  import { useAiStore } from '@/stores/ai.js';
  import { useCommunicationStore } from '@/stores/communication.js';

  import { marked } from 'marked';

  import aiAssistantPhone from './aiAssistantPhone.vue';

  const communicationStore = useCommunicationStore();
  const aiStore = useAiStore();

  // 组件自适应
  const CurrentComponent = ref();
  function updateComponentByScreenSize() {
    const width = window.innerWidth;
    CurrentComponent.value = aiAssistantPhone;
    // if (width >= 768) {
    //   CurrentComponent.value = aiAssistantWeb;
    // } else {
    //   CurrentComponent.value = aiAssistantPhone;
    // }
  }

  const agents = ref([]);

  const messages = ref([]);
  const functions = ref([]);
  const currentAgentIndex = ref(null);
  const isLoading = ref(false);
  const isReceiving = ref(false); // 正在回复
  const currentTaskId = ref(null);
  const userInfo = ref({
    idCard: null,
    thumbAvatar: '',
    userid: null,
    username: '',
  });

  onMounted(async () => {
    console.log('打开ai助手界面');
    updateComponentByScreenSize();
    window.addEventListener('resize', updateComponentByScreenSize);
    await getUserInfo();
    restoreConversation();
    preloadImages();
    agents.value = await aiStore.getAgentsList();
    functions.value = await aiStore.getCurrentFunc(agents.value);
    currentAgentIndex.value = await aiStore.cachedCurrentAgentIndex();
    // 存储变更监听
    window.WeSpaceSDK.onStorageChange(`functionsList_${userInfo.value.userid}`, async (val) => {
      console.log(
        'onStorageChange数据解码处理functionsList---------',
        JSON.stringify(JSON.parse(decodeURIComponent(escape(val))), null, 2),
      );
      functions.value = JSON.parse(decodeURIComponent(escape(val)));
    });
    window.WeSpaceSDK.onStorageChange(`currentAgentIndex_${userInfo.value.userid}`, async (val) => {
      console.log(
        'onStorageChange数据解码处理currentAgentIndex---------',
        JSON.stringify(JSON.parse(decodeURIComponent(escape(val))), null, 2),
      );
      currentAgentIndex.value = JSON.parse(decodeURIComponent(escape(val)));
    });
    window.WeSpaceSDK.onClose(() => {
      saveConversationState();
    });
  });

  onUnmounted(() => {
    console.log('aiAssistant组件onUnmountedt');
    window.removeEventListener('resize', updateComponentByScreenSize);
    communicationStore.removeStorageChange(`agentsList_${userInfo.value.userid}`);
    communicationStore.removeStorageChange(`currentAgentIndex_${userInfo.value.userid}`);
  });

  // 预加载图片
  function preloadImages() {
    const imageUrls = ['/static/ai/user_avatar.png', '/static/ai/ai_avatar.png'];
    if (userInfo.value?.thumbAvatar) {
      imageUrls.push(userInfo.value.thumbAvatar);
    }
    imageUrls.forEach((url) => {
      const img = new Image();
      img.src = url;
    });
  }

  // 选择智能体
  function handleFunction(func) {
    if (currentAgentIndex.value === func.index) {
      const targetItem = agents.value.find((item) => item.name?.toLowerCase() === 'deepseek');
      currentAgentIndex.value = targetItem?.index;
    } else {
      currentAgentIndex.value = func?.index;
    }
    aiStore.setCurrentAgent(currentAgentIndex.value);
  }

  // 滚动到底部
  async function scrollToBottom() {
    await nextTick();
    const chatContainer = document.querySelector('.chat-history');
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
        console.log('打印用户信息', JSON.stringify(userInfo.value, null, 2));
      } else {
        console.error('获取用户信息失败或WeSpaceSDK不可用');
      }
    } catch (error) {
      console.error(`获取用户信息错误: ${error.message}`);
    }
  }

  // 发送消息
  function sendMessage(message) {
    if (isLoading.value) return;
    const msg = message.trim();
    if (!msg) return;
    // 添加用户消息
    messages.value.push({
      content: msg,
      timestamp: Date.now(),
      type: 'user',
      userid: userInfo.value.userid,
    });
    saveConversationState();
    callAiApi(msg);
    scrollToBottom();
  }

  // 流式输出的状态管理
  const streamState = ref({
    displayedContent: '', // 当前显示内容
    isStreaming: false, // 是否正在流式输出
    pendingContent: '', // 待显示的内容
    rawContent: '', // 本地保存的完整内容
    taskId: null, // 任务标识
    timer: null, // 定时器引用
  });
  const ERROR_MESSAGE = '业务繁忙，请稍后再试';
  const ERROR_TIMEOUT = '查询超时';
  const SEARCHING_SHOW = '正在查询...';

  // 调用 AI API
  async function callAiApi(message) {
    try {
      isLoading.value = true;
      isReceiving.value = false;
      const params = {
        agent: currentAgentIndex.value,
        content: message,
        userID: userInfo.value.idCard, // 传递身份证号码
        userName: userInfo.value.username,
      };
      currentTaskId.value = await aiApi.newTask(params);

      // 清空之前的流式状态
      resetStreamState();

      // 使用流式输出
      await startStreamOutput(currentTaskId.value);

      isLoading.value = false;
      isReceiving.value = false;
      scrollToBottom();
    } catch (error) {
      console.error('调用 AI API 失败:', error);
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
    streamState.value.displayedContent = '';
    streamState.value.timer = null;
    streamState.value.rawContent = '';
    streamState.value.pendingContent = '';
  }

  // 开始流式输出
  async function startStreamOutput(taskId, isHistory = false) {
    const INTERVAL = 1000;
    const END_TAG = '[end]';
    const MAX_TIMEOUT = 5000; // 最长等待有效响应时间
    let lastResponseTime = Date.now(); // 上次收到有效响应的时间

    streamState.value.taskId = taskId;
    streamState.value.isStreaming = true;
    if (isHistory) {
      if (streamState.value.pendingContent) {
        startDisplayTimer();
      }
    } else {
      // 非退出重进的历史加载
      // 创建临时消息
      const tempMessage = {
        content: SEARCHING_SHOW,
        isStreaming: true,
        timestamp: Date.now(),
        type: 'ai',
        userid: userInfo.value.userid,
      };
      messages.value.push(tempMessage);
    }

    isReceiving.value = true;
    // 首次调用 getAnswer 前等待 200ms，给后端预留处理时间
    await new Promise((resolve) => setTimeout(resolve, 200));
    while (true) {
      try {
        const res = await aiApi.getAnswer({ id: taskId });
        console.log('API响应:', JSON.stringify(res, null, 2));

        // null 继续轮询
        if (!res) {
          // 检查无响应时间是否超时
          if (Date.now() - lastResponseTime > MAX_TIMEOUT) {
            console.warn('长时间无有效响应，终止轮询');
            handleError(ERROR_TIMEOUT);
            return;
          }
          continue;
        }
        // 收到有效响应，更新最后响应时间
        lastResponseTime = Date.now();

        // 检查错误响应：以 [ERROR] 结尾都停止轮询
        const cleanedRes = res.replace(END_TAG, '').trim();
        if (cleanedRes.endsWith('[ERROR]')) {
          if (cleanedRes === '[ERROR]') {
            handleError('业务繁忙，请稍后再试');
          } else {
            // xxxx [ERROR]：展示原始内容，停止轮询
            streamState.value.isStreaming = false;
            streamState.value.taskId = null;
            const targetIndex = messages.value.length - 1;
            if (targetIndex >= 0) {
              messages.value[targetIndex].content = cleanedRes;
              messages.value[targetIndex].isStreaming = false;
            }
          }
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

          // 确保所有内容都显示出来
          if (!streamState.value.timer && streamState.value.pendingContent) {
            streamState.value.displayedContent += streamState.value.pendingContent;
            streamState.value.pendingContent = '';
            updateMessageContent();
          }

          streamState.value.isStreaming = false;

          break;
        }
      } catch (error) {
        console.error('轮询错误:', error);
        handleError(ERROR_MESSAGE);
        return;
      }

      await new Promise((resolve) => setTimeout(resolve, INTERVAL));
    }
  }

  // 开始显示内容
  function startDisplayTimer() {
    streamState.value.timer = setInterval(() => {
      if (streamState.value.pendingContent) {
        // 每次显示一个字符
        const charToDisplay = streamState.value.pendingContent.charAt(0);
        streamState.value.displayedContent += charToDisplay;
        streamState.value.pendingContent = streamState.value.pendingContent.substring(1);
        updateMessageContent();
      } else {
        // 没有更多待显示的内容，清除定时器
        clearInterval(streamState.value.timer);
        streamState.value.timer = null;

        // 如果流式输出已完成，更新消息状态
        if (!streamState.value.isStreaming) {
          const lastIndex = messages.value.length - 1;
          if (lastIndex >= 0 && messages.value[lastIndex].isStreaming) {
            messages.value[lastIndex].isStreaming = false;
          }
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
        content: marked.parse(val),
        isStreaming: false,
        timestamp: Date.now(),
        type: 'ai',
        userid: userInfo.value.userid,
      };
    } else {
      messages.value.push({
        content: marked.parse(val),
        isStreaming: false,
        timestamp: Date.now(),
        type: 'ai',
        userid: userInfo.value.userid,
      });
    }

    scrollToBottom();
  }

  // 更新消息内容
  function updateMessageContent() {
    const lastIndex = messages.value.length - 1;
    if (lastIndex >= 0 && messages.value[lastIndex].isStreaming) {
      messages.value[lastIndex].content = marked.parse(streamState.value.displayedContent);
      scrollToBottom();
    }
  }

  // 从本地存储恢复对话状态
  async function restoreConversation() {
    try {
      const originData = await window.WeSpaceSDK.getStorage(`chatHistory_${userInfo.value.userid}`);
      const parsedState = JSON.parse(decodeURIComponent(escape(originData)));
      // const parsedState = uni.getStorageSync(
      //   `chatHistory_${userInfo.value.userid}`
      // );
      if (parsedState) {
        // 恢复消息历史
        messages.value = parsedState.messages || [];

        // 恢复流式输出状态
        streamState.value = parsedState.streamState;
        streamState.value.timer = null;
        const { displayedContent, isStreaming, pendingContent, rawContent, taskId } =
          streamState.value;

        const lastMessage = messages.value ? messages.value[messages.value.length - 1] : [];

        // 如果正在流式输出，重新启动定时器和API轮询
        if (isStreaming || pendingContent !== '') {
          if (pendingContent === '') {
            scrollToBottom();
          }
          if (taskId) {
            await startStreamOutput(taskId, true);
            isLoading.value = false;
          }
        } else if (lastMessage?.type === 'user') {
          messages.value.push({
            content: '查询失败',
            isStreaming: false,
            timestamp: Date.now(),
            type: 'ai',
            userid: userInfo.value.userid,
          });
        } else if (lastMessage?.type === 'ai' && lastMessage?.content === SEARCHING_SHOW) {
          messages.value[messages.value.length - 1] = {
            content: '查询失败',
            isStreaming: false,
            timestamp: Date.now(),
            type: 'ai',
            userid: userInfo.value.userid,
          };
        }
        scrollToBottom();
        console.log('对话状态已从本地存储恢复');
        return true;
      }
    } catch (error) {
      console.error('恢复对话状态失败:', error);
    }
    return false;
  }
  // 保存对话状态到本地存储
  const saveConversationState = () => {
    console.log('saveConversationState更新', new Date());
    try {
      const stateToSave = {
        messages: messages.value,
        streamState: streamState.value,
      };
      const escaped = unescape(encodeURIComponent(JSON.stringify(stateToSave)));
      window.WeSpaceSDK.setStorage(`chatHistory_${userInfo.value.userid}`, escaped);
      // uni.setStorageSync(`chatHistory_${userInfo.value.userid}`, stateToSave);
      console.log('对话状态已保存到本地存储');
    } catch (error) {
      console.error('保存对话状态失败:', error);
    }
  };

  // 复制
  function copyText(msg) {
    const textToCopy = extractText(msg);
    uni.setClipboardData({
      complete: () => {
        console.log('uni.setClipboardData执行完毕-------');
      },
      data: textToCopy,
      fail: (e) => {
        console.error('uni.setClipboardData复制失败----------', e);
        fallbackCopyTextToClipboard(textToCopy);
      },
      success: () => {
        console.log('uni.setClipboardData复制成功-------');
        uni.showToast({
          icon: 'none',
          title: '复制成功',
        });
      },
    });
  }
  function extractText(html) {
    if (typeof html !== 'string') return '';

    if (html.includes('<') && html.includes('>')) {
      return html.replaceAll(/<[^>]*>/g, '');
    }

    return html;
  }
  function fallbackCopyTextToClipboard(text) {
    const textArea = document.createElement('textarea');
    textArea.value = text;
    textArea.style.position = 'fixed';
    textArea.style.top = '-9999px';
    textArea.style.left = '-9999px';
    document.body.append(textArea);
    // 选中文本
    textArea.focus();
    textArea.select();

    try {
      const successful = document.execCommand('copy');
      if (!successful) {
        console.error('document.execCommand("copy")失败', document.execCommand('copy'));
        throw new Error('复制命令执行失败');
      }
      uni.showToast({ icon: 'none', title: '复制成功' });
    } catch (error) {
      uni.showToast({ icon: 'none', title: '复制失败，请稍后再试' });
      console.error('document.execCommand复制失败:', error);
    } finally {
      textArea.remove();
    }
  }
</script>

<template>
  <component
    :is="CurrentComponent"
    :current-agent-index="currentAgentIndex"
    :functions="functions"
    :is-loading="isLoading"
    :is-receiving="isReceiving"
    :messages="messages"
    :user-avatar="userInfo.thumbAvatar"
    @copy-text="copyText"
    @handle-function="handleFunction"
    @send-message="sendMessage"
  />
</template>
<style scoped></style>
