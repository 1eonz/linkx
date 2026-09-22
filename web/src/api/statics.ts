import http from '@/utils/http';
const baseUrl = '/linkx/desktop/collaboration/v1';
const adminBaseUrl = '/linkx/desktop/admin/v1/';
const dashboardBaseUrl = '/linkx/desktop/dashboard/v1';
// 获取版本信息
export const getVersion = () => http.get<any>(`${baseUrl}/base/version`);

// 协同监测统计导出
// departmentCode 部门编码，为空代表全部数据
// startTime 开始时间（必填，YYYY-MM-DD）
// endTime 结束时间（必填，YYYY-MM-DD）
// includeChildren 是否包含子部门，0-不包含 1-包含（默认 1）
// 限制：最大导出半年的数据（前后端都需限制）
export const exportStatics = (params: {
  departmentCode?: string;
  startTime: string;
  endTime: string;
  includeChildren?: 0 | 1;
}) => {
  return http.get<Blob>(`${dashboardBaseUrl}/static/export`, {
    params,
    responseType: 'blob',
  });
};

// 当前组织
export const getCurrentOrganization = (data: any) =>
  http.get<any>(`${baseUrl}/post/queryUserByIdCard?idCard=${data.idCard}`, data);
// 下级组织
export const queryDepartment = (data: any, isQueryAll = false) => {
  const url = `${baseUrl}/post/queryDepartment`;
  // isQueryAll=true 时不传 parentCode
  const params = isQueryAll ? { peerId: data.peerId } : data;
  return http.get(url, { params });
};
// 组织一次性获取子孙节点
export const queryDepartmentTree = (data: any, isQueryAll = false) => {
  const url = `${baseUrl}/organization/tree`;
  // isQueryAll=true 时不传 parentCode
  const params = isQueryAll ? { peerId: data.peerId } : data;
  return http.get(url, { params });
};

// 协同岗统计
export const getStaticCounts = (data: any) =>
  http.get<any>(`${baseUrl}/statistics/count`, { params: data });

// 0人员在线协同岗分页查询
export const listZeroOnDutyPosts = (data: any) =>
  http.get<any>(`${baseUrl}/unattended/page`, { params: data });

// 协同处置消息top10
export const dispositionCount = (data: any) =>
  http.get<any>(`${baseUrl}/statistics/dispositionCount`, { params: data });

// 统计协同岗平均回复时长top10
export const replyDuration = (data: any) =>
  http.get<any>(`${baseUrl}/statistics/replyDuration`, { params: data });

// 创群榜单根据人员统计
export const groupByUser = (data: any) =>
  http.get<any>(`${baseUrl}/statistics/group/create/count/by/user`, { params: data });
// 协同岗在线统计
export const getOnlineStatistics = (data: any) =>
  http.get<any>(`${baseUrl}/statistics/online/list`, { params: data });
// 人员核查统计
export const getPersonnelVerificationStatistics = (data: any) =>
  http.get<any>(`${baseUrl}/statistics/personnel/verification/list`, { params: data });
// 问题处理统计
export const getProblemStatistics = (data: any) =>
  http.get<any>(`${baseUrl}/statistics/task/count`, { params: data });
// 回复统计top10-回复总榜数
export const getReplyStatistics = (data: any) =>
  http.get<any>(`${baseUrl}/statistics/replyCount`, { params: data });
// 回复统计top10-平均回复时长榜
export const getReplyDurationStatistics = (data: any) =>
  http.get<any>(`${baseUrl}/statistics/disposition/replyDuration`, { params: data });
// 创群榜单-组织
export const getCreateGroupStatistics = (data: any) =>
  http.get<any>(`${baseUrl}/statistics/group/create/count`, { params: data });
// 协同群组统计
export const getCollaborationGroupsCount = (data: any) =>
  http.get<any>(`${baseUrl}/groups/count`, { params: data });
// 逾期回复列表
export const getOverdueReplyList = (data: any) =>
  http.get<any>(`${baseUrl}/tasks/expired/page`, { params: data });

