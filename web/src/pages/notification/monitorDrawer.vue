<script setup lang="ts">
  import { computed, ref, unref } from 'vue';

  import MessageBox from '@/components/MessageBox';
  import { useI18n } from '@/hooks';
  import { useMonitorStore } from '@/store';

  import Draggable from 'vuedraggable';

  import MonitorContainer from './monitorContainer.vue';

  const { t } = useI18n();
  const monitorStore = useMonitorStore();

  const full = ref(false);
  const containerRef = ref();
  const listRef = ref();

  const monitorList = computed<any>(() => monitorStore.monitorDrawerData);

  function onFullScreen(data, id) {
    full.value = data;
    onclick(id);
  }

  function onclick(id) {
    listRef.value.querySelectorAll('.monitor-container').forEach((el) => {
      el.style.zIndex = el.id === id ? '10' : '0';
    });
  }

  function dragEnd() {
    monitorStore.setMonitorDrawerData(unref(monitorList));
  }

  async function handleClose() {
    const res = await MessageBox({ text: t('desktop.other.sureClose') });
    if (res) {
      monitorStore.clearMonitorDrawerData();
    }
  }
</script>

<template>
  <div
    v-show="monitorList.length > 0"
    id="monitorDrawer"
    class="monitor-drawer"
    :class="{
      'full-screen': full,
    }"
  >
    <div class="ground-glass header">
      <span>{{ t('resource.poll.videoMonitoring') }}</span>
      <Icon class="close-btn" name="close" @click="handleClose" />
    </div>

    <div ref="listRef" class="video-list">
      <Draggable draggable=".mover" item-key="account" :list="monitorList" @end="dragEnd">
        <template #item="{ element }">
          <MonitorContainer
            ref="containerRef"
            :drag-resize="true"
            :dragger="true"
            :info="element"
            :show-close="true"
            :show-header="true"
            @on-full-screen="onFullScreen"
            @onclick="onclick"
          />
        </template>
      </Draggable>
    </div>
  </div>
</template>

<style scoped lang="less">
  .monitor-drawer {
    position: absolute;
    top: 90px;
    right: 76px;
    z-index: 100;
    width: 368px;
    min-height: 1px;
    max-height: 798px;
    padding-right: 8px;
    overflow: hidden auto;

    .header {
      position: relative;
      display: flex;
      align-items: center;
      justify-content: space-between;
      width: 100%;
      height: 34px;
      padding: 0 10px 0 28px;
      background: url('@/assets/images/popup/frame_title_bg_normal.png') no-repeat;
      background-size: 100% 100%;

      .close-btn {
        cursor: pointer;
      }
    }

    .video-list {
      max-height: 850px;
      overflow: hidden auto;
    }
  }

  .full-screen {
    position: fixed;
    z-index: 2500;
    transform: none !important;
  }
</style>
