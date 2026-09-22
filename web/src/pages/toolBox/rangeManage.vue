<script lang="ts" setup>
  import { computed, onMounted, ref } from 'vue';

  import { getAccountsByEquipmentIds } from '@/api/equipment';
  import { Dialog } from '@/components/Dialog';
  import { appConfig } from '@/config';
  import { useEmitter, useI18n, usePermissions } from '@/hooks';
  import ChooseSourceList from '@/pages/bigScreen/mapCenter/chooseSourceList.vue';
  import CreateControlTask from '@/pages/videoControl/controlTask/createControlTask.vue';
  import { mapIsReady, mapManager } from '@/plugins/map';
  import { useMapCenterStore, useMapStore, useMonitorStore } from '@/store';
  import { isOnline } from '@/utils/is';

  const props = defineProps({
    mapId: {
      default: '',
      type: String,
    },
  });

  const { t } = useI18n();
  const mapStore = useMapStore();
  const monitorStore = useMonitorStore();
  const mapCenterStore = useMapCenterStore();
  const showIconInfo = ref(false);
  const showTraffic = ref(false);
  const rangeType = ref<any[]>([]);
  const children = ref([
    {
      toolPath: 'circle_round',
      checked: true,
      val: 'circle',
      text: t('homePage.mapToolData.circle'),
    },
    {
      toolPath: 'circle_square',
      checked: false,
      val: 'rectangle',
      text: t('homePage.mapToolData.rectangle'),
    },
    {
      toolPath: 'circle_irregular',
      checked: false,
      val: 'polygon',
      text: t('homePage.mapToolData.polygon'),
    },
  ]);

  const isShow = computed(() => {
    return monitorStore.allBtnShowType === 'range';
  });

  const showTerminal = computed(() => {
    const { CAPABILITY_SWITCH } = appConfig.settingData;
    return CAPABILITY_SWITCH !== '0';
  });
  const showCar = computed(() => {
    const { CAR_BREAKER } = appConfig.settingData;
    return CAR_BREAKER === '1';
  });
  const showMonitor = computed(() => {
    const { CAPABILITY_SWITCH } = appConfig.settingData;
    let show = usePermissions('DISPATCH');
    if (CAPABILITY_SWITCH === '0') {
      show = false;
    }
    return show;
  });

  onMounted(() => {
    initShow();
    initRangeType();
    trafficVisibility();
  });

  function initRangeType() {
    rangeType.value = [
      {
        checked: false,
        hasChild: true,
        show: true,
        text: t('resource.resourcePublic.mapSelect'),
        toolPath: 'circle_resource',
        val: 4,
      },
      {
        checked: false,
        hasChild: true,
        show: usePermissions('CONTROL'),
        text: t('homePage.mapToolData.control'),
        toolPath: 'deploy_control',
        val: 5,
      },
      {
        checked: false,
        hasChild: false,
        show: true,
        text: t('homePage.mapToolData.ranging'),
        toolPath: 'range_line',
        val: 0,
      },
      {
        checked: false,
        hasChild: false,
        show: true,
        text: t('homePage.mapToolData.surfaceMeasurement'),
        toolPath: 'range_area',
        val: 1,
      },
      {
        checked: false,
        hasChild: false,
        show: usePermissions('DISPATCH') && usePermissions('eBC'),
        text: t('homePage.mapToolData.information'),
        toolPath: 'layer_filter',
        val: 2,
      },
      {
        checked: false,
        hasChild: false,
        show: usePermissions('eBC'),
        text: t('homePage.mapToolData.road'),
        toolPath: 'layer_traffic',
        val: 3,
      },
    ];
  }

  function initShow() {
    const showIcon = `${localStorage.getItem('showIconInfo')}`;
    showIconInfo.value = JSON.parse(showIcon);
    mapStore.setShowIconInfo(showIconInfo.value);

    const showTra = `${localStorage.getItem('showTraffic')}`;
    showTraffic.value = JSON.parse(showTra);
  }

  function clickTool() {
    monitorStore.setAllBtnShowType('range');
  }

  function clickType(val) {
    rangeType.value.forEach((item) => {
      item.checked = item.val === val ? !item.checked : false;
    });
    if (val < 4) {
      chooseRangeType(val);
    }
  }

  function clickTypeChildren(val, child) {
    if (val === 4) {
      // 圈选
      circleAllResource(child.val);
    } else if (val === 5) {
      // 布控
      circleTaskResource(child.val);
    }
    monitorStore.setAllBtnShowType('');
  }

  function chooseRangeType(type) {
    if (type === 2) {
      // 信息上图
      showIconInfo.value = !showIconInfo.value;
      localStorage.setItem('showIconInfo', `${showIconInfo.value}`);
      mapStore.setShowIconInfo(showIconInfo.value);
    } else if (type === 3) {
      // 路况
      showTraffic.value = !showTraffic.value;
      localStorage.setItem('showTraffic', `${showTraffic.value}`);
      trafficVisibility();
    } else {
      // 测距测面
      const mapObj = mapManager.get(props.mapId);
      mapObj.addRangeTool(type);
    }
    monitorStore.setAllBtnShowType('');
  }

  // 路况显示隐藏
  async function trafficVisibility() {
    await mapIsReady(props.mapId);
    const mapObj = mapManager.get(props.mapId);
    if (showTraffic.value) {
      mapObj.addTrafficLayer();
    } else {
      mapObj.deleteLayer(['Traffic'], false);
    }
  }

  // 圈选
  function circleAllResource(type) {
    const layerIds = [
      'monitorOnline',
      'monitorOffline',
      'GBRecorderOffline',
      'GBRecorderOnline',
      'terminalOnline',
      'terminalOffline',
      'recorderOnline',
      'recorderOffline',
      'ballCameraOnline',
      'ballCameraOffline',
      'pdtOnline',
      'pdtOffline',
      'carPhotoOnline',
      'carPhotoOffline',
      'uavOffline',
      'uavOnline',
      'seatOffline',
      'seatOnline',
    ];
    const map = mapManager.get(props.mapId);
    map.boxToSelect(type, layerIds, async (markers) => {
      if (markers.length === 0) {
        return;
      }
      const ids: string[] = [];
      const arr = markers.map((item) => {
        ids.push(item.id);
        return {
          check: true,
          ...item,
        };
      });

      const { code, data } = await getAccountsByEquipmentIds(ids);
      if (code === 0) {
        const obj: any = {};
        data.forEach((item) => {
          obj[item.id] = item.account;
        });
        arr.forEach((item) => {
          item.account = obj[item.id];
        });
      }

      mapCenterStore.addChooseSourcesList(arr);
      Dialog({
        cid: 'chooseSourceList',
        content: ChooseSourceList,
      });
    });
  }

  // 设防
  function circleTaskResource(type) {
    const layerIds = ['recorderOnline', 'recorderOffline', 'ballCameraOnline', 'ballCameraOffline'];
    if (showTerminal.value) {
      layerIds.push('terminalOnline', 'terminalOffline');
    }
    if (showMonitor.value) {
      layerIds.push('monitorOnline', 'monitorOffline');
    }
    if (showCar.value) {
      layerIds.push('policeCar');
    }
    const map = mapManager.get(props.mapId);
    map.boxToSelect(type, layerIds, (markers) => {
      const data: any[] = [];
      markers.forEach((item) => {
        if (isOnline(item)) {
          data.unshift(item);
        } else {
          data.push(item);
        }
      });

      const result = data.slice(0, 1000);
      useEmitter().emit('circleResource', result);

      Dialog({
        cid: 'createControlTask',
        content: CreateControlTask,
        data: {
          childCircleData: result,
          clickType: 'create',
        },
        offset: ['450px', '80px'],
      });
    });
  }
