import request from '@/utils/request'

export function getApplicationList() {
  return request({
    url: '/api/application/list',
    method: 'post'
  })
}

export function createApplication(data) {
  return request({
    url: '/api/application/create',
    method: 'post',
    data
  })
}

export function updateApplication(data) {
  return request({
    url: '/api/application/update',
    method: 'post',
    data
  })
}

export function deleteApplication(data) {
  return request({
    url: '/api/application/delete',
    method: 'post',
    data
  })
}
