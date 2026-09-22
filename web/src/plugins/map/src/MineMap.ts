/**
 * 文档
 * https://minedata.cn/nce-support/demoCenter?activePath=page-map-show
 *
 * 官网 https://minedata.cn/nce-support/helpAndSupport/Common-Questions?type=development
 * 目前 MineData 默认输出的地图坐标系是 GCJ-02 坐标系（火星坐标），符合国测局规定。也支持 WGS84 坐标系的定制输出。
 */

import speedImg from '@/assets/images/marker/speed_marker.png';
import vehicleBgImg from '@/assets/images/marker/vehicle_marker_bg.png';
import { useCreateApp, useSetInterval } from '@/hooks';
import CursorList from '@/pages/map/cursorList.vue';
import MapPopup from '@/pages/map/mapPopup.vue';
import KindMarker from '@/pages/map/marker/kindMarker.vue';
import { useResourceStore } from '@/store';
import { colorRGBtoHex, guid } from '@/utils';
import { isArray, isNumber } from '@/utils/is';

import * as Turf from '@turf/turf';
import axios from 'axios';
import { cloneDeep } from 'lodash-es';

import { mapManager } from '../EMap';
import {
  BaseMap,
  BaseTrack,
  defaultLayerOptions,
  drawCallback,
  formatLonLat,
  getLayerColor,
  getMapConfig,
  mapIsReady,
  pointsWithinPolygon,
  thirdMaps,
} from '../helper';
import { coordinateTransform } from '../transformLonlat';

const loadImageList: string[] = [];

