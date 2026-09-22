import request from '@/utils/request'

/**
 * 查询服务器节点列表（分页）
 * @param {Object} params - { pageNum?: number, pageSize?: number, keyword?: string }
 * @returns {Promise}
 */
export function getServers(params) {
  return request({
    url: '/node/v1/p2p/servers',
    method: 'get',
    params
  })
}

/**
 * 创建服务器节点
 * @param {Object} data - { ip: string, port: number, name?: string, tag?: string }
 * @returns {Promise}
 */
export function createServer(data) {
  return request({
    url: '/node/v1/p2p/servers',
    method: 'post',
    data
  })
}

/**
 * 更新服务器节点
 * @param {number|string} id - 服务器节点ID
 * @param {Object} data - { ip: string, port: number, name?: string, tag?: string }
 * @returns {Promise}
 */
export function updateServer(id, data) {
  return request({
    url: `/node/v1/p2p/servers/${id}`,
    method: 'put',
    data
  })
}

/**
 * 删除服务器节点
 * @param {number|string} id - 服务器节点ID
 * @returns {Promise}
 */
export function deleteServer(id) {
  return request({
    url: `/node/v1/p2p/servers/${id}`,
    method: 'delete'
  })
}
