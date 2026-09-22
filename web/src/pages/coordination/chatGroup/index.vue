<script setup lang="ts">
  import { ref, watch } from 'vue';

  import { getGroupsNotices } from '@/api/pim';
  import { Dialog } from '@/components/Dialog';
  import { usePIMStore } from '@/store';

  import GroupMember from './groupMember.vue';
  import GroupNotice from './groupNotice.vue';

  const props = defineProps<{
    noticeTip?: string;
  }>();

  const PIMStore = usePIMStore();
  const notice = ref<any>(null);

  watch(
    () => props.noticeTip,
    () => {
      getGroupNoticeData(PIMStore.chatListActive);
    },
  );

  watch(
    () => PIMStore.chatListActive,
    (val) => {
      getGroupNoticeData(val);
    },
  );

  async function getGroupNoticeData(groupId) {
    notice.value = null;
    const { code, data } = await getGroupsNotices(groupId);
    if (code === 0) {
      notice.value = data[0];
    }
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
</script>

<template>
  <div class="chat-group">
    <div class="notice">
      <div class="title">
        <span>群公告</span>
        <Icon class="right-btn" name="right_triangle_arrow" @click="openNoticeCard" />
      </div>
      <div v-if="notice?.content" class="text tip">{{ notice?.content }}</div>
      <div v-else class="tip">暂无公告</div>
    </div>
    <GroupMember :class="notice?.content ? 'short' : 'long'" />
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .chat-group {
    width: 320px;
    height: calc(100% - 10px);
    padding: 10px 16px;
    border-bottom: 1px solid rgb(0 0 0 / 10%);

    .notice {
      .title {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 44px;
        padding: 12px 0;

        span {
          font-size: 14px;
          font-weight: 500;
          color: var(--text-color);
        }

        .right-btn {
          width: 10px;
          height: 10px;
          color: var(--item-text-color);
          cursor: pointer;
        }
      }

      .text {
        word-break: break-all;
        white-space: pre-wrap;
        .ellipsis(6);
      }

      .tip {
        width: 290px;
        //padding: 10px 0;
        font-size: 12px;
        color: var(--text-color);
      }
    }

    .short {
      height: calc(100% - 140px);
    }

    .long {
      height: calc(100% - 50px);
    }
  }
</style>
