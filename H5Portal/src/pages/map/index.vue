<template>
  <div class="map-box">
    <!-- <div class="map-type-switcher">
      <button
        v-for="item in mapTypeOptions"
        :key="item.value"
        class="map-type-switcher__btn"
        :class="{ active: item.value === currentMapType }"
        @click="handleMapTypeSelect(item.value)"
      >
        {{ item.label }}
      </button>
    </div> -->
    <!-- 顶部导航栏 -->
    <van-icon
      class="nav-back"
      name="arrow-left"
      :size="adaptationSize.iconSize"
      color="#333"
      :style="{ top: statusBarHeight + 16 + 'px' }"
      @click="goBack"
    />
    <div v-if="showDeviceLayer" class="layer-btn" :style="{ top: statusBarHeight + 30 + 'px' }" @click="setLayerPanel(true)">
      <img src="@/assets/images/map/layers_icon.png" alt="" class="layer-btn__img" />
      <div class="layer-btn__text">图层</div>
    </div>

    <BottomMap
      :map-id="mapId"
      :show-device-layer="showDeviceLayer"
      :show-markers="showMarkers"
      :show-tool="!markDetailVisible"
      :show-fly="showFly"
      :device-layers="deviceLayers"
      :layer-panel-visible="layerPanelVisible"
      :show-mark-list="showMarkList"
      :list-length="activeMarkerList.length"
      @mapReady="handleMapReady"
      @markerClick="handleMarkerClick"
    />

    <DeviceLayerPanel
      v-if="mapReady && layerPanelVisible"
      :layers="deviceLayers"
      :points-map="layerPoints"
      @toggle="handleLayerToggle"
      @setLayerPanel="setLayerPanel"
    />

    <MarkList
      v-if="showMarkList && activeMarkerList.length"
      :list="activeMarkerList"
      :layers="deviceLayers"
      @view="handleListItemView"
      @close="handleListClose"
    />

    <MarkDetail
      v-if="markDetailVisible && currentMarkerDetail"
      :detail="currentMarkerDetail"
      :index="activeMarkerIndex"
      :total="markerTotal"
      :layers="deviceLayers"
      @close="handleDetailClose"
      @prev="handleDetailPrev"
      @next="handleDetailNext"
    />
  </div>
</template>

<script lang="ts" setup>
  import { computed, nextTick, onMounted, reactive, ref, shallowRef, watch } from 'vue';

  import BottomMap from './bottomMap.vue';
  import DeviceLayerPanel from './deviceLayerPanel.vue';
  import MarkDetail from './markDetail.vue';
  import MarkList from './markList.vue';

  // 资源图标
  import monitorImg from '@/assets/images/map/monitor.png';
  import monitorAggImg from '@/assets/images/map/monitor_agg.png';
  import monitor_layer from '@/assets/images/map/monitor_layer.png';
  import monitorHeader from '@/assets/images/map/monitor_header.png';
  import personImg from '@/assets/images/map/person.png';
  import personAggImg from '@/assets/images/map/person_agg.png';
  import person_layer from '@/assets/images/map/person_layer.png';
  import personHeader from '@/assets/images/map/person_header.png';
  import recorderImg from '@/assets/images/map/recorder.png';
  import recorderAggImg from '@/assets/images/map/recorder_agg.png';
  import recorder_layer from '@/assets/images/map/recorder_layer.png';
  import recorderHeader from '@/assets/images/map/recorder_header.png';
  import surveillanceImg from '@/assets/images/map/surveillance.png';
  import surveillanceAggImg from '@/assets/images/map/surveillance_agg.png';
  import surveillance_layer from '@/assets/images/map/surveillance_layer.png';
  import surveillanceHeader from '@/assets/images/map/surveillance_header.png';
  import { useEmitter } from '@/hooks/useEmitter';
  import { getMapConfig, mapManager, setStoredMapType } from '@/plugins/map';
  import { useCommunicationStore } from '@/stores/communication.js';
  import { useDeviceAdapter } from '@/stores/useDeviceAdapter.js';
  import { getDeviceLayerList } from '@/common/api/map.js';
  import { transformImageUrl } from '@/utils/imgUrlParse.js';
