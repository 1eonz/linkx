import type { Position } from '../type';

import { selectInversecode, selectPoi } from '@/api/lbsLocation';
import vehicleBgImg from '@/assets/images/marker/vehicle_marker_bg.png';
import { appConfig } from '@/config';
import { useCreateApp, useSetInterval } from '@/hooks';
import CursorList from '@/pages/map/cursorList.vue';
import MapPopup from '@/pages/map/mapPopup.vue';
import KindMarker from '@/pages/map/marker/kindMarkerOpenlayer.vue';
import RangeMarker from '@/pages/map/marker/rangeMarker.vue';
import { useResourceStore } from '@/store';
import {
  delay,
  domConvertsToBase64,
  getLocationOrigin,
  guid,
  hexToRgb,
  imageToBase64,
} from '@/utils';
import { isArray, isDef, isNumber } from '@/utils/is';

import * as Turf from '@turf/turf';
import { cloneDeep, debounce } from 'lodash-es';
import Collection from 'ol/Collection';
import { ScaleLine } from 'ol/control';
import MousePosition from 'ol/control/MousePosition';
import { createStringXY } from 'ol/coordinate';
import { boundingExtent, getTopLeft, getWidth } from 'ol/extent';
import Feature from 'ol/Feature';
import { MVT, WMTSCapabilities } from 'ol/format';
import { Circle, Geometry, LineString, MultiLineString, Point, Polygon } from 'ol/geom';
import { fromExtent } from 'ol/geom/Polygon';
import { Modify, Translate } from 'ol/interaction';
import Draw, { createBox } from 'ol/interaction/Draw';
import { Tile as TileLayer, Vector as VectorLayer, VectorTile as VectorTileLayer } from 'ol/layer';
import OlMap from 'ol/Map';
import { unByKey } from 'ol/Observable';
import Overlay from 'ol/Overlay';
import * as olProj from 'ol/proj';
import { register } from 'ol/proj/proj4';
import {
  Cluster,
  TileWMS,
  Vector as VectorSource,
  VectorTile as VectorTileSource,
  XYZ as XYZSource,
} from 'ol/source';
import WMTS, { optionsFromCapabilities } from 'ol/source/WMTS';
import { getArea } from 'ol/sphere';
import { Circle as CircleStyle, Fill, Icon, Style, Text } from 'ol/style';
import Stroke from 'ol/style/Stroke';
import * as olTilegrid from 'ol/tilegrid';
import WMTSTileGrid from 'ol/tilegrid/WMTS.js';
import View from 'ol/View';
import proj4 from 'proj4';

import { mapManager } from '../EMap';
import {
  BaseMap,
  BaseTrack,
  drawCallback,
  formatLonLat as formatLonLatHelper,
  getMapConfig,
  thirdMaps,
} from '../helper';
import { createMapboxVectorStyle } from '../openlayers/mapBoxVector';
import { coordinateTransform } from '../transformLonlat';

// 经纬度转换
function transform(coordinate) {
  if (getMapConfig().projection === 'BD09') {
    return olProj.fromLonLat(coordinate, 'BD09');
  }
  return olProj.fromLonLat(coordinate);
}

// 格式化经纬度以及坐标系转换
function formatLonLat(coordinate) {
  if (!coordinate) {
    return coordinate;
  }
  return transform(coordinateTransform(coordinate));
}

// 投影系转换
function to4326(coordinate) {
  return coordinateTransform(olProj.transform(coordinate, 'EPSG:3857', 'EPSG:4326'), true);
}

const getColor = (c, o) => {
  if (c?.includes('#')) {
    const cv = hexToRgb(c, true).join(',');
    return cv === '0,0,0' ? 'rgba(0,0,0,0)' : `rgba(${cv},${o})`;
  }
  return `rgba(${c || '252, 167, 1'},${o})`;
};

// 注册 EPSG:4490 投影
function register4490() {
  proj4.defs('EPSG:4490', '+proj=longlat +ellps=GRS80 +no_defs +type=crs');
  register(proj4);
  const projection = new olProj.Projection({
    axisOrientation: 'neu',
    code: 'EPSG:4490',
    units: 'degrees',
  });
  projection.setExtent([-180, -90, 180, 90]);
  projection.setWorldExtent([-180, -90, 180, 90]);
  olProj.addProjection(projection);
}

// 注册BD09坐标系
function registerBD09() {
  proj4.defs('BD09', '+proj=merc +lon_0=0 +units=m +ellps=clrk66 +no_defs');
  register(proj4);
}

class _OpenLayers extends BaseMap {
  addressMarkerClick: any;
  boxToSelectDraw: any;
  boxToSelectLayer: any;
  defenseMarkers: any = new Map();
  dragInteraction: any;
  drawLayer: any[] = [];
  drawModify: any;
  drawStepList: any[] = [];
  editDrawer: any;
  markerClick: any;
  markerSource: any;
  offTextClick: any;
  rangeTool: any;
  trafficClearTimer: any;
  vehicleMarkers: any = [];

  constructor(config) {
    super(config);
    this.initMap();
  }

  /**
   * 添加点位
   * @param {string} type 操作类型 add | edit | look
   * @param {*} image 图片url
   * @param {{position, callback}[]} config
   * @param {boolean} showPopup 是否显示弹窗
   */
  addAddressMarker(type, image, config, showPopup = true) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const { callback, onlyRemove, position } = config;
    let draggable = type !== 'look';
    if (isDef(config.draggable)) {
      draggable = config.draggable;
    }

    this.deleteMarker([type]);

    this.offAddressMarkerClick();

    if (onlyRemove) {
      return;
    }

    const change = async (lngLat) => {
      const _lngLat = to4326(lngLat);
      const address = await this.queryAddressByLocation(_lngLat);
      this.upDataAddressMarker(type, address, image);
      callback?.(_lngLat.join(','), address || '');
    };

    const addMarker = (lngLat) => {
      const feature = new Feature({
        geometry: new Point(lngLat),
      });

      // 创建图片样式
      const iconStyle = new Style({
        image: new Icon({
          anchor: [0.5, 1],
          src: image, // 替换为你的图片 URL
        }),
      });

      // 设置特征样式
      feature.setStyle(iconStyle);

      this.markerSource.addFeature(feature);
      this.markers.set(type, { data: { position: lngLat, showPopup }, marker: feature });

      if (draggable) {
        // 添加拖拽交互
        const dragInteraction = new Translate({
          features: new Collection([feature]),
        });
        dragInteraction.on('translateend', (e) => {
          change(e.coordinate);
        });
        this.dragInteraction = dragInteraction;
        map.addInteraction(this.dragInteraction);
      }

      change(lngLat);
    };

