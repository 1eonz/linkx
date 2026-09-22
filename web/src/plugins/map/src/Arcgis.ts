import type { Position } from '../type';

import { useCreateApp } from '@/hooks';
import CursorList from '@/pages/map/cursorList.vue';
import MapPopup from '@/pages/map/mapPopup.vue';
import { delay } from '@/utils';
import { isArray, isNumber, isObject } from '@/utils/is';

import esriConfig from '@arcgis/core/config';
import { Point, Polygon, Polyline } from '@arcgis/core/geometry';
import Circle from '@arcgis/core/geometry/Circle';
import * as projection from '@arcgis/core/geometry/projection';
import SpatialReference from '@arcgis/core/geometry/SpatialReference';
import * as webMercatorUtils from '@arcgis/core/geometry/support/webMercatorUtils';
import Graphic from '@arcgis/core/Graphic';
import FeatureLayer from '@arcgis/core/layers/FeatureLayer';
import GraphicsLayer from '@arcgis/core/layers/GraphicsLayer';
import { cloneDeep } from 'lodash-es';
// eslint-disable-next-line import/no-named-default
import { default as ArcgisMap } from '@arcgis/core/Map';
import Query from '@arcgis/core/rest/support/Query';
import PictureMarkerSymbol from '@arcgis/core/symbols/PictureMarkerSymbol';
import SimpleLineSymbol from '@arcgis/core/symbols/SimpleLineSymbol';
import MapView from '@arcgis/core/views/MapView'; // import error tips
import Sketch from '@arcgis/core/widgets/Sketch'; // import error tips

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

import '@arcgis/core/assets/esri/themes/light/main.css';

/**
 * @description arcgis地图封装
 * @class _ArcgisMap
 */
class _ArcgisMap extends BaseMap {
  drawCreateEventer: any;
  drawUpdateEventer: any;
  fptElement: any;
  sketch: any;
  view: any;

  constructor(config) {
    super(config);

    Object.assign(this, {
      fptElement: null, // feature popup template content
      sketch: null,
      view: null,
    });
    this.initMap();
  }

  /**
   * 添加文字图层
   * @param {string} layerId 图层id
   * @param {{text, position, color}[]} data
   */
  addLabelLayer(config) {
    const { data, layerId } = config;
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const layer = new GraphicsLayer({ id: layerId });

    data.forEach((item) => {
      const symbol = {
        color: item.color,
        text: item.text,
        type: 'text' as const,
      };

      const graphic = new Graphic({
        geometry: new Point(item.position),
        symbol,
      });

      layer.add(graphic);
    });

    map.add(layer);
    this.layers.set(layerId, layer);
  }

