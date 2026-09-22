<script setup lang="ts">
  import { computed, onActivated, onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import simpleMarkerImg from '@/assets/images/marker/simple_marker.png';
  import { useI18n } from '@/hooks';
  import { getTimeStr } from '@/pages/alarmRecord/alarmCommon';
  import { getSupportLevel, getSupportType } from '@/pages/planSafety/common';
  import { EMap, mapIsReady, mapManager } from '@/plugins/map';
  import { useMapStore } from '@/store';

  type DataType = {
    id: string;
    supportAddress: string;
    supportBeginTime: string;
    supportDesc: string;
    supportEndTime: string;
    supportLevel: string;
    supportName: string;
    supportPosition: string;
    supportType: string;
  };

  const props = defineProps<{
    detailData: DataType;
  }>();

  const { t } = useI18n();
  const mapStore = useMapStore();
  const mapId = 'mapId_planSafetyDetails';
  let mapObj: any = null;
  const mapReady = ref(false);

  const supportTime = computed(() => {
    const { supportBeginTime, supportEndTime } = props.detailData;
    if (!supportBeginTime) return t('resource.detailType.noYet');
    const str = `${getTimeStr(Number(supportBeginTime))} ~ ${getTimeStr(Number(supportEndTime))}`;
    return str;
  });

  watch(
    () => props.detailData,
    () => {
      getMapAddress();
    },
  );

  onMounted(() => {
    EMap({
      mapId,
      onLoad: (context) => {
        mapReady.value = true;
        mapObj = context;
        mapObj.setStyle(mapStore.getMapStyle());
        getMapAddress();
      },
    });
  });

  onActivated(() => {
    mapObj?.resize?.();
  });

  onBeforeUnmount(() => {
    mapManager.get(mapId)?.destroyMap();
    mapManager.delete(mapId);
  });

  async function getMapAddress() {
    await mapIsReady(mapId);
    const map = mapManager.get(mapId);
    map.deleteMarker();
    const { supportPosition } = props.detailData;
    if (supportPosition) {
      const position = supportPosition.split(',');
      map.addAddressMarker('look', simpleMarkerImg, { position });
    }
  }
</script>

<template>
  <div class="plan-safety-detail">
    <TdTitle type="normal">{{ t('planSafety.normalInfo') }}</TdTitle>
    <div class="detail-row">
      <div class="detail-label">{{ `${t('planSafety.planTitle')}：` }}</div>
      <TdTooltip :content="detailData.supportName" placement="top">
        <div class="detail-text">
          {{ detailData.supportName || t('resource.detailType.noYet') }}
        </div>
      </TdTooltip>
    </div>
    <div class="detail-multi">
      <div class="detail-multi-left">
        <div class="detail-label">{{ `${t('planSafety.planType')}：` }}</div>
        <div class="detail-text">{{ getSupportType(detailData.supportType) }}</div>
      </div>
      <div class="detail-multi-right">
        <div class="detail-label">{{ `${t('planSafety.planLevel')}：` }}</div>
        <div class="detail-text">{{ getSupportLevel(detailData.supportLevel) }}</div>
      </div>
    </div>
    <div class="detail-row">
      <div class="detail-label">{{ `${t('planSafety.planTime')}：` }}</div>
      <div class="detail-text">{{ supportTime }}</div>
    </div>
    <div class="detail-row">
      <div class="detail-label">{{ `${t('planSafety.planAddress')}：` }}</div>
      <div class="detail-text">{{ detailData.supportAddress }}</div>
    </div>
    <div :id="mapId" class="map-container"></div>
    <TdTitle type="normal">{{ t('planSafety.planDetail') }}</TdTitle>
    <div class="detail-area">
      <TdEmpty v-if="!detailData.supportDesc" />
      {{ detailData.supportDesc }}
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .plan-safety-detail {
    width: 100%;
    height: calc(100% - 50px);

    .map-container {
      width: 728px;
      height: 178px;
      margin-bottom: 20px;
    }

    .detail-row {
      display: flex;
      width: 100%;
      margin-bottom: 20px;
    }

    .detail-multi {
      display: flex;
      width: 100%;
      margin-bottom: 20px;

      .detail-multi-left {
        display: flex;
        width: 300px;
      }

      .detail-multi-right {
        display: flex;
        width: 100%;
      }
    }

    .detail-label {
      min-width: 85px;
      max-width: 150px;
      font-size: 14px;
      font-weight: 400;
      line-height: 22px;
      color: rgb(255 255 255 / 60%);
      letter-spacing: 0;
    }

    .detail-text {
      width: 85%;
      font-size: 14px;
      font-weight: 400;
      line-height: 22px;
      color: rgb(255 255 255 / 100%);
      letter-spacing: 0;
      .ellipsis1();
    }

    .detail-area {
      width: 728px;
      height: 170px;
      padding: 0 10px 10px;
      overflow-y: auto;
    }
  }
</style>
