<script setup lang="ts">
import { ref, computed, nextTick, watch } from 'vue';
import { ArrowDown, ArrowUp, Close } from '@element-plus/icons-vue';

const props = withDefaults(
  defineProps<{
    modelValue?: string | number;
    options?: Array<any>;
    placeholder?: string;
    clearable?: boolean;
    disabled?: boolean;
    loading?: boolean;
    isLoading?: boolean;
    isEnd?: boolean;
    labelKey?: string;
    valueKey?: string;
    filterable?: boolean;
    filterMethod?: (query: string) => void;
  }>(),
  {
    modelValue: undefined,
    options: () => [],
    placeholder: '请选择',
    clearable: false,
    disabled: false,
    loading: false,
    isLoading: false,
    isEnd: false,
    labelKey: 'label',
    valueKey: 'value',
    filterable: false,
    filterMethod: undefined,
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | number | undefined): void;
  (e: 'change', value: string | number | undefined): void;
  (e: 'blur'): void;
  (e: 'loadMore'): void;
}>();

const popoverVisible = ref(false);
const listRef = ref<HTMLElement>();
const triggerRef = ref<HTMLElement>();
const filterInputRef = ref<HTMLInputElement>();
const filterQuery = ref('');
const selectedLabel = ref('');

const filteredOptions = computed(() => {
  if (!props.filterable || !filterQuery.value) return props.options;
  if (props.filterMethod) return props.options;
  const query = filterQuery.value.toLowerCase();
  return props.options.filter((item) => {
    const label = getLabel(item);
    return label.toLowerCase().includes(query);
  });
});

function handleFilterInput() {
  if (props.filterMethod) {
    props.filterMethod(filterQuery.value);
  }
}

// 获取选项的 label
const getLabel = (item: any) => {
  return item?.[props.labelKey] ?? '';
};

// 获取选项的 value
const getValue = (item: any) => {
  return item?.[props.valueKey];
};

// 当前选中项的 label
const currentLabel = computed(() => {
  const option = props.options.find((item) => getValue(item) === props.modelValue);
  const label = option ? getLabel(option) : '';
  if (label) {
    selectedLabel.value = label;
  }
  return label || selectedLabel.value;
});

// 是否显示清除图标
const showClear = computed(() => {
  return props.clearable && props.modelValue !== undefined && props.modelValue !== '' && props.modelValue !== -1;
});

const isHovering = ref(false);

/**
 * 选择选项
 */
function handleSelect(item: any) {
  if (props.disabled) return;
  const value = getValue(item);
  emit('update:modelValue', value);
  emit('change', value);
  popoverVisible.value = false;
}

/**
 * 清除选中
 */
function handleClear(e: MouseEvent) {
  e.stopPropagation();
  selectedLabel.value = '';
  emit('update:modelValue', undefined as any);
  emit('change', undefined as any);
}

/**
 * 滚动加载更多
 */
function handleScroll() {
  if (!listRef.value || props.isLoading || props.isEnd) return;

  const { scrollHeight, scrollTop, clientHeight } = listRef.value;
  if (scrollHeight - scrollTop - clientHeight < 50) {
    emit('loadMore');
  }
}

// 监听 popover 显示，重置滚动位置
watch(popoverVisible, (visible) => {
  if (visible) {
    filterQuery.value = '';
    if (props.filterable && props.filterMethod) {
      props.filterMethod('');
    }
    nextTick(() => {
      if (listRef.value) {
        listRef.value.scrollTop = 0;
      }
      if (props.filterable && filterInputRef.value) {
        filterInputRef.value.focus();
      }
    });
  } else {
    emit('blur');
  }
});

