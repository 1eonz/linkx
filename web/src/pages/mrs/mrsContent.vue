<script setup lang="ts">
  import type { RecordResListItem } from '@/plugins/mspPlayer';

  import {
    computed,
    nextTick,
    onBeforeUnmount,
    onMounted,
    reactive,
    ref,
    unref,
    watch,
    watchEffect,
  } from 'vue';

  import { mrsAudioAndVideoUrl, mrsDownload } from '@/api/evidence';
  import { Message } from '@/components/Message';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n } from '@/hooks';
  import { useCommunicationStore } from '@/store';
  import { getLocationOrigin } from '@/utils';
  import dateUtil from '@/utils/dateUtil';

  import dayjs from 'dayjs';
  import { throttle } from 'lodash-es';
  import SiriWave from 'siriwave';

  import { getCallTypeOptions } from './common';

  type Query = {
    callee: string;
    caller: string;
    callType: string;
    resourceId: string;
    time: any;
  };

  type MediaInfo = {
    currentTime: number;
    duration: number;
  };
  const props = withDefaults(
    defineProps<{
      isMax?: boolean;
      messageVideo?: boolean; // 短消息历史记录里边的视频播放
      videoItem?: any;
      videoUrl?: string;
    }>(),
    {},
  );

  const emit = defineEmits(['closeWindow', 'changeFull']);
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  let fileSrc = '';
  const dems = ref(false);
  const progress = ref(0);
  const callTypeOptions = getCallTypeOptions();
  const mediaInfo = reactive<MediaInfo>({
    currentTime: 0,
    duration: 0,
  });
  const playStatus = ref<'pause' | 'play' | 'stop'>('pause');
  const volume = ref(100);
  let siriWave: any = null;
  const showList = ref(false);
  const query = reactive<Query>({
    callee: '',
    caller: '',
    callType: '0',
    resourceId: '',
    time: '',
  });
  const playFile = ref<any>({});
  const loading = ref(false);
  const videoRef = ref();
  const audioRef = ref();
  const isMuted = ref(false);

  const fileList = computed<RecordResListItem[]>(() => {
    const arr: RecordResListItem[] = [];
    communicationStore.mrsList.data.forEach((item) => {
      callTypeOptions.forEach((type) => {
        if (item.call_type === type.value) {
          arr.push({ ...item, callType: type.label });
        }
      });
    });
    return arr;
  });
  const svgFil = computed(() => (unref(dems) ? '#000' : '#fff'));
  const fileType = computed(() => unref(fileList)[0]?.call_type);
  const isAudio = computed(() => unref(playFile).call_type === '0');

  watch(
    () => query.callType,
    () => {
      Object.assign(query, {
        callee: '',
        caller: '',
        resourceId: '',
      });
    },
  );

  watch(fileList, () => {
    showList.value = true;
  });

  watch(
    () => props.videoUrl,
    (item: any) => {
      if (!item) return;
      fileSrc = item;
    },
    { immediate: true },
  );

  watch(
    () => props.videoItem,
    (item: any) => {
      if (!item) return;
      playFile.value = item;
    },
    { deep: true, immediate: true },
  );

  watchEffect(() => {
    const { currentTime, duration } = mediaInfo;
    progress.value = duration === 0 || currentTime === 0 ? 0 : (currentTime / duration) * 100;
  });

  useEmitter('closeMrs', closePlay);

  onMounted(() => {
    const timeFormat = 'YYYY-MM-DD HH:mm:ss';
    query.time = [`${dayjs().format('YYYY-MM-DD')} 00:00:00`, dayjs().format(timeFormat)];

    initPlayerStatus();
    initSiriWave();

    registerEvent(audioRef.value);
    registerEvent(videoRef.value);

    if (fileSrc && props.messageVideo) {
      play();
    }
  });

  onBeforeUnmount(() => {
    initPlayerStatus();
  });

  // 关闭
  function closeMrs() {
    emit('closeWindow');
  }

  // 全屏
  function fullScreen() {
    emit('changeFull');
  }

  function registerEvent(media) {
    // 加载数据总时长
    media.addEventListener('loadedmetadata', () => {
      if (media.src) {
        mediaInfo.duration = media.duration;
      }
    });

    // 更新当前时间
    media.addEventListener('timeupdate', () => {
      if (media.src) {
        mediaInfo.currentTime = media.currentTime;
      }
    });

    // 播放开始
    media.addEventListener('play', () => {
      mediaInfo.duration = media.duration;
      playStatus.value = 'play';
    });

    // 播放中
    media.addEventListener('playing', () => {
      playStatus.value = 'play';
    });

    // 加载
    media.addEventListener('waiting', () => {
      // console.log('加载中');
    });

    // 暂停
    media.addEventListener('pause', () => {
      playStatus.value = 'pause';
    });

    // 结束
    media.addEventListener(
      'ended',
      () => {
        playStatus.value = 'stop';
        nextFile();
        disposeSiriWave();
      },
      false,
    );
  }

  // 拖动进度条
  function sliderChange(percent: number) {
    // 进度设置不是百分比，传多少秒
    const currentTime = mediaInfo.duration * (percent / 100);
    if (unref(isAudio)) {
      audioRef.value.currentTime = currentTime;
    } else {
      videoRef.value.currentTime = currentTime;
    }
  }

  // 快进/快退
  function fastForward(step: number) {
    if (unref(isAudio)) {
      audioRef.value.currentTime += step;
    } else {
      videoRef.value.currentTime += step;
    }
  }

  /**
   * 播放下一个文件
   * 由于播放需要后端缓存，不自动播放下一个文件减少服务器压力
   */
  function nextFile() {
    // const index = unref(fileList).findIndex((item) => item.url_rtsp === unref(playFile).url_rtsp);
    // if (index === -1 || index === unref(fileList).length - 1) return;
    // const target = unref(fileList)[index + 1];
    // if (!target) return;
    // play(target);
  }

  // 将时间长度转为 hh:mm:ss
  function transformDurationToTimeLong(duration: number) {
    const { hour, minute, second } = dateUtil.getHMSByMsec(duration * 1000);
    return `${hour}:${minute}:${second}`;
  }

  // 播放
  async function play(file?) {
    if (file) {
      // 播放前先暂定之前播放
      pause();
      playFile.value = file;
      getFileUrl(file, 'play');
    } else {
      if (unref(isAudio)) {
        audioRef.value.src = fileSrc;
      } else {
        videoRef.value.src = fileSrc;
      }
    }
  }

  // 播放按钮
  function playBtn() {
    if (!fileSrc || !unref(playFile)) {
      return;
    }
    siriWave?.start();
    playStatus.value = 'play';

    if (unref(isAudio)) {
      audioRef.value?.play();
    } else {
      videoRef.value?.play();
    }
  }

  async function getFileUrl(file, type: 'download' | 'play') {
    loading.value = true;

    const { isdn, isdnPass } = appConfig;
    const { code, data, msg } = await mrsAudioAndVideoUrl({
      password: isdnPass,
      type: file.call_type === '0' ? 'mp3' : 'mp4',
      url: file.url_https || file.attach,
      username: isdn,
    });
    if (code === 0) {
      if (data.url) {
        loading.value = false;
        const url = getLocationOrigin() + data.url;

        if (type === 'play') {
          fileSrc = url;
          showList.value = false;
          play();
        } else {
          const res = await mrsDownload(data.url);
          const link = document.createElement('a');
          const urlArr = url.split('/');
          const fileName = urlArr[urlArr.length - 1];
          const href = URL.createObjectURL(res as unknown as Blob);
          link.href = href;
          link.download = fileName;
          link.click();
        }
      } else {
        setTimeout(() => {
          getFileUrl(file, type);
        }, 500);
      }
    } else {
      loading.value = false;
      Message({
        message: msg,
        type: 'error',
      });
    }
  }

  // 暂停
  function pause() {
    if (unref(playStatus) !== 'play') {
      return;
    }
    siriWave?.stop();
    playStatus.value = 'pause';

    if (unref(isAudio)) {
      audioRef.value?.pause();
    } else {
      videoRef.value?.pause();
    }
  }

  // 初始化播放器
  function initPlayerStatus() {
    videoRef.value?.pause();
    audioRef.value?.pause();
    mediaInfo.currentTime = 0;
    mediaInfo.duration = 0;
    progress.value = 0;
    disposeSiriWave();
  }

  // 初始化音浪
  async function initSiriWave() {
    disposeSiriWave();
    await nextTick();

    const container = document.querySelector('#audio-wave') as HTMLElement;
    siriWave = new SiriWave({
      autostart: false,
      container,
      height: 300,
      style: 'ios9',
      width: 600,
    });

    siriWave.start();
  }

  function disposeSiriWave() {
    siriWave?.dispose();
    siriWave = null;
  }

  // 设置音量
  function volumeChange(vol: number) {
    if (unref(isMuted)) {
      muteHandle(false);
    }
    if (videoRef.value) {
      videoRef.value.volume = vol / 100;
    }
    if (audioRef.value) {
      audioRef.value.volume = vol / 100;
    }
  }

  // 静音
  function muteHandle(muted: boolean) {
    isMuted.value = muted;
    volume.value = muted ? 0 : 100;
    if (videoRef.value) {
      videoRef.value.muted = muted;
    }
    if (audioRef.value) {
      audioRef.value.muted = muted;
    }
  }

  // 展开视频列表
  function expandFileList(data: boolean) {
    showList.value = data;
  }

  // 下载
  async function download(file: RecordResListItem) {
    getFileUrl(file, 'download');
  }

  // 文件列表拉下加载
  const handleScroll = throttle((e) => {
    const el = e.srcElement;
    const scrollTop = el.scrollTop; // 滚动高度
    const scrollHeight = el.scrollHeight; // 内容高度
    const clientHeight = el.clientHeight; // 可见高度

    if (scrollTop + clientHeight >= scrollHeight - 1) {
      communicationStore.loadMoreMrsList();
    }
  }, 200);

  // 关闭播放
  function closePlay() {
    playStatus.value = 'stop';

    if (unref(isAudio)) {
      if (audioRef.value) {
        audioRef.value.pause();
        audioRef.value.src = '';
        audioRef.value.load();
      }
    } else {
      if (videoRef.value) {
        videoRef.value.pause();
        videoRef.value.src = '';
        videoRef.value.load();
      }
    }

    disposeSiriWave();

    fileSrc = '';
    progress.value = 0;
    playFile.value = {};
    mediaInfo.currentTime = 0;
    mediaInfo.duration = 0;
  }
