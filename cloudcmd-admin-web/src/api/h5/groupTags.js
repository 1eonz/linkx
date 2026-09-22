import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
// 查询标签管理分页
export function getInfoPage(params) {
  return request({
    url: `${baseUrl}/tags/page`,
    method: 'get',
    params
  })
}

// 查询标签管理详情
export function getInfo(id) {
  return request({
    url: `${baseUrl}/tags/${id}`,
    method: 'get',
  })
}

// 新增标签管理
export function createInfo(data) {
  return request({
    url: `${baseUrl}/tags`,
    method: 'post',
    data
  })
}

// 修改标签管理
export function updateInfo(data) {
  return request({
    url: `${baseUrl}/tags/${data.id}`,
    method: 'put',
    data
  })
}

// 删除标签管理
export function deleteInfo(id) {
  return request({
    url: `${baseUrl}/tags/${id}`,
    method: 'delete'
  })
}

// 群组标签批量删除
export function tagsBatchDelete(ids) {
  return request({
    url: `${baseUrl}/tags/delete/list`,
    method: 'delete',
    data: ids
  })
}
