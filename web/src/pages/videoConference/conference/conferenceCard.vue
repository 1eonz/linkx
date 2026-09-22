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

  import { queryConference } from '@/api/conference';
  import { Dialog } from '@/components/Dialog';
  import MessageBox from '@/components/MessageBox';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n, useWatchSizeChange } from '@/hooks';
  import { removeDragElement } from '@/pages/communicationCenter/common';
  import ModuleHeader from '@/pages/communicationCenter/resources/synthesizeDesktop/moduleHeader.vue';
  import {
    deleteConferenceHandler,
    saveConferenceInfoHandler,
  } from '@/pages/videoConference/common';
  import { confFunc } from '@/plugins/mspPlayer';
  import { useCommunicateDispatchStore, useConferenceStore } from '@/store';

  import Draggable from 'vuedraggable';

  import ConferenceCardControl from './conferenceCardControl.vue';
  import ConferenceDetail from './conferenceDetail.vue';
  import ConferenceMember from './conferenceMember.vue';
  import ConferenceOperation from './conferenceOperation.vue';

  const props = withDefaults(
    defineProps<{
      bigScreen?: boolean;
      isMain?: boolean;
      onlyConference?: boolean;
    }>(),
    {
      bigScreen: false,
    },
  );
  const emit = defineEmits(['closeDialog', 'dragFunc']);

  const { t } = useI18n();
  const conferenceStore = useConferenceStore();
  const communicateDispatchStore = useCommunicateDispatchStore();

  const containerId = 'conferenceVideoContainer';
  const showMember = ref(true);
  const isMini = ref(false);
  const isFull = ref(false);
  const showInfo = ref(false);
  const conference = ref<any>({});
  const conferenceHeader = ref<any>({});
  let player: any = null;
  const isMax = ref(false);
  const containerRef = ref();
  let onChangeResolution = false;

  const isFullShow = computed(() => {
    if (props.onlyConference) {
      return props.onlyConference;
    } else {
      if (isFull.value) {
        return true;
      }
      return false;
    }
  });
  const isChair = computed(() => {
    let isChairman = false;
    conferenceStore.confMember.forEach((item) => {
      if (item.isChairman === 1 && item.number === appConfig.isdn) {
        isChairman = true;
      }
    });
    return isChairman;
  });
  const dropName = computed(() => {
    if (showInfo.value) {
      return 'drop_up';
    }
    return 'drop_down';
  });
  const isVideo = computed(() => {
    return conference.value?.isVideo === 1;
  });
  const micSwitch = computed(() => {
    return (
      conferenceStore.confer.conferenceMember?.find((v) => v.number === appConfig.isdn)?.isMute ===
      '0'
    );
  });
  const conferenceIng = computed(() => {
    const val = conferenceStore.confer;
    if (val.conferenceId && val.status === 'success') {
      return true;
    }
    return false;
  });
  const isSynthesize = computed(() => {
    return communicateDispatchStore.curActiveDesktop === 'SynthesizeDesktop';
  });

  useWatchSizeChange(containerId, () => {
    changeResolution();
  });

  watch(conferenceStore.confer, handleConferChange, { deep: true });
  watch(isFull, (val) => {
    if (props.bigScreen) {
      isMax.value = val;
    } else {
      Dialog('conferenceCard').fullScreen(val);
    }
  });
  watch(() => communicateDispatchStore.closeConferFlag, reLeaveConf);

  // 创建会议成功
  useEmitter('OnCreateConfSuccess', (confer) => {
    conferenceStore.clearBroadcastData();
    saveConferenceInfoHandler(confer);
  });
  useEmitter('OnEndConfSuccess', () => {
    try {
      player?.stop?.();
      player = null;
      conferenceStore.clearConferData();
      close();
    } catch {
      //
    }
  });
  useEmitter('OnUnsubscribeConfSuccess', close);
  useEmitter('conferMemberChooseFromMap', (val) => {
    if (val) {
      miniCard();
    } else {
      recover();
    }
  });

  onMounted(() => {
    conferenceHeader.value = {
      backWidth: '100%',
      componentName: 'SynthesizeDesktop',
      iconName: 'conference',
      id: 'ConferenceDesktop',
      onlyConference: true,
      otherName: t('desktop.button.goConference'),
      title: t('videoConference.conferenceInfo.member'),
    };

    handleConferChange(true);

    window.addEventListener('beforeunload', reLeaveConf);
    window.addEventListener('keydown', handleKeydownCode);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('beforeunload', reLeaveConf);
    window.removeEventListener('keydown', handleKeydownCode);
  });

  onActivated(() => {
    handleConferChange(true);
  });

  onDeactivated(() => {
    player = null;
  });

  function handleConferChange(init?: boolean) {
    getConferDetail();

    const { confer } = conferenceStore;
    const { cid, status, wssUrl } = confer;

    const initPlayer = () => {
      const el = document.getElementById(containerId);
      if (!el) return;
      const playerConfig = {
        cid,
        el,
        sharpType: {
          height: el?.offsetHeight,
          width: el?.offsetWidth,
        },
        wsUrl: wssUrl,
      };
      player = new window.MSP_PLAYER(playerConfig);
      changeResolution();
    };

    if (init === true && wssUrl) {
      setTimeout(initPlayer, 0);
    } else if (status === 'success' && wssUrl && !player) {
      initPlayer();
    } else if (status === 'end' && conferenceStore.uniqueCode) {
      close();
    }

    reBroadcastMember(confer);
  }

  function handleKeydownCode(e) {
    if (e.keyCode === 27) {
      recover();
    }
  }

  function dragFuncConfer(e) {
    removeDragElement();

    const data = JSON.parse(e.from.dataset.info);
    emit('dragFunc', [], true, [data]);
  }

  async function getConferDetail() {
    const { uniqueCode } = conferenceStore;
    if (uniqueCode) {
      const { code, data } = await queryConference({ uniqueCode });
      if (code === 0 && data) {
        conference.value = data;
      }
    }
  }

  function toOther() {
    communicateDispatchStore.setCurActiveDeskTop(conferenceHeader.value.id);
  }

  // 离会的广播成员重新上会需要重新广播
  function reBroadcastMember(confer) {
    const { conferenceId, conferenceMember } = confer;
    const { broadcastMember, broadcastOrWatch, flexType, lastNotConnectedBroadcastMember } =
      conferenceStore;
    // 说明存在广播
    if (broadcastMember.length === 0) return;

    const online: string[] = [];
    const offline: string[] = [];
    conferenceMember.forEach((item) => {
      const { number, participantStatus } = item;
      // 过滤出是广播and状态是Connected
      if (broadcastMember.includes(number)) {
        if (participantStatus === 'Connected') {
          online.push(number);
        } else {
          offline.push(number);
        }
      }
    });

    if (lastNotConnectedBroadcastMember.length > 0) {
      let reConnected = false;
      online.forEach((item) => {
        if (lastNotConnectedBroadcastMember.includes(item)) {
          reConnected = true;
        }
      });
      if (reConnected) {
        const memberInfos = broadcastMember.map((number) => {
          return { number };
        });
        const params: [string, any[], string] = [conferenceId, memberInfos, flexType];
        if (broadcastOrWatch === 'broadcast') {
          confFunc.broadcastMixPicture(...params);
        } else {
          confFunc.watchMixPicture(...params);
        }
      }
    }

    conferenceStore.setLastNotConnectedBroadcastMember(offline);
  }

  function handlerMicSwitch(type) {
    confFunc.muteConfMember(conferenceStore.confer.conferenceId, String(type), appConfig.isdn);
  }

  function showDetail() {
    showInfo.value = !showInfo.value;
  }

  function showPerson() {
    showMember.value = !showMember.value;
  }

  function muteConf(isMute) {
    confFunc.muteConf(conferenceStore.confer.conferenceId, isMute);
  }

  // 最小
  function miniCard() {
    showInfo.value = false;
    isFull.value = false;
    isMini.value = true;
    showMember.value = false;
    nextTick(() => {
      const el = containerRef.value;
      if (el) {
        el.style.top = '100px';
        el.style.left = `${window.innerWidth - 410}px`;
      }
    });
  }

  // 复原
  function recover() {
    isFull.value = false;
    isMini.value = false;
    showMember.value = true;
    nextTick(() => {
      const el = containerRef.value;
      if (el && !props.bigScreen) {
        const { offsetHeight, offsetWidth } = el;
        el.style.left = `calc(50% - ${offsetWidth / 2}px - 15vh)`;
        el.style.top = `calc(50% - ${offsetHeight / 2}px - 15vh)`;
        el.style.transform = 'none';
      }
    });
  }

  // 全屏
  function fullScreen() {
    isFull.value = true;
    isMini.value = false;
    showMember.value = true;
  }

  async function closeCard() {
    const text = isChair.value
      ? t('videoConference.conferenceInfo.endMsg')
      : t('videoConference.conferenceInfo.leaveMsg');
    const exitRes = await MessageBox({ iconName: 'icon_warning', text });
    if (!exitRes) {
      return;
    }
    reLeaveConf();
    if (unref(isFull)) {
      recover();
    }
  }

  // 结束会议 - 不管调用成功失败都要关闭弹窗
  async function reLeaveConf() {
    const { cid, conferenceId, isVideo } = conferenceStore.confer;
    if (isChair.value) {
      confFunc.endConf(conferenceId);
      const { code } = await deleteConferenceHandler();
      if (code === 0) {
        conferenceStore.setConfMember([]);
      }
    } else {
      if (isVideo === 'true') {
        confFunc.exitVideoConf(cid);
      } else {
        confFunc.exitAudioConf(cid);
      }
    }
    conference.value = {};
    close();
  }

  function close() {
    // 关闭邀请弹窗
    Dialog('chooseConferenceMember')?.close();
    emit('closeDialog');
  }

  // 分辨率改变
  function changeResolution() {
    if (onChangeResolution) {
      return;
    }
    nextTick(() => {
      const el = document.getElementById(containerId);
      if (player?.changeVideResolution && el) {
        player.changeVideResolution({
          height: el?.offsetHeight,
          width: el?.offsetWidth,
        });
      }
      onChangeResolution = false;
    });
  }
