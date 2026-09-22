<template>
  <view class="custom-tab-bar">
    <view class="tab-item" @click="navigateTo('/pagesMain/xieTong')">
      <img
        :src="
          currentPath === '/linkx/h5portal/pagesMain/xieTong' ? selectedIconXieTong : iconXieTong
        "
        class="tab-icon"
      />
      <text
        :class="{
          'active-text': currentPath === '/linkx/h5portal/pagesMain/xieTong',
        }"
        >协同</text
      >
    </view>
    <view class="tab-item" @click="navigateTo('/linkx/h5portal/pagesMain/renWu')">
      <img
        :src="currentPath === '/linkx/h5portal/pagesMain/renWu' ? selectedIconRenWu : iconRenWu"
        class="tab-icon"
      />
      <text
        :class="{
          'active-text': currentPath === '/linkx/h5portal/pagesMain/renWu',
        }"
        >任务</text
      >
    </view>
    <view class="middle-button" @click="navigateTo('/linkx/h5portal/pages/index')">
      <text class="button-text">5110</text>
    </view>
    <view class="tab-item" @click="navigateTo('/linkx/h5portal/pagesMain/tongXunLu')">
      <img
        :src="
          currentPath === '/linkx/h5portal/pagesMain/tongXunLu'
            ? selectedIconTongXunLu
            : iconTongXunLu
        "
        class="tab-icon"
      />
      <text
        :class="{
          'active-text': currentPath === '/linkx/h5portal/pagesMain/tongXunLu',
        }"
        >通讯录</text
      >
    </view>
    <view class="tab-item" @click="navigateTo('/linkx/h5portal/pagesMain/yingYong')">
      <img
        :src="
          currentPath === '/linkx/h5portal/pagesMain/yingYong' ? selectedIconYingYong : iconYingYong
        "
        class="tab-icon"
      />
      <text
        :class="{
          'active-text': currentPath === '/linkx/h5portal/pagesMain/yingYong',
        }"
        >应用</text
      >
    </view>
  </view>
</template>

<script>
  import { useRouter } from 'vue-router'; // ✅ Vue 3 使用 useRouter 来跳转路由

  import iconTongXunLuImg from '@/static/tabIcon/address_book.png';
  import iconXieTongImg from '@/static/tabIcon/message.png';
  import iconYingYongImg from '@/static/tabIcon/shield-star-fill.png';
  import iconRenWuImg from '@/static/tabIcon/task-square.png';
  import selectedIconXieTongImg from '@/static/tabIcon/temp_selected.png';
  // import selectedIconRenWuImg from '@/static/tabIcon/temp_selected.png';
  // import selectedIconTongXunLuImg from '@/static/tabIcon/temp_selected.png';
  // import selectedIconYingYongImg from '@/static/tabIcon/temp_selected.png';
  export default {
    props: {
      currentPath: {
        type: String,
        default: '',
      },
    },
    data() {
      return {
        router: useRouter(),
        // 这里的图标路径需要替换为您实际的图标文件路径
        iconXieTong: iconXieTongImg,
        selectedIconXieTong: selectedIconXieTongImg,
        iconRenWu: iconRenWuImg,
        selectedIconRenWu: selectedIconXieTongImg,
        iconTongXunLu: iconTongXunLuImg,
        selectedIconTongXunLu: selectedIconXieTongImg,
        iconYingYong: iconYingYongImg,
        selectedIconYingYong: selectedIconXieTongImg,
      };
    },
    methods: {
      navigateTo(path) {
        this.router.push(path);
      },
    },
  };
</script>

<style>
  .custom-tab-bar {
    position: fixed;
    bottom: 0;
    left: 0;
    right: 0;
    display: flex;
    justify-content: space-around;
    align-items: center;
    height: 50px;
    background-color: #ffffff;
    border-top: 1px solid #eee;
    padding-bottom: constant(safe-area-inset-bottom);
    padding-bottom: env(safe-area-inset-bottom);
    z-index: 999;
    will-change: transform;
    transform: translateZ(0);
    -webkit-transform: translateZ(0);
    backface-visibility: hidden;
    -webkit-backface-visibility: hidden;
  }

  .tab-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    flex: 1;
    font-size: 12px;
    color: #4d4d4d;
    will-change: transform;
    transform: translateZ(0);
    -webkit-transform: translateZ(0);
    backface-visibility: hidden;
    -webkit-backface-visibility: hidden;
  }

  .tab-icon {
    width: 22px;
    height: 22px;
    margin-bottom: 2px;
  }

  .active-text {
    color: #4d4d4d; /* 可以根据需求更改选中颜色 */
  }

  .middle-button {
    width: 50px;
    height: 50px;
    border-radius: 50%;
    background-color: #007aff;
    display: flex;
    justify-content: center;
    align-items: center;
    color: #ffffff;
    font-size: 14px;
    position: relative;
    top: -15px;
    box-shadow: 0px -2px 5px rgba(0, 0, 0, 0.2);
    will-change: transform;
    transform: translateZ(0);
    -webkit-transform: translateZ(0);
    backface-visibility: hidden;
    -webkit-backface-visibility: hidden;
  }
</style>
