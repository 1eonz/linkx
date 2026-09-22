import { describe, it, expect, vi, afterEach } from 'vitest';

import { withInstall, S4, guid, addUnit, displayImage } from '@/utils/index';

// 每个 case 后清理 mock 与 env stub，避免状态污染
afterEach(() => {
  vi.restoreAllMocks();
  vi.unstubAllEnvs();
});

describe('utils/index —— 工具函数', () => {
  describe('withInstall - 给组件添加 install 方法', () => {
    it('传入对象后返回值应具有 install 方法', () => {
      const Comp = { name: 'MyComp' } as any;
      const result = withInstall(Comp);
      expect(typeof result.install).toBe('function');
    });

    it('install 方法应能注册主组件到 app', () => {
      const Comp = { name: 'MyComp' } as any;
      const result = withInstall(Comp);
      const registered: Record<string, any> = {};
      const app = {
        component(name: string, comp: any) {
          registered[name] = comp;
        },
      } as any;
      (result.install as any)(app);
      expect(registered['MyComp']).toBe(Comp);
    });

    it('传入 extra 时，extra 中的组件应被注册到 app', () => {
      const Comp = { name: 'Main' } as any;
      const Child = { name: 'Child' } as any;
      const result = withInstall(Comp, { Child });
      const registered: Record<string, any> = {};
      const app = {
        component(name: string, comp: any) {
          registered[name] = comp;
        },
      } as any;
      (result.install as any)(app);
      expect(registered['Main']).toBe(Comp);
      expect(registered['Child']).toBe(Child);
    });

    it('传入 extra 时，extra 组件应被挂载到主组件对象上', () => {
      const Comp = { name: 'Main' } as any;
      const Child = { name: 'Child' } as any;
      const result = withInstall(Comp, { Child }) as any;
      expect(result.Child).toBe(Child);
    });

    it('未传入 extra 时 install 仅注册主组件，不抛错', () => {
      const Comp = { name: 'Main' } as any;
      const result = withInstall(Comp);
      const registered: Record<string, any> = {};
      const app = {
        component(name: string, comp: any) {
          registered[name] = comp;
        },
      } as any;
      expect(() => (result.install as any)(app)).not.toThrow();
      expect(Object.keys(registered)).toEqual(['Main']);
    });
  });

  describe('S4 - 生成 4 位随机 hex', () => {
    it('应返回字符串', () => {
      const result = S4();
      expect(typeof result).toBe('string');
    });

    it('返回值应为 4 位 hex 字符串', () => {
      const result = S4();
      expect(result).toMatch(/^[0-9a-f]{4}$/);
    });

    it('使用 mock 验证 Math.random 返回 0.5 时的确定性输出', () => {
      vi.spyOn(Math, 'random').mockReturnValue(0.5);
      // 源码: (((1 + 0.5) * 0x10000) | 0).toString(16).slice(1)
      // 1.5 * 0x10000 = 98304, | 0 = 98304
      // (98304).toString(16) = '18000', slice(1) = '8000'
      expect(S4()).toBe('8000');
    });

    it('使用 mock 验证 Math.random 返回 0 时的确定性输出', () => {
      vi.spyOn(Math, 'random').mockReturnValue(0);
      // (1 + 0) * 0x10000 = 65536, | 0 = 65536
      // (65536).toString(16) = '10000', slice(1) = '0000'
      expect(S4()).toBe('0000');
    });

    it('多次调用应返回 4 位 hex', () => {
      for (let i = 0; i < 10; i++) {
        expect(S4()).toMatch(/^[0-9a-f]{4}$/);
      }
    });
  });

  describe('guid - 生成 UUID', () => {
    it('应返回字符串', () => {
      expect(typeof guid()).toBe('string');
    });

    it('应返回 UUID 格式（含 4 个连字符 -）', () => {
      const result = guid();
      // 源码: S4+S4-S4-S4-S4+S4+S4 — 共 5 段，4 个 '-'
      expect(result.split('-')).toHaveLength(5);
      expect(result.match(/-/g)).toHaveLength(4);
    });

    it('UUID 各段应为 hex 字符', () => {
      const result = guid();
      const segments = result.split('-');
      // 段长度: 8-4-4-4-12
      expect(segments[0]).toHaveLength(8);
      expect(segments[1]).toHaveLength(4);
      expect(segments[2]).toHaveLength(4);
      expect(segments[3]).toHaveLength(4);
      expect(segments[4]).toHaveLength(12);
      for (const seg of segments) {
        expect(seg).toMatch(/^[0-9a-f]+$/);
      }
    });

    it('mock Math.random 后 guid 应为确定值', () => {
      vi.spyOn(Math, 'random').mockReturnValue(0.5);
      // S4() = '8000'
      // 期望: 80008000-8000-8000-8000-800080008000
      expect(guid()).toBe('80008000-8000-8000-8000-800080008000');
    });
  });

  describe('addUnit - 数值加单位', () => {
    it('数字 1 应转为 1px', () => {
      expect(addUnit(1)).toBe('1px');
    });

    it('字符串 2 应原样返回（源码 isString 分支直接 return value）', () => {
      expect(addUnit('2')).toBe('2');
    });

    it('已带单位的字符串 3em 应原样返回', () => {
      expect(addUnit('3em')).toBe('3em');
    });

    it('0 是 falsy，源码 if(!value) return ""，应返回空串', () => {
      expect(addUnit(0)).toBe('');
    });

    it('空串应返回空串', () => {
      expect(addUnit('')).toBe('');
    });

    it('自定义默认单位生效', () => {
      expect(addUnit(5, 'em')).toBe('5em');
    });

    it('数字 + 自定义单位 rem', () => {
      expect(addUnit(10, 'rem')).toBe('10rem');
    });

    it('负数应转为带单位的字符串', () => {
      expect(addUnit(-3)).toBe('-3px');
    });

    it('浮点数应转为带单位的字符串', () => {
      expect(addUnit(1.5)).toBe('1.5px');
    });
  });

  describe('displayImage - 处理图片地址', () => {
    it('http:// 开头的 url 应原样返回', () => {
      vi.stubEnv('DEV', true);
      vi.stubEnv('VITE_PROXY', '/proxy');
      const url = 'http://example.com/img.png';
      expect(displayImage(url)).toBe(url);
    });

    it('https:// 开头的 url 应原样返回', () => {
      vi.stubEnv('DEV', true);
      vi.stubEnv('VITE_PROXY', '/proxy');
      const url = 'https://example.com/img.png';
      expect(displayImage(url)).toBe(url);
    });

    it('DEV=true 且 VITE_PROXY=/proxy 时，相对路径应拼接为 VITE_PROXY+baseUrl+url', () => {
      vi.stubEnv('DEV', true);
      vi.stubEnv('VITE_PROXY', '/proxy');
      // baseUrl = '/agent/admin'
      expect(displayImage('/img/a.png')).toBe('/proxy/agent/admin/img/a.png');
    });

    it('DEV=false 时，相对路径应拼接为 baseUrl+url（无 VITE_PROXY 前缀）', () => {
      vi.stubEnv('DEV', false as any);
      vi.stubEnv('VITE_PROXY', '/proxy');
      expect(displayImage('/img/a.png')).toBe('/agent/admin/img/a.png');
    });

    it('空字符串 url 应返回空串', () => {
      vi.stubEnv('DEV', true);
      vi.stubEnv('VITE_PROXY', '/proxy');
      expect(displayImage('')).toBe('');
    });

    it('DEV=true 且 VITE_PROXY 为空串时，应拼接空串+baseUrl+url', () => {
      vi.stubEnv('DEV', true);
      vi.stubEnv('VITE_PROXY', '');
      expect(displayImage('/x.png')).toBe('/agent/admin/x.png');
    });

    it('http 开头但非 http:// / https:// 的 url（如 httpabc）不应原样返回', () => {
      vi.stubEnv('DEV', true);
      vi.stubEnv('VITE_PROXY', '/proxy');
      // 源码 startsWith('http://') / startsWith('https://') 均为 false
      // 源码不做斜杠补全，url 原样拼接到 baseUrl 后
      expect(displayImage('httpabc.png')).toBe('/proxy/agent/adminhttpabc.png');
    });

    it('相对路径不带前导 / 时与 baseUrl 直接拼接', () => {
      vi.stubEnv('DEV', true);
      vi.stubEnv('VITE_PROXY', '/proxy');
      // 源码: VITE_PROXY + baseUrl + url，不做任何斜杠处理
      expect(displayImage('img/a.png')).toBe('/proxy/agent/adminimg/a.png');
    });
  });
});
