import http from '@/utils/http';

// 添加车辆幻化接口
export const createVehicle = (data) =>
  http.post<any>('/resource/vehiclechange/createVehicle', data);

// 更新车辆幻化列表接口
export const updateVehicle = (data) =>
  http.post<any>('/resource/vehiclechange/updateVehicle', data);

// 添加车辆幻化列表接口
export const createVehicleList = (data) =>
  http.post<any>('/resource/vehiclechange/createVehicleList', data);

// 查询车辆幻化列表接口
export const queryVehicle = (data) => http.post<any>('/resource/vehiclechange/queryVehicle', data);

// 删除车辆幻化接口
export const deleteVehicle = (data) =>
  http.post<any>(`/resource/vehiclechange/deleteVehicle?id=${data}`);
