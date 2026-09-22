import request from '@/utils/request'

// 平台查询
export function getRuleNames() {
  return request({
    url: '/api/iapselector/rule/names',
    method: 'get'
  })
}

// 查询所有任务
export function getRuleAll(params) {
  return request({
    url: '/api/iapselector/rule/all/' + params,
    method: 'get'
  })
}

// upsert全部规则
export function ruleUpsert(data, type) {
  return request({
    url: '/api/iapselector/rule/upsert/' + type,
    method: 'post',
    data
  })
}

// 算法查询
export function getRuleTypes(params) {
  return request({
    url: '/api/iapselector/rule/types/' + params,
    method: 'get'
  })
}
