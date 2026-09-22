import http from '@/utils/http';

// 收藏地址
export const collectAddress = (data) => http.post<any>('/resource/address/collect', data);

export const updateAddress = (data) => http.post<any>('/resource/address/update', data);

export const selectAddress = (data) => http.post<any>('/resource/address/select', data);

export const deleteAddress = (params) => http.get<any>('/resource/address/delete', { params });

// 地址类型
export const createAddressType = (data) => http.post<any>('/resource/address/type/create', data);

export const updateAddressType = (data) => http.post<any>('/resource/address/type/update', data);

export const selectAddressType = (data) => http.post<any>('/resource/address/type/select', data);

export const deleteAddressType = (params) =>
  http.get<any>('/resource/address/type/delete', { params });
