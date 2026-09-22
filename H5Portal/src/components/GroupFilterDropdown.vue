<script setup lang="ts">
import { computed, ref, watch } from 'vue';

import caretDownDefault from '@/assets/svg/caret-down-default.svg';
import caretDownActive from '@/assets/svg/caret-down-active.svg';

interface Option {
  label: string;
  value: string | number;
}

const props = defineProps<{
  modelValue: string | number;
  options: Option[];
  placement?: string;
}>();

const emit = defineEmits<{
  (e: 'update:modelValue', v: string | number): void;
  (e: 'change', v: string | number): void;
}>();

const show = ref(false);

// 是否选中了非默认值（非首个选项）
const isSelected = computed(() => {
  const defaultValue = props.options[0]?.value;
  return String(props.modelValue) !== String(defaultValue);
});

// 当前图标：选中状态用active图标，否则用default图标
const caretIcon = computed(() => (isSelected.value ? caretDownActive : caretDownDefault));

// 当前选中项的文本
const currentLabel = computed(() => {
  const hit = props.options.find((o) => String(o.value) === String(props.modelValue));
  return hit?.label ?? props.options[0]?.label ?? '';
});

// popover 选项列表
const actions = computed(() =>
  props.options.map((o) => ({
    text: o.label,
    value: o.value,
    className: String(o.value) === String(props.modelValue) ? 'is-selected' : '',
  })),
);

// 选中操作
const onSelect = (action: any) => {
  const value = action?.value ?? props.options[0]?.value;
  emit('update:modelValue', value);
  emit('change', value);
  show.value = false;
};

// 外部变更 modelValue 时关闭弹层
watch(
  () => props.modelValue,
  () => {
    show.value = false;
  },
);
</script>

<template>
  <van-popover
    v-model:show="show"
    :placement="placement || 'bottom-start'"
    trigger="click"
    :actions="actions"
    @select="onSelect"
    class="group-filter-dropdown"
  >
    <template #reference>
      <view class="trigger" :class="{ selected: isSelected }">
        <text class="label">{{ currentLabel }}</text>
        <img class="caret" :class="{ rotate: show }" :src="caretIcon" alt="" />
      </view>
    </template>
  </van-popover>
</template>

<style lang="scss" scoped>
.trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  height: 32px;
  padding: 0 8px;
  font-size: 14px;
  color: rgba(3, 11, 38, 1);
  box-sizing: border-box;
  cursor: pointer;
}

.trigger.selected {
  color: rgba(38, 78, 209, 1);
}

.label {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.caret {
  width: 12px;
  height: 12px;
  flex-shrink: 0;
  transition: transform 0.2s;
}

.caret.rotate {
  transform: rotate(180deg);
}
</style>

<style lang="scss">
/* Popover 内容 teleport 到 body，需要非 scoped 样式 */
.group-filter-dropdown {
  .van-popover__wrapper {
    width: 100%;
  }

  .van-popover__content {
    padding: 0;
    max-height: 280px;
    overflow-y: auto;
    -webkit-overflow-scrolling: touch;
  }

  .van-popover__action-text {
    text-align: left;
    justify-content: flex-start;
  }

  .van-popover__action {
    padding: 12px 40px 12px 16px;
    line-height: 22px;
    color: rgba(3, 11, 38, 1);
    font-size: 15px;
    white-space: nowrap;
  }

  .van-popover__action.is-selected {
    color: #264ed1;
    background: rgba(38, 78, 209, 0.08);
  }
}
</style>
