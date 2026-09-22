import 'ol/ol.css';

import { defaults as defaultControls } from 'ol/control';
import Feature from 'ol/Feature';
import { MVT } from 'ol/format';
import WMTSCapabilities from 'ol/format/WMTSCapabilities';
import Point from 'ol/geom/Point';
import { defaults as defaultInteractions, PinchZoom } from 'ol/interaction';
import { Tile as TileLayer, Vector as VectorLayer, VectorTile as VectorTileLayer } from 'ol/layer';
import Map from 'ol/Map';
import { fromLonLat, toLonLat, transform } from 'ol/proj';
import {
  Cluster as ClusterSource,
  TileWMS,
  Vector as VectorSource,
  VectorTile as VectorTileSource,
  WMTS as WMTSSource,
  XYZ as XYZSource,
} from 'ol/source';
import { optionsFromCapabilities } from 'ol/source/WMTS';
import { Circle as CircleStyle, Fill, Icon, Stroke, Style, Text } from 'ol/style';
import { createXYZ } from 'ol/tilegrid';
import WMTSTileGrid from 'ol/tilegrid/WMTS.js';
import View from 'ol/View';

import { BaseMap, formatLonLat, getMapConfig } from '../helper';
import { createMapboxVectorStyle, createStyleFromLayerConfigs } from '../openlayers/mapBoxVector';

import { getGlobalsConfigByKey } from '@/common/utils';

// const FALLBACK_TILE_URL =
//   "https://services.minedata.cn/service/data/satellite?x={x}&y={y}&z={z}";
const FALLBACK_TILE_URL = '';
const SUPERSET_HTTPS_BASE = getNormalizedSupersetBase('SUPERSET_SERVER_URL');
const SUPERSET_HTTP_BASE = getNormalizedSupersetBase('SUPERSET_SERVER_URL_HTTP');
const DEFAULT_ATTRIBUTION =
  '© <a href="https://www.openstreetmap.org/">OpenStreetMap</a> contributors';

function getGlobalSettingValue(key) {
  if (typeof window === 'undefined') {
    return '';
  }
  const sources = [
    window.__APP_CONFIG__?.settingData?.[key],
    window.__APP_CONFIG__?.[key],
    window.appConfig?.settingData?.[key],
    window.appConfig?.[key],
    window.APP_CONFIG?.settingData?.[key],
    window.APP_CONFIG?.[key],
    window[key],
  ];
  return sources.find((item) => typeof item === 'string' && item.length > 0) || '';
}

function normalizeSupersetHost(url) {
  if (!url || typeof url !== 'string') {
    return '';
  }
  let next = url.trim();
  if (!next) {
    return '';
  }
  if (typeof window !== 'undefined' && window.location?.hostname) {
    next = next.replace(/:\/\/ip/gi, `://${window.location.hostname}`.replace(/:+$/, ''));
  }
  const lower = next.toLowerCase();
  const supersetIndex = lower.indexOf('/superset');
  if (supersetIndex > -1) {
    next = next.slice(0, supersetIndex);
  }
  return next.replace(/\/+$/, '');
}

function getNormalizedSupersetBase(key) {
  return normalizeSupersetHost(getGlobalSettingValue(key));
}

function resolveTileUrl(value) {
  if (value == null) {
    return '';
  }
  let next = `${value}`.trim();
  if (!next) {
    return '';
  }

  if (typeof window !== 'undefined' && window.location?.hostname) {
    const host = window.location.hostname;
    next = next.replace(/:\/\/ip/gi, `://${host}`);
    next = next.replace(/\{SUPERSET_IP\}/gi, host);
  }

  if (SUPERSET_HTTP_BASE) {
    next = next.replace(/SUPERSET_SERVER_URL_HTTP/g, SUPERSET_HTTP_BASE);
  }
  if (SUPERSET_HTTPS_BASE) {
    next = next.replace(/SUPERSET_SERVER_URL/g, SUPERSET_HTTPS_BASE);
  }

  if (/\/superset\/welcome\/?$/i.test(next) && !next.includes('{')) {
    const trimmed = normalizeSupersetHost(next);
    if (trimmed) {
      next = trimmed;
    }
  }

  return next;
}

// 批量解析瓦片URL数组，过滤空值
function resolveTileUrls(tiles) {
  if (!Array.isArray(tiles)) return [];
  return tiles.map(url => resolveTileUrl(url)).filter(Boolean);
}

function getDefaultTileUrl() {
  if (typeof window === 'undefined') {
    return SUPERSET_HTTPS_BASE || SUPERSET_HTTP_BASE || FALLBACK_TILE_URL;
  }
  const prefersHttp = window.location?.protocol === 'http:';
  const preferred = prefersHttp ? SUPERSET_HTTP_BASE : SUPERSET_HTTPS_BASE;
  return preferred || (prefersHttp ? SUPERSET_HTTPS_BASE : SUPERSET_HTTP_BASE) || FALLBACK_TILE_URL;
}

