<script lang="ts" setup>
  import damage_back from '@/assets/images/mrs/damage_back.png';

  defineProps<{
    attach?: string;
    loadingVideo?: boolean;
    videoItem: any;
  }>();
</script>

<template>
  <div class="video-history">
    <div class="img-box" :class="{ 'fail-box': videoItem?.sendType === 'fail' }">
      <Icon
        v-if="videoItem?.sendType === 'fail'"
        class="waring-icon"
        name="icon_waring"
        prefix="bigScreen"
        :size="16"
      />
      <img
        alt=""
        class="video-img"
        :class="{ 'demage-img': !attach }"
        :src="attach || damage_back"
      />
      <Icon
        v-if="loadingVideo"
        class="show-icon"
        name="icon_upload_video"
        prefix="bigScreen"
        :size="16"
      />
      <icon v-else class="show-icon" name="icon_show_video" prefix="bigScreen" :size="16" />
    </div>
  </div>
</template>

<style lang="less" scoped>
  .video-history {
    margin-bottom: 10px;

    .img-box {
      position: relative;
      cursor: pointer;

      .waring-icon {
        position: absolute;
        top: 24px;
        left: -25px;
      }

      .demage-img {
        width: 120px;
        height: 64px;
      }

      .video-img {
        max-width: 160px;
        max-height: 150px;
      }

      .show-icon {
        position: absolute;
        top: 50%;
        left: 50%;
        z-index: 200;
        width: 16px !important;
        height: 16px !important;
        margin-top: -8px;
        margin-left: -8px;
      }
    }

    .fail-box {
      cursor: unset;
    }

    :deep(.frame-box) {
      position: absolute;
      top: 0;
      left: 320px;
      width: 500px !important;
      height: 324px !important;
    }
  }
</style>