</script>

<template>
  <div class="mrs-content">
    <div class="player" :class="{ 'player-msg': messageVideo }">
      <video v-show="playFile.call_type !== '0'" ref="videoRef" autoplay>
        <source type="video/mp4" />
      </video>

      <audio v-show="playFile.call_type === '0'" ref="audioRef" autoplay>
        <source type="audio/mpeg" />
      </audio>

      <div v-show="playFile.call_type === '0'" id="audio-wave" class="wave"></div>

      <Icon
        v-if="messageVideo"
        class="close-icon"
        fill="#fff"
        name="not_border_close"
        @click.stop="closeMrs()"
      />
    </div>
    <div v-if="messageVideo" class="out-line"></div>
    <div class="control ground-glass">
      <div class="progress">
        {{ transformDurationToTimeLong(mediaInfo.currentTime) }}
        /
        {{ transformDurationToTimeLong(mediaInfo.duration) }}
      </div>
      <div class="left-btn">
        <TdTooltip :content="t('mrs.operation.close')">
          <Icon :fill="svgFil" name="video_stop" @click="closePlay" />
        </TdTooltip>
        <TdTooltip :content="t('mrs.operation.fastBackward')">
          <Icon :fill="svgFil" name="fast_backward" @click="fastForward(-5)" />
        </TdTooltip>
        <TdTooltip v-if="playStatus === 'play'" :content="t('mrs.operation.pause')">
          <Icon :fill="svgFil" name="pause" @click="pause()" />
        </TdTooltip>
        <TdTooltip v-else :content="t('mrs.operation.play')">
          <Icon :fill="svgFil" name="play" @click="playBtn()" />
        </TdTooltip>
        <TdTooltip :content="t('mrs.operation.fastForward')">
          <Icon :fill="svgFil" name="fast_forward" @click="fastForward(5)" />
        </TdTooltip>
      </div>
      <div class="slider-box">
        <ElSlider
          v-model="progress"
          class="slider"
          :disabled="playStatus !== 'play'"
          height="100px"
          :show-tooltip="false"
          @input="sliderChange"
        />
      </div>
      <div class="volume-box">
        <div class="location">
          <div class="slider">
            <ElSlider
              v-model="volume"
              class="inner"
              height="100px"
              placement="bottom-end"
              vertical
              @input="volumeChange"
            />
          </div>
          <TdTooltip :content="t('mrs.operation.volume')" placement="left">
            <Icon
              v-if="isMuted || volume === 0"
              :fill="svgFil"
              name="voice_quiet"
              @click="muteHandle(false)"
            />
            <Icon v-else :fill="svgFil" name="voice_on" @click="muteHandle(true)" />
          </TdTooltip>
        </div>
      </div>
      <div v-if="messageVideo" class="full-box" @click="fullScreen()">
        <TdTooltip
          :content="isMax ? t('mrs.operation.exitFullscreen') : t('mrs.operation.fullscreen')"
        >
          <Icon
            class="full-icon"
            :fill="svgFil"
            :name="isMax ? 'icon_nofullscreen' : 'icon_fullscreen'"
            prefix="bigScreen"
          />
        </TdTooltip>
      </div>
      <TdButton
        v-else
        class="search-item query"
        :text="t('mrs.list.playBackList')"
        @click="expandFileList(true)"
      />
    </div>
    <div v-show="showList" class="file-list ground-glass">
      <div class="title">
        <div class="left">{{ t('mrs.list.playBackList') }}</div>
        <div class="right">
          <Icon name="not_border_close" @click.stop="expandFileList(false)" />
        </div>
      </div>
      <div class="header">
        <div class="item">{{ t('videoControl.createEvent.type') }}</div>
        <div v-if="fileType === '5'" class="item">{{ t('mrs.list.userNumber') }}</div>
        <div v-else-if="fileType === '3'" class="item">{{ t('mrs.list.groupNumber') }}</div>
        <div v-else-if="fileType === '6'" class="item">{{ t('mrs.list.terminalNumber') }}</div>
        <template v-else-if="fileType === '2'">
          <div class="item">{{ t('mrs.list.videoSourceNumber') }}</div>
          <div class="item">{{ t('mrs.list.receiverNumber') }}</div>
        </template>
        <template v-else>
          <div class="item">{{ t('mrs.list.callNumber') }}</div>
          <div class="item">{{ t('mrs.list.calledNumber') }}</div>
        </template>
        <div class="item">{{ t('mission.missionList.startTime') }}</div>
        <div class="item">{{ t('mission.missionList.endTime') }}</div>
        <div class="item">{{ t('alarm.btn.operation') }}</div>
      </div>
      <div v-if="fileList.length > 0" class="list" @scroll="handleScroll">
        <div
          v-for="item in fileList"
          :key="item.url_rtsp"
          class="row"
          :class="{ active: item.url_https === playFile.url_https }"
        >
          <div class="item">{{ item.callType }}</div>
          <div v-if="!['3', '5', '6'].includes(item.call_type)" class="item">{{ item.caller }}</div>
          <div class="item">{{ item.callee }}</div>
          <div class="item">{{ item.start_sec }}</div>
          <div class="item">{{ item.end_sec }}</div>
          <div class="item opt">
            <span @click="play(item)">{{ t('mrs.operation.play') }}</span>
            <span @click="download(item)">{{ t('mrs.operation.downLoad') }}</span>
          </div>
        </div>
      </div>
      <div v-else class="list">
        <TdEmpty />
      </div>
    </div>
    <div v-show="loading" class="loading">
      <img src="@/assets/images/common/earth_loading.gif" />
    </div>
  </div>
