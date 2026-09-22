<script setup lang="ts">
  import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';

  import { getGlobalsList } from '@/api/dictionary';
  import {
    // responsesTask,
    queryDetail,
    updateStatus,
  } from '@/api/problemSolve';
  import {
    closeChatUI,
    getUserInfo,
    highlightMsg,
    isWebView2Env,
    openChatUI,
    reply,
  } from '@/bridge/post.js';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useEmitter } from '@/hooks';
  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import { usePIMStore } from '@/store';

  // import ChatPanel from '../chatPanel/index.vue';

  const props = defineProps<{
    detailParams: any;
    showDetails: boolean;
    title: string;
  }>();
  const emit = defineEmits(['closeDetail']);
  const PIMStore = usePIMStore();
  const detailInfo = ref<any>({});
  const stateName = ref('');
  const brnArr = ref<number[]>([]);
  const REPLYDIRECTLY = ref(false);
  // WebView2 环境下，info 宽度 100%，chat-box 宽度 0%
  const isWebView2 = ref(isWebView2Env());

  watch(
    () => props.showDetails,
    (newVal) => {
      if (newVal) {
        openChat(true);
      } else {
        closeChatUI();
      }
    },
    { immediate: true },
  );

  // 处理页面可见性变化
  function handleVisibilityChange() {
    // 当页面重新可见时，如果详情页是打开的，则重新打开聊天框
    if (!document.hidden && props.showDetails) {
      openChat();
    }
  }
  async function getReplyDirectly() {
    const { code, data } = await getGlobalsList();
    if (code === 0) {
      REPLYDIRECTLY.value = !!(data?.REPLY_DIRECTLY === 'true' || data?.REPLY_DIRECTLY === true);
    }
  }

  onMounted(async () => {
    getUserInfoFunc();
    getReplyDirectly(); // 添加回复开关控制
    // 监听刷新事件
    useEmitter('refreshDetailsQuery', () => {
      // 只有当当前详情页的任务ID与事件中的任务ID匹配时才刷新
      // if (props.detailParams.id === taskId) {
      query();
      // }
    });

    // 监听页面可见性变化，当页面重新可见时恢复聊天框
    document.addEventListener('visibilitychange', handleVisibilityChange);
  });

  onUnmounted(() => {
    // chatPanelRef.value?.clearInput();
    // PIMStore.setQuoteMsg({});
    closeChatUI();
    // 移除页面可见性监听
    document.removeEventListener('visibilitychange', handleVisibilityChange);
  });

  // 打开聊天框的函数
  function openChat(flag?: any, isReply: boolean = false) {
    if (!props.showDetails) return;
    nextTick(() => {
      const { groupId, icsMsgId, fromUserId } = props.detailParams;
      const chatBox = document.querySelector('.chat-box') as HTMLElement | null;
      const rect = chatBox?.getBoundingClientRect();
      const width = rect ? `${Math.round(rect.width)}px` : '320px';
      const height = rect ? `${Math.round(rect.height)}px` : '800px';
      const top = rect ? `${Math.round(rect.top)}px` : '150px';
      const right = rect ? `${Math.round(window.innerWidth - rect.right)}px` : '15px';
      // SDK 兼容说明：
      // - 新版 openChatUI 使用 msgid 参数实现高亮，不支持窗口布局参数
      // - 旧版 openChatUI 支持窗口布局参数，需要单独调用 highlightMsg 高亮
      // - post.js 会根据环境自动处理参数转换
      const params = {
        draggable: false,
        groupId,
        msgid: flag ? icsMsgId : undefined, // 新版使用此参数高亮
        showClose: false, // bspc 环境显示关闭按钮
        height,
        position: { right, top },
        width,
        atUserId: fromUserId,
      };
      openChatUI(params, isReply);
      // 旧版环境需要单独调用 highlightMsg 高亮消息
      // 新版环境会在 post.js 中忽略此调用
      if (flag) {
        setTimeout(() => {
          highlightMsg({ groupId, msgId: icsMsgId });
        }, 1500);
      }
    });
  }
  async function getUserInfoFunc() {
    const res: any = await getUserInfo();

    if (res) {
      PIMStore.setUserMyInfo(res);
    }
    // 查询问题详情
    query();
    // 显示对应群组消息框
    getGroupDetail();
  }
  async function getGroupDetail() {
    // const { groupId, icsMsgId } = props.detailParams;
    const { groupId } = props.detailParams;
    const sessionId = Number(groupId);
    // await PIMStore.judgeChatInList(sessionId);
    setTimeout(() => {
      PIMStore.setChatListActive(sessionId);
    }, 500);
    // setTimeout(() => {
    //   chatPanelRef.value?.toQuoteMsg({ msgId: icsMsgId });
    // }, 1500);
  }

  async function query() {
    const { id, postId } = props.detailParams;
    const { code, data } = await queryDetail({
      postId,
      taskId: id,
    });
    if (code === 0) {
      detailInfo.value = data;
      getStateName(detailInfo.value.status, detailInfo.value.archive);
    }
  }

  async function answer(type) {
    const { id } = props.detailParams;
    if (type === 5) {
      // 聊天框联动
      answerHandle();
    } else {
      const params = [
        {
          fromExecutorId: PIMStore.user.userid,
          id,
          status: type,
        },
      ];
      // 调用后端更新状态接口
      const { code, msg } = await updateStatus(params);
      if (code === 0) {
        Message('操作成功');
        query();
        // 触发刷新事件
        useEmitter().emit('refreshTabsData');
      } else {
        Message(msg);
      }
    }
  }

  function close() {
    emit('closeDetail');
    // 触发刷新事件
    useEmitter().emit('refreshDetailsQuery');
    useEmitter().emit('refreshTableData');
  }

  // 消息回复时，自动引用提问消息,并@提问人员
  function answerHandle() {
    if (isWebView2.value) {
      openChat(true, true);
    } else {
      const { fromUserId, fromUserName, groupId, icsMsgId } = props.detailParams;
      const message = `[@${fromUserId}:@${fromUserName}]`;
      const params: any = {
        groupId,
        message,
        quote: {
          msgId: icsMsgId,
        },
      };
      reply(params);
    }
  }

  function getStateName(status, archive) {
    let str = '';
    let arr: number[] = [];

    switch (status) {
      case 1: {
        str = '待办';
        arr = [1, 2, 3, 4];
        break;
      }
      case 2: {
        str = '跟踪中';
        arr = [2, 3, 4];
        break;
      }
      case 3: {
        str = '已办结';
        arr = [];
        break;
      }
      case 4: {
        str = '无需处理';
        arr = [];
        break;
      }
      case 7: {
        arr = [1, 2, 3, 4];
        break;
      }
      case 8: {
        arr = [1, 2, 3, 4];
        break;
      }
      // case 4: {
      //   str = '已回复问题';
      //   arr = [2, 3, 4];
      //   break;
      // }
    }
    // 非未归档状态
    let newArr: any = [...arr];
    if (archive !== 0) {
      newArr = arr.filter((item) => item !== 4);
    }

    stateName.value = str;

    // 判断是否为该群组支撑人员
    const user: any = PIMStore.user;

    if (!user && (!user?.cooperationUsers || !user?.cooperationUser)) return;
    let ids: any = [];
    if (
      appConfig.settingData?.MULTIPLE_COLLABORATION === 'true' ||
      appConfig.settingData?.MULTIPLE_COLLABORATION === true
    ) {
      user?.cooperationUsers.map((item) => {
        ids = ids.concat(item.groupIds);
      });
    } else {
      ids = user?.cooperationUser?.groupIds || [];
    }

    let includeFlag = true;
    if (ids.length === 0) {
      includeFlag = false;
    }
    const { groupId } = props.detailParams;
    includeFlag = ids?.includes(Number(groupId)) || ids?.includes(groupId);

    if (!includeFlag) {
      // 不是该群组支撑人员，直接没有任何处理任务的权限
      // let index = newArr.indexOf(4);
      // if (index !== -1) {
      //   newArr.splice(index, 1); // 从索引位置开始删除1个元素
      // }
      newArr = [];
    }
    brnArr.value = newArr;
  }
