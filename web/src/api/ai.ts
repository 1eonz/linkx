import iccHttp from '@/utils/http';

export const getAgents = (params) =>
  iccHttp.get<any>('/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/agents', { params });

export const newTask = (params) =>
  iccHttp.post<any>('/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/v2/contend', params);

export const getAnswer = (params) =>
  iccHttp.get<any>('/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/v2/detail', { params });

export const getAllTabs = (params) =>
  iccHttp.get<any>('/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/category/list', { params });

// 分页查询申请列表
export const getAgentSubmissionPage = (params) =>
  iccHttp.get<any>('/linkx/desktop/collaboration/v1/agent/submission/page', { params });
// 创建智能体申请
export const addAgentSubmission = (data) =>
  iccHttp.post<any>('/linkx/desktop/collaboration/v1/agent/submission', data);
// 查询智能体部署配置
export const getDeployConfig = () => iccHttp.get<any>('/linkx/desktop/admin/v1/globals/ai/deploy');

//查询历史对话列表
export const getAiHistory = (params) =>
  iccHttp.get<any>('/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/record/user/list', { params });

//分页查询智能体对话历史V2
export const getAiHistoryDetail = (params) =>
  iccHttp.get<any>('/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/v2/record/history/page', {
    params,
  });

// 删除用户智能体历史记录
export const deleteAiHistory = (idCard: string, params: any) =>
  iccHttp.delete<any>(`/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/record/user/${idCard}/list`, { data: params });

// 修改回复读取状态
export const updateReplyPauseState = (params) =>
  iccHttp.post<any>('/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/v2/reply/read-state', params);

export const updateApprovalWsSession = (params) =>
  iccHttp.post<any>('/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/approval/status/update', params);

// web/src/api/ai.ts

/**
 * 文件上传（支持进度和取消）
 * @param formData - 包含文件的 FormData
 * @param options - 配置选项
 * @param options.onProgress - 进度回调 => void
 * @param options.signal - 取消信号
 * @returns Promise
 */
export const uploadFile = (formData: FormData, options: any = {}) => {
  const { onProgress, signal } = options;

  const handler = iccHttp.post(
    '/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/file/upload',
    formData,
    {
      onUploadProgress: (progressEvent: any) => {
        if (progressEvent.lengthComputable && onProgress) {
          const progress = Math.round((progressEvent.loaded / progressEvent.total) * 100);
          onProgress(progress);
        }
      },
    },
  );

  // 监听取消信号
  if (signal) {
    signal.addEventListener('abort', () => {
      handler.abortFetch();
    });
  }

  return handler;
};

/**
 * 获取文件能力配置
 * @param agentId - 智能体 ID
 * @returns Promise
 */
export const getFileCapabilities = (agentId: string) => {
  return iccHttp.get(`/linkx/desktop/XA-ics-agent/proxy/ai/v1/deepseek-zjk/xa/dk/file/capabilities/${agentId}`);
};

export const sendApproveCard = (data: { userId: number; approveUser: string; toLeaderUrl: string }) =>
  iccHttp.post<any>('/linkx/desktop/collaboration/v1/im/send/approve/card', data);

// 根据身份证号查询用户信息
export const getUserInfoByIdCard = (idCard: string | number) => iccHttp.get<any>(`/linkx/desktop/collaboration/v1/im/users/idCard/${idCard}`);
