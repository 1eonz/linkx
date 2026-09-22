<!--
 * @FileDescription: 基础视频点呼组件，非功能性业务逻辑请勿写在这里
 * @Author: 作者信息
 * @Date: 文件创建时间
 * @LastEditors: 最后更新作者
 * @LastEditTime: 最后更新时间
 -->

<script lang="ts" setup>
  import {
    computed,
    nextTick,
    onActivated,
    onBeforeUnmount,
    onDeactivated,
    onMounted,
    ref,
    unref,
    watch,
  } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { VideoCallEnum } from '@/enums';
  import { useEmitter, useI18n, useSetInterval, useWatchSizeChange } from '@/hooks';
  import { mediaFunc } from '@/plugins/mspPlayer';
  import commOpt from '@/plugins/mspPlayer/commOpt';
  import { useCommunicateDispatchStore, useCommunicationStore, useMonitorStore } from '@/store';

  import { debounce, throttle } from 'lodash-es';

  import DistributeVideo from './distributeVideo.vue';

  const props = withDefaults(
    defineProps<{
      account: string;
      communicateCenter?: boolean;
      infoData: any;
      mute?: boolean; // 是否静音，默认静音
    }>(),
    {
      mute: true,
    },
  );
  const emit = defineEmits(['onClose', 'onFullScreen', 'closeDistributeVideo', 'closeMonitor']);
  defineExpose({ changeCardSize, distributeVideo, hangUpClick, maximizeHandle, minimizeHandle });
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();
  const communicateDispatchStore = useCommunicateDispatchStore();
  const monitorStore = useMonitorStore();

  const callStatus = ref('');
  const buttonType = ref('radio');
  const showCallTime = ref(false); // 是否显示计时器标识
  const fullScreen = ref(false);
  const isMonitor = ref(false);
  const cid = ref('');
  const distributeBtnActive = ref(false);
  const playing = ref(false);
  let player: any = null;
  let clearTimer: any = null;
  let intervalTime = 0;
  const operateShow = ref(true);

  const container = computed(() => {
    return VideoCallEnum.CONTAINER + props.account;
  });

  // 播放
  const playMonitorShowClick = debounce(() => {
    playClick();
  }, 300);

  const mounted = throttle(() => {
    if (props.communicateCenter) {
      playMonitorShowClick();
    }
  }, 500);

  const unmount = throttle(() => {
    closeTimer();
    hangUpClick();
    try {
      player?.stop();
      player = null;
      playing.value = false;
    } catch {
      //
    }
  }, 500);

  useWatchSizeChange(unref(container), () => {
    changeResolution();
  });

  watch(
    communicationStore.comm,
    (val) => {
      commHandler(val);
    },
    { deep: true, immediate: true },
  );

  watch(fullScreen, (val) => {
    // 全屏监听ESC缩小
    if (val) {
      window.addEventListener('keydown', handleKeydownCode, true);
    } else {
      window.removeEventListener('keydown', handleKeydownCode, true);
    }
  });

  watch(
    () => props.infoData,
    () => {
      playMonitorShowClick();
    },
    { deep: true },
  );

  onMounted(() => {
    mounted();
  });

  onBeforeUnmount(() => {
    unmount();
    window.removeEventListener('keydown', handleKeydownCode, true);
  });

  onActivated(() => {
    mounted();
  });

  onDeactivated(() => {
    unmount();
  });

  function mouseout() {
    operateShow.value = false;
  }

  function mouseover() {
    operateShow.value = true;
  }

  function handleKeydownCode(e) {
    if (e.keyCode === 27) {
      minimizeHandle();
    }
  }

  async function playClick() {
    const obj = communicationStore.comm[props.account];
    if (obj?.video) {
      closeTimer();
      return;
    }
    const re = await commOpt.monitorCall({
      container: unref(container),
      toUid: unref(props.account),
      type: '1',
    });
    if (re === '-5') {
      if (!props.communicateCenter) {
        Message(t('monitor.monitorFunction.maxViews'));
        return;
      }
      if (intervalTime >= 30) {
        closeTimer();
        return;
      }
      if (!clearTimer && props.communicateCenter) {
        clearTimer = useSetInterval(() => {
          playClick();
          intervalTime += 5;
        }, 5000);
      }
      return;
    }
    closeTimer();
    if (re !== '0') {
      player?.play();
      player = null;
      playing.value = false;
    }
  }

  function closeTimer() {
    if (clearTimer) {
      clearTimer?.();
      clearTimer = null;
      intervalTime = 0;
    }
  }

  // 挂断
  function hangUpClick() {
    const { account } = props;

    useEmitter().emit('isShowDistribute', account);
    commOpt.hangUp(unref(isMonitor) ? 'monitor' : 'video', appConfig.isdn, account);

    try {
      player?.stop();
      emit('onClose', props.infoData);
    } catch {
      emit('onClose', props.infoData);
    }
    callStatus.value = '';
    showCallTime.value = false;
    player = null;
    playing.value = false;
  }

  // 视频分发
  async function distributeVideo() {
    distributeBtnActive.value = true;
    const { account } = props;
    Dialog({
      cid: `distributeVideo${account}`,
      content: DistributeVideo,
      data: { playItemCode: account },
      offset: ['100px', '100px'],
      onClose() {
        distributeBtnActive.value = false;
        if (props.communicateCenter) {
          emit('closeDistributeVideo');
        }
      },
      zIndexDefault: 2001,
    });
  }

  function changeCardSize() {
    if (fullScreen.value) {
      minimizeHandle();
      operateShow.value = true;
    } else {
      maximizeHandle();
      setTimeout(() => {
        operateShow.value = false;
      }, 3000);
    }
  }

  // 放大
  function maximizeHandle() {
    emit('onFullScreen', true);
    fullScreen.value = true;
    setTimeout(() => {
      operateShow.value = false;
    }, 3000);
  }

  // 缩小
  function minimizeHandle() {
    emit('onFullScreen', false);
    fullScreen.value = false;
    operateShow.value = true;
  }

  function changeResolution() {
    nextTick(() => {
      const el = document.getElementById(unref(container));
      if (player?.changeVideResolution && el) {
        player.changeVideResolution({
          height: el?.offsetHeight,
          width: el?.offsetWidth,
        });
      }
    });
  }

  // 放大状态关闭
  function closeHandle() {
    minimizeHandle();
    hangUpClick();
    if (props.communicateCenter) {
      communicateDispatchStore.deleteMonitor(props.infoData);
    }
  }

  // comm变化
  function commHandler(val) {
    const { account } = props;
    const { isdn, opt, type, updateTime, value } = val.updateInfo;

    if (!isdn || isdn !== account || type !== 'video') {
      return;
    }

    const date = Date.now() - updateTime;

    // 关闭
    if (opt === 'delete' && date < 200) {
      callStatus.value = '';
      showCallTime.value = false;
      monitorStore.deleteMonitorDrawerDataByAccount(props.infoData.account);
      emit('onClose', props.infoData);
      return;
    }

    if (!val[isdn]) return;

    // 播放
    const target = val[isdn][type];
    const { wssUrl } = value || {};
    isMonitor.value = type === 'monitor';
    callStatus.value = target.status?.msg;

    if (!player && wssUrl) {
      showCallTime.value = true;
      callStatus.value = '';
      cid.value = value.cid;
      nextTick(() => {
        const el = document.getElementById(unref(container));
        const playerConfig = {
          cid: unref(cid),
          el,
          sharpType: { height: el?.offsetHeight, width: el?.offsetWidth },
          wsUrl: wssUrl,
        };
        if (el?.offsetWidth && el?.offsetHeight) {
          player = new window.MSP_PLAYER(playerConfig);
          playing.value = true;
        }

        setTimeout(() => {
          changeResolution();
          if (props.mute) {
            mediaFunc.muteSpeaker(unref(cid));
          }
        }, 0);
      });
    }

    if (target.endFlag === 0) {
      showCallTime.value = false;
      callStatus.value = '';
    }
  }
