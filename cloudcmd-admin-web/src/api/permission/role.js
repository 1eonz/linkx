import request from '@/utils/request'

export function getRoleList() {
  return request({
    url: 'role/list',
    method: 'post'
  })
}

export function getRoleListByPage(data) {
  return request({
    url: '/api/role/listByPage',
    method: 'post',
    data
  })
}

export function getRoleTypeList() {
  return request({
    url: '/api/role/getRoleTypeList',
    method: 'post'
  })
}

export function updateRole(data) {
  return request({
    url: '/api/role/update',
    method: 'post',
    data
  })
}

export function deleteRole(id) {
  return request({
    url: '/api/role/delete',
    method: 'post',
    params: { id }
  })
}

export function createRole(data) {
  return request({
    url: '/api/role/create',
    method: 'post',
    data
  })
}

export function createManyRole(data) {
  return request({
    url: '/api/trUserRole/createMany ',
    method: 'post',
    data
  })
}

export function getManyUserRole(userId) {
  return request({
    url: '/api/trUserRole/id',
    method: 'post',
    params: { userId }
  })
}

