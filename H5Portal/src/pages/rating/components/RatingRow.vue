<template>
  <view class="rating-row">
    <view class="rating-label">{{ label }}:</view>
    <van-rate
      :model-value="modelValue"
      :size="size"
      color="#fa781b"
      void-color="#d9dade"
      :readonly="readonly"
      @update:model-value="handleUpdate"
    />
    <view class="rating-text">{{ scoreLabel }}</view>
  </view>
</template>

<script lang="ts" setup>
  import { computed } from 'vue';
  import { Rate } from 'vant';

  // 打星文案映射
  const SCORE_LABELS: Record<number, string> = {
    1: '很差',
    2: '差',
    3: '一般',
    4: '满意',
    5: '很满意',
  };

  const props = withDefaults(
    defineProps<{
      label?: string;
      modelValue?: number;
      size?: number;
      readonly?: boolean;
    }>(),
    {
      label: '',
      modelValue: 0,
      size: 20,
      readonly: false,
    },
  );

  const emit = defineEmits<{
    (e: 'update:modelValue', value: number): void;
  }>();

  // 当前评分对应的文案
  const scoreLabel = computed(() => SCORE_LABELS[props.modelValue] || '');

  const handleUpdate = (value: number) => {
    if (!props.readonly) {
      emit('update:modelValue', value);
    }
  };
</script>

<style lang="scss" scoped>
  .rating-row {
    display: flex;
    // align-items: center;
    margin-bottom: 17px;
    line-height: 20px;
    font-weight: 400;
    letter-spacing: 0px;

    &:last-child {
      margin-bottom: 0;
    }
  }

  .rating-label {
    width: 114px;
    padding-right: 4px;
    flex-shrink: 0;
    color: rgba(76, 76, 76, 1);
  }

  .rating-text {
    margin-left: 16px;
    font-size: 12px;
    color: rgba(91, 96, 114, 1);
    flex: none;
  }

  :deep(.van-rate__item:not(:last-child)) {
    padding-right: 12px;
  }
</style>
