<script lang="ts" setup>
  import { computed } from 'vue';

  defineOptions({
    name: 'TdCorner',
  });

  const props = withDefaults(
    defineProps<{
      color?: string;
      height?: string;
      width?: string;
    }>(),
    {
      color: 'rgb(0 194 255 / 100%)',
    },
  );

  const sty = computed(() => {
    return {
      height: `${props.height || 32}px`,
      width: `${props.width || 32}px`,
    };
  });
</script>

<template>
  <div class="td-corner lt" :style="sty"></div>
  <div class="td-corner lb" :style="sty"></div>
  <div class="td-corner rt" :style="sty"></div>
  <div class="td-corner rb" :style="sty"></div>
</template>

<style lang="less" scoped>
  .td-corner {
    position: absolute;
    z-index: 1;
    pointer-events: none;
    border-top: 3px solid v-bind(color);
    border-left: 3px solid v-bind(color);

    &.lt {
      top: 0;
      left: 0;
    }

    &.lb {
      bottom: 0;
      left: 0;
      transform: rotate(270deg);
    }

    &.rt {
      top: 0;
      right: 0;
      transform: rotate(90deg);
    }

    &.rb {
      right: 0;
      bottom: 0;
      transform: rotate(180deg);
    }
  }
</style>
