/* stylelint-disable */
/* stylelint-disable-next-line */
import locationIcon from '@/assets/images/map/location1.png';
import defaultAvatar from '@/assets/images/map/person_header.png';
import { imageToBase64 } from '@/utils';

// WGS84 -> GCJ-02（高德） 坐标转换
const PI = Math.PI;
const A = 6378245.0; // 长半轴
const EE = 0.00669342162296594323; // 偏心率平方

function outOfChina(lng, lat) {
  // 中国范围外不做偏移
  return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
}

function transformLat(x, y) {
  let ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0;
  ret += ((20.0 * Math.sin(y * PI) + 40.0 * Math.sin((y / 3.0) * PI)) * 2.0) / 3.0;
  ret += ((160.0 * Math.sin((y / 12.0) * PI) + 320 * Math.sin((y * PI) / 30.0)) * 2.0) / 3.0;
  return ret;
}

function transformLng(x, y) {
  let ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
  ret += ((20.0 * Math.sin(6.0 * x * PI) + 20.0 * Math.sin(2.0 * x * PI)) * 2.0) / 3.0;
  ret += ((20.0 * Math.sin(x * PI) + 40.0 * Math.sin((x / 3.0) * PI)) * 2.0) / 3.0;
  ret += ((150.0 * Math.sin((x / 12.0) * PI) + 300.0 * Math.sin((x / 30.0) * PI)) * 2.0) / 3.0;
  return ret;
}

export function wgs84ToGcj02(lng, lat) {
  lng = Number(lng);
  lat = Number(lat);
  if (isNaN(lng) || isNaN(lat)) return [lng, lat];
  if (outOfChina(lng, lat)) {
    // 国外坐标不做偏移
    return [lng, lat];
  }
  let dLat = transformLat(lng - 105.0, lat - 35.0);
  let dLng = transformLng(lng - 105.0, lat - 35.0);
  const radLat = (lat / 180.0) * PI;
  let magic = Math.sin(radLat);
  magic = 1 - EE * magic * magic;
  const sqrtMagic = Math.sqrt(magic);
  dLat = (dLat * 180.0) / (((A * (1 - EE)) / (magic * sqrtMagic)) * PI);
  dLng = (dLng * 180.0) / ((A / sqrtMagic) * Math.cos(radLat) * PI);
  const mgLat = lat + dLat;
  const mgLng = lng + dLng;
  return [mgLng, mgLat];
}

