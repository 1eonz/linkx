import request from '@/utils/request'

export function getUserList(data) {
  return request({
    url: '/api/user/list',
    method: 'post',
    data
  })
}

export function createUser(data) {
  return request({
    url: '/api/user/create',
    method: 'post',
    data
  })
}

export function getUserById(id) {
  return request({
    url: '/api/user/id',
    method: 'post',
    params: { id }
  })
}

export function updateUser(data) {
  return request({
    url: '/api/user/update',
    method: 'post',
    data
  })
}

export function changeUserPwd(data) {
  return request({
    url: '/api/user/updatePwd',
    method: 'post',
    data
  })
}

export function deleteUser(data) {
  return request({
    url: '/api/user/delete',
    method: 'post',
    data
  })
}

export function getAssociatePerson(id) {
  return request({
    url: '/api/user/getAssociateExecutorById',
    method: 'post',
    params: { id }
  })
}

export function getAssociatePersonIdList(category) {
  return request({
    url: '/api/user/getAssociateExecutorIdList',
    method: 'post',
    params: { category }
  })
}
