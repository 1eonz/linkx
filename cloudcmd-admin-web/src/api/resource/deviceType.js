// api/device/deviceType.js

import request from '@/utils/request'
const baseUrl = '/proxy/icp/v1';
/**
 * 获取设备类型列表
 * @returns {Promise}
 */
export function getDeviceTypeList() {
  return request({
    url: `${baseUrl}/isdnType/list`,
    method: 'get'
  })
}

/**
 * 更新设备类型
 * @param {Object} data - 设备类型数据
 * @returns {Promise}
 */
export function updateDeviceType(data) {
  return request({
    url: `${baseUrl}/isdnType/${data.id}`,
    method: 'put',
    data
  })
}
/**
 * 上传图标
 * @param {Object} file - 图标formData
 * @returns {Promise}
 */
export function uploadIcon(file) {
  return request({
    url: `${baseUrl}/isdnType/uploadIcon`,
    method: 'POST',
    data:file,
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
// 跟新类型是否展示
export function updateIsShow(data) {
  return request({
    url: `${baseUrl}/isdnType/${data.id}/isShow`,
    method: 'put',
    params: data
  })
}