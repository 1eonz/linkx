import http from '@/utils/http';

export const queryFacilityDetailById = (data) =>
  http.post<any>('/resource/facility/queryFacilityDetailById', data);

export const addFacilityToFavorites = (data) =>
  http.post<any>('/resource/facility/addFacilityToFavorites', data);

export const deleteFacilityToFavorites = (data) =>
  http.post<any>('/resource/facility/deleteFacilityToFavorites', data);

export const queryFacilityCatalogByParam = (data) =>
  http.post<any>('/resource/facility/queryFacilityCatalogByParam', data);

export const queryFacilitiesByParamAndPage = (data) =>
  http.post<any>('/resource/facility/queryFacilitiesByParamAndPage', data);

export const queryFacilitiesByDistance = (data) =>
  http.post<any>('/resource/facility/queryFacilitiesByDistance', data);

export const queryFacilitiesByLike = (data) =>
  http.post<any>('/resource/facility/queryFacilitiesByLike', data);

export const queryFacilitiesByEquipmentId = (params) =>
  http.get<any>(`/resource/facility/queryFacilitiesByEquipmentId/${params}`);

export const queryFacilityTypeByCategory = (data) =>
  http.post<any>('/resource/facility/queryFacilityTypeByCategory', data);

// 查所有摄像头数据返回的是csv
export const dumpFacilities = () => http.get<any>('/resource/facility/dumpFacilities');
