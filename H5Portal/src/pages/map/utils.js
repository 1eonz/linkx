import { mapManager, getMapConfig } from '@/plugins/map';
import { imageToBase64 } from '@/utils';

// ==================== 缓存机制 ====================

/**
 * 图片Base64缓存
 * - 避免重复转换相同图片
 * - 减少主线程阻塞时间
 */
const imageBase64Cache = new Map();

/**
 * 获取缓存的图片Base64
 * @param {string} url - 图片URL
 * @returns {Promise<string>} - Base64字符串
 */
export async function getCachedImageBase64(url) {
  // 参数校验
  if (!url) return '';
  
  // 检查缓存
  if (imageBase64Cache.has(url)) {
    return imageBase64Cache.get(url);
  }
  
  try {
    // 转换图片为Base64
    const base64 = await imageToBase64(url);
    
    // 缓存结果
    imageBase64Cache.set(url, base64);
    console.log('[utils] 图片缓存新增:', url);
    
    return base64;
  } catch (error) {
    console.warn('[utils] 图片转换失败:', url, error);
    return '';
  }
}

// ==================== 地图工具函数 ====================

/**
 * 回到地图中心点
 * @param {string} mapId - 地图ID
 */
export const flyToCenter = (mapId) => {
  const { center, zoom } = getMapConfig();
  const mapObj = mapManager.get(mapId);
  if (mapObj) {
    mapObj.flyAction(center, zoom);
  }
};

/**
 * 生成模拟点位
 * @param {string} prefix 点位前缀/类型
 * @param {number} count 数量
 */
export function createMockPoints(prefix, count = 50) {
  const { center = [104.0668, 30.5728] } = getMapConfig();
  const [baseLng, baseLat] = center;
  const list = [];
  for (let i = 0; i < count; i += 1) {
    const lng = baseLng + (Math.sin(i * 1.3) + (Math.random() - 0.5)) * 0.35;
    const lat = baseLat + (Math.cos(i * 0.7) + (Math.random() - 0.5)) * 0.25;
    // 随机生成在线/离线状态，约70%在线，30%离线
    const status = Math.random() > 0.3 ? 'online' : 'offline';
    list.push({
      id: `${prefix}-${i}`,
      position: [Number(lng.toFixed(6)), Number(lat.toFixed(6))],
      type: prefix === 'camera' ? 'monitor' : prefix,
      status,
    });
  }
  return list;
}

/**
 * 创建单个点位的 HTML 标记
 */
export function createImageMarker(imageUrl, isOnline = true) {
  // 在线：绿色圆点 rgba(67, 207, 124, 1)
  // 离线：灰色圆点 rgba(166, 169, 171, 1)
  const dotColor = isOnline ? 'rgba(67, 207, 124, 1)' : 'rgba(166, 169, 171, 1)';
  const dotSize = '8px'; // 圆点大小
  const style = [
    'width:32px',
    'height:32px',
    'display:flex',
    'align-items:center',
    'justify-content:center',
    'position:relative',
  ].join(';');
  return `<div style="${style}">
    <img src="${imageUrl}" style="width:100%;height:100%;object-fit:contain;" alt="marker" />
    <div style="position:absolute;top:3px;right:5px;width:${dotSize};height:${dotSize};background:${dotColor};border:1px solid #fff;border-radius:50%;transform:translate(25%, -25%);"></div>
  </div>`;
}

/**
 * 创建聚合点位标记
 */
export function renderClusterMarker(context, clusterImageUrl) {
  const safeUrl = clusterImageUrl || '';
  const displayCount = context.count > 99 ? '99+' : context.count;
  const html = `<div style="position:relative;width:32px;height:32px;display:flex;align-items:center;justify-content:center;">
    <img src="${safeUrl}" alt="cluster" style="width:100%;height:100%;object-fit:contain;" />
    <span style="position:absolute;top:12px;left:50%;transform:translate(-50%, -50%);color:#fff;font-size:14px;font-weight:bold;text-shadow:0 1px 2px rgba(0,0,0,0.8);pointer-events:none;">
      ${displayCount}
    </span>
  </div>`;

  // 兼容 AMap 和 OpenLayers：AMap 依赖 setOffset，OpenLayers 会读取 HTML
  if (
    typeof context.marker?.setOffset === 'function' &&
    typeof window !== 'undefined' &&
    window.AMap?.Pixel
  ) {
    context.marker.setOffset(new AMap.Pixel(-24, -24));
  }
  if (typeof context.marker?.setContent === 'function') {
    context.marker.setContent(html);
    return;
  }
  if (typeof document !== 'undefined') {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = html;
    return wrapper.firstElementChild || wrapper;
  }
  return html;
}

/**
 * 在地图上批量添加设备聚合图层（优化版）
 * @param {any} targetMap - 地图实例
 * @param {Array} deviceLayers - 图层配置数组
 * @param {Record<string, any[]>} layerPoints - 图层对应的点位数据
 * @param {(payload: any) => void} onMarkerClick - 点位点击回调
 */
