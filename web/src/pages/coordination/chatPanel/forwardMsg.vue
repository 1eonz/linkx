<script lang="ts" setup>
  import { computed, onMounted, ref, unref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import { groupsCreate, sendMessage } from '@/api/pim';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { useEmitter } from '@/hooks';
  import { getCategoryNames } from '@/pages/coordination/common';
  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';
  import { usePIMStore } from '@/store';

  import GroupAdd from '../chatGroup/groupAdd.vue';
  import { chatListSort } from '../common';

  const props = defineProps<{
    forwardMsgList: any;
    type: number; // 1逐条转发 2合并转发
  }>();
  const emit = defineEmits(['closeDialog', 'submit']);
  const route = useRoute();
  const router = useRouter();

  const PIMStore = usePIMStore();

  const filterText = ref('');
  const boardText = ref('');
  const chartData = ref<any>([]);
  const checkData = ref<any>([]);

  const disable = computed(() => {
    const len = checkData.value.length;
    return len === 0 || len > 10;
  });
  const userId = computed(() => PIMStore.user?.id);
  const collaboration = computed(() => PIMStore.collaboration);
  const groupIds = computed(() => PIMStore.user?.cooperationUser?.groupIds || []);
  const xietongId = computed(() => PIMStore.user?.cooperationUser?.userId);

  onMounted(() => {
    initChartData();
  });

  function initChartData() {
    const ret = chatListSort(PIMStore.chatList);
    const newArr: any = [...ret];
    chartData.value = newArr.map((item) => {
      return { ...item, checked: false };
    });
  }

  function handleCheck() {
    checkData.value = unref(chartData).filter((item) => item.checked);
  }

  function handleDelete(id) {
    chartData.value.forEach((item) => {
      if (item.id === id) {
        item.checked = false;
      }
    });
    checkData.value = unref(chartData).filter((item) => {
      return item.checked && item.id !== id;
    });
  }

  function handleConfirm() {
    // 当msgType为8时，使用合并转发逻辑转发聊天记录
    // 普通群组
    const normalGroups: any = [];
    // 协同群组
    const xietongGroups: any = [];
    unref(checkData).forEach((item) => {
      const { id, sessionId, sessionType } = item;
      const toObjId = sessionType === 1 ? id : sessionId;
      if (unref(groupIds).includes(toObjId) && unref(collaboration)) {
        xietongGroups.push({
          category: sessionType,
          toObjId,
        });
      } else {
        normalGroups.push({
          category: sessionType,
          toObjId,
        });
      }
    });
    const groupsArr: any = {
      normalGroups,
      xietongGroups,
    };

    for (const i in groupsArr) {
      const arr: any = groupsArr[i];
      if (arr.length === 0) continue;
      const xietongFlag = i === 'xietongGroups';
      if ([1, 3].includes(props.type)) {
        const oneList: any = [];
        props.forwardMsgList.forEach((item: any) => {
          if (item?.msgType === 8) {
            handleForwardMerge(xietongFlag, arr, item.msg?.forwardMsgs);
          } else {
            oneList.push(item);
          }
        });
        if (oneList.length === 0) return;
        handleForwardOne(xietongFlag, arr, oneList);
      } else {
        handleForwardMerge(xietongFlag, arr);
      }
    }
  }

  // 逐条转发
  async function handleForwardOne(xietongFlag, forwardList, oneList?) {
    // 消息
    const forwardMsgs: any = [];
    const forwardMsgList: any = oneList || props.forwardMsgList;
    forwardMsgList.forEach((item) => {
      const { msg, msgType } = item;
      const obj: any = {
        category: 4,
        forwardList,
        from: unref(userId),
        msg,
        msgType: msgType || 1,
        plaintext: 1,
      };
      if (xietongFlag) {
        obj.from = unref(xietongId);
        obj.fromRealUserId = unref(userId);
      }
      forwardMsgs.push(obj);
    });
    // 备注
    if (unref(boardText)) {
      const obj: any = {
        category: 4,
        forwardList,
        from: unref(userId),
        msg: {
          text: unref(boardText),
        },
        msgType: 1,
        plaintext: 1,
      };
      if (xietongFlag) {
        obj.from = unref(xietongId);
        obj.fromRealUserId = unref(userId);
      }
      forwardMsgs.unshift(obj);
    }

    const params: any = {
      category: 4,
      from: unref(userId),
      msg: {
        forwardMsgs,
      },
      msgType: 9,
    };
    if (xietongFlag) {
      params.from = unref(xietongId);
      params.fromRealUserId = unref(userId);
    }
    const { code } = await sendMessage(params);
    if (code === 0) {
      emit('submit');
      closeWindow();
      Message({ message: '转发成功', type: 'success' });
    } else {
      Message({ message: '转发失败', type: 'error' });
    }
  }

  // 合并转发
  async function handleForwardMerge(xietongFlag, forwardList, arr?) {
    // 消息
    let forwardMsgs: any = [];
    props.forwardMsgList.forEach((item) => {
      const { category, from, fromRealUserId, msg, msgType, seq, sessionSeqId, time, to } = item;
      let obj: any = {
        category,
        from,
        fromRealUserId,
        msgType,
        seq,
        sessionSeqId,
        text: msg.text,
        time,
        to,
      };
      // 彩信
      if (msgType === 2) {
        obj = { ...obj, ...msg };
      }
      if (msgType === 8) {
        obj.title = `${getCategoryNames(msg.forwardMsgs)}`;
        // 合并转发格式修改
        obj.userTxt = JSON.stringify(item.msg);
      }
      // if (xietongFlag) {
      //   obj.from = unref(xietongId);
      //   obj.fromRealUserId = unref(userId);
      // }
      forwardMsgs.push(obj);
    });

    if (arr) {
      forwardMsgs = [...arr];
    }

    const params: any = {
      category: 4,
      forwardList,
      from: unref(userId),
      msg: {
        forwardMsgs,
        title: boardText.value === '' ? getCategoryNames(forwardMsgs) : unref(boardText),
      },
      msgType: 8,
      plaintext: 1,
    };
    if (xietongFlag) {
      params.from = unref(xietongId);
      params.fromRealUserId = unref(userId);
    }

    const { code } = await sendMessage(params);
    if (code === 0) {
      emit('submit');
      closeWindow();
      PIMStore.setForwardMsgList(params.msg);
      Message({ message: '转发成功', type: 'success' });
    } else {
      Message({ message: '转发失败', type: 'error' });
    }
  }

  function closeWindow() {
    emit('closeDialog');
  }

  function createNewChat() {
    Dialog({
      cid: 'ChartGroupAddForward',
      content: GroupAdd,
      data: {
        boardTextDefault: unref(boardText),
        closeDialog: () => {
          closeGroupDialog();
        },
        forward: true,
        isLight: true,
        maxNum: 50,
        onSubmit: (data, text) => {
          boardText.value = text;
          createGroup(data);
        },
        title: '创建新聊天',
        type: '2',
      },
      offset: ['40%', '10%'],
    });
  }
  // 创建群聊
  async function createGroup(userList) {
    const members: any = [];
    userList.forEach(({ id }) => {
      members.push(id);
    });
    const addMembers = members.join(',');
    const { code, data, msg } = await groupsCreate({ addMembers });

    if (code === 0) {
      Message({ message: msg, type: 'success' });
      await PIMStore.getChartGroupList();
      await PIMStore.startChat({ sessionId: data.groupId, sessionType: 2 });
      checkData.value = [
        {
          id: data.groupId,
          sessionId: data.groupId,
          sessionType: 2,
        },
      ];
      setTimeout(() => {
        handleConfirm();
      }, 1000);
      // emit('submit');
      closeGroupDialog();
    } else {
      Message({ message: msg, type: 'error' });
      // 发送失败消息
      useEmitter().emit('submitFailed');
    }
  }
  function closeGroupDialog() {
    Dialog('ChartGroupAddForward')?.close();
    closeWindow();
    if (!route.path.includes('/coordination')) {
      router.push('/coordination');
    }
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="true"
    size="small"
    title="选择会话"
    @close-frame-box="closeWindow"
  >
    <div class="forward-msg-content">
      <div class="content-left">
        <TdInput v-if="false" v-model="filterText" placeholder="搜索" type="searchInput" />
        <TdButton
          class="create-btn"
          :is-light="true"
          text="创建新聊天"
          type="normal"
          @click="createNewChat"
        />
        <div class="text">最近聊天</div>
        <div class="chart-list">
          <div v-for="item in chartData" :key="item.id" class="chart-item">
            <TdCheckbox
              v-model="item.checked"
              class="check-box"
              @change="handleCheck"
              @click.stop
            />
            <div class="avatar">
              <TdAvatar v-if="item.sessionType === 1" />
              <img v-else alt="" src="@/assets/images/pim/group.png" />
            </div>
            <div class="name">{{ item.name || item.undefinedName }}</div>
          </div>
        </div>
      </div>
      <div class="content-right">
        <div class="title">发送给</div>
        <div class="member-right">
          <div class="member-checked">
            <div v-for="item in checkData" :key="item.userId" class="member-item">
              <div class="avatar">
                <TdAvatar v-if="item.sessionType === 1" />
                <img v-else alt="" src="@/assets/images/pim/group.png" />
              </div>
              <div class="name">{{ item.name || item.undefinedName }}</div>
              <Icon class="delete" name="close" @click="handleDelete(item.id)" />
            </div>
          </div>
        </div>
        <div class="msg-list">
          <div v-if="type === 1" class="type">[逐条转发]共{{ forwardMsgList.length }}条消息</div>
          <div v-else-if="type === 3" class="type hidden-text">
            [逐条转发]
            <EmojiView :is-list="true" style="display: inline" :text="forwardMsgList[0].msg.text" />
          </div>
          <div v-else class="type">{{ getCategoryNames(forwardMsgList) }}</div>
          <TdInput v-model="boardText" class="board" placeholder="留言" type="text" />
          <div class="group-button">
            <TdButton class="btn" :is-light="true" text="取消" type="normal" @click="closeWindow" />
            <TdButton
              class="btn"
              :disable="disable"
              :is-light="true"
              text="发送"
              type="normal"
              @click="handleConfirm"
            />
          </div>
        </div>
      </div>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .forward-msg-content {
    display: flex;

    .content-left {
      width: 50%;
      padding: 20px 10px 10px 0;
      border-right: 1px solid rgb(102 102 102 / 20%);

      .create-btn {
        width: 100%;
        margin: 12px 0 24px;
        background: var(--button-text-inner) !important;

        :deep(.button-text-light) {
          color: var(--item-text-color) !important;
        }
      }

      .text {
        font-size: 14px;
        font-weight: 400;
        line-height: 20px;
        color: var(--item-text-color);
      }

      .chart-list {
        height: 450px;
        overflow: hidden auto;

        .chart-item {
          display: flex;
          align-items: center;
          height: 56px;

          .check-box {
            width: 16px;
            height: 16px;
            cursor: pointer;
          }

          .avatar {
            width: 36px;
            height: 36px;
            margin: 0 10px;
          }

          .name {
            font-size: 14px;
            font-weight: 700;
            color: var(--item-text-color);
            white-space: pre;
          }
        }
      }
    }

    .content-right {
      width: 50%;
      padding: 20px 0 10px 10px;

      .title {
        padding-bottom: 20px;
        font-size: 14px;
        font-weight: 500;
        line-height: 20px;
        color: var(--item-text-color);
      }

      .member-right {
        height: 400px;
        overflow-y: auto;

        .member-checked {
          .member-item {
            display: flex;
            align-items: center;
            height: 56px;

            .avatar {
              width: 36px;
              height: 36px;
              margin-right: 12px;
            }

            .name {
              width: 234px;
              overflow: hidden;
              font-size: 14px;
              font-weight: 700;
              color: var(--item-text-color);
              white-space: pre;
            }

            .delete {
              width: 8px;
              height: 8px;
              cursor: pointer;
              filter: var(--svg-filter);
            }
          }
        }
      }

      .msg-list {
        border-top: 1px solid rgb(255 255 255 / 10%);

        .type {
          margin: 12px 0;
          font-size: 12px;
          font-weight: 500;
          color: #99cefb;
        }

        .hidden-text {
          max-height: 100px;
          overflow: auto;
        }

        .board {
          margin-bottom: 24px;
          background: var(--td-input-inner-bg) !important;
        }

        .group-button {
          display: flex;
          justify-content: space-between;

          .btn {
            width: 150px;
            background: var(--td-input-inner-bg) !important;

            :deep(.button-text-light) {
              color: var(--item-text-color);
            }
          }

          .disabled {
            background: transparent !important;
          }
        }
      }
    }
  }

  :deep(.td-input-inner) {
    color: var(--item-text-color) !important;
  }
</style>
