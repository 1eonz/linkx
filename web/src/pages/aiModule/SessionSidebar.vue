<template>
  <!-- 显示侧边栏按钮（当侧边栏隐藏时显示） -->
  <Teleport v-if="isHidden" to=".ai-module.session-mode">
    <div class="show-sidebar-btn">
      <img @click="toggleHide" src="@/assets/svg/foldUp.svg" alt="显示" class="show-icon" />
      <img @click="handleNewChat" src="@/assets/svg/newChatBlack.svg" alt="新对话" class="show-icon" />
      <img @click="handleAgentClick" src="@/assets/svg/agent.svg" alt="智能体" class="show-icon" />
    </div>
  </Teleport>
  <div class="session-sidebar" :class="{ 'session-sidebar-hidden': isHidden }">
    <div class="sidebar-header">
      <div class="title">
        <img src="@/assets/svg/Aigc.svg" alt="AI" class="title-icon" />
        助手
      </div>
      <div class="hide-btn" @click="toggleHide">
        <img v-if="isHidden" src="@/assets/svg/expand.svg" alt="展开" class="hide-icon" />
        <img v-else src="@/assets/svg/foldUp.svg" alt="收起" class="hide-icon" />
      </div>
    </div>
    <div class="sidebar-content">
      <div class="new-chat-btn" @click="handleNewChat">
        <img src="@/assets/svg/newChat.svg" alt="新对话" class="new-chat-icon" />
        新对话
      </div>
      <div class="agent-btn-content" @click="handleAgentClick">
        <img src="@/assets/svg/agent.svg" alt="智能体" class="agent-icon-right" />
        <div class="agent-title">智能体</div>
      </div>
      <div class="box-div" />
      <div class="section">
        <div class="section-title">
          <img src="@/assets/svg/historyChat.svg" alt="历史对话" class="section-title-icon" />
          历史对话
        </div>
        <div class="history-list" v-if="historyList.length > 0">
          <div
            v-for="item in historyList"
            :key="item.sessionId"
            class="history-item"
            :class="{ active: item.sessionId === currentSessionId }"
            @click="handleHistoryClick(item)"
          >
            <div class="history-icon">
              <AuthImg v-if="item.agentPicUrl" :picUrl="item.agentPicUrl" class="agent-icon" />
              <img v-else :src="AiIcon" class="agent-icon" alt="AI" />
            </div>
            <div class="history-content">
              <div class="history-message" :title="item.lastMessage">
                {{ item.lastMessage }}
              </div>
              <!-- 状态标签 -->
              <div
                v-if="getStatusInfo(item).label"
                class="status-tag"
                :class="getStatusInfo(item).type"
              >
                <img
                  v-if="getStatusInfo(item).type === 'processing'"
                  src="@/assets/svg/loading.svg"
                  class="status-tag-icon"
                  alt="进行中"
                />
                <img
                  v-if="getStatusInfo(item).type === 'completed'"
                  src="@/assets/svg/circle2.svg"
                  class="status-tag-icon"
                  alt="已完成"
                />
                {{ getStatusInfo(item).label }}
              </div>
            </div>
            <div
              class="history-action"
              @click.stop="handleActionClick($event, item)"
            >
              <img src="@/assets/svg/moreAction.svg" alt="更多" class="more-action-icon" />
            </div>
            <el-popover
              v-model:visible="popoverVisible[item.sessionId]"
              placement="bottom"
              :width="100"
              :trigger="'manual' as any"
              popper-class="session-sidebar-popover"
            >
              <div class="popover-content">
                <div class="popover-item" @click="handleDelete(item)">删除</div>
              </div>
              <template #reference>
                <div></div>
              </template>
            </el-popover>
          </div>
        </div>
        <!-- <div v-else class="no-history">暂无历史对话</div> -->
      </div>
    </div>
  </div>
</template>

