<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { groupsCreate } from '@/api/pim';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { useEmitter } from '@/hooks';
  import { imChatHistory } from '@/pages/coordination/common';
  import MemberDetailsPopover from "./memberDetailsPopover/index.vue"
  import { deleteMessages } from '@/plugins/pim/indexDB';
  import { usePIMStore } from '@/store';

  import GroupAdd from '../chatGroup/groupAdd.vue';

  const emit = defineEmits(['closeDialog', 'toChatMsg']);
  const PIMStore = usePIMStore();
  const showClearTip = ref(false);
  // 开关初始化,解决el-switch首次加载触发change问题
  const stickyOnTopSwitch = ref(false);
  const muteNotificationsSwitch = ref(false);

  const currentChat = computed(() => PIMStore.currentChat);
  const sessionStatus = computed(() => {
    let ret = {
      isMuteNotifications: false,
      isStickyOnTop: false,
    };
    PIMStore.user?.chatProfiles?.forEach((i) => {
      if (i.targetId === unref(currentChat).id) {
        ret = i;
      }
    });
    if (unref(currentChat).sessionType !== 1) {
      closeWindow();
    }
    return ret;
  });

  watch(
    () => currentChat.value.sessionType,
    (val) => {
      if (val !== 1) {
        closeWindow();
      }
    },
  );

  onMounted(() => {});

  function closeWindow() {
    emit('closeDialog');
  }

  // 清空聊天记录
  function handleConfirmClear() {
    PIMStore.clearChatList(currentChat.value);
    deleteMessages(currentChat.value, true);
    handelCancelClear(false);
  }

  function handelCancelClear(flag?) {
    showClearTip.value = flag;
  }

  // 设置置顶+免打扰
  function handleSet(type, val) {
    if (type === 1) {
      if (!stickyOnTopSwitch.value) {
        return;
      }
      PIMStore.setTopChat(unref(currentChat), val);
    } else {
      if (!muteNotificationsSwitch.value) {
        return;
      }
      PIMStore.setChatDoNotDisturb(unref(currentChat), val);
    }
  }

  function stickyOnTopBeforeChange() {
    stickyOnTopSwitch.value = true;
    return stickyOnTopSwitch.value;
  }

  function muteNotificationsBeforeChange() {
    muteNotificationsSwitch.value = true;
    return muteNotificationsSwitch.value;
  }

  function create() {
    Dialog({
      cid: 'ChartGroupAdd_config',
      content: GroupAdd,
      data: {
        isLight: true,
        maxNum: 50,
        onSubmit: (data) => {
          createGroup(data);
        },
        title: '创建群聊',
        type: '2',
      },
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
      PIMStore.getChartGroupList();
      PIMStore.startChat({ sessionId: data.groupId, sessionType: 2 });
      closeGroupDialog();
    } else {
      Message({ message: msg, type: 'error' });
      // 发送失败消息
      useEmitter().emit('submitFailed');
    }
  }

  function closeGroupDialog() {
    Dialog('ChartGroupAdd_config')?.close();
    closeWindow();
  }

  function toQuoteMsg(item) {
    emit('toChatMsg', item);
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="true"
    size="another"
    title="聊天设置"
    @close-frame-box="closeWindow"
  >
    <div class="group-details-content">
      <div class="content-left">
        <div class="head-content">
          <div class="header">
            <MemberDetailsPopover :user-id="currentChat.id">
              <TdChatHead
                :avatar-id="currentChat.avatar"
                class="avatar"
              />
            </MemberDetailsPopover>
            <span class="name">{{ currentChat.name }}</span>
          </div>
          <div class="head">
            <Icon class="icon" name="group_add" prefix="im" @click="create" />
            <span class="name">添加</span>
          </div>
        </div>
        <div class="item border" @click="() => imChatHistory(toQuoteMsg)">
          <span>查找聊天记录</span>
          <Icon class="right-btn" name="right_triangle_arrow" />
        </div>
        <div class="item">
          <span>聊天置顶</span>
          <ElSwitch
            v-model="sessionStatus.isStickyOnTop"
            :active-value="1"
            :before-change="stickyOnTopBeforeChange"
            class="content"
            :inactive-value="0"
            @change="(v) => handleSet(1, v)"
          />
        </div>
        <div class="item border">
          <span>消息免打扰</span>
          <ElSwitch
            v-model="sessionStatus.isMuteNotifications"
            :active-value="1"
            :before-change="muteNotificationsBeforeChange"
            class="content"
            :inactive-value="0"
            @change="(v) => handleSet(2, v)"
          />
        </div>
        <div class="item" @click="handelCancelClear(true)">
          <span class="red">清空聊天记录</span>
          <Icon class="right-btn" name="right_triangle_arrow" />
        </div>
      </div>
      <TdTip
        v-if="showClearTip"
        msg="确定清空聊天记录吗？"
        @handel-cancel="handelCancelClear"
        @handle-confirm="handleConfirmClear"
      />
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .group-details-content {
    position: relative;
    display: flex;
    padding: 20px 6px 16px;

    .content-left {
      width: 100%;
      height: 400px;
      padding-right: 16px;

      .head-content {
        display: flex;
      }

      .head {
        align-items: center;
        width: 50px;
        padding: 10px 0;
        margin-left: 10px;

        .name {
          margin: 0 10px;
          font-size: 12px;
          font-weight: 400;
          line-height: 150%;
          color: var(--item-text-color);
        }

        .icon {
          width: 45px;
          height: 40px;
          cursor: pointer;
        }
      }

      .header {
        display: grid;
        align-items: center;
        padding: 10px 0;
        cursor: pointer;

        .name {
          padding-top: 2px;
          font-size: 12px;
          font-weight: 400;
          line-height: 150%;
          color: var(--item-text-color);
        }
      }

      .notice {
        border-bottom: 1px solid rgb(255 255 255 / 20%);

        .title {
          display: flex;
          align-items: center;
          justify-content: space-between;
          height: 44px;
          padding: 12px 0;

          span {
            font-size: 14px;
            color: #7d9bbd;
          }

          .right-btn {
            width: 10px;
            height: 10px;
            color: #7d9bbd;
            cursor: pointer;
          }
        }

        .text {
          width: 290px;
          padding: 10px 0;
          font-size: 12px;
          line-height: 150%;
          color: #fff;
        }
      }

      .item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 44px;
        cursor: pointer;

        span {
          font-size: 14px;
          font-weight: 500;
          color: var(--item-text-color);
        }

        .red {
          color: #e34242;
        }

        .right-btn {
          width: 10px;
          height: 10px;
          color: #7d9bbd;
          cursor: pointer;
        }
      }

      .border {
        border-bottom: 1px solid rgb(255 255 255 / 20%);
      }
    }
  }
</style>
