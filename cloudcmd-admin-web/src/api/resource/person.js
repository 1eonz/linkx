import request from '@/utils/request'
const baseUrl = '/auth/v1';
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
    url: `${baseUrl}/user/${data.userId}/role/${data.roleId}`,
    method: 'put',
    data
  })
}

export function setBatchRole(data) {
  return request({
    url: `${baseUrl}/role/${data.roleId}/user`,
    method: 'put',
    data: data.userIds
  })
}

// ==================== 用户管理接口 ====================

// 用户列表（分页）
export function getUserListByPage(data) {
  return request({
    // url: 'http://172.24.65.22:8081${baseUrl}/user/adminuser/page',
    url: `${baseUrl}/user/adminuser/page`,
    method: 'get',
    params: data
  })
}

// 新增用户
export function createUser(data) {
  return request({
    // url: 'http://172.24.65.22:8081${baseUrl}/user/adminuser',
    url: `${baseUrl}/user/adminuser`,
    method: 'post',
    data
  })
}

// 编辑用户
export function updateUser(data) {
  return request({
    // url: 'http://172.24.65.22:8081${baseUrl}/user/adminuser',
    url: `${baseUrl}/user/adminuser`,
    method: 'put',
    data
  })
}

// 用户详情
export function getUserById(id) {
  return request({
    // url: 'http://172.24.65.22:8081${baseUrl}/user/adminuser/' + id,
    url: `${baseUrl}/user/adminuser/${id}`,
    method: 'get',
  })
}

// 删除用户
export function deleteUser(id) {
  return request({
    // url: 'http://172.24.65.22:8081${baseUrl}/user/adminuser/' + id,
    url: `${baseUrl}/user/adminuser/${id}`,
    method: 'delete'
  })
}

// 重置密码
export function resetUserPassword(data) {
  return request({
    // url: `http://172.24.65.22:8081${baseUrl}/user/${data.id}/pwd`,
    url: `${baseUrl}/adminuser/${data.id}/pwd`,
    method: 'put',
    data
  })
}