// 获取角色权限树数据通过用户id
export const queryRoleDepartMentTreebyUserId = (data: any) =>
  http.get(`/linkx/desktop/auth/v1/role/byUserId?userId=${data.userId}`);
// -------------------------------------------------------------协同群组接口
// 统计
export const getCollaborationStatistics = () =>
  http.get(`${baseUrl}/groups/count`);
// 标签列表
export const getCollaborationTagList = (data: any) =>
  http.get(`${baseUrl}/tags/page?pageSize=${data.pageSize}`);
// 标签列表-包含删除的标签
export const getCollaborationTagListAll = (data: any) =>
  http.get(`${baseUrl}/tags/all?userId=${data.userId}`);
// 编辑标签（支持批量修改）
export const updateCollaborationTag = (data: any) =>
  http.put(`${baseUrl}/groups/batch/tags`, data);
// 未归档/已归档列表
export const getCollaborationGroupList = (data: any) => {
  let url = `${baseUrl}/groups/tags/page?pageNum=${data.pageNum}&pageSize=${data.pageSize}&archived=${data.archived}&userId=${data.userId}&keywords=${data.keywords}&tagName=${data.tagName}`;

  // 只有当 archivedTime 存在且不为空时才添加到查询参数中
  if (data.archivedTime !== undefined && data.archivedTime !== null && data.archivedTime !== '') {
    url += `&archivedTime=${data.archivedTime}`;
  }
  if (data.type) {
    url += `&type=${data.type}`;
  }

  return http.get(url);
};
// 收藏
export const getCollect = (data: any) =>
  http.post(`${baseUrl}/cares?groupId=${data.groupId}&userId=${data.userId}`);
// 收藏群组列表
export const getCollectList = (data: any) =>
  http.get(
    `${baseUrl}/groups/page?pageNum=${data.pageNum}&pageSize=${data.pageSize}&userId=${data.userId}&archived=${data.archived}&archivedTime=${data.archivedTime}&orgIds=${data?.orgIds}`,
  );
// 取消收藏
export const getCancelCollect = (data: any) =>
  http.delete(`${baseUrl}/cares?groupId=${data.groupId}&userId=${data.userId}`);
// 已归档时间列表
export const getArchivedTimeList = (data: any) =>
  http.get(
    `${baseUrl}/groups/archive/timeline?userId=${data.userId}&keywords=${data.keywords}`,
  );
// 我的收藏-已归档时间列表
export const getMyCollectArchivedTimeList = (data: any) =>
  http.get(`${baseUrl}/groups/cared/archive/timeline?userId=${data.userId}`);

// 归档
export const getArchived = (data: any) =>
  http.put(
    `${baseUrl}/groups/archive?groupId=${data.groupId}&userId=${data.userId}`,
    data,
  );
// 聊天消息
export const getChatHistory = (data: any) => {
  let url = `${baseUrl}/groups/${data.groupId}/msgs/archive/page`;

  return http.get(url, { params: data });
};
// 查询群成员
export const getChatHistoryCount = (data: any) =>
  http.get(
    `${baseUrl}/groups/${data.groupId}/members/archive/page?pageSize=${data.pageSize}`,
  );
// 查询群成员
export const getChatMember = (params) =>
  http.get(
    `${baseUrl}/post/queryUserByGroupId?groupId=${params.groupId}`,
    params,
  );

// 获取引用消息
export const getQuoteMsg = (data: any) =>
  http.get(`${baseUrl}/groups/${data.groupId}/${data.messageId}`);
// license控制
export const getLicenseInfo = () => http.get(`${adminBaseUrl}/msip/license/info`);
// 根据用户id批量获取信息
export const getUserInfo = (data: any) =>
  http.post(`${baseUrl}/post/queryUser/batch`, data.userIds);
// 根据协同岗id批量获取协同岗信息
export const getCollocation = (data: any) =>
  http.post(`${baseUrl}/post/query/batch`, data.userIds);
