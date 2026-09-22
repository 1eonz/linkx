import http from '@/utils/http';

// 地标单位
export const getLandmarkList = (data) => http.post<any>('/resource/landmarkUnits/list', data);

export const addLandmark = (data) => http.post<any>('/resource/landmarkUnits/add', data);

export const updateLandmark = (data) => http.post<any>('/resource/landmarkUnits/update', data);

export const deleteLandmark = (data) => http.post<any>('/resource/landmarkUnits/delete', data);

export const collectLandmark = (data) => http.post<any>('/resource/landmarkUnits/collect', data);
