/**
 * 获取全局配置的 Composable Hook（跨页面缓存复用）
 * 自动获取 communicationStore，符合 Vue Composition API 风格
 *
 * @example
 * // 在 Vue 组件 setup 中使用
 * import { useCachedGlobalsConfig } from '@/hooks/useCachedGlobalsConfig';
 *
 * const { getConfig, getConfigs } = useCachedGlobalsConfig();
 *
 * // 单个 key
 * const value = await getConfig('H5_MAP_SWITCH');
 *
 * // 多个 key
 * const { H5_MAP_SWITCH, CAMERA_USE_SDK } = await getConfigs(['H5_MAP_SWITCH', 'CAMERA_USE_SDK']);
 */
import { useCommunicationStore } from '@/stores/communication.js';
import { getGlobalsConfigByKey, setCachedGlobalsConfig } from '@/common/utils';

export function useCachedGlobalsConfig() {
  const communicationStore = useCommunicationStore();

  /**
   * 获取单个全局配置（优先从缓存）
   * @param {string} key - 配置 key
   * @param {boolean} refresh - 是否强制刷新，默认 false
   * @returns {Promise<any>} 配置值
   */
  async function getConfig(key, refresh = false) {
    return await _getFromCacheOrFetch(key, refresh);
  }

  /**
   * 批量获取全局配置（优先从缓存）
   * @param {string[]} keys - 配置 key 数组
   * @param {boolean} refresh - 是否强制刷新，默认 false
   * @returns {Promise<object>} { key: value } 对象
   */
  async function getConfigs(keys, refresh = false) {
    return await _getFromCacheOrFetch(keys, refresh);
  }

  /**
   * 内部方法：从缓存获取或请求接口
   * @param {string|string[]} keys
   * @param {boolean} refresh
   * @returns {Promise<any|object>}
   */
  async function _getFromCacheOrFetch(keys, refresh) {
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
  }

  return {
    getConfig,
    getConfigs,
  };
}
