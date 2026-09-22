<script lang="ts" setup>
  import { computed, nextTick, onMounted, onUpdated, ref, unref } from 'vue';

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

  defineExpose({ getContainerHeight, updateScroll });

  const itemHeight = ref(1);
  const containerHeight = ref(0);
  const startIndex = ref(0);
  const translateY = ref(0);
  const containerRef = ref();

  const displayCount = computed(() => {
    return Math.floor(unref(containerHeight) / unref(itemHeight));
  });
  const virtualList = computed(() => {
    let start = unref(startIndex);
    // 如果列表播放到最后时切换全屏,则重新计算显示元素个数
    if (start + unref(displayCount) > props.data.length) {
      start = props.data.length - unref(displayCount);
    }
    const ret = props.data.slice(start, start + unref(displayCount));
    return ret || [];
  });
  const fillHeight = computed(() => unref(itemHeight) * unref(props.total));
  const transform = computed(() => `translateY(${unref(translateY) - unref(fillHeight)}px)`);

  onMounted(() => {
    getContainerHeight();
  });

  onUpdated(() => {
    getItemHeight();
  });

  function getContainerHeight() {
    containerHeight.value = unref(containerRef).offsetHeight;
  }

  function getItemHeight() {
    nextTick(() => {
      const item = document.querySelector('.virtual-scroll-list .virtual-item') as HTMLElement;
      if (item) {
        const h = item.offsetHeight;
        itemHeight.value = item.offsetHeight;
        if (h === 0) {
          setTimeout(() => getItemHeight, 500);
        }
      }
    });
  }

  function handleScroll(e) {
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
    const scrollTop = index * unref(itemHeight);
    containerRef.value.scrollTo(0, scrollTop);
  }
</script>

<template>
  <div ref="containerRef" class="virtual-scroll-list" @scroll="handleScroll">
    <div
      v-if="containerHeight !== 0"
      class="virtual-fill"
      :style="{ height: `${fillHeight}px` }"
    ></div>

    <ul class="virtual-list" :style="{ transform }">
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
