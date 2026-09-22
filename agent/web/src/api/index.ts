import http from '@/utils/http';

// 新增智能体
export const addAiagent = (data) =>
  http.post<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/management', data);

// 获取智能体列表
export const getAiagentPage = (params) =>
  http.get<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/management/page', { params });

// 删除智能体
export const deleteAiagent = (id) =>
  http.delete<any>(`/agent/admin/admin-api/proxy/ai/v1/aiagent/management/${id}`);

// 更新智能体
export const updateAiagent = (data) =>
  http.put<any>(`/agent/admin/admin-api/proxy/ai/v1/aiagent/management/${data.id}`, data);

// 文件上传
export const uploadFile = (data) =>
  http.post<any>('/agent/admin/admin-api/proxy/ai/v1/infra/file/upload', data);

// 获取查询统计

export const getAiagentRecordPage = (params) =>
  http.get<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/management/record', { params });

// 删除查询统计

export const deleteAiagentRecord = (id) =>
  http.delete<any>(`/agent/admin/admin-api/proxy/ai/v1/aiagent/management/record/${id}`);

// 导出查询统计
export const exportAiagentRecord = (params) =>
  http.get<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/management/record/export', {
    params,
    responseType: 'blob',
  });

// 新增分类
export const createCategory = (data) =>
  http.post<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/management/category/saveOrUpdate/batch', data);

// 查询所有分类
export const queryCategory = () =>
  http.get<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/management/category/list');

// 查询所有分类
export const deleteCategory = (params) =>
  http.delete<any>(`/agent/admin/admin-api/proxy/ai/v1/aiagent/management/category/${params.id}`);

// 创建智能体绑定关系
export const createAssistantAgent = (data) => http.post('/agent/admin/admin-api/proxy/ai/v1/aiagent/virtual/user/bind/list', data);
// 更新智能体绑定关系
export const updateAssistantAgent = (id, data) => {
 return http.put(`/agent/admin/admin-api/proxy/ai/v1/aiagent/virtual/user/agent/${id}`, data);
};
//智能体绑定关系列表
export const assistantAgentList = (params) => {
 return http.get(`/agent/admin/admin-api/proxy/ai/v1/aiagent/virtual/user/agent/page`, { params });
};
export const getAgentFileList = () =>
  http.get<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/attachment-config/list');

export const createAgentFile = (data) =>
  http.post<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/attachment-config/create', data);

export const updateAgentFile = (data) =>
  http.put<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/attachment-config/update', data);

export const deleteAgentFile = (id) =>
  http.delete<any>(`/agent/admin/admin-api/proxy/ai/v1/aiagent/attachment-config/delete/${id}`);

//虚拟用户列表
export const getVirtualUserList = () => {
 return http.get(`/agent/admin/admin-api/proxy/ai/v1/aiagent/virtual/user/virtual`,);
};
// 查询全局变量（不传 name 查全部）
export const getGlobals = (params?: { name?: string }) =>
  http.get<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/management/settings/globals', { params });

// 修改全局变量
export const updateGlobals = (data: {
  id: number;
  value: string;
  remark?: string;
  status?: number;
}) =>
  http.post<any>('/agent/admin/admin-api/proxy/ai/v1/aiagent/management/settings/globals', data);