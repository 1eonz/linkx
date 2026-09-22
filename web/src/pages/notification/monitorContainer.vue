<script setup lang="ts">
  import { computed, nextTick, onBeforeMount, onMounted, ref, unref, watch } from 'vue';

  import { useDraggable, useDragResize } from '@/hooks';
  import MonitorCard from '@/pages/communicationCard/monitorCard/monitorCard.vue';
  import VideoDistributePerson from '@/pages/communicationCard/monitorCard/videoDistributePerson.vue';
  import { useCommunicationStore, useMonitorStore } from '@/store';
  import { addUnit, guid } from '@/utils';

  import { throttle } from 'lodash-es';

  const props = defineProps<{
    dragger?: boolean;
    dragResize?: boolean;
    info: any;
    showClose?: boolean;
    showHeader?: boolean;
  }>();

  const emit = defineEmits(['onFullScreen', 'onclick']);
  defineExpose({ changeCardSize });

  const communicationStore = useCommunicationStore();
  const monitorStore = useMonitorStore();

  const showDistribute = ref(false);
  const distributeListData = ref([]);
  const monitorRef = ref();
  const uid = `monitor${guid()}`;
  const contentUid = `monitorContent${guid()}`;
  const full = ref(false);
  const unMover = ref(false);
  const mousedown = ref(false);
  const draggable = ref(true);

  const account = computed(() => {
    const { account, code } = props.info;
    return account || code;
  });
  const canDrag = computed(() => {
    return !!props.dragResize && !full.value && unMover.value;
  });

  const handleRect = throttle((transform?: any) => {
    nextTick(() => {
      const drawerEl = document.querySelector('#monitorDrawer');
      const el = document.getElementById(uid);

      if (!drawerEl || !el) {
        return;
      }

      if (transform) {
        const { offsetX, offsetY } = transform;
        el!.style.transform = `translate(${addUnit(offsetX)}, ${addUnit(offsetY)})`;
      }

      const docWidth = document.documentElement.clientWidth;
      const targetRect = el.getBoundingClientRect();
      if (targetRect.right >= docWidth && !unref(full)) {
        el.style.position = 'relative';
        el.style.transform = '';

        const contentEl = document.getElementById(contentUid);
        contentEl!.style.width = '22.1875rem';
        contentEl!.style.height = '12.1875rem';
        unMover.value = false;
      } else {
        unMover.value = true;
      }
    });
  }, 500);

  useDraggable({
    callback: (mouseStatus, options) => {
      const drawerEl = document.getElementById('monitorDrawer') as any;
      const el = document.getElementById(uid) as any;

      switch (mouseStatus) {
        case 'mousedown': {
          emit('onclick', uid);

          if (!drawerEl || !el) {
            return;
          }

          unMover.value = false;

          if (!el.style.transform || el.style.transform === 'none') {
            const drawerRect = drawerEl.getBoundingClientRect();
            const targetRect = el.getBoundingClientRect();
            const drawerHeaderHeight = drawerEl.querySelector('.header')?.clientHeight || 0;
            const _offsetY = targetRect.top - drawerRect.top;
            el.style.transform = `translate(0, ${_offsetY - drawerHeaderHeight}px)`;
            el.style.position = 'fixed';
          }
          break;
        }
        case 'mousemove': {
          const { offsetX, offsetY } = options.transform;
          if (offsetX !== 0 || offsetY !== 0) {
            el.style.position = 'fixed';
          }
          break;
        }
        case 'mouseup': {
          const { offsetX, offsetY } = options.transform;
          if (offsetX === 0 && offsetY === 0) {
            el.style.transform = 'none';
            el.style.position = 'relative';
          }
          handleRect(options.transform);
          break;
        }
      }
    },
    draggable,
    uid,
  });

  useDragResize({
    callback: () => {
      handleRect();
      nextTick(() => {
        monitorRef.value?.handleSizeChange();
      });
    },
    canDrag,
    minH: 135,
    minW: 240,
    outsideUid: uid,
    uid: contentUid,
  });

  watch(communicationStore.distributeStatus, (val) => {
    const { isdn } = val.updateInfo;
    if (!isdn || isdn !== unref(account)) {
      return;
    }
    if (val[isdn]) {
      showDistribute.value = true;
      distributeListData.value = Object.values(val[isdn]);
    } else {
      showDistribute.value = false;
    }
  });

  onMounted(() => {
    window.addEventListener('mouseup', handleMouseup);
  });

  onBeforeMount(() => {
    window.removeEventListener('mouseup', handleMouseup);
  });

  function onFullScreen(data) {
    draggable.value = !data;
    full.value = data;
    emit('onFullScreen', data, uid);
  }

  function onClose(data) {
    monitorStore.deleteMonitorDrawerDataByAccount(data.account);
  }

  function changeCardSize() {
    (monitorRef.value as any)?.changeCardSize?.(false);
  }

  function handleMousedown() {
    mousedown.value = true;
  }

  function handleMouseup() {
    mousedown.value = false;
  }
</script>

<template>
  <div
    :id="uid"
    class="monitor-container"
    :class="{
      mover: !unMover,
      'monitor-container_full': full,
    }"
    @click="emit('onclick', uid)"
    @mousedown="handleMousedown"
  >
    <div :id="contentUid" class="monitor-content">
      <MonitorCard
        ref="monitorRef"
        :dragger="dragger && !full"
        :monitor-info="info"
        :mousedown="mousedown"
        :show-close="showClose"
        @on-close="onClose(info)"
        @on-full-screen="onFullScreen"
      />
    </div>

    <!-- 视频分发人员 -->
    <VideoDistributePerson
      v-show="showDistribute"
      v-model:person-list="distributeListData"
      class="distribute-person"
      :src-code="account"
    />
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .monitor-container {
    position: relative;
    float: left;
    margin: 2px 0;
    overflow: hidden;
    border: 1px solid rgb(25.5 255 251.175 / 100%);

    &_full {
      z-index: 10 !important;
      transform: none !important;
    }

    .monitor-content {
      box-sizing: border-box;
      width: 355px;
      height: 185px;
      border: 2px solid transparent;
    }
  }
</style>
