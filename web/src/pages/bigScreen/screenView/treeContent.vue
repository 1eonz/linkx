<script setup lang="ts">
  import { computed, onMounted, ref, unref, useAttrs } from 'vue';

  import {
    countEquipmentAndFacilitiesByArea,
    countEquipmentAndFacilitiesByOrg,
    queryArea,
    queryOrganizationByArea,
    queryStatusCount,
  } from '@/api/count';
  import { queryRegionRangeByAreaCode } from '@/api/region';
  import { CategoryEnum } from '@/enums';
  import { useEmitter } from '@/hooks';
  import { getResourceTypes } from '@/pages/resource/resourceHelper';
  import { getAreaCenter, mapManager } from '@/plugins/map';

  import { Timeout } from '#/index';

  import Tree from './tree.vue';

  const props = withDefaults(
    defineProps<{
      activeCount?: string;
      defaultCheckKeys?: string[];
      from?: number; // 来源
      iccOnly?: boolean;
      navId: number | string;
      showResource?: any[];
    }>(),
    {},
  );
  const emit = defineEmits(['click', 'dblclick', 'checkChange', 'navChange']);
  defineExpose({ setCheckedKeys });

  const attrs = useAttrs();
  const isShowSub = ref(false);
  const navLink = ref<any[]>([]);
  const currentOrgList = ref<any[]>([]);
  const treeComponents = ref<any>(getResourceTypes());
  const treeRef = ref();
  let timer: Timeout;
  let orgTimer: Timeout;
  const checkKeys = ref<string[]>([]);
  const checkDataMap = new Map();

  const showEmpty = computed(() => {
    const { navId } = props;
    const arr = unref(treeComponents);
    if (navId === 'all') {
      return arr.every((item) => item.total === 0);
    } else {
      if (navId === 'thirdEquipment') {
        const filterList = filterData([CategoryEnum.uav, CategoryEnum.confTerminal]);
        return filterList.every((item) => item.total === 0);
      } else if (navId === CategoryEnum.recorder) {
        const filterList = filterData([CategoryEnum.GBRecorder, CategoryEnum.recorder]);
        return filterList.every((item) => item.total === 0);
      } else {
        for (const item of arr) {
          if (item.id === navId) {
            return item.total === 0;
          }
        }
      }
    }
    return false;
  });
  const orgId = computed(() => {
    const arr = unref(navLink);
    if (arr[arr.length - 1]?.administrativeArea) {
      return arr[arr.length - 1].id;
    }
    return '';
  });

  const adCode = computed(() => {
    const arr = unref(navLink);
    if (arr[arr.length - 1]?.administrativeArea) {
      return arr[arr.length - 1].administrativeArea;
    }
    return arr[arr.length - 1]?.code || '';
  });

  onMounted(() => {
    init();
    getArea();
    useEmitter('mainNavClick', navClickListener);
  });

  function navClickListener(params) {
    console.log('mainNavClick', params);
    unref(navLink).splice(1);
    navLink.value[0].children.forEach((element) => {
      if (params.adCode === element.code) {
        navLink.value.push(element);
      }
    });
    if (navLink.value.length < 2) {
      return;
    }
    queryOrganization(navLink.value[1]);
    getOnline();
  }

  function showSub() {
    isShowSub.value = !isShowSub.value;
  }

  function filterData(arr: any) {
    const treeList = unref(treeComponents);
    return treeList.filter((item) => {
      return arr.includes(item.id);
    });
  }

  function init() {
    const { showResource } = props;
    if (showResource) {
      treeComponents.value.forEach((item) => {
        if (item.show) {
          item.show = showResource.includes(item.type);
        }
      });
    }
  }

  function orgOnline(item) {
    const { offline = 0, online = 0 } = item;
    switch (props.activeCount) {
      case 'offline': {
        return offline;
      }
      case 'online': {
        return online;
      }
      default: {
        return `${online}/${online + offline}`;
      }
    }
  }

  function showSubMenu(item) {
    if (!isShowSub.value) {
      return false;
    }
    if (navLink.value.length <= 2) {
      return true;
    }
    const { offline = 0, online = 0 } = item;
    switch (props.activeCount) {
      case 'offline': {
        return offline > 0;
      }
      case 'online': {
        return online > 0;
      }
      default: {
        return online + offline > 0;
      }
    }
  }

  function showTree(item) {
    const { navId } = props;
    const { id } = item;
    if (navId === id) {
      return true;
    } else if (navId === 'all') {
      return true;
    } else if (
      navId === CategoryEnum.recorder &&
      [CategoryEnum.GBRecorder, CategoryEnum.recorder].includes(id)
    ) {
      return true;
    } else if (
      navId === 'thirdEquipment' &&
      [CategoryEnum.confTerminal, CategoryEnum.uav].includes(id)
    ) {
      return true;
    }
    return false;
  }

  // 获取行政区域
  async function getArea() {
    const { code, data } = await queryArea();
    if (code === 0 && data && data.length > 0) {
      navLink.value = [data[0]];
      const { children } = data[0];
      currentOrgList.value = children || [];
      getOnline();
    }
  }

  function handleMenuClick(data) {
    navLink.value.push(data);
    if (navLink.value.length === 2) {
      queryOrganization(data);
      getOnline();
    } else if (navLink.value.length > 2) {
      currentOrgList.value = data.children || [];
      getOnline();
      remarkAdmin(data.code);
      emit('navChange', navLink.value, data);
    } else {
      emit('navChange', navLink.value, data);
    }
    useEmitter().emit('mainChartClick', {
      adCode: data?.administrativeArea || data?.code,
    });
  }

  async function queryOrganization(val) {
    const { code, data } = await queryOrganizationByArea({ adcode: val.code });
    currentOrgList.value = [];
    if (code === 0 && data && data.length > 0) {
      currentOrgList.value = data[0].children || [];
      // getOnline();
    }
    emit('navChange', navLink.value, val);
  }

  function handlePath(data, index) {
    useEmitter().emit('mainChartClick', {
      adCode: data?.administrativeArea || data?.code,
    });
    if (index === 0) {
      getArea();
      unref(navLink).splice(index + 1);
      emit('navChange', navLink.value, data);
      return;
    }
    if (index < unref(navLink).length - 1) {
      unref(navLink).splice(index + 1);
    }
    if (navLink.value.length === 2) {
      queryOrganization(data);
      getOnline();
    } else if (navLink.value.length > 2) {
      currentOrgList.value = data.children || [];
      getOnline();
      remarkAdmin(data.code);
    }
    emit('navChange', navLink.value, data);
  }

  async function remarkAdmin(adcode) {
    const mapTempId = 'mainMap';
    const mapObj = mapManager.get(mapTempId);
    if (!mapObj) return;
    mapObj.closeBoxToSelect();
    const { code, data } = await queryRegionRangeByAreaCode({ adcode });

    if (code === 0) {
      data.forEach(({ color, geo }) => {
        mapObj.draw({
          center: '',
          color,
          handle: 'look',
          hex: true,
          highlight: true,
          path: geo,
          radius: '',
          type: 'polygon',
        });
      });
      setCenter({ path: data[0]?.geo }, mapTempId);
    }
  }

  function setCenter(geo, mapTempId) {
    const mapObj = mapManager.get(mapTempId);

    const room = getRoom();
    const center = getAreaCenter(geo);

    mapObj?.setCenter(center, room);
  }

  function getRoom() {
    const mapType = mapManager.get('mapType');
    const room = { AMap: 15, ArcgisMap: 14, BMap: 15, MapAbc: 8, MineMap: 15, Openlayers: 14 }[
      mapType
    ];
    return room;
  }

  function checkChange(category, checks, origin) {
    const keys: string[] = [];
    checks.forEach((item) => {
      const { id } = item;
      keys.push(id);
      const index = unref(checkKeys).indexOf(id);
      if (index === -1) {
        checkKeys.value.push(id);
      }
      checkDataMap.set(id, { ...unref(item), category });
    });
    const cancelCheckKeys: string[] = [];
    origin.forEach((item) => {
      const { id } = item;
      if (keys.includes(id)) return;
      const index = unref(checkKeys).indexOf(id);
      if (index !== -1) {
        cancelCheckKeys.push(id);
        checkKeys.value.splice(index, 1);
        checkDataMap.delete(id);
      }
    });

    const ret: any[] = [];
    for (const value of checkDataMap.values()) {
      if (Number(value.category) === Number(category)) {
        ret.push(value);
      }
    }
    emit('checkChange', category, ret, cancelCheckKeys);
  }

  function handleTreeClick(data, type) {
    emit('click', data, type);
  }

  function handleTreeDblclick(data, type) {
    emit('dblclick', data, type);
  }

  // 设置目前选中的节点
  function setCheckedKeys(data) {
    const obj: any = {};
    unref(treeComponents).forEach((item) => {
      obj[item.id] = [];
    });

    checkDataMap.clear();
    const keys = data.map((item) => {
      if (item.resourceType === 'Executor') {
        item.category = CategoryEnum.person;
      }
      checkDataMap.set(item.id, item);
      return item.id;
    });
    checkKeys.value = keys;

    for (const data of checkDataMap.values()) {
      const { category } = data;
      obj[category]?.push(data);
    }
    Object.keys(obj).forEach((key) => {
      emit('checkChange', key, obj[key]);
    });
  }

  function getOnline() {
    getOrgOnlineCount();
    getOnlineScale();
  }

  // 获取下级组织的资源统计
  async function getOrgOnlineCount() {
    clearTimeout(orgTimer);
    const { navId } = props;
    const param: any = { orgId: unref(orgId) };
    const paramArea: any = { adcode: unref(adCode) };
    switch (props.navId) {
      case 'all': {
        const category = unref(treeComponents)
          .filter((i) => i.show && i.id !== 'thirdEquipment')
          .map((i) => i.id)
          .join(',');
        param.category = category;
        paramArea.category = category;

        break;
      }
      case CategoryEnum.recorder: {
        const category = unref(treeComponents)
          .filter((i) => i.show && [CategoryEnum.GBRecorder, CategoryEnum.recorder].includes(i.id))
          .map((i) => i.id)
          .join(',');
        param.category = category;

        break;
      }
      case 'thirdEquipment': {
        const category = unref(treeComponents)
          .filter(
            (i) => i.show && (CategoryEnum.uav === i.id || CategoryEnum.confTerminal === i.id),
          )
          .map((i) => i.id)
          .join(',');
        param.category = category;

        break;
      }
      default: {
        param.category = navId;
      }
    }
    if (navLink.value.length <= 2) {
      const { code, data } = await countEquipmentAndFacilitiesByArea(paramArea);
      if (code === 0) {
        currentOrgList.value.forEach((item) => {
          data.forEach((count) => {
            if (count.orgId === item.id) {
              item.online = count.onlineCnt;
              item.offline = count.offlineCnt;
              item.total = item.online + item.offline;
            }
          });
        });
      }
    } else {
      const { code, data } = await countEquipmentAndFacilitiesByOrg(param);
      if (code === 0) {
        currentOrgList.value.forEach((item) => {
          data.forEach((count) => {
            if (count.orgId === item.id) {
              item.online = count.onlineCnt;
              item.offline = count.offlineCnt;
              item.total = item.online + item.offline;
            }
          });
        });
      }
    }

    orgTimer = setTimeout(() => {
      getOrgOnlineCount();
    }, 15 * 1000);
  }

  // 获取在线离线统计
  async function getOnlineScale() {
    clearTimeout(timer);

    const params: any = [];
    unref(treeComponents).forEach((item) => {
      if (!item.show || item.id === 'thirdEquipment') return;
      const category = item.id;
      const param: any = {
        adcode: unref(adCode),
        orgId: unref(orgId),
        type: '1',
      };
      if (Number(category) === CategoryEnum.monitor) {
        param.type = '2';
      } else if (Number(category) === CategoryEnum.person) {
        param.type = '0';
        // param.iccOnly = iccOnly;
      } else {
        param.category = category;
      }
      params.push(param);
    });

    const { code, data } = await queryStatusCount(params);
    if (code === 0) {
      data.forEach((item) => {
        const { online, total, type } = item;
        let category = Number(item.category);
        if (type === '2') {
          category = CategoryEnum.monitor;
        } else if (type === '0') {
          category = CategoryEnum.person;
        }
        treeComponents.value.forEach((comp) => {
          if (Number(comp.id) === category) {
            Object.assign(comp, { online, total });
          }
        });
      });
    }
    timer = setTimeout(() => {
      getOnlineScale();
    }, 15 * 1000);
  }
