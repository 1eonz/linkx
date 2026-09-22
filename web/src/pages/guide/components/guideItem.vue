<script setup lang="ts">
  defineProps<{
    guideItem: any;
  }>();
</script>

<template>
  <div class="guide-item common-list-item">
    <div class="guide-item-left">
      <Icon class="left-icon" name="map_feature17" prefix="bigScreen" />
    </div>
    <div class="guide-item-center">{{ guideItem.title }}</div>
    <div class="guide-item-right">
      <Icon class="icon-back" name="icon_back" prefix="bigScreen" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .guide-item {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    cursor: pointer;

    .guide-item-left {
      margin-top: 4px;
      line-height: 22px;

      .left-icon {
        width: 14px;
        height: 15px;
      }
    }

    .guide-item-center {
      display: -webkit-box;
      width: 239px;
      margin-left: 8px;
      overflow: hidden;
      font-size: 14px;
      font-weight: 500;
      line-height: 22px;
      color: rgb(255 255 255 / 100%);
      text-overflow: ellipsis;
      -webkit-line-clamp: 2;
      letter-spacing: 0;
      -webkit-box-orient: vertical;
    }

    .guide-item-right {
      .icon-back {
        width: 13px;
        height: 13px;
        margin-top: 4px;
      }
    }
  }
</style>
