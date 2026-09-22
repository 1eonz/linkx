import http from '@/utils/http';

export const queryAttendanceStatusList = (data) =>
  http.post<any>('/resource/attendance/queryAttendanceStatusList', data);

export const queryAttendanceById = (params) =>
  http.get<any>('/resource/attendance/queryAttendanceById', { params });

export const attendanceExport = (data) =>
  http.postDownload<any>('/resource/attendance/export/data/status', data);

export const queryAttendanceStatusHistoryList = (data) =>
  http.post<any>('/resource/attendance/queryAttendanceStatusHistoryList', data);