    if (position) {
      addMarker(formatLonLat(position));
    } else {
      const click = (e) => {
        const coordinate = e.coordinate;
        addMarker(coordinate);
        map.un('singleclick', click);
      };
      map.on('singleclick', click);
      this.addressMarkerClick = click;
    }
  }

  addCircleControlMarker(type, image, config) {
    this.addAddressMarker(type, image, config, false);
  }

  /**
   * 添加三道防线/自定义图层点位
   * @param {*} url 图片
   * @param {*} position 定位
   */
  addDefenseMarker(url, data, windowTemplate, type) {
    const map = thirdMaps.get(this.mapId);
    const layerId = type === 1 ? 'defense' : 'custom';
    const el = document.createElement('div');
    el.style['background-image'] = `url(${url})`;
    el.style['background-size'] = 'cover';
    el.style['background-repeat'] = 'no-repeat';
    el.style.width = '32px';
    el.style.height = '32px';

    const position = formatLonLat([data.lon, data.lat]);

    el.addEventListener('click', (e) => {
      e.stopPropagation();
      e.preventDefault();
      this.closeCustomPopup();
      setTimeout(() => {
        const createEl = (data) => {
          const parent = document.createElement('div');
          const instance = useCreateApp(MapPopup, {
            data,
            layerId,
            mapId: this.mapId,
            template: windowTemplate,
          });
          return instance.mount(parent).$el;
        };
        this.markerPopup(createEl(data), position, [-4, 0]);
      }, 300);
    });

    const overlay = new Overlay({
      element: el,
      id: data.id,
      position,
      positioning: 'center-center',
    });
    map.addOverlay(overlay);
    overlay.setPosition(position);

    this.defenseMarkers.set(layerId + data.id, overlay);
  }

  addImageIcon() {
    console.log('Unrealized function');
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
    const features: Feature[] = [];

    data.forEach((item) => {
      const style = new Style({
        text: new Text({
          fill: new Fill({ color: item.color }),
          text: item.text,
        }),
      });
      const feature = new Feature({
        geometry: new Point(formatLonLat(item.position)),
      });
      feature.setStyle(style);
      features.push(feature);
    });

    const layer = new VectorLayer({
      properties: {
        layerId,
      },
      source: new VectorSource({
        features,
      }),
    });

    map.addLayer(layer);
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
   */
  addLayer(options) {
    options.data = cloneDeep(options.data);
    const {
      clusterImage,
      clusterSymple,
      data,
      image,
      isCluster = true,
      isShow = true,
      layerId,
      renderCluster,
      renderer,
    } = options;
    const { center, mapId } = this;
    const features: Feature[] = [];
    const sourceMap = new Map();
    const map = thirdMaps.get(mapId);

    data.forEach((item) => {
      const position = formatLonLat(item.position || center);
      const feature = new Feature({
        geometry: new Point(position),
        layerId,
        type: 'icon',
      });

      feature.setId(item.id);
      features.push(feature);
      // 所有的position都应该保存原始的而不是转换后的数据
      sourceMap.set(item.id, item);
    });

    const source = new VectorSource({ features });
    const clusterSource = new Cluster({
      distance: 100,
      source,
    });

    // 当缩放到最大层级时设置聚合距离为1
    map.on('moveend', () => {
      const isMaxZoom = this.getZoom() >= this.zooms[1];
      clusterSource.setDistance(isMaxZoom ? 10 : 100);
    });

    const layer = new VectorLayer({
      properties: {
        layerId,
      },
      source: clusterSource,
      style: (feature) => {
        const features = feature.get('features');
        const size = features?.length || 0;
        const text = size > 99 ? '99+' : size.toString();

        let config: any = {
          image: new Icon({
            scale: 1,
            src: image,
          }),
        };
        if (isCluster && size > 1) {
          if (renderCluster) {
            config.image = new Icon({
              scale: 1,
              src: renderCluster(text),
            });
          } else {
            config.image = new Icon({
              scale: 1,
              src: clusterImage,
            });
            config.text = new Text({
              fill: new Fill({
                color: clusterSymple?.color || '#fff',
              }),
              font: 'normal 12px Arial',
              offsetX: 0,
              offsetY: -3,
              text,
              textAlign: 'center',
            });
          }
        } else if (renderer) {
          const data = sourceMap.get(features[0].getId());
          config = {
            image: new Icon({
              scale: 1,
              src: renderer(data, { mapType: 'openLayer' }),
            }),
          };
        }
        const style = new Style(config);
        return style;
      },
      visible: isShow,
      zIndex: 1,
    });

    map.addLayer(layer);
    this.layers.set(layerId, {
      source,
      sourceMap,
      ...options,
    });
  }

  /**
   * 添加线图层
   * @param config
   */
  addLineLayer(config) {
    const { layerId, path, style } = config;
    const map = thirdMaps.get(this.mapId);
    const lineCoordinates = path.map((i) => formatLonLat(i));
    // 创建线条几何对象
    const lineString = new LineString(lineCoordinates);

    // 创建特征
    const lineFeature = new Feature({
      geometry: lineString,
    });

    // 创建矢量图层并添加特征
    const vectorSource = new VectorSource({
      features: [lineFeature],
    });

    const vectorLayer = new VectorLayer({
      properties: {
        layerId,
      },
      source: vectorSource,
      style: new Style({
        stroke: new Stroke({
          color: style.fillOutlineColor,
          width: style.fillOutlineWidth,
        }),
      }),
    });

    // 将矢量图层添加到地图
    map.addLayer(vectorLayer);
  }

  /**
   * 添加marker点
   * @param {String} url 图片
   * @param {Object} data 点数据
   * @param {Object} config
   */
  addMarkerAndPopup(url, data, config) {
    if (this.markers.has(data.id)) {
      return;
    }

    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const position = formatLonLat(config.position);
    data.position = position;

    // 创建标记
    const marker = new Feature({
      geometry: new Point(position),
    });
    marker.setProperties({ addMarker: true, data });

    // 创建图片样式
    const iconStyle = new Style({
      image: new Icon({
        anchor: [0.5, 1],
        src: url, // 替换为你的图片 URL
      }),
    });

    // 设置特征样式
    marker.setStyle(iconStyle);

    this.markerSource.addFeature(marker);

    const addPopup = (data) => {
      const { layerId, windowTemplate } = config;
      const parent = document.createElement('div');
      const instance = useCreateApp(MapPopup, {
        data,
        layerId,
        mapId,
        template: windowTemplate,
      });
      const el = instance.mount(parent).$el;
      const popup = map.getOverlayById('popup');
      popup.setOffset([-6, -22]);
      popup.setPositioning('bottom-center');
      popup.setElement(el);
      popup.setPosition(data.position);
    };

    if (!this.markerClick) {
      // 添加点击事件
      const click = (e) => {
        // 获取特征
        map.forEachFeatureAtPixel(e.pixel, (feature) => {
          const { addMarker, data } = feature.getProperties();
          if (addMarker) {
            addPopup(data);
            config.callback?.();
          }
        });
      };
      map.on('singleclick', click);
      this.markerClick = click;
    }

    this.markers.set(data.id, { data, marker });
  }

  /**
   * @description 添加marker点弹窗
   * @param {String} layerId 图层id
   * @param {Object} source 对应marker点数据
   */
  async addMarkerCustomPopup(layerId, source) {
    const { center, mapId } = this;
    const { id } = source;
    const map = thirdMaps.get(mapId);
    const { sourceMap, windowTemplate } = this.layers.get(layerId);
    let data = sourceMap.get(id);

    if (!data) {
      this.updateLayer(layerId, {
        addList: [source],
        updateList: [...sourceMap.values()],
      });
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
    const element = instance.mount(parent).$el;
    const popup = map.getOverlayById('popup');

    popup.setOffset([-6, -4]);
    popup.setPositioning('bottom-center');
    popup.setElement(element);
    const position = data.position || center;
    popup.setPosition(formatLonLat(position));
    this.setCenter(position);
  }

  addPlanMarker(url, position) {
    const map = thirdMaps.get(this.mapId);
    const startMarker = new Feature({
      geometry: new Point(formatLonLat(position)),
      type: 'start',
    });
    // 删除残留点位图层
    this.removeLayer('planMarker');

    const styles = {
      start: {
        image: new Icon({
          src: url,
        }),
      },
    };
    const vectorLayer = new VectorLayer({
      properties: {
        layerId: 'planMarker',
      },
      source: new VectorSource({
        features: [startMarker],
      }),
      style: (feature) => {
        return new Style(styles[feature.get('type')]);
      },
    });
    vectorLayer.setZIndex(1);
    map.addLayer(vectorLayer);
  }

  /**
   * 测量工具
   * @param {*} type 框选类型 测距0 测面1
   */
  async addRangeTool(rangeType) {
    const map = thirdMaps.get(this.mapId);
    const type = rangeType === 1 ? 'Polygon' : 'LineString';
    let listener;

    const clear = () => {
      if (!this.rangeTool) {
        this.rangeTool = {};
        return;
      }
      const { measureTooltip, sketch, source } = this.rangeTool;
      source.removeFeature(sketch);
      this.rangeTool = { source };
      map.removeOverlay(measureTooltip);
    };
    clear();

    const formatLength = (line) => {
      const length = line.getLength();
      return Math.round((length / 1000) * 100) / 100;
    };

    const formatArea = (polygon) => {
      const area = getArea(polygon);
      return Math.round((area / 1_000_000) * 100) / 100;
    };

    const createMeasureTooltip = () => {
      const el = document.createElement('div');
      el.className = 'ol-tooltip ol-tooltip-measure';
      const tip = new Overlay({
        element: el,
        insertFirst: false,
        offset: [0, -15],
        positioning: 'bottom-center',
        stopEvent: false,
      });
      this.rangeTool.measureTooltipElement = el;
      this.rangeTool.measureTooltip = tip;
      map.addOverlay(tip);
    };
    createMeasureTooltip();

    const style = new Style({
      fill: new Fill({
        color: 'rgba(255, 255, 255, 0.2)',
      }),
      stroke: new Stroke({
        color: '#ffcc33',
        width: 2,
      }),
    });
    if (!this.rangeTool?.source) {
      const source = new VectorSource();
      const vector = new VectorLayer({
        source,
        style,
      });
      this.rangeTool.source = source;
      map.addLayer(vector);
    }

    const draw = new Draw({
      source: this.rangeTool.source,
      style: () => style,
      type,
    });
    map.addInteraction(draw);

    draw.on('drawstart', (e: any) => {
      const sketch = e.feature;
      this.rangeTool.sketch = sketch;

      let tooltipCoord = e.coordinate;
      listener = sketch.getGeometry().on('change', (e) => {
        const geom = e.target;
        let output;
        if (geom instanceof Polygon) {
          output = formatArea(geom);
          tooltipCoord = geom.getInteriorPoint().getCoordinates();
        } else if (geom instanceof LineString) {
          output = formatLength(geom);
          tooltipCoord = geom.getLastCoordinate();
        }
        const instance = useCreateApp(RangeMarker, {
          distance: output,
          onClear: clear,
          type: rangeType,
        });
        const parent = document.createElement('div');
        const el = instance.mount(parent).$el;

        this.rangeTool.measureTooltipElement.innerHTML = '';
        this.rangeTool.measureTooltipElement.append(el);
        this.rangeTool.measureTooltip.setPosition(tooltipCoord);
      });
    });

    draw.on('drawend', () => {
      unByKey(listener);
      map.removeInteraction(draw);
    });
  }

  /**
   * @description 添加比例尺以及鼠标移动显示鼠标经纬度
   * @param {*} scaleContainer 比例尺容器
   * @param {*} positionContainer 经纬度容器
   */
  addScaleAndPosition(scaleContainer, positionContainer) {
    const map = thirdMaps.get(this.mapId);
    const scaleControl = new ScaleLine({
      units: 'metric',
      // className: 'custom-scale'
      // target: scaleContainer
    });
    map.addControl(scaleControl);

    map.on('moveend', () => {
      setTimeout(() => {
        let scale: number | string = scaleControl.getScaleForResolution() / 100;
        scale = scale >= 1000 ? `${(scale / 1000).toFixed(0)}km` : `${scale.toFixed(0)}m`;
        scaleContainer.innerHTML = scale;
      }, 0);
    });

    const mousePositionControl = new MousePosition({
      className: 'custom-mouse-position',
      coordinateFormat: createStringXY(4),
      projection: 'EPSG:4326',
      target: positionContainer,
    });
    map.addControl(mousePositionControl);
  }

  /**
   * @description 添加路况图层
   */
  async addTrafficLayer() {
    this.trafficClearTimer?.();

    const { trafficTime, trafficUrl } = getMapConfig();
    const map = thirdMaps.get(this.mapId);
    const time = Number(trafficTime || 1) * 60 * 1000;

    const getSource = () => {
      const url =
        'https://service.minedata.cn/service/data/dynamic-traffic/ertic?servicetype=0&z={z}&x={x}&y={y}&key=356850718dfb464b876136fc9dbc12ed&solu=11003';
      return new VectorTileSource({
        format: new MVT({ featureClass: Feature as any }),
        tileGrid: olTilegrid.createXYZ(),
        url: trafficUrl || url,
      });
    };

    const layer = new VectorTileLayer({
      properties: {
        layerId: 'Traffic',
      },
      source: getSource(),
      style: (feature) => {
        const status = feature.get('status'); // 获取属性值
        let color;
        if (status <= 0) {
          color = '#999999';
        } else if (status <= 1) {
          color = '#66cc00';
        } else if (status <= 2) {
          color = '#ff9900';
        } else if (status <= 3) {
          color = '#cc0000';
        } else {
          color = '#9d0404';
        }
        return new Style({
          stroke: new Stroke({
            color,
            lineCap: 'round',
            lineJoin: 'round',
            width: 2,
          }),
        });
      },
    });
    layer.setZIndex(1);
    map.addLayer(layer);

    // 定时更新
    this.trafficClearTimer = useSetInterval(() => {
      layer.setSource(getSource());
    }, time);
  }

  /**
   * 添加车辆幻化图标
   * @param data
   */
  addVehicleMarker(data) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);

    const { direction = 0.25, id } = data;
    const position = formatLonLat(data.position);
    const index = this.vehicleMarkers.findIndex((i: any) => i.id === id);

    if (index === -1) {
      const el = document.createElement('div');
      el.style['background-image'] = `url(${vehicleBgImg})`;
      el.style['background-size'] = 'contain';
      el.style['background-repeat'] = 'no-repeat';
      el.style.display = 'flex';
      el.style['align-items'] = 'center';
      el.style['justify-content'] = 'center';
      el.style.width = '120px';
      el.style.height = '120px';
      el.style.transform = `rotate(${direction * 360}deg)`;

      const inner = document.createElement('div');
      inner.style['background-image'] = `url(${data.iconUrl})`;
      inner.style['background-size'] = 'contain';
      inner.style['background-repeat'] = 'no-repeat';
      inner.style['background-position'] = 'center';
      inner.style.width = '46px';
      inner.style.height = '88px';
      el.append(inner);

      const overlay = new Overlay({
        element: el,
        id,
        position, // 初始位置
        positioning: 'center-center',
      });
      map.addOverlay(overlay);
      overlay.setPosition(position); // 将 DOM 元素移动到地图上的指定位置

      this.vehicleMarkers.push({ ...data, overlay });

      el.addEventListener('click', (e) => {
        e.stopPropagation();
        e.preventDefault();
        this.closeCustomPopup();
        setTimeout(() => {
          const index = this.vehicleMarkers.findIndex((i) => i.id === id);
          this.addMarkerCustomPopup(data.layerId, this.vehicleMarkers[index] || data);
        }, 300);
      });
    } else {
      const target = this.vehicleMarkers[index];
      const { iconUrl, overlay } = target;

      const el = overlay.getElement();
      if (iconUrl !== data.iconUrl) {
        el.childNodes[0].style['background-image'] = `url(${data.iconUrl})`;
      }
      el.style.transform = `rotate(${direction * 360}deg)`;

      overlay.setPosition(position);
      Object.assign(target, data);
    }
  }

  /**
   * @description 框选
   * @param {*} type 框选类型 点point 线polyline 多边形polygon 矩形rectangle 圆circle
   * @param {*} layerIds 图层ids
   * @param {*} callback 回调函数
   */
  boxToSelect(type, layerIds, callback) {
    // 销毁上一次的圈选
    this.closeBoxToSelect();
    const typeFormat = {
      circle: 'Circle',
      point: 'Point',
      polygon: 'Polygon',
      polyline: 'LineString',
      rectangle: 'Circle',
    };
    const map = thirdMaps.get(this.mapId);
    const source = new VectorSource({ wrapX: false });
    const vector = new VectorLayer({ source });
    let geometryFunction: any = null;
    if (type === 'rectangle') {
      geometryFunction = createBox();
    }
    const stroke: { color: string; lineDash?: number[]; width: number } = {
      color: 'rgba(25.5, 255, 251.175, 1)',
      width: 4,
    };
    stroke.lineDash = [6, 8, 6, 8];
    const draw = new Draw({
      geometryFunction,
      source,
      style: new Style({
        stroke: new Stroke(stroke),
        // fill: new Fill({
        //   color: 'rgba(21, 154, 255, 0.3)',
        // }),
      }),
      type: typeFormat[type],
    });
    // 开始绘制，清除已有要素
    draw.on('drawstart', () => {});

    // 结束绘制
    draw.on('drawend', (e) => {
      const retObj = {};
      const features: any[] = [];

      if (e.feature && layerIds.length > 0) {
        // 获取多边形的外接矩形范围
        const polygon: Geometry | undefined = e.feature.getGeometry();
        const extent = polygon?.getExtent();
        const layers = map.getLayers().getArray();

        const showLayerIds = layerIds.filter((item) => this.getLayerVisible(item));
        layers.forEach((layer) => {
          const { layerId } = layer.getProperties();
          if (!layerId) {
            return;
          }

          if (showLayerIds.length > 0 && !showLayerIds.includes(layerId)) {
            return;
          }

          if (!layer.getVisible()) {
            return;
          }

          retObj[layerId] = [];

          const source = layer.getSource();
          // 查询范围内的所有点
          source.forEachFeatureIntersectingExtent(extent, (feature) => {
            const { sourceMap } = this.layers.get(layerId);
            const allFeatures = feature.get('features');
            allFeatures.forEach((item) => {
              const data = sourceMap.get(item.getId());
              features.push(data);
              retObj[layerId].push(data);
            });

            // 判断该点是否在内部
            // const coordinates = feature.getGeometry().getCoordinates();
            // if (polygon.intersectsCoordinate(coordinates)) {
            //   console.log(coordinates);
            // }
          });
        });
      }
      drawCallback(callback, features, retObj);
      map.removeLayer(vector);
      map.removeInteraction(draw);
    });

    // 右键取消框选
    const mousedown = (e) => {
      if (e.button === 2) {
        e.preventDefault();
        map.removeLayer(vector);
        map.removeInteraction(draw);
        window.removeEventListener('mousedown', mousedown);
      }
    };
    window.addEventListener('mousedown', mousedown);

    this.boxToSelectLayer = vector;
    this.boxToSelectDraw = draw;
    map.addLayer(vector);
    map.addInteraction(draw);
  }

  // 清空地图
  clearMap() {
    const { drawLayer, mapId } = this;
    const map = thirdMaps.get(mapId);
    const layers = map.getAllLayers();

    layers.forEach((item) => {
      const id = item.get('id');
      if (id === 'TileLayer') return;
      if (id && this[id]) {
        map.removeLayer(item);
        this[id] = null;
      }
    });

    drawLayer.forEach((i) => {
      if (i.get('layerId')) {
        i.getSource()
          .getFeatures()
          .forEach((f) => {
            i.getSource().removeFeature(f);
          });
      }
    });
  }

  // 克隆Feature
  cloneFeature(originalFeature) {
    const clonedFeature = {
      featureId: originalFeature.getId(),
      geometry: originalFeature.getGeometry().clone(),
      properties: originalFeature.getProperties(),
    };
    return clonedFeature;
  }

  // 关闭圈选
  closeBoxToSelect() {
    const map = thirdMaps.get(this.mapId);
    map.removeLayer(this.boxToSelectLayer);
    map.removeInteraction(this.boxToSelectDraw);
  }

  // 关闭自定义弹窗
  closeCustomPopup() {
    const map = thirdMaps.get(this.mapId);
    const popup = map.getOverlayById('popup');
    popup.setPosition(null);
  }

  deleteAllDraw() {
    console.log('Unrealized function');
  }

  /**
   * 删除三道防线/自定义图层点位
   * @param {string} keyWord 关键字
   */
  deleteCustomMarker(keyWord) {
    const map = thirdMaps.get(this.mapId);
    this.defenseMarkers.forEach((val, key) => {
      if (key.includes(keyWord)) {
        map.removeOverlay(val);
        this.defenseMarkers.delete(key);
      }
    });
  }

  // 删除标绘图层
  deleteDrawLayer() {
    this.drawLayer.forEach((i) => {
      if (i.get('layerId') === 'plotLayer') {
        i.getSource()
          .getFeatures()
          .forEach((f) => {
            i.getSource().removeFeature(f);
          });
      }
    });
  }

  // 删除图层
  deleteLayer(layerIds) {
    if (!isArray(layerIds)) {
      layerIds = [layerIds];
    }

    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    layerIds.forEach((layerId) => {
      const layer = this.getLayer(layerId);
      map?.removeLayer(layer);
      this.drawLayer = this.drawLayer.filter((i) => {
        return i.get('layerId') !== layerId;
      });
    });
  }

  // 删除marker
  deleteMarker(idList) {
    if (isArray(idList)) {
      idList.forEach((id) => {
        const f = this.markers.get(id);
        if (f) {
          this.markerSource.removeFeature(f.marker);
          this.markers.delete(id);
        }
      });
    }

    this.offAddressMarkerClick();
  }

  // 销毁地图
  destroyMap() {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);

    // 销毁所有图层
    map.getLayers().forEach((layer) => {
      layer.setMap(null);
    });

    // 销毁地图控件
    map.getControls().forEach((control) => {
      map.removeControl(control);
    });

    // 销毁地图的交互
    map.getInteractions().forEach((interaction) => {
      map.removeInteraction(interaction);
    });

    // 销毁地图本身
    map.setTarget(null);

    mapManager.delete(mapId);
  }

  /**
   * 隐藏显示绘制的区域
   * @param {boolean} visible 是否显示draw
   */
  disableDraw(visible) {
    const { drawLayer } = this;
    drawLayer.forEach((i) => i.setVisible(visible));
  }

  /**
   * 拖拽marker点
   * @param {Object} data
   */
  dragendMarker(_, config) {
    const { callback } = config;
    const query = debounce(async (e) => {
      const p = to4326(e.coordinate);
      const address = await this.queryAddressByLocation(p);
      if (address) {
        callback?.(p.join(','), address);
      }
    }, 500);
    this.dragInteraction?.on('translating', query);
  }

  /**
   * 绘制区域
   * @param {point | polyline | rectangle | circle | polygon | line_arrow} type 点、线、矩形、圆、多边形、箭头
   * @param {add | modify | look} handle 新增、修改、查看
   * @param {array} path 路径点集合
   * @param {array} center 圆的中心点
   * @param {number} radius 圆的半径
   * @param {function} change 绘制更新事件
   * @param {boolean} highlight 是否高亮
   * @param {string} color 颜色 '252, 167, 1'
   * @param {string} lineStyle 实线 solid，虚线 dashed
   * @param {boolean} saveBefore 是否保留之前绘制图案 默认true
   * @param {number} textSize 文字字号
   * @param {number} textValue 文字内容
   * @param {number} opacity 不透明度
   * @param {string} imageName icon名字
   * @param {string} iconUrl type为icon的时候一起传的图片地址
   */
  draw(config) {
    this.offTextClick?.();

    const {
      center,
      change,
      color,
      drawLayerId,
      fillColor,
      handle,
      highlight,
      iconUrl,
      id,
      layerId,
      lineStyle,
      lineWidth,
      opacity,
      radius,
      saveBefore = true,
      textSize,
      type,
    } = config;
    let { path, textValue } = config;
    const { drawLayer, drawModify, editDrawer, mapId } = this;
    const typeFormat = {
      circle: 'Circle',
      icon: 'Point',
      line_arrow: 'LineString',
      point: 'Point',
      polygon: 'Polygon',
      polyline: 'LineString',
      rectangle: 'Circle',
      text: 'Point',
    };
    const map = thirdMaps.get(mapId);
    let draw: Draw;
    let source: any = null;
    let vector: any = null;

    const _layerId = drawLayerId || layerId || 'drawLayer';
    let targetLayer: any = null;
    drawLayer.forEach((i) => {
      if (i.get('layerId') === _layerId) {
        targetLayer = i;
      }
    });
    if (targetLayer) {
      source = targetLayer.getSource();
    } else {
      source = new VectorSource();
      const properties = { layerId: _layerId };
      vector = new VectorLayer({ properties, source, zIndex: 1 });
      this.drawLayer.push(vector);
      map.addLayer(vector);
    }

    const featureId = id || guid();
    const style = {
      fillColor: getColor(fillColor || color, isNumber(opacity) ? opacity : 0.5), // 填充颜色
      strokeColor: getColor(color, opacity || 1), // 描边颜色
      strokeWeight: lineWidth || 4, // 描边宽度
    };

    // 获取feature特征
    const getStye = ({ id, path }: any) => {
      if (type === 'point') {
        const c = new CircleStyle({
          fill: new Fill({
            color: style.strokeColor,
          }),
          radius: radius || 5,
        });
        c.setOpacity(opacity);
        return new Style({ image: c });
      }

      if (type === 'text') {
        return new Style({
          text: new Text({
            fill: new Fill({
              color: style.strokeColor,
            }),
            font: `${textSize}px Calibri,sans-serif`,
            text: textValue, // 获取特征的名称作为文本
          }),
        });
      }

      if (type === 'icon') {
        return new Style({
          image: new Icon({
            scale: 0.8, // 图标缩放比例
            src: iconUrl, // 图标的 URL
          }),
        });
      }

      const stroke: any = {
        color: style.strokeColor,
        width: style.strokeWeight,
      };
      if (lineStyle === 'dashed') {
        stroke.lineDash = [20, 10, 40, 20];
      }

      if (type === 'line_arrow') {
        const _id = `arrow${id || featureId}`;
        const oldArrow = source.getFeatureById(_id);
        source.removeFeature(oldArrow);

        const arrow = this.drawArrow(path, { ...stroke, fillColor: style.fillColor });
        arrow.setId(_id);
        source.addFeature(arrow);
      }

      return new Style({
        fill: new Fill({
          color: style.fillColor,
        }),
        stroke: new Stroke(stroke),
      });
    };

    const pathChange = (f, record = true) => {
      // 是否需要记录
      if (record) {
        this.drawStepList.push(this.cloneFeature(f));
      }

      const geo = f.getGeometry();
      const id = f.getId();
      const _type = f.get('type') || type;
      let coords = [];
      switch (_type) {
        case 'circle': {
          const center = geo.getCenter();
          const radius = geo.getRadius();
          change({ center: to4326(center), radius }, id);
          break;
        }
        case 'icon':
        case 'point':
        case 'text': {
          const coords = geo.getCoordinates();
          change(to4326(coords), id);
          break;
        }
        case 'line_arrow': {
          coords = geo.getCoordinates();
          if (id.includes('arrow')) {
            const _id = id.replace('arrow', '');
            const _coords = source.getFeatureById(_id).getGeometry().getCoordinates();
            getStye({ id: _id, path: _coords });
            return;
          }
          getStye({ id, path: coords });
          break;
        }
        case 'polygon': {
          coords = geo.getCoordinates()[0];
          break;
        }
        case 'polyline': {
          coords = geo.getCoordinates();
          break;
        }
        case 'rectangle': {
          coords = geo.getCoordinates()[0];
          coords.splice(4);
          break;
        }
      }
      if (coords.length > 0) {
        change(
          coords.map((i) => to4326(i)),
          id,
        );
      }
    };

    const createFeature = () => {
      // 避免重复添加
      const f = source.getFeatureById(featureId);
      if (f) {
        source.removeFeature(f);
      }

      let coordinates = [];
      coordinates = ['icon', 'point', 'text'].includes(type)
        ? formatLonLat(path)
        : path?.map((item) => formatLonLat(item));
      let geometry: any = null;
      switch (type) {
        case 'circle': {
          geometry = new Circle(formatLonLat(center), radius);
          break;
        }
        case 'icon':
        case 'point':
        case 'text': {
          geometry = new Point(coordinates);
          break;
        }
        case 'line_arrow':
        case 'polyline': {
          geometry = new LineString(coordinates);
          break;
        }
        case 'polygon': {
          geometry = new Polygon([coordinates]);
          break;
        }
        case 'rectangle': {
          let min = coordinates[0];
          let max = coordinates[0];
          coordinates.forEach((item) => {
            if (item[0] < min[0] || item[1] < min[1]) {
              min = item;
            }
            if (item[0] > max[0] || item[1] > max[1]) {
              max = item;
            }
          });
          const extent = [...min, ...max];
          geometry = fromExtent(extent);
          break;
        }
      }

      const feature = new Feature({ geometry, handle });
      feature.setId(featureId);
      if (textValue !== '') {
        config.textValue = textValue;
      }
      feature.setProperties(config);
      feature.setStyle(getStye({ id: featureId, path: coordinates }));

      return feature;
    };

    // 绘制工具
    const drawer = () => {
      map.removeInteraction(this.editDrawer);

      let geometryFunction: any = null;
      if (type === 'rectangle') {
        geometryFunction = createBox();
      }

      draw = new Draw({
        geometryFunction,
        source,
        type: typeFormat[type],
      });
      map.addInteraction(draw);
      this.editDrawer = draw;

      // 结束绘制
      draw.on('drawend', (e) => {
        const { feature } = e;
        const geo: any = feature.getGeometry();
        const coords = geo.getCoordinates();
        map.removeInteraction(draw);
        feature.setId(featureId);
        feature.setStyle(getStye({ path: coords }));
        feature.setProperties(config);
        pathChange(feature);
      });
    };

    const registerModifyEvent = (modify) => {
      if (modify.modifyEvent) {
        modify.un('modifyend', modify.modifyEvent);
      }

      const m = (e) => {
        e.features.forEach((f) => {
          // 确保矩形不被拖拽变形
          if (f.get('type') === 'rectangle') {
            const geometry: any = f.getGeometry();
            const points = geometry.getCoordinates()[0];
            points.splice(-1);

            // 4个点
            if (points.length === 4) {
              points.forEach((item, index) => {
                const [x, y] = item;
                const before = points[index === 0 ? 3 : index - 1];
                const after = points[index === 3 ? 0 : index + 1];
                if (
                  !before.includes(x) &&
                  !before.includes(y) &&
                  !after.includes(x) &&
                  !after.includes(y)
                ) {
                  if (index % 2 === 1) {
                    before[1] = y;
                    after[0] = x;
                  } else {
                    before[0] = x;
                    after[1] = y;
                  }
                }
              });
            }

            // 5个点
            if (points.length === 5) {
              for (let i = 0; i < points.length; i++) {
                const [x, y] = points[i];
                const before = points[i === 0 ? 4 : i - 1];
                const after = points[i === 4 ? 0 : i + 1];

                if ((before[0] === x && after[0] === x) || (before[1] === y && after[1] === y)) {
                  break;
                }

                if (
                  !before.includes(x) &&
                  !before.includes(y) &&
                  !after.includes(x) &&
                  !after.includes(y)
                ) {
                  if (before[0] === after[0]) {
                    before[0] = x;
                    after[0] = x;
                  } else {
                    before[1] = y;
                    after[1] = y;
                  }
                  points.splice(i, 1);
                  break;
                }
              }
            }

            points.push(points[0]);
            geometry.setCoordinates([points]);
          }
          pathChange(f);
        });
      };
      modify.modifyEvent = m;
      modify.on('modifyend', m);
    };

    const modifier = (active = true) => {
      this.drawStepList = [];

      const interactions = map.getInteractions().getArray();
      interactions.forEach((i) => {
        if (i.getProperties().layerId === _layerId) {
          map.removeInteraction(i);
        }
      });

      const modify: any = new Modify({ source });
      modify.setProperties({ layerId: _layerId });
      map.addInteraction(modify);

      modify.pathChange = pathChange;
      this.drawModify = modify;
      registerModifyEvent(modify);
      modify.setActive(active);
    };

    if (!saveBefore) {
      const drawModifyId = drawModify?.get('layerId');
      if (!((drawModifyId || layerId) && drawModifyId !== layerId)) {
        drawModify?.source_.clear();
        map.removeInteraction(editDrawer);
      }
    }

    // 添加feature到地图上
    const addFeature = () => {
      if (!highlight) {
        style.strokeWeight = 1;
        style.fillColor = style.fillColor.replace('0.5', '0.1');
      }

      const f = createFeature();
      source.addFeature(f);
    };

    // 添加文字输入框
    const onceClick = (e) => {
      const el = document.createElement('input');
      el.style.color = '#fff';
      el.style.backgroundColor = '#000';

      setTimeout(() => {
        const lngLat = e.coordinate;
        this.markerPopup(el, lngLat);
        el.focus();
        el.addEventListener(
          'blur',
          (e: any) => {
            const val = e.target?.value;
            if (val !== '') {
              textValue = val;
              path = to4326(lngLat);
              addFeature();
            }
            change(path, featureId, val);
          },
          false,
        );
      }, 500);
    };

    switch (handle) {
      case 'add': {
        modifier();
        if (type === 'text') {
          map.once('click', onceClick);
          this.offTextClick = () => {
            map.un('click', onceClick);
          };
        } else {
          drawer();
        }
        break;
      }
      case 'look': {
        modifier(false);
        addFeature();
        break;
      }
      case 'modify': {
        modifier();
        const f = createFeature();
        source.addFeature(f);
        break;
      }
    }
  }

  /**
   * 根据线画箭头
   */
  drawArrow(path, style) {
    // 创建箭头的几何形状
    const createArrowGeometry = (coordinates) => {
      // const line = new LineString(coordinates);
      const arrowHead = new LineString([
        coordinates[coordinates.length - 2], // 线的倒数第二个点
        coordinates[coordinates.length - 1], // 线的最后一个点
      ]);
      const arrow = arrowHead.getCoordinates();

      // 计算箭头的方向
      const dx = arrow[1][0] - arrow[0][0];
      const dy = arrow[1][1] - arrow[0][1];
      const angle = Math.atan2(dy, dx);

      // 箭头的长度和宽度
      const arrowLength = 50;
      // const arrowWidth = 5;

      // 箭头的两个边
      const leftPoint = [
        arrow[1][0] - arrowLength * Math.cos(angle - Math.PI / 6),
        arrow[1][1] - arrowLength * Math.sin(angle - Math.PI / 6),
      ];
      const rightPoint = [
        arrow[1][0] - arrowLength * Math.cos(angle + Math.PI / 6),
        arrow[1][1] - arrowLength * Math.sin(angle + Math.PI / 6),
      ];

      return new MultiLineString([arrow, [arrow[1], leftPoint], [arrow[1], rightPoint]]);
    };

    // 添加箭头图层
    const arrowFeature = new Feature({
      geometry: createArrowGeometry(path.slice(-2)), // 绘制箭头的坐标(取线的最后两个坐标)
    });

    const arrowStyle = new Style({
      fill: new Fill({
        color: style.fillColor,
      }),
      stroke: new Stroke(style),
    });

    arrowFeature.setStyle(arrowStyle);
    return arrowFeature;
  }

  endDraw() {
    console.log('Unrealized function');
  }

  /**
   * @description 飞行
   * @param {LngLat} center 中心点
   * @param {Number} zoom 缩放
   * @param {Number} bearing 旋转
   */
  flyAction(center, zoom?, bearing?) {
    const map = thirdMaps.get(this.mapId);
    if (!isNumber(zoom)) {
      zoom = this.getZoom();
    }
    map.getView().animate({
      center: formatLonLat(center), // 目标位置
      constrainRotation: bearing, // 使旋转
      duration: 2000, // 动画持续时间（毫秒）
      zoom, // 目标缩放级别
    });
  }

  // 获取所有layerId
  getAllLayerId() {
    const ret: string[] = [];
    const map = thirdMaps.get(this.mapId);
    map
      ?.getLayers()
      ?.getArray()
      .forEach((i) => {
        const id = i.get('layerId');
        if (id) {
          ret.push(id);
        }
      });
    return ret;
  }

  // 底图
  async getBaseLayer(map) {
    const config = getMapConfig();
    const {
      center,
      layer,
      LAYERS,
      matrixSet,
      maxZoom,
      minZoom,
      projection,
      resolutions,
      server,
      xml,
      XYZmaxZoom = 14,
      zoom,
    } = config;
    const { type } = appConfig.mapConfig;
    let url = config.url || '';
    if (!url.startsWith('http')) {
      url = getLocationOrigin() + url;
    }

    const view = map.getView();
    if (center) {
      view.setCenter(formatLonLat(center));
    }
    if (zoom) {
      view.setZoom(zoom);
    }

    if (isNumber(minZoom)) {
      this.zooms[0] = minZoom;
      view.setMinZoom(minZoom);
    }
    if (isNumber(maxZoom)) {
      this.zooms[1] = maxZoom;
      view.setMaxZoom(maxZoom);
    }

    // 0栅格 1矢量
    if (type === 0) {
      let source: any;
      switch (server.toLowerCase()) {
        case 'wms': {
          source = new TileWMS({
            params: { LAYERS },
            projection,
            url,
            wrapX: true,
          });
          break;
        }
        case 'wmts': {
          /**
           * layer：<Layer><ows:Identifier>China</ows:Identifier></Layer>
           * matrixSet+projection: <TileMatrixSet>
           *        <ows:Identifier>ChinaPublicServices_China</ows:Identifier>
           *        <ows:SupportedCRS>urn:ogc:def:crs:EPSG::4326</ows:SupportedCRS>
           *    </TileMatrixSet>
           */

          if (xml.includes('?')) {
            // 方式1url带参数直接访问
            // 带key校验参数直接拼在xml参数里
            const _projection = olProj.get(projection) as any;
            const projectionExtent = _projection.getExtent();
            const size = getWidth(projectionExtent) / 256;
            const _resolutions: any = [];
            const matrixIds: any = [];

            if (resolutions) {
              for (const [z, resolution] of resolutions.entries()) {
                _resolutions[z] = resolution;
                matrixIds[z] = z;
              }
            } else {
              for (let z = 0; z < 19; ++z) {
                // generate resolutions and matrixIds arrays for this WMTS
                _resolutions[z] = size / 2 ** z;
                matrixIds[z] = z;
              }
            }

            /**
             * 通过ScaleDenominator计算resolutions
             */
            // result.Contents.TileMatrixSet[0].TileMatrix.forEach((item) => {
            //   resolutions.push(
            //     (0.0254 * item.ScaleDenominator) / (96 * ((Math.PI * 2 * 6378137) / 360)),
            //   );
            //   matrixIds.push(item.Identifier);
            // });

            source = new WMTS({
              format: 'image/png',
              layer,
              matrixSet,
              projection: _projection,
              style: 'default',
              tileGrid: new WMTSTileGrid({
                matrixIds,
                origin: getTopLeft(projectionExtent),
                resolutions: _resolutions,
              }),
              url: xml,
              wrapX: true,
            });
          } else {
            // 方式2通过xml获取相关值
            const text = await fetch(xml as string).then((r) => r.text());
            const parser = new WMTSCapabilities();
            const result = parser.read(text);

            const options = optionsFromCapabilities(result, {
              layer, // layer Identifier
              matrixSet, // TileMatrixSet Identifier
            });

            source = new WMTS({
              ...options,
              tileLoadFunction(imageTile, src) {
                // 确保xml里的请求协议/ip/端口和配置的一致（Nginx配置代理）
                const u = new URL(xml);
                const r = new RegExp(`(http)(.*?)(${u.pathname})`, 'g');
                imageTile.getImage().src = src.replace(r, xml);
              },
            } as any);
          }
          break;
        }
        case 'xyz': {
          if (projection === 'BD09') {
            // 定义百度地图分辨率与瓦片网格
            const resolutions: number[] = [];
            for (let i = 0; i <= 18; i++) {
              resolutions[i] = 2 ** (18 - i);
            }

            const tilegrid = new olTilegrid.TileGrid({
              extent: olProj.transformExtent([-180, -74, 180, 74], 'EPSG:4326', 'BD09'),
              origin: [0, 0],
              resolutions,
            });
            source = new XYZSource({
              projection: 'BD09',
              tileGrid: tilegrid,
              // 百度地图的子域
              tileLoadFunction: (imageTile: any) => {
                const { tileCoord } = imageTile;
                const z = tileCoord[0];
                let x: any = tileCoord[1];
                let y: any = -tileCoord[2] - 1;
                const hash = (x << z) + y;
                console.log(hash);
                if (x < 0) {
                  x = `M${-x}`;
                }
                if (y < 0) {
                  y = `M${-y}`;
                }
                imageTile.getImage().src = url
                  .replace('{x}', x)
                  .replace('{y}', y)
                  .replace('{z}', z);
              },
              url,
            });
          } else {
            source = new XYZSource({ url });
          }
          break;
        }
      }

      const tLayer = new TileLayer({
        properties: {
          id: `TileLayer${Date.now()}`,
        },
        source, // 设置瓦片源
      });
      map.addLayer(tLayer);
      tLayer.setZIndex(0);
    } else {
      const vtLayer = new VectorTileLayer({
        properties: {
          id: `VectorTileLayer${Date.now()}`,
        },
        source: new VectorTileSource({
          format: new MVT(),
          tileGrid: olTilegrid.createXYZ({ maxZoom: XYZmaxZoom }),
          url,
        }),
        style: createMapboxVectorStyle(),
      });
      map.addLayer(vtLayer);
      vtLayer.setZIndex(0);
    }
  }

  getCanvas() {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const canvas = map.getTargetElement().querySelector('canvas');
    return canvas;
  }

  // 获取地图中心点
  getCenter() {
    const map = thirdMaps.get(this.mapId);
    const view = map.getView();
    const center = view.getCenter();
    return to4326(center);
  }

  /**
   * 获取圆的最大经纬度+最小经纬度
   * @param center 圆心的经纬度
   * @param radius 半径（单位为米）
   * @returns
   */
  getCoordinates(center, radius) {
    // 创建圆形的 GeoJSON 对象
    const circle = Turf.circle(center, radius, { units: 'meters' });
    // 获取边界框
    const bbox = Turf.bbox(circle);
    return {
      maxLatitude: bbox[3],
      maxLongitude: bbox[2],
      minLatitude: bbox[1],
      minLongitude: bbox[0],
    };
  }

  // 获取图层
  getLayer(layerId) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const layers = map?.getLayers();
    let layer = null;

    layers?.array_.forEach((item) => {
      if (item.get('layerId') === layerId) {
        layer = item;
      }
    });
    return layer;
  }

  /**
   * @description 获取图层数据
   * @param {*} layerId
   * @returns {array}
   */
  getLayerData(layerId) {
    const layer: any = this.getLayer(layerId);
    if (!layer) {
      return [];
    }

    const source = layer.getSource();
    const features: any = [];
    source.getFeatures().forEach((i) => {
      i.values_.features.forEach((f) => {
        features.push(f);
      });
    });

    const { sourceMap } = this.layers.get(layerId);
    const ret: any[] = [];

    features.forEach((item) => {
      const data = sourceMap.get(item.getId());
      if (data) {
        ret.push(data);
      }
    });
    return ret;
  }

  /**
   * @description 获取图层是否显示
   * @param {string} layerId
   */
  getLayerVisible(layerId) {
    const layer: any = this.getLayer(layerId);
    return layer?.getVisible();
  }

  // 获取当前地图缩放级别
  getZoom() {
    const map = thirdMaps.get(this.mapId);
    const view = map.getView();
    return view.getZoom();
  }

  // 初始化地图
  async initMap() {
    register4490();
    registerBD09();

    const { mapId } = this;
    const { center, maxZoom, minZoom, zoom } = getMapConfig();

    // 弹窗
    const el = document.createElement('div');
    const overlay = new Overlay({
      autoPan: {
        animation: {
          duration: 250,
        },
      },
      element: el,
      id: 'popup',
    });

    const map = new OlMap({
      layers: [],
      overlays: [overlay],
      target: this.mapId,
      view: new View({
        center: formatLonLat(center),
        maxZoom,
        minZoom,
        projection: 'EPSG:3857',
        zoom,
      }),
    });
    thirdMaps.set(mapId, map);

    this.getBaseLayer(map);

    // 设置鼠标样式
    map.getTargetElement().style.cursor = 'pointer';

    this.isReady = true;
    this.onLoad(this);
    this.onLayerClick(map);
    this.initMarkerLayer();
    this.registerDrawClick();
  }

  // 创建marker点图层
  initMarkerLayer() {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);

    // 创建矢量源
    const source = new VectorSource({
      features: [],
    });
    this.markerSource = source;

    // 创建矢量图层
    const markerLayer = new VectorLayer({ source });

    // 将标记图层添加到地图上
    map.addLayer(markerLayer);
    markerLayer.setZIndex(1);
  }

  /**
   * @description marker弹窗
   * @param {*} el dom
   * @param {*} options 图层参数
   * @param {*} offset 偏移量
   */
  markerPopup(el, position, offset?) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);

    this.closeCustomPopup();
    const popup = map.getOverlayById('popup');
    popup.setOffset(offset || [-6, -4]);
    popup.setPositioning('bottom-center');
    popup.setElement(el);
    popup.setPosition(position);
  }

  // 清除点位添加
  offAddressMarkerClick() {
    if (this.addressMarkerClick) {
      const map = thirdMaps.get(this.mapId);
      map.un('singleclick', this.addressMarkerClick);
    }
  }

  // 图层点击事件
  onLayerClick(map) {
    const selectFnc = (features, layerId, coordinate) => {
      const { mapId } = this;
      const layer = this.layers.get(layerId);
      if (!layer) {
        return;
      }

      const { sourceMap, windowTemplate } = layer;
      if (!windowTemplate) {
        return;
      }

      const popup = map.getOverlayById('popup');
      popup.setPosition(coordinate);
      popup.setOffset([-4, -4]);
      popup.setPositioning('bottom-center');

      if (features.length > 1) {
        const data: any = [];
        features.forEach((feature) => {
          const sourceItem = sourceMap.get(feature.getId());
          if (!sourceItem) {
            console.error('data not find');
            return;
          }
          data.push(sourceItem);
        });
        const parent = document.createElement('div');
        const instance = useCreateApp(CursorList, {
          data,
          layerId,
          mapId,
          position: to4326(coordinate),
          template: windowTemplate,
        });
        const element = instance.mount(parent).$el;
        popup.setElement(element);
      } else {
        const feature = features[0];
        const data = sourceMap.get(feature.getId());
        const parent = document.createElement('div');
        const instance = useCreateApp(MapPopup, {
          data,
          layerId,
          mapId,
          template: windowTemplate,
        });
        const element = instance.mount(parent).$el;
        popup.setElement(element);
      }

      this.setCenter(to4326(coordinate));
    };

    // 没有采用select是因为同一个feture不能连续选择
    const queue: Feature[] = [];
    map.on('singleclick', (e) => {
      // 点击空白处关闭弹窗
      this.closeCustomPopup();
      queue.length = 0;
      map.forEachFeatureAtPixel(
        e.pixel,
        (f) => {
          queue.push(f);
          if (queue.length > 1) {
            return;
          }
          const features = f.get('features');
          if (!features) {
            return;
          }
          const geometry = f.getGeometry();
          const { layerId } = features[0].getProperties();
          selectFnc(features, layerId, geometry.flatCoordinates);
        },
        {
          hitTolerance: 20,
        },
      );
    });
  }

  /**
   * @description 计算图形区域内的点
   * @param type 图形类型 点marker 线polyline 多边形polygon 矩形rectangle 圆circle
   * @param layerId 图层id
   * @returns
   */
  pointsWithinPolygon({ center, lineWidth, path, radius, type }, layerId) {
    const layer = this.getLayer(layerId) as any;

    if (!layer?.get('visible')) {
      return;
    }

    let searchWithin: any;
    switch (type) {
      case 'circle': {
        const options = { steps: 10, units: 'meters' };
        searchWithin = Turf.circle(center, radius, options as any);
        break;
      }
      case 'polygon':
      case 'rectangle': {
        searchWithin = Turf.multiPolygon([[path]]);
        break;
      }
      case 'polyline': {
        const line = Turf.lineString(path);
        // 获取一条线并返回具有指定距离偏移量的线
        const line1 = Turf.lineOffset(line, -lineWidth / 2, { units: 'meters' });
        const line2 = Turf.lineOffset(line, lineWidth / 2, { units: 'meters' });
        const coordinates2 = [...line2.geometry.coordinates];
        coordinates2.reverse();
        const coordinates = [...line1.geometry.coordinates, ...coordinates2];
        searchWithin = Turf.multiPolygon([[coordinates]]);

        // 保证图形首位相连
        const coordinate = searchWithin.geometry.coordinates[0][0];
        coordinate.push(coordinate[0]);
        break;
      }
    }

    const { sourceMap } = this.layers.get(layerId) || {};

    const _points: any = [];
    this.getLayerData(layerId).forEach((item) => {
      const point = Turf.point(formatLonLatHelper(item.position) as any, item);
      _points.push(point);
    });
    const points = Turf.featureCollection(_points);

    const ptsWithin = Turf.pointsWithinPolygon(points as any, searchWithin);
    const features = ptsWithin?.features || [];

    const ret: any[] = [];
    for (const feature of features) {
      const { id } = feature.properties as any;
      const target = sourceMap.get(id);
      if (target) {
        ret.push(target);
      }
    }
    return ret;
  }

  /**
   * @description 逆地理编码搜索
   * @param {string} location 坐标点
   */
  async queryAddressByLocation(location) {
    let ret = '';
    const { code, data } = await selectInversecode({
      langType: 0,
      location: location.join(','),
    });
    if (code === 0 && data) {
      ret = data.fullPath;
    }
    return ret;
  }

  /**
   * @description 关键词搜索
   * @param {string} keywords 关键词
   * @param {number} page 当前页
   */
  async queryAllSearch(keywords, page) {
    let ret: any = { count: 0, pois: [] };
    if (keywords === '') {
      return ret;
    }
    const { code, data } = await selectPoi({
      keywords,
      page,
      size: 10,
    });
    if (code === 0 && data) {
      const pois = data.dataList.map((i) => {
        return {
          address: i.address,
          city: '',
          district: '',
          location: i.location,
          name: i.name,
          nid: i.name + i.location,
          phone: '',
          province: '',
          tel: '',
          type: '',
          typeName: '',
        };
      });
      ret = { count: Number(data.total), pois };
    }
    return ret;
  }

  // 注册draw点击事件
  registerDrawClick() {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);
    const { setSelectFeature } = useResourceStore();
    let feature: any = null;

    const getDrawLayer = () => {
      let ret: any = null;
      this.drawLayer.forEach((i) => {
        if (i.get('layerId') === 'plotLayer') {
          ret = i;
        }
      });
      return ret;
    };

    window.addEventListener(
      'keydown',
      (e) => {
        if (['Backspace', 'Delete'].includes(e.key)) {
          const source = getDrawLayer()?.getSource();
          const id = feature?.getId();
          if (id) {
            source.removeFeature(feature);

            const arrowFeature = source.getFeatureById(`arrow${id}`);
            if (arrowFeature) {
              source.removeFeature(arrowFeature);
            }

            setSelectFeature({ delete: true, id });
          }
        }
      },
      true,
    );

    map.on('singleclick', (e) => {
      feature = null;

      const interactions = map.getInteractions().getArray();
      for (const item of interactions) {
        const { layerId } = item.getProperties();
        if (layerId === 'plotLayer' && !item.getActive()) {
          return;
        }
      }

      const source = getDrawLayer()?.getSource();
      if (!source) {
        return;
      }

      map.forEachFeatureAtPixel(e.pixel, (f) => {
        try {
          const ft = f.values_?.features?.[0];
          let id = f?.getId();
          if (!id && ft) {
            id = ft.getId();
            f = ft;
          }

          if (source.getFeatureById(id)) {
            feature = f;
            const c = f.getProperties();
            if (Object.keys(c).length <= 1) {
              return;
            }
            const dasharray = String(c.lineStyle === 'dashed');
            const p: any = {
              circleBorderColor: c.color,
              circleColor: c.fillColor,
              circleOpacity: c.opacity,
              feature_type: c.type,
              fillColor: c.fillColor,
              fillOpacity: c.opacity,
              fillOutlineColor: c.color,
              fillOutlineDasharray: dasharray,
              fillOutlineWidth: c.lineWidth,
              id,
              lineColor: c.fillColor,
              lineDasharray: dasharray,
              lineOpacity: c.opacity,
              lineWidth: c.lineWidth || 4,
              textColor: c.color,
              textSize: c.textSize,
            };
            setSelectFeature(p);
          }
        } catch {
          //
        }
      });

      if (!feature) {
        setSelectFeature('');
      }
    });
  }

  /**
   * 删除图层
   * @param layerId
   *
   */
  removeLayer(layerId) {
    const map = thirdMaps.get(this.mapId);
    const layer = this.getLayer(layerId);
    if (layer) {
      map?.removeLayer(layer);
    }
  }

  // 删除marker
  removeMarkers(data) {
    this.deleteMarker(data.map((i) => i.id));
  }

  /**
   * 删除车辆幻化图标
   * @param data
   */
  removeVehicleMarker(data) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);

    this.vehicleMarkers = this.vehicleMarkers.filter((item) => {
      if (item.id === data.id) {
        map.removeOverlay(item.overlay);
        return false;
      }
      return true;
    });
  }

  // 重复上一步
  repeatPrevious() {}

  /**
   * @description 设置地图中心点与缩放级别
   * @param {LngLat} center
   * @param {Number} zoom
   */
  setCenter(center: Position, zoom?: number) {
    const map = thirdMaps.get(this.mapId);
    const view = map.getView();
    const [min, max] = this.zooms;

    if (!isNumber(zoom)) {
      zoom = max;
    } else if (zoom > max) {
      zoom = max;
    } else if (zoom < min) {
      zoom = min;
    }

    const position = formatLonLat(center);
    if (!position) {
      console.error('center is null');
      return;
    }
    view.setCenter(position);
    view.setZoom(zoom);
  }

  /**
   * 设置标绘是否可以编辑
   * @param {Array} idList features的id数组 不传取全部
   * @param {Boolean} isLock 是否锁定  true锁定 false解锁
   */
  setFeatureLock(isLock) {
    const map = thirdMaps.get(this.mapId);
    const interactions = map.getInteractions().getArray();

    const active = !isLock;
    interactions.forEach((i) => {
      if (i.getProperties().layerId === 'plotLayer') {
        i.setActive(active);
      }
    });
  }

  /**
   * @description 设置图层是否显示
   * @param {array} layerIds
   * @param {boolean} visible
   */
  setLayersVisible(layerIds, visible) {
    layerIds.forEach((layerId) => {
      if (layerId.includes('region')) {
        this.drawLayer.forEach((i) => {
          if (i.get('layerId').includes('region')) {
            i.setVisible(visible);
          }
        });
      } else {
        const layer: any = this.getLayer(layerId);
        layer?.setVisible(visible);
      }
    });
  }

  /**
   * 设置marker点是否做拽
   * @param {Object} data
   * @param {Boolean}  drag
   */
  setMarkerDraggable(data, drag) {
    const { mapId } = this;
    const map = thirdMaps.get(mapId);

    const f = this.markers.get(data.id);
    if (!f) {
      return;
    }
    if (drag) {
      // 添加拖拽交互
      this.dragInteraction = new Translate({
        features: new Collection([f.marker]),
      });
      map.addInteraction(this.dragInteraction);
    } else {
      map.removeInteraction(this.dragInteraction);
    }
  }

  // 设置二次编辑图形样式
  setSecondEditStyle(data) {
    const {
      feature_type,
      fillColor,
      fillOpacity,
      id,
      lineColor,
      lineDash,
      lineWidth,
      strokeColor,
      strokeOpacity,
      strokeWidth,
      textColor,
      textSize,
    } = data;
    let source: any;
    let feature: any;

    this.drawLayer.forEach((i) => {
      if (i.get('layerId') === 'plotLayer') {
        source = i.getSource();
        feature = source.getFeatureById(id);
      }
    });

    const config = feature.getProperties();
    feature.setProperties({
      ...config,
      color: feature_type === 'text' ? textColor : strokeColor,
      fillColor: feature_type === 'polyline' ? lineColor : fillColor,
      lineStyle: lineDash,
      lineWidth,
      opacity: fillOpacity,
      textSize,
    });

    switch (feature_type) {
      case 'point': {
        const image = new CircleStyle({
          fill: new Fill({
            color: getColor(fillColor, fillOpacity),
          }),
          radius: 5,
        });
        feature.setStyle(new Style({ image }));
        break;
      }
      case 'text': {
        const text = new Text({
          fill: new Fill({
            color: textColor,
          }),
          font: `${textSize}px Calibri,sans-serif`,
          offsetY: -15, // 文本偏移
          text: feature.get('textValue'), // 获取特征的名称作为文本
        });
        feature.setStyle(new Style({ text }));
        break;
      }
      default: {
        const stroke: any = {
          color: getColor(strokeColor, strokeOpacity),
          offset: -strokeWidth / 2,
          width: strokeWidth,
        };
        const style = {
          fillColor: getColor(fillColor, fillOpacity),
        };

        if (lineDash === 'dashed') {
          stroke.lineDash = [20, 10, 40, 20];
        }

        if (feature_type === 'line_arrow') {
          const _id = `arrow${id}`;
          const oldArrow = source.getFeatureById(_id);
          source.removeFeature(oldArrow);

          const path = source.getFeatureById(id).getGeometry().getCoordinates();
          const arrow = this.drawArrow(path, { ...stroke, fillColor: style.fillColor });
          arrow.setId(_id);
          source.addFeature(arrow);
        }

        const s = new Style({
          fill: new Fill({
            color: style.fillColor,
          }),
          stroke: new Stroke(stroke),
        });
        feature.setStyle(s);
      }
    }
  }

  /**
   * @description 设置地图图层样式
   */
  async setStyle() {
    const map = thirdMaps.get(this.mapId);
    const mnpLayers = map.getLayers();
    mnpLayers.array_.forEach((l) => {
      const id = l.values_.id || '';
      if (id.includes('TileLayer') || id.includes('VectorTileLayer')) {
        map.removeLayer(l);
      }
    });
    this.getBaseLayer(map);
  }

  /**
   * 根据状态隐藏/显示所有marker
   * @param {boolean} visible 显示状态
   */
  showMarkerByFlag(visible) {
    this.defenseMarkers.forEach((i) => {
      i.element.style.display = visible ? `` : `none`;
    });
    this.vehicleMarkers.forEach((i) => {
      i.marker.element.style.display = visible ? `flex` : `none`;
    });
  }

  // 返回动向实例
  track(config) {
    return new _Track({ map: this, ...config });
  }

  // 返回轨迹跟踪实例
  trackRealTime(config) {
    return new _TrackRealTime({ map: this, ...config });
  }

  /**
   * 撤销上一步绘制操作
   * @param {boolean} keepFirstStep 保留第一步
   * @returns
   */
  undoPreviousDraw(config) {
    const { keepFirstStep } = config || {};
    const { drawModify, drawStepList } = this;

    // 保留初始绘制
    if (keepFirstStep && drawStepList.length === 1) {
      return;
    }

    if (drawStepList.length === 0) {
      return;
    }

    const source = drawModify.source_;

    // 最后一个既是当前状态/删除最后一个
    const lastFeature = drawStepList.pop();
    const lastFeatureId = lastFeature.featureId;
    const index = drawStepList.findIndex((i) => i.featureId === lastFeatureId);
    // 如果当前状态Feature已经没有之前的状态的话就删除
    if (index === -1) {
      source.removeFeature(source.getFeatureById(lastFeatureId));
      return;
    }

    // 倒数第二个是上一步状态
    const beforeFeature = drawStepList[drawStepList.length - 1];
    const { featureId, geometry } = beforeFeature;
    const resetFeature = source.getFeatureById(featureId);
    resetFeature.setGeometry(geometry.clone());

    drawModify.pathChange(resetFeature, false);
  }

  // 取消绘制
  undraw() {
    const map = thirdMaps.get(this.mapId);
    map.removeInteraction(this.editDrawer);
  }

  /**
   * 更新点位
   *@param {string} type 操作类型 add | edit
   * @param {*} text 定位
   */
  async upDataAddressMarker(type, text, image) {
    const { marker } = this.markers.get(type);

    imageToBase64(image).then((res) => {
      const parent = document.createElement('div');
      const instance = useCreateApp(KindMarker, { image: res, text });
      const el = instance.mount(parent).$el;
      const src = domConvertsToBase64(el, 400, 100);

      // 创建图片样式
      const iconStyle = new Style({
        image: new Icon({
          anchor: [0.5, 0.4],
          src, // 替换为你的图片 URL
        }),
      });

      // 设置特征样式
      marker.setStyle(iconStyle);
    });

    this.drawLayer.forEach((i) => {
      if (i.get('layerId')) {
        i.getSource()
          .getFeatures()
          .forEach((f) => {
            i.getSource().removeFeature(f);
          });
      }
    });
  }

  /**
   * 更新/新增/删除图层
   * @param {string} layerId 图层id
   * @param {addList, deleteList, updateList} options
   * @returns
   */
  updateLayer(layerId, options) {
    const { addList = [], deleteList = [], updateList = [] } = options;
    const { center, mapId } = this;
    const map = thirdMaps.get(mapId);
    if (!map) return;
    const layer = this.layers.get(layerId);
    if (!layer) return;
    const { source, sourceMap } = layer;

    // 删除
    if (deleteList?.length > 0) {
      deleteList.forEach((item) => {
        // const feature = source.getFeatureById(item.id)
        // source.removeFeature(feature)
        sourceMap.delete(item.id);
      });
    }
    // 新增
    const addFeatures: Feature[] = [];
    if (addList?.length > 0) {
      addList.forEach((item) => {
        const { position } = item;
        const feature = new Feature({
          geometry: new Point(formatLonLat(position || center)),
          layerId,
          type: 'icon',
        });
        feature.setId(item.id);
        sourceMap.set(item.id, item);
        addFeatures.push(feature);
      });
    }
    // 更新
    const updateFeatures: Feature[] = [];
    if (updateList?.length > 0) {
      updateList.forEach((item) => {
        const feature = item.id ? source.getFeatureById(item.id) : null;
        if (!feature) {
          return;
        }
        const { position } = item;
        const newFeature = new Feature({
          geometry: new Point(formatLonLat(position || center)),
          layerId,
          type: 'icon',
        });
        newFeature.setId(item.id);
        sourceMap.set(item.id, item);
        updateFeatures.push(newFeature);
        sourceMap.set(item.id, item);
      });
    }

    // 刷新数据源将清空所有数据，所以以上不用再做删除操作（减少了dom操作）
    source.clear();
    source.refresh();
    source.addFeatures([...addFeatures, ...updateFeatures]);
  }
}

