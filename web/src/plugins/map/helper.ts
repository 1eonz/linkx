import type { Location, PolygonType, Position, TrackStatus } from './type';

import { Message } from '@/components/Message';
import { appConfig } from '@/config';
import { useI18n, useSetInterval } from '@/hooks';
import { isArray } from '@/utils/is';

import * as Turf from '@turf/turf';

import { mapManager } from './EMap';

export class BaseMap {
  center: null | Position; // 中心点
  clusters: Map<string, any>; // 聚合点集
  criZoom: number; // 临界层级
  customPopup: any; // 弹窗
  drawLayer: any; // 绘制图层
  isReady: boolean; // 地图是否准备就绪
  layers: Map<string, any>; // 图层集
  map: any; // 地图对象
  mapId: string; // 地图容器ID
  mapStyle = ''; // 地图样式
  markers: Map<string, any>; // 点合集
  onLoad: (context?: any) => void; // 地图加载完成回调
  popupConfig: any;
  zoom: number; // 初始化层级
  zooms: [number, number]; // 地图显示的缩放范围

  constructor(config) {
    const { center, CRITICAL_ZOOM, maxZoom, minZoom, zoom } = getMapConfig();

    this.mapId = '';
    this.center = (config.center || center).map(Number);
    this.zoom = config.zoom || zoom || 11;
    this.zooms = (config.zooms || [minZoom, maxZoom]).map(Number);
    this.criZoom = Number(CRITICAL_ZOOM) || 10;
    this.drawLayer = ['AMap', 'BMap', 'MapAbc', 'MineMap'].includes(config.mapType) ? [] : null;
    this.isReady = false;
    this.layers = new Map();
    this.clusters = new Map();
    this.markers = new Map();
    this.onLoad = function () {};
    Object.assign(this, config);
  }
}

/**
 * @description 合并地图配置项
 * @param {*} context 执行上下文
 * @param {*} config 配置项 如下
 * @param {string} mapId 地图容器ID 必须
 * @param {string} mapType 地图类型 GAODE or EGIS 可选
 * @param {array} center 中心点 [Lon, lat] 可选
 * @param {number} zoom 初始化层级 可选
 * @param {array} zooms 地图显示的缩放范围 [min, max] 可选
 * @param {function} onLoad 地图加载完成回调 可选
 */
export function mergeMapOptions(context, config) {
  const { DEFAULT_ZOOM, MAP_CENTER, MAX_ZOOM, MIN_ZOOM } = appConfig.settingData;
  const defaultConfig = {
    center: null,
    mapId: '',
    mapType: 'GAODE',
    onLoad: null,
    zoom: null,
    zooms: null,
  };
  Object.assign(config, {
    center: (config.center || MAP_CENTER.split(',')).map(Number),
    zoom: config.zoom || DEFAULT_ZOOM || 11,
    zooms: (config.zooms || [MIN_ZOOM, MAX_ZOOM]).map(Number),
  });

  const options = {
    cluster: new Map(), // 聚合点集
    customPopup: null, // 弹窗
    drawLayer: config.mapType === 'AMap' ? [] : null,
    isReady: false, // 地图是否准备就绪
    layer: new Map(), // 图层集
    map: null, // 地图对象
    ...defaultConfig,
    ...config,
  };

  Object.assign(context, options);
}

export class BaseTrack {
  color?: string;
  data: any[];
  endImage?: string;
  image?: string;
  map: any;
  mapId: string;
  move?: (index: number) => void;
  speed: number;
  startImage?: string;
  status: TrackStatus;
  statusChange?: (status: TrackStatus) => void;

  constructor(config) {
    this.data = [];
    this.speed = 1;
    this.status = '';
    this.color = '#FFC047';
    this.mapId = config.map.mapId;
    const data = config.data.map((item) => {
      const position = formatLonLat(item);
      return position;
    });
    Object.assign(this, config, { data });
  }
}

/**
 * @description 合并动向配置项
 * @param {*} context 执行上下文
 * @param {*} config 配置项 如下
 * @param {number} speed 速度
 * @param {array} data 经纬度集 [[lon, lat]]
 * @param {string} imagePath 图片地址
 * @param {string} imageName 图片名称
 * @param {string} startImage 起点图片
 * @param {string} endImage 终点图片
 * @param {function} move 点移动事件
 * @param {string} color 颜色
 * @class Track
 */
