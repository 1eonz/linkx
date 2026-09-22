import { http } from '@/common/network/http.js';

// 获取节点列表
// 接口返回 { departments: [...], nodes: [...] }，直接返回 nodes（按接口字段定义，不做前端映射）
// 节点字段：userId / name / departmentId / departmentPeerNode / departmentPeerNodeGateWayPrefix / departmentPeerNodeIP / version
export const getNodeList = async (userId) => {
  const res = await http.get(`/collaboration/v1/users/${userId}/profile`);
  console.log('getNodeList', res);
  return res?.nodes || [];
};
