<script lang="ts" setup>
  import { Dialog } from '@/components/Dialog';
  import { useI18n } from '@/hooks';
  import RealTimeSet from '@/pages/trackReplay/realTimeSet.vue';

  const props = defineProps({
    btnType: {
      default: '',
      type: String,
    },
    infoData: {
      default: () => {},
      type: Object,
    },
    size: {
      default: '',
      type: String,
    },
  });
  defineExpose({ trigger: trackRealTimeClick });
  const { t } = useI18n();

  async function trackRealTimeClick() {
    Dialog({
      cid: 'set_track_realtime',
      content: RealTimeSet,
      data: {
        infoData: props.infoData,
      },
    });
  }
</script>

<template>
  <!-- 轨迹跟踪 -->
  <div class="track-realtime-btn">
    <TdTooltip :content="t('resource.policeTrackPlay.trackRealTime')" placement="top">
      <TdButton
        icon-name="track_realtime"
        :size="size"
        :type="btnType"
        @click.stop="trackRealTimeClick"
      />
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .track-realtime-btn {
    display: flex;
    align-items: center;
  }
</style>
