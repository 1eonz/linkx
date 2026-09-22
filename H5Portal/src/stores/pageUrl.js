import { defineStore } from 'pinia';

import { getBaseUrlAll } from '@/utils';

export const usePageUrlStore = defineStore('pageUrl', {
  state: () => ({
    pageUrl: null,
    isLoading: false,
    httpBaseUrl: null,
  }),

  getters: {
    // 获取完整的页面URL
    getFullPageUrl: (state) => (path) => {
      if (!state.pageUrl) return '';
      // 确保 path 以 / 开头，避免双斜杠
      const normalizedPath = path.startsWith('/') ? path : `/${path}`;
      return `${state.pageUrl}${normalizedPath}`;
    },

    // 获取视频监控URL
    getVideoMonitorUrl: (state) => {
      if (!state.pageUrl) return '';
      return `${state.pageUrl}/pages/resources`;
    },

    // 获取位置分享地址
    getLocationShareUrl: (state) => {
      if (!state.pageUrl) return '';
      return `${state.pageUrl}/pages/locationShare`;
    },
  },

  actions: {
    // 初始化pageUrl和httpBaseUrl
    async initPageUrl(refresh = false) {
      if (this.pageUrl && !refresh) {
        return this.pageUrl;
      }

      this.isLoading = true;
      try {
        //初始化pageUrl
        console.log(location, '===location');

        const origin = location.origin || '';
        // 提取base路径部分，支持网关前缀（如 /zxwg/linkx/h5portal）
        const basePath = getBaseUrlAll();
        // 移除URL末尾的斜杠，确保路径格式统一
        const baseUrl = (origin + basePath).replace(/\/$/, '');
        this.pageUrl = baseUrl;
        //初始化httpBaseUrl
        const originArr = baseUrl.split(origin);
        let httpBaseUrl = '';
        if (originArr.length > 0) {
          httpBaseUrl = originArr[1] || '';
        }
        this.httpBaseUrl = httpBaseUrl;
        console.log('===初始化pageUrl===', this.pageUrl);
        console.log('===初始化httpBaseUrl===', httpBaseUrl);

        return this.pageUrl;
      } catch (error) {
        console.error('获取pageUrl失败:', error);
        throw error;
      } finally {
        this.isLoading = false;
      }
    },

    // 设置pageUrl（用于测试或特殊情况）
    setPageUrl(url) {
      this.pageUrl = url;
    },

    // 重置pageUrl
    resetPageUrl() {
      this.pageUrl = null;
    },
  },
});
