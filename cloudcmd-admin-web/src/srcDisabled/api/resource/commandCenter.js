import request from '@/utils/request'

export function getCommandCenterList() {
  return request({
    url: '/api/commandCenter/list',
    method: 'post'
  })
}

export function updateCommandCenter(data) {
  return request({
    url: '/api/commandCenter/update',
    method: 'post',
    data
  })
}

export function deleteCommandCenter(id) {
  return request({
    url: '/api/commandCenter/delete',
    method: 'post',
    params: { id }
  })
}

export function getCommandCenterById(id) {
  return request({
    url: '/api/commandCenter/id',
    method: 'post',
    params: { id }
  })
}

export function createCommandCenter(data) {
  return request({
    url: '/api/commandCenter/create',
    method: 'post',
    data
  })
}

export function getOrganizationListByCommandCenterId(commandCenterId) {
  return request({
    url: '/api/commandCenter/organizationList',
    method: 'post',
    params: { commandCenterId }
  })
}

export function updateManageOrganizationList(data) {
  return request({
    url: '/api/commandCenter/organizationUpdate',
    method: 'post',
    data
  })
}

export function getExecutorListByCommandCenterId(commandCenterId) {
  return request({
    url: '/api/commandCenter/executorList',
    method: 'post',
    params: { commandCenterId }
  })
}

export function updateManageExecutorList(data) {
  return request({
    url: '/api/commandCenter/executorUpdate',
    method: 'post',
    data
  })
}

export function getCommandCenterChildren(id) {
  return request({
    url: '/api/commandCenter/getChildren',
    method: 'post',
    params: { id }
  })
}

export function moveCommandCenterNode(data) {
  return request({
    url: '/api/commandCenter/move',
    method: 'post',
    data
  })
}
