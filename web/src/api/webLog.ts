import http from '@/utils/http';

export const insertWeblog = (data) => http.post<any>('/weblogs/insertWeblog', data);

export const selectWeblogSwitch = (data) =>
  http.post<any>('/weblogs/weblogSwitch/selectWeblogSwitch', data);

export const updateWeblogSwitch = (data) =>
  http.post<any>('/weblogs/weblogSwitch/updateWeblogSwitch', data);
