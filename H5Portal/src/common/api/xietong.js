import { http } from '@/common/network/http.js';
const baseUrl = '/collaboration/v1';
//检测用户是否绑定协同岗及协同岗开关状态
export const getSwitchStatus = (params) =>
  http.get(baseUrl + '/attendance/getSwitchStatus', params);

//统计该协同岗当前剩余人数
export const getLastNum = (params) =>
  http.get(baseUrl + '/attendance/getLastNum', params);

//切换上下岗状态
export const switchStatus = (params) =>
  http.post(baseUrl + '/attendance/switchStatus', params);

// 心跳检测
export const heartbeatApi = (data) =>
  http.get(`${baseUrl}/attendance/heartbeat/${data.userId}`);

//位置自动上报
export const heartbeatLocation = (data) =>
  http.post(baseUrl + '/label/update/location', data);
