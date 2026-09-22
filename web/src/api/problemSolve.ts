import http from '@/utils/http';
const baseUrl = '/linkx/desktop/collaboration/v1';
// 警务协同任务批量答复
export const responsesTask = (data) =>
  http.post<any>(`${baseUrl}/tasks/response/responses`, data);

// 查询警务协同问题详情
export const queryDetail = (params) =>
  http.get<any>(`${baseUrl}/tasks/detail`, {
    params,
  });

// 更新协同任务状态
export const updateStatus = (data) =>
  http.put<any>(`${baseUrl}/tasks/update`, data);

// 通过消息id查找对应的协同任务
export const getTaskByMsgId = (params) =>
  http.get<any>(`${baseUrl}/tasks/detailBySeqId`, {
    params,
  });
// 警务协同任务非引用批量答复
export const responsesTaskAll = (data) =>
  http.post<any>(`${baseUrl}/tasks/response/reply/all`, data);
