import http from '@/utils/http';

// 删除辖区
export const deleteRegion = (params) => http.get<any>(`/resource/regionrange/delete/${params}`, {});

// 获取组织
export const getOrgs = () => http.get<any>('/resource/regionrange/orgs', {});

// 添加自定义辖区
export const saveRegion = (data: any) => http.post<any>('/resource/regionrange/save', data);

// 更新辖区
export const updateRegion = (data: any) => http.post<any>('/resource/regionrange/update', data);

// 查询辖区列表
export const queryRegionList = () =>
  http.get<any>('/resource/regionrange/queryOwnerRegionRangeByAreaCode', {});

// 辖区地图
export const queryRegionRangeByAreaCode = (params) =>
  http.post<any>('/resource/regionrange/queryRegionRangeByAreaCode', params);
