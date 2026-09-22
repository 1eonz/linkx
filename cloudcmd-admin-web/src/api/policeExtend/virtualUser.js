import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
// 获取虚拟用户分页列表
export function getVirtualUserList(params) {
  return request({
    url: `${baseUrl}/im/users/virtual`,
    method: 'get',
    params
  })
}

// 添加虚拟用户
export function addVirtualUser(data) {
  return request({
    url: `${baseUrl}/im/users/virtual`,
    method: 'post',
    data
  })
}

// 更新虚拟用户
export function updateVirtualUser(data) {
  return request({
    url: `${baseUrl}/im/users/virtual/${data.id}`,
    method: 'put',
    data
  })
}

// 删除虚拟用户
export function deleteVirtualUser(id) {
  return request({
    url: `${baseUrl}/im/users/virtual/${id}`,
    method: 'delete'
  })
}
