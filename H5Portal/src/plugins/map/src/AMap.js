import { BaseMap, formatLonLat, mapIsReady } from '../helper';

let AMap = null;

/**
 * @description 高德地图封装
 * @class _AMap
 */
class _AMap extends BaseMap {
  constructor(config) {
    super(config);
    this.initMap();
  }

  async initMap() {
    try {
      // 检查 DOM 元素是否存在
      const mapElement = document.getElementById(this.mapId);
      if (!mapElement) {
        console.error(`地图容器 ${this.mapId} 不存在`);
        // 等待 DOM 准备好
        await new Promise((resolve) => setTimeout(resolve, 100));
        const retryElement = document.getElementById(this.mapId);
        if (!retryElement) {
          console.error(`地图容器 ${this.mapId} 仍然不存在`);
          return;
        }
      }

      // 动态加载高德地图 SDK
      if (typeof window !== 'undefined' && !window.AMap) {
        await this.loadAMapScript();
      }

      AMap = window.AMap;
      if (!AMap) {
        console.error('高德地图 SDK 加载失败');
        return;
      }

      const { center, zoom, zooms, mapId } = this;

      // 添加调试信息
      console.log('地图初始化参数:', { center, zoom, zooms, mapId });

      // 验证 center 是否为有效数组
      let finalCenter = center;
      if (!Array.isArray(center) || center.length !== 2) {
        console.error(`无效的中心点坐标: ${JSON.stringify(center)}，使用默认值`);
        finalCenter = [104.0668, 30.5728];
        this.center = finalCenter;
      } else {
        const [lng, lat] = center.map(Number);
        if (isNaN(lng) || isNaN(lat)) {
          console.error(`中心点坐标包含 NaN: [${lng}, ${lat}]，使用默认值`);
          finalCenter = [104.0668, 30.5728];
          this.center = finalCenter;
        } else {
          finalCenter = [lng, lat];
        }
      }

      // 创建地图实例
      const mapOptions = {
        center: finalCenter,
        zoom: Number(zoom) || 11,
        zooms: Array.isArray(zooms) ? zooms.map(Number) : [3, 18],
        viewMode: '3D',
        mapStyle: 'amap://styles/normal',
      };

      console.log('创建地图实例，参数:', mapOptions);

      this.map = new AMap.Map(mapId, mapOptions);

      // 明确打开所有缩放交互能力，避免被外部配置覆盖
      const zoomStatus = {
        scrollWheel: true,
        doubleClickZoom: true,
        zoomEnable: true,
        touchZoom: true,
        keyboardEnable: true,
      };
      if (typeof this.map.setStatus === 'function') {
        this.map.setStatus(zoomStatus);
      } else {
        console.warn('当前 AMap 版本缺少 setStatus 方法，请检查 SDK 版本');
      }

      // 地图加载完成
      this.map.on('complete', () => {
        this.isReady = true;
        if (this.onLoad) {
          this.onLoad(this);
        }
      });

      // 错误处理
      this.map.on('error', (error) => {
        console.error('地图加载错误:', error);
      });
    } catch (error) {
      console.error('初始化地图失败:', error);
    }
  }

