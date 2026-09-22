<script setup lang="ts">
  import { watch } from 'vue';

  import NotificationItem from '@/pages/notification/notificationItem.vue';
  import { useCommunicationStore } from '@/store';

  const props = defineProps<{
    data: any;
  }>();
  const emit = defineEmits(['closeDialog']);

  const communicationStore = useCommunicationStore();

  watch(
    () => communicationStore.comm,
    ({ updateInfo }) => {
      const { isdn, opt, type } = updateInfo;
      if (['halfdial', 'voice'].includes(type) && isdn === props.data.isdn && opt === 'delete') {
        emit('closeDialog');
      }
    },
    { deep: true },
  );
</script>

<template>
  <div class="voice-popup">
    <div class="dragger"></div>
    <NotificationItem :data="data" />
  </div>
</template>

<style scoped lang="less">
  .voice-popup {
    position: relative;

    .dragger {
      position: absolute;
      top: 8px;
      left: 0;
      width: 100%;
      height: 36px;
    }

    :deep(.message-item) {
      margin: 8px 0;
      background: linear-gradient(270deg, rgb(4 64 105 / 90%) 0%, rgb(6 41 74 / 90%) 100%);
      backdrop-filter: blur(2px);
      border: 2px solid rgb(0 194 255 / 100%);
    }

    :deep(.notification-item) {
      flex-direction: column;
      width: 260px;
      height: 162px;
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
