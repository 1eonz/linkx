<script lang="ts" setup>
  import { computed, onMounted, ref, unref } from 'vue';

  import { flowMissionDetail } from '@/api/mission';
  import { useEmitter, useI18n } from '@/hooks';
  import eventAndMissionUtil from '@/pages/mission/eventAndMissionUtil';

  const props = defineProps({
    data: {
      default: () => {},
      type: Object,
    },
    layerId: {
      default: '',
      type: String,
    },
    mapId: {
      default: '',
      type: String,
    },
  });
  const emit = defineEmits(['setDetailsText']);
  defineExpose({ details, pageNumChange });

  const { t } = useI18n();
  const missionData = ref<any>({ payload: {} });
  const loading = ref(true);

  onMounted(() => {
    emit('setDetailsText', t('mission.missionList.missionDetail'));
    getMissionData();
  });

  const eventClassColor = computed(() => {
    return eventAndMissionUtil.levelColor(unref(missionData).payload?.importantLevel);
  });

  async function getMissionData() {
    loading.value = true;
    const param = {
      id: props.data.flowId,
    };
    const { code, data } = await flowMissionDetail(param);
    if (code === 0) {
      const info = eventAndMissionUtil.evenAndMissionDataInit([data])[0];
      missionData.value = info;
      useEmitter().emit('mapMissionCardDetailsChange', info.flowId);
    }
    loading.value = false;
  }

  function details() {
    const { flowId } = props.data;
    eventAndMissionUtil.openMissionCard(flowId);
    useEmitter().emit('closeMissionDetails');
  }

  function pageNumChange() {
    getMissionData();
  }
</script>

<template>
  <div v-loading="loading" class="map-mission-popup">
    <div class="mission-title">
      <TdTag
        class="title-level"
        :label="missionData.importantLevelName || ''"
        :type="eventClassColor"
      />
      <span class="title-text">{{ missionData.payload.title }}</span>
    </div>
    <div class="mission-details">
      <span>{{ t('mission.missionList.caller') }}:</span>
      <span>{{ missionData.payload.cameraName }}</span>
    </div>
    <div class="mission-details">
      <span>{{ t('mission.taskExecution.alarmTime') }}:</span>
      <span>{{ missionData.payload.alarmTime }}</span>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .map-mission-popup {
    width: 100%;

    .mission-title {
      .title-level {
        margin-right: 4px;
      }

      .title-text {
        font-size: 14px;
        font-weight: bold;
        line-height: 20px;
        color: var(--text-title-first);
        word-break: break-all;
      }
    }

    .mission-details {
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
