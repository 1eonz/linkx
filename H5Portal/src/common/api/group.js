import { http } from '@/common/network/http.js';
const baseUrl = '/proxy/icp/v1';
//创建或更新动态群组
export const upsert = (params) => http.post(`${baseUrl}/dynamic-group/upsert`, params);
//添加群组成员
export const addUser = (params) =>
  http.put(`${baseUrl}/dynamic-group/${params.groupId}/add-member`, params.members);
//删除群组成员
export const delUser = (params) =>
  http.put(`${baseUrl}/dynamic-group/${params.groupId}/del-members`, params);
//获取群组信息
export const getGroupInfo = (groupId) => http.get(`${baseUrl}/dynamic-group/${groupId}/info`, {});
//获取动态群组列表
export const getDymicGroupList = (params) => http.get(`${baseUrl}/dynamic-group/list`, params);
//开启或关闭对话按钮
export const setTalk = (params) =>
  http.put(`${baseUrl}/dynamic-group/${params.groupId}/button/${params.status}`, {});
//获取当前群组对话按钮状态
export const getTalkStatus = (groupId) => http.get(`${baseUrl}/dynamic-group/${groupId}/button`, {});

// 群组心跳
export const groupHeartbeat = (params) =>
  http.post(`${baseUrl}/dynamic-group/${params.groupId}/heartbeat/${params.userId}`, {});
// 创建位置共享
export const createLocationShare = (params) => http.post(`/third/v1/locationshare`, params);
// 获取位置共享
export const getLocationShare = (shareId) => http.get(`/third/v1/locationshare/my`, { shareId });
// 退出位置共享
export const exitLocationShare = (shareId, params) => http.post(`/third/v1/locationshare/${shareId}/exit`, params);
// 退出位置共享（合并接口：群主设置按钮状态 + 退出位置共享 + 删除ICS群组成员）
export const exitLocationShareCombined = (params) =>
  http.post(`${baseUrl}/dynamic-group/exit-location-share`, params);