export async function renderDeviceLayers(targetMap, deviceLayers, layerPoints, onMarkerClick) {
  // 参数校验
  if (!targetMap?.addMarkerCluster) {
    console.warn('[renderDeviceLayers] 地图实例无效或缺少 addMarkerCluster 方法');
    return;
  }
  
  console.info('[renderDeviceLayers] 开始渲染图层:', {
    layerCount: deviceLayers.length,
    layerIds: deviceLayers.map(l => l.id),
    layerPoints: Object.keys(layerPoints).map(key => ({
      layerId: key,
      pointCount: layerPoints[key]?.length || 0,
    })),
  });
  
  // 遍历每个图层
  for (const layer of deviceLayers) {
    console.info(`[renderDeviceLayers] layer: `,layer,layerPoints );
    // 获取该图层的点位数据
    const rawPoints = layerPoints[layer.key] || [];
    console.log(rawPoints,'=====rawPoints===')
    console.info(`[renderDeviceLayers] 图层 ${layer.name} (${layer.id}):`, {
      rawPointCount: rawPoints.length,
      visible: layer.visible,
    });
    
    // 为每个点位生成唯一ID和图层信息
    const points = rawPoints.map((item, index) => ({
      __id: item.__id || `${layer.id}-${item.id ?? index}`,
      ...item,
      layerId: layer.key,
    }));
    
    //  使用缓存的图片转换（优化点）
    const [markerImage, clusterImage] = await Promise.all([
      getCachedImageBase64(layer.image),        // 普通图标
      getCachedImageBase64(layer.clusterImage), // 聚合图标
    ]);
    
    // 添加聚合图层到地图
    targetMap.addMarkerCluster(layer.key, points, {
      color: layer.color,
      size: layer.size,
      visible: layer.visible,
      icon: layer.icon,
      image: markerImage,
      clusterImage,
      // 渲染单个点位Marker
      renderMarker: (data) => {
        // 根据在线/离线状态选择图片
        const isOnline = data?.status === 'online';
        return createImageMarker(markerImage, isOnline);
      },
      // 渲染聚合点位Marker
      renderClusterMarker: (context) => renderClusterMarker(context, clusterImage),
      // 点击回调
      onMarkerClick,
    });
    
    console.info(`[renderDeviceLayers] 图层 ${layer.name} 渲染完成，实际添加点位: ${points.length}`);
  }
  
  console.info('[renderDeviceLayers] 所有图层渲染完成');
}

// ==================== 分帧处理工具函数 ====================

/**
 * 分批处理数据（兼容所有系统）
 * @param {Array} data - 待处理的数据数组
 * @param {number} batchSize - 每批处理的数量
 * @param {Function} processor - 数据处理函数
 * @param {Function} onProgress - 进度回调函数
 * @returns {Promise<Array>} - 处理后的结果数组
 */
export async function processInBatches(data, batchSize, processor, onProgress) {
  // 参数校验
  if (!Array.isArray(data) || data.length === 0) return [];
  
  // 分批
  const batches = [];
  for (let i = 0; i < data.length; i += batchSize) {
    batches.push(data.slice(i, i + batchSize));
  }
  
  // 分帧处理
  const results = [];
  for (let i = 0; i < batches.length; i++) {
    //  等待下一帧（使用 requestAnimationFrame，兼容鸿蒙）
    await new Promise(resolve => requestAnimationFrame(resolve));
    
    // 处理当前批次
    const batchResult = processor(batches[i]);
    results.push(...batchResult);
    
    // 进度回调
    if (typeof onProgress === 'function') {
      onProgress(results.length, data.length);
    }
  }
  
  return results;
}

/**
 * 兼容所有系统的空闲调度器
 * @param {Function} callback - 回调函数
 * @param {Object} options - 配置选项
 * @returns {number} - 定时器ID
 */
export function scheduleIdleWork(callback, options = { timeout: 100 }) {
  // 检测是否支持 requestIdleCallback
  if (typeof requestIdleCallback !== 'undefined') {
    return requestIdleCallback(callback, options);
  }
  
  // 降级方案：使用 requestAnimationFrame
  return requestAnimationFrame(() => {
    const start = performance.now();
    const deadline = {
      didTimeout: false,
      timeRemaining: () => Math.max(0, 50 - (performance.now() - start))
    };
    callback(deadline);
  });
}

/**
 * 简单的防抖函数
 * @param {Function} fn - 待防抖的函数
 * @param {number} delay - 防抖延迟时间（ms）
 * @returns {Function} - 防抖后的函数
 */
export function debounce(fn, delay) {
  let timer = null;
  return function(...args) {
    if (timer) clearTimeout(timer);
    timer = setTimeout(() => {
      fn.apply(this, args);
    }, delay);
  };
}
