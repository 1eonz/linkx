/**
 * 文档
 * https://lbs.amap.com/api/javascript-api/summary
 */

import type { Position } from '../type';

import { Message } from '@/components/Message';
import { useCreateApp, useI18n } from '@/hooks';
import CursorList from '@/pages/map/cursorList.vue';
import MapPopup from '@/pages/map/mapPopup.vue';
import { delay } from '@/utils';
import { isArray, isNumber } from '@/utils/is';

import { cloneDeep } from 'lodash-es';

import { mapManager } from '../EMap';
import {
  BaseMap,
  BaseTrack,
  defaultLayerOptions,
  drawCallback,
  formatLonLat,
  getMapConfig,
  mapIsReady,
  thirdMaps,
} from '../helper';
import { gcj02towgs84, wgs84togcj02 } from '../transformLonlat';

let AMap: any = null;

/**
 * 坐标系转换
 * @param data
 * @param cto
 * @returns
 */
const convertFrom = (data: Position[], cto?: 'gcj02' | 'wgs84') => {
  const fn: (x: Position) => Position = cto === 'wgs84' ? gcj02towgs84 : wgs84togcj02;
  const ret: Position[] = [];

  data.forEach((item) => {
    ret.push(fn(item));
  });
  return ret;
};

/**
 * @description 高德地图封装
 * @class _AMap
 */
class _AMap extends BaseMap {
  drawEditor: any;
  drawTool: any;
  mouseTool: any;

  /**
   * Creates an instance of Map.
   * @param {*} config
   * @memberof Map
   */
  constructor(config) {
    super(config);
    // 不小于4 因为MarkerClusterer会不显示
    const minZoom = this.zooms[0];
    if (isNumber(minZoom) && minZoom < 4) {
      this.zooms[0] = 4;
    }
    this.initMap();
  }

  // 添加自定义弹窗
  addCustomPopup(options) {
    if (this.customPopup) {
      this.closeCustomPopup();
    }
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const { anchor, content, layerId, offset, position } = options;
    const infoWindow = new AMap.InfoWindow({
      anchor,
      content,
      isCustom: true, // 使用自定义窗体
      offset,
    });
    infoWindow.layerId = layerId;
    this.customPopup = infoWindow;
    infoWindow.open(map, position);
  }

  /**
   * 添加文字图层
   * @param {string} layerId 图层id
   * @param {{text, position, color}[]} data
   */
  async addLabelLayer(config) {
    const { data, layerId } = config;
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const layer = new AMap.LabelsLayer({
      zIndex: 0,
    });
    let points = data.map((item) => item.position);

    points = convertFrom(points);

    data.forEach((item, index) => {
      const lable = new AMap.LabelMarker({
        position: points[index],
        text: {
          content: item.text,
          style: {
            fillColor: item.color,
          },
        },
      });
      layer.add(lable);
    });

    map.add(layer);
    this.clusters.set(layerId, layer);
  }

  /**
   * @description 添加图层
   * @param {array} data marker点数据
   * @param {string} layerId 图层id
   * @param {string} image 点图片
   * @param {string} clusterImage 聚合点图片
   * @param {*} windowTemplate 弹窗模板
   * @param {boolean} isShow 是否显示 默认显示
   * @param {boolean} isCluster 是否聚合
   * @param {object} clusterSymple color
   * @param {number} width
   * @param {number} height
   */
  async addLayer(options) {
    await mapIsReady(this);
    options = { ...defaultLayerOptions, ...options };
    options.data = cloneDeep(options.data);
    const { data, isCluster = true, isShow = true, layerId, windowTemplate } = options;
    const { center, clusters, mapId } = this;
    const markers: any[] = [];
    const sourceMap = new Map();

    // 避免重复创建
    if (clusters.get(layerId)) return;

    if (!isArray(data)) {
      console.error('data must be array !');
      return;
    }

    const positions: Position[] = [];
    data.forEach((item) => {
      const pos = formatLonLat(item.position) || center;
      positions.push(pos as Position);
    });

    const convertFromPositions = convertFrom(positions);

    data.forEach((item, index) => {
      const position = convertFromPositions[index];
      const marker = this.getFeature({ ...item, position }, options);
      markers.push(marker);
      sourceMap.set(item.id, item);
    });

    // 聚合点
    if (isCluster) {
      const cluster = new _MarkerCluster({
        click: (e) => {
          if (!windowTemplate) return;
          this.markerPopup(e.markers, e.lnglat, options);
        },
        mapId,
        markers,
        renderClusterMarker: (context) => {
          this.renderClusterMarker({
            context,
            ...options,
          });
        },
        visible: isShow,
      });
      clusters.set(layerId, cluster);
    } else {
      const layer = new _CustomLayer({
        mapId,
        markers,
      });
      clusters.set(layerId, layer);
    }

    this.layers.set(layerId, { sourceMap, ...options });
  }

