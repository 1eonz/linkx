import request from '@/utils/request'

// 三道防线图层管理
export function createLayer(data) {
  return request({
    url: '/api/customLayer/create',
    method: 'post',
    data
  })
}

export function deleteLayer(data) {
  return request({
    url: '/api/customLayer/deleteBatch',
    method: 'post',
    data
  })
}

export function updateLayer(data) {
  return request({
    url: '/api/customLayer/update',
    method: 'put',
    data
  })
}

export function getLayerList(data) {
  return request({
    url: '/api/customLayer/list',
    method: 'post',
    data
  })
}

export function getLayerById(id) {
  return request({
    url: '/api/customLayer/selectCustomLayerById',
    method: 'get',
    params: { id }
  })
}

// 三道防线点位管理
export function createPoint(data) {
  return request({
    url: '/api/customPoint/create',
    method: 'post',
    data
  })
}

export function deletePoint(data) {
  return request({
    url: '/api/customPoint/deleteBatch',
    method: 'post',
    data
  })
}

export function updatePoint(data) {
  return request({
    url: '/api/customPoint/update',
    method: 'put',
    data
  })
}

export function getPointList(data) {
  return request({
    url: '/api/customPoint/list',
    method: 'post',
    data
  })
}

export function getPointById(id) {
  return request({
    url: '/api/customPoint/selectCustomPointById',
    method: 'get',
    params: { id }
  })
}

// 模板导入
export function importPoint(data) {
  return request({
    url: '/api/customPoint/import',
    method: 'post',
    timeout: '0',
    data
  })
}

// 模板导出
export function exportModel() {
  return request({
    url: '/api/customPoint/exportModel',
    method: 'post',
    responseType: 'blob'
  })
}
