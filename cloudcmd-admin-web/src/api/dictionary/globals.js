import request from '@/utils/request'
const baseUrl = ''
export function queryGlobalsList() {
  return request({
    url: '/base/v1/globals/getGlobalsList',
    method: 'post'
  })
}

export function getGlobalsList(params) {
  return request({
    url:baseUrl +  '/api/globals/list',
    method: 'post',
    params
  })
}

export function createGlobals(data) {
  return request({
    url: baseUrl + '/api/globals/create',
    method: 'post',
    data
  })
}

export function updateGlobals(data) {
  return request({
    url: baseUrl + '/api/globals/update',
    method: 'post',
    data
  })
}

export function deleteGlobals(id) {
  return request({
    url: baseUrl + '/api/globals/delete',
    method: 'post',
    params: { id }
  })
}
