<template>
  <div class="scrollable-tabs" ref="tabsContainerRef">
    <!-- 左箭头 -->
    <div
      v-show="showLeftArrow"
      class="tabs-nav-prev"
      @click="scrollPrev"
    >
      <el-icon><ArrowLeft /></el-icon>
    </div>

    <!-- tabs 滚动容器 -->
    <div class="tabs-nav-scroll" ref="scrollRef">
      <div class="tabs-nav" ref="navRef">
        <div
          v-for="item in tabs"
          :key="item.id"
          class="tabs-item"
          :class="{ 'is-active': modelValue === item.id }"
          :ref="el => setTabRef(el, item.id)"
          @click="handleTabClick(item)"
        >
          <span>{{ item.name }}</span>
          <!-- badge 插槽 -->
          <slot name="badge" :tab="item" />
        </div>
      </div>
    </div>

    <!-- 右箭头 -->
    <div
      v-show="showRightArrow"
      class="tabs-nav-next"
      @click="scrollNext"
    >
      <el-icon><ArrowRight /></el-icon>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, nextTick, onMounted, onBeforeUnmount } from 'vue';
import { useResizeObserver } from '@vueuse/core';
import { ArrowLeft, ArrowRight } from '@element-plus/icons-vue';

interface TabItem {
  id: string;
  name: string;
  [key: string]: any;
}

interface Props {
  tabs: TabItem[];
  modelValue: string;
  scrollDistance?: number; // 每次滚动距离，默认 100px
}

const props = withDefaults(defineProps<Props>(), {
  scrollDistance: 100,
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  'tab-click': [tab: TabItem];
}>();

// refs
const tabsContainerRef = ref();
const scrollRef = ref();
const navRef = ref();
const tabRefs = ref<Map<string, HTMLElement>>(new Map());
const showLeftArrow = ref(false);
const showRightArrow = ref(false);

// 设置 tab ref
function setTabRef(el: any, id: string) {
  if (el) {
    tabRefs.value.set(id, el);
  }
}

// 更新箭头显示状态
function updateArrowVisibility() {
  if (!scrollRef.value || !navRef.value) return;

  const containerWidth = scrollRef.value.offsetWidth;
  const totalWidth = navRef.value.scrollWidth;

  // 判断是否需要显示箭头（只要容器不足以展示全部 tab 就一直显示）
  const needArrows = totalWidth > containerWidth;
  showLeftArrow.value = needArrows;
  showRightArrow.value = needArrows;
}

// 左箭头点击
function scrollPrev() {
  if (!scrollRef.value) return;

  const currentLeft = scrollRef.value.scrollLeft;
  const targetLeft = Math.max(0, currentLeft - props.scrollDistance);

  scrollRef.value.scrollTo({
    left: targetLeft,
    behavior: 'smooth'
  });
}

// 右箭头点击
function scrollNext() {
  if (!scrollRef.value) return;

  const currentLeft = scrollRef.value.scrollLeft;
  const maxLeft = scrollRef.value.scrollWidth - scrollRef.value.offsetWidth;
  const targetLeft = Math.min(maxLeft, currentLeft + props.scrollDistance);

  scrollRef.value.scrollTo({
    left: targetLeft,
    behavior: 'smooth'
  });
}

// 滚动到激活的 tab（带边缘判断）
function scrollToActiveTab() {
  nextTick(() => {
    if (!scrollRef.value || !navRef.value) return;

    const activeTabEl = tabRefs.value.get(props.modelValue);
    if (!activeTabEl) return;

    const containerWidth = scrollRef.value.offsetWidth;
    const scrollWidth = scrollRef.value.scrollWidth;
    const activeLeft = activeTabEl.offsetLeft;
    const activeWidth = activeTabEl.offsetWidth;

    // 计算居中位置
    let targetScrollLeft = activeLeft - (containerWidth / 2) + (activeWidth / 2);

    // 边缘判断：左边缘
    if (targetScrollLeft < 0) {
      targetScrollLeft = 0;
    }
    // 边缘判断：右边缘
    else if (targetScrollLeft > scrollWidth - containerWidth) {
      targetScrollLeft = scrollWidth - containerWidth;
    }

    scrollRef.value.scrollTo({
      left: targetScrollLeft,
      behavior: 'smooth'
    });
  });
}

// Tab 点击处理
function handleTabClick(item: TabItem) {
  if (item.id === props.modelValue) return; // 防止重复点击

  emit('update:modelValue', item.id);
  emit('tab-click', item);
}

// 监听滚动事件，更新箭头状态
function handleScroll() {
  updateArrowVisibility();
}

// 监听容器大小变化
useResizeObserver(tabsContainerRef, () => {
  updateArrowVisibility();
  scrollToActiveTab(); // 容器大小变化时重新居中
});

// 监听 tabs 数据变化
watch(() => props.tabs, () => {
  nextTick(() => {
    updateArrowVisibility();
    scrollToActiveTab();
  });
}, { deep: true });

// 监听激活 tab 变化
watch(() => props.modelValue, () => {
  scrollToActiveTab();
});

// 挂载时初始化
onMounted(() => {
  if (scrollRef.value) {
    scrollRef.value.addEventListener('scroll', handleScroll);
  }
  nextTick(() => {
    updateArrowVisibility();
    scrollToActiveTab();
  });
});

// 卸载时清理
onBeforeUnmount(() => {
  if (scrollRef.value) {
    scrollRef.value.removeEventListener('scroll', handleScroll);
  }
});

// 暴露方法（供父组件调用）
defineExpose({
  scrollToActiveTab,
  updateArrowVisibility,
});
</script>

<style scoped lang="less">
.scrollable-tabs {
  display: flex;
  align-items: center;
  height: 100%;
  width: 100%;
}

.tabs-nav-prev,
.tabs-nav-next {
  width: 24px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--tabs-color, #333);
  cursor: pointer;
  flex-shrink: 0;
  transition: color 0.3s;

  &:hover {
    color: var(--tabs-active-color, #1b61f0);
  }

  .el-icon {
    font-size: 14px;
  }
}

.tabs-nav-scroll {
  flex: 1;
  min-width: 0;
  overflow-x: auto;
  overflow-y: hidden;
  height: 100%;

  // 隐藏滚动条
  &::-webkit-scrollbar {
    display: none;
  }
  -ms-overflow-style: none;
  scrollbar-width: none;
}

.tabs-nav {
  display: flex;
  align-items: center;
  height: 100%;
  white-space: nowrap;
}

.tabs-item {
  padding: 0 16px;
  height: 100%;
  display: flex;
  align-items: center;
  color: var(--tabs-color, #333);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: color 0.3s;
  user-select: none;

  &:hover {
    color: var(--tabs-active-color, #1b61f0);
  }

  &.is-active {
    color: var(--tabs-active-color, #1b61f0);
  }
}
</style>
