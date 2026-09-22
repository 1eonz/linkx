<template>
  <div class="content">
    <div :id="mapId" class="map"></div>
    <MapElementTool
      v-if="showTool && mapReady"
      :map-id="mapId"
      :show-fly="showFly"
      :layer-panel-visible="layerPanelVisible"
      :show-detail="showDetail"
      :show-mark-list="showMarkList"
      :list-length="listLength"
      :layers-length="deviceLayers.length"
      @click="handleFlyToCenter"
    />
  </div>
</template>

<script setup lang="ts">
  import { onActivated, onBeforeUnmount, onMounted, reactive, ref, watch, toRef } from 'vue';

  import MapElementTool from './mapElementTool.vue';
  import { flyToCenter, renderDeviceLayers } from './utils';
  import { usePointLoader } from './usePointLoader';
  import { fromLonLat } from 'ol/proj';
  import { getCameraPoints } from '@/common/api/map.js';
  import { useEmitter } from '@/hooks/useEmitter';
  import { wgs84ToGcj02 } from '@/pages/locationShare/tool';
  import { EMap, getMapConfig, mapManager, setStoredMapType } from '@/plugins/map';
  import { useCommunicationStore } from '@/stores/communication.js';

  const props = defineProps({
    mapId: {
      type: String,
      default: 'bottomMap',
    },
    showDeviceLayer: {
      type: Boolean,
      default: false,
    },
    showMarkers: {
      type: Boolean,
      default: false,
    },
    // 是否显示右下角工具条
    showTool: {
      type: Boolean,
      default: true,
    },
    // 是否显示飞到中心按钮
    showFly: {
      type: Boolean,
      default: true,
    },
    // 图层面板是否打开（仅用于调整工具条位置）
    layerPanelVisible: {
      type: Boolean,
      default: false,
    },
    // 详情是否展示（仅用于调整工具条位置）
    showDetail: {
      type: Boolean,
      default: false,
    },
    // 列表是否展示（仅用于调整工具条位置）
    showMarkList: {
      type: Boolean,
      default: false,
    },
    // 列表长度（仅用于调整工具条位置）
    listLength: {
      type: Number,
      default: 0,
    },
    // 地图中心点，可选传入 [lng, lat]
    center: {
      type: Array,
      default: null,
    },
    deviceLayers: { type: Array as any, default: () => [] },
  });

  const emit = defineEmits(['mapReady', 'markerClick']);
  const communicationStore = useCommunicationStore();

  const pageSize = 3000; // 摄像头加载的pageSize（保持不变）
  const mapReady = ref(false);
  let mapObj = null; // 地图对象
  let visibleChangeHandler = null;

  const defaultCenter = ref([104.0668, 30.5728]);
  const mapTypeOptions = [
    { label: '高德地图', value: 'AMap' },
    { label: 'OpenLayers', value: 'OpenLayers' },
  ];
  const currentMapType = ref(mapTypeOptions[0].value);
  const cameraLayerPoints = ref({
    fixedCameraLayer: [],
    cameraLayer: [],
  });
  const userLayerPoints = ref({
    recorderLayer: [],
    personLayer: [],
    surveillanceLayer: [],
    // 新增设备类型
    terminalWecommLayer: [],
    recorder5gLayer: [],
    recorderThirdPartyLayer: [],
    droneLayer: [],
    personalSoldierLayer: [],
    pdtLayer: [],
    terminalLayer: [],
    dingqiaoRecorderLayer: [],
    surveillance2Layer: [],
    gatewayProxyLayer: [],
  });

  const localDeviceLayers = toRef(props, 'deviceLayers');
  const emitter = useEmitter('mapTypeChange', (nextType) => {
    if (!nextType || nextType === currentMapType.value) return;
    switchMap(nextType);
  });

  onMounted(async () => {
    // 监听地图容器的尺寸变化
    const mapElement = document.getElementById(props.mapId);
    if (mapElement) {
      const resizeObserver = new ResizeObserver((entries) => {
        for (const entry of entries) {
          const { width, height } = entry.contentRect;

          // 如果尺寸为 0，地图会黑屏
          if (width === 0 || height === 0) {
            console.error('[bottomMap] 地图容器尺寸为 0，可能导致黑屏');
          }
        }
      });
      resizeObserver.observe(mapElement);

      // 在组件卸载时清理
      onBeforeUnmount(() => {
        resizeObserver.disconnect();
      });

      // 自定义双指缩放实现，防止 NaN
      let lastTouchDistance = 0;
      let lastTouchCenter = null;
      const MIN_TOUCH_DISTANCE = 10; // 最小触摸距离，防止除以 0

      mapElement.addEventListener(
        'touchstart',
        (e) => {
          if (e.touches.length === 2) {
            const dx = e.touches[0].clientX - e.touches[1].clientX;
            const dy = e.touches[0].clientY - e.touches[1].clientY;
            lastTouchDistance = Math.sqrt(dx * dx + dy * dy);

            // 计算双指中心点
            lastTouchCenter = {
              x: (e.touches[0].clientX + e.touches[1].clientX) / 2,
              y: (e.touches[0].clientY + e.touches[1].clientY) / 2,
            };

            // 如果距离太小，设置为最小值
            if (lastTouchDistance < MIN_TOUCH_DISTANCE) {
              lastTouchDistance = MIN_TOUCH_DISTANCE;
            }
          }
        },
        { passive: true },
      );

      mapElement.addEventListener(
        'touchmove',
        (e) => {
          if (e.touches.length === 2) {
            const dx = e.touches[0].clientX - e.touches[1].clientX;
            const dy = e.touches[0].clientY - e.touches[1].clientY;
            const distance = Math.sqrt(dx * dx + dy * dy);

            // 如果距离太小，跳过此次缩放
            if (distance < MIN_TOUCH_DISTANCE || lastTouchDistance < MIN_TOUCH_DISTANCE) {
              lastTouchDistance = distance;
              return;
            }

            const scale = distance / lastTouchDistance;

            // 限制缩放比例范围，防止过快缩放
            const clampedScale = Math.max(0.9, Math.min(1.1, scale));

            // 手动缩放地图
            const map = mapManager.get(props.mapId);
            if (map && map.map) {
              const view = map.map.getView();
              const currentZoom = view.getZoom();

              // 检查当前缩放级别是否正常
              if (isNaN(currentZoom) || !isFinite(currentZoom)) {
                console.error('[bottomMap] 当前缩放级别异常，重置');
                view.setZoom(11);
              } else {
                // 计算新的缩放级别
                const zoomDelta = Math.log2(clampedScale);
                const newZoom = Math.max(3, Math.min(18, currentZoom + zoomDelta));

                // 检查新缩放级别是否正常
                if (!isNaN(newZoom) && isFinite(newZoom)) {
                  view.setZoom(newZoom);
                }
              }
            }

            // 更新上次距离
            lastTouchDistance = distance;
          }
        },
        { passive: true },
      );

      mapElement.addEventListener(
        'touchend',
        () => {
          // 重置状态
          lastTouchDistance = 0;
          lastTouchCenter = null;

          // 延迟检查地图状态
          setTimeout(() => {
            const map = mapManager.get(props.mapId);
            if (map && map.map) {
              const view = map.map.getView();
              const zoom = view.getZoom();
              const center = view.getCenter();

              // 如果状态异常，强制刷新地图
              if (
                isNaN(zoom) ||
                !isFinite(zoom) ||
                !center ||
                isNaN(center[0]) ||
                isNaN(center[1])
              ) {
                console.error('[bottomMap] 地图状态异常，强制刷新');

                // 重置状态
                if (isNaN(zoom) || !isFinite(zoom)) {
                  view.setZoom(11);
                }
                if (!center || isNaN(center[0]) || isNaN(center[1])) {
                  view.setCenter(fromLonLat([104.0668, 30.5728]));
                }

                // 强制重新渲染
                map.map.render();
                map.map.updateSize();
              }
            }
          }, 100);
        },
        { passive: true },
      );
    }

    visibleChangeHandler = (val) => {
      // val 可能是 boolean 或 string 类型
      const isVisible = val === true || val === 'true';

      if (!isVisible) {
        // 页面不可见时销毁地图，释放资源
        destroyMap();
      } else {
        // 页面重新可见时，重新初始化地图
        // 使用 setTimeout 确保 DOM 已经准备好
        setTimeout(() => {
          const mapElement = document.getElementById(props.mapId);
          if (mapElement && !mapManager.get(props.mapId)) {
            initEMap();
          }
        }, 100);
      }
    };
    window.WeSpaceSDK?.onVisibleChange(visibleChangeHandler);

    initdeviceLayers();
    // 先拿到配置再初始化，避免 mapType 退回默认 AMap
    try {
      const config = await getMapConfig();
      if (Array.isArray(config?.center) && config.center.length === 2) {
        defaultCenter.value = config.center;
      }
      if (config?.mapType) {
        currentMapType.value = config.mapType;
      }
    } catch (error) {
      console.warn('获取地图配置失败，使用默认配置', error);
    }

    // 确保 DOM 已经渲染完成
    setTimeout(initEMap, 0);
  });

  onActivated(() => {
    // 地图被隐藏后重新显示
    const mapObj = mapManager.get(props.mapId);
    if (mapObj && mapObj.resize) {
      mapObj.resize();
    }
  });

  onBeforeUnmount(() => {
    // 停止后台加载
    stopBackgroundLoading();

    // 移除 WeSpaceSDK 的可见性变化监听器
    if (window.WeSpaceSDK && window.WeSpaceSDK.offVisibleChange && visibleChangeHandler) {
      try {
        window.WeSpaceSDK.offVisibleChange(visibleChangeHandler);
      } catch (error) {
        console.warn('移除 WeSpaceSDK 监听器时出错:', error);
      }
      visibleChangeHandler = null;
    }

    // 销毁地图实例
    destroyMap();
  });

  function initEMap() {
    console.log('[bottomMap] initEMap 开始', {
      mapId: props.mapId,
      mapType: currentMapType.value,
      existingMap: !!mapManager.get(props.mapId),
    });

    // 检查地图容器是否存在
    const mapElement = document.getElementById(props.mapId);
    if (!mapElement) {
      console.error(`地图容器 ${props.mapId} 不存在`);
      return;
    }

    //  关键调试：检查地图容器的计算样式
    const computedStyle = window.getComputedStyle(mapElement);
    console.log('[bottomMap] 地图容器样式:', {
      width: computedStyle.width,
      height: computedStyle.height,
      display: computedStyle.display,
      visibility: computedStyle.visibility,
      opacity: computedStyle.opacity,
      zIndex: computedStyle.zIndex,
      position: computedStyle.position,
    });

    //  关键修复：检查并清理已存在的旧实例
    const existingMap = mapManager.get(props.mapId);
    if (existingMap) {
      console.warn(`[bottomMap] 地图实例 ${props.mapId} 已存在，先销毁旧实例`);
      try {
        existingMap.destroyMap();
      } catch (error) {
        console.warn('[bottomMap] 销毁旧地图实例时出错:', error);
      }
      mapManager.delete(props.mapId);
      mapObj = null;
      mapReady.value = false;
    }

    // 清空地图容器，确保干净的初始化环境
    if (mapElement.innerHTML) {
      console.log('[bottomMap] 清空地图容器');
      mapElement.innerHTML = '';
    }

    const config = {
      mapId: props.mapId,
      mapType: currentMapType.value,
      center:
        Array.isArray(props.center) && props.center.length === 2
          ? props.center
          : defaultCenter.value, // 如果未传，则使用默认成都中心点
      zoom: 11,
      zooms: [3, 18],
      onLoad: async (context) => {
        console.log('[bottomMap] 地图加载完成');
        mapObj = context;
        mapReady.value = true;
        emit('mapReady', context);
        if (props.showMarkers) {
          // 添加 await 和错误处理
          try {
            await renderUserLayers();
            console.log('[bottomMap] 用户图层渲染完成');
          } catch (error) {
            console.error('[bottomMap] 用户图层渲染失败:', error);
          }
          //在地图移动结束后获取最新视野范围
          context.map?.on('moveend', () => {
            getViewPoints();
          });
        }
      },
    };

    try {
      EMap(config);
    } catch (error) {
      console.error('初始化地图失败:', error);
    }
  }

  function destroyMap() {
    const { mapId } = props;
    const map = mapManager.get(mapId);
    if (map) {
      try {
        map.destroyMap();
      } catch (error) {
        console.warn('销毁地图实例时出错:', error);
      }
      mapManager.delete(mapId);
    }
    // 清空 mapObj 变量
    mapObj = null;
    mapReady.value = false;
    // 重置地图容器
    const mapElement = document.getElementById(mapId);
    if (mapElement) {
      mapElement.innerHTML = '';
    }
  }

  // 监听外部传入的中心点，地图准备好后动态更新
  watch(
    () => props.center,
    (val) => {
      if (mapReady.value && Array.isArray(val) && val.length === 2 && mapObj?.setCenter) {
        mapObj.setCenter(val);
      }
    },
  );

  function handleFlyToCenter() {
    // 优先飞到传入的中心（当前经纬度），否则使用地图当前中心/默认值
    let targetCenter =
      (Array.isArray(props.center) && props.center.length === 2 && props.center) ||
      mapObj?.center ||
      defaultCenter.value;

    // 将 WGS84 坐标转换为 GCJ-02（高德）坐标
    if (Array.isArray(targetCenter) && targetCenter.length === 2) {
      const [lng, lat] = targetCenter;
      targetCenter = wgs84ToGcj02(lng, lat);
    }

    const targetZoom =
      (typeof mapObj?.getZoom === 'function' && mapObj.getZoom()) || mapObj?.zoom || 11;

    if (mapObj?.flyAction) {
      mapObj.flyAction(targetCenter, targetZoom);
    } else if (mapObj?.setCenter) {
      mapObj.setCenter(targetCenter, targetZoom);
    } else {
      flyToCenter(props.mapId);
    }
  }

  //图层显示隐藏状态
  async function initdeviceLayers() {
    if (!props.showDeviceLayer) {
      return;
    }
    try {
      const invisibleLayer = await communicationStore.getStorage('invisibleLayer');
      if (invisibleLayer) {
        const invisibleIdArr = invisibleLayer.split(',');
        localDeviceLayers.value.forEach((i) => (i.visible = !invisibleIdArr.includes(i.id)));
      }
    } catch (error) {
      console.error('获取图层失败:', error);
    }
  }

  function switchMap(type) {
    currentMapType.value = type;
    setStoredMapType(type);
    mapReady.value = false;
    if (mapObj) {
      mapObj.destroyMap();
      mapObj = null;
    }
    setTimeout(initEMap, 0);
  }

  function handleMapTypeSelect(type) {
    if (type === currentMapType.value) return;
    emitter?.emit('mapTypeChange', type);
  }

  // 可视区域左上、右下坐标点
  function getViewPoints() {
    if (!mapObj) return;
    const currentZoom = mapObj.getZoom();
    if (currentZoom < 14) {
      // 移除所有摄像头图层（固定摄像头 + 三方摄像头）
      const cameraLayers = props.deviceLayers.filter((item) => item.category === '1');
      cameraLayers.forEach((layer) => mapObj.removeLayer(layer.key));
      return;
    }
    const { lp, rp } = mapObj.getViewPoints();
    renderCameraLayers(lp, rp);
  }

  // 摄像头图层
  async function renderCameraLayers(lp, rp) {
    try {
      // 按摄像头子类分桶：固定摄像头(fixedCameraLayer) 和 三方摄像头(cameraLayer)
      const cameraBuckets = {
        fixedCameraLayer: [],
        cameraLayer: [],
      };

      let cameraPageNum = 1;
      while (true) {
        const params = {
          pageNum: cameraPageNum,
          pageSize,
          lp,
          rp,
          hasLocation: 1,
        };
        const res = await getCameraPoints(params);
        console.log('获取摄像头', '/icp/camera', res);
        const list = res?.records || [];
        if (list.length === 0) break;
        list.forEach((item) => {
          const { lon, lat, statusValue, apptype } = item || {};
          if (lon && lat && !isNaN(Number(lon)) && !isNaN(Number(lat))) {
            // 按 apptype 区分子类，和 getEquipmentType 的判断保持一致
            const bucketKey = Number(apptype) === 111 ? 'fixedCameraLayer' : 'cameraLayer';
            cameraBuckets[bucketKey].push({
              ...item,
              position: [Number(lon), Number(lat)],
              type: Number(apptype) === 111 ? 'fixedCamera' : 'camera',
              status: Number(statusValue) === 4011 ? 'online' : 'offline',
            });
          }
        });
        if (list.length < pageSize) break;
        cameraPageNum++;
      }

      cameraLayerPoints.value = cameraBuckets;
      initdeviceLayers();
      const cameraLayers = props.deviceLayers.filter((item) => item.category === '1');
      await renderDeviceLayers(mapObj, cameraLayers, cameraLayerPoints.value, (payload) =>
        emit('markerClick', payload),
      );
    } catch (error) {
      console.error('获取摄像头失败', '/icp/camera', error);
    }
  }

  // ==================== 点位加载器 ====================

  /**
   * 使用点位加载器 Hook
   * - 分批渐进加载点位数据
   * - 队列处理机制，避免并发冲突
   * - 智能暂停，控制内存占用
   */
  const {
    loadingProgress,
    loadPoints,
    stopLoading,
    processEquipmentData,
    processUserData,
    mergePointsData,
  } = usePointLoader({
    // 设备类型
    deviceLayers: props.deviceLayers,
    // 点位更新回调
    onPointsUpdate: (newData) => {
      console.info('[bottomMap] onPointsUpdate 回调触发:', {
        recorderPoints: newData.recorderPoints?.length || 0,
        surveillancePoints: newData.surveillancePoints?.length || 0,
        personPoints: newData.personPoints?.length || 0,
        // 新增设备类型
        terminalWecommPoints: newData.terminalWecommPoints?.length || 0,
        recorder5gPoints: newData.recorder5gPoints?.length || 0,
        recorderThirdPartyPoints: newData.recorderThirdPartyPoints?.length || 0,
        dronePoints: newData.dronePoints?.length || 0,
        personalSoldierPoints: newData.personalSoldierPoints?.length || 0,
        pdtPoints: newData.pdtPoints?.length || 0,
        terminalPoints: newData.terminalPoints?.length || 0,
        dingqiaoRecorderPoints: newData.dingqiaoRecorderPoints?.length || 0,
        gatewayProxyPoints: newData.gatewayProxyPoints?.length || 0,
      });

      // 合并到现有数据
      userLayerPoints.value = mergePointsData(userLayerPoints.value, newData);

      console.info('[bottomMap] 合并后的 userLayerPoints:', {
        recorderLayer: userLayerPoints.value.recorderLayer?.length || 0,
        personLayer: userLayerPoints.value.personLayer?.length || 0,
        surveillanceLayer: userLayerPoints.value.surveillanceLayer?.length || 0,
           // 新增设备类型
        terminalWecommPoints: newData.terminalWecommPoints?.length || 0,
        recorder5gPoints: newData.recorder5gPoints?.length || 0,
        recorderThirdPartyPoints: newData.recorderThirdPartyPoints?.length || 0,
        dronePoints: newData.dronePoints?.length || 0,
        personalSoldierPoints: newData.personalSoldierPoints?.length || 0,
        pdtPoints: newData.pdtPoints?.length || 0,
        terminalPoints: newData.terminalPoints?.length || 0,
        dingqiaoRecorderPoints: newData.dingqiaoRecorderPoints?.length || 0,
        gatewayProxyPoints: newData.gatewayProxyPoints?.length || 0,
      });

      // 重新渲染图层
      // const userLayers = props.deviceLayers.slice(1, 4);
      const userLayers = props.deviceLayers.filter((item) => item.category !== '1');
      console.log(userLayers,'====userLayers====')
      console.info('[bottomMap] 准备渲染用户图层:', {
        layerIds: userLayers.map((l) => l.id),
        layerNames: userLayers.map((l) => l.name),
      });

      renderDeviceLayers(mapObj, userLayers, userLayerPoints.value, (payload) =>
        emit('markerClick', payload),
      );
    },
    // 进度更新回调
    onProgress: (progress) => {
      console.log('[bottomMap] 加载进度:', progress);
    },
  });

  /**
   * 渲染用户/记录仪/布控球图层（优化版）
   *
   * 优化策略：
   * 1. 首批快速加载1000个点位，立即渲染（1-2秒）
   * 2. 后台渐进加载剩余数据，增量添加（用户无感知）
   * 3. 用户可立即操作地图，不用等待全部加载完成
   */
  async function renderUserLayers() {
    await loadPoints();
  }

  /**
   * 停止后台加载
   * - 页面卸载时调用
   */
  function stopBackgroundLoading() {
    stopLoading();
  }
