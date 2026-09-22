import http from '@/utils/http';

export const queryVideoPollingByParam = (data: any) =>
  http.post<any>('/resource/videoPolling/queryVideoPollingByParam', data); // 视频轮巡列表

export const saveVideoPolling = (data: any) =>
  http.post<any>('/resource/videoPolling/saveVideoPolling', data); // 保存轮巡列表

export const delVideoPollingGroup = (data: any) =>
  http.post<any>('/resource/videoPolling/delVideoPollingGroup', data); // 删除视频轮巡

export const modifyVideoPolling = (data: any) =>
  http.post<any>('/resource/videoPolling/modifyVideoPolling', data); // 修改视频轮巡
