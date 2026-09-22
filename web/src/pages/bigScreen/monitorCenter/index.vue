<script lang="ts" setup>
  import { onBeforeUnmount, onMounted, ref } from 'vue';

  import DesktopContent from '@/pages/communicationCenter/desktopContent.vue';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useCommunicateDispatchStore } from '@/store';

  import ScreenChange from '../components/screenChange.vue';

  const communicateDispatchStore = useCommunicateDispatchStore();
  const desktopContent = ref();

  onMounted(() => {
    window.addEventListener('beforeunload', beforeunloadFunc);
    // 根据自己需要来监听对应的key
    window.addEventListener('storage', storageFunc);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', beforeunloadFunc);
    window.removeEventListener('storage', storageFunc);
  });

  function beforeunloadFunc() {
    commOpt.hangUpAll();
  }
  function storageFunc(e) {
    if (e.key === 'layoutMonitor') {
      communicateDispatchStore.initLayoutMonitor();
    }
  }
</script>

<template>
  <div
    class="monitor-center"
    :class="{ 'monitor-center-full': communicateDispatchStore.monitorCenterFull }"
  >
    <div class="monitor-content">
      <DesktopContent ref="desktopContent" :monitor-center="true" />
    </div>
    <ScreenChange />
  </div>
</template>

<style lang="less" scoped>
  .monitor-center {
    width: 100%;
    height: 100%;
    background-size: 100% 100%;

    .monitor-content {
      position: absolute;
      top: 92px;
      left: 11%;
      width: 78%;
      height: calc(100% - 150px);

      :deep(.desktop-content) {
        width: 100% !important;
      }

      .sybthesize-operate {
        position: absolute;
        top: -40px;
        right: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100px;
        height: 32px;
        line-height: 32px;
        background:
          rgb(0 0 0 / 60%),
          linear-gradient(180deg, rgb(19 147 232 / 7%) 0%, rgb(19 147 232 / 100%) 100%);
        border: 1px solid rgb(0 194 255 / 100%);
        box-shadow: inset 0 0 4px rgb(0 118 252 / 100%);

        .screen-p-icon {
          width: 14px;
          height: 14px;
          padding: 0 !important;
          margin-right: 5px;
          fill: #fff !important;
        }

        span {
          color: rgb(153 206 251 / 100%);
        }
      }

      .content {
        height: calc(100% - 110px);
      }
    }
  }

  .monitor-center-full {
    .monitor-content {
      position: absolute;
      top: 0;
      left: 0;
      z-index: 2000 !important;
      width: 100%;
      height: 100%;

      .desktop-content {
        margin: 0;
      }

      :deep(.synthesize-desktop) {
        position: absolute;
        top: 0;
        height: 100%;
        padding: 0;

        .monitor-desktop-box {
          height: 100%;
        }
      }
    }
  }
</style>
