import iccHttp from '@/utils/http';
import http, { getConfig } from '@/utils/pimHttp';
const collaborationBaseUrl = '/linkx/desktop/collaboration/v1';
// 用户登录
export const userLogin = (data) => http.post('/cm/auth/v2/user/login', data);

// 用户登出
export const userLogout = () => http.post('/cm/auth/v2/user/logout');

// 刷新用户token
export const userRefreshToken = (data, opt) => {
  const config = getConfig();
  config.headers = { ...config.headers, ...opt };
  return http.post('/cm/auth/v2/user/refreshtoken', data, config);
};

// 获取二维码唯一标识
export const getQRcode = () => http.get('/cm/auth/v2/connect/qrcode');

// 获取二维码的状态
export const getQRcodeStatus = (params) =>
  http.get('/cm/auth/v2/connect/qrcode/status', { params });

// 更新二维码状态
export const updataQRcodeStatus = (params) =>
  http.put('/cm/auth/v2/connect/qrcode/status', { params });

// 通讯录树形展示
export const addressbookTree = (params) =>
  http.get('/cm/addressbook/v2/addressbook/tree', { params });

// 通讯录搜索（分页）
export const addressbookTreeQuery = (params) =>
  http.get('/cm/addressbook/v2/addressbook/query', { params });

// 批量获取指定人员列表的人员详情
export const addressbookBatch = (data) =>
  http.post('/cm/addressbook/v2/addressbook/users/batch', data);

// 文件上传
export const uploadFile = (data) => {
  const config = getConfig();
  config.headers = { ...config.headers, 'Content-Type': 'multipart/form-data' };
  return http.post('/cm/filemanager/v2/file/upload', data, config);
};

// 文件下载
export const downloadFile = (fileId, thumbId, params) => {
  const config = getConfig();
  config.headers = { ...config.headers, Accept: '*/*' };
  return http.get(`/cm/filemanager/v2/file/download/${fileId}/${thumbId}`, {
    params,
    responseType: 'blob',
  });
};

// 文件删除
export const deleteFile = (data) => http.delete('/cm/filemanager/v2/file/delete', data);

//  图片合并
export const mergeImages = (data) => http.post('/cm/filemanager/v2/file/images/merge', data);

// 文件复制
export const copyFile = (data) => http.post('/cm/filemanager/v2/file/copy', data);

//  文件分片上传
export const uploadChunkFile = (data) => http.post('/cm/filemanager/v2/file/chunk/upload', data);

//  文件分片合并
export const mergeChunkFile = (data) => http.post('/cm/filemanager/v2/file/chunk/merge', data);

//  文件分片上传断点检查
export const checkChunkFile = (data) => http.post('/cm/filemanager/v2/file/chunk/check', data);

//  检查文件状态
export const checkStatusFile = (data) => http.post('/cm/filemanager/v2/file/status/check', data);

// 关注/去关注用户对象
export const concernedUsers = (data) => http.post('/cm/friend/v2/concerned/users', data);

// 发送短信/彩信/已读回执/撤回/合并转发/逐条转发
export const sendMessage = (data) => http.post('/api/im/v1/messages', data);

// 查询离线消息会话id集合
export const getSessionList = (data) => http.post('/api/im/v1/messages/offline/sessionlist', data);

// 创建群组
export const groupsCreate = (data) => http.post('/cm/group/v2/groups', data);

// 修改群组
export const groupsUpdate = (id, data) => http.put(`/cm/group/v2/groups/${id}`, data);

// 解散指定的群组
export const groupDelete = (id) => http.delete(`/cm/group/v2/groups/${id}`);

// 获取指定群组的详细信息（群组详情+成员列表）
export const groupDetails = (id, params) => http.get(`/cm/group/v2/groups/${id}`, { params });

// 全量和增量查询用户所在群组列表(群组详情+群公告)
export const groupQuery = (params) => http.get('/cm/group/v2/groups', { params });

// 获取群组成员列表
export const batchGroupsMembers = (data) => http.post('/cm/group/v2/groups/members/batch', data);

// 成员退出群组
export const exitGroupsMembers = (id) => http.delete(`/cm/group/v2/groups/${id}/members/quit`);

// 移除群组成员
export const deleteGroupsMembers = (id, data) =>
  http.delete(`/cm/group/v2/groups/${id}/members`, { data });

// 新增群组成员
export const joinGroupsMembers = (id, data) => http.post(`/cm/group/v2/groups/${id}/members`, data);

// 创建公告
export const createGroupsNotices = (data) => http.post('/cm/group/v2/groups/notices', data);

// 编辑群公告
export const updateGroupsNotices = (id, data) =>
  http.put(`/cm/group/v2/groups/notices/${id}`, data);

// 删除群公告
export const deleteGroupsNotices = (groupId, id) =>
  http.delete(`/cm/group/v2/groups/notices/${groupId}/${id}`);

// 获取单个群公告列表
export const getGroupsNotices = (groupId) => http.get(`/cm/group/v2/groups/${groupId}/notices`);

// 获取好友成员列表（全量接口--分页查询）
export const friendsPage = (params) => http.get('/cm/friend/v2/friends/page', { params });

// 查询离线消息
export const messagesOfflinePage = (data) => http.post('/api/im/v1/messages/offline/page', data);

// 查询指定某个人员的详情
export const addressbookUsersId = (id) => http.get(`/cm/addressbook/v2/addressbook/users/${id}`);

// 设置聊天会话配置
export const addressbookUsersChatprofile = (data) =>
  http.put('/cm/addressbook/v2/addressbook/users/chat_profile', data);

// 查询当前用户信息
export const addressbookUsersProfile = () =>
  http.get('/cm/addressbook/v2/addressbook/users/profile');

// 更新当前用户信息
export const modifyAddressbookUsersProfile = (data) =>
  http.put('/cm/addressbook/v2/addressbook/users/profile', data);

// 获取协同用户列表
export const getCooperationusers = (data) =>
  http.get('/cm/addressbook/v2/client/cooperationusers', data);

// 新增协同任务
export const tasksSave = (data) =>
  iccHttp.post<any>(`${collaborationBaseUrl}/tasks/save`, data);

// 获取协同任务列表
export const tasksList = (params) =>
  iccHttp.get<any>(`${collaborationBaseUrl}/tasks`, { params });  

// 获取协同任务反馈列表
export const tasksResponse = (params) =>
  iccHttp.get<any>(`${collaborationBaseUrl}/tasks/response`, { params });

// 获取统计列表
export const tasksStatistics = (params) =>
  iccHttp.get<any>(`${collaborationBaseUrl}/tasks/statistics`, { params });

// 获取警务协同处置提醒状态数
export const getTaskNum = (params) =>
  iccHttp.get<any>(`${collaborationBaseUrl}/tasks/taskNum`, { params });

// 修改列表中的群组名称
export const updateListGroupName = (params) =>
  iccHttp.put<any>(`${collaborationBaseUrl}/tasks/update/groupName`, { params });

// 修改协同岗状态开启或者关闭
export const updateCollaboration = (data) =>
  iccHttp.post<any>(
    `/linkx/desktop/autn/v1/oauth/user/update?collaboration=${data.collaboration}&userId=${data.userId}`,
  );
