/**
 * 文档
 * http://121.36.99.212:35001/mapdemo/index.htm#core/map
 */

import speedImg from '@/assets/images/marker/speed_marker.png';
import vehicleBgImg from '@/assets/images/marker/vehicle_marker_bg.png';
import { Message } from '@/components/Message';
import { useCreateApp, useSetInterval } from '@/hooks';
import CursorList from '@/pages/map/cursorList.vue';
import MapPopup from '@/pages/map/mapPopup.vue';
import KindMarker from '@/pages/map/marker/kindMarker.vue';
import RangeMarker from '@/pages/map/marker/rangeMarker.vue';
import { getIconUrlById } from '@/pages/resource/resourceHelper';
import { useResourceStore } from '@/store';
import { colorRGBtoHex, guid } from '@/utils';
import { getToken } from '@/utils/auth';
import { isArray, isNumber } from '@/utils/is';

import * as Turf from '@turf/turf';
import axios from 'axios';
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

function _MapAbc(config) {
  let mapabcgl: any = null;
  const mapKey = '';
  let mapStyle: any = null;
  let mapboxGlDrawSnapMode: any = null;
  let turf: any = null;
  // let DragCircleMode: any = null;
  // let DrawRectangle: any = null;
  let posArray: any[] = [];

  /**
   * @description MapAbc地图封装
   * @class _MapAbc
   */
  class _MapAbc extends BaseMap {
    clearTimer: any = null;
    deleteArea: any;
    drawBar: any;
    drawStepList: any[] = [];
    editDrawer: any;
    interactiveEvent: string[] = [];
    textClick: any;
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
     * @param {string} type 操作类型 add | edit | look
     * @param {*} image 图片url
     * @param {{position, callback}[]} config
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
        const location = `${lng},${lat}`;
        const { formatted_address } = await this.queryAddressByLocation(location);
        if (formatted_address) {
          this.upDataAddressMarker(type, formatted_address);
          callback?.(location, formatted_address);
        }
      };

      const addMarker = (lngLat) => {
        const _marker = new mapabcgl.Marker({ draggable, element: el, offset: [-16, -32] })
          .setLngLat(lngLat)
          .addTo(map);
        map.setCenter(lngLat);

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
        const lngLat = formatLonLat(position);
        addMarker(lngLat);
      } else {
        map.getCanvas().style.cursor = `url(${image}) 16 32, auto`;
        map.once('click', clickMap);
        this.markers.set('listen', clickMap);
      }
    }

    /**
     * 添加多边形/矩形图层
     * @param {{path,style,layerId}[]} config
     */
    async addAreaLayer(config) {
      const { drawLayer, mapId } = this;
      const { highlight, id, layerId, path, style } = config;
      const map = thirdMaps.get(mapId);
      const uid = guid();
      const layerUid = id || `area-${uid}`;
      const _layerId = layerId ? `${layerId}-${uid}` : layerUid;
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
          'fill-opacity': style.opacity,
          'fill-outline-color': style.strokeColor,
        },
        source: areaSource,
        type: 'fill',
      };
      map.addLayer(layer);
      drawLayer.push({ highlight, layerId: _layerId });
      this.layers.set(_layerId, { ...config });
      this.moveLayer(_layerId);

      this.changeSelectFeature(_layerId, config);
    }

    /**
     * 添加圈层防控
     * @param {*} image 图片
     * @param {string} type 操作类型 add | edit | look
     * @param {{position, callback}[]} config
     */
    addCircleControlMarker(type, image, config) {
      const { mapId } = this;
      const map = thirdMaps.get(mapId);
      if (map.getCanvas().style.cursor) return;

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
        const location = `${lng},${lat}`;
        const { formatted_address } = await this.queryAddressByLocation(location);
        if (formatted_address) {
          this.upDataAddressMarker(type, formatted_address, true);
          callback?.(location, formatted_address);
        }
      };

      const addMarker = (lngLat) => {
        const _marker = new mapabcgl.Marker({ draggable, element: el, offset: [0, -16] })
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
        const lngLat = formatLonLat(position);
        addMarker(lngLat);
      } else {
        map.getCanvas().style.cursor = `url("${image}") 16 32, auto`;
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
    async addCircleLayer(config) {
      const { drawLayer, mapId } = this;
      const { center, circleControlLayerId, highlight, id, radius, style } = config;
      const map = thirdMaps.get(mapId);
      let layerId = id || `circle-${guid()}`;
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
          'fill-opacity': style.opacity,
          'fill-outline-color': style.strokeColor,
        },
        source: layerId,
        type: 'fill',
      };

      map.addLayer(layer);
      drawLayer.push({ highlight, layerId });
      this.layers.set(layerId, { ...config });

      this.changeSelectFeature(layerId, config);
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
          this.markerPopup(createEl(data), formatLonLat(lngLat), [-32, -32]);
        }, 300);
      });
      const marker = new mapabcgl.Marker(el, { offset: [0, -16] });
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
    async addFeatures(type, path, style, config) {
      const map = thirdMaps.get(this.mapId);
      const { center, change, color, id, radius } = config;
      const { imageName, textSize, textValue } = style;
      let feature: any;
      const oldMarker = this.markers.get(id);
      if (oldMarker) {
        return;
      }
      switch (type) {
        case 'circle': {
          const geojson = this.createGeoJSONCircle(center, radius);
          feature = Object.assign(geojson.data.features[0], {
            properties: {},
          });
          break;
        }
        case 'icon': {
          const url = await getIconUrlById(imageName);
          const el = document.createElement('div');
          el.id = id;
          el.style['background-image'] = `url(${url})`;
          el.style['background-size'] = 'cover';
          el.style.width = '32px';
          el.style.height = '32px';
          const _marker = new mapabcgl.Marker({ draggable: true, element: el })
            .setLngLat(path)
            .addTo(map);
          _marker.on('dragend', async (e) => {
            const { lat, lng } = e.target._lngLat;
            change([lng, lat], id, imageName);
          });

          this.markers.set(id, _marker);
          this.keydownDelMarker(el, id, _marker);
          break;
        }
        case 'point': {
          feature = {
            geometry: {
              coordinates: path,
              type: 'Point',
            },
            properties: {},
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
            properties: {},
            type: 'Feature',
          };
          break;
        }
        // case 'line_arrow':
        case 'polyline': {
          feature = {
            geometry: {
              coordinates: path,
              type: 'LineString',
            },
            properties: {},
            type: 'Feature',
          };
          break;
        }
        case 'text': {
          const el = document.createElement('div');
          el.id = id;
          el.innerHTML = textValue;
          el.style.color = color;
          el.style.fontSize = `${textSize}px`;
          el.style.minWidth = '100px';
          el.style.height = '32px';
          const _marker = new mapabcgl.Marker({ draggable: true, element: el })
            .setLngLat(path)
            .addTo(map);

          _marker.on('dragend', async (e) => {
            const { lat, lng } = e.target._lngLat;
            change([lng, lat], id, el.innerHTML);
          });

          this.markers.set(id, _marker);
          this.keydownDelMarker(el, id, _marker);
          break;
        }
      }

      if (['icon', 'line_arrow', 'text'].includes(type)) {
        return;
      }
      if (id) {
        feature.id = id;
      }
      this.editDrawer.add(feature);
    }

    // 添加icon图层
    addIconLayer(config) {
      const { drawLayer, mapId } = this;
      const { id, path, style } = config;
      const map = thirdMaps.get(mapId);
      const layerId = id || `icon-${guid()}`;
      const layer = {
        id: layerId,
        layout: {
          'icon-image': style.imageName,
          'icon-size': 0.7,
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

      this.changeSelectFeature(layerId, config);
    }

    /** 添加自定义图片icon
     * @param {string} iconName icon名称 具有唯一性
     * @param {string} image 图片路径
     */
    addImageIcon(iconName, image) {
      const map = thirdMaps.get(this.mapId);
      if (!map || map?.hasImage(iconName)) return;
      map.loadImage(image, (error, img) => {
        if (error) throw error;
        if (!map.hasImage(iconName)) {
          map.addImage(iconName, img);
        }
      });
    }

    /**
     * 添加文字图层
     * @param {string} layerId 图层id
     * @param {{text, position, color}[]} data
     */
    async addLabelLayer(config) {
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
          'text-font': ['sourcehansanscn-normal'],
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
      const { mapId } = this;
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
      const map = thirdMaps.get(mapId);
      const sourceMap = new Map();
      const isAlarmLayer = layerId === 'alarm';
      const clusterId = `${layerId}-cluster`;

      if (!isArray(data)) {
        console.error('data must be array !');
        return;
      }

      renderer?.(null, { map, mapType: 'mapabc' });

      const jsonData = this.getJsonData(layerId, sourceMap, data);
      map.addSource(layerId, {
        cluster: isCluster,
        data: jsonData,
        type: 'geojson',
      });

      // 非聚合(图标点)
      map.loadImage(image, (error, img) => {
        if (error) throw error;
        if (!map.hasImage(layerId)) {
          map.addImage(layerId, img);
        }

        map.addLayer({
          filter: ['!has', 'point_count'],
          id: layerId,
          layout: {
            'icon-allow-overlap': true, // 图标允许重叠
            'icon-image': '{imageId}',
            'icon-size': 1,
            'text-allow-overlap': true, // 文本允许重叠
            'text-anchor': 'top',
            'text-field': '{newName}',
            'text-font': ['sourcehansanscn-normal'],
            'text-offset': [0, 1.5],
            'text-size': 12,
            visibility: isShow ? 'visible' : 'none',
          },
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
          source: layerId,
          type: 'symbol',
        });

        this.addSpeedLayer(layerId, isShow);
      });

      // 聚合(图标点)
      if (isCluster) {
        map.loadImage(clusterImage, (error, img) => {
          if (error) throw error;
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
              'text-font': ['sourcehansanscn-normal'],
              'text-offset': isAlarmLayer ? [0, 0.3] : [0, 0.1],
              'text-size': 12,
              visibility: isShow ? 'visible' : 'none',
            },
            paint: {
              'text-color': isAlarmLayer ? '#ff3b55' : '#ffffff',
            },
            source: layerId,
            type: 'symbol',
          });
        });
      }

      this.layers.set(layerId, { sourceMap, ...options });
      this.layers.set(clusterId, { sourceMap, ...options });
    }

    /**
     * 添加线图层
     * @param {string} layerId 图层id
     * @param {{path, style}[]} config
     */
    async addLineLayer(config) {
      const { drawLayer, mapId } = this;
      const { highlight, layerId, lineStyle, path, style } = config;
      const map = thirdMaps.get(mapId);
      const _layerId = layerId || `line-${guid()}`;

      const dashed = lineStyle === 'dashed';
      const linePaint = {
        'line-color': style.strokeColor,
        'line-opacity': style.opacity,
        'line-width': style.strokeWeight,
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
              coordinates: path,
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

      this.changeSelectFeature(_layerId, config);
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

      const _marker = new mapabcgl.Marker({ draggable, element: el, offset: [-16, -32] })
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

      const _popup = new mapabcgl.Popup({
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
      const { sourceMap, windowTemplate } = this.layers.get(layerId);
      let data = sourceMap.get(id);

      if (!data) {
        data = source;
      }

      const parent = document.createElement('div');

      const position = formatLonLat(data.position || center);
      const createEl = (data) => {
        const instance = useCreateApp(MapPopup, {
          data,
          layerId,
          mapId,
          template: windowTemplate,
        });
        return instance.mount(parent).$el;
      };

      this.markerPopup(createEl(data), position);
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

      const marker = new mapabcgl.Marker(el, { offset: [-25, -86] });
      marker.setLngLat(position).addTo(map);
      this.markers.set('plan', marker);
    }

    // 添加点图层
    addPointLayer(config) {
      const { drawLayer, mapId } = this;
      const { id, path, style } = config;
      const map = thirdMaps.get(mapId);
      const layerId = id || `point-${guid()}`;

      const layer = {
        id: layerId,
        paint: {
          'circle-color': style.strokeColor,
          'circle-opacity': style.opacity,
          'circle-radius': 5,
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

      this.changeSelectFeature(layerId, config);
    }

    /**
     * 测量工具
     * @param {*} type 框选类型 测距0 测面1
     */
    async addRangeTool(type) {
      const { editDrawer, mapId } = this;
      const map = thirdMaps.get(mapId);

      const mode = type ? 'draw_polygon' : 'draw_line_string';
      editDrawer?.changeMode(mode);

      const createRange = (e) => {
        let position: any = [];
        let distance: any = null;
        const { geometry, id } = e.features[0];

        if (type === 1) {
          const area = turf.area({
            features: e.features,
            type: 'FeatureCollection',
          });
          distance = area / 1_000_000; // 平方千米
          position = geometry.coordinates[0][0];
        } else {
          const line = {
            geometry,
            type: 'Feature',
          };
          distance = turf.lineDistance(line).toLocaleString(); // 千米
          position = geometry.coordinates[0];
        }

        const parent = document.createElement('div');
        const instance = useCreateApp(RangeMarker, {
          distance,
          id,
          mapId,
          type,
        });
        const el = instance.mount(parent).$el;

        const _marker = new mapabcgl.Marker(el).setLngLat(position).addTo(map);
        this.markers.set(id, _marker);
      };

      const deleteRange = (e) => {
        const { id } = e.features[0];
        const marker = this.markers.get(id);
        marker?.remove();
        this.markers.set(id, null);
      };

      map.once('draw.create', createRange);
      map.once('draw.delete', deleteRange);
      // map.once('draw.update', createRange);
    }

    /**
     *  @description 添加卫星影像底图
     */
    addRasterLayer() {
      const map = thirdMaps.get(this.mapId);
      const layerId = 'raster';
      if (map.getLayer(layerId)) {
        return;
      }
      const { rasterUrl } = getMapConfig();
      map.addLayer({
        id: layerId,
        source: {
          tiles: [rasterUrl],
          tileSize: 256,
          type: 'raster',
        },
        type: 'raster',
      });
    }

    /**
     * @description 添加比例尺以及鼠标移动显示鼠标经纬度
     * @param {*} scaleContainer 比例尺容器
     * @param {*} positionContainer 经纬度容器
     */
    addScaleAndPosition(scaleContainer, positionContainer) {
      const map = thirdMaps.get(this.mapId);
      const scale = new mapabcgl.ScaleControl({});

      const setScale = () => {
        const { innerText } = scale._container;
        scaleContainer.innerHTML = innerText;
      };

      map.on('zoom', () => {
        setScale();
      });
      map.on('mousemove', (e) => {
        const { lat, lng } = e.lngLat;
        positionContainer.innerHTML = `${lng.toFixed(6)}，${lat.toFixed(6)}`;
      });

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
          source: layerId,
          type: 'symbol',
        });
      };

      if (map.hasImage(speedLayerId)) {
        createLayer();
      } else {
        map.loadImage(speedImg, (error, img) => {
          if (error) throw error;
          if (!map.hasImage(speedLayerId)) {
            map.addImage(speedLayerId, img);
          }
          createLayer();
        });
      }
    }

    // 文字图层---单个
    addTextLayer(config) {
      const { drawLayer, mapId } = this;
      const { id, path, style } = config;
      const { strokeColor, textSize, textValue } = style;
      const map = thirdMaps.get(mapId);
      const layerId = id || `text-${guid()}`;
      const layer = {
        id: layerId,
        layout: {
          'text-field': '{title}',
          'text-font': ['sourcehansanscn-normal'],
          'text-size': Number(textSize),
        },
        paint: {
          'text-color': strokeColor,
        },
        source: {
          data: {
            geometry: {
              coordinates: path,
              type: 'Point',
            },
            properties: {
              title: textValue,
            },
            type: 'Feature',
          },
          type: 'geojson',
        },
        type: 'symbol',
      };
      map.addLayer(layer);
      drawLayer.push({ layerId });
      this.layers.set(layerId, { ...config });

      this.changeSelectFeature(layerId, config);
    }

    /**
     * @description 添加路况图层
     */
    addTrafficLayer() {
      const map = thirdMaps.get(this.mapId);
      const time = Number(getMapConfig().trafficTime) || 5;

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
          visibility: 'visible',
        },
        paint: {
          'line-color': {
            default: 'rgba(19, 134, 22, 1)',
            property: 'traffic_status',
            stops: [
              [{ value: 1, zoom: 10 }, 'rgba(19, 134, 22, 1)'],
              [{ value: 2, zoom: 10 }, 'rgba(222, 161, 29, 1)'],
              [{ value: 3, zoom: 10 }, 'rgba(222, 29, 29, 1)'],
              [{ value: 4, zoom: 10 }, 'rgba(98, 3, 3, 1)'],
              [{ value: 5, zoom: 10 }, 'rgba(124, 124, 122, 1)'],
            ],
            type: 'categorical',
          },
          'line-width': {
            stops: [
              [8, 2],
              [10, 2.2],
              [15, 3],
            ],
          },
        },
        source: 'Traffic',
        'source-layer': 'vectortraffic',
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
        const marker = new mapabcgl.Marker(el, { offset: [0, 20] })
          .setLngLat(position)
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
        marker.setLngLat(position);
        Object.assign(target, data);
      }
    }

    /**
     * @description 框选
     * @param {*} type 框选类型 点marker 线polyline 多边形polygon 矩形rectangle 圆circle
     * @param {*} layerIds 图层ids
     * @param {*} callback 回调函数
     */
    boxToSelect(type, layerIds, callback) {
      const { drawBar, editDrawer, mapId } = this;
      const map = thirdMaps.get(mapId);
      const style = {
        fillColor: '#00FFCA',
        fillOutlineDasharray: 'true',
        fillOutlineOpacity: 1,
        opacity: 0.3,
        strokeColor: '#00FFCA',
        strokeWeight: 4,
      };
      map.removeControl(drawBar);
      this.setDrawColor(style);
      map.addControl(drawBar);
      this.closeBoxToSelect();

      switch (type) {
        case 'marker': {
          editDrawer?.changeMode('draw_point');
          break;
        }
        case 'polyline': {
          editDrawer?.changeMode('draw_line_string');
          break;
        }
        default: {
          editDrawer?.changeMode(`draw_${type}`);
          break;
        }
      }

      const getMarkers = (e) => {
        const retObj = {};
        const retMarkers: any[] = [];
        const showLayerIds = layerIds.filter((item) => this.getLayerVisible(item));
        if (isArray(showLayerIds)) {
          showLayerIds.forEach((layerId) => {
            const layer = map.getLayer(layerId);
            const { sourceMap } = this.layers.get(layerId);

            if (!layer || layer.visibility !== 'visible') return;

            retObj[layerId] = [];
            const source = map.getSource(layer.source);
            const ptsWithin = turf.pointsWithinPolygon(source._data, e.features[0]);

            for (let i = 0; i < ptsWithin.features.length; i++) {
              const feature = ptsWithin.features[i];
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
        this.closeBoxToSelect();
        if (callback) {
          drawCallback(callback, retMarkers, retObj);
          map.off('draw.create', getMarkers);
        }
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
      const layer = map.getLayer(layerId);
      if (!layer) return;
      const show = JSON.parse(`${localStorage.getItem('showIconInfo')}`);
      if (show) {
        map.setLayoutProperty(layerId, 'text-field', '{org}');
      } else {
        map.setLayoutProperty(layerId, 'text-field', '{status}');
      }
    }

    /**
     * 监听标绘图层的点击和删除
     * @param {string} layerId 图层id
     * @param {{type,style}[]} config
     */
    changeSelectFeature(layerId, config) {
      const { editable, id, style, type } = config;
      const map = thirdMaps.get(this.mapId);
      const { setSelectFeature } = useResourceStore();

      const keydown = (e) => {
        if ([8, 46].includes(e.keyCode)) {
          map.removeLayerAndSource(layerId);
          this.layers.delete(layerId);
          setSelectFeature({ delete: true, id });
        }
      };

      const fun = () => {
        const { fillColor, fillOutlineDasharray, opacity, strokeColor, strokeWeight, textSize } =
          style;
        const params = {
          circleBorderColor: strokeColor,
          circleColor: fillColor,
          circleOpacity: opacity,
          feature_type: type,
          fillColor,
          fillOpacity: opacity,
          fillOutlineColor: strokeColor,
          fillOutlineDasharray,
          fillOutlineWidth: strokeWeight,
          id,
          layerId,
          lineColor: strokeColor,
          lineDasharray: fillOutlineDasharray,
          lineOpacity: opacity,
          lineWidth: strokeWeight,
          textColor: strokeColor,
          textSize,
        };
        setSelectFeature(params);
        window.addEventListener('keydown', keydown);
      };

      if (editable) {
        map.on('click', layerId, fun);
      }
      const res = this.layers.get(id);
      if (res?.fun) {
        return;
      }
      this.layers.set(id, { ...res, fun, keydown });
    }

    // 清空地图
    clearMap() {
      this.deleteLayer(null);
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
          this.markerPopup(createEl(data), lngLat, [-45, -32]);
        }, 300);
        callback?.(data);
      });
    }

    // 删除全部框选
    closeBoxToSelect() {
      this.editDrawer?.deleteAll();
    }

    // 关闭自定义弹窗
    closeCustomPopup() {
      this.customPopup?.remove();
      this.customPopup = null;
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
              properties: {
                center,
                isCircle: true,
                radiusInKm: km,
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
      this.editDrawer?.deleteAll();
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
      const map = thirdMaps.get(this.mapId);
      this.drawLayer.forEach(({ layerId }) => {
        const has = layerId?.includes('region');
        if (!has && layerId) {
          const res = this.layers.get(layerId);
          if (res?.fun) {
            map.off('click', layerId, res.fun);
            window.removeEventListener('keydown', res.keydown);
          }

          map.removeLayerAndSource(layerId);
          this.layers.delete(layerId);
        }
      });

      const markers = [...this.markers.keys()];
      markers.forEach((item) => {
        const has = item?.includes('draw');
        if (has) {
          const res = this.markers.get(item);
          if (res?.fun) {
            const index = item.indexOf('+');
            const id = item.slice(0, Math.max(0, index));
            const el = document.getElementById(id) as HTMLElement;
            el?.removeEventListener('click', res.fun);
            window.removeEventListener('keydown', res.keydown);
          } else {
            res?.remove();
          }
          this.markers.delete(item);
        }
      });
    }

    /**
     * 删除单个图形
     * @param {String} id
     */
    deleteFeatureById(id) {
      this.editDrawer?.delete(id);
    }

    /**
     * @description 删除图层
     * @param {string[]} layerIds 图层ids 不传删全部
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

      layerIds.forEach((layerId) => {
        map.removeLayerAndSource(layerId);
        map.removeLayerAndSource(`${layerId}-cluster`);
        layers.delete(layerId);
        layers.delete(`${layerId}-cluster`);
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
        const types = ['add', 'edit', 'look', 'plan'];
        types.forEach((item) => {
          const oldMarker = this.markers.get(item);
          oldMarker?.remove();
        });
      }

      const fun = this.markers.get('listen');
      map.off('click', fun);
    }

    // 销毁地图
    destroyMap() {
      const { mapId } = this;
      let map = thirdMaps.get(mapId);
      if (map) {
        this.editDrawer = null;
        map.remove();
        map = null;
        thirdMaps.delete(mapId);
      }
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
      if (this.editDrawer) {
        this.editDrawer.deleteAll();
        this.editDrawer = null;
      }
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
        const location = `${lng},${lat}`;
        const { formatted_address } = await this.queryAddressByLocation(location);
        if (formatted_address) {
          callback?.(location, formatted_address);
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
        drawEditable = false,
        editable,
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
      const { drawBar, mapId } = this;
      const map = thirdMaps.get(mapId);

      map.removeControl(drawBar);
      map.off('draw.create', this.updateArea);
      map.off('draw.update', this.updateArea);
      map.off('draw.delete', this.deleteArea);
      map.off('click', this.textClick);

      if (['polygon', 'rectangle'].includes(type)) {
        path?.push(path[0]);
      }

      const colorHex = hex ? color : colorRGBtoHex(`(${color})`);
      const fillColorHex = fillColor || colorHex || '#00FFCA';

      const style = {
        fillColor: fillColorHex,
        fillOutlineDasharray: lineStyle === 'dashed' ? 'true' : 'false',
        fillOutlineOpacity: fillOutlineOpacity >= 0 ? fillOutlineOpacity : 1,
        imageName,
        opacity: opacity >= 0 ? opacity : 0.3,
        strokeColor: colorHex || '#00FFCA',
        strokeWeight: lineWidth >= 0 ? lineWidth : 4,
        textSize,
        textValue,
      };

      this.setDrawColor(style);
      map.addControl(drawBar);

      if (!saveBefore) {
        this.editDrawer?.deleteAll();
      }

      switch (handle) {
        case 'add': {
          this.drawChangeMode(type, style, config);
          break;
        }
        case 'look': {
          if (!highlight) {
            style.strokeWeight = 1;
            style.opacity = 0.1;
          }
          this.drawerLayer(type, path, style, config);
          break;
        }
        case 'modify': {
          if (['icon', 'text'].includes(type) && editable) {
            this.addFeatures(type, path, style, config);
          } else {
            this.drawerLayer(type, path, style, config);
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

        const { geometry, id, properties } = feature;
        const { coordinates } = geometry;
        const { center, iconImage, isCircle, radiusInKm } = properties;
        if (type === 'circle' || isCircle) {
          if (e.action === 'move') {
            const polygon = Turf.polygon(coordinates);
            const centroid = Turf.centroid(polygon);
            const center = centroid.geometry.coordinates;
            change({ center, radius: radiusInKm * 1000 }, id);
          } else {
            change({ center, radius: radiusInKm * 1000 }, id);
          }
          config.center = center;
          config.radius = radiusInKm * 1000;
        } else if (type === 'icon') {
          change(coordinates, id, iconImage);
        } else if (['point', 'polyline'].includes(type)) {
          change(coordinates, id);
        } else {
          const retPoints = coordinates[0].slice(0, -1);
          change(retPoints, id);
        }

        if (!drawEditable) {
          this.saveBeforeDrawer(id, coordinates, style, config);
        }
      };
      this.updateArea = updateArea;

      const deleteArea = () => {
        change(false);
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
     * 绘制图形
     * @param {point | polyline | rectangle | circle | polygon} type 点、线、矩形、圆、多边形
     */
    async drawChangeMode(type, style, config) {
      const { editDrawer, mapId } = this;
      const map = thirdMaps.get(mapId);
      const { imageName, textSize } = style;
      const { change, color } = config;
      const id = `draw-${guid()}`;
      switch (type) {
        case 'icon': {
          const url = await getIconUrlById(imageName);
          const el = document.createElement('div');
          el.id = id;
          el.style['background-image'] = `url(${url})`;
          el.style['background-size'] = 'cover';
          el.style.width = '32px';
          el.style.height = '32px';

          map.once('click', (event) => {
            const lngLat: any = [event.lngLat.lng, event.lngLat.lat];
            const _marker = new mapabcgl.Marker({ draggable: true, element: el })
              .setLngLat(lngLat)
              .addTo(map);
            _marker.on('dragend', (e) => {
              const { lat, lng } = e.target._lngLat;
              change([lng, lat], id, imageName);
            });
            change(lngLat, id, imageName);
            this.markers.set(id, _marker);
            this.keydownDelMarker(el, id, _marker);
          });
          break;
        }
        case 'marker': {
          editDrawer?.changeMode('draw_point');
          break;
        }
        case 'polyline': {
          editDrawer?.changeMode('draw_line_string');
          break;
        }
        case 'text': {
          const onceClick = (event) => {
            const el = document.createElement('input');
            const lngLat: any = [event.lngLat.lng, event.lngLat.lat];
            this.markerPopup(el, formatLonLat(lngLat));
            el.focus();
            el.style.color = '#000';
            el.addEventListener(
              'blur',
              (e: any) => {
                if (e.target?.value !== '') {
                  const el = document.createElement('div');
                  el.id = id;
                  el.innerHTML = e.target?.value;
                  el.style.fontSize = `${textSize}px`;
                  el.style.color = color;
                  el.style.minWidth = '100px';
                  el.style.height = '32px';

                  const _marker = new mapabcgl.Marker({ draggable: true, element: el })
                    .setLngLat(lngLat)
                    .addTo(map);
                  _marker.on('dragend', (e) => {
                    const { lat, lng } = e.target._lngLat;
                    change([lng, lat], id, el.innerHTML);
                  });
                  change(lngLat, id, el.innerHTML);
                  this.markers.set(id, _marker);
                  this.keydownDelMarker(el, id, _marker);
                }
              },
              false,
            );
          };

          this.textClick = onceClick;
          map.once('click', onceClick);
          break;
        }
        default: {
          editDrawer?.changeMode(`draw_${type}`);
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
      const {
        center,
        circleControlLayerId,
        editable = true,
        highlight,
        id,
        layerId,
        lineStyle,
        radius,
      } = config;
      const param = {
        center,
        circleControlLayerId,
        editable,
        highlight,
        id,
        layerId,
        lineStyle,
        path,
        radius,
        style,
        type,
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
      this.editDrawer?.trash();

      const map = thirdMaps.get(this.mapId);
      map.getCanvas().style.cursor = 'pointer';
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
      const layers = Object.keys(map.style._layers);
      this.drawLayer?.forEach(({ layerId }) => {
        layers.push(layerId || '');
      });
      return layers;
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
      return center ? [center.lng, center.lat] : this.center;
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
     * @param {string} layerId 图层id
     * @param {*} sourceMap
     * @param {array} data 图层数据
     * @returns {array} jsonData
     */
    getJsonData(layerId, sourceMap, data) {
      const map = thirdMaps.get(this.mapId);
      const { center } = this;
      const jsonData = {
        features: <any>[],
        type: 'FeatureCollection',
      };
      const { renderer } = this.layers.get(layerId) || {};

      data.forEach((item) => {
        const {
          attendance,
          id,
          name,
          number,
          organizationName,
          position,
          resourceType,
          speed,
          statusName,
          text,
        } = item;
        let newName = statusName || text || number;
        let imageId = layerId;
        if (resourceType === 'Executor') {
          newName = name;
        }
        if (renderer) {
          imageId = renderer?.(item, { map, mapType: 'mapabc' }) || layerId;
        }
        const coordinates = formatLonLat(position) || center;
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
            org: name ? `${name}(${organizationName})` : '',
            speed,
            status: statusName,
            title: text,
          },
          type: 'Feature',
        };
        jsonData.features.push(feature);
        item.position = coordinates;
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
      const map = thirdMaps.get(this.mapId);
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
     */
    getLayerVisible(layerId) {
      const map = thirdMaps.get(this.mapId);
      const layer = map.getLayer(layerId);
      return layer?.visibility === 'visible';
    }

    // 获取当前地图缩放级别
    getZoom() {
      const map = thirdMaps.get(this.mapId);
      return map.getZoom();
    }

    /**
     * @description 初始化地图
     */
    async initMap() {
      const { center, mapId, zoom, zooms } = this;
      let map: any;
      const load = () => {
        if (!mapabcgl.supported()) {
          Message({
            duration: 3000,
            message: '您的浏览器不支持Mapabc GL',
            type: 'error',
          });
          return;
        }

        map = new mapabcgl.Map({
          center,
          container: mapId,
          maxZoom: zooms[1],
          minZoom: zooms[0],
          pitch: 0,
          style: mapStyle,
          zoom,
        });
        map.on('load', () => {
          thirdMaps.set(mapId, map);
          this.isReady = true;
          this.onLoad(this);
          this.registerMapClick(map);
          this.registerDraw(map);
        });

        this.interactiveListen(map);
      };

      if (mapabcgl) {
        load();
      } else {
        const token = `token ${getToken()}`;
        const { mapKey } = getMapConfig();
        const baseApi = import.meta.env.VITE_BASE_API || '';
        const mapVersion = ['1.13.0-dev', '0.16']; // 现场版本

        const getStyle = () => {
          const http = new XMLHttpRequest();
          http.open(
            'GET',
            `${baseApi}/webglapi/styles?n=mapabc80&addSource=true&sourceType=http&ak=${mapKey}`,
            true,
          );
          http.setRequestHeader('Authorization', token);
          http.addEventListener('readystatechange', () => {
            if (http.readyState === XMLHttpRequest.DONE && http.status === 200) {
              const res = JSON.parse(http.responseText);
              if (res.status === '102') {
                Message({
                  duration: 3000,
                  message: res.message,
                  type: 'error',
                });
              }

              mapStyle = res;
              const sources = mapStyle?.sources;
              Object.getOwnPropertyNames(sources).forEach((key) => {
                const str = sources[key].tiles[0];
                const index = str.indexOf('/', 7);
                const tiles = window.location.origin + baseApi + str.substring(index, str.length);
                mapStyle.sources[key].tiles[0] = tiles;
              });
            }
          });
          http.send();
        };
        getStyle();

        const getResource = (item) => {
          return new Promise((resolve) => {
            const http = new XMLHttpRequest();
            // 'ec85d3648154874552835438ac6a02b2';
            http.open('GET', `${baseApi}/webglapi/item?${item}&ak=${mapKey}`, true);
            http.setRequestHeader('Authorization', token);
            http.addEventListener('readystatechange', () => {
              if (http.readyState === XMLHttpRequest.DONE && http.status === 200) {
                const exports: any = {};
                const module = { exports };
                let text = http.responseText;

                if (text.includes('Key校验未通过')) {
                  Message({
                    duration: 3000,
                    message: '地图MAP_KEY校验未通过',
                    type: 'error',
                  });
                }

                if (text.includes('lib not exists')) {
                  resolve(false);
                  return;
                }

                if (item.includes('circle')) {
                  text += 'exports.DragCircleMode = DragCircleMode';
                }

                // eslint-disable-next-line no-eval
                eval(text);

                if (item.includes('n=mapabc-gl-min&')) {
                  const baseApi = import.meta.env.VITE_BASE_API || '';
                  mapabcgl = module.exports;
                  mapabcgl.baseApiUrl = window.location.origin + baseApi;
                  mapabcgl.config = cloneDeep(mapabcgl.config);
                  window.mapabcgl = mapabcgl;
                } else if (item.includes('n=turf-min&')) {
                  turf = module.exports;
                  window.turf = module.exports;
                } else if (item.includes('n=mapabc-gl-draw&')) {
                  mapabcgl.Draw = module.exports;
                } else if (item.includes('n=mapabc-gl-draw-snap-mode&')) {
                  mapboxGlDrawSnapMode = module.exports;
                }
                // else if (item.includes('n=mapabc-gl-draw-rectangle-mode.min&')) {
                //   DrawRectangle = module.exports.default;
                // } else if (item.includes('n=mapabc-gl-draw-circle&')) {
                //   DragCircleMode = module.exports.DragCircleMode;
                // }

                resolve(true);
              }
            });
            http.send();
          });
        };
        await getResource(`n=mapabc-gl-min&t=js&v=${mapVersion[0]}`);
        // if (!MapJs) {
        //   mapVersion = ['3.0.0', '0.17']; // 公网版本
        //   MapJs = await getResource(`n=mapabc-gl-min&t=js&v=${mapVersion[0]}`);
        // }
        mapabcgl.accessToken = mapKey;

        getResource('n=turf-min&t=js&v=1.0');
        getResource('n=mapabc-gl-draw&t=js&v=1.1.0');
        getResource(`n=mapabc-gl-draw-snap-mode&t=js&v=${mapVersion[1]}`);
        // await getResource('n=mapabc-gl-draw-rectangle-mode.min&t=js&v=1.0.0');
        // await getResource('n=mapabc-gl-draw-circle&t=js&v=1.0.0');
        // 加载离线资源url
        if (!window.DragCircleMode) {
          const MapCss = document.createElement('link');
          MapCss.type = 'text/css';
          MapCss.rel = 'stylesheet';
          MapCss.href = `${baseApi}/webglapi/item?n=mapabc-gl-min&t=css&v=${mapVersion[0]}&ak=${mapKey}`;
          document.body.append(MapCss);

          const DrawCss = document.createElement('link');
          DrawCss.type = 'text/css';
          DrawCss.rel = 'stylesheet';
          DrawCss.href = `${baseApi}/webglapi/item?n=mapabc-gl-draw&t=css&v=1.1.0&ak=${mapKey}`;
          document.body.append(DrawCss);

          const publicPath = window.location.origin + import.meta.env.VITE_PUBLIC_PATH;

          // rectangle
          const rectangleJs = document.createElement('script');
          rectangleJs.type = 'text/javascript';
          rectangleJs.src = `${publicPath}/libs/mapAbc/rectangle.js`;
          document.head.append(rectangleJs);

          // circle
          const circleJS = document.createElement('script');
          circleJS.type = 'text/javascript';
          circleJS.src = `${publicPath}/libs/mapAbc/circle.js`;
          document.head.append(circleJS);

          // geojson
          const geojson = document.createElement('script');
          geojson.type = 'text/javascript';
          geojson.src = `${publicPath}/libs/mapAbc/item.js`;
          document.head.append(geojson);
        }
        load();
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
     * 删除键删除循环中的图标
     * @param {*} el dom
     * @param {*} id 图标id
     * @param {*} marker 图标
     */
    keydownDelMarker(el, id, marker) {
      const { color, fontSize } = el.style;
      const { setSelectFeature } = useResourceStore();
      const keydown = (event, _id) => {
        if ([8, 46].includes(event.keyCode)) {
          marker.remove();
          this.markers.delete(_id);
          setSelectFeature({ delete: true, id: _id });
        }
      };

      const fun = (e) => {
        e.stopPropagation();
        e.preventDefault();
        const _id = e.target.id;
        window.addEventListener('keydown', (event) => keydown(event, _id));
        if (fontSize) {
          setSelectFeature({
            feature_type: 'text',
            id: _id,
            textColor: color,
            textSize: fontSize.slice(0, 2),
          });
        }
      };

      el.addEventListener('click', fun);

      const res = this.markers.get(`${id}+event`);
      if (res?.fun) {
        return;
      }
      this.markers.set(`${id}+event`, { fun, keydown });
    }

    /**
     * @description marker弹窗
     * @param {*} el dom
     * @param {*} position 坐标
     * @param {*} offset 偏移量
     */
    markerPopup(el, position, offset?) {
      const map = thirdMaps.get(this.mapId);
      const location = new mapabcgl.LngLat(position[0], position[1]);

      this.closeCustomPopup();

      const newPopup = new mapabcgl.Popup({
        anchor: 'bottom',
        closeButton: false,
        closeOnClick: true,
        offset: offset || [-32, 0],
      });
      this.customPopup = newPopup;
      map.setCenter(position);
      newPopup.setLngLat(location).setDOMContent(el).addTo(map);
    }

    // 将图层的zIndex移到摄像头的层级之上
    moveLayer(layerId) {
      const map = thirdMaps.get(this.mapId);
      const id = 'monitorOffline';
      if (map.getLayer(id)) {
        map.moveLayer(layerId, id);
      }
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
      const { poiUrl } = getMapConfig();
      // const poiUrl = 'http://121.36.99.212:35001/gss/geocode/v2'
      await axios({
        method: 'get',
        params: {
          ak: mapKey,
          location,
        },
        url: poiUrl,
      })
        .then((res) => {
          const { result, status } = res.data;
          if (status === '0') {
            resultData = result[0];
          }
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
      const { city, searchUrl } = getMapConfig();
      // const searchUrl = 'http://121.36.99.212:35001/as/search/poi'
      await axios({
        method: 'get',
        params: {
          ak: mapKey,
          page_num: currentPage,
          page_size: 10,
          query: keywords,
          region: city || '成都',
          scope: 1,
        },
        url: searchUrl,
      })
        .then((res) => {
          const { results, total } = res.data;
          const count = Math.min(total, 200);
          const pois: any = [];
          results.forEach((item) => {
            const { address, location, name, telephone, type, uid } = item;
            pois.push({
              address,
              location: `${location.lng},${location.lat}`,
              name,
              nid: uid,
              phone: telephone,
              type,
            });
          });
          resultData = { count, pois };
        })
        .catch(() => {});
      return resultData;
    }

    /**
     * @description 注册绘图
     */
    registerDraw(map) {
      const { SnapLineMode, SnapModeDrawStyles, SnapPointMode, SnapPolygonMode } =
        mapboxGlDrawSnapMode;

      const draw = new mapabcgl.Draw({
        controls: {
          combine_features: false,
          point: false,
          uncombine_features: false,
        },
        guides: false,
        modes: {
          ...mapabcgl.Draw.modes,
          draw_circle: window.DragCircleMode,
          draw_line_string: SnapLineMode,
          draw_point: SnapPointMode,
          draw_polygon: SnapPolygonMode,
          // @ts-ignore 全局变量
          draw_rectangle: DrawRectangle.default,
        },
        snap: true,
        snapOptions: {
          snapPx: 15,
          snapToMidPoints: true,
          snapVertexPriorityDistance: 1,
        },
        styles: SnapModeDrawStyles, // 绘制样式
        userProperties: true,
      });

      this.editDrawer = draw;
      this.drawBar = new ExtendDrawBar({ draw });

      map.addControl(this.drawBar);
    }

    // 图层点击事件
    registerMapClick(map) {
      const param = {
        show: true,
        type: 'Point',
      };
      map.layerClick(param, (e) => {
        if (e.length > 0) {
          const layerId = e[0].layer.id;
          const { cluster, cluster_id, id, point_count } = e[0].properties;

          const layer = this.layers.get(layerId);
          if (!layer) return;
          const { sourceMap, windowTemplate } = layer;
          if (!windowTemplate) return;

          const source = map.getSource(e[0].source);
          if (cluster) {
            // 聚合
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
      const layer = map.getLayer(layerId);
      if (layer) {
        map.removeLayerAndSource(layerId);
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
     * 保存上一次绘制的图形
     * @param {string} id
     * @param {Array} coordinates
     * @param {Object} style
     * @param {Object} config
     */
    saveBeforeDrawer(id, coordinates, style, config) {
      const { saveBefore = true, type } = config;
      if (saveBefore) {
        let path: any = [];
        path = ['polygon', 'rectangle'].includes(type) ? coordinates[0] : coordinates;
        this.drawerLayer(type, path, style, config);
        this.deleteFeatureById(id);
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
      if (!position) {
        console.log('center is null');
        return;
      }
      map.setCenter(position);
      map.zoomTo(zoom);
    }

    /**
     * 修改绘图颜色
     * @param {Object} style
     */
    setDrawColor(style?) {
      const { fillColor, fillOutlineDasharray, opacity, strokeColor, strokeWeight } = style;
      const { SnapModeDrawStyles } = mapboxGlDrawSnapMode;
      SnapModeDrawStyles.forEach((item) => {
        const { type } = item;
        switch (type) {
          case 'circle': {
            item.paint['circle-color'] = strokeColor;
            break;
          }
          case 'fill': {
            item.paint['fill-color'] = fillColor;
            item.paint['fill-outline-color'] = strokeColor;
            item.paint['fill-opacity'] = opacity;
            break;
          }
          case 'line': {
            item.paint['line-color'] = strokeColor;
            item.paint['line-width'] = strokeWeight;
            if (fillOutlineDasharray === 'true') {
              item.paint['line-dasharray'] = [2, 2];
            } else {
              delete item.paint['line-dasharray'];
            }
            break;
          }
        }
      });
    }

    /**
     * feature 锁定编辑
     * @param {Array} ids features的id数组 不传取全部
     * @param {Boolean} isLock 是否锁定  true锁定 false解锁
     */
    setFeatureLock(isLock, ids?) {
      const map = thirdMaps.get(this.mapId);
      ids?.forEach((id) => {
        const res = this.layers.get(id);
        if (isLock && res) {
          const { fun, keydown } = res;
          map.off('click', id, fun);
          window.removeEventListener('keydown', keydown);
        }
      });
    }

    /**
     * @description 设置图层是否显示
     * @param {array} layerIds
     * @param {boolean} isShow
     */
    setLayersVisible(layerIds, isShow) {
      const visible = isShow ? 'visible' : 'none';
      const map = thirdMaps.get(this.mapId);
      if (map && layerIds?.length) {
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
          const ctLayer = map.getLayer(`${layerId}-cluster`);
          if (layer) {
            map.setLayoutProperty(layerId, 'visibility', visible);
          }
          if (ctLayer) {
            map.setLayoutProperty(`${layerId}-cluster`, 'visibility', visible);
          }
        });
      }
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
      const {
        feature_type,
        fillColor,
        fillOpacity,
        fillOutlineColor,
        id,
        layerId,
        lineColor,
        lineDasharray,
        lineOpacity,
        lineWidth,
        textColor,
        textSize,
      } = val;
      const map = thirdMaps.get(this.mapId);
      if (['circle', 'polygon', 'rectangle'].includes(feature_type)) {
        map.setPaintProperty(layerId, 'fill-color', fillColor);
        map.setPaintProperty(layerId, 'fill-opacity', fillOpacity);
        map.setPaintProperty(layerId, 'fill-outline-color', fillOutlineColor);
      } else
        switch (feature_type) {
          case 'point': {
            map.setPaintProperty(layerId, 'circle-color', fillColor);
            map.setPaintProperty(layerId, 'circle-opacity', fillOpacity);

            break;
          }
          case 'polyline': {
            const dashedArr = lineDasharray === 'true' ? [0.2, 2] : null;
            map.setPaintProperty(layerId, 'line-dasharray', dashedArr);
            map.setPaintProperty(layerId, 'line-color', lineColor);
            map.setPaintProperty(layerId, 'line-width', lineWidth);
            map.setPaintProperty(layerId, 'line-opacity', lineOpacity);

            break;
          }
          case 'text': {
            const _markers = this.markers.get(id);
            _markers.getElement().style.color = textColor;
            _markers.getElement().style.fontSize = `${textSize}px`;

            break;
          }
          // No default
        }
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
     * @description 设置地图图层样式
     * @param {String} key
     */
    setStyle(key) {
      console.log(key);
      // const paths = {
      //   white: 'mapabc://style/mapabc79w-01',
      //   dark: 'mapabc://style/mapabc80-01',
      // };
      // let map = thirdMaps.get(this.mapId);
      // map.setStyle(paths[key]);
      // map = thirdMaps.get(this.mapId);
      // for (const i of this.layers.keys()) {
      //   setTimeout(() => {
      //     this.addLayer(this.layers.get(i));
      //   }, 500);
      // }
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
      this.deleteCustomMarker('defense');
      this.deleteCustomMarker('custom');
    }

    // 返回动向实例
    track(config) {
      return new _Track({ map: this, ...config });
    }

    // 返回轨迹跟踪实例
    trackRealTime(config) {
      return new _TrackRealTime({ map: this, ...config });
    }

    // 坐标转换
    transform(data) {
      return data;
    }

    /**
     * 撤销上一步绘制操作
     * @param {boolean} keepFirstStep 保留第一步
     * @returns
     */
    undoPreviousDraw(config) {
      const { keepFirstStep } = config || {};
      const { drawStepList, editDrawer } = this;

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
    async upDataAddressMarker(type, text, CircleControl?) {
      const parent = document.createElement('div');
      const instance = useCreateApp(KindMarker, { text });
      const el = instance.mount(parent).$el;

      const _popup = new mapabcgl.Popup({
        closeButton: false,
        closeOnClick: false,
        offset: CircleControl ? [-12, 34] : [-19, 22],
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
      if (!map) return;

      const layer = map.getLayer(layerId);
      if (!layer) return;

      // 用户操作过程中更新会让用户感觉卡顿
      if (this.interactiveEvent.length > 0) return;

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

      // setData是替换原来的数据源
      const jsonData = this.getJsonData(layerId, sourceMap, newLayerData);
      map.getSource(layerId).setData(jsonData);

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
      const demo_url = 'http://121.36.99.212:18883/amptraffic?t={z}-{x}-{y}';
      const url = getMapConfig().trafficUrl || demo_url;
      map.addSource('Traffic', {
        tiles: [url],
        traffic: true,
        type: 'vector',
      });
    }
  }

  /**
   * @description Draw 绘制区域
   * @class Draw
   */
  class ExtendDrawBar {
    dMap: any;
    draw: any;
    elContainer: any;
    onAddOrig: any;
    onRemoveOrig: any;
    constructor(opt) {
      this.draw = opt.draw;
      this.onAddOrig = opt.draw.onAdd;
      this.onRemoveOrig = opt.draw.onRemove;
    }

    onAdd(map) {
      this.dMap = map;
      this.elContainer = this.onAddOrig(map);
      return this.elContainer;
    }

    onRemove(map) {
      this.dMap = map;
      this.onRemoveOrig(map);
    }
  }

  /**
   * @description Track 动向
   * @class Track
   */
  class _Track extends BaseTrack {
    currentIndex: number;
    markers: any = [];
    playIndex: number;
    playTimer: any;
    uid = guid();

    constructor(config) {
      super(config);
      this.currentIndex = 0;
      this.playIndex = 0;
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

    // 改变状态
    changeStatus(status) {
      this.status = status;
      this.statusChange?.(status);
    }

    // 销毁动向
    destroyTrack() {
      const { map, markers, uid } = this;
      const mmap = thirdMaps.get(map.mapId);

      markers.forEach((marker) => {
        marker.remove();
      });
      this.markers = [];

      if (mmap.getLayer(`newLineLayer-${uid}`)) {
        mmap.removeLayerAndSource(`newLineLayer-${uid}`);
      }
      if (mmap.getLayer(`carLayer-${uid}`)) {
        mmap.removeLayerAndSource(`carLayer-${uid}`);
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
            'icon-allow-overlap': true,
            'icon-ignore-placement': true,
            'icon-image': moveCarName,
            'icon-offset': [0, 0],
            'icon-rotate': angle,
            'icon-size': 0.8,
          },
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
          if (!mmap.hasImage(moveCarName)) {
            mmap.addImage(moveCarName, img);
          }
          addCar();
        });
      }
    }

    // 画端点
    drawEndpoint(url, lnglat) {
      const { map, markers } = this;
      const mmap = thirdMaps.get(map.mapId);

      const el = document.createElement('div');
      el.id = 'marker';
      el.style['background-image'] = `url(${url})`;
      el.style['background-size'] = 'cover';
      el.style.width = '36px';
      el.style.height = '38px';

      const marker = new mapabcgl.Marker(el, { offset: [0, -16] });
      marker.setLngLat(lnglat).addTo(mmap);
      markers.push(marker);
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

    // 计算两个点之间角度
    getAngle(point1, point2) {
      // -90是因为car图片的初始指向是90°
      return turf.bearing(point1, point2) - 90;
    }

    // 初始化动向
    async initTrack() {
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
      mmap.setPaintProperty(layerId, 'line-width', 8);
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
      this.marker?.remove();
      if (mmap.getLayer('realTimeLineLayer')) {
        mmap.removeLayerAndSource('realTimeLineLayer');
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
      el.style.width = '36px';
      el.style.height = '38px';

      const marker = new mapabcgl.Marker(el, { offset: [0, -16] });
      marker.setLngLat(lnglat).addTo(mmap);
      return marker;
    }

    // 画线
    drawLine() {
      const { color, data, map } = this;
      const mmap = thirdMaps.get(map.mapId);
      const layerId = 'realTimeLineLayer';
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
      this.data = data.map((element) => {
        return map.transform(element);
      });
      map.setCenter(this.data[0]);
      // gis点位画线
      this.drawLine();
      // 画起点
      this.drawEndpoint(startImage, this.data[0]);
      // 画实时位置
      this.marker = this.drawEndpoint(endImage, this.data[this.data.length - 1]);
    }

    updateData(inputData) {
      this.data = inputData.map((element) => {
        return this.map.transform(element);
      });
      this.drawLine();
      // 画实时位置
      this.marker = this.drawEndpoint(this.endImage, this.data[this.data.length - 1]);
    }
  }

  return new _MapAbc(config);
}

export default _MapAbc;