watch(() => props.modelValue, (val) => {
  if (val === undefined || val === '' || val === null) {
    selectedLabel.value = '';
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
      :disabled="disabled"
    >
      <!-- 搜索框 -->
      <div v-if="filterable" class="custom-select-filter">
        <input
          ref="filterInputRef"
          v-model="filterQuery"
          class="custom-select-filter-input"
          placeholder="搜索..."
          @input="handleFilterInput"
        />
      </div>
      <!-- 下拉列表 -->
      <div
        ref="listRef"
        class="custom-select-dropdown"
        @scroll="handleScroll"
      >
        <!-- 初始加载中 -->
        <div v-if="loading" class="custom-select-loading">
          加载中...
        </div>
        <!-- 选项列表 -->
        <template v-else>
          <div
            v-for="(item, index) in filteredOptions"
            :key="getValue(item) || index"
            class="custom-select-option"
            :class="{ 'is-selected': getValue(item) === modelValue }"
            @click="handleSelect(item)"
          >
            {{ getLabel(item) }}
          </div>
          <!-- 无数据 -->
          <div v-if="filteredOptions.length === 0 && !isLoading && !isEnd" class="custom-select-empty">
            暂无数据
          </div>
          <!-- 加载更多中 -->
          <div v-if="isLoading" class="custom-select-load-more">
            加载中...
          </div>
          <!-- 没有更多了 -->
          <div v-if="isEnd && options.length > 0" class="custom-select-no-more">
            没有更多了
          </div>
        </template>
      </div>

      <!-- 触发元素 -->
      <template #reference>
        <div 
          ref="triggerRef"
          class="custom-select-trigger" 
          :class="{ 
            'is-focus': popoverVisible, 
            'is-disabled': disabled
          }"
          @mouseenter="isHovering = true"
          @mouseleave="isHovering = false"
        >
          <span class="custom-select-value" :class="{ 'is-placeholder': !currentLabel }">
            {{ currentLabel || placeholder }}
          </span>
          <span class="custom-select-suffix">
            <ElIcon v-if="showClear && isHovering" class="clear-icon" @click="handleClear">
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

<script lang="ts">
export default {
  name: 'CustomSelect',
};
</script>

<style scoped lang="less">
.custom-select {
  display: inline-block;
  width: 100%;
}

.custom-select-trigger {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  height: 36px;
  padding: 0 12px;
  background: var(--background-white-color, #fff);
  border: 1px solid var(--border-color, #dcdfe6);
  border-radius: 4px;
  cursor: pointer;
  transition: border-color 0.2s, background-color 0.2s;

  &:hover:not(.is-disabled) {
    border-color: var(--button-active-color, #264ed1);
  }

  &.is-focus {
    border-color: var(--button-active-color, #264ed1);
  }

  &.is-disabled {
    background-color: #f5f5f5;
    cursor: not-allowed;
    opacity: 0.7;
  }
}

.custom-select-value {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 14px;
  color: var(--text-color, #303133);

  &.is-placeholder {
    color: #c0c4cc;
  }
}

.custom-select-suffix {
  display: flex;
  align-items: center;
  margin-left: 8px;
  color: var(--text-color, #909399);
}

.clear-icon {
  cursor: pointer;

  &:hover {
    color: var(--button-active-color, #264ed1);
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

.custom-select-filter {
  padding: 4px 8px 0;
}

.custom-select-filter-input {
  width: 100%;
  height: 30px;
  padding: 0 8px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  outline: none;
  font-size: 13px;
  color: #303133;
  background: #fff;
  box-sizing: border-box;

  &::placeholder {
    color: #c0c4cc;
  }

  &:focus {
    border-color: var(--button-active-color, #264ed1);
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
  padding: 0 12px;
  height: 34px;
  line-height: 34px;
  font-size: 14px;
  color: var(--text-color, #606266);
  cursor: pointer;
  transition: background 0.2s;

  &:hover {
    background: var(--table-hover-color, #f5f7fa);
  }

  &.is-selected {
    color: var(--button-active-color, #264ed1);
    font-weight: 500;
  }
}

.custom-select-loading,
.custom-select-empty,
.custom-select-load-more,
.custom-select-no-more {
  padding: 10px 0;
  text-align: center;
  font-size: 14px;
  color: #909399;
}

.custom-select-load-more {
  color: #409eff;
}

// el-form-item 验证错误状态样式
.el-form-item.is-error {
  .custom-select-trigger {
    border-color: #f56c6c;
  }
}
</style>
