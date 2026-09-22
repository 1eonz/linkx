import request from '@/utils/request'

export function getRoleList(params) {
  return request({
    url: '/api/role',
    method: 'get',
    params
  })
}

export function createRole(data) {
  return request({
    url: '/api/role',
    method: 'post',
    data
  })
}

export function updateRole(data) {
  return request({
    url: '/api/role',
    method: 'put',
    data
  })
}

export function deleteRole(data) {
  return request({
    url: '/api/role/deleteBatch',
    method: 'post',
    data
  })
}
