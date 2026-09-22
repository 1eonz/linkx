import request from '@/utils/request';
// import { method } from `lodash`
const icpBaseUrl = `/proxy/icp/v1`;
export function getOrganizationList(status) {
  return request({
    url: `/api/organization/list`,
    method: 'get',
    params: { status }
  })
}

export function getDepartmentList(params) {
  return request({
    url: `/collaboration/v1/post/queryDepartment`,
    method: 'get',
    params
  })
}
export function updateOrganization(data) {
  return request({
    url: `/api/organization/update`,
    method: 'post',
    data
  })
}

export function deleteOrganization(id) {
  return request({
    url: `/api/organization/delete`,
    method: 'post',
    params: { id }
  })
}

export function getOrganizationById(id) {
  return request({
    url: `/api/organization/id`,
    method: 'post',
    params: { id }
  })
}

export function createOrganization(data) {
  return request({
    url: `/api/organization/create`,
    method: 'post',
    data
  })
}

export function getOrganizationChildren(id) {
  return request({
    url: `/api/organization/getChildren`,
    method: `post`,
    params: { id }
  })
}

export function moveOrganizationNode(data) {
  return request({
    url: `/api/organization/move`,
    method: `post`,
    data
  })
}

export function getOrganizationTypeList() {
  return request({
    url: `/api/organizationType/list`,
    method: `get`
  })
}

export function getIcpDepartmentTree() {
  return request({
    url: `${icpBaseUrl}/department/tree`,
    method: `get`
  })
}

export function getIcpCameraTree() {
  return request({
    url: `${icpBaseUrl}/camera-level/tree`,
    method: `get`
  })
}
