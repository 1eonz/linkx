import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// mock @/plugins/map，避免触发地图副作用
const { mockMapManager, mockGetMapConfig, mockImageToBase64 } = vi.hoisted(() => ({
  mockMapManager: {
    get: vi.fn(),
  },
  mockGetMapConfig: vi.fn(() => ({})),
  mockImageToBase64: vi.fn(),
}));

vi.mock('@/plugins/map', () => ({
  mapManager: mockMapManager,
  getMapConfig: mockGetMapConfig,
}));

// mock @/utils 切断循环依赖并控制 imageToBase64 行为
vi.mock('@/utils', () => ({
  imageToBase64: mockImageToBase64,
}));

// mock @/common/config 切断循环依赖
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
  createImageMarker,
  debounce,
  getCachedImageBase64,
  flyToCenter,
  createMockPoints,
  renderClusterMarker,
  renderDeviceLayers,
  processInBatches,
  scheduleIdleWork,
} from '@/pages/map/utils.js';

describe('pages/map/utils.js - createImageMarker + debounce', () => {
  describe('createImageMarker', () => {
    it('应返回 HTML 字符串', () => {
      const result = createImageMarker('https://example.com/icon.png');
      expect(typeof result).toBe('string');
      expect(result.startsWith('<div')).toBe(true);
    });

    it('HTML 中应包含传入的 imageUrl', () => {
      const url = 'https://example.com/avatar.png';
      const result = createImageMarker(url);
      expect(result).toContain(url);
    });

    it('应包含 img 标签', () => {
      const result = createImageMarker('icon.png');
      expect(result).toContain('<img');
      expect(result).toContain('object-fit:contain');
    });

    it('isOnline=true（默认）应使用绿色圆点', () => {
      const result = createImageMarker('icon.png');
      expect(result).toContain('rgba(67, 207, 124, 1)');
    });

    it('isOnline=true 时不应使用灰色圆点', () => {
      const result = createImageMarker('icon.png', true);
      expect(result).not.toContain('rgba(166, 169, 171, 1)');
    });

    it('isOnline=false 应使用灰色圆点', () => {
      const result = createImageMarker('icon.png', false);
      expect(result).toContain('rgba(166, 169, 171, 1)');
    });

    it('isOnline=false 时不应使用绿色圆点', () => {
      const result = createImageMarker('icon.png', false);
      expect(result).not.toContain('rgba(67, 207, 124, 1)');
    });

    it('应包含 32x32 的尺寸设置', () => {
      const result = createImageMarker('icon.png');
      expect(result).toContain('width:32px');
      expect(result).toContain('height:32px');
    });

    it('应包含圆点样式（8px 圆点 + 白色边框）', () => {
      const result = createImageMarker('icon.png', true);
      expect(result).toContain('width:8px');
      expect(result).toContain('height:8px');
      expect(result).toContain('border:1px solid #fff');
      expect(result).toContain('border-radius:50%');
    });

    it('在线与离线生成的 HTML 仅颜色不同（长度可能因颜色值不同而略有差异）', () => {
      const online = createImageMarker('icon.png', true);
      const offline = createImageMarker('icon.png', false);
      // 两者都应是有效 HTML 字符串
      expect(typeof online).toBe('string');
      expect(typeof offline).toBe('string');
      // 在线状态应包含在线颜色标记
      expect(online).not.toBe(offline);
    });

    it('未传 isOnline 参数时应默认为在线状态', () => {
      const defaultResult = createImageMarker('icon.png');
      const onlineResult = createImageMarker('icon.png', true);
      expect(defaultResult).toBe(onlineResult);
    });

    it('不同 imageUrl 应生成不同的 HTML', () => {
      const r1 = createImageMarker('a.png');
      const r2 = createImageMarker('b.png');
      expect(r1).not.toBe(r2);
    });

    it('空字符串 imageUrl 应正常生成 HTML（不做校验）', () => {
      const result = createImageMarker('');
      expect(result).toContain('src=""');
    });
  });

  describe('debounce', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('应在延迟后执行一次', () => {
      const func = vi.fn();
      const debounced = debounce(func, 300);

      debounced();
      expect(func).not.toHaveBeenCalled();

      vi.advanceTimersByTime(300);
      expect(func).toHaveBeenCalledTimes(1);
    });

    it('连续多次触发应只执行最后一次', () => {
      const func = vi.fn();
      const debounced = debounce(func, 200);

      debounced(1);
      debounced(2);
      debounced(3);

      vi.advanceTimersByTime(200);
      expect(func).toHaveBeenCalledTimes(1);
      expect(func).toHaveBeenLastCalledWith(3);
    });

    it('在延迟内再次触发应重新计时', () => {
      const func = vi.fn();
      const debounced = debounce(func, 300);

      debounced();
      vi.advanceTimersByTime(200);
      expect(func).not.toHaveBeenCalled();

      debounced();
      vi.advanceTimersByTime(200);
      expect(func).not.toHaveBeenCalled();

      vi.advanceTimersByTime(100);
      expect(func).toHaveBeenCalledTimes(1);
    });

    it('应保留 this 上下文', () => {
      const obj = {
        value: 99,
        method() {
          return this.value;
        },
      };
      const spy = vi.spyOn(obj, 'method');
      const debounced = debounce(obj.method, 100);

      debounced.call(obj);
      vi.advanceTimersByTime(100);

      expect(spy).toHaveBeenCalledTimes(1);
      expect(spy.mock.results[0].value).toBe(99);
    });

    it('应正确传递参数', () => {
      const func = vi.fn();
      const debounced = debounce(func, 100);

      debounced('a', 'b', 'c');
      vi.advanceTimersByTime(100);

      expect(func).toHaveBeenCalledWith('a', 'b', 'c');
    });

    it('delay=0 应在下一个 tick 执行', () => {
      const func = vi.fn();
      const debounced = debounce(func, 0);

      debounced();
      expect(func).not.toHaveBeenCalled();

      vi.advanceTimersByTime(0);
      expect(func).toHaveBeenCalledTimes(1);
    });

    it('返回的函数不应带 cancel 方法（与 utils/index.js 版本不同）', () => {
      const func = vi.fn();
      const debounced = debounce(func, 300);
      expect(typeof debounced.cancel).toBe('undefined');
    });
  });
});

