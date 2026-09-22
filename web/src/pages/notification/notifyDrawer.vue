<script setup lang="ts">
  import { useI18n } from '@/hooks';

  import CallHistory from './callHistory.vue';
  import DialPlate from './dialPlate.vue';

  const emit = defineEmits(['close']);

  const { t } = useI18n();

  function closeDrawer() {
    emit('close');
  }
</script>

<template>
  <TdFrameBox
    class="notify-drawer"
    :title="t('communication.call.list')"
    @close-frame-box="closeDrawer"
  >
    <div class="notify-container">
      <CallHistory class="list" />
      <DialPlate class="dial-plate" />
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  .notify-drawer {
    position: fixed;
    top: 94px;
    right: 30px;
    z-index: 101;
    height: calc(100vh - 120px);
  }

  .notify-container {
    display: flex;
    flex-direction: column;
    height: 100%;
    padding: 10px 0;
    pointer-events: all;

    .list {
      flex: 1;
      width: 100%;
      height: 0;
    }
  }
</style>
