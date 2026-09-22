import request from '@/utils/request'

// 新建租户
export function createTenantOrganization(data) {
  return request({
    url: '/api/tenant/createTenantOrganization',
    method: 'post',
    data
  })
}

// 查询租户列表
export function getTenantOrganizatio(data) {
  return request({
    url: '/api/tenant/selectTenantOrganization',
    method: 'post',
    data
  })
}

// 删除租户
export function deleteTenant(data) {
  return request({
    url: '/api/tenant/deleteTenant',
    method: 'post',
    data
  })
}

// 租户参数列表查询
export function selectTenantGlobals(data) {
  return request({
    url: '/api/tenant/selectTenantGlobals',
    method: 'post',
    data
  })
}

// 租户参数修改/恢复
export function updateTenantGlobals(data) {
  return request({
    url: '/api/tenant/updateTenantGlobals',
    method: 'post',
    data
  })
}
