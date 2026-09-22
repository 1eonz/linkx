<script lang="ts" setup>
  import { computed } from 'vue';

  defineOptions({
    name: 'Icon',
  });

  const props = defineProps({
    color: {
      default: '',
      type: String,
    },
    name: {
      required: true,
      type: String,
    },
    prefix: {
      default: '',
      type: String,
    },
  });

  const symbolId = computed(() => {
    const { name, prefix } = props;
    let id = `#icon-${name}`;
    if (prefix) {
      id = `#icon-${prefix}-${name}`;
    }
    return id;
  });
</script>

<template>
  <svg class="td-icon" fill="currentColor">
    <use :fill="color" :xlink:href="symbolId" />
  </svg>
</template>

<style scoped lang="less">
  .td-icon {
    width: 14px;
    height: 14px;

    &:focus {
      outline: none !important;
    }
  }
</style>
