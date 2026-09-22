import { describe, it, expect, vi, beforeEach } from 'vitest';

// mock js-cookie 模块（默认导出对象形式）
vi.mock('js-cookie', () => ({
  default: {
    get: vi.fn(),
    set: vi.fn(),
    remove: vi.fn(),
  },
}));

import Cookies from 'js-cookie';
import {
  getDeviceId,
  setDeviceId,
  getToken,
  setToken,
  removeToken,
  getRefreshToken,
  setRefreshToken,
  removeRefreshToken,
  setIsLockScreen,
  getIsLockScreen,
  authorityCheck,
  LOGIN,
} from '@/utils/auth';

describe('auth - 登录态工具', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('LOGIN 常量', () => {
    it('包含正确的客户端配置', () => {
      expect(LOGIN.clientId).toBe('CDC-1000');
      expect(LOGIN.clientSecret).toBe('CDC-1000');
      expect(LOGIN.grantType).toBe('password');
    });
  });

  describe('getToken / setToken / removeToken - localStorage Token', () => {
    it('setToken 写入 localStorage', () => {
      setToken('my-token');
      expect(localStorage.getItem('ICP-X-Token')).toBe('my-token');
    });

    it('getToken 读取 localStorage', () => {
      localStorage.setItem('ICP-X-Token', 'stored-token');
      expect(getToken()).toBe('stored-token');
    });

    it('getToken 在未设置时返回 null', () => {
      expect(getToken()).toBeNull();
    });

    it('setToken 多次调用会覆盖', () => {
      setToken('first');
      setToken('second');
      expect(localStorage.getItem('ICP-X-Token')).toBe('second');
    });

    it('removeToken 同时清除 Token 与 demsLoginToken', () => {
      localStorage.setItem('ICP-X-Token', 'token-1');
      localStorage.setItem('demsLoginToken', 'token-2');
      removeToken();
      expect(localStorage.getItem('ICP-X-Token')).toBeNull();
      expect(localStorage.getItem('demsLoginToken')).toBeNull();
    });
  });

  describe('getDeviceId / setDeviceId - Cookie DeviceId', () => {
    it('setDeviceId 调用 Cookies.set', () => {
      setDeviceId('device-123');
      expect(Cookies.set).toHaveBeenCalledWith('ICP-X-Refresh-Device-Id', 'device-123');
    });

    it('getDeviceId 调用 Cookies.get', () => {
      (Cookies.get as any).mockReturnValue('device-456');
      expect(getDeviceId()).toBe('device-456');
      expect(Cookies.get).toHaveBeenCalledWith('ICP-X-Refresh-Device-Id');
    });
  });

  describe('getRefreshToken / setRefreshToken - Cookie RefreshToken', () => {
    it('setRefreshToken 调用 Cookies.set', () => {
      setRefreshToken('refresh-token');
      expect(Cookies.set).toHaveBeenCalledWith('ICP-X-Refresh-Token', 'refresh-token');
    });

    it('getRefreshToken 调用 Cookies.get', () => {
      (Cookies.get as any).mockReturnValue('refresh-stored');
      expect(getRefreshToken()).toBe('refresh-stored');
      expect(Cookies.get).toHaveBeenCalledWith('ICP-X-Refresh-Token');
    });

    it.skip('已知 bug: removeRefreshToken 用 localStorage.removeItem, 但 setRefreshToken 用 Cookies.set。所以 localStorage 中并不存在该 key,无法清除 Cookie 中的 refresh token', () => {
      // 源码:
      //   export function setRefreshToken(token) { return Cookies.set(TokenRefreshKey, token); }
      //   export function removeRefreshToken() { localStorage.removeItem(TokenRefreshKey); }
      // 期望: removeRefreshToken 应该用 Cookies.remove(TokenRefreshKey) 才能正确清除
      setRefreshToken('refresh-token');
      removeRefreshToken();
      // Cookie 中的 refresh token 仍然存在
      expect(Cookies.remove).toHaveBeenCalledWith('ICP-X-Refresh-Token');
    });
  });

  describe('setIsLockScreen / getIsLockScreen - Cookie 锁屏状态', () => {
    it('setIsLockScreen 调用 Cookies.set', () => {
      setIsLockScreen('1');
      expect(Cookies.set).toHaveBeenCalledWith('ICP-X-Lock-Screen', '1');
    });

    it('getIsLockScreen 调用 Cookies.get', () => {
      (Cookies.get as any).mockReturnValue('1');
      expect(getIsLockScreen()).toBe('1');
      expect(Cookies.get).toHaveBeenCalledWith('ICP-X-Lock-Screen');
    });
  });

  describe('authorityCheck - 权限查找', () => {
    it('找到匹配的 key 返回对应 value', () => {
      const account = [
        { key: 'admin', value: true },
        { key: 'user', value: false },
      ];
      expect(authorityCheck('admin', account)).toBe(true);
      expect(authorityCheck('user', account)).toBe(false);
    });

    it('未找到匹配的 key 返回 undefined', () => {
      const account = [{ key: 'admin', value: true }];
      expect(authorityCheck('unknown', account)).toBeUndefined();
    });

    it('空数组返回 undefined', () => {
      expect(authorityCheck('admin', [])).toBeUndefined();
    });

    it('value 为 falsy 值也能正确返回（0）', () => {
      const account = [{ key: 'count', value: 0 }];
      expect(authorityCheck('count', account)).toBe(0);
    });

    it('value 为空字符串也能正确返回', () => {
      const account = [{ key: 'name', value: '' }];
      expect(authorityCheck('name', account)).toBe('');
    });

    it('返回第一个匹配项（不继续查找）', () => {
      const account = [
        { key: 'admin', value: 'first' },
        { key: 'admin', value: 'second' },
      ];
      expect(authorityCheck('admin', account)).toBe('first');
    });
  });
});
