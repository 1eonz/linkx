import request from '@/utils/request'
import Cookies from 'js-cookie'
const baseUrl = '/auth/v1';
export function login(data) {
  return request({
    url: `${baseUrl}/oauth/v2/login`,
    method: 'post',
    data
  })
}

export function logout() {
  return request({
    url: `${baseUrl}/oauth/v2/logout`,
    method: 'post'
  })
}

export function keepalive() {
  return request({
    url: `${baseUrl}/oauth/v2/keepalive`,
    method: 'post'
  })
}

export function changePwd(data) {
  return request({
    url: `${baseUrl}/oauth/v2/changePwd`,
    method: 'post',
    data,
    timeout: 60000
  })
}

export function getRolePermissions() {
  return request({
    url: `${baseUrl}/oauth/v2/permissions`,
    method: 'post'
  })
}

export function getDeviceId() {
  return new Date().getTime()
}

export function setDeviceId(id) {
  return Cookies.set('ICP-X-Refresh-Device-Id', id)
}

export function getVersion() {
  return request({
    url: '/collaboration/v1/base/version',
    method: 'get'
  })
}
