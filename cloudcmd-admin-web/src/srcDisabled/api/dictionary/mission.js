import request from '@/utils/request'

export function getMissionList() {
  return request({
    url: '/api/push/mission/list',
    method: 'get'
  })
}

export function createMission(data) {
  return request({
    url: '/api/push/mission/create',
    method: 'post',
    data
  })
}

export function updateMission(data) {
  return request({
    url: '/api/push/mission/update',
    method: 'post',
    data
  })
}

export function deleteMission(id) {
  return request({
    url: '/api/push/mission/delete',
    method: 'post',
    params: { id }
  })
}

export function getMissionById(id) {
  return request({
    url: '/api/push/mission/id',
    method: 'post',
    params: { id }
  })
}

export function getPlatformList() {
  return request({
    url: '/api/push/mission/platform/list',
    method: 'get'
  })
}
