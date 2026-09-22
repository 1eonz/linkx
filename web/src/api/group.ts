import http from '@/utils/http';

// 查询群组(静态、动态)
export const queryGroups = (data) => http.post<any>('/resource/groupUser/queryGroups', data);

// 派接组相关
export const queryPatchGroups = (data) =>
  http.post<any>('/resource/patchGroup/queryPatchGroups', data);

// 新增派接组
export const createPatchGroup = (data) =>
  http.post<any>('/resource/patchGroup/createPatchGroup', data);

// 编辑派接组成员
export const createPatchGroupUser = (data) =>
  http.post<any>('/resource/patchGroup/createPatchGroupUser', data);

// 删除派接组
export const deletePatchGroups = (params) =>
  http.delete<any>('/resource/patchGroup/delete', { params });

export const queryGroupUserByGroupId = (data) =>
  http.post<any>('/resource/groupUser/queryGroupUserByGroupId', data);

export const subscribeTalkingGroup = (data) =>
  http.post<any>('/resource/subscribeTalkingGroup/save', data);

export const findSubscribeTalkingGroup = (data) =>
  http.post<any>('/resource/subscribeTalkingGroup/list', data);

export const findAllSubscribeTalkingGroup = (data) =>
  http.post<any>('/resource/subscribeTalkingGroup/page-list', data);

// 首页看板-新增活跃群组
export const createActiveGroup = (params) =>
  http.post<any>('/resource/active/group/createActiveGroup', params);

// 首页看板-查询活跃群组
export const queryActiveGroup = (params) =>
  http.post<any>('/resource/active/group/queryActiveGroup', params);

// 首页看板-修改活跃群组
export const updateActiveGroup = (params) =>
  http.post<any>('/resource/active/group/updateActiveGroup', params);

// 首页看板-删除活跃群组
export const deleteActiveGroup = (params) =>
  http.post<any>('/resource/active/group/deleteActiveGroup', params);

export const queryPersonGroup = () => http.post<any>('/resource/personGroup/queryPersonGroup');
