import http from '@/utils/http';

// 逆地理编码接口
export const getAddressByLocation = (data) =>
  http.post<any>('/lbs/geocoding/getAddressByLocation', data);

// 地理编码接口
export const getLocationByAddress = (params) =>
  http.get<any>('/lbs/geoDecoding/getLocationByAddress', { params });

// 动向回放列表
export const queryTrackList = (data) => http.post<any>('/lbs/trackQuery/list', data);

// 动向回放统计数据
export const queryTrackHistorySummary = (data) =>
  http.post<any>('/lbs/queryTrackHistorySummary', data);

// 分页加载动向回放数据
export const queryTrackHistoryInfo = (data) => http.post<any>('/lbs/queryTrackHistoryInfo', data);

// 警车动向回放统计数据
export const queryPoliceCarTrackSummary = (data) =>
  http.post<any>('/resource/policeCar/traceSummary', data);

// 警车分页加载动向回放数据
export const queryPoliceCarTrace = (data) => http.post<any>('/resource/policeCar/trace', data);

// 获取服务器时间
export const queryServerTime = () => http.get<any>('/base/globals/serverTime');

// 查询离线地图列表
export const selectListMap = (data) => http.post<any>('/resource/map/selectListMap', data);

// 地理逆编码查询
export const selectInversecode = (data) => http.post<any>('/resource/map/selectInversecode', data);

// POI查询
export const selectPoi = (data) => http.post<any>('/resource/map/selectPoi', data);