  loadAMapScript() {
    return new Promise((resolve, reject) => {
      if (window.AMap) {
        resolve();
        return;
      }

      // 配置安全密钥（如果需要，请在高德开放平台获取）
      // 如果您的 key 需要安全密钥，请取消下面的注释并填入您的安全密钥
      // window._AMapSecurityConfig = {
      //   securityJsCode: '您的安全密钥'
      // };

      // 检查是否已经有加载中的脚本
      const existingScript = document.querySelector('script[src*="webapi.amap.com/maps"]');
      if (existingScript) {
        // 等待现有脚本加载完成
        const checkInterval = setInterval(() => {
          if (window.AMap) {
            clearInterval(checkInterval);
            resolve();
          }
        }, 100);

        // 设置超时，避免无限等待
        setTimeout(() => {
          clearInterval(checkInterval);
          if (!window.AMap) {
            reject(new Error('等待现有脚本加载超时'));
          }
        }, 10000);
        return;
      }

      // 生成唯一的回调函数名，避免冲突
      const callbackName = `initAMap_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

      const script = document.createElement('script');
      script.type = 'text/javascript';
      script.async = true;
      script.defer = true;

      // 使用正确的 URL 格式，确保版本号正确传递
      // 注意：高德地图 API 需要明确的版本号，不能使用 undefined
      const apiKey = 'ebd4af226c75a32abd5fc4b879b0da92';
      const apiVersion = '1.4.15';
      script.src = `https://webapi.amap.com/maps?v=${apiVersion}&key=${apiKey}&plugin=AMap.MarkerClusterer,AMap.MouseTool,AMap.PolyEditor,AMap.RectangleEditor,AMap.CircleEditor&callback=${callbackName}`;

      console.log('加载高德地图 SDK:', script.src);

      script.onerror = () => {
        console.error('高德地图 SDK 脚本加载失败');
        delete window[callbackName];
        reject(new Error('高德地图 SDK 脚本加载失败'));
      };

      // 设置超时
      const timeout = setTimeout(() => {
        console.error('高德地图 SDK 加载超时');
        delete window[callbackName];
        reject(new Error('高德地图 SDK 加载超时'));
      }, 15000);

      // 设置回调函数
      window[callbackName] = () => {
        clearTimeout(timeout);
        delete window[callbackName];

        // 确保 AMap 对象已完全加载
        if (window.AMap && window.AMap.Map) {
          console.log('高德地图 SDK 加载成功');
          resolve();
        } else {
          // 如果还没完全加载，再等待一下
          setTimeout(() => {
            if (window.AMap && window.AMap.Map) {
              console.log('高德地图 SDK 加载成功（延迟）');
              resolve();
            } else {
              console.error('高德地图 SDK 对象未正确初始化');
              reject(new Error('高德地图 SDK 初始化失败：AMap 对象不存在'));
            }
          }, 200);
        }
      };

      // 先设置回调，再添加脚本
      document.head.appendChild(script);
    });
  }

  // 获取地图中心点
  getCenter() {
    if (!this.map) return this.center;
    const center = this.map.getCenter();
    if (!center) return this.center;
    if (typeof center.toArray === 'function') {
      return center.toArray();
    }
    const lng = center.lng ?? center.getLng?.();
    const lat = center.lat ?? center.getLat?.();
    if (typeof lng === 'number' && typeof lat === 'number') {
      return [lng, lat];
    }
    console.warn('AMap.getCenter 返回未知格式，使用缓存中心点', center);
    return this.center;
  }