// 创建位置共享标记HTML（类似微信位置共享样式）
export async function createLocationMarkerHtml(
  avatarUrl,
  locationIconUrl = locationIcon,
  defaultAvatarUrl = defaultAvatar,
) {
  // 头像大小
  const avatarSize = 40;
  // 头像边框宽度
  const avatarBorderWidth = 2;
  // 位置图标大小（增大尺寸以便更清晰显示）
  const locationIconSize = 32;
  // 总高度（头像高度 + 位置图标高度）
  // 之前只保留了位置图标的 30% 可见高度，导致实际 DOM 高度与传给地图的 markerHeight 不一致，
  // 缩放时锚点会发生偏移。这里直接使用完整高度，保证与地图参数一致。
  const totalHeight = avatarSize + locationIconSize;
  // 容器宽度需要考虑头像边框（左右各2px）
  const totalWidth = Math.max(avatarSize + avatarBorderWidth * 2, locationIconSize);

  // 将图片转换为base64
  let avatarBase64 = '';
  let locationBase64 = '';

  // 处理头像：优先使用用户头像，失败则使用默认头像
  const finalAvatarUrl = avatarUrl || defaultAvatarUrl;
  // console.log("开始转换头像，URL:", finalAvatarUrl);

  // 确保URL是完整的（处理相对路径）
  const resolveImageUrl = (url) => {
    if (!url) return url;
    // 如果已经是完整URL（http/https/data），直接返回
    if (/^(data:|https?:|blob:)/i.test(url)) {
      return url;
    }
    // 如果是相对路径，转换为绝对路径
    if (url.startsWith('/')) {
      return `${window.location.origin}${url}`;
    }
    // 如果是通过import导入的路径（通常是完整URL），直接返回
    return url;
  };

  const resolvedAvatarUrl = resolveImageUrl(finalAvatarUrl);
  const resolvedLocationUrl = resolveImageUrl(locationIconUrl);

  try {
    avatarBase64 = await imageToBase64(resolvedAvatarUrl);
    console.log('头像转换成功，base64长度:', avatarBase64.length);
  } catch (error) {
    console.warn('头像转换失败，尝试使用默认头像', error, resolvedAvatarUrl);
    try {
      const resolvedDefaultAvatar = resolveImageUrl(defaultAvatarUrl);
      avatarBase64 = await imageToBase64(resolvedDefaultAvatar);
      console.log('默认头像转换成功');
      // 如果转换成功，使用转换后的base64
      if (avatarBase64 && avatarBase64.startsWith('data:')) {
        // base64转换成功，继续使用
      } else {
        avatarBase64 = resolvedDefaultAvatar;
      }
    } catch (e) {
      console.error('默认头像转换也失败，使用原始路径', e);
      // 如果转换失败，使用原始URL
      const resolvedDefaultAvatar = resolveImageUrl(defaultAvatarUrl);
      avatarBase64 = resolvedDefaultAvatar || defaultAvatarUrl;
    }
  }

  // 如果头像base64为空或不是base64格式，使用原始URL
  if (!avatarBase64 || (!avatarBase64.startsWith('data:') && !avatarBase64.startsWith('http'))) {
    avatarBase64 = resolvedAvatarUrl || finalAvatarUrl;
  }
  try {
    locationBase64 = await imageToBase64(resolvedLocationUrl);
    // console.log("位置图标转换成功，base64长度:", locationBase64.length);
  } catch {
    // console.warn("位置图标转换失败，使用原始路径", error, resolvedLocationUrl);
    locationBase64 = resolvedLocationUrl || locationIconUrl;
  }

  // 如果位置图标base64为空或不是base64格式，使用原始URL
  if (
    !locationBase64 ||
    (!locationBase64.startsWith('data:') && !locationBase64.startsWith('http'))
  ) {
    locationBase64 = resolvedLocationUrl || locationIconUrl;
  }

  // 创建类似微信位置共享的样式：头像在上，位置图标在下
  // 确保内容精确对齐，无额外空间，便于锚点计算
  // 使用flex布局而不是绝对定位，确保在OpenLayers的SVG中正确显示
  const html = `
    <div style="
      position: relative;
      margin: 0;
      padding: 0;
      box-sizing: border-box;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: flex-start;
      width: ${totalWidth}px;
      height: ${totalHeight}px;
      overflow: visible;
    ">
      <!-- 头像 -->
      <div style="
        width: ${avatarSize}px;
        height: ${avatarSize}px;
        border-radius: 50%;
        overflow: hidden;
        border: ${avatarBorderWidth}px solid #fff;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
        background: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        z-index: 2;
        flex-shrink: 0;
        box-sizing: border-box;
      ">
        <img 
          src="${avatarBase64}" 
          style="
            width: 100%;
            height: 100%;
            object-fit: cover;
            display: block;
          " 
          alt="avatar"
          onerror="this.onerror=null; this.src='${defaultAvatarUrl}'"
        />
      </div>
      <!-- 位置图标 - 紧贴头像底部，使用flex布局而不是绝对定位 -->
      <div style="
        width: ${locationIconSize}px;
        height: ${locationIconSize}px;
        margin: 0;
        padding: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;
      ">
        <img 
          src="${locationBase64}" 
          style="
            width: 100%;
            height: 100%;
            object-fit: contain;
            display: block;
          " 
          alt="location"
          onerror="this.onerror=null; this.src='${locationIconUrl}'"
        />
      </div>
    </div>
  `;

  return html;
}

