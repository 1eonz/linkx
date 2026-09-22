<script lang="ts" setup>
  defineProps({
    data: {
      default: () => {},
      type: Object,
    },
  });
</script>

<template>
  <div class="map-custom-popup">
    <TdTooltip :content="data.title" placement="top">
      <p class="custom-title">{{ data.title }}</p>
    </TdTooltip>

    <TdTooltip :content="data.content" placement="top">
      <div class="custom-status">
        <span class="custom-content">
          {{ data.content || data.context }}
        </span>
      </div>
    </TdTooltip>
  </div>
</template>

<style lang="less" scoped>
  .map-custom-popup {
    .custom-title {
      font-size: 16px;
      font-weight: bolder;
      color: var(--color-white);
      cursor: pointer;
    }

    .custom-status {
      font-size: 12px;

      .custom-name {
        color: var(--text-title-second);
      }
    }
  }
</style>