  // 创建/更新聚合图层
  async addMarkerCluster(layerId, points = [], options = {}) {
    await mapIsReady(this);

    if (!this.map || !AMap) {
      console.warn('地图尚未初始化，无法添加聚合图层');
      return;
    }
    if (!layerId) {
      console.warn('layerId 不能为空');
      return;
    }
    if (!Array.isArray(points) || points.length === 0) {
      console.warn(`图层 ${layerId} 没有可用点位数据`);
      this.removeLayer(layerId);
      return;
    }

    this.removeLayer(layerId);

    const {
      color = '#36c',
      size = 18,
      clusterSize = 80,
      renderClusterMarker,
      renderMarker,
      image,
      clusterImage,
      offset = new AMap.Pixel(-12, -12),
      visible = true,
      zIndex = 120,
      onMarkerClick,
    } = options;

    if (typeof onMarkerClick === 'function') {
      this.registerLayerClickHandler(layerId, onMarkerClick);
    } else {
      this.registerLayerClickHandler(layerId);
    }

    const createMarkerContent = (data) => {
      if (typeof renderMarker === 'function') {
        return renderMarker(data);
      }
      // 如果提供了图片，使用图片
      if (image) {
        const isOnline = data?.status === 'online';
        // 在线：绿色圆点 rgba(67, 207, 124, 1)
        // 离线：灰色圆点 rgba(166, 169, 171, 1)
        const dotColor = isOnline ? 'rgba(67, 207, 124, 1)' : 'rgba(166, 169, 171, 1)';
        const dotSize = '8px'; // 圆点大小
        const diameter = `${size}px`;
        const style = [
          'display:flex',
          'align-items:center',
          'justify-content:center',
          'position:relative',
          `width:${diameter}`,
          `height:${diameter}`,
        ].join(';');
        return `<div style="${style}">
          <img src="${image}" style="width:100%;height:100%;object-fit:contain;" alt="marker" />
          <div style="position:absolute;top:0;right:0;width:${dotSize};height:${dotSize};background:${dotColor};border:1px solid #fff;border-radius:50%;transform:translate(25%, -25%);"></div>
        </div>`;
      }
      // 默认样式
      const diameter = `${size}px`;
      const style = [
        'display:flex',
        'align-items:center',
        'justify-content:center',
        'color:#fff',
        'font-size:12px',
        'border-radius:50%',
        'border:2px solid rgba(255,255,255,0.75)',
        `width:${diameter}`,
        `height:${diameter}`,
        `background:${color}`,
        'box-shadow:0 0 8px rgba(0,0,0,0.35)',
      ].join(';');
      return `<div style="${style}"></div>`;
    };

    const markerList = points
      .map((item) => {
        const position = formatLonLat(item.position || item.lnglat || item.lngLat);
        if (!position) return null;
        const marker = new AMap.Marker({
          position,
          content: createMarkerContent(item),
          offset,
          zIndex,
          extData: { ...item, layerId },
        });
        if (typeof onMarkerClick === 'function') {
          marker.on('click', (event) => {
            const extData = marker.getExtData() || item;
            this.triggerMarkerClick(layerId, {
              layerId,
              data: extData,
              clusterData: extData ? [extData] : [],
              initialIndex: 0,
              event,
              mapInstance: this,
            });
          });
        }
        return marker;
      })
      .filter(Boolean);

    if (markerList.length === 0) {
      console.warn(`图层 ${layerId} 没有可用 marker`);
      return;
    }

    const clusterOptions = {
      gridSize: clusterSize,
      // 自定义聚合点点击行为，关闭默认点击放大
      zoomOnClick: false,
      renderClusterMarker:
        typeof renderClusterMarker === 'function'
          ? renderClusterMarker
          : (context) => {
              const div = document.createElement('div');
              const count = context.count;
              const displayCount = count > 99 ? '99+' : count;
              // 如果提供了聚合图片，使用图片
              if (clusterImage) {
                div.style.cssText = [
                  'position:relative',
                  'width:32px',
                  'height:32px',
                  'display:flex',
                  'align-items:center',
                  'justify-content:center',
                ].join(';');

                const img = document.createElement('img');
                img.src = clusterImage;
                img.style.cssText = ['width:100%', 'height:100%', 'object-fit:contain'].join(';');
                img.alt = 'cluster';

                const countText = document.createElement('span');
                countText.textContent = displayCount;
                countText.style.cssText = [
                  'position:absolute',
                  'top:50%',
                  'left:50%',
                  'transform:translate(-50%, -50%)',
                  'color:#fff',
                  'font-size:14px',
                  'font-weight:bold',
                  'text-shadow:0 1px 2px rgba(0,0,0,0.8)',
                  'pointer-events:none',
                ].join(';');

                div.appendChild(img);
                div.appendChild(countText);
              } else {
                // 默认样式
                const clusterColor = color;
                const style = [
                  'min-width:34px',
                  'height:34px',
                  'padding:4px',
                  'border-radius:17px',
                  'box-sizing:border-box',
                  'background:rgba(0,0,0,0.7)',
                  `border:3px solid ${clusterColor}`,
                  'display:flex',
                  'align-items:center',
                  'justify-content:center',
                  'color:#fff',
                  'font-size:12px',
                  'box-shadow:0 0 10px rgba(0,0,0,0.45)',
                ].join(';');
                div.setAttribute('style', style);
                div.innerHTML = `${displayCount}`;
              }
              context.marker.setContent(div);
              context.marker.setOffset(new AMap.Pixel(-24, -24));
            },
    };

    const cluster = new AMap.MarkerClusterer(this.map, markerList, clusterOptions);
    if (typeof onMarkerClick === 'function' && cluster?.on) {
      cluster.on('click', (event) => {
        const markerGetter = event?.cluster?.getMarkers?.bind(event.cluster);
        const clusterMarkers =
          (typeof markerGetter === 'function' ? markerGetter() : null) ||
          event?.markers ||
          event?.cluster?.markers ||
          [];
        const clusterData = (clusterMarkers || [])
          .map((marker) => marker?.getExtData?.())
          .filter(Boolean);
        if (clusterData.length === 0) return;
        this.triggerMarkerClick(layerId, {
          layerId,
          data: clusterData[0],
          clusterData,
          initialIndex: 0,
          event,
          mapInstance: this,
        });
      });
    }
    if (!visible) {
      cluster.setMap(null);
    }

    this.layers.set(layerId, {
      type: 'cluster',
      cluster,
      markers: markerList,
      visible,
      options,
    });
    this.clusters.set(layerId, cluster);
    this.markers.set(layerId, markerList);
  }

