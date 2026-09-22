<script setup lang="ts">
  import { nextTick, onMounted, onUnmounted, ref, watch } from 'vue';

  import { getCollaborationStatistics } from '@/api/statics';
  import { useDC, useEmitter } from '@/hooks';

  import StatCardItem from './StatCardItem.vue';
  // import DC from '@/utils/DC';

  interface StatCardItem {
    color?: string;
    count: number;
    icon?: string;
    tagName: string;
  }
  const props = defineProps<{
    refreshTagStatistics: number;
  }>();

  watch(
    () => props.refreshTagStatistics,
    () => {
      getStatistics(); // 任何变化都触发刷新
    },
  );
  const emitter = useEmitter();
  // const originalOnMessage = DC.onmessage;

  const statCards = ref<StatCardItem[]>([]);
  let statisticsTimer: null | ReturnType<typeof setInterval> = null;
  const titleContainerRef = ref<HTMLElement | null>(null);

  // 滚动条相关状态
  const showScrollbar = ref(false);
  const thumbWidth = ref(0);
  const thumbPosition = ref(0);

  // 是否需要显示箭头和滚动条（基于内容是否溢出）
  const shouldShowArrows = ref(false);

  // ResizeObserver 用于监听容器宽度变化
  let resizeObserver: ResizeObserver | null = null;

  // 获取统计数据
  async function getStatistics() {
    try {
      const res = await getCollaborationStatistics();
      if (res && res.data) {
        statCards.value = (res.data as StatCardItem[]).filter(
          (item) => item.tagName !== '未设置标签',
        );
        // 更新滚动条状态
        nextTick(() => {
          if (titleContainerRef.value) {
            updateScrollbar(titleContainerRef.value);
          }
        });
      }
    } catch (error) {
      console.error('获取统计数据失败:', error);
    }
  }

  // 更新滚动条状态和箭头显示
  const updateScrollbar = (container: HTMLElement) => {
    if (!container) {
      shouldShowArrows.value = false;
      showScrollbar.value = false;
      return;
    }

    const { clientWidth, scrollLeft, scrollWidth } = container;

    // 基于实际内容宽度判断是否需要显示箭头和滚动条
    shouldShowArrows.value = scrollWidth > clientWidth;
    showScrollbar.value = shouldShowArrows.value;

    if (showScrollbar.value) {
      // 计算滑块宽度（基于2%容器宽度的比例）
      const scrollbarContainerWidth = clientWidth * 0.02;
      thumbWidth.value = Math.max((clientWidth / scrollWidth) * scrollbarContainerWidth, 20);

      // 计算滑块位置
      const maxScroll = scrollWidth - clientWidth;
      const scrollRatio = scrollLeft / maxScroll;
      const maxThumbPosition = scrollbarContainerWidth - thumbWidth.value;
      thumbPosition.value = scrollRatio * maxThumbPosition;
    }
  };

  // 拖动相关状态
  const isDragging = ref(false);
  const startX = ref(0);
  const scrollLeft = ref(0);
  // 启动滚动更新监听
  let scrollUpdateTimer: null | number = null;

  // 鼠标按下事件
  const onMouseDown = (e: MouseEvent) => {
    if (!titleContainerRef.value || !shouldShowArrows.value) return;

    isDragging.value = true;
    startX.value = e.pageX - titleContainerRef.value.offsetLeft;
    scrollLeft.value = titleContainerRef.value.scrollLeft;
    titleContainerRef.value.style.cursor = 'grabbing';
    titleContainerRef.value.style.userSelect = 'none';

    // 添加滚动事件监听
    titleContainerRef.value.addEventListener('scroll', handleScroll);
  };

  // 鼠标移动事件
  const onMouseMove = (e: MouseEvent) => {
    if (!isDragging.value || !titleContainerRef.value || !shouldShowArrows.value) return;
    e.preventDefault();
    const x = e.pageX - titleContainerRef.value.offsetLeft;
    const walk = (x - startX.value) * 2; // 拖动速度系数
    titleContainerRef.value.scrollLeft = scrollLeft.value - walk;
  };

  // 鼠标松开事件
  const onMouseUp = () => {
    isDragging.value = false;
    if (titleContainerRef.value) {
      titleContainerRef.value.style.cursor = 'grab';
      titleContainerRef.value.style.removeProperty('user-select');
      titleContainerRef.value.removeEventListener('scroll', handleScroll);
    }
  };

  // 鼠标离开容器事件
  const onMouseLeave = () => {
    isDragging.value = false;
    if (titleContainerRef.value) {
      titleContainerRef.value.style.cursor = 'grab';
      titleContainerRef.value.style.removeProperty('user-select');
      titleContainerRef.value.removeEventListener('scroll', handleScroll);
    }
  };

  // 滚动处理函数
  const handleScroll = (e: Event) => {
    updateScrollbar(e.target as HTMLElement);
  };

  // 点击箭头滚动
  const scrollLeftCards = () => {
    if (titleContainerRef.value && shouldShowArrows.value) {
      titleContainerRef.value.scrollBy({ behavior: 'smooth', left: -300 });
      // 添加滚动监听以更新滚动条
      startScrollUpdate();
    }
  };

  const scrollRightCards = () => {
    if (titleContainerRef.value && shouldShowArrows.value) {
      titleContainerRef.value.scrollBy({ behavior: 'smooth', left: 300 });
      // 添加滚动监听以更新滚动条
      startScrollUpdate();
    }
  };

  const startScrollUpdate = () => {
    if (scrollUpdateTimer) {
      cancelAnimationFrame(scrollUpdateTimer);
    }

    const update = () => {
      if (titleContainerRef.value) {
        updateScrollbar(titleContainerRef.value);
        // 继续监听滚动直到停止
        scrollUpdateTimer = requestAnimationFrame(update);
      }
    };

    update();

    // 设置一个超时时间，避免长时间执行
    setTimeout(() => {
      if (scrollUpdateTimer) {
        cancelAnimationFrame(scrollUpdateTimer);
        scrollUpdateTimer = null;
      }
    }, 500);
  };

  // 添加滚动监听
  const addScrollListener = () => {
    if (titleContainerRef.value) {
      titleContainerRef.value.addEventListener('scroll', handleScroll);
    }
  };

  // 移除滚动监听
  const removeScrollListener = () => {
    if (titleContainerRef.value) {
      titleContainerRef.value.removeEventListener('scroll', handleScroll);
    }
  };
  useDC('GROUP_TAG', 'GROUP_TAG', (isShow) => {
    console.log('GROUP_TAG', isShow);
    getStatistics();
  });
  // 自定义消息处理
  // DC.onmessage = (message) => {
  //   // 调用原始处理逻辑
  //   if (originalOnMessage) {
  //     originalOnMessage.call(DC, message);
  //   }

  //   try {
  //     // 处理 ArrayBuffer 类型的消息
  //     if (message.data instanceof ArrayBuffer) {
  //       // 将 ArrayBuffer 转换为字符串查看内容
  //       const textDecoder = new TextDecoder('utf-8');
  //       const text = textDecoder.decode(message.data);

  //       // 解析为 JSON
  //       try {
  //         const data = JSON.parse(text);
  //         handleArchivingMessage(data);
  //       } catch (e) {
  //         console.log('接收到二进制消息:', text);
  //       }
  //     }
  //     // 处理文本类型的消息
  //     else if (typeof message.data === 'string') {
  //       // 解析 JSON 数据
  //       let data;
  //       try {
  //         data = JSON.parse(message.data);
  //       } catch (e) {
  //         return;
  //       }

  //       handleArchivingMessage(data);
  //     }
  //   } catch (e) {
  //     console.error('错误:', e);
  //   }
  // };
  // 处理归档相关消息的专门函数
  // const handleArchivingMessage = (data) => {
  //   // 编辑标签刷新统计数据
  //   if (data.module === 'GROUP_TAG' || data.notifyType === 'GROUP_TAG') {
  //     getStatistics();
  //   }
  // };

  onMounted(() => {
    getStatistics();
    emitter.on('refresh-tag-statistics', getStatistics);
    nextTick(() => {
      addScrollListener();
      // 初始化滚动条状态
      if (titleContainerRef.value) {
        updateScrollbar(titleContainerRef.value);
        // 设置 ResizeObserver 监听容器宽度变化
        resizeObserver = new ResizeObserver(() => {
          if (titleContainerRef.value) {
            updateScrollbar(titleContainerRef.value);
          }
        });
        resizeObserver.observe(titleContainerRef.value);
      }
    });
  });

  // 组件销毁时清理定时器和事件监听
  onUnmounted(() => {
    if (statisticsTimer) {
      clearInterval(statisticsTimer);
      statisticsTimer = null;
    }
    if (resizeObserver) {
      resizeObserver.disconnect();
      resizeObserver = null;
    }
    removeScrollListener();
    // DC.onmessage = originalOnMessage;
    emitter.off('refresh-tag-statistics', getStatistics);
  });
