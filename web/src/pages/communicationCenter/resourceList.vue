<script lang="ts" setup>
  import { computed, onMounted, ref, shallowRef, unref, watch } from 'vue';
  import { useRoute } from 'vue-router';

  import { Message } from '@/components/Message';
  import { CategoryEnum } from '@/enums';
  import { useEmitter, useI18n, useUtils } from '@/hooks';
  import VideoPolling from '@/pages/bigScreen/mapCenter/videoPolling.vue';
  import PlanInfo from '@/pages/bigScreen/planSpecial/planInfo.vue';
  import PimSearchSlot from '@/pages/coordination/chatList/pimSearchSlot.vue';
  import MrsSearch from '@/pages/mrs/mrsSearch.vue';
  import {
    getResourceType,
    monitorCall,
    resourceCategory,
    singleClick,
    trackPlay,
    videoPointCall,
    voiceHalfCall,
    voicePointCall,
  } from '@/pages/resource/resourceHelper';
  import ResourceSearch from '@/pages/resource/resourceSearch/index.vue';
  import FavoriteTree from '@/pages/tree/favoriteTree/index.vue';
  import PlanTree from '@/pages/tree/planTree/index.vue';
  import ResourceTree from '@/pages/tree/resourceTree/index.vue';
  import { useCommunicateDispatchStore, useTreeStore } from '@/store';

  import { getMonitorTab, getPlayTab, getResourceTab } from './common';
  import EquipmentCount from './components/equipmentCount.vue';
  import GroupList from './components/groupList.vue';
  import QuickOperate from './components/quickOperate.vue';
  import VideoPollTree from './components/videoPollTree.vue';

  type CheckMap = {
    [key: number]: Map<string, any>;
  };

  withDefaults(
    defineProps<{
      showCloseBtn?: boolean;
      showCollect?: boolean;
    }>(),
    {
      showCloseBtn: true,
    },
  );
  const emit = defineEmits(['closeDialog', 'tabClick']);

  const { t } = useI18n();
  const { isCommPanel } = useUtils();
  const route = useRoute();
  const communicateDispatchStore = useCommunicateDispatchStore();
  const treeStore = useTreeStore();
  const tabData = getResourceTab();
  const monitorTabData = getMonitorTab();
  const playTabTab = getPlayTab();

  const topTabData = ref<any>([]);
  const showSearchResult = ref(false);
  const currentComponent = shallowRef<any>(ResourceTree); // 默认选中的卡片
  const activeId = ref('ResourceTree');
  const category = ref(0);
  const activeCount = ref('all');
  const treeRef = ref();
  const checkKeys = ref<string[]>([]);
  const checkMap: CheckMap = {};
  const activeTab = ref('');

  const showQuickOperate = computed(() => {
    const other = ['GroupList', 'VideoPollTree'].includes(activeId.value);
    return !other;
  });
  const showTabData = computed(() => {
    const ret = tabData.filter((item) => {
      return item.show;
    });
    return ret;
  });
  const isMonitorScreen = computed(() => {
    return communicateDispatchStore.curActiveDesktop === 'MonitorDesktop';
  });
  const showTopTab = computed(() => {
    return (isCommPanel && unref(isMonitorScreen)) || useUtils().isSpecial;
  });
  const showResource = computed(() => {
    let show = true;
    show = useUtils().isSpecial
      ? activeTab.value === 'resource'
      : !isMonitorScreen.value || (isMonitorScreen.value && activeTab.value === 'monitor');
    return show;
  });

  watch(
    () => route.name,
    () => {
      // 切换菜单重置选择项
      checkKeys.value = [];
      initCheckMap();
      treeStore.clearResourceCheckedList();

      if (useUtils().isSpecial) {
        topTabData.value = [...playTabTab];
        activeTab.value = 'plan';
      } else {
        topTabData.value = [...monitorTabData];
        // 如果是从录音录像跳转
        if (route.query.callType) {
          setTimeout(() => {
            topTabClick({ id: 'history' });
          }, 0);
          return;
        }
        activeTab.value = 'monitor';
        handleActiveDesktop();
      }
    },
    { deep: true, immediate: true },
  );
  watch(() => communicateDispatchStore.curActiveDesktop, handleActiveDesktop, {
    deep: true,
    immediate: true,
  });

  useEmitter('removeCheckedList', removeListener);

  onMounted(() => {
    initCheckMap();
  });

  function initCheckMap() {
    resourceCategory.forEach((item) => {
      checkMap[item] = new Map();
    });
  }

  function removeListener(id) {
    checkKeys.value = unref(checkKeys).filter((i) => i !== id);
    Object.keys(checkMap).forEach((key) => {
      checkMap[key].delete(id);
      treeStore.setResourceCheckedList({
        data: [...checkMap[key].values()],
        type: CategoryEnum[key],
      });
    });
  }

  // tab选择
  function showChildMessage(data) {
    const comp = {
      FavoriteTree,
      GroupList,
      PlanTree,
      ResourceTree,
      VideoPollTree,
    };
    const { id } = data;
    currentComponent.value = comp[id];
    activeId.value = id;
    useEmitter().emit('clearFilterText', id);
  }

  function searchChange(val) {
    showSearchResult.value = val !== '';
  }

  function checkChange(category, checks, cancelCheckKeys?) {
    cancelCheckKeys = cancelCheckKeys || [];
    let defaultKeys: any[] = [];

    const keySet = new Set();
    defaultKeys = unref(checkKeys).filter((id) => {
      keySet.add(id);
      const check = !cancelCheckKeys.includes(id);
      if (!check) {
        checkMap[category].delete(id);
      }
      return check;
    });

    checks.forEach((item) => {
      const { id } = item;
      if (!keySet.has(id)) {
        defaultKeys.push(id);
        checkMap[category].set(id, item);
      }
    });

    checkKeys.value = defaultKeys;

    const arr = [...checkMap[category].values()];
    treeStore.setResourceCheckedList({
      data: arr,
      type: CategoryEnum[category],
    });
  }

  function countChange(id) {
    activeCount.value = id;
  }

  function handleNodeClick(data: any) {
    if (isCommPanel || data.type === 'click') {
      return;
    }
    const type = getResourceType(data);
    singleClick({ data, type });
  }

  function handleNodeDblclick(data: any) {
    const type = getResourceType(data);
    if (type === 'person') {
      return;
    }

    const param = { ...data, account: data.account || data.code, resourceType: type };
    if (type === 'monitor') {
      const arr = [...communicateDispatchStore.monitorDesktopList];
      const index = arr.findIndex((item) => {
        return item?.id === data?.id;
      });
      if (index !== -1 && isCommPanel) {
        Message({
          message: data.name + t('homePage.noticeCenterData.onMonitorScreen'),
          type: 'warning',
        });
        return;
      }
      monitorCall(param);
    } else {
      const { capability = '', category = 0 } = data;
      // pdt半双工点呼
      if (category === CategoryEnum.pdt) {
        voiceHalfCall(param);
      } else if (category === CategoryEnum.carPhoto && type === 'equipment') {
        // 车载图传属于equipment但与固定监控功能相同 都有视频监控功能
        const pictureParam = {
          ...data,
          account: data.account || data.code,
          resourceType: 'monitor',
        };
        monitorCall(pictureParam);
      } else if (capability.includes('512001')) {
        voicePointCall(param.account);
      } else if (capability.includes('512006')) {
        videoPointCall(param);
      } else if (capability.includes('512005')) {
        trackPlay({ infoData: data, resourceType: type });
      } else if (capability.includes('512007')) {
        monitorCall(param);
      }
    }
  }

  function closeResourceBox() {
    emit('closeDialog');
  }

  function handleActiveDesktop() {
    const val = communicateDispatchStore.curActiveDesktop;
    if (val === 'CommunicateDesktop') {
      activeId.value = 'GroupList';
      showChildMessage({ id: 'GroupList' });
    } else if (val === 'MonitorDesktop') {
      topTabClick({ id: 'monitor' });
    } else {
      activeId.value = 'ResourceTree';
      showChildMessage({ id: 'ResourceTree' });
    }
  }

  function topTabClick(data) {
    if (data?.id) {
      activeTab.value = data.id;
      emit('tabClick', data.id);
    }
  }