  /**
   * @description 添加图层
   * @param {array} data marker点数据
   * @param {string} layerId 图层id
   * @param {string} image 点图片
   * @param {string} clusterImage 聚合点图片
   * @param {element} windowTemplate 弹窗模板
   * @param {boolean} isShow 是否显示 默认显示
   * @param {boolean} isCluster 是否聚合
   * @param {number} width
   * @param {number} height
   * @param {object} clusterSymple color
   */
  async addLayer(options) {
    await mapIsReady(this);
    options = { ...defaultLayerOptions, ...options };
    options.data = cloneDeep(options.data);
    const {
      clusterImage,
      clusterSymple,
      data,
      height = 48,
      image,
      isCluster = true,
      isShow = true,
      layerId,
      renderer,
      width = 40.5,
      windowTemplate,
    } = options;
    const { center, mapId, view } = this;
    const features: any[] = [];
    const map = thirdMaps.get(mapId);
    const sourceMap = new Map();
    let layer: any = null;

    if (!isArray(data)) {
      console.error('data must be array !');
      return;
    }

    // 空的source 创建不了图层(先创建然后删除)
    if (data.length === 0) {
      const f = {
        id: 'empty',
        position: [0, 90],
      };
      data.push(f);
      setTimeout(() => {
        layer.applyEdits({
          deleteFeatures: features,
        });
        sourceMap.delete('empty');
      }, 0);
    }

    data.forEach((item) => {
      item.position = formatLonLat(item.position) || center;
      // features 数组大小越小性能就越好
      features.push({
        attributes: {
          ...item,
          id: item.id,
          objectId: item.id,
        },
        geometry: {
          latitude: item.position[1],
          longitude: item.position[0],
          type: 'point',
        },
      });
      sourceMap.set(item.id, item);
    });

    const layerConfig: any = {
      // 自定义属性以及类型
      fields: [
        {
          alias: 'objectId',
          name: 'objectId',
          type: 'oid',
        },
        {
          alias: 'id',
          name: 'id',
          type: 'string',
        },
      ],
      id: layerId,
      maxScale: 0,
      minScale: 0,
      // 区别每个marker点的标识字段
      objectIdField: 'objectId',
      // 暴露所有字段(通过查询可以显示出来)
      outFields: ['*'],
      // 自定义marker点样式
      renderer: {
        symbol: {
          height: `${height}px`,
          outline: {
            width: 0,
          },
          type: 'picture-marker',
          url: image,
          width: `${width}px`,
        },
        type: 'simple',
      },
      source: features,
      visible: isShow,
    };

    if (renderer) {
      renderer(data, {
        layerConfig,
        mapType: 'arcgis',
      });
    }

    if (windowTemplate) {
      // marker点弹窗
      layerConfig.popupTemplate = {
        content: async (feature) => {
          const { attributes } = feature.graphic;
          const data = sourceMap.get(attributes.id);
          const el = this.renderPopup({
            data,
            layerId,
            template: windowTemplate,
          });
          const ret = await this.featurePopupTemplate(el);
          return ret;
        },
      };
    }

    layer = new FeatureLayer(layerConfig);

    if (isCluster && clusterImage) {
      const size = `${Math.max(width, height)}px`;
      const featureReduction: any = {
        clusterMaxSize: size,
        clusterMinSize: size,
        clusterRadius: '100px', // 聚合半径
        // 聚合点样式
        labelingInfo: [
          {
            allowOverrun: false,
            deconflictionStrategy: 'static',
            labelExpressionInfo: {
              expression: "IIF($feature.cluster_count > 99, '99+', $feature.cluster_count)",
            },
            labelPlacement: 'center-center',
            symbol: {
              color: clusterSymple?.color || '#fff',
              font: {
                family: 'Noto Sans',
                size: '18px',
                weight: 'bold',
              },
              type: 'text',
              xoffset: 0,
              yoffset: 5,
            },
          },
        ],
        symbol: {
          height: '48px',
          type: 'picture-marker', // autocasts as new PictureMarkerSymbol()
          url: clusterImage,
          width: '40px',
        },
        type: 'cluster',
      };
      if (windowTemplate) {
        // 聚合点弹窗 --- 需要优化 异步查询导致弹窗打开太慢
        featureReduction.popupTemplate = {
          content: async (feature) => {
            const { geometry } = feature.graphic;
            const features = await this.queryFeatures(layerId, {
              distance: 100 * view.resolution, // 聚合半径 * 分辨率
              geometry, // 查询范围为绘制图形的范围
            });
            const { latitude, longitude } = geometry;
            const data: any[] = [];
            features.forEach((item) => {
              const sourceItem = sourceMap.get(item.attributes.id);
              if (sourceItem) {
                data.push(sourceItem);
              }
            });
            const el = this.renderPopup({
              data,
              layerId,
              position: [longitude, latitude],
              template: windowTemplate,
            });
            const ret = await this.featurePopupTemplate(el);
            return ret;
          },
        };
      }

      layer.featureReduction = featureReduction;

      // 当缩放到最大层级时设置聚合距离为1 --- (有bug 设置后会导致内存溢出)
      // const setcr = debounce(() => {
      //   const isMaxZoom = this.getZoom() >= 22;
      //   layer.featureReduction.clusterRadius = isMaxZoom ? 1 : 100;
      //   layer.refresh();
      // }, 500);
      // view.watch('scale', setcr);
    }

    map.add(layer);

    // sourceMap 优化遍历
    this.layers.set(layerId, { sourceMap, ...options });
  }

