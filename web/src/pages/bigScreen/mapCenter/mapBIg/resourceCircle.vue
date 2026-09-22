<script lang="ts" setup>
  import { computed, onMounted, ref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { getAccountsByEquipmentIds } from '@/api/equipment';
  import { Dialog } from '@/components/Dialog';
  import { useEmitter, useI18n } from '@/hooks';
  import chooseSourceList from '@/pages/bigScreen/mapCenter/chooseSourceList.vue';
  import { mapManager } from '@/plugins/map';
  import { useMapCenterStore, useMonitorStore } from '@/store';

  const { t } = useI18n();
  const route = useRoute();
  const monitorStore = useMonitorStore();
  const mapCenterStore = useMapCenterStore();

  const defaultIcon = ref('circle_round');
  const isCircle = ref(false);
  const showTools = ref(false);
  const chooseType = ref('circle');
  const circleType = ref([
    {
      checked: true,
      text: t('homePage.mapToolData.circle'),
      toolPath: 'circle_round',
      val: 'circle',
    },
    {
      checked: false,
      text: t('homePage.mapToolData.rectangle'),
      toolPath: 'circle_square',
      val: 'rectangle',
    },
    {
      checked: false,
      text: t('homePage.mapToolData.polygon'),
      toolPath: 'circle_irregular',
      val: 'polygon',
    },
  ]);
  const isHover = ref(false);
  const isCircled = ref(false);

  const clicksShowTool = computed(() => {
    return monitorStore.allBtnShowType === 'circleType';
  });

  watch(route, () => {
    if (route.path.includes('policeTask')) {
      return;
    }
    Dialog('chooseSourceList')?.close();
    mapCenterStore.initChooseSourcesList();
  });
  watch(isCircled, () => {
    isHover.value = false;
  });
  watch(isCircle, () => {
    isHover.value = false;
  });
  watch(showTools, (val) => {
    if (val) {
      changeOpenState();
    }
  });

  onMounted(() => {
    useEmitter('isCircleMoreMapData', isCircleMoreMapDataHandler);
  });

  function isCircleMoreMapDataHandler() {
    chooseCircleToolsType('circle');
  }

  // 默认圆形圈选工具
  function changeOpenState() {
    document.addEventListener(
      'click',
      () => {
        showTools.value = false;
        useEmitter().emit('isShowNoticeEntry', true);
      },
      true,
    );
  }

  function clickTool() {
    monitorStore.setAllBtnShowType('circleType');
  }

  function chooseCircleToolsType(type) {
    chooseType.value = type;
    if (type === 'circle') {
      defaultIcon.value = 'camera_circle_round';
    } else if (type === 'polygon') {
      defaultIcon.value = 'camera_circle_irregular';
    } else {
      defaultIcon.value = 'camera_circle_square';
    }
    showTools.value = false;
    isCircle.value = true;
    circleSearchResource(type);
    isCircle.value = false;
    isCircled.value = false;
    useEmitter().emit('isShowNoticeIcon', true);
  }

  // 圈选功能
  function circleSearchResource(type) {
    // 显示的设备
    const map = mapManager.get('mapId_main');
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
    ];
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
        content: chooseSourceList,
        offset: ['20%', '20%'],
      });
    });
  }
</script>

<template>
  <div class="resource-circle-tools">
    <TdTooltip :content="t('resource.resourcePublic.mapSelect')" placement="left">
      <div class="tool-btn" :class="clicksShowTool ? 'click-tool' : ''" @click="clickTool">
        <Icon class="icon-tool" name="circle_resource" />
      </div>
    </TdTooltip>

    <ElIcon v-show="clicksShowTool" class="caret-right">
      <CaretRight />
    </ElIcon>

    <div v-show="clicksShowTool" class="tools-wrapper ground-glass">
      <ul class="type-list">
        <li
          v-for="item in circleType"
          :key="item.val"
          class="filter-item"
          @click="chooseCircleToolsType(item.val)"
        >
          <Icon class="circle-img" :name="item.toolPath" />
          <span class="circle-text">{{ item.text }}</span>
        </li>
      </ul>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .frame-box {
    position: fixed;
    top: 75px;
    right: 75px;
    height: calc(100vh - 125px);
  }

  .frame-box-bigScreen {
    position: fixed;
    top: 75px;
    right: 75px;
    height: calc(100vh - 125px);
  }

  .resource-circle-tools {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: flex-end;
    margin-top: 4px;
    font-size: var(--font-size-small);
    border-radius: 50px/50px;

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
      width: 128px;
      height: 102px;
      overflow: hidden;

      .type-list {
        display: flex;
        flex-direction: column;
        justify-content: space-between;
        height: 102px;

        .filter-item {
          display: flex;
          align-items: center;
          height: 32px;
          padding-left: 10px;
          cursor: pointer;

          .circle-img {
            width: 16px;
            height: 16px;
            fill: var(--icon-color-default);
          }

          .circle-text {
            padding-left: 10px;
            color: var(--icon-color-default);
          }

          &:hover {
            background: linear-gradient(
              90deg,
              rgb(41 233 194 / 80%) 0%,
              rgb(55 219 157 / 28%) 100%
            );

            .circle-img {
              fill: var(--icon-color-click);
            }

            .circle-text {
              color: var(--icon-color-click);
            }
          }
        }
      }
    }
  }
</style>
