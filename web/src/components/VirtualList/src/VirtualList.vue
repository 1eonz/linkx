<script lang="ts" setup>
  import {
    computed,
    nextTick,
    onActivated,
    onBeforeUnmount,
    onMounted,
    ref,
    unref,
    watch,
  } from 'vue';

  import { useWatchSizeChange } from '@/hooks';

  defineOptions({
    name: 'TdVirtualList',
  });

  const props = withDefaults(
    defineProps<{
      data: any[];
      total: number;
    }>(),
    {
      total: 0,
    },
  );

  const emit = defineEmits(['scroll']);

  defineExpose({ getContainerHeight, reset, updateScroll });

  const itemHeight = ref(1);
  const containerHeight = ref(0);
  const startIndex = ref(0);
  const translateY = ref(0);
  const containerRef = ref();
  let observer: ResizeObserver;

  const displayCount = computed(() => {
    return Math.floor(unref(containerHeight) / unref(itemHeight));
  });
  const virtualList = computed(() => {
    const start = unref(startIndex);
    const ret = props.data.slice(start, start + unref(displayCount));
    return ret || [];
  });
  const fillHeight = computed(() => unref(itemHeight) * unref(props.total));
  const transform = computed(() => `translateY(${unref(translateY) - unref(fillHeight)}px)`);
  const overflowY = computed(() => unref(fillHeight) > unref(containerHeight));

  watch(() => props.total, reset);

  onMounted(() => {
    getContainerHeight();
    getItemHeight();

    useWatchSizeChange(unref(containerRef), getContainerHeight);
    watchBodyHeight();
  });

  // 在组件被激活时重新计算位置
  onActivated(() => {
    nextTick(() => {
      getContainerHeight();
      getItemHeight();
      // 重新触发滚动事件以确保正确显示
      if (containerRef.value) {
        handleScroll({ target: containerRef.value });
      }
    });
  });

  onBeforeUnmount(() => {
    observer?.disconnect();
  });

  function getContainerHeight() {
    if (containerRef.value) {
      containerHeight.value = unref(containerRef).offsetHeight;
    }
  }

  function getItemHeight() {
    if (itemHeight.value > 1) {
      return;
    }

    nextTick(() => {
      const item = document.querySelector('.virtual-scroll-list .virtual-item') as HTMLElement;
      if (item) {
        const h = item.offsetHeight;
        itemHeight.value = item.offsetHeight;
        if (h === 0) {
          setTimeout(() => getItemHeight(), 500);
        }
      }
    });
  }

  function handleScroll(e) {
    // 确保元素存在
    if (!e || !e.target) return;

    // 触底就不再调用
    const el = e?.srcElement;
    if (el) {
      const { clientHeight, scrollHeight, scrollTop } = el;
      if (scrollTop + clientHeight >= scrollHeight) {
        return;
      }
    }

    const { scrollTop } = e.target;
    translateY.value = scrollTop;

    let start = Math.floor(scrollTop / unref(itemHeight));
    const maxStart = props.total - unref(displayCount);
    if (maxStart > 0 && start > maxStart) {
      start = maxStart;
    }

    startIndex.value = start;
    emit('scroll', start, e);
  }

  function updateScroll(index) {
    if (containerRef.value) {
      const scrollTop = index * unref(itemHeight);
      containerRef.value.scrollTo(0, scrollTop);
    }
  }

  function watchBodyHeight() {
    // 创建观察器
    observer = new ResizeObserver(getContainerHeight);

    // 开始观察文档根元素
    observer.observe(document.documentElement);

    // 需要停止监听时调用：
    // observer.disconnect();
  }

  function reset() {
    getContainerHeight();
    startIndex.value = 0;
    translateY.value = 0;
    if (containerRef.value) {
      containerRef.value.scrollTo(0, 0);
    }
  }
</script>
<template>
  <div ref="containerRef" class="virtual-scroll-list" @scroll="handleScroll">
    <div v-if="overflowY" class="virtual-fill" :style="{ height: `${fillHeight}px` }"></div>

    <ul class="virtual-list" :style="overflowY ? { transform } : {}">
      <li
        v-for="(item, index) in virtualList"
        :key="item"
        :class="`virtual-item ${startIndex + index}`"
      >
        <slot :item="item" name="item"></slot>
      </li>
    </ul>
  </div>
</template>

<style scoped lang="less">
  .virtual-scroll-list {
    width: 100%;
    height: 100%;
    overflow-y: scroll;

    .list {
      width: 100%;

      .item {
        width: 100%;
      }
    }
  }
</style>
