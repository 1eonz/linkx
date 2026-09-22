import request from '@/utils/request'

// ========== 出局数据（作为客户端）==========

/**
 * 查询对服务器的开放数据授权
 * @param {string} peerId - 服务器节点标识
 * @returns {Promise}
 */
export function getServerOpenDataGrant(peerId) {
  return request({
    url: `node/v1/p2p/servers/${peerId}/opendata/grant`,
    method: 'get'
  })
}

/**
 * 更新对服务器的开放数据授权
 * @param {string} peerId - 服务器节点标识
 * @param {Object} data - { users: 0|1, groups: 0|1, msg: 0|1, h5: 0|1, agent: 0|1 }
 * @returns {Promise}
 */
export function updateServerOpenDataGrant(peerId, data) {
  return request({
    url: `node/v1/p2p/servers/${peerId}/opendata/grant`,
    method: 'post',
    data
  })
}

// ========== 入局数据（作为服务器）==========

/**
 * 查询对客户端的开放数据授权
 * @param {string} peerId - 客户端节点标识
 * @returns {Promise}
 */
export function getClientOpenDataGrant(peerId) {
  return request({
    url: `/v1/p2p/clients/${peerId}/opendata/grant`,
    method: 'get'
  })
}

/**
 * 更新对客户端的开放数据授权
 * @param {string} peerId - 客户端节点标识
 * @param {Object} data - { users: 0|1, groups: 0|1, msg: 0|1, h5: 0|1, agent: 0|1 }
 * @returns {Promise}
 */
export function updateClientOpenDataGrant(peerId, data) {
  return request({
    url: `/v1/p2p/clients/${peerId}/opendata/grant`,
    method: 'post',
    data
  })
}

// ========== 统计数据 & 业务数据 ==========

/**
 * 查询统计数据
 * @param {string} peerId - 节点标识
 * @returns {Promise}
 */
export function getOpenDataStatistic(peerId) {
  return request({
    url: `node/v1/p2p/${peerId}/opendata/statistic`,
    method: 'get'
  })
}

/**
 * 搜索协同岗（业务数据）
 * @param {string} peerId - 节点标识
 * @param {Object} params - 查询参数
 * @returns {Promise}
 */
export function searchCoopData(peerId, params) {
  return request({
    url: `node/v1/p2p/${peerId}/opendata/coop/search`,
    method: 'get',
    params
  })
}
