<script lang="ts" setup>
  import { computed, ref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useEmitter } from '@/hooks';
  import MonitorCard from '@/pages/communicationCard/monitorCard/monitorCard.vue';
  import VideoDistributePerson from '@/pages/communicationCard/monitorCard/videoDistributePerson.vue';
  import VideoCallVolume from '@/pages/communicationCard/videoCallVolume.vue';
  import { useCommunicationStore } from '@/store';

  const props = defineProps<{
    account: string;
    cid: string;
    communicateCenter?: boolean;
    infoData: any;
  }>();
  const emit = defineEmits(['closeDialog']);

  const communicationStore = useCommunicationStore();

  const isMax = ref(false);
  const showDistribute = ref(false);
  const distributeListData = ref([]);
  const videoCallRef = ref();
  const monitorRef = ref();

  const isMonitor = computed(() => {
    return ['facility', 'monitor'].includes(props.infoData.resourceType);
  });

  watch(communicationStore.distributeStatus, (val) => {
    const { isdn } = val.updateInfo;
    if (!isdn || isdn !== props.account) {
      return;
    }
    if (val[isdn]) {
      showDistribute.value = true;
      distributeListData.value = Object.values(val[isdn]);
    } else {
      showDistribute.value = false;
    }
  });

  useEmitter('enlargeZIndex', enlargeListener);

  function enlargeListener(val) {
    isMax.value = val;
  }

  // 是否全屏
  function fullScreen(data) {
    Dialog(props.cid).fullScreen(data);
  }
  // 子组件关闭 触发关闭弹窗
  function onClose() {
    setTimeout(() => emit('closeDialog'), 0);
  }
</script>

<template>
  <div class="video-popup-container ground-glass">
    <div class="section">
      <div class="video-box">
        <MonitorCard
          v-if="isMonitor"
          ref="monitorRef"
          class="map-monitor-card"
          :class="{ 'map-monitor-card_min': !isMax }"
          :monitor-info="infoData"
          @on-close="onClose"
          @on-full-screen="fullScreen"
        />

        <VideoCallVolume
          v-else
          ref="videoCallRef"
          :account="account"
          :communicate-center="communicateCenter"
          :info-data="infoData"
          :mute="false"
          @on-close="onClose"
          @on-full-screen="fullScreen"
        />
      </div>

      <!-- 视频分发人员 -->
      <VideoDistributePerson
        v-show="showDistribute && !communicateCenter"
        v-model:person-list="distributeListData"
        class="distribute-person"
        :src-code="account"
      />
    </div>
  </div>
</template>

<style lang="less">
  .map-monitor-card {
    border: none !important;
    border-radius: unset;

    &_min {
      width: auto !important;

      .name {
        display: none !important;
      }

      .monitor-content {
        .close-icon {
          display: none;
        }

        .size-icon {
          display: none;
        }
      }
    }

    .details-content {
      .detail {
        margin-top: 0 !important;

        .info {
          display: none !important;
        }
      }
    }
  }
</style>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .video-popup-container {
    position: relative;
    display: flex;

    .header {
      position: absolute;
      top: 0;
      left: 0;
      z-index: 100;
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 32px;
      padding: 0 10px;
      background: linear-gradient(180deg, rgb(6 41 74 / 64%) 0%, rgb(6 41 74 / 26%) 100%);
      backdrop-filter: blur(8px);

      .title {
        z-index: 1;
        padding-left: 24px;
        font-size: 16px;
        background: url('@/assets/images/popup/title_bg.png') no-repeat;
        background-size: 116px 32px;
      }

      .info-span {
        .ellipsis1();
      }

      .btn {
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: flex-end;

        .close-btn {
          width: 20px;
          height: 20px;
          margin-left: 8px;
          cursor: pointer;
        }

        .zoom-btn {
          width: 16px;
          height: 16px;
          cursor: pointer;
          fill: var(--icon-color-normal);
        }
      }

      .header-bg {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
      }
    }

    .section {
      position: relative;

      .video-box {
        width: 412px;
        height: 232px;

        :deep(.info-title) {
          top: 36px;
        }

        :deep(.monitor-show-card) {
          display: none;
        }

        :deep(.detail .info) {
          display: none;
        }

        :deep(.info-name) {
          display: none;
        }
      }
    }
  }
</style>
