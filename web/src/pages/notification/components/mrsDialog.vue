<script lang="ts" setup>
  import { onBeforeUnmount, onMounted, ref } from 'vue';

  import MrsContent from '@/pages/mrs/mrsContent.vue';

  defineProps<{
    videoItem: any;
    videoUrl: string;
  }>();

  defineExpose({ clickVideo, closeWindow });

  const showVideo = ref(false);
  const isMax = ref(false);

  onMounted(() => {
    window.addEventListener('keydown', handleKeydownCode);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', handleKeydownCode);
  });

  function handleKeydownCode(e) {
    if (e.keyCode === 27) {
      changeFull();
    }
  }
  function clickVideo() {
    showVideo.value = true;
  }
  function closeWindow() {
    showVideo.value = false;
  }
  function changeFull() {
    isMax.value = !isMax.value;
  }
</script>

<template>
  <teleport :disabled="!isMax" to="body">
    <div v-if="showVideo" class="video-dialog" :class="{ 'max-card': isMax }">
      <MrsContent
        :is-max="isMax"
        :message-video="true"
        :video-item="videoItem"
        :video-url="videoUrl"
        @change-full="changeFull"
        @close-window="closeWindow"
      />
    </div>
  </teleport>
</template>

<style lang="less" scoped>
  .video-dialog {
    position: absolute;
    top: 0;
    left: 320px;
    z-index: 1001;
    width: 700px;
    height: 455px;

    .mrs-content {
      height: 100% !important;

      .control {
        height: 50px !important;
      }
    }
  }

  .max-card {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 2000;
    width: 100vw;
    height: 100vh;
    margin: 0;
    background: black;
  }
  // :deep(.frame-box) {
  //   width: 754px !important;
  //   height: 424px !important;
  // }
</style>
