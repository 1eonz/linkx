import http from '@/utils/http';

// 获取用户沙盘绘制数据
export const querySandTableMarkers = () =>
  http.post<any>('/resource/planGroup/querySandTableMarkers');

// 保存用户沙盘绘制数据
export const insertSandTableMarkers = (data) =>
  http.post<any>('/resource/planGroup/insertSandTableMarkers', data);

// 获取沙盘绘制和车辆幻化图标列表
export const getIconList = (data) => http.get<any>(`/resource/icon/list?type=${data}`);
