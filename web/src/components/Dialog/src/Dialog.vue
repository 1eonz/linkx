<script lang="ts" setup>
  import type { Ref } from 'vue';
  import { computed, nextTick, onMounted, unref, watch } from 'vue';

  import { useDraggable } from '@/hooks';
  import { guid, isWebView2 } from '@/utils';
  import { isDef } from '@/utils/is';

  import { Dialog, setZIndex, zIndexSet } from './helper';

  const props = withDefaults(
    defineProps<{
      cid: string;
      content?: object; // 内容组件
      draggable: boolean; // 是否可以拖拽
      fullScreenZIndex?: 'auto' | 'fixed' | number;
      isFullScreen?: Ref;
      offset?: Ref<string[]>;
      onOpen?: Function;
      propsData?: any;
      shade?: boolean;
      zIndexDefault?: number;
    }>(),
    {
      cid: '',
      draggable: false,
      fullScreenZIndex: 'fixed',
    },
  );

  defineExpose({ setOffset });

  const uid = computed(() => props.propsData?.uid || `dialog${guid()}`);

  // WebView2 环境判断
  const isWebView2Env = isWebView2();

  const draggable = computed(() => props.draggable);
  const zIndex = computed(() => {
    if (props.zIndexDefault) {
      return props.zIndexDefault;
    }
    const { cid } = props;
    return 1000 + zIndexSet.value.indexOf(cid);
  });
  const dialogStyle = computed(() => {
    const ret = {
      zIndex: zIndex.value,
    };

    return ret;
  });
  const dialogClass = computed(() => {
    const { fullScreenZIndex, isFullScreen } = props;
    const ret = {
      'dialog-full-screen': isFullScreen?.value,
      'dialog-full-screen-fixed': isFullScreen?.value && unref(fullScreenZIndex) === 'fixed',
    };
    return ret;
  });

  useDraggable({ draggable, uid: unref(uid) });

  onMounted(() => {
    setOffset();
    props.onOpen?.();
  });

  watch(
    () => props.offset,
    () => {
      setOffset();
    },
    { deep: true },
  );

  function clickDialog() {
    setZIndex(props.cid, true);
  }

  function closeDialog() {
    Dialog(props.cid)?.close();
  }

  function setOffset() {
    nextTick(() => {
      const el = document.getElementById(unref(uid)) as HTMLElement;
      const { offset } = props;
      if (el) {
        if (unref(offset)?.length === 2) {
          const [left, top] = unref(offset) as string[];
          if (isDef(left)) el.style.left = left;
          if (isDef(top)) el.style.top = top;
          el.style.transform = 'translate(-50%, -50%)';
        } else {
          el.style.left = '50%';
          el.style.top = '50%';
          el.style.transform = 'translate(-50%, -50%)';
        }
      }
    });
  }
  function shadeHandle() {
    console.log('shade click');
  }
</script>

<template>
  <!-- WebView2 环境下禁用 teleport，避免焦点丢失导致弹窗无法显示 -->
  <teleport v-if="!isWebView2Env" to="body">
    <component
      :is="content"
      v-if="content"
      :id="uid"
      class="td-dialog"
      :class="dialogClass"
      :style="dialogStyle"
      tabindex="1"
      v-bind="propsData"
      @click="clickDialog"
      @close-dialog="closeDialog"
    />

    <!-- 遮罩 -->
    <div
      v-if="shade"
      class="dialog-shadow"
      :style="{ zIndex: zIndex - 1 }"
      @click="shadeHandle"
    ></div>
  </teleport>
  <!-- WebView2 环境下直接渲染，不使用 teleport -->
  <template v-else>
    <component
      :is="content"
      v-if="content"
      :id="uid"
      class="td-dialog"
      :class="dialogClass"
      :style="dialogStyle"
      tabindex="1"
      v-bind="propsData"
      @click="clickDialog"
      @close-dialog="closeDialog"
    />

    <!-- 遮罩 -->
    <div
      v-if="shade"
      class="dialog-shadow"
      :style="{ zIndex: zIndex - 1 }"
      @click="shadeHandle"
    ></div>
  </template>
</template>

<style lang="less">
  .td-dialog {
    position: fixed !important;
    left: 50%;
    top: 50%;
    transform: translate(-50%, -50%);
    z-index: 1000;

    .dragger {
      cursor: move;

      img {
        pointer-events: none !important;
      }
    }
  }

  .dialog-shadow {
    position: fixed;
    top: 0;
    left: 0;
    z-index: -1;
    width: 100vw;
    height: 100vh;
    background: #000;
    filter: alpha(opacity=60);
    opacity: 0.6 !important;
  }

  .dialog-full-screen {
    position: fixed !important;
    top: 0 !important;
    left: 0 !important;
    width: 100vw !important;
    height: 100vh !important;
    margin: 0 !important;
    transform: none !important;
  }

  .dialog-full-screen-fixed {
    z-index: 2000 !important;
  }
</style>
