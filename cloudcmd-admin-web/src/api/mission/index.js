import request from '@/utils/request'

// 新增配置
export function createGuide(data) {
  return request({
    url: '/api/flow/config',
    method: 'post',
    data
  })
}

// 任务类型
export function flowConfigTypes(data) {
  return request({
    url: '/api/flow/config/types',
    method: 'get',
    data
  })
}

// 列表
export function flowConfigAll(pageSize, pageNum) {
  return request({
    url: `/api/flow/config/all?pageSize=${pageSize}&pageNum=${pageNum}`,
    method: 'get'
  })
}