  /**
   * @description 添加marker点弹窗
   * @param {string} layerId 图层id
   * @param {Object} source 对应marker点数据
   */
  async addMarkerCustomPopup(layerId, source) {
    const { center, mapId, view } = this;
    const { id } = source;
    const { sourceMap } = this.layers.get(layerId);
    let data = sourceMap.get(id);

    if (!data) {
      this.updateLayer(layerId, { addList: [source] });
      await delay(500);
      data = source;
    }

    const { windowTemplate } = this.layers.get(layerId);
    const content = this.renderPopup({
      data,
      layerId,
      mapId,
      template: windowTemplate,
    });
    const location = formatLonLat(data.position || center);
    this.setCenter(location);
    await view.popup.open({
      content,
      location,
    });
    this.formatPopupStyle(500);
  }

  /**
   * @description 添加比例尺以及鼠标移动显示鼠标经纬度
   * @param {*} scaleContainer 比例尺容器
   * @param {*} positionContainer 经纬度容器
   */
  addScaleAndPosition(scaleContainer, positionContainer) {
    const { mapId, view } = this;
    const map = thirdMaps.get(mapId);

    if (!map) return;

    const scaleRender = () => {
      const scale = view.scale / 100;
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

    view.watch('scale', scaleRender);

    view.on('mouse-wheel', () => {
      scaleRender();
    });

    view.on('pointer-move', (e) => {
      const point = view.toMap({ x: e.x, y: e.y });
      const { latitude, longitude } = point;
      positionContainer.innerHTML = `${longitude.toFixed(7)}，${latitude.toFixed(7)}`;
    });
  }

  /**
   * @description 框选
   * @param {*} type 框选类型 点marker 线polyline 多边形polygon 矩形rectangle 圆circle
   * @param {*} layerIds 图层ids
   * @param {*} callback 回调函数
   */
  boxToSelect(type, layerIds, callback) {
    this.registerSketch();
    const { mapId, sketch } = this;
    const map = thirdMaps.get(mapId);

    sketch.create(type);
    const queryFeatures = (layer, query) => {
      return new Promise((resolve) => {
        if (!layer?.visible || typeof layer.queryFeatures !== 'function') {
          resolve([]);
        } else {
          layer.queryFeatures(query).then((res) => {
            const { sourceMap } = this.layers.get(layer.id);
            const ret: any[] = [];
            const f = res.features;
            f.forEach((item) => {
              const data = sourceMap.get(item.attributes.id);
              if (data) {
                ret.push(data);
              }
            });

            resolve(ret);
          });
        }
      });
    };

    const listen = sketch.on('create', (e) => {
      if (e.state !== 'complete' || !callback) return;

      const { graphic } = e;
      // 建立查询
      const query = new Query({
        geometry: graphic.geometry, // 查询范围为绘制图形的范围
        outFields: ['*'],
        returnGeometry: true,
      });

      const retObj = {};
      const markers: any[] = [];

      const showLayerIds = layerIds.filter((item) => this.getLayerVisible(item));
      if (showLayerIds.length > 0) {
        showLayerIds.forEach(async (layerId) => {
          const theLayer = map.findLayerById(layerId);
          const res = await queryFeatures(theLayer, query);
          markers.push(res as any);
          retObj[layerId] = res;
          if (markers.length === showLayerIds.length) {
            drawCallback(callback, markers.flat(), retObj);
            listen.remove();
          }
        });
      } else {
        callback?.([]);
        listen.remove();
      }
      sketch.delete(graphic);
      sketch.layer.removeAll();
    });
  }

  // 清空地图
  clearMap() {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const layerIds = Object.keys(this.layers);

    map.layers.items.forEach((layer) => {
      const { id } = layer;
      if (this[id]) {
        this[id] = null;
      }
    });
    this.deleteLayer(layerIds);
    map.layers.removeAll();
    map.removeAll();
  }

  /**
   * @description 关闭自定义弹窗
   */
  closeCustomPopup() {
    const { view } = this;
    view.popup.close();
  }

  /**
   * @description 删除图层
   * @param {string[]} layerIds 图层ids 不传删全部
   */
  deleteLayer(layerIds) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    if (layerIds) {
      if (!isArray(layerIds)) {
        layerIds = [layerIds];
      }
    } else {
      // 删除全部图层
      map.layers.removeAll();
    }
    layerIds.forEach((layerId) => {
      const layer = map.findLayerById(layerId);
      if (layer) {
        layer.destroy();
        map.layers.remove(layer);
      }
    });
  }

