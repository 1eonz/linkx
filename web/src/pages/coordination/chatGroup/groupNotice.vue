<script setup lang="ts">
  import { computed, onMounted, ref } from 'vue';

  import { createGroupsNotices, deleteGroupsNotices, updateGroupsNotices } from '@/api/pim';
  import { Message } from '@/components/Message';
  import MessageBox from '@/components/MessageBox';
  import { usePIMStore } from '@/store';
  import dateUtil from '@/utils/dateUtil';

  const props = withDefaults(
    defineProps<{
      groupId: number;
      notice?: any;
    }>(),
    {},
  );

  const emit = defineEmits(['closeDialog', 'success']);
  const PIMStore = usePIMStore();

  const isLook = ref(true);
  const text = ref('');
  const timeStr = ref('');
  const master = ref<any>(null);
  const isFirst = ref(false);

  const showBtn = computed(() => {
    return PIMStore.user?.id === master.value?.id;
  });

  onMounted(() => {
    getNoticeData();
  });

  async function getNoticeData() {
    PIMStore.groupMemberActive.forEach((item) => {
      if (item.role === 2) {
        master.value = item;
      }
    });

    if (props.notice) {
      const { content, gmtModified } = props.notice;
      isFirst.value = false;
      text.value = content;
      const date = new Date(gmtModified);
      timeStr.value = dateUtil.formatDate(date, 'yyyy-MM-dd HH:mm:ss');
    } else {
      isFirst.value = true;
    }
  }

  function updataNotice() {
    isLook.value = false;
  }
  function cancelNotice() {
    text.value = props.notice?.content;
    isLook.value = true;
  }
  async function confirmNotice() {
    if (!text.value) {
      deleteNotices();
      return;
    }

    const res = await MessageBox({
      cancelText: '取消',
      confirmText: '确定',
      isLight: true,
      offset: ['40%', '10%'],
      text: '发布的群公告会通知全部群成员，确定发布吗？',
    });
    if (!res) {
      return;
    }

    if (isFirst.value) {
      createNotices();
    } else {
      updateNotices();
    }
  }

  async function createNotices() {
    const params = {
      content: text.value,
      groupId: props.groupId,
    };
    const { code, msg } = await createGroupsNotices(params);
    if (code === 0) {
      isLook.value = true;
      isFirst.value = false;
      emit('success');
      Message({ message: msg, type: 'success' });
    } else {
      isLook.value = false;
      Message({ message: msg, type: 'error' });
    }
  }

  async function updateNotices() {
    const params = {
      content: text.value,
      groupId: props.groupId,
    };
    const { id } = props.notice;
    const { code, msg } = await updateGroupsNotices(id, params);
    if (code === 0) {
      isLook.value = true;
      isFirst.value = false;
      emit('success');
      Message({ message: msg, type: 'success' });
    } else {
      isLook.value = false;
      Message({ message: msg, type: 'error' });
    }
  }

  async function deleteNotices() {
    const { id } = props.notice;
    const { code, msg } = await deleteGroupsNotices(props.groupId, id);
    if (code === 0) {
      emit('success');
      emit('closeDialog');
      Message({ message: msg, type: 'success' });
    } else {
      isLook.value = false;
      Message({ message: msg, type: 'error' });
    }
  }

  function closeFrame() {
    emit('closeDialog');
  }
</script>

<template>
  <TdFrameBox
    class="group-notice"
    :dragger="true"
    :is-light="true"
    :show-line="true"
    size="another"
    title="群公告"
    @close-frame-box="closeFrame"
  >
    <div v-if="isLook && text" class="notice-text">
      <div class="person">
        <TdChatHead :avatar-id="master?.avatar" />
        <div class="user">
          <div class="name">{{ master?.name }} </div>
          <div class="time">{{ timeStr }}</div>
        </div>
      </div>
      <div v-hyperlinkClick class="text">{{ text }}</div>
    </div>
    <ElInput
      v-if="!isLook"
      v-model="text"
      class="notice-input"
      maxlength="500"
      placeholder="请输入公告"
      :rows="12"
      show-word-limit
      type="textarea"
    />
    <img v-if="!text && isLook" class="empty" src="@/assets/images/common/empty.png" />
    <div v-if="showBtn" class="btns">
      <TdButton
        v-show="isLook"
        :active="true"
        class="btn"
        :is-light="true"
        text="编辑"
        type="normal"
        @click="updataNotice"
      />
      <TdButton
        v-show="!isLook"
        class="btn"
        :is-light="true"
        text="取消"
        type="normal"
        @click="cancelNotice"
      />
      <TdButton
        v-show="!isLook"
        :active="true"
        class="btn"
        :is-light="true"
        text="确定"
        type="normal"
        @click="confirmNotice"
      />
    </div>
    <div v-else class="tip">仅群主可编辑</div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  .group-notice {
    .notice-text {
      padding-top: 13px;
      border-top: 1px solid rgb(153 206 251);

      .person {
        display: flex;
        align-items: center;
        margin-bottom: 10px;

        .user {
          margin-left: 12px;

          .name {
            font-size: 14px;
            font-weight: 400;
            line-height: 150%;
            color: var(--text-color);
          }

          .time {
            font-size: 12px;
            font-weight: 400;
            line-height: 150%;
            color: #7d9bbd;
          }
        }
      }

      .text {
        height: 210px;
        overflow: hidden auto;
        font-size: 14px;
        font-weight: 400;
        line-height: 150%;
        color: var(--text-color);
        word-break: break-all;
        white-space: normal;
      }
    }

    .notice-input {
      padding-top: 9px;
    }

    .empty {
      margin: 64px auto;
    }

    .btns {
      display: flex;
      align-items: center;
      justify-content: flex-end;
      margin: 13px 0;

      .btn {
        width: 52px;
        margin-left: 16px;
        background: var(--td-input-inner-bg) !important;

        :deep(.button-text-light) {
          color: var(--item-text-color);
        }
      }
    }

    .tip {
      margin-bottom: 20px;
      font-size: 12px;
      font-weight: 400;
      color: #7d9bbd;
      text-align: center;
    }
  }

  :deep(.el-input__count) {
    bottom: -12px;
    background-color: transparent;
  }

  :deep(.el-textarea__inner) {
    color: var(--item-text-color) !important;

    &:focus-within {
      color: rgb(125 155 189);
      background: rgb(153 206 251 / 10%);
      border: 1px solid #00c2ff;
    }
  }
</style>