  /**
   * @description 添加marker点弹窗
   * @param {String} layerId 图层id
   * @param {Object} source 对应marker点数据
   */
  async addMarkerCustomPopup(layerId, source) {
    const { center, mapId } = this;
    const { id } = source;
    const { sourceMap, windowTemplate } = this.layers.get(layerId);
    let data = sourceMap.get(id);

    if (!data) {
      this.updateLayer(layerId, { addList: [source] });
      await delay(500);
      data = source;
    }

    const parent = document.createElement('div');
    const createEl = (data) => {
      const instance = useCreateApp(MapPopup, {
        data,
        layerId,
        mapId,
        template: windowTemplate,
      });
      return instance.mount(parent).$el;
    };
    const position = formatLonLat(data.position || center);

    this.setCenter(position);

    const convertFromPositions = convertFrom([position as Position]);

    this.popupConfig = {
      createEl,
      id: data.id,
      layerId,
    };
    this.addCustomPopup({
      content: createEl(data),
      layerId,
      offset: new AMap.Pixel(6, -2),
      position: convertFromPositions[0],
    });
  }

  /**
   * @description 添加比例尺以及鼠标移动显示鼠标经纬度
   * @param {*} scaleContainer 比例尺容器
   * @param {*} positionContainer 经纬度容器
   */
  addScaleAndPosition(scaleContainer, positionContainer) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    if (!map) {
      return;
    }

    const scaleRender = () => {
      const scale: number = map.getScale() / 100;
      let text = '';

      if (scale >= 1000) {
        text = `${(scale / 1000).toFixed(0)} km`;
      } else if (scale >= 100) {
        text = `${((scale / 100) as any).toFixed(0) * 100} m`;
      } else if (scale >= 10) {
        text = `${((scale / 10) as any).toFixed(0) * 10} m`;
      } else {
        text = `${scale.toFixed(0)} m`;
      }
      scaleContainer.innerHTML = text;
    };
    scaleRender();

    map.on('zoomchange', () => {
      scaleRender();
    });

