<script>
  import { appConfig } from '@/config';

  export default {
    name: 'AuthImg',
    props: {
      picUrl: {
        default: '',
        type: String,
      },
    },
    watch: {
      picUrl() {
        this.loadImg();
      },
    },
    mounted() {
      this.loadImg();
    },
    methods: {
      loadImg() {
        const aiBase = (appConfig.settingData && appConfig.settingData.AI_WEB) || '';
        const setToken =
          'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBWZXJzaW9uIjoiMSIsImFwcFR5cGUiOiIyIiwiaXNzIjoiaGVibXBwLm9yZyIsImFwcEtleSI6Ik9iQ3c5STJDIiwiZXhwIjoxNjc1NDE0MDc5OSwiaWF0IjoxNjg5NDIwMDUyLCJhcHBab25lIjoiMiIsImp0aSI6ImFhYTFmYWNjLTQzYzAtNGU3Ny1iYjc2LWYxZWZiZWNkOWEzNSIsInVzZXJuYW1lIjoieGlhbmdydWkifQ.QTkLkHHVs0Azkjr5VpbmC4hotLQX01r6GzbUdZ-GkRk';
        const img = this.$refs.img;
        let url = this.picUrl;
        // 若 picUrl 为相对路径或以 / 开头，则拼接 AI_WEB 作为基址
        if (url) {
          const isAbsolute = /^https?:\/\//i.test(url);
          if (!isAbsolute && aiBase) {
            url = aiBase.replace(/\/$/, '') + (url.startsWith('/') ? '' : '/') + url;
          } else if (isAbsolute && aiBase && url.startsWith('/')) {
            url = aiBase.replace(/\/$/, '') + url;
          }
        }

        const xhr = new XMLHttpRequest();

        xhr.open('GET', url, true);
        xhr.responseType = 'blob';
        if (url.includes('XA-ics-agent')) {
          xhr.setRequestHeader('appToken', setToken);
        }
        xhr.onreadystatechange = (e) => {
          if (xhr.readyState === XMLHttpRequest.DONE && xhr.status === 200) {
            img.src = URL.createObjectURL(xhr.response);
            img.onload = () => {
              URL.revokeObjectURL(img.src);
            };
          }
        };
        xhr.send();
      },
    },
  };
</script>
<template>
  <img ref="img" class="icon" />
</template>
<style scoped>
  .icon {
    width: 32px;
    height: 32px;
  }
</style>