  /**
   * @description 销毁地图
   */
  destroyMap() {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);

    this.clearMap();
    // destroy the map and any remaining associated resources
    map.destroy();
    mapManager.delete(mapId);
  }

  /**
   * 隐藏显示绘制的区域
   * @param {boolean} visible 是否显示draw
   */
  disableDraw(visible) {
    const { drawLayer } = this;
    if (drawLayer) {
      drawLayer.visible = visible;
    }
  }

  /**
   * 绘制区域
   * @param {polyline | rectangle | circle | polygon} type 线、矩形、圆、多边形
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
    this.registerSketch();
    const { center, change, color, handle, highlight, lineStyle, path, radius, type } = config;
    const { drawCreateEventer, drawLayer, drawUpdateEventer, mapId, sketch } = this;
    const map = thirdMaps.get(mapId);
    const sketchLayer = sketch.layer;
    let isUpdateOptions = false;
    let symbol: any = {
      color: color ? `rgba(${color}, 0.5)` : 'rgba(252, 167, 1, 0.5)',
      outline: {
        color: color ? `rgba(${color}, 1)` : 'rgba(252, 167, 1, 1)',
        width: 4,
      },
      type: 'simple-fill',
    };

    // 保证上次draw被清空且不会影响现在的操作
    drawCreateEventer?.remove();
    drawUpdateEventer?.remove();
    sketchLayer.removeAll();

    const setUpdateOptions = (graphic) => {
      // 确保只被设置一次
      if (isUpdateOptions) return;
      isUpdateOptions = true;
      switch (type) {
        case 'circle':
        case 'rectangle': {
          sketch.update(graphic, {
            // 缩放
            enableScaling: true,
            // 统一缩放操作
            preserveAspectRatio: true,
            // 不操作多个点
            toggleToolOnClick: false,
          });
          break;
        }
        default: {
          sketch.update(graphic, {
            enableScaling: false,
          });
        }
      }
    };

    const to4326 = (geometry) => {
      const geo: any = projection.project(
        geometry,
        new SpatialReference({
          wkid: 4326, // projection
        }),
      );
      return geo;
    };

    const pathChange = (geo) => {
      switch (type) {
        case 'circle': {
          // 如果将圆拖成了椭圆就保留短的半径
          const { center } = to4326(geo).extent;
          const { width } = geo.extent;
          change({
            center: [center.longitude, center.latitude],
            radius: width / 2,
          });
          break;
        }
        case 'polygon': {
          const path = to4326(geo).rings[0];
          if (path[3] && path[0].toString() === path[3].toString()) {
            path.splice(3, 1);
          }
          change(path);
          break;
        }
        case 'polyline': {
          const path = to4326(geo).paths[0];
          change(path);
          break;
        }
        case 'rectangle': {
          const path = to4326(geo).rings[0];
          path.splice(4);
          change(path);
          break;
        }
      }
    };

    if (handle === 'look' && !highlight) {
      symbol.outline.width = 1;
      symbol.color = symbol.color.replace('0.5', '0.1');
    }

    if (type === 'polyline') {
      symbol = {
        color: symbol.outline.color,
        type: 'simple-line',
        width: symbol.outline.width,
      };
      if (lineStyle === 'dashed') {
        symbol.style = 'dash';
      }
    } else if (type === 'point') {
      symbol = {
        color: symbol.outline.color,
        outline: {
          width: 0,
        },
        size: 10,
        type: 'simple-marker',
      };
    }

    if (handle === 'add') {
      sketch.create(type);
    } else {
      let geometry: any = null;
      switch (type) {
        case 'circle': {
          geometry = new Circle({
            center,
            geodesic: true,
            radius,
          });
          break;
        }
        case 'point': {
          geometry = new Point(center);
          break;
        }
        case 'polygon': {
          geometry = new Polygon({
            rings: path,
          });
          break;
        }
        case 'polyline': {
          geometry = new Polyline({
            paths: path,
          });
          break;
        }
        case 'rectangle': {
          geometry = new Polygon({
            rings: path,
          });
          break;
        }
      }

      const graphic = new Graphic({ geometry, symbol });
      if (handle === 'look') {
        if (highlight) {
          const layer = new GraphicsLayer({
            graphics: [graphic],
          });
          map.add(layer);
        } else {
          if (drawLayer) {
            drawLayer.add(graphic);
          } else {
            const layer = new GraphicsLayer({
              graphics: [graphic],
              id: 'drawLayer',
            });
            this.drawLayer = layer;
            map.add(layer);
          }
        }
      } else {
        sketchLayer.add(graphic);
      }
    }

    // 区域改变事件
    if (handle !== 'look') {
      this.drawCreateEventer = sketch.on('create', (e) => {
        const { graphic, state } = e;
        if (state === 'complete') {
          setUpdateOptions(graphic);
          graphic.symbol = symbol;
        }
      });
      this.drawUpdateEventer = sketch.on('update', (e) => {
        const { graphics, state } = e;
        if (state === 'complete') {
          setUpdateOptions(graphics[0]);
        }
        pathChange(graphics[0].geometry);
      });
      this.drawUpdateEventer = sketch.on('delete', () => {
        change();
      });
    }
  }

  /**
   * @description feture点弹窗content
   * @param {any} el
   * @returns el
   */
  featurePopupTemplate(el) {
    this.fptElement = el;
    // 每次点击的时候，需要返回层级最大的图层(最后添加的图层)所对应的弹窗
    return new Promise((resolve) => {
      setTimeout(() => {
        resolve(this.fptElement);
        this.formatPopupStyle();
      }, 100);
    });
  }

  formatPopupStyle(time?: number) {
    setTimeout(() => {
      const item = document?.querySelector('calcite-flow-item');
      const root1 = item?.shadowRoot;
      const root2 = root1?.querySelector('calcite-panel')?.shadowRoot;
      const header = root2?.querySelector('header');
      const footer = item?.querySelector('.esri-features__footer');
      if (header) {
        header.style.display = 'none';
      }
      if (footer) {
        footer?.remove();
      }
    }, time || 0);
  }

  /**
   * @description 获取地图中心点
   * @returns [lon, lat]
   */
  getCenter() {
    const { latitude, longitude } = this.view.center;
    if (!isNumber(longitude) || !isNumber(latitude)) {
      console.error('Failed to get center point');
    }
    return [longitude, latitude];
  }

  /**
   * @description 获取图层数据
   * @param {string} layerId
   * @returns {array}
   */
  getLayerData(layerId) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const layer = map.findLayerById(layerId);
    const ret: any[] = [];
    if (layer) {
      layer.source.items.forEach((item) => {
        ret.push(item.attributes);
      });
    }
    return ret;
  }

  /**
   * @description 获取图层是否显示
   * @param {string} layerId
   */
  getLayerVisible(layerId) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    let layer = map.findLayerById(layerId);

    if (layer) {
      return layer.visible;
    } else {
      let isShow = false;
      layer = this.layers.get(layerId);
      if (layer) {
        isShow = layer.isShow;
      }
      return isShow;
    }
  }

  /**
   * @description 获取当前地图缩放级别
   * @returns number
   */
  getZoom() {
    const zoom = this.view?.zoom;
    if (!isNumber) {
      console.error('Failed to get zoom');
    }
    return zoom;
  }

  /**
   * @description 初始化地图
   */
  async initMap() {
    const { center, mapId, zoom } = this;
    const { mapKey } = getMapConfig();

    // 设置key
    esriConfig.apiKey = mapKey; // 'AAPKe221ba9d481e4b669bcc7beb579601e0Je69hX4z0wkbS2y_5eQMA7AQzMhP1tQ2pu-9-oWN7Tqu7PsvlmYwlH9iFnOddCzQ';

    // 创建地图
    const map = new ArcgisMap({
      basemap: 'osm-dark-gray',
    });
    thirdMaps.set(mapId, map);

    // 创建地图视图
    const view = new MapView({
      center: center as Position, // Longitude, latitude
      container: mapId, // Div element
      map,
      popup: {
        autoCloseEnabled: false,
        highlightEnabled: false, // 高亮显示(一个发亮边框)
        // autoOpenEnabled: false
      },
      zoom, // Zoom level
    });
    this.view = view;

    // 事件监听
    view.when(() => {
      this.isReady = true;
      this.onLoad(this);
    });
  }

  /**
   * @description 查询图层的features
   * @param {*} layerId
   * @param {*} params
   * @returns promise
   */
  queryFeatures(layerId, params): Promise<Array<any>> {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const layer = map.findLayerById(layerId);
    const query = new Query({
      outFields: ['*'],
      // where: `id = ${id}`,
      returnGeometry: true,
      ...params,
    });

    return new Promise((resolve) => {
      layer.queryFeatures(query).then((res) => {
        resolve(res.features);
      });
    });
  }

  /**
   * @description 注册Sketch，绘图
   */
  registerSketch() {
    if (this.sketch) return;

    const { mapId, view } = this;
    const map = thirdMaps.get(mapId);
    const sketchLayer = new GraphicsLayer();
    const sketch = new Sketch({
      creationMode: 'single',
      defaultUpdateOptions: {
        enableRotation: false, // 是否旋转
      },
      layer: sketchLayer,
      view,
      visible: false, // 不显示
    });

    map.add(sketchLayer);
    view.ui.add(sketch, 'top-right');
    this.sketch = sketch;
    return { sketch, sketchLayer };
  }

  /**
   * @description 渲染弹窗
   * @param {*} options 图层设置参数
   */
  renderPopup(options) {
    const { mapId } = this;
    const { data, layerId, position, template } = options;
    let el = '';
    if (isArray(data)) {
      const parent = document.createElement('div');
      const instance = useCreateApp(CursorList, {
        data: data as any,
        layerId,
        mapId,
        position,
        template,
      });
      el = instance.mount(parent).$el;
    } else if (isObject(data)) {
      const parent = document.createElement('div');
      const instance = useCreateApp(MapPopup, {
        data: data as any,
        layerId,
        mapId,
        template,
      });
      el = instance.mount(parent).$el;
    }
    if (!el) {
      console.error('popup is not render');
    }
    return el;
  }

  /**
   * @description 设置地图中心点与缩放级别
   * @param {lnglat} center
   * @param {number} zoom
   */
  setCenter(center, zoom?: number) {
    const { view } = this;
    if (!isNumber(zoom)) {
      zoom = this.zooms[1];
    }
    const position = formatLonLat(center);
    if (!position) {
      console.error('position is null');
      return;
    }
    view.goTo({ center: position, zoom }).then(() => {});
  }

  /**
   * @description 设置图层是否显示
   * @param {string[]} layerIds
   * @param {boolen} visible
   */
  setLayersVisible(layerIds, visible) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    layerIds.forEach((layerId) => {
      let layer = map.findLayerById(layerId);

      if (layer) {
        layer.visible = visible;
      } else {
        layer = this.layers.get(layerId);
        if (layer) {
          layer.isShow = visible;
        }
      }
    });
  }

  /**
   * @description 返回动向实例
   * @param {*} config
   * @returns _Track
   */
  track(config) {
    return new _Track({ map: this, ...config });
  }

  /**
   * @description 更新图层
   * @param {string} layerId 图层id
   * @param {addList, deleteList, updateList} options
   */
  updateLayer(layerId, options) {
    // 浏览器页面可见性 不可见时不刷新
    if (document.visibilityState === 'hidden') return;

    const { center, mapId } = this;
    const map = thirdMaps.get(mapId);
    const layer = map.findLayerById(layerId);
    const { addList = [], deleteList = [], updateList = [] } = options;

    if (!layer) return;
    layer.queryFeatures().then(async (res) => {
      const { sourceMap } = this.layers.get(layerId);
      const featureMap = new Map();
      const addFeatures: any[] = [];
      // const updateFeatures: any[] = [];
      const deleteFeatures: any[] = [];

      res.features.forEach((item) => {
        featureMap.set(item.attributes.id, item);
      });

      // 更新
      if (updateList?.length > 0) {
        updateList.forEach((item) => {
          const f = featureMap.get(item.id);
          const { latitude, longitude } = f?.geometry || {};
          if (item.position[0] === longitude && item.position[1] === latitude) {
            sourceMap.set(item.id, item);
          } else if (f) {
            deleteList.push(item);
            addList.push(item);
          }
        });
      }
      // 删除
      if (deleteList?.length > 0) {
        deleteList.forEach((item) => {
          const f = featureMap.get(item.id);
          if (f) {
            deleteFeatures.push(f);
            sourceMap.delete(item.id);
          }
        });

        await layer.applyEdits({
          deleteFeatures,
        });
        layer.refresh();
      }
      // 新增
      if (addList?.length > 0) {
        addList.forEach((item) => {
          const { id, name, statusName } = item;
          const position = formatLonLat(item.position) || center;
          const f = new Graphic({
            attributes: {
              id,
              name,
              objectId: id,
              statusName,
            },
            geometry: new Point(position as any),
          });
          addFeatures.push(f);
          sourceMap.set(item.id, { ...item, position });
        });

        await layer.applyEdits({
          addFeatures,
        });
        layer.refresh();
      }

      // 更新 -> 有bug 更新后图层显示不正常，先删除后增加代替
      // [...updateList].forEach((item) => {
      //   const { id, name, statusName } = item;
      //   const position = formatLonLat(item.position) || center;
      //   const f = new Graphic({
      //     geometry: new Point(position as any),
      //     attributes: {
      //       id,
      //       objectId: id,
      //       statusName,
      //       name,
      //     },
      //   });
      //   updateFeatures.push(f);
      //   sourceMap.set(item.id, { ...item, position });
      // });
    });
  }
}

