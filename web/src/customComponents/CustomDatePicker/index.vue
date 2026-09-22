<script setup lang="ts">
import { ref, watch, computed, nextTick, onMounted, onUnmounted } from 'vue';
import { useWebView2SingleDatePicker } from '@/composables/useWebView2SingleDatePicker';

const props = withDefaults(
  defineProps<{
    modelValue?: string;
    type?: 'datetime' | 'date';
    placeholder?: string;
    disabled?: boolean;
    valueFormat?: string;
    format?: string;
  }>(),
  {
    modelValue: undefined,
    type: 'datetime',
    placeholder: '请选择日期',
    disabled: false,
    valueFormat: undefined,
    format: undefined,
  }
);

const emit = defineEmits<{
  (e: 'update:modelValue', value: string | undefined): void;
  (e: 'change', value: string | undefined): void;
  (e: 'blur'): void;
}>();

// 内部绑定值
const innerValue = ref<string | undefined>(props.modelValue);

// 监听外部值变化
watch(() => props.modelValue, (val) => {
  innerValue.value = val;
});

// 是否包含时间
const includeTime = computed(() => props.type === 'datetime');

// 实际使用的 valueFormat
const actualValueFormat = computed(() => {
  if (props.valueFormat) return props.valueFormat;
  if (props.type === 'datetime') return 'YYYY-MM-DD HH:mm:ss';
  return 'YYYY-MM-DD';
});

// 实际使用的 format
const actualFormat = computed(() => {
  if (props.format) return props.format;
  if (props.type === 'datetime') return 'YYYY-MM-DD HH:mm:ss';
  return 'YYYY-MM-DD';
});

// 处理变化回调
const handleChange = (val: string | undefined) => {
  innerValue.value = val;
  emit('update:modelValue', val);
  emit('change', val);
};

// 处理 blur 回调
const handleBlur = () => {
  emit('blur');
};

// 使用 hook（仅 datetime 类型需要）
const {
  datePickerRef,
  datePickerVisible,
  handleCalendarChange,
  handlePanelChange,
  handleDateChange,
  handleVisibleChange,
  popperOptions,
  teleported,
  popperClass,
} = useWebView2SingleDatePicker(innerValue, handleChange, {
  includeTime: includeTime.value,
  valueFormat: actualValueFormat.value,
});

// date 和 time 类型直接使用原生事件
const handleNativeChange = (val: string | undefined) => {
  handleChange(val);
};

const containerRef = ref<HTMLElement>();

// 处理 visible 变化（弹窗关闭时触发 blur）
const handleVisibleChangeWithBlur = (visible: boolean) => {
  handleVisibleChange(visible);
  if (!visible) {
    handleBlur();
  }
};

/**
 * 监听容器内的 mousedown 事件，直接打开弹窗
 * 解决 WebView2 下点击输入框不触发弹窗的问题
 */
const handleContainerMouseDown = (e: MouseEvent) => {
  if (props.disabled) return;
  
  // 检查点击的是否是输入框区域
  const target = e.target as HTMLElement;
  const wrapper = containerRef.value?.querySelector('.el-input__wrapper');
  if (wrapper && wrapper.contains(target)) {
    console.log('[CustomDatePicker] 用户点击输入框');
    // 直接打开弹窗
    if (!datePickerVisible.value) {
      nextTick(() => {
        datePickerVisible.value = true;
      });
    }
  }
};

onMounted(() => {
  // 在容器上监听 mousedown 事件（捕获阶段）
  if (containerRef.value) {
    containerRef.value.addEventListener('mousedown', handleContainerMouseDown, true);
  }
});

onUnmounted(() => {
  if (containerRef.value) {
    containerRef.value.removeEventListener('mousedown', handleContainerMouseDown, true);
  }
});
</script>

<template>
  <div ref="containerRef" class="custom-date-picker">
    <!-- datetime 类型：使用 hook 处理 webview 兼容性 -->
    <el-date-picker
      v-if="type === 'datetime'"
      ref="datePickerRef"
      v-model="innerValue"
      v-model:visible="datePickerVisible"
      type="datetime"
      :placeholder="placeholder"
      :disabled="disabled"
      :value-format="actualValueFormat"
      :format="actualFormat"
      :teleported="teleported"
      :popper-options="popperOptions"
      :popper-class="popperClass"
      style="width: 100%"
      @calendar-change="handleCalendarChange"
      @panel-change="handlePanelChange"
      @change="handleDateChange"
      @visible-change="handleVisibleChangeWithBlur"
    />
    
    <!-- date 和 time 类型：直接使用原生组件 -->
    <el-date-picker
      v-else
      v-model="innerValue"
      :type="type"
      :placeholder="placeholder"
      :disabled="disabled"
      :value-format="actualValueFormat"
      :format="actualFormat"
      :teleported="true"
      style="width: 100%"
      @change="handleNativeChange"
      @blur="handleBlur"
    />
  </div>
</template>

<script lang="ts">
export default {
  name: 'CustomDatePicker',
};
</script>

<style scoped lang="less">
.custom-date-picker {
  display: inline-block;
  width: 100%;

  :deep(.el-date-editor) {
    width: 100%;
    border: none !important;
  }

  :deep(.el-input__wrapper) {
    background-color: transparent !important;
    border: 1px solid var(--border-color, #dcdfe6) !important;
    border-radius: 4px !important;
    height: 36px !important;
    min-height: 36px !important;
    padding: 1px 11px !important;
  }

  :deep(.el-input__wrapper:hover) {
    box-shadow: none !important;
  }

  :deep(.el-input__wrapper.is-focus) {
    box-shadow: 0 0 0 1px var(--button-active-color, #264ed1) inset !important;
  }

  :deep(.el-input__inner) {
    height: 32px;
    line-height: 32px;
    color: var(--text-color, #303133);

    &::placeholder {
      color: var(--text-color, #c0c4cc);
    }
  }

  :deep(.el-date-editor.is-disabled) {
    .el-input__wrapper {
      background-color: #f5f5f5 !important;
      cursor: not-allowed;
    }
  }
}
</style>

<style lang="less">
// el-form-item 验证错误状态样式
.el-form-item.is-error {
  .custom-date-picker {
    .el-input__wrapper {
      border-color: #f56c6c !important;
    }
  }
}
</style>
