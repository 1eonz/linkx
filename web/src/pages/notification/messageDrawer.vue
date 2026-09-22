<script setup lang="ts">
  import { computed, onMounted, ref, unref } from 'vue';

  import { emptyChatMessage } from '@/api/sms';
  import { openGroupDetailsPopup } from '@/comm/group';
  import { openMessageDetailsPopup } from '@/comm/message';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  import { useCommunicationStore } from '@/store';

  import MessageItem from './messageItem.vue';

  const emit = defineEmits(['close']);

  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  const keyword = ref('');

  // 短信列表及搜索
  const messageList = computed(() => {
    let msgList: Array<any> = [];
    communicationStore.msgListCache?.forEach((list) => {
      const txt = unref(keyword);
      if (txt) {
        let show = false;

        if (list.title.includes(txt)) {
          show = true;
        }
        if (!list.groupId && list.userId.includes(txt)) {
          show = true;
        }
        if (show) {
          msgList.push(list);
        }
      } else {
        msgList = communicationStore.msgListCache;
      }
    });

    return msgList;
  });

  useEmitter('updateMsgList', () => {
    communicationStore.queryMessageList();
  });

  onMounted(async () => {
    await communicationStore.queryMessageList();
    await communicationStore.queryMessageList();
  });

  function closeDrawer() {
    emit('close');
  }

  function itemClick(data) {
    if (data.groupId) {
      openGroupDetailsPopup({
        data,
        id: data.groupId,
      });
    } else {
      openMessageDetailsPopup(data);
    }
  }

  // 一键清空
  async function handleClearMsg() {
    const { code } = await emptyChatMessage({ account: appConfig.isdn });
    if (code === 0) {
      communicationStore.queryMessageList();
    }
  }
</script>

<template>
  <TdFrameBox
    class="message-drawer"
    :title="t('communication.msgFunction.list')"
    @close-frame-box="closeDrawer"
  >
    <div class="message-container">
      <div class="title">
        <TdInput
          v-model="keyword"
          :placeholder="t('common.search.searchContext')"
          type="searchInput"
        />
        <TdButton
          :text="t('communication.msgFunction.clear')"
          type="normal"
          @click="handleClearMsg"
        />
      </div>

      <div v-if="messageList.length > 0" class="list">
        <MessageItem
          v-for="(item, index) in messageList"
          :key="index"
          :data="item"
          @click="itemClick(item)"
        />
      </div>
      <TdEmpty v-else />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .message-drawer {
    position: absolute;
    top: 94px;
    right: 30px;
    z-index: 101;
    height: calc(100vh - 120px);

    .message-container {
      display: flex;
      flex-direction: column;
      height: 100%;

      .title {
        display: flex;
        padding: 16px 0;

        .td-input {
          margin-right: 4px;
        }
      }

      .list {
        flex: 1;
        width: 100%;
        overflow: hidden auto;
      }
    }
  }
</style>
