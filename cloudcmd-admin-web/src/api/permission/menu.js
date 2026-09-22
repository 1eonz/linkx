import request from '@/utils/request'

export function getMenuList(data) {
  return request({
    url: '/api/menu/list',
    method: 'post',
    data
  })
}

export function updateMenu(data) {
  return request({
    url: '/api/menu/update',
    method: 'post',
    data
  })
}

export function moveNode(data) {
  return request({
    url: '/api/menu/move',
    method: 'post',
    data
  })
}

export function getMenuChildren(data) {
  return request({
    url: '/api/menu/getChildren',
    method: 'post',
    data
  })
}

export function deleteMenu(id) {
  return request({
    url: '/api/menu/delete',
    method: 'post',
    params: { id }
  })
}

export function getMenuById(id) {
  return request({
    url: '/api/menu/id',
    method: 'post',
    params: { id }
  })
}

export function createMenu(data) {
  return request({
    url: '/api/menu/create',
    method: 'post',
    data
  })
}

