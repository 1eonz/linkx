import request from '@/utils/request'

// 新增配置
export function getAgentSubmissionPage(params) {
  return request({
    url: '/collaboration/v1/agent/submission/page',
    method: 'get',
    params
  })
}
