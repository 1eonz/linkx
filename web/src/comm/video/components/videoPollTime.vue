<script setup lang="ts">
  import { computed } from 'vue';

  import { useVideoPollStore } from '@/store';
  import dateUtil from '@/utils/dateUtil';

  const videoPollStore = useVideoPollStore();

  const time = computed(() => {
    const { hour, minute, second } = dateUtil.getHMSByMsec(videoPollStore.remainingTime * 1000);
    return `${hour}:${minute}:${second}`;
  });
  const isPlaying = computed(() => {
    return !videoPollStore.pausePlayPoll;
  });

  function playPoll() {
    videoPollStore.setPausePlayPoll(!videoPollStore.pausePlayPoll);
  }
</script>

<template>
  <div class="video-poll-time-control">
    <Icon
      :name="isPlaying ? 'poll_pause' : 'poll_play'"
      prefix="bigScreen"
      @click.stop="playPoll"
    />
    <span>{{ time }}</span>
  </div>
</template>

<style scoped lang="less">
  .video-poll-time-control {
    display: flex;
    align-items: center;
    height: 34px;

    .td-icon {
      width: 14px;
      height: 14px;
      margin: 0 4px 0 0;
      cursor: pointer;
    }

    span {
      line-height: 18px;
    }
  }
</style>
