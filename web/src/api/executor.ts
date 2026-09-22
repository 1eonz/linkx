import http from '@/utils/http';

export const queryExecutorByUserId = (data) =>
  http.post<any>('/resource/executor/queryExecutorByUserId', data);

export const queryExecutorDetailById = (data) =>
  http.post<any>('/resource/executor/queryExecutorDetailById', data);

export const queryExecutorsByParamAndPage = (data) =>
  http.post<any>('/resource/executor/queryExecutorsByParamAndPage', data);

export const queryExecutorsByDistance = (data) =>
  http.post<any>('/resource/executor/queryExecutorsByDistance', data);

export const queryExecutorDetailByIds = (data) =>
  http.post<any>('/resource/executor/queryExecutorDetailByIds', data);

// 通过isdn号获取人员信息
export const queryExecutorDetailByAccount = (data) =>
  http.post<any>('/resource/executor/queryExecutorDetailByAccount', data);

// 资源搜索
export const queryExecutorsByLike = (data) =>
  http.post<any>('/resource/executor/queryExecutorsByLike', data);

// 提醒配置
export const selectRemindSwitch = (data) =>
  http.post<any>('/resource/remindSwitch/selectRemindSwitch', data);

// 全局配置
export const updateRemindSwitch = (data) =>
  http.post<any>('/resource/remindSwitch/updateRemindSwitch', data);
