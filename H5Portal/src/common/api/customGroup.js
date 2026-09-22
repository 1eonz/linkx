import { http } from '@/common/network/http.js';
import axios from 'axios';

const baseUrl = '/collaboration/v1';
const nodeBaseUrl = '/node/v1';
//获取用户列表
export const usersPage = (params) => http.get(baseUrl + '/im/users/page', params);

//查询协同岗
export const collaborationPage = (params) =>
  http.get(baseUrl + '/post/pageList', params);

// 查询服务器节点列表
export const getServers = (params) =>
  http.get(nodeBaseUrl + '/p2p/clients', params);

// 查询分享协同岗列表 type: 0全部 1已分享 2接受的
export const getCoopUsersPage = (params) =>
  http.get(baseUrl + '/coopusers/group-candidates', params);

//分页获取指定用户的好友/关注列表
export const usersPageOfType = (userId, params) =>
  http.get(baseUrl + `/im/users/${userId}/page`, params);

// 自定义建群
export const groupCreate = (data) => http.post(`${baseUrl}/im/group/create`, data);

//获取用户树列表
export const usersTree = (userId, params) =>
  http.get(baseUrl + `/im/users/${userId}/departments`, params);

//查询协同岗层级
export const cooplevelsChildren = (levelId) =>
  http.get(baseUrl + `/cooplevels/${levelId}/children`);
// 根据层级查询协同岗列表
export const cooplevelsMember = (levelId, params) =>
  http.get(baseUrl + `/cooplevels/${levelId}/member`, params);

/* ------------职能建群接口 ------------*/

// 获取默认协同岗
export const pageDefaultCoop = (params) =>
  http.get(baseUrl + `/functionaldepts/default/coop/page`, params);

// 获取职能分类树
export const getFunctionaldeptsChildren = (levelId) =>
  http.get(baseUrl + `/functionaldepts/${levelId}/children`);

// 获取指定职能分类下的协同岗用户列表
export const getFunctionaldeptsMembers = (levelId, params) =>
  http.get(baseUrl + `/functionaldepts/${levelId}/coop`, params);
// 职能建群
export const coopCreateGroup = (data) =>
  http.post(`${baseUrl}/im/functionaldepts/group/create`, data);

/* ------------值班调度接口 ------------*/

// 根据组织获取值班人员
export const onDutyPersonal = (params) =>
  http.get(baseUrl + `/duty/schedule/onDutyPersonal`, params);

// 获取标签 {scope：0：所有标签 1：一键建群 2： 职能建群} 
export const getTags = (params) =>  
  http.get(`${baseUrl}/label/all`, params);

// 根据标签查询关联人员 {id: 标签id}
export const getTagUsers = (params) =>  
  http.get(`${baseUrl}/label/binding/user`, params);
