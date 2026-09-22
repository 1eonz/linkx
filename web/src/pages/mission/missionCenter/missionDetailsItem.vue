<script setup lang="ts">
  defineProps<{
    info: any;
  }>();
</script>

<template>
  <div class="details-item">
    <span class="label">{{ info.label }}</span>
    <TdImage v-if="info.type === 'PICTURE'" :preview-src-list="[info.value]" :url="info.value" />
    <div v-else class="content">
      <TdTooltip :content="`${info.value}`">
        <span class="text">{{ info.value }}</span>
      </TdTooltip>
    </div>
  </div>
</template>

<style scoped lang="less">
  @import '@/styles/mixin.less';

  .details-item {
    display: flex;
    width: 100%;
    padding: 4px 0;
    font-size: 14px;

    .label {
      display: inline-block;
      width: 64px;
      margin-right: 4px;
      color: var(--text-title-second);
    }

    .content {
      position: relative;
      display: flex;
      align-items: center;
      max-width: calc(100% - 70px);

      .text {
        width: 200px;
        color: var(--text-color-normal);
        word-break: break-all;
        .ellipsis(2);
      }
    }

    img {
      width: auto;
      max-width: calc(100% - 72px);
      height: 80px;
    }

    .inner-item {
      display: flex;
      flex-direction: column;
    }
  }
</style>
