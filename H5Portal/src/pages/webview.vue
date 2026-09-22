<template>
  <div class="webview-container">
    <!-- Vant NavBar 导航栏 -->
    <van-nav-bar
      :title="title"
      left-text="返回"
      left-arrow
      @click-left="onClickLeft"
      :border="false"
      custom-class="custom-navbar"
    >
      <!-- 自定义标题插槽（如果需要） -->
      <!-- <template #title>
        <span class="nav-title">{{ title }}</span>
      </template> -->
    </van-nav-bar>

    <!-- iframe 作为 webview 替代 -->
    <iframe
      :src="url"
      :style="iframeStyles"
      frameborder="0"
      scrolling="auto"
      class="webview-iframe"
    ></iframe>
  </div>
</template>

<script setup>
  import { ref, reactive, onMounted } from 'vue';
  import { useRouter, useRoute } from 'vue-router';

  import { useCommunicationStore } from '@/stores/communication.js';
  const communicationStore = useCommunicationStore();
  const router = useRouter();
  const route = useRoute();

  // 响应式数据
  const title = ref('');
  const url = ref('');

  // 系统信息（简化版，实际项目中可能需要更复杂的处理）
  const navbarHeight = ref(46); // Vant NavBar 默认高度约 46px
  const statusBarHeight = ref(20); // 需要根据实际情况调整

  // iframe 样式
  const iframeStyles = reactive({
    position: 'fixed',
    top: '46px', // 导航栏高度
    left: 0,
    right: 0,
    bottom: 0,
    width: '100%',
    height: 'calc(100vh - 46px)',
    border: 'none',
  });

  // 模拟 UniApp 的 onLoad 生命周期
  onMounted(() => {
    // 从路由参数获取参数（模拟 UniApp 的 onLoad）
    const query = route.query;
    title.value = query.title || '外部链接';
    url.value = query.url || 'https://www.baidu.com';

    // 获取系统信息（这里需要实际项目中的实现）
    initSystemInfo();
  });

  // 初始化系统信息
  const initSystemInfo = async () => {
    // 在实际项目中，可以通过其他方式获取状态栏高度
    // 这里使用固定值作为示例
    try {
      // 如果是移动端，可以尝试获取更精确的高度
      const isMobile = /Android|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
        navigator.userAgent,
      );
      if (isMobile) {
        statusBarHeight.value = await communicationStore.fetchStatusBarHeight(); // 移动端状态栏大致高度
        navbarHeight.value = statusBarHeight.value + 46;
        iframeStyles.top = navbarHeight.value + 'px';
        iframeStyles.height = `calc(100vh - ${navbarHeight.value}px)`;
      }
    } catch (error) {
      console.warn('无法获取精确的系统信息，使用默认高度');
    }
  };

  // 点击返回按钮
  const onClickLeft = () => {
    // 检查 iframe 是否可以返回
    try {
      const iframe = document.querySelector('.webview-iframe');
      if (iframe && iframe.contentWindow && iframe.contentWindow.history.length > 1) {
        iframe.contentWindow.history.back();
      } else {
        // 返回上一页
        if (window.history.length > 1) {
          router.go(-1);
        } else {
          router.push('/home'); // 跳转到首页
        }
      }
    } catch (error) {
      // 跨域情况下无法直接操作 iframe history，直接返回
      if (window.history.length > 1) {
        router.go(-1);
      } else {
        router.push('/home');
      }
    }
  };

  // 暴露给父组件的方法（如果需要）
  defineExpose({
    title,
    url,
  });
</script>

<style lang="scss" scoped>
  .webview-container {
    position: relative;
    width: 100%;
    height: 100vh;
    overflow: hidden;
  }

  // 自定义导航栏样式
  :deep(.custom-navbar) {
    background-color: #1989fa;

    .van-nav-bar__title {
      color: white;
      font-weight: 500;
    }

    .van-nav-bar__left {
      .van-nav-bar__arrow {
        color: white;
      }

      .van-nav-bar__text {
        color: white;
      }
    }
  }

  .nav-title {
    color: white;
    font-size: 16px;
    font-weight: 500;
  }

  // iframe 样式
  .webview-iframe {
    display: block;
    background-color: #fff;

    // 加载状态
    &::before {
      content: '';
      position: absolute;
      top: 50%;
      left: 50%;
      transform: translate(-50%, -50%);
      width: 32px;
      height: 32px;
      border: 2px solid #f0f0f0;
      border-top-color: #1989fa;
      border-radius: 50%;
      animation: spin 1s linear infinite;
    }
  }

  @keyframes spin {
    to {
      transform: translate(-50%, -50%) rotate(360deg);
    }
  }
</style>