</script>

<template>
  <div class="video-call-volume" :class="{ 'full-screen': fullScreen }" @dblclick="changeCardSize">
    <!-- header -->
    <div class="volume-header">
      <div class="name dragger">
        <span>{{ infoData.name }}</span>
        <span v-show="!!callStatus" class="status">（{{ callStatus }}）</span>
      </div>

      <div class="btn">
        <Icon name="close" @click.stop="closeHandle" />
      </div>
    </div>

    <TdCallTimer v-if="showCallTime" class="call-time" :isdn="props.account" type="video" />

    <!-- 视频容器 -->
    <div :id="container" class="monitor-container" :class="{ 'placeholder-bg': !playing }"></div>

    <!-- footer btn -->
    <div v-show="showCallTime" class="footer-btn" @mouseout="mouseout" @mouseover="mouseover">
      <div class="left" @dblclick.stop>
        <!-- 麦克风 -->
        <TdMicrophone :cid="cid" class="btn" color="rgba(26, 255, 251, 1)" :in-flag="true" />
        <!-- 音量 -->
        <TdVolumeRange
          :button-type="buttonType"
          :cid="cid"
          class="btn"
          color="rgba(26, 255, 251, 1)"
          voice-img="voice_on"
        />
        <div v-if="!fullScreen" class="btn">
          <TdTooltip :content="t('mrs.operation.fullscreen')" placement="top">
            <Icon
              color="rgba(26, 255, 251, 1)"
              name="full"
              prefix="bigScreen"
              @click.stop="maximizeHandle"
            />
          </TdTooltip>
        </div>
        <div v-else class="btn">
          <TdTooltip :content="t('desktop.button.exitFull')" placement="top">
            <Icon
              color="rgba(26, 255, 251, 1)"
              name="exit_full"
              prefix="bigScreen"
              @click.stop="minimizeHandle"
            />
          </TdTooltip>
        </div>
      </div>

      <div class="right">
        <!-- 挂断 -->
        <TdTooltip :content="t('communication.communicationFunction.hangUpMonitor')">
          <TdButton
            class="hang-up"
            icon-name="phone_hang_up"
            type="radioWarn"
            @click.stop="closeHandle"
          />
        </TdTooltip>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .video-call-volume {
    // background: url('@/assets/images/camera/no_camera.png') no-repeat center;
    position: relative;
    width: 100%;
    height: 100%;
    background-color: #19293c;

    .volume-header {
      position: absolute;
      top: 0;
      left: 0;
      z-index: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 32px;
      padding: 0 8px 0 0;
      font-size: 14px;
      font-weight: 700;
      color: rgb(255 255 255 / 100%);
      letter-spacing: 2px;
      background: linear-gradient(180deg, rgb(6 41 74 / 64%) 0%, rgb(6 41 74 / 26%) 100%);
      backdrop-filter: blur(8px);

      .name {
        width: 290px;
        padding-left: 24px;
        overflow: hidden;
        line-height: 18px;
        text-overflow: ellipsis;
        white-space: nowrap;
        background: url('@/assets/images/popup/title_bg.png') no-repeat;
        background-size: 116px 32px;

        .status {
          font-size: 12px;
        }
      }

      .btn {
        display: flex;

        .td-icon {
          width: 16px;
          height: 16px;
          margin-left: 16px;
          cursor: pointer;
        }
      }
    }

    .call-time {
      position: absolute;
      top: 36px;
      left: 0;
      z-index: 1;
      padding-left: 8px;
    }

    .monitor-container {
      width: 100%;
      height: 100%;
    }

    .footer-btn {
      position: absolute;
      right: 0;
      bottom: 10px;
      z-index: 1;
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 32px;
      padding: 0 10px;

      & > div {
        display: flex;
        align-items: center;
      }

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

      .hang-up {
        float: right;
        width: 22px;
        height: 22px;
      }
    }
  }

  .full-screen {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 2000 !important;
    width: 100%;
    height: 100%;

    .info-title {
      position: absolute;
      top: 28px;
      left: 32px;
    }
  }

  .icon-hang-down {
    position: absolute;
    left: 10px;
    width: 22px;
    height: 22px;
    font-size: var(--font-size-large);
    cursor: pointer;
    background: none;
    border: none;

    :deep(.icon) {
      width: 22px;
      height: 22px;
    }

    &:hover {
      background: none !important;
    }
  }

  .full-monitor-operate {
    position: absolute;
    bottom: 0;
    z-index: 502;
    width: 100%;
    height: 76px;

    :deep(.operate-btn) {
      display: flex-block;
      background: rgb(255 255 255 / 0%);
    }
  }

  .hidden-operate {
    :deep(.operate-btn) {
      display: none;
    }

    .monitor-operate-box {
      visibility: hidden !important;
    }
  }
</style>
