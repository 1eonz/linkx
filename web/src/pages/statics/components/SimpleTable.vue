<script lang="ts" setup>
  import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue';

  import { emojiList, emojiUrl } from '@/pages/coordination/emoji/emojiHelper';
  import EmojiView from '@/pages/coordination/emoji/emojiView.vue';

  // 统一管理各处延时取值（ms）
  // 用户手动滚动后禁用自动滚动的影响时长
  const SCROLL_DISABLE_DELAY = 300;
  // 窗口/容器尺寸变化后，等待布局稳定再重新评估滚动状态的防抖延时
  const RESIZE_DEBOUNCE_DELAY = 150;
  // 数据变化后重启自动滚动前等待 DOM 更新的延时
  const DATA_CHANGE_RESTART_DELAY = 50;

  interface TableColumn {
    align?: 'center' | 'left' | 'right';
    label: string;
    prop: string;
    render?: (value: any, row: any) => string;
    width?: number | string;
  }

  interface Props {
    autoScroll?: boolean;
    avatarColumns?: string[];
    border?: boolean;
    columns: TableColumn[];
    currentPage?: number;
    height?: number | string;
    pageSize?: number;
    scrollSpeed?: number;
    // 新增分页相关属性
    showPagination?: boolean;
    // 是否显示查看消息操作列
    showViewMessage?: boolean;
    // 状态标签列（已处理/未处理等）
    statusColumns?: string[];
    stripe?: boolean;
    tableData: any[];
    total?: number;
    // 查看消息操作列文案（支持按行动态计算）
    viewMessageText?: string;
    viewMessageTextFn?: (row: any) => string;
  }

  const props = withDefaults(defineProps<Props>(), {
    autoScroll: false,
    avatarColumns: () => [],
    border: false,
    currentPage: 1,
    height: '100%',
    pageSize: 10,
    scrollSpeed: 1,
    showPagination: false,
    showViewMessage: false,
    statusColumns: () => [],
    stripe: true,
    total: 0,
    viewMessageText: '查看',
  });

  // 定义分页事件
  const emit = defineEmits<{
    pageChange: [page: number, size: number];
    viewMessage: [row: any, event?: Event];
  }>();

  const getViewMessageText = (row: any) => {
    return props.viewMessageTextFn?.(row) ?? props.viewMessageText;
  };

  // 判断是否为头像列
  const isAvatarColumn = (prop: string) => {
    return props.avatarColumns.includes(prop);
  };

  // 判断是否为状态标签列
  const isStatusColumn = (prop: string) => {
    return props.statusColumns.includes(prop);
  };

  // 获取状态标签样式类
  const getStatusClass = (value: string) => {
    if (value === '已处理') return 'status-tag--processed';
    if (value === '未处理') return 'status-tag--unprocessed';
    return '';
  };

  const tableRef = ref<any>(null);
  let animationFrameId: null | number = null;
  let lastTimestamp = 0;
  const scrollStep = ref(props.scrollSpeed);
  const scrollPosition = ref(0);
  const isPaused = ref(false);
  const scrollWrapper = ref<HTMLElement | null>(null);
  const wrapperHeight = ref(0);
  const isHovering = ref(false);
  const hasData = ref(false);
  // 创建 emoji 列表
  const emojiMap = emojiList.map((i) => `[${i}]`);
  const disabled = ref(false);
  let scrollTimeout: null | number = null;
  // 容器尺寸变化监听
  let resizeObserver: null | ResizeObserver = null;
  let resizeDebounceTimer: null | number = null;
  // 当前已观察的目标元素（用于判断 scrollWrapper 是否变化，避免反复重建 observer）
  let observedTarget: { container: Element | null; wrapper: HTMLElement | null } | null = null;

  // 处理包含 emoji 的文本
  function processTooltipContent(text: any): string {
    if (text === undefined || text === null) return '';
    const displayText = text === 0 ? '0' : text;
    let result = '';
    const imgRegex = /(\[.*?\])/g;
    const matches = String(displayText)?.split(imgRegex) || [];

    matches.forEach((i: string) => {
      if (emojiMap.includes(i)) {
        const name = i.replace('[', '').replace(']', '');
        result += `<img src="${emojiUrl[name]}" class="emoji-img" style="width: 1em; height: 1em; vertical-align: middle;" />`;
      } else {
        const atRegex = /(:@.*?\])/g;
        const atMatches = i.split(atRegex) || [];
        atMatches.forEach((j: string) => {
          if (j.includes(':@')) {
            const inner = j.substring(1, j.length - 1);
            const atText = inner === '@all' ? '@所有人 ' : `${inner} `;
            result += `${atText}`;
          } else if (!j.includes('[@')) {
            result += j;
          }
        });
      }
    });

    return result;
  }

  // 检查是否需要双倍数据
  const needsDuplicateData = computed(() => {
    if (!props.autoScroll) return false;

    if (props.tableData.length === 0) return false;

    const estimatedRowHeight = 42;
    const estimatedContentHeight = props.tableData.length * estimatedRowHeight;

    const containerHeight = typeof props.height === 'number' ? props.height : wrapperHeight.value;

    return estimatedContentHeight > containerHeight;
  });

  // 根据autoScroll状态和悬停状态处理数据
  const processedData = computed(() => {
    if (!props.autoScroll || props.tableData.length === 0) {
      hasData.value = props.tableData.length > 0;
      return props.tableData;
    }

    if (needsDuplicateData.value && !isHovering.value) {
      hasData.value = true;
      return [...props.tableData, ...props.tableData];
    }

    hasData.value = props.tableData.length > 0;
    return props.tableData;
  });

  // 处理滚动事件
  const handleScroll = () => {
    disabled.value = true;

    if (scrollTimeout) {
      clearTimeout(scrollTimeout);
    }

    scrollTimeout = window.setTimeout(() => {
      disabled.value = false;
    }, SCROLL_DISABLE_DELAY);
  };

  // 处理容器尺寸变化：重新评估是否需要复制数据和滚动
  const handleResize = () => {
    if (resizeDebounceTimer) {
      clearTimeout(resizeDebounceTimer);
    }
    resizeDebounceTimer = window.setTimeout(() => {
      if (!scrollWrapper.value || !props.autoScroll || props.tableData.length === 0) {
        return;
      }

      // 更新容器高度，触发 needsDuplicateData 重新计算
      const oldNeedsDuplicate = needsDuplicateData.value;
      wrapperHeight.value = scrollWrapper.value.clientHeight;
      const newNeedsDuplicate = needsDuplicateData.value;

      // 状态发生切换时调整滚动
      if (oldNeedsDuplicate !== newNeedsDuplicate) {
        stopAutoScroll();
        scrollPosition.value = 0;
        scrollWrapper.value.scrollTop = 0;

        // 数据集变化（复制或还原）需要等 DOM 更新后再启动滚动
        // 放宽重启条件：即使 newNeedsDuplicate 为 false 也重启
        // 1. 行高估算偏差（实际 > 42px）会导致 needsDuplicateData 误判为 false，
        //    若不重启将永久停止滚动；
        // 2. smoothScroll 已支持无复制数据的滚动分支（滚到底回顶），
        //    内容完全容纳时重启也不会产生视觉副作用。
        nextTick(() => {
          if (!isHovering.value && hasData.value) {
            startAutoScroll();
          }
        });
      } else if (newNeedsDuplicate) {
        // 仍需滚动：修正越界的滚动位置，避免视觉错位
        const scrollHeight = scrollWrapper.value.scrollHeight;
        const dataHeight = scrollHeight / 2;
        if (scrollPosition.value >= dataHeight) {
          scrollPosition.value -= dataHeight;
          scrollWrapper.value.scrollTop = scrollPosition.value;
        }
      }
    }, RESIZE_DEBOUNCE_DELAY);
  };

  // 初始化滚动容器并获取高度
  const initScrollWrapper = () => {
    nextTick(() => {
      removeEventListeners();

      scrollWrapper.value =
        tableRef.value?.$el.querySelector('.el-scrollbar__wrap') ||
        tableRef.value?.$el.querySelector('.el-table__body-wrapper');

      if (scrollWrapper.value) {
        wrapperHeight.value = scrollWrapper.value.clientHeight;
        initEventListeners();

        nextTick(() => {
          if (scrollWrapper.value) {
            if (!isHovering.value && needsDuplicateData.value) {
              scrollPosition.value = 0;
              scrollWrapper.value.scrollTop = 0;
            }

            if (!needsDuplicateData.value) {
              scrollPosition.value = 0;
              scrollWrapper.value.scrollTop = 0;
            }
          }
        });
      }
    });
  };

  // 平滑滚动函数
  const smoothScroll = (timestamp: number) => {
    if (!lastTimestamp) lastTimestamp = timestamp;
    const elapsed = timestamp - lastTimestamp;
    lastTimestamp = timestamp;

    if (
      !scrollWrapper.value ||
      !props.autoScroll ||
      processedData.value.length === 0 ||
      isHovering.value
    ) {
      animationFrameId = requestAnimationFrame(smoothScroll);
      return;
    }

    const scrollHeight = scrollWrapper.value.scrollHeight;
    const clientHeight = scrollWrapper.value.clientHeight;

    const distance = (scrollStep.value * elapsed) / 16;

    scrollPosition.value += distance;
    if (needsDuplicateData.value && processedData.value.length > props.tableData.length) {
      const dataHeight = scrollHeight / 2;

      if (scrollPosition.value >= dataHeight) {
        scrollPosition.value -= dataHeight;
        scrollWrapper.value.scrollTop = scrollPosition.value;
      } else {
        scrollWrapper.value.scrollTop = scrollPosition.value;
      }
    } else {
      if (scrollPosition.value > scrollHeight - clientHeight) {
        scrollPosition.value = 0;
      }
      scrollWrapper.value.scrollTop = scrollPosition.value;
    }

    animationFrameId = requestAnimationFrame(smoothScroll);
  };

  const startAutoScroll = () => {
    if (animationFrameId || !hasData.value) return;
    lastTimestamp = 0;
    scrollPosition.value = 0;
    if (scrollWrapper.value) {
      scrollWrapper.value.scrollTop = 0;
    }
    animationFrameId = requestAnimationFrame(smoothScroll);
  };

  const stopAutoScroll = () => {
    if (animationFrameId) {
      cancelAnimationFrame(animationFrameId);
      animationFrameId = null;
    }
  };

  // 处理鼠标悬停
  const handleMouseEnter = () => {
    isPaused.value = true;
    isHovering.value = true;
    disabled.value = false;

    if (scrollWrapper.value && needsDuplicateData.value) {
      scrollWrapper.value.scrollTop = 0;
    }
  };

  const handleMouseLeave = (event: MouseEvent) => {
    const container = scrollWrapper.value?.closest('.auto-scroll-table-container');
    if (!container) return;

    const relatedTarget = event.relatedTarget as HTMLElement;
    
    // 检查是否仍在滚动区域
    const isOverScrollbar = 
      relatedTarget?.closest('.el-scrollbar__bar') !== null ||
      relatedTarget?.closest('.el-scrollbar__thumb') !== null;

    if (isOverScrollbar) return; // 在滚动条上时不触发离开

    // 真正离开容器时执行逻辑
    isPaused.value = false;
    isHovering.value = false;
    disabled.value = true;

    if (scrollWrapper.value && needsDuplicateData.value) {
      scrollPosition.value = 0;
      scrollWrapper.value.scrollTop = 0;
    }
  };

  const handleTableMouseEnter = () => {
    // 当鼠标进入表格内部时，不触发容器离开事件
    isHovering.value = true;
  };

  // const handleTableMouseLeave = (event: MouseEvent) => {
  //   // 检查是否离开整个容器范围
  //   const container = scrollWrapper.value?.closest('.auto-scroll-table-container');
  //   if (container && !container.contains(event.relatedTarget as Node)) {
  //     handleMouseLeave();
  //   }
  // };

  const handleTableMouseLeave = (event: MouseEvent) => {
    const container = scrollWrapper.value?.closest('.auto-scroll-table-container');
    if (!container) return;

    const relatedTarget = event.relatedTarget as HTMLElement;
    
    // 检查是否仍在滚动区域
    const isOverScrollbar = 
      relatedTarget?.closest('.el-scrollbar__bar') !== null ||
      relatedTarget?.closest('.el-scrollbar__thumb') !== null;

    if (isOverScrollbar) return;
    handleMouseLeave(event); 
  };

  // 初始化事件监听
  const initEventListeners = () => {
    if (scrollWrapper.value) {
      const container = scrollWrapper.value.closest('.auto-scroll-table-container');
      if (container) {
        container.addEventListener('mouseenter', handleMouseEnter);
        container.addEventListener('mouseleave', handleMouseLeave);
      }
      // 保留滚动事件监听
      scrollWrapper.value.addEventListener('scroll', handleScroll);
      scrollWrapper.value.style.pointerEvents = 'auto';

      // 监听滚动容器尺寸变化，触发重新评估滚动状态
      // 仅在 scrollWrapper 元素引用真正变化时才重建观察器，避免随数据轮询反复重建
      const observedWrapper = observedTarget?.wrapper;
      if (observedWrapper !== scrollWrapper.value) {
        if (resizeObserver) {
          resizeObserver.disconnect();
        } else {
          resizeObserver = new ResizeObserver(handleResize);
        }
        resizeObserver.observe(scrollWrapper.value);
        if (container) {
          resizeObserver.observe(container);
        }
        observedTarget = { wrapper: scrollWrapper.value, container };
      } else if (observedTarget && observedTarget.container !== container) {
        // wrapper 不变但容器变化：替换观察目标
        if (observedTarget.container) {
          resizeObserver?.unobserve(observedTarget.container);
        }
        if (container) {
          resizeObserver?.observe(container);
        }
        observedTarget.container = container;
      }
    }
  };

  // 移除事件监听（不清理 ResizeObserver，避免随数据轮询反复重建）
  const removeEventListeners = () => {
    if (scrollWrapper.value) {
      const container = scrollWrapper.value.closest('.auto-scroll-table-container');
      if (container) {
        container.removeEventListener('mouseenter', handleMouseEnter);
        container.removeEventListener('mouseleave', handleMouseLeave);
      }
      scrollWrapper.value.removeEventListener('scroll', handleScroll);
    }

    if (scrollTimeout) {
      clearTimeout(scrollTimeout);
      scrollTimeout = null;
    }

    if (resizeDebounceTimer) {
      clearTimeout(resizeDebounceTimer);
      resizeDebounceTimer = null;
    }
  };

  // 彻底销毁 ResizeObserver 与 window resize 监听（仅组件卸载时调用）
  const destroyResizeObserver = () => {
    if (resizeObserver) {
      resizeObserver.disconnect();
      resizeObserver = null;
    }
    observedTarget = null;
  };

  // 处理页码变化
  const handleCurrentChange = (page: number) => {
    emit('pageChange', page, props.pageSize);
  };

  // 处理每页条数变化
  const handleSizeChange = (size: number) => {
    emit('pageChange', 1, size);
  };

  // 监听autoScroll变化
  watch(
    () => props.autoScroll,
    (enabled) => {
      if (enabled && hasData.value) {
        startAutoScroll();
      } else {
        stopAutoScroll();
        scrollPosition.value = 0;
        if (scrollWrapper.value) {
          scrollWrapper.value.scrollTop = 0;
        }
      }
    },
    { immediate: true },
  );

  // 监听滚动速度变化
  watch(
    () => props.scrollSpeed,
    (newVal) => {
      scrollStep.value = Math.max(0.5, Math.min(newVal, 5));
    },
  );

  // 监听数据变化重置滚动
  watch(
    () => props.tableData,
    (newData, oldData) => {
      nextTick(() => {
        initScrollWrapper();

        if ((!oldData || oldData.length === 0) && newData && newData.length > 0) {
          scrollPosition.value = 0;
          if (scrollWrapper.value) {
            scrollWrapper.value.scrollTop = 0;
          }
        }

        if (props.autoScroll && newData && newData.length > 0) {
          stopAutoScroll();
          setTimeout(() => {
            if (needsDuplicateData.value) {
              startAutoScroll();
            }
          }, DATA_CHANGE_RESTART_DELAY);
        } else {
          stopAutoScroll();
          scrollPosition.value = 0;
          if (scrollWrapper.value) {
            scrollWrapper.value.scrollTop = 0;
          }
        }
      });
    },
    { deep: true },
  );

  onMounted(() => {
    nextTick(() => {
      initScrollWrapper();
      if (props.autoScroll && hasData.value) {
        startAutoScroll();
      }
    });
  });

  onBeforeUnmount(() => {
    stopAutoScroll();
    removeEventListeners();
    destroyResizeObserver();
  });
