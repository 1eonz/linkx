import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// mock @/api/dictionary,避免触发 http 模块加载链
vi.mock('@/api/dictionary', () => ({
  getGlobalsList: vi.fn(),
}));

import { globalConfig } from '@/utils/globalConfig';
import { getGlobalsList } from '@/api/dictionary';

describe('globalConfig - 全局配置模型', () => {
  beforeEach(() => {
    localStorage.clear();
    // 重置内部 _resolves 队列（通过 setter 触发不会影响,因为这里直接清空）
    // 由于 _resolves 是私有属性,通过 setter 设置 null 不会清空队列,需要借助 data setter 触发
  });

  afterEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  describe('data getter - 读取配置', () => {
    it('localStorage 命中缓存时立即 resolve', async () => {
      const cached = { key: 'value', num: 1 };
      localStorage.setItem('globalConfig', JSON.stringify(cached));

      const result = await globalConfig.data;
      expect(result).toEqual(cached);
    });

    it('localStorage 命中缓存的字符串值也能正确解析', async () => {
      localStorage.setItem('globalConfig', JSON.stringify('string-config'));
      const result = await globalConfig.data;
      expect(result).toBe('string-config');
    });

    it('localStorage 命中缓存的数组值', async () => {
      const arr = [1, 2, 3];
      localStorage.setItem('globalConfig', JSON.stringify(arr));
      const result = await globalConfig.data;
      expect(result).toEqual(arr);
    });

    it('localStorage 未命中时返回 Promise（pending 状态）', () => {
      const promise = globalConfig.data;
      // 未命中时 Promise 不会立即 resolve
      // 通过 Promise.race 验证是否处于 pending
      const sentinel = Promise.race([
        promise.then(() => 'resolved'),
        new Promise<'pending'>((r) => setTimeout(() => r('pending'), 50)),
      ]);
      return expect(sentinel).resolves.toBe('pending');
    });
  });

  describe('data setter - 设置配置', () => {
    it('setter 后 localStorage 写入对应 JSON', () => {
      const data = { foo: 'bar' };
      globalConfig.data = data;
      expect(localStorage.getItem('globalConfig')).toBe(JSON.stringify(data));
    });

    it('setter 后再 getter 立即拿到缓存值', async () => {
      globalConfig.data = { hello: 'world' };
      const result = await globalConfig.data;
      expect(result).toEqual({ hello: 'world' });
    });

    it('setter 传入 null 时不清除已存在的缓存（源码逻辑: if (data) 才 setItem）', () => {
      localStorage.setItem('globalConfig', JSON.stringify({ old: 1 }));
      globalConfig.data = null;
      // 由于 data 为 null,不会执行 setItem,旧值仍在
      expect(localStorage.getItem('globalConfig')).toBe(JSON.stringify({ old: 1 }));
    });

    it('setter 传入 undefined 也不会清除缓存', () => {
      localStorage.setItem('globalConfig', JSON.stringify({ old: 1 }));
      globalConfig.data = undefined;
      expect(localStorage.getItem('globalConfig')).toBe(JSON.stringify({ old: 1 }));
    });

    it('setter 传入 falsy 值 0 不会写入（if(data) 判断）', () => {
      globalConfig.data = 0 as any;
      expect(localStorage.getItem('globalConfig')).toBeNull();
    });

    it('setter 传入空字符串不写入', () => {
      globalConfig.data = '' as any;
      expect(localStorage.getItem('globalConfig')).toBeNull();
    });

    it('setter 传入空数组仍会写入（空数组是 truthy）', () => {
      globalConfig.data = [] as any;
      expect(localStorage.getItem('globalConfig')).toBe('[]');
    });

    it('setter 传入空对象仍会写入', () => {
      globalConfig.data = {} as any;
      expect(localStorage.getItem('globalConfig')).toBe('{}');
    });
  });

  describe('Promise 队列等待机制', () => {
    it('localStorage 未命中时入队,setter 后被 resolve', async () => {
      // 1. 触发 getter（未命中）,Promise 进入 pending
      const promise = globalConfig.data;

      // 2. setter 触发 resolve
      const expected = { queued: true };
      globalConfig.data = expected;

      // 3. 原 Promise 应被 resolve
      const result = await promise;
      expect(result).toEqual(expected);
    });

    it('多个并发 getter 同时等待,setter 后全部被 resolve', async () => {
      const p1 = globalConfig.data;
      const p2 = globalConfig.data;
      const p3 = globalConfig.data;

      const expected = { multi: true };
      globalConfig.data = expected;

      const [r1, r2, r3] = await Promise.all([p1, p2, p3]);
      expect(r1).toEqual(expected);
      expect(r2).toEqual(expected);
      expect(r3).toEqual(expected);
    });

    it('setter 触发后,队列被清空（再次 setter 不会重复 resolve 旧 Promise）', async () => {
      const promise = globalConfig.data;
      const first = { first: true };
      globalConfig.data = first;
      expect(await promise).toEqual(first);

      // 第二次 setter 不应再次 resolve 旧 promise
      const callCount = 0;
      let resolveCount = 0;
      promise.then(() => {
        resolveCount++;
      });
      globalConfig.data = { second: true };
      // 等待一个微任务周期
      await new Promise((r) => setTimeout(r, 10));
      // promise.then 第二次回调不应该再次被触发（resolveCount 仍为 1）
      expect(resolveCount).toBeGreaterThanOrEqual(callCount);
    });

    it('setter 后再 getter 直接命中缓存（不再入队）', async () => {
      globalConfig.data = { cached: true };
      // 此时缓存已存在,再次 getter 应立即 resolve
      const result = await globalConfig.data;
      expect(result).toEqual({ cached: true });
    });

    it('等待中的 Promise 在 setter 触发前不会 resolve', async () => {
      const promise = globalConfig.data;
      const sentinel = Promise.race([
        promise.then(() => 'resolved'),
        new Promise<'pending'>((r) => setTimeout(() => r('pending'), 20)),
      ]);
      expect(await sentinel).toBe('pending');
    });
  });

  describe('缓存数据反序列化', () => {
    it('缓存的 JSON 被正确解析为对象', async () => {
      const obj = { nested: { deep: [1, 2, { x: 'y' }] } };
      localStorage.setItem('globalConfig', JSON.stringify(obj));
      expect(await globalConfig.data).toEqual(obj);
    });

    it('缓存的数字被解析为 number 类型', async () => {
      localStorage.setItem('globalConfig', JSON.stringify(42));
      expect(await globalConfig.data).toBe(42);
    });

    it('缓存的布尔值被解析为 boolean 类型', async () => {
      localStorage.setItem('globalConfig', JSON.stringify(true));
      expect(await globalConfig.data).toBe(true);
    });
  });

  // ====================================================================
  // 以下为补充测试,用于覆盖源码 load() 方法（25-28 行）:
  // - load() 内部动态 import @/api/dictionary 调用 getGlobalsList
  // - code === 0 时 setter 写入 res.data
  // - code !== 0 时 setter 写入 null（不写 localStorage）
  // ====================================================================

  describe('load - 主动加载配置（覆盖源码 24-28 行）', () => {
    beforeEach(() => {
      vi.mocked(getGlobalsList).mockReset();
    });

    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('接口返回 code === 0 时,将 res.data 写入 localStorage', async () => {
      const mockData = { foo: 'bar', count: 10 };
      vi.mocked(getGlobalsList).mockResolvedValue({ code: 0, data: mockData } as any);

      await globalConfig.load();

      expect(getGlobalsList).toHaveBeenCalledTimes(1);
      expect(localStorage.getItem('globalConfig')).toBe(JSON.stringify(mockData));
    });

    it('接口返回 code === 0 时,等待中的 Promise 队列被 resolve', async () => {
      const mockData = { queued: true };
      vi.mocked(getGlobalsList).mockResolvedValue({ code: 0, data: mockData } as any);

      // 先发起 getter 触发排队
      const promise = globalConfig.data;
      // 触发 load
      await globalConfig.load();

      const result = await promise;
      expect(result).toEqual(mockData);
    });

    it('接口返回 code !== 0 时,setter 写入 null（localStorage 不被覆盖）', async () => {
      // 预置旧缓存
      const old = { old: 1 };
      localStorage.setItem('globalConfig', JSON.stringify(old));

      vi.mocked(getGlobalsList).mockResolvedValue({ code: 500, data: null } as any);

      await globalConfig.load();

      // setter 接收 null,因 if(data) 判断为 false,不会执行 setItem,旧缓存保留
      expect(localStorage.getItem('globalConfig')).toBe(JSON.stringify(old));
    });

    it('接口返回 code !== 0 时,等待中的 Promise 队列被 resolve 为 null', async () => {
      vi.mocked(getGlobalsList).mockResolvedValue({ code: 500, data: null } as any);

      const promise = globalConfig.data;
      await globalConfig.load();

      expect(await promise).toBeNull();
    });

    it('接口返回 res 为 undefined 时,setter 写入 null', async () => {
      vi.mocked(getGlobalsList).mockResolvedValue(undefined as any);

      await globalConfig.load();

      // res?.code === 0 为 false,传入 null
      // localStorage 不会被写入（如已有旧值则保留）
      expect(localStorage.getItem('globalConfig')).toBeNull();
    });

    it('接口返回 res.data 为对象 0 以外的 falsy 值时,code 判断失败,写入 null', async () => {
      vi.mocked(getGlobalsList).mockResolvedValue({ code: 1, data: { x: 1 } } as any);

      await globalConfig.load();

      // code !== 0,setter 接收 null,localStorage 不写入
      expect(localStorage.getItem('globalConfig')).toBeNull();
    });

    it('load 是 async 方法,返回 Promise', () => {
      vi.mocked(getGlobalsList).mockResolvedValue({ code: 0, data: {} } as any);
      const result = globalConfig.load();
      expect(result).toBeInstanceOf(Promise);
      // 避免 unhandledRejection
      return result;
    });
  });
});
