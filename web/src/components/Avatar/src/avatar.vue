<script setup lang="ts">
  import { ref, unref, watchEffect } from 'vue';

  import headerColImg from '@/assets/images/ai/header_col.png';
  import defaultImg from '@/assets/svg/tree/tree_person.svg';
  import { getOnlineStatus, getResourceAvatar } from '@/pages/resource/resourceHelper';
  import { getIp } from '@/utils';
  import { getToken } from '@/utils/auth';

  defineOptions({
    name: 'TdAvatar',
  });

  const props = defineProps<{
    info?: any;
    item?: any;
    url?: string;
  }>();

  const emit = defineEmits(['click']);

  const imgRef = ref();
  const loading = ref(false);

  watchEffect(() => {
    const { info, url } = props;
    if (props.url) {
      loading.value = true;
      loadImage();
      return;
    }
    if (!url && !info) {
      setDefaultUrl();
    }
  });

  function getImageFullUrl (url) {
    // 判断是否是带协议的完整 URL
    if (url.startsWith('http://') || url.startsWith('https://')) {
      return url;
    }
    
    return `${getIp()}/linkx/desktop${url}`;
  }

  async function loadImage() {
    const url = getImageFullUrl(props.url);
    const http = new XMLHttpRequest();
    http.responseType = 'blob';
    http.open('get', url, true);
    http.setRequestHeader('Authorization', `token ${getToken()}` as string);
    http.addEventListener('readystatechange', () => {
      if (http.readyState === XMLHttpRequest.DONE) {
        loading.value = false;
        if (http.status === 200) {
          if (!unref(imgRef)) return;
          unref(imgRef).src = URL.createObjectURL(http.response);
          unref(imgRef).addEventListener('load', () => {
            URL.revokeObjectURL(unref(imgRef).src);
          });
        } else {
          setDefaultUrl();
        }
      }
    });
    http.send();
  }

  function setDefaultUrl() {
    loading.value = false;
    if (unref(imgRef)) {
      // 如果存在 fromUserId，使用 AI 头像，否则使用默认头像
      const fallback = props?.item?.fromRealUserId ? headerColImg : defaultImg;
      unref(imgRef).src = fallback;
    }
  }

  function clickHandle() {
    emit('click');
  }
</script>

<template>
  <div class="td-avatar-wrapper">
    <!-- 加载中骨架屏 -->
    <div v-if="loading" class="td-avatar-skeleton"></div>
    <img
      v-show="!loading && (url || (!url && !info))"
      ref="imgRef"
      alt=""
      class="td-avatar"
      src=""
      @click="clickHandle"
    />
    <div v-if="!loading && info" class="avatar" :class="getOnlineStatus(info)">
      <Icon class="icon" :name="getResourceAvatar(info)" prefix="tree" />
    </div>
  </div>
</template>

<style scoped>
  .td-avatar-wrapper {
    position: relative;
    display: inline-block;
  }

  .td-avatar {
    width: 36px;
    height: 36px;
  }

  .td-avatar-skeleton {
    width: 36px;
    height: 36px;
    background: linear-gradient(90deg, #f0f0f0 25%, #e8e8e8 50%, #f0f0f0 75%);
    background-size: 200% 100%;
    animation: skeleton-loading 1.5s infinite;
    border-radius: 50%;
  }

  @keyframes skeleton-loading {
    0% {
      background-position: 200% 0;
    }
    100% {
      background-position: -200% 0;
    }
  }

  .avatar {
    margin-right: 4px;
  }
</style>