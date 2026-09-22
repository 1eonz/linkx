import request from '@/utils/request'

export function getExtendInfoPropertiesListByCode(code) {
  return request({
    url: '/api/extendInfoProperties/listByCode',
    method: 'get',
    params: { code }
  })
}
