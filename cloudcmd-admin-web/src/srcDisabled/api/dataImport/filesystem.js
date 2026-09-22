import request from '@/utils/request'

export function getFilesystemDetail() {
  return request({
    url: '/api/filesystem/detail',
    method: 'post'
  })
}

export function deleteFilesystem() {
  return request({
    url: '/api/filesystem/delete',
    method: 'post'
  })
}
