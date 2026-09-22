import request from '@/utils/request'

export function getAuthList(data) {
  return request({
    url: '/api/cookie/list',
    method: 'post',
    data
  })
}

export function getUserLogList(data) {
  return request({
    url: '/api/userLog/list',
    method: 'post',
    data
  })
}

export function getEquipmentLogList(data) {
  return request({
    url: '/api/log/list',
    method: 'post',
    data
  })
}

export function downloadLog(taskId) {
  return request({
    url: '/api/log/download',
    method: 'get',
    responseType: 'blob',
    params: { taskId }
  })
}

export function queryWeblogs(data) {
  return request({
    url: '/api/weblogs/queryWeblogs',
    method: 'post',
    data
  })
}

export function exportWeblogs(data) {
  return request({
    url: '/api/weblogs/exportWeblogs',
    method: 'post',
    responseType: 'blob',
    data
  })
}

export function deleteWeblogs(data) {
  return request({
    url: '/api/weblogs/deleteWeblogs',
    method: 'post',
    data
  })
}