</script>

<template>
  <div class="tree-content">
    <div class="organization ml-10px">
      <div class="nav-link-main" @click="showSub()">
        <template v-for="(item, index) in navLink" :key="item.id">
          <div v-if="index <= 1 || index >= navLink.length - 2" class="link">
            <span :class="{ active: index < navLink.length - 1 }" @click="handlePath(item, index)">
              <span>{{ item.name }}</span>
            </span>
            <Icon v-if="index < navLink.length - 1" class="icon" name="right_triangle_arrow" />
          </div>

          <div v-if="index === 2 && navLink.length > 4" class="link omit">
            <span class="active">...</span>
            <Icon class="icon" name="right_triangle_arrow" />
          </div>
        </template>
      </div>
      <div class="sub-menu sub-menu-main">
        <div
          v-for="item in currentOrgList"
          v-show="showSubMenu(item)"
          :key="item.id"
          class="menu-item"
          @click="handleMenuClick(item)"
        >
          <TdTooltip v-if="navLink.length >= 2" :content="`${item.name}(${orgOnline(item)})`">
            <span>{{ `${item.name}(${orgOnline(item)})` }}</span>
          </TdTooltip>
          <TdTooltip v-else :content="`${item.name}`">
            <span>{{ `${item.name}` }}</span>
          </TdTooltip>
          <Icon class="icon" name="right_triangle_arrow" />
        </div>
      </div>
    </div>

    <template v-for="item in treeComponents" :key="item.id">
      <Tree
        v-if="item.show && item.id !== 'thirdEquipment'"
        v-show="!showEmpty && showTree(item)"
        v-bind="attrs"
        ref="treeRef"
        :active-count="activeCount"
        :adcode="adCode"
        :category="Number(item.id)"
        :check-keys="checkKeys"
        :from="from"
        :org-id="orgId"
        :resource-info="item"
        @check-change="checkChange"
        @click="handleTreeClick"
        @dblclick="handleTreeDblclick"
      />
    </template>

    <TdEmpty v-if="showEmpty" class="empty" />
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .tree-content {
    flex: 1;
    overflow-y: scroll;

    .title {
      display: flex;
      align-items: center;
      height: 24px;
      margin-bottom: 8px;
      background-color: rgb(1 12 23 / 40%);

      .td-icon {
        width: 16px;
        height: 16px;
        margin: -2px 4px 0 0;
      }
    }

    .organization {
      margin-bottom: 10px;

      .icon {
        width: 12px;
        height: 12px;
        margin: 0 4px;
        fill: rgb(153 206 251 / 100%);
      }

      .nav-link {
        display: flex;
        align-items: center;

        .link {
          display: flex;
          align-items: center;

          span {
            font-size: 14px;
            font-weight: 400;
            cursor: pointer;
          }

          .active {
            color: rgb(153 206 251 / 100%);
          }
        }
      }

      .nav-link-main {
        display: flex;
        align-items: center;
        width: 388px;
        height: 32px;
        background: rgb(57 69 102 / 50%);
        border-radius: 2px;

        .link {
          display: flex;
          align-items: center;
          margin-left: 10px;

          span {
            font-size: 14px;
            font-weight: 400;
            cursor: pointer;
          }

          .active {
            color: rgb(153 206 251 / 100%);
          }
        }
      }

      .sub-menu {
        width: 100%;

        .menu-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          height: 24px;
          margin: 0 0 0 18px;
          cursor: pointer;

          span {
            font-size: 14px;
            font-weight: 400;
            .ellipsis1();
          }
        }
      }

      .sub-menu-main {
        width: 100%;
        width: 388px;
        background: rgb(57 69 102 / 50%);
        border-radius: 2px;

        .menu-item {
          display: flex;
          align-items: center;
          justify-content: space-between;
          height: 24px;
          margin: 0 0 0 18px;
          cursor: pointer;

          span {
            font-size: 14px;
            font-weight: 400;
            .ellipsis1();
          }
        }
      }
    }

    .empty {
      height: 50%;
    }
  }
</style>
