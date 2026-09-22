<script lang="ts" setup>
  import { onBeforeMount, onMounted, ref, unref, watch } from 'vue';
  import { useRoute, useRouter } from 'vue-router';

  import VideoPollTime from '@/comm/video/components/videoPollTime.vue';
  import MessageBox from '@/components/MessageBox';
  import { useEmitter, useI18n, usePermissions } from '@/hooks';
  import MrsContent from '@/pages/mrs/mrsContent.vue';
  import { handleSubscribeGroup, multiScreenJump } from '@/pages/resource/resourceHelper';
  import { useCommunicateDispatchStore, useTreeStore, useVideoPollStore } from '@/store';

  import DesktopChangeTabs from './components/desktopChangeTabs.vue';
  import SynthesizeDesktop from './resources/synthesizeDesktop/synthesizeDesktop.vue';

  const props = defineProps<{
    history?: boolean;
    monitorCenter?: boolean;
  }>();
  defineExpose({ closeMonitor });

  const { t } = useI18n();
  const route = useRoute();
  const router = useRouter();
  const communicateDispatchStore = useCommunicateDispatchStore();
  const treeStore = useTreeStore();
  const videoPollStore = useVideoPollStore();

  const componentsArr = ref<any[]>([]);
  const monitorLayoutArr = ref<any[]>([]);
  const monitorPixelArr = ref<any[]>([]); // 摄像头分辨率调整oldMonitors
  const commLayoutArr = ref<any[]>([]);
  const loadingFlag = ref(false);
  const deskTopActive = ref<any>({});

  watch(
    () => treeStore.resourceOneKeyType,
    (val) => {
      if (val === 'VideoWatch') {
        unref(componentsArr).forEach((item) => {
          if (item.id === 'MonitorDesktop') {
            desktopChange(item);
          }
        });
      } else if (['VideoConf', 'VoiceConf'].includes(val)) {
        unref(componentsArr).forEach((item) => {
          // 只有在通信调度屏才需要去切换tab
          if (item.id === 'ConferenceDesktop' && route.path === '/communicationCenter') {
            desktopChange(item);
          }
        });
      }
    },
    { deep: true },
  );

  watch(route, (oldVal) => {
    if (oldVal.path.includes('communicationCenter')) {
      const index = unref(componentsArr).findIndex((i) => i.show);
      communicateDispatchStore.setCurActiveDeskTop(unref(componentsArr)[index].id);
    }
  });
  watch(
    () => communicateDispatchStore.curActiveDesktop,
    (val) => {
      if (!val) return;
      getDeskTopActive();
    },
    { deep: true },
  );

  onMounted(() => {
    initTabs();
    window.addEventListener('beforeunload', closeConfer, { passive: false });
    window.addEventListener('keydown', handleKeydown, true);
    useEmitter('goConfTab', goConfTab);
  });

  onBeforeMount(() => {
    window.removeEventListener('beforeunload', closeConfer);
    window.removeEventListener('keydown', handleKeydown, true);
  });

  function goConfTab() {
    const { id } = deskTopActive.value;
    if (['CommunicateDesktop', 'MonitorDesktop'].includes(id)) {
      desktopChange(componentsArr.value[2]);
    }
  }

  function handleKeydown(e) {
    if (e.keyCode === 27) {
      communicateDispatchStore.setMonitorCenterFull(false);
    }
  }

  function initTabs() {
    componentsArr.value = [
      {
        componentName: 'SynthesizeDesktop',
        iconName: 'synthesize',
        id: 'SynthesizeDesktop',
        name: t('desktop.type.synthesize'),
        show: usePermissions('CONFERENCE'),
      },
      {
        componentName: 'SynthesizeDesktop',
        iconName: 'monitor',
        id: 'MonitorDesktop',
        name: t('desktop.type.monitor'),
        onlyMonitor: true,
        show: true,
      },
      {
        componentName: 'SynthesizeDesktop',
        iconName: 'micro',
        id: 'CommunicateDesktop',
        name: t('desktop.type.communicate'),
        onlyCommunicate: true,
        show: true,
      },
      {
        componentName: 'SynthesizeDesktop',
        iconName: 'conference',
        id: 'ConferenceDesktop',
        name: t('desktop.type.conference'),
        onlyConference: true,
        show: usePermissions('CONFERENCE'),
      },
    ];
    monitorLayoutArr.value = [
      {
        id: 24,
        name: '1×1',
      },
      {
        id: 12,
        name: '2×2',
      },
      {
        id: 8,
        name: '3×3',
      },
      {
        id: 6,
        name: '4×4',
      },
    ];
    monitorPixelArr.value = [
      {
        id: 1,
        name: 'QCIF',
        support: 16,
      },
      {
        id: 2,
        name: 'CIF',
        support: 16,
      },
      {
        id: 4,
        name: 'D1',
        support: 16,
      },
      {
        id: 8,
        name: '720P',
        support: 16,
      },
      {
        id: 16,
        name: '1080P',
        support: 12,
      },
      {
        id: 32,
        name: '2K',
        support: 6,
      },
      {
        id: 64,
        name: '4K',
        support: 3,
        // disabled: true,
      },
    ];
    commLayoutArr.value = [
      {
        id: 8,
        name: '3×4',
      },
      {
        id: 6,
        name: '4×4',
      },
    ];

    getDeskTopActive();

    if (props.monitorCenter) {
      deskTopActive.value = componentsArr.value[0];
    }
  }

  // 通信调度当前选中桌面
  function getDeskTopActive() {
    const curActive = communicateDispatchStore.curActiveDesktop;
    const index = componentsArr.value.findIndex((item) => {
      return item.id === curActive;
    });
    deskTopActive.value = componentsArr.value[index];
  }

  async function desktopChange(item) {
    const { clearPollTimer, clearVideoPollTimer } = videoPollStore;
    if (clearPollTimer && item.id === 'SynthesizeDesktop') {
      const res = await MessageBox({
        text: t('homePage.noticeCenterData.pollNotSupport'),
        type: 'ok',
      });
      if (!res) {
        return;
      }
      clearVideoPollTimer();
    }

    if (item.id === 'SynthesizeDesktop') {
      communicateDispatchStore.resetMonitorList();
    }

    deskTopActive.value = item;
    communicateDispatchStore.setCurActiveDeskTop(item.id);
  }

  function closeConfer() {
    communicateDispatchStore.setCloseConferFlag();
  }

  async function closeAll() {
    const res = await MessageBox({ text: t('desktop.other.sureClose') });
    if (!res) {
      return;
    }
    switch (unref(deskTopActive).id) {
      case 'CommunicateDesktop': {
        closeComm();
        break;
      }
      case 'ConferenceDesktop': {
        closeConfer();
        break;
      }
      case 'MonitorDesktop': {
        closeMonitor();
        useEmitter().emit('closeMrs');
        break;
      }
      case 'SynthesizeDesktop': {
        closeMonitor();
        closeComm();
        closeConfer();
        break;
      }
    }
  }

  function closeComm() {
    const arr = [...communicateDispatchStore.selectedCommList];
    arr.forEach((item) => {
      handleSubscribeGroup(item, true);
    });
  }

  function closeAllVideoPoll() {
    communicateDispatchStore.addMonitorList([null, null, null, null]);
    videoPollStore.clearVideoPollTimer();
  }

  function closeMonitor(rePlay?: boolean) {
    if (videoPollStore.clearPollTimer && !rePlay) {
      closeAllVideoPoll();
    }

    const arr = [...communicateDispatchStore.monitorDesktopList];

    communicateDispatchStore.clearMonitor();

    // 切换视频清晰度之后重新播放
    const play = () => {
      if (arr.length > 0) {
        communicateDispatchStore.addMonitorList(arr.shift());
        setTimeout(play, 700);
      } else {
        loadingFlag.value = false;
      }
    };
    if (rePlay) {
      loadingFlag.value = true;
      setTimeout(play, 1000);
    }
  }

  // 投屏
  async function projection() {
    if (communicateDispatchStore.projectionCenter) {
      communicateDispatchStore.windowOpen?.close();
      return;
    }
    const config = {
      iconName: 'bigscreen_monitor',
      openScreen: 3,
      path: '/monitorCenter',
      title: t('homePage.navigateData.monitorCenter'),
    };
    const popup = await multiScreenJump(config, router);
    if (popup) {
      communicateDispatchStore.setWindowOpen(popup);
    }
  }