    map.on('mousemove', (e) => {
      const { lat, lng } = e.lnglat;
      positionContainer.innerHTML = `${lng}，${lat}`;
    });
  }

  /**
   * @description 框选
   * @param {*} type 框选类型 点marker 线polyline 多边形polygon 矩形rectangle 圆circle
   * @param {*} layerIds 图层ids
   * @param {*} callback 回调函数
   */
  boxToSelect(type, layerIds, callback) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const mouseTool = new AMap.MouseTool(map);

    this.closeBoxToSelect();
    this.mouseTool = mouseTool;

    const style = {
      fillColor: '#00b0ff',
      strokeColor: '#80d8ff',
    };

    switch (type) {
      case 'circle': {
        mouseTool.circle(style);
        break;
      }
      case 'marker': {
        mouseTool.marker();
        break;
      }
      case 'polygon': {
        mouseTool.polygon(style);
        break;
      }
      case 'polyline': {
        mouseTool.polyline(style);
        break;
      }
      case 'rectangle': {
        mouseTool.rectangle(style);
        break;
      }
    }

    mouseTool.on('draw', (e) => {
      const retObj = {};
      const retMarkers: any[] = [];

      if (isArray(layerIds)) {
        const showLayerIds = layerIds.filter((item) => this.getLayerVisible(item));
        showLayerIds.forEach((layerId) => {
          const { sourceMap } = this.layers.get(layerId);
          const cluster = this.clusters.get(layerId);
          const markers = cluster.getMarkers();

          retObj[layerId] = [];

          for (const marker of markers) {
            const point = marker.getPosition();
            // 判断点是否在多边形、矩形、圆内
            const isContains = e.obj.contains(point);

            if (!isContains) continue;

            const sourceItem = sourceMap.get(marker.getExtData().id);
            if (sourceItem) {
              if (retMarkers.length >= 201) break;
              retMarkers.push(sourceItem);
              retObj[layerId].push(sourceItem);
            }
          }
        });
      }

      mouseTool.close(true);
      drawCallback(callback, retMarkers, retObj);
    });
  }

  // 清空地图
  clearMap() {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);

    this.clusters.clear();
    map?.clearMap();
  }

  // 关闭圈选
  closeBoxToSelect() {
    const { mouseTool } = this;
    mouseTool?.close(true);
  }

  // 关闭自定义弹窗
  closeCustomPopup() {
    if (this.customPopup) {
      this.customPopup.close();
      this.customPopup = null;
    }
  }

  /**
   * @description 删除图层
   * @param {*} layerIds 图层ids 不传删全部
   */
  deleteLayer(layerIds) {
    const { layers } = this;

    if (layerIds) {
      if (!isArray(layerIds)) {
        layerIds = [];
      }
    } else {
      // 删除全部图层
      layerIds = layers.keys();
    }

    layerIds.forEach((layerId) => {
      // 删除图层对应的聚合点
      const cluster = this.clusters.get(layerId);

      if (cluster) {
        cluster.clear();
        this.clusters.delete(layerId);
      }

      layers.delete(layerId);
    });
  }

  /**
   * @description 删除点
   * @param {String} layerId 图层id
   * @param {String} id 图标点id
   */
  deleteMarker(layerId, id) {
    const cluster = this.clusters.get(layerId);
    if (!cluster) return;
    const markers = cluster.getMarkers();
    markers.forEach((marker) => {
      const ext = marker.getExtData();
      if (ext.id === id) {
        marker.setMap(null);
        marker = null;
      }
    });
  }

  // 销毁地图
  destroyMap() {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);

    this.clearMap();
    map?.destroy();
    mapManager.delete(mapId);
  }

  /**
   * 隐藏显示绘制的区域
   * @param {boolean} visible 是否显示draw
   */
  disableDraw(visible) {
    const { drawLayer, mapId } = this;
    const map = thirdMaps.get(mapId);

    drawLayer?.forEach((item) => {
      const { graphics, handle, highlight } = item;
      if (handle === 'look' && !highlight) {
        if (visible) {
          map.add(graphics);
        } else {
          map.remove(graphics);
        }
      }
    });
  }

  /**
   * 绘制区域
   * @param {point | polyline | rectangle | circle | polygon} type 点、线、矩形、圆、多边形
   * @param {add | modify | look} handle 新增、修改、查看
   * @param {array} path 路径点集合
   * @param {array} center 圆的中心点
   * @param {number} radius 圆的半径
   * @param {function} change 绘制更新事件
   * @param {boolean} highlight 是否高亮
   * @param {string} color 颜色 '252, 167, 1'
   * @param {string} lineStyle 实线 solid，虚线 dashed
   */
  draw(config) {
    const { change, color, handle, highlight, lineStyle, radius, type } = config;
    let path = config.path;
    let center = config.center;

    if (config.path) {
      path = convertFrom(config.path);
    }
    if (config.center) {
      const convertFromPositions = convertFrom([config.center]);
      center = convertFromPositions[0];
    }

    const { drawEditor, drawLayer, drawTool, mapId } = this;
    const map = thirdMaps.get(mapId);
    let mouseTool: any = null;
    const style = {
      fillColor: color ? `rgba(${color}, 0.5)` : 'rgba(252, 167, 1, 0.5)',
      strokeColor: color ? `rgba(${color}, 1)` : 'rgba(252, 167, 1, 1)',
      strokeWeight: 4,
    };

    drawTool?.close(true);
    drawEditor?.close();
    drawLayer.forEach((item) => {
      if (item.handle !== 'look') {
        map.remove(item.graphics);
      }
    });

    const getGraphics = ({
      center,
      path,
      radius,
    }: {
      center?: Position[];
      path?: Array<Position[]>;
      radius?: number;
    }) => {
      const { fillColor, strokeColor, strokeWeight } = style;
      let graphics = null;

      switch (type) {
        case 'circle': {
          graphics = new AMap.Circle({
            center, // 圆心位置
            fillColor, // 圆形填充颜色
            radius, // 圆半径
            strokeColor, // 描边颜色
            strokeWeight, // 描边宽度
          });
          break;
        }
        case 'point': {
          graphics = new AMap.CircleMarker({
            center,
            fillColor: strokeColor,
            radius: radius || 5,
            strokeColor,
            strokeWeight: 0,
          });
          break;
        }
        case 'polygon': {
          graphics = new AMap.Polygon({
            fillColor, // 多边形填充颜色
            path,
            strokeColor, // 线条颜色
            strokeWeight, // 线条宽度
          });
          break;
        }
        case 'polyline': {
          graphics = new AMap.Polyline({
            lineJoin: 'round', // 折线拐点连接处样式
            path,
            strokeColor, // 线条颜色
            strokeStyle: lineStyle || 'solid',
            strokeWeight, // 线条宽度
          });
          break;
        }
        case 'rectangle': {
          graphics = new AMap.Rectangle({
            bounds: path,
            fillColor,
            strokeColor,
            strokeWeight,
          });
          break;
        }
      }
      return graphics;
    };

    const pathChange = (obj) => {
      switch (type) {
        case 'circle': {
          const center = obj.getCenter();
          const radius = obj.getRadius();
          const point = convertFrom([[center.lng, center.lat]], 'wgs84')[0];
          change({ center: point, radius });
          break;
        }
        case 'polygon':
        case 'polyline':
        case 'rectangle': {
          const path = obj.getPath().map((item) => [item.lng, item.lat]);
          change(convertFrom(path, 'wgs84'));
          break;
        }
      }
    };

    const drawer = ({
      center,
      path,
      radius,
      type,
    }: {
      center?: Position[];
      path?: Array<Position[]>;
      radius?: number;
      type: string;
    }) => {
      const isEdit = ['add', 'modify'].includes(handle);
      let graphics: any = null;
      let editor: any = null;

      switch (type) {
        case 'circle': {
          graphics = getGraphics({ center, radius });
          if (isEdit) {
            editor = new AMap.CircleEditor(map, graphics);
          }
          break;
        }
        case 'point': {
          graphics = getGraphics({ center, radius });
          break;
        }
        case 'polygon': {
          graphics = getGraphics({ path });
          if (isEdit) {
            editor = new AMap.PolyEditor(map, graphics);
          }
          break;
        }
        case 'polyline': {
          graphics = getGraphics({ path });
          if (isEdit) {
            editor = new AMap.PolyEditor(map, graphics);
          }
          break;
        }
        case 'rectangle': {
          const bounds = new AMap.Bounds(path?.[0], path?.[2]);
          graphics = getGraphics({ path: bounds });
          if (isEdit) {
            editor = new AMap.RectangleEditor(map, graphics);
          }
          break;
        }
      }

      map.add(graphics);
      if (isEdit) {
        editor.on('adjust', (e) => {
          pathChange(e.target);
        });
        this.drawEditor = editor;
      }
      this.drawLayer.push({ graphics, handle, highlight });

      return editor;
    };

    switch (handle) {
      case 'add': {
        mouseTool = new AMap.MouseTool(map);
        this.drawTool = mouseTool;

        switch (type) {
          case 'circle': {
            mouseTool.circle(style);
            break;
          }
          case 'polygon': {
            mouseTool.polygon(style);
            break;
          }
          case 'polyline': {
            mouseTool.polyline(style);
            break;
          }
          case 'rectangle': {
            mouseTool.rectangle(style);
            break;
          }
        }
        break;
      }
      case 'look': {
        if (!highlight) {
          style.strokeWeight = 1;
          style.fillColor = style.fillColor.replace('0.5', '0.1');
        }
        drawer({ center, path, radius, type });
        break;
      }
      case 'modify': {
        const editor = drawer({ center, path, radius, type });
        editor.open();
        break;
      }
    }

    mouseTool?.on('draw', (e) => {
      const { obj } = e;
      let editor: any = null;

      switch (type) {
        case 'circle': {
          const center = obj.getCenter();
          const radius = obj.getRadius();
          editor = drawer({ center, radius, type });
          break;
        }
        case 'polygon': {
          const path = obj.getPath();
          editor = drawer({ path, type });
          break;
        }
        case 'polyline': {
          const path = obj.getPath();
          editor = drawer({ path, type });
          break;
        }
        case 'rectangle': {
          const path = obj.getPath().map((item) => [item.lng, item.lat]);
          editor = drawer({ path, type });
          break;
        }
      }

      pathChange(obj);
      mouseTool.close(true);
      editor.open();
    });
  }

  // 获取地图中心点
  getCenter() {
    const map = thirdMaps.get(this.mapId);
    const center = map?.getCenter();
    return center ? [center.lng, center.lat] : this.center;
  }

  // 获取可视范围最大半径
  getDistance() {
    const map = thirdMaps.get(this.mapId);
    return map.getBounds();
  }

  /**
   * 获取marker点特征
   * @param {*} data
   * @param {*} options
   * @returns marker
   */
  getFeature(data, options) {
    const { isShow = true, windowTemplate } = options;
    const { position } = data;
    const content = this.getFeatureContent(data, options);
    const markerConfig = {
      extData: {
        id: data.id,
      },
      position,
      visible: isShow,
      // 没有设置偏移值缩放会发生偏移，x轴偏移半个宽度，y轴偏移整个高度
      // offset: new AMap.Pixel(-width / 2, -height),
    };

    markerConfig[content.key] = content.value;

    const marker = new AMap.Marker(markerConfig);
    // 鼠标点击marker事件
    marker.on('click', () => {
      if (!windowTemplate) return;
      this.markerPopup([marker], position, options);
    });
    return marker;
  }

  getFeatureContent(data, options) {
    const { height = 48, image, renderer, width = 40.5 } = options;
    if (renderer) {
      return {
        key: 'content',
        value: renderer(data, { mapType: 'amap' }),
      };
    } else {
      const icon = new AMap.Icon({
        image,
        imageSize: new AMap.Size(width, height),
        size: new AMap.Size(width, height),
      });
      return {
        key: 'icon',
        value: icon,
      };
    }
  }

  /**
   * @description 获取图层数据
   * @param {*} layerId
   * @returns {array}
   */
  getLayerData(layerId) {
    const layer = this.layers.get(layerId);
    if (layer) {
      return layer.data;
    }
    return [];
  }

  /**
   * @description 获取图层是否显示
   * @param {string} layerId
   */
  getLayerVisible(layerId) {
    const cluster = this.clusters.get(layerId);
    return Boolean(cluster?.getVisible());
  }

  // 获取当前地图缩放级别
  getZoom() {
    const map = thirdMaps.get(this.mapId);
    return map.getZoom();
  }

  // 初始化地图
  initMap() {
    const { t } = useI18n();
    const { center, mapId, zoom, zooms } = this;
    const load = () => {
      AMap = window.AMap;
      const map = new AMap.Map(mapId, {
        center,
        mapStyle: 'amap://styles/darkblue',
        resizeEnable: true,
        zoom,
        zooms,
      });

      map.on('complete', () => {
        thirdMaps.set(mapId, map);
        this.isReady = true;
        this.onLoad(this);
        this.zoomChange();

        // 先用这种方式校验key吧
        AMap.convertFrom(center, 'gps', (_, res) => {
          if (res === 'INVALID_USER_KEY') {
            // 用户key非法或过期
            Message({
              duration: 3000,
              message: t('resource.map.userKeyError'),
              type: 'error',
            });
          }
        });
      });
    };

    if (window.AMap) {
      load();
    } else {
      // 加载高德地图、插件
      const plugins = [
        'MarkerClusterer',
        'MouseTool',
        'PolyEditor',
        'RectangleEditor',
        'CircleEditor',
      ];
      const loadPlugin = plugins.map((item) => `AMap.${item}`).join(',');
      const AMapApi = document.createElement('script');
      const { mapKey } = getMapConfig(); // 'ebd4af226c75a32abd5fc4b879b0da92';

      // 加载成功
      window.onload = () => {
        load();

        // AMapUI 组件库
        const AMapUI = document.createElement('script');
        AMapUI.type = 'text/javascript';
        AMapUI.src = `https://webapi.amap.com/ui/1.1/main.js?v=1.1.1`;
        document.head.append(AMapUI);
      };

      AMapApi.type = 'text/javascript';
      AMapApi.src = `https://webapi.amap.com/maps?v=1.4.15&key=${mapKey}&plugin=${loadPlugin}&callback=onload`;
      document.head.append(AMapApi);

      // 隐藏logo
      const style = document.createElement('style');
      style.type = 'text/css';
      style.innerHTML = `.amap-logo{display:none !important;}`;
      document.head.append(style);
    }
  }

  /**
   * @description marker弹窗
   * @param {*} markers marker数据
   * @param {*} position 定位
   * @param {*} options 图层参数
   */
  markerPopup(markers, position, options) {
    const { mapId } = this;
    const { layerId, windowTemplate } = options;
    const { sourceMap } = this.layers.get(layerId);

    if (markers.length > 1) {
      // 聚合列表弹窗
      const data: any = [];
      markers.forEach((item) => {
        const sourceItem = sourceMap.get(item.getExtData().id);
        if (sourceItem) {
          data.push(sourceItem);
        }
      });
      const createEl = (data) => {
        const parent = document.createElement('div');
        const instance = useCreateApp(CursorList, {
          data,
          layerId,
          mapId,
          position: convertFrom([[position.lng, position.lat]], 'wgs84')[0],
          template: windowTemplate,
        });
        return instance.mount(parent).$el;
      };
      this.popupConfig = {
        createEl,
        id: data.id,
        layerId,
      };
      this.addCustomPopup({
        content: createEl(data),
        layerId,
        offset: new AMap.Pixel(6, -2),
        position,
      });
    } else {
      // marker 弹窗
      const data = sourceMap.get(markers[0].getExtData().id);
      const createEl = (data) => {
        const parent = document.createElement('div');
        const instance = useCreateApp(MapPopup, {
          data,
          layerId,
          mapId,
          template: windowTemplate,
        });
        return instance.mount(parent).$el;
      };
      const newPosition = formatLonLat(data.position) as Position;
      this.popupConfig = {
        createEl,
        id: data.id,
        layerId,
      };
      this.addCustomPopup({
        content: createEl(data),
        layerId,
        offset: new AMap.Pixel(6, -2),
        position: convertFrom([newPosition])[0],
      });
    }
  }

  /**
   * @description 自定义聚合点样式
   * @param {Object} context
   * @param {string} color 聚合点颜色
   * @param {string} clusterImage
   */
  renderClusterMarker(options) {
    const {
      clusterImage,
      clusterSymple,
      context,
      height = 48,
      renderCluster,
      width = 40.5,
    } = options;
    const { count } = context;
    const el = document.createElement('div');
    const countStr = count > 99 ? '99+' : count;

    if (renderCluster) {
      // 方案1 dom
      const size = 14 + `${count}`.length * 8;
      el.style.cssText = `
        width: ${width}px;
        height: ${height}px;
      `;
      el.innerHTML = renderCluster(countStr, { mapType: 'amap' });
      context.marker.setOffset(new AMap.Pixel(-size / 2, -size / 2));
      context.marker.setContent(el);
    } else {
      // 方案2 图片
      const img = document.createElement('img');
      img.src = clusterImage;
      img.style.cssText = `
        width: ${width}px;
        height: ${height}px;
        position: absolute;
        top: 0;
        left: 0;
        z-index: -1;
      `;
      el.style.cssText = `
        width: ${width}px;
        height: ${height}px;
        position: relative;
        display: flex;
        justify-content: center;
        padding-top: 8px;
        font-size: 18px;
        weight: bold;
        color: ${clusterSymple?.color || '#fff'}
      `;
      el.innerText = countStr;
      el.append(img);
      context.marker.setContent(el);
    }
  }

  /**
   * @description 设置地图中心点
   * @param {LngLat} center
   * @param {Number} zoom
   */
  async setCenter(center, zoom?) {
    const map = thirdMaps.get(this.mapId);
    if (!isNumber(zoom)) {
      zoom = this.zooms[1];
    }
    const position = formatLonLat(center);
    const convertFromPositions = convertFrom([position as Position]);
    if (!position) {
      console.log('center is null');
      return;
    }
    map.setZoomAndCenter(zoom, convertFromPositions[0]);
  }

  /**
   * @description 设置图层是否显示
   * @param {Array} layerIds
   * @param {Boolen} isShow
   */
  setLayersVisible(layerIds, isShow) {
    layerIds.forEach((layerId) => {
      const cluster = this.clusters.get(layerId);
      if (!cluster) return;
      if (isShow) {
        cluster.show();
      } else {
        cluster.hide();
        if (this.customPopup?.layerId === layerId) {
          this.closeCustomPopup();
        }
      }
    });
  }

  // 返回动向实例
  track(config) {
    return new _Track({ map: this, ...config });
  }

  /**
   * @description 更新图层
   * @param {string} layerId 图层id
   * @param {addList, deleteList, updateList} options
   */
  async updateLayer(layerId, options) {
    const { addList = [], deleteList = [], updateList = [] } = options;
    const { center, mapId } = this;
    const map = thirdMaps.get(mapId);
    if (!map) return;
    const layer = this.layers.get(layerId);
    const cluster = this.clusters.get(layerId);
    if (!layer || !cluster) return;
    const { sourceMap } = layer;
    const isShow = this.getLayerVisible(layerId);
    const markerMap = new Map();
    const markers = cluster.getMarkers();
    markers.forEach((item) => {
      const ext = item.getExtData();
      markerMap.set(ext.id, item);
    });

    // 删除
    if (deleteList?.length > 0) {
      const deleteFeatures: any[] = [];
      deleteList.forEach((item) => {
        const marker = markerMap.get(item.id);
        if (marker) {
          deleteFeatures.push(marker);
          sourceMap.delete(item.id);
        }
      });

      cluster.removeMarkers(deleteFeatures);
    }

    // 新增
    if (addList?.length > 0) {
      const addFeatures: any[] = [];
      const positions: Position[] = [];
      addList.forEach((item) => {
        const position = formatLonLat(item.position) || center;
        positions.push(position as Position);
      });
      const convertFromPositions = convertFrom(positions);

      addList.forEach((item, index) => {
        const position = convertFromPositions[index];
        const data = { ...item, position };
        const marker = this.getFeature(data, { ...layer, isShow });
        addFeatures.push(marker);
        sourceMap.set(item.id, item);
      });
      cluster.addMarkers(addFeatures);
    }

    // 更新
    if (updateList?.length > 0) {
      updateList.forEach((item) => {
        const marker = markerMap.get(item.id);
        const content = this.getFeatureContent(item, layer);
        if (content.key === 'icon') {
          marker.setIcon(content.value);
        } else {
          marker.setContent(content.value);
        }

        const position = (formatLonLat(item.position) || center) as Position;
        const convertFromPositions = convertFrom([position]);
        marker.setPosition(convertFromPositions[0]);

        sourceMap.set(item.id, { ...item, position });
      });
    }
  }

  // 注册地图缩放事件
  zoomChange() {
    const { clusters, mapId } = this;
    const map = thirdMaps.get(mapId);
    map.on('zoomchange', () => {
      const isMaxZoom = map.getZoom() >= this.zooms[1];
      for (const c of clusters.values()) {
        if (c.setGridSize) {
          c.setGridSize(isMaxZoom);
        }
      }
    });
  }
}

