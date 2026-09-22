import request from '@/utils/request'

/**
 * 分页查询预警推送列表
 */
export function getWarningRelationList(params) {
  return request({
    url: '/api/warning/ralation/page',
    method: 'get',
    params: params
  })
}

/**
 * 新增预警推送记录
 */
export function createWarningRelation(data) {
  return request({
    url: '/api/warning/ralation/create',
    method: 'post',
    data: data
  })
}

/**
 * 更新预警推送记录
 * 接口只会处理一条记录，targetId应该是单个对象
 */
export function updateWarningRelation(data) { 
  return request({
    url: '/api/warning/ralation/update',
    method: 'post',
    data: data
  })
}

/**
 * 批量删除预警推送记录
 */
export function deleteWarningRelation(ids) {
  return request({
    url: '/api/warning/ralation/delete/list',
    method: 'delete',
    data: ids
  })
}

/**
 * 查询群组
 * type: 群组类型
 * key: 群组key
 */
export function queryGroupInfo(params) {
  return request({
    url: '/collaboration/v1/im/query/group/type',
    method: 'get',
    params: params
  })
}

/**
 * 查询用户
 * code: 部门code
 * includeChildren: 是否包含子部门用户
 */
export function queryUserInfo(params) {
  return request({
    url: '/collaboration/v1/im/queryUser',
    method: 'get',
    params: params
  })
}
