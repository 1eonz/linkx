import http from '@/utils/http';

export const queryVideoControlTaskList = (data) => http.post<any>('/iap/1.0/suspect-tasks', data);

export const queryVideoControlTaskDetail = (params) =>
  http.get<any>('/iap/1.0/suspect-task-detail', { params });

export const addSuspectTask = (data, formDate?: boolean) => {
  const config = {
    headers: {
      'Content-Type': 'multipart/form-data',
    },
  };
  return http.post<any>('/iap/1.0/suspect-task', data, formDate ? config : {});
};

export const editSuspectTask = (data) => http.put<any>('/iap/1.0/suspect-task', data);

export const enableSuspectTask = (data) => http.post<any>('/iap/1.0/enable-suspect-task', data);

export const stopSuspectTask = (data) => http.post<any>('/iap/1.0/stop-suspect-task', data);

export const deleteSuspectTask = (data) => http.post<any>('/iap/suspect-task/delete', data);

export const stopVideoControl = (data) => http.post<any>('/videoControl/stopVideoControl', data);

export const queryVideoControlWarningList = (data) => http.post<any>('/iap/1.0/alarms', data);

export const queryVideoControlWarningDetail = (params) =>
  http.get<any>('/iap/1.0/alarm-detail', { params });

export const operateWarningDetail = (data) => http.post<any>('/iap/1.0/operate-alarm', data);

export const deleteDisableSuspectTask = (params) =>
  http.delete<any>('/iap/1.0/suspect-task-delete-disable', { params });

export const deleteTrueTask = (data) => http.post<any>('/iap/1.0/delete-suspect-task', data);
