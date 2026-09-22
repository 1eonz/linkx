import request from '@/utils/request'
const baseUrl = '/api';
// 系统参数设置
export function setSystemConfig(data) {
  return request({ url: `${baseUrl}/system/config`, method: 'put', data })
}

// 获取系统参数配置
export function getSystemConfig() {
  return request({ url: `${baseUrl}/system/config`, method: 'get' })
}

// 创建板块
export function createSection(data) {
  return request({ url: `${baseUrl}/layout/app/sections`, method: 'post', data })
}

// 修改板块属性
export function updateSection(sectionId, data) {
  return request({ url: `${baseUrl}/layout/app/sections/${sectionId}`, method: 'put', data })
}

// 删除板块
export function deleteSection(sectionId) {
  return request({ url: `${baseUrl}/layout/app/sections/${sectionId}`, method: 'delete' })
}

// 获取板块详情
export function getSectionDetail(sectionId) {
  return request({ url: `${baseUrl}/layout/app/sections/${sectionId}`, method: 'get' })
}

// 获取板块列表
export function getSectionList(params) {
  return request({ url: `${baseUrl}/layout/app/sections`, method: 'get', params })
}