</script>

<template>
  <div class="stat-cards-container">
    <!-- 左箭头 -->
    <div v-show="shouldShowArrows" class="scroll-arrow left" @click="scrollLeftCards">
      <el-icon><ArrowLeft /></el-icon>
    </div>

    <!-- 卡片区域 -->
    <div
      ref="titleContainerRef"
      class="cards-container"
      :class="{ scrollable: shouldShowArrows }"
      @mousedown="onMouseDown"
      @mouseleave="onMouseLeave"
      @mousemove="onMouseMove"
      @mouseup="onMouseUp"
    >
      <el-card v-for="(item, index) in statCards" :key="index" class="stat-card" shadow="hover">
        <StatCardItem
          :color="item.color || 'rgba(64, 158, 255, 0.1)'"
          :icon="item.icon || 'fas fa-user'"
          :special-style="false"
          :title="item.tagName"
          type="SecondCards"
          :value="item.count"
        />
      </el-card>
    </div>

    <!-- 自定义滚动条指示器 -->
    <div v-show="showScrollbar && shouldShowArrows" class="custom-scrollbar">
      <div
        class="scrollbar-thumb"
        :style="{
          width: `${thumbWidth}px`,
          transform: `translateX(${thumbPosition}px)`,
        }"
      ></div>
    </div>

    <!-- 右箭头 -->
    <div v-show="shouldShowArrows" class="scroll-arrow right" @click="scrollRightCards">
      <el-icon><ArrowRight /></el-icon>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .stat-cards-container {
    position: relative;
    box-sizing: border-box;
    width: 100%;
    max-width: 100%;
    // overflow: hidden;

    .custom-scrollbar {
      position: relative;
      width: 2%;
      height: 6px;
      margin: 0 auto;
      background-color: #fff;
      border-radius: 3px;

      .scrollbar-thumb {
        position: absolute;
        height: 100%;
        background-color: rgb(38 78 209 / 100%);
        border-radius: 3px;
        transition: transform 0.1s ease-out;
      }
    }
  }

  /* 滚动箭头 */
  .scroll-arrow {
    position: absolute;
    z-index: 10;
    display: flex;
    flex-shrink: 0;
    align-items: center;
    justify-content: center;
    width: 30px;
    height: 30px;
    color: var(--arrow-color);
    cursor: pointer;
    background-color: var(--arrow-background);
    border: 1px solid #dcdfe6;
    border-radius: 50%;
    box-shadow: 0 2px 4px rgb(0 0 0 / 10%);
    transition: all 0.3s;

    &.left {
      top: calc((90px - 30px) / 2);
      left: 0;
    }

    &.right {
      top: calc((90px - 30px) / 2);
      right: 15px;
    }

    &:hover {
      color: #254dd1;
      background-color: #f2f7ff;
      border-color: #254dd1;
    }

    .el-icon {
      font-size: 16px;
    }
  }

  /* 卡片容器 */
  .cards-container {
    flex: 1;
    max-width: 100%;
    height: 90px;
    overflow-x: hidden; // 默认隐藏滚动条
    white-space: nowrap;
    cursor: default;
    scrollbar-width: none; /* Firefox */
    -ms-overflow-style: none; /* IE and Edge */

    &.scrollable {
      overflow-x: auto;
      cursor: grab;
    }

    /* 隐藏滚动条 - Chrome, Safari and Opera */
    &::-webkit-scrollbar {
      display: none;
    }

    /* 拖动时的样式 */
    &.scrollable:active {
      cursor: grabbing;
    }
  }

  /* 卡片样式 */
  .stat-card {
    box-sizing: border-box;
    display: inline-block;
    flex: 1 1 auto;
    width: 259px;
    margin-right: 15px;
    background-color: var(--background-white-color);
    border: 1px solid var(--el-card-border);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 4px 12px rgb(0 0 0 / 10%);
      transform: translateY(-5px);
    }

    :deep(.el-card__body) {
      padding: 15px;
      padding-left: 30px;
      background: var(--group-type);
    }
  }
</style>