// 非聚合点图层
class _CustomLayer {
  mapId: string;
  markers: any[];
  visible: boolean;
  constructor(config) {
    this.mapId = '';
    this.visible = true;
    this.markers = [];
    Object.assign(this, config);
    this.init();
  }
  addMarkers(markers) {
    const map = thirdMaps.get(this.mapId);
    markers.forEach((item) => {
      if (this.visible) {
        item.show();
      } else {
        item.hide();
      }
    });

    map.add(markers);
    this.markers.push(...markers);
  }
  clear() {
    this.removeMarkers(this.getMarkers());
    this.markers = [];
  }
  getMarkers() {
    return this.markers;
  }
  getVisible() {
    return this.visible;
  }
  hide() {
    const { markers } = this;
    markers.forEach((item) => {
      item.hide();
    });
    this.visible = false;
  }
  init() {
    const map = thirdMaps.get(this.mapId);
    map.add(this.markers);
  }
  removeMarkers(markers) {
    const map = thirdMaps.get(this.mapId);
    const removeMap = new Map();
    if (!markers) return;

    map.remove(markers);
    markers.forEach((item) => {
      removeMap.set(item.id, item);
    });
    this.markers = this.markers.filter((item) => {
      return !removeMap.has(item.id);
    });
  }
  show() {
    const { markers } = this;
    markers.forEach((item) => {
      item.show();
    });
    this.visible = true;
  }
}

