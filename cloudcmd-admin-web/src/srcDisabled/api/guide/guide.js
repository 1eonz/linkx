import request from '@/utils/request'

// 单张上传文件
export function uploadGuideFile(file) {
  return request({
    url: '/api/alertDispositionGuide/uploadFile',
    method: 'post',
    data: file,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 新建处警指引
export function createGuide(data) {
  return request({
    url: '/api/alertDispositionGuide/create',
    method: 'post',
    data
  })
}

// 编辑处警指引
export function editGuide(data) {
  return request({
    url: '/api/alertDispositionGuide/update',
    method: 'post',
    data
  })
}

// 删除处警指引
export function deleteGuide(data) {
  return request({
    url: '/api/alertDispositionGuide/deleteBatch',
    method: 'post',
    data
  })
}

// 获取处警指引列表
export function getGuideList(params) {
  return request({
    url: '/api/alertDispositionGuide/list',
    method: 'get',
    params
  })
}
