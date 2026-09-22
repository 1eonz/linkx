<script lang="ts" setup>
  import { computed, onActivated, onBeforeUnmount, onMounted, ref } from 'vue';
  import { useRoute } from 'vue-router';

  import { useEmitter, usePermissions, useUtils } from '@/hooks';
  import MapElementTool from '@/pages/map/mapElementTool.vue';
  import MapSearch from '@/pages/map/mapSearch.vue';
  import { flyToCenter } from '@/pages/map/utils';
  import { EMap, mapManager } from '@/plugins/map';
  import { usePlanStore } from '@/store';

  const props = withDefaults(
    defineProps<{
      bigScreen?: boolean;
      mapId: string;
      showTool?: boolean;
    }>(),
    {
      bigScreen: false,
      mapId: 'bottomMap',
      showTool: true,
    },
  );
  const emit = defineEmits(['mapReady']);

  const { isAdmin } = useUtils();
  const route = useRoute();
  const planStore = usePlanStore();

  const mapReady = ref(false);
  let mapObj: any = null; // 地图对象

  const showSearch = computed(() => {
    return ['/mapCenter', '/mapCommand'].includes(route.path) && usePermissions('eBC');
  });

  useEmitter('mapTypeChange', () => {
    mapObj.destroyMap();
    setTimeout(initEMap, 0);
  });

  onMounted(() => {
    initEMap();
  });

  onActivated(() => {
    // 地图被隐藏后重新显示
    const mapObj = mapManager.get(props.mapId);
    mapObj?.resize?.();
  });

  onBeforeUnmount(() => {
    const { mapId } = props;
    mapManager.get(mapId)?.destroyMap();
    mapManager.delete(mapId);
  });

  function initEMap() {
    const config = {
      mapId: props.mapId,
      onLoad: (context) => {
        mapObj = context;
        mapReady.value = true;
        emit('mapReady', context);
      },
    };
    EMap(config);
  }

  function handleFlyToCenter() {
    const { path } = route;
    const isCenter = ['/mapCenter', '/mapCommand'].includes(path) || path.includes('policeTask');
    if (isCenter || isAdmin) {
      flyToCenter(props.mapId);
    } else {
      const { supportPosition } = planStore.planData;
      const position = supportPosition?.split(',') || [];
      mapObj.flyAction(position, null, 10, 30);
    }
  }

</script>

<template>
  <div class="content">
    <div :id="mapId" class="map"></div>
    <MapSearch v-if="showSearch" :map-id="mapId" />
    <MapElementTool
      v-if="showTool && mapReady"
      :big-screen="bigScreen"
      :map-id="mapId"
      @click="handleFlyToCenter"
    />
  </div>
</template>

<style lang="less" scoped>
  .content {
    width: 100%;
    height: 100%;

    .map {
      position: relative;
      float: left;
      width: 100%;
      height: 100%;
      background-color: var(--background-dark-map);
      will-change: auto;
    }
  }
</style>
