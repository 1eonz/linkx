import http from '@/utils/http';

// 统计所有的资源在线离线数
export const countEquipmentAndFacilities = (params) =>
  http.post<any>('/resource/equipment/countEquipmentAndFacilities', params);

export const countEquipmentAndFacilitiesByOrg = (params) =>
  http.get<any>('/resource/equipment/countEquipmentAndFacilitiesByOrg', { params });

export const countEquipmentAndFacilitiesByArea = (params) =>
  http.get<any>('/resource/equipment/countEquipmentAndFacilitiesByArea', { params });

// eChart地图jsonData
export const getMapJson = () => http.get<any>('/cloudcmd/data/data-city.json');

// 查询行政区域
export const queryArea = () => http.post<any>('/resource/administrative/query');

// 查询组织
export const queryOrganizationByArea = (params) =>
  http.post<any>('/resource/organization/queryOrganizationByArea', params);

export const queryStatusCount = (data, isConf?) =>
  http.post<any>(`/resource/status/queryStatusCount${isConf ? '?isVideoConference=1' : ''}`, data);
