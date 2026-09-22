<template>
  <view class="container">
    <text>任务页面</text>
  </view>
  <custom-tab-bar :current-path="currentPath"></custom-tab-bar>
</template>

<script>
  import { useRoute } from 'vue-router';

  import CustomTabBar from '@/components/CustomTabBar/CustomTabBar.vue';

  export default {
    components: {
      CustomTabBar,
    },
    data() {
      return {
        route: useRoute(),
      };
    },
    computed: {
      currentPath() {
        return this.route.path;
        // let pages = getCurrentPages();
        // let currentPage = pages[pages.length - 1];
        // return `/${currentPage.route}`;
      },
    },
    methods: {},
  };
</script>

<style>
  .container {
    display: flex;
    flex-direction: column; /* 调整为列方向 */
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    font-size: 24px;
    padding-bottom: 50px; /* 为底部导航栏留出空间，根据 CustomTabBar 的高度调整 */
    box-sizing: border-box; /* 确保 padding 包含在高度内 */
  }
</style>
