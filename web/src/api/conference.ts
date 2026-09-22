import http from '@/utils/http';

// 创建会议
export const createConference = (data) => http.post<any>('/resource/conference/conference', data);

// 根据会议id获取详情
export const queryConference = (data) => http.put<any>('/resource/conference/conference', data);

// 删除会议
export const deleteConference = (params) =>
  http.delete<any>('/resource/conference/conference', { params });

// 获取与会人员
export const queryConferenceItems = (params) =>
  http.get<any>('/resource/conference/items', { params });

// 新增与会人员
export const addConferenceItems = (data) => http.post<any>('/resource/conference/items', data);

// 删除与会人员
export const deleteConferenceItems = (params) =>
  http.delete<any>('/resource/conference/items', { params });
