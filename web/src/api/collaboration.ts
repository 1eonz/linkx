// src/api/collaboration.ts
import http from '@/utils/http';
const baseUrl = '/linkx/desktop/collaboration/v1';
// ─────────────────────────────────────────────────────────────
// API 导出
// ─────────────────────────────────────────────────────────────

/** 查询指定协同岗层级的子层级 */
export const getCoopLevelChildren = (levelId: number | string) => {
  return http.get<any>(`${baseUrl}/cooplevels/${levelId}/children`);
};

/** 获取指定协同岗层级下的成员列表（分页） */
export const getCoopLevelMembers = (
  levelId: number | string,
  params: { pageNum: number; pageSize: number },
) => {
  return http.get<any>(`${baseUrl}/cooplevels/${levelId}/member`, { params });    
};

/**
 * 根据 userId + 可选 departmentId 获取组织树（一级）
 * departmentId（根节点不传点击子部门传对应id） pageNum， pageSize
 */
export const getUserOrgTree = (
  userId: number | string,
  params?: { departmentId?: string | number; pageNo: number; pageSize: number },
) => {
  return http.get<any>(`${baseUrl}/im/users/${userId}/tree`, { params });
};



/** 获取全量协同岗位（分页，降级模式） */
// pageNum， pageSize， postName 搜索协同岗位名称
export const getAllCoopDutys = (params: {
  type?: number;
  pageNum: number;
  pageSize: number;
  postName?: string;
}) => {
  return http.get<any>(`${baseUrl}/post/page`, { params });
};

/**
 * 全量用户分页搜索
 * POST /linkx/desktop/collaboration/im/users/page
 */
export const getAllUsers = (params: {
  keywords?: string;
  pageNo?: number;
  pageSize?: number;
  name?: string;
  departmentId?: string | number;
}) => {
  return http.get<any>(`${baseUrl}/im/users/page`, { params });
};
/**
 * 获取用户下的所有部门（分页）
 */
export const getGroupList = (
  userId: number | string,
  params: { pageNo: number; pageSize: number; departmentId?: string | number },
) => {
  return http.get<any>(`${baseUrl}/im/users/${userId}/departments`, { params });
};

/** 分页获取指定用户的好友/关注列表 */
export const getUsersPageOfType = (
  userId: number | string,
  params: { pageNo: number; pageSize: number; keywords?: string; userType?: number },
) => {
  return http.get<any>(`${baseUrl}/im/users/${userId}/page`, { params });
};

// 获取默认协同岗
export const pageDefaultCoop = (params) =>
  http.get(`${baseUrl}/functionaldepts/default/coop/page`, { params });

// 获取职能分类树
export const getFunctionaldeptsChildren = (levelId) =>
  http.get(`${baseUrl}/functionaldepts/${levelId}/children`);

// 获取指定职能分类下的协同岗用户列表
export const getFunctionaldeptsMembers = (levelId, params) =>
  http.get(`${baseUrl}/functionaldepts/${levelId}/coop`, { params });
// 职能建群
export const coopCreateGroup = (data) =>
  http.post(`${baseUrl}/im/functionaldepts/group/create`, data);

// 获取部门组织
export const queryDepartment = (data: any, isQueryAll = false) => {
  let url = `${baseUrl}/post/queryDepartment`;
  if (!isQueryAll) {
    url = `${url}?parentCode=${data.parentCode}`;
  }
  return http.get(url, data);
};

/* 一键调度接口 */
// 获取值班人员
export const onDutyPersonal = (params: any) =>
  http.get(`${baseUrl}/duty/schedule/onDutyPersonal`, { params });

// 创建群组
export const createGroup = (data: any) =>
  http.post(`${baseUrl}/im/group/create`, data);

// 获取标签 {scope：0：所有标签 1：一键建群 2： 职能建群}`
export const getTags = (params) =>  
  http.get<any>(`${baseUrl}/label/all`, { params });

// 根据标签查询关联人员 {id: 标签id}
export const getTagUsers = (params: { id: string }) =>  
  http.get<any>(`${baseUrl}/label/binding/user`, {params});

// 根据用户ID查询协同岗信息
export const queryCoopPostByUserId = (params?: { userId?: string | number }) =>
  http.get<any>(`${baseUrl}/post/queryByUserId`, { params });

/** 根据用户ID查询用户详情 */
export const getUserDetailById = (userId: number | string) => {
  return http.get<any>(`${baseUrl}/im/users/${userId}`);
};

/** 查询服务器节点列表 */
export const getServers = (params: { pageNum: number; pageSize: number }) => {
  return http.get<any>(`/linkx/desktop/node/v1/p2p/clients`, { params });
};

/** 查询协同岗列表（本节点传 peerId: ''，其他节点传对应 peerId） */
export const getCoopUsersPage = (params: {
  peerId?: string;
  pageNum?: number;
  pageSize?: number;
  coopUserName?: string;
}) => {
  return http.get<any>(`${baseUrl}/coopusers/group-candidates`, { params });
};
