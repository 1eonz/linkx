import request from '@/utils/request'

// 获取分组列表
export function getGroupPage(params) {
  return request({
    url: '/third/v1/apps/groups',
    method: 'get',
    params
  })
}

// 创建分组
export function createGroup(data) {
  return request({
    url: '/third/v1/apps/groups/app',
    method: 'post',
    data
  })
}

// 更新分组
export function updateGroup(data) {
  return request({
    url: '/third/v1/apps/groups/app',
    method: 'put',
    data
  })
}

// 删除分组
export function deleteGroup(id) {
  return request({
    url: `/third/v1/apps/groups/${id}`,
    method: 'delete'
  })
}