</script>

<template>
  <div
    id="desktopContent"
    class="desktop-content"
    :class="{
      'monitor-center-full': communicateDispatchStore.monitorCenterFull,
    }"
  >
    <div class="desktop-header">
      <div>
        <DesktopChangeTabs
          v-if="!monitorCenter"
          :active-id="deskTopActive.id"
          class="top-change"
          :tabs-arr="componentsArr"
          @change="desktopChange"
        />
      </div>
      <div class="header-right">
        <VideoPollTime v-if="videoPollStore.clearPollTimer" />
        <TdButton class="layout-item" @click="projection">
          <Icon name="show_stop" prefix="bigScreen" />
          <span>
            {{
              communicateDispatchStore.projectionCenter
                ? t('desktop.button.stopPlay')
                : t('desktop.button.startPlay')
            }}
          </span>
        </TdButton>
        <TdButton class="layout-item" type="normal" @click="closeAll">
          <Icon name="close" prefix="bigScreen" />
          <span>{{ t('desktop.button.closeAll') }}</span>
        </TdButton>
      </div>
    </div>

    <MrsContent v-if="history" />
    <SynthesizeDesktop
      v-else
      :component-id="deskTopActive.id"
      :loading-flag="loadingFlag"
      :monitor-center="monitorCenter"
      :only-communicate="deskTopActive.onlyCommunicate"
      :only-conference="deskTopActive.onlyConference"
      :only-monitor="deskTopActive.onlyMonitor"
      :synthesize-flag="deskTopActive.id === 'SynthesizeDesktop'"
      @change="desktopChange"
      @close-all="closeAll"
    />
  </div>
