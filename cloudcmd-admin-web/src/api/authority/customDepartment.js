import request from '@/utils/request'
const baseUrl = '/auth/v1'                           
// ============================================================
// 组织管理接口（一级）
// ============================================================

/**
 * 查询组织列表
 * @returns {Promise}
 */
export function getOrganizationTree() {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/tree`,
    url: `${baseUrl}/custom-department/tree`,
    method: 'get'
  })
}

/**
 * 创建组织
 * @param {Object} data - { name: string }
 * @returns {Promise}
 */
export function createOrganization(data) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/tree`,
    url: `${baseUrl}/custom-department/tree`,
    method: 'post',
    data
  })
}

/**
 * 更新组织
 * @param {number|string} id - 组织ID
 * @param {Object} data - { name: string }
 * @returns {Promise}
 */
export function updateOrganization(id, data) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/tree/${id}`,
    url: `${baseUrl}/custom-department/tree/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除组织
 * @param {number|string} id - 组织ID
 * @returns {Promise}
 */
export function deleteOrganization(id) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/tree/${id}`,
    url: `${baseUrl}/custom-department/tree/${id}`,
    method: 'delete'
  })
}

// ============================================================
// 自定义组织部门接口（二级及以下）
// ============================================================

/**
 * 创建自定义组织部门
 * @param {Object} data - { code: string, name: string, parentId: number, type: 1|2, departmentCustomId: number|string }
 * @returns {Promise}
 */
export function createCustomDepartment(data) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/node`,
    url: `${baseUrl}/custom-department/node`,
    method: 'post',
    data
  })
}

/**
 * 更新自定义组织部门
 * @param {number|string} id - 部门ID
 * @param {Object} data - { code: string, name: string, parentId: number, type: 1|2, departmentCustomId: number|string }
 * @returns {Promise}
 */
export function updateCustomDepartment(id, data) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/node/${id}`,
    url: `${baseUrl}/custom-department/node/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除自定义组织部门
 * @param {number|string} id - 部门ID
 * @returns {Promise}
 */
export function deleteCustomDepartment(id) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/node/${id}`,
    url: `${baseUrl}/custom-department/node/${id}`,
    method: 'delete'
  })
}

/**
 * 查询子部门/子单位
 * @param {Object} params - { parentId: number|string, departmentCustomId: number|string }
 * @returns {Promise}
 */
export function getCustomDepartmentChildren(params) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/node`,
    url: `${baseUrl}/custom-department/node`,
    method: 'get',
    params
  })
}

/**
 * 查询节点绑定警员
 * @param {number|string} customDeptId - 部门ID
 * @param {Object} params - { keywords: string, page: number, pageSize: number }
 * @returns {Promise}
 */
export function getCustomDepartmentUserPage(customDeptId, params) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/node/${customDeptId}/user/page`,
    url: `${baseUrl}/custom-department/node/${customDeptId}/user/page`,
    method: 'get',
    params
  })
}

/**
 * 绑定多个警员
 * @param {number|string} id - 部门ID
 * @param {Object} data - { userIds: array }
 * @returns {Promise}
 */
export function bindCustomDepartmentUsers(id, data) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/node/${id}/user`,
    url: `${baseUrl}/custom-department/node/${id}/user`,
    method: 'post',
    data
  })
}

/**
 * 删除警员绑定
 * @param {number|string} id - 部门ID
 * @param {Object} params - { userIds: string }
 * @returns {Promise}
 */
export function unbindCustomDepartmentUsers(id, params) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/node/${id}/user`,
    url: `${baseUrl}/custom-department/node/${id}/user`,
    method: 'delete',
    params
  })
}

/**
 * 查询可绑定警员
 * @param {Object} params - { pageNum: number, pageSize: number, keyword: string }
 * @returns {Promise}
 */
export function getAvailableUsers(params) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/available-user/page`,
    url: `${baseUrl}/custom-department/available-user/page`,
    method: 'get',
    params
  })
}

/**
 * 获取节点详情（检查是否被使用）
 * @param {number|string} id - 部门ID
 * @returns {Promise} - 返回 { used: boolean, ... }
 */
export function getCustomDepartmentDetail(id) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/node/${id}`,
    url: `${baseUrl}/custom-department/node/${id}`,
    method: 'get'
  })
}

// ============================================================
// 一键调度相关接口
// ============================================================

/**
 * 获取组织树（一键调度）
 * @param {Object} params - { dutyType: number, dutyStartDate: string, dutyEndDate: string, imUserId: string }
 * @returns {Promise}
 */
export function getCustomDepartmentTree(params) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/tree`,
    url: `${baseUrl}/custom-department/tree`,
    method: 'get',
    params
  })
}

/**
 * 获取子部门（一键调度）
 * @param {Object} params - { dutyType: number, dutyStartDate: string, dutyEndDate: string, parentId: number|string, departmentCustomId: number|string, imUserId: string }
 * @returns {Promise}
 */
export function getCustomDepartmentNode(params) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/node`,
    url: `${baseUrl}/custom-department/node`,
    method: 'get',
    params
  })
}

/**
 * 获取人员信息（一键调度）
 * @param {number|string} id - 部门ID
 * @param {Object} params - { dutyType: number, dutyStartDate: string, dutyEndDate: string, imUserId: string }
 * @returns {Promise}
 */
export function getDutyScheduleUser(id, params) {
  return request({
    // url: `http://172.24.65.16:8081/auth/custom-department/node/${id}/duty-schedule-user`,
    url: `${baseUrl}/custom-department/node/${id}/duty-schedule-user`,
    method: 'get',
    params
  })
}

// ============================================================
// 管理员绑定警员相关接口
// ============================================================

/**
 * 查询管理员绑定的警员
 * @returns {Promise}
 */
export function getBindUser() {
  return request({
    // url: `http://172.24.65.16:8081/auth/user/adminuser/bind/im-user`,
    url: `${baseUrl}/user/adminuser/bind/im-user`,
    method: 'get'
  })
}

/**
 * 绑定警员
 * @param {Object} data - { imUserId: number|string }
 * @returns {Promise}
 */
export function bindUser(data) {
  return request({
    // url: `http://172.24.65.16:8081/auth/user/adminuser/bind/im-user`,
    url: `${baseUrl}/user/adminuser/bind/im-user`,
    method: 'put',
    data
  })
}

/**
 * 解绑警员
 * @returns {Promise}
 */
export function unbindUser(data) {
  return request({
    // url: `http://172.24.65.16:8081/auth/user/adminuser/im-user`,
    url: `${baseUrl}/user/adminuser/im-user`,
    method: 'delete',
    data
  })
}
