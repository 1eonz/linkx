<script lang="ts" setup>
  import { useRoute } from 'vue-router';

  import PollingStep from '@/pages/bigScreen/leadVehicle/pollingStep.vue';
  import BottomMap from '@/pages/home/bottomMap.vue';
  import LayerManage from '@/pages/toolBox/layerManage.vue';
  import RangeManage from '@/pages/toolBox/rangeManage.vue';
  import SpeedManage from '@/pages/toolBox/speedManage.vue';

  import ResourceCircle from './resourceCircle.vue';

  const route = useRoute();

  const mainMapId = 'mapId_main';
</script>

<template>
  <div id="gisDiv" class="gis-class">
    <!-- 视频轮巡进度条 -->
    <PollingStep v-if="route.name === 'planSpecial'" class="steps" />
    <!-- 图层管理 -->
    <BottomMap :big-screen="true" :map-id="mainMapId" />
    <LayerManage :big-screen="true" :map-id="mainMapId" />
    <div class="quick-tool-box">
      <!-- 速度告警 -->
      <SpeedManage />
      <!-- 测距/测面 -->
      <RangeManage />
      <!-- 全部资源圈选 -->
      <ResourceCircle />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .gis-class {
    height: 100%;
  }

  .quick-tool-box {
    position: absolute;
    right: 16px;
    bottom: 240px;
    z-index: 9;
  }

  .steps {
    position: fixed;
    top: 94px;
    left: 50%;
    z-index: 9;
    transform: translateX(-50%);
  }
</style>
