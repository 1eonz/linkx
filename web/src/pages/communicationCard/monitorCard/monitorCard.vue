<!--
 * 基础视频监控组件，非功能性业务逻辑请勿写在这里
 * TODO：怎么保证传入数据的一致性？（查询设备详情比较好点吗）
 -->

<script lang="ts" setup>
  import { computed, nextTick, onBeforeUnmount, onMounted, ref, unref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { CategoryEnum, VideoCallEnum } from '@/enums';
  import { useEmitter, useI18n, useSetInterval, useWatchSizeChange } from '@/hooks';
  import { getAccountByEquipmentData } from '@/pages/resource/resourceHelper';
  import { mediaFunc, queryFunc, videoFunc } from '@/plugins/mspPlayer';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { communicationStatus } from '@/plugins/mspPlayer/commStatus';
  import {
    useCommunicateDispatchStore,
    useCommunicationStore,
    useMainStore,
    useMonitorStore,
    useVideoPollStore,
  } from '@/store';
  import { delay } from '@/utils';
  import { isArray } from '@/utils/is';

  import { throttle } from 'lodash-es';

  import DistributeVideo from '../distributeVideo.vue';
  import MonitorCardHeader from './monitorCardHeader.vue';
  import { setQueueList } from './monitorQueue';
  import PtzControl from './ptzControl.vue';

  const props = withDefaults(
    defineProps<{
      dragger?: boolean;
      isMax?: boolean;
      monitorInfo: any;
      mousedown?: boolean; // 鼠标是否按下，优化拉伸时调用改变分辨率方法的频率
      mute?: boolean; // 是否静音，默认静音
      showClose?: boolean;
      showHeader?: boolean;
    }>(),
    {
      mute: true,
      showClose: true,
      showHeader: true,
    },
  );
  const emit = defineEmits(['onClose', 'onFullScreen', 'closeDistributeVideo']);

  const route = useRoute();
  const monitorStore = useMonitorStore();
  const communicationStore = useCommunicationStore();
  const communicateDispatchStore = useCommunicateDispatchStore();
  const videoPollStore = useVideoPollStore();

  const { t } = useI18n();
  const { setIsFull } = useMainStore();

  const isHover = ref(false);
  const isFull = ref(false);
  const cid = ref('');
  const playing = ref(false);
  let player: any = null;
  let clearTimer: any;
  let intervalTime = 0;

  const facilityIsdn = computed(() => {
    return getAccountByEquipmentData(props.monitorInfo) || '';
  });
  const container = computed(() => {
    return VideoCallEnum.MONITOR_CONTAINER + unref(facilityIsdn);
  });
  const isFixedMonitor = computed(() => {
    return Number(props.monitorInfo.category) === CategoryEnum.monitor;
  });
  const isDispatch = computed(() => {
    return !!props.monitorInfo.comm?.value.dispatch;
  });
  const isCommunicateCenter = computed(() => route.name === 'communicateCenter');

  const handleSizeChange = throttle(() => {
    if (!props.mousedown) {
      setQueueList(changeResolution);
    }
  }, 500);

  useWatchSizeChange(unref(container), handleSizeChange);

  defineExpose({ changeCardSize, closeCard, distributeVideo, handleSizeChange });

  watch(isFull, (val) => {
    setIsFull(val);
    useEmitter().emit('enlargeZIndex', val);

    // 全屏监听ESC缩小
    if (val) {
      window.addEventListener('keydown', handleKeydown, true);
    } else {
      window.removeEventListener('keydown', handleKeydown, true);
      nextTick(leaveMonitor);
    }
  });
  watch(communicationStore.comm, (val) => {
    commChange(val);
  });
  watch(
    () => props.mute,
    (val) => {
      if (val) {
        mediaFunc.muteSpeaker(unref(cid));
      } else {
        // 固定摄像头静音
        if (!unref(isFixedMonitor)) {
          mediaFunc.unmuteSpeaker(unref(cid));
        }
      }
    },
  );

  // 播放
  const playClick = async () => {
    const isdn = unref(facilityIsdn);

    // 判断通信能力是否正常
    if (!isdn || !communicationStore.hasComm) {
      return;
    }

    const el = document.getElementById(unref(container));
    if (!el) {
      console.info('el not fond');
      return;
    }

    const rsp: any = await commOpt.monitorCall({
      container: unref(container),
      mute: unref(isFixedMonitor) && !unref(isDispatch) ? '1' : '0', // 固定摄像头直接静音拉起
      toUid: isdn,
      type: '0',
    });

    if (rsp === '-5') {
      reconnection();
    } else {
      handleClearTimer();
    }
  };

  onMounted(() => {
    isFull.value = !!props.isMax;

    const isdn = unref(facilityIsdn);
    const { dispatch } = communicationStore.comm[isdn]?.monitor || {};
    // 视频回传
    if (dispatch) {
      handleClearTimer();
      commChange(communicationStore.comm);
    } else {
      // 视频查看
      playClick();
    }
  });

  onBeforeUnmount(() => {
    // 如果组件卸载时是全屏状态则需要取消全屏状态
    if (unref(isFull)) {
      setIsFull(false);
      emit('onFullScreen', false);
    }

    handleClearTimer();
    // 断开通信
    clickHangUp();
    window.removeEventListener('keydown', handleKeydown, true);
  });

  function handleKeydown(e) {
    if (e.keyCode === 27) {
      changeCardSize();
    }
  }

  // 通讯状态改变
  function commChange(val) {
    const { isdn, opt, type, updateTime, value } = val.updateInfo;
    if (isdn !== unref(facilityIsdn) || type !== 'monitor') {
      return;
    }

    const date = Date.now() - updateTime;
    // TODO: 什么情况下不需要关闭卡片？需要一个参数来控制？
    if (opt === 'delete' && date < 200) {
      // 界面恢复
      stopPlayer();
      const { monitorInfo } = props;
      monitorStore.deleteMonitorDrawerDataByAccount(monitorInfo.account);
      communicateDispatchStore.deleteMonitor(monitorInfo);
    }

    if (!val[isdn]) {
      return;
    }

    const monitorStatus = val[isdn]?.monitor?.status.status;
    const { CALLING } = communicationStatus();

    if (monitorStatus === CALLING.status) {
      cid.value = value.cid;
      {
        const { cid, wssUrl } = value;
        const el = document.getElementById(unref(container));
        if (!el) {
          return;
        }
        const playerConfig = {
          cid,
          el,
          sharpType: { height: el.offsetHeight, width: el.offsetWidth },
          wsUrl: wssUrl,
        };
        player = new window.MSP_PLAYER(playerConfig);
        playing.value = true;
        handleSizeChange();
        setTimeout(() => {
          if ((props.mute && !unref(isDispatch)) || unref(isFixedMonitor)) {
            mediaFunc.setVolume(cid, 1);
            mediaFunc.muteSpeaker(cid);
          }
        }, 200);
      }
    }
  }

  // 重连
  function reconnection() {
    handleClearTimer();
    if (intervalTime >= 30) {
      return;
    }
    if (!clearTimer && unref(isCommunicateCenter)) {
      clearTimer = useSetInterval(() => {
        playClick();
        intervalTime += 5;
      }, 5000);
    }
  }

  function handleClearTimer() {
    clearTimer?.();
    clearTimer = null;
    intervalTime = 0;
  }

  // 视频分享
  function distributeVideo() {
    Dialog({
      cid: `distributeVideo${unref(facilityIsdn)}`,
      content: DistributeVideo,
      data: { playItemCode: unref(facilityIsdn) },
      offset: ['500px', '80px'],
      onClose() {
        if (unref(isCommunicateCenter)) {
          emit('closeDistributeVideo');
        }
      },
      zIndexDefault: 2001,
    });
  }

  let fullTime = 0;
  async function changeCardSize(hasCallback = true) {
    const max = !isFull.value;
    if (max) {
      fullTime = Date.now();
      player?.changeVideResolution({
        height: window.innerHeight,
        width: window.innerWidth,
      });
      await delay(500);
    }
    isFull.value = max;
    if (hasCallback) {
      emit('onFullScreen', max);
    }
  }

  // 分辨率改变
  function changeResolution() {
    // 全屏时已经提前改变了分辨率
    if (Date.now() - fullTime < 1500) {
      return;
    }

    const el = document.getElementById(unref(container));
    if (player?.changeVideResolution && el) {
      player.changeVideResolution({
        height: el?.offsetHeight,
        width: el?.offsetWidth,
      });
    }
  }

  // 挂断
  function clickHangUp() {
    hangUp();
    if (!unref(isCommunicateCenter)) {
      emit('onClose', props.monitorInfo);
    }
  }

  function hangUp() {
    const { monitorInfo } = props;
    commOpt.hangUp('monitor', appConfig.isdn, unref(facilityIsdn));
    stopPlayer();
    isHover.value = true;
    useEmitter().emit('isShowDistribute', monitorInfo.id);
    monitorStore.updatePlayingList({
      facilityId: monitorInfo.id,
      isAdd: false,
    });
  }

  function stopPlayer() {
    player?.stop?.();
    /**
     * Too many active WebGL contexts. Oldest context will be lost
     * 必须调用 destroy loseContext 保证当前页面活动的WebGL context小于上限
     */
    player?.destroy?.();
    player = null;
    playing.value = false;
    const el = document.getElementById(unref(container));
    if (el) {
      const canvas = el.querySelector('canvas');
      const gl = canvas?.getContext('webgl');
      gl?.getExtension('WEBGL_lose_context')?.loseContext();

      el.innerHTML = '';
    }
  }

  function enterMonitor() {
    isHover.value = true;
  }

  function leaveMonitor() {
    isHover.value = false;
  }

  // 上墙
  async function upWall() {
    const { rsp, list } = await queryFunc.queryDecoder();
    if (rsp === '0' && isArray(list)) {
      videoFunc.videoStartUploadWall(unref(facilityIsdn), list[0].chnno);
    } else {
      Message(t('monitor.monitorFunction.upWallFailed'));
    }
  }

  // 关闭业务
  function closeCard() {
    if (appConfig.isdn) {
      clickHangUp();
    } else {
      console.log('appConfig.isdn is null,hangUp failed.');
    }
    emit('onClose', props.monitorInfo);
    if (isFull.value) {
      changeCardSize();
    }
    if (unref(isCommunicateCenter)) {
      communicateDispatchStore.deleteMonitor(props.monitorInfo);
    }
  }
</script>

<template>
  <div
    class="monitor-card"
    :class="{ 'monitor-card-max': isFull }"
    @dblclick="changeCardSize(true)"
    @mouseenter="enterMonitor"
    @mouseleave="leaveMonitor"
    @mousemove="enterMonitor"
  >
    <!-- header -->
    <MonitorCardHeader
      v-show="showHeader && isHover"
      :dragger="dragger"
      :full="isFull"
      :info="monitorInfo"
      :show-close="showClose"
      @close="closeCard"
    />

    <!-- 视频容器 -->
    <div :id="container" class="video-container" :class="{ 'placeholder-bg': !playing }"></div>

    <!-- 云台控制 -->
    <PtzControl
      v-show="isHover"
      :category="Number(monitorInfo.category)"
      :isdn="facilityIsdn"
      :ptz-width="isFull ? 98 : 40"
    />

    <!-- footer btn -->
    <div v-if="isHover" class="footer-operation" @dblclick.stop>
      <div class="left">
        <div class="btn">
          <TdTooltip :content="t('monitor.monitorFunction.share')" placement="top">
            <Icon
              class="projection-icon"
              color="rgba(26, 255, 251, 1)"
              name="projection"
              prefix="bigScreen"
              @click.stop="distributeVideo"
            />
          </TdTooltip>
        </div>
        <TdVolumeRange
          v-if="!isFixedMonitor"
          button-type="radio"
          :cid="cid"
          class="btn"
          color="rgba(26, 255, 251, 1)"
        />
        <div class="btn">
          <TdTooltip
            :content="isFull ? t('desktop.button.exitFull') : t('mrs.operation.fullscreen')"
            placement="top"
          >
            <Icon
              class="projection-icon"
              color="rgba(26, 255, 251, 1)"
              :name="isFull ? 'exit_full' : 'full'"
              prefix="bigScreen"
              @click.stop="changeCardSize"
            />
          </TdTooltip>
        </div>
        <div class="btn">
          <TdTooltip :content="t('monitor.monitorFunction.upWall')" placement="top">
            <Icon
              class="projection-icon"
              color="rgba(26, 255, 251, 1)"
              name="up_wall"
              prefix="bigScreen"
              @click.stop="upWall"
            />
          </TdTooltip>
        </div>
      </div>
      <div class="right">
        <TdTooltip
          v-if="!videoPollStore.clearPollTimer"
          :content="t('monitor.monitorFunction.stop')"
          placement="top"
        >
          <Icon class="close" name="video_stop" @click.stop="closeCard" />
        </TdTooltip>
      </div>
    </div>

    <TdCorner />
  </div>
</template>

<style lang="less" scoped>
  @import '@/styles/mixin.less';

  .monitor-card {
    position: relative;
    width: 100%;
    height: 100%;
    background-color: #19293c;

    .video-container {
      width: 100%;
      height: 100%;
      pointer-events: none;
      user-select: none;

      &.placeholder-bg::after {
        position: absolute;
        top: 50%;
        left: 50%;
        z-index: 1;
        width: 60px;
        height: 60px;
        margin-top: -30px;
        margin-left: -30px;
        content: '';
        background: url('@/assets/images/camera/no_camera.png') no-repeat;
        background-position: center;
        background-size: contain;
      }
    }

    .footer-operation {
      position: absolute;
      bottom: 0;
      left: 0;
      display: flex;
      justify-content: space-between;
      width: 100%;
      padding: 8px;

      .left,
      .right {
        display: flex;
        align-items: center;

        .btn {
          display: flex;
          align-items: center;
          justify-content: center;
          width: 22px;
          height: 22px;
          margin-right: 8px;
          cursor: pointer;
          background: rgb(7 18 42 / 100%);
          border: 1px solid rgb(25.5 255 251.175 / 100%);
          border-radius: 50%;

          svg {
            width: 12px !important;
            height: 12px !important;
            fill: rgb(25.5 255 251.175 / 100%);
          }

          :deep(.icon-box) {
            background: none;
            border: none;
            border-radius: none;

            svg {
              width: 12px !important;
              height: 12px !important;
            }
          }
        }

        :deep(.slider-box) {
          margin-top: -160px;
        }

        .close {
          width: 22px;
          height: 22px;
          cursor: pointer;
        }
      }
    }
  }

  .monitor-card-max {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 2000;
    width: 100vw !important;
    height: calc(100vh - 1px) !important;
    cursor: default;
    background: black;
  }
</style>
