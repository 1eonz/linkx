<script setup lang="ts">
  import { ref, watch, onMounted, onBeforeUnmount } from 'vue';
  import { appConfig } from '@/config';
  import defaultAvatar from '@/assets/images/ai/ai_header.png';

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
  const observer = ref<IntersectionObserver | null>(null);
  const isInView = ref(false);

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

    // 先设置为默认ai头像，避免初始化过程中显示空白，后续再替换为实际图片，如果 props.picUrl 为空，也显示默认头像
    img.src = defaultAvatar;

    let url = props.picUrl || '';

    // 空URL直接清空图片
    if (!url) {
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

  // 初始化懒加载:使用 IntersectionObserver 监听图片是否进入可视窗口,仅在可见时触发请求
  const setupLazyLoad = () => {
    const img = imgRef.value;
    if (!img || typeof IntersectionObserver === 'undefined') {
      // 不支持时降级直接加载
      isInView.value = true;
      loadImg();
      return;
    }

    observer.value = new IntersectionObserver(
      (entries) => {
        entries.forEach((entry) => {
          if (entry.isIntersecting) {
            isInView.value = true;
            loadImg();
            // 首次进入可视区域后停止观察,避免重复触发
            observer.value?.disconnect();
            observer.value = null;
          }
        });
      },
      {
        root: null,
        rootMargin: '0px',
        threshold: 0,
      },
    );

    observer.value.observe(img);
  };

  onMounted(() => {
    setupLazyLoad();
  });

  onBeforeUnmount(() => {
    observer.value?.disconnect();
    observer.value = null;
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