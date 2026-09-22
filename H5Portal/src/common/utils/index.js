import { userApi, commonApi, h5Api } from '@/common/api/index.js';
import { themeStyle } from '@/common/themeStyle.js';
// import { isTabBarPage } from "@/uni_modules/x-tools/tools/index.js";

let setThemeIconTimer = null;
export const setThemeIcon = function (theme, ms = 0) {
  setThemeIconTimer && clearTimeout(setThemeIconTimer);

  setThemeIconTimer = setTimeout(() => {
    // if (!isTabBarPage()) return;

    uni.setStorageSync('theme', theme);

    uni.setTabBarStyle({
      selectedColor: themeStyle[theme]['--theme-color'],
    });

    // 设置每个 TabBar 的主题图标
    ['home', 'me'].forEach((i, index) => {
      uni.setTabBarItem({
        index,
        selectedIconPath: `/static/tabIcon/${i}_${theme}.png`,
      });
    });

    setThemeIconTimer = null;
  }, ms);
};

// 微信支付
export const wxPay = async (orderNo) => {
  if (!orderNo) return Promise.reject();

  const params = await commonApi.wxPay({
    orderNo,
  });

  await uni.requestPayment({
    provider: 'wxpay',
    orderInfo: params,
  });
};

// 微信小程序支付
export const mpWxPay = async (orderNo) => {
  if (!orderNo) return Promise.reject();

  const params = await commonApi.mpWxPay({
    orderNo,
  });

  await uni.requestPayment({
    provider: 'wxpay',
    timeStamp: params.timeStamp,
    nonceStr: params.nonceStr,
    package: params.package,
    signType: params.signType,
    paySign: params.sign,
  });
};

// 微信小程序手机号登陆
export const mpWxPhoneLogin = async function (phoneNumberInfo = {}) {
  try {
    if (!phoneNumberInfo) return Promise.reject('手机号信息缺失');

    delete phoneNumberInfo.errMsg;

    const { code } = await uni.login();

    const { iv, encryptedData } = await uni.getUserInfo();

    const userInfo = await userApi.login({
      code,
      iv,
      encryptedData,
      phoneNumberInfo,
    });

    const { token } = userInfo;

    if (token) {
      return token;
    }

    return Promise.reject(userInfo);
  } catch (error) {
    return Promise.reject(error);
  }
};

// 获取位置信息
export const getLocation = async function (regeo = true, options = {}) {
  // #ifdef H5
  return {
    area: '福田区',
    city: '深圳市',
    province: '广东省',
    latitude: 22.520922,
    longitude: 114.055198,
  };
  // #endif
  const { longitude, latitude } = await uni.getLocation({
    type: 'gcj02',
    ...options,
  });

  if (regeo) {
    const { provinceName, cityName, areaName } = await commonApi.regeo({
      longitude,
      latitude,
    });

    return {
      longitude,
      latitude,
      province: provinceName,
      city: cityName,
      area: areaName,
    };
  }

  return {
    longitude,
    latitude,
  };
};

// 选择位置
export const chooseLocation = async function (regeo = true, options = {}) {
  // #ifdef H5
  return {
    address: '广东省深圳市宝安区海秀路19号',
    area: '宝安区',
    city: '深圳市',
    latitude: 22.526018,
    longitude: 114.036043,
    name: '国际西岸商务大厦',
    province: '广东省',
  };
  // #endif

  const { longitude, latitude, name, address } = await uni.chooseLocation(options);

  if (regeo) {
    const { provinceName, cityName, areaName } = await commonApi.regeo({
      longitude,
      latitude,
    });

    return {
      name,
      address,
      longitude,
      latitude,
      province: provinceName,
      city: cityName,
      area: areaName,
    };
  }

  return {
    name,
    address,
    longitude,
    latitude,
  };
};

// 格式化金额
export const formatAmount = function (amount) {
  if (!amount) return amount;
  return amount.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ',');
};

// 根据key获取全局配置
let globalsConfig = null;
export const getGlobalsConfigByKey = async (key, refresh) => {
  if (globalsConfig && !refresh) {
    return globalsConfig[key];
  }
  const res = await h5Api.getGlobalsList();
  globalsConfig = res;
  return globalsConfig[key];
};

// 获取当前已缓存的 globalsConfig 对象（供跨页面缓存使用）
export const getCachedGlobalsConfig = () => globalsConfig;

// 设置 globalsConfig 缓存（供跨页面从 Storage 恢复使用）
export const setCachedGlobalsConfig = (config) => {
  globalsConfig = config;
};

