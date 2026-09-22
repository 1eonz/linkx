import request from '@/utils/request'

/**
 * 查询客户端节点列表（分页）
 * @param {Object} params - { pageNum?: number, pageSize?: number, keyword?: string }
 * @returns {Promise}
 */
export function getClients(params) {
  return request({
    url: '/node/v1/p2p/clients',
    method: 'get',
    params
  })
}

/**
 * 更新客户端节点信息（授权等）
 * @param {string} peerId - 客户端节点标识
 * @param {Object} data - { remark?: string, grant?: number, expiredIn?: number, tag?: string }
 * @returns {Promise}
 */
export function updateClient(peerId, data) {
  return request({
    url: `/node/v1/p2p/clients/${peerId}`,
    method: 'post',
    data
  })
}

/**
 * 删除客户端节点
 * @param {number|string} id - 客户端节点ID
 * @returns {Promise}
 */
export function deleteClient(id) {
  return request({
    url: `/node/v1/p2p/clients/${id}`,
    method: 'delete'
  })
}

/**
 * 拒绝客户端节点
 * @param {string} peerId - 客户端节点标识
 * @param {string} desc - 拒绝原因（非必填）
 * @returns {Promise}
 */
export function rejectClient(peerId, desc) {
  return request({
    url: `/node/v1/p2p/clients/${peerId}/reject`,
    method: 'put',
    params: { desc }
  })
}
