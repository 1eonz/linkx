import http from '@/utils/http';
const baseUrl = '/linkx/desktop/collaboration/v1';
// 警单列表
export const getPoliceticket = (params) =>
  http.get(`${baseUrl}/policeticket/page`, { params });
// 关联警单
export const relativePoliceticket = ({ data, groupId }) =>
  http.post(`${baseUrl}/policeticket/groupbind/${groupId}`, data);

// 删除警单关联
export const deletePoliceticket = ({ groupId, ticketId }) =>
  http.delete(`${baseUrl}/policeticket/groupbind/${groupId}/${ticketId}`);

// 任务列表
export const getTasksList = (params) =>
  http.get(`${baseUrl}/tasksgroup/page`, { params });
// 关联任务
export const relativeTask = ({ data, groupId }) =>
  http.post(`${baseUrl}/tasksgroup/groupbind/${groupId}`, data);

// 删除任务关联
export const deleteTask = ({ groupId, ticketId }) =>
  http.delete(`${baseUrl}/tasksgroup/groupbind/${groupId}/${ticketId}`);