/**
 * @description Track 动向
 * @class Track
 */
class _Track extends BaseTrack {
  currentIndex = 0;
  moveMarker: any;
  movePosition: any;
  originData: any;
  playIndex = 0;
  playTimer: any;
  routeMarker: any;
  vectorLayer: any;
  vm: any;

  constructor(config) {
    super(config);
    this.originData = [...this.data];
    this.data = this.data.map((item) => formatLonLat(item));
    this.initTrack();
  }

  // 运动
  animation() {
    const { currentIndex, move, originData } = this;

    if (originData.length <= 1) {
      return;
    }

    if (currentIndex === originData.length - 1) {
      this.changeStatus('pause');
      return;
    }

    const p1 = originData[currentIndex];
    const p2 = originData[++this.currentIndex];
    const points = this.interpolatePoint(p1, p2);
    const angle = this.getRotation(p1, p2);

    this.playIndex = 0;
    this.playInterpolation(points, angle);

    if (move) {
      move(this.currentIndex);
    }
  }

  changeStatus(status) {
    this.status = status;
    this.statusChange?.(status);
  }

  // 销毁动向
  destroyTrack() {
    const { map, vectorLayer } = this;
    const olmap = thirdMaps.get(map.mapId);

    this.pause();
    olmap.removeLayer(vectorLayer);
  }

