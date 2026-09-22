import request from '@/utils/request'
const mapUrl = '/api'
export function createMap(data) {
  return request({
    url: `${mapUrl}/map/createMap`,
    method: 'post',
    data
  })
}

export function deleteMap(id) {
  return request({
    url: `${mapUrl}/map/deleteMap?id=${id}`,
    method: 'post'
  })
}

export function updateMap(data) {
  return request({
    url: `${mapUrl}/map/updateMap`,
    method: 'post',
    data
  })
}

export function selectPageMap(data) {
  return request({
    url: `${mapUrl}/map/selectPageMap`,
    method: 'post',
    data
  })
}

export function uploadMapIcon(data) {
  return request({
    url: `${mapUrl}/map/uploadMapIcon`,
    method: 'post',
    data
  })
}

export function uploadBaseMap(data) {
  return request({
    url: `${mapUrl}/map/uploadBaseMap`,
    method: 'post',
    data,
    timeout: 300000
  })
}

export function selectPageBaseMap(data) {
  return request({
    url: `${mapUrl}/map/selectPageBaseMap`,
    method: 'post',
    data
  })
}

export function deleteBaseMap(id) {
  return request({
    url: `${mapUrl}/map/deleteBaseMap?id=${id}`,
    method: 'post'
  })
}

// 地图数据列表查询
export function selectListGeo(data) {
  return request({
    url: `${mapUrl}/map/selectListGeo`,
    method: 'post',
    data
  })
}

// 地图数据更新
export function updateGeo(data) {
  return request({
    url: `${mapUrl}/map/updateGeo`,
    method: 'post',
    data
  })
}

// 行政区划列表查询
export function selectDivision() {
  return request({
    url: `${mapUrl}/map/selectDivision`,
    method: 'post'
  })
}

// 更新底图
export function initBaseMap() {
  return request({
    url: `${mapUrl}/map/initBaseMap`,
    method: 'post'
  })
}

// 导出行政区域
export function updateDivision(data) {
  return request({
    url: `${mapUrl}/map/updateDivision`,
    method: 'post',
    data
  })
}

// 查询选中的地图数据
export function selectGeo() {
  return request({
    url: `${mapUrl}/map/selectGeo`,
    method: 'post',
    data: { id: null }
  })
}

// 导出行政区划
export function downloadRegion(nodeId = '0') {
  return request({
    url: `${mapUrl}/map/downloadRegion`,
    method: 'post',
    params: {
      nodeId
    }
  })
}
