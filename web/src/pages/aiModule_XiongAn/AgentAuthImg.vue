<script setup lang="ts">
  import { ref, watch, onMounted, onBeforeUnmount } from 'vue';
  import { appConfig } from '@/config';

  const props = withDefaults(
    defineProps<{
      picUrl?: string;
      imgClass?: string;
      width?: number | string;
      height?: number | string;
      style?: string | Record<string, string>;
    }>(),
    {
      picUrl: '',
      imgClass: 'ai-avatar-img',
      width: undefined,
      height: undefined,
      style: '',
    },
  );

  const imgRef = ref<HTMLImageElement | null>(null);
  const xhrInstance = ref<XMLHttpRequest | null>(null);
  const currentPicUrl = ref('');

  const abortXhr = () => {
    if (xhrInstance.value && xhrInstance.value.readyState !== XMLHttpRequest.DONE) {
      xhrInstance.value.abort();
    }
    xhrInstance.value = null;
  };

  const loadImg = () => {
    const aiBase = (appConfig.settingData && appConfig.settingData.AI_WEB) || '';
    const setToken =
      'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBWZXJzaW9uIjoiMSIsImFwcFR5cGUiOiIyIiwiaXNzIjoiaGVibXBwLm9yZyIsImFwcEtleSI6Ik9iQ3c5STJDIiwiZXhwIjoxNjc1NDE0MDc5OSwiaWF0IjoxNjg5NDIwMDUyLCJhcHBab25lIjoiMiIsImp0aSI6ImFhYTFmYWNjLTQzYzAtNGU3Ny1iYjc2LWYxZWZiZWNkOWEzNSIsInVzZXJuYW1lIjoieGlhbmdydWkifQ.QTkLkHHVs0Azkjr5VpbmC4hotLQX01r6GzbUdZ-GkRk';
    const img = imgRef.value;
    if (!img) return;

    let url = props.picUrl || '';

    // 空URL直接清空图片
    if (!url) {
      img.src = '';
      return;
    }

    // 若 picUrl 为相对路径或以 / 开头，则拼接 AI_WEB 作为基址
    const isAbsolute = /^https?:\/\//i.test(url);
    if (!isAbsolute && aiBase) {
      url = aiBase.replace(/\/$/, '') + (url.startsWith('/') ? '' : '/') + url;
    } else if (isAbsolute && aiBase && url.startsWith('/')) {
      url = aiBase.replace(/\/$/, '') + url;
    }

    // 取消之前未完成的请求，避免响应错乱
    abortXhr();

    // 记录当前要加载的URL，用于后续校验
    currentPicUrl.value = url;

    const xhr = new XMLHttpRequest();
    xhrInstance.value = xhr;

    xhr.open('GET', url, true);
    xhr.responseType = 'blob';
    if (url.includes('XA-ics-agent')) {
      xhr.setRequestHeader('appToken', setToken);
    }
    xhr.onreadystatechange = () => {
      if (xhr.readyState === XMLHttpRequest.DONE) {
        xhrInstance.value = null;
        if (xhr.status === 200 && currentPicUrl.value === url) {
          // 先释放旧的URL对象，避免内存泄漏
          if (img.src) {
            URL.revokeObjectURL(img.src);
          }
          img.src = URL.createObjectURL(xhr.response);
          img.onload = () => {
            URL.revokeObjectURL(img.src);
          };
        }
      }
    };
    xhr.onerror = () => {
      xhrInstance.value = null;
    };
    xhr.onabort = () => {
      xhrInstance.value = null;
    };
    xhr.send();
  };

  watch(
    () => props.picUrl,
    () => {
      loadImg();
    },
  );

  onMounted(() => {
    loadImg();
  });

  onBeforeUnmount(() => {
    abortXhr();
    const img = imgRef.value;
    if (img && img.src) {
      URL.revokeObjectURL(img.src);
    }
  });
</script>

<template>
  <img ref="imgRef" :class="imgClass" :style="style" :width="width" :height="height" />
</template>

<style scoped>
  .ai-avatar-img {
    width: 45px;
    height: 45px;
    border-radius: 50%;
  }
</style>