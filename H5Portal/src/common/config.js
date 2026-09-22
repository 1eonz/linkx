import configApi from '@/config/env.js';
import projectLogo from '@/static/logo.png';
import { getBaseUrlAll } from '@/utils';

const pathname = getBaseUrlAll();
export const domain = configApi[import.meta.env.MODE].baseUrl + pathname;

// 缓存store实例，避免重复导入
let pageUrlStoreInstance = null;

// 获取动态baseUrl的函数
const DEV_BASE_URL = 'http://172.16.23.8:30280' + pathname;

export const getBaseUrl = () => {
  // 开发环境固定走本地调试地址
  if (import.meta.env.MODE === 'development') {
    return DEV_BASE_URL;
  }

  try {
    // 如果store实例已缓存，直接使用
    if (pageUrlStoreInstance) {
      return pageUrlStoreInstance.httpBaseUrl || '';
    }

    // 尝试从全局获取store实例
    if (typeof window !== 'undefined' && window.__PINIA_STORE__) {
      const store = window.__PINIA_STORE__.pageUrl;
      if (store) {
        pageUrlStoreInstance = store;
        return store.httpBaseUrl || '';
      }
    }

    return '';
  } catch (error) {
    console.warn('获取httpBaseUrl失败，使用默认值:', error);
    return '';
  }
};

// 设置store实例的方法，供外部调用
export const setPageUrlStore = (store) => {
  pageUrlStoreInstance = store;
  // 同时更新 baseUrl 的值
  baseUrl = getBaseUrl();
};

// 请求基础路径 - 动态获取pageUrl的httpBaseUrl
// 注意：这里不能直接调用 getBaseUrl()，因为此时 store 还未初始化
// 改为导出一个函数，在需要时动态获取
export const getBaseUrlValue = () => {
  const url = getBaseUrl();
  return url;
};

// 为了向后兼容，保留 baseUrl 导出，但改为动态获取
// 注意：这个值在模块加载时可能为空，建议使用 getBaseUrlValue() 函数
// 移除立即执行，改为延迟获取
export let baseUrl = '';

export const basePath = `http://${domain}`;

// 页面基础路径
// export const pageUrl = baseUrl.slice(0, -2);

// 静态资源访问路径
export const staticBaseUrl = `http://${domain}`;

// 获取动态websocketUrl的函数
export const getSocketUrl = () => {
  try {
    // 如果store实例已缓存，直接使用
    if (pageUrlStoreInstance) {
      return pageUrlStoreInstance.websocketUrl || `ws://${domain}/cagent`;
    }

    // 尝试从全局获取store实例
    if (typeof window !== 'undefined' && window.__PINIA_STORE__) {
      const store = window.__PINIA_STORE__.pageUrl;
      if (store) {
        pageUrlStoreInstance = store;
        return store.websocketUrl || `ws://${domain}/cagent`;
      }
    }

    return `ws://${domain}/cagent`;
  } catch (error) {
    console.warn('获取websocketUrl失败，使用默认值:', error);
    return `ws://${domain}/cagent`;
  }
};

// 模拟器链接路径
export const socketUrl = getSocketUrl();
// webSokcet 连接路径
// export const socketUrl = `ws://${location.host}/cagent`;

// export const socketUrl = `ws://10.28.64.83:30021/cagent`;

// 文件上传路径
export const uploadUrl = (getBaseUrl() || '') + '/api/upload';

// 项目 logo
export const logo = projectLogo;

// 系统信息
// export const sysInfo = uni.getSystemInfoSync();

// 版本号
// export const appVersion = sysInfo.appVersion;

// const { platform, uniPlatform } = sysInfo;

// export const isIos = platform == "ios";

// export const isIosApp = platform == "ios" && uniPlatform == "app";
