<script setup lang="ts">
  import { onActivated, onBeforeUnmount, onMounted, ref } from 'vue';
  import { useRoute } from 'vue-router';

  import { EMap, mapIsReady, mapManager } from '@/plugins/map';
  import { useMapStore, usePlanStore } from '@/store';
  import { delay } from '@/utils';
  import { addPlotLayer } from '@/utils/addLayerUtil';

  import PollingStep from './pollingStep.vue';

  const route = useRoute();
  const planStore = usePlanStore();
  const mapStore = useMapStore();

  const mapId = 'mapId_current';
  let mapObj: any = null;
  const mapReady = ref(false);

  onMounted(() => {
    EMap({
      mapId,
      onLoad: async (context) => {
        await delay(500);
        mapReady.value = true;
        mapObj = context;
        mapObj.setStyle(mapStore.getMapStyle());
        initMapData();
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

  async function initMapData() {
    await mapIsReady(mapId);
    const { plottingMap } = planStore.planData;

    mapObj.deleteAllDraw();
    if (plottingMap) {
      const plotData = JSON.parse(plottingMap);
      addPlotLayer({
        data: plotData,
        disable: false,
        handle: 'modify',
        map: mapObj,
      });
      mapObj.setFeatureLock(true);
    }
  }
</script>

<template>
  <div class="current-track-map">
    <div :id="mapId" class="map-container"> </div>
    <PollingStep v-if="route.name === 'leadVehicle'" class="steps" />
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .current-track-map {
    position: relative;
    width: 100%;
    height: 100%;

    .map-container {
      width: 100%;
      height: 100%;
    }

    .steps {
      position: absolute;
      top: 76%;
      left: 50%;
      transform: translateX(-50%);
    }
  }
</style>
