// src/api/h5/coopLevel.js
import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
/**
 * 创建职能分类节点
 * @param {{ parentId?: number, name: string }} data
 * @returns {Promise} data 为新建节点ID
 */
export const createFunctionaldepts = data =>
  request({ url: `${baseUrl}/functionaldepts`, method: 'post', data })

/**
 * 修改职能分类属性
 * @param {string} coopLevelId - 待修改的层级节点ID
 * @param {{ parentId?: number, name: string }} data
 * @returns {Promise}
 */
export const updateFunctionaldepts = (coopLevelId, data) =>
  request({
    url: `${baseUrl}/functionaldepts/${coopLevelId}`,
    method: 'put',
    data
  })

/**
 *  删除职能分类节点
 * @param {string} coopLevelId - 待删除的层级节点ID
 * @returns {Promise}
 */
export const deleteFunctionaldepts = coopLevelId =>
  request({
    url: `${baseUrl}/functionaldepts/${coopLevelId}`,
    method: 'delete'
  })

/**
 * 查询指定职能分类的子层级
 * @param {number} levelId - 待查询的层级ID
 * @returns {Promise}
 */
export const getFunctionaldeptsChildren = levelId =>
  request({
    url: `${baseUrl}/functionaldepts/${levelId}/children`,
    method: 'get'
  })

/**
 * 获取指定职能分类下的协同岗用户列表
 * @param {number} levelId - 待查询的层级ID
 * @returns {Promise}
 */
export const getFunctionaldeptsMembers = (levelId, params) =>
  request({
    url: `${baseUrl}/functionaldepts/${levelId}/coop`,
    method: 'get',
    params
  })

/**
 * 更新指定职能分类下的协同岗用户列表
 * @param {number} levelId - 待更新的层级ID
 * @param {{ coopUserIds: string }} data - 协同岗用户ID列表（警信ID）
 * @returns {Promise}
 */
export const updateFunctionaldeptsMembers = (levelId, data) =>
  request({
    url: `${baseUrl}/functionaldepts/${levelId}/coop`,
    method: 'put',
    data
  })

/**
 * 删除职能分类下的用户
 * @param {object[]}  - 协同岗用户列表
 * @returns {Promise}
 */
export const deleteFunctionaldeptsMembers = data =>
  request({
    url: `${baseUrl}/functionaldepts/coop`,
    method: 'delete',
    data
  })
/**
 * 修改协同岗是否选中
 * @param {id[]} id - 协同岗用户id列表
 * @param {boolean} checked - 是否选中
 * @param {string} updater - 修改人
 * @returns {Promise}
 */
export const checkedFunctionaldeptsMembers = data =>
  request({
    url: `${baseUrl}/functionaldepts/checked/coop`,
    method: 'put',
    data
  })
/**
 * 新增默认协同岗
 * @param {{  name: string }} data
 * @returns {Promise}
 */
export const creatDefaultCoop = data =>
  request({
    url: `${baseUrl}/functionaldepts/default/coop/batch`,
    method: 'post',
    data
  })

/**
 *  获取协同岗分页列表
 * @param {string} id - 待删除的层级节点ID
 * @returns {Promise}
 */
export const pageDefaultCoop = params =>
  request({
    url: `${baseUrl}/functionaldepts/default/coop/page`,
    method: 'get',
    params
  })
/**
 *  删除默认协同岗
 * @param {string} id - 待删除的层级节点ID
 * @returns {Promise}
 */
export const deleteDefaultCoop = id =>
  request({
    url: `${baseUrl}/functionaldepts/default/coop/${id}`,
    method: 'delete'
  })
