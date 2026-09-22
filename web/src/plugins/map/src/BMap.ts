/**
 * 文档
 * https://lbsyun.baidu.com/index.php?title=jspopularGL
 * https://mapopen-pub-jsapi.bj.bcebos.com/jsapi/reference/jsapi_webgl_1_0.html
 * https://lbsyun.baidu.com/solutions/mapvdata
 */

import type { Position } from '../type';

import { useCreateApp } from '@/hooks';
import CursorList from '@/pages/map/cursorList.vue';
import MapPopup from '@/pages/map/mapPopup.vue';
import { colorRGBtoHex, delay } from '@/utils';
import { isArray, isNumber } from '@/utils/is';

import { cloneDeep } from 'lodash-es';

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
import { bd09towgs84, wgs84tobd09 } from '../transformLonlat';

function _BMap(config) {
  let BMapGL: any = null;
  let mapvgl: any = null;
  let BMapGLLib: any = null;
  let view: any = null;
  let isHttps: any = null;

  // 经纬度转换
  const transformTo = (position) => {
    if (isHttps) {
      return wgs84tobd09(position);
    }
    return position;
  };

  /**
   * 坐标系转换
   * @param data
   * @returns
   */
  const convertFrom = (data: Position[]) => {
    const ret: Position[] = [];
    data.forEach((item) => {
      ret.push(transformTo(item));
    });
    return ret;
  };

  /**
   * @description 百度地图封装
   * @class _BMap
   */
  class _BMap extends BaseMap {
    areaIndex = 1;
    circleIndex = 1;
    editDrawer: any;
    lineIndex = 1;
    /**
     * Creates an instance of Map.
     * @param {*} config
     * @memberof Map
     */
    constructor(config) {
      super(config);
      // 缩放等级范围[3,17]
      this.zooms[0] = Math.max(this.zooms[0], 3);
      this.zooms[1] = Math.min(this.zooms[1], 17);
      this.initMap();
    }

    /**
     * 添加多边形/矩形图层
     * @param {string} layerId 图层id
     * @param {{path,style}[]} config
     */
    async addAreaLayer(config) {
      const { areaIndex, drawLayer, mapId } = this;
      const { highlight, path, style } = config;
      const map = thirdMaps.get(mapId);
      const layerId = `area-${areaIndex}`;
      const pts: any = [];

      path.pop();
      path.forEach((item) => {
        const pos = transformTo(item);
        pts.push(new BMapGL.Point(pos[0], pos[1]));
      });

      const polygon = new BMapGL.Polygon(pts, style);
      map.addOverlay(polygon);

      drawLayer.push({ highlight, layer: polygon, layerId });
      this.areaIndex = areaIndex + 1;
    }

    /**
     * 添加圆形图层
     * @param {string} layerId 图层id
     * @param {{center, radius,style}[]} config
     */
    async addCircleLayer(config) {
      const { circleIndex, drawLayer, mapId } = this;
      const { center, highlight, radius, style } = config;
      const map = thirdMaps.get(mapId);
      const layerId = `circle-${circleIndex}`;
      const pos = transformTo(center);
      const pt = new BMapGL.Point(pos[0], pos[1]);
      const circle = new BMapGL.Circle(pt, radius, style);
      map.addOverlay(circle);

      drawLayer.push({ highlight, layer: circle, layerId });
      this.circleIndex = circleIndex + 1;
    }

    /**
     * @description 添加聚合点弹窗
     * @param {String} layerId 图层id
     * @param {Object} source 对应聚合点数据
     */
    async addCursorCustomPopup(layerId, source) {
      const { center, mapId } = this;
      const { geometry, children } = source;
      const { sourceMap, windowTemplate } = this.layers.get(layerId);
      const data: any = [];
      children?.forEach((item) => {
        const target = sourceMap.get(item.properties.id);
        if (target) {
          data.push(target);
        }
      });
      const parent = document.createElement('div');
      const position = formatLonLat(geometry.coordinates || center);
      const poi = isHttps ? bd09towgs84(position as Position) : position;
      const instance = useCreateApp(CursorList, {
        data,
        layerId,
        mapId,
        position: poi,
        template: windowTemplate,
      });
      this.markerPopup(instance.mount(parent).$el, position);
    }

    /**
     * 添加文字图层
     * @param {string} layerId 图层id
     * @param {{text, position, color}[]} data
     */
    async addLabelLayer(config) {
      const { data } = config;
      if (data.length === 0) return;
      const map = thirdMaps.get(this.mapId);
      data.forEach((item) => {
        const { color, position, text } = item;
        const pos = transformTo(position);
        const opts = {
          position: new BMapGL.Point(pos[0], pos[1]),
        };

        const label = new BMapGL.Label(text, opts);
        label.setStyle({
          backgroundColor: 'none',
          border: 'none',
          color,
          fontSize: '14px',
        });
        map.addOverlay(label);
      });
    }

    /**
     * @description 添加图层
     * @param {array} data marker点数据
     * @param {string} layerId 图层id
     * @param {string} image 点图片
     * @param {string} clusterImage 聚合点图片 clusterImage
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
      const {
        clusterImage,
        data,
        image,
        isShow = true,
        layerId,
        renderer,
        windowTemplate,
      } = options;

      const isAlarmLayer = layerId === 'alarm';
      const sourceMap = new Map();
      if (!isArray(data)) {
        console.error('data must be array !');
        return;
      }

      const jsonData = this.getJsonData(sourceMap, data, image, renderer);
      const layer = new mapvgl.IconClusterLayer({
        clusterRadius: 200, // 聚合范围半径
        enablePicked: true,
        iconExtent: {
          0: clusterImage,
          1000: clusterImage,
          5000: clusterImage,
          10_000: clusterImage,
        },
        iconOptions: {
          height: 48,
          width: 40.5,
        },
        maxZoom: 17,
        minZoom: 3,
        onClick: (e) => {
          if (e.dataItem && windowTemplate) {
            if (e.dataItem.children) {
              this.addCursorCustomPopup(layerId, e.dataItem);
            } else {
              this.addMarkerCustomPopup(layerId, e.dataItem);
            }
          }
        },
        showText: true,

        textOptions: {
          color: isAlarmLayer ? '#ff3b55' : '#ffffff',
          fontSize: 12,
          format: (count) => {
            if (count >= 10_000) {
              return `${Math.round(count / 1000)}k`;
            }
            return count >= 1000 ? `${Math.round(count / 100) / 10}k` : count;
          },
          offset: [0, -7],
        },
      });

      view.addLayer(layer);
      layer.setData(jsonData);
      if (!isShow) {
        view.hideLayer(layer);
        layer._visible = false;
      }
      this.layers.set(layerId, { sourceMap, ...options, layer });
    }

    /**
     * 添加线图层
     * @param {string} layerId 图层id
     * @param {{path, style}[]} config
     */
    async addLineLayer(config) {
      const { drawLayer, lineIndex, mapId } = this;
      const { highlight, lineStyle, path, style } = config;
      const map = thirdMaps.get(mapId);
      const layerId = `line-${lineIndex}`;
      const pts: any = [];
      if (lineStyle) {
        style.strokeStyle = 'dashed';
      }

      path.forEach((item) => {
        const pos = transformTo(item);
        pts.push(new BMapGL.Point(pos[0], pos[1]));
      });
      const polyline = new BMapGL.Polyline(pts, style);
      map.addOverlay(polyline);

      drawLayer.push({ highlight, layer: polyline, layerId });
      this.lineIndex = lineIndex + 1;
    }

    /**
     * @description 添加marker点弹窗
     * @param {String} layerId 图层id
     * @param {Object} source 对应marker点数据
     */
    async addMarkerCustomPopup(layerId, source) {
      const { center, mapId } = this;
      const id = source.id || source.properties.id;
      const { sourceMap, windowTemplate } = this.layers.get(layerId);
      let data = sourceMap.get(id);

      if (!data) {
        this.updateLayer(layerId, { addList: [source] });
        await delay(500);
        data = source;
      }

      const parent = document.createElement('div');
      const instance = useCreateApp(MapPopup, {
        data,
        layerId,
        mapId,
        template: windowTemplate,
      });
      const position = formatLonLat(data.position || center);
      if (!position) {
        console.error('position is null');
        return;
      }
      const pos = transformTo(position);
      this.markerPopup(instance.mount(parent).$el, pos);
    }

    /**
     * @description 添加比例尺以及鼠标移动显示鼠标经纬度
     * @param {*} scaleContainer 比例尺容器
     * @param {*} positionContainer 经纬度容器
     */
    addScaleAndPosition(scaleContainer, positionContainer) {
      const map = thirdMaps.get(this.mapId);
      const scale = new BMapGL.ScaleControl();

      const setScale = () => {
        const { innerText } = scale._container;
        scaleContainer.innerHTML = innerText;
      };

      map.on('zoomend', () => {
        setScale();
      });
      map.on('mousemove', (e) => {
        const { lat, lng } = e.latlng;
        positionContainer.innerHTML = `${lng.toFixed(6)}，${lat.toFixed(6)}`;
      });

      map.addControl(scale, 'BMAP_ANCHOR_BOTTOM_LEFT');
      setScale();
    }

    /**
     * @description 框选
     * @param {*} type 框选类型 点marker 线polyline 多边形polygon 矩形rectangle 圆circle
     * @param {*} layerIds 图层ids
     * @param {*} callback 回调函数
     */
    boxToSelect(type, layerIds, callback) {
      const { editDrawer, mapId } = this;
      const map = thirdMaps.get(mapId);
      this.drawChangeMode(type);

      const getMarkers = (overlay) => {
        editDrawer.removeEventListener('overlaycomplete', getMarkers);

        map.clearOverlays();
        this.closeBoxToSelect();

        const retObj = {};
        const retMarkers: any[] = [];
        const showLayerIds = layerIds.filter((item) => this.getLayerVisible(item));

        if (isArray(showLayerIds)) {
          showLayerIds.forEach((layerId) => {
            const { layer, sourceMap } = this.layers.get(layerId);
            if (sourceMap.size === 0) return;
            const markers = layer.data;

            retObj[layerId] = [];
            for (const { geometry, properties } of markers) {
              const point = geometry.coordinates;
              const isContains = this.pointIsContains(overlay, point);
              if (!isContains) continue;

              const sourceItem = sourceMap.get(properties.id);
              if (sourceItem) {
                if (retMarkers.length >= 201) break;
                retMarkers.push(sourceItem);
                retObj[layerId].push(sourceItem);
              }
            }
          });
        }
        drawCallback(callback, retMarkers, retObj);
      };

      const cancel = () => {
        editDrawer.removeEventListener('overlaycancel', cancel);
        drawCallback(callback, [], {});
      };

      // 框选完成
      editDrawer.addEventListener('overlaycomplete', getMarkers);
      // 框选取消
      editDrawer.addEventListener('overlaycancel', cancel);
    }

    /**
     * 修改图层颜色
     * @param {string} layerId 图层id
     * @param {{type,colorHex}[]} config
     */
    async changeLayerColor(config) {
      const { colorHex, type } = config;
      let id = '';
      switch (type) {
        case 'circle': {
          id = 'circle-1';
          break;
        }
        case 'polyline': {
          id = 'line-1';
          break;
        }
        default: {
          id = 'area-1';
          break;
        }
      }
      this.drawLayer?.forEach((item) => {
        const { layer, layerId } = item;
        if (layerId === id) {
          layer.setStrokeColor(colorHex);
          layer.setFillColor(colorHex);
        }
      });
    }

    // 清空地图
    clearMap() {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      map?.clearOverlays();
    }

    // 删除全部框选
    closeBoxToSelect() {
      this.editDrawer?.close();
    }

    // 关闭自定义弹窗
    closeCustomPopup() {
      const map = thirdMaps.get(this.mapId);
      map.removeOverlay(this.customPopup);
      this.customPopup = null;
    }

    /**
     * @description 删除图层
     * @param {string[]} layerIds 图层ids 不传删全部
     */
    deleteLayer(layerIds) {
      if (layerIds) {
        if (!isArray(layerIds)) {
          layerIds = [];
        }
      } else {
        view.removeAllLayers();
        return;
      }

      layerIds.forEach((layerId) => {
        const res = this.layers.get(layerId);

        view.removeLayer(res?.layer);
      });
    }

    // 销毁地图
    destroyMap() {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      this.disposeBoxToSelect();
      this.clearMap();
      map?.destroy();
    }

    /**
     * 隐藏显示绘制的区域
     * @param {boolean} visible 是否显示draw
     */
    disableDraw(visible) {
      const { drawLayer, mapId } = this;
      const map = thirdMaps.get(mapId);
      const overlays = map.getOverlays();

      overlays?.forEach((item) => {
        if (!item?.content) return;
        if (visible) {
          item.show();
        } else {
          item.hide();
        }
      });

      drawLayer?.forEach((item) => {
        const { highlight, layer } = item;
        if (highlight) return;
        if (visible) {
          layer.show();
        } else {
          layer.hide();
        }
      });
    }

    // 销毁框选
    disposeBoxToSelect() {
      this.editDrawer?.close();
      this.editDrawer = null;
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
      const { center, change, color, handle, highlight, lineStyle, path, radius, type } = config;
      const colorHex = colorRGBtoHex(`(${color})`);
      const style = {
        fillColor: colorHex || '#FCA701',
        fillOpacity: 0.2,
        strokeColor: colorHex || '#FCA701',
        strokeOpacity: 0.5,
        strokeWeight: 2,
      };

      this.closeBoxToSelect();

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
        const param = { center, highlight, lineStyle, path, radius, style };
        switch (type) {
          case 'circle': {
            this.addCircleLayer(param);
            break;
          }
          case 'polygon': {
            this.addAreaLayer(param);
            break;
          }
          case 'polyline': {
            this.addLineLayer(param);
            break;
          }
          case 'rectangle': {
            this.addAreaLayer(param);
            break;
          }
        }
      };

      switch (handle) {
        case 'add': {
          this.drawChangeMode(type, style);
          break;
        }
        case 'look': {
          if (!highlight) {
            style.strokeWeight = 1;
            style.fillOpacity = 0.1;
          }
          drawer({ center, path, radius, type });
          break;
        }
        case 'modify': {
          this.changeLayerColor({ colorHex, type });
          break;
        }
      }

      const updateArea = (e, res) => {
        this.editDrawer.removeEventListener('overlaycomplete', updateArea);
        const { drawingMode, overlay } = res;
        const { _radiusMercator, latLng } = overlay;
        const points = e.overlay.getPath();

        if (drawingMode === 'circle') {
          change({ center: [latLng.lng, latLng.lat], radius: _radiusMercator });
          return;
        }

        const retPoints: any = [];
        points.forEach((item) => {
          const { lat, lng } = item;
          retPoints.push([lng, lat]);
        });
        change(retPoints);
      };

      // 区域改变事件
      if (handle !== 'look') {
        // 结束绘制
        this.editDrawer.addEventListener('overlaycomplete', updateArea);
      }
    }
    /**
     * 绘制图形
     * @param {point | polyline | rectangle | circle | polygon} type 点、线、矩形、圆、多边形
     * @param {*} style color
     */
    drawChangeMode(type, style?) {
      const { editDrawer } = this;
      const drawObj = {
        circle: window.BMAP_DRAWING_CIRCLE,
        polygon: window.BMAP_DRAWING_POLYGON,
        polyline: window.BMAP_DRAWING_POLYLINE,
        rectangle: window.BMAP_DRAWING_RECTANGLE,
      };
      const drawingType = drawObj[type];
      const active = editDrawer.getDrawingMode();

      if (style) {
        switch (type) {
          case 'circle': {
            editDrawer.setCircleOptions(style);
            break;
          }
          case 'polygon': {
            editDrawer.setPolygonOptions(style);
            break;
          }
          case 'polyline': {
            editDrawer.setPolylineOptions(style);
            break;
          }
          case 'rectangle': {
            editDrawer.setRectangleOptions(style);
            break;
          }
        }
      }

      if (editDrawer._isOpen && active === drawingType) {
        editDrawer.close();
      } else {
        editDrawer.setDrawingMode(drawingType);
        editDrawer.open();
      }
    }

    // 获取地图中心点
    getCenter() {
      const map = thirdMaps.get(this.mapId);
      const center = map?.getCenter();
      const point = center ? [center.lng, center.lat] : this.center;
      const poi = isHttps ? bd09towgs84(point as Position) : point;
      return poi;
    }

    /**
     * @description 构造jsonData
     * @param {*} sourceMap
     * @param {array} data 图层数据
     * @returns {array} jsonData
     */
    getJsonData(sourceMap, data, image, renderer) {
      console.log('======image', image);
      const { center } = this;
      const jsonData: any = [];
      data.forEach((item) => {
        const { id, name, position } = item;
        const icon = renderer ? renderer(item, { mapType: 'BMap' }) : image;
        const pos = formatLonLat(position) || center;
        const coordinates = transformTo(pos as Position);
        const style = {
          height: 48,
          width: 40.5,
        };
        if (renderer && name) {
          const width = name.length * 10;
          style.width = Math.max(width, 54);
          style.height = 64;
        }

        const feature = {
          geometry: {
            coordinates,
            type: 'Point',
          },
          properties: {
            height: style.height,
            icon,
            id,
            width: style.width,
          },
        };
        jsonData.push(feature);
        item.position = pos;
        sourceMap.set(id, item);
      });
      return jsonData;
    }

    /**
     * @description 获取图层数据
     * @param {*} layerId
     * @returns {array}
     */
    getLayerData(layerId) {
      const { layer, sourceMap } = this.layers.get(layerId);
      const ret: any = [];

      if (!layer) {
        return ret;
      }

      const _data = layer.getData();
      _data.forEach((item) => {
        const { id } = item.properties;
        const target = sourceMap.get(id);
        if (target) {
          ret.push(target);
        }
      });

      return ret;
    }

    /**
     * @description 获取图层是否显示
     * @param {string} layerId
     */
    getLayerVisible(layerId) {
      const { layer } = this.layers.get(layerId);
      return layer?._visible;
    }

    // 获取当前地图缩放级别
    getZoom() {
      const map = thirdMaps.get(this.mapId);
      return map.getZoom();
    }

    // 初始化地图
    initMap() {
      const { center, mapId, zoom, zooms } = this;
      const { BMAPGL_STATIC_URL, BMAPGL_STYLE_URL, BMAPGL_URL, mapKey, TRAFFIC_URL } =
        getMapConfig();
      const mapCenter = center || [];
      const poi = transformTo(mapCenter as Position);
      let map: any = null;

      const viewLoad = () => {
        mapvgl = window.mapvgl;
        view = new mapvgl.View({
          map,
        });
        this.isReady = true;
      };

      const drawLoad = () => {
        BMapGLLib = window.BMapGLLib;
        this.registerDraw(map);
      };

      // 加载离线资源JS
      const loadJS = () => {
        map.removeEventListener('tilesloaded');
        if (window.BMapGLLib || window.mapvgl) {
          viewLoad();
          drawLoad();
          return;
        }

        const publicPath = window.location.origin + import.meta.env.VITE_PUBLIC_PATH;
        // mapvgl
        const VglJS = document.createElement('script');
        VglJS.type = 'text/javascript';
        VglJS.src = `${publicPath}/libs/BMap/mapvgl.min.js`;
        document.head.append(VglJS);

        // DrawCss
        const DrawCss = document.createElement('link');
        DrawCss.type = 'text/css';
        DrawCss.rel = 'stylesheet';
        DrawCss.href = `${publicPath}/libs/BMap/DrawingManager.min.css`;
        document.body.append(DrawCss);

        // DrawJs
        const DrawJs = document.createElement('script');
        DrawJs.type = 'text/javascript';
        DrawJs.src = `${publicPath}/libs/BMap/DrawingManager.min.js`;
        document.head.append(DrawJs);

        VglJS.addEventListener('load', () => {
          viewLoad();
        });
        DrawJs.addEventListener('load', () => {
          drawLoad();
        });
      };

      const init = () => {
        BMapGL = window.BMapGL;
        map = new BMapGL.Map(mapId, {
          maxZoom: zooms[1],
          minZoom: zooms[0],
        });
        const point = new BMapGL.Point(poi[0], poi[1]);
        map.centerAndZoom(point, zoom);
        map.enableScrollWheelZoom();

        map.addEventListener('tilesloaded', () => {
          this.onLoad(this);
          thirdMaps.set(mapId, map);
          loadJS();
        });
      };

      if (window.BMapGL) {
        init();
        return;
      }

      window.initBMap = init;

      isHttps = !BMAPGL_STATIC_URL;
      if (isHttps) {
        // 公网
        const mapApi = document.createElement('script');
        mapApi.type = 'text/javascript';
        // jwY5w9AzmuUG58K2OFG5N8RA9fDBu0Tw
        mapApi.src = `https://api.map.baidu.com/api?type=webgl&v=3.0&ak=${mapKey}&callback=initBMap`;
        document.head.append(mapApi);
      } else {
        // 辽宁现场
        window.BMAP_AUTHENTIC_KEY = mapKey;
        window.BMAPGL_STATIC_URL = BMAPGL_STATIC_URL;
        window.BMAPGL_URL = BMAPGL_URL;
        window.TRAFFIC_URL = TRAFFIC_URL;
        window.BMAPGL_STYLE_URL = BMAPGL_STYLE_URL;
        window.BMAPGL_PATH = '/dugis-demo-3d';
        window.BMAPGL_84 = true;

        const bmapCss = document.createElement('link');
        bmapCss.type = 'text/css';
        bmapCss.rel = 'stylesheet';
        bmapCss.href = `${window.BMAPGL_STATIC_URL + window.BMAPGL_PATH}/api/bmap.css`;
        document.body.append(bmapCss);

        const ApiJS = document.createElement('script');
        ApiJS.type = 'text/javascript';
        ApiJS.src = `${window.BMAPGL_STATIC_URL + window.BMAPGL_PATH}/api/api.js`;
        document.head.append(ApiJS);

        ApiJS.addEventListener('load', () => {
          window.initBMap();
        });
      }
    }

    /**
     * @description marker弹窗
     * @param {*} el dom
     * @param {*} options 图层参数
     */
    markerPopup(el, position) {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);

      this.closeCustomPopup();

      const createDOM = () => {
        return el;
      };
      const point = new BMapGL.Point(position[0], position[1]);
      this.customPopup = new BMapGL.CustomOverlay(createDOM, {
        offsetX: -5,
        offsetY: -20,
        point,
      });
      map.addOverlay(this.customPopup);
      map.setCenter(point);
    }

    /**
     * @description  判断点是否在多边形polygon、矩形rectangle、圆circle内
     * @param {*} layer 绘制的图形
     * @param {*} point 点
     */
    pointIsContains(layer, point) {
      const map = thirdMaps.get(this.mapId);
      const pt = {
        lat: point[1],
        lng: point[0],
      };
      const { drawingMode, overlay } = layer;
      const { _radiusMercator, _userPoints, latLng } = overlay;
      switch (drawingMode) {
        case 'circle': {
          const dis = map.getDistance(pt, latLng);
          return dis <= _radiusMercator;
        }
        case 'polygon': {
          const x = pt.lng;
          const y = pt.lat;
          let inside = false;
          for (let i = 0, j = _userPoints.length - 1; i < _userPoints.length; j = i++) {
            const xi = _userPoints[i].latLng.lng;
            const yi = _userPoints[i].latLng.lat;
            const xj = _userPoints[j].latLng.lng;
            const yj = _userPoints[j].latLng.lat;
            const intersect = yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi;
            if (intersect) inside = !inside;
          }
          return inside;
        }
        case 'rectangle': {
          const bound = map.getBounds();
          return bound.containsPoint(pt);
        }
        // No default
      }
      return false;
    }

    /**
     * @description 注册绘图
     */
    registerDraw(map) {
      const styleOptions = {
        fillColor: '#5E87DB', // 填充颜色
        fillOpacity: 0.2,
        strokeColor: '#00FFCA', // 边线颜色
        strokeOpacity: 1,
        strokeWeight: 2,
      };

      const labelOptions = {
        background: '#FFFBCC',
        border: '.0625rem solid #E1E1E1',
        borderRadius: '.125rem',
        color: '#703A04',
        fontSize: '.75rem',
        letterSpacing: '0',
        padding: '.3125rem',
      };

      const draw = new BMapGLLib.DrawingManager(map, {
        circleOptions: styleOptions, // 圆的样式
        enableCalculate: false, // 绘制是否进行测距测面
        enableSorption: true, // 是否开启边界吸附功能
        labelOptions, // label样式
        polygonOptions: styleOptions, // 多边形的样式
        polylineOptions: styleOptions, // 线的样式
        rectangleOptions: styleOptions, // 矩形的样式
        sorptiondistance: 20, // 边界吸附距离
      });

      this.editDrawer = draw;
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
      if (!position) {
        console.log('center is null');
        return;
      }
      const pos = transformTo(position);
      const point = new BMapGL.Point(pos[0], pos[1]);
      map.centerAndZoom(point, zoom);
    }

    /**
     * @description 设置图层是否显示
     * @param {Array} layerIds
     * @param {Boolen} isShow
     */
    setLayersVisible(layerIds, isShow) {
      if (layerIds?.length) {
        layerIds.forEach((layerId) => {
          const layerResource = this.layers.get(layerId);
          if (!layerResource) return;
          const { layer } = layerResource;
          const visible = layer?._visible;

          if (isShow && !visible) {
            view.showLayer(layer);
            layerResource.layer._visible = true;
            this.layers.set(layerId, layerResource);
          } else if (!isShow && visible) {
            view.hideLayer(layer);
            layerResource.layer._visible = false;
            this.layers.set(layerId, layerResource);
          }
        });
      }
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
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      if (!map) return;
      const layerResource = this.layers.get(layerId);
      if (!layerResource) return;
      const { image, layer, renderer, sourceMap } = layerResource;
      const layerData = this.getLayerData(layerId);
      let newLayerData: any[] = [];

      // 删除
      if (deleteList?.length > 0) {
        const deleteIds = new Map();
        deleteList.forEach((item) => {
          deleteIds.set(item.id, item.id);
          sourceMap.delete(item.id);
        });
        layerData.forEach((item) => {
          if (!deleteIds.has(item.id)) {
            newLayerData.push(item);
          }
        });
      }
      // 更新
      if (updateList?.length > 0) {
        newLayerData = updateList;
      }
      // 新增
      if (addList?.length > 0) {
        newLayerData = [...layerData, ...addList];
      }

      const jsonData = this.getJsonData(sourceMap, newLayerData, image, renderer);
      layer.setData(jsonData);
    }
  }
  /**
   * @description Track 动向
   * @class Track
   */
  class _Track extends BaseTrack {
    carMarker: any;
    currentIndex: number;
    lineLayer: any;
    playIndex: number;
    playTimer: any;
    viewTrack: any;
    constructor(config) {
      super(config);
      this.viewTrack = null;
      this.lineLayer = null;
      this.carMarker = null;
      this.currentIndex = 0;
      this.playIndex = 0;

      this.initTrack();
    }

    // 运动
    animation() {
      const { currentIndex, data, move } = this;
      if (data.length === 0) return;
      if (currentIndex === data.length - 1) {
        this.changeStatus('pause');
        return;
      }
      const p1 = data[currentIndex];
      const p2 = data[++this.currentIndex];
      const points = this.interpolation(p1, p2);
      const angle = this.getAngle(p1, p2);

      this.playIndex = 0;
      this.playInterpolation(points, angle);
      if (move) {
        move(this.currentIndex);
      }
    }

    // 改变状态
    changeStatus(status) {
      this.status = status;
      this.statusChange?.(status);
    }
    // 销毁动向
    destroyTrack() {
      const { map, viewTrack } = this;
      const mmap = thirdMaps.get(map.mapId);
      mmap.clearOverlays();
      viewTrack.removeAllLayers();
      viewTrack.destroy();
    }
    // 画小车
    drawCar(lnglat) {
      const { image, map } = this;
      const mmap = thirdMaps.get(map.mapId);
      const pos = transformTo(lnglat);
      const pt = new BMapGL.Point(pos[0], pos[1]);
      const car = new BMapGL.Icon(image, new BMapGL.Size(48, 48));
      this.carMarker = new BMapGL.Marker(pt, { icon: car });
      mmap.addOverlay(this.carMarker);
    }
    // 画端点
    drawEndpoint(url, lnglat) {
      const mmap = thirdMaps.get(this.map.mapId);
      const myIcon = new BMapGL.Icon(url, new BMapGL.Size(36, 38));
      const pos = transformTo(lnglat);
      const pt = new BMapGL.Point(pos[0], pos[1]);
      const marker = new BMapGL.Marker(pt, {
        icon: myIcon,
        offset: new BMapGL.Size(0, -19),
      });
      mmap.addOverlay(marker);
    }
    // 画线
    drawLine() {
      const { color, data, map } = this;
      const mmap = thirdMaps.get(map.mapId);
      this.viewTrack = new mapvgl.View({
        map: mmap,
      });
      const coordinates = convertFrom(data);
      const jsonData = [
        {
          geometry: {
            coordinates,
            type: 'LineString',
          },
        },
      ];
      const lineLayer = new mapvgl.LineRainbowLayer({
        color: [color],
        style: 'road',
        width: 5,
      });
      this.lineLayer = lineLayer;
      this.viewTrack.addLayer(lineLayer);
      lineLayer.setData(jsonData);
    }
    // 计算图片的角度
    getAngle(p1, p2) {
      const p1x = p1[0];
      const p1y = p1[1];
      const p2x = p2[0];
      const p2y = p2[1];
      let tan = 0;
      tan =
        p2x === p1x
          ? (Math.atan(0) * 180) / Math.PI
          : (Math.atan(Math.abs((p2y - p1y) / (p2x - p1x))) * 180) / Math.PI;

      if (p2x >= p1x && p2y >= p1y) {
        // 第一象限
        return -tan;
      } else if (p2x > p1x && p2y < p1y) {
        // 第四象限
        return tan;
      } else if (p2x < p1x && p2y > p1y) {
        // 第二象限
        return tan - 180;
      } else {
        // 第三象限
        return 180 - tan;
      }
    }
    // 两点间的中间点
    getMidPoint(pointA, pointB) {
      const pointArr = [
        new BMapGL.Point(pointA[0], pointA[1]),
        new BMapGL.Point(pointB[0], pointB[1]),
      ];
      const lngA = Number.parseFloat(pointArr[0].lng);
      const latA = Number.parseFloat(pointArr[0].lat);
      const lngB = Number.parseFloat(pointArr[1].lng);
      const latB = Number.parseFloat(pointArr[1].lat);
      const lngca = (Math.max(lngA, lngB) - Math.min(lngA, lngB)) / 2;
      const latca = (Math.max(latA, latB) - Math.min(latA, latB)) / 2;
      const lngCenter = Math.min(lngA, lngB) + lngca;
      const latCenter = Math.min(latA, latB) + latca;
      const midpoint = [lngCenter, latCenter];
      return midpoint;
    }
    // 初始化动向
    async initTrack() {
      const { data, endImage, map, startImage } = this;
      map.setCenter(data[0]);
      this.drawLine();
      this.drawEndpoint(startImage, data[0]);
      this.drawEndpoint(endImage, data[data.length - 1]);
      this.drawCar(data[0]);
    }
    // 两个点之间插值 - 根据两点之间的距离来插值
    interpolation(point1, point2) {
      const mmap = thirdMaps.get(this.map.mapId);
      const ptA = {
        lat: point1[1],
        lng: point1[0],
      };
      const ptB = {
        lat: point2[1],
        lng: point2[0],
      };
      const distance = mmap.getDistance(ptA, ptB); // 两点距离-米
      if (distance === 0) {
        return [point1];
      }

      let route = [point1, point2];
      const getMid = (points) => {
        const arr: any = [];
        points.forEach((point, index) => {
          if (index >= points.length - 1) {
            arr.push(point);
            return;
          }
          const midpoint = this.getMidPoint(point, points[index + 1]);
          arr.push(point, midpoint);
        });
        route = arr;
      };

      while (route.length <= distance) {
        getMid(route);
      }

      return route;
    }
    // 暂停
    pause() {
      if (this.data.length === 1) return;
      this.changeStatus('pause');
    }
    // 播放两个点之间的插值
    playInterpolation(points, angle) {
      const { carMarker, playIndex, playTimer, speed, status } = this;
      if (playTimer) clearTimeout(playTimer);
      this.playTimer = setTimeout(
        () => {
          if (status === 'destroy') return;
          if (status === 'pause') {
            this.playInterpolation(points, angle);
            return;
          }
          if (playIndex < points.length - 1) {
            const lnglat = points[++this.playIndex];
            const pos = transformTo(lnglat);
            const pt = new BMapGL.Point(pos[0], pos[1]);
            carMarker.setPosition(pt);
            carMarker.setRotation(angle);
            this.playInterpolation(points, angle);
          } else {
            this.playIndex = 0;
            clearTimeout(playTimer);
            this.animation();
          }
        },
        (10 / speed) * 10,
      );
    }

    // 恢复显示
    recover() {
      const { color, lineLayer } = this;
      lineLayer.setOptions({
        color: [color],
        width: 4,
      });
    }

    // 重新开始
    reStart() {
      if (this.data.length === 1) return;
      this.changeStatus('start');
      this.setPosition(0);
    }

    // 设置高亮
    setHighlight() {
      const { color, lineLayer } = this;
      lineLayer.setOptions({
        color: [color],
        width: 8,
      });
    }

    // 设置播放点
    setPosition(index) {
      this.changeStatus('start');
      this.currentIndex = index;
      this.animation();
    }

    // 设置速度
    setSpeed(speed) {
      this.speed = Math.max(speed, 0);
    }

    // 开始
    start() {
      const { currentIndex, data, playIndex } = this;
      if (data.length === 1) return;
      this.changeStatus('start');
      if (currentIndex === data.length - 1) {
        this.currentIndex = 0;
      }
      if (playIndex === 0) {
        this.animation();
      }
    }
  }

  return new _BMap(config);
}
export default _BMap;