  // 获取角度
  getRotation(start, end) {
    if (!start || !end) {
      return 0;
    }
    const dx = end[0] - start[0];
    const dy = end[1] - start[1];
    const rotation = Math.atan2(dy, dx);
    return -rotation;
  }

  // 初始化动向
  initTrack() {
    const map = thirdMaps.get(this.map.mapId);
    const { color, data, endImage, image, startImage } = this;
    const routeMarker = new Feature({
      geometry: new LineString(data),
      type: 'route',
    });
    const startMarker = new Feature({
      geometry: new Point(data[0]),
      type: 'start',
    });
    const endMarker = new Feature({
      geometry: new Point(data[data.length - 1]),
      type: 'end',
    });
    const position = startMarker.getGeometry()?.clone();
    const moveMarker = new Feature({
      geometry: position,
      type: 'move',
    });
    const styles = {
      end: {
        image: new Icon({
          src: endImage,
        }),
      },
      move: {
        image: new Icon({
          rotation: this.getRotation(data[0], data[1]),
          src: image,
        }),
      },
      route: {
        stroke: new Stroke({
          color,
          width: 2,
        }),
      },
      start: {
        image: new Icon({
          src: startImage,
        }),
      },
    };
    const vectorLayer = new VectorLayer({
      source: new VectorSource({
        features: [routeMarker, startMarker, endMarker, moveMarker],
      }),
      style: (feature) => {
        return new Style(styles[feature.get('type')]);
      },
    });

    // 居中展示
    const extent = boundingExtent(routeMarker.getGeometry()?.getCoordinates() as any);
    map.getView().fit(extent);

    this.vectorLayer = vectorLayer;
    this.routeMarker = routeMarker;
    this.moveMarker = moveMarker;
    this.movePosition = position;
    map.addLayer(vectorLayer);
  }