/**
 * @description Track 动向
 * @class Track
 */
class _Track extends BaseTrack {
  carLayer: any;
  currentIndex: number; // 当前运动到位置下标
  lineLayer: any;
  playIndex: number;
  playTimer?: Timeout;
  polylineGraphic: any;

  constructor(config) {
    super(config);
    this.currentIndex = 0;
    this.playIndex = 0;

    Object.assign(this, config, {
      carLayer: null, // 小车图层
      lineLayer: null, // 起止点、动向线图层
      playTimer: null,
    });
    this.initTrack();
  }
  // 墨卡托转经纬度
  changePosPoint(pt) {
    const lnglat = webMercatorUtils.xyToLngLat(pt.x, pt.y);
    return { lat: lnglat[1], lng: lnglat[0] };
  }
  // 经纬度转墨卡托
  changePosXY(pt) {
    const lnglat = webMercatorUtils.lngLatToXY(pt.lng, pt.lat);
    return { x: lnglat[0], y: lnglat[1] };
  }
  changeStatus(status) {
    this.status = status;
    this.statusChange?.(status);
  }
  // 销毁动向
  destroyTrack() {
    const { carLayer, lineLayer } = this;

    this.changeStatus('destroy');
    carLayer.removeAll();
    lineLayer.removeAll();
  }
  // 画小车
  drowCar(lnglat, angle?) {
    const { carLayer, image } = this;
    const mapObj = thirdMaps.get(this.map.mapId);

    if (carLayer) {
      carLayer.removeAll();
    } else {
      const layer = new GraphicsLayer();
      this.carLayer = layer;
      mapObj.add(layer);
    }

    const carSymbol = new PictureMarkerSymbol({
      height: '32px',
      url: image,
      width: '32px',
    });

    if (angle !== undefined) {
      carSymbol.angle = angle;
    }

    const carMarker = new Graphic({
      geometry: new Point({
        x: lnglat.lng,
        y: lnglat.lat,
      }),
      symbol: carSymbol,
    });

    this.carLayer.add(carMarker);
  }
  // 画动向线以及起止点
  drowPolyline() {
    const { color, data, endImage, startImage } = this;
    const mapObj = thirdMaps.get(this.map.mapId);
    const layer = new GraphicsLayer();
    this.lineLayer = layer;
    mapObj.add(layer);

    // 画动向线
    const paths = data.map((item) => {
      return [item.lng, item.lat];
    });
    const polylineGraphic = new Graphic({
      geometry: new Polyline({ paths }),
      symbol: new SimpleLineSymbol({
        color,
        width: 2,
      }),
    });
    this.polylineGraphic = polylineGraphic;
    layer.add(polylineGraphic);
    // 居中展示
    const { extent } = polylineGraphic.geometry as Polyline;
    this.map.view.goTo({ extent });
    // 画起止点
    const start = data[0];
    const startMarker = new Graphic({
      geometry: new Point({
        x: start.lng,
        y: start.lat,
      }),
      symbol: new PictureMarkerSymbol({
        height: '32px',
        url: startImage,
        width: '32px',
      }),
    });
    layer.add(startMarker);

    const end = data[data.length - 1];
    const endMarker = new Graphic({
      geometry: new Point({
        x: end.lng,
        y: end.lat,
      }),
      symbol: new PictureMarkerSymbol({
        height: '32px',
        url: endImage,
        width: '32px',
      }),
    });
    layer.add(endMarker);
  }
  // 计算图片的角度
  getAngle(p1, p2) {
    const p1x = p1.x;
    const p1y = p1.y;
    const p2x = p2.x;
    const p2y = p2.y;
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
  // 初始化动向
  initTrack() {
    const { data } = this;
    this.data = data.map((item) => {
      return {
        lat: item[1],
        lng: item[0],
      };
    });
    this.drowPolyline();
    this.drowCar(this.data[0]);
  }
  // 根据回放速度在两点之间进行插值
  interpolation(pointA, pointB) {
    const tmp: any[] = [];
    let count = Math.abs(this.speed);
    const x1 = Math.abs(pointB.x - pointA.x);
    const y1 = Math.abs(pointB.y - pointA.y);
    const z1 = Math.hypot(x1, y1);

    count = z1 / (count * 10);

    const disX = (pointB.x - pointA.x) / count;
    const disY = (pointB.y - pointA.y) / count;
    let i = 0;
    while (i <= count) {
      const x = pointA.x + i * disX;
      const y = pointA.y + i * disY;
      tmp.push({ x, y });
      i++;
    }
    tmp.push(pointB); // 防止插值出来的最后一个点到不了B点
    return tmp;
  }
  // 暂停
  pause() {
    if (this.data.length === 1) return;
    this.changeStatus('pause');
  }
  // 播放
  play(tmpPoints, angle) {
    const { playIndex, playTimer, speed, status } = this;
    if (playTimer) clearTimeout(playTimer);
    this.playTimer = setTimeout(
      () => {
        if (status === 'destroy') {
          return;
        }
        if (status === 'pause') {
          this.play(tmpPoints, angle);
          return;
        }
        if (playIndex < tmpPoints.length - 1) {
          const lnglat = this.changePosPoint(tmpPoints[++this.playIndex]);
          this.drowCar(lnglat, angle);
          this.play(tmpPoints, angle);
        } else {
          this.playIndex = 0;
          clearTimeout(playTimer);
          this.run();
        }
      },
      (100 / speed) * 10,
    );
  }
  // 恢复显示
  recover() {
    const { color } = this;
    this.polylineGraphic.symbol = new SimpleLineSymbol({
      color,
      width: 2,
    });
  }
  // 从新开始
  reStart() {
    if (this.data.length === 1) return;
    this.setPosition(0);
  }
  // 小车运动
  run() {
    const { currentIndex, data, move } = this;
    if (data.length === 0) {
      return;
    }
    if (currentIndex === data.length - 1) {
      this.changeStatus('pause');
      return;
    }
    const p1 = this.changePosXY(data[currentIndex]);
    const p2 = this.changePosXY(data[++this.currentIndex]);
    const tempPoints = this.interpolation(p1, p2);
    const angle = Math.ceil(this.getAngle(p1, p2));
    this.playIndex = 0;
    this.play(tempPoints, angle);
    if (move) {
      move(this.currentIndex);
    }
  }
  // 设置高亮
  setHighlight() {
    const { color } = this;
    this.polylineGraphic.symbol = new SimpleLineSymbol({
      color,
      width: 6,
    });
  }
  // 设置播放点
  setPosition(index) {
    this.changeStatus('start');
    this.currentIndex = index;
    this.run();
  }
  // 设置速度
  setSpeed(speed) {
    this.speed = Math.max(speed, 0);
  }
  // 开始
  start() {
    if (this.data.length === 1) return;
    this.changeStatus('start');
    if (this.currentIndex === this.data.length - 1) {
      this.currentIndex = 0;
    }
    if (this.playIndex === 0) {
      this.run();
    }
  }
}

export default _ArcgisMap;
