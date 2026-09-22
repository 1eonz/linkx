import http from '@/utils/http';

export const queryEquipmentDetailById = (data) =>
  http.post<any>('/resource/equipment/queryEquipmentDetailById', data);

export const queryEquipmentExt = (params) =>
  http.get<any>('/resource/equipment/equipmentExt', { params });

export const queryEquipmentDetailByAccount = (data) =>
  http.post<any>('/resource/equipment/queryEquipmentDetailByAccount', data);

export const queryEquipmentsByParam = (data) =>
  http.post<any>('/resource/equipment/queryEquipmentsByParam', data);

export const queryEquipmentsByPage = (data) =>
  http.post<any>('/resource/equipment/queryEquipmentsByPage', data);

export const queryEquipmentsByDistance = (data) =>
  http.post<any>('/resource/equipment/queryEquipmentsByDistance', data);

// 设备ids批量获取通信号
export const getAccountsByEquipmentIds = (data) =>
  http.post<any>('/resource/equipment/accounts', data);

// 查询是否具备gis的设备类型
export const queryEquipmentTypeByGisFlag = () =>
  http.get<any>('/resource/equipment/queryEquipmentTypeByGisFlag?gisFlag=0');

export const queryEquipmentsByLike = (data) =>
  http.post<any>('/resource/equipment/queryEquipmentsByLike', data);

export const queryCarFuzzySearching = (data) =>
  http.post<any>('/resource/policeCar/fuzzySearching', data);

export const queryPoliceCar = (params) => http.get<any>(`/resource/policeCar/${params}`);

export const queryPoliceCarPage = (data) => http.post<any>('/resource/policeCar/page', data);

export const queryPoliceCarOnlineOverview = (data) =>
  http.post<any>('/resource/policeCar/onlineOverview', data);

// 通过人员绑定的身份证号查人员绑定的设备列表
export const queryEquipmentByIdCard = (data) =>
  http.post<any>('/resource/equipment/queryEquipmentByIdCard', data);
