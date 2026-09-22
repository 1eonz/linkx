import http from '@/utils/http';
const baseUrl = '/linkx/desktop/collaboration/v1';
// 检测用户是否绑定协同岗及协同岗开关状态
export const getSwitchStatus = ({ userId }) =>
  http.get(`${baseUrl}/attendance/getSwitchStatus?userId=${userId}`);

// 统计该协同岗当前剩余人数
export const getLastNum = ({ userId }) =>
  http.get(`${baseUrl}/attendance/getLastNum?userId=${userId}`);

// 切换上下岗状态
export const switchStatus = (data) =>
  http.post(`${baseUrl}/attendance/switchStatus`, data);

// 标签列表
export const collaborationLabelList = (scope?: number) =>
  http.get(`${baseUrl}/label/list${scope ? `?scope=${scope}` : ''}`);

// 一键建群
export const labelCreateGroup = (data) =>
  http.post(`${baseUrl}/label/createGroup`, data);

// 心跳检测
export const heartbeatApi = (data) =>
  http.get(`${baseUrl}/attendance/heartbeat/${data.userId}`);
