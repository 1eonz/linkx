<script lang="ts" setup>
  import { computed, ref } from 'vue';
  import { useRoute } from 'vue-router';

  import { usePermissions } from '@/hooks';
  import FeatureBox from '@/pages/bigScreen/components/featureBox.vue';
  import PollingStep from '@/pages/bigScreen/leadVehicle/pollingStep.vue';
  import { openMonitorCard } from '@/pages/communicationCard/openResourcesCard';
  import BottomMap from '@/pages/home/bottomMap.vue';
  import EmergencyManage from '@/pages/toolBox/emergencyManage.vue';
  import LayerManage from '@/pages/toolBox/layerManage.vue';
  import MissionManage from '@/pages/toolBox/missionManage.vue';
  import RangeManage from '@/pages/toolBox/rangeManage.vue';
  import ResourceManage from '@/pages/toolBox/resourceManage.vue';
  import SpeedManage from '@/pages/toolBox/speedManage.vue';
  import WarningManage from '@/pages/toolBox/warningManage.vue';
  import { useMonitorStore } from '@/store';

  const route = useRoute();
  const monitorStore = useMonitorStore();
  const mainMapId = ref('mapId_main');

  const isCommand = computed(() => {
    return route.path === '/mapCommand';
  });
  // const showJurisdiction = computed(() => {
  //   return usePermissions('REGION') && usePermissions('eBC');
  // });
  const showMission = computed(() => {
    return usePermissions('MISSION');
  });
  const showWarn = computed(() => {
    return usePermissions('ALARM');
  });
  // const showGuide = computed(() => usePermissions('eBC'));
  const showSpeed = computed(() => {
    return usePermissions('DISPATCH') && usePermissions('eBC');
  });

  function dragOver(e) {
    e.preventDefault();
  }

  async function receiveDropData(e) {
    dragOver(e);
    const { monitorDragData } = monitorStore;
    const dragIndex = e.dataTransfer.getData('Text');

    if (!dragIndex.replaceAll(/\s*/g, '').includes(monitorDragData.name)) return;
    if (monitorStore.currentInterface !== 'map') return;
    if (!monitorStore.idsArrOfDataOfMapMode.includes(monitorDragData.id)) {
      monitorStore.addDataOfMapMode(monitorDragData);
      openMonitorCard(monitorDragData);
    }
  }
</script>

<template>
  <div id="gisDiv" class="gis-content" @dragover="dragOver" @drop="receiveDropData">
    <!-- 视频轮巡进度条 -->
    <PollingStep v-if="route.name === 'planSpecial'" class="steps" />
    <BottomMap :map-id="mainMapId" />

    <div v-if="route.path.includes('policeTask')" class="right-top-tool">
      <!-- 资源 -->
      <ResourceManage v-if="usePermissions('DISPATCH')" />
    </div>

    <!-- 辖区需要初始化展示所以使用v-show -->
    <div v-show="['/mapCommand', '/planSpecial'].includes(route.path)" class="right-top-tool">
      <!-- 任务列表 -->
      <MissionManage v-if="showMission && isCommand" />
      <!-- 预警通知 -->
      <WarningManage v-if="showWarn && isCommand" />
      <!-- 紧急事件 -->
      <EmergencyManage v-if="isCommand" />
      <!-- 辖区管理 -->
      <!-- <JurisdictionManage v-if="showJurisdiction && isCommand" /> -->
      <!-- 功能按钮组合 -->
      <FeatureBox />
      <!-- 执法宝典 -->
      <!-- <GuideManage v-if="showGuide && isCommand" /> -->
    </div>

    <div class="right-bottom-tool">
      <!-- 图层管理 -->
      <LayerManage :map-id="mainMapId" />
      <!-- 工具箱 -->
      <RangeManage :map-id="mainMapId" />
      <!-- 速度告警 -->
      <SpeedManage v-if="showSpeed" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .gis-content {
    position: relative;
    width: 100%;
    height: 100%;
    overflow: hidden;
  }

  .right-top-tool {
    position: absolute;
    top: 80px;
    right: 24px;

    & > div:nth-of-type(1) {
      margin-top: 0;
    }
  }

  .right-bottom-tool {
    position: absolute;
    right: 20px;
    bottom: 230px;
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 56px;
  }

  .steps {
    position: fixed;
    top: 94px;
    left: 50%;
    z-index: 9;
    transform: translateX(-50%);
  }
</style>