import { usePointLoader } from './usePointLoader';
  const { adaptationSize } = useDeviceAdapter();
  const communicationStore = useCommunicationStore();

  // 状态栏高度，用于顶部元素定位
  const statusBarHeight = ref(0);

  const mapId = 'bottomMap';
  const showDeviceLayer = true;
  const showMarkers = true;
  const showFly = true;

  const mapReady = ref(false);
  const markDetailVisible = ref(false);
  const showMarkList = ref(false);
  // 使用shallowRef避免深度响应式，仅监听引用变化
  const activeMarkerList = shallowRef<any[]>([]);
  const activeMarkerIndex = ref(0);

  const currentMarkerDetail = computed(() => {
    const list = activeMarkerList.value;
    if (!Array.isArray(list) || list.length === 0) {
      return null;
    }
    const index = Math.min(Math.max(activeMarkerIndex.value || 0, 0), list.length - 1);
    return list[index];
  });

  const markerTotal = computed(() => activeMarkerList.value.length || 0);

  const layerPanelVisible = ref(false);
  const userInfo = computed(() => communicationStore.userInfo);

  // 为图层面板准备的图层配置（仅用于 UI）
  const deviceLayers = ref([]);
  async function fetchDeviceLayers() {
    try {
      const {getEquipmentType} = usePointLoader();
      const res = await getDeviceLayerList();
      const layers = res || [];
            console.log('layers1111', layers);
      if (Array.isArray(layers) && layers.length > 0) {
        deviceLayers.value =layers.filter(item=>item.isShow === 1).map((item) => {
          const iconUri = item.iconUri ? transformImageUrl(`/map-api${item.iconUri}`) : '';

          // 聚合图标
          if (item.category === '1') {
            // 摄像头
            item.clusterImage =iconUri || monitorAggImg;
            item.image = iconUri || monitorImg;
            item.layerImage = iconUri || monitor_layer;
            item.headerImage = iconUri || monitorHeader;
            item.color = '#4B7AFA';
          } else if (item.category === '9') {
            if (item.apptype === '0' && item.subusercategory === '3') {
              // 布控球
              item.clusterImage =iconUri || surveillanceAggImg;
              item.image = iconUri || surveillanceImg;
              item.layerImage = iconUri || surveillance_layer;
              item.headerImage = iconUri || surveillanceHeader;
              item.color = '#F5222D';
            }if (item.apptype === '0' && item.subusercategory === '0') {
              // 用户
              item.clusterImage = personImg;
              item.image = iconUri || personAggImg;
              item.layerImage = iconUri || person_layer;
              item.headerImage = iconUri || personHeader;
              item.color = '#F5222D';
            } else {
              // 记录仪
              item.clusterImage =iconUri ||  recorderAggImg;
              item.image = iconUri || recorderImg;
              item.layerImage = iconUri || recorder_layer;
              item.headerImage = iconUri || recorderHeader;
              item.color = '#FF8C42';
            }
          } else if (item.category === '10') {
            // 用户
            item.clusterImage = personImg;
            item.image = iconUri || personAggImg;
            item.layerImage = iconUri || person_layer;
            item.headerImage = iconUri || personHeader;
            item.color = '#52C41A';
          }
          item.size = 32;
          item.key = getEquipmentType(item)+'Layer'
          item.visible = true;
          return item;
        });
        console.log('[地图] 设备图层配置加载成功:', deviceLayers.value);
      } else {
        console.warn('[地图] 未获取到设备图层配置，使用默认配置');
      }
    } catch (error) {
      console.error('[地图] 获取设备图层配置失败:', error);
    }
  }
  // 图层点位映射，目前图层面板模板未使用计数，这里先占位
  const layerPoints = reactive<Record<string, any[]>>({});

  const mapTypeOptions = [
    { label: '高德地图', value: 'AMap' },
    { label: 'OpenLayers', value: 'OpenLayers' },
  ];
  const currentMapType = ref(mapTypeOptions[0].value);

  // 通过事件总线通知 bottomMap 切换地图类型
  const emitter = useEmitter('mapTypeChange');

  watch(
    () => userInfo.value?.idCard,
    async (idCard) => {
      if (!idCard) return;
      await communicationStore.h5permissions();
    },
    { immediate: true },
  );
  onMounted(async () => {
    // 获取状态栏高度
    statusBarHeight.value = await communicationStore.fetchStatusBarHeight();
    await communicationStore.getUserInfo();
    await fetchDeviceLayers();
    try {
      const config = await getMapConfig();
      if (config?.mapType) {
        currentMapType.value = config.mapType;
      }
    } catch (error) {
      console.warn('获取地图配置失败，使用默认地图类型', error);
    }

    initdeviceLayers();
  });

  //图层显示隐藏状态
  async function initdeviceLayers() {
    try {
      const invisibleLayer = await communicationStore.getStorage('invisibleLayer');
      if (invisibleLayer) {
        const invisibleIdArr = invisibleLayer.split(',');
        deviceLayers.value.forEach((i) => (i.visible = !invisibleIdArr.includes(i.id)));
      }
    } catch (error) {
      console.error('获取图层失败:', error);
    }
  }

  function handleMapReady() {
    mapReady.value = true;
  }

  function setLayerPanel(flag: boolean) {
    layerPanelVisible.value = flag ? !layerPanelVisible.value : false;
    showMarkList.value = false;
    markDetailVisible.value = false;
  }

  function handleLayerToggle({ id, visible }: { id: string; visible: boolean }) {
    const layer = deviceLayers.value.find((item) => item.key === id);
    if (layer) {
      layer.visible = visible;
    }
    const targetMap = mapManager.get(mapId);
    targetMap?.setLayerVisible(id, visible);

    // 缓存隐藏的图层
    const invisibleLayer = deviceLayers.value
      .filter((i) => !i.visible)
      .map((i) => i.id)
      .join(',');
    communicationStore.setStorage('invisibleLayer', invisibleLayer);
  }

  // 创建图层映射缓存，避免重复查找
  const layerMapCache = new Map<string, (typeof deviceLayers.value)[0]>();
  function getLayerById(layerId: string) {
    if (!layerMapCache.has(layerId)) {
      const layer = deviceLayers.value.find((l) => l.id === layerId);
      if (layer) {
        layerMapCache.set(layerId, layer);
      }
    }
    return layerMapCache.get(layerId);
  }
  // 建立图标缓存，每次取图标从缓存中取
  const layerIconMap = new Map<string, string>();
  let layerIconMapInitialized = false;

  /**
   * 构建图层图标映射
   */
  function buildLayerIconMap() {
    layerIconMap.clear();
    for (const layer of deviceLayers.value) {
      layerIconMap.set(layer.id, layer.image);
    }
    layerIconMapInitialized = true;
  }

  // 监听 deviceLayers.value 变化，自动重建缓存
  watch(
    () => deviceLayers.value.map((l) => ({ id: l.id, image: l.image })),
    () => {
      buildLayerIconMap();
    },
    { deep: true },
  );

  // ==================== 分帧处理配置 ====================

  /**
   * 每帧处理的点位数量
   */
  const FRAME_BATCH_SIZE = 100;

  /**
   * 处理单个点位数据
   * @param {Object} item - 点位数据
   * @param {number} index - 索引
   * @returns {Object} - 处理后的点位数据
   */
  function processClusterItem(item, index) {
    const layerId = item.layerId;
    return {
      __id: item.__id || `${layerId || 'layer'}-${item.id ?? index}`,
      ...item,
      layerId,
      icon: layerIconMap.get(layerId) || item.icon,
    };
  }

  /**
   * 处理聚合点位数据（同步版本）
   * @param {Array} clusterList - 聚合点位列表
   * @returns {Array} - 处理后的点位列表
   *
   */
  function processClusterList(clusterList: any[]): any[] {
    if (!layerIconMapInitialized) {
      buildLayerIconMap();
    }
    return clusterList.map((item, index) => processClusterItem(item, index));
  }

  /**
   * 分帧处理聚合数据（优化版）
   * @param {Array} clusterList - 聚合点位列表
   * @returns {Promise<Array>} - 处理后的点位列表
   */
  async function processClusterDataInFrames(clusterList: any[]): Promise<any[]> {
    // 确保图标映射已初始化
    if (!layerIconMapInitialized) {
      buildLayerIconMap();
    }

    // 分批
    const batches = [];
    for (let i = 0; i < clusterList.length; i += FRAME_BATCH_SIZE) {
      batches.push(clusterList.slice(i, i + FRAME_BATCH_SIZE));
    }

    console.log('[index] 开始处理聚合数据，总数:', clusterList.length, '批次数:', batches.length);

    // 分帧处理
    let processedData = [];
    for (let i = 0; i < batches.length; i++) {
      //  等待下一帧（使用 requestAnimationFrame，兼容鸿蒙）
      await new Promise((resolve) => requestAnimationFrame(resolve));

      // 处理当前批次
      const batchResult = batches[i].map((item, index) =>
        processClusterItem(item, i * FRAME_BATCH_SIZE + index),
      );

      // 增量更新（用户能看到渐进加载）
      processedData = [...processedData, ...batchResult];
      activeMarkerList.value = [...processedData];
    }

    console.log('[index] 聚合数据处理完成，总数:', processedData.length);
    return processedData;
  }

  /**
   * 处理Marker点击事件（优化版）
   * @param {Object} payload - 点击事件数据
   */
  function handleMarkerClick(payload: any = {}) {
    // 提取聚合数据
    const clusterList = Array.isArray(payload?.clusterData)
      ? payload.clusterData.filter(Boolean)
      : [];

    const hasCluster = clusterList.length > 1;
    const data = payload?.data || clusterList[0] || null;

    // 处理空数据情况
    if (!data && clusterList.length === 0) return;

    // 关闭图层面板
    layerPanelVisible.value = false;

    // ==================== 聚合点处理 ====================
    if (hasCluster) {
      markDetailVisible.value = false;
      activeMarkerIndex.value = 0;

      //  立即显示弹窗（<100ms）
      showMarkList.value = true;
      activeMarkerList.value = []; // 显示Loading状态

      //  分帧处理数据（不阻塞UI）
      processClusterDataInFrames(clusterList);
      return;
    }

    // ==================== 单个点位处理 ====================
    activeMarkerList.value = [data];
    activeMarkerIndex.value = 0;
    showMarkList.value = false;
    markDetailVisible.value = true;
  }

  function handleDetailClose() {
    markDetailVisible.value = false;
    activeMarkerList.value = [];
    activeMarkerIndex.value = 0;
  }

  function handleListClose() {
    showMarkList.value = false;
    activeMarkerList.value = [];
    activeMarkerIndex.value = 0;
  }

  function handleDetailPrev() {
    if (activeMarkerIndex.value <= 0) return;
    activeMarkerIndex.value -= 1;
  }

  function handleDetailNext() {
    if (activeMarkerIndex.value >= markerTotal.value - 1) return;
    activeMarkerIndex.value += 1;
  }

  function handleListItemView(item: any) {
    if (!item) return;
    const list = activeMarkerList.value;
    const idx = list.findIndex((cur) => cur.__id === item.__id || cur.id === item.id) || 0;
    activeMarkerIndex.value = idx < 0 ? 0 : idx;
    showMarkList.value = false;
    markDetailVisible.value = true;
  }

  function handleMapTypeSelect(type: string) {
    if (type === currentMapType.value) return;
    currentMapType.value = type;
    setStoredMapType(type);
    emitter?.emit('mapTypeChange', type);
  }

  const goBack = async () => {
    await communicationStore.close();
  };
</script>

<style lang="scss" scoped>
  .map-box {
    width: 100%;
    height: 100%;
    position: relative;
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

  .nav-back {
    position: absolute;
    top: 16px;
    left: 16px;
    z-index: 30;
  }

  .layer-btn {
    position: absolute;
    top: 30px;
    right: 16px;
    display: flex;
    align-items: center;
    gap: 8px;
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
</style>
