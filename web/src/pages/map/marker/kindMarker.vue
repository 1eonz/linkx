<script setup lang="ts">
  defineProps<{
    image?: string;
    text?: string;
  }>();
</script>

<template>
  <div v-if="image" class="kind-marker">
    <img alt="" class="image" :src="image" />
  </div>
  <div v-else class="text ground-glass">
    {{ text }}
  </div>
</template>

<style lang="less" scoped>
  .kind-marker {
    display: inline-block;
    width: 38px;
    height: 38px;

    .image {
      display: block;
      width: 38px;
      height: 38px;
    }
  }

  .text {
    height: 33px;
    padding: 0 12px;
    font-size: 14px;
    line-height: 33px;
    white-space: nowrap;
  }
</style>
