<script setup>
  import { ref, computed, watch, nextTick } from 'vue';
  import { ArrowDown, ArrowUp, Close, Check } from '@element-plus/icons-vue';

  const props = defineProps({
    modelValue: {
      type: [String, Number],
      default: undefined,
    },
    options: {
      type: Array,
      default: () => [],
    },
    placeholder: {
      type: String,
      default: '请选择',
    },
    clearable: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
    hasMore: {
      type: Boolean,
      default: true,
    },
  });

  const emit = defineEmits(['update:modelValue', 'change', 'loadMore']);

  const popoverVisible = ref(false);
  const listRef = ref();

  // 当前选中项的 label
  const currentLabel = computed(() => {
    const option = props.options.find((item) => item.value === props.modelValue);
    return option?.label || '';
  });

  // 是否显示清除图标
  const showClear = computed(() => {
    return props.clearable && props.modelValue !== undefined && props.modelValue !== '';
  });

  /**
   * 选择选项
   */
  function handleSelect(item) {
    emit('update:modelValue', item.value);
    emit('change', item.value);
    popoverVisible.value = false;
  }

  /**
   * 清除选中
   */
  function handleClear(e) {
    e.stopPropagation();
    emit('update:modelValue', '');
    emit('change', '');
  }

  /**
   * 滚动加载更多
   */
  function handleScroll() {
    if (!listRef.value || props.loading || !props.hasMore) return;

    const { scrollHeight, scrollTop, clientHeight } = listRef.value;
    // 距离底部 50px 时加载更多
    if (scrollHeight - scrollTop - clientHeight < 50) {
      emit('loadMore');
    }
  }

  // 监听 popover 显示，重置滚动位置
  watch(popoverVisible, (visible) => {
    if (visible) {
      nextTick(() => {
        if (listRef.value) {
          listRef.value.scrollTop = 0;
        }
      });
    }
  });
</script>

<template>
  <div class="custom-select">
    <ElPopover
      v-model:visible="popoverVisible"
      placement="bottom-start"
      trigger="click"
      :width="200"
      popper-class="custom-select-popper"
    >
      <!-- 下拉列表 -->
      <div
        ref="listRef"
        class="custom-select-dropdown"
        @scroll="handleScroll"
      >
        <div
          v-for="item in options"
          :key="item.value"
          class="custom-select-option"
          :class="{ 'is-selected': String(item.value) === String(modelValue) }"
          @click="handleSelect(item)"
        >
          <!-- 头像（如果存在） -->
          <img
            v-if="item.avatar"
            class="option-avatar"
            :src="item.avatar"
            :alt="`${item.label}的头像`"
          />
          <!-- 名称 -->
          <span class="option-label">{{ item.label }}</span>
          <!-- 选中状态 -->
          <ElIcon v-if="String(item.value) === String(modelValue)" class="option-check">
            <Check />
          </ElIcon>
        </div>
        <!-- 加载中 -->
        <div v-if="loading" class="custom-select-loading">
          加载中...
        </div>
        <!-- 无数据 -->
        <div v-if="!loading && options.length === 0" class="custom-select-empty">
          暂无数据
        </div>
      </div>

      <!-- 触发元素 -->
      <template #reference>
        <div class="custom-select-trigger" :class="{ 'is-focus': popoverVisible }">
          <span class="custom-select-value" :class="{ 'is-placeholder': !currentLabel }">
            {{ currentLabel || placeholder }}
          </span>
          <span class="custom-select-suffix">
            <ElIcon v-if="showClear" class="clear-icon" @click="handleClear">
              <Close />
            </ElIcon>
            <ElIcon v-else class="arrow-icon">
              <component :is="popoverVisible ? ArrowUp : ArrowDown" />
            </ElIcon>
          </span>
        </div>
      </template>
    </ElPopover>
  </div>
</template>

<style scoped lang="less">
  .custom-select {
    display: block;
    width: 100%;
  }

  .custom-select-trigger {
    display: flex;
    align-items: center;
    justify-content: space-between;
    width: 100%;
    height: 32px;
    padding: 0 8px;
    background: var(--background-white-color);
    border: 1px solid var(--border-color);
    border-radius: 4px;
    cursor: pointer;
    transition: border-color 0.2s;

    &:hover {
      border-color: var(--button-active-color);
    }

    &.is-focus {
      border-color: var(--button-active-color);
    }
  }

  .custom-select-value {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    color: var(--text-color);
    font-size: 14px;

    &.is-placeholder {
      color: var(--text-color);
      opacity: 0.6;
    }
  }

  .custom-select-suffix {
    display: flex;
    align-items: center;
    margin-left: 8px;
    color: var(--text-color);
  }

  .clear-icon {
    cursor: pointer;

    &:hover {
      color: var(--button-active-color);
    }
  }

  .arrow-icon {
    transition: transform 0.2s;
  }
</style>

<style lang="less">
  // popover 样式（非 scoped）
  .custom-select-popper {
    padding: 4px 0 !important;
    min-width: 200px !important;

    .el-popper__arrow::before {
      display: none !important;
    }
  }

  .custom-select-dropdown {
    max-height: 274px;
    overflow-y: auto;
    padding: 4px 0;

    &::-webkit-scrollbar {
      width: 6px;
    }

    &::-webkit-scrollbar-thumb {
      background: #dcdfe6;
      border-radius: 3px;
    }

    &::-webkit-scrollbar-track {
      background: transparent;
    }
  }

  .custom-select-option {
    display: flex;
    align-items: center;
    padding: 0 12px;
    height: 40px;
    font-size: 14px;
    color: var(--text-color);
    cursor: pointer;
    transition: background 0.2s cubic-bezier(0.4, 0, 0.2, 1);

    &:hover {
      background: var(--table-hover-color, #f5f7fa);
    }

    &.is-selected {
      background: #f0f5ff;
      color: var(--button-active-color);
      font-weight: 500;
    }
    
    .option-avatar {
      width: 28px;
      height: 28px;
      border-radius: 4px;
      margin-right: 8px;
      object-fit: cover;
      flex-shrink: 0;
    }
    
    .option-label {
      flex: 1;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    
    .option-check {
      margin-left: 8px;
      color: var(--button-active-color);
      flex-shrink: 0;
    }
  }

  .custom-select-loading,
  .custom-select-empty {
    padding: 10px 0;
    text-align: center;
    font-size: 14px;
    color: #909399;
  }
</style>
