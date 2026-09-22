import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

describe('utils/clientEnv.js - clientEnv', () => {
  let originalNavigator;
  let originalWindow;

  beforeEach(() => {
    // 备份原始 navigator / window
    originalNavigator = globalThis.navigator;
    originalWindow = globalThis.window;
    vi.resetModules();
  });

  afterEach(() => {
    // 恢复原始 navigator / window
    if (originalNavigator === undefined) {
      delete globalThis.navigator;
    } else {
      globalThis.navigator = originalNavigator;
    }
    if (originalWindow === undefined) {
      delete globalThis.window;
    } else {
      globalThis.window = originalWindow;
    }
  });

  // 工具：以指定 UA + screen 重新加载 clientEnv 模块
  async function loadClientEnvWithUA(userAgent, screenInfo = { width: 1920, height: 1080 }) {
    vi.stubGlobal('navigator', { userAgent });
    vi.stubGlobal('window', {
      screen: screenInfo,
    });

    const mod = await import('@/utils/clientEnv');
    return mod;
  }

  describe('CLIENT_TYPE 常量', () => {
    it('应包含全部 6 种客户端类型枚举', async () => {
      const { CLIENT_TYPE } = await loadClientEnvWithUA('Mozilla/5.0');

      expect(CLIENT_TYPE).toEqual({
        PC: '1',
        CS: '2',
        H5: '3',
        ADMIN: '4',
        RESTFUL: '5',
        JS_SDK: '6',
      });
    });

    it('各枚举值应为字符串类型', async () => {
      const { CLIENT_TYPE } = await loadClientEnvWithUA('Mozilla/5.0');

      Object.values(CLIENT_TYPE).forEach((v) => {
        expect(typeof v).toBe('string');
      });
    });
  });

  describe('getBrowserInfo - UA 分支', () => {
    it('Chrome UA 应返回 Chrome + 版本', async () => {
      const { getBrowserInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      );

      expect(getBrowserInfo()).toBe('Chrome 120');
    });

    it('Edge UA 应优先返回 Edge + 版本', async () => {
      const { getBrowserInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0',
      );

      expect(getBrowserInfo()).toBe('Edge 120');
    });

    it('Firefox UA 应返回 Firefox + 版本', async () => {
      const { getBrowserInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0',
      );

      expect(getBrowserInfo()).toBe('Firefox 121');
    });

    it('Safari UA 应返回 Safari + 版本', async () => {
      // Safari UA 不含 Chrome / Edg / Firefox
      const { getBrowserInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15',
      );

      expect(getBrowserInfo()).toBe('Safari 605');
    });

    it('iOS Chrome (CriOS) UA 应返回 Chrome + 版本', async () => {
      const { getBrowserInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.6099.119 Mobile/15E148 Safari/604.1',
      );

      expect(getBrowserInfo()).toBe('Chrome 120');
    });

    it('iOS Firefox (FxiOS) UA 应返回 Firefox + 版本', async () => {
      const { getBrowserInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/121.0 Mobile/15E148 Safari/605.1.15',
      );

      expect(getBrowserInfo()).toBe('Firefox 121');
    });

    it('未知 UA 应返回空字符串', async () => {
      const { getBrowserInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (compatible; CustomBot/1.0)',
      );

      expect(getBrowserInfo()).toBe('');
    });
  });

  describe('getOSInfo - UA 分支', () => {
    it('Windows NT 10.0 应返回 Windows 10', async () => {
      const { getOSInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36',
      );

      expect(getOSInfo()).toBe('Windows 10');
    });

    it('Windows NT 6.3 应返回 Windows 8.1', async () => {
      const { getOSInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 6.3; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36',
      );

      expect(getOSInfo()).toBe('Windows 8.1');
    });

    it('Windows NT 6.2 应返回 Windows 8', async () => {
      const { getOSInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36',
      );

      expect(getOSInfo()).toBe('Windows 8');
    });

    it('Windows NT 6.1 应返回 Windows 7', async () => {
      const { getOSInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 Chrome/120.0.0.0 Safari/537.36',
      );

      expect(getOSInfo()).toBe('Windows 7');
    });

    it('macOS UA 应返回 macOS + 版本', async () => {
      const { getOSInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15',
      );

      expect(getOSInfo()).toBe('macOS 10.15');
    });

    it('Android UA 应返回 Android + 版本', async () => {
      const { getOSInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36',
      );

      expect(getOSInfo()).toBe('Android 13');
    });

    it('iOS UA 应返回 iOS + 版本', async () => {
      const { getOSInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1',
      );

      expect(getOSInfo()).toBe('iOS 17.0');
    });

    it('未知 UA 应返回空字符串', async () => {
      const { getOSInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (compatible; CustomBot/1.0)',
      );

      expect(getOSInfo()).toBe('');
    });
  });

  describe('getScreenInfo', () => {
    it('应返回 window.screen 的 width x height', async () => {
      const { getScreenInfo } = await loadClientEnvWithUA('Mozilla/5.0', {
        width: 1920,
        height: 1080,
      });

      expect(getScreenInfo()).toBe('1920x1080');
    });

    it('不同分辨率应正确返回', async () => {
      const { getScreenInfo } = await loadClientEnvWithUA('Mozilla/5.0', {
        width: 375,
        height: 812,
      });

      expect(getScreenInfo()).toBe('375x812');
    });
  });

  describe('缓存行为', () => {
    it('getBrowserInfo 多次调用应返回同一缓存值', async () => {
      const { getBrowserInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0 Safari/537.36',
      );

      const a = getBrowserInfo();
      const b = getBrowserInfo();

      expect(a).toBe(b);
      expect(a).toBe('Chrome 120');
    });

    it('getOSInfo 多次调用应返回同一缓存值', async () => {
      const { getOSInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0',
      );

      const a = getOSInfo();
      const b = getOSInfo();

      expect(a).toBe(b);
      expect(a).toBe('Windows 10');
    });

    it('getScreenInfo 多次调用应返回同一缓存值', async () => {
      const { getScreenInfo } = await loadClientEnvWithUA('Mozilla/5.0', {
        width: 1280,
        height: 720,
      });

      const a = getScreenInfo();
      const b = getScreenInfo();

      expect(a).toBe(b);
      expect(a).toBe('1280x720');
    });
  });

  describe('getVersionCode', () => {
    it('versionCode 未设置时应返回 0（Number(null)）', async () => {
      const { getVersionCode } = await loadClientEnvWithUA('Mozilla/5.0');

      expect(getVersionCode()).toBe(0);
    });

    it('preheatOSInfo 设置 versionCode 后 getVersionCode 应返回对应 Number', async () => {
      const { preheatOSInfo, getVersionCode } = await loadClientEnvWithUA('Mozilla/5.0');

      // 手动模拟 SDK 提供版本号
      vi.stubGlobal('window', {
        screen: { width: 1920, height: 1080 },
        WeSpaceSDK: {
          getVersion: () =>
            Promise.resolve({
              deviceType: 'Windows-PC',
              versionCode: '20241001',
            }),
        },
      });

      preheatOSInfo();
      // 等待 Promise 解析
      await vi.waitFor(() => {
        expect(getVersionCode()).toBe(20241001);
      });
    });

    it('preheatOSInfo 应更新 getOSInfo 返回值（deviceType）', async () => {
      const { preheatOSInfo, getOSInfo } = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0',
      );

      // 初始为 UA 解析
      expect(getOSInfo()).toBe('Windows 10');

      vi.stubGlobal('window', {
        screen: { width: 1920, height: 1080 },
        WeSpaceSDK: {
          getVersion: () =>
            Promise.resolve({
              deviceType: 'Custom-Device',
              versionCode: '1',
            }),
        },
      });

      preheatOSInfo();

      await vi.waitFor(() => {
        expect(getOSInfo()).toBe('Custom-Device');
      });
    });

    it('无 WeSpaceSDK 时 preheatOSInfo 不报错', async () => {
      const { preheatOSInfo } = await loadClientEnvWithUA('Mozilla/5.0');

      expect(() => preheatOSInfo()).not.toThrow();
    });
  });
});