// 将自己添加到地图上
export async function addSelfToMap({
  mapInstance,
  userInfo,
  nowLocation,
  defaultAvatarUrl = defaultAvatar,
  locationIconUrl = locationIcon,
}) {
  if (!mapInstance || !userInfo || !nowLocation?.latitude || !nowLocation?.longitude) {
    return;
  }

  const isdn = userInfo.isdn;
  if (!isdn) {
    console.warn('用户isdn不存在，无法添加图层');
    return;
  }

  // 确定头像URL（兼容 tumbAvatar 和 thumbAvatar 两种字段名）
  const avatarUrl = userInfo.tumbAvatar || userInfo.thumbAvatar || defaultAvatarUrl;

  // 创建标记HTML
  console.log('开始创建标记HTML，头像URL:', avatarUrl, '位置图标:', locationIconUrl);
  const markerHtml = await createLocationMarkerHtml(avatarUrl, locationIconUrl, defaultAvatarUrl);
  console.log('标记HTML创建完成，长度:', markerHtml.length);

  // 先将 GPS84（WGS84）坐标转换为高德 GCJ-02 坐标，再转换成 [longitude, latitude] 数组
  const [gcjLng, gcjLat] = wgs84ToGcj02(nowLocation.longitude, nowLocation.latitude);

  // 转换经纬度格式：从 {latitude, longitude} 转为 [longitude, latitude]（已是高德坐标）
  const position = [gcjLng, gcjLat];

  // 验证经纬度
  if (isNaN(position[0]) || isNaN(position[1])) {
    console.error('经纬度格式错误:', nowLocation);
    return;
  }

  // 验证经纬度范围（经度：-180到180，纬度：-90到90）
  if (position[0] < -180 || position[0] > 180 || position[1] < -90 || position[1] > 90) {
    console.error('经纬度超出有效范围:', position);
    return;
  }

  console.log('准备添加点位，位置:', position, '图层ID:', isdn);
  console.log('原始位置数据:', nowLocation);
  console.log('用户信息:', {
    isdn: isdn,
    name: userInfo.name || userInfo.userName,
    thumbAvatar: userInfo.thumbAvatar,
  });

  // 创建点位数据（确保格式正确）
  const point = {
    id: isdn,
    position: position, // [longitude, latitude]
    isdn: isdn,
    name: userInfo.name || userInfo.userName || '我',
    // 添加其他可能需要的字段
    lnglat: position, // 兼容其他格式
    lngLat: position, // 兼容其他格式
    coordinates: position, // 兼容其他格式
  };

  console.log('点位数据:', point);

  // 使用addMarkerCluster添加图层（即使只有一个点也可以使用这个方法）
  try {
    // 使用HTML标记的实际尺寸（与createLocationMarkerHtml保持一致）
    const avatarSize = 40;
    const avatarBorderWidth = 2;
    const locationIconSize = 32; // 增大位置图标尺寸
    // 使用与 createLocationMarkerHtml 中一致的尺寸，确保锚点准确
    const totalHeight = avatarSize + locationIconSize; // 72px
    const totalWidth = Math.max(avatarSize + avatarBorderWidth * 2, locationIconSize); // 44px
    const markerSize = Math.ceil(Math.max(totalWidth, totalHeight));

    console.log('开始添加图层，参数:', {
      layerId: isdn,
      point: point,
      position: position,
      markerSize: markerSize,
      actualWidth: totalWidth,
      actualHeight: totalHeight,
    });

    mapInstance.addMarkerCluster(isdn, [point], {
      color: '#4B7AFA',
      size: markerSize,
      markerWidth: totalWidth, // 传递实际宽度，确保容器宽度足够
      markerHeight: totalHeight, // 传递实际高度，避免缩放偏移
      clusterSize: 60,
      visible: true,
      anchor: [0.5, 1.0], // 锚点设置为底部中心，确保缩放时位置不变
      renderMarker: (data) => {
        // renderMarker需要接收data参数并返回HTML
        console.log('渲染标记，数据:', data);
        return markerHtml;
      },
      renderClusterMarker: (context) => {
        // renderClusterMarker也需要返回HTML
        console.log('渲染聚合标记，上下文:', context);
        return markerHtml;
      },
    });

    console.log('图层添加完成，图层ID:', isdn, '位置:', position);

    // 检查图层是否成功添加
    try {
      const layers = mapInstance.layers;
      if (layers && typeof layers.get === 'function') {
        const layerInfo = layers.get(isdn);
        console.log('图层信息:', layerInfo);
        if (layerInfo) {
          console.log('图层已成功添加到地图，图层类型:', layerInfo.type);
        } else {
          console.warn('图层信息未找到，图层ID:', isdn);
        }
      } else {
        console.warn('无法访问layers属性或get方法');
      }
    } catch (error) {
      console.warn('检查图层信息时出错:', error);
    }

    // 添加点位后，将地图视图移动到该位置
    setTimeout(() => {
      try {
        if (mapInstance.flyAction) {
          // 使用flyAction平滑移动到位置，缩放级别设为15（比较接近的级别）
          mapInstance.flyAction(position, 14, 1000);
          console.log('地图视图已移动到位置:', position, '缩放级别: 14');
        } else if (mapInstance.setCenter) {
          // 如果没有flyAction，使用setCenter
          mapInstance.setCenter(position, 14);
          console.log('地图中心已设置到位置:', position, '缩放级别: 14');
        } else {
          console.warn('地图实例没有flyAction或setCenter方法');
        }
      } catch (error) {
        console.error('移动地图视图失败:', error);
      }
    }, 500); // 延迟500ms确保图层已添加
  } catch (error) {
    console.error('添加地图图层失败:', error);
    console.error('错误详情:', {
      error: error.message,
      stack: error.stack,
      mapInstance: mapInstance,
      point: point,
    });
  }
}

