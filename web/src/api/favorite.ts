import http from '@/utils/http';

// 收藏数量
export const countEquipmentFavorites = () =>
  http.post<any>('/resource/equipment/countEquipmentFavorites');

// 收藏列表
export const listFavorite = (data) => http.post<any>('/resource/equipment/listFavorite', data);

// 收藏
export const addFavorite = (data) => http.post<any>('/resource/equipment/addFavorite', data);

// 取消收藏
export const delFavorite = (data) => http.post<any>('/resource/equipment/delFavorite', data);
