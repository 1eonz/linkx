import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
// 警单分页查询
export function policeticketPage(params) {
  return request({
    url: `${baseUrl}/policeticket/page`,
    method: 'get',
    params
  })
}
// 根据id查警单详情
export function getPoliceticketById(id) {
  return request({
    url: `${baseUrl}/policeticket/${id}`,
    method: 'get'
  })
}

//警单对接
export function saveDock(data) {
  return request({
    url: `${baseUrl}/poltclients`,
    method: 'post',
    data
  })
}

//警单对接更新
export function updateDock(data) {
  return request({
    url: `${baseUrl}/poltclients`,
    method: 'put',
    data
  })
}

//分页查询警单配置
export function getDockPage(params) {
  return request({
    url: `${baseUrl}/poltclients/page`,
    method: 'get',
    params
  })
}
//删除警单配置
export function deletePoliceticket(id) {
  return request({
    url: `${baseUrl}/poltclients/${id}`,
    method: 'delete'
  })
}
//启用禁用
export function changeEnable(data) {
  return request({
    url: `${baseUrl}/poltclients/enable`,
    method: 'put',
    data
  })
}

//查询警单类型
export function getPolicetickettypes(params) {
  return request({
    url: `${baseUrl}/policetickettype/list`,
    method: 'get',
    params
  })
}

//分页查询警单类型
export function getPolicetickettypesPage(params) {
  return request({
    url: `${baseUrl}/policetickettype/page`,
    method: 'get',
    params
  })
}

//警单类型新增
export function savePolicetickettype(data) {
  return request({
    url: `${baseUrl}/policetickettype`,
    method: 'post',
    data
  })
}

//警单类型更新
export function updatePolicetickettypes(data) {
  return request({
    url: `${baseUrl}/policetickettype`,
    method: 'put',
    data
  })
}
//删除警单类型
export function deletePolicetickettypes(id) {
  return request({
    url: `${baseUrl}/policetickettype/${id}`,
    method: 'delete'
  })
}
