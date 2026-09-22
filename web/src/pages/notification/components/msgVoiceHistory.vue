<script lang="ts" setup>
  import { ref } from 'vue';

  const props = defineProps<{
    audioTime: number | string;
    isSelf: boolean;
    selfId?: any;
    src: string;
  }>();
  const audio = ref();
  const isPlay = ref(false);

  function playVoice() {
    // 先暂停非自己本身的音频播放
    const allAudios = document.querySelectorAll('.audioPlay');
    if (allAudios && allAudios.length > 0) {
      [...(allAudios as any)].forEach((item) => {
        if (!item.paused && item.id !== props.selfId) {
          item?.pause();
        }
      });
    }
    // 判断音频是否正在播放
    if (audio.value.paused || audio.value.ended) {
      audio.value?.load();
      audio.value?.play();
      isPlay.value = true;
    } else {
      audio.value?.pause();
      isPlay.value = false;
    }

    const playTime = Math.max(Number(props.audioTime), 0);
    // 超过语音时长后取消动效
    setTimeout(() => {
      isPlay.value = false;
    }, playTime * 1000);
  }
</script>

<template>
  <div :class="isSelf ? 'voice-history' : 'voice-history-reverse'" @click="playVoice()">
    <img
      v-if="isPlay"
      alt=""
      class="voice-img"
      :src="
        isSelf
          ? '/src/assets/images/mrs/voice_effect_reverse.gif'
          : '/src/assets/images/mrs/voice_effect.gif'
      "
    />
    <Icon v-else class="voice-icon" :name="isSelf ? 'voice_icon_right' : 'voice_icon_left'" />
    <p>{{ `${audioTime}"` }}</p>
    <audio :id="selfId" ref="audio" class="audioPlay" preload="true">
      <source :src="src" type="audio/mp3" />
    </audio>
  </div>
</template>

<style lang="less" scoped>
  .voice-history {
    display: flex;
    flex-direction: row-reverse;
    width: 100px;

    .voice-img {
      width: 12px;
      height: 12px;
      margin-top: 4px;
      margin-left: 1px;
    }

    p {
      color: rgb(255 255 255);
    }

    .voice-icon {
      width: 12px;
      height: 12px;
      margin-top: 4px;
      margin-left: 1px;

      :deep(.icon) {
        width: 12px;
        height: 12px;
      }
    }
  }

  .voice-history-reverse {
    display: flex;
    width: 100px;

    .voice-img {
      width: 12px;
      height: 12px;
      margin-top: 4px;
      margin-left: 1px;
    }

    p {
      color: rgb(51 51 51);
    }

    .voice-icon {
      width: 12px;
      height: 12px;
      margin-top: 4px;
      margin-right: 3px;

      :deep(.icon) {
        width: 12px;
        height: 12px;
      }
    }
  }
</style>