  // 图层显隐
  setLayerVisible(layerId, visible) {
    const layer = this.layers.get(layerId);
    if (!layer) return;
    if (layer.cluster) {
      layer.cluster.setMap(visible ? this.map : null);
    } else if (Array.isArray(layer.markers)) {
      layer.markers.forEach((marker) => {
        if (visible) {
          marker.show();
        } else {
          marker.hide();
        }
      });
    }
    layer.visible = visible;
  }

  // 删除图层
  removeLayer(layerId) {
    const layer = this.layers.get(layerId);
    if (!layer) return;
    if (layer.cluster) {
      layer.cluster.setMap(null);
    }
    if (Array.isArray(layer.markers)) {
      layer.markers.forEach((marker) => marker.setMap(null));
    }
    this.layers.delete(layerId);
    this.clusters.delete(layerId);
    this.markers.delete(layerId);
    this.registerLayerClickHandler(layerId);
  }

  // 设置地图中心点和缩放级别
  setCenter(center, zoom) {
    if (this.map) {
      if (zoom !== undefined) {
        this.map.setZoomAndCenter(zoom, center);
      } else {
        this.map.setCenter(center);
      }
    }
  }

  // 获取缩放级别
  getZoom() {
    return this.map ? this.map.getZoom() : this.zoom;
  }

  // 飞行到指定位置
  flyAction(position, zoom, duration = 1000) {
    if (this.map) {
      if (zoom !== undefined) {
        this.map.setZoomAndCenter(zoom, position, true, duration);
      } else {
        this.map.setCenter(position, true, duration);
      }
    }
  }

  // 地图大小调整
  resize() {
    if (this.map) {
      this.map.getSize();
      setTimeout(() => {
        this.map.resize();
      }, 100);
    }
  }

  // 销毁地图
  destroyMap() {
    // 移除所有图层
    for (const layerId of this.layers.keys()) {
      this.removeLayer(layerId);
    }

    // 清空所有集合
    this.layers.clear();
    this.clusters.clear();
    this.markers.clear();
    this.layerClickHandlers?.clear?.();

    // 销毁地图实例
    if (this.map) {
      try {
        // 移除地图上的所有控件和覆盖物
        this.map.clearMap();
        // 销毁地图实例
        this.map.destroy();
      } catch (error) {
        console.warn('销毁地图时出错:', error);
      }
      this.map = null;
      this.isReady = false;
    }
  }

  // 添加比例尺和位置显示（简化版）
  addScaleAndPosition(scaleRef, positionRef) {
    if (!this.map || !AMap) return;

    try {
      // 添加比例尺控件
      this.map.addControl(
        new AMap.Scale({
          position: 'LB',
        }),
      );

      // 监听鼠标移动，更新位置显示
      if (positionRef) {
        this.map.on('mousemove', () => {
          // if (positionRef.value) {
          //   const { lng, lat } = e.lnglat;
          //   positionRef.value.textContent = `经度: ${lng.toFixed(
          //     6
          //   )}, 纬度: ${lat.toFixed(6)}`;
          // }
        });
      }
    } catch (error) {
      console.warn('添加比例尺控件失败:', error);
    }
  }

  //计算当前视野范围
  getViewPoints() {
    const { path } = this.map.getBounds();
    const minX = path[0][0];
    const maxY = path[0][1];
    const maxX = path[2][0];
    const minY = path[2][1];
    return [minX, minY, maxX, maxY];
  }
}

export default _AMap;
