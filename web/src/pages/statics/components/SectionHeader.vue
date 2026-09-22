<script setup lang="ts">
  defineProps<{
    title: string;
    extraTitle?: string;
  }>();
</script>

<template>
  <div class="section-header">
    <div class="title-decoration"></div>
    <h3 class="section-title">{{ title }} <span v-if="extraTitle" class="extra-title">{{ extraTitle }}</span></h3>
  </div>
</template>

<style lang="less" scoped>
  .section-header {
    display: flex;
    align-items: center;
    //   padding: 15px 0 0 15px;
    .title-decoration {
      width: 4px;
      height: 12px;
      margin-right: 5px;
      background-color: var(--tabs-active-color);
      border-radius: 2px;
    }

    .section-title {
      margin: 0;
      font-size: 12px;
      font-weight: 600;
      color: var(--text-color);
    }
    .extra-title {
      color: #333
    }
  }
</style>