// 通过全局配置动态获取瓦片地址，优先使用 MineMapRasterUrl
let defaultTileUrlPromise = null;
const getDefaultTileUrlAsync = async () => {
  if (defaultTileUrlPromise) return defaultTileUrlPromise;

  defaultTileUrlPromise = (async () => {
    // 1. 优先从全局配置获取 MineMapRasterUrl
    try {
      const urlFromConfig = await getGlobalsConfigByKey('MineMapRasterUrl', true);
      const resolvedConfigUrl = resolveTileUrl(urlFromConfig);
      if (resolvedConfigUrl) {
        return resolvedConfigUrl;
      }
    } catch (error) {
      console.warn('获取 MineMapRasterUrl 失败，将使用默认底图地址:', error);
    }

    // 2. 兜底使用原有 SUPSERSET / FALLBACK 逻辑
    return getDefaultTileUrl();
  })();

  return defaultTileUrlPromise;
};

const createHtmlDataUrl = (html, width, height, align = 'center') => {
  const safeHtml = typeof html === 'string' ? html : '';
  // align: 'center' | 'bottom' - 控制内容在 SVG 容器中的对齐方式
  let alignItems = 'center';
  let justifyContent = 'center';
  if (align === 'bottom') {
    alignItems = 'flex-end'; // 底部对齐
    justifyContent = 'center'; // 水平居中
  }
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}">
      <foreignObject width="100%" height="100%">
        <div xmlns="http://www.w3.org/1999/xhtml" style="width:100%;height:100%;display:flex;align-items:${alignItems};justify-content:${justifyContent};">
          ${safeHtml}
        </div>
      </foreignObject>
    </svg>
  `;
  return `data:image/svg+xml;charset=utf-8,${encodeURIComponent(svg)}`;
};

const resolveAssetUrl = (value) => {
  if (!value) return '';
  if (/^(data:|https?:|blob:)/i.test(value)) {
    return value;
  }
  if (typeof window !== 'undefined') {
    try {
      if (value.startsWith('//')) {
        return `${window.location.protocol}${value}`;
      }
      if (value.startsWith('/')) {
        return `${window.location.origin}${value}`;
      }
      return new URL(value, window.location.origin).href;
    } catch (error) {
      console.warn('resolveAssetUrl error', error);
    }
  }
  return value;
};

const toHtmlString = (value) => {
  if (!value) return '';
  if (typeof value === 'string') return value;
  if (typeof HTMLElement !== 'undefined' && value instanceof HTMLElement) {
    return value.outerHTML;
  }
  return '';
};

const withAMapPixel = (runner) => {
  if (typeof window === 'undefined') {
    return runner();
  }
  const existed = window.AMap;
  let created = false;
  if (!existed) {
    window.AMap = {
      Pixel: class Pixel {
        constructor(x = 0, y = 0) {
          this.x = x;
          this.y = y;
        }
      },
    };
    created = true;
  } else if (!existed.Pixel) {
    existed.Pixel = class Pixel {
      constructor(x = 0, y = 0) {
        this.x = x;
        this.y = y;
      }
    };
  }
  try {
    return runner();
  } finally {
    if (created) {
      delete window.AMap;
    }
  }
};

const createDefaultMarkerStyle = (options) => {
  const diameter = options.size || 24;
  const icon = options.icon || '';
  const circleStyle = new CircleStyle({
    radius: diameter / 2,
    fill: new Fill({ color: options.color || '#4B7AFA' }),
    stroke: new Stroke({
      color: 'rgba(255,255,255,0.85)',
      width: 2,
    }),
  });

  const text = icon
    ? new Text({
        text: icon,
        fill: new Fill({ color: '#fff' }),
        font: '16px/16px sans-serif',
        textBaseline: 'middle',
        textAlign: 'center',
      })
    : undefined;

  return new Style({
    image: circleStyle,
    text,
  });
};

const createIconStyleFromHtml = (html, width, height, anchor) => {
  if (!html) {
    return null;
  }
  // 如果锚点是底部（y = 1.0），则内容在 SVG 中应该底部对齐
  const align = anchor && anchor[1] === 1.0 ? 'bottom' : 'center';
  const dataUrl = createHtmlDataUrl(html, width, height, align);
  return new Style({
    image: new Icon({
      anchor: anchor || [0.5, 0.5],
      src: dataUrl,
      imgSize: [width, height],
    }),
  });
};

// 计算瓦片请求的最大层级，强制不超过 14
function getMaxTileZoom(...candidates) {
  const fallback = 14;
  for (const val of candidates) {
    if (val !== undefined && val !== null) {
      return Math.min(fallback, Number(val) || fallback);
    }
  }
  return fallback;
}

// 为瓦片源添加降级逻辑：超过 maxTileZoom 的层级复用低层级瓦片
function applyTileUrlDegradation(source, maxTileZoom) {
  const originalFn = source.getTileUrlFunction();
  if (typeof originalFn !== 'function') return;
  const safeMax = Number.isFinite(maxTileZoom) ? Math.max(0, Math.floor(maxTileZoom)) : 14;
  source.setTileUrlFunction((tileCoord, pixelRatio, projection) => {
    if (!tileCoord) return undefined;
    let [z, x, y] = tileCoord;
    if (z > safeMax) {
      const factor = 2 ** (z - safeMax);
      z = safeMax;
      x = Math.floor(x / factor);
      y = Math.floor(y / factor);
    }
    const maxTileCount = 2 ** z;
    if (x < 0 || x >= maxTileCount || y < 0 || y >= maxTileCount) {
      return undefined;
    }
    return originalFn([z, x, y], pixelRatio, projection);
  });
}

class _OpenLayers extends BaseMap {
  constructor(config) {
    super(config);
    this.baseLayer = null;
    this.baseLayerType = config?.baseLayerType || 'raster';
    this.vectorTileOptions = config?.vectorTileOptions || null;
    this.baseTileUrl = config?.tileUrl || config?.baseTileUrl || null;
    this._markerClickListener = null;
    this.initMap();
  }

  initMap() {
    if (typeof window === 'undefined') return;
    const container = document.getElementById(this.mapId);
    if (!container) {
      console.warn(`地图容器 ${this.mapId} 不存在，稍后重试`);
      setTimeout(() => this.initMap(), 100);
      return;
    }

    (async () => {
      const mapConfig = await getMapConfig().catch(() => null);
      const apiLayer = await this.createBaseLayerFromConfig(mapConfig);
      this.zooms = [mapConfig?.minZoom || 3, mapConfig?.maxZoom || 18];

      const shouldUseVector =
        !apiLayer && this.baseLayerType === 'vector' && this.vectorTileOptions?.url;
      const rasterUrl = this.baseTileUrl || (await getDefaultTileUrlAsync());
      // 瓦片请求的最大层级，强制不超过 14
      const baseMaxTileZoom = getMaxTileZoom(mapConfig?.XYZmaxZoom, mapConfig?.maxZoom);

      if (Array.isArray(apiLayer)) {
        this.baseLayer = apiLayer[0];
      } else {
        this.baseLayer =
          apiLayer ||
          (shouldUseVector && this.createVectorTileBaseLayer(this.vectorTileOptions)) ||
          // 默认底图瓦片只请求到 baseMaxTileZoom（最大 14 级），更高缩放级别使用该级别瓦片放大填充
          this.createRasterBaseLayer(rasterUrl, {
            maxTileZoom: baseMaxTileZoom,
          });
      }

      this.map = new Map({
        layers: [this.baseLayer],
        target: container,
        // 去掉 OpenLayers 默认的缩放按钮和旋转控件
        controls: defaultControls({ zoom: false, rotate: false }),
        //  关键配置：使用 Canvas 渲染，避免 WebGL 兼容性问题
        renderer: 'canvas',
        //  关键配置：禁用旋转交互和双指缩放（使用自定义实现）
        interactions: defaultInteractions({
          rotate: false, // 禁用旋转
          pinchRotate: false, // 禁用双指旋转
          altShiftDragRotate: false, // 禁用 Alt+Shift+拖拽旋转
          shiftDragZoom: false, // 禁用 Shift+拖拽缩放
          dragPan: true, // 启用拖拽平移
          pinchZoom: false, //  完全禁用双指缩放，使用自定义实现
          mouseWheelZoom: true, // 启用鼠标滚轮缩放
          doubleClickZoom: true, // 启用双击缩放
        }),
        view: new View({
          center: fromLonLat(this.center),
          minZoom: this.zooms?.[0] ?? 3,
          maxZoom: this.zooms?.[1] ?? 18,
          zoom: this.zoom,
          //  关键配置：启用平滑缩放，防止缩放时黑屏
          smoothExtentConstraint: true,
          smoothZoomConstraint: true,
          constrainResolution: false, // 允许任意缩放级别
          //  关键配置：禁用旋转
          enableRotation: false,
        }),
      });

      if (Array.isArray(apiLayer) && apiLayer.length > 1) {
        for (let i = 1; i < apiLayer.length; i++) {
          this.map.addLayer(apiLayer[i]);
        }
      }

      // 监听渲染事件
      this.map.on('rendercomplete', () => {
        console.log('[OpenLayers] 渲染完成');
      });

      this.map.on('precompose', (event) => {
        const frameState = event.frameState;
        if (!frameState || !frameState.viewState) return;
        
        // 如果缩放级别为 NaN，阻止渲染
        const viewState = frameState.viewState;
        if (isNaN(viewState.resolution) || !isFinite(viewState.resolution)) {
          console.error('[OpenLayers] resolution 异常:', viewState.resolution);
          const view = this.map.getView();
          view.setResolution(undefined);
        }
      });

      this.map.on('postcompose', (event) => {
        const canvas = event.frameState?.canvas;
        if (canvas && (canvas.width === 0 || canvas.height === 0)) {
          console.error('[OpenLayers] Canvas 尺寸为 0');
        }
      });

      // 监听地图移动结束
      this.map.on('moveend', () => {
        const view = this.map.getView();
        const zoom = view.getZoom();
        
        // 检查缩放级别是否正常
        if (isNaN(zoom) || !isFinite(zoom)) {
          console.error('[OpenLayers] moveend: 缩放级别异常:', zoom);
          view.setZoom(this.zoom || 11);
        }
      });

      // 监听 WebGL 上下文丢失
      const viewport = this.map.getViewport();
      if (viewport) {
        const canvas = viewport.querySelector('canvas');
        if (canvas) {
          canvas.addEventListener('webglcontextlost', (event) => {
            console.error('[OpenLayers] WebGL 上下文丢失');
            event.preventDefault();
          });

          canvas.addEventListener('webglcontextrestored', () => {
            console.log('[OpenLayers] WebGL 上下文恢复');
            this.map.render();
          });
        }
      }

      //  关键修复：监听 View 状态变化，防止 NaN
      const view = this.map.getView();
      view.on('change:resolution', () => {
        const zoom = view.getZoom();
        
        // 如果缩放级别为 NaN，重置为默认值
        if (isNaN(zoom) || !isFinite(zoom)) {
          console.error('[OpenLayers] 缩放级别异常:', zoom, '，重置为默认值');
          view.setZoom(this.zoom || 11);
          setTimeout(() => {
            this.map.render();
            this.map.updateSize();
          }, 0);
        }
      });

      view.on('change:center', () => {
        const center = view.getCenter();
        if (!center || isNaN(center[0]) || isNaN(center[1])) {
          console.error('[OpenLayers] 中心点异常:', center, '，重置为默认值');
          view.setCenter(fromLonLat(this.center));
          setTimeout(() => {
            this.map.render();
            this.map.updateSize();
          }, 0);
        }
      });

      this.map.once('postrender', () => {
        console.log('[OpenLayers] 地图准备就绪');
        this.isReady = true;
        
        // 强制更新地图尺寸
        setTimeout(() => {
          this.map.updateSize();
          
          // 检查 Canvas 是否正确创建
          const viewport = this.map.getViewport();
          if (viewport) {
            const canvas = viewport.querySelector('canvas');
            if (canvas && (canvas.width === 0 || canvas.height === 0)) {
              console.error('[OpenLayers] Canvas 尺寸为 0，强制更新');
              this.resize();
            }
          }
        }, 100);
        
        if (typeof this.onLoad === 'function') {
          this.onLoad(this);
        }
      });

      // 监听地图错误
      this.map.on('error', (error) => {
        console.error('[OpenLayers] 地图错误:', error);
      });

      this.map.on('singleclick', (e) => {
        // e.coordinate 是点击位置的墨卡托坐标（EPSG:3857）
        // 转换为 WGS84 经纬度（EPSG:4326）：[经度, 纬度]
        const lngLat = transform(e.coordinate, 'EPSG:3857', 'EPSG:4326');

        // 保留6位小数（经纬度常用精度），方便阅读
        const lng = lngLat[0].toFixed(6); // 经度
        const lat = lngLat[1].toFixed(6); // 纬度

        // 3. 打印经纬度（控制台 + 页面显示）
        console.log('地图点击经纬度：', `经度：${lng}°E，纬度：${lat}°N`);
      });
    })();
  }

  createRasterBaseLayer(tileUrl, options = {}) {
    const { maxTileZoom = 14 } = options;
    const resolvedUrl = resolveTileUrl(tileUrl) || FALLBACK_TILE_URL;

    //  关键检查：瓦片 URL 是否有效
    if (!resolvedUrl) {
      console.error('[OpenLayers] 瓦片 URL 为空，地图将无法显示！');
      console.error('[OpenLayers] tileUrl:', tileUrl);
      console.error('[OpenLayers] 请检查地图配置或网络连接');
    }

    const tileGrid = createXYZ({ maxZoom: maxTileZoom });

    const source = new XYZSource({
      url: resolvedUrl,
      crossOrigin: 'anonymous',
      attributions: DEFAULT_ATTRIBUTION,
      tileGrid,
    });

    // 添加瓦片加载错误处理
    source.on('tileloaderror', (event) => {
      console.error('[OpenLayers] 瓦片加载失败:', {
        url: resolvedUrl,
        tileCoord: event.tile?.getTileCoord?.(),
        error: event.error,
      });
    });

    // 包装原始 tileUrlFunction，在更高层级时“降级”到 maxTileZoom 的瓦片坐标
    applyTileUrlDegradation(source, maxTileZoom);

    return new TileLayer({
      source,
    });
  }

  async createBaseLayerFromConfig(config = null) {
    if (!config) {
      return null;
    }

    console.error('[OpenLayers] createBaseLayerFromConfig config:', JSON.stringify(config, null, 2));

    if (config.style?.sources && config.style?.layers) {
      return this.createLayersFromStyleConfig(config);
    }

    const url = resolveTileUrl(config.url);
    // 如果后端未显式声明 mapType，但 URL 指向 pbf/mvt/vector，则自动按矢量瓦片处理
    const inferredType =
      (config.service || config.server || config.type || config.mapType || '').toLowerCase() ||
      (url && /\.(pbf|mvt)(\?|$)/i.test(url) ? 'vector' : '');
    const type = inferredType;
    if (!url) {
      return null;
    }
    // 优先支持矢量瓦片
    if (type === 'vector') {
      // 矢量瓦片同样限制服务器实际请求的最大层级，强制不超过 14
      const maxTileZoom = getMaxTileZoom(config.XYZmaxZoom, config.maxZoom);
      return this.createVectorTileBaseLayer({
        url,
        maxZoom: maxTileZoom,
        maxTileZoom,
      });
    }

    if (type === 'wms') {
      const layers = config.LAYERS || config.layer;
      return new TileLayer({
        source: new TileWMS({
          url,
          params: { LAYERS: layers },
          crossOrigin: 'anonymous',
        }),
      });
    }

    if (type === 'wmts') {
      // 优先从 capabilities 获取参数
      if (config.xml) {
        try {
          const text = await fetch(config.xml).then((r) => r.text());
          const parser = new WMTSCapabilities();
          const result = parser.read(text);
          const options = optionsFromCapabilities(result, {
            layer: config.layer || config.LAYERS,
            matrixSet: config.matrixSet,
          });
          return new TileLayer({
            source: new WMTSSource({
              ...options,
              crossOrigin: 'anonymous',
            }),
          });
        } catch (error) {
          console.warn('加载 WMTS capabilities 失败，使用简单模式:', error);
        }
      }

      const matrixIds = [];
      const resolutions = [];
      for (let z = 0; z < 19; z += 1) {
        matrixIds[z] = z;
        resolutions[z] = Math.pow(2, 18 - z);
      }

      return new TileLayer({
        source: new WMTSSource({
          layer: config.layer || config.LAYERS,
          matrixSet: config.matrixSet,
          url,
          tileGrid: new WMTSTileGrid({
            matrixIds,
            resolutions,
          }),
          crossOrigin: 'anonymous',
        }),
      });
    }
    console.log(url, '==============url');
    // 默认按 XYZ 处理；瓦片最大级别强制不超过 14
    const maxTileZoom = getMaxTileZoom(config.XYZmaxZoom, config.maxZoom);
    return this.createRasterBaseLayer(url, {
      maxTileZoom,
    });
  }

  createLayersFromStyleConfig(config) {
    const olLayers = [];
    const styleConfig = config.style;

    const bgLayer = styleConfig.layers.find(l => l.type === 'background');
    if (bgLayer?.paint?.['background-color']) {
      const container = document.getElementById(this.mapId);
      if (container) {
        container.style.backgroundColor = bgLayer.paint['background-color'];
      }
    }

    const sourceGroups = {};
    const sourceLayerMap = {};

    for (const layerConfig of styleConfig.layers) {
      if (layerConfig.type === 'background') continue;
      const sourceId = layerConfig.source;
      if (!sourceId) continue;

      if (!sourceGroups[sourceId]) {
        sourceGroups[sourceId] = styleConfig.sources[sourceId];
        sourceLayerMap[sourceId] = [];
      }
      sourceLayerMap[sourceId].push(layerConfig);
    }

    for (const [sourceId, sourceConfig] of Object.entries(sourceGroups)) {
      const layerConfigs = sourceLayerMap[sourceId];

      if (sourceConfig.type === 'raster') {
        const tiles = resolveTileUrls(sourceConfig.tiles);
        const tileGrid = createXYZ({ maxZoom: sourceConfig.maxzoom || 14 });
        const source = new XYZSource({
          urls: tiles,
          crossOrigin: 'anonymous',
          tileGrid,
          tileSize: sourceConfig.tileSize || 256,
        });
        olLayers.push(new TileLayer({ source }));
      } else if (sourceConfig.type === 'vector') {
        const tileUrls = resolveTileUrls(sourceConfig.tiles);
        const tileUrl = tileUrls[0] || '';
        const maxTileZoom = getMaxTileZoom(sourceConfig.maxzoom, config.maxZoom);
        console.error('[OpenLayers] createLayersFromStyleConfig debug:', {
          rawTiles: sourceConfig.tiles,
          resolvedTiles: tileUrls,
          finalUrl: tileUrl,
          sourceId,
          sourceConfig,
          configMaxZoom: config.maxZoom,
          maxTileZoom,
        });
        const tileGrid = createXYZ({ maxZoom: maxTileZoom });
        const source = new VectorTileSource({
          format: new MVT(),
          tileGrid,
          url: tileUrl,
          crossOrigin: 'anonymous',
        });

        applyTileUrlDegradation(source, maxTileZoom);

        const styleFn = createStyleFromLayerConfigs(layerConfigs);

        olLayers.push(new VectorTileLayer({
          source,
          style: styleFn,
          declutter: true,
        }));
      }
    }

    return olLayers;
  }

  createVectorTileBaseLayer(options = {}) {
    const { url, maxZoom = 14, maxTileZoom, tileGrid, styleFactory, styleFunction } = options;
    const normalizedUrl = resolveTileUrl(url);
    if (!normalizedUrl) {
      console.warn('Vector tile url is required when enabling Mapbox vector style.');
      return null;
    }

    // 最终用于请求的最大层级，同样强制不超过 14
    const finalMaxTileZoom = getMaxTileZoom(maxTileZoom, maxZoom);

    const vtTileGrid = tileGrid || createXYZ({ maxZoom: finalMaxTileZoom });

    const source = new VectorTileSource({
      crossOrigin: 'anonymous',
      format: new MVT(),
      tileGrid: vtTileGrid,
      url: normalizedUrl,
    });

    // 包装原始 tileUrlFunction，在更高层级时“降级”到 finalMaxTileZoom 的瓦片坐标
    applyTileUrlDegradation(source, finalMaxTileZoom);
    const styleFn =
      typeof styleFunction === 'function'
        ? styleFunction
        : typeof styleFactory === 'function'
          ? styleFactory()
          : createMapboxVectorStyle();

    return new VectorTileLayer({
      source,
      style: styleFn,
    });
  }

  useMapboxVectorBase(options = {}) {
    const nextVectorOptions = {
      ...this.vectorTileOptions,
      ...options,
    };
    const vectorLayer = this.createVectorTileBaseLayer(nextVectorOptions);
    if (!vectorLayer) {
      return null;
    }

    this.baseLayerType = 'vector';
    this.vectorTileOptions = nextVectorOptions;

    if (!this.map) {
      this.baseLayer = vectorLayer;
      return vectorLayer;
    }

    if (this.baseLayer) {
      this.map.removeLayer(this.baseLayer);
    }
    this.baseLayer = vectorLayer;
    this.map.getLayers().insertAt(0, vectorLayer);
    return vectorLayer;
  }

  getView() {
    return this.map?.getView();
  }

  getCenter() {
    const view = this.getView();
    const center = view?.getCenter();
    if (!center) return this.center;
    return toLonLat(center);
  }

  setCenter(center, zoom) {
    const view = this.getView();
    if (!view || !Array.isArray(center)) return;
    view.setCenter(fromLonLat(center));
    if (typeof zoom === 'number') {
      view.setZoom(zoom);
    }
  }

  getZoom() {
    const view = this.getView();
    return typeof view?.getZoom === 'function' ? view.getZoom() : this.zoom;
  }

  flyAction(position, zoom, duration = 600) {
    const view = this.getView();
    if (!view || !Array.isArray(position)) return;
    const animation = {
      center: fromLonLat(position),
      duration,
    };
    if (typeof zoom === 'number') {
      animation.zoom = zoom;
    }
    view.animate(animation);
  }

  resize() {
    if (!this.map) {
      console.warn('[OpenLayers] resize: 地图实例不存在');
      return;
    }
    
    // 检查地图容器尺寸
    const container = document.getElementById(this.mapId);
    if (!container) {
      console.warn('[OpenLayers] resize: 地图容器不存在');
      return;
    }
    
    const rect = container.getBoundingClientRect();
    if (rect.width === 0 || rect.height === 0) {
      console.error('[OpenLayers] resize: 地图容器尺寸为 0', rect);
      return;
    }
    
    // 先调用 updateSize，然后强制重新渲染
    this.map.updateSize();
    
    // 检查 View 状态是否正常
    const view = this.map.getView();
    const zoom = view.getZoom();
    const center = view.getCenter();
    
    if (isNaN(zoom) || !isFinite(zoom)) {
      console.error('[OpenLayers] resize: 缩放级别异常，重置');
      view.setZoom(this.zoom || 11);
    }
    
    if (!center || isNaN(center[0]) || isNaN(center[1])) {
      console.error('[OpenLayers] resize: 中心点异常，重置');
      view.setCenter(fromLonLat(this.center));
    }
    
    // 强制重新渲染
    this.map.render();
  }

  destroyMap() {
    if (this.map) {
      try {
        // 移除所有事件监听器
        if (this._markerClickListener) {
          this.map.un('singleclick', this._markerClickListener);
          this._markerClickListener = null;
        }

        // 移除所有图层
        this.layers.forEach(({ layer, source, clusterSource }) => {
          if (layer && this.map) {
            this.map.removeLayer(layer);
          }
          // 清理数据源
          if (source) {
            source.clear();
            if (source.dispose) {
              source.dispose();
            }
          }
          if (clusterSource) {
            clusterSource.clear();
            if (clusterSource.dispose) {
              clusterSource.dispose();
            }
          }
        });

        // 清空所有集合
        this.layers.clear();
        this.clusters.clear();
        this.markers.clear();

        // 清理地图实例
        if (this.map.dispose) {
          this.map.dispose();
        }

        // 重置地图容器
        const container = document.getElementById(this.mapId);
        if (container) {
          container.innerHTML = '';
        }

        // 移除目标并清空地图实例
        this.map.setTarget(null);
        this.map = null;
      } catch (error) {
        console.warn('销毁地图时出错:', error);
      }
    }

    // 重置状态
    this.isReady = false;
    this.layerClickHandlers?.clear?.();
    this.baseLayer = null;
  }

  removeLayer(layerId) {
    const layerInfo = this.layers.get(layerId);
    if (!layerInfo) return;
    if (layerInfo.layer && this.map) {
      this.map.removeLayer(layerInfo.layer);
    }
    this.layers.delete(layerId);
    this.clusters.delete(layerId);
    this.markers.delete(layerId);
    this.registerLayerClickHandler(layerId);
    this.cleanupMarkerClickBinding();
  }

  setLayerVisible(layerId, visible) {
    const info = this.layers.get(layerId);
    if (!info || !info.layer) return;
    info.layer.setVisible(visible);
    info.visible = visible;
  }

  ensureMarkerClickBinding() {
    if (!this.map || this._markerClickListener) return;
    this._markerClickListener = (event) => {
      if (!this.layerClickHandlers || this.layerClickHandlers.size === 0) {
        return;
      }
      const options = {
        hitTolerance: 6,
      };
      this.map.forEachFeatureAtPixel(
        event.pixel,
        (feature) => {
          const featureList = this.collectFeatureList(feature);
          if (!featureList.length) {
            return false;
          }
          const dataList = featureList.map((item) => item?.get?.('data')).filter(Boolean);
          if (dataList.length === 0) {
            return false;
          }
          const layerId =
            featureList.find((item) => item?.get?.('layerId'))?.get('layerId') ||
            dataList[0]?.layerId;
          if (!layerId || !this.layerClickHandlers.has(layerId)) {
            return false;
          }
          const coordinate =
            Array.isArray(event.coordinate) && event.coordinate.length >= 2
              ? toLonLat(event.coordinate)
              : undefined;
          this.triggerMarkerClick(layerId, {
            layerId,
            data: dataList[0],
            clusterData: dataList,
            initialIndex: 0,
            coordinate,
            pixel: event.pixel,
            event,
            mapInstance: this,
          });
          return true;
        },
        options,
      );
    };
    this.map.on('singleclick', this._markerClickListener);
  }

  cleanupMarkerClickBinding() {
    if (
      this._markerClickListener &&
      (!this.layerClickHandlers || this.layerClickHandlers.size === 0) &&
      this.map
    ) {
      this.map.un('singleclick', this._markerClickListener);
      this._markerClickListener = null;
    }
  }

  collectFeatureList(feature) {
    if (!feature || typeof feature.get !== 'function') return [];
    const features = feature.get('features');
    if (Array.isArray(features) && features.length > 0) {
      return features;
    }
    return [feature];
  }

  addMarkerCluster(layerId, points = [], options = {}) {
    if (!this.map || !layerId) {
      return;
    }

    if (!Array.isArray(points) || points.length === 0) {
      this.removeLayer(layerId);
      return;
    }

    this.removeLayer(layerId);

    const {
      color = '#4B7AFA',
      size = 24,
      clusterSize = 60,
      visible = true,
      renderMarker,
      renderClusterMarker,
      icon,
      image,
      clusterImage,
      onMarkerClick,
      anchor,
      markerWidth,
      markerHeight,
    } = options;

    if (typeof onMarkerClick === 'function') {
      this.registerLayerClickHandler(layerId, onMarkerClick);
      this.ensureMarkerClickBinding();
    } else {
      this.registerLayerClickHandler(layerId);
      this.cleanupMarkerClickBinding();
    }

    const vectorSource = new VectorSource();
    const markerList = points
      .map((item) => {
        const position = formatLonLat(
          item.position || item.lnglat || item.lngLat || item.coordinates,
        );
        if (!position) return null;
        const feature = new Feature({
          geometry: new Point(fromLonLat(position)),
        });
        const data = { ...item, layerId };
        feature.set('data', data);
        feature.set('layerId', layerId);
        return feature;
      })
      .filter(Boolean);

    if (markerList.length === 0) {
      console.warn(`图层 ${layerId} 缺少有效点位`);
      return;
    }

    vectorSource.addFeatures(markerList);

    const clusterSource = new ClusterSource({
      distance: clusterSize,
      source: vectorSource,
    });

    const iconSize = size; // 直接使用传入的 size，统一为 32px
    // 如果提供了 markerWidth 和 markerHeight，使用它们；否则使用 iconSize
    const markerW = markerWidth !== undefined ? markerWidth : iconSize;
    const markerH = markerHeight !== undefined ? markerHeight : iconSize;
    const view = this.map?.getView?.();
    const markerStyleFactory = (feature) => {
      const data = feature.get('data');
      if (typeof renderMarker === 'function') {
        try {
          const html = toHtmlString(renderMarker(data));
          const htmlStyle = createIconStyleFromHtml(html, markerW, markerH, anchor);
          if (htmlStyle) {
            return htmlStyle;
          }
        } catch (error) {
          console.warn('renderMarker 解析失败:', error);
        }
      }

      // 如果提供了图片，使用图片
      if (image) {
        const resolvedImage = resolveAssetUrl(image);
        const isOnline = data?.status === 'online';
        const dotColor = isOnline ? 'rgba(67, 207, 124, 1)' : 'rgba(166, 169, 171, 1)';
        const baseStyle = new Style({
          image: new Icon({
            anchor: [0.5, 0.5],
            src: resolvedImage,
            imgSize: [iconSize, iconSize],
          }),
        });
        const dotRadius = Math.max(4, Math.round(iconSize * 0.15));
        const dotStyle = new Style({
          geometry: (feat) => {
            const geometry = feat.getGeometry();
            if (!geometry || typeof geometry.getCoordinates !== 'function') {
              return geometry;
            }
            const coordinate = geometry.getCoordinates();
            const resolution = view?.getResolution?.() || 1;
            const offset = iconSize * 0.35 * resolution;
            return new Point([coordinate[0] + offset, coordinate[1] + offset]);
          },
          image: new CircleStyle({
            radius: dotRadius,
            fill: new Fill({ color: dotColor }),
            stroke: new Stroke({ color: '#fff', width: 1 }),
          }),
        });
        return [baseStyle, dotStyle];
      }
      return createDefaultMarkerStyle({ color, size, icon });
    };

    const clusterStyleFactory = (feature) => {
      const features = feature.get('features');
      if (Array.isArray(features) && features.length > 1) {
        const featureCount = features.length;
        const displayCount = featureCount > 99 ? '99+' : featureCount;
        let clusterHtml = '';
        if (typeof renderClusterMarker === 'function') {
          const markerProxy = {
            setOffset() {},
            setContent(value) {
              clusterHtml = toHtmlString(value);
            },
          };
          const context = {
            count: featureCount,
            marker: markerProxy,
            layerId,
            features,
          };
          withAMapPixel(() => renderClusterMarker(context));
        }
        // 如果提供了聚合图片，创建包含图片和数量的 HTML
        if (!clusterHtml && clusterImage) {
          const resolvedClusterImage = resolveAssetUrl(clusterImage);
          return new Style({
            image: new Icon({
              anchor: [0.5, 0.5],
              src: resolvedClusterImage,
              imgSize: [iconSize, iconSize],
            }),
            text: new Text({
              text: `${displayCount}`,
              fill: new Fill({ color: '#fff' }),
              stroke: new Stroke({
                color: 'rgba(0,0,0,0.45)',
                width: 2,
              }),
              font: 'bold 12px sans-serif',
              textBaseline: 'middle',
              textAlign: 'center',
            }),
          });
        }
        if (clusterHtml) {
          const customStyle = createIconStyleFromHtml(clusterHtml, markerW, markerH, anchor);
          if (customStyle) {
            return customStyle;
          }
        }
        // 默认样式
        return new Style({
          image: new CircleStyle({
            radius: Math.max(size, 20),
            fill: new Fill({ color: 'rgba(0,0,0,0.65)' }),
            stroke: new Stroke({
              color,
              width: 3,
            }),
          }),
          text: new Text({
            text: `${displayCount}`,
            fill: new Fill({ color: '#fff' }),
            font: 'bold 12px sans-serif',
            textBaseline: 'middle',
            textAlign: 'center',
          }),
        });
      }
      const singleFeature = features?.[0] || feature;
      return markerStyleFactory(singleFeature);
    };

    const layer = new VectorLayer({
      source: clusterSource,
      visible,
      style: clusterStyleFactory,
    });

    this.map.addLayer(layer);
    this.layers.set(layerId, {
      type: 'cluster',
      layer,
      source: vectorSource,
      clusterSource,
      options,
      visible,
    });
    this.clusters.set(layerId, clusterSource);
    this.markers.set(layerId, markerList);
  }

  //计算当前视野范围
  getViewPoints() {
    const view = this.getView();
    const extent = view.calculateExtent(this.map.getSize());
    const topLeft = toLonLat([extent[2], extent[3]]);
    const bottomRight = toLonLat([extent[0], extent[1]]);
    // 为啥取反了
    const ret = {
      rp: `${topLeft[1]},${topLeft[0]}`,
      lp: `${bottomRight[1]},${bottomRight[0]}`,
    };
    return ret;
  }
}

export default _OpenLayers;
