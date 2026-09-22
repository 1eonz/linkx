import http from '@/utils/http';

// 证据下载
export const download = (params) => http.get<any>('/evidence/download', { params });

export const tokenGet = (data) => http.post<any>('/evidence/token/get', data);

export const queryProgressBar = (data) => http.post<any>('/evidence/queryProgressBar', data);

export const upload = (data) => http.post<any>('/evidence/upload', data);

export const getEvidenceList = (data) => http.post<any>('/evidence/list', data);

export const mrsAudioAndVideoUrl = (data) => http.post<any>('/evidence/mrs/audioAndVideoUrl', data);

export const mrsDownload = (data) =>
  http.get<any>(data, {
    responseType: 'blob',
    timeout: 7 * 24 * 60 * 60 * 1000,
  });

export const mrsPostDownload = (data) => http.postDownload<any>('/resource/mrs/download', data);
