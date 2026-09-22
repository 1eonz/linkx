import request from '@/utils/request'

export function getAccountList(data) {
  return request({
    url: '/api/serviceAccount/list',
    method: 'post',
    data
  })
}

export function downloadAccounts() {
  return request({
    url: '/api/serviceAccount/download',
    method: 'post',
    responseType: 'blob'
  })
}

export function createAccount(data) {
  return request({
    url: '/api/serviceAccount/create',
    method: 'post',
    data
  })
}

export function getAccountById(id) {
  return request({
    url: '/api/serviceAccount/id',
    method: 'post',
    params: { id }
  })
}

export function updateAccount(data) {
  return request({
    url: '/api/serviceAccount/update',
    method: 'post',
    data
  })
}

export function deleteAccount(data) {
  return request({
    url: '/api/serviceAccount/delete',
    method: 'post',
    data
  })
}

export function getAccountTypeList() {
  return request({
    url: '/api/serviceType/list',
    method: 'post'
  })
}
export function getAccountConnect() {
  return request({
    url: '/api/serviceType/connect',
    method: 'post'
  })
}
export function getEquipmentConnect() {
  return request({
    url: '/api/serviceType/connect',
    method: 'post'
  })
}

