<script setup lang="ts">
  import { computed, onActivated, onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import { flyToCenter } from '@/pages/map/utils';
  import { EMap, mapIsReady, mapManager } from '@/plugins/map';
  import { useMainStore, useMapStore } from '@/store';
  import { delay } from '@/utils';
  import { addPlotLayer } from '@/utils/addLayerUtil';

  type DataType = {
    plottingMap: string;
    supportPosition: string;
  };

  const props = defineProps<{
    detailData: DataType;
  }>();

  const { setIsFull } = useMainStore();
  const mapStore = useMapStore();

  const mapId = 'mapId_draw';
  let mapObj: any = null; // 地图对象
  const mapReady = ref(false);
  const isFull = ref(false);
  const plotData = ref<any>([]);

  const iconName = computed(() => {
    return isFull.value ? 'exit_full_screen' : 'full_screen';
  });

  watch(
    () => props.detailData.plottingMap,
    (val) => {
      if (val) {
        initPlotLayer();
      }
    },
  );

  watch(isFull, (val) => {
    setIsFull(val);
  });

  onMounted(() => {
    EMap({
      mapId,
      onLoad: (context) => {
        mapObj = context;
        mapReady.value = true;
        mapObj.setStyle(mapStore.getMapStyle());
        initPlotLayer();
      },
    });
  });

  onActivated(() => {
    isFull.value = false;
    mapObj?.resize?.();
  });

  onBeforeUnmount(() => {
    mapManager.get(mapId)?.destroyMap();
    mapManager.delete(mapId);
  });

  async function initPlotLayer() {
    await mapIsReady(mapId);
    mapObj?.closeBoxToSelect();
    mapObj?.deleteDrawLayer();

    handleFlyToCenter();

    const { plottingMap } = props.detailData;
    if (plottingMap) {
      plotData.value = JSON.parse(plottingMap);
      addPlotLayer({
        data: plotData.value,
        disable: false,
        handle: 'modify',
        map: mapObj,
      });
      mapObj.setFeatureLock(true);
    }
  }

  function handleFlyToCenter() {
    const { supportPosition } = props.detailData;
    if (supportPosition) {
      const center = supportPosition.split(',');
      mapObj.flyAction(center);
    } else {
      flyToCenter(mapId);
    }
  }

  async function handleFull() {
    isFull.value = !isFull.value;
    await delay(50);
    mapObj?.resize?.();
  }
</script>

<template>
  <div :class="isFull ? 'map-detail-full' : 'map-detail'">
    <div :id="mapId" class="content-map"> </div>
    <Icon class="full-btn" :name="iconName" @click="handleFull" />
  </div>
</template>

<style scoped lang="less">
  .map-detail {
    width: 100%;
    height: calc(100% - 40px);

    .content-map {
      width: 100%;
      height: 100%;
    }

    .full-btn {
      position: absolute;
      top: 70px;
      right: 40px;
      z-index: 2001;
      width: 20px;
      height: 20px;
      cursor: pointer;
    }

    .map-tool {
      z-index: 2001;
      margin-right: 20px;
      margin-bottom: 20px;
    }
  }

  .map-detail-full {
    position: fixed;
    top: 0;
    left: 0;
    z-index: 2000;
    width: 100vw;
    height: 100vh;
    background-color: var(--background-dark-map);

    .full-btn {
      position: fixed;
      top: 20px;
      right: 20px;
      z-index: 2001;
      width: 20px;
      height: 20px;
      cursor: pointer;
    }

    .content-map {
      position: fixed;
      top: 0;
      left: 0;
      z-index: 2000;
      width: 100vw;
      height: 100vh;
    }

    .map-tool {
      position: fixed;
      right: 20px;
      bottom: 0;
      z-index: 2001;
    }
  }
</style>
