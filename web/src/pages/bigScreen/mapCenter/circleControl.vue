<script setup lang="ts">
  import { onBeforeUnmount, onMounted, ref } from 'vue';

  import { queryCircleLayerEquipment } from '@/api/plan';
  import simpleMarkerImg from '@/assets/images/marker/simple_marker.png';
  import { appConfig } from '@/config';
  import { useI18n, useSetInterval } from '@/hooks';
  import QuickOperate from '@/pages/communicationCenter/components/quickOperate.vue';
  import CircleControl from '@/pages/map/popup/circleControl.vue';
  import { getResourceType, singleClick } from '@/pages/resource/resourceHelper';
  import CircleControlTree from '@/pages/tree/circleControl/index.vue';
  import { mapManager } from '@/plugins/map';
  import { usePlanStore } from '@/store';

  const props = defineProps<{
    position: any;
  }>();
  const emit = defineEmits(['close']);
  const planStore = usePlanStore();
  const { t } = useI18n();
  let clearTimer: any;
  const layerId = 'circle-control';
  const mapId = 'mapId_main';
  const btnSwitch = ref(true);
  const activeTab = ref(Number(appConfig.settingData.CORE_CIRCLE_RADIUS_ONE) || 1);
  const pos = ref<Number[]>([]);
  const isEmpty = ref(true);
  const isMaxUser = ref(false);
  const dataList = ref<any[]>([]);
  const tabData = ref([
    {
      id: Number(appConfig.settingData.CORE_CIRCLE_RADIUS_ONE) || 1,
      name: `${appConfig.settingData.CORE_CIRCLE_RADIUS_ONE}KM`,
    },
    {
      id: Number(appConfig.settingData.CORE_CIRCLE_RADIUS_TWO) || 2,
      name: `${appConfig.settingData.CORE_CIRCLE_RADIUS_TWO}KM`,
    },
    {
      id: Number(appConfig.settingData.CORE_CIRCLE_RADIUS_THREE) || 5,
      name: `${appConfig.settingData.CORE_CIRCLE_RADIUS_THREE}KM`,
    },
  ]);

  onMounted(() => {
    // 有点位收藏时按点位收藏的点画圈并查询数据,没有点位收藏时查询历史点位,有历史点位则画圈并查询数据
    if (props.position.length === 2) {
      handleStart('circle_control', false, true, props.position);
    } else {
      queryHistoryData(false);
    }
    clearTimer = useSetInterval(() => {
      refreshListener();
    }, 5000);
  });

  onBeforeUnmount(() => {
    clearTimer?.();
    planStore.initChooseData();
    handleStart('circle_control', true, false);
  });

  function refreshListener() {
    queryHistoryData(true);
  }

  function checkChange(checkData, cancelCheckKeys) {
    planStore.addChooseSourcesList(checkData, cancelCheckKeys);
  }

  function handleNodeClick(data) {
    const type = getResourceType(data);
    singleClick({ data, type });
  }

  function changeSwitch(val) {
    const map = mapManager.get(mapId);
    btnSwitch.value = val;
    map.setLayersVisible([layerId], val);
  }

  function monitorTabClick({ id }) {
    activeTab.value = id;
    reDrawCircleControl(true);
  }

  function close() {
    handleStart('circle_control', true, false);
    emit('close');
  }

  function reDrawCircleControl(isQuery) {
    if (pos.value.length === 2) {
      const map = mapManager.get(mapId);
      let color = '';
      if (activeTab.value === Number(appConfig.settingData.CORE_CIRCLE_RADIUS_ONE)) {
        color = '252, 167, 1';
      }
      if (activeTab.value === Number(appConfig.settingData.CORE_CIRCLE_RADIUS_TWO)) {
        color = '26, 255, 251';
      }
      if (activeTab.value === Number(appConfig.settingData.CORE_CIRCLE_RADIUS_THREE)) {
        color = '21, 154, 255';
      }
      map.deleteLayer([layerId]);
      const config = {
        center: pos.value,
        circleControlLayerId: layerId,
        color,
        handle: 'look',
        hex: false,
        highlight: true,
        layerId,
        radius: activeTab.value * 1000,
        type: 'circle',
      };
      map.draw(config);
      // 获取外接矩形点位
      if (isQuery) {
        const coordinates = map.getCoordinates(config.center, config.radius);
        queryData(pos.value, coordinates);
      }
      map.setLayersVisible([layerId], btnSwitch.value);
    }
  }

  async function queryHistoryData(isRefresh) {
    if (!isRefresh) {
      isMaxUser.value = true;
    }
    const param = {
      isRefresh: isRefresh ? 0 : 1,
      latitude: '',
      longitude: '',
      maxLatitude: '',
      maxLongitude: '',
      minLatitude: '',
      minLongitude: '',
      radius: activeTab.value * 1000,
    };
    const { code, data } = await queryCircleLayerEquipment(param);
    isEmpty.value = true;
    dataList.value = [];
    if (code === 0) {
      // 超过后端最大用户数时直接界面给出提示
      if (data === 'waiting') {
        isEmpty.value = true;
        isMaxUser.value = true;
        return;
      }
      if (data.latitude !== null && data.longitude !== null) {
        dataList.value.push(data.equipments);
        isEmpty.value = false;
        // 刷新情况下不需要重绘圆形
        if (!isRefresh) {
          handleStart('circle_control', false, false, [data.longitude, data.latitude]);
        }
      }
      isMaxUser.value = false;
    }
  }

  async function queryData(center, coordinates) {
    isMaxUser.value = true;
    const param = {
      isRefresh: 0,
      latitude: center[1],
      longitude: center[0],
      maxLatitude: coordinates.maxLatitude,
      maxLongitude: coordinates.maxLongitude,
      minLatitude: coordinates.minLatitude,
      minLongitude: coordinates.minLongitude,
      radius: activeTab.value * 1000,
    };
    const { code, data } = await queryCircleLayerEquipment(param);
    isEmpty.value = true;
    dataList.value = [];
    if (code === 0 && data.latitude !== null && data.longitude !== null) {
      if (data === 'waiting') {
        isEmpty.value = true;
        isMaxUser.value = true;
        return;
      }
      if (data.latitude !== null && data.longitude !== null) {
        dataList.value.push(data.equipments);
        isEmpty.value = false;
        isMaxUser.value = false;
      }
    }
  }

  function handleStart(type, onlyRemove, isQuery, position?) {
    const map = mapManager.get(mapId);
    map.deleteLayer([layerId]);
    map.addCircleControlMarker(type, simpleMarkerImg, {
      callback: (location) => {
        const arr = location.split(',');
        if (arr.length > 1) {
          const [lon, lat] = arr;
          pos.value = [Number(lon), Number(lat)];
          reDrawCircleControl(isQuery);
        }
      },
      draggable: false,
      onlyRemove,
      position,
      windowTemplate: CircleControl,
    });
  }