// 聚合点图层
class _MarkerCluster {
  click: () => void;
  cluster: any;
  mapId: string;
  markers: any[];
  renderClusterMarker: () => void;
  visible: boolean;
  constructor(config) {
    this.mapId = '';
    this.markers = [];
    this.visible = true;
    this.cluster = null;
    this.renderClusterMarker = () => {};
    this.click = () => {};
    Object.assign(this, config);
    this.init();
  }
  addMarkers(markers) {
    const { cluster, visible } = this;
    markers.forEach((item) => {
      if (visible) {
        item.show();
      } else {
        item.hide();
      }
      this.markers.push(item);
    });
    if (visible) {
      cluster.addMarkers(markers);
    }
  }
  clear() {
    this.cluster.clearMarkers();
    this.markers = [];
  }
  getMarkers() {
    return this.markers;
  }
  getVisible() {
    return this.visible;
  }
  hide() {
    const { cluster } = this;
    this.visible = false;
    const markers = cluster.getMarkers();
    cluster.removeMarkers(markers);
    markers.forEach((item) => item.hide());
  }
  init() {
    const map = thirdMaps.get(this.mapId);
    const markers = this.visible ? this.markers : [];
    const cluster = new AMap.MarkerClusterer(map, markers, {
      averageCenter: true,
      gridSize: 100,
      // 自定义聚合点样式
      renderClusterMarker: this.renderClusterMarker,
      zoomOnClick: false,
    });
    cluster.on('click', this.click);
    this.cluster = cluster;
  }
  removeMarkers(markers) {
    const { cluster } = this;
    const map = thirdMaps.get(this.mapId);
    const removeMap = new Map();

    if (!markers) return;
    markers.forEach((item) => {
      const { id } = item.getExtData();
      removeMap.set(id, item);
    });

    cluster.removeMarkers(markers);
    map.remove(markers);

    this.markers = this.markers.filter((item) => {
      const { id } = item.getExtData();
      return !removeMap.has(id);
    });
  }
  // 当缩放到最大层级时设置聚合距离为1
  setGridSize(isMaxZoom) {
    this.cluster.setGridSize(isMaxZoom ? 1 : 100);
  }
  show() {
    const { cluster, markers } = this;
    this.visible = true;
    cluster.addMarkers(markers);
    markers.forEach((item) => item.show());
  }
}

