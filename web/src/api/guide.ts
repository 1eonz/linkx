import http from '@/utils/http';

export const getGuideList = (params) =>
  http.get<any>('/resource/alertDispositionGuide/list', { params });

export const getGuideDetail = (params) =>
  http.get<any>('/resource/alertDispositionGuide/byId', { params });
