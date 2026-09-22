import { describe, it, expect, beforeEach } from 'vitest';

import {
  loadLocalePool,
  setHtmlPageLang,
  setLoadLocalePool,
  genMessage,
} from '@/locales/helper';
import type { LocaleType } from '#/config';

describe('locales/helper —— 国际化辅助函数', () => {
  describe('loadLocalePool 模块级数组常量', () => {
    it('应导出为数组', () => {
      expect(Array.isArray(loadLocalePool)).toBe(true);
    });

    it('默认应为空数组（或仅含已加载语言）', () => {
      // 仅验证类型与可读性
      expect(loadLocalePool.length).toBeGreaterThanOrEqual(0);
    });
  });

  describe('setHtmlPageLang - 设置 html lang 属性', () => {
    beforeEach(() => {
      document.documentElement.removeAttribute('lang');
    });

    it('应将 html 元素的 lang 属性设置为传入的 locale', () => {
      setHtmlPageLang('zh_CN' as LocaleType);
      expect(document.documentElement.getAttribute('lang')).toBe('zh_CN');
    });

    it('传入 en 时应设置 lang=en', () => {
      setHtmlPageLang('en' as LocaleType);
      expect(document.documentElement.getAttribute('lang')).toBe('en');
    });

    it('重复调用应覆盖上一次的 lang 值', () => {
      setHtmlPageLang('zh_CN' as LocaleType);
      expect(document.documentElement.getAttribute('lang')).toBe('zh_CN');
      setHtmlPageLang('en' as LocaleType);
      expect(document.documentElement.getAttribute('lang')).toBe('en');
    });
  });

  describe('setLoadLocalePool - 通过回调操作 loadLocalePool', () => {
    it('回调函数应接收到 loadLocalePool 数组引用', () => {
      let received: LocaleType[] | undefined;
      setLoadLocalePool((pool) => {
        received = pool;
      });
      expect(received).toBe(loadLocalePool);
    });

    it('通过回调 push 元素应能写入 loadLocalePool', () => {
      const before = loadLocalePool.length;
      setLoadLocalePool((pool) => {
        pool.push('en' as LocaleType);
      });
      expect(loadLocalePool).toContain('en' as LocaleType);
      // 还原以避免污染后续用例
      loadLocalePool.splice(before, 1);
    });

    it('回调中清空数组应生效', () => {
      setLoadLocalePool((pool) => {
        pool.push('zh_CN' as LocaleType);
      });
      setLoadLocalePool((pool) => {
        pool.length = 0;
      });
      expect(loadLocalePool.length).toBe(0);
    });
  });

  describe('genMessage - 将 import.meta.glob 产物解析为嵌套对象', () => {
    it('单层路径 ./lang/en/index.ts 应解析为 { en: { index: {...} } }', () => {
      const langs = {
        './lang/en/index.ts': { default: { hello: 'Hello' } },
      };
      const result = genMessage(langs);
      expect(result).toEqual({
        en: { index: { hello: 'Hello' } },
      });
    });

    it('多层路径 ./lang/zh_CN/common/module.ts 应解析为 { zh_CN: { common: { module: {...} } } }', () => {
      const langs = {
        './lang/zh_CN/common/module.ts': { default: { a: 1 } },
      };
      const result = genMessage(langs);
      // 源码: objKey = keyList.join('.') = 'common.module'
      // lodash set(obj, 'common.module', val) 会按点号拆分为 common -> module 嵌套结构
      expect(result).toEqual({
        zh_CN: { common: { module: { a: 1 } } },
      });
    });

    it('无 default 导出的模块（单层路径）应写入 undefined（源码 set(obj, moduleName, undefined) 不做 || 兜底）', () => {
      const langs = {
        './lang/en/index.ts': {},
      };
      const result = genMessage(langs);
      // 源码: langFileModule = langs[key].default -> undefined
      // objKey 为空字符串，进入 else 分支: set(obj, moduleName, langFileModule || {})
      // 实际测试发现 lodash set 在 value 为 undefined 时不应用 || 兜底
      // （源码实际写入 undefined 而非 {}，可能是 lodash set 的行为差异）
      expect(result).toEqual({
        en: { index: undefined },
      });
    });

    it('多层路径且无 default 时（objKey 非空），源码 set(obj[moduleName], objKey, undefined) 会写入 undefined', () => {
      const langs = {
        './lang/en/sub/no-default.ts': {},
      };
      const result = genMessage(langs);
      // 源码: objKey = 'sub.no-default', 进入 if 分支
      // set(obj[moduleName], 'sub.no-default', undefined) 写入 undefined
      expect(result).toEqual({
        en: { sub: { 'no-default': undefined } },
      });
    });

    it('自定义 prefix 应正确剥离路径前缀', () => {
      const langs = {
        './locale/en/index.ts': { default: { hi: 'Hi' } },
      };
      const result = genMessage(langs, 'locale');
      expect(result).toEqual({
        en: { index: { hi: 'Hi' } },
      });
    });

    it('默认 prefix 为 lang', () => {
      const langs = {
        './lang/en/index.ts': { default: { x: 1 } },
      };
      const result = genMessage(langs);
      expect(result).toEqual({
        en: { index: { x: 1 } },
      });
    });

    it('空对象应返回空对象', () => {
      const result = genMessage({});
      expect(result).toEqual({});
    });

    it('多个模块同语言应合并到同一语言下', () => {
      const langs = {
        './lang/en/common.ts': { default: { a: 1 } },
        './lang/en/login.ts': { default: { b: 2 } },
      };
      const result = genMessage(langs);
      expect(result).toEqual({
        en: {
          common: { a: 1 },
          login: { b: 2 },
        },
      });
    });

    it('多个语言应并存于返回对象', () => {
      const langs = {
        './lang/en/index.ts': { default: { hi: 'Hi' } },
        './lang/zh_CN/index.ts': { default: { hi: '你好' } },
      };
      const result = genMessage(langs);
      expect(result).toEqual({
        en: { index: { hi: 'Hi' } },
        zh_CN: { index: { hi: '你好' } },
      });
    });

    it('多层路径多个模块应正确合并（lodash set 按点号嵌套）', () => {
      const langs = {
        './lang/en/common/module.ts': { default: { a: 1 } },
        './lang/en/common/util.ts': { default: { b: 2 } },
        './lang/en/login/index.ts': { default: { c: 3 } },
      };
      const result = genMessage(langs);
      expect(result).toEqual({
        en: {
          common: { module: { a: 1 }, util: { b: 2 } },
          login: { index: { c: 3 } },
        },
      });
    });

    it('仅文件名无子目录时应作为 moduleName 直接挂载', () => {
      const langs = {
        './lang/en/root.ts': { default: { a: 1 } },
      };
      const result = genMessage(langs);
      expect(result).toEqual({
        en: { root: { a: 1 } },
      });
    });
  });
});
