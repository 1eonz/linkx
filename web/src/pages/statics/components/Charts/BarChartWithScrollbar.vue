<script setup lang="ts">
  import { computed, ref, watch } from 'vue';

  import BarChart from './BarChart.vue';

  interface Props {
    barWidth?: number | string;
    colors?: {
      background?: string;
      bar?: string;
      label?: string;
    };
    data: {
      series: number[];
      xAxis: string[];
    };
    height?: string;
    isShowAll?: boolean;
    maxLabelLength?: number;
    showLabel?: boolean;
    title?: string;
    unit?: string;
    width?: string;
  }

  const props = withDefaults(defineProps<Props>(), {
    barWidth: '60%',
    colors: () => ({
      background: 'rgba(246,248,250,0.8)',
      bar: '#1B61F0',
      label: '#FF7A7A',
    }),
    height: '100%',
    isShowAll: false,
    maxLabelLength: 10,
    showLabel: true,
    title: '',
    unit: '',
    width: '100%',
  });

  // 范围控制
  const minVisible = 1; // 最少显示1条
  const defaultVisible = 10; // 默认显示10条
  const showScrollbarThreshold = 10; // 超过10条才显示滚动条

  const startIndex = ref(0);
  const endIndex = ref(defaultVisible - 1);
  const showScrollbar = computed(() => props.data.xAxis.length > showScrollbarThreshold);
  const totalCount = computed(() => props.data.xAxis.length);
  // 最多显示数量：动态设置为数据总量，但不超过100
  const maxVisible = computed(() => Math.min(totalCount.value, 100));

  // 当前显示的数据
  const displayData = computed(() => {
    if (!showScrollbar.value) {
      return props.data;
    }

    const actualEnd = Math.min(endIndex.value, totalCount.value - 1);
    return {
      series: props.data.series.slice(startIndex.value, actualEnd + 1),
      xAxis: props.data.xAxis.slice(startIndex.value, actualEnd + 1),
    };
  });

  // 当前显示数量
  const visibleCount = computed(() => endIndex.value - startIndex.value + 1);

  // 左滑块位置百分比
  const leftPercent = computed(() => {
    if (totalCount.value <= 1) return 0;
    return (startIndex.value / (totalCount.value - 1)) * 100;
  });

  // 右滑块位置百分比
  const rightPercent = computed(() => {
    if (totalCount.value <= 1) return 100;
    return (endIndex.value / (totalCount.value - 1)) * 100;
  });

  // 拖拽状态
  const isDraggingLeft = ref(false);
  const isDraggingRight = ref(false);
  const isDraggingMiddle = ref(false);
  const dragStartX = ref(0);
  const dragStartStartIndex = ref(0);
  const dragStartEndIndex = ref(0);
  const scrollbarRef = ref<HTMLElement>();

  // 左滑块拖拽
  const handleLeftMouseDown = (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    isDraggingLeft.value = true;
    dragStartX.value = e.clientX;
    dragStartStartIndex.value = startIndex.value;

    document.addEventListener('mousemove', handleLeftMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  };

  const handleLeftMouseMove = (e: MouseEvent) => {
    if (!isDraggingLeft.value || !scrollbarRef.value) return;

    const deltaX = e.clientX - dragStartX.value;
    const scrollbarWidth = scrollbarRef.value.offsetWidth;
    const deltaPercent = (deltaX / scrollbarWidth) * 100;
    const deltaIndex = Math.round((deltaPercent / 100) * (totalCount.value - 1));

    const newStartIndex = dragStartStartIndex.value + deltaIndex;
    // 限制范围：不能小于0，不能超过endIndex - minVisible + 1
    const maxStart = endIndex.value - minVisible + 1;
    startIndex.value = Math.max(0, Math.min(newStartIndex, maxStart));
  };

  // 右滑块拖拽
  const handleRightMouseDown = (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    isDraggingRight.value = true;
    dragStartX.value = e.clientX;
    dragStartEndIndex.value = endIndex.value;

    document.addEventListener('mousemove', handleRightMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  };

  const handleRightMouseMove = (e: MouseEvent) => {
    if (!isDraggingRight.value || !scrollbarRef.value) return;

    const deltaX = e.clientX - dragStartX.value;
    const scrollbarWidth = scrollbarRef.value.offsetWidth;
    const deltaPercent = (deltaX / scrollbarWidth) * 100;
    const deltaIndex = Math.round((deltaPercent / 100) * (totalCount.value - 1));

    const newEndIndex = dragStartEndIndex.value + deltaIndex;
    // 限制范围：不能小于startIndex + minVisible - 1，不能超过totalCount - 1或startIndex + maxVisible - 1
    const minEnd = startIndex.value + minVisible - 1;
    const maxEnd = Math.min(totalCount.value - 1, startIndex.value + maxVisible.value - 1);
    endIndex.value = Math.max(minEnd, Math.min(newEndIndex, maxEnd));
  };

  // 中间区域拖拽（整体移动）
  const handleMiddleMouseDown = (e: MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    isDraggingMiddle.value = true;
    dragStartX.value = e.clientX;
    dragStartStartIndex.value = startIndex.value;
    dragStartEndIndex.value = endIndex.value;

    document.addEventListener('mousemove', handleMiddleMouseMove);
    document.addEventListener('mouseup', handleMouseUp);
  };

  const handleMiddleMouseMove = (e: MouseEvent) => {
    if (!isDraggingMiddle.value || !scrollbarRef.value) return;

    const deltaX = e.clientX - dragStartX.value;
    const scrollbarWidth = scrollbarRef.value.offsetWidth;
    const deltaPercent = (deltaX / scrollbarWidth) * 100;
    const deltaIndex = Math.round((deltaPercent / 100) * (totalCount.value - 1));

    const newStartIndex = dragStartStartIndex.value + deltaIndex;
    const newEndIndex = dragStartEndIndex.value + deltaIndex;
    const range = dragStartEndIndex.value - dragStartStartIndex.value;

    // 限制范围：整体不能超出边界
    if (newStartIndex < 0) {
      startIndex.value = 0;
      endIndex.value = range;
    } else if (newEndIndex > totalCount.value - 1) {
      endIndex.value = totalCount.value - 1;
      startIndex.value = totalCount.value - 1 - range;
    } else {
      startIndex.value = newStartIndex;
      endIndex.value = newEndIndex;
    }
  };

  const handleMouseUp = () => {
    isDraggingLeft.value = false;
    isDraggingRight.value = false;
    isDraggingMiddle.value = false;
    document.removeEventListener('mousemove', handleLeftMouseMove);
    document.removeEventListener('mousemove', handleRightMouseMove);
    document.removeEventListener('mousemove', handleMiddleMouseMove);
    document.removeEventListener('mouseup', handleMouseUp);
  };

  // 监听数据变化重置
  watch(
    () => props.data.xAxis.length,
    (newLength) => {
      startIndex.value = 0;
      endIndex.value = Math.min(defaultVisible - 1, newLength - 1);
    },
  );
</script>

<template>
  <div class="bar-chart-with-scrollbar">
    <!-- 图表区域 -->
    <div class="chart-area">
      <BarChart
        :bar-width="barWidth"
        :colors="colors"
        :data="displayData"
        :height="height"
        :is-show-all="true"
        :max-label-length="maxLabelLength"
        :show-label="showLabel"
        :title="title"
        :unit="unit"
        :width="width"
      />
    </div>

    <!-- 底部双滑块滚动条 -->
    <div v-if="showScrollbar" class="scrollbar-wrapper">
      <!-- 滚动条轨道 -->
      <div ref="scrollbarRef" class="scrollbar-track">
        <!-- 选中区域 -->
        <div
          class="scrollbar-range"
          :class="{ dragging: isDraggingMiddle }"
          :style="{
            left: `${leftPercent}%`,
            width: `${rightPercent - leftPercent}%`,
          }"
          @mousedown="handleMiddleMouseDown"
        >
          <!-- 左滑块 -->
          <div
            class="scrollbar-handle left-handle"
            :class="{ active: isDraggingLeft }"
            @mousedown="handleLeftMouseDown"
          >
            <div class="handle-dot"></div>
          </div>

          <!-- 右滑块 -->
          <div
            class="scrollbar-handle right-handle"
            :class="{ active: isDraggingRight }"
            @mousedown="handleRightMouseDown"
          >
            <div class="handle-dot"></div>
          </div>
        </div>
      </div>

      <!-- 数据范围提示 -->
      <div class="scrollbar-info">
        <span class="info-text">
          显示 {{ startIndex + 1 }}-{{ Math.min(endIndex + 1, totalCount) }} 条（共 {{ visibleCount }} 条），总计
          {{ totalCount }} 条
        </span>
      </div>
    </div>
  </div>
</template>

<style lang="less" scoped>
  .bar-chart-with-scrollbar {
    width: 100%;
    height: 100%;
    display: flex;
    flex-direction: column;

    .chart-area {
      flex: 1;
      min-height: 0;
    }

    .scrollbar-wrapper {
      padding: 10px 16px 4px;
      display: flex;
      flex-direction: column;
      gap: 6px;

      .scrollbar-track {
        height: 4px;
        background: rgba(0, 0, 0, 0.1);
        border-radius: 2px;
        position: relative;
        cursor: pointer;
        transition: background 0.2s;

        &:hover {
          background: rgba(0, 0, 0, 0.15);
        }

        .scrollbar-range {
          position: absolute;
          top: 0;
          height: 100%;
          background: linear-gradient(
            90deg,
            rgba(64, 158, 255, 0.3) 0%,
            rgba(64, 158, 255, 0.3) 100%
          );
          border-radius: 2px;
          cursor: move;
          transition: background 0.2s;

          &:hover {
            background: linear-gradient(
              90deg,
              rgba(64, 158, 255, 0.4) 0%,
              rgba(64, 158, 255, 0.4) 100%
            );
          }

          &.dragging {
            background: linear-gradient(
              90deg,
              rgba(64, 158, 255, 0.5) 0%,
              rgba(64, 158, 255, 0.5) 100%
            );
          }

          .scrollbar-handle {
            position: absolute;
            top: 50%;
            transform: translate(-50%, -50%);
            width: 8px;
            height: 8px;
            cursor: ew-resize;
            z-index: 10;

            .handle-dot {
              width: 100%;
              height: 100%;
              background: rgba(64, 158, 255, 0.6);
              border-radius: 100%;
              transition: all 0.2s;
            }

            &:hover .handle-dot {
              background: rgba(64, 158, 255, 0.9);
              box-shadow: 0 0 6px rgba(64, 158, 255, 0.6);
            }

            &.active .handle-dot {
              background: #409EFF;
              box-shadow: 0 0 8px rgba(64, 158, 255, 0.8);
            }

            &.left-handle {
              left: 0;
            }

            &.right-handle {
              left: 100%;
            }
          }
        }
      }

      .scrollbar-info {
        display: flex;
        justify-content: center;
        align-items: center;

        .info-text {
          font-size: 12px;
          color: #909399;
          background: rgba(0, 0, 0, 0.03);
          padding: 2px 10px;
          border-radius: 10px;
          white-space: nowrap;
          user-select: none;
          transition: all 0.2s;

          &:hover {
            background: rgba(64, 158, 255, 0.08);
            color: #606266;
          }
        }
      }
    }
  }

  // 暗色主题
  @media (prefers-color-scheme: dark) {
    .bar-chart-with-scrollbar {
      .scrollbar-wrapper {
        .scrollbar-track {
          background: rgba(255, 255, 255, 0.15);

          &:hover {
            background: rgba(255, 255, 255, 0.2);
          }

          .scrollbar-range {
            background: linear-gradient(
              90deg,
              rgba(64, 158, 255, 0.4) 0%,
              rgba(64, 158, 255, 0.4) 100%
            );

            &:hover {
              background: linear-gradient(
                90deg,
                rgba(64, 158, 255, 0.5) 0%,
                rgba(64, 158, 255, 0.5) 100%
              );
            }

            &.dragging {
              background: linear-gradient(
                90deg,
                rgba(64, 158, 255, 0.6) 0%,
                rgba(64, 158, 255, 0.6) 100%
              );
            }

            .scrollbar-handle .handle-dot {
              background: rgba(64, 158, 255, 0.7);
              border-radius: 1.5px;

              &:hover {
                background: rgba(64, 158, 255, 1);
                box-shadow: 0 0 8px rgba(64, 158, 255, 0.8);
              }
            }
          }
        }

        .scrollbar-info {
          .info-text {
            color: #a0a0a0;
            background: rgba(255, 255, 255, 0.08);

            &:hover {
              background: rgba(64, 158, 255, 0.15);
              color: #d0d0d0;
            }
          }
        }
      }
    }
  }
</style>