</script>

<template>
  <div
    class="auto-scroll-table-container"
    :style="{ height: typeof height === 'number' ? `${height}px` : height }"
    @mouseenter="handleMouseEnter"
    @mouseleave="handleMouseLeave"
  >
    <el-table
      ref="tableRef"
      :border="border"
      :data="processedData"
      :height="showPagination ? undefined : height"
      :stripe="stripe"
      style="width: 100%; overflow: hidden"
      @mouseenter.native.stop="handleTableMouseEnter"
      @mouseleave.native.stop="handleTableMouseLeave"
    >
      <el-table-column
        v-for="column in columns"
        :key="column.prop"
        :align="column.align || 'left'"
        :label="column.label"
        :prop="column.prop"
        :width="column.width"
      >
        <template #default="{ row }">
          <div class="cell-content-wrapper">
            <!-- 头像图标 -->
            <TdAvatar
              v-if="row[column.prop] && isAvatarColumn(column.prop)"
              class="avatar"
              :url="row[column.prop]"
            />
            <!-- 状态标签（不带 TdTooltip） -->
            <div v-else-if="isStatusColumn(column.prop)" class="cell-content">
              <div
                class="status-tag"
                :class="getStatusClass(column.render ? column.render(row[column.prop], row) : row[column.prop])"
              >
                {{ column.render ? column.render(row[column.prop], row) : row[column.prop] }}
              </div>
            </div>
            <!-- 文本内容 -->
            <TdTooltip
              v-else
              :content="
                processTooltipContent(
                  column.render ? column.render(row[column.prop], row) : row[column.prop],
                )
              "
              placement="top"
            >
              <div class="cell-content">
                <EmojiView v-if="!column.render" :is-list="true" :text="row[column.prop]" />
                <span v-else>{{ column.render(row[column.prop], row) }}</span>
              </div>
            </TdTooltip>
          </div>
        </template>
      </el-table-column>

      <!-- 操作列：查看消息 -->
      <el-table-column
        v-if="showViewMessage"
        label="操作"
        align='center'
        width="80"
        fixed='right'
      >
        <template #default="{ row }">
          <div class="cell-content-wrapper action-cell-wrapper">
            <span class="view-message-link" @click.stop="emit('viewMessage', row, $event)">
              {{ getViewMessageText(row) }}
            </span>
          </div>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页组件 -->
    <div v-if="showPagination" class="pagination-container">
      <el-pagination
        :background="true"
        :current-page="currentPage"
        layout="total, prev, pager, next, jumper"
        :page-size="pageSize"
        :small="true"
        :total="total"
        @current-change="handleCurrentChange"
        @size-change="handleSizeChange"
      />
    </div>
  </div>