  // 两个点之间插值 - 根据两点之间的距离来插值
  interpolatePoint(point1, point2) {
    const kilometers = Turf.rhumbDistance(point1, point2, {
      units: 'kilometers',
    });
    const distance = kilometers * 100;

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
        const midpoint = Turf.midpoint(point, points[index + 1]);
        arr.push(point, midpoint.geometry.coordinates);
      });
      route = arr;
    };

    while (route.length <= distance) {
      getMid(route);
    }

    return route.map((i) => formatLonLat(i as Position));
  }

  // 暂停
  pause() {
    if (this.data.length <= 1) {
      return;
    }
    this.changeStatus('pause');
  }

  // 播放两个点之间的插值
  playInterpolation(points, angle) {
    const { image, playIndex, playTimer, speed, status } = this;

    if (playTimer) {
      clearTimeout(playTimer);
    }

    this.playTimer = setTimeout(
      () => {
        if (status === 'destroy') {
          return;
        }

        if (status === 'pause') {
          this.playInterpolation(points, angle);
          return;
        }

        if (playIndex < points.length - 1) {
          const coordinates = points[++this.playIndex];
          const s = new Style({
            image: new Icon({
              rotation: angle,
              src: image,
            }),
          });
          this.moveMarker.setStyle(s);
          this.moveMarker.getGeometry().setCoordinates(coordinates);
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
    const { color } = this;
    const style = new Style({
      stroke: new Stroke({ color, width: 2 }),
    });
    this.routeMarker.setStyle(style);
  }

  // 从新开始
  reStart() {
    if (this.data.length <= 1) {
      return;
    }
    this.currentIndex = 0;
    this.start();
  }

  // 设置高亮
  setHighlight() {
    const { color } = this;
    const style = new Style({
      stroke: new Stroke({ color, width: 6 }),
    });
    this.routeMarker.setStyle(style);
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
    this.changeStatus('start');

    const { currentIndex, data, playIndex } = this;
    if (data.length <= 1) {
      return;
    }

    if (currentIndex === data.length - 1) {
      this.currentIndex = 0;
    }

    if (playIndex === 0) {
      this.animation();
    }
  }
}

class _TrackRealTime extends BaseTrack {
  endMarker: any;
  routeMarker: any;
  vectorLayer: any;

  constructor(config) {
    super(config);
    this.data = this.data.map((item) => formatLonLat(item));
    this.initTrackRealTime();
  }

  // 销毁轨迹跟踪
  destroyRealTimeTrack() {
    const { map, vectorLayer } = this;
    const olmap = thirdMaps.get(map.mapId);
    olmap.removeLayer(vectorLayer);
  }

  endData(img) {
    const style = new Style({
      image: new Icon({
        src: img,
      }),
    });
    this.endMarker.setStyle(style);
  }

  // 初始化动向
  initTrackRealTime() {
    const map = thirdMaps.get(this.map.mapId);
    const { color, data, endImage, startImage } = this;
    const routeMarker = new Feature({
      geometry: new LineString(data),
      type: 'route',
    });
    const startMarker = new Feature({
      geometry: new Point(data[0]),
      type: 'start',
    });
    const endMarker = new Feature({
      geometry: new Point(data[data.length - 1]),
      type: 'end',
    });
    const styles = {
      end: {
        image: new Icon({
          src: endImage,
        }),
      },
      route: {
        stroke: new Stroke({
          color,
          width: 2,
        }),
      },
      start: {
        image: new Icon({
          src: startImage,
        }),
      },
    };
    const vectorLayer = new VectorLayer({
      source: new VectorSource({
        features: [routeMarker, startMarker, endMarker],
      }),
      style: (feature) => {
        return new Style(styles[feature.get('type')]);
      },
    });

    // 居中展示
    const extent = boundingExtent(routeMarker.getGeometry()?.getCoordinates() as any);
    map.getView().fit(extent);

    this.vectorLayer = vectorLayer;
    this.routeMarker = routeMarker;
    this.endMarker = endMarker;
    vectorLayer.setZIndex(1);
    map.addLayer(vectorLayer);
  }

  updateData(inputData) {
    const map = thirdMaps.get(this.map.mapId);
    this.data = inputData.map((item) => formatLonLat(item));
    this.routeMarker.setGeometry(new LineString(this.data));
    this.endMarker.setGeometry(new Point(this.data[this.data.length - 1]));
    map.render();
  }
}

export default _OpenLayers;