describe('pages/map/utils.js - 其他工具函数', () => {
  describe('getCachedImageBase64', () => {
    beforeEach(() => {
      mockImageToBase64.mockReset();
    });

    it('url 为空时应返回空字符串', async () => {
      const result = await getCachedImageBase64('');
      expect(result).toBe('');
      expect(mockImageToBase64).not.toHaveBeenCalled();
    });

    it('url 为 null/undefined 时应返回空字符串', async () => {
      expect(await getCachedImageBase64(null)).toBe('');
      expect(await getCachedImageBase64(undefined)).toBe('');
      expect(mockImageToBase64).not.toHaveBeenCalled();
    });

    it('imageToBase64 成功时应返回 base64 字符串', async () => {
      mockImageToBase64.mockResolvedValue('data:image/png;base64,AAAA');
      const result = await getCachedImageBase64('http://a.com/icon.png');
      expect(result).toBe('data:image/png;base64,AAAA');
      expect(mockImageToBase64).toHaveBeenCalledWith('http://a.com/icon.png');
    });

    it('imageToBase64 抛错时应返回空字符串（不抛出）', async () => {
      const warnSpy = vi.spyOn(console, 'warn').mockImplementation(() => {});
      mockImageToBase64.mockRejectedValue(new Error('convert fail'));
      const result = await getCachedImageBase64('http://b.com/bad.png');
      expect(result).toBe('');
      expect(warnSpy).toHaveBeenCalled();
    });

    it('相同 url 第二次应命中缓存（不再调用 imageToBase64）', async () => {
      mockImageToBase64.mockResolvedValue('data:image/png;base64,CACHE');
      const url = 'http://c.com/cached.png';
      const r1 = await getCachedImageBase64(url);
      const r2 = await getCachedImageBase64(url);
      expect(r1).toBe('data:image/png;base64,CACHE');
      expect(r2).toBe(r1);
      // 第二次应命中缓存，不再调用 imageToBase64
      expect(mockImageToBase64).toHaveBeenCalledTimes(1);
    });

    it('不同 url 应分别转换', async () => {
      mockImageToBase64
        .mockResolvedValueOnce('data:image/png;base64,A1')
        .mockResolvedValueOnce('data:image/png;base64,A2');
      const r1 = await getCachedImageBase64('http://d.com/1.png');
      const r2 = await getCachedImageBase64('http://d.com/2.png');
      expect(r1).toBe('data:image/png;base64,A1');
      expect(r2).toBe('data:image/png;base64,A2');
      expect(mockImageToBase64).toHaveBeenCalledTimes(2);
    });
  });

  describe('flyToCenter', () => {
    beforeEach(() => {
      mockGetMapConfig.mockReset();
      mockMapManager.get.mockReset();
    });

    it('mapManager.get 返回实例时应调用 flyAction(center, zoom)', () => {
      const center = [104.0668, 30.5728];
      const zoom = 12;
      mockGetMapConfig.mockReturnValue({ center, zoom });
      const flyAction = vi.fn();
      mockMapManager.get.mockReturnValue({ flyAction });

      flyToCenter('map-1');

      expect(mockMapManager.get).toHaveBeenCalledWith('map-1');
      expect(flyAction).toHaveBeenCalledWith(center, zoom);
    });

    it('mapManager.get 返回 undefined 时不应抛错', () => {
      mockGetMapConfig.mockReturnValue({ center: [0, 0], zoom: 10 });
      mockMapManager.get.mockReturnValue(undefined);
      expect(() => flyToCenter('non-existent')).not.toThrow();
    });

    it('getMapConfig 未返回 center/zoom 时也应不抛错', () => {
      mockGetMapConfig.mockReturnValue({});
      const flyAction = vi.fn();
      mockMapManager.get.mockReturnValue({ flyAction });
      flyToCenter('map-2');
      expect(flyAction).toHaveBeenCalledWith(undefined, undefined);
    });
  });

  describe('createMockPoints', () => {
    beforeEach(() => {
      mockGetMapConfig.mockReset();
      // 固定 Math.random 以便断言
      vi.spyOn(Math, 'random').mockReturnValue(0.5);
    });

    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('应生成指定数量的点位', () => {
      mockGetMapConfig.mockReturnValue({ center: [104, 30] });
      const list = createMockPoints('camera', 10);
      expect(list).toHaveLength(10);
    });

    it('默认数量为 50', () => {
      mockGetMapConfig.mockReturnValue({ center: [104, 30] });
      const list = createMockPoints('person');
      expect(list).toHaveLength(50);
    });

    it('每个点位应包含 id、position、type、status 字段', () => {
      mockGetMapConfig.mockReturnValue({ center: [104, 30] });
      const list = createMockPoints('camera', 3);
      list.forEach((p, i) => {
        expect(p.id).toBe(`camera-${i}`);
        expect(Array.isArray(p.position)).toBe(true);
        expect(p.position).toHaveLength(2);
        expect(p.type).toBe('monitor'); // prefix='camera' → 'monitor'
        expect(['online', 'offline']).toContain(p.status);
      });
    });

    it('prefix="camera" 时 type 应为 "monitor"', () => {
      mockGetMapConfig.mockReturnValue({ center: [0, 0] });
      const list = createMockPoints('camera', 1);
      expect(list[0].type).toBe('monitor');
    });

    it('prefix 非 camera 时 type 应等于 prefix', () => {
      mockGetMapConfig.mockReturnValue({ center: [0, 0] });
      const list = createMockPoints('person', 1);
      expect(list[0].type).toBe('person');
    });

    it('position 应基于 center 偏移生成', () => {
      mockGetMapConfig.mockReturnValue({ center: [100, 20] });
      const list = createMockPoints('car', 1);
      const [lng, lat] = list[0].position;
      // 经度应接近 100 附近（偏移幅度 ±0.35 量级）
      expect(lng).toBeGreaterThan(99);
      expect(lng).toBeLessThan(101);
      // 纬度应接近 20 附近（偏移幅度 ±0.25 量级）
      expect(lat).toBeGreaterThan(19);
      expect(lat).toBeLessThan(21);
    });

    it('position 经纬度应保留 6 位小数', () => {
      mockGetMapConfig.mockReturnValue({ center: [104.0668, 30.5728] });
      const list = createMockPoints('recorder', 1);
      const [lng, lat] = list[0].position;
      // toFixed(6) 后转 Number，最多 6 位小数
      const lngDecimals = (String(lng).split('.')[1] || '').length;
      const latDecimals = (String(lat).split('.')[1] || '').length;
      expect(lngDecimals).toBeLessThanOrEqual(6);
      expect(latDecimals).toBeLessThanOrEqual(6);
    });

    it('getMapConfig 未返回 center 时应使用默认值 [104.0668, 30.5728]', () => {
      mockGetMapConfig.mockReturnValue({});
      const list = createMockPoints('test', 1);
      const [lng, lat] = list[0].position;
      // 默认中心点附近
      expect(lng).toBeGreaterThan(103);
      expect(lng).toBeLessThan(105);
      expect(lat).toBeGreaterThan(29);
      expect(lat).toBeLessThan(31);
    });

    it('count=0 时应返回空数组', () => {
      mockGetMapConfig.mockReturnValue({ center: [0, 0] });
      const list = createMockPoints('empty', 0);
      expect(list).toHaveLength(0);
    });

    it('status 应根据 Math.random 阈值确定（>0.3 为 online）', () => {
      mockGetMapConfig.mockReturnValue({ center: [0, 0] });
      // Math.random 固定为 0.5 → > 0.3 → online
      const list = createMockPoints('p', 1);
      expect(list[0].status).toBe('online');
    });

    it('Math.random <= 0.3 时 status 应为 offline', () => {
      mockGetMapConfig.mockReturnValue({ center: [0, 0] });
      vi.spyOn(Math, 'random').mockReturnValue(0.2);
      const list = createMockPoints('p', 1);
      expect(list[0].status).toBe('offline');
    });
  });

  describe('renderClusterMarker', () => {
    it('count <= 99 时应显示原始数字', () => {
      const context = {
        count: 5,
        marker: { setContent: vi.fn() },
      };
      renderClusterMarker(context, 'cluster.png');
      expect(context.marker.setContent).toHaveBeenCalled();
      const html = context.marker.setContent.mock.calls[0][0];
      // 源码 span 标签内有缩进换行，数字前后含空白
      expect(html).toContain('5');
      expect(html).not.toContain('99+');
    });

    it('count > 99 时应显示 "99+"', () => {
      const context = {
        count: 150,
        marker: { setContent: vi.fn() },
      };
      renderClusterMarker(context, 'cluster.png');
      const html = context.marker.setContent.mock.calls[0][0];
      expect(html).toContain('99+');
      expect(html).not.toContain('>150<');
    });

    it('count=99 时应显示 99（边界值）', () => {
      const context = {
        count: 99,
        marker: { setContent: vi.fn() },
      };
      renderClusterMarker(context, 'img.png');
      const html = context.marker.setContent.mock.calls[0][0];
      expect(html).toContain('99');
      expect(html).not.toContain('99+');
    });

    it('count=100 时应显示 99+（边界值）', () => {
      const context = {
        count: 100,
        marker: { setContent: vi.fn() },
      };
      renderClusterMarker(context, 'img.png');
      const html = context.marker.setContent.mock.calls[0][0];
      expect(html).toContain('99+');
    });

    it('clusterImageUrl 为空时也应正常生成 HTML', () => {
      const context = {
        count: 1,
        marker: { setContent: vi.fn() },
      };
      renderClusterMarker(context, '');
      const html = context.marker.setContent.mock.calls[0][0];
      expect(html).toContain('src=""');
    });

    it('clusterImageUrl 为 undefined 时应使用空字符串', () => {
      const context = {
        count: 1,
        marker: { setContent: vi.fn() },
      };
      renderClusterMarker(context, undefined);
      const html = context.marker.setContent.mock.calls[0][0];
      expect(html).toContain('src=""');
    });

    it('marker 有 setOffset + window.AMap.Pixel 时应调用 setOffset', () => {
      const pixelInstance = { x: -24, y: -24 };
      const AMapPixel = vi.fn(() => pixelInstance);
      window.AMap = { Pixel: AMapPixel };
      const setOffset = vi.fn();
      const context = {
        count: 1,
        marker: { setOffset, setContent: vi.fn() },
      };
      try {
        renderClusterMarker(context, 'cluster.png');
        expect(setOffset).toHaveBeenCalledWith(pixelInstance);
        expect(AMapPixel).toHaveBeenCalledWith(-24, -24);
      } finally {
        delete window.AMap;
      }
    });

    it('marker 无 setContent 但在浏览器环境时应返回 DOM 元素', () => {
      const context = {
        count: 1,
        marker: {},
      };
      const result = renderClusterMarker(context, 'cluster.png');
      // 应进入 document.createElement 分支
      expect(result).toBeInstanceOf(Element);
    });

    it('HTML 中应包含 clusterImageUrl', () => {
      const context = {
        count: 1,
        marker: { setContent: vi.fn() },
      };
      renderClusterMarker(context, 'http://img.com/cluster.png');
      const html = context.marker.setContent.mock.calls[0][0];
      expect(html).toContain('http://img.com/cluster.png');
    });

    it('marker.setOffset 不是 function 时应跳过 setOffset 调用', () => {
      window.AMap = { Pixel: vi.fn() };
      const context = {
        count: 1,
        marker: { setOffset: 'not-fn', setContent: vi.fn() },
      };
      try {
        expect(() => renderClusterMarker(context, 'x.png')).not.toThrow();
      } finally {
        delete window.AMap;
      }
    });

    it('window.AMap 不存在时即使有 setOffset 也应跳过', () => {
      // 确保 window.AMap 不存在
      delete window.AMap;
      const setOffset = vi.fn();
      const context = {
        count: 1,
        marker: { setOffset, setContent: vi.fn() },
      };
      renderClusterMarker(context, 'x.png');
      expect(setOffset).not.toHaveBeenCalled();
    });
  });

  describe('processInBatches', () => {
    let originalRAF;

    beforeEach(() => {
      originalRAF = global.requestAnimationFrame;
      // 同步触发 requestAnimationFrame 回调
      global.requestAnimationFrame = vi.fn((cb) => {
        cb();
        return 1;
      });
    });

    afterEach(() => {
      global.requestAnimationFrame = originalRAF;
    });

    it('data 非数组时应返回空数组', async () => {
      const result = await processInBatches(null, 10, vi.fn());
      expect(result).toEqual([]);
    });

    it('空数组应返回空数组', async () => {
      const result = await processInBatches([], 10, vi.fn());
      expect(result).toEqual([]);
    });

    it('应分批处理数据并返回合并结果', async () => {
      const data = [1, 2, 3, 4, 5];
      const processor = vi.fn((batch) => batch.map((x) => x * 2));
      const result = await processInBatches(data, 2, processor);
      expect(result).toEqual([2, 4, 6, 8, 10]);
      // 5 个元素分 2 个一批 → 3 批
      expect(processor).toHaveBeenCalledTimes(3);
    });

    it('batchSize 大于数据长度时应只处理一批', async () => {
      const data = [1, 2, 3];
      const processor = vi.fn((batch) => batch);
      const result = await processInBatches(data, 100, processor);
      expect(result).toEqual([1, 2, 3]);
      expect(processor).toHaveBeenCalledTimes(1);
    });

    it('batchSize=1 时每个元素一批', async () => {
      const data = [1, 2, 3];
      const processor = vi.fn((batch) => batch);
      const result = await processInBatches(data, 1, processor);
      expect(result).toEqual([1, 2, 3]);
      expect(processor).toHaveBeenCalledTimes(3);
    });

    it('应调用 onProgress 回调并传递已处理数量和总数', async () => {
      const data = [1, 2, 3, 4];
      const processor = (batch) => batch;
      const onProgress = vi.fn();
      await processInBatches(data, 2, processor, onProgress);
      // 2 批，每批 2 个
      expect(onProgress).toHaveBeenCalledTimes(2);
      expect(onProgress).toHaveBeenNthCalledWith(1, 2, 4);
      expect(onProgress).toHaveBeenNthCalledWith(2, 4, 4);
    });

    it('onProgress 非函数时不应抛错', async () => {
      const data = [1, 2];
      const processor = (batch) => batch;
      await expect(processInBatches(data, 1, processor, null)).resolves.toEqual([1, 2]);
      await expect(processInBatches(data, 1, processor, 'not-fn')).resolves.toEqual([1, 2]);
    });

    it('应调用 requestAnimationFrame 等待下一帧', async () => {
      const rafSpy = vi.spyOn(global, 'requestAnimationFrame');
      const data = [1, 2, 3];
      await processInBatches(data, 1, (b) => b);
      // 每批等待一帧，3 批 → 3 次
      expect(rafSpy).toHaveBeenCalledTimes(3);
    });

    it('processor 返回的批次结果应按顺序合并', async () => {
      const data = ['a', 'b', 'c', 'd'];
      const processor = (batch) => batch.map((x) => `_${x}`);
      const result = await processInBatches(data, 2, processor);
      expect(result).toEqual(['_a', '_b', '_c', '_d']);
    });
  });

  describe('scheduleIdleWork', () => {
    let originalRIC;
    let originalRAF;

    beforeEach(() => {
      originalRIC = global.requestIdleCallback;
      originalRAF = global.requestAnimationFrame;
    });

    afterEach(() => {
      global.requestIdleCallback = originalRIC;
      global.requestAnimationFrame = originalRAF;
      delete global.requestIdleCallback;
    });

    it('支持 requestIdleCallback 时应使用它', () => {
      const ricSpy = vi.fn(() => 42);
      global.requestIdleCallback = ricSpy;
      const cb = vi.fn();
      const options = { timeout: 200 };
      const result = scheduleIdleWork(cb, options);
      expect(ricSpy).toHaveBeenCalledWith(cb, options);
      expect(result).toBe(42);
    });

    it('不支持 requestIdleCallback 时应降级到 requestAnimationFrame', () => {
      delete global.requestIdleCallback;
      const rafSpy = vi.fn((cb) => {
        cb();
        return 7;
      });
      global.requestAnimationFrame = rafSpy;
      const work = vi.fn();
      const result = scheduleIdleWork(work, { timeout: 100 });
      expect(rafSpy).toHaveBeenCalled();
      expect(work).toHaveBeenCalled();
      expect(result).toBe(7);
    });

    it('降级回调中 deadline 应包含 didTimeout=false 和 timeRemaining 函数', () => {
      delete global.requestIdleCallback;
      global.requestAnimationFrame = (cb) => {
        cb();
        return 1;
      };
      let captured;
      scheduleIdleWork((deadline) => {
        captured = deadline;
      });
      expect(captured).toBeDefined();
      expect(captured.didTimeout).toBe(false);
      expect(typeof captured.timeRemaining).toBe('function');
      // timeRemaining 应返回 0~50 之间的数
      const remaining = captured.timeRemaining();
      expect(remaining).toBeGreaterThanOrEqual(0);
      expect(remaining).toBeLessThanOrEqual(50);
    });

    it('未传 options 时应使用默认 { timeout: 100 }', () => {
      const ricSpy = vi.fn(() => 1);
      global.requestIdleCallback = ricSpy;
      const cb = vi.fn();
      scheduleIdleWork(cb);
      expect(ricSpy).toHaveBeenCalledWith(cb, { timeout: 100 });
    });

    it('支持 requestIdleCallback 时不调用 requestAnimationFrame', () => {
      global.requestIdleCallback = vi.fn(() => 1);
      const rafSpy = vi.spyOn(global, 'requestAnimationFrame');
      scheduleIdleWork(vi.fn());
      expect(rafSpy).not.toHaveBeenCalled();
    });
  });
});

