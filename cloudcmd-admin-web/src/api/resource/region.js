import request from '@/utils/request'

export function getRegionList(status) {
  return request({
    url: '/api/regionrange/list',
    method: 'get',
    params: { status }
  })
}

export function updateRegion(data) {
  return request({
    url: '/api/regionrange/update',
    method: 'post',
    data
  })
}

export function deleteRegion(id) {
  return request({
    url: `/api/regionrange/delete/${id}`,
    method: 'get'
  })
}

export function getRegionById(id) {
  return request({
    url: `/api/regionrange/detail/${id}`,
    method: 'get'
  })
}

export function createRegion(data) {
  return request({
    url: '/api/regionrange/save',
    method: 'post',
    data
  })
}

export function regionDownload(data) {
  return request({
    url: '/api/regionrange/export',
    method: 'get',
    data,
    responseType: 'blob'
  })
}
export function regionDownloadModel() {
  return request({
    url: '/api/regionrange/export/model',
    method: 'get',
    responseType: 'blob'
  })
}
export function uploadFile(file) {
  return request({
    url: '/api/regionrange/load',
    method: 'post',
    data: file
  })
}

