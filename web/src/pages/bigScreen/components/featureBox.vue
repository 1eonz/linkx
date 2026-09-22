<script lang="ts" setup>
  import { onMounted, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { updatePlanGroup } from '@/api/plan';
  import { insertSandTableMarkers, querySandTableMarkers } from '@/api/sandTable';
  import { Message } from '@/components/Message';
  import { useEmitter, useI18n, usePermissions, useUtils } from '@/hooks';
  import CircleControl from '@/pages/bigScreen/mapCenter/circleControl.vue';
  import SandTableDraw from '@/pages/bigScreen/mapCenter/sandTableDraw.vue';
  import VideoPolling from '@/pages/bigScreen/mapCenter/videoPolling.vue';
  import SupportGroupManage from '@/pages/bigScreen/planSpecial/supportGroupManage.vue';
  import SupportOneKeyManage from '@/pages/bigScreen/planSpecial/supportOneKeyManage.vue';
  import AddressManage from '@/pages/toolBox/addressManage.vue';
  import LandmarkManage from '@/pages/toolBox/landmarkManage.vue';
  import VehicleIllusion from '@/pages/toolBox/vehicleIllusion.vue';
  import { mapIsReady, mapManager } from '@/plugins/map';
  import { useMonitorStore, usePlanStore, useResourceStore } from '@/store';
  import { delay } from '@/utils';
  import { addPlotLayer } from '@/utils/addLayerUtil';

  const { t } = useI18n();
  const { isMapPanel } = useUtils();
  const route = useRoute();
  const planStore = usePlanStore();
  const monitorStore = useMonitorStore();
  const resourceStore = useResourceStore();

  const mapId = 'mapId_main';
  const tabOptions = ref<any>([]);
  const activeTab = ref(0);
  const activePosition = ref<any[]>([]);
  const drawMapData = ref<any>('');

  watch(
    () => monitorStore.allBtnShowType,
    (val) => {
      if (val) {
        activeTab.value = 0;
      }
    },
    { deep: true, immediate: true },
  );

  watch(
    () => route.name,
    () => {
      activeTab.value = 0;
      initTabOptions();
      if (isMapPanel) {
        queryDrawMapData();
      }
    },
    { deep: true, immediate: true },
  );

  watch(
    () => activeTab.value,
    async (val) => {
      await mapIsReady(mapId);
      await delay(500);
      const mapObj = mapManager.get(mapId);
      if (!drawMapData.value) return;
      const ids = JSON.parse(drawMapData.value).map((i) => i.id);
      mapObj.setFeatureLock(val !== 3, ids);
    },
    { deep: true, immediate: true },
  );

  watch(
    () => resourceStore.layerChecked,
    (newVal, oldVal) => {
      const newCheck = newVal.includes('sandbox');
      const oldCheck = oldVal.includes('sandbox');
      if (newCheck !== oldCheck) {
        queryDrawMapData();
      }
    },
  );

  useEmitter('pollStart', tabListener);

  useEmitter('mapTypeChange', (mapType) => {
    setTimeout(queryDrawMapData, mapType === 'MineMap' ? 3000 : 1500);
  });

  onMounted(() => {
    initTabOptions();
  });

  function tabListener() {
    activeTab.value = 0;
  }

  function initTabOptions() {
    const { isSpecial } = useUtils();
    const arr = [
      {
        active: 'map_feature6_active',
        id: 1,
        name: 'map_feature6',
        show: usePermissions('eBC'),
        title: t('resource.resourceTab.videoPoll'),
      },
      {
        active: 'map_feature7_active',
        id: 2,
        name: 'map_feature7',
        show: usePermissions('SAFETY') && usePermissions('eBC'),
        title: t('resource.resourceTab.circleControl'),
      },
      {
        active: 'map_feature8_active',
        id: 3,
        name: 'map_feature8',
        show: usePermissions('SAFETY') && usePermissions('eBC'),
        title: t('resource.resourceTab.sandboxDrawing'),
      },
      {
        active: 'map_feature11_active',
        id: 4,
        name: 'map_feature11',
        show: usePermissions('eBC'),
        title: t('resource.resourceTab.pointCollection'),
      },
      {
        active: 'map_feature12_active',
        id: 5,
        name: 'map_feature12',
        show: usePermissions('SAFETY') && usePermissions('eBC'),
        title: t('resource.resourceTab.vehicle'),
      },
      {
        active: 'map_feature2_active',
        id: 6,
        name: 'map_feature2',
        show: isSpecial,
        title: t('planSafety.detailTabs.group'),
      },
      {
        active: 'map_feature3_active',
        id: 7,
        name: 'map_feature3',
        show: isSpecial && usePermissions('CONFERENCE'),
        title: t('resource.resourceTab.fastMeeting'),
      },
      {
        active: 'map_feature13_active',
        id: 8,
        name: 'map_feature13',
        show: isSpecial,
        title: t('resource.resourceTab.fastNotification'),
      },
      {
        active: 'map_feature17_active',
        id: 9,
        name: 'map_feature17',
        show: usePermissions('eBC'),
        title: t('homePage.mapToolData.landmark'),
      },
    ];
    tabOptions.value = arr.filter((i) => i.show);
  }

  function tabClick(id) {
    if (activeTab.value === id) {
      activeTab.value = 0;
    } else {
      activeTab.value = id;
      monitorStore.setAllBtnShowType('');
    }
  }

  // 绘制图上标绘
  async function queryDrawMapData() {
    await mapIsReady(mapId);
    const show = usePermissions('SAFETY') && usePermissions('eBC');
    if (!show) {
      return;
    }
    const mapObj = mapManager.get(mapId);
    mapObj.deleteDrawLayer();

    drawMapData.value = '';
    let arrData: any = [];
    const check = resourceStore.layerChecked.includes('sandbox');
    if (!check) {
      return;
    }
    if (useUtils().isSpecial) {
      await delay(500);
      const { plottingMap } = planStore.planData;
      if (plottingMap) {
        arrData = JSON.parse(plottingMap);
        drawMapData.value = plottingMap;
      }
    } else {
      const { code, data } = await querySandTableMarkers();
      if (code === 0 && data) {
        arrData = JSON.parse(data);
        drawMapData.value = data;
      }
    }

    addPlotLayer({
      data: arrData,
      disable: false,
      handle: 'look',
      map: mapObj,
    });
  }

  function showTab(id) {
    return activeTab.value === id;
  }

  function handleClose(position?) {
    if (position && position.length === 2) {
      activeTab.value = 2;
      activePosition.value = position;
    } else {
      if (position === 3) {
        queryDrawMapData();
      }
      activePosition.value = [];
      activeTab.value = 0;
    }
  }

  async function handleSave(val) {
    if (useUtils().isSpecial) {
      const params = planStore.planData;
      params.plottingMap = val;
      const { code } = await updatePlanGroup(params);
      if (code === 0) {
        Message(t('sandTableDraw.sandTableDrawTip.editSuccess'));
      }
    } else {
      const { code } = await insertSandTableMarkers(val);
      if (code === 0) {
        Message(t('sandTableDraw.sandTableDrawTip.saveTip'));
      }
    }
  }
</script>

<template>
  <!-- 功能按钮 -->
  <div class="feature-box">
    <div class="tabs">
      <TdTooltip v-for="item in tabOptions" :key="item.id" :content="item.title" placement="left">
        <div class="item" :class="{ active: activeTab === item.id }" @click="tabClick(item.id)">
          <Icon :name="activeTab === item.id ? item.active : item.name" prefix="bigScreen" />
        </div>
      </TdTooltip>
    </div>

    <!-- 视频轮巡 -->
    <VideoPolling v-if="showTab(1)" @close="handleClose" />

    <!-- 圈层防控 -->
    <CircleControl v-if="showTab(2)" :position="activePosition" @close="handleClose" />
    <!-- 沙盘绘制 -->
    <SandTableDraw
      v-if="showTab(3)"
      :draw-map-data="drawMapData"
      map-id="mapId_main"
      @close="handleClose"
      @save="handleSave"
    />
    <!-- 点位收藏 -->
    <AddressManage v-if="showTab(4)" @close="handleClose" />
    <!-- 保障组 -->
    <SupportGroupManage v-if="showTab(6)" @close="handleClose" />
    <!-- 一键会议 -->
    <SupportOneKeyManage v-if="showTab(7)" :tab="activeTab" @close="handleClose" />
    <!-- 一键信息 -->
    <SupportOneKeyManage v-if="showTab(8)" :tab="activeTab" @close="handleClose" />
    <!-- 车辆幻化 -->
    <VehicleIllusion v-if="showTab(5)" @close="handleClose" />
    <!-- 地标单位 -->
    <LandmarkManage v-if="showTab(9)" @close="handleClose" />
  </div>
</template>

<style lang="less" scoped>
  .feature-box {
    position: relative;
  }

  .tabs {
    display: flex;
    flex-direction: column;

    .item {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 40px;
      height: 40px;
      margin: 2px 4px;
      cursor: pointer;
      background: url('@/assets/images/map/map_feature_btn.png') no-repeat;
      background-size: 100% 100%;

      :deep(.td-icon) {
        z-index: 1;
        width: 20px;
        height: 20px;
      }

      &.active {
        position: relative;
        background: url('@/assets/images/map/map_feature_btn_active.png') no-repeat;
        background-size: 100% 100%;

        &::after {
          position: absolute;
          width: 100%;
          height: 100%;
          content: '';
          background: url('@/assets/images/map/map_feature_btn_active.gif') no-repeat;
          background-size: 100% 100%;
        }
      }
    }
  }
</style>
