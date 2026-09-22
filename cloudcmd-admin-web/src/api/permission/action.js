import request from '@/utils/request'

export function getActionList(data) {
  return request({
    url: '/api/action/list',
    method: 'post',
    data
  })
}

export function createAction(data) {
  return request({
    url: '/api/action/create',
    method: 'post',
    data
  })
}

export function updateAction(data) {
  return request({
    url: '/api/action/update',
    method: 'post',
    data
  })
}

export function deleteAction(data) {
  return request({
    url: '/api/action/delete',
    method: 'post',
    data
  })
}
