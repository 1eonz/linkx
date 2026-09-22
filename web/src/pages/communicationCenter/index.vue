<script lang="ts" setup>
  import { onBeforeMount, ref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';
  import { useCommunicateDispatchStore } from '@/store';

  import DesktopContent from './desktopContent.vue';
  import ResourceList from './resourceList.vue';

  const communicateDispatchStore = useCommunicateDispatchStore();

  const isMonitor = ref(false);
  const activeType = ref('monitor');

  watch(
    () => communicateDispatchStore.curActiveDesktop,
    (val) => {
      isMonitor.value = val === 'MonitorDesktop';
    },
    { immediate: true },
  );

  onBeforeMount(() => {
    Dialog('conferenceCard')?.close();
  });

  function tabClick(val) {
    activeType.value = val;
  }
</script>

<template>
  <!-- 资源调度中心 -->
  <div class="communicate-dispatch">
    <!-- 左侧资源列表 -->
    <ResourceList :show-close-btn="false" :show-collect="true" @tab-click="tabClick" />
    <!-- 中间桌面部分 -->
    <DesktopContent :history="isMonitor && activeType === 'history'" :monitor-center="false" />
  </div>
</template>

<style lang="less" scoped>
  .all-resource-list {
    height: 100% !important;
  }

  .router-view {
    padding: 0 !important;
    margin: 40px;
  }

  .communicate-dispatch {
    position: relative;
    box-sizing: border-box;
    display: flex;
    width: 100%;
    height: 100%;
    padding: 20px 30px 10px 0;
  }
</style>
