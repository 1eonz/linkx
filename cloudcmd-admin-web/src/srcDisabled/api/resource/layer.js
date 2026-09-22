import request from '@/utils/request'

export function getLayerList(data) {
  return request({
    url: '/api/layer/list',
    method: 'post',
    data
  })
}
export function createLayer(data) {
  return request({
    url: '/api/layer/create',
    method: 'post',
    data
  })
}
export function getLayerById(id) {
  return request({
    url: '/api/layer/id',
    method: 'post',
    params: { id }
  })
}

export function updateLayer(data) {
  return request({
    url: '/api/layer/update',
    method: 'post',
    data
  })
}

export function deleteLayer(data) {
  return request({
    url: '/api/layer/delete',
    method: 'post',
    data
  })
}
