<script lang="ts" setup>
  import type { LayerManageOptions } from './type';

  import type { PropType } from 'vue';
  import { ref, watch } from 'vue';

  import { LayerIdEnum } from '@/enums';
  import { useI18n, usePermissions } from '@/hooks';
  import { mapIsReady, mapManager } from '@/plugins/map';
  import { useResourceStore } from '@/store';

  import { throttle } from 'lodash-es';

  const props = defineProps({
    bigScreen: {
      default: false,
      type: Boolean,
    },
    dataSource: {
      default: () => [],
      type: Array as PropType<LayerManageOptions[]>,
    },
    mapId: {
      default: '',
      type: String,
    },
  });

  const emit = defineEmits(['closeWindow']);
  const { t } = useI18n();
  const { setLayerChecked, setPoliceLayerChecked } = useResourceStore();

  const allChecked = ref(false);
  const layerOptions = ref<LayerManageOptions[]>([]);
  const checkedAllStatus = ref(false);
  watch(
    layerOptions,
    (val) => {
      let nodeNum = 0;
      let checkedNum = 0;
      val.forEach((item) => {
        if (item.show === false) return;
        nodeNum++;
        item.checked && checkedNum++;
        item.children?.forEach((child) => {
          if (child.show === false) return;
          nodeNum++;
          child.checked && checkedNum++;
        });
      });
      allChecked.value = !!(checkedNum && checkedNum === nodeNum);

      checkedAllStatus.value = checkedNum > 0 && checkedNum < nodeNum;
    },
    { deep: true },
  );

  watch(
    () => props.dataSource,
    (val) => {
      layerOptions.value = val;
      setVisible();
    },
    { deep: true, immediate: true },
  );

  const handleCheckAll = throttle(checkAll, 500);
  const handleCheckItem = throttle(checkItem, 500);

  // 全选
  function checkAll(checked: boolean) {
    layerOptions.value.forEach((item) => {
      item.checked = checked;
      item.children?.forEach((child) => {
        child.checked = checked;
      });
    });
    setVisible();
  }

  function checkItem(item, parent?) {
    const { checked, children } = item;
    const currChecked = checked;

    item.checked = currChecked;
    children?.forEach((child) => {
      child.checked = currChecked;
    });

    setVisible();

    if (!parent) return;
    const allChecked = parent.children.every((child) => {
      return child.checked;
    });
    parent.checked = allChecked;
  }

  function checkedStatus(item) {
    const { children } = item;
    if (!children) return false;
    const checkedNum = children.filter((child) => child.checked);
    const checkedLen = checkedNum.length;
    return checkedLen > 0 && checkedLen < children.length;
  }

  function getChecked() {
    const checked: string[] = [];
    const unCheck: string[] = [];
    layerOptions.value.forEach((item) => {
      if (item.checked && item.show) {
        checked.push(item.id);
      } else {
        unCheck.push(item.id);
      }

      item.children?.forEach((child) => {
        if (child.checked && item.show) {
          checked.push(child.id);
        } else {
          unCheck.push(child.id);
        }
      });
    });
    return {
      checked: [...new Set(checked)],
      unCheck: [...new Set(unCheck)],
    };
  }

  // 图层显示隐藏
  async function setVisible() {
    let { checked, unCheck } = getChecked();
    // 在线 离线 | 空闲 忙碌 离岗
    let hasOnline = false;
    let hasDuty = false;
    const online: string[] = [LayerIdEnum.personOnline, LayerIdEnum.personOffline];
    const duty: Set<LayerIdEnum> = new Set([
      LayerIdEnum.personBusy,
      LayerIdEnum.personDimission,
      LayerIdEnum.personFree,
    ]);
    const policeChecked: string[] = [];
    checked.forEach((item) => {
      if (online.includes(item)) {
        hasOnline = true;
      }
      if (duty.has(item as LayerIdEnum)) {
        hasDuty = true;
      }
      if (item.includes('person')) {
        policeChecked.push(item);
      }
    });
    if (hasDuty && !hasOnline) {
      checked.push(...online);
      policeChecked.push(...online);
      unCheck = unCheck.filter((item) => !online.includes(item));
    }

    setPoliceLayerChecked(policeChecked);
    setLayerChecked(checked);

    await mapIsReady(props.mapId);
    const map = mapManager.get(props.mapId);
    map.setLayersVisible(checked, true);
    map.setLayersVisible(unCheck, false);
  }

  function closeFilterBox() {
    emit('closeWindow');
  }
