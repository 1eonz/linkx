import { http } from '@/common/network/http.js';
const authUrl = '/auth/v1/';
const collaborationUrl = '/collaboration/v1';
const adminUrl = '/admin/v1/';
const thirdUrl = '/third/v1/';
// 获取全部应用
export const getAppPage = (params) => http.get(adminUrl + 'content/app/info/apps', params);
// 获取应用详情 {id}
export const getPrerequisiteInfo = (params) => http.get(adminUrl + 'content/app/info/get', params);

//获取本地应用修改状态
export const getInitStatus = (params) =>
  http.get(adminUrl + 'content/app/common/getInitStatus', params);

// 获取全部轮播图
export const getBannerPage = (params) => http.get(adminUrl + 'content/carousel/page', params);

// 根据键名获取参数配置
export const getThirdUrl = (params) => http.get(adminUrl + 'infra/config/get-value-by-key', params);

// 获取全局配置
export const getGlobalsList = () => http.post( '/base/v1/globals/getGlobalsList');

// 获取当前用户常用应用
export const getCommonApp = (params) => http.get(adminUrl + 'content/app/common/get_my', params);

// 保存用户常用应用
export const saveCommonApp = (data) => http.post(adminUrl + 'content/app/common/save', data);

// 标签列表
export const collaborationLabelList = (scope) =>
  http.get(collaborationUrl + '/label/list' + (scope ? `?scope=${scope}` : ''));

// 一键建群
export const labelCreateGroup = (data) =>
  http.post(collaborationUrl + '/label/createGroup', data);

//消息新建任务
export const taskSave = (data) => http.post(collaborationUrl + '/tasks/save', data);

// 通过消息id查找对应的协同任务
export const getTaskByMsgId = (params) =>
  http.get(collaborationUrl + '/tasks/detailBySeqId', params);

//引用回复消息
export const responsesTask = (data) =>
  http.post(collaborationUrl + '/tasks/response/responses', data);

//非引用回复消息修改所有状态
export const responsesTaskAll = (data) =>
  http.post(collaborationUrl + '/tasks/response/reply/all', data);

// h5权限管理
export const h5permissions = (data) => http.get(adminUrl + 'role/h5permissions', data);

// h5登录
export const h5login = (data) => http.post(authUrl + 'oauth/v2/h5login', data);
export const tokenLogin = (data) => http.post(authUrl + 'oauth/v2/tokenLogin', data);

// 通过身份证查询用户
export const queryUserByIdCard = (params) =>
  http.get(collaborationUrl + '/post/queryUserByIdCard', params);

//获取license权限
export const getLicensePermissions = (params) => http.get(adminUrl + 'msip/license/info', params);

// 获取系统配置
export const getSystemConfig = (params) => http.get(adminUrl + `system/config`, params);

// 获取首页布局配置
export const getLayoutSections = (params) =>
  http.get(adminUrl+`layout/app/sections`, params);

// ==================== 应用分组管理 ====================

/**
 * 创建三方应用分组
 * @param {Object} data - { tenantId, name, type, sort, createUser }
 */
export const createAppGroup = (data) => http.post(`${thirdUrl}apps/groups`, data);

/**
 * 删除应用分组
 * @param {Long} id - 分组ID
 */
export const deleteAppGroup = (id) =>
  http.request(`${thirdUrl}apps/groups/${id}`, undefined, 'delete');

/**
 * 更新分组
 * @param {Array} data - [{ groupId, sort, name }]
 */
export const updateAppGroup = (data) => http.put(`${thirdUrl}apps/groups`, data);

/**
 * 获取应用分组列表
 * @param {Object} params - { type, userId }
 */
export const getAppGroupList = (params) => http.get(`${thirdUrl}apps/groups`, params);

/**
 * 绑定应用到分组
 * @param {Long} groupsId - 分组id
 * @param {Object} data - { type, appIds, userId }
 */
export const bindAppToGroups = (groupsId, data) =>
  http.put(`${thirdUrl}apps/${groupsId}/groups`, data);

/**
 * 删除分组中的应用
 * @param {Long} groupsId - 分组id
 * @param {Object} data - { type, groupsId, userId }
 */
export const removeAppFromGroups = (groupsId, data) =>
  http.request(`${thirdUrl}apps/${groupsId}/groups`, data, 'delete');

/**
 * 创建应用使用记录
 * @param {Long} appId - 应用id
 * @param {Object} data - { appId, userId, client, time }
 */
export const createAppUsedRecord = (appId, data) =>
  http.post(`${thirdUrl}apps/${appId}/used`, data);

/**
 * 获取应用使用记录排行
 * @param {Object} params - { userId, terminalType }
 */
export const getAppUsedRanking = (params) => http.get(`${thirdUrl}apps/used`, params);
/**
 * 获取应用展示方式
 * @param {Object} data - { type, userId }
 */
export const setSortType = (data) => http.post(`${collaborationUrl}/apps/sortType`, data);
/**
 * 获取应用展示方式
 * @param {Object} data - {userId }
 */
export const getSortType = (params) =>
  http.get(`${collaborationUrl}/apps/sortType`, params);

/**
 * 获取信息通知列表
 * @param {Object} params
 */
export const fetchNotifyListApi = (params) => http.get(`${thirdUrl}push-message`, params);

/**
 * 创建信息通知记录
 * @param {Object}
 */
export const createNotifyMessage = (data) => http.post(`${thirdUrl}push-message`, data);
