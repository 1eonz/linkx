import request from '@/utils/request'

export function reportUdcInfo(data) {
  return request({
    url: '/api/udc/reportUdc',
    method: 'post',
    data
  })
}

export function udcSync() {
  return request({
    url: '/api/udc/sync',
    method: 'post',
    timeout: 100000
  })
}

export function getReportInfo() {
  return request({
    url: '/api/udc/ccmdinfo',
    method: 'post'
  })
}
