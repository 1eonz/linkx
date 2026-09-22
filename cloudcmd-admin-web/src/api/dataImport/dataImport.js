import request from '@/utils/request'

export function getSheets(file) {
  return request({
    url: '/api/data/getSheets',
    method: 'post',
    data: file
  })
}

export function downloadModal() {
  return request({
    url: '/api/data/export',
    method: 'post',
    responseType: 'blob'
  })
}

export function exportData() {
  return request({
    url: '/api/data/export/data',
    method: 'post',
    responseType: 'blob',
    timeout: 60 * 1000
  })
}

export function downloadResult() {
  return request({
    url: '/api/data/result',
    method: 'post',
    responseType: 'blob'
  })
}

export function importData(data) {
  return request({
    url: '/api/data/import',
    method: 'post',
    timeout: '0',
    data
  })
}

export function importGetProgress() {
  return request({
    url: '/api/data/getProgress',
    method: 'post'
  })
}
export function synchronizationData() {
  return request({
    url: '/api/resource/synchronization/data',
    timeout: '0',
    method: 'post'
  })
}

export function synchronizationGetProcess() {
  return request({
    url: '/api/resource/synchronization/getProcess',
    method: 'post'
  })
}

export function querydataRemotes() {
  return request({
    url: '/api/data/remotes',
    method: 'get'
  })
}

export function dataCopy(data) {
  return request({
    url: '/api/data/copy/' + data,
    method: 'post'
  })
}

export function dataGetCopyProgress() {
  return request({
    url: '/api/data/getCopyProgress',
    method: 'post'
  })
}
