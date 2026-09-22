import request from '@/utils/request'
const agentUrl = `/XA-ics-agent/proxy/ai/v1`; 
// 获取部署配置
export const getDeploySettings = () =>
  request({ url: `/api/globals/ai/deploy`, method: 'get' })

// 更新部署配置
export const updateDeploySettings = data =>
  request({ url: `/api/globals/ai/deploy`, method: 'put', data })

// 获取智能体配置
export const getAiagentSettings = () =>
  request({ url: `${agentUrl}/aiagent/management/settings`, method: 'get' })

// 更新智能体配置
export const updateAiagentSettings = data =>
  request({ url: `${agentUrl}/aiagent/management/settings`, method: 'put', data })

// 新增智能体
export const addAiagent = data =>
  request({ url: `${agentUrl}/aiagent/management`, method: 'post', data })

// 删除智能体
export const deleteAiagent = id =>
  request({ url: `${agentUrl}/aiagent/management/${id}`, method: 'delete' })

// 更新智能体
export const updateAiagent = data =>
  request({ url: `${agentUrl}/aiagent/management/${data.id}`, method: 'put', data })

// 获取智能体分页列表
export const getAiagentPage = params =>
  request({ url: `${agentUrl}/aiagent/management/page`, method: 'get', params })

// 上传文件
export const uploadFile = data =>
  request({ url: '/XA-ics-agent/admin-api/proxy/ai/v1/infra/file/upload', method: 'post', data })

// 获取智能体执行记录分页列表
export const getAiagentRecordPage = params =>
  request({ url: `${agentUrl}/aiagent/management/record`, method: 'get', params })

// 删除智能体执行记录
export const deleteAiagentRecord = id =>
  request({ url: `${agentUrl}/aiagent/management/record/${id}`, method: 'delete' })

// 导出智能体执行记录
export const exportAiagentRecord = params =>
  request({ url: `${agentUrl}/aiagent/management/record/export`, method: 'get', params, responseType: 'blob' })

// 新增智能体分类
export const createCategory = data =>
  request({ url: `${agentUrl}/aiagent/management/category/saveOrUpdate/batch`, method: 'post', data })

// 获取智能体分类列表
export const queryCategory = () =>
  request({ url: `${agentUrl}/aiagent/management/category/list`, method: 'get' })

// 删除智能体分类
export const deleteCategory = id =>
  request({ url: `${agentUrl}/aiagent/management/category/${id}`, method: 'delete' })

// 导入智能体
export const importAiagent = data =>
  request({ url: `${agentUrl}/aiagent/management/import`, method: 'post', data })

// 导出智能体模板
export const exportAiagentTemplate = () =>
  request({ url: `${agentUrl}/aiagent/management/template`, method: 'get', responseType: 'blob' })

// 创建智能体绑定关系
export const createAssistantAgent = data =>
  request({ url: `/collaboration/v1/ai/assistant/agent`, method: 'post', data })

// 更新智能体绑定关系
export const updateAssistantAgent = (id, data) =>
  request({ url: `/collaboration/v1/ai/assistant/agent/${id}`, method: 'put', data })

// 智能体绑定关系列表
export const assistantAgentList = params =>
  request({ url: `/collaboration/v1/ai/assistant/agent/page`, method: 'get', params })

// 获取Agent文件配置列表
export const getAgentFileList = () =>
  request({ url: `${agentUrl}/aiagent/attachment-config/list`, method: 'get' })

// 创建Agent文件配置
export const createAgentFile = data =>
  request({ url: `${agentUrl}/aiagent/attachment-config/create`, method: 'post', data })

// 更新Agent文件配置
export const updateAgentFile = data =>
  request({ url: `${agentUrl}/aiagent/attachment-config/update`, method: 'put', data })

// 删除Agent文件配置
export const deleteAgentFile = id =>
  request({ url: `${agentUrl}/aiagent/attachment-config/delete/${id}`, method: 'delete' })
