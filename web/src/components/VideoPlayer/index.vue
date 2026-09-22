<script lang="ts" setup>
  import { ref } from 'vue';

  import { useEmitter, useI18n } from '@/hooks';

  const props = defineProps({
    itemId: {
      default: '',
      type: String,
    },
    maximize: {
      default: () => {},
      type: Function,
    },
    videoUrl: {
      default: '',
      type: String,
    },
  });
  const emit = defineEmits(['closeDialog']);
  const { t } = useI18n();
  const isFull = ref(false);
  const playerRef = ref();

  useEmitter(`pauseVidoe${props.itemId}`, pauseHandler);
  useEmitter(`playVidoe${props.itemId}`, playHandler);

  function pauseHandler(data) {
    const { itemId } = props;
    if (data.id === itemId) {
      playerRef.value?.pause();
    }
  }
  function playHandler(data) {
    const { itemId } = props;
    if (data.id === itemId) {
      playerRef.value?.play();
    }
  }
  function closeWindow() {
    emit('closeDialog');
  }
  function handleMaximize() {
    props.maximize();
    isFull.value = !isFull.value;
  }
  // esc 退出全屏
  function keyEsc(event) {
    if (event.keyCode === 27) {
      handleMaximize();
    }
  }
</script>

<template>
  <div class="evidence-player" :class="{ 'langer-player': isFull }" @keyup.esc="keyEsc">
    <div class="heard dragger">
      <p class="video-heard">
        {{ t('mission.missionInformation.lawEnforcementEvidence') }}
      </p>
      <div class="right-btn">
        <TdButton
          :icon-name="isFull ? 'minimize' : 'maximize'"
          type="iconNormal"
          @click="handleMaximize"
        />
        <TdButton class="close-button" icon-name="delete" type="iconNormal" @click="closeWindow" />
      </div>
      <svg class="bg-svg">
        <use xlink:href="#icon-popup_header_bg_big" />
      </svg>
    </div>

    <!-- 视频 -->
    <div v-show="videoUrl" class="video-center">
      <video
        ref="playerRef"
        autoplay
        class="video"
        controls
        controlsList="nodownload"
        disablePictureInPicture
      >
        <source :src="videoUrl" type="video/mp4" />
      </video>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .evidence-player {
    width: 660px;
    height: 520px;

    .heard {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 34px;
      padding-left: 20px;
      background: var(--background-frame);

      .video-heard {
        z-index: 1;
        display: flex;
        align-items: center;
        font-size: 16px;
      }

      .right-btn {
        z-index: 1;
        display: flex;
        align-items: center;
        justify-content: flex-end;

        .close-button {
          :deep(.icon) {
            width: 24px;
            height: 24px;
          }
        }
      }

      .bg-svg {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
      }
    }

    .video-center {
      width: 100%;
      height: calc(100% - 40px);

      .video {
        width: 100%;
        height: 100%;
        background-color: var(--text-title-second);
      }
    }
  }

  .langer-player {
    .heard {
      .bg-svg {
        display: none;
      }
    }
  }
</style>
