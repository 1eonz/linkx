<script setup lang="ts">
  import { computed, onBeforeUnmount, onMounted, ref, unref, useAttrs, watch } from 'vue';

  import { queryOrganizationById } from '@/api/resource';
  import { Message } from '@/components/Message';
  import { CategoryEnum } from '@/enums';
  import { useBaseData, useEmitter, useI18n, useSetInterval } from '@/hooks';
  import {
    drawJurisdiction,
    filterRegionLayer,
  } from '@/pages/bigScreen/mapCenter/jurisdiction/helper';
  import { getOnlineStatus, getResourceTypes } from '@/pages/resource/resourceHelper';
  import { mapManager } from '@/plugins/map';
  import { useResourceStore } from '@/store';

  import { cloneDeep, throttle } from 'lodash-es';

  import Tree from './tree.vue';

  const props = withDefaults(
    defineProps<{
      activeCount?: string;
      defaultCheckKeys?: string[];
      from?: number; // 来源
      iccOnly?: boolean;
      isMain?: boolean; // 是否一屏统览
      navId: number | string;
      showPim?: boolean;
      showResource?: any[];
    }>(),
    {
      activeCount: 'all',
      isMain: false,
    },
  );

  const emit = defineEmits(['click', 'dblclick', 'checkChange', 'navChange']);

  const { t } = useI18n();

  defineExpose({ setCheckedKeys });

  const attrs = useAttrs();
  const resourceStore = useResourceStore();

  const isShowSub = ref(!props.isMain);
  const navLink = ref<any[]>([]);
  const currentOrgList = ref<any[]>([]);
  const checkKeys = ref<string[]>([]);
  const checkDataMap = new Map();
  const treeData = ref<any[]>([]);
  const treeContentRef = ref();
  const titleRef = ref();
  let clearTimer: any = null;
  const pageNum = ref({});

  const orgId = computed(() => {
    const arr = unref(navLink);
    return arr[arr.length - 1]?.id || '';
  });
  const treeComponents = computed(() => {
    let ret = cloneDeep(getResourceTypes());

    const { showResource } = props;
    if (showResource) {
      ret.forEach((item) => {
        item.show = showResource.includes(item.type);
      });
    }

    ret.forEach((item) => {
      item.show = item.show && showTree(item);
    });

    if (props.showPim) {
      ret = ret.filter((i) => i.id !== CategoryEnum.person);
    }

    return ret;
  });

  watch(
    () => props.defaultCheckKeys,
    (val = []) => {
      for (const key of checkDataMap.keys()) {
        if (!val.includes(key)) {
          checkDataMap.delete(key);
        }
      }

      val.forEach((item) => {
        if (!checkDataMap.has(item)) {
          checkDataMap.set(item, item);
        }
      });
      checkKeys.value = [...val];
    },
    { immediate: true },
  );
  watch(
    () => resourceStore.organization,
    (val) => {
      const item = val[0];
      if (!item) {
        return;
      }
      const target = cloneDeep(item);
      currentOrgList.value = target.children || [];
      navLink.value = [target];
    },
    {
      immediate: true,
    },
  );

  useEmitter('clickLandmark', setCurrentOrg);

  onMounted(() => {
    initPage();
    setTimer(true);
    window.addEventListener('keydown', handleKeyDown);
  });

  onBeforeUnmount(() => {
    clearTimer?.();
  });

  function handleKeyDown(e) {
    if (e.keyCode === 32) {
      e.preventDefault();
    }
  }

  function initPage() {
    getResourceTypes().forEach((item) => {
      pageNum.value[item.id] = 20;
    });
  }

  const update = throttle(() => {
    getAccount();
    getOrganization();
  }, 1000);

  watch([() => props.activeCount, orgId, () => props.navId], update, {
    deep: true,
    immediate: true,
  });

  function showSub() {
    if (props.isMain) {
      isShowSub.value = !isShowSub.value;
    }
  }

  function setTimer(immediate: boolean) {
    clearTimer = useSetInterval(update, 10 * 1000, immediate);
  }

  // 统计组织及下级组织数量
  function getAccount() {
    const { getResourceOrganization } = useBaseData();
    const arr = cloneDeep(unref(currentOrgList));
    arr.forEach((data) => {
      const { navId } = props;
      let online = 0;
      let offline = 0;
      const mapper = (data) => {
        unref(treeComponents).forEach((item) => {
          if (!item.show) {
            return;
          }

          const category = Number(item.id);
          if (!['all', 'thirdEquipment'].includes(navId as string) && navId !== category) {
            return;
          }

          let keys = [category];
          if (
            navId === CategoryEnum.recorder &&
            [CategoryEnum.GBRecorder, CategoryEnum.recorder].includes(category) &&
            props.showResource?.includes(item.type)
          ) {
            keys = [CategoryEnum.recorder, CategoryEnum.GBRecorder];
          } else if (navId === 'thirdEquipment' && data.id === 'thirdEquipment') {
            keys = [CategoryEnum.uav, CategoryEnum.confTerminal];
          }

          keys.forEach((c) => {
            const resource = getResourceOrganization[c] || [];
            resource[data.id]?.forEach((item) => {
              if (getOnlineStatus(item) === 'online') {
                online++;
              } else {
                offline++;
              }
            });
          });
        });
        data.children?.forEach(mapper);
      };
      mapper(data);
      Object.assign(data, { offline, online, total: online + offline });
    });
    currentOrgList.value = arr;
  }

  // 获取当前组织下资源树
  function getOrganization() {
    const { getResourceOrganization } = useBaseData();
    const organization: any[] = [];
    unref(treeComponents).forEach((item) => {
      if (!item.show) {
        return;
      }

      const arr = cloneDeep(getResourceOrganization[item.id]?.[unref(orgId)] || []);
      const online: any[] = [];
      const offline: any[] = [];
      for (const item of arr) {
        if (getOnlineStatus(item) === 'online') {
          online.push(item);
        } else {
          offline.push(item);
        }
      }

      const { activeCount } = props;
      const onLen = online.length;
      const offLen = offline.length;
      if (
        arr.length === 0 ||
        (activeCount === 'online' && onLen === 0) ||
        (activeCount === 'offline' && offLen === 0)
      ) {
        return;
      }

      const _name = {
        all: `${onLen}/${arr.length}`,
        offline: offLen,
        online: onLen,
      };
      const _children = {
        all: [...online, ...offline],
        offline,
        online,
      };
      organization.push({
        category: item.id,
        id: item.id,
        name: `${item.name}(${_name[activeCount as string]})`,
        children: _children[activeCount as string] || [],
      });
    });

    getShowTreeData(organization);
  }

  function getShowTreeData(organization) {
    treeData.value = organization.map((item) => {
      const { category } = item;
      const children = item.children.slice(0, unref(pageNum)[category] || 20);
      if (children.length < item.children.length) {
        children.push({ category, loadMore: true });
      }
      return { ...item, allChild: item.children, children: cloneDeep(children) };
    });
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
    if (navId === id || navId === 'all') {
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

  function handleMenuClick(data) {
    navLink.value.push(data);
    currentOrgList.value = data.children || [];
    menuChange(data);
  }

  async function handlePath(index: number) {
    const { id } = unref(navLink)[index];
    const res = await queryOrganizationById({ id });
    const data = res.data[0];
    if (index < unref(navLink).length - 1) {
      unref(navLink).splice(index + 1);
      currentOrgList.value = data.children || [];
    }
    menuChange(data);
  }

  function delMapRegion() {
    const mapObj = mapManager.get('mapId_main');
    mapObj?.closeBoxToSelect();
    mapObj?.deleteLayer(filterRegionLayer('region'));
  }

  function menuChange(data) {
    initPage();
    // 需要先删除之前绘制的辖区
    delMapRegion();
    drawJurisdiction({ adcode: data.code });
    emit('navChange', navLink.value, data);
  }

  // 通过组织id自动匹配对应组织树
  async function setCurrentOrg(orgId) {
    const { code, data } = await queryOrganizationById({ id: orgId });
    if (code === 0) {
      const { fullPath, fullPathName, children } = data[0];
      const index = resourceStore.organization[0].level - 1;
      const pathName = fullPathName.split('>').slice(index);
      const path = fullPath.split(',').slice(index);
      const nav: any = [];
      pathName.forEach((item, index) => {
        nav.push({
          id: path[index],
          name: item,
        });
      });
      navLink.value = nav;
      currentOrgList.value = children || [];
      menuChange(data[0]);
    }
  }

  function checkChange(category, check, data) {
    checkDataMapVal();

    const cancelCheckKeys: string[] = [];
    let total = checkDataMap.size;

    const setVal = (id, data) => {
      if (total < 200) {
        checkDataMap.set(id, data);
      }
      total++;
    };

    data.forEach((item) => {
      if (check) {
        setVal(item.id, item);
      } else {
        checkDataMap.delete(item.id);
        cancelCheckKeys.push(item.id);
      }
    });

    const ret: any[] = [];
    const keys: any[] = [];
    for (const val of checkDataMap.values()) {
      keys.push(val.id);
      if (Number(val.category) === Number(category)) {
        ret.push(val);
      }
    }
    checkKeys.value = keys;

    if (total > 200) {
      Message({
        duration: 3000,
        message: t('resource.poll.maxSelected'),
        type: 'warning',
      });
    }
    emit('checkChange', category, ret, cancelCheckKeys);
  }

  // 确保checkDataMap值与选中的一致
  function checkDataMapVal() {
    const { resourceOrigin } = useBaseData();
    unref(checkKeys).forEach((key) => {
      if (!checkDataMap.has(key)) {
        Object.keys(resourceOrigin).forEach((category) => {
          const target = resourceOrigin[category][key];
          if (target) {
            checkDataMap.set(key, cloneDeep(target));
          }
        });
      }
    });
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

  function handleLoadMore(data, num) {
    if (num) {
      pageNum.value[data.category] = num;
    } else {
      pageNum.value[data.category] += 20;
    }
  }
</script>

<template>
  <div ref="treeContentRef" class="tree-content">
    <div v-if="!isMain" ref="titleRef" class="title">
      <Icon name="organization" prefix="tree" />{{ t('policeAdmin.track.organizationName') }}
    </div>

    <div class="organization" :class="isMain ? 'ml-10px' : ''">
      <div :class="isMain ? 'nav-link-main' : 'nav-link'" @click="showSub">
        <template v-for="(item, index) in navLink" :key="item.id">
          <div v-if="index < 1 || index >= navLink.length - 1" class="link">
            <span :class="{ active: index < navLink.length - 1 }" @click="handlePath(index)">
              <span>{{ item.name }}</span>
            </span>
            <Icon v-if="index < navLink.length - 1" class="icon" name="right_triangle_arrow" />
          </div>

          <div v-if="index === 1 && navLink.length > 2" class="link">
            <span class="active" @click="handlePath(navLink.length - 2)">...</span>
            <Icon class="icon" name="right_triangle_arrow" />
          </div>
        </template>
      </div>
      <div class="sub-menu" :class="isMain ? 'sub-menu-main' : 'sub-menu'">
        <div
          v-for="item in currentOrgList"
          v-show="showSubMenu(item)"
          :key="item.id"
          class="menu-item"
          @click="handleMenuClick(item)"
        >
          <TdTooltip :content="`${item.name}(${orgOnline(item)})`">
            <span>{{ `${item.name}(${orgOnline(item)})` }}</span>
          </TdTooltip>
          <Icon class="icon" name="right_triangle_arrow" />
        </div>
      </div>
    </div>

    <div v-if="!isMain" class="title">
      <Icon name="equipment" prefix="tree" />{{ t('homePage.mapToolData.equipment') }}
    </div>

    <div v-scrollHideTooltips class="tree">
      <template v-for="item in treeData" :key="item.id">
        <Tree
          v-bind="attrs"
          :check-keys="checkKeys"
          :data="item"
          @check-change="checkChange"
          @click="handleTreeClick"
          @dblclick="handleTreeDblclick"
          @load-more="handleLoadMore"
        />
      </template>
    </div>

    <TdEmpty v-if="treeData.length === 0" class="empty" />
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .tree-content {
    display: flex;
    flex-direction: column;
    width: calc(100% - 32px);
    height: 100%;
    padding-left: 6px;

    .title {
      display: flex;
      align-items: center;
      height: 24px;
      padding-left: 4px;
      margin-bottom: 8px;
      background-color: rgb(1 12 23 / 40%);

      .td-icon {
        width: 16px;
        height: 16px;
        margin: -2px 4px 0 0;
      }
    }

    .organization {
      max-height: 40%;
      margin-bottom: 10px;
      overflow-y: scroll;

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
          flex-shrink: 0;
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
          margin: 8px 0 8px 18px;
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

    .tree {
      flex: 1;
      overflow: hidden auto;
    }

    .empty {
      height: 50%;
    }

    :deep(.el-tree-virtual-list) {
      will-change: auto !important;
    }
  }
</style>
