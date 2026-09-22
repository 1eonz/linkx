import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
/**
 * 分页查询值班类型列表
 * @param {Object} params - { pageNum: number, pageSize: number, name?: string }
 * @returns {Promise}
 */
export function getDutyTypes(params) {
  return request({
    // url: 'http://172.24.65.16:8081/collaboration/duty/type/page',
    url: `${baseUrl}/duty/type/page`,
    method: 'get',
    params
  })
}

/**
 * 查询所有值班类型列表
 * @returns {Promise}
 */
export function getAllDutyTypes() {
  return request({
    // url: 'http://172.24.65.16:8081/collaboration/duty/type/all',
    url: `${baseUrl}/duty/type/all`,
    method: 'get'
  })
}

/**
 * 值班类型详情
 * @param {number|string} type - 类型标识
 * @returns {Promise}
 */
export function getDutyTypeDetail(type) {
  return request({
    // url: `http://172.24.65.16:8081/collaboration/duty/type/${type}`,
    url: `${baseUrl}/duty/type/${type}`,
    method: 'get'
  })
}

/**
 * 创建值班类型
 * @param {Object} data - { name: string }
 * @returns {Promise}
 */
export function createDutyType(data) {
  return request({
    // url: 'http://172.24.65.16:8081/collaboration/duty/type',
    url: `${baseUrl}/duty/type`,    
    method: 'post',
    data
  })
}

/**
 * 更新值班类型
 * @param {number|string} type - 类型标识
 * @param {Object} data - { name: string }
 * @returns {Promise}
 */
export function updateDutyType(type, data) {
  return request({
    // url: `http://172.24.65.16:8081/collaboration/duty/type/${type}`,
    url: `${baseUrl}/duty/type/${type}`,    
    method: 'put',
    data
  })
}

/**
 * 删除值班类型
 * @param {number|string} type - 类型标识
 * @returns {Promise}
 */
export function deleteDutyType(type) {
  return request({
    // url: `http://172.24.65.16:8081/collaboration/duty/type/${type}`,
    url: `${baseUrl}/duty/type/${type}`,    
    method: 'delete'
  })
}