export function mergeTrackOptions(context, config) {
  const defaultConfig = {
    color: '#FFC047',
    data: [],
    endImage: '',
    move: null,
    speed: 1,
    startImage: '',
  };
  const data = config.data.forEach((item) => {
    const position = formatLonLat(item);
    if (position) {
      return position;
    }
  });
  const options = {
    layerId: 'trackLayer', // 图层ID
    map: null, // 地图实例
    mapId: config.map.mapId, // 地图ID
    navgator: null, // 巡航器
    status: '', // 状态 start or pause
    ...defaultConfig,
    ...config,
    data,
  };
  Object.assign(context, options);
}

// 地图是否准备就绪
export function mapIsReady(context: any | string) {
  const isReady = () => {
    if (typeof context === 'string') {
      return mapManager.get(context)?.isReady;
    }
    return context?.isReady;
  };

  return new Promise((resolve, _) => {
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

/**
 * @description 返回两个数组的相同id项
 * @param {array} arr1 作为比较相同项的数组
 * @param {array} arr2 需要返回data的数组
 * @returns {array}
 */
export function findSameIdFrom2Arr(arr1, arr2) {
  const map = new Map();
  arr1.forEach((item) => {
    map.set(item.id, item);
  });
  const arr = arr2.filter((item) => {
    return map.get(item.id);
  });
  return arr;
}

/**
 * @description 全局变量存储 地图实例(此地图对象为第三方地图实例)
 * @param {string} key mapId
 * @returns {map} map实例 { _layers }
 * @notice 没有将map存储在 实例化的地图对象上是因为第三方地图实例有可能会占很大的内存
 */
export const thirdMaps = new Map();

/**
 * @description 格式化经纬度
 * @param position Position
 * @returns Position
 */
export function formatLonLat(position: Position): null | Position {
  if (!isArray(position)) {
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

/**
 * @description 根据layerId 获取 layerName
 * @param {string} layerId
 * @returns layerName
 */
export const customLayerName = {};
export function getLayerName(layerId: string): string {
  const { t } = useI18n();
  let name = '';
  if (layerId.includes('order')) {
    name = t('homePage.navigateData.order');
  }
  if (layerId.includes('person')) {
    name = t('homePage.mapToolData.police');
  }
  if (layerId.includes('task')) {
    name = t('homePage.mapToolData.task');
  }
  if (layerId.includes('alarm')) {
    name = t('homePage.mapToolData.alarm');
  }
  if (layerId.includes('recorder')) {
    name = t('homePage.mapToolData.recorder');
  }
  if (layerId.includes('ballCamera')) {
    name = t('homePage.mapToolData.ballCamera');
  }
  if (layerId.includes('terminal')) {
    name = t('homePage.mapToolData.terminal');
  }
  if (layerId.includes('pdt')) {
    name = t('homePage.mapToolData.pdt');
  }
  if (layerId.includes('monitor')) {
    name = t('homePage.mapToolData.monitor');
  }
  if (layerId.includes('message')) {
    name = t('homePage.menus.emergencyMessages');
  }
  if (layerId.includes('policeCar')) {
    name = t('homePage.mapToolData.policeCar');
  }
  if (layerId.includes('carPhoto')) {
    name = t('homePage.mapToolData.picture');
  }
  if (layerId.includes('GBRecorder')) {
    name = t('homePage.mapToolData.GBRecorder');
  }
  if (layerId.includes('uav')) {
    name = t('homePage.mapToolData.uav');
  }
  if (layerId.includes('seat')) {
    name = t('homePage.mapToolData.seat');
  }
  if (layerId.includes('searchResource')) {
    name = t('homePage.mapToolData.place');
  }
  if (layerId.includes('landmark')) {
    name = t('homePage.mapToolData.landmark');
  }
  // if (layerId.includes('customLayer_')) {
  //   name = customLayerName[layerId];
  // }
  if (layerId.includes('custom')) {
    name = t('homePage.mapToolData.custom');
  }
  if (layerId.includes('defense')) {
    name = t('homePage.mapToolData.defense');
  }
  return name;
}

/**
 * 格式化draw颜色
 * @param str rgb(x,x,x)
 */
export function formatColor(str: string): string {
  return str?.replace('rgb(', '').replace(')', '');
}

/**
 * 获取区域中心
 * @param geo
 */
export function getAreaCenter(geo) {
  const { center, path } = geo;

  if (path) {
    let lon = 0;
    let lat = 0;
    const len = path.length;
    path.forEach((item) => {
      lon += Number(item[0]);
      lat += Number(item[1]);
    });
    return [lon / len, lat / len];
  } else {
    return center;
  }
}

// 圈选回调事件
export function drawCallback(
  callback: (features: any[], data: any) => void,
  features: any[],
  data: any,
) {
  if (!callback) return;
  if (features.length > 200) {
    const { t } = useI18n();
    Message({
      duration: 1000,
      message: t('resource.map.maxSelectLength'),
      type: 'info',
    });
    callback(features.slice(0, 200), data);
  } else {
    callback(features, data);
  }
}

/**
 * @description 根据layerId 获取 layerColor
 * @param {string} layerId 图层id
 * @returns layerColor
 */
export function getLayerColor(layerId) {
  const colorObj = {
    alarm: 'rgba(252, 233, 83, 1)',
    ballCamera: 'rgba(148, 168, 238, 1)',
    carPhoto: 'rgba(20, 164, 231, 1)',
    GBRecorder: 'rgba(66, 135, 255, 1)',
    message: 'rgba(255, 59, 56, 1)',
    monitor: 'rgba(240, 151, 43, 1)',
    pdt: 'rgba(16, 197, 218, 1)',
    person: 'rgba(111, 196, 227, 1)',
    recorder: 'rgba(148, 168, 238, 1)',
    seat: 'rgba(94, 124, 255, 1)',
    task: 'rgba(255, 59, 56, 1)',
    terminal: 'rgba(19, 189, 81, 1)',
    uav: 'rgba(136, 217, 15, 1)',
  };

  for (const key in colorObj) {
    const same = layerId.includes(key);
    if (same) {
      return colorObj[key];
    }
  }
}

export type PointsWithinPolygon = (
  config: {
    center?: Position;
    lineWidth?: number;
    path?: Position[];
    radius?: number;
    type: PolygonType;
  },
  points: Position[],
) => Position[];

/**
 * @description 计算图形区域内的点
 * @param type 图形类型 点marker 线polyline 多边形polygon 矩形rectangle 圆circle
 * @param data 图层id
 * @returns points
 */
export const pointsWithinPolygon: PointsWithinPolygon = (
  { center, lineWidth, path, radius, type },
  points,
) => {
  let searchWithin: any = null;
  switch (type) {
    case 'circle': {
      if (center && radius) {
        const options = { steps: 10, units: 'meters' };
        searchWithin = Turf.circle(center, radius, options as any);
      }
      break;
    }
    case 'polygon':
    case 'rectangle': {
      if (path) {
        searchWithin = Turf.multiPolygon([[path]]);
      }
      break;
    }
    case 'polyline': {
      if (path && lineWidth) {
        const line = Turf.lineString(path);
        // 获取一条线并返回具有指定距离偏移量的线
        const line1 = Turf.lineOffset(line, -lineWidth / 2, { units: 'meters' });
        const line2 = Turf.lineOffset(line, lineWidth / 2, { units: 'meters' });
        const coordinates2 = [...line2.geometry.coordinates];
        coordinates2.reverse();
        const coordinates = [...line1.geometry.coordinates, ...coordinates2];
        searchWithin = Turf.multiPolygon([[coordinates]]);
      }
      break;
    }
  }

  const ptsWithin = Turf.pointsWithinPolygon(Turf.points([...points]), searchWithin);
  const features = ptsWithin?.features || [];

  return features.map((i) => i.geometry.coordinates) as Position[];
};

/**
 * @description 转换后台返回的经纬度格式 - 并在对象上添加position属性
 * @param {T} data
 * @returns {T & { position: Position }}
 */
export const setPosition = <T>(data: T) => {
  if ((data as { position: Position } & T).position) {
    return;
  }
  const coordinates = (data as unknown as Location & T).location?.coordinates?.[0];
  const { lat, lon } = coordinates || {};
  if (lon && lat) {
    (data as { position: Position } & T).position = [
      Number(Number(lon).toFixed(6)),
      Number(Number(lat).toFixed(6)),
    ];
  }
};

/**
 * @description 计算两点之间的距离
 * @param from
 * @param to
 * @returns number(米)
 */
export const distanceBePoints = (from: Position, to: Position): number => {
  const options = { units: 'meters' as Turf.Units };
  const distance = Turf.rhumbDistance(Turf.point(from), Turf.point(to), options);
  return distance;
};

// 获取地图配置项
export function getMapConfig() {
  return appConfig.mapConfig.configuration;
}
