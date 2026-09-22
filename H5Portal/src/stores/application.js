import { defineStore } from 'pinia';

import { getLayoutSections } from '@/common/api/h5.js';
import { h5Api } from '@/common/api/index.js';
import { SPECIAL_APP_ID } from '@/common/constants.js';
import { getGlobalsConfigByKey } from '@/common/utils';
import { useCommunicationStore } from '@/stores/communication.js';
import { useSystemConfigStore } from '@/stores/systemConfig.js';
import { preloadIconsToBase64 } from '@/utils/index.js';
import { transformImageUrl } from '@/utils/imgUrlParse.js';
import { logJxVersion } from '@/utils/version.js';
import { showCustomToast } from '@/utils/toast';

export const useApplicationStore = defineStore('application', {
  state: () => ({
    bannerList: [], // 轮播图
    time: '', //轮播图切换时间
    allApp: [], // 常用应用
    currentApp: [], // 当前应用
    sortType: null, // 排序类型
    scope: null, // 应用展示范围[1:鸿蒙移动端 | 2:安卓移动端 | 4:PC浏览器 | 8:PC桌面端]
  }),
  getters: {
    userInfo() {
      return useCommunicationStore().userInfo;
    },
    userId() {
      return useCommunicationStore().userInfo?.userid;
    },
  },
  actions: {
    // 查询轮播图
    async getBannerData() {
      try {
        const res = await getGlobalsConfigByKey('swiper-interval');
        if (res) {
          this.time = Number(res) < 3 ? 3000 : res + '000';
        }
      } catch (e) {
        return Promise.reject(e);
      }

      try {
        const res = await h5Api.getBannerPage({ pageNum: 1, pageSize: 100 });
        const list = res.records;
        list.forEach((item) => {
          item.pciUrl = transformImageUrl('/admin-api' + item.pciUrl);
        });
        this.bannerList = list;
      } catch (e) {
        return Promise.reject(e);
      }
    },
    // 查询全部应用app
    async getAllApp(init) {
      try {
        
        const versionInfo = await logJxVersion();
        const deviceType = versionInfo?.deviceType || '';

        let scope = undefined;
        if (deviceType === 'ohos') {
          scope = 1;
        } else if (deviceType === 'android') {
          scope = 2;
        } else if (deviceType === 'pc') {
          scope = 4;
        }

        const res = await h5Api.getAppPage({ pageNum: 1, pageSize: 100, scope: scope });
        this.allApp = res?.records
          .filter((item) => item.status === 0)
          .sort((a, b) => (a.sort ?? 0) - (b.sort ?? 0));
        preloadIconsToBase64(this.allApp);
        if (init) {
          // 获取板块配置来计算默认保存数量
          let maxCount = null;
          try {
            const sectionsRes = await getLayoutSections();
            const commonAppSection = sectionsRes?.find((item) => item.type === 2);
            if (commonAppSection?.custom) {
              const custom = JSON.parse(commonAppSection.custom);
              if (custom.rowCount) {
                const systemConfigStore = useSystemConfigStore();
                const columnNum = systemConfigStore.getAppCountByDevice();
                maxCount = columnNum * parseInt(custom.rowCount);
              }
            }
          } catch (e) {
            console.error('获取板块配置失败:', e);
          }
          this.saveCurrentApp(init, maxCount);
        }
      } catch (e) {
        return Promise.reject(e);
      }
    },
    // 查询
    async getCurrentApp() {
      if (!this.userId) {
        await useCommunicationStore().getUserInfo();
      }
      const systemConfigStore = useSystemConfigStore();
      const type = systemConfigStore.getDeviceTypeValue();
      const sortType = await this.getSortType(this.userId);
      console.info('当前使用排序类型，1-热度排序，2-固定排序', sortType);
      
      // 增加应用范围展示逻辑
      const versionInfo = await logJxVersion();
      const deviceType = versionInfo?.deviceType || '';
      let scope = undefined;

      if (deviceType === 'ohos') {
        scope = 1;
      } else if (deviceType === 'android') {
        scope = 2;
      } else if (deviceType === 'pc') {
        scope = 4;
      }

      let res = [];
      if (sortType == 1) {
        // 热度排序
        res = (await h5Api.getAppUsedRanking({ userId: this.userId, type, scope: scope })) || [];
      } else {
        // 固定排序
        res = (await h5Api.getCommonApp({ userId: this.userId, type, scope: scope })) || [];
      }

      this.currentApp = res
        ?.filter((item) => item)
        ?.map((item) => {
          if (item.app.status === 0) {
            return item.app;
          }
        });
      preloadIconsToBase64(this.currentApp);
      //initStatus为真表示修改过，修改过的话就不用再走saveCurrentApp方法去保存默认前8个应用了
      const initStatus = (await h5Api.getInitStatus({ userId: this.userId, type })) || false;

      if (!initStatus) {
        this.getAllApp(true);
      }
    },
    // 保存
    async saveCurrentApp(init, maxCount) {
      if (!this.userId) {
        await useCommunicationStore().getUserInfo();
      }
      const systemConfigStore = useSystemConfigStore();
      const type = systemConfigStore.getDeviceTypeValue();

      let apps = [];
      let list = [];
      if (init) {
        // 用户初始时根据设备类型和配置决定默认应用数量
        const columnNum = systemConfigStore.getAppCountByDevice();
        // 如果传入了 maxCount 则使用，否则默认取 columnNum * 2（2行）
        const defaultCount = maxCount || columnNum * 2;

        // 查找特殊应用（需要保存在首位）
        const specialApp = this.allApp.find((item) => item.id === SPECIAL_APP_ID);

        // 获取其他应用（排除特殊应用）
        let otherApps = this.allApp.filter((item) => item.id !== SPECIAL_APP_ID);

        // 构建最终列表
        let finalApps;
        if (specialApp) {
          // 特殊应用放首位，其他应用取 defaultCount - 1 个
          otherApps = otherApps.slice(0, defaultCount - 1);
          finalApps = [specialApp, ...otherApps];
        } else {
          finalApps = otherApps.slice(0, defaultCount);
        }

        apps = finalApps.map((item, index) => {
          return { id: item.id, sort: index };
        });
      } else {
        // 根据当前数组顺序生成 sort 值（从 0 开始）
        list = this.currentApp.filter((item) => item);
        // 如果传入了 maxCount，则截断到 maxCount
        if (maxCount && list.length > maxCount) {
          list = list.slice(0, maxCount);
        }
        apps = list.map((item, index) => {
          return { id: item.id, sort: index };
        });
      }

      const params = { userId: this.userId, type, apps };
      try {
        await h5Api.saveCommonApp(params);
        if (init) {
          setTimeout(() => {
            this.getCurrentApp();
          }, 300);
        } else {
          // 更新本地数据为截断后的列表
          this.currentApp = list;
          const randomNum = Math.floor(Math.random() * 1000).toString();
          this.setStorage('currentAppSet', randomNum);
          showCustomToast('保存成功');
        }
      } catch {
        showCustomToast('保存失败');
      }
    },

    // 修改（添加/移除应用，不排序）
    async setCurrentApp(data) {
      const ret = this.currentApp.filter((item) => item);
      const index = ret.findIndex((item) => {
        return item.id === data.id;
      });
      if (index === -1) {
        // 添加到末尾
        ret.push(data);
      } else {
        // 移除
        ret.splice(index, 1);
      }
      this.currentApp = ret;
    },

    // 更新应用顺序（用于拖拽排序）
    // 参数支持两种形式：
    // 1. 传入新数组: updateCurrentAppOrder(newList)
    // 2. 传入索引: updateCurrentAppOrder({ oldIndex, newIndex })
    updateCurrentAppOrder(params) {
      if (Array.isArray(params)) {
        // 直接传入新数组
        this.currentApp = params;
      } else if (
        params &&
        typeof params === 'object' &&
        'oldIndex' in params &&
        'newIndex' in params
      ) {
        // 传入 { oldIndex, newIndex }
        const { oldIndex, newIndex } = params;
        const ret = this.currentApp.filter((item) => item);
        const [removed] = ret.splice(oldIndex, 1);
        ret.splice(newIndex, 0, removed);
        this.currentApp = ret;
      }
    },

    // 设置存储
    async setStorage(key, value) {
      try {
        if (window.WeSpaceSDK) {
          await window.WeSpaceSDK.setStorage(key, value);
          return true;
        }
        return false;
      } catch (error) {
        console.error('设置存储失败:', error);
        return false;
      }
    },

    // 获取存储
    async getStorage(key) {
      try {
        if (window.WeSpaceSDK) {
          return await window.WeSpaceSDK.getStorage(key);
        }
        return null;
      } catch (error) {
        console.error('获取存储失败:', error);
        return null;
      }
    },

    // 获取排序类型
    async getSortType(userId) {
      try {
        const res = await h5Api.getSortType({ userId });
        this.sortType = res || 2;
        return res || 2;
      } catch (error) {
        console.error('获取用户偏好排序类型失败:', error);
        this.sortType = 2;
        return 2;
      }
    },
    // 设置排序类型
    async setSortType(val) {
      this.sortType = val;
      await this.getCurrentApp();
    },
  },
});
