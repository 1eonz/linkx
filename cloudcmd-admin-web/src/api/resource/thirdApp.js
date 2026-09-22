import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
export function collaborationList(data) {
  return request({
    url: `${baseUrl}/client/list`,
    method: 'post',
    data
  })
}
export function collaborationCreate(data) {
  return request({
    url: `${baseUrl}/client/create `,
    method: 'post',
    data
  })
}

export function collaborationDelete(id) {
  return request({
    url: `${baseUrl}/client/delete?id=` + id,
    method: 'delete'
  })
}

export function collaborationUpdate(data) {
  return request({
    url: `${baseUrl}/client/update`,
    method: 'put',
    data
  })
}

export function collaborationDetail(id) {
  return request({
    url: `${baseUrl}/client/detail?id=` + id,
    method: 'get'
  })
}
