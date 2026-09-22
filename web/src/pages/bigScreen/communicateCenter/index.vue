<script lang="ts" setup>
  import CommunicationCenter from '@/pages/communicationCenter/index.vue';
  import { useCommunicateDispatchStore } from '@/store';

  import ScreenChange from '../components/screenChange.vue';

  const communicateDispatchStore = useCommunicateDispatchStore();
</script>

<template>
  <div
    class="communicate-center"
    :class="{ 'communicate-center-full': communicateDispatchStore.monitorCenterFull }"
  >
    <div class="content">
      <CommunicationCenter />
    </div>
    <ScreenChange />
  </div>
</template>

<style lang="less" scoped>
  .communicate-center {
    width: 100%;
    height: calc(100% - 94px);

    .content {
      height: calc(100% - 16px);

      :deep(.communicate-dispatch) {
        padding-top: 0;
        background: none;
      }
    }
  }

  .communicate-center-full {
    .content {
      width: 100%;
      height: 100%;

      :deep(.monitor-desktop-box) {
        height: 100%;
        background-color: black;
      }
    }

    .map-header {
      display: none;
    }
  }
</style>
