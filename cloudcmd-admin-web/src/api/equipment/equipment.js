import request from '@/utils/request'

export function getEquipmentList(data) {
  return request({
    url: '/api/equipment/list',
    method: 'post',
    data
  })
}

export function createEquipment(data) {
  return request({
    url: '/api/equipment/create',
    method: 'post',
    data
  })
}

export function getEquipmentById(id) {
  return request({
    url: '/api/equipment/id',
    method: 'post',
    params: { id }
  })
}

export function updateEquipment(data) {
  return request({
    url: '/api/equipment/update',
    method: 'post',
    data
  })
}

export function deleteEquipment(data) {
  return request({
    url: '/api/equipment/delete',
    method: 'post',
    data
  })
}

export function getEquipmentTypeList(category) {
  return request({
    url: '/api/equipmentType/listByCategory',
    method: 'post',
    params: { category }
  })
}

export function getAssociateAccountByEquipmentId(id) {
  return request({
    url: '/api/equipment/getAssociateAccountByEquipmentId',
    method: 'post',
    params: { id }
  })
}

export function getEquipmentAssociateAccountIdList() {
  return request({
    url: '/api/equipment/getEquipmentAssociateAccountIdList',
    method: 'post'
  })
}

export function getEquipmentCapabilityList(type) {
  return request({
    url: '/api/equipment/capabilityList',
    method: 'post',
    params: { type }
  })
}

// 新增/修改警车
export function editPoliceCar(data) {
  return request({
    url: '/api/policeCar',
    method: 'put',
    data
  })
}

// 查询警车列表
export function fuzzySearchingPoliceCar(data) {
  return request({
    url: '/api/policeCar/fuzzySearching',
    method: 'post',
    data
  })
}

// 删除警车
export function deletePoliceCar(data) {
  return request({
    url: '/api/policeCar/delete',
    method: 'post',
    data
  })
}

// 启用警车
export function enablePoliceCar(params) {
  return request({
    url: '/api/policeCar/enable/' + params,
    method: 'post'
  })
}

// 查询警车
export function queryPoliceCarById(params) {
  return request({
    url: '/api/policeCar/' + params,
    method: 'get'
  })
}

// 领用警车
export function bindingPoliceCar(params) {
  return request({
    url: '/api/policeCar/binding',
    method: 'get',
    params
  })
}

// 分页模糊查询警车
export function queryPoliceCarByPage(data) {
  return request({
    url: '/api/policeCar/page',
    method: 'post',
    data
  })
}

export function getExtractLog(data) {
  return request({
    url: '/api/log/extract',
    method: 'post',
    data
  })
}

// 南向设备
export function getSouthboundList() {
  return request({
    url: '/api/south/list',
    method: 'post',
  })
}

export function querySouthboundList(data) {
  return request({
    url: '/api/south/queryByParam',
    method: 'post',
    data
  })
}