// 添加或更新人员位置到地图上
export async function addOrUpdatePersonToMap({
  mapInstance,
  personInfo,
  location,
  defaultAvatarUrl = defaultAvatar,
  locationIconUrl = locationIcon,
}) {
  if (
    !mapInstance ||
    !personInfo ||
    !personInfo.isdn ||
    !location?.latitude ||
    !location?.longitude
  ) {
    return;
  }

  const isdn = personInfo.isdn;

  // 确定头像URL（兼容 tumbAvatar 和 thumbAvatar 两种字段名）
  const avatarUrl =
    personInfo.tumbAvatar || personInfo.thumbAvatar || personInfo.avatar || defaultAvatarUrl;

  // 创建标记HTML
  const markerHtml = await createLocationMarkerHtml(avatarUrl, locationIconUrl, defaultAvatarUrl);

  // 先将 GPS84（WGS84）坐标转换为高德 GCJ-02 坐标
  const [gcjLng, gcjLat] = wgs84ToGcj02(location.longitude, location.latitude);

  // 转换经纬度格式：从 {latitude, longitude} 转为 [longitude, latitude]（已是高德坐标）
  const position = [gcjLng, gcjLat];

  // 验证经纬度
  if (isNaN(position[0]) || isNaN(position[1])) {
    console.error('经纬度格式错误:', location);
    return;
  }

  // 验证经纬度范围（经度：-180到180，纬度：-90到90）
  if (position[0] < -180 || position[0] > 180 || position[1] < -90 || position[1] > 90) {
    console.error('经纬度超出有效范围:', position);
    return;
  }

  // 创建点位数据（确保格式正确）
  const point = {
    id: isdn,
    position: position, // [longitude, latitude]
    isdn: isdn,
    name: personInfo.name || personInfo.userName || '未知',
    // 添加其他可能需要的字段
    lnglat: position, // 兼容其他格式
    lngLat: position, // 兼容其他格式
    coordinates: position, // 兼容其他格式
  };

  // 使用addMarkerCluster添加或更新图层（addMarkerCluster会先removeLayer再添加，所以可以用于更新）
  try {
    // 使用HTML标记的实际尺寸（与createLocationMarkerHtml保持一致）
    const avatarSize = 40;
    const avatarBorderWidth = 2;
    const locationIconSize = 32; // 增大位置图标尺寸
    // 使用与 createLocationMarkerHtml 中一致的尺寸，确保锚点准确
    const totalHeight = avatarSize + locationIconSize; // 72px
    const totalWidth = Math.max(avatarSize + avatarBorderWidth * 2, locationIconSize); // 44px
    const markerSize = Math.ceil(Math.max(totalWidth, totalHeight));

    mapInstance.addMarkerCluster(isdn, [point], {
      color: '#4B7AFA',
      size: markerSize,
      markerWidth: totalWidth, // 传递实际宽度，确保容器宽度足够
      markerHeight: totalHeight, // 传递实际高度，避免缩放偏移
      clusterSize: 60,
      visible: true,
      anchor: [0.5, 1.0], // 锚点设置为底部中心，确保缩放时位置不变
      renderMarker: () => {
        return markerHtml;
      },
      renderClusterMarker: () => {
        return markerHtml;
      },
    });

    console.log('人员位置已更新，图层ID:', isdn, '位置:', position);
  } catch (error) {
    console.error('更新人员位置失败:', error);
  }
}
