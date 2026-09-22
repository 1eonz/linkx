import { mapManager } from './EMap';

import { taskApi } from '@/common/api/index.js';
import { useSetInterval } from '@/hooks/useSetInterval';
import { usePageUrlStore } from '@/stores/pageUrl';

export const MAP_TYPE_STORAGE_KEY = 'H5_PORTAL_MAP_TYPE';

export function getStoredMapType() {
  if (typeof window === 'undefined' || !window.localStorage) {
    return '';
  }
  try {
    return window.localStorage.getItem(MAP_TYPE_STORAGE_KEY) || '';
  } catch (error) {
    console.warn('读取地图类型失败:', error);
    return '';
  }
}

export function setStoredMapType(value) {
  if (typeof window === 'undefined' || !window.localStorage) {
    return;
  }
  try {
    window.localStorage.setItem(MAP_TYPE_STORAGE_KEY, value);
  } catch (error) {
    console.warn('保存地图类型失败:', error);
  }
}

/**
 * 地图基础类
 */
export class BaseMap {
  constructor(config) {
    const {
      center = [104.0668, 30.5728], // 成都中心点坐标
      zoom = 11,
      zooms = [3, 18],
    } = config || {};

    this.mapId = config?.mapId || '';

    // 确保 center 是有效的数组
    if (Array.isArray(center) && center.length === 2) {
      this.center = center.map(Number);
      // 验证转换后的值
      if (isNaN(this.center[0]) || isNaN(this.center[1])) {
        console.warn('中心点坐标无效，使用默认值');
        this.center = [104.0668, 30.5728]; // 成都中心点坐标
      }
    } else {
      console.warn('中心点坐标格式不正确，使用默认值');
      this.center = [104.0668, 30.5728]; // 成都中心点坐标
    }

    this.zoom = Number(zoom) || 11;
    this.zooms = Array.isArray(zooms) ? zooms.map(Number) : [3, 18];
    this.criZoom = 10;
    this.drawLayer = null;
    this.isReady = false;
    this.layers = new Map();
    this.clusters = new Map();
    this.markers = new Map();
    this.map = null;
    this.mapStyle = '';
    this.customPopup = null;
    this.popupConfig = null;
    this.onLoad = config?.onLoad || function () { };
    this.layerClickHandlers = new Map();

    // 仅合并this上不存在的属性，避免非排除属性被覆盖
    Object.entries(config).forEach(([key, value]) => {
      if (!Object.prototype.hasOwnProperty.call(this, key)) {
        this[key] = value;
      }
    });

    // 确保 center 始终是有效的数组（防止被覆盖）
    if (
      !Array.isArray(this.center) ||
      this.center.length !== 2 ||
      isNaN(this.center[0]) ||
      isNaN(this.center[1])
    ) {
      console.warn('center 被覆盖或无效，重新设置为默认值', this.center);
      this.center = [104.0668, 30.5728];
    }
  }

  registerLayerClickHandler(layerId, handler) {
    if (!layerId) return;
    if (typeof handler === 'function') {
      this.layerClickHandlers.set(layerId, handler);
      return;
    }
    this.layerClickHandlers.delete(layerId);
  }

  triggerMarkerClick(layerId, payload) {
    if (!layerId) return;
    const handler = this.layerClickHandlers.get(layerId);
    if (typeof handler === 'function') {
      handler(payload);
    }
  }
}

/**
 * 格式化经纬度
 * @param {Array} position 经纬度数组 [lon, lat]
 * @returns {Array|null} 格式化后的经纬度数组
 */
export function formatLonLat(position) {
  if (!Array.isArray(position)) {
    return null;
  }
  const lon = Number(position[0]);
  const lat = Number(position[1]);
  if (Number.isNaN(lon) || Number.isNaN(lat)) {
    console.error(`The position ${JSON.stringify(position)} format is incorrect`);
    return null;
  }
  return [lon, lat];
}

// 地图是否准备就绪
export function mapIsReady(context) {
  const isReady = () => {
    if (typeof context === 'string') {
      return mapManager.get(context)?.isReady;
    }
    return context?.isReady;
  };

  return new Promise((resolve) => {
    if (isReady()) {
      resolve(true);
    }
    const clearTimer = useSetInterval(() => {
      if (isReady()) {
        clearTimer?.();
        resolve(true);
      }
    }, 100);
  });
}

// 图层默认属性
export const defaultLayerOptions = {
  isCluster: true, // 是否聚合
  isShow: true, // 是否显示
};

// 获取地图配置项
export async function getMapConfig() {
  let config = await getMapList();
  // 默认配置，可以根据实际需求从配置文件读取
  // if (!config) {
  //   config = {
  //     center: [104.0668, 30.5728], // 成都中心点坐标
  //     zoom: 11,
  //     minZoom: 3,
  //     maxZoom: 18,
  //     CRITICAL_ZOOM: 10,
  //     url:''
  //     mapType: "OpenLayers",
  //   };
  // }
  console.log(JSON.parse(JSON.stringify(config)), '==============config');

  // const storedType = getStoredMapType();
  // if (storedType) {
  //   config.mapType = storedType;
  // }

  return config;
}

function joinUrl(base, path) {
  const b = String(base || '').replace(/\/$/, '');
  const p = String(path || '').startsWith('/') ? String(path || '') : `/${path}`;
  return `${b}${p}`;
}

// 将相对URL转换为绝对URL，绝对URL原样返回
async function resolveRelativeUrl(rawUrl) {
  const trimmed = String(rawUrl || '').trim();
  if (!trimmed) return trimmed;
  if (/^https?:\/\//i.test(trimmed)) return trimmed;
  const pageUrlStore = usePageUrlStore();
  if (!pageUrlStore.pageUrl) {
    await pageUrlStore.initPageUrl();
  }
  if (!pageUrlStore.pageUrl) return trimmed;
  const urlPath = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
  // 生产环境：使用 pageUrlStore.pageUrl 作为 baseUrl
  // 开发环境：固定指向指定的后端地址
  return import.meta.env.DEV
    ? 'http://172.16.23.30:30280/linkx/h5portal' + urlPath
    : joinUrl(pageUrlStore.pageUrl, urlPath);
}

//获取离线地图列表
async function getMapList() {
  const mapList = await taskApi.selectListMap({ activation: 1 });
  if (mapList.length > 0) {
    const obj = mapList[0];
    if (obj.configuration) {
      const newObj = JSON.parse(obj.configuration) || {};
      if (newObj.url) {
        newObj.url = await resolveRelativeUrl(newObj.url);
      }

      if (newObj.style?.sources) {
        for (const sourceConfig of Object.values(newObj.style.sources)) {
          if (Array.isArray(sourceConfig.tiles)) {
            sourceConfig.tiles = await Promise.all(
              sourceConfig.tiles.map(tileUrl => resolveRelativeUrl(tileUrl))
            );
          }
        }
      }
      return { ...newObj, mapType: obj.mapType };
    }
  }
  return null;
}