</script>

<style lang="scss" scoped>
  .content {
    width: 100%;
    height: 100%;
    position: relative;

    .map {
      position: absolute; //  修复：使用 absolute 替代 relative + float
      top: 0;
      left: 0;
      width: 100%;
      height: 100%;
      background-color: #1e1e1e;
      //  关键：防止触摸事件穿透
      touch-action: pan-x pan-y pinch-zoom;
      //  关键：防止 GPU 加速导致的黑屏
      transform: translateZ(0);
      backface-visibility: hidden;
      //  新增：确保 Canvas 可见
      overflow: visible;
      z-index: 0;
    }
  }

  //  新增：确保 OpenLayers Canvas 样式正确
  .map :deep(canvas) {
    display: block !important;
    visibility: visible !important;
    opacity: 1 !important;
  }

  .map-type-switcher {
    position: absolute;
    top: 20px;
    left: 20px;
    display: flex;
    gap: 8px;
    z-index: 30;

    &__btn {
      padding: 6px 12px;
      border-radius: 16px;
      border: 1px solid rgba(90, 153, 255, 0.6);
      background: rgba(4, 22, 39, 0.8);
      color: #e5f2ff;
      font-size: 12px;
      cursor: pointer;
      transition: all 0.2s;

      &:hover,
      &.active {
        background: rgba(90, 153, 255, 0.9);
        color: #fff;
        border-color: transparent;
      }
    }
  }

  .layer-btn {
    position: absolute;
    top: 16px;
    right: 16px;
    display: flex;
    align-items: center;
    gap: 8px;
    z-index: 30;
    cursor: pointer;
    display: flex;
    align-items: center;
    gap: 2px;
    flex-direction: column;
    background: #fff;
    border-radius: 10px;
    padding: 12px;
    z-index: 1;
    .layer-btn__img {
      width: 24px;
      height: 24px;
    }
    .layer-btn__text {
      font-size: 12px;
      color: rgba(59, 114, 255, 1);
    }
  }

  .cluster-marker {
    font-size: 12px;
    text-align: center;
    line-height: 1.35;
  }
</style>
