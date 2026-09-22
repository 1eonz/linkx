import request from '@/utils/request'
const baseUrl = '/api';
// 查询应用管理分页
export function getInfoPage(params) {
  return request({
    url: `${baseUrl}/content/app/info/page`,
    method: 'get',
    params
  })
}

// 查询应用管理详情
export function getInfo(id) {
  return request({
    url: `${baseUrl}/content/app/info/get?id=${id}`,
    method: 'get',
  })
}

// 新增应用管理
export function createInfo(data) {
  return request({
    url: `${baseUrl}/content/app/info/create`,
    method: 'post',
    data
  })
}

// 修改应用管理
export function updateInfo(data) {
  return request({
    url: `${baseUrl}/content/app/info/update`,
    method: 'put',
    data
  })
}

// 修改应用管理
export function updateStatus(id, status) {
  const data = {
    id,
    status
  }
  return request({
    url: `${baseUrl}/content/app/info/update_status`,
    method: 'put',
    data
  })
}

// 删除应用管理
export function deleteInfo(id) {
  return request({
    url: `${baseUrl}/content/app/info/delete?id=${id}`,
    method: 'delete'
  })
}
// 查询前置应用
export function getPrerequisiteList(params) {
  console.log('查询前置应用参数:', params)
  return request({
    url: `${baseUrl}/content/app/info/prerequisite`,
    method: 'get',
    params
  })
}
