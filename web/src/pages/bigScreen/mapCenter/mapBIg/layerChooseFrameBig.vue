<script lang="ts" setup>
  /**
   * 写代码注重可读性与简洁性，谢谢！！！
   */

  import type { LayerManageOptions } from '@/pages/toolBox/type';

  import type { PropType } from 'vue';
  import { computed, ref, unref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { CategoryEnum } from '@/enums';
  import { useBaseData } from '@/hooks';
  import { getOnlineStatus } from '@/pages/resource/resourceHelper';
  import { mapManager } from '@/plugins/map';
  import { useResourceStore } from '@/store';

  import { cloneDeep, throttle } from 'lodash-es';

  const props = defineProps({
    dataSource: {
      default: () => [],
      type: Array as PropType<LayerManageOptions[]>,
    },
    mapId: {
      default: '',
      type: String,
    },
  });

  const resourceStore = useResourceStore();
  const route = useRoute();

  const layerOptions = ref<LayerManageOptions[]>([]);
  const dropList = ref<LayerManageOptions[]>([]);
  const showDropDown = ref(false);
  const expand = ref(false);
  const dropSelect = ref(['GBRecorder', 'recorder']);

  const resourceCount = computed(() => {
    const line: any = {};
    unref(layerOptions).forEach((item) => {
      line[item.id] = item.checked;
    });

    const { getResourceOrigin } = useBaseData();
    const ret = {};

    const origin = getResourceOrigin;
    Object.keys(origin).forEach((category) => {
      const target = origin[category];
      let online = 0;
      let total = 0;
      target.forEach((item) => {
        total++;
        if (getOnlineStatus(item) === 'online') {
          online++;
        }
      });

      const key = CategoryEnum[category];
      if (line.online && line.offline) {
        ret[key] = total;
      } else if (line.online) {
        ret[key] = online;
      } else if (line.offline) {
        ret[key] = total - online;
      } else {
        ret[key] = 0;
      }
    });

    return ret;
  });

  watch(
    () => route.path,
    (val) => {
      getLayerOptions(props.dataSource);
      if (val === '/planSpecial') {
        setCheckedItem(false);
      } else if (val === '/mapCenter') {
        setCheckedItem(true);
      }
    },
    { deep: true, immediate: true },
  );

  function setCheckedItem(init) {
    let checkList: string[] = [];
    if (init) {
      checkList = ['online', 'pdt', 'monitor', 'carPhoto', 'terminal', 'uav', 'recorder'];
      dropSelect.value = ['GBRecorder', 'recorder'];
    } else {
      checkList = ['online', 'offline'];
      dropSelect.value = [];
    }
    layerOptions.value.forEach((item) => {
      item.checked = checkList.includes(item.id);
    });
    resourceStore.setLayerChecked(getChecked().checked);
  }

  function getLayerOptions(val) {
    dropList.value = [];
    const options: any[] = [
      {
        checked: false,
        icon: '',
        id: 'online',
        name: '在线',
      },
      {
        checked: false,
        icon: '',
        id: 'offline',
        name: '离线',
      },
    ];

    val.forEach((item) => {
      switch (item.id) {
        case 'carPhoto':
        case 'pdt':
        case 'uav': {
          options.push(cloneDeep(item));
          break;
        }
        case 'GBRecorder': {
          const data = cloneDeep(item);
          dropList.value.push(data);
          break;
        }
        case 'monitor': {
          const data = cloneDeep(item);
          data.name = '固定监控';
          options.push(data);
          break;
        }
        case 'recorder': {
          const hw = cloneDeep(item);
          hw.name = '华为记录仪';
          dropList.value.push(hw);

          const data = cloneDeep(item);
          data.id = 'recorder';
          options.push(data);
          break;
        }
        case 'terminal': {
          const data = cloneDeep(item);
          data.name = '警务终端';
          options.push(data);
          break;
        }
      }
    });

    layerOptions.value = options;

    if (route.name === 'planSpecial') {
      hideAllResource();
      return;
    }

    const { checked } = getChecked(true);
    resourceStore.setLayerChecked(checked);
  }

  function showCounts(item) {
    const ret = !['offline', 'online', 'recorder'].includes(item.id);
    return ret;
  }

  const handleCheckItem = throttle((item) => {
    if (item.id === 'recorder') {
      return;
    }
    item.checked = !item.checked;
    setVisible();
  }, 500);

  const handleCheckDropItem = throttle((item, checked?: boolean) => {
    if (checked) {
      dropSelect.value.push(item.id);
    } else {
      const index = unref(dropSelect).indexOf(item.id);
      dropSelect.value.splice(index, 1);
    }
    layerOptions.value.forEach((item) => {
      if (item.id === 'recorder') {
        item.checked = unref(dropSelect).length > 0;
      }
    });
    setVisible();
  }, 500);

  function handleShowDropDown(data) {
    if (data.id === 'recorder') {
      showDropDown.value = !showDropDown.value;
      expand.value = !expand.value;
    }
  }

  /**
   * 获取当前选中与未选中值
   * @param {*} init
   * @returns {*} { checked, unCheck }
   */
  function getChecked(init?: boolean): { checked: string[]; unCheck: string[] } {
    const checkedList: string[] = [];
    const unCheckList: string[] = [];

    const line: { [propsName: string]: boolean } = {};
    unref(layerOptions).forEach((item) => {
      const { checked, id, children = [] } = item;

      line[id] = checked;

      if (id === 'recorder') {
        return;
      }

      children?.forEach((child) => {
        if (init) {
          if (child.checked) {
            checkedList.push(child.id);
          } else {
            unCheckList.push(child.id);
          }
          return;
        }
        if (checked) {
          if (
            (child.id.includes('Online') && line.online) ||
            (child.id.includes('Offline') && line.offline)
          ) {
            checkedList.push(child.id);
          } else {
            unCheckList.push(child.id);
          }
        } else {
          unCheckList.push(child.id);
        }
      });
    });

    unref(dropList).forEach((item) => {
      item.children?.forEach((child) => {
        if (init) {
          checkedList.push(child.id);
          return;
        }
        const check = unref(dropSelect).includes(item.id);
        if (check) {
          if (
            (child.id.includes('Online') && line.online) ||
            (child.id.includes('Offline') && line.offline)
          ) {
            checkedList.push(child.id);
          } else {
            unCheckList.push(child.id);
          }
        } else {
          unCheckList.push(child.id);
        }
      });
    });

    return { checked: checkedList, unCheck: unCheckList };
  }

  // 图层显示隐藏
  function setVisible() {
    const map = mapManager.get(props.mapId);
    const { checked, unCheck } = getChecked(false);

    resourceStore.setLayerChecked(checked);

    if (route.name !== 'planSpecial') {
      map.setLayersVisible(checked, true);
      map.setLayersVisible(unCheck, false);
    }
  }

  function hideAllResource() {
    const { checked } = getChecked(false);
    resourceStore.setLayerChecked(checked);
  }
</script>

<template>
  <div class="layer-choose-box">
    <div
      v-for="item in layerOptions"
      :key="item.id"
      class="choose-item"
      :class="{
        'choose-item_active': item.checked,
      }"
      @click="handleCheckItem(item)"
    >
      <div class="list-wrapper">
        <div class="filter-type" @click="handleShowDropDown(item)">
          <!-- <Icon v-show="item.icon" class="icon-type" :name="item.icon" /> -->
          <span class="label-type">
            {{ item.name }}
            <span v-show="showCounts(item)"> ({{ resourceCount[item.id] || 0 }}) </span>
          </span>
          <Icon
            v-show="item.id === 'recorder'"
            class="expand-icon"
            :class="{ expand }"
            name="node_expand"
          />
        </div>
        <div v-if="item.id === 'recorder'" v-show="showDropDown" class="recorder-drop ground-glass">
          <div v-for="(dropItem, index) in dropList" :key="index" class="tree-node">
            <TdCheckbox
              class="checkbox"
              :label="dropItem.name"
              :model-value="dropSelect.includes(dropItem.id)"
              @change="(check) => handleCheckDropItem(dropItem, check)"
            />
            ({{ resourceCount[dropItem.id] || 0 }})
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .layer-choose-box {
    position: fixed;
    right: 50%;
    bottom: 29px;
    z-index: 3;
    display: flex;
    flex-direction: row;
    align-items: center;
    justify-content: center;
    width: 100%;
    margin: auto;
    transform: translateX(50%);

    .choose-item {
      box-sizing: border-box;
      display: flex;
      align-items: center;
      justify-content: center;
      height: 32px;
      padding: 4px 18px;
      margin-right: 10px;
      cursor: pointer;
      background-image:
        url('@/assets/images/button/inactive_left.png'),
        url('@/assets/images/button/inactive_center.png'),
        url('@/assets/images/button/inactive_right.png');
      background-repeat: no-repeat;
      background-position: left, center, right;
      background-size:
        18px 32px,
        calc(100% - 36px) 32px,
        18px 32px;

      &_active {
        background-image:
          url('@/assets/images/button/active_left.png'),
          url('@/assets/images/button/active_center.png'),
          url('@/assets/images/button/active_right.png');
      }
    }

    .list-wrapper {
      position: relative;

      .expand-icon {
        width: 10px;
        height: 5px;
        margin-left: 4px;
      }

      .expand {
        transform: rotate(-180deg);
      }

      .tree-node {
        position: relative;
        display: flex;
        align-items: center;
        width: 160px;
        height: 28px;
        padding: 0 6px;
        cursor: pointer;

        .drag-box {
          display: flex;
          align-items: center;
        }

        .checkbox {
          margin-right: 4px;
        }

        .node-info {
          display: flex;
          align-items: center;
        }

        .name {
          display: inline-block;
          height: 22px;
          margin-left: 4px;
          font-size: 14px;
          line-height: 22px;
        }

        .operation {
          position: absolute;
          right: 0;
          opacity: 0;
        }

        &:hover {
          background: linear-gradient(90deg, rgb(41 233 194 / 80%) 0%, rgb(55 219 157 / 28%) 100%);

          .operation {
            opacity: 1;
          }
        }
      }
    }

    .icon-type {
      width: 20px;
      height: 20px;
      margin-right: 6px;
      fill: var(--icon-color-normal);
    }

    .filter-type {
      display: flex;
      align-items: center;
      height: 20px;

      .label-type {
        font-size: 14px;
        font-weight: 400;
        line-height: 20px;
        color: rgb(255 255 255 / 100%);
        letter-spacing: 0;
      }
    }

    .recorder-drop {
      position: absolute;
      bottom: 34px;
      left: -9px;
    }
  }
</style>
