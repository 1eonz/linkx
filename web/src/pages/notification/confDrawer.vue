<script setup>
  import { useI18n } from '@/hooks';
  import VideoConference from '@/pages/videoConference/index.vue';

  const emit = defineEmits(['close']);

  const { t } = useI18n();

  function closeDrawer() {
    emit('close');
  }
</script>

<template>
  <TdFrameBox
    class="conf-drawer"
    :title="t('homePage.navigateData.videoConference')"
    @close-frame-box="closeDrawer"
  >
    <VideoConference />
  </TdFrameBox>
</template>

<style lang="less">
  .conf-drawer {
    position: fixed;
    top: 85px;
    right: 30px;
    z-index: 101;
    height: 577px;
  }
</style>
