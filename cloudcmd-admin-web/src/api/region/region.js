import request from '@/utils/request'

export function getRegionList() {
  return request({
    url: '/api/region/list',
    method: 'get'
  })
}

export function autoLoad(data) {
  return request({
    url: '/api/regionrange/autoLoad',
    method: 'post',
    data
  })
}
