<script lang="ts" setup>
  import { computed } from 'vue';

  defineOptions({
    name: 'TdLabel',
  });

  const props = defineProps({
    number: {
      default: 0,
      type: Number,
    },
  });

  const num = computed(() => {
    if (props.number > 99) {
      return '99+';
    }
    return props.number;
  });
</script>

<template>
  <div class="td-label">
    <TdTooltip :content="`${number}`" placement="top">
      <div class="msg-detail-label">
        {{ num }}
      </div>
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .msg-detail-label {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 20px;
    height: 20px;
    font-size: 12px;
    color: var(--color-white);
    background-color: #ff173d;
    border-radius: 50%;
  }
</style>
