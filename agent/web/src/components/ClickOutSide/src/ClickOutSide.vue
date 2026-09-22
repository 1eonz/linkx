<script lang="ts" setup>
  import { onMounted, ref } from 'vue';

  import { onClickOutside } from '@vueuse/core';

  defineOptions({
    name: 'TdClickOutSide',
  });

  const emit = defineEmits(['mounted', 'clickOutside']);

  const wrap = ref<HTMLElement>();

  onClickOutside(wrap, () => {
    emit('clickOutside');
  });

  onMounted(() => {
    emit('mounted');
  });
</script>

<template>
  <div ref="wrap">
    <slot></slot>
  </div>
</template>
