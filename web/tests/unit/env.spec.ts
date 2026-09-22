import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { isWebView2 } from '@/utils/env';

describe('env - 环境判断', () => {
  let originalChrome: any;
  let originalLocation: any;
  let originalPIM: any;

  beforeEach(() => {
    // 保存原始值
    originalChrome = (window as any).chrome;
    originalLocation = (window as any).location;
    originalPIM = (window as any).PIM_GetPlatform;
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    // 还原
    if (originalChrome === undefined) {
      delete (window as any).chrome;
    } else {
      (window as any).chrome = originalChrome;
    }
    if (originalLocation === undefined) {
      delete (window as any).location;
    } else {
      (window as any).location = originalLocation;
    }
    if (originalPIM === undefined) {
      delete (window as any).PIM_GetPlatform;
    } else {
      (window as any).PIM_GetPlatform = originalPIM;
    }
  });

  describe('isWebView2 - WebView2 环境判断', () => {
    describe('分支 1: window.chrome.webview 存在', () => {
      it('chrome.webview 存在时返回 true', () => {
        vi.stubGlobal('chrome', { webview: {} });
        expect(isWebView2()).toBe(true);
      });

      it('chrome 存在但 webview 不存在时,该分支不命中', () => {
        vi.stubGlobal('chrome', { runtime: {} });
        // 同时确保其它分支也不命中
        vi.stubGlobal('location', { search: '', href: 'http://localhost' });
        delete (window as any).PIM_GetPlatform;
        expect(isWebView2()).toBe(false);
      });

      it('chrome 不存在时,该分支不命中', () => {
        delete (window as any).chrome;
        vi.stubGlobal('location', { search: '', href: 'http://localhost' });
        delete (window as any).PIM_GetPlatform;
        expect(isWebView2()).toBe(false);
      });

      it('chrome.webview 为 falsy（null）时,该分支不命中', () => {
        vi.stubGlobal('chrome', { webview: null });
        vi.stubGlobal('location', { search: '', href: 'http://localhost' });
        delete (window as any).PIM_GetPlatform;
        expect(isWebView2()).toBe(false);
      });
    });

    describe('分支 2: URL 参数 clientType=CSPC', () => {
      beforeEach(() => {
        // 清除 chrome 分支
        delete (window as any).chrome;
        delete (window as any).PIM_GetPlatform;
      });

      it('?clientType=CSPC 时返回 true', () => {
        vi.stubGlobal('location', {
          search: '?clientType=CSPC',
          href: 'http://localhost/?clientType=CSPC',
        });
        expect(isWebView2()).toBe(true);
      });

      it('?clientType=other 时返回 false', () => {
        vi.stubGlobal('location', {
          search: '?clientType=other',
          href: 'http://localhost/?clientType=other',
        });
        expect(isWebView2()).toBe(false);
      });

      it('?other=CSPC 时（参数名不是 clientType）返回 false', () => {
        vi.stubGlobal('location', {
          search: '?other=CSPC',
          href: 'http://localhost/?other=CSPC',
        });
        expect(isWebView2()).toBe(false);
      });

      it('无查询参数时返回 false', () => {
        vi.stubGlobal('location', {
          search: '',
          href: 'http://localhost/',
        });
        expect(isWebView2()).toBe(false);
      });

      it('多个参数时也能识别 clientType=CSPC', () => {
        vi.stubGlobal('location', {
          search: '?foo=bar&clientType=CSPC&baz=1',
          href: 'http://localhost/?foo=bar&clientType=CSPC&baz=1',
        });
        expect(isWebView2()).toBe(true);
      });
    });

    describe('分支 3: PIM_GetPlatform 函数存在', () => {
      beforeEach(() => {
        delete (window as any).chrome;
        vi.stubGlobal('location', { search: '', href: 'http://localhost/' });
      });

      it('PIM_GetPlatform 是函数时返回 true', () => {
        vi.stubGlobal('PIM_GetPlatform', vi.fn(() => 'CSPC'));
        expect(isWebView2()).toBe(true);
      });

      it('PIM_GetPlatform 是字符串（非函数）时返回 false', () => {
        vi.stubGlobal('PIM_GetPlatform', 'CSPC');
        expect(isWebView2()).toBe(false);
      });

      it('PIM_GetPlatform 不存在时返回 false', () => {
        delete (window as any).PIM_GetPlatform;
        expect(isWebView2()).toBe(false);
      });
    });

    describe('三个分支都不命中时返回 false', () => {
      it('无 chrome、无 URL 参数、无 PIM_GetPlatform', () => {
        delete (window as any).chrome;
        delete (window as any).PIM_GetPlatform;
        vi.stubGlobal('location', { search: '', href: 'http://localhost/' });
        expect(isWebView2()).toBe(false);
      });
    });

    describe('优先级：分支1命中时不检查后续分支', () => {
      it('chrome.webview 存在 + URL 无 CSPC 也返回 true', () => {
        vi.stubGlobal('chrome', { webview: {} });
        vi.stubGlobal('location', { search: '', href: 'http://localhost/' });
        delete (window as any).PIM_GetPlatform;
        expect(isWebView2()).toBe(true);
      });
    });
  });
});
