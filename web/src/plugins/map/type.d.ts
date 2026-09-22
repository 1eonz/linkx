// 经纬度
export type Position = number[];

// 点 线 多边形 圆 矩形
export type PolygonType = 'circle' | 'marker' | 'polygon' | 'polyline' | 'rectangle';

// 轨迹播放状态
export type TrackStatus = '' | 'destroy' | 'pause' | 'start';

// 后台返回经纬度数据
export type Location = {
  location: {
    coordinates: { lat: number; lon: number }[];
  };
};