<script lang="ts" setup>
  import { ref } from 'vue';
  import { ElPopover, ElMessageBox } from 'element-plus';
  import AuthImg from './authImg.vue';
  import AiIcon from '@/assets/svg/ai-icon.svg';
  import AiAgentChatPausePosition from './index.js';
  import { useAiModuleStore } from '@/store/modules/aiModule';

  const aiModuleStore = useAiModuleStore();

  interface HistoryItem {
    sessionId: string;
    agentIndex: string;
    agentName: string;
    agentPicUrl: string;
    agentDesc: string;
    lastMessage: string;
    lastMsgTime: number;
    lastAiMessage?: any;
    lastViewedAiTime?: number;
    latestReplyPaused?: boolean;
    latestReplyPosition?: number;
  }

  interface Props {
    historyList?: HistoryItem[];
    currentSessionId?: string | null;
    isHidden?: boolean;
  }

  withDefaults(defineProps<Props>(), {
    historyList: () => [],
    currentSessionId: null,
    isHidden: false,
  });

  const emit = defineEmits<{
    'new-chat': [];
    'agent-click': [];
    'history-click': [item: HistoryItem];
    'delete-history': [item: HistoryItem];
    'toggle-hide': [];
  }>();

  const popoverVisible = ref<Record<string, boolean>>({});


  function toggleHide() {
    emit('toggle-hide');
  }

  function handleNewChat() {
    emit('new-chat');
  }

  function handleAgentClick() {
    emit('agent-click');
  }

  function handleHistoryClick(item: HistoryItem) {
    emit('history-click', item);
  }

  function handleActionClick(event: MouseEvent, item: HistoryItem) {
    event.stopPropagation();
    popoverVisible.value[item.sessionId] = true;
  }

  async function handleDelete(item: HistoryItem) {
    popoverVisible.value[item.sessionId] = false;
    try {
      await ElMessageBox.confirm('删除后，该对话将不可恢复。确认删除吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      });
      emit('delete-history', item);
    } catch {
      // 用户取消
    }
  }

  // 获取状态标签信息
  function getStatusInfo(item: HistoryItem) {
    const latestReplyPaused = AiAgentChatPausePosition.getPosition(item.sessionId)
    const isFineshed = latestReplyPaused || aiModuleStore.isStreamingTaskId(item.sessionId);
    return isFineshed ? { label: '进行中', type: 'processing' } : { label: '', type: '' };
    // 点击historyItem后, 更新replyPaused replyPosition
    
    // // 1) 优先从 lastAiMessage 获取 AI 消息
    // let aiMsg = item.lastAiMessage || null;
    
    // // 2) 如果没有 lastAiMessage，尝试从 lastMessage 判断（但 lastMessage 是字符串，需要从摘要中获取完整对象）
    // // 这里我们只能依赖传入的 lastAiMessage
    // if (!aiMsg) {
    //   return { label: '', type: '' };
    // }

    // // 3) 没有 AI 消息则不显示标签
    // if (!aiMsg || aiMsg.type !== 'ai') {
    //   return { label: '', type: '' };
    // }

    // // 兼容旧缓存缺少 isStreaming，默认已完成
    // const isStreaming = aiMsg.isStreaming === undefined ? false : aiMsg.isStreaming;
    // if (isStreaming === true) {
    //   return { label: '进行中', type: 'processing' };
    // }

    // // 已完成：只有"未查看"才显示标签；查看后隐藏
    // // - lastViewedAiTime: 保存到摘要中的"用户最后一次查看到的AI完成消息时间"
    // // - aiMsg.time / aiMsg.timestamp: 本条AI消息的时间戳（摘要用time，实时消息用timestamp）
    // // const viewedTime = item.lastViewedAiTime || 0;
    // // const aiTime = aiMsg?.time || aiMsg?.timestamp || 0;
    // if (isStreaming === false) {
    //   // 如果拿不到时间戳，为保持兼容，仍然显示"已完成"
    //   // if (!aiTime) return { label: '已完成', type: 'completed' };
    //   // if (aiTime > viewedTime) return { label: '已完成', type: 'completed' };
    //   return { label: '', type: '' };
    // }
    // return { label: '', type: '' };
  }
</script>

<style lang="less" scoped>
  .session-sidebar {
    width: 278px;
    height: 100%;
    background: var(--background-other-color);
    display: flex;
    flex-direction: column;
    transition: transform 0.3s ease;
    gap: 16px;
    padding: 12px 12px 0px 12px;

    &.hidden {
      transform: translateX(-100%);
    }

    .sidebar-header {
      display: flex;
      align-items: center;
      justify-content: space-between;

      .title {
        font-size: 16px;
        font-weight: 600;
        color: var(--text-color);
        display: flex;
        align-items: center;
        gap: 4px;
        line-height: 27px;

        .title-icon {
          width: 20px;
          height: 20px;
        }
      }

      .hide-btn {
        width: 24px;
        height: 24px;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;

        .hide-icon {
          width: 16px;
          height: 16px;
        }

        &:hover {
          opacity: 0.7;
        }
      }
    }

    .sidebar-content {
      overflow: hidden;
      gap: 16px;
      display: flex;
      flex-direction: column;
      flex: 1;

      .new-chat-btn {
        width: 100%;
        height: 30px;
        line-height: 22px;
        text-align: center;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 0 12px;
        gap: 8px;
        color: rgba(38, 78, 209, 1);
        font-family: HarmonyHeiTi;
        font-size: 15px;
        font-weight: 600;
        letter-spacing: 0px;
        text-align: left;
        box-sizing: border-box;
        border: 1px solid rgba(38, 78, 209, 0.3);
        border-radius: 6px;
        background: rgba(38, 78, 209, 0.1);

        .new-chat-icon {
          width: 16px;
          height: 16px;
          flex-shrink: 0;
        }

        &:hover {
          background: var(--button-bg-hover);
        }
      }

      .agent-btn-content{
        width: 100%;
        display: flex;
        align-items: center;
        justify-content: flex-start;
        gap: 12px;
        cursor: pointer;
        .agent-title{
          line-height: 24px;
          color: rgba(3, 11, 38, 1);
          font-family: HarmonyHeiTi;
          font-size: 16px;
          font-weight: 600;
          letter-spacing: 0px;
          text-align: left;
        }
        .agent-icon-right {
          width: 16px;
          height: 16px;
          flex-shrink: 0;
        }
      }

      .box-div{
        border-bottom: 1px solid rgba(0, 0, 0, 0.1);
      }

      .section {
        flex: 1;
        display: flex;
        flex-direction: column;
        gap: 6px;
        overflow: hidden;

        .section-title {
          display: flex;
          align-items: center;
          justify-content: flex-start;
          color: rgba(91, 96, 114, 1);
          font-family: HarmonyHeiTi;
          font-size: 12px;
          font-weight: 600;
          line-height: 18px;
          letter-spacing: 0px;
          text-align: left;
          gap: 4px;

          .section-title-icon {
            width: 16px;
            height: 16px;
            flex-shrink: 0;
          }

          &:hover {
            color: var(--tabs-active-color);
          }
        }

        .history-list {
          
          overflow-y: auto;
          height: 100%;
          .history-item {
            display: flex;
            align-items: center;
            padding: 4px;
            margin-bottom: 6px;
            border-radius: 6px;
            cursor: pointer;
            position: relative;

            &:hover{
              background: rgba(0, 0, 0, 0.05);
            }
            &.active {
              background: rgba(233, 237, 250, 0.5);
            }

            .history-icon {
              flex-shrink: 0;
              width: 12px;
              height: 12px;
              margin-right: 3px;

              .agent-icon {
                width: 12px;
                height: 12px;
              }
            }

            .history-content {
              flex: 1;
              min-width: 0;
              margin-right: 8px;
              display: flex;
              gap: 4px;

              .history-message {
                overflow: hidden;
                text-overflow: ellipsis;
                white-space: nowrap;
                line-height: 21px;
                color: rgba(0, 0, 0, 1);
                font-family: HarmonyHeiTi;
                font-size: 14px;
                font-weight: 500;
                letter-spacing: 0px;
                text-align: left;
              }

              .status-tag {
                display: flex;
                align-items: center;
                gap: 2px;
                padding: 2px 4px;
                border-radius: 3px;
                font-size: 10px;
                white-space: nowrap;
                flex-shrink: 0;
                color: rgba(255, 255, 255, 1);
                font-weight: 400;
                line-height: 14px;
                letter-spacing: 0px;
                text-align: left;
                width: fit-content;

                .status-tag-icon {
                  flex-shrink: 0;
                }

                &.completed {
                  background: rgba(57, 206, 75, 1);
                  .status-tag-icon {
                    width: 4px;
                    height: 4px;
                  }
                }

                &.processing {
                  background: rgba(38, 78, 209, 1);
                  .status-tag-icon {
                    width: 9px;
                    height: 9px;
                    animation: rotate 1s linear infinite;
                  }
                }
              }
            }

            .history-action {
              flex-shrink: 0;
              width: 20px;
              height: 20px;
              display: flex;
              align-items: center;
              justify-content: center;
              cursor: pointer;
              opacity: 0;
              pointer-events: none;
              transition: opacity 0.2s ease;

              .more-action-icon {
                width: 20px;
                height: 20px;
              }

              &:hover {
                opacity: 0.7;
              }
            }

            &:hover .history-action,
            &.active .history-action {
              opacity: 1;
              pointer-events: auto;
            }
          }
        }

        .no-history {
          font-size: 12px;
          color: var(--input-placeholder);
          text-align: center;
          padding: 20px 0;
        }
      }
    }
  }
  .session-sidebar-hidden{
    display: none !important;
  }

  .show-sidebar-btn {
    position: absolute;
    top: 16px;
    left: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    z-index: 10;
    gap: 12px;
    padding: 8px;
    box-sizing: border-box;
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 6px;
    box-shadow: 0px 10px 20px 0px rgba(0, 0, 0, 0.1);
    background: rgba(255, 255, 255, 1);

    .show-icon {
      width: 16px;
      height: 16px;

      &:hover {
        background: var(--selected-tab-back);
      }
    }
  }

  .popover-content {
    .popover-item {
      padding: 12px 16px;
      cursor: pointer;
      font-size: 14px;
      color: rgba(235, 82, 85, 1);

      &:hover {
        background: var(--selected-tab-back);
      }
    }
  }

  @keyframes rotate {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }
</style>

<!-- 全局样式：用于 Teleport 渲染到 body 的 popover -->
<style lang="less">
  .session-sidebar-popover {
    padding: 0 !important;

    .el-popover {
      padding: 0 !important;
      border-radius: 20px;
    }
  }
</style>


