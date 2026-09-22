<script setup lang="ts">
  import { computed, onBeforeUnmount, onDeactivated, onMounted, onUpdated, ref, watch } from 'vue';

  import SandTableDraw from '@/pages/bigScreen/mapCenter/sandTableDraw.vue';
  import { EMap, mapManager } from '@/plugins/map';
  import { useMainStore, useMapStore } from '@/store';
  import { delay } from '@/utils';

  const props = defineProps<{
    defaultForm: any;
    position: any;
  }>();

  const emit = defineEmits(['change']);
  const { setIsFull } = useMainStore();
  const mapStore = useMapStore();

  const mapId = 'mapId_plot';
  let mapObj: any = null;
  const mapReady = ref(false);
  const isFull = ref(false);

  const iconName = computed(() => {
    return isFull.value ? 'exit_full_screen' : 'full_screen';
  });

  watch(isFull, (val) => {
    setIsFull(val);
  });

  onMounted(async () => {
    await delay(500);
    EMap({
      mapId,
      onLoad: (context) => {
        mapObj = context;
        mapReady.value = true;
        mapObj.setStyle(mapStore.getMapStyle());
        setMapCenter();
      },
    });
  });

  onDeactivated(() => {
    isFull.value = false;
  });

  onUpdated(() => {
    mapObj?.resize?.();
  });

  onBeforeUnmount(() => {
    mapManager.get(mapId)?.destroyMap();
    mapManager.delete(mapId);
    mapObj = null;
  });

  function handleSave(val) {
    emit('change', val);
  }

  function setMapCenter() {
    if (props.position) {
      const position = props.position.split(',');
      mapObj.setCenter(position);
    }
  }

  async function handleFull() {
    isFull.value = !isFull.value;
    await delay(50);
    const mapObj = mapManager.get(mapId);
    mapObj?.resize?.();
  }
</script>

<template>
  <div :class="isFull ? 'plot-edit-full' : 'plot-edit'">
    <div :id="mapId" class="content-map"></div>
    <Icon class="full-btn" :name="iconName" @click="handleFull" />
    <SandTableDraw
      v-if="mapReady"
      class="draw-box"
      :draw-map-data="defaultForm"
      :map-id="mapId"
      :show-close-btn="false"
      @save="handleSave"
    />
  </div>
</template>

<style scoped lang="less">
  .plot-edit {
    position: relative;
    width: 100%;
    height: 100%;

    .content-map {
      width: 100%;
      height: 100%;
    }

    .full-btn {
      position: absolute;
      top: 10px;
      right: 20px;
      z-index: 2001;
      width: 20px;
      height: 40px;
      cursor: pointer;
    }

    .map-tool {
      z-index: 2001;
      margin-bottom: 20px;
    }
  }

  .plot-edit-full {
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

    .draw-box {
      position: fixed;
      top: 10px;
      left: 10px;
      z-index: 2001;
      background: linear-gradient(180deg, rgba(6 41 74 / 64%) 0%, rgba(6 41 74 / 26%) 100%);
      backdrop-filter: blur(8px);
      border: 1px solid transparent;
      border-image: linear-gradient(
        180deg,
        rgba(26 255 251 / 20%) 0%,
        rgba(26 255 251 / 100%) 100%
      );
      border-image-slice: 1;
    }

    .map-tool {
      position: fixed;
      right: 20px;
      bottom: 0;
      z-index: 2001;
    }
  }
</style>
