import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
// 查询协同岗列表
export function getCollaborationPage(params) {
  return request({
    url: `${baseUrl}/post/page`,
    method: 'get',
    params
  })
}

// 使用身份证查询当前用户所属组织
export function queryUserByIdCard(params) {
  return request({
    url: `${baseUrl}/post/queryUserByIdCard`,
    method: 'get',
    params
  })
}

// 新增协同岗
export function createCollaboration(data) {
  return request({
    url: `${baseUrl}/post/save`,  
    method: 'post',
    data
  })
}

// 修改协同岗
export function updateCollaboration(data) {
  return request({
    url: `${baseUrl}/post/update`,
    method: 'post',
    data
  })
}
// 删除协同岗
export function deleteCollaboration(id) {
  return request({
    url: `${baseUrl}/post/delete/${id}`,
    method: 'delete'
  })
}

// 批量删除协同岗
export function delBatchCollaboration(data) {
  return request({
    url: `${baseUrl}/post/deleteBatch`, 
    method: 'delete',
    data
  })
}

// 查询协同岗部门
export function queryDepartment(params = {}) {
  return request({
    url: `${baseUrl}/post/queryDepartment`,
    method: 'get',
    params
  })
}

// 查询协同岗部门-一次性加载
export function queryDepartmentTree(params = {}) {
  return request({
    url: `${baseUrl}/organization/tree`,
    method: 'get',
    params
  })
}

// 根据用户id查询用户权限
export function queryRoleAuthByUserId(params = {}) {
  return request({
    url: '/auth/v1/role/byUserId',
    method: 'get',
    params
  })
}

// 查询协同岗人员
export function queryUser(params) {
  return request({
    url: `${baseUrl}/post/queryUser`,
    method: 'get',
    params
  })
}

// 分页查询协同岗人员
export function queryUserByPage(params) {
  return request({
    url: `${baseUrl}/post/queryUserByPage`,
    method: 'get',
    params
  })
}

// 查询上下岗记录
export function getAttendancePage(params) {
  return request({
    url: `${baseUrl}/attendance/page`,    
    method: 'get',
    params
  })
}

// 导出上下岗记录
export function exportAttendance(params) {
  return request({
    url: `${baseUrl}/attendance/export`,
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// 单张上传图片
export function uploadColTmp(file) {
  return request({
    url: `${baseUrl}/post/upload/icon`,
    method: 'post',
    data: file,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 单张上传图片
export function downloadColIcon(params) {
  return request({
    url: `${baseUrl}/post/downloadIcon`,
    method: 'get',
    params
  })
}

// 查询协同岗是否重名
export function querySameName(params) {
  return request({
    url: `${baseUrl}/post/queryByName`,
    method: 'get',
    params
  })
}

// 同步老的协同岗数据
export function syncPostFromIm(params) {
  return request({
    url: `${baseUrl}/post/syncPostFromIm`,
    method: 'get',
    params
  })
}

// 获取老的协同岗数据是否已同步
export function getImSyncStatus(params) {
  return request({    
    url: `${baseUrl}/post/getImSyncStatus`,
    method: 'get',
    params
  })
}

// 查询协同岗编辑列表
export function getCollaborationEditPage(params) {
  return request({    
    url: `${baseUrl}/post/log/page`,
    method: 'get',
    params
  })
}

// 导出协同岗编辑列表
export function exportCollaborationEditPage(params) {
  return request({
    url: `${baseUrl}/post/log/export`,
    method: 'get',
    params,
    responseType: 'blob'
  })
}

// 导出协同岗编辑列表
export function getProcess(params = {}) {
  return request({
    url: `${baseUrl}/post/getProcess`,
    method: 'get',
    params
  })
}

// 根据用户id查询用户部门树
export function getDeptTreeByUserId(params) {
  return request({
    url: `${baseUrl}/im/users/tree`,
    method: 'get',
    params
  })
}

// 查询协同岗在岗人员列表
export function getOnDutyUsersByPostId(postId) {
  return request({
    url: `${baseUrl}/attendance/getOnline`,
    method: 'get',
    params: { postId }
  })
}

// 人员下岗
export function offDutyUser(data) {
  return request({
    url: `${baseUrl}/attendance/admin/offline`,
    method: 'post',
    data: {
      userId: data.userId,
      userName: data.userName,
      switchType: 3,
      postId: data.postId,
      postName: data.postName,
    }
  })
}


// 判断是否为最后一个在岗人员
export function getLastNum(userId) {
  return request({
    url: `${baseUrl}/attendance/getLastNum`,
    method: 'get',
    params: { userId }
  })
}

// ========== 组织数据权限接口（/auth/v1/orgUser）==========

/**
 * 构建当前管理员组织权限树
 * @returns {Promise} res.data 为 RoleDataPriv 树节点数组
 */
export function getOrgPrivTree(params) {
  return request({
    url: '/auth/v1/orgUser/orgPrivTree',
    method: 'get',
    params
  })
}

/**
 * 权限范围内分页搜索人员
 * @param {Object} params - { pageNum, pageSize, orgId?, isChildren, name?, type? }
 * @returns {Promise} res.data 为 PageResult<ImUserVO>
 */
export function searchUserByPage(params) {
  return request({
    url: '/auth/v1/orgUser/user/page',
    method: 'get',
    params
  })
}
// 分享协同岗
export function shareCollaboration(id, data) {
  return request({
    url: `${baseUrl}/coopusers/${id}/share`,
    method: 'post',
    data
  })
}
// 取消分享协同岗
export function unshareCollaboration(id, data) {
  return request({
    url: `${baseUrl}/coopusers/${id}/unshare`,
    method: 'post',
    data
  })
}
// 查询分享协同岗列表
/**
 * @description: 查询分享协同岗列表
 * @param {*} params.type 0:全部协同岗 1：已分享协同岗 2：接受的协同岗
 * @return {*}
 */
export function getCoopUsersPage(params) {
  return request({
    url: `${baseUrl}/coopusers`,
    method: 'get',
    params
  })
}

/**
 * 权限范围内搜索人员（不分页）
 * @param {Object} params - { orgId, isChildren, name?, type? }
 * @returns {Promise} res.data 为 List<ImUserVO>
 */
export function searchUserList(params) {
  return request({
    url: '/auth/v1/orgUser/user/list',
    method: 'get',
    params
  })
}
// 查询指定协同岗已分享节点信息
export function getSharedNodes(coopUserId) {
  return request({
    url: `${baseUrl}/coopusers/${coopUserId}/shared-nodes`,
    method: 'get',
  })
}
// 查询指定协同岗已分享节点信息
export function getVersion() {
  return request({
    url: `base/v1/version`,
    method: 'get',
  })
}