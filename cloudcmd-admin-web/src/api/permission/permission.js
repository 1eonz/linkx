import request from '@/utils/request'
const baseUrl = '/proxy/icp/v1';
export function getPermissionList(data) {
  return request({
    url: '/api/permission/list',
    method: 'post',
    data
  })
}

export function getPermissionListByParam(data) {
  return request({
    url: '/api/permission/listByParam',
    method: 'post',
    data
  })
}

export function submitPermissionConfig(data) {
  return request({
    url: '/api/permission/configPermission',
    method: 'post',
    data
  })
}

export function getImuserPriv(id) {
  return request({
    url: `${baseUrl}/imuser/priv/${id}`,
    method: 'get'
  })
}

export function setImuserPriv(id, data) {
  return request({
    url: `${baseUrl}/imuser/priv/${id}`,
    method: 'put',
    data
  })
}

export function getCameraPriv(id) {
  return request({
    url: `${baseUrl}/camera/priv/${id}`,
    method: 'get'
  })
}

export function setCameraPriv(id, data) {
  return request({
    url: `${baseUrl}/camera/priv/${id}`,
    method: 'put',
    data
  })
}

export function batchSetDeptOrgPriv(data) {
  return request({
    url: `${baseUrl}/imuser/priv/dept`,
    method: 'put',
    data
  })
}

export function batchSetDeptCameraPriv(data) {
  return request({
    url: `${baseUrl}/camera/priv/dept`,
    method: 'put',
    data
  })
}
