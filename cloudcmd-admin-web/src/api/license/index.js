import request from '@/utils/request'

// 新增配置
export function getLicenseInfo() {
  return request({
    url: '/api/msip/license/info',
    method: 'get'
  })
}