/**
 * @description Track 动向
 * @class Track
 */
class _Track extends BaseTrack {
  endpoint: any[];
  navgator: any;
  pathSimplifierIns: any;

  constructor(config) {
    super(config);
    this.endpoint = []; // 起止点
    this.initTrack();
  }

  changeStatus(status) {
    this.status = status;
    this.statusChange?.(status);
  }

  // 设置端点
  createEndpoint() {
    const { data, endImage, endpoint, mapId, startImage } = this;
    const amap = thirdMaps.get(mapId);
    // 添加图标点
    const addMarker = (image, position) => {
      const icon = new AMap.Icon({
        image,
        imageSize: new AMap.Size(36, 36),
      });
      const marker = new AMap.Marker({
        icon,
        offset: new AMap.Pixel(-18, -18),
        position,
        zIndex: 99,
      });
      amap.add(marker);
      endpoint.push(marker);
    };
    // 起点
    addMarker(startImage, data[0]);
    // 终点
    addMarker(endImage, data[data.length - 1]);
  }

  // 销毁动向
  destroyTrack() {
    const { endpoint } = this;

    endpoint.forEach((point) => {
      point.setMap(null);
    });
    this.pathSimplifierIns?.setData([]);
    this.navgator?.destroy();
  }

  // 初始化动向
  async initTrack() {
    const { color, data, image, mapId, move, speed } = this;
    const amap = thirdMaps.get(mapId);

    const paths = convertFrom(data);
    if (isArray(paths)) {
      this.data = paths;
    }

    window.AMapUI.load(['ui/misc/PathSimplifier'], (PathSimplifier) => {
      if (!PathSimplifier.supportCanvas) {
        Message('当前环境不支持 Canvas！');
        return;
      }

      // 创建组件实例
      const pathSimplifierIns = new PathSimplifier({
        clickToSelectPath: false,
        getHoverTitle() {
          return '';
        },
        getPath: (pathData) => {
          // 返回动向数据中的节点坐标信息
          return pathData.path;
        },
        map: amap, // 所属的地图实例
        renderOptions: {
          // 动向线的样式
          pathLineStyle: {
            dirArrowStyle: true,
            lineWidth: 2,
            strokeStyle: color,
          },
        },
        zIndex: 100,
      });

      this.pathSimplifierIns = pathSimplifierIns;

      // 动向
      const arr = [
        {
          name: 'track',
          path: paths,
        },
      ];

      pathSimplifierIns.setData(arr);

      const onload = () => {
        pathSimplifierIns.renderLater();
      };

      const onerror = () => {
        Message('图片加载失败！');
      };

      // 创建一个巡航器
      const navgator = pathSimplifierIns.createPathNavigator(0, {
        loop: false, // 循环播放
        pathNavigatorStyle: {
          // 使用图片
          content: PathSimplifier.Render.Canvas.getImageContent(image, onload, onerror),
          fillStyle: null,
          height: 48,
          initRotateDegree: -90, // 初始旋转角度
          // 经过路径的样式
          pathLinePassedStyle: {
            dirArrowStyle: {
              stepSpace: 15,
              strokeStyle: 'red',
            },
            lineWidth: 6,
            strokeStyle: 'black',
          },
          strokeStyle: null,
          width: 48,
        },
        speed: speed * 100,
      });

      // 移动事件
      navgator.on('move', (e) => {
        const cursor = e.target.getCursor();
        if (move) move(cursor.idx);
      });

      // 停止事件
      navgator.on('pause', () => {
        this.changeStatus('pause');
      });

      this.createEndpoint();
      this.navgator = navgator;
    });
  }

  // 暂停
  pause() {
    if (this.data.length === 1) return;
    this.navgator.pause();
    this.changeStatus('pause');
  }

  // 恢复显示
  recover() {
    const { pathLineStyle } = this.pathSimplifierIns.getRenderOptions();
    pathLineStyle.lineWidth = 2;
    this.pathSimplifierIns.render();
  }

  // 从新开始
  reStart() {
    if (this.data.length === 1) return;
    this.navgator.start();
    this.changeStatus('start');
  }

  // 设置高亮
  setHighlight() {
    const { pathLineStyle } = this.pathSimplifierIns.getRenderOptions();
    pathLineStyle.lineWidth = 6;
    this.pathSimplifierIns.render();
  }

  // 设置播放点
  setPosition(index) {
    this.navgator.start(index);
  }

  // 设置速度
  setSpeed(speed) {
    // 10倍数作为基数 因为实在太慢了
    this.speed = Math.max(speed, 0);
    this.navgator.setSpeed(this.speed * 100);
  }

  // 开始
  start() {
    const { data, navgator, status } = this;
    if (data.length === 1) return;
    if (status === 'pause') {
      navgator.resume();
    } else {
      navgator.start();
    }
    this.changeStatus('start');
  }
}

export default _AMap;