function _MineMap(config) {
  let minemap: any = null;
  const mapKey = '';
  let turf: any = null;
  let minemaputil: any = null;
  let posArray: any[] = [];

  /**
   * @description 四维图新地图封装
   * @class _MineMap
   */
  class _MineMap extends BaseMap {
    clearTimer: any = null;
    deleteArea: any;
    drawStepList: any[] = [];
    edit: any;
    interactiveEvent: string[] = [];
    offAddPopupClick: any;
    offTextClick: any;
    updateArea: any;
    vehicleMarkers: any[] = [];

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
     * 添加点位
     * @param {*} image 图片
     * @param {string} type 操作类型 add | edit | look
     * @param {*} position 定位
     * @param {*} callback 回调函数
     */
    addAddressMarker(type, image, config) {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      if (map.getCanvas().style.cursor) return;

      const { callback, position } = config;
      const draggable = type !== 'look';

      const oldMarker = this.markers.get(type);
      oldMarker?.remove();

      const parent = document.createElement('div');
      const instance = useCreateApp(KindMarker, { image });
      const el = instance.mount(parent).$el;

      const change = async (lngLat) => {
        const { lat, lng } = lngLat;
        const location = coordinateTransform([lng, lat], true).join(',');
        const { formatted_address } = await this.queryAddressByLocation(location);
        if (formatted_address) {
          this.updateAddressMarker(type, formatted_address);
          callback?.(location, formatted_address);
        }
      };

      const addMarker = (lngLat) => {
        const _marker = new minemap.Marker({ draggable, element: el, offset: [-16, -32] })
          .setLngLat(lngLat)
          .addTo(map);
        map.setCenter(lngLat);
        map.getCanvas().style.cursor = '';
        this.markers.set(type, _marker);

        // 图标拖拽
        _marker.on('dragend', (e) => {
          change(e.target._lngLat);
        });
        change(_marker._lngLat);
      };

      const clickMap = (e) => {
        addMarker(e.lngLat);
      };

      if (position) {
        const lngLat = coordinateTransform(position);
        addMarker(lngLat);
      } else {
        const canvas = map.getCanvas();
        canvas.style.cursor = `url(${image}) 16 32, auto`;
        map.once('click', clickMap);
        this.markers.set('listen', clickMap);
      }
    }

    /**
     * 添加多边形/矩形图层
     * @param {string} layerId 图层id
     * @param {{path,style}[]} config
     */
    addAreaLayer(config) {
      const { drawLayer, mapId } = this;
      const { highlight, layerId, path, style } = config;
      const map = thirdMaps.get(mapId);
      const uid = guid();
      const _layerId = layerId ? `${layerId}-${uid}` : `area-${uid}`;
      const areaSource = {
        data: {
          geometry: {
            coordinates: [path],
            type: 'Polygon',
          },
          type: 'Feature',
        },
        type: 'geojson',
      };
      const layer = {
        id: _layerId,
        paint: {
          'fill-color': style.fillColor,
          'fill-opacity': style.fillOpacity,
          'fill-outline-color': style.fillOutlineColor,
        },
        source: areaSource,
        type: 'fill',
      };

      map.addLayer(layer);
      drawLayer.push({ highlight, layerId: _layerId });
      this.layers.set(_layerId, { ...config });
    }

    /**
     *  @description 添加最底部图层
     */
    addBottomLayer(map) {
      map.addSource('bottom-layer', {
        cluster: false,
        data: {
          features: [],
          type: 'FeatureCollection',
        },
        type: 'geojson',
      });
      map.addLayer({
        id: 'bottom-layer',
        source: 'bottom-layer',
        type: 'symbol',
      });
    }

    /**
     * 添加圈层防控
     * @param {*} image 图片
     * @param {string} type 操作类型 add | edit | look
     * @param {*} position 定位
     * @param {*} callback 回调函数
     */
    addCircleControlMarker(type, image, config) {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      if (map.getCanvas().style.cursor) {
        return;
      }

      const { callback, draggable, onlyRemove, position } = config;

      const oldMarker = this.markers.get(type);
      oldMarker?.remove();
      if (onlyRemove) {
        return;
      }

      const parent = document.createElement('div');
      const instance = useCreateApp(KindMarker, { image });
      const el = instance.mount(parent).$el;

      const change = async (lngLat) => {
        const { lat, lng } = lngLat;
        const location = coordinateTransform([lng, lat], true).join(',');
        const { formatted_address = '成都天府广场' } = await this.queryAddressByLocation(location);
        if (formatted_address) {
          // this.addMarkerCircleControlPopup(type, windowTemplate, formatted_address);
          this.updateAddressMarker(type, formatted_address);
          callback?.(location, formatted_address);
        }
      };

      const addMarker = (lngLat) => {
        const _marker = new minemap.Marker({ draggable, element: el, offset: [-19, -38] })
          .setLngLat(lngLat)
          .addTo(map);
        map.setCenter(lngLat);
        map.getCanvas().style.cursor = '';
        this.markers.set(type, _marker);

        // 图标拖拽
        _marker.on('dragend', (e) => {
          change(e.target._lngLat);
        });
        change(_marker._lngLat);
      };

      if (position) {
        const lngLat = coordinateTransform(position);
        addMarker(lngLat);
      } else {
        map.getCanvas().style.cursor = `url("${image}"),default`;
        map.once('click', (e) => {
          addMarker(e.lngLat);
        });
      }
    }

    /**
     * 添加圆形图层
     * @param {string} layerId 图层id
     * @param {{center, radius,style}[]} config
     */
    addCircleLayer(config) {
      const { drawLayer, mapId } = this;
      const { center, circleControlLayerId, highlight, radius, style } = config;
      const map = thirdMaps.get(mapId);
      let layerId = `circle-${guid()}`;
      const geojson = this.createGeoJSONCircle(center, radius);
      // 圈层防控始终只有一个圈层图层进行覆盖
      if (circleControlLayerId) {
        layerId = circleControlLayerId;
        // 记录生成圆形点位信息
        posArray = geojson.data.features[0].geometry.coordinates;
      }
      map.addSource(layerId, geojson);

      const layer = {
        id: layerId,
        paint: {
          'fill-color': style.fillColor,
          'fill-opacity': style.fillOpacity,
          'fill-outline-color': style.fillOutlineColor,
        },
        source: layerId,
        type: 'fill',
      };
      map.addLayer(layer);
      drawLayer.push({ highlight, layerId });
      this.layers.set(layerId, { ...config });
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
      el.id = 'marker';
      el.style['background-image'] = `url(${url})`;
      el.style['background-size'] = 'cover';
      el.style['background-repeat'] = 'no-repeat';
      el.style.width = '32px';
      el.style.height = '32px';

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
          const lngLat: any = [data.lon, data.lat];
          this.markerPopup(createEl(data), formatLonLat(lngLat), [12, -16]);
        }, 300);
      });
      const marker = new minemap.Marker(el, { offset: [0, -16] });
      marker.setLngLat({ lat: data.lat, lon: data.lon }).addTo(map);
      this.markers.set(layerId + data.id, marker);
    }

    /**
     * 生成可编辑图形
     * @param {point | polyline | rectangle | circle | polygon} type 点、线、矩形、圆、多边形
     * @param {Array} path
     * @param {Object} style
     * @param {Object} config
     */
    addFeatures(type, _path, style, config) {
      const { id, radius } = config;
      const path = ['icon', 'point', 'text'].includes(type)
        ? coordinateTransform(_path)
        : _path?.map((i) => coordinateTransform(i));
      const {
        fillColor,
        fillOpacity,
        fillOutlineColor,
        fillOutlineDasharray,
        fillOutlineOpacity,
        fillOutlineWidth,
        imageName,
        textSize,
        textValue,
      } = style;
      let feature: any;
      switch (type) {
        case 'circle': {
          const center = coordinateTransform(config.center);
          const geojson = this.createGeoJSONCircle(center, radius);
          feature = Object.assign(geojson.data.features[0], {
            properties: {
              center,
              custom_style: 'true',
              feature_type: 'circle',
              fillColor,
              fillOpacity,
              fillOutlineColor,
              fillOutlineDasharray,
              fillOutlineOpacity,
              fillOutlineWidth,
              radius,
            },
          });
          break;
        }
        case 'icon': {
          feature = {
            geometry: {
              coordinates: path,
              type: 'Point',
            },
            properties: {
              custom_style: 'true',
              feature_type: 'icon',
              iconImage: imageName,
              iconSize: 0.7,
            },
            type: 'Feature',
          };
          break;
        }
        case 'line_arrow':
        case 'polyline': {
          feature = {
            geometry: {
              coordinates: path,
              type: 'LineString',
            },
            properties: {
              custom_style: 'true',
              feature_type: type,
              lineColor: fillColor,
              lineDasharray: fillOutlineDasharray,
              lineOpacity: fillOpacity,
              lineWidth: fillOutlineWidth,
            },
            type: 'Feature',
          };
          break;
        }
        case 'point': {
          feature = {
            geometry: {
              coordinates: path,
              type: 'Point',
            },
            properties: {
              circleBorderColor: fillOutlineColor,
              circleBorderOpacity: fillOutlineOpacity,
              circleColor: fillColor,
              circleOpacity: fillOpacity,
              custom_style: 'true',
              feature_type: type,
              fillOutlineWidth,
            },
            type: 'Feature',
          };
          break;
        }
        case 'polygon':
        case 'rectangle': {
          feature = {
            geometry: {
              coordinates: [path],
              type: 'Polygon',
            },
            properties: {
              custom_style: 'true',
              feature_type: type,
              fillColor,
              fillOpacity,
              fillOutlineColor,
              fillOutlineDasharray,
              fillOutlineOpacity,
              fillOutlineWidth,
            },
            type: 'Feature',
          };
          break;
        }
        case 'text': {
          feature = {
            geometry: {
              coordinates: path,
              type: 'Point',
            },
            properties: {
              custom_style: 'true',
              feature_type: 'text',
              textColor: fillOutlineColor,
              textField: textValue,
              textSize,
            },
            type: 'Feature',
          };
          break;
        }
      }
      if (id) {
        feature.id = id;
      }
      this.edit.draw?.add(feature);
    }

    // 添加icon图层
    addIconLayer(config) {
      const { drawLayer, mapId } = this;
      const { path, style } = config;
      const map = thirdMaps.get(mapId);
      const layerId = `icon-${guid()}`;
      const layer = {
        id: layerId,
        layout: {
          'icon-image': style.imageName,
          'icon-size': 0.5,
        },
        source: {
          data: {
            features: [
              {
                geometry: {
                  coordinates: path,
                  type: 'Point',
                },
                type: 'Feature',
              },
            ],
            type: 'FeatureCollection',
          },
          type: 'geojson',
        },
        type: 'symbol',
      };
      map.addLayer(layer);
      drawLayer.push({ layerId });
      this.layers.set(layerId, { ...config });
    }

    /** 添加自定义图片icon
     * @param {string} iconName icon名称 具有唯一性
     * @param {string} image 图片路径
     */
    addImageIcon(iconName, image) {
      const map = thirdMaps.get(this.mapId);
      if (!map || map?.hasImage(iconName)) {
        return;
      }

      if (loadImageList.includes(iconName)) {
        return;
      }

      loadImageList.push(iconName);

      map.loadImage(image, (error, img) => {
        if (error) throw error;
        map.addImage(iconName, img);
      });
    }

    /**
     * 添加文字图层
     * @param {string} layerId 图层id
     * @param {{text, position, color}[]} data
     */
    addLabelLayer(config) {
      const { data, layerId } = config;
      if (data.length === 0) return;
      const textColor: any = [];
      data.forEach((item) => {
        const stop = [item.text, item.color];
        textColor.push(stop);
      });

      const map = thirdMaps.get(this.mapId);
      const sourceMap = new Map();
      const jsonData = this.getJsonData(layerId, sourceMap, data);
      map.addSource(layerId, {
        data: jsonData,
        type: 'geojson',
      });

      const layer = {
        id: layerId,
        layout: {
          'text-field': '{title}',
          'text-size': 14,
        },
        paint: {
          'text-color': {
            property: 'title',
            stops: textColor,
            type: 'categorical',
          },
        },
        source: layerId,
        type: 'symbol',
      };
      map.addLayer(layer);
      this.layers.set(layerId, { ...config });
    }

    /**
     * @description 添加图层
     * @param {array} data marker点数据
     * @param {string} layerId 图层id
     * @param {string} imgPath 图片地址
     * @param {string} image 点图片
     * @param {string} clusterImage 聚合点图片
     * @param {*} windowTemplate 弹窗模板
     * @param {boolean} isShow 是否显示 默认显示
     * @param {boolean} isCluster 是否聚合
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
        image,
        isCluster = true,
        isShow = true,
        layerId,
        renderer,
      } = options;
      const { criZoom, mapId } = this;
      const map = thirdMaps.get(mapId);
      renderer?.(null, { map, mapType: 'mineMap' });
      const sourceMap = new Map();
      const jsonData = this.getJsonData(layerId, sourceMap, data);
      const clusterId = `${layerId}-cluster`;

      map.addSource(clusterId, {
        cluster: true,
        data: jsonData,
        type: 'geojson',
      });

      // 非聚合(脉冲点)
      const color = getLayerColor(layerId);
      if (color) {
        map.addSource(layerId, {
          cluster: false,
          data: jsonData,
          type: 'geojson',
        });

        map.addLayer({
          id: layerId,
          layout: {
            visibility: isShow ? 'visible' : 'none',
          },
          maxzoom: criZoom,
          paint: {
            'circle-color': color,
            'circle-radius': 3,
          },
          source: layerId,
          type: 'circle',
        });
      }

      // 非聚合(图标点)
      map.loadImage(image, (error, img) => {
        if (error) {
          throw error;
        }
        if (!map.hasImage(layerId)) {
          map.addImage(layerId, img);
        }

        map.addLayer({
          filter: ['!has', 'point_count'],
          id: `${layerId}-point`,
          layout: {
            'icon-allow-overlap': true, // 图标允许重叠
            'icon-image': '{imageId}',
            'icon-size': 1,
            'text-allow-overlap': true, // 文本允许重叠
            'text-anchor': 'top',
            'text-field': '{newName}',
            'text-line-height': 1,
            'text-offset': [0, 1.5],
            'text-size': 12,
            visibility: isShow ? 'visible' : 'none',
          },
          minzoom: criZoom,
          paint: {
            'text-color': '#ffffff',
            'text-halo-color': {
              default: clusterSymple?.color || '#3299E2',
              property: 'attendance',
              stops: [
                [1, '#7ec8ff'],
                [2, '#8d8e8e'],
                [3, '#fca701'],
              ],
              type: 'categorical',
            },
            'text-halo-width': 4,
          },
          source: clusterId,
          type: 'symbol',
        });

        this.addSpeedLayer(layerId, isShow);
      });

      // 聚合(图标点)
      if (isCluster) {
        map.loadImage(clusterImage, (error, img) => {
          if (error) {
            throw error;
          }
          if (!map.hasImage(clusterId)) {
            map.addImage(clusterId, img);
          }
          map.addLayer({
            filter: ['has', 'point_count'],
            id: clusterId,
            layout: {
              'icon-image': clusterId,
              'icon-size': 1,
              'text-anchor': 'bottom',
              'text-field': '{point_count}',
              'text-offset': [0, 0.1],
              'text-size': 12,
              visibility: isShow ? 'visible' : 'none',
            },
            minzoom: criZoom,
            paint: {
              'text-color': '#ffffff',
            },
            source: clusterId,
            type: 'symbol',
          });
        });
      }

      this.layers.set(layerId, { sourceMap, ...options });
    }

    /**
     * 添加线图层
     * @param {{layerId,path, style}[]} config
     */
    addLineLayer(config) {
      const { drawLayer, mapId } = this;
      const { highlight, layerId, lineStyle, path, style } = config;
      const map = thirdMaps.get(mapId);
      const _layerId = layerId || `line-${guid()}`;
      const dashed = lineStyle === 'dashed';
      const linePaint = {
        'line-color': style.fillOutlineColor,
        'line-width': style.fillOutlineWidth,
      };
      if (dashed) {
        linePaint['line-dasharray'] = [0.2, 2];
      }

      const layer = {
        id: _layerId,
        layout: {
          'line-cap': 'round',
          'line-join': 'round',
        },
        paint: linePaint,
        source: {
          data: {
            geometry: {
              coordinates: path.map((i) => coordinateTransform(i)),
              type: 'LineString',
            },
            type: 'Feature',
          },
          type: 'geojson',
        },
        type: 'line',
      };
      map.addLayer(layer);
      drawLayer.push({ highlight, layerId: _layerId });
      this.layers.set(_layerId, { ...config });
    }

    /**
     * 添加marker点
     * @param {String} url 图片
     * @param {Object} data 点数据
     * @param {Object} config
     * @param {Boolean} draggable 是否允许拖拽 false
     */
    async addMarkerAndPopup(url, data, config, draggable?) {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      const oldMark = this.markers.get(data.id);
      if (oldMark) {
        return;
      }
      const { position } = config;
      const lngLat = formatLonLat(position);

      const el = document.createElement('div');
      el.style.backgroundImage = `url(${url})`;
      el.style.width = '43px';
      el.style.height = '49px';

      const _marker = new minemap.Marker({
        anchor: 'bottom',
        draggable,
        element: el,
        offset: [-2, 10],
      })
        .setLngLat(lngLat)
        .addTo(map);
      this.markers.set(data.id, _marker);
      this.clickMarker(el, data, config);
    }

    /**
     * @description 添加圈层防控点弹窗
     * @param {String} layerId 图层id
     * @param {Object} windowTemplate 对应marker点弹窗
     * @param {Object} data 对应marker点数据
     */
    async addMarkerCircleControlPopup(type, windowTemplate, data) {
      const { mapId } = this;
      const parent = document.createElement('div');
      const instance = useCreateApp(MapPopup, {
        data,
        layerId: 'searchResource',
        mapId,
        template: windowTemplate,
      });
      const el = instance.mount(parent).$el;

      const _popup = new minemap.Popup({
        closeButton: false,
        closeOnClick: false,
        offset: [-5, -38],
      }).setDOMContent(el);

      const marker = this.markers.get(type);
      marker.setPopup(_popup).togglePopup();
    }

    /**
     * @description 添加marker点弹窗
     * @param {String} layerId 图层id
     * @param {Object} source 对应marker点数据
     */
    async addMarkerCustomPopup(layerId, source) {
      const { center, mapId } = this;
      const { id } = source;
      const config = this.layers.get(layerId);

      if (!config) {
        return;
      }

      const { sourceMap, windowTemplate } = this.layers.get(layerId);
      let data = sourceMap.get(id);

      if (!data) {
        // this.updateLayer(layerId, { addList: [source] });
        // await delay(500);
        data = { ...source, position: coordinateTransform(source.position) };
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

      this.markerPopup(instance.mount(parent).$el, position);
    }

    /**
     * 添加保障点位
     * @param {*} url 图片
     * @param {*} position 定位
     */
    addPlanMarker(url, position) {
      const oldMarker = this.markers.get('plan');
      oldMarker?.remove();

      const map = thirdMaps.get(this.mapId);
      const el = document.createElement('div');
      el.id = 'marker';
      el.style['background-image'] = `url(${url})`;
      el.style['background-size'] = 'cover';
      el.style.width = '50px';
      el.style.height = '86px';

      const marker = new minemap.Marker(el, { offset: [-25, -86] });
      marker.setLngLat(position).addTo(map);
      this.markers.set('plan', marker);
    }

    // 添加点图层
    addPointLayer(config) {
      const { drawLayer, mapId } = this;
      const { path, style } = config;
      const map = thirdMaps.get(mapId);
      const layerId = `point-${guid()}`;
      const layer = {
        id: layerId,
        paint: {
          'circle-color': style.fillColor,
          'circle-opacity': style.fillOpacity,
        },
        source: {
          data: {
            geometry: {
              coordinates: path,
              type: 'Point',
            },
            type: 'Feature',
          },
          type: 'geojson',
        },
        type: 'circle',
      };
      map.addLayer(layer);
      drawLayer.push({ layerId });
      this.layers.set(layerId, { ...config });
    }

    // 双击结束提示框
    addPopup(type) {
      const map = thirdMaps.get(this.mapId);
      this.offAddPopupClick?.();
      let lngLatData: any[] = [];
      let popup1 = null;
      const popupsData: any[] = [];
      const add = () => {
        popup1 = new minemap.Popup({
          anchor: 'bottom',
          closeButton: true,
          closeOnClick: true,
          minWidth: '205px',
          offset: [0, 0],
        })
          .setLngLat(lngLatData[lngLatData.length - 1])
          .setText('双击鼠标右键结束绘制！')
          .addTo(map);
        popupsData.push(popup1);
      };
      const clickMap = (event) => {
        lngLatData.push([event.lngLat.lng, event.lngLat.lat]);
        if (type === 'line' && lngLatData.length > 1) {
          add();
        } else if (type === 'polygon' && lngLatData.length > 2) {
          add();
        }
      };
      const offClick = () => {
        lngLatData = [];
        map.off('click', clickMap);
        popup1 = null;
        for (const popupsDatum of popupsData) {
          popupsDatum.remove();
        }
      };
      if (type === 'line' || type === 'polygon') {
        map.on('click', clickMap);
        map.on('dblclick', offClick);
        this.offAddPopupClick = () => {
          map.off('click', clickMap);
        };
      } else {
        map.off('click', clickMap);
      }
    }

    /**
     * 测量工具
     * @param {*} type 框选类型 测距0 测面1
     */
    async addRangeTool(type) {
      const map = thirdMaps.get(this.mapId);
      const tool = new minemaputil.RangingTool(map, {
        color: '#1AFFFB',
        decimals: 2,
        type,
        unit: 'km',
      });
      tool.turnOn();
    }

    /**
     *  @description 添加卫星影像底图
     */
    addRasterLayer() {
      const { mapId, zooms } = this;
      const map = thirdMaps.get(mapId);
      const layerId = 'raster';
      const source = {
        tiles: [getMapConfig().rasterUrl],
        tileSize: 256,
        type: 'raster',
      };

      if (map.getLayer(layerId)) {
        return;
      }

      this.clearVectorStyle();

      map.addSource(layerId, source);
      map.addLayer({
        id: layerId,
        maxZoom: zooms[1],
        minZoom: zooms[0],
        source: layerId,
        type: 'raster',
      });

      if (map.getLayer('bottom-layer')) {
        map.moveLayer(layerId, 'bottom-layer');
      }
    }

    /**
     * @description 添加比例尺以及鼠标移动显示鼠标经纬度
     * @param {*} scaleContainer 比例尺容器
     * @param {*} positionContainer 经纬度容器
     */
    addScaleAndPosition(scaleContainer, positionContainer) {
      const map = thirdMaps.get(this.mapId);
      const scale = new minemap.Scale();

      const setScale = () => {
        const { innerText } = scale._container;
        scaleContainer.innerHTML = innerText.replace('公里', 'km').replace('米', 'm');
      };

      map.on('mousemove', (e) => {
        const { lat, lng } = e.lngLat;
        positionContainer.innerHTML = `${lng.toFixed(6)}，${lat.toFixed(6)}`;
      });

      map.on('zoom', setScale);

      map.addControl(scale, 'bottom-right');

      setScale();
    }

    /**
     * @description 添加车速图层
     * @param {string} layerId 图层id
     * @param {boolean} isShow 是否显示
     */
    addSpeedLayer(layerId, isShow) {
      if (!layerId.includes('carPhoto')) return;

      const { criZoom, mapId } = this;
      const map = thirdMaps.get(mapId);
      const speedLayerId = `${layerId}-speed`;

      const createLayer = () => {
        map.addLayer({
          filter: ['!has', 'point_count'],
          id: speedLayerId,
          layout: {
            'icon-allow-overlap': true,
            'icon-image': speedLayerId,
            'icon-offset': [0, -34],
            'icon-size': 1,
            'text-allow-overlap': true,
            'text-anchor': 'top',
            'text-field': '{speed}',
            'text-line-height': 1,
            'text-offset': [0, -3],
            'text-size': 14,
            visibility: isShow ? 'visible' : 'none',
          },
          minzoom: criZoom,
          paint: {
            'text-color': '#ffffff',
          },
          source: `${layerId}-cluster`,
          type: 'symbol',
        });
      };

      if (map.hasImage(speedLayerId)) {
        createLayer();
      } else {
        map.loadImage(speedImg, (error, img) => {
          if (error) throw error;
          map.addImage(speedLayerId, img);
          createLayer();
        });
      }
    }

    // 文字图层---单个
    addTextLayer(config) {
      const { path, style } = config;
      const map = thirdMaps.get(this.mapId);
      const uid = guid();
      const layer = {
        id: uid,
        layout: {
          'text-field': '{title}',
          'text-size': style.textSize,
        },
        paint: {
          'text-color': {
            property: 'kind',
            stops: [['text', style.fillOutlineColor]],
            type: 'categorical',
          },
        },
        source: {
          data: {
            geometry: {
              coordinates: path,
              type: 'Point',
            },
            properties: {
              kind: 'text',
              title: style.textValue,
            },
            type: 'Feature',
          },
          type: 'geojson',
        },
        type: 'symbol',
      };
      map.addLayer(layer);
    }

    /**
     * @description 添加路况图层
     */
    addTrafficLayer() {
      const map = thirdMaps.get(this.mapId);
      const time = Number(getMapConfig().trafficTime);

      // 定时更新
      this.clearTimer?.();
      this.clearTimer = useSetInterval(
        () => {
          this.updateTrafficSource();
        },
        time * 60 * 1000,
        true,
      );

      map.addLayer({
        id: 'Traffic',
        layout: {
          'line-cap': 'round',
          'line-join': 'round',
        },
        paint: {
          'line-color': {
            property: 'status',
            stops: [
              [0, '#999999'],
              [1, '#66cc00'],
              [2, '#ff9900'],
              [3, '#cc0000'],
              [4, '#9d0404'],
            ],
          },
          'line-width': {
            base: 1.2,
            stops: [
              [5, 1],
              [18, 3],
            ],
          },
        },
        source: 'Traffic',
        'source-layer': 'Trafficrtic',
        type: 'line',
      });
    }

    /**
     * 添加车辆幻化图标
     * @param data
     */
    addVehicleMarker(data) {
      const { direction = 0.25, id, position } = data;
      const index = this.vehicleMarkers.findIndex((i) => i.id === id);

      const p = coordinateTransform(position);
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

        const inner = document.createElement('div');
        inner.style['background-image'] = `url(${data.iconUrl})`;
        inner.style['background-size'] = 'contain';
        inner.style['background-repeat'] = 'no-repeat';
        inner.style['background-position'] = 'center';
        inner.style.width = '46px';
        inner.style.height = '88px';
        el.append(inner);

        const map = thirdMaps.get(this.mapId);
        const marker = new minemap.Marker(el, { offset: [-60, -60] })
          .setLngLat(p)
          .setRotation((direction - 0.5) * 360)
          .addTo(map);
        this.vehicleMarkers.push({
          ...data,
          marker,
        });

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
        const { iconUrl, marker } = target;
        if (iconUrl !== data.iconUrl) {
          marker.getElement().childNodes[0].style['background-image'] = `url(${data.iconUrl})`;
        }
        marker.setLngLat(p);
        Object.assign(target, data);
      }
    }

    /**
     * @description  围栏
     * @param {*} type 类型 多边形polygon 矩形rectangle 圆circle
     * @param {*} callback 回调函数
     */
    boxToFence(type, callback) {
      const { edit, mapId } = this;
      const map = thirdMaps.get(mapId);

      this.closeBoxToSelect();
      edit?.enableDraw();

      edit?.onBtnCtrlActive(type);

      const getPoints = (e) => {
        const retPoints = e.features[0].geometry.coordinates[0];
        const location = e.features[0].properties.center;
        const range = e.features[0].properties.radius;
        edit.disableDraw();
        if (callback) {
          callback(retPoints, location, range);
          map.off('draw.create', getPoints);
        }
      };

      map.on('draw.create', getPoints);
    }

    /**
     * @description 框选
     * @param {*} type 框选类型 点marker 线polyline 多边形polygon 矩形rectangle 圆circle
     * @param {*} layerIds 图层ids
     * @param {*} callback 回调函数
     */
    boxToSelect(type, layerIds, callback) {
      const { edit, mapId } = this;
      const map = thirdMaps.get(mapId);

      map.off('draw.create', this.updateArea);

      switch (type) {
        case 'marker': {
          edit?.onBtnCtrlActive('point');
          break;
        }
        case 'polyline': {
          edit?.onBtnCtrlActive('line');
          break;
        }
        default: {
          edit?.onBtnCtrlActive(type);
          break;
        }
      }

      const getMarkers = (e) => {
        const retObj = {};
        const retMarkers: any[] = [];

        this.vehicleMarkers.forEach((i) => {
          const points = turf.points([i.position]);
          const ptsWithin = turf.pointsWithinPolygon(points, e.features[0]);
          const features = ptsWithin?.features || [];
          if (features.length > 0 && retMarkers.length < 201) {
            const obj = { ...i };
            delete obj.marker;
            retMarkers.push(obj);
          }
        });

        if (isArray(layerIds)) {
          layerIds.forEach((layerId) => {
            const layer = map.getLayer(layerId);
            const { sourceMap } = this.layers.get(layerId);

            if (!layer || layer.visibility !== 'visible') return;

            retObj[layerId] = [];
            const source = map.getSource(layer.source);
            const ptsWithin = turf.pointsWithinPolygon(source._data, e.features[0]);

            const features = ptsWithin?.features || [];
            for (const feature of features) {
              const { id } = feature.properties;
              const target = sourceMap.get(id);

              if (target) {
                if (retMarkers.length >= 201) break;
                retMarkers.push(target);
                retObj[layerId].push(target);
              }
            }
          });
        }

        if (callback) {
          drawCallback(callback, retMarkers, retObj);
          map.off('draw.create', getMarkers);
        }

        [...e.features].forEach((item: any) => {
          edit.draw.delete(item.id);
        });
        map.on('draw.create', this.updateArea);
      };

      // 框选完成
      map.on('draw.create', getMarkers);
    }

    /**
     * 改变图标上的文字
     * @param {string} layerId 图层id
     */
    changeLayoutProperty(layerId) {
      const map = thirdMaps.get(this.mapId);
      const ptLayerId = `${layerId}-point`;
      const layer = map.getLayer(ptLayerId);
      if (!layer) return;
      const show = JSON.parse(`${localStorage.getItem('showIconInfo')}`);
      if (show) {
        map.setLayoutProperty(ptLayerId, 'text-field', '{org}');
      } else {
        map.setLayoutProperty(ptLayerId, 'text-field', '{status}');
      }
    }

    // 清空地图
    clearMap() {
      const map = thirdMaps.get(this.mapId);
      const layers = map.getAllLayers();
      const sources = map.getAllSources();

      const jump = (key) => {
        return (
          !key ||
          key.includes('MERGE') ||
          key.includes('minemap') ||
          key === 'POI_flyover' ||
          key === 'editSignSource' ||
          key === 'raster'
        );
      };

      Object.keys(layers).forEach((key) => {
        const { source } = layers[key];
        if (jump(source)) {
          return;
        }
        map.removeLayer(key);
      });

      Object.keys(sources).forEach((key) => {
        if (jump(key)) {
          return;
        }
        map.removeSource(key);
      });
    }

    /**
     *  @description 清除矢量底图
     */
    clearVectorStyle() {
      const map = thirdMaps.get(this.mapId);
      const style = {
        glyphs: 'minemap://fonts/{fontstack}/{range}',
        layers: [
          {
            id: 'background',
            paint: {
              'background-color': '#19293c',
            },
            source: '',
            type: 'background',
          },
        ],
        sources: {},
        sprite: 'minemap://sprite/sprite',
        version: 8,
      };
      map.setStyle(style, { diff: true, keepUserInfo: true });
    }

    /**
     * 点击marker点
     * @param {String} el 点dom
     * @param {Object} data 点数据
     * @param {Object} config
     */
    async clickMarker(el, data, config) {
      const { mapId } = this;
      const { callback, layerId, position, windowTemplate } = config;
      const lngLat = formatLonLat(position);

      el.addEventListener('click', (e) => {
        e.stopPropagation();
        e.preventDefault();
        this.closeCustomPopup();
        setTimeout(() => {
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
          this.markerPopup(createEl(data), lngLat, [-5, -15]);
        }, 300);
        callback?.(data);
      });
    }

    // 删除全部框选
    closeBoxToSelect() {
      this.edit?.draw.deleteAll();
    }

    // 关闭自定义弹窗
    closeCustomPopup() {
      this.customPopup?.remove();
      // 圈层防控弹窗关闭
      const marker = this.markers.get('circle_control');
      if (marker) {
        marker.getPopup()?.remove();
      }
    }

    // 返回圆形geojson
    createGeoJSONCircle(center, radius) {
      const points = 64;
      const coords = {
        latitude: center[1],
        longitude: center[0],
      };
      const km = radius / 1000;
      const ret: any = [];
      const distanceX = km / (111.32 * Math.cos((coords.latitude * Math.PI) / 180));
      const distanceY = km / 110.574;

      let theta, x, y;
      for (let i = 0; i < points; i++) {
        theta = (i / points) * (2 * Math.PI);
        x = distanceX * Math.cos(theta);
        y = distanceY * Math.sin(theta);

        ret.push([coords.longitude + x, coords.latitude + y]);
      }
      ret.push(ret[0]);

      return {
        data: {
          features: [
            {
              geometry: {
                coordinates: [ret],
                type: 'Polygon',
              },
              type: 'Feature',
            },
          ],
          type: 'FeatureCollection',
        },
        type: 'geojson',
      };
    }

    // 删除编辑池
    deleteAllDraw() {
      this.edit?.draw.deleteAll();
    }

    /**
     * 删除三道防线/自定义图层点位
     * @param {*} keyWord 关键字
     */
    deleteCustomMarker(keyWord) {
      this.markers.forEach((val, key) => {
        if (key.includes(keyWord)) {
          val?.remove();
        }
      });
    }

    // 删除标绘图层
    deleteDrawLayer() {
      this.drawLayer.forEach(({ layerId }) => {
        const has = layerId?.includes('region');
        if (!has && layerId) {
          this.deleteLayer(layerId);
        }
      });

      this.edit.draw.deleteAll();
    }

    /**
     * @description 删除图层
     * @param {string[]} layerIds 图层ids 不传删全部
     * @param {boolean} isCluster 是否聚合
     */
    deleteLayer(layerIds) {
      const { layers, mapId } = this;
      const map = thirdMaps.get(mapId);

      if (layerIds) {
        if (!isArray(layerIds)) {
          layerIds = [layerIds];
        }
      } else {
        layerIds = [...layers.keys()];
      }

      layerIds?.forEach((layerId) => {
        const layer = map.getLayer(layerId);
        const ptLayer = map.getLayer(`${layerId}-point`);
        if (layer) {
          map.removeSource(layerId);
          map.removeLayer(layerId);
        }
        if (ptLayer) {
          map.removeLayer(`${layerId}-point`);
          map.removeSource(`${layerId}-cluster`);
          if (map.getLayer(`${layerId}-cluster`)) {
            map.removeLayer(`${layerId}-cluster`);
          }
        }
        if (layerId.includes('carPhoto')) {
          map.removeLayer(`${layerId}-speed`);
        }
      });
    }

    /**
     * 删除点位
     * @param {array} types  类型 [add | edit | look | plan]
     */
    deleteMarker(types) {
      const map = thirdMaps.get(this.mapId);
      map.getCanvas().style.cursor = '';
      if (types?.length) {
        types.forEach((item) => {
          const oldMarker = this.markers.get(item);
          oldMarker?.remove();
        });
      } else {
        const markers = map.getAllMarkers();
        markers.forEach((item) => {
          item?.remove();
        });
      }

      const fun = this.markers.get('listen');
      map.off('click', fun);
    }

    // 销毁地图
    destroyMap() {
      const { mapId } = this;
      let map = thirdMaps.get(mapId);

      this.disposeBoxToSelect();

      map?.remove();
      map = null;
      mapManager.delete(mapId);
    }

    /**
     * 隐藏显示绘制的区域
     * @param {boolean} visible 是否显示draw
     */
    disableDraw(visible) {
      const { drawLayer, mapId } = this;
      const map = thirdMaps.get(mapId);
      const isShow = visible ? 'visible' : 'none';

      drawLayer?.forEach((item) => {
        const { highlight, layerId } = item;
        if (!highlight && layerId) {
          map.setLayoutProperty(layerId, 'visibility', isShow);
        }
      });
    }

    // 销毁框选
    disposeBoxToSelect() {
      this.edit?.dispose();
      this.edit = null;
    }

    /**
     * 拖拽marker点
     * @param {Object} data
     */
    dragendMarker(data, config) {
      const { callback } = config;
      const _marker = this.markers.get(data.id);

      _marker.on('dragend', async (e) => {
        const { lat, lng } = e.target._lngLat;
        const location = coordinateTransform([lng, lat], true).join(',');
        const { formatted_address } = await this.queryAddressByLocation(location);
        if (formatted_address) {
          callback?.(location, formatted_address);
        }
      });
    }

    /**
     * 绘制区域
     * @param {point | polyline | rectangle | circle | polygon | text | icon} type 点、线、矩形、圆、多边形、文字
     * @param {add | modify | look} handle 新增、修改、查看
     * @param {array} path 路径点集合
     * @param {array} center 圆的中心点
     * @param {number} radius 圆的半径
     * @param {function} change 绘制更新事件
     * @param {boolean} highlight 是否高亮
     * @param {string} color 颜色 '252, 167, 1'
     * @param {string} fillColor 填充颜色 '252, 167, 1'
     * @param {string} lineStyle 实线 solid，虚线 dashed
     * @param {number} lineWidth 线或者边框的宽度
     * @param {boolean} saveBefore 是否保留之前绘制图案 默认true
     * @param {number} textSize 文字字号
     * @param {number} textValue 文字内容
     * @param {number} opacity 不透明度
     * @param {string} imageName icon名字
     * @param {boolean} editable 是否有编辑的能力,默认true
     */
    draw(config) {
      const {
        change,
        color,
        editable = true,
        fillColor,
        fillOutlineOpacity,
        handle,
        hex,
        highlight,
        imageName,
        lineStyle,
        lineWidth,
        opacity,
        path,
        saveBefore = true,
        textSize,
        textValue,
        type,
      } = config;
      const map = thirdMaps.get(this.mapId);
      map.off('draw.create', this.updateArea);
      map.off('draw.update', this.updateArea);
      map.off('draw.delete', this.deleteArea);
      this.offTextClick?.();
      if (['polygon', 'rectangle'].includes(type)) {
        path?.push(path[0]);
      }
      const colorHex = hex ? color : colorRGBtoHex(`(${color})`);
      const fillColorHex = fillColor || colorHex || '#FCA701';

      const style = {
        fillColor: fillColorHex,
        fillOpacity: opacity >= 0 ? opacity : 0.5,
        fillOutlineColor: colorHex || '#FCA701',
        fillOutlineDasharray: lineStyle === 'dashed' ? 'true' : 'false',
        fillOutlineOpacity: fillOutlineOpacity >= 0 ? fillOutlineOpacity : 1,
        fillOutlineWidth: lineWidth >= 0 ? lineWidth : 4,
        imageName,
        textSize,
        textValue,
      };

      if (!saveBefore) {
        this.edit?.draw.deleteAll();
      }

      switch (handle) {
        case 'add': {
          this.drawChangeMode(type, style, config);
          break;
        }
        case 'look': {
          if (!highlight) {
            style.fillOutlineWidth = 1;
            style.fillOpacity = 0.1;
          }
          this.drawerLayer(type, path, style, config);
          break;
        }
        case 'modify': {
          if (editable === false) {
            this.drawerLayer(type, path, style, config);
          } else {
            this.addFeatures(type, path, style, config);
          }
          break;
        }
      }

      const updateArea = (e, record = true) => {
        const feature = e.features[0];

        // 是否需要记录
        if (record) {
          this.drawStepList.push(feature);
        }

        const { geometry, id, properties } = e.features[0];
        const { coordinates, type } = geometry;
        const { center, feature_type, iconImage, radius } = properties;

        if (feature_type === 'circle') {
          if (e.action === 'move') {
            const polygon = Turf.polygon(coordinates);
            const centroid = Turf.centroid(polygon);
            const center = centroid.geometry.coordinates;
            change({ center: coordinateTransform(center, true), radius: radius * 1000 }, id);
          } else {
            change({ center: coordinateTransform(center, true), radius: radius * 1000 }, id);
          }
          return;
        }
        if (feature_type === 'icon') {
          change(coordinateTransform(coordinates, true), id, iconImage);
        } else if (type === 'LineString') {
          change(
            coordinates.map((i) => coordinateTransform(i, true)),
            id,
          );
        } else if (type === 'Point') {
          change(coordinateTransform(coordinates, true), id);
        } else {
          const retPoints = coordinates[0].slice(0, -1);
          change(
            retPoints.map((i) => coordinateTransform(i, true)),
            id,
          );
        }
      };
      this.updateArea = updateArea;

      // 删除
      const deleteArea = (e) => {
        change(false, e.features[0].id);
      };
      this.deleteArea = deleteArea;

      // 区域改变事件
      if (handle !== 'look') {
        // 结束绘制
        map.on('draw.create', updateArea);
        map.on('draw.update', updateArea);
        map.on('draw.delete', deleteArea);
      }
    }

    /**
     * 手动绘制图形
     * @param {point | polyline | rectangle | circle | polygon | line_arrow} type 点、线、矩形、圆、多边形、箭头
     * @param {Object} style
     */
    drawChangeMode(type, style, config) {
      const { edit, mapId } = this;
      const map = thirdMaps.get(mapId);
      const { fillOpacity, fillOutlineColor, fillOutlineDasharray, fillOutlineWidth, textSize } =
        style;
      const { change } = config;
      switch (type) {
        case 'icon': {
          edit?.onBtnCtrlActive('icon', {
            style: {
              iconImage: style.imageName,
              iconSize: 0.7,
            },
          });
          break;
        }
        case 'line_arrow': {
          edit?.onBtnCtrlActive('line_arrow', {
            style: {
              lineColor: fillOutlineColor,
              lineDasharray: fillOutlineDasharray,
              lineOpacity: fillOpacity,
              lineWidth: fillOutlineWidth,
            },
          });
          break;
        }
        case 'marker': {
          edit?.onBtnCtrlActive('point');
          break;
        }
        case 'point': {
          edit?.onBtnCtrlActive('point', {
            style: {
              circleBorderColor: fillOutlineColor,
              circleColor: fillOutlineColor,
              circleOpacity: fillOpacity,
            },
          });
          break;
        }
        case 'point-icon': {
          edit?.onBtnCtrlActive('icon', {
            style: {
              iconImage: style.imageName,
              iconSize: 0.5,
            },
          });
          break;
        }
        case 'polyline': {
          this.addPopup('line');
          edit?.onBtnCtrlActive('line', {
            style: {
              lineColor: fillOutlineColor,
              lineDasharray: fillOutlineDasharray,
              lineOpacity: fillOpacity,
              lineWidth: fillOutlineWidth,
            },
          });
          break;
        }
        case 'text': {
          const onceClick = (event) => {
            const uuid = guid();
            const el = document.createElement('input');
            const lngLat: any = [event.lngLat.lng, event.lngLat.lat];
            this.markerPopup(el, formatLonLat(lngLat));
            el.focus();
            el.style.color = '#000';
            el.addEventListener(
              'blur',
              (e: any) => {
                if (e.target?.value !== '') {
                  const feature = {
                    geometry: {
                      coordinates: lngLat,
                      id: uuid,
                      type: 'Point',
                    },
                    id: uuid,
                    properties: {
                      custom_style: 'true',
                      feature_type: 'text',
                      textColor: style.fillOutlineColor,
                      textField: e.target?.value,
                      textSize,
                    },
                    type: 'Feature',
                  };
                  edit.draw.add(feature);
                }
                change(lngLat, uuid, e.target?.value);
              },
              false,
            );
          };

          map.once('click', onceClick);
          this.offTextClick = () => {
            map.off('click', onceClick);
          };
          break;
        }
        default: {
          this.addPopup(type);
          edit?.onBtnCtrlActive(type, { style });
          break;
        }
      }
    }

    /**
     * 生成图形图层
     * @param {point | polyline | rectangle | circle | polygon} type 点、线、矩形、圆、多边形
     * @param {Array} path
     * @param {Object} style
     * @param {Object} config
     */
    drawerLayer(type, path, style, config) {
      const { center, circleControlLayerId, highlight, layerId, lineStyle, radius } = config;
      const param = {
        center: coordinateTransform(center),
        circleControlLayerId,
        highlight,
        layerId,
        lineStyle,
        path: path?.map((p) => coordinateTransform(p)),
        radius,
        style,
      };
      switch (type) {
        case 'circle': {
          this.addCircleLayer(param);
          break;
        }
        case 'icon': {
          this.addIconLayer(param);
          break;
        }
        case 'point': {
          this.addPointLayer(param);
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
        case 'text': {
          this.addTextLayer(param);
          break;
        }
      }
    }

    // 结束绘制
    endDraw() {
      this.edit?.onBtnCtrlActive('trash');
    }

    /**
     * @description 飞行
     * @param {LngLat} center 中心点
     * @param {Number} zoom 缩放
     * @param {Number} bearing 旋转
     * @param {Number} pitch 倾斜
     */
    flyAction(center, zoom?, bearing?, pitch?) {
      const map = thirdMaps.get(this.mapId);
      if (!isNumber(zoom)) {
        zoom = map.getZoom();
      }
      map.flyTo({
        bearing: bearing || 0,
        center,
        duration: 2000, // 飞行时长，毫秒
        pitch: pitch || 0,
        zoom,
      });
    }

    // 获取所有图层id
    getAllLayerId() {
      const map = thirdMaps.get(this.mapId);
      const layers = map.getAllLayers();
      return Object.keys(layers);
    }

    getBounds() {
      const map = thirdMaps.get(this.mapId);
      return map.getBounds();
    }

    getCanvas() {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      return map.getCanvas();
    }

    // 获取地图中心点
    getCenter() {
      const map = thirdMaps.get(this.mapId);
      const center = map?.getCenter();
      return center ? [center.lng.toFixed(6), center.lat.toFixed(6)] : this.center;
    }

    // 获取区域坐标点
    getCoordinates() {
      if (posArray.length > 0) {
        const tempArray = posArray[0];
        if (tempArray.length > 0) {
          let maxLatitude = tempArray[0][1];
          let minLatitude = tempArray[0][1];
          let maxLongitude = tempArray[0][0];
          let minLongitude = tempArray[0][0];
          tempArray.forEach((item) => {
            if (item[1] > maxLatitude) {
              maxLatitude = item[1];
            }
            if (item[1] < minLatitude) {
              minLatitude = item[1];
            }
            if (item[0] > maxLongitude) {
              maxLongitude = item[0];
            }
            if (item[0] < minLongitude) {
              minLongitude = item[0];
            }
          });
          return {
            maxLatitude,
            maxLongitude,
            minLatitude,
            minLongitude,
          };
        }
      }
      return {
        maxLatitude: 0,
        maxLongitude: 0,
        minLatitude: 0,
        minLongitude: 0,
      };
    }

    /**
     * @description 构造jsonData
     * @param {*} sourceMap
     * @param {array} layerId layerId
     * @param {array} data 图层数据
     * @returns {array} jsonData
     */
    getJsonData(layerId, sourceMap, data) {
      const map = thirdMaps.get(this.mapId);
      const jsonData: any = {
        features: [],
        type: 'FeatureCollection',
      };
      const { renderer } = this.layers.get(layerId) || {};
      data.forEach((item) => {
        const {
          attendance,
          id,
          name,
          nid,
          number,
          organizationName,
          resourceType,
          speed,
          statusName,
          text,
        } = item;
        const position = nid ? item.position : coordinateTransform(item.position);
        let newName = statusName || text || number;
        let imageId = layerId;
        if (resourceType === 'Executor') {
          newName = name;
        }
        if (renderer) {
          imageId = renderer?.(item, { map, mapType: 'mineMap' }) || layerId;
        }
        const coordinates = formatLonLat(position);
        const org = name ? `${name}(${organizationName})` : '';
        const feature = {
          geometry: {
            coordinates,
            type: 'Point',
          },
          properties: {
            attendance,
            id,
            imageId,
            newName,
            org: layerId.includes('task') ? '' : org,
            speed,
            status: statusName,
            title: text,
          },
          type: 'Feature',
        };
        jsonData.features.push(feature);
        item.position = coordinates;
        sourceMap.set(item.id, item);
      });
      return jsonData;
    }

    /**
     * @description 获取图层数据
     * @param {string} layerId
     * @returns {array}
     */
    getLayerData(layerId) {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      const layer = map.getLayer(layerId);
      const { sourceMap } = this.layers.get(layerId);
      const ret: any = [];

      if (!layer) {
        return ret;
      }

      const { _data } = map.getSource(layer.source);
      _data.features.forEach((feature) => {
        const { id } = feature.properties;
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
     * @returns {boolean}
     */
    getLayerVisible(layerId) {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      const layer = map.getLayer(layerId);

      return layer?.visibility === 'visible';
    }

    /**
     * @description 绘制图层脉冲点
     * @param {string} layerId 图层id
     * @returns pulsingDot
     */
    getPulsingDot(layerId) {
      const map = thirdMaps.get(this.mapId);
      const color = getLayerColor(layerId);
      if (!color) return false;
      const size = 60;
      const duration = 1000; // 持续时间s
      let context: any;
      const pulsingDot = {
        data: new Uint8Array(size * size * 4),
        height: size,
        onAdd: () => {
          const canvas = document.createElement('canvas');
          canvas.width = size;
          canvas.height = size;
          context = canvas.getContext('2d', { willReadFrequently: true });
        },
        render() {
          const t = (performance.now() % duration) / duration;

          const radius = (size / 2) * 0.3;
          const outerRadius = (size / 4) * 0.7 * t + radius;

          // 画外围圆
          context.clearRect(0, 0, this.width, this.height);
          context.beginPath();
          context.arc(this.width / 2, this.height / 2, outerRadius, 0, Math.PI * 2);
          context.fillStyle = color.replace(/(.*)1/, `$1${1 - t}`);
          context.fill();

          // 画内圈圆
          context.beginPath();
          context.arc(this.width / 2, this.height / 2, radius, 0, Math.PI * 2);
          context.fillStyle = color;
          context.strokeStyle = color;
          // context.lineWidth = 1 + 4 * (1 - t);
          context.fill();
          context.stroke();

          // 从canvas中更新图片数据
          this.data = context.getImageData(0, 0, this.width, this.height).data;

          // 不断地重新绘制地图，导致平滑的动画点
          map.triggerRepaint();

          // 返回`true`让映射知道图像已经更新
          return true;
        },
        width: size,
      };
      return pulsingDot;
    }

    // 获取当前地图缩放级别
    getZoom() {
      const map = thirdMaps.get(this.mapId);
      return map.getZoom();
    }

    // 初始化地图
    initMap() {
      const { center, mapId, mapStyle, zoom, zooms } = this;
      const {
        dataDomainUrl,
        domainUrl,
        editJs,
        mapCss,
        mapJs,
        mapKey,
        projection,
        serverDomainUrl,
        serviceUrl,
        solution,
        spriteUrl,
        style,
        templateJs,
        turfJs,
        utilJs,
      } = getMapConfig();

      // const projection = 'MERCATOR';
      // const mapKey = '2584b3b7a60343b68caddc5e2245d721';
      // const solution = '11003,11001';
      // const domainUrl = 'https://minemap.minedata.cn';
      // const dataDomainUrl = 'https://minemap.minedata.cn';
      // const serverDomainUrl = 'https://sd-data.minedata.cn';
      // const spriteUrl = 'https://minemap.minedata.cn/minemapapi/v2.1.1/sprite/sprite';
      // const serviceUrl = 'https://service.minedata.cn/service';
      // const style = 'https://service.minedata.cn/map/solu/style';
      // const mapCss = 'https://minemap.minedata.cn/minemapapi/v2.1.1/minemap.css';
      // const mapJs = 'https://minemap.minedata.cn/minemapapi/v2.1.1/minemap.js';
      // const templateJs =
      //   'https://minemap.minedata.cn/minemapapi/minemap-plugins/template/template.js';
      // const editJs =
      //   'https://minemap.minedata.cn/minemapapi/minemap-plugins/edit/minemap-edit.js';
      // const turfJs = 'https://minemap.minedata.cn/minemapapi/minemap-CDN/turf/turf.min.js';
      // const utilJs =
      //   'https://minemap.minedata.cn/minemapapi/minemap-plugins/2d-util/minemap-util.js';

      const load = () => {
        minemap = window.minemap;

        // 全局参数设置
        minemap.domainUrl = domainUrl;
        minemap.dataDomainUrl = dataDomainUrl;
        if (serverDomainUrl) {
          minemap.serverDomainUrl = serverDomainUrl;
        }
        minemap.spriteUrl = spriteUrl;
        minemap.serviceUrl = serviceUrl;

        // key、solution设置
        minemap.key = mapKey;
        minemap.accessToken = mapKey;
        const solutions = solution.split(',');
        let Solution = solutions[0];
        if (mapStyle) {
          const key = {
            dark: 0,
            white: 1,
          }[mapStyle];
          Solution = solutions[key as number];
        }
        minemap.solution = Solution;

        // 初始化地图实例
        const map = new minemap.Map({
          center,
          container: mapId,
          maxZoom: zooms[1],
          minZoom: zooms[0],
          pitch: 0,
          projection,
          style: `${style}/${Solution}`,
          zoom,
        });

        map.on('load', () => {
          thirdMaps.set(mapId, map);
          this.isReady = true;
          this.onLoad(this);
          this.addBottomLayer(map);
          this.registerMapClick(map);
          this.registerBoxToSelect(map);
          this.registerEvent(map);
        });

        this.interactiveListen(map);
      };

      if (minemap) {
        load();
      } else {
        // 加载css
        const MapCss = document.createElement('link');
        MapCss.type = 'text/css';
        MapCss.rel = 'stylesheet';
        MapCss.href = mapCss;
        document.body.append(MapCss);

        // 加载js
        const MapJs = document.createElement('script');
        MapJs.type = 'text/javascript';
        MapJs.src = mapJs;
        document.body.append(MapJs);

        // 加载插件
        MapJs.addEventListener('load', () => {
          // edit
          const EditJs = document.createElement('script');
          EditJs.type = 'text/javascript';
          EditJs.src = editJs;
          document.body.append(EditJs);

          // turf
          const TurfJs = document.createElement('script');
          TurfJs.type = 'text/javascript';
          TurfJs.src = turfJs;
          TurfJs.addEventListener('load', () => {
            turf = window.turf;
          });
          document.body.append(TurfJs);

          // util
          const UtilJs = document.createElement('script');
          UtilJs.type = 'text/javascript';
          UtilJs.src = utilJs;
          document.body.append(UtilJs);
          UtilJs.addEventListener('load', () => {
            minemaputil = window.minemaputil;
          });

          // template
          const TemplateJs = document.createElement('script');
          TemplateJs.type = 'text/javascript';
          TemplateJs.src = templateJs;
          document.body.append(TemplateJs);
          load();
        });
      }
    }

    /**
     * @description 地图事件监听
     */
    interactiveListen(map) {
      map.on('movestart', () => {
        this.interactiveEvent.push('move');
      });
      map.on('moveend', () => {
        const index = this.interactiveEvent.indexOf('move');
        this.interactiveEvent.splice(index, 1);
      });
      map.on('zoomstart', () => {
        this.interactiveEvent.push('zoom');
      });
      map.on('zoomend', () => {
        const index = this.interactiveEvent.indexOf('zoom');
        this.interactiveEvent.splice(index, 1);
      });
      map.on('dragstart', () => {
        this.interactiveEvent.push('drag');
      });
      map.on('dragend', () => {
        const index = this.interactiveEvent.indexOf('drag');
        this.interactiveEvent.splice(index, 1);
      });
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

      const newPopup = new minemap.Popup({
        closeButton: true,
        closeOnClick: false,
        offset: offset || [-5, 5],
      });
      this.customPopup = newPopup;
      this.setCenter(position, null, false);
      newPopup.setLngLat(position).setDOMContent(el).addTo(map);
    }

    /**
     * @description 计算图形区域内的点
     * @param type 图形类型 点marker 线polyline 多边形polygon 矩形rectangle 圆circle
     * @param layerId 图层id
     * @returns
     */
    pointsWithinPolygon({ center, lineWidth, path, radius, type }, layerId) {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      const layer = map.getLayer(layerId);
      const { sourceMap } = this.layers.get(layerId) || {};
      if (!layer || layer.visibility !== 'visible') return;

      const source = map.getSource(layer.source);
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
          break;
        }
      }

      const ptsWithin = Turf.pointsWithinPolygon(source._data, searchWithin);
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
      let resultData: any = {};
      const { coordType, serviceUrl } = getMapConfig();
      const url = `${serviceUrl}/lbs/reverse/v1/regeo`;
      // const url='https://service.minedata.cn/service/lbs/reverse/v1/regeo'
      await axios({
        method: 'get',
        params: {
          coordtype: coordType || '02', // 坐标系:现场84/官网02
          key: mapKey,
          location,
        },
        url,
      })
        .then((res) => {
          resultData = res.data.regeocodes[0];
        })
        .catch(() => {});
      return resultData;
    }

    /**
     * @description 关键词搜索
     * @param {string} keywords 关键词
     */
    async queryAllSearch(keywords, currentPage) {
      let resultData: any = null;
      const { city, serviceUrl } = getMapConfig();
      const url = `${serviceUrl}/lbs/search/v1/keywords`;
      await axios({
        method: 'get',
        params: {
          city: city || '成都',
          key: mapKey,
          keywords,
          page_idx: currentPage,
          page_size: 10,
        },
        url,
      })
        .then((res) => {
          const count = Math.min(res.data.count, 200);
          resultData = { count, pois: res.data.pois };
        })
        .catch(() => {});
      return resultData;
    }

    /**
     * @description 周边查询
     * @param {string} keywords 关键词
     * @param {string} range 范围
     * @param {string} location 关键词
     */
    async queryAroundSearch(location, keywords, range) {
      const { serviceUrl } = getMapConfig();
      let resultData = null;
      const url = `${serviceUrl}/search/keyword2`;
      await axios({
        method: 'get',
        params: {
          keywords,
          location,
          range: Math.ceil(range * 1000),
          search_type: 'for_around',
        },
        url,
      })
        .then((res) => {
          resultData = res.data.data.pois;
        })
        .catch(() => {});
      return resultData;
    }

    /**
     * @description 视野查询
     * @param {string} keywords 关键词
     * @param {string} polygon 	经纬度坐标串组成的多边形区域
     */
    async queryBoxSearch(keywords, polygon) {
      const { serviceUrl } = getMapConfig();
      const { center } = this;
      let resultData = null;
      const url = `${serviceUrl}/search/keyword2`;

      const arr = polygon.split(';');
      const arr1: any = [];
      arr1.push(arr[0], arr[2]);
      const area = arr1.join(';');

      await axios({
        method: 'get',
        params: {
          area,
          keywords,
          location: center?.toString(),
          zoom: 13,
        },
        url,
      })
        .then((res) => {
          resultData = res.data.data.pois;
        })
        .catch(() => {});
      return resultData;
    }

    /**
     * @description 注册框选事件
     */
    registerBoxToSelect(map) {
      const EditInit = minemap.edit?.init;

      if (!EditInit) {
        setTimeout(() => {
          this.registerBoxToSelect(map);
        }, 100);
        return;
      }

      this.edit = new EditInit(map, {
        boxSelect: true,
        displayControlsDefault: true,
        keybindings: true, // 是否允许键盘交互;
        secondEdit: true,
        showButtons: false,
        touchEnabled: false,
        userStyles: {
          active: {
            circleColor: '#00FFCA',
            fillColor: '#00FFCA',
            fillOutlineColor: '#00FFCA',
            lineColor: '#00FFCA',
          },
        },
      });
    }

    // 注册全局事件
    registerEvent(map) {
      const { setSelectFeature } = useResourceStore();

      map.on('edit.selected', (event) => {
        const { features } = this.edit.draw.getAll();
        features.forEach((i) => {
          if (i.id === event.featureIds[0]) {
            setSelectFeature({ id: event.featureIds[0], ...i.properties });
          }
        });
      });

      map.on('edit.unselected', () => {
        setSelectFeature('');
      });
    }

    // 图层点击事件
    registerMapClick(map) {
      map.on('click', (e) => {
        // 点击地图空白处需要关闭地图弹窗
        this.closeCustomPopup();

        const layerIds: any[] = [];
        Object.keys(map.style._layers).forEach((key) => {
          if (key.startsWith('1')) return;
          layerIds.push(key);
        });
        const features = map.queryRenderedFeatures(e.point, {
          layers: layerIds,
        });
        const feature = features.length > 0 ? features[0] : null;
        if (!feature || feature?.source === 'minemap-draw-cold') return;
        const { cluster_id, id, point_count } = feature.properties;
        const layerId = feature.layer.id.split('-')[0];
        const layer = this.layers.get(layerId);
        if (!layer) return;
        const { sourceMap, windowTemplate } = layer;
        if (!windowTemplate) return;

        const source = map.getSource(feature.source);
        // 聚合
        if (cluster_id && point_count) {
          source.getClusterLeaves(cluster_id, point_count, 0, (_, res) => {
            const data: any = [];
            res?.forEach((item) => {
              const target = sourceMap.get(item.properties.id);
              if (target) {
                data.push(target);
              }
            });

            const { center, mapId } = this;
            const parent = document.createElement('div');
            const position = formatLonLat(data[0].position || center);

            const instance = useCreateApp(CursorList, {
              data,
              layerId,
              mapId,
              position,
              template: windowTemplate,
            });

            this.markerPopup(instance.mount(parent).$el, position);
          });
        } else {
          // 非聚合
          const target = sourceMap.get(id);
          this.addMarkerCustomPopup(layerId, target);
        }
      });
    }

    /**
     * 删除图层
     * @param layerId
     */
    removeLayer(layerId) {
      const map = thirdMaps.get(this.mapId);
      const layer = map.getLayer(layerId);
      if (layer) {
        map.removeSource(layerId);
        map.removeLayer(layerId);
      }
    }

    /**
     * @description 删除marker点
     * @param {Array} data
     */
    async removeMarkers(data) {
      data.forEach((item) => {
        const { id } = item;
        const marker = this.markers.get(id);
        marker?.remove();
        this.markers.set(id, null);
      });
    }

    /**
     * 删除车辆幻化图标
     * @param data
     */
    removeVehicleMarker(data) {
      this.vehicleMarkers = this.vehicleMarkers.filter((item) => {
        if (item.id === data.id) {
          item.marker.remove();
          return false;
        }
        return true;
      });
    }

    // 根据地图的 container 元素的尺寸调整地图的大小
    resize() {
      const map = thirdMaps.get(this.mapId);
      map.resize();
    }

    /**
     * @description 设置地图中心点、层级
     * @param {LngLat} center 中心点
     * @param {Number} zoom 层级
     * @param {boolean} trans 需要坐标系转换
     */
    setCenter(center, zoom?, trans = true) {
      const map = thirdMaps.get(this.mapId);

      const position = formatLonLat(center);
      if (!position) {
        console.error('position is null');
        return;
      }

      map.setCenter(trans ? coordinateTransform(position) : position);

      if (zoom) {
        if (!isNumber(zoom)) {
          throw new Error('zoom invalid');
        }
        map.setZoom(zoom);
      }
    }

    /**
     * 修改绘图颜色
     * @param {string} fillOutlineColor 边框颜色
     * @param {string} fillColor 填充颜色
     */
    setDrawColor(fillOutlineColor, fillColor) {
      const options = {
        userStyles: {
          active: {
            circleColor: fillOutlineColor,
            fillColor,
            fillOutlineColor,
            lineColor: fillOutlineColor,
          },
        },
      };
      this.edit = this.edit.setOptions(options);
    }

    /**
     * feature 锁定编辑
     * @param {Array} ids features的id数组 不传取全部
     * @param {Boolean} isLock 是否锁定  true锁定 false解锁
     */
    setFeatureLock(isLock, ids?) {
      const { edit } = this;
      if (!edit) {
        return;
      }
      const { features } = edit.draw.getAll();
      if (features.length === 0) {
        return;
      }
      let idsData = [];
      idsData =
        ids ||
        features.map((item) => {
          return item.id;
        });
      edit.setLockByIds(idsData, isLock);
    }

    /**
     * @description 设置图层是否显示
     * @param {string[]} layerIds
     * @param {boolean} isShow
     */
    setLayersVisible(layerIds, isShow) {
      const visible = isShow ? 'visible' : 'none';
      const { mapId } = this;
      const map = thirdMaps.get(mapId);

      layerIds.forEach((layerId) => {
        if (layerId === 'region') {
          const arr = this.getAllLayerId();
          arr.forEach((i) => {
            if (i.includes('region')) {
              map.setLayoutProperty(i, 'visibility', visible);
            }
          });
          return;
        }

        const layer = map.getLayer(layerId);
        const ptLayer = map.getLayer(`${layerId}-point`);
        const ctLayer = map.getLayer(`${layerId}-cluster`);
        if (layer) {
          map.setLayoutProperty(layerId, 'visibility', visible);
        }
        if (ptLayer) {
          map.setLayoutProperty(`${layerId}-point`, 'visibility', visible);
        }
        if (ctLayer) {
          map.setLayoutProperty(`${layerId}-cluster`, 'visibility', visible);
        }
      });
    }

    /**
     * 设置marker点是否做拽
     * @param {Object} data
     * @param {Boolean}  drag
     */
    setMarkerDraggable(data, drag) {
      const _marker = this.markers.get(data.id);
      _marker?.setDraggable(drag);
    }

    // 设置二次编辑图形样式
    setSecondEditStyle(val) {
      this.edit.setFeaturePropertiesByIds([val.id], val);
    }

    /**
     * 车速图层的显示和隐藏
     * @param {string} layerId 图层id
     */
    setSpeedLayerVisible(layerId) {
      const map = thirdMaps.get(this.mapId);
      if (!layerId.includes('carPhoto')) return;
      const show = JSON.parse(`${localStorage.getItem('showIconInfo')}`);
      const speedLayerId = `${layerId}-speed`;
      const layer = map.getLayer(speedLayerId);
      if (!layer) return;
      if (show) {
        const visible = map.getLayoutProperty(layerId, 'visibility');
        map.setLayoutProperty(speedLayerId, 'visibility', visible);
      } else {
        map.setLayoutProperty(speedLayerId, 'visibility', 'none');
      }
    }

    /**
     * @description 设置地图图层样式，solution传多个用逗号隔开，默认是深色
     * @param {String} key
     */
    setStyle(key) {
      const map = thirdMaps.get(this.mapId);
      const { solution, style } = getMapConfig();
      // const style = 'https://service.minedata.cn/map/solu/style';
      // const solution = '11002,11001';
      const solutions = solution.split(',');
      const paths = {
        dark: `${style}/${solutions[0]}`,
        white: `${style}/${solutions[1]}`,
      };
      if (key === 'raster') {
        this.addRasterLayer();
      } else {
        if (map.getLayer('raster')) {
          map.removeLayer('raster');
          map.removeSource('raster');
        }
        // 设置矢量底图
        minemap.key = mapKey;
        minemap.accessToken = mapKey;
        map.solution = solutions[0];
        map.setStyle(paths[key], { diff: true, keepUserInfo: true });
      }
    }

    /**
     * 根据状态隐藏/显示所有marker
     * @param {*} flag 显示状态
     */
    showMarkerByFlag(flag) {
      this.markers.forEach((val) => {
        if (flag) {
          val.getElement().style.display = ``;
        } else {
          val.getElement().style.display = `none`;
        }
      });
      this.vehicleMarkers.forEach((val) => {
        if (flag) {
          val.marker.getElement().style.display = `flex`;
        } else {
          val.marker.getElement().style.display = `none`;
        }
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
      const { drawStepList } = this;
      const editDrawer = this.edit?.draw;

      // 保留初始绘制
      if (keepFirstStep && drawStepList.length === 1) {
        return;
      }

      if (drawStepList.length === 0) {
        return;
      }

      // 最后一个既是当前状态/删除最后一个
      const lastFeature = drawStepList.pop();
      const lastFeatureId = lastFeature.id;
      const index = drawStepList.findIndex((i) => i.id === lastFeatureId);
      // 如果当前状态Feature已经没有之前的状态的话就删除
      if (index === -1) {
        editDrawer.delete(lastFeature);
        return;
      }

      // 倒数第二个是上一步状态
      const beforeFeature = drawStepList[drawStepList.length - 1];
      const { id } = beforeFeature;
      const featureList = editDrawer.getAll();
      const resetFeature = featureList.features.find((i) => i.id === id);
      editDrawer.delete(resetFeature);
      editDrawer.add(beforeFeature);

      this.updateArea({ features: [beforeFeature] }, false);
    }

    /**
     * 更新点位
     *@param {string} type 操作类型 add | edit
     * @param {*} text 定位
     */
    async updateAddressMarker(type, text) {
      const parent = document.createElement('div');
      const instance = useCreateApp(KindMarker, { text });
      const el = instance.mount(parent).$el;

      const _popup = new minemap.Popup({
        closeButton: false,
        closeOnClick: false,
        offset: [0, 34],
      }).setDOMContent(el);

      const marker = this.markers.get(type);
      marker.setPopup(_popup).togglePopup();
    }

    /**
     * @description 更新图层
     * @param {string} layerId 图层id
     * @param {addList, deleteList, updateList} options
     */
    async updateLayer(layerId, options) {
      const map = thirdMaps.get(this.mapId);
      if (!map) {
        return;
      }

      const layer = map.getLayer(layerId);
      if (!layer) {
        return;
      }

      // 用户操作过程中更新会让用户感觉卡顿
      if (this.interactiveEvent.length > 0) {
        return;
      }

      const { addList = [], deleteList = [], updateList = [] } = options;
      const { sourceMap } = this.layers.get(layerId);
      const newLayerData: any[] = [];

      // 删除
      if (deleteList?.length > 0) {
        const deleteIds = new Map();
        deleteList.forEach((item) => {
          deleteIds.set(item.id, item.id);
          sourceMap.delete(item.id);
        });
      }

      // 更新
      if (updateList?.length > 0) {
        newLayerData.push(...updateList);
      }

      // 新增
      if (addList?.length > 0) {
        newLayerData.push(...addList);
      }

      // 只渲染可视范围内的点
      const { _ne, _sw } = this.getBounds();
      const withinPoints: any = [];
      newLayerData.forEach((point) => {
        if (!point.position) {
          return;
        }
        const within = pointsWithinPolygon(
          {
            path: [
              [_sw.lng, _sw.lat],
              [_sw.lng, _ne.lat],
              [_ne.lng, _ne.lat],
              [_ne.lng, _sw.lat],
            ],
            type: 'rectangle',
          },
          [coordinateTransform(point.position)],
        );
        if (within.length > 0) {
          withinPoints.push(point);
        }
      });

      // 替换原来的数据源并重新渲染地图
      const jsonData = this.getJsonData(layerId, sourceMap, withinPoints);
      map.getSource(layerId).setData(jsonData);
      map.getSource(`${layerId}-cluster`).setData(jsonData);

      this.changeLayoutProperty(layerId);
      this.setSpeedLayerVisible(layerId);
    }

    /**
     * @description 更新路况数据源
     */
    updateTrafficSource() {
      const map = thirdMaps.get(this.mapId);
      if (map.getSource('Traffic')) {
        map.removeSource('Traffic');
      }

      const { solution, trafficUrl } = getMapConfig();
      const solutions = solution.split(',');
      const real_url = `${trafficUrl}/data/dynamic-traffic/ertic?servicetype=0&z={z}&x={x}&y={y}&key=${mapKey}&solu=${solutions[0]}`;
      const demo_url = `https://service.minedata.cn/service/data/dynamic-traffic/ertic?servicetype=0&z={z}&x={x}&y={y}&key=${mapKey}&solu=${solutions[0]}`;
      const url = trafficUrl ? real_url : demo_url;

      map.addSource('Traffic', {
        tiles: [url],
        traffic: true,
        type: 'vector',
      });
    }
  }

  /**
   * @description Track 动向
   * @class Track
   */
  class _Track extends BaseTrack {
    currentIndex: number;
    markers: any[];
    playIndex: number;
    playTimer: any;
    uid = guid();

    constructor(config) {
      super(config);
      this.currentIndex = 0; // 当前运动到位置下标
      this.playIndex = 0;
      this.data = config.data.map((i) => coordinateTransform(i));
      this.markers = [];
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
      const points = this.interpolatePoint(p1, p2);
      const angle = this.getAngle(p1, p2);

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
      const { map, uid } = this;
      const mmap = thirdMaps.get(map.mapId);

      this.markers.forEach((marker) => {
        marker.remove();
      });
      this.markers = [];

      const layerId = `newLineLayer-${uid}`;
      if (mmap.getLayer(layerId)) {
        mmap.removeSource(layerId);
        mmap.removeLayer(layerId);
      }
      const carLayerId = `carLayer-${uid}`;
      if (mmap.getLayer(carLayerId)) {
        mmap.removeSource(carLayerId);
        mmap.removeLayer(carLayerId);
      }
    }

    // 画小车
    drawCar() {
      const { data, image, map, uid } = this;
      const moveCarName = image?.split('/')[5];
      const mmap = thirdMaps.get(map.mapId);
      const layerId = `carLayer-${uid}`;
      const addCar = () => {
        const jsonData = {
          features: [
            {
              geometry: {
                coordinates: data[0],
                type: 'Point',
              },
              properties: {},
              type: 'Feature',
            },
          ],
          type: 'FeatureCollection',
        };

        mmap.addSource(layerId, {
          cluster: false,
          data: jsonData,
          type: 'geojson',
        });

        const angle = this.getAngle(data[0], data[1]);
        mmap.addLayer({
          id: layerId,
          layout: {
            'icon-allow-overlap': true, // 图标允许重叠
            'icon-image': moveCarName,
            'icon-rotate': angle,
            'icon-size': 0.5,
            'text-allow-overlap': true, // 文本允许重叠
            visibility: 'visible',
          },
          paint: {},
          source: layerId,
          type: 'symbol',
        });
      };

      const imgs = mmap.style.imageManager.images;
      if (imgs.key === moveCarName) {
        addCar();
      } else {
        mmap.loadImage(image, (error, img) => {
          if (error) throw error;
          if (!mmap.hasImage(moveCarName)) mmap.addImage(moveCarName, img);
          addCar();
        });
      }
    }

    // 画端点
    drawEndpoint(url, lnglat) {
      const { map } = this;
      const mmap = thirdMaps.get(map.mapId);

      const el = document.createElement('div');
      el.id = 'marker';
      el.style['background-image'] = `url(${url})`;
      el.style['background-size'] = 'cover';
      el.style.width = '40px';
      el.style.height = '40px';

      const marker = new minemap.Marker(el, { offset: [-20, -40] });
      marker.setLngLat(lnglat).addTo(mmap);
      this.markers.push(marker);
    }

    // 画线
    drawLine() {
      const { color, data, map, uid } = this;
      const mmap = thirdMaps.get(map.mapId);
      const layerId = `newLineLayer-${uid}`;
      const jsonData = {
        features: [
          {
            geometry: {
              coordinates: data,
              type: 'LineString',
            },
            type: 'Feature',
          },
        ],
        type: 'FeatureCollection',
      };
      mmap.addSource(layerId, {
        data: jsonData,
        type: 'geojson',
      });
      mmap.addLayer({
        id: layerId,
        layout: {
          'line-cap': 'round',
          'line-join': 'round',
        },
        paint: {
          'line-color': color,
          'line-width': 4,
        },
        source: layerId,
        type: 'line',
      });
    }

    /**
     * @description 添加采样速率点图层
     * @param {array} data marker点数据
     * @param {string} layerId 图层id
     * @param {string} image 点图片
     * @param {boolean} bothIcon 是否同时显示箭头和圆圈图标
     */
    drawSample(options) {
      const mmap = thirdMaps.get(this.map.mapId);
      const { bothIcon = false, data, image, key, layerId } = options;

      const showNum = layerId === 'sampleCircle';
      const addSample = () => {
        const jsonData = {
          features: <any>[],
          type: 'FeatureCollection',
        };
        data.forEach((item, index) => {
          const coordinates = formatLonLat(item.position);
          let angle = 0;
          if (index < data.length - 1) {
            const next = index + 1;
            const p1 = coordinates;
            const p2 = formatLonLat(data[next].position);
            angle = this.getAngle(p1, p2);
          }

          const feature = {
            geometry: {
              coordinates,
              type: 'Point',
            },
            properties: {
              angle: showNum ? 0 : angle,
              title: index + 1,
            },
            type: 'Feature',
          };
          jsonData.features.push(feature);
          item.position = coordinates;
        });

        mmap.addSource(layerId + key, {
          cluster: false,
          data: jsonData,
          type: 'geojson',
        });

        mmap.addLayer({
          id: layerId + key,
          layout: {
            'icon-allow-overlap': true,
            'icon-image': layerId,
            'icon-offset': bothIcon ? [-3, -4] : [0, 0],
            'icon-rotate': {
              property: 'angle',
              type: 'identity',
            },
            'icon-size': 1,
            'text-allow-overlap': false,
            'text-anchor': 'bottom',
            'text-field': showNum ? '{title}' : '',
            'text-offset': [0, 0.3],
            'text-size': 14,
            visibility: 'visible',
          },
          paint: {
            'text-color': '#000000',
          },
          source: layerId + key,
          type: 'symbol',
        });
      };
      const imgs = mmap.style.imageManager.images;
      if (imgs.key === layerId) {
        addSample();
      } else {
        if (!mmap.hasImage('moveCar')) {
          mmap.loadImage(image, (error, img) => {
            if (error) throw error;
            mmap.addImage(layerId, img);
            addSample();
          });
        }
      }
    }

    // 计算两个点之间角度
    getAngle(point1, point2) {
      // -90是因为car图片的初始指向是90°
      return turf.bearing(point1, point2) - 90;
    }

    // 初始化动向
    initTrack() {
      const { data, endImage, map, startImage } = this;
      const mmap = thirdMaps.get(map.mapId);
      mmap.setCenter(data[0]);
      this.drawLine();
      this.drawEndpoint(startImage, data[0]);
      this.drawEndpoint(endImage, data[data.length - 1]);
      this.drawCar();
    }

    // 两个点之间插值 - 根据两点之间的距离来插值
    interpolatePoint(point1, point2) {
      const kilometers = turf.rhumbDistance(point1, point2, {
        units: 'kilometers',
      });
      const distance = kilometers * 1000;

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
          const midpoint = turf.midpoint(point, points[index + 1]);
          arr.push(point, midpoint.geometry.coordinates);
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
      const { map, playIndex, playTimer, speed, status, uid } = this;
      const mmap = thirdMaps.get(map.mapId);
      const layerId = `carLayer-${uid}`;
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
            mmap.getSource(layerId).setData({
              features: [
                {
                  geometry: {
                    coordinates: lnglat,
                    type: 'Point',
                  },
                  properties: {},
                  type: 'Feature',
                },
              ],
              type: 'FeatureCollection',
            });
            mmap.setLayoutProperty(layerId, 'icon-rotate', angle);
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
      const { color, map, uid } = this;
      const mmap = thirdMaps.get(map.mapId);
      const layerId = `newLineLayer-${uid}`;
      mmap.setPaintProperty(layerId, 'line-width', 4);
      mmap.setPaintProperty(layerId, 'line-color', color);
    }

    // 重新开始
    reStart() {
      if (this.data.length === 1) return;
      this.changeStatus('start');
      this.setPosition(0);
    }

    // 设置高亮
    setHighlight() {
      const { color, map, uid } = this;
      const mmap = thirdMaps.get(map.mapId);
      const layerId = `newLineLayer-${uid}`;
      mmap.setPaintProperty(layerId, 'line-width', 6);
      mmap.setPaintProperty(layerId, 'line-color', color);
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
      if (data.length === 1) return;
      if (currentIndex === data.length - 1) {
        this.currentIndex = 0;
      }

      if (playIndex === 0) {
        this.animation();
      }
    }
  }

  /**
   * @description TrackRealTime 实时动向
   * @class TrackRealTime
   */
  class _TrackRealTime extends BaseTrack {
    marker: any;
    constructor(config) {
      super(config);
      this.marker = null;
      this.initTrackRealTime();
    }

    // 销毁轨迹跟踪
    destroyRealTimeTrack() {
      const mmap = thirdMaps.get(this.map.mapId);
      const endpoint = mmap.getAllMarkers();

      endpoint.forEach((marker) => {
        marker.remove();
      });

      const layerId = 'realTimeLineLayer';
      if (mmap.getLayer(layerId)) {
        mmap.removeSource(layerId);
        mmap.removeLayer(layerId);
      }
    }

    // 画端点
    drawEndpoint(url, lnglat) {
      const { map } = this;
      const mmap = thirdMaps.get(map.mapId);
      this.marker?.remove();

      const el = document.createElement('div');
      el.id = 'marker';
      el.style['background-image'] = `url(${url})`;
      el.style['background-size'] = 'cover';
      el.style.width = '40px';
      el.style.height = '40px';

      const marker = new minemap.Marker(el, { offset: [-20, -40] });
      marker.setLngLat(lnglat).addTo(mmap);
      return marker;
    }

    // 画线
    drawLine() {
      const { color, data, map } = this;
      const mmap = thirdMaps.get(map.mapId);
      const jsonData = {
        features: [
          {
            geometry: {
              coordinates: data,
              type: 'LineString',
            },
            type: 'Feature',
          },
        ],
        type: 'FeatureCollection',
      };
      const layerId = 'realTimeLineLayer';
      if (mmap.getLayer(layerId)) {
        mmap.getSource(layerId).setData(jsonData);
      } else {
        mmap.addSource(layerId, {
          data: jsonData,
          type: 'geojson',
        });
        mmap.addLayer({
          id: layerId,
          layout: {
            'line-cap': 'round',
            'line-join': 'round',
          },
          paint: {
            'line-color': color,
            'line-width': 8,
          },
          source: layerId,
          type: 'line',
        });
      }
    }

    endData(img) {
      this.marker = this.drawEndpoint(img, this.data[this.data.length - 1]);
    }

    // 初始化动向
    initTrackRealTime() {
      const { data, endImage, map, startImage } = this;
      this.data = data.map((element) => coordinateTransform(element));
      map.setCenter(this.data[0]);
      // gis点位画线
      this.drawLine();
      // 画起点
      this.drawEndpoint(startImage, this.data[0]);
      // 画实时位置
      this.marker = this.drawEndpoint(endImage, this.data[this.data.length - 1]);
    }

    updateData(inputData) {
      this.data = inputData.map((element) => coordinateTransform(element));
      this.drawLine();
      // 画实时位置
      this.marker = this.drawEndpoint(this.endImage, this.data[this.data.length - 1]);
    }
  }

  return new _MineMap(config);
}
export default _MineMap;
