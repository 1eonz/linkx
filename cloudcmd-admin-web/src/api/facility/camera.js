import request from '@/utils/request'

export function getCameraList(data) {
  return request({
    url: '/api/facility/list',
    method: 'post',
    data
  })
}

export function createCamera(data) {
  return request({
    url: '/api/facility/create',
    method: 'post',
    data
  })
}

export function getCameraById(id) {
  return request({
    url: '/api/facility/id',
    method: 'post',
    params: { id }
  })
}

export function updateCamera(data) {
  return request({
    url: '/api/facility/update',
    method: 'post',
    data
  })
}

export function deleteCamera(data) {
  return request({
    url: '/api/facility/delete',
    method: 'post',
    data
  })
}

export function forEverdeleteCamera(data) {
  return request({
    url: '/api/facility/physicalDelete',
    method: 'post',
    data
  })
}

export function getCameraTypeList(category) {
  return request({
    url: '/api/facilityType/list',
    method: 'post',
    params: { category }
  })
}
