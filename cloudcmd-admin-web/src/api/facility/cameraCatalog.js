import request from '@/utils/request'

export function getCameraCatalogList(data) {
  return request({
    url: '/api/facilityCatalog/list',
    method: 'post',
    params: data
  })
}

export function updateCameraCatalog(data) {
  return request({
    url: '/api/facilityCatalog/update',
    method: 'post',
    data
  })
}

export function deleteCameraCatalog(id) {
  return request({
    url: '/api/facilityCatalog/delete',
    method: 'post',
    params: { id }
  })
}

export function getCameraCatalogById(id) {
  return request({
    url: '/api/facilityCatalog/id',
    method: 'post',
    params: { id }
  })
}

export function getCameraCatalogChildren(id) {
  return request({
    url: '/api/facilityCatalog/getChildren',
    method: 'post',
    params: { id }
  })
}

export function moveCameraCatalogNode(data) {
  return request({
    url: '/api/facilityCatalog/move',
    method: 'post',
    data
  })
}

export function createCameraCatalog(data) {
  return request({
    url: '/api/facilityCatalog/create',
    method: 'post',
    data
  })
}

export function queryByFacilityCatalogId(catalogId) {
  return request({
    url: '/api/facilityCatalogOrganization/queryByFacilityCatalogId',
    method: 'post',
    params: { catalogId }
  })
}

export function createFacilityCatalogOrg(data) {
  return request({
    url: '/api/facilityCatalogOrganization/create',
    method: 'post',
    data
  })
}
