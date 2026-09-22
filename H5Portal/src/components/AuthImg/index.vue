<template>
  <img ref="img" class="icon" />
</template>

<script>
  export default {
    name: 'AuthImg',
    props: {
      picUrl: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        // 存储当前实例的请求对象，用于取消未完成的请求
        xhrInstance: null,
        // 存储当前正在加载的图片URL，用于校验响应是否匹配
        currentPicUrl: '',
      };
    },
    watch: {
      picUrl(newVal) {
        // 图片URL变化时，重新加载
        this.loadImg(newVal);
      },
    },
    mounted() {
      this.loadImg(this.picUrl);
    },
    beforeUnmount() {
      // 组件销毁前，取消未完成的请求，清理URL对象
      this.abortXhr();
      const img = this.$refs.img;
      if (img && img.src) {
        URL.revokeObjectURL(img.src);
      }
    },
    methods: {
      // 取消当前未完成的请求
      abortXhr() {
        if (this.xhrInstance && this.xhrInstance.readyState !== XMLHttpRequest.DONE) {
          this.xhrInstance.abort();
        }
        this.xhrInstance = null;
      },
      loadImg(picUrl) {
        // 空URL直接清空图片
        if (!picUrl) {
          this.$refs.img.src = '';
          return;
        }

        // 1. 取消之前未完成的请求，避免响应错乱
        this.abortXhr();

        // 2. 记录当前要加载的URL，用于后续校验
        this.currentPicUrl = picUrl;

        const setToken =
          'eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJhcHBWZXJzaW9uIjoiMSIsImFwcFR5cGUiOiIyIiwiaXNzIjoiaGVibXBwLm9yZyIsImFwcEtleSI6Ik9iQ3c5STJDIiwiZXhwIjoxNjc1NDE0MDc5OSwiaWF0IjoxNjg5NDIwMDUyLCJhcHBab25lIjoiMiIsImp0aSI6ImFhYTFmYWNjLTQzYzAtNGU3Ny1iYjc2LWYxZWZiZWNkOWEzNSIsInVzZXJuYW1lIjoieGlhbmdydWkifQ.QTkLkHHVs0Azkjr5VpbmC4hotLQX01r6GzbUdZ-GkRk';

        const img = this.$refs.img;
        const xhr = new XMLHttpRequest();
        // 保存当前请求实例到组件数据中
        this.xhrInstance = xhr;

        xhr.open('GET', picUrl, true);
        xhr.responseType = 'blob';

        if (picUrl.includes('XA-ics-agent')) {
          xhr.setRequestHeader('appToken', setToken);
        }

        xhr.onreadystatechange = (e) => {
          // 3. 校验：请求完成、状态200、且响应匹配当前要加载的URL
          if (xhr.readyState === XMLHttpRequest.DONE) {
            // 无论成功失败，都清空当前请求实例
            this.xhrInstance = null;

            if (xhr.status === 200 && this.currentPicUrl === picUrl) {
              // 先释放旧的URL对象，避免内存泄漏
              if (img.src) {
                URL.revokeObjectURL(img.src);
              }
              // 赋值新的图片URL
              img.src = URL.createObjectURL(xhr.response);
              img.onload = () => {
                // 图片加载完成后，释放blob URL
                URL.revokeObjectURL(img.src);
              };
            }
          }
        };

        // 处理请求失败/取消的情况
        xhr.onerror = () => {
          this.xhrInstance = null;
        };
        xhr.onabort = () => {
          this.xhrInstance = null;
        };

        xhr.send();
      },
    },
  };
</script>

<style scoped>
  /* 平板适配 */
  @media screen and (min-height: 1200px) {
    .icon {
      width: 32px;
      height: 32px;
    }
  }
  @media screen and (min-height: 2001px) {
    .icon {
      width: 42px;
      height: 42px;
    }
  }
</style>
