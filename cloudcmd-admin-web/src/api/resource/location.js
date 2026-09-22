import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
// 位置列表
export function getLocationList(data) {
  return request({
    url: `${baseUrl}/dept/location/list`,
    method: 'get',
    params: data
  })
}

// 创建位置
export function createLocation(data) {
  return request({
    url: `${baseUrl}/dept/location/save`,
    method: 'post',
    data
  })
}

// 更新位置
export function updateLocation(data) {
  return request({
    url: `${baseUrl}/dept/location/save`,
    method: 'post',
    data
  })
}
//位置详情
export function getLocationDetail(id) {
  return request({
    url: `${baseUrl}/dept/location/get/`+id,
    method: 'get',
  })
}
// 删除位置
export function deleteLocation(id) {
  return request({
    url: `${baseUrl}/dept/location/delete/`+id,
    method: 'post',
  })
}
