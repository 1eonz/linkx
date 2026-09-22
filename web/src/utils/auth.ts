/**
 * 登录相关
 */
import Cookies from 'js-cookie';

const TokenKey = 'ICP-X-Token';
const TokenRefreshKey = 'ICP-X-Refresh-Token';
const DeviceIdKey = 'ICP-X-Refresh-Device-Id';
const IsLockScreen = 'ICP-X-Lock-Screen';

const demsLoginToken = 'demsLoginToken';

export const LOGIN = {
  clientId: 'CDC-1000',
  clientSecret: 'CDC-1000',
  grantType: 'password',
  redirectUri: '',
  scope: '',
  state: '',
};

export function getDeviceId() {
  return Cookies.get(DeviceIdKey);
}

export function setDeviceId(id) {
  return Cookies.set(DeviceIdKey, id);
}

export function getToken() {
  return localStorage.getItem(TokenKey);
}

export function setToken(token) {
  return localStorage.setItem(TokenKey, token);
}

export function removeToken() {
  localStorage.removeItem(TokenKey);
  localStorage.removeItem(demsLoginToken);
}

export function getRefreshToken() {
  return Cookies.get(TokenRefreshKey);
}

export function setRefreshToken(token) {
  return Cookies.set(TokenRefreshKey, token);
}

export function removeRefreshToken() {
  localStorage.removeItem(TokenRefreshKey);
}

export function setIsLockScreen(data) {
  // 存储登录页提示用户10分钟未操作弹窗开关数据
  return Cookies.set(IsLockScreen, data);
}

export function getIsLockScreen() {
  return Cookies.get(IsLockScreen);
}

// 权限check方法
export function authorityCheck(type, account) {
  for (const i in account) {
    if (account[i].key === type) {
      const status = account[i].value;
      return status;
    }
  }
}
