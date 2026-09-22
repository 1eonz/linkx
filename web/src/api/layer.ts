import http from '@/utils/http';

// 自定义图层
export const queryCustomLayer = (data) => http.post<any>('/resource/layer/layerId', data);

// 获取三道防线/自定义图层
export const getDefenseLayerList = (params) =>
  http.get<any>('/resource/customLayer/selectCustomLayerList', { params });

// 获取三道防线/自定义图层对应图层点位信息
export const getDefensePointListByLayerId = (params) =>
  http.get<any>('/resource/customLayer/selectPointListByLayerId', { params });
