import http from '@/utils/http';

export const queryFenceAlarmList = (data) =>
  http.post<any>('/iap/fenceAlarm/queryFenceAlarmList', data);

export const queryFenceAlarmById = (params) =>
  http.get<any>('/iap/fenceAlarm/queryFenceAlarmById', { params });

export const exportFenceAlarmData = (data) =>
  http.postDownload<any>('/iap/fenceAlarm/exportFenceAlarmData', data);

export const queryDeviceAlarmCount = (data) =>
  http.post<any>('/iap/resource/deviceAlarm/count', data);

export const exportCountAlarmData = (data) =>
  http.postDownload<any>('/iap/fenceAlarm/exportCountAlarmData', data);

export const createAlarmRule = (data) => http.post<any>('/resource/rule/create', data);

export const alarmRuleList = (data) => http.post<any>('/resource/rule/list', data);

export const recoverFenceAlarmById = (data) =>
  http.post<any>('/iap/fenceAlarm/recoverFenceAlarmById', data);
