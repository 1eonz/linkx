<script lang="ts" setup>
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import {
    exitGroupsMembers,
    getGroupsNotices,
    groupDelete,
    groupsUpdate,
    updateListGroupName,
  } from '@/api/pim';
  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { imChatHistory } from '@/pages/coordination/common';
  import { deleteMessages } from '@/plugins/pim/indexDB';
  import { usePIMStore } from '@/store';

  import GroupMember from './groupMember.vue';
  import GroupNotice from './groupNotice.vue';

  const emit = defineEmits(['closeDialog', 'toChatMsg']);
  const PIMStore = usePIMStore();

  const notice = ref<any>(null);
  const oldGroupName = ref('');
  const newGroupName = ref('');
  const newGroupNameError = ref('');
  const showEditTip = ref(false);
  const showClearTip = ref(false);
  // 开关初始化,解决el-switch首次加载触发change问题
  const stickyOnTopSwitch = ref(false);
  const muteNotificationsSwitch = ref(false);

  const groupId = computed(() => PIMStore.chatListActive);
  const currentChat = computed(() => PIMStore.currentChat);
  const xietongGroupIds = computed(() => PIMStore.user?.cooperationUser?.groupIds || []);
  const sessionStatus = computed(() => {
    let ret = {
      isMuteNotifications: false,
      isStickyOnTop: false,
    };
    PIMStore.user?.chatProfiles?.forEach((i) => {
      if (i.targetId === unref(groupId)) {
        ret = i;
      }
    });
    return ret;
  });
  // 是否为协同群组
  const xietongGroupFlag = computed(() => {
    return unref(xietongGroupIds).includes(unref(groupId));
  });

  const isMaster = computed(() => {
    const index = PIMStore.groupMemberActive.findIndex(
      (i) => i.role === 2 && i.id === PIMStore.user?.id,
    );
    return index !== -1;
  });

  watch(
    () => currentChat.value,
    (val) => {
      if (!val.id) return;
      if (val.sessionType !== 2) {
        closeWindow();
        return;
      }
      newGroupName.value = val.name || '';
      oldGroupName.value = val.name || '';
    },
    { immediate: true },
  );

  onMounted(() => {
    PIMStore.getGroupMembers(groupId.value);
    getGroupNoticeData(groupId.value);
  });

  async function getGroupNoticeData(groupId) {
    notice.value = null;
    const { code, data } = await getGroupsNotices(groupId);
    if (code === 0) {
      notice.value = data[0];
    }
  }

  // 解散群组
  async function handleDelete() {
    const res = await handleReConfirm('解散');
    if (!res) {
      return;
    }
    const { code, msg } = await groupDelete(groupId.value);
    if (code === 0) {
      Message({ message: msg, type: 'success' });
      closeWindow();
    } else {
      Message({ message: msg, type: 'error' });
    }
  }

  // 退出群组
  async function handleExit() {
    const res = await handleReConfirm('退出');
    if (!res) {
      return;
    }
    const { code, msg } = await exitGroupsMembers(groupId.value);
    if (code === 0) {
      Message({ message: msg, type: 'success' });
      closeWindow();
    } else {
      Message({ message: msg, type: 'error' });
    }
  }

  async function handleReConfirm(text) {
    const res = await MessageBox({
      cancelText: '取消',
      confirmText: '确定',
      isLight: true,
      offset: ['40%', '10%'],
      text: `确定要${text}${currentChat.value.name}群组吗？`,
    });
    return res;
  }

  function closeWindow() {
    emit('closeDialog');
  }

  // 修改群名称
  async function handleConfirm() {
    if (oldGroupName.value === newGroupName.value) {
      Message({ message: '修改的群名称不可与原群名称一致', type: 'error' });
      return;
    }
    const { code, msg } = await groupsUpdate(groupId.value, { name: newGroupName.value });
    if (code === 0) {
      Message({ message: msg, type: 'success' });
      PIMStore.editChatList(currentChat.value, newGroupName.value);
      // 更新ics列表中的群组名称
      await updateListGroupName({ groupId: groupId.value, groupName: newGroupName.value });
      handelCancel();
    } else {
      Message({ message: msg, type: 'error' });
    }
  }

  function handelCancel(flag?) {
    showEditTip.value = flag;
    newGroupName.value = currentChat.value.name || currentChat.value.undefinedName;
    oldGroupName.value = currentChat.value.name || currentChat.value.undefinedName;
    newGroupNameError.value = '';
  }

  function validateNewGroupName() {
    if (newGroupName.value) {
      newGroupNameError.value = '';
      return;
    }
    newGroupNameError.value = newGroupName.value ? '' : '群名称不能为空';
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

  // 管理
  function openNoticeCard() {
    const { chatListActive } = PIMStore;
    const cid = `groupNotice${chatListActive}`;
    Dialog({
      cid,
      content: GroupNotice,
      data: {
        groupId: chatListActive,
        notice: notice.value,
        onSuccess: () => {
          getGroupNoticeData(chatListActive);
        },
      },
    });
  }

  function toQuoteMsg(item) {
    emit('toChatMsg', item);
  }
</script>

<template>
  <TdFrameBox
    :dragger="true"
    :is-light="true"
    :show-arrow="true"
    :show-headerline="true"
    size="small"
    title="群组详情"
    @close-frame-box="closeWindow"
  >
    <div class="group-details-content">
      <div class="content-left">
        <div class="head">
          <TdChatHead :session-type="2" />
          <span class="name">{{ currentChat.name || currentChat.undefinedName }}</span>
          <Icon class="edit" name="option_edit" @click="handelCancel(true)" />
        </div>
        <div class="notice">
          <div class="title">
            <span>群公告</span>
            <Icon class="right-btn" name="right_triangle_arrow" @click="openNoticeCard" />
          </div>
          <div v-if="notice?.content" class="text">{{ notice?.content }}</div>
          <div v-else class="text">暂无公告</div>
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
        <div v-if="!PIMStore.collaboration" class="item border">
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

        <div v-if="isMaster" class="item" @click="handleDelete">
          <span class="red">解散群组</span>
          <Icon class="right-btn" name="right_triangle_arrow" />
        </div>
        <div v-else-if="!xietongGroupFlag" class="item" @click="handleExit">
          <span class="red">退出群组</span>
          <Icon class="right-btn" name="right_triangle_arrow" />
        </div>
      </div>
      <GroupMember class="content-right" />
      <TdTip
        v-if="showEditTip"
        :disable="Boolean(newGroupNameError)"
        @handel-cancel="handelCancel"
        @handle-confirm="handleConfirm"
      >
        <div class="newGroupName">
          <ElInput
            v-model.trim="newGroupName"
            maxlength="50"
            :rows="3"
            show-word-limit
            type="textarea"
            @input="validateNewGroupName()"
          />
          <div v-show="newGroupNameError" class="tip">{{ newGroupNameError }}</div>
        </div>
      </TdTip>
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
  @import '@/styles/mixin.less';

  .group-details-content {
    position: relative;
    display: flex;
    padding: 20px 6px 16px;

    .content-left {
      width: 50%;
      padding-right: 16px;
      border-right: 1px solid rgb(255 255 255 / 20%);

      .head {
        display: flex;
        align-items: center;
        padding: 10px 0;

        .name {
          width: 240px;
          margin: 0 10px;
          color: var(--text-color);
          .ellipsis(2);

          white-space: pre;
        }

        .edit {
          cursor: pointer;
          filter: var(--svg-filter);
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
            color: var(--text-color);
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
          //padding-bottom: 10px;
          font-size: 12px;
          line-height: 150%;
          color: #7d9bbd;
          word-break: break-all;
          white-space: pre-wrap;
          .ellipsis(3);
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

        :deep(.el-switch) {
          .el-switch__core {
            background-color: rgb(125 155 189 / 100%) !important;
            border-color: rgb(125 155 189 / 100%) !important;
          }

          &.is-checked .el-switch__core {
            background-color: rgb(33 150 254 / 100%) !important;
            border-color: rgb(33 150 254 / 100%) !important;
          }
        }
      }

      .border {
        border-bottom: 1px solid rgb(255 255 255 / 20%);
      }
    }

    .content-right {
      width: 50%;
      height: 560px;
      padding-left: 16px;
      border-left: 1px solid rgb(102 102 102 / 20%);
    }

    .newGroupName {
      position: relative;

      .tip {
        position: absolute;
        font-size: 12px;
        color: #ff3b55;
      }
    }
  }
</style>
