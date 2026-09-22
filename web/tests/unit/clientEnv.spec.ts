import { describe, it, expect, vi, afterEach } from 'vitest';

// clientEnv 模块加载时计算并缓存 UA / screen 信息
// 测试不同 UA 时需要 vi.resetModules() + 动态 import() 重新加载模块

interface ClientEnvModule {
  getBrowserInfo: () => string;
  getOSInfo: () => string;
  getScreenInfo: () => string;
  CLIENT_TYPE: Record<string, string>;
}

async function loadClientEnvWithUA(ua: string, screen = { width: 1920, height: 1080 }) {
  vi.resetModules();
  vi.stubGlobal('navigator', { userAgent: ua });
  vi.stubGlobal('screen', screen);
  const mod = (await import('@/utils/clientEnv')) as unknown as ClientEnvModule;
  return mod;
}

describe('clientEnv - 客户端环境信息', () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.resetModules();
  });

  describe('CLIENT_TYPE 常量', () => {
    it('包含正确的客户端类型枚举', async () => {
      const mod = await loadClientEnvWithUA('Mozilla/5.0');
      expect(mod.CLIENT_TYPE.PC).toBe('1');
      expect(mod.CLIENT_TYPE.CS).toBe('2');
      expect(mod.CLIENT_TYPE.H5).toBe('3');
      expect(mod.CLIENT_TYPE.ADMIN).toBe('4');
      expect(mod.CLIENT_TYPE.RESTFUL).toBe('5');
      expect(mod.CLIENT_TYPE.JS_SDK).toBe('6');
    });
  });

  describe('getBrowserInfo - 浏览器信息', () => {
    it('Chrome 浏览器', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
      );
      expect(mod.getBrowserInfo()).toBe('Chrome 120');
    });

    it('Edge 浏览器（优先于 Chrome 匹配）', async () => {
      // Edge UA 同时包含 Edg 和 Chrome,但 Edge 应优先匹配
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0',
      );
      expect(mod.getBrowserInfo()).toBe('Edge 120');
    });

    it('Firefox 浏览器（桌面版）', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0',
      );
      expect(mod.getBrowserInfo()).toBe('Firefox 121');
    });

    it('Safari 浏览器（不包含 Chrome）', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 Safari/605.1.15',
      );
      expect(mod.getBrowserInfo()).toBe('Safari 605');
    });

    it('iOS Chrome (CriOS)', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.6099.119 Mobile/15E148 Safari/604.1',
      );
      expect(mod.getBrowserInfo()).toBe('Chrome 120');
    });

    it('iOS Firefox (FxiOS)', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/121.0 Mobile/15E148 Safari/605.1.15',
      );
      expect(mod.getBrowserInfo()).toBe('Firefox 121');
    });

    it('未知浏览器返回空字符串', async () => {
      const mod = await loadClientEnvWithUA('Mozilla/5.0 (Custom Browser)');
      expect(mod.getBrowserInfo()).toBe('');
    });

    it('Edge 优先级高于 Chrome（即使 Chrome 在前）', async () => {
      // 验证匹配顺序：Edg → Chrome → CriOS → FxiOS → Safari → Firefox
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 Chrome/120.0 Safari/537.36 Edg/118.0',
      );
      expect(mod.getBrowserInfo()).toBe('Edge 118');
    });
  });

  describe('getOSInfo - 操作系统信息', () => {
    it('Windows 10', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0',
      );
      expect(mod.getOSInfo()).toBe('Windows 10');
    });

    it('Windows 8.1 (NT 6.3)', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 6.3; Win64; x64) Chrome/120.0',
      );
      expect(mod.getOSInfo()).toBe('Windows 8.1');
    });

    it('Windows 8 (NT 6.2)', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 6.2; Win64; x64) Chrome/120.0',
      );
      expect(mod.getOSInfo()).toBe('Windows 8');
    });

    it('Windows 7 (NT 6.1)', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 6.1; Win64; x64) Chrome/120.0',
      );
      expect(mod.getOSInfo()).toBe('Windows 7');
    });

    it('macOS', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) Safari/605.1.15',
      );
      expect(mod.getOSInfo()).toBe('macOS 10.15');
    });

    it('Android', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (Linux; Android 13; SM-S901B) AppleWebKit/537.36 Chrome/120.0 Mobile Safari/537.36',
      );
      expect(mod.getOSInfo()).toBe('Android 13');
    });

    it('iOS (iPhone OS)', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) Safari/604.1',
      );
      expect(mod.getOSInfo()).toBe('iOS 17.0');
    });

    it('未知 OS 返回空字符串', async () => {
      const mod = await loadClientEnvWithUA('Mozilla/5.0 (Unknown OS)');
      expect(mod.getOSInfo()).toBe('');
    });
  });

  describe('getScreenInfo - 屏幕信息', () => {
    it('返回 width x height 格式', async () => {
      const mod = await loadClientEnvWithUA('Mozilla/5.0', {
        width: 1920,
        height: 1080,
      });
      expect(mod.getScreenInfo()).toBe('1920x1080');
    });

    it('移动端屏幕尺寸', async () => {
      const mod = await loadClientEnvWithUA('Mozilla/5.0', {
        width: 375,
        height: 812,
      });
      expect(mod.getScreenInfo()).toBe('375x812');
    });
  });

  describe('模块加载时缓存机制', () => {
    it('同一模块实例多次调用返回相同缓存值', async () => {
      const mod = await loadClientEnvWithUA(
        'Mozilla/5.0 Chrome/120.0',
      );
      const v1 = mod.getBrowserInfo();
      const v2 = mod.getBrowserInfo();
      expect(v1).toBe(v2);
      expect(v1).toBe('Chrome 120');
    });

    it('UA 改变后需要重新 import 才能拿到新值（缓存机制）', async () => {
      // 第一次加载 Chrome
      const mod1 = await loadClientEnvWithUA('Mozilla/5.0 Chrome/120.0');
      expect(mod1.getBrowserInfo()).toBe('Chrome 120');

      // 第二次加载 Firefox
      const mod2 = await loadClientEnvWithUA(
        'Mozilla/5.0 (Windows NT 10.0; rv:121.0) Gecko/20100101 Firefox/121.0',
      );
      expect(mod2.getBrowserInfo()).toBe('Firefox 121');
      // 旧实例仍是旧缓存
      expect(mod1.getBrowserInfo()).toBe('Chrome 120');
    });
  });
});