</template>

<style lang="less" scoped>
  .desktop-content {
    position: relative;
    box-sizing: border-box;
    width: calc(100% - 422px);
    min-width: 800px;
    height: 100%;
    margin-left: 12px;

    .desktop-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 11px;

      .header-right {
        position: relative;
        display: flex;
        align-items: center;
        justify-content: flex-end;

        .pixel-content {
          position: absolute;
          top: 36px;
          right: 0;
          z-index: 3;
          box-sizing: border-box;
          width: 294px;
          padding: 24px 16px 14px;

          .pixel-content-item {
            box-sizing: border-box;
            height: 32px;
            margin-bottom: 10px;
            line-height: 32px;
            text-align: center;
            cursor: pointer;
            background: rgb(255 255 255 / 0%);
            box-shadow: inset 0 0 4px rgb(0 118 252 / 100%);
          }

          .pixel-content-item-active {
            color: rgb(26 255 251 / 100%);
            background: linear-gradient(
              180deg,
              rgb(19 147 232 / 7%) 0%,
              rgb(19 147 232 / 100%) 100%
            );
            border: 1px solid rgb(0 194 255 / 100%);
            box-shadow: inset 0 0 4px rgb(0 118 252 / 100%);
          }

          .pixel-item-disable {
            color: rgb(166 169 171);
            cursor: not-allowed;
          }
        }
      }

      .layout-item {
        width: auto;
        height: 32px;
        margin-left: 12px;
        font-size: 16px;
        font-weight: 500;
        line-height: 32px;
        text-align: center;
        vertical-align: top;

        &:hover {
          span {
            color: #fff;
          }

          .td-icon {
            fill: #fff;
          }
        }

        span {
          color: rgb(153 206 251 / 100%);
        }

        .td-icon {
          width: 16px;
          height: 16px;
          margin-right: 4px;
          fill: rgb(153 206 251 / 100%);
        }
      }

      .create-group {
        height: 24px;
        padding: 0 10px;
        font-family: '优设标题黑';
        font-size: 16px;
        font-weight: 400;
        color: rgb(224 240 255 / 100%);
        text-align: center;
        background: url('@/assets/images/communicate/create-group.png') no-repeat;
        background-size: 100% 100%;
      }
    }
  }

  .monitor-center-full {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 999 !important;
    width: 100%;
    height: 100%;
    margin: 0;

    .btn {
      position: absolute;
      top: 32px;
      right: 16px;
      z-index: 11;
      display: flex;
      align-items: center;

      .icon-btn {
        width: 22px;
        height: 22px;
        margin-right: 10px;
        cursor: pointer;
        fill: var(--icon-color-normal);
      }

      .full-close {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 48px;
        height: 32px;
        cursor: pointer;
        background: rgb(255 96 96 / 100%);

        .icon-btn {
          width: 16px;
          height: 16px;
          margin-right: 0;
        }
      }
    }

    :deep(.synthesize-desktop) {
      height: 100%;
      padding-bottom: 0;
    }

    :deep(.desktop-header) {
      display: none;
      margin: 0;
    }

    .monitor-content {
      position: absolute;
      top: 0;
      left: 0;
      z-index: 10;
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

      :deep(.monitor-operate) {
        display: none;
      }
    }
  }
</style>