</template>

<style scoped lang="less">
  .auto-scroll-table-container {
    position: relative;
    display: flex;
    flex-direction: column;
    width: 100%;
    overflow: hidden;

    /* 阻止子元素触发父元素事件 */
    > * {
      pointer-events: none;
    }
    

    .pagination-container,
      .pagination-container > * {
      pointer-events: auto;
    }

    /* 允许滚动区域触发事件 */
    .el-scrollbar__wrap {
      pointer-events: auto;
    }
  }

  // 分页容器样式
  .pagination-container {
    display: flex;
    flex-shrink: 0;
    justify-content: center;
    padding: 10px 0;
    background-color: var(--table-body-bg);

    :deep(.el-pagination) {
      color: var(--text-color);

      .btn-prev,
      .btn-next,
      .el-pager li {
        color: var(--text-color);
        background-color: var(--table-body-cell);
        border: 1px solid var(--el-border-color);
      }

      .btn-prev:disabled,
      .btn-next:disabled {
        color: var(--el-text-color-placeholder);
      }

      .el-pager li.active {
        color: var(--el-color-primary);
        border-color: var(--el-color-primary);
      }

      .el-pagination__total,
      .el-pagination__jump {
        color: var(--text-color);
      }

      .el-input__inner {
        color: var(--text-color);
        background-color: var(--table-body-cell);
        border: 1px solid var(--el-border-color);
      }
    }
  }

  :deep(.el-table .cell) {
    padding: 0 2px !important;
  }

  .cell-content-wrapper {
    display: flex;
    gap: 5px;
    align-items: center;

    .avatar {
      flex-shrink: 0;
      width: 20px;
      height: 20px;
      margin: 0 auto;
    }

    .cell-content {
      display: inline-block;
      flex: 1;
      width: 100%;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
      vertical-align: middle;
    }

    // 操作列居中样式
    &.action-cell-wrapper {
      justify-content: center;
    }
  }

  .view-message-link {
    color: var(--el-color-primary) !important;
    cursor: pointer;
    transition: color 0.3s;
    font-weight: 600;
    padding: 0 2px;
    border-radius: 0;
  }

  // 状态标签样式
  .status-tag {
    display: inline-block;
    // height: 22px;
    line-height: 22px;
    padding: 0 8px;
    border-radius: 4px;
    font-size: 14px;
    font-weight: 400;
  }

  .status-tag--processed {
    background: rgba(216, 255, 215, 0.5);
    color: rgba(44, 176, 48, 1);
    border: 1px solid rgba(177, 255, 94, 1);
  }

  .status-tag--unprocessed {
    border: 1px solid rgba(255, 0, 0, 0.24);
    background: rgba(248, 23, 23, 0.07);
    color: rgba(255, 65, 65, 1);
  }

  /* 确保表格高度生效 */
  :deep(.el-table) {
    flex: 1;
    height: 100%;
    background-color: transparent !important;
  }

  :deep(.el-table tr) {
    background-color: transparent !important;
  }

  :deep(.el-scrollbar__wrap) {
    overflow-x: hidden;
    overflow-y: scroll !important;
  }

  :deep(.el-scrollbar__wrap),
  :deep(.el-table__body-wrapper) {
    pointer-events: auto;
  }

  :deep(.el-table th.el-table__cell .cell) {
    color: var(--table-title-text) !important;
  }

  :deep(thead .el-table__cell) {
    background: var(--table-title-bg) !important;
  }

  :deep(.el-table__body) {
    width: 100% !important;
  }

  :deep(.el-table__row--striped) {
    background-color: var(--table-title-bg) !important;
  }

  :deep(.el-table__empty-block) {
    background-color: var(--table-body-bg) !important;
  }

  :deep(.el-table__inner-wrapper::before) {
    background-color: var(--table-body-bg) !important;
  }

  :deep(.el-table__empty-block .el-table__empty-text) {
    color: var(--text-color) !important;
  }

  :deep(.el-table--striped .el-table__body tr.el-table__row--striped td.el-table__cell) {
    background-color: var(--table-body-striped) !important;
  }

  :deep(.el-table__body tr.el-table__row td.el-table__cell) {
    background-color: var(--table-body-cell) !important;
  }

  :deep(.el-scrollbar) {
    background-color: var(--table-body-cell) !important;
  }

  :deep(.el-table td.el-table__cell .cell) {
    color: var(--text-color) !important;
  }

  :deep(.el-table__header) {
    width: 100% !important;
  }

  :deep(.el-table__cell) {
    border: none !important;
  }

  // :deep(.el-table) {
  //   --el-table-border-color: #fff !important;
  // }

  .cell-content {
    display: inline-block;
    width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    vertical-align: middle;
  }

  :deep(.emoji-view) {
    display: inline-block;
    width: 100%;
    height: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
</style>