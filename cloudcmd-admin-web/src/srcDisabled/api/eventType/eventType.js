import request from '@/utils/request'

export function getEventTypeList(category) {
  return request({
    url: '/api/eventType/list',
    method: 'post',
    params: { category }
  })
}

export function updateEventType(data) {
  return request({
    url: '/api/eventType/update',
    method: 'post',
    data
  })
}

export function deleteEventType(id) {
  return request({
    url: '/api/eventType/delete',
    method: 'post',
    params: { id }
  })
}

export function getEventTypeById(id) {
  return request({
    url: '/api/eventType/id',
    method: 'post',
    params: { id }
  })
}

export function createEventType(data) {
  return request({
    url: '/api/eventType/create',
    method: 'post',
    data
  })
}
