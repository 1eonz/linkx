import request from '@/utils/request'

export function getExecutorEquipmentList(data) {
  return request({
    url: '/api/executorToEquipment/list',
    method: 'post',
    data
  })
}

export function deleteExecutorEquipmentList(data) {
  return request({
    url: '/api/executorToEquipment/delete',
    method: 'post',
    data
  })
}

export function getHasChosenEquipment(data) {
  return request({
    url: '/api/executorToEquipment/chosenEquipment',
    method: 'post',
    data
  })
}

export function queryHasChosenEquipment() {
  return request({
    url: '/api/policeCar/egdegateway',
    method: 'get'
  })
}

export function getEquipmentTypeList() {
  return request({
    url: '/api/equipmentType/list',
    method: 'post'
  })
}

export function createExecutorToEquipment(data) {
  return request({
    url: '/api/executorToEquipment/create',
    method: 'post',
    data
  })
}

export function getEquipmentsByExecutorId(executorId) {
  return request({
    url: '/api/executorToEquipment/getEquipmentsByExecutorId',
    method: 'post',
    params: { executorId }
  })
}
