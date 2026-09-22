import request from '@/utils/request'

// 查询车辆幻化/图上绘制图标列表
export function queryIconList(params) {
  return request({
    url: '/api/icon/list',
    method: 'get',
    params
  })
}

// 批量添加车辆幻化/图上绘制图标
export function multipartIcon(data, type) {
  return request({
    url: `/api/icon?type=${type}`,
    method: 'post',
    data,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

// 删除车辆幻化/图上绘制图标
export function deleteIconBatch(data) {
  return request({
    url: '/api/icon/deleteBatch',
    method: 'post',
    data
  })
}

// 查询具有gis设备类型
export function getEquipmentTypeByGisFlag(params) {
  return request({
    url: '/api/equipmentType/getEquipmentTypeByGisFlag',
    method: 'get',
    params
  })
}

// 更新设备类型车辆幻化开关
export function updateVehicleIllusionSwitch(data) {
  return request({
    url: '/api/equipmentType/updateVehicleIllusionSwitch',
    method: 'post',
    data
  })
}
