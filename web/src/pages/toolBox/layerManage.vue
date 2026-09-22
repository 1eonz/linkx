<script lang="ts" setup>
  import type { LayerManageOptions } from './type';

  import { computed, onMounted, ref, shallowRef, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { getDefenseLayerList } from '@/api/layer';
  import { appConfig } from '@/config';
  import { LayerIdEnum } from '@/enums';
  import { useI18n, usePermissions, useUtils } from '@/hooks';
  import LayerChooseFrameBig from '@/pages/bigScreen/mapCenter/mapBIg/layerChooseFrameBig.vue';
  import { mapManager } from '@/plugins/map';
  import { useMonitorStore, useResourceStore } from '@/store';
  import { delay } from '@/utils';

  import LayerChooseFrame from './layerChooseFrame.vue';

  const props = defineProps({
    bigScreen: {
      default: false,
      type: Boolean,
    },
    location: {
      default: () => [],
      type: Array,
    },
    mapId: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();
  const route = useRoute();
  const monitorStore = useMonitorStore();
  const resourceStore = useResourceStore();

  const buttonDisabled = ref(false);
  const layerControls = ref<LayerManageOptions[]>([]);
  const currentComponent = shallowRef<any>(LayerChooseFrame); // 默认选中的卡片

  const isShow = computed(() => {
    return monitorStore.allBtnShowType === 'resourceType';
  });

  watch(
    () => props.bigScreen,
    () => {
      getCurComp();
    },
  );

  watch(
    () => route.name,
    (val, old) => {
      if (val === 'planSpecial' || old === 'planSpecial') {
        initLayerControls();
      }
      if (usePermissions('ALARM')) return;
      layerControls.value.forEach((item) => {
        if (item.id === 'alarm') {
          item.show = useUtils().isCommand;
        }
      });
    },
  );

  // 地图选看后禁用图层
  watch(
    () => resourceStore.mapChooseData,
    async (newVal) => {
      const map = mapManager.get(props.mapId);
      if (newVal.length > 0) {
        // 等待图层数据更新再显示，不然会看到不是选看的数据
        await delay(500);

        // 选看时不受图层勾选影响
        buttonDisabled.value = true;
        if (monitorStore.allBtnShowType === 'resourceType') {
          monitorStore.setAllBtnShowType('');
        }
        map.setLayersVisible(
          [
            'recorderOnline',
            'recorderOffline',
            'terminalOnline',
            'terminalOffline',
            'monitorOnline',
            'monitorOffline',
            'pdtOnline',
            'pdtOffline',
            'carPhotoOnline',
            'carPhotoOffline',
            'GBRecorderOnline',
            'GBRecorderOffline',
            'uavOnline',
            'uavOffline',
          ],
          true,
        );
        map.setLayersVisible(
          [
            'personOnline',
            'personOffline',
            'personFree',
            'personBusy',
            'personDimission',
            'policeCar',
            'orderWaiting',
            'orderGoing',
            'orderCompleted',
            'taskWaiting',
            'taskGoing',
            'taskCompleted',
            'alarmWaiting',
            'alarmCompleted',
            'message',
            'custom',
          ],
          false,
        );
        map.showMarkerByFlag(false);
      } else {
        // 取消选看时恢复默认图层显示
        buttonDisabled.value = false;
        getCurComp();
        initLayerControls();
        map.setLayersVisible(['recorderOnline', 'monitorOnline'], true);
        map.setLayersVisible(
          [
            'personOnline',
            'personOffline',
            'personFree',
            'personBusy',
            'personDimission',
            'policeCar',
            'orderWaiting',
            'orderGoing',
            'taskWaiting',
            'taskGoing',
            'alarmWaiting',
            'orderCompleted',
            'taskCompleted',
            'alarmCompleted',
            'recorderOffline',
            'terminalOnline',
            'terminalOffline',
            'monitorOffline',
            'pdtOnline',
            'pdtOffline',
            'carPhotoOnline',
            'carPhotoOffline',
            'GBRecorderOnline',
            'GBRecorderOffline',
            'uavOnline',
            'uavOffline',
            'seatOnline',
            'seatOffline',
            'message',
            'custom',
          ],
          false,
        );
        map.showMarkerByFlag(true);
      }
    },
  );

  onMounted(() => {
    getCurComp();
    initLayerControls();
  });

  function getCurComp() {
    currentComponent.value = props.bigScreen ? LayerChooseFrameBig : LayerChooseFrame;
  }

  function initLayerControls() {
    const show = usePermissions('eBC');
    const { CAR_BREAKER, LAYER_DISPLAY } = appConfig.settingData;
    const { isCommand, isSpecial } = useUtils();
    const checked = (id) => {
      const val = LAYER_DISPLAY.toLowerCase();
      return val.includes(id.toLowerCase());
    };
    const controls = [
      {
        checked: checked('region'),
        icon: 'region',
        id: 'region',
        name: t('homePage.mapToolData.region'),
        show: show && usePermissions('DISPATCH') && !isSpecial,
        children: [],
      },
      {
        checked: checked('sandbox'),
        icon: 'sandbox',
        id: 'sandbox',
        name: t('resource.resourceTab.sandboxDrawing'),
        show: show && usePermissions('SAFETY'),
        children: [],
      },
      {
        checked: false,
        icon: 'coverage_person',
        id: 'police',
        name: t('homePage.mapToolData.police'),
        show: true,
        children: [
          {
            name: t('resource.fillColor.online'),
            id: LayerIdEnum.personOnline,
            checked: checked('police'),
            icon: 'online',
          },
          {
            name: t('resource.fillColor.offline'),
            id: LayerIdEnum.personOffline,
            checked: false,
            icon: 'offline',
          },
          {
            name: t('resource.fillColor.idle'),
            id: LayerIdEnum.personFree,
            icon: 'police_free',
            checked: false,
          },
          {
            name: t('resource.fillColor.busy'),
            id: LayerIdEnum.personBusy,
            icon: 'police_busy',
            checked: false,
          },
          {
            name: t('resource.fillColor.leave'),
            id: LayerIdEnum.personDimission,
            icon: 'police_dimission',
            checked: false,
          },
        ],
      },
      {
        checked: checked('policeCar'),
        icon: 'police_car',
        id: 'policeCar',
        name: t('homePage.mapToolData.policeCar'),
        show: CAR_BREAKER === '1',
      },
      {
        checked: true,
        icon: 'home_warning',
        id: 'order',
        name: t('homePage.navigateData.order'),
        show: false,
        children: [
          {
            name: t('mission.missionList.pending'),
            id: 'orderWaiting',
            checked: true,
            icon: 'busy',
          },
          {
            name: t('mission.missionList.processing'),
            id: 'orderGoing',
            checked: true,
            icon: 'underway',
          },
          {
            name: t('mission.missionList.completed'),
            id: 'orderCompleted',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_task',
        id: 'task',
        name: t('homePage.mapToolData.task'),
        show: usePermissions('MISSION') && !isSpecial,
        children: [
          {
            name: t('mission.missionList.pending'),
            id: 'taskWaiting',
            checked: checked('task'),
            icon: 'busy',
          },
          {
            name: t('mission.missionList.processing'),
            id: 'taskGoing',
            checked: false,
            icon: 'underway',
          },
          {
            name: t('mission.missionList.completed'),
            id: 'taskCompleted',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_alarm',
        id: 'alarm',
        name: t('homePage.mapToolData.alarm'),
        show: (usePermissions('ALARM') || isCommand) && !isSpecial,
        children: [
          {
            name: t('videoControl.warningInformation.untreated'),
            id: 'alarmWaiting',
            checked: checked('warn'),
            icon: 'busy',
            show,
          },
          {
            name: t('videoControl.warningInformation.treated'),
            id: 'alarmCompleted',
            checked: false,
            icon: 'offline',
            show,
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_recorder',
        id: 'recorder',
        name: t('homePage.mapToolData.recorder'),
        show: true,
        children: [
          {
            name: t('resource.fillColor.online'),
            id: 'recorderOnline',
            checked: checked('recorder'),
            icon: 'online',
          },
          {
            name: t('resource.fillColor.offline'),
            id: 'recorderOffline',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_phone',
        id: 'terminal',
        name: t('homePage.mapToolData.terminal'),
        show: true,
        children: [
          {
            name: t('resource.fillColor.online'),
            id: 'terminalOnline',
            checked: checked('terminal'),
            icon: 'online',
          },
          {
            name: t('resource.fillColor.offline'),
            id: 'terminalOffline',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_monitor',
        id: 'monitor',
        name: t('homePage.mapToolData.monitor'),
        show: true,
        children: [
          {
            name: t('resource.fillColor.online'),
            id: 'monitorOnline',
            checked: checked('camera'),
            icon: 'online',
          },
          {
            name: t('resource.fillColor.offline'),
            id: 'monitorOffline',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_ballCamera',
        id: 'ballCamera',
        name: t('homePage.mapToolData.ballCamera'),
        show: true,
        children: [
          {
            name: t('resource.fillColor.online'),
            id: 'ballCameraOnline',
            checked: checked('ballCamera'),
            icon: 'online',
          },
          {
            name: t('resource.fillColor.offline'),
            id: 'ballCameraOffline',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_pdt',
        id: 'pdt',
        name: t('homePage.mapToolData.pdt'),
        show,
        children: [
          {
            name: t('resource.fillColor.online'),
            id: 'pdtOnline',
            checked: checked('pdt'),
            icon: 'online',
          },
          {
            name: t('resource.fillColor.offline'),
            id: 'pdtOffline',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_picture',
        id: 'carPhoto',
        name: t('homePage.mapToolData.picture'),
        show,
        children: [
          {
            name: t('resource.fillColor.online'),
            id: 'carPhotoOnline',
            checked: checked('vehicle'),
            icon: 'online',
          },
          {
            name: t('resource.fillColor.offline'),
            id: 'carPhotoOffline',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_GBRec',
        id: 'GBRecorder',
        name: t('homePage.mapToolData.GBRecorder'),
        show,
        children: [
          {
            name: t('resource.fillColor.online'),
            id: 'GBRecorderOnline',
            checked: checked('gbbwc'),
            icon: 'online',
          },
          {
            name: t('resource.fillColor.offline'),
            id: 'GBRecorderOffline',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_uav',
        id: 'uav',
        name: t('homePage.mapToolData.uav'),
        show,
        children: [
          {
            name: t('resource.fillColor.online'),
            id: 'uavOnline',
            checked: checked('uav'),
            icon: 'online',
          },
          {
            name: t('resource.fillColor.offline'),
            id: 'uavOffline',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: false,
        icon: 'coverage_seat',
        id: 'seat',
        name: t('homePage.mapToolData.seat'),
        show: true,
        children: [
          {
            name: t('resource.fillColor.online'),
            id: 'seatOnline',
            checked: checked('seat'),
            icon: 'online',
          },
          {
            name: t('resource.fillColor.offline'),
            id: 'seatOffline',
            checked: false,
            icon: 'offline',
          },
        ],
      },
      {
        checked: checked('sms'),
        icon: 'coverage_messages',
        id: 'message',
        name: t('homePage.menus.emergencyMessages'),
        show: !isSpecial,
      },
      {
        checked: checked('custom'),
        icon: 'coverage_diy',
        id: 'custom',
        name: t('homePage.mapToolData.custom'),
        show: show && usePermissions('DISPATCH') && !isSpecial,
        children: [],
      },
      {
        checked: checked('defense'),
        icon: 'coverage_defense',
        id: 'defense',
        name: t('homePage.mapToolData.defense'),
        show: show && usePermissions('DISPATCH') && !isSpecial,
        children: [],
      },
    ];
    layerControls.value = controls;
    // 获取三道防线图层
    getDefenseLayers();
    // 获取自定义图层
    getCustomLayers();
  }

  async function getDefenseLayers() {
    const { code, data } = await getDefenseLayerList({ type: 1 });
    if (code === 0) {
      const defenseLayer = layerControls.value.find((item) => item.id === 'defense');
      data.forEach((element) => {
        defenseLayer?.children?.push({
          checked: false,
          icon: 'online',
          id: `defense_${element.id}`,
          name: element.customLayerName,
        });
      });
    }
  }

  async function getCustomLayers() {
    const { code, data } = await getDefenseLayerList({ type: 2 });
    if (code === 0) {
      const customLayer = layerControls.value.find((item) => item.id === 'custom');
      data.forEach((element) => {
        customLayer?.children?.push({
          checked: false,
          icon: 'online',
          id: `custom_${element.id}`,
          name: element.customLayerName,
        });
      });
    }
  }

  function closeWindow() {
    monitorStore.setAllBtnShowType('');
  }

  function clickShow() {
    if (buttonDisabled.value) {
      return;
    }
    monitorStore.setAllBtnShowType('resourceType');
  }
</script>

<template>
  <div class="layer-control-tools">
    <TdTooltip v-if="!bigScreen" :content="t('homePage.mapToolData.mapLayer')" placement="left">
      <div
        :id="`resource${mapId}`"
        class="tool-btn resource"
        :class="{
          'click-tool': isShow,
          'disabled-style': buttonDisabled,
        }"
        @click="clickShow"
      >
        <Icon class="icon-tool" name="layer_filter" />
      </div>
    </TdTooltip>

    <ElIcon v-if="!bigScreen" v-show="isShow" class="caret-right"><CaretRight /></ElIcon>

    <component
      :is="currentComponent"
      v-show="isShow || bigScreen"
      :data-source="layerControls"
      :map-id="mapId"
      @close-window="closeWindow"
    />
  </div>
</template>

<style lang="less" scoped>
  .layer-control-tools {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    margin-top: 4px;

    .tool-btn {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      cursor: pointer;
      background: rgb(6 41 74 / 80%);
      border: 1px solid rgb(50 152 226 / 100%);
      box-shadow: inset 0 0 16px 1px rgb(14 111 179 / 100%);

      & > span {
        font-size: var(--font-size-default);
        color: var(--icon-color-default);
      }

      &:last-child {
        margin-bottom: 0;
      }

      .icon-tool {
        width: 20px;
        height: 20px;
        fill: var(--icon-color-default);
      }

      &:hover {
        .icon-tool {
          fill: #fff;
        }
      }
    }

    .click-tool {
      background: var(--button-color-special-active);
      border: none;
      box-shadow: none;

      .icon-tool {
        fill: var(--icon-color-click) !important;
      }
    }

    .disabled-style {
      cursor: not-allowed !important;
    }

    .caret-right {
      position: absolute;
      left: -15px;
      color: var(--button-color-normal-default);
    }
  }
</style>
