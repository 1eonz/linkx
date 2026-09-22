<template>
  <view class="wrapper">
    <view class="container">
      <!-- <x-fixed-header> -->
      <view class="userInfo">
        <!-- <x-placeholder></x-placeholder> -->
        <!-- <image class="size-50 radius-10 mt-35" :src="$joinUrl(userInfo.avatar) || logo" mode=""></image> -->
        <!-- <image
            class="size-50 radius-10 mt-35"
            src="/static/tabIcon/me.png"
            mode=""
          ></image> -->
        <img
          class="size-50 radius-10 mt-35"
          style="width: 25px; height: 25px; border-radius: 10px"
          src="@/static/tabIcon/me.png"
        />
        <text style="color: #ffffff">应用</text>
      </view>
      <!-- </x-fixed-header> -->
      <view class="common-app">
        <view class="section-header" @click="navigateToUrl('/pages/application?param=0')">
          <text class="section-title">常用应用</text>
          <van-icon name="arrow" :size="adaptationSize.iconSize" />
        </view>
        <view class="app-grid">
          <!-- <uv-grid v-if="currentApp.length" :col="4">
            <uv-grid-item
              class="app-item"
              v-for="(item, index) in currentApp"
              :key="index"
              @click="handleClickApp(item)"
            >
              <uv-image
                :src="transformImageUrl(`/admin-api${item?.icon}`)"
                width="50px"
                height="50px"
                radius="14px"
              ></uv-image>
              <text class="app-name">{{ item?.name }}</text>
            </uv-grid-item>
          </uv-grid> -->
          <van-grid v-if="currentApp.length" :column-num="4" :border="false">
            <van-grid-item
              v-for="(item, index) in currentApp"
              :key="index"
              @click="handleClickApp(item)"
            >
              <view class="app-item">
                <img
                  :src="transformImageUrl(`/admin-api${item?.icon}`)"
                  width="50px"
                  height="50px"
                  style="border-radius: 14px"
                />
                <text class="app-name">{{ item?.name }}</text>
              </view>
            </van-grid-item>
          </van-grid>
          <view v-else class="app-empty">
            <text class="empty-text">还没有应用哦</text>
            <view class="empty-btn" @click="navigateToUrl('/pages/application?param=1')">
              添加
            </view>
          </view>
        </view>
      </view>
      <view class="common-app">
        <view class="section-header" @click="navigateToUrl('/pages/application?param=0')">
          <text class="section-title">本地应用</text>
          <van-icon name="arrow" :size="adaptationSize.iconSize" />
        </view>
        <view class="app-grid">
          <!-- <uv-grid v-if="currentApp.length" :col="4">
            <uv-grid-item
              class="app-item"
              v-for="(item, index) in currentApp"
              :key="index"
              @click="handleClickApp(item)"
            >
              <uv-image
                :src="transformImageUrl(`/admin-api${item?.icon}`)"
                width="50px"
                height="50px"
                radius="14px"
              ></uv-image>
              <text class="app-name">{{ item?.name }}</text>
            </uv-grid-item>
          </uv-grid> -->
          <van-grid v-if="currentApp.length" :column-num="4" :border="false">
            <van-grid-item
              v-for="(item, index) in currentApp"
              :key="index"
              @click="handleClickApp(item)"
            >
              <view class="app-item">
                <img
                  :src="transformImageUrl(`/admin-api${item?.icon}`)"
                  width="50px"
                  height="50px"
                  style="border-radius: 14px"
                />
                <text class="app-name">{{ item?.name }}</text>
              </view>
            </van-grid-item>
          </van-grid>
          <view v-else class="app-empty">
            <text class="empty-text">还没有应用哦</text>
            <view class="empty-btn" @click="navigateToUrl('/pages/application?param=1')">
              添加
            </view>
          </view>
        </view>
      </view>
      <view class="common-app">
        <view class="section-header" @click="navigateToUrl('/pages/application?param=0')">
          <text class="section-title">全国应用</text>
          <uv-icon name="arrow-right" :size="adaptationSize.iconSize"></uv-icon>
        </view>
        <view class="app-grid">
          <!-- <uv-grid v-if="allApp.length" :col="4">
            <uv-grid-item
              class="app-item"
              v-for="(item, index) in allApp"
              :key="index"
              @click="handleClickApp(item)"
            >
              <uv-image
                :src="transformImageUrl(`/admin-api${item?.icon}`)"
                width="50px"
                height="50px"
                radius="14px"
              ></uv-image>
              <text class="app-name">{{ item?.name }}</text>
            </uv-grid-item>
          </uv-grid> -->
          <van-grid v-if="allApp.length" :column-num="4" :border="false">
            <van-grid-item
              v-for="(item, index) in allApp"
              :key="index"
              @click="handleClickApp(item)"
            >
              <view class="app-item">
                <img
                  :src="transformImageUrl(`/admin-api${item?.icon}`)"
                  width="50px"
                  height="50px"
                  style="border-radius: 14px"
                />
                <text class="app-name">{{ item?.name }}</text>
              </view>
            </van-grid-item>
          </van-grid>
          <view v-else class="app-empty">
            <text class="empty-text">还没有应用哦</text>
            <view class="empty-btn" @click="navigateToUrl('/pages/application?param=1')">
              添加
            </view>
          </view>
        </view>
      </view>
    </view>
    <custom-tab-bar :current-path="currentPath"></custom-tab-bar>
  </view>
</template>

