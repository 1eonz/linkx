<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import { queryVideoControlWarningDetail } from '@/api/videoControl';
  import { useEmitter, useI18n } from '@/hooks';
  import { levelColor, levelText, openWarningNoticeCard } from '@/pages/videoControl/common';

  type AlarmData = {
    address: string;
    alarmLevel: string;
    alarmTime: string;
    cameraIsdn: string;
    cameraName: string;
  };

  const props = defineProps({
    data: {
      default: () => {},
      type: Object,
    },
  });
  const emit = defineEmits(['setDetailsText']);
  defineExpose({ details, pageNumChange });

  const { t } = useI18n();

  const alarmData = ref<AlarmData>({
    address: '',
    alarmLevel: '',
    alarmTime: '',
    cameraIsdn: '',
    cameraName: '',
  });
  const loading = ref(true);

  onMounted(() => {
    emit('setDetailsText', t('alarm.alarmDetails'));
    getAlarmData();
  });

  async function getAlarmData() {
    const { code, data } = await queryVideoControlWarningDetail({
      alarmId: props.data.alarmId,
    });
    if (code !== 0 || !data) {
      console.error('Failed to query warning data');
      return;
    }
    loading.value = false;
    alarmData.value = data;
    useEmitter().emit('mapAlarmCardDetailsChange', data);
  }

  function details() {
    openWarningNoticeCard(props.data.alarmId);
  }

  function pageNumChange() {
    loading.value = true;
    getAlarmData();
  }
</script>

<template>
  <div v-loading="loading" class="map-alarm-popup">
    <div class="alarm-title">
      <TdTag class="level-tag" :label="levelText(alarmData)" :type="levelColor(alarmData)" />
      <span class="title-text">{{ alarmData.address }}</span>
    </div>
    <div class="alarm-details">
      <span>{{ t('mission.missionList.caller') }}:</span>
      <span>{{ alarmData.cameraName }}</span>
      <span>{{ alarmData.cameraIsdn }}</span>
      <span>{{ alarmData.alarmTime }}</span>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .map-alarm-popup {
    width: 100%;

    .alarm-title {
      .level-tag {
        margin-right: 5px;
      }

      .title-text {
        font-size: 14px;
        font-weight: bold;
        line-height: 20px;
        color: var(--text-title-first);
        word-break: break-all;
      }
    }

    .alarm-details {
      font-size: 12px;
      line-height: 16px;
      color: var(--text-default);

      span {
        margin-right: 4px;
        color: var(--text-title-second);
      }
    }
  }

  :deep(.el-loading-spinner .circular) {
    width: 32px;
    height: 32px;
  }
</style>