</script>

<template>
  <div class="range-manage-tools">
    <TdTooltip :content="t('homePage.mapToolData.range')" placement="left">
      <div class="tool-btn" :class="isShow ? 'click-tool' : ''" @click="clickTool">
        <Icon class="icon-tool" name="range_tool" />
      </div>
    </TdTooltip>

    <ElIcon v-show="isShow" class="caret-right"><CaretRight /></ElIcon>

    <div v-show="isShow" class="tools-wrapper ground-glass">
      <ul class="type-list">
        <li v-for="item in rangeType" :key="item.val" @click="clickType(item.val)">
          <div v-show="item.show" class="filter-item" :class="{ active: item.checked }">
            <Icon class="img" :name="item.toolPath" />
            <span class="text">{{ item.text }}</span>
            <Icon
              v-if="item.hasChild"
              class="right"
              :name="item.checked ? 'drop_up' : 'drop_down'"
            />
            <Icon
              v-if="item.val === 2"
              class="right"
              :name="showIconInfo ? 'check_circle' : 'uncheck_circle'"
            />
            <Icon
              v-if="item.val === 3"
              class="right"
              :name="showTraffic ? 'check_circle' : 'uncheck_circle'"
            />
          </div>

          <!-- children -->
          <div v-if="item.hasChild && item.checked" class="children">
            <div
              v-for="(child, index) in children"
              :key="index"
              class="child"
              @click.stop="clickTypeChildren(item.val, child)"
            >
              <Icon class="img" :name="child.toolPath" />
              <span class="text">{{ child.text }}</span>
            </div>
          </div>
        </li>
      </ul>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .range-manage-tools {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    margin-top: 4px;
    font-size: var(--font-size-small);
    border-radius: 50px/50px;

    .tool-btn {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      width: 32px;
      height: 32px;
      cursor: pointer;
      background: rgb(6 41 74 / 80%);
      border: 1px solid rgb(50 152 226 / 100%);
      box-shadow: inset 0 0 16px 1px rgb(14 111 179 / 100%);

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

    .caret-right {
      position: absolute;
      left: -15px;
      color: var(--button-color-normal-default);
    }

    .tools-wrapper {
      position: absolute;
      right: 68px;
      z-index: 101;
      width: 128px;

      .type-list {
        display: flex;
        flex-direction: column;
        justify-content: space-between;

        .filter-item {
          position: relative;
          display: flex;
          align-items: center;
          height: 32px;
          padding-left: 10px;
          cursor: pointer;

          .img {
            width: 16px;
            height: 16px;
            fill: var(--icon-color-default);
          }

          .text {
            padding-left: 10px;
            color: var(--icon-color-default);
          }

          .right {
            position: absolute;
            top: 12px;
            right: 10px;
            width: 12px;
            height: 12px;
          }

          &:hover {
            background: linear-gradient(
              90deg,
              rgb(41 233 194 / 80%) 0%,
              rgb(55 219 157 / 28%) 100%
            );

            .img {
              fill: var(--icon-color-click);
            }

            .text {
              color: var(--icon-color-click);
            }

            .right {
              fill: var(--icon-color-click);
            }
          }
        }

        .active {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .img {
            fill: var(--icon-color-click);
          }

          .text {
            color: var(--icon-color-click);
          }

          .right {
            fill: var(--icon-color-click);
          }
        }

        .children {
          .child {
            display: flex;
            align-items: center;
            height: 32px;
            padding-left: 30px;

            .img {
              width: 16px;
              height: 16px;
              fill: var(--icon-color-default);
            }

            .text {
              padding-left: 10px;
              color: var(--icon-color-default);
            }

            &:hover {
              background: linear-gradient(
                90deg,
                rgb(41 233 194 / 80%) 0%,
                rgb(55 219 157 / 28%) 100%
              );

              .img {
                fill: var(--icon-color-click);
              }

              .text {
                color: var(--icon-color-click);
              }
            }
          }
        }
      }
    }
  }
</style>