<script>
  import { useRoute } from 'vue-router';

  import CustomTabBar from '@/components/CustomTabBar/CustomTabBar.vue';
  import { useCommon } from '@/hooks/useCommon.js';
  import { useApplicationStore } from '@/stores/application.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';

  export default {
    components: {
      CustomTabBar,
    },
    data() {
      const { adaptationSize } = useDeviceAdapter();
      return {
        adaptationSize,
        applicationStore: useApplicationStore(),
        communicationStore: useCommunicationStore(),
        pageUrlStore: usePageUrlStore(),
        userStore: null,
        userInfo: null,
        bodyInfo: null,
        route: useRoute(),
      };
    },
    created() {
      // 调用useCommon获取数据
      const { userStore } = useCommon();
      this.userStore = userStore;

      // 从userStore解构数据
      const { userInfo } = userStore;
      this.userInfo = userInfo;
    },
    computed: {
      currentApp() {
        return this.applicationStore.currentApp;
      },
      allApp() {
        return this.applicationStore.allApp;
      },
      currentPath() {
        // let pages = getCurrentPages();
        // let currentPage = pages[pages.length - 1];
        // return `/${currentPage.route}`;
        return this.route.path;
      },
    },
    methods: {
      handleClickApp(item) {
        if (item.name === '设备调度') {
          const videoUrl = this.pageUrlStore.getVideoMonitorUrl;
          this.communicationStore.openUrl(videoUrl);
        } else if (item.name === '测试开关') {
          const url = `http://10.28.65.65:8001/linkx/h5portal/`;
          this.communicationStore.openUrl(url);
        } else {
          const path = item.params ? `${item.url}?${item.params}` : item.url;
          if (item.appId) {
            this.communicationStore.openApplet(item.appId);
          } else if (item.url.startsWith('http')) {
            this.communicationStore.openUrlApp(path);
          } else if (item.url) {
            this.communicationStore.openApp(path);
          }
        }
      },
      navigateToUrl(path) {
        if (path.includes('application')) {
          this.applicationStore.getCurrentApp();
        }
        const url = this.pageUrlStore.getFullPageUrl(path);
        this.communicationStore.openUrl(url);
      },
    },
  };
</script>

<style scoped lang="scss">
  .container {
    background-color: #f7f7f7;
    box-sizing: border-box;
    width: 100%;
    height: 100vh;
    display: flex;
    flex-direction: column;
    overflow-y: auto;
  }
  .userInfo {
    padding: 10px;
    // margin-left: 8px;
    background-color: #264ed1;
    display: flex;
    align-items: center;
    padding-right: (var(--x-safe-right));
    // padding-top: (var(--x-safe-top));

    .text {
      margin-top: 10px;
      font-size: 16px;
    }
  }
  .common-app,
  .third-part {
    background-color: #fff;
    margin: 8px 16px 0;
    border-radius: 12px;
  }
  @media (min-height: 667px) and (max-height: 1200px) {
    .common-app {
      height: 300px;
    }
  }
  @media (min-height: 1200px) and (max-height: 1600px) {
    .common-app {
      height: 400px;
    }
  }
  @media (min-height: 1600px) and (max-height: 2560px) {
    .common-app {
      height: 500px;
    }
  }
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px;

    .section-title {
      font-size: 14px;
      font-weight: 500;
      line-height: 22px;
      height: 22px;
    }
    /* 平板适配 */
    @media screen and (min-height: 1200px) {
      .section-title {
        font-size: 1.5rem;
      }
    }

    /* 大屏设备适配 */
    @media screen and (min-height: 2560px) {
      .section-title {
        font-size: 1.7rem;
      }
    }
    .section-more {
      font-size: clamp(12px, 3vw, 14px);
      color: #999;
    }
  }

  .app-grid {
    height: 168px;
    overflow-y: auto;

    .app-item {
      margin-bottom: 12px;
      width: 100%;
      text-align: center;
    }

    .app-name {
      display: inline-block;
      font-size: 11px;
      color: #5a6383;
      font-weight: 500;
      line-height: 20px;
      margin-top: 2px;
      width: 100%;
      text-align: center;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
    /* 平板适配 */
    @media screen and (min-height: 1200px) {
      .app-name {
        font-size: 2rem;
        line-height: 60px;
      }
    }

    /* 大屏设备适配 */
    @media screen and (min-height: 2560px) {
      .app-name {
        font-size: 2.5rem;
        line-height: 60px;
      }
    }

    .app-empty {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      height: 100%;

      .empty-text {
        font-size: 14px;
        color: #898fa3;
      }
      .empty-btn {
        width: 70px;
        height: 28px;
        border-radius: 72px;
        background-color: #1e52f2;
        color: #fff;
        line-height: 28px;
        text-align: center;
        font-size: 12px;
        margin-top: 12px;
      }
      /* 平板适配 */
      @media screen and (min-height: 1200px) {
        .empty-text {
          font-size: 1.5rem;
        }
        .empty-btn {
          width: 240px;
          height: 66px;
          line-height: 66px;
          font-size: 1.5rem;
          margin-top: 24px;
        }
      }

      /* 大屏设备适配 */
      @media screen and (min-height: 2560px) {
        .empty-text {
          font-size: 2rem;
        }
        .empty-btn {
          width: 240px;
          height: 66px;
          line-height: 66px;
          font-size: 1.5rem;
          margin-top: 24px;
        }
      }
    }
  }
</style>