</script>

<template>
  <TdFrameBox
    class="layer-choose-frame"
    :class="{ 'short-layer-frame': !usePermissions('eBC') }"
    :title="t('homePage.mapToolData.yalesShow')"
    @close-frame-box="closeFilterBox"
  >
    <div class="layer-choose-box">
      <div class="filter-item all-choose">
        <div class="filter-type">
          <Icon class="icon-type" name="coverage_all_select" />
          <label class="label-type">
            {{ t('common.tdcomp.allChoose') }}
          </label>
        </div>
        <TdCheckbox
          v-model="allChecked"
          :indeterminate="checkedAllStatus"
          @change="handleCheckAll"
        />
      </div>

      <template v-for="item in layerOptions">
        <div v-if="item.show" :key="item.id" class="choose-item">
          <div class="filter-item">
            <div class="filter-type">
              <Icon class="icon-type" :name="item.icon" />
              <label class="label-type">
                {{ item.name }}
              </label>
            </div>
            <TdCheckbox
              v-model="item.checked"
              :indeterminate="checkedStatus(item)"
              @change="handleCheckItem(item)"
            />
          </div>

          <div v-if="!bigScreen" class="item-box">
            <div v-for="child in item.children" :key="child.id" class="item-children">
              <div v-if="child.show !== false" class="filter-child">
                <div class="child-type">
                  <Icon v-if="item.childIcon" class="child-icon" :name="child.icon" />
                  <div v-else class="status-color" :class="`${child.icon}-fill`"></div>
                  {{ child.name }}
                </div>
                <TdCheckbox v-model="child.checked" @change="handleCheckItem(child, item)" />
              </div>
            </div>
          </div>
        </div>
      </template>
    </div>
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .layer-choose-frame {
    position: fixed;
    right: 98px;
    bottom: 50px;
    z-index: 101;
    height: calc(100vh - 124px);
  }

  .short-layer-frame {
    height: auto;
  }

  .layer-choose-box {
    display: flex;
    flex-flow: row wrap;
    width: fit-content;
    height: 100%;
    padding-top: 10px;
    overflow: auto;

    .icon-type {
      width: 16px;
      height: 16px;
      fill: var(--icon-color-normal);
    }

    .filter-type {
      display: flex;
      align-items: center;

      .label-type {
        margin-left: 4px;
        font-size: 14px;
        font-weight: bold;
      }
    }

    .filter-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 26px;
      padding: 0 5px 0 10px;

      &:hover {
        background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
      }
    }

    .all-choose {
      margin-bottom: 10px;
    }

    .choose-item {
      z-index: 12;
      width: 100%;
      margin-bottom: 10px;

      .item-children {
        width: 100%;
      }

      .filter-child {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 0 5px 0 30px;

        .child-type {
          display: flex;
          align-items: center;
          font-size: 12px;
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);
        }
      }

      .status-color {
        width: 10px;
        height: 10px;
        margin-right: 4px;
        border-radius: 50px;
      }

      .offline-fill {
        background-color: var(--icon-color-offline);
      }

      .online-fill {
        background-color: var(--icon-color-online);
      }

      .busy-fill {
        background-color: var(--icon-color-busy);
      }

      .underway-fill {
        background-color: var(--icon-color-underway);
      }

      /* stylelint-disable-next-line selector-class-pattern */
      .police_free-fill {
        background-color: #7ec8ff;
      }

      /* stylelint-disable-next-line selector-class-pattern */
      .police_busy-fill {
        background-color: #fca701;
      }

      /* stylelint-disable-next-line selector-class-pattern */
      .police_dimission-fill {
        background-color: #8d8e8e;
      }

      .child-icon {
        width: 14px;
        height: 14px;
        margin-right: 4px;
        fill: var(--icon-color-normal);
      }
    }
  }
</style>
