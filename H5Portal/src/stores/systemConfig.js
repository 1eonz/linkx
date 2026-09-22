import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import { getSystemConfig } from '@/common/api/h5.js';
import { getDeviceType } from '@/common/utils';

export const useSystemConfigStore = defineStore('systemConfig', () => {
  // 系统配置数据
  const configData = ref([]);

  // 解析配置项的value字段（新接口返回的value是JSON字符串，需要解析后取其中的value属性）
  const parseConfigValue = (value) => {
    if (!value) return '';
    try {
      const parsed = JSON.parse(value);
      return parsed?.value ?? value;
    } catch {
      // 如果解析失败，直接返回原值（兼容旧格式）
      return value;
    }
  };

  // 系统名称（优先级最高）
  const systemName = computed(() => {
    const config = configData.value.find(item => item.key === 'SYSTEM_NAME');
    console.log('本地调试---系统名称', parseConfigValue(config?.value))
    return parseConfigValue(config?.value) || '';
  });

  // 应用每行显示数量配置（格式: "手机端;pad端;pc端" 如 "4;8;12"）
  const appCountInRow = computed(() => {
    const config = configData.value.find(item => item.key === 'APP_COUNT_IN_ROW');
    console.log('本地调试---应用每行显示数量配置', parseConfigValue(config?.value))
    return parseConfigValue(config?.value) || '4;8;12';
  });

  // 根据设备类型获取每行应用数量
  const getAppCountByDevice = (deviceType) => {
    // 如果未传入设备类型，使用公共方法自动判断
    const type = deviceType || getDeviceType();
    const counts = appCountInRow.value.split(';');
    // counts[0] - 手机端, counts[1] - tablet端, counts[2] - pc端
    switch (type) {
      case 'mobile':
        return parseInt(counts[0]) || 4;
      case 'tablet':
        return parseInt(counts[1]) || 8;
      case 'pc':
        return parseInt(counts[2]) || 12;
      default:
        return parseInt(counts[0]) || 4;
    }
  };

  // 获取设备类型对应的接口参数值（mobile=1, tablet=2, pc=3）
  const getDeviceTypeValue = (deviceType) => {
    const type = deviceType || getDeviceType();
    switch (type) {
      case 'mobile':
        return 1;
      case 'tablet':
        return 2;
      case 'pc':
        return 3;
      default:
        return 1;
    }
  };

  // 请求Promise缓存（防止并发重复请求）
  let fetchPromise = null;

  // 获取系统配置
  const fetchSystemConfig = async () => {
    // 如果已有正在进行的请求，返回该Promise（避免重复请求）
    if (fetchPromise) {
      return fetchPromise;
    }

    fetchPromise = (async () => {
      try {
        const res = await getSystemConfig();
        configData.value = res || [];
        return res;
      } catch (error) {
        console.error('获取系统配置失败:', error);
        return [];
      } finally {
        fetchPromise = null;
      }
    })();

    return fetchPromise;
  };

  return {
    configData,
    systemName,
    appCountInRow,
    getAppCountByDevice,
    getDeviceTypeValue,
    fetchSystemConfig,
  };
});