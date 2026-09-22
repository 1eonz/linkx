// src/api/h5/coopLevel.js
import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
/**
 * 创建协同岗层级节点
 * @param {{ parentId?: number, name: string }} data
 * @returns {Promise} data 为新建节点ID
 */
export const createCoopLevel = (data) =>
  request({ url: `${baseUrl}/cooplevels`, method: 'post', data })

/**
 * 修改协同岗层级属性
 * @param {string} coopLevelId - 待修改的层级节点ID
 * @param {{ parentId?: number, name: string }} data
 * @returns {Promise}
 */
export const updateCoopLevel = (coopLevelId, data) =>
  request({ url: `${baseUrl}/cooplevels/${coopLevelId}`, method: 'put', data })

/**
 *  删除协同岗层级节点
 * @param {string} coopLevelId - 待删除的层级节点ID
 * @returns {Promise}
 */
export const deleteCoopLevel = (coopLevelId) =>
  request({ url: `${baseUrl}/cooplevels/${coopLevelId}`,  method: 'delete' })

/**
 * 查询指定协同岗层级的子层级
 * @param {number} levelId - 待查询的层级ID
 * @returns {Promise}
 */
export const getCoopLevelChildren = (levelId) =>
  request({ url: `${baseUrl}/cooplevels/${levelId}/children`, method: 'get' })

/**
 * 获取指定协同岗层级下的协同岗用户列表
 * @param {number} levelId - 待查询的层级ID
 * @returns {Promise}
 */
export const getCoopLevelMembers = (levelId, params) =>
  request({ url: `${baseUrl}/cooplevels/${levelId}/member`, method: 'get', params })

/**
 * 获取指定协同岗层级下的协同岗用户列表（可搜索）
 * @param {number} levelId - 待查询的层级ID
 * @param {number} pageNum - 页码
 * @param {number} pageSize - 每页条
 * @param {number} orgId - 组织id
 * @param {number} name - 协同岗名称
 * @param {string} startTime - 开始时间
 * @param {string} endTime - 结束时间
 * @returns {Promise}
 */
export const getCoopLevelMembersBySearch = (params) =>
  request({ url: `${baseUrl}/cooplevels/member`, method: 'get', params })

/**
 * 更新指定协同岗层级下的协同岗用户列表
 * @param {number} levelId - 待更新的层级ID
 * @param {{ coopUserIds: string }} data - 协同岗用户ID列表（警信ID）
 * @returns {Promise}
 */
export const updateCoopLevelMembers = (levelId, data) =>
  request({ url: `${baseUrl}/cooplevels/${levelId}/member`, method: 'put', data })

/**
 * 删除协同岗层级下的用户
 * @param {number[]} ids - 协同岗用户id列表
 * @returns {Promise}
 */
export const deleteCoopLevelMembers = (ids) =>
  request({ url: `${baseUrl}/cooplevels/member`,  method: 'delete', data: ids })
