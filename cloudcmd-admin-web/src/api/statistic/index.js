import request from '@/utils/request'

// 导出日活数据
export function exportLoginStatistic(params) {
  return request({
    url: '/dashboard/v1/statistic/login/export',
    method: 'get',
    params,
    responseType: 'blob'
  })
}