</script>

<template>
  <div class="details">
    <div class="info" :class="{ 'info-no-btn': brnArr.length === 0, 'info-webview2': isWebView2 }">
      <div class="header">
        <div class="title">{{ `${title}/问题详情` }}</div>
        <div class="back-btn" @click="close">
          <Icon class="icon" name="arrow-left" prefix="im" />返回
        </div>
      </div>
      <div class="ask-info">
        <div class="ask-info-item">
          <div>提问时间：</div>
          <div>{{ detailInfo.msgSentTime }}</div>
        </div>
        <div class="ask-info-item">
          <div>群名称：</div>
          <TdTooltip :content="detailInfo.groupName">
            <div class="hidden-detail">{{ detailInfo.groupName }}</div>
          </TdTooltip>
        </div>
        <div class="ask-info-item">
          <div>提问人单位：</div>
          <div>{{ detailInfo.fromUserDepartmentName }}</div>
        </div>
        <div class="ask-info-item">
          <div>提问人账号：</div>
          <div>{{ detailInfo.fromUserName }}</div>
        </div>
        <div class="ask-info-item">
          <div>提问人昵称：</div>
          <div>{{ detailInfo.fromUserNick }}</div>
        </div>
      </div>
      <div class="ask-content">
        <div class="tile">提问内容：</div>
        <!-- <div class="content">{{ detailInfo.text }}</div> -->
        <!-- 文字回显 -->
        <EmojiView class="text" :is-list="true" :text="detailInfo.text" />
        <div v-if="detailInfo.reply === 1" class="tag">已回复问题</div>
      </div>
      <div class="dispose">
        <TdButton
          v-if="brnArr.includes(4) && !REPLYDIRECTLY"
          :is-light="true"
          text="回复"
          type="normal"
          @click="answer(5)"
        />
        <TdButton
          v-if="brnArr.includes(1)"
          :is-light="true"
          text="跟踪"
          type="normal"
          @click="answer(2)"
        />
        <TdButton
          v-if="brnArr.includes(3)"
          :is-light="true"
          text="忽略"
          type="normal"
          @click="answer(4)"
        />
        <TdButton
          v-if="brnArr.includes(2)"
          :is-light="true"
          text="办结"
          type="normal"
          @click="answer(3)"
        />
      </div>
      <div class="respone-box">
        <div class="respone-content">
          <div v-if="detailInfo?.responses?.length > 0" class="reply-content">
            <div class="tile">回复信息：</div>
            <div v-for="item in detailInfo.responses" :key="item.taskId" class="reply">
              <div class="item">
                <div class="item-content">
                  <div>回复内容：</div>
                  <div>
                    <EmojiView :is-list="true" :text="item.content || '请至app查看该消息'"
                  /></div>
                </div>
                <div>
                  <div>回复时间：</div>
                  <div>{{ item.gmtCreated }}</div>
                </div>
                <div>
                  <div>回复人昵称：</div>
                  <div>{{ item.userName }}</div>
                </div>
                <div>
                  <div>回复人账号：</div>
                  <div>{{ item.userName }}</div>
                </div>
                <div>
                  <div>回复人单位：</div>
                  <div>{{ item.departmentName }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
    <div class="chat-box" :class="{ 'chat-box-webview2': isWebView2 }"></div>
    <!-- <ChatPanel
      ref="chatPanelRef"
      :ability="[]"
      chat-id="chatPanelContent2"
      chat-type="detail"
      :task-answer="true"
      title-back-type="dark"
    /> -->
  </div>
</template>

<style scoped lang="less">
  .details {
    display: flex;
    width: 100%;
    height: 100%;

    .info {
      width: calc(100% - 375px);
      height: 100%;
      padding: 16px 12px;
      border-right: 0.0625rem solid rgb(0 0 0 / 10%);

      .header {
        display: flex;
        justify-content: space-between;

        .back-btn {
          display: flex;
          font-size: 14px;
          font-weight: 700;
          color: var(--item-text-color);
          cursor: pointer;

          .icon {
            width: 14px;
            height: 14px;
            margin-top: 3px;
            filter: var(--svg-filter);
          }
        }
      }

      .ask-info {
        display: flex;
        flex-wrap: wrap;
        margin: 24px 0 12px;

        .ask-info-item {
          display: flex;
          width: calc(33% - 10px);
          height: 34px;
          margin-right: 10px;

          div {
            font-size: 14px;
            font-weight: 700;
            color: var(--item-text-color);
          }

          .hidden-detail {
            width: calc(100% - 60px);
            overflow: hidden;
            text-overflow: ellipsis;
            white-space: nowrap;
          }
        }
      }

      .ask-content {
        position: relative;
        padding: 8px 16px;
        background: var(--is-leaf);
        backdrop-filter: blur(27.18px);
        border: 1px solid var(--problem-bg);

        .tile {
          margin-bottom: 16px;
          font-size: 14px;
          font-weight: 700;
          color: var(--item-text-color);
        }

        .content {
          font-size: 14px;
          font-weight: 500;
          color: var(--item-text-color);
        }

        .tag {
          position: absolute;
          top: 8px;
          right: 16px;
          padding: 2px 8px;
          font-size: 12px;
          font-weight: 700;
          color: rgb(0 165 91);
          background: rgb(0 165 91 / 10%);
          border: 1px solid rgb(0 165 91);
          border-radius: 4px;
        }
      }

      .dispose {
        display: flex;
        margin: 12px 0 25px;

        .td-button {
          margin-right: 12px;
          font-size: 14px;
          color: var(--text-color);
          background: var(--button-bg) !important;
          border: 1px solid var(--td-button-border);
          opacity: 0.8;

          :deep(.button-text-light) {
            color: var(--text-color) !important;
          }

          &:hover {
            color: var(--tabs-active-color);
            border: 1px solid var(--button-active-color);
            opacity: 1;

            :deep(.button-text-light) {
              color: var(--tabs-active-color) !important;
            }
          }
        }
      }

      .respone-box {
        width: 100%;
        height: calc(100% - 268px);
        overflow: hidden;

        .respone-content {
          width: 100%;
          height: 100%;
          overflow-y: scroll;
        }

        .reply-content {
          max-height: 400px;
          padding: 8px 16px;
          overflow: auto;
          background: var(--is-leaf);
          backdrop-filter: blur(27.18px);
          border: 1px solid var(--problem-bg);

          .tile {
            margin: 5px 0 10px;
            font-size: 14px;
            font-weight: 700;
            color: var(--item-text-color);
          }

          .reply {
            padding: 10px 0;
            border-top: 1px solid rgb(173 204 240 / 20%);

            .item {
              display: flex;
              flex-wrap: wrap;
              border-bottom: 1px dashed rgb(255 255 255/10%);

              & > div {
                display: flex;
                flex-wrap: wrap;
                width: calc(33% - 10px);
                margin: 2px 0;
                margin-right: 10px;

                div {
                  font-size: 14px;
                  font-weight: 500;
                  color: var(--item-text-color);
                }
              }

              .item-content {
                flex-wrap: nowrap;
                width: calc(100% - 30px);

                & > div:first-child {
                  flex-shrink: 0;
                  margin-right: 8px;
                  font-weight: 700;
                }

                & > div:last-child {
                  flex: 1;
                  word-break: break-all;
                }
              }

              // & > div:not(:first-child) {
              //   width: calc(33% - 10px);
              //   margin-right: 10px;
              // }
            }
          }
        }
      }
    }

    .info-no-btn {
      .dispose {
        margin: 12px 0 15px;
      }

      .respone-box {
        height: calc(100% - 230px);
      }
    }

    // WebView2 环境下，info 宽度 100%，chat-box 宽度 0%
    .info-webview2 {
      width: 100%;
      border-right: none;
    }

    .chat-box {
      width: 375px;
      height: 100%;
    }

    // WebView2 环境下隐藏 chat-box
    .chat-box-webview2 {
      width: 0;
    }

    :deep(.chat-panel) {
      width: 375px;
      height: 100%;

      .frame-box-header {
        justify-content: center;
        background: var(--el-table-tbody-td-hover) !important;

        .header-name {
          text-align: center;
        }
      }

      .list-wrapper {
        background: var(--el-table-tbody-td-hover) !important;
        border-right: none;
        border-bottom: none;
      }
    }
  }

  :deep(.video-dialog) {
    left: -700px !important;
  }

  :deep(.msg-content) {
    max-width: 270px !important;
  }
</style>
