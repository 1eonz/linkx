import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest'

describe('utils/clientEnv.js', () => {
  let originalNavigator
  let originalWindow

  beforeEach(() => {
    // 备份原始值
    originalNavigator = globalThis.navigator
    originalWindow = globalThis.window
    vi.resetModules()
  })

  afterEach(() => {
    // 恢复
    if (originalNavigator !== undefined) {
      Object.defineProperty(globalThis, 'navigator', {
        value: originalNavigator,
        configurable: true,
        writable: true
      })
    } else {
      Object.defineProperty(globalThis, 'navigator', {
        value: { userAgent: '' },
        configurable: true,
        writable: true
      })
    }
    if (originalWindow !== undefined) {
      globalThis.window = originalWindow
    }
    vi.unstubAllGlobals()
    vi.resetModules()
  })

  // 辅助函数：stub navigator.userAgent 后重新加载模块
  async function loadModuleWithUA(ua) {
    vi.stubGlobal('navigator', { userAgent: ua })
    vi.resetModules()
    return await import('@/utils/clientEnv')
  }

  describe('CLIENT_TYPE 常量', () => {
    it('包含 6 个客户端类型枚举', async () => {
      const mod = await loadModuleWithUA('')
      expect(mod.CLIENT_TYPE).toEqual({
        PC: '1',
        CS: '2',
        H5: '3',
        ADMIN: '4',
        RESTFUL: '5',
        JS_SDK: '6'
      })
    })
  })

  describe('getBrowserInfo - 浏览器解析', () => {
    it('Edge UA 应解析为 "Edge <version>"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0'
      )
      expect(mod.getBrowserInfo()).toBe('Edge 120')
    })

    it('Chrome UA 应解析为 "Chrome <version>"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36'
      )
      expect(mod.getBrowserInfo()).toBe('Chrome 119')
    })

    it('iOS Chrome UA (CriOS) 应解析为 "Chrome <version>"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) CriOS/120.0.6099.119 Mobile/15E148 Safari/604.1'
      )
      expect(mod.getBrowserInfo()).toBe('Chrome 120')
    })

    it('iOS Firefox UA (FxiOS) 应解析为 "Firefox <version>"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) FxiOS/118.0 Mobile/15E148 Safari/605.1.15'
      )
      expect(mod.getBrowserInfo()).toBe('Firefox 118')
    })

    it('桌面 Firefox UA 应解析为 "Firefox <version>"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:118.0) Gecko/20100101 Firefox/118.0'
      )
      // Firefox UA 中无 Chrome/，但有 Firefox/ → 命中 firefoxMatch
      expect(mod.getBrowserInfo()).toBe('Firefox 118')
    })

    it('Safari UA（无 Chrome/Edg/Firefox）应解析为 "Safari <version>"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Safari/605.1.15'
      )
      // 该 UA 含 Safari/ 但不含 Chrome/Edg/FxiOS/Firefox/ → 命中 safariMatch
      expect(mod.getBrowserInfo()).toBe('Safari 605')
    })

    it('无法识别的 UA 返回空字符串', async () => {
      const mod = await loadModuleWithUA('curl/7.79.1')
      expect(mod.getBrowserInfo()).toBe('')
    })
  })

  describe('getOSInfo - 操作系统解析', () => {
    it('Windows 10 UA 应解析为 "Windows 10"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/119.0.0.0 Safari/537.36'
      )
      expect(mod.getOSInfo()).toBe('Windows 10')
    })

    it('Windows 8.1 UA 应解析为 "Windows 8.1"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Windows NT 6.3; Trident/7.0; rv:11.0) like Gecko'
      )
      expect(mod.getOSInfo()).toBe('Windows 8.1')
    })

    it('Windows 8 UA 应解析为 "Windows 8"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36'
      )
      expect(mod.getOSInfo()).toBe('Windows 8')
    })

    it('Windows 7 UA 应解析为 "Windows 7"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36'
      )
      expect(mod.getOSInfo()).toBe('Windows 7')
    })

    it('macOS UA 应解析为 "macOS <version>"（下划线转为点）', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 Safari/605.1.15'
      )
      expect(mod.getOSInfo()).toBe('macOS 10.15')
    })

    it('Android UA 应解析为 "Android <version>"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 Chrome/119.0.0.0 Mobile Safari/537.36'
      )
      expect(mod.getOSInfo()).toBe('Android 13')
    })

    it('iOS UA 应解析为 "iOS <version>"', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (iPhone; CPU iPhone OS 16_0 like Mac OS X) AppleWebKit/605.1.15 Mobile/15E148 Safari/604.1'
      )
      expect(mod.getOSInfo()).toBe('iOS 16.0')
    })

    it('无法识别 OS 的 UA 返回空字符串', async () => {
      const mod = await loadModuleWithUA('curl/7.79.1')
      expect(mod.getOSInfo()).toBe('')
    })
  })

  describe('getScreenInfo - 屏幕尺寸', () => {
    it('window 存在时返回 "WxH" 格式', async () => {
      // happy-dom 默认有 window.screen，stub 一个具体尺寸
      vi.stubGlobal('window', {
        screen: { width: 1920, height: 1080 }
      })
      vi.resetModules()
      const mod = await import('@/utils/clientEnv')
      expect(mod.getScreenInfo()).toBe('1920x1080')
    })

    it('window 不存在时返回空字符串（SSR 场景）', async () => {
      // 临时移除 window
      const original = globalThis.window
      // eslint-disable-next-line no-global-assign
      delete globalThis.window
      vi.resetModules()
      const mod = await import('@/utils/clientEnv')
      expect(mod.getScreenInfo()).toBe('')
      globalThis.window = original
    })
  })

  describe('模块缓存特性', () => {
    it('多次调用 getBrowserInfo 返回同一缓存值', async () => {
      const mod = await loadModuleWithUA(
        'Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/119.0.0.0 Safari/537.36'
      )
      const r1 = mod.getBrowserInfo()
      const r2 = mod.getBrowserInfo()
      expect(r1).toBe(r2)
      expect(r1).toBe('Chrome 119')
    })
  })
})
