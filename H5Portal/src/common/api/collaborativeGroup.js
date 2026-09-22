import { http } from '@/common/network/http.js';
const baseUrl = '/collaboration/v1';
// const baseUrlDev = 'http://172.24.66.94:8099/';
// 标签列表
export const getCollaborationTagList = (params) =>
  http.get(`${baseUrl}/tags/page`, params);
// 标签列表-包含删除的标签
export const getCollaborationTagListAll = (params) =>
  http.get(`${baseUrl}/tags/all`, params);
// 编辑标签
export const updateCollaborationTag = (params) =>
  http.request(`${baseUrl}/groups/batch/tags`, params, 'put');
// 未归档/已归档/我的群组列表
export const getCollaborationGroupList = (params) => {
  let url = `${baseUrl}/groups/tags/page?pageNum=${params.pageNum}&pageSize=${params.pageSize}&userId=${params.userId}&keywords=${params.keywords}&tagName=${params.tagName}`;

  // archived参数：0-未归档、2-已归档，空字符串或不传则不拼接该参数（获取全部数据）
  if (
    params.archived !== undefined &&
    params.archived !== null &&
    params.archived !== ''
  ) {
    url += `&archived=${params.archived}`;
  }

  // 只有当 archivedTime 存在且不为空时才添加到查询参数中
  if (
    params.archivedTime !== undefined &&
    params.archivedTime !== null &&
    params.archivedTime !== ''
  ) {
    url += `&archivedTime=${params.archivedTime}`;
  }

  if (params.type) {
    url += `&type=${params.type}`;
  }

  // 建群方式筛选
  if (params.createType) {
    url += `&createType=${params.createType}`;
  }

  // 我的群组范围筛选
  if (params.scope) {
    url += `&scope=${params.scope}`;
  }

  const tokenParam = { token: params.token };

  return http.get(url, tokenParam);
};
// 收藏
export const getCollect = (params) =>
  http.post(
    `${baseUrl}/cares?groupId=${params.groupId}&userId=${params.userId}`,
    params,
  );
// 收藏群组列表
export const getCollectList = (params) => http.get(`${baseUrl}/groups/page`, params);
// 取消收藏
export const getCancelCollect = (params) =>
  http.request(
    `${baseUrl}/cares?groupId=${params.groupId}&userId=${params.userId}`,
    params,
    'delete',
  );
// 已归档时间列表
export const getArchivedTimeList = (params) =>
  http.get(`${baseUrl}/groups/archive/timeline`, params);
// 我的收藏-已归档时间列表
export const getMyCollectArchivedTimeList = (params) =>
  http.get(`${baseUrl}/groups/cared/archive/timeline`, params);

// 归档
export const getArchived = (params) =>
  http.request(
    `${baseUrl}/groups/archive?groupId=${params.groupId}&userId=${params.userId}`,
    params,
    'put',
  );
// 聊天消息
export const getChatHistory = (params) =>
  http.get(`${baseUrl}/groups/${params.groupId}/msgs/archive/page`, params);
// 查询群成员
export const getChatHistoryCount = (params) =>
  http.get(`${baseUrl}/groups/${params.groupId}/members/archive/page`, params);
// 查询群成员
export const getChatMember = (params) =>
  http.get(`${baseUrl}/post/queryUserByGroupId`, params);

//警单列表
export const getPoliceticket = (params) =>
  http.get(`${baseUrl}/policeticket/page`, params);
//关联警单
export const relativePoliceticket = ({ groupId, data }) =>
  http.post(`${baseUrl}/policeticket/groupbind/${groupId}`, data);

//删除警单关联
export const deletePoliceticket = (params) =>
  http.request(
    `${baseUrl}/policeticket/groupbind/${params.groupId}/${params.ticketId}`,
    params,
    'delete',
  );

//任务列表
export const getTasksList = (params) => http.get(`${baseUrl}/tasksgroup/page`, params);
//关联任务
export const relativeTask = ({ groupId, data }) =>
  http.post(`${baseUrl}/tasksgroup/groupbind/${groupId}`, data);

//删除任务关联
export const deleteTask = (params) =>
  http.request(
    `${baseUrl}/tasksgroup/groupbind/${params.groupId}/${params.ticketId}`,
    params,
    'delete',
  );
// 根据用户id批量获取信息
export const getUserInfo = (params) =>
  http.post(`${baseUrl}/post/queryUser/batch`, params.userIds);
// 根据协同岗id批量获取信息
export const getColloration = (params) =>
  http.post(`${baseUrl}/post/query/batch`, params.userIds);
// 根据用户ID查询用户详情（与 web 端 getUserDetailById 对齐，后端做权限校验）
export const getUserDetailById = (userId) =>
  http.get(`${baseUrl}/im/users/${userId}`);
//获取群组详情
export const getGroupListByGroupIds = (params) =>
  http.post(`${baseUrl}/groups/group/list`, params);
// 根据群ID获取群信息
export const glassesGroupinfo = (params) =>
  http.get(baseUrl + '/glasses/groupinfo', params);

// 查询指定群组的协同岗列表
export const getGroupCoopUsers = (groupId, params) =>
  http.get(`${baseUrl}/groups/${groupId}/coop/list`, params);

// 对群组协同岗发起评分
export const submitGroupRating = (groupId, data) =>
  http.post(`${baseUrl}/groups/${groupId}/rating`, data);

// 查询我对指定群组协同岗的已评价列表
export const getGroupRatingList = (groupId, params) =>
  http.get(`${baseUrl}/groups/${groupId}/rating/list`, params);

// 查询当前用户对指定群组的评价状态
export const getGroupRatingStatus = (groupId, params) =>
  http.get(`${baseUrl}/groups/${groupId}/rating/status`, params);

// 任务通知模块列表
export const getNotificationModules = (params) =>
  http.get(`${baseUrl}/notification/modules`, params);

// 任务通知计数(含已读、未读)
export const getNotificationModuleCount = (moduleName, params) =>
  http.get(`${baseUrl}/notification/modules/${moduleName}/count`, params);

// 任务通知已读更新
export const readNotification = (params) =>
  http.post(`${baseUrl}/notification/read`, params);