</script>

<template>
  <TdFrameBox
    v-loading="isMaxUser"
    class="circle-control"
    :element-loading-text="t('common.loading')"
    :is-circle-control="true"
    :title="t('resource.resourceTab.circleControl')"
    @change-switch="changeSwitch"
    @close-frame-box="close"
  >
    <div v-if="!isEmpty" class="circle_content">
      <TdTab class="tab" :data="tabData" :default-value="activeTab" @click="monitorTabClick" />
      <div class="circle-tree">
        <CircleControlTree
          :active="activeTab"
          :can-drag="false"
          :data="dataList"
          keywords=""
          :operation="true"
          @check-change="checkChange"
          @click="handleNodeClick"
        />
      </div>
      <div class="buttons">
        <TdButton
          :text="t('planSafety.selectControlPoint')"
          type="normal"
          @click="handleStart('circle_control', false, true)"
        />
        <QuickOperate :is-support="true" />
      </div>
    </div>
    <div v-else class="empty-circle-control">
      <div class="buttons-empty">
        <TdButton
          :text="t('planSafety.selectControlPoint')"
          type="normal"
          @click="handleStart('circle_control', false, true)"
        />
      </div>
    </div>
  </TdFrameBox>
</template>

<style scoped lang="less">
  .circle-control {
    position: fixed;
    top: 80px;
    right: 96px;
    z-index: 1210;
    width: 310px;
    height: calc(100vh - 124px);

    :deep(.frame-box-container) {
      display: flex;
      flex-direction: column;
      padding: 0;
    }

    .circle-tree {
      height: calc(100vh - 290px);
      overflow-y: scroll;
    }

    .buttons {
      padding: 10px;

      .td-button {
        width: 100%;
      }
    }

    .buttons-empty {
      padding: 10px;
      margin-top: 150px;

      .td-button {
        width: 100%;
      }
    }

    .buttons-max {
      align-items: center;
      margin-top: 50px;
    }
  }
</style>
