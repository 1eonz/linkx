import request from '@/utils/request'
const baseUrl = '/api/auth/v1';
// 人员列表
export function getPersonList(data) {
  return request({
    url: `${baseUrl}/user/page`,
    method: 'get',
    params: data
  })
}

// 创建人员
export function createPerson(data) {
  return request({
    url: '/api/executor',
    method: 'post',
    data
  })
}

// 人员详情
export function getPersonById(id) {
  return request({
    url: '/api/executor/byId',
    method: 'get',
    params: { id }
  })
}

export function getUserRoleByUserId(id) {
  return request({
    url: `${baseUrl}/user/${id}/role`,
    method: 'put'
  })
}

// 更新人员
export function updatePerson(data) {
  return request({
    url: '/api/executor',
    method: 'put',
    data
  })
}

// 删除人员
export function deletePerson(data) {
  return request({
    url: `${baseUrl}/user?ids=` + data.ids,
    method: 'delete'
  })
}

export function getPersonTypeList(category) {
  return request({
    url: '/api/executorType/list',
    method: 'post',
    params: { category }
  })
}

export function getAssociateAccountByExecutorId(id) {
  return request({
    url: '/api/executor/getAssociateAccountByExecutorId',
    method: 'post',
    params: { id }
  })
}

export function getExecutorAssociateAccountIdList() {
  return request({
    url: '/api/executor/getExecutorAssociateAccountIdList',
    method: 'post'
  })
}

// 更新状态
export function updatePersonStatus(data) {
  return request({
    url: `${baseUrl}/user/${data.id}/status/${data.status}`,
    method: 'put'
  })
}

// 更新密码
export function updatePersonPwd(data) {
  return request({
    url: `${baseUrl}/user/${data.id}/pwd`,
    method: 'put',
    data
  })
}

// 单张上传图片
export function uploadTmp(file) {
  return request({
    url: '/api/content/carousel/uploadBatch',
    method: 'post',
    data: file,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
// 批量上传图片
export function uploadBatch(files) {
  return request({
    url: '/api/executor/uploadBatch',
    method: 'post',
    data: files,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 新增角色
export function bindRole(data) {
  return request({
    url: `${baseUrl}/user`,
    method: 'post',
    data
  })
}

export function setRole(data) {
  return request({
    url: `${baseUrl}/user/${data.userId}/roles`,
    method: 'put',
    data: { roleIds: data.roleIds }
  })
}

export function setBatchRole(data) {
  return request({
    url: `${baseUrl}/role/user`,
    method: 'put',
    data: { userIds: data.userIds, roleIds: data.roleIds }
  })
}