describe('pages/map/utils.js - renderDeviceLayers', () => {
  let targetMap;
  let addMarkerClusterMock;

  beforeEach(() => {
    vi.clearAllMocks();
    addMarkerClusterMock = vi.fn();
    targetMap = {
      addMarkerCluster: addMarkerClusterMock,
    };
    // 让 getCachedImageBase64 始终返回固定值，避免触发 imageToBase64 真实调用
    mockImageToBase64.mockResolvedValue('data:image/png;base64,MOCK');
    // 安静 console.info / console.warn
    vi.spyOn(console, 'info').mockImplementation(() => {});
    vi.spyOn(console, 'warn').mockImplementation(() => {});
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it('targetMap 无 addMarkerCluster 方法时应打印 warn 并直接返回', async () => {
    const warnSpy = vi.spyOn(console, 'warn');
    // 传入空对象作为 targetMap
    await renderDeviceLayers({}, [], {}, vi.fn());
    expect(warnSpy).toHaveBeenCalledWith(
      expect.stringContaining('地图实例无效'),
    );
  });

  it('targetMap 为 null 时应打印 warn 并直接返回', async () => {
    const warnSpy = vi.spyOn(console, 'warn');
    await renderDeviceLayers(null, [], {}, vi.fn());
    expect(warnSpy).toHaveBeenCalled();
  });

  it('targetMap 为 undefined 时应打印 warn 并直接返回', async () => {
    const warnSpy = vi.spyOn(console, 'warn');
    await renderDeviceLayers(undefined, [], {}, vi.fn());
    expect(warnSpy).toHaveBeenCalled();
  });

  it('空 deviceLayers 数组时应完成遍历但不调用 addMarkerCluster', async () => {
    await renderDeviceLayers(targetMap, [], {}, vi.fn());
    expect(addMarkerClusterMock).not.toHaveBeenCalled();
  });

  it('单图层时应正确调用 addMarkerCluster 并传入点位', async () => {
    const layers = [
      {
        id: 'camera',
        name: '摄像头',
        image: 'http://a.com/camera.png',
        clusterImage: 'http://a.com/cluster.png',
        color: '#ff0',
        size: 32,
        visible: true,
        icon: 'cam',
      },
    ];
    const layerPoints = {
      camera: [
        { id: 'c1', position: [104, 30], status: 'online' },
        { id: 'c2', position: [104.1, 30.1], status: 'offline' },
      ],
    };

    await renderDeviceLayers(targetMap, layers, layerPoints, vi.fn());

    expect(addMarkerClusterMock).toHaveBeenCalledTimes(1);
    const [layerId, points, options] = addMarkerClusterMock.mock.calls[0];
    expect(layerId).toBe('camera');
    expect(points).toHaveLength(2);
    // 应附加 __id 和 layerId
    expect(points[0].__id).toBe('camera-c1');
    expect(points[0].layerId).toBe('camera');
    expect(points[1].__id).toBe('camera-c2');
    expect(points[1].layerId).toBe('camera');
    // options 应包含图层配置
    expect(options.color).toBe('#ff0');
    expect(options.size).toBe(32);
    expect(options.visible).toBe(true);
    expect(options.icon).toBe('cam');
    expect(options.image).toBe('data:image/png;base64,MOCK');
    expect(options.clusterImage).toBe('data:image/png;base64,MOCK');
    // 应包含 renderMarker / renderClusterMarker / onMarkerClick
    expect(typeof options.renderMarker).toBe('function');
    expect(typeof options.renderClusterMarker).toBe('function');
    expect(typeof options.onMarkerClick).toBe('function');
  });

  it('多个图层时应分别调用 addMarkerCluster', async () => {
    const layers = [
      {
        id: 'l1',
        name: 'L1',
        image: 'a.png',
        clusterImage: 'ca.png',
        visible: true,
      },
      {
        id: 'l2',
        name: 'L2',
        image: 'b.png',
        clusterImage: 'cb.png',
        visible: false,
      },
    ];
    const layerPoints = {
      l1: [{ id: 'p1', position: [0, 0] }],
      l2: [{ id: 'p2', position: [1, 1] }],
    };

    await renderDeviceLayers(targetMap, layers, layerPoints, vi.fn());

    expect(addMarkerClusterMock).toHaveBeenCalledTimes(2);
    expect(addMarkerClusterMock.mock.calls[0][0]).toBe('l1');
    expect(addMarkerClusterMock.mock.calls[1][0]).toBe('l2');
  });

  it('layerPoints 中无对应图层 id 时应使用空数组', async () => {
    const layers = [
      {
        id: 'no-data',
        name: 'NoData',
        image: 'a.png',
        clusterImage: 'b.png',
        visible: true,
      },
    ];

    await renderDeviceLayers(targetMap, layers, {}, vi.fn());

    expect(addMarkerClusterMock).toHaveBeenCalledTimes(1);
    const [, points] = addMarkerClusterMock.mock.calls[0];
    expect(points).toEqual([]);
  });

  it('点位 __id 已存在时应保留原始 __id', async () => {
    const layers = [
      {
        id: 'l1',
        name: 'L1',
        image: 'a.png',
        clusterImage: 'b.png',
        visible: true,
      },
    ];
    const layerPoints = {
      l1: [{ id: 'p1', __id: 'custom-id', position: [0, 0] }],
    };

    await renderDeviceLayers(targetMap, layers, layerPoints, vi.fn());

    const [, points] = addMarkerClusterMock.mock.calls[0];
    expect(points[0].__id).toBe('custom-id');
    expect(points[0].layerId).toBe('l1');
  });

  it('点位无 id 时应使用索引生成 __id', async () => {
    const layers = [
      {
        id: 'l1',
        name: 'L1',
        image: 'a.png',
        clusterImage: 'b.png',
        visible: true,
      },
    ];
    const layerPoints = {
      l1: [{ position: [0, 0] }, { position: [1, 1] }],
    };

    await renderDeviceLayers(targetMap, layers, layerPoints, vi.fn());

    const [, points] = addMarkerClusterMock.mock.calls[0];
    expect(points[0].__id).toBe('l1-0');
    expect(points[1].__id).toBe('l1-1');
  });

  it('renderMarker 应根据 status 返回在线/离线 HTML', async () => {
    const layers = [
      {
        id: 'l1',
        name: 'L1',
        image: 'a.png',
        clusterImage: 'b.png',
        visible: true,
      },
    ];
    const layerPoints = {
      l1: [{ id: 'p1', position: [0, 0] }],
    };

    await renderDeviceLayers(targetMap, layers, layerPoints, vi.fn());

    const options = addMarkerClusterMock.mock.calls[0][2];

    // 在线
    const onlineHtml = options.renderMarker({ status: 'online' });
    expect(onlineHtml).toContain('rgba(67, 207, 124, 1)');

    // 离线
    const offlineHtml = options.renderMarker({ status: 'offline' });
    expect(offlineHtml).toContain('rgba(166, 169, 171, 1)');

    // status 不是 online 时按离线处理
    const otherHtml = options.renderMarker({ status: 'unknown' });
    expect(otherHtml).toContain('rgba(166, 169, 171, 1)');

    // status 为 undefined
    const noStatusHtml = options.renderMarker({});
    expect(noStatusHtml).toContain('rgba(166, 169, 171, 1)');
  });

  it('renderClusterMarker 应委托给全局 renderClusterMarker', async () => {
    const layers = [
      {
        id: 'l1',
        name: 'L1',
        image: 'a.png',
        clusterImage: 'b.png',
        visible: true,
      },
    ];
    const layerPoints = {
      l1: [{ id: 'p1', position: [0, 0] }],
    };

    await renderDeviceLayers(targetMap, layers, layerPoints, vi.fn());

    const options = addMarkerClusterMock.mock.calls[0][2];
    const context = {
      count: 5,
      marker: { setContent: vi.fn() },
    };
    options.renderClusterMarker(context);
    const html = context.marker.setContent.mock.calls[0][0];
    expect(html).toContain('5');
  });

  it('onMarkerClick 应透传传入的回调函数', async () => {
    const layers = [
      {
        id: 'l1',
        name: 'L1',
        image: 'a.png',
        clusterImage: 'b.png',
        visible: true,
      },
    ];
    const layerPoints = {
      l1: [{ id: 'p1', position: [0, 0] }],
    };
    const onMarkerClick = vi.fn();

    await renderDeviceLayers(targetMap, layers, layerPoints, onMarkerClick);

    const options = addMarkerClusterMock.mock.calls[0][2];
    const payload = { point: { id: 'p1' } };
    options.onMarkerClick(payload);
    expect(onMarkerClick).toHaveBeenCalledWith(payload);
  });

  it('onMarkerClick 为 undefined 时 options.onMarkerClick 也应为 undefined', async () => {
    const layers = [
      {
        id: 'l1',
        name: 'L1',
        image: 'a.png',
        clusterImage: 'b.png',
        visible: true,
      },
    ];
    const layerPoints = {
      l1: [{ id: 'p1', position: [0, 0] }],
    };

    await renderDeviceLayers(targetMap, layers, layerPoints);

    const options = addMarkerClusterMock.mock.calls[0][2];
    expect(options.onMarkerClick).toBeUndefined();
  });

  it('图层配置中无 image/clusterImage 时应仍能完成', async () => {
    const layers = [
      {
        id: 'l1',
        name: 'L1',
        visible: true,
      },
    ];
    const layerPoints = {
      l1: [{ id: 'p1', position: [0, 0] }],
    };

    await renderDeviceLayers(targetMap, layers, layerPoints, vi.fn());

    expect(addMarkerClusterMock).toHaveBeenCalledTimes(1);
    const options = addMarkerClusterMock.mock.calls[0][2];
    expect(options.image).toBe(''); // getCachedImageBase64('') 返回 ''
    expect(options.clusterImage).toBe('');
  });
});
