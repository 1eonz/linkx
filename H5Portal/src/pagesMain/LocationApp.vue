<template>
  <view class="container-box">
    <!-- 加载中 -->
    <view v-if="loading" class="loading-container">
      <van-loading size="24px" color="#1e52f2" />
    </view>

    <!-- 正常内容 -->
    <view
      v-else-if="licensePermissions.LINKXBS === '1' && licensePermissions.LINKXBCF === '1'"
      class="common-app"
    >
      <view
        class="section-header"
        @click="navigateToUrl('/pages/application?param=0', null, 'noTitleStyle')"
      >
        <text class="section-title">本地应用</text>
        <view class="arrow-right"></view>
      </view>
      <view class="app-grid">
        <van-grid v-if="allApp.length" :column-num="4" :border="false">
          <van-grid-item v-for="(item, index) in allApp" :key="index" @click="handleClickApp(item)">
            <view class="app-item" style="width: 100%; text-align: center">
              <van-image
                :src="transformImageUrl(`/admin-api${item?.icon}`)"
                width="38px"
                height="38px"
                radius="14px"
              />
              <view class="app-name">{{ item?.name }}</view>
            </view>
          </van-grid-item>
        </van-grid>
        <view v-else class="app-empty">
          <text class="empty-text">还没有应用哦</text>
          <view
            class="empty-btn"
            @click="navigateToUrl('/pages/application?param=1', null, 'noTitleStyle')"
          >
            添加
          </view>
        </view>
      </view>
    </view>

    <!-- 功能受限 -->
    <view v-else class="license-limit">本地应用功能受限，请联系管理员</view>
  </view>
</template>

<script>
  import { h5Api } from '@/common/api/index.js';
  import { NAVIGATE_APP_IDS } from '@/common/constants.js';
  import { getGlobalsConfigByKey } from '@/common/utils';
  import { useCommon } from '@/hooks/useCommon.js';
  import { useApplicationStore } from '@/stores/application.js';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { usePageUrlStore } from '@/stores/pageUrl.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { useSystemConfigStore } from '@/stores/systemConfig.js';
  import { useUserStore } from '@/stores/user.js';
  import { locationShareFunc } from '@/utils';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
  import { openAppByCheckPre } from '@/utils/appHandler.js';
  export default {
    components: {},
    data() {
      const { adaptationSize } = useDeviceAdapter();
      return {
        adaptationSize,
        applicationStore: useApplicationStore(),
        communicationStore: useCommunicationStore(),
        pageUrlStore: usePageUrlStore(),
        systemConfigStore: useSystemConfigStore(),
        userStore: null,
        userInfo: null,
        bodyInfo: null,
        licensePermissions: {},
        loading: true, // 加载状态
      };
    },
    async created() {
      // 调用useCommon获取数据
      const { userStore } = useCommon();
      this.userStore = userStore;

      // 从userStore解构数据
      const { userInfo } = userStore;
      this.userInfo = userInfo;
      await this.initData();
      await this.setDebugQueryString();
      await this.getLicensePermissionsFunc();
      this.loading = false;
    },
    computed: {
      currentApp() {
        return this.applicationStore.currentApp;
      },
      allApp() {
        return this.applicationStore.allApp.slice(0, 8);
      },
      currentPath() {
        let pages = getCurrentPages();
        let currentPage = pages[pages.length - 1];
        return `/${currentPage.route}`;
      },
    },
    methods: {
      transformImageUrl,
      //获取license权限
      async getLicensePermissionsFunc() {
        try {
          const licensePermissionsRes = await h5Api.getLicensePermissions({});
          this.licensePermissions = licensePermissionsRes || {};
        } finally {
          this.loading = false;
        }
      },
      // 设置调试参数
    async setDebugQueryString() {
        try {
          const queryString = window?.location?.search;
          const urlParams = new URLSearchParams(queryString || '');
          window?.WeSpaceSDK?.setStorage(
            'debugQueryString',
            urlParams.get('debug') === 'true' ? 'debug=true' : '',
          );
        } catch (error) {
          console.log(error);
        }
      },
      async handleClickApp(item) {
        const { name, params, url, appId, icon, id } = item;
        if (NAVIGATE_APP_IDS.includes(Number(id)) && url) {
          this.navigateToUrl(url, null, 'noTitleStyle');
          setUseRecord(id);
          return;
        }
        if (name === '设备调度') {
          const videoUrl = this.pageUrlStore.getVideoMonitorUrl;
          await this.communicationStore.openUrl(videoUrl, null, 'noTitleStyle');
        } else if (name === '位置共享') {
          locationShareFunc(item);
        } else if (name === '测试开关') {
          const url = `http://10.28.64.52:8001/linkx/h5portal/`;
          await this.communicationStore.openUrl(url);
        } else if (name === '测试yang') {
          const url = `http://172.28.89.159:8001/linkx/h5portal/pagesMain/yingYong`;
          await this.communicationStore.openUrl(url);
        } else if (name === '全国警信测试') {
          const url = `http://10.28.64.75:8001/linkx/h5portal/`;
          await this.communicationStore.openUrl(url);
        } else if (name === 'xl测试') {
          const url = `http://10.28.64.57:8001/linkx/h5portal/`;
          await this.communicationStore.openUrl(url);
        } else {
          await openAppByCheckPre(item);
        }
        setUseRecord(id);
      },
      // 添加使用记录
      setUseRecord(id) {
        try {
          const type = this.systemConfigStore.getDeviceTypeValue();
          h5Api.createAppUsedRecord(id, {
            appId: id,
            userId: this.userInfo.userid,
            client: type,
          });
        } catch (error) {
          console.error(`获取用户信息错误: ${error.message}`);
        }
      },
      navigateToUrl(path, title = null, titleStyle = null) {
        if (path.includes('application')) {
          this.applicationStore.getCurrentApp();
        }
        const url = this.pageUrlStore.getFullPageUrl(path);
        this.communicationStore.openUrl(url, title, titleStyle);
      },
      async initData() {
        // 确保 token 登录，解决直接进入页面时请求不携带 token 的问题
        const userStore = useUserStore();
        await userStore.setUserInfo();
        await this.systemConfigStore.fetchSystemConfig();
        await this.pageUrlStore.initPageUrl();
        this.title = await getGlobalsConfigByKey('title');
        await this.applicationStore.getAllApp();
        await this.applicationStore.getCurrentApp();
        await this.communicationStore.h5permissions();
      },
    },
  };