</script>

<template>
  <TdFrameBox
    class="resource-list-wrapper"
    :class="{
      'list-index-max': !isCommPanel,
    }"
    :dragger="false"
    :show-close-btn="showCloseBtn"
    :title="t('homePage.navigateData.resource')"
    @close-frame-box="closeResourceBox"
  >
    <!-- 最上层Tab -->
    <TdTab
      v-show="showTopTab"
      class="top-tab"
      :data="topTabData"
      :default-value="activeTab"
      tab-type="card"
      @click="topTabClick"
    />

    <!-- 资源 -->
    <div
      v-show="showResource"
      class="resource-body"
      :class="{
        'resource-body-short': showTopTab,
      }"
    >
      <!-- 资源Tab -->
      <TdTab :data="showTabData" :default-value="activeId" @click="showChildMessage" />

      <!-- 设备统计 -->
      <EquipmentCount v-show="activeId === 'ResourceTree'" @change="countChange" />

      <!-- 资源搜索 -->
      <ResourceSearch
        :active-id="activeId"
        :can-drag="true"
        :class="{ 'resource-search': showSearchResult }"
        :default-check-keys="checkKeys"
        :no-map="true"
        :operation="true"
        :placeholder="t('resource.policeResourceData.searchPlaceHolder')"
        :show-plan="activeId === 'PlanTree'"
        @change="searchChange"
        @check-change="checkChange"
        @click="handleNodeClick"
        @dblclick="handleNodeDblclick"
      >
        <template #suffix>
          <PimSearchSlot v-if="activeId !== 'PlanTree'" />
        </template>
      </ResourceSearch>

      <!-- 资源树 -->
      <KeepAlive>
        <component
          :is="currentComponent"
          v-show="!showSearchResult"
          ref="treeRef"
          :active-count="activeCount"
          :auto-update="true"
          :can-drag="true"
          :category="category"
          class="resource-content"
          :default-check-keys="checkKeys"
          :no-map="true"
          :operation="true"
          :show-check-box="true"
          :show-collect="showCollect"
          :show-pim="true"
          @check-change="checkChange"
          @click="handleNodeClick"
          @dblclick="handleNodeDblclick"
        />
      </KeepAlive>

      <!-- 一键按钮 -->
      <QuickOperate v-show="showQuickOperate" />
    </div>

    <VideoPolling v-if="activeTab === 'polling'" class="polling" from-list />
    <MrsSearch v-if="isMonitorScreen && activeTab === 'history'" />
    <PlanInfo v-if="activeTab === 'plan'" />
  </TdFrameBox>
</template>

<style lang="less" scoped>
  .resource-list-wrapper {
    position: relative;
    z-index: 2;
    box-sizing: border-box;
    display: flex;
    flex-direction: column;
    width: 412px;
    height: 100%;

    &.list-index-max {
      z-index: 100 !important;
    }

    :deep(.frame-box-container) {
      padding: 0;
    }

    .top-tab {
      margin: 10px 8px 0;
    }

    .resource-body {
      display: flex;
      flex-direction: column;
      width: 100%;
      height: 100%;
      padding: 10px 8px 8px;

      .resource-search {
        flex: 1;
        height: 0;
      }

      .resource-content {
        flex: 1;
        height: 0;
      }
    }

    .resource-body-short {
      height: calc(100% - 44px);
      padding-top: 0;
    }

    .polling {
      position: relative;
      top: 0;
      left: 0;
      width: 100%;
      height: calc(100% - 44px);
      background: none;
      border: none;
      box-shadow: none;

      :deep(.frame-box-header) {
        display: none;
      }

      :deep(.frame-box-container) {
        height: 100%;
        padding-bottom: 10px;
      }
    }
  }
</style>
