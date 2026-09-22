import { http } from '@/common/network/http.js';
const baseUrl = '/proxy/icp/v1';
export const getCameraPoints = (params) => http.get(`${baseUrl}/camera`, params);

export const getEquipmentPoints = (params) => http.get(`${baseUrl}/user`, params);

export const getImUserPoints = (params) => http.get(`${baseUrl}/imuser`, params);

export const getCameraLevel = (params) => http.get(`${baseUrl}/camera-level/tree/privs`, params);

export const getDepartment = (params) => http.get(`${baseUrl}/department/tree/privs`, params);

//逆地理编码
export const getAddressByLatlon = (params) => http.get(`/map/v1/api/geocode/regeo`, params);

export const getDeviceLayerList = () => http.get(`${baseUrl}/isdnType/list`);
