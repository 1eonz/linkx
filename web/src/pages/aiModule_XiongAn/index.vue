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
              />
            </div>
            <!-- 没有消息时：提示文字+输入框居中显示 -->
            <div v-if="showEmptyHeader" class="empty-center-wrapper">
              <div class="empty-tip">👋嗨，今天有什么我可以帮你的吗？</div>
              <div class="footer-input">
                <div class="input-wrapper">
                  <input
                    type="text"
                    v-model="inputValue"
                    placeholder="输入您需要查找的内容"
                    class="search-input"
                    @keyup.enter="keyupEnterMsg"
                  />
                  <div
                    class="send-btn"
                    :class="{ disabled: isSendDisabled }"
                    @click="!isSendDisabled && sendMessage()"
                  >
                    <img :src="getImageSrc()" alt="发送" />
                  </div>
                </div>
              </div>
            </div>
            <!-- 有消息时：底部输入框 -->
            <div v-else class="footer-wrap">
              <div class="footer-input">
                <div class="input-wrapper">
                  <input
                    type="text"
                    v-model="inputValue"
                    placeholder="输入您需要查找的内容"
                    class="search-input"
                    @keyup.enter="keyupEnterMsg"
                  />
                  <!-- 暂停按钮 -->
                  <div v-if="(isStreaming || isDisplaying) && !isPaused" class="send-btn pause-btn" @click="pausedMessage">
                    <img :src="getPuaseSrc()" alt="暂停" />
                  </div>
                  <div
                    v-else
                    class="send-btn"
                    :class="{ disabled: isSendDisabled }"
                    @click="!isSendDisabled && sendMessage()"
                  >
                    <img :src="getImageSrc()" alt="发送" />
                  </div>
                </div>
              </div>
            </div>
          </template>
        </div>
      </div>
    </template>
    <!-- 综合模式：原有布局 -->
    <template v-else>
      <div class="all-wrap">
        <div v-if="showEmptyHeader" class="empty-header">
          <img :src="currentTheme === 'light' ? AiNav : AiNavDark" class="header-img" />
        </div>
        <ChatList
          v-show="!showEmptyHeader"
          ref="chatListRef"
          v-model:inputValue="inputValue"
          v-model:activeModuleId="activeModuleId"
        />
      </div>
      <div class="footer-wrap">
        <ModulesList
          v-model:selectedModules="selectedModules"
          v-model:activeModuleId="activeModuleId"
          @module-click="handleModuleClick"
        />
        <div class="footer-input">
          <div class="input-wrapper">
            <input
              type="text"
              v-model="inputValue"
              placeholder="输入您需要查找的内容"
              class="search-input"
              @keyup.enter="keyupEnterMsg"
            />
            <!-- 暂停按钮 -->
            <div v-if="(isStreaming || isDisplaying) && !isPaused" class="send-btn pause-btn" @click="pausedMessage">
              <div class="pause-item-wrap"> 
                <span class="pause-item"></span>
              </div>
            </div>
            <div
              v-else
              class="send-btn"
              :class="{ disabled: messageLoading }"
              @click="!messageLoading && sendMessage()"
            >
              <img :src="getImageSrc()" alt="发送" />
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script lang="ts" setup>
  import { ref, computed, onMounted, watch, nextTick } from 'vue';
  import { themeService } from '../../data/useTheme';
  import ModulesList from './modulesList.vue';
  import ChatList from './chatList.vue';
  import SessionSidebar from './SessionSidebar.vue';
  import { getGlobalsList } from '@/api/dictionary';
  import { usePIMStore } from '@/store/modules/pim';
  import { generateUUID, checkIsSessionMode } from '@/utils';
  import AiNav from '@/assets/images/ai/ai_nav.png';
  import AiNavDark from '@/assets/images/ai/ai_nav_dark.png';
  import Send from '@/assets/images/ai/send.png';
  import SendDark from '@/assets/images/ai/send_dark.png';
  import SendDisabled from '@/assets/images/ai/send_disabled.png';
  import SendDisabledDark from '@/assets/images/ai/send_disabled_dark.png';
  import AiPause from '@/assets/svg/aiPause.svg';
  import AiPauseDisabled from '@/assets/svg/aiPauseDisabled.svg';
  import AiSendPause from '@/assets/svg/ai_send_pause.svg';
  import { useLocaAgentId } from "../aiModule/hooks/useLocaAgentId"

  const pimStore = usePIMStore();
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
  const currentAgentId = ref<string>('');
  const historyList = ref<any[]>([]);
  // 当前智能体头像（会话模式下，优先取选中智能体，其次取当前会话摘要里的头像）
  const currentAgentPicUrl = computed(() => {
    if (!isSessionMode.value) return '';
    // 优先根据当前智能体 ID 在已选智能体列表中查找
    if (currentAgentId.value && currentAgentId.value !== '-1') {
      const fromSelected =
        selectedModules.value.find((m: any) => m.index === currentAgentId.value) || null;
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
  // 会话模式下按钮是否禁用（输入为空或正在加载）
  const isSendDisabled = computed(() => {
    if (isSessionMode.value) {
      return !inputValue.value?.trim() || messageLoading.value;
    }
    return messageLoading.value;
  });

  // 获取当前智能体名称
  const currentAgentName = computed(() => {
    if (!isSessionMode.value || !currentAgentId.value || currentAgentId.value === '-1') {
      return '';
    }
    // 先从 selectedModules 中查找
    const agent = selectedModules.value.find((m: any) => m.index === currentAgentId.value);
    if (agent && agent.name) {
      return agent.name;
    }
    // 如果没找到，从历史记录中查找
    const historyItem = historyList.value.find((h: any) => h.agentIndex === currentAgentId.value);
    if (historyItem && historyItem.agentName) {
      return historyItem.agentName;
    }
    return '';
  });

  onMounted(async () => {
    getTheme();
    await checkSessionMode();
    if (isSessionMode.value) {
      loadHistoryList();
    }
  });

  const { getLocaAgentId } = useLocaAgentId()

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
          if (Array.isArray(cachedSelected) && cachedSelected.length > 0) {
            selectedModules.value = cachedSelected.slice(0, 3);
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

  // 检查是否为会话模式
  async function checkSessionMode() {
    try {
      const { code, data } = await getGlobalsList();
      console.log(data, 'data-getGlobalsList')
      if (code === 0 && data) {
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
  function loadHistoryList() {
    if (!userId.value) return;
    try {
      const summaryKey = `chatSummary_${userId.value}_session`;
      const summaryData = localStorage.getItem(summaryKey);
      if (summaryData) {
        const summary = JSON.parse(decodeURIComponent(escape(summaryData)));
        // 迁移/补齐：老数据可能没有 lastUserMessage，这里尝试从 chatHistory 中推导一次并写回 summary
        let summaryChanged = false;
        const tryGetLastUserFromHistory = (sessionId: string, agentIndex?: string) => {
          try {
            const agentId = agentIndex || '-1';
            const historyKey = `chatHistory_${sessionId}_${userId.value}_${agentId}`;
            const originData = localStorage.getItem(historyKey);
            if (!originData) return null;
            const parsed = JSON.parse(decodeURIComponent(escape(originData)));
            const msgs = parsed?.messages || [];
            for (let i = msgs.length - 1; i >= 0; i -= 1) {
              const m = msgs[i];
              if (m?.type === 'user' && m?.content) return m;
            }
          } catch (e) {
            // ignore
          }
          return null;
        };
        // 转换为历史列表格式
        historyList.value = Object.keys(summary).map((key) => {
          const item = summary[key];
          // sidebar 预览：优先展示最后一条 user 提问
          const lastUserMessage =
            item.lastUserMessage ||
            (item.lastMessage?.type === 'user' ? item.lastMessage : null) ||
            tryGetLastUserFromHistory(key, item.agentIndex);
          if (lastUserMessage && !item.lastUserMessage) {
            item.lastUserMessage = lastUserMessage;
            summaryChanged = true;
          }
          
          // 尝试从 chatHistory 中获取最新的 AI 消息状态（用于判断 isStreaming）
          const tryGetLatestAiMessageFromHistory = (sessionId: string, agentIndex?: string) => {
            try {
              const agentId = agentIndex || '-1';
              const historyKey = `chatHistory_${sessionId}_${userId.value}_${agentId}`;
              const originData = localStorage.getItem(historyKey);
              if (!originData) return null;
              const parsed = JSON.parse(decodeURIComponent(escape(originData)));
              const msgs = parsed?.messages || [];
              const streamState = parsed?.streamState || {};
              // 查找最后一条 AI 消息
              for (let i = msgs.length - 1; i >= 0; i -= 1) {
                const m = msgs[i];
                if (m?.type === 'ai') {
                  // 优先信任 chatHistory 里保存的 message.isStreaming（它会在真正完成时被置为 false）
                  // 只有在 message 缺失 isStreaming 时，才兜底使用 streamState.isStreaming
                  const msgIsStreaming =
                    m.isStreaming === undefined ? !!streamState.isStreaming : !!m.isStreaming;
                  return {
                    ...m,
                    isStreaming: msgIsStreaming,
                    timestamp: m.timestamp || m.time,
                  };
                }
              }
            } catch (e) {
              // ignore
            }
            return null;
          };
          
          // 优先使用 chatHistory 中的最新状态，如果没有则使用摘要中的
          const latestAiMessage = tryGetLatestAiMessageFromHistory(key, item.agentIndex) || item.lastAiMessage;
          
          return {
            sessionId: key,
            agentIndex: item.agentIndex || '',
            agentName: item.agentName || '',
            agentPicUrl: item.agentPicUrl || '',
            agentDesc: item.agentDesc || '',
            lastMessage: lastUserMessage?.content || '暂无消息',
            lastMsgTime: item.lastMsgTime || Date.now(),
            // 添加状态标签所需的信息
            lastAiMessage: latestAiMessage,
            lastViewedAiTime: item.lastViewedAiTime || 0,
          };
        });
        // 按时间倒序排列
        historyList.value.sort((a, b) => b.lastMsgTime - a.lastMsgTime);
        // 写回迁移后的 summary（只在本次确实补齐字段时写一次）
        if (summaryChanged) {
          const escaped = unescape(encodeURIComponent(JSON.stringify(summary)));
          localStorage.setItem(summaryKey, escaped);
        }
      }
    } catch (error) {
      console.error('加载历史对话列表失败:', error);
    }
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
      loadHistoryList();
    } catch (error) {
      console.error('保存摘要失败:', error);
    }
  }

  // 更新摘要中的 lastViewedAiTime
  async function updateSummaryLastViewedAiTime(sessionId: string, aiTime: number) {
    if (!userId.value) return;
    try {
      const summaryKey = `chatSummary_${userId.value}_session`;
      const summaryData = localStorage.getItem(summaryKey);
      if (!summaryData) return;
      const summary = JSON.parse(decodeURIComponent(escape(summaryData)));
      if (summary[sessionId]) {
        summary[sessionId].lastViewedAiTime = aiTime;
        const escaped = unescape(encodeURIComponent(JSON.stringify(summary)));
        localStorage.setItem(summaryKey, escaped);
        loadHistoryList();
      }
    } catch (error) {
      console.error('更新 lastViewedAiTime 失败:', error);
    }
  }

  function handleModuleClick(item) {
    activeModuleId.value = item.index || '';
    if (isSessionMode.value && showAgentStore.value) {
      // 会话模式下选择智能体后，准备新对话
      // 注意：会话ID统一在首次发送消息时创建，避免重复创建或丢失
      currentSessionId.value = null; // 清空会话ID，等待首次发送消息时创建
      currentAgentId.value = item.index || '';
      activeModuleId.value = item.index || '';
      showAgentStore.value = false;
      // 清空当前聊天消息，开始新会话
      if (chatListRef.value) {
        chatListRef.value.messages = [];
      }
    }
  }
  // 回车: 文本框有值才发送
  function keyupEnterMsg() {
    if (messageLoading.value) return;
    inputValue.value?.trim()
     ? sendMessage()
     : inputValue.value = ''
  }
  // 发送
  async function sendMessage() {
    if (!chatListRef.value || !chatListRef.value.sendMessageFunc) return;

    // 会话模式下，如果没有会话ID，创建新会话
    if (isSessionMode.value && !currentSessionId.value) {
      currentSessionId.value = generateUUID();
    }
    // 会话模式下，如果没有智能体ID，设置为 -1（表示未选择智能体）
    if (isSessionMode.value && !currentAgentId.value) {
      currentAgentId.value = '-1';
    }

    // 等待一帧，确保 ChatList 收到最新的 sessionId / agentId props
    await nextTick();

    chatListRef.value.sendMessageFunc(inputValue.value);
  }
  // 暂停
  function pausedMessage() {
    if ((isStreaming.value || isDisplaying.value) && !isPaused.value) {
      if (chatListRef.value && chatListRef.value.pauseStream) {
        chatListRef.value.pauseStream();
      }
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

  function getImageSrc() {
    // 会话模式下使用特殊的暂停图标
    if (isSessionMode.value) {
      if (isSendDisabled.value) {
        return AiPauseDisabled;
      }
      return AiPause;
    }
    // 综合模式使用原有的发送图标
    if (currentTheme.value === 'light') {
      if (messageLoading.value) {
        return SendDisabled;
      }
      return Send;
    } else {
      if (messageLoading.value) {
        return SendDisabledDark;
      }
      return SendDark;
    }
  }

  function getPuaseSrc() {
    return AiSendPause;
  }

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
    // 清理当前会话的流式状态，防止旧定时器继续运行
    if (chatListRef.value?.resetStreamState) {
      chatListRef.value.resetStreamState();
    }
    
    currentSessionId.value = generateUUID();
    currentAgentId.value = '';
    activeModuleId.value = '';
    showAgentStore.value = false;
    if (chatListRef.value) {
      chatListRef.value.messages = [];
    }
  }

  function handleAgentClick() {
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

  async function handleHistoryClick(item: any) {
    // 切换前先保存当前会话状态，防止数据丢失
    if (currentSessionId.value && chatListRef.value) {
      // 强制保存当前会话状态
      if (chatListRef.value.saveConversationState) {
        chatListRef.value.saveConversationState();
      }
    }
    
    // 清理当前会话的流式状态，防止定时器继续写入新会话
    if (chatListRef.value?.resetStreamState) {
      chatListRef.value.resetStreamState();
    }
    
    currentSessionId.value = item.sessionId;
    // 如果 agentIndex 为空，使用 -1 作为默认值
    currentAgentId.value = item.agentIndex || '-1';
    // 只有当 agentIndex 不为 -1 时，才设置 activeModuleId
    activeModuleId.value = item.agentIndex && item.agentIndex !== '-1' ? item.agentIndex : '';
    
    // 如果历史记录中有智能体信息，确保它存在于 selectedModules 中，以便显示名称
    if (item.agentIndex && item.agentIndex !== '-1' && item.agentName) {
      const existingIndex = selectedModules.value.findIndex((m: any) => m.index === item.agentIndex);
      if (existingIndex === -1) {
        // 如果不存在，添加到 selectedModules
        const agentInfo = {
          index: item.agentIndex,
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
    
    // 标记该会话"已查看完成结果"：用于会话列表的"任务已完成"标签查看后消失
    // 仅影响"已完成"标签；"任务进行中"仍按 isStreaming 展示
    try {
      const aiTime =
        item?.lastAiMessage?.time ||
        item?.lastAiMessage?.timestamp ||
        (item?.lastMessage?.type === 'ai' ? (item?.lastMessage?.time || item?.lastMessage?.timestamp) : null);
      if (item.sessionId && aiTime) {
        await updateSummaryLastViewedAiTime(item.sessionId, aiTime);
      }
    } catch (e) {
      console.warn('标记会话已查看失败（可忽略）', e);
    }
    
    showAgentStore.value = false;
    // 等待 props 更新完成
    await nextTick();
    // 恢复对话历史
    console.log(chatListRef.value && chatListRef.value.restoreConversation, 'chatListRef.value.restoreConversation')
    if (chatListRef.value && chatListRef.value.restoreConversation) {
      const restored = await chatListRef.value.restoreConversation(item.sessionId, currentAgentId.value);
      // 等待响应式更新完成，确保 showEmptyHeader 正确计算
      await nextTick();
      // 如果恢复成功且有消息，确保 ChatList 可见
      if (restored && chatListRef.value?.messages?.length > 0) {
        // 再次等待确保 UI 更新
        await nextTick();
      }
    }
  }

  function handleDeleteHistory(item: any) {
    if (!userId.value) return;
    try {
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
                height: 64px;
                padding: 16px 12px 16px 12px;
                box-sizing: border-box;
                border: 1px solid rgba(0, 0, 0, 0.1);
                border-radius: 16px;
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
            padding-bottom: 24px;

            .footer-input {
              width: 100%;

              .input-wrapper {
                display: flex;
                align-items: center;
                height: 64px;
                padding: 16px 12px 16px 12px;
                box-sizing: border-box;
                border: 1px solid rgba(0, 0, 0, 0.1);
                border-radius: 16px;
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

    .all-wrap {
      width: 100%;
      height: calc(100% - 120px);

      .empty-header {
        width: 100%;
        height: 223px;

        .header-img {
          width: 100%;
          height: 100%;
        }
      }
    }

    .footer-wrap {
      width: 100%;

      .footer-input {
        .input-wrapper {
          display: flex;
          align-items: center;
          height: 50px;
          padding: 0 12px;
          background: var(--td-input-inner-bg2);
          border: 1px solid var(--td-input-border);
          border-radius: 25px;

          .search-input {
            flex: 1;
            padding: 0 12px;
            font-size: 16px;
            color: var(--text-color);
            background: transparent;
            border: none;
            outline: none;

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
              border-radius:16px;
              display: flex;
              align-items: center;
              justify-content: center;
              .pause-item {
                background: #ffffff;
                width: 13px;
                height: 13px;
                border-radius:2px;
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