</script>

<template>
  <div
    ref="containerRef"
    class="conference-card"
    :class="{
      'min-card': isMini,
      'max-card': isMax,
      'map-card': !isMain,
      'big-screen-card': bigScreen,
      synthesize: isSynthesize,
    }"
  >
    <!-- 最小化顶部 -->
    <div v-show="isMini" class="conference-header-min dragger ground-glass">
      <TdTooltip :content="conference.name || ''">
        <div class="conference-name"> {{ conference.name || '' }}</div>
      </TdTooltip>
      <Icon class="screen-p-icon" name="close" @click="closeCard" />
    </div>

    <!-- 会商视频 -->
    <div class="conference-card-left">
      <ConferenceDetail v-if="showInfo" :confer-info="conference" :conference-ing="conferenceIng" />

      <div class="conference-view">
        <div v-show="!isMini" class="info-header dragger ground-glass">
          <div class="info-header-left">
            <div class="header-btn" @click="showDetail">
              <span class="info-name"> {{ t('videoConference.conferenceInfo.id') }}</span>
              <span v-show="conferenceIng" class="info-id">
                {{ conferenceStore.uniqueCode }}
              </span>
              <Icon class="info-drop" :name="dropName" />
            </div>
          </div>

          <!-- T.Right btn -->
          <ConferenceCardControl
            v-show="!showMember || bigScreen"
            :big-screen="bigScreen"
            :is-full="isFull"
            :is-mini="isMini"
            @close-card="closeCard"
            @full-screen="fullScreen"
            @mini-card="miniCard"
            @recover="recover"
          />
        </div>

        <!-- video contain -->
        <div class="conference-main">
          <div :id="containerId" style="width: 100%; height: 100%"></div>
          <Draggable
            class="conference-drag"
            group="bigDrag"
            item-key="id"
            :list="[{}]"
            @add="dragFuncConfer"
          >
            <template #item>
              <div v-show="!conferenceIng" class="conference-block">
                <ElRow>
                  <ElCol v-for="item in [1, 2, 3, 4]" :key="item" :span="12">
                    <TdEmpty v-if="isMain" :has-bg="true" :title="t('common.tdcomp.drag')" />
                    <TdEmpty v-else :has-bg="true" />
                  </ElCol>
                </ElRow>
              </div>
            </template>
          </Draggable>

          <TdCorner />
        </div>
      </div>

      <!-- bottom btn -->
      <ConferenceOperation
        :conference-ing="conferenceIng"
        :disabled="!player"
        :is-chair="isChair"
        :is-main="isMain"
        :is-mini="isMini"
        :is-video="isVideo"
        :mic-switch="micSwitch"
        :only-conference="onlyConference"
        @close-conference="closeCard"
        @handler-mic-switch="handlerMicSwitch"
        @recover="recover"
        @show-person="showPerson"
      />
    </div>

    <!-- 与会人员 -->
    <div v-show="showMember" class="conference-card-right ground-glass">
      <div v-if="bigScreen" class="numbers-header">
        <ModuleHeader :header-obj="conferenceHeader" :prohibit="isFullShow" @to-other="toOther" />
      </div>

      <ConferenceCardControl
        v-else
        :big-screen="bigScreen"
        :is-full="isFull"
        :is-mini="isMini"
        @close-card="closeCard"
        @full-screen="fullScreen"
        @mini-card="miniCard"
        @recover="recover"
      />

      <ConferenceMember v-if="conferenceIng" :is-video="isVideo" />

      <div v-if="isChair" class="conference-mute">
        <span @click="muteConf('true')">
          {{ t('videoConference.confFunc.muteAll') }}
        </span>
        |
        <span @click="muteConf('false')">
          {{ t('videoConference.confFunc.unMuteAll') }}
        </span>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .conference-card {
    position: relative;
    display: flex;
    width: 970px;
    height: 520px;

    .conference-card-left {
      position: relative;
      display: flex;
      flex: 1;
      flex-direction: column;
      height: 100%;

      .full-btn {
        position: absolute;
        top: 8px;
        right: 8px;
        width: 16px;
        height: 16px;
        cursor: pointer;
      }

      .info-header {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 100%;
        height: 32px;
        border-right: none;
        border-bottom: none;

        .info-header-left {
          box-sizing: border-box;
          padding-left: 10px;
          font-size: 14px;
          background-size: 100% 100%;

          .header-btn {
            display: flex;
            align-items: center;
            justify-content: left;
            width: 100%;
            height: 32px;
            padding-left: 16px;
            cursor: pointer;
            background: url('@/assets/images/popup/title_bg.png') no-repeat;
            background-size: 115px 32px;

            span {
              font-size: 16px;
            }

            .screen-p-icon {
              width: 24px;
              height: 24px;
              margin-right: 3px;
            }

            .info-name {
              display: inline-block;
              height: 24px;
              margin-right: 5px;
              line-height: 24px;
            }

            .info-id {
              margin: 0 5px;
              word-break: break-all;
            }

            .info-drop {
              width: 14px;
              height: 14px;
              fill: var(--icon-color-normal);
            }

            .info-copy {
              width: 40px;
              height: 18px;
              font-size: 12px;
            }
          }
        }
      }

      .conference-view {
        height: calc(100% - 46px);
      }

      .conference-main {
        position: relative;
        width: 100%;
        height: calc(100% - 32px);
        background: rgb(0 0 0 / 100%);
        border: 1px solid rgb(35 133 219 / 100%);
        box-shadow: inset 0 0 6px 4px rgb(35 133 219 / 40%);

        .conference-block {
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 100%;

          .el-row {
            width: 100%;
            height: 100%;

            .el-col {
              &:nth-of-type(odd) {
                border-right: 1px solid #3298e2;
              }

              &:nth-of-type(-n + 2) {
                border-bottom: 1px solid #3298e2;
              }

              .block-page {
                border: none;
              }

              :deep(.empty-bg) {
                border: none !important;
              }
            }
          }
        }

        .conference-drag {
          position: absolute;
          top: 0;
          z-index: 1;
          width: 100%;
          height: 100%;
        }
      }
    }

    .conference-card-right {
      width: 310px;
      height: 100%;
      background: rgb(6 41 74 / 60%);
      backdrop-filter: blur(8px);

      .conference-mute {
        display: flex;
        align-items: center;
        height: 46px;

        span {
          display: flex;
          justify-content: center;
          width: 50%;
          font-size: 14px;
          color: rgb(153 206 251 / 100%);
          cursor: pointer;
        }
      }
    }
  }

  .min-card {
    left: 308px !important;
    display: block;
    width: 308px !important;
    height: 185px !important;
    backdrop-filter: blur(8px);

    .conference-header-min {
      display: flex;
      align-items: center;
      justify-content: space-between;
      height: 32px;
      border-bottom: none;

      .conference-name {
        width: 270px;
        margin-left: 8px;
        overflow: hidden;
        font-size: 14px;
        color: #fff;
        text-overflow: ellipsis;
        white-space: nowrap;
      }
    }

    .conference-card-left {
      width: 100%;
      height: calc(100% - 32px);

      .conference-main {
        height: 100%;
      }
    }

    .screen-p-icon {
      width: 20px;
      height: 20px;
      margin-right: 8px;
      cursor: pointer;
    }
  }

  .max-card {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 2000;
    width: 100vw;
    height: 100vh;
    background: black;
  }

  .big-screen-card.max-card {
    z-index: 1000;
  }

  .synthesize {
    .conference-card-left {
      flex-direction: row;

      .conference-view {
        flex: 1;
        height: auto;
      }
    }
  }

  .map-card {
    position: relative;
    top: 250px;
    left: 100px;
    z-index: 1999;
    width: 1450px;
    height: 780px;
  }
</style>
