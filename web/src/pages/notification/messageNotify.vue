<script setup lang="ts">
  import { computed, onMounted, ref, unref, watch } from 'vue';

  import { openGroupDetailsPopup } from '@/comm/group';
  import { openMessageDetailsPopup } from '@/comm/message';
  import { appConfig } from '@/config';
  import { useEmitter } from '@/hooks';
  import { openVoicePointCallPopup } from '@/pages/map/openVideoPopup';
  import NotificationItem from '@/pages/notification/notificationItem.vue';
  import { communicationStatus } from '@/plugins/mspPlayer/commStatus';
  import {
    useCommunicationStore,
    useConferenceStore,
    useMonitorStore,
    useVideoPollStore,
  } from '@/store';

  import MessageItem from './messageItem.vue';

  const conferenceStore = useConferenceStore();
  const communicationStore = useCommunicationStore();
  const monitorStore = useMonitorStore();
  const videoPollStore = useVideoPollStore();

  const msgList = ref<any[]>([]);
  const incomingList = ref<any[]>([]);
  const holdList = ref<string[]>([]);

  const showIncomingList = computed(() => {
    return unref(incomingList).filter((item) => {
      if (item.value.callee === appConfig.isdn) {
        return communicationStore.sdkInitStatus === '0';
      }
      return true;
    });
  });
  const hasMonitor = computed<any>(() => {
    return (
      monitorStore.monitorDrawerData.length > 0 || videoPollStore.videoPollDrawerData.length > 0
    );
  });

  watch(
    () => communicationStore.comm,
    ({ updateInfo }) => {
      const { RINGING, WAITING_ANSWER } = communicationStatus();
      const { isdn, opt, type, value } = updateInfo;
      const { status } = updateInfo.status || {};

      if (opt === 'delete') {
        incomingList.value = unref(incomingList).filter((item) => {
          return !(item.type === type && item.isdn === isdn);
        });
        // 取消保持或者挂断后清除保持列表内的数据
        const index = unref(holdList).indexOf(isdn);
        if (index !== -1) {
          holdList.value.splice(index, 1);
        }
        return;
      }

      if (['monitor', 'video'].includes(type) && status?.status !== 'incoming') {
        incomingList.value = unref(incomingList).filter((item) => {
          return !(item.type === type && item.isdn === isdn);
        });
        return;
      }

      const param = { account: isdn, isdn, status, type, value };
      if ([RINGING.status, WAITING_ANSWER.status].includes(status?.status)) {
        if (type === 'voice') {
          incomingList.value.push(param);
        } else if (status.status === 'incoming') {
          incomingList.value.push(param);
        }
      }

      // 点击通话保持按钮后才加入保持列表，初次通话保持接入时不显示
      if (type === 'hold') {
        if (!holdList.value.includes(isdn)) {
          holdList.value.push(isdn);
        }
        return;
      }

      if (type === 'halfdial' && ['calling', 'connecting', 'init'].includes(status?.status)) {
        openVoicePointCallPopup(param);
      }
    },
    { deep: true },
  );
  watch(conferenceStore.confer, (val) => {
    const remove = () => {
      const index = unref(incomingList).findIndex((i) => i.value.conferenceId);
      incomingList.value.splice(index, 1);
    };
    if (!val.conferenceId) {
      remove();
      return;
    }

    if (val.status === 'ringing') {
      confDialInRingingHandler(val);
    } else {
      remove();
    }
  });

  onMounted(() => {
    useEmitter('newMsg', (data) => {
      const time = new Date();
      msgList.value.push({
        ...data,
        time,
      });
      setTimeout(() => {
        handleClose(unref(msgList).findIndex((i) => i.time === time));
      }, 3000);
    });
  });

  async function confDialInRingingHandler(val) {
    const { isVideo, unifiedAccessCode } = val;
    val.type = isVideo === 'true' ? 'videoConfer' : 'audioConfer';
    const index = unref(incomingList).findIndex((i) => i.value.conferenceId);
    if (index === -1) {
      const param = {
        isdn: unifiedAccessCode,
        status: { status: 'incoming' },
        type: isVideo === 'true' ? 'videoConfer' : 'audioConfer',
        value: val,
      };
      incomingList.value.push(param);
    }
  }

  async function itemClick(data) {
    // 警务协同消息提示
    if (data.isCollaboration) {
      return;
    }
    // 短彩信消息提示
    if (data.groupId) {
      openGroupDetailsPopup({
        data,
        id: data.groupId,
      });
    } else {
      openMessageDetailsPopup(data);
    }
  }

  function handleClose(index) {
    msgList.value.splice(index, 1);
  }
</script>

<template>
  <div class="message-notify" :class="{ 'has-monitor-play': hasMonitor }">
    <div class="current-notify">
      <MessageItem
        v-for="(item, index) in msgList"
        :key="index"
        :data="item"
        :show-close-icon="true"
        @click="itemClick(item)"
        @close="handleClose(index)"
      />

      <NotificationItem
        v-for="(item, index) in showIncomingList"
        :key="index"
        :data="item"
        :hold-list="holdList"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .message-notify {
    position: fixed;
    top: 85px;
    right: 40px;
    z-index: 2000;

    &.has-monitor-play {
      right: 450px;
    }
  }

  .current-notify {
    :deep(.message-item) {
      background: linear-gradient(270deg, rgb(4 64 105 / 90%) 0%, rgb(6 41 74 / 90%) 100%);
      backdrop-filter: blur(2px);
      border: 2px solid rgb(0 194 255 / 100%);
    }

    :deep(.notification-item) {
      flex-direction: column;
      width: 260px;
      height: 138px;
      padding: 16px;
      margin: 8px 0;
      background: linear-gradient(270deg, rgb(4 64 105 / 90%) 0%, rgb(6 41 74 / 90%) 100%);
      border: 2px solid rgb(0 194 255 / 100%);
      box-shadow: 0 0 10px rgb(38 229 246 / 40%);

      .item-name {
        .user-name {
          margin-bottom: 4px;
          font-size: 14px;
          font-weight: 500;
          text-align: center;
        }

        .right-bottom {
          justify-content: center;
          font-size: 12px;
          font-weight: 400;
          text-align: center;
        }
      }

      .operation {
        .btn-item {
          width: 36px;
          height: 36px;
          margin: 0;
        }

        .answer-btn {
          margin-right: 64px;
        }
      }
    }
  }
</style>
