<script setup lang="ts">
  import { onMounted, ref, watch } from 'vue';

  import { Dialog } from '@/components/Dialog';

  import ImagePreview from './ImagePreview.vue';

  defineOptions({ name: 'TdImage' });

  const props = withDefaults(
    defineProps<{
      previewSrcList?: string[];
      title?: string;
      url: string;
    }>(),
    {
      previewSrcList: () => [],
    },
  );

  const imgRef = ref();
  const loadStatus = ref('');

  watch(() => props.url, loadImage);

  onMounted(() => {
    loadImage();
  });

  // 加载图片
  function loadImage() {
    imgRef.value.onload = function () {
      loadStatus.value = 'success';
    };
    imgRef.value.onerror = function () {
      loadStatus.value = 'fail';
    };
    imgRef.value.src = props.url;
  }

  // 图片预览
  function previewImage() {
    const { previewSrcList, title } = props;
    if (previewSrcList.length === 0) {
      return;
    }

    const dialog = Dialog({
      cid: 'ImagePreview',
      content: ImagePreview,
      data: {
        onMaximize() {
          dialog.fullScreen();
        },
        previewSrcList,
        title,
      },
    });
  }
</script>

<template>
  <img
    v-if="loadStatus === 'fail'"
    alt=""
    class="td-image"
    src="@/assets/images/common/no-image.png"
  />
  <img v-else ref="imgRef" class="td-image" @click="previewImage" />
</template>

<style scoped></style>