</template>

<style scoped lang="less">
  .mrs-content {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    height: calc(100% - 45px);
    background: url('@/assets/images/communicate/history_video_bg.png') no-repeat;
    background-size: 100% 100%;

    .player {
      display: flex;
      align-items: center;
      width: 100%;
      height: calc(100% - 85px);
      margin-bottom: 9px;
      border: 1px solid rgb(50 152 226 / 100%);

      video {
        width: 100%;
        height: 100%;
      }

      audio {
        width: 100%;
        height: 100%;
      }

      .wave {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 100%;
        height: 100%;
      }

      .close-icon {
        position: absolute;
        top: 10px;
        right: 10px;
        z-index: 1002;
        cursor: pointer;
      }
    }

    .player-msg {
      margin-bottom: 0;
    }

    .out-line {
      width: 100%;
      height: 4px;
      background: #525558;
      border: 1px solid rgb(50 152 226 / 100%);
      border-top: none;
      border-bottom: none;
    }

    .control {
      display: flex;
      align-items: center;
      width: 100%;
      height: 76px;
      padding: 0 24px;

      .progress {
        font-size: 12px;
        font-weight: 400;
      }

      .left-btn {
        display: flex;
        margin: 0 25px;

        .td-icon {
          width: 16px;
          height: 16px;
          margin-right: 20px;
          cursor: pointer;
        }
      }

      .slider-box {
        flex: 1;

        :deep(.el-slider) {
          .el-slider__button-wrapper {
            top: -4px;
          }
        }
      }

      .volume-box {
        position: relative;
        margin: 0 24px;

        &:hover {
          .slider {
            opacity: 1;
          }
        }

        .td-icon {
          width: 16px;
          height: 16px;
          cursor: pointer;
        }

        .slider {
          position: absolute;
          top: -150px;
          left: -7px;
          display: flex;
          justify-content: center;
          width: 30px;
          height: 150px;
          opacity: 0;
        }

        :deep(.el-slider) {
          .el-slider__button-wrapper {
            left: -4px;
          }
        }
      }

      .full-box {
        display: flex;
        align-items: center;
        justify-content: space-between;
        width: 29px;
        height: 29px;
        cursor: pointer;
        background: rgb(7 18 42 / 100%);
        border: 1px solid rgb(25.5 255 251.175 / 100%);

        .td-icon {
          margin: auto;
        }
      }

      :deep(.el-slider) {
        .el-slider__button-wrapper {
          width: 14px !important;
          height: 14px !important;
          background: rgb(67 207 124 / 100%);
          border: 2px solid rgb(255 255 255 / 100%);
          border-radius: 50%;
        }

        .el-slider__runway {
          background: rgb(67 207 124 / 20%);
        }

        .el-slider__bar {
          background: rgb(67 207 124 / 100%);
        }
      }
    }

    .file-list {
      position: absolute;
      bottom: 0;
      width: 100%;
      height: 548px;
      padding: 12px 8px;

      .title {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 32px;
        margin-bottom: 12px;
        background: url('@/assets/images/popup/title_bg.png') no-repeat;
        background-position: 0 100%;
        background-size: 115px 32px;

        .left {
          padding-left: 16px;
        }

        .right {
          .td-icon {
            width: 16px;
            height: 16px;
            cursor: pointer;
          }
        }
      }

      .header,
      .row {
        display: flex;
        align-items: center;
        justify-content: space-between;
        height: 40px;
        border-bottom: 1px solid rgb(255 255 255 / 20%);

        .item {
          flex: 1;

          &:nth-of-type(1) {
            padding-left: 24px;
          }
        }

        .opt {
          span {
            margin-right: 12px;
            cursor: pointer;
          }
        }
      }

      .header {
        background: rgb(255 255 255 / 10%);
      }

      .list {
        height: calc(100% - 88px);
        overflow: hidden auto;

        .active {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
        }

        .row:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
        }
      }
    }

    .loading {
      position: absolute;
      top: 0;
      left: 0;
      display: flex;
      align-items: center;
      justify-content: center;
      width: 100%;
      height: 100%;

      img {
        width: 160px;
        height: 160px;
      }
    }
  }
</style>
