import http from '@/utils/http';

import { ListResponse } from '#/axios';

export const queryMessageAlertList = (data) =>
  http.post<ListResponse>('/message/messageAlert/list', data);

export const queryMessageAlertUpdate = (data) =>
  http.post<any>('/message/messageAlert/update', data);

// 转入有效
export const createEventAndMission = (data) =>
  http.post<any>('/message/messageAlert/createEventAndMission', data);

export const queryMessageAlertDetails = (params) =>
  http.get<any>('/message/messageAlert/detail', { params });
