import request from '@/utils/request'

export function getExtendInfoPropertiesList() {
  return request({
    url: '/api/extendInfoProperties/list',
    method: 'post'
  })
}

export function createExtendInfoProperties(data) {
  return request({
    url: '/api/extendInfoProperties/create',
    method: 'post',
    data
  })
}

export function updateExtendInfoProperties(data) {
  return request({
    url: '/api/extendInfoProperties/update',
    method: 'post',
    data
  })
}

export function deleteExtendInfoProperties(id) {
  return request({
    url: '/api/extendInfoProperties/delete',
    method: 'post',
    params: { id }
  })
}