// 群聊消息发送排行榜TOP10
export const getGroupMsgsTop = (params: any) =>
  http.get(`/linkx/desktop/dashboard/v1/group/msgs/top`, { params });

// 获取系统配置
export const getSystemConfig = () =>
  http.get<{ key: string; value: string }[]>(`${adminBaseUrl}system/config`);

// 通过消息id查找对应的协同任务
export const getTaskByMsgId = (params) =>
  http.get(`${baseUrl}/tasks/detailBySeqId`, { params });

// 引用回复消息
export const responsesTask = (data) =>
  http.post(`${baseUrl}/tasks/response/responses`, data);

// 非引用回复消息修改所有状态
export const responsesTaskAll = (data) =>
  http.post(`${baseUrl}/tasks/response/reply/all`, data);

// 查询指定群组的协同岗列表
export const getGroupCoopUsers = (groupId: string | number) =>
  // http.get(`http://172.24.66.94:8099/collaboration/groups/${groupId}/coop/list`);
  http.get(`${baseUrl}/groups/${groupId}/coop/list`);

// 对群组协同岗发起评分
export const submitGroupRating = (groupId: string | number, data: any) =>
  // http.post(`http://172.24.66.94:8099/collaboration/groups/${groupId}/rating`, data);
  http.post(`${baseUrl}/groups/${groupId}/rating`, data);

// 查询我对指定群组协同岗的已评价列表
export const getGroupRatingList = (groupId: string | number, params: any) =>
  // http.get(`http://172.24.66.94:8099/collaboration/groups/${groupId}/rating/list`, { params });
  http.get(`${baseUrl}/groups/${groupId}/rating/list`, { params });

// 查询当前用户对指定群组的评价状态
export const getGroupRatingStatus = (groupId: string | number) =>
  // http.get(`http://172.24.66.94:8099/collaboration/groups/${groupId}/rating/status`);
  http.get(`${baseUrl}/groups/${groupId}/rating/status`);

// 获取协同案件支撑评分统计
export const getGroupTagRatingStat = (params: any) =>
  http.get('/linkx/desktop/dashboard/v1/group/rating/tag', { params });

// 获取协同岗评分
export const getCoopUserRatingStat = (params: any) =>
  http.get('/linkx/desktop/dashboard/v1/group/rating/coopUser', { params });

export const getTaskNavConfig = () => http.get(`${adminBaseUrl}/api/v1/app/callable`)

// ============================================================
// 一键调度相关接口
// ============================================================

/**
 * 获取组织列表（一键调度）
 * @param {Object} params - { imUserId: string }
 */
export const getCustomDepartmentTree = (params: any) =>
  // http.get('http://172.24.65.16:8081/auth/custom-department/tree', { params });
  http.get('/linkx/desktop/admin/auth/v1/custom-department/tree', { params });

/**
 * 获取一级及子部门（一键调度）
 * @param {Object} params - { dutyType: number, dutyStartDate: string, dutyEndDate: string, departmentCustomId: string|number, imUserId: string }
 */
export const getCustomDepartmentNode = (params: any) =>
  // http.get('http://172.24.65.16:8081/auth/custom-department/node', { params });
  http.get('/linkx/desktop/admin/auth/v1/custom-department/node', { params });

/**
 * 获取人员信息（一键调度）
 * @param {string|number} id - 部门ID
 * @param {Object} params - { dutyType: number, dutyStartDate: string, dutyEndDate: string, imUserId: string }
 */
export const getDutyScheduleUser = (id: string | number, params: any) =>
  // http.get(`http://172.24.65.16:8081/auth/custom-department/node/${id}/duty-schedule-user`, { params });
  http.get(`/linkx/desktop/admin/auth/v1/custom-department/node/${id}/duty-schedule-user`, { params });

// 查询推送消息列表
export const getNotifyList = (params) =>
  http.get(`/linkx/desktop/third/v1/push-message`, { params });

// 查询当前节点关联的授权 P2P 节点列表
export const getP2PNodes = (params: { grant: string }) =>
  http.get('/linkx/desktop/node/v1/p2p', { params });