/**
 * 从 SDK Storage 缓存获取全局配置（跨页面复用）- 工具函数方式
 * @param {string|string[]} keys - 要获取的配置 key 或 key 数组
 * @param {object} communicationStore - communication store 实例
 * @param {boolean} refresh - 是否强制刷新（忽略缓存），默认 false
 * @returns {Promise<any|object>} - 单个 key 返回值，多个 key 返回 { key: value } 对象
 * @example
 * // 单个 key
 * const value = await getCachedGlobalsConfigByKey('H5_MAP_SWITCH', communicationStore);
 *
 * // 多个 key
 * const { H5_MAP_SWITCH, CAMERA_USE_SDK } = await getCachedGlobalsConfigByKey(
 *   ['H5_MAP_SWITCH', 'CAMERA_USE_SDK'],
 *   communicationStore
 * );
 */
export const getCachedGlobalsConfigByKey = async (keys, communicationStore, refresh = false) => {
  // 如果不强制刷新，尝试从 SDK Storage 获取缓存
  if (!refresh) {
    try {
      const cachedStr = await communicationStore.getStorage('cachedGlobalsConfig');
      if (cachedStr) {
        const cachedGlobals = JSON.parse(decodeURIComponent(escape(cachedStr)));
        if (cachedGlobals) {
          // 恢复模块级缓存，避免后续 getGlobalsConfigByKey 重复请求
          setCachedGlobalsConfig(cachedGlobals);
          // 返回结果
          if (Array.isArray(keys)) {
            const result = {};
            keys.forEach((key) => {
              result[key] = cachedGlobals[key];
            });
            return result;
          }
          return cachedGlobals[keys];
        }
      }
    } catch (e) {
      console.error('从缓存获取全局配置失败:', e);
    }
  }

  // 缓存无值或强制刷新，走接口请求
  if (Array.isArray(keys)) {
    const result = {};
    for (const key of keys) {
      result[key] = await getGlobalsConfigByKey(key, true);
    }
    return result;
  }
  return await getGlobalsConfigByKey(keys, true);
};

/**
 * 检查配置开关是否开启
 * @param options 配置选项
 * @param options.configKey 配置项key，默认为 'SHOW_331_FEATURE'
 * @param options.defaultValue 配置不存在时的默认值，默认为 false
 * @param options.refresh 是否强制刷新缓存，默认为 false
 * @returns Promise<boolean>
 */
export const checkConfigSwitch = async (options = {}) => {
  const { configKey = 'SHOW_331_FEATURE', defaultValue = false, refresh = false } = options;
  const switchValue = await getGlobalsConfigByKey(configKey, refresh);
  if (switchValue === undefined) return defaultValue;
  return switchValue === 'true' || switchValue === true;
};

/**
 * 根据浏览器 User-Agent 判断设备类型
 * @returns {'mobile' | 'tablet' | 'pc'} 设备类型
 */
export const getDeviceType = () => {
  // 检查是否在浏览器环境
  if (typeof navigator === 'undefined' || typeof window === 'undefined') {
    // PC端再根据屏幕宽度保底判断
    const screenWidth = window.screen.width * (window.devicePixelRatio || 1);
    if (screenWidth >= 1200) {
      return 'pc'; // 大屏PC
    } else if (screenWidth >= 768) {
      return 'tablet'; // 中等屏幕，可能是平板或小屏PC
    }
    return 'mobile'; // 小屏幕
  } else {
    const ua = navigator.userAgent.toLowerCase();

    // 优先判断鸿蒙设备
    const isHarmonyOS = /harmonyos|openharmony/i.test(ua);
    console.log(isHarmonyOS,'=isHarmonyOS')
    console.log(window.screen.width,'=screenwidth')
    if (isHarmonyOS) {
      // 根据屏幕宽度判断是平板还是手机
      const screenWidth = window.screen.width;
      if (screenWidth >= 768) {
        return 'tablet';
      }
      return 'mobile';
    }

    // 判断是否为平板
    const isTablet = /ipad|android(?!.*mobile)|tablet|kindle|silk/i.test(ua);
    // 判断是否为移动设备
    const isMobile = /android|webos|iphone|ipod|blackberry|iemobile|opera mini/i.test(ua);

    if (isTablet) {
      return 'tablet';
    }
    if (isMobile) {
      return 'mobile';
    }
    return 'pc';
  }
};

// 是否为鸿蒙设备
export function isHarmonyOS() {
    const ua = navigator.userAgent;
    // 最新的鸿蒙系统UA通常是: Mozilla/5.0 (Phone; HarmonyOS 4.0) ... 
    // 或者包含 OpenHarmony
    return /HarmonyOS|OpenHarmony/i.test(ua);
}