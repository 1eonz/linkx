import { http, notInterceptorHttp } from '@/common/network/http.js';
import { getBaseUrl } from '@/common/config.js';

//智能权限申请
const baseUrl = '/collaboration/v1';

//更新申请状态
export const agentSubmissionStatus = (params) => {
  return http.request(
    baseUrl + `/agent/submission/${params.id}/status`,
    params,
    'put',
  );
};

//创建智能体申请
export const agentSubmission = (params) => {
  return http.post(baseUrl + `/agent/submission`, params);
};

//根据ID查询申请详情
export const getagentSubmissionDetailById = (params) => {
  return http.get(baseUrl + `/agent/submission/${params.id}`, params);
};

//分页查询申请列表
export const getAgentSubmissionPage = (params) => {
  return http.get(baseUrl + `/agent/submission/page`, params);
};

// 获取智能体列表
export const getAgents = (params, signal) => {
  return http.get('/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/agents', params, {}, { signal });
};

// 获取最近使用的智能体列表
// pramas：{ userId }
export const getUsedAgents = (params) => {
  return http.get('/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/record/user/list', params);
};

// 获取智能体分类
export const getAllTabs = () => {
  return http.get('/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/category/list');
};

// 获取审批配置
export const getApprovalConfig = () => {
  return http.get('/agent/api/proxy/ai/v1/aiagent/management/settings');
};

// 获取部署配置（从 groupAiHost 拼接路径）
export const getDeployConfig = () => http.get('/admin/v1/globals/ai/deploy');

// 发起询问（支持审批模式）
export const newTask = (params) => {
  return http.post('/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/v2/contend', params);
};

// 获取审批状态
export const getApprovalDetail = (params) => {
  return http.get('/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/v2/approval/detail', params);
};

// 询问结果
export const getAnswer = (params) => {
  return http.get('/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/v2/detail', params);
};

// 按智能体查询记录
export const getRecordsByAgent = (params) => {
  return http.get('/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/v2/record/history/page', params);
};

// 删除用户智能体历史记录
export const deleteUsedAgent = (idCard, params) => {
  return http.delete(`/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/record/user/${idCard}/list`, params);
}

// 修改回复读取状态
export const updateReplyPauseState = (params) => {
  return http.post('/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/v2/reply/read-state', params);
};

// 更新审批wsSessionId
export const updateApprovalWsSession = (params) => {
  return http.post('/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/approval/status/update', params);
};

/**
 * 文件上传
 * @param {FormData} formData - 包含文件的 FormData 对象
 * @returns {Promise}
 * @example
 * const formData = new FormData();
 * formData.append('file', file);
 * formData.append('agentId', agentId);
 * const res = await uploadFile(formData);
 */
export const uploadFile = (formData, options = {}) => {
  const { onProgress, signal } = options;
  return http.upFile(
    '/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/file/upload',
    formData,
    {
      'Content-Type': ' multipart/form-data',
    },
    {
      // 上传进度回调
      onUploadProgress: (progressEvent) => {
        if (progressEvent.lengthComputable && onProgress) {
          const progress = Math.round((progressEvent.loaded / progressEvent.total) * 100);
          onProgress(progress);
        }
      },
      // 取消信号
      signal: signal,
      // 文件上传超时时间30分钟
      timeout: 1000 * 60 * 30,
    },
  );
};

/**
 * 获取智能体文件能力配置
 * @param {string} agentId - 智能体ID
 * @returns {Promise}
 * @example
 * const res = await getFileCapabilities('agent-123');
 */
export const getFileCapabilities = (agentId) => {
  return http.get(`/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/file/capabilities/${agentId}`);
};

export const sendApproveCard = (params) => {
  return http.post(baseUrl + '/im/send/approve/card', params);
};

// 根据身份证号查询用户信息
export const getUserInfoByIdCard = (idCard) => http.get(baseUrl + `/im/users/idCard/${idCard}`);