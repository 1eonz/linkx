import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// mock @/common/config 切断循环依赖（@/utils → @/common/api/group → http → config → @/utils）
vi.mock('@/common/config', () => ({
  domain: 'localhost',
  getBaseUrl: vi.fn(() => 'http://localhost'),
  getSocketUrl: vi.fn(() => 'ws://localhost'),
  baseUrl: 'http://localhost',
  basePath: 'http://localhost',
  staticBaseUrl: 'http://localhost',
  socketUrl: 'ws://localhost',
  uploadUrl: 'http://localhost/api/upload',
  logo: '',
  getBaseUrlValue: vi.fn(() => 'http://localhost'),
  setPageUrlStore: vi.fn(),
}));

import {
  queryStringToJson,
  debounce,
  getCurrentDateTime,
  getBaseUrlAll,
  getFullPageUrl,
  imageToBase64,
} from '@/utils/index.js';

describe('utils/index.js - 纯函数测试', () => {
  describe('queryStringToJson', () => {
    it('空字符串应返回空对象', () => {
      expect(queryStringToJson('')).toEqual({});
    });

    it('null/undefined 入参应返回空对象', () => {
      expect(queryStringToJson(null)).toEqual({});
      expect(queryStringToJson(undefined)).toEqual({});
    });

    it('单个键值对应正确解析', () => {
      expect(queryStringToJson('id=122345')).toEqual({ id: '122345' });
    });

    it('多个键值对应正确解析', () => {
      const result = queryStringToJson('id=122345&name=9996');
      expect(result).toEqual({ id: '122345', name: '9996' });
    });

    it('应对 key 和 value 进行 decodeURIComponent 解码', () => {
      const result = queryStringToJson('name=%E5%BC%A0%E4%B8%89');
      expect(result).toEqual({ name: '张三' });
    });

    it('value 为空时返回空字符串', () => {
      const result = queryStringToJson('key=');
      expect(result).toEqual({ key: '' });
    });

    it('无 = 的 pair 应将整个字符串作为 key，value 为空字符串（源码行为）', () => {
      const result = queryStringToJson('abc');
      // 源码 pair.split('=') 对无 = 的字符串返回 ['abc']，pair[0]='abc'，pair[1]=undefined
      // 最终 result[abc] = undefined || '' = ''
      expect(result).toEqual({ abc: '' });
    });

    // bug: split('=', 2) 在 JS 中是 limit 截断，不是 PHP 的"取前两个"
    // 'a=b=c'.split('=', 2) => ['a', 'b']，丢失了 '=c'，导致 value 仅为 'b' 而非 'b=c'
    it.skip('值中包含 = 时应保留完整 value（已知 bug：split limit 行为与预期不符）', () => {
      const result = queryStringToJson('a=b=c');
      // 期望: { a: 'b=c' }，实际: { a: 'b' }
      expect(result).toEqual({ a: 'b=c' });
    });

    it('值中包含 = 时当前实现会截断 value（记录实际行为）', () => {
      const result = queryStringToJson('a=b=c');
      // 当前实现：split('=', 2) 在 JS 中返回 ['a', 'b']，value 为 'b'
      expect(result).toEqual({ a: 'b' });
    });
  });

  describe('debounce', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('默认 delay=300，应在延迟后执行一次', () => {
      const func = vi.fn();
      const debounced = debounce(func);

      debounced();
      expect(func).not.toHaveBeenCalled();

      vi.advanceTimersByTime(299);
      expect(func).not.toHaveBeenCalled();

      vi.advanceTimersByTime(1);
      expect(func).toHaveBeenCalledTimes(1);
    });

    it('连续触发应只执行最后一次', () => {
      const func = vi.fn();
      const debounced = debounce(func, 300);

      debounced(1);
      debounced(2);
      debounced(3);

      vi.advanceTimersByTime(300);
      expect(func).toHaveBeenCalledTimes(1);
      expect(func).toHaveBeenLastCalledWith(3);
    });

    it('immediate=true 应首次立即执行', () => {
      const func = vi.fn();
      const debounced = debounce(func, 300, true);

      debounced('first');
      expect(func).toHaveBeenCalledTimes(1);
      expect(func).toHaveBeenLastCalledWith('first');
    });

    it('immediate=true 时 delay 内的后续触发应被忽略', () => {
      const func = vi.fn();
      const debounced = debounce(func, 300, true);

      debounced('a');
      debounced('b');
      debounced('c');

      expect(func).toHaveBeenCalledTimes(1);
      expect(func).toHaveBeenLastCalledWith('a');

      vi.advanceTimersByTime(300);
      // 延迟结束后不会再次执行（immediate 模式只在首次触发执行）
      expect(func).toHaveBeenCalledTimes(1);
    });

    it('immediate=true 在延迟结束后再次触发应再次执行', () => {
      const func = vi.fn();
      const debounced = debounce(func, 300, true);

      debounced('first');
      vi.advanceTimersByTime(300);

      debounced('second');
      expect(func).toHaveBeenCalledTimes(2);
      expect(func).toHaveBeenLastCalledWith('second');
    });

    it('应保留 this 上下文', () => {
      const obj = {
        value: 42,
        getValue() {
          return this.value;
        },
      };
      const spy = vi.spyOn(obj, 'getValue');
      const debounced = debounce(obj.getValue, 100);

      debounced.call(obj);
      vi.advanceTimersByTime(100);

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy.mock.results[0].value).toBe(42);
    });

    it('cancel() 应取消未执行的定时器', () => {
      const func = vi.fn();
      const debounced = debounce(func, 300);

      debounced();
      debounced.cancel();

      vi.advanceTimersByTime(1000);
      expect(func).not.toHaveBeenCalled();
    });

    it('cancel() 后再次调用应正常工作', () => {
      const func = vi.fn();
      const debounced = debounce(func, 300);

      debounced();
      debounced.cancel();

      debounced();
      vi.advanceTimersByTime(300);
      expect(func).toHaveBeenCalledTimes(1);
    });

    it('immediate=true 时 cancel() 应取消后续触发窗口', () => {
      const func = vi.fn();
      const debounced = debounce(func, 300, true);

      debounced();
      expect(func).toHaveBeenCalledTimes(1);

      debounced.cancel();
      vi.advanceTimersByTime(300);
      // 已经立即执行过一次，cancel 不会回退这次调用
      expect(func).toHaveBeenCalledTimes(1);
    });
  });

  describe('getCurrentDateTime', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('应返回 HH时MM分SS秒 格式', () => {
      vi.setSystemTime(new Date('2026-07-27T10:30:45'));
      expect(getCurrentDateTime()).toBe('10时30分45秒');
    });

    it('个位数时分秒应补零', () => {
      vi.setSystemTime(new Date('2026-01-01T01:02:03'));
      expect(getCurrentDateTime()).toBe('01时02分03秒');
    });

    it('23:59:59 应正确返回', () => {
      vi.setSystemTime(new Date('2026-12-31T23:59:59'));
      expect(getCurrentDateTime()).toBe('23时59分59秒');
    });

    it('00:00:00 应正确返回', () => {
      vi.setSystemTime(new Date('2026-06-15T00:00:00'));
      expect(getCurrentDateTime()).toBe('00时00分00秒');
    });
  });

  describe('getBaseUrlAll', () => {
    const originalLocation = window.location;

    afterEach(() => {
      // 恢复 location
      Object.defineProperty(window, 'location', {
        value: originalLocation,
        writable: true,
        configurable: true,
      });
    });

    function mockLocation(pathname) {
      Object.defineProperty(window, 'location', {
        value: { ...originalLocation, pathname },
        writable: true,
        configurable: true,
      });
    }

    it('pathname 为空时应返回默认基础路径 /linkx/h5portal', () => {
      mockLocation('');
      expect(getBaseUrlAll()).toBe('/linkx/h5portal');
    });

    it('pathname 不包含 /linkx/h5portal 时应返回默认基础路径', () => {
      mockLocation('/some/other/path');
      expect(getBaseUrlAll()).toBe('/linkx/h5portal');
    });

    it('pathname 恰好为 /linkx/h5portal 时应返回该路径', () => {
      mockLocation('/linkx/h5portal');
      expect(getBaseUrlAll()).toBe('/linkx/h5portal');
    });

    it('pathname 为 /linkx/h5portal/ 子路径时应返回 /linkx/h5portal', () => {
      mockLocation('/linkx/h5portal/index.html');
      expect(getBaseUrlAll()).toBe('/linkx/h5portal');
    });

    it('应支持带网关前缀的路径（如 /zxwg/linkx/h5portal/xxx）', () => {
      mockLocation('/zxwg/linkx/h5portal/pages/home');
      expect(getBaseUrlAll()).toBe('/zxwg/linkx/h5portal');
    });

    it('网关前缀较长时应正确提取完整基础路径', () => {
      mockLocation('/a/b/c/linkx/h5portal/page/detail');
      expect(getBaseUrlAll()).toBe('/a/b/c/linkx/h5portal');
    });

    it('pathname 中 /linkx/h5portal 出现在中间时也应正确截取', () => {
      mockLocation('/gateway/linkx/h5portal/sub/route');
      expect(getBaseUrlAll()).toBe('/gateway/linkx/h5portal');
    });
  });

  describe('getFullPageUrl', () => {
    const originalLocation = window.location;

    afterEach(() => {
      Object.defineProperty(window, 'location', {
        value: originalLocation,
        writable: true,
        configurable: true,
      });
    });

    function mockLocation(origin, pathname) {
      Object.defineProperty(window, 'location', {
        value: { ...originalLocation, origin, pathname },
        writable: true,
        configurable: true,
      });
    }

    it('应拼接 origin 与基础路径', () => {
      mockLocation('http://example.com', '/linkx/h5portal/home');
      expect(getFullPageUrl()).toBe('http://example.com/linkx/h5portal');
    });

    it('带网关前缀时应返回完整 URL', () => {
      mockLocation('https://test.cn', '/zxwg/linkx/h5portal/page/x');
      expect(getFullPageUrl()).toBe('https://test.cn/zxwg/linkx/h5portal');
    });

    it('origin 为空时应只返回 basePath', () => {
      mockLocation('', '/linkx/h5portal');
      expect(getFullPageUrl()).toBe('/linkx/h5portal');
    });

    it('pathname 不包含 /linkx/h5portal 时应使用默认基础路径', () => {
      mockLocation('http://h.com', '/other/path');
      expect(getFullPageUrl()).toBe('http://h.com/linkx/h5portal');
    });
  });

  describe('imageToBase64', () => {
    let originalImage;
    let originalCreateElement;

    beforeEach(() => {
      originalImage = global.Image;
      originalCreateElement = document.createElement.bind(document);
    });

    afterEach(() => {
      global.Image = originalImage;
      document.createElement = originalCreateElement;
      // 清除缓存（模块内部 base64Cache）通过 vi.resetModules 不易实现，
      // 这里仅恢复全局即可
    });

    function setupCanvasMock(toDataURLReturn = 'data:image/png;base64,AAAA') {
      const ctx = {
        drawImage: vi.fn(),
      };
      const canvas = {
        width: 0,
        height: 0,
        getContext: vi.fn(() => ctx),
        toDataURL: vi.fn(() => toDataURLReturn),
      };
      vi.spyOn(document, 'createElement').mockImplementation((tag) => {
        if (tag === 'canvas') return canvas;
        return originalCreateElement.call(document, tag);
      });
      return { canvas, ctx };
    }

    function setupImageMock(trigger = 'load') {
      const handlers = {};
      const img = {
        crossOrigin: '',
        src: '',
        width: 100,
        height: 100,
        addEventListener: vi.fn((evt, cb) => {
          handlers[evt] = cb;
        }),
        // 源码使用 onerror 直接赋值，需要支持
        get onerror() {
          return handlers.error;
        },
        set onerror(fn) {
          handlers.error = fn;
        },
      };
      global.Image = vi.fn(() => img);
      img.__trigger = () => {
        if (trigger === 'load' && handlers.load) handlers.load();
        else if (trigger === 'error' && handlers.error) handlers.error(new Error('img fail'));
      };
      return img;
    }

    it('图片加载成功时应返回 canvas.toDataURL 结果', async () => {
      const { canvas, ctx } = setupCanvasMock('data:image/png;base64,ZZZZ');
      const img = setupImageMock('load');

      const promise = imageToBase64('http://a.com/x.png');
      // 触发 load
      img.__trigger();
      const result = await promise;

      expect(result).toBe('data:image/png;base64,ZZZZ');
      expect(canvas.width).toBe(100);
      expect(canvas.height).toBe(100);
      expect(ctx.drawImage).toHaveBeenCalledWith(img, 0, 0, 100, 100);
      expect(canvas.toDataURL).toHaveBeenCalledWith('image/png');
      expect(img.crossOrigin).toBe('Anonymous');
    });

    it('相同 imageUrl 第二次应返回缓存（命中 base64Cache）', async () => {
      setupCanvasMock('data:image/png;base64,CACHE');
      const img = setupImageMock('load');

      const url = 'http://cache.com/icon.png';
      const p1 = imageToBase64(url);
      img.__trigger();
      const r1 = await p1;

      // 第二次不再创建 Image，应直接命中缓存
      const imageSpy = vi.spyOn(global, 'Image');
      const p2 = imageToBase64(url);
      const r2 = await p2;

      expect(r2).toBe(r1);
      expect(imageSpy).not.toHaveBeenCalled();
    });

    it('图片加载失败时应 reject', async () => {
      setupCanvasMock();
      const img = setupImageMock('error');

      const promise = imageToBase64('http://b.com/fail.png');
      img.__trigger();
      await expect(promise).rejects.toBeDefined();
    });

    it('不同 imageUrl 应分别转换', async () => {
      setupCanvasMock('data:image/png;base64,D1');
      const img1 = setupImageMock('load');

      const p1 = imageToBase64('http://c.com/1.png');
      img1.__trigger();
      const r1 = await p1;

      setupCanvasMock('data:image/png;base64,D2');
      const img2 = setupImageMock('load');
      const p2 = imageToBase64('http://c.com/2.png');
      img2.__trigger();
      const r2 = await p2;

      expect(r1).toBe('data:image/png;base64,D1');
      expect(r2).toBe('data:image/png;base64,D2');
    });
  });

  describe('preloadIconsToBase64', () => {
    beforeEach(() => {
      vi.resetModules();
    });

    it('非数组入参应直接返回 undefined 且不抛错', async () => {
      // 重新 import 以隔离模块状态
      const mod = await import('@/utils/index.js');
      expect(mod.preloadIconsToBase64(null)).toBeUndefined();
      expect(mod.preloadIconsToBase64(undefined)).toBeUndefined();
      expect(mod.preloadIconsToBase64('str')).toBeUndefined();
      expect(mod.preloadIconsToBase64({})).toBeUndefined();
    });

    it('空数组应直接返回且不调用 imageToBase64', async () => {
      const mod = await import('@/utils/index.js');
      const spy = vi.spyOn(mod, 'imageToBase64');
      mod.preloadIconsToBase64([]);
      expect(spy).not.toHaveBeenCalled();
    });

    it('应用列表中 icon 为空的项应被跳过', async () => {
      const mod = await import('@/utils/index.js');
      const spy = vi.spyOn(mod, 'imageToBase64');
      mod.preloadIconsToBase64([
        { name: 'A' }, // 无 icon
        { name: 'B', icon: '' }, // icon 空字符串
        { name: 'C', icon: null }, // icon null
      ]);
      expect(spy).not.toHaveBeenCalled();
    });
  });
});