</script>

<style scoped lang="scss">
  .container-box {
    background-color: #f7f7f7;
    box-sizing: border-box;
    width: 100%;
    height: 180px;
    display: flex;
    flex-direction: column;
  }
  .loading-container {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
  }
  .license-limit {
    width: 100%;
    height: 100%;
    display: flex;
    justify-content: center;
    align-items: center;
    font-size: 16px;
    font-weight: 500;
    color: #333333;
  }
  .common-app,
  .third-part {
    background-color: #fff;
    border-radius: 12px;
  }
  // @media (min-height: 667px) and (max-height: 1200px) {
  //   .common-app {
  // 	height: 200px;
  // 	overflow: hidden;
  //   }
  // }
  // @media (min-height: 1200px) and (max-height: 1600px) {
  //   .common-app {
  //     height: 270px;
  //     overflow: hidden;
  //   }
  // }
  // @media (min-height: 1600px) and (max-height: 2560px) {
  //   .common-app {
  //     height: 420px;
  //     overflow: hidden;
  //   }
  // }
  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 9px 12px;

    .section-title {
      font-size: 14px;
      font-weight: 500;
      line-height: 22px;
      height: 22px;
    }
    .arrow-right {
      width: 7px;
      height: 7px;
      border-right: 1px solid rgb(96, 98, 102);
      border-bottom: 1px solid rgb(96, 98, 102);
      transform: rotate(-45deg);
    }
    /* 平板适配 */
    // @media screen and (min-height: 1200px) {
    //   .section-title {
    //     font-size: 1.5rem;
    //   }
    // }

    /* 大屏设备适配 */
    // @media screen and (min-height: 2560px) {
    //   .section-title {
    //     font-size: 1.7rem;
    //   }
    // }
    .section-more {
      font-size: clamp(12px, 3vw, 14px);
      color: #999;
    }
  }

  .app-grid {
    // height: 150px;
    :deep(.van-grid-item__content) {
      padding: 0px;
    }
    .app-item {
      margin-bottom: 2px;
    }

    .app-name {
      display: inline-block;
      font-size: 10px;
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
    // @media screen and (min-height: 1200px) {
    //   .app-name {
    //     font-size: 0.8rem;
    //     line-height: 60px;
    //   }
    //   .app-item {
    //     margin-bottom: 60px;
    //   }
    // }
    // @media screen and (min-height: 2001px) {
    //   .app-name {
    //     font-size: 0.6rem;
    //   }
    // }

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
        margin-bottom: 20px;
      }
      /* 平板适配 */
      @media screen and (min-height: 1200px) {
        .empty-text {
          font-size: 0.8rem;
        }
        .empty-btn {
          width: 340px;
          height: 90px;
          line-height: 90px;
          font-size: 0.8rem;
          margin-top: 24px;
          margin-bottom: 80px;
        }
      }
      @media screen and (min-height: 2001px) {
        .empty-text,
        .empty-btn {
          font-size: 0.6rem;
        }
      }
    }
  }
</style>
