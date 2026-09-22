<script lang="ts" setup>
  import { useI18n } from '@/hooks';
  import { trackPlay } from '@/pages/resource/resourceHelper';

  const props = defineProps({
    alarm: Boolean,
    alarmTime: {
      default: () => [],
      type: Array,
    },
    btnType: {
      default: '',
      type: String,
    },
    infoData: {
      default: () => {},
      type: Object,
    },
    resourceType: {
      default: '',
      type: String,
    },
    showEquipmentTabs: {
      default: true,
      type: Boolean,
    },
    size: {
      default: '',
      type: String,
    },
  });
  defineExpose({ trigger: trackHistoryClick });
  const { t } = useI18n();

  async function trackHistoryClick() {
    const { alarm, alarmTime, infoData, resourceType, showEquipmentTabs } = props;

    trackPlay({
      alarm,
      infoData,
      resourceType,
      showEquipmentTabs,
      time: alarmTime as [string, string],
    });
  }
</script>

<template>
  <!-- 动向回放 -->
  <div class="track-history-btn">
    <TdTooltip :content="t('resource.policeTrackPlay.trackPlay')" placement="top">
      <TdButton
        icon-name="track_play"
        :size="size"
        :type="btnType"
        @click.stop="trackHistoryClick"
      />
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .track-history-btn {
    display: flex;
    align-items: center;
  }
</style>
