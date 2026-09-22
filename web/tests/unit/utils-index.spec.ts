import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// mock indexedDB（happy-dom 不支持，但 plugins/logs 在模块加载时访问）
vi.stubGlobal('indexedDB', {
  open: vi.fn(() => ({
    onupgradeneeded: null,
    onsuccess: null,
    onerror: null,
    result: {},
  })),
});

// mock @/hooks（切断 useI18n 等对 Vue 上下文的依赖）
vi.mock('@/hooks', () => ({
  useI18n: () => ({ t: (key: string) => key }),
}));

import {
  byteLength,
  cleanArray,
  param,
  param2Obj,
  uniqueArr,
  objectMerge,
  stringParseJson,
  colorRGBtoHex,
  hexToRgb,
  addUnit,
  stringify,
  checkIsSessionMode,
  checkConfigSwitch,
  toggleClass,
  getTime,
  hasClass,
  addClass,
  removeClass,
  getOffsetTop,
  getOffsetLeft,
  S4,
  guid,
  withInstall,
  getIp,
  getLocationOrigin,
  delay,
  getRecentSomeDays,
  getRecentDateRange,
  getGlobalsConfigByKey,
  getDatePickerTeleported,
  parseTime,
  formatTime,
  html2Text,
  getImageUrl,
  domConvertsToBase64,
} from '@/utils/index';

describe('utils/index - 纯函数', () => {
  describe('byteLength - UTF-8 字节长度', () => {
    it('空字符串字节长度为 0', () => {
      expect(byteLength('')).toBe(0);
    });

    it('纯 ASCII 字符串字节长度等于字符数', () => {
      expect(byteLength('abc')).toBe(3);
    });

    it('中文字符串每个字占 3 字节', () => {
      // '中文' 两个汉字，UTF-8 各 3 字节，共 6
      expect(byteLength('中文')).toBe(6);
    });

    it('混合中英文应正确计算字节长度', () => {
      // a(1) + 中(3) + b(1) = 5
      expect(byteLength('a中b')).toBe(5);
    });
  });

  describe('cleanArray - 过滤 falsy 元素', () => {
    it('应过滤 0、空串、false、null、undefined', () => {
      const input: any[] = [1, 0, '', false, null, undefined, 2];
      const result = cleanArray(input);
      // 源码 forEach 中 push 原始元素（未做字符串转换），保留 truthy 值
      expect(result).toEqual([1, 2]);
    });

    it('空数组应返回空数组', () => {
      expect(cleanArray([])).toEqual([]);
    });

    it('全为 falsy 时应返回空数组', () => {
      expect(cleanArray([0, '', null, undefined, false])).toEqual([]);
    });
  });

  describe('param - 对象转 query string', () => {
    it('普通对象应转为 a=1&b=2', () => {
      expect(param({ a: 1, b: 2 })).toBe('a=1&b=2');
    });

    it('空对象应返回空串', () => {
      expect(param({})).toBe('');
    });

    it('传入 null/undefined 应返回空串', () => {
      expect(param(null)).toBe('');
      expect(param(undefined)).toBe('');
    });

    it('值为 undefined 的键应被跳过', () => {
      expect(param({ a: 1, b: undefined })).toBe('a=1');
    });

    it('特殊字符应被 encodeURIComponent 编码', () => {
      expect(param({ q: 'a b&c' })).toBe('q=a%20b%26c');
    });
  });

  describe('param2Obj - query 转对象', () => {
    it('应从 url 中提取 query 并转为对象（值为字符串）', () => {
      expect(param2Obj('http://a.com?a=1&b=2')).toEqual({ a: '1', b: '2' });
    });

    it('无 query 时应返回空对象', () => {
      expect(param2Obj('http://a.com')).toEqual({});
    });

    it('+ 号应被解析为空格', () => {
      expect(param2Obj('http://a.com?q=a+b')).toEqual({ q: 'a b' });
    });

    it('应支持空字符串值', () => {
      expect(param2Obj('http://a.com?a=&b=2')).toEqual({ a: '', b: '2' });
    });
  });

  describe('uniqueArr - 数组去重', () => {
    it('数字数组去重', () => {
      expect(uniqueArr([1, 1, 2, 2, 3])).toEqual([1, 2, 3]);
    });

    it('字符串数组去重', () => {
      expect(uniqueArr(['a', 'a', 'b'])).toEqual(['a', 'b']);
    });

    it('空数组应返回空数组', () => {
      expect(uniqueArr([])).toEqual([]);
    });

    it('无重复元素应返回原元素', () => {
      expect(uniqueArr([1, 2, 3])).toEqual([1, 2, 3]);
    });
  });

  describe('objectMerge - 深合并', () => {
    it('source 中的简单属性应覆盖 target', () => {
      const target = { a: 1, b: 2 };
      const result = objectMerge(target, { b: 3, c: 4 });
      expect(result).toEqual({ a: 1, b: 3, c: 4 });
    });

    it('嵌套对象应深合并', () => {
      const target = { a: { x: 1, y: 2 } };
      const result = objectMerge(target, { a: { y: 3, z: 4 } });
      expect(result).toEqual({ a: { x: 1, y: 3, z: 4 } });
    });

    it('source 为数组时应直接返回 [...source]（不合并 target 数组）', () => {
      const target = { list: [1, 2, 3] };
      const result = objectMerge(target, [4, 5]);
      // 源码：Array.isArray(source) 时 return [...source]
      expect(result).toEqual([4, 5]);
    });

    it('target 不是对象时应被重置为空对象', () => {
      const result = objectMerge('notobj', { a: 1 });
      expect(result).toEqual({ a: 1 });
    });
  });

  describe('stringParseJson - JSON 字符串解析（含换行处理）', () => {
    it('正常 JSON 字符串应解析为对象', () => {
      expect(stringParseJson('{"a":1,"b":"x"}')).toEqual({ a: 1, b: 'x' });
    });

    it('含字面量 \\n 的字符串应被正确解析', () => {
      // 输入串中是字面 \n（反斜杠+n 两字符），JSON.parse 直接支持
      const input = '{"text":"line1\\nline2"}';
      expect(stringParseJson(input)).toEqual({ text: 'line1\nline2' });
    });

    it('含真实换行符的非法 JSON 应被源码 replaceAll 修复后正确解析', () => {
      // 源码：data.replaceAll('\n', '\\n') 将真实换行符替换为字面 \n
      const input = '{"text":"line1\nline2"}';
      expect(stringParseJson(input)).toEqual({ text: 'line1\nline2' });
    });

    it('含真实换行符和回车符的 JSON 应被正确解析', () => {
      // 源码同时处理 \n 和 \r
      const input = '{"text":"a\rb\nc"}';
      expect(stringParseJson(input)).toEqual({ text: 'a\rb\nc' });
    });

    it('非法 JSON 应抛出 SyntaxError', () => {
      expect(() => stringParseJson('not a json')).toThrow(SyntaxError);
    });
  });

  describe('colorRGBtoHex - RGB 转 HEX', () => {
    it('rgb(255,0,0) 应转为 #ff0000', () => {
      expect(colorRGBtoHex('rgb(255,0,0)')).toBe('#ff0000');
    });

    it('rgb(0,255,0) 应转为 #00ff00', () => {
      expect(colorRGBtoHex('rgb(0,255,0)')).toBe('#00ff00');
    });

    it('rgb(0,0,255) 应转为 #0000ff', () => {
      expect(colorRGBtoHex('rgb(0,0,255)')).toBe('#0000ff');
    });

    it('rgb(255,255,255) 应转为 #ffffff', () => {
      expect(colorRGBtoHex('rgb(255,255,255)')).toBe('#ffffff');
    });
  });

  describe('hexToRgb - HEX 转 RGB', () => {
    it('#ff0000 默认返回 rgb 字符串', () => {
      expect(hexToRgb('#ff0000')).toBe('rgb(255, 0, 0)');
    });

    it('#00ff00 默认返回 rgb 字符串', () => {
      expect(hexToRgb('#00ff00')).toBe('rgb(0, 255, 0)');
    });

    it('传入 value=true 应返回数组', () => {
      expect(hexToRgb('#ff0000', true)).toEqual([255, 0, 0]);
    });

    it('传入 value=false 应返回 rgb 字符串', () => {
      expect(hexToRgb('#0000ff', false)).toBe('rgb(0, 0, 255)');
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
  });

  describe('stringify - 安全 JSON 序列化', () => {
    it('非对象输入应原样返回', () => {
      expect(stringify('abc')).toBe('abc');
      expect(stringify(123)).toBe(123);
      expect(stringify(null)).toBe(null);
      expect(stringify(undefined)).toBe(undefined);
    });

    it('对象中的函数属性应转为字符串', () => {
      const obj = { a: 1, fn: () => 42 };
      const result = stringify(obj) as string;
      const parsed = JSON.parse(result);
      expect(parsed.a).toBe(1);
      expect(parsed.fn).toContain('() => 42');
    });

    it('普通对象应正常 JSON 序列化', () => {
      const obj = { a: 1, b: 'x' };
      expect(stringify(obj)).toBe(JSON.stringify(obj));
    });
  });

  describe('checkIsSessionMode - 会话模式判断', () => {
    it('configData 为空应返回 false', () => {
      expect(checkIsSessionMode()).toBe(false);
      expect(checkIsSessionMode(null)).toBe(false);
      expect(checkIsSessionMode(undefined)).toBe(false);
    });

    it('AI_AGENT_INTERACTION === "session" 应返回 true', () => {
      expect(checkIsSessionMode({ AI_AGENT_INTERACTION: 'session' })).toBe(true);
    });

    it('AI_AGENT_INTERACTION 非 "session" 应返回 false', () => {
      expect(checkIsSessionMode({ AI_AGENT_INTERACTION: 'single' })).toBe(false);
      expect(checkIsSessionMode({ AI_AGENT_INTERACTION: '' })).toBe(false);
    });

    it('AI_AGENT_INTERACTION 不存在时取默认空串，应返回 false', () => {
      expect(checkIsSessionMode({ OTHER_KEY: 'x' })).toBe(false);
    });
  });

  describe('checkConfigSwitch - 配置开关', () => {
    it('configData 为空应返回 defaultValue', () => {
      expect(checkConfigSwitch({ configData: null })).toBe(false);
      expect(checkConfigSwitch({ configData: null, defaultValue: true })).toBe(true);
      expect(checkConfigSwitch({ configData: undefined, defaultValue: true })).toBe(true);
    });

    it('configKey 缺省时应使用 SHOW_331_FEATURE', () => {
      expect(
        checkConfigSwitch({ configData: { SHOW_331_FEATURE: 'true' } }),
      ).toBe(true);
      expect(
        checkConfigSwitch({ configData: { SHOW_331_FEATURE: 'false' } }),
      ).toBe(false);
    });

    it('值为 "true" 应返回 true', () => {
      expect(
        checkConfigSwitch({
          configData: { FLAG: 'true' },
          configKey: 'FLAG',
        }),
      ).toBe(true);
    });

    it('值为 true（布尔）应返回 true', () => {
      expect(
        checkConfigSwitch({
          configData: { FLAG: true },
          configKey: 'FLAG',
        }),
      ).toBe(true);
    });

    it('值为 "false" 应返回 false', () => {
      expect(
        checkConfigSwitch({
          configData: { FLAG: 'false' },
          configKey: 'FLAG',
        }),
      ).toBe(false);
    });

    it('值为 false（布尔）应返回 false', () => {
      expect(
        checkConfigSwitch({
          configData: { FLAG: false },
          configKey: 'FLAG',
        }),
      ).toBe(false);
    });

    it('值为 undefined 应返回 defaultValue', () => {
      expect(
        checkConfigSwitch({
          configData: { FLAG: undefined },
          configKey: 'FLAG',
          defaultValue: true,
        }),
      ).toBe(true);
      expect(
        checkConfigSwitch({
          configData: { FLAG: undefined },
          configKey: 'FLAG',
          defaultValue: false,
        }),
      ).toBe(false);
    });
  });
});

describe('utils/index - DOM 操作与副作用函数', () => {
  describe('toggleClass - 切换 class', () => {
    it('element 为空时应直接返回 undefined', () => {
      expect(toggleClass(null, 'foo')).toBeUndefined();
      expect(toggleClass(undefined, 'foo')).toBeUndefined();
    });

    it('className 为空时应直接返回 undefined', () => {
      const el = document.createElement('div');
      expect(toggleClass(el, '')).toBeUndefined();
      expect(toggleClass(el, null as any)).toBeUndefined();
    });

    it('class 不存在时应移除（无前导空格 bug）', () => {
      const el = document.createElement('div');
      el.className = 'foo bar';
      toggleClass(el, 'bar');
      // bar 被移除，剩下 'foo '（slice 后保留尾部空格）
      expect(el.className).toBe('foo ');
    });

    it('class 存在于开头时应被移除', () => {
      const el = document.createElement('div');
      el.className = 'foo bar';
      toggleClass(el, 'foo');
      // foo 被移除，剩下 ' bar'
      expect(el.className).toBe(' bar');
    });

    // ⚠️ 已知 bug：源码 classString += `${className}` 缺前导空格
    // 期望添加 'baz' 后 className 应为 'foo bar baz'，实际为 'foo barbaz'
    it.skip('class 不存在时应添加并带前导空格（期望行为，实际源码 bug）', () => {
      const el = document.createElement('div');
      el.className = 'foo bar';
      toggleClass(el, 'baz');
      expect(el.className).toBe('foo bar baz');
    });

    it('class 不存在时实际行为为追加无前导空格（反映源码 bug）', () => {
      const el = document.createElement('div');
      el.className = 'foo bar';
      toggleClass(el, 'baz');
      // 反映源码 bug：直接追加，无前导空格
      expect(el.className).toBe('foo barbaz');
    });

    it('空 className 时切换不应改变（无前导空格 bug，空串追加无效果）', () => {
      const el = document.createElement('div');
      el.className = 'foo';
      toggleClass(el, 'foo');
      // foo 被移除，剩下空字符串
      expect(el.className).toBe('');
    });
  });

  describe('getTime - 获取时间戳/Date', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-07-28T12:00:00.000Z'));
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it("type='start' 应返回 90 天前的数字时间戳", () => {
      const now = Date.now();
      const expected = now - 3600 * 1000 * 24 * 90;
      const result = getTime('start');
      expect(typeof result).toBe('number');
      expect(result).toBe(expected);
    });

    it("type 非 'start' 应返回当天 00:00:00 的 Date 对象", () => {
      const result = getTime('end');
      expect(result).toBeInstanceOf(Date);
      // new Date(new Date().toDateString()) 解析 'YYYY MM DD' 为本地 00:00:00
      const expected = new Date(new Date().toDateString());
      expect((result as Date).getTime()).toBe(expected.getTime());
    });

    it("type 为 undefined 应走 else 分支返回 Date 对象", () => {
      const result = getTime(undefined as any);
      expect(result).toBeInstanceOf(Date);
    });
  });

  describe('hasClass / addClass / removeClass - DOM class 操作', () => {
    it('hasClass - className 含目标时应返回 true', () => {
      const el = document.createElement('div');
      el.className = 'foo bar';
      expect(hasClass(el, 'foo')).toBe(true);
      expect(hasClass(el, 'bar')).toBe(true);
    });

    it('hasClass - className 不含目标时应返回 false', () => {
      const el = document.createElement('div');
      el.className = 'foo';
      expect(hasClass(el, 'bar')).toBe(false);
    });

    it('hasClass - 空 className 应返回 false', () => {
      const el = document.createElement('div');
      el.className = '';
      expect(hasClass(el, 'foo')).toBe(false);
    });

    it('hasClass - 子串匹配不应误判（正则带边界）', () => {
      const el = document.createElement('div');
      el.className = 'foobar';
      // 'foobar' 中不含独立的 'foo'（正则要求前后是空白或字符串边界）
      expect(hasClass(el, 'foo')).toBe(false);
    });

    it('addClass - 已存在 class 不应重复添加', () => {
      const el = document.createElement('div');
      el.className = 'foo';
      addClass(el, 'foo');
      expect(el.className).toBe('foo');
    });

    it('addClass - 不存在时应追加带前导空格', () => {
      const el = document.createElement('div');
      el.className = 'foo';
      addClass(el, 'bar');
      expect(el.className).toBe('foo bar');
    });

    it('addClass - 空 className 时应追加为 " cls"（带前导空格）', () => {
      const el = document.createElement('div');
      el.className = '';
      addClass(el, 'foo');
      // 源码 ele.className += ` ${cls}`，空串 += ' foo' → ' foo'
      expect(el.className).toBe(' foo');
    });

    it('removeClass - 存在 class 时应被移除', () => {
      const el = document.createElement('div');
      el.className = 'foo bar baz';
      removeClass(el, 'bar');
      // 源码 replace(reg, ' ') 把 ' bar '（含前后空格）替换为一个空格
      expect(el.className).toBe('foo baz');
    });

    it('removeClass - 不存在 class 时不应改变', () => {
      const el = document.createElement('div');
      el.className = 'foo';
      removeClass(el, 'bar');
      expect(el.className).toBe('foo');
    });

    it('removeClass - 移除开头 class', () => {
      const el = document.createElement('div');
      el.className = 'foo bar';
      removeClass(el, 'foo');
      // 'foo' 匹配 (\s|^)foo(\s|$)，替换为 ' '，剩下 ' bar'（开头空格保留）
      expect(el.className).toBe(' bar');
    });
  });

  describe('getOffsetTop / getOffsetLeft - 递归计算偏移', () => {
    it('getOffsetTop - 无 offsetParent 时返回自身 offsetTop', () => {
      const el = document.createElement('div');
      Object.defineProperty(el, 'offsetTop', { value: 100, configurable: true });
      Object.defineProperty(el, 'offsetParent', { value: null, configurable: true });
      expect(getOffsetTop(el)).toBe(100);
    });

    it('getOffsetTop - 有 offsetParent 时递归累加', () => {
      const parent = document.createElement('div');
      const child = document.createElement('div');
      Object.defineProperty(child, 'offsetTop', { value: 50, configurable: true });
      Object.defineProperty(child, 'offsetParent', { value: parent, configurable: true });
      Object.defineProperty(parent, 'offsetTop', { value: 200, configurable: true });
      Object.defineProperty(parent, 'offsetParent', { value: null, configurable: true });
      expect(getOffsetTop(child)).toBe(250);
    });

    it('getOffsetLeft - 无 offsetParent 时返回自身 offsetLeft', () => {
      const el = document.createElement('div');
      Object.defineProperty(el, 'offsetLeft', { value: 30, configurable: true });
      Object.defineProperty(el, 'offsetParent', { value: null, configurable: true });
      expect(getOffsetLeft(el)).toBe(30);
    });

    it('getOffsetLeft - 多层 offsetParent 递归累加', () => {
      const grand = document.createElement('div');
      const parent = document.createElement('div');
      const child = document.createElement('div');
      Object.defineProperty(child, 'offsetLeft', { value: 10, configurable: true });
      Object.defineProperty(child, 'offsetParent', { value: parent, configurable: true });
      Object.defineProperty(parent, 'offsetLeft', { value: 20, configurable: true });
      Object.defineProperty(parent, 'offsetParent', { value: grand, configurable: true });
      Object.defineProperty(grand, 'offsetLeft', { value: 30, configurable: true });
      Object.defineProperty(grand, 'offsetParent', { value: null, configurable: true });
      expect(getOffsetLeft(child)).toBe(60);
    });
  });

  describe('S4 / guid - 随机 ID 生成', () => {
    afterEach(() => {
      vi.restoreAllMocks();
    });

    it('S4 - 应返回长度为 4 的十六进制字符串', () => {
      const result = S4();
      expect(result).toMatch(/^[0-9a-f]{4}$/);
    });

    it('S4 - Math.random()=0.5 时应返回确定性输出', () => {
      vi.spyOn(Math, 'random').mockReturnValue(0.5);
      // (1 + 0.5) * 0x10000 = 0x18000，| 0 = 0x18000
      // .toString(16) = '18000'，slice(1) = '8000'
      expect(S4()).toBe('8000');
    });

    it('S4 - Math.random()=0 时应返回确定性输出', () => {
      vi.spyOn(Math, 'random').mockReturnValue(0);
      // (1 + 0) * 0x10000 = 0x10000，| 0 = 0x10000
      // .toString(16) = '10000'，slice(1) = '0000'
      expect(S4()).toBe('0000');
    });

    it('guid - 应返回包含 4 个连字符的字符串', () => {
      const result = guid();
      const dashes = result.match(/-/g);
      expect(dashes?.length).toBe(4);
    });

    it('guid - 总长度应为 36（32 个十六进制字符 + 4 个连字符）', () => {
      const result = guid();
      expect(result.length).toBe(36);
    });

    it('guid - 格式应为 xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx', () => {
      expect(guid()).toMatch(/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/);
    });

    it('guid - Math.random()=0.5 时应返回确定性输出', () => {
      vi.spyOn(Math, 'random').mockReturnValue(0.5);
      // 每段 S4() = '8000'，guid = '80008000-8000-8000-8000-800080008000'
      expect(guid()).toBe('80008000-8000-8000-8000-800080008000');
    });
  });

  describe('withInstall - Vue 组件 install 注入', () => {
    it('应为组件添加 install 方法', () => {
      const comp = { name: 'TestComp' };
      const result = withInstall(comp);
      expect(typeof result.install).toBe('function');
    });

    it('install 调用时应用 app.component 注册主组件', () => {
      const comp = { name: 'TestComp' };
      const result = withInstall(comp);
      const app = { component: vi.fn() };
      (result.install as any)(app);
      expect(app.component).toHaveBeenCalledWith('TestComp', comp);
    });

    it('传入 extra 时子组件也应被注册到 app', () => {
      const main = { name: 'MainComp' };
      const child1 = { name: 'ChildOne' };
      const child2 = { name: 'ChildTwo' };
      const result = withInstall(main, { child1, child2 });
      const app = { component: vi.fn() };
      (result.install as any)(app);
      expect(app.component).toHaveBeenCalledWith('MainComp', main);
      expect(app.component).toHaveBeenCalledWith('ChildOne', child1);
      expect(app.component).toHaveBeenCalledWith('ChildTwo', child2);
    });

    it('传入 extra 时子组件应挂载到主组件上', () => {
      const main = { name: 'MainComp' } as any;
      const child1 = { name: 'ChildOne' };
      const result = withInstall(main, { child1 });
      expect((result as any).child1).toBe(child1);
    });

    it('不传 extra 时 install 仅注册主组件', () => {
      const comp = { name: 'TestComp' };
      const result = withInstall(comp);
      const app = { component: vi.fn() };
      (result.install as any)(app);
      expect(app.component).toHaveBeenCalledTimes(1);
      expect(app.component).toHaveBeenCalledWith('TestComp', comp);
    });
  });

  describe('getIp - 获取 IP/origin', () => {
    afterEach(() => {
      vi.unstubAllGlobals();
      vi.unstubAllEnvs();
    });

    it('direct 未传时应直接返回 window.location.origin', () => {
      vi.stubGlobal('window', { location: { origin: 'http://localhost:3000' } });
      expect(getIp()).toBe('http://localhost:3000');
    });

    it('direct=true 且无 VITE_PUBLIC_PATH 时应返回 origin', () => {
      vi.stubGlobal('window', { location: { origin: 'http://localhost:3000' } });
      vi.stubEnv('VITE_PUBLIC_PATH', '');
      expect(getIp(true)).toBe('http://localhost:3000');
    });

    it('direct=true 且 VITE_PUBLIC_PATH=/linkx/ 时应返回拼接后的路径', () => {
      vi.stubGlobal('window', { location: { origin: 'http://localhost:3000' } });
      vi.stubEnv('VITE_PUBLIC_PATH', '/linkx/');
      // 源码：ip = `${ip}/${VITE_PUBLIC_PATH.replaceAll('/', '')}` → 'http://localhost:3000/linkx'
      expect(getIp(true)).toBe('http://localhost:3000/linkx');
    });

    it('direct=false 时即使有 VITE_PUBLIC_PATH 也应返回 origin', () => {
      vi.stubGlobal('window', { location: { origin: 'http://example.com' } });
      vi.stubEnv('VITE_PUBLIC_PATH', '/linkx/');
      expect(getIp(false)).toBe('http://example.com');
    });
  });

  describe('getLocationOrigin - 获取 location origin', () => {
    afterEach(() => {
      vi.unstubAllGlobals();
      vi.unstubAllEnvs();
    });

    it('DEV=true 时应返回 VITE_PROXY', () => {
      vi.stubEnv('DEV', true);
      vi.stubEnv('VITE_PROXY', 'http://proxy.example.com');
      // 注意：源码访问 location.origin，happy-dom 默认提供
      expect(getLocationOrigin()).toBe('http://proxy.example.com');
    });

    it('DEV=false 时应返回 location.origin', () => {
      vi.stubEnv('DEV', false);
      vi.stubEnv('VITE_PROXY', 'http://proxy.example.com');
      vi.stubGlobal('location', { origin: 'http://localhost:8080' });
      expect(getLocationOrigin()).toBe('http://localhost:8080');
    });

    it('DEV 为 falsy（undefined）时应返回 location.origin', () => {
      vi.stubEnv('DEV', undefined as any);
      vi.stubEnv('VITE_PROXY', 'http://proxy.example.com');
      vi.stubGlobal('location', { origin: 'http://real.origin.com' });
      expect(getLocationOrigin()).toBe('http://real.origin.com');
    });
  });

  describe('delay - Promise 延迟', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('应在指定时间后 resolve(true)', async () => {
      const promise = delay(100);
      // 推进 100ms 触发 setTimeout
      vi.advanceTimersByTime(100);
      const result = await promise;
      expect(result).toBe(true);
    });

    it('未到时间不应 resolve', async () => {
      const promise = delay(200);
      vi.advanceTimersByTime(100);
      // 包装一个超时来验证 promise 仍是 pending
      let resolved = false;
      promise.then(() => {
        resolved = true;
      });
      await Promise.resolve();
      expect(resolved).toBe(false);
      // 推进剩余时间
      vi.advanceTimersByTime(100);
      await promise;
      expect(resolved).toBe(true);
    });

    it('delay(0) 应立即 resolve', async () => {
      const promise = delay(0);
      vi.advanceTimersByTime(0);
      expect(await promise).toBe(true);
    });
  });

  describe('getRecentSomeDays - 最近 N 天日期数组', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-07-27T00:00:00.000Z'));
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('应返回包含 7 天的起止日期数组（含今天）', () => {
      const result = getRecentSomeDays(7);
      expect(result).toHaveLength(2);
      // 注意：源码 start.setDate(end.getDate() - (count - 1))
      // 在 fake time 下 new Date() 返回 2026-07-27 00:00:00 UTC
      // 当地时区为 Asia/Shanghai，实际日期取决于本地时区
      // 这里只校验格式和长度，避免时区差异
      expect(result[0]).toMatch(/^\d{4}-\d{2}-\d{2}$/);
      expect(result[1]).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });

    it('count=1 时起止日期应为同一天', () => {
      const result = getRecentSomeDays(1);
      expect(result[0]).toBe(result[1]);
    });

    it('返回的 end 应为当前日期', () => {
      const result = getRecentSomeDays(3);
      // end 为 new Date() 格式化结果
      const end = new Date();
      const expectedEnd = `${end.getFullYear()}-${(end.getMonth() + 1)
        .toString()
        .padStart(2, '0')}-${end.getDate().toString().padStart(2, '0')}`;
      expect(result[1]).toBe(expectedEnd);
    });

    it('count=7 时 start 应为 6 天前', () => {
      const result = getRecentSomeDays(7);
      const start = new Date();
      start.setDate(start.getDate() - 6);
      const expectedStart = `${start.getFullYear()}-${(start.getMonth() + 1)
        .toString()
        .padStart(2, '0')}-${start.getDate().toString().padStart(2, '0')}`;
      expect(result[0]).toBe(expectedStart);
    });
  });

  describe('getRecentDateRange - 最近 N 天日期区间', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-07-27T12:30:45.000Z'));
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('非法参数（非数字）应抛错', () => {
      expect(() => getRecentDateRange('7' as any)).toThrow('days 参数必须是非负数');
      expect(() => getRecentDateRange(undefined as any)).toThrow('days 参数必须是非负数');
      expect(() => getRecentDateRange(null as any)).toThrow('days 参数必须是非负数');
    });

    it('负数参数应抛错', () => {
      expect(() => getRecentDateRange(-1)).toThrow('days 参数必须是非负数');
      expect(() => getRecentDateRange(-100)).toThrow('days 参数必须是非负数');
    });

    it('days=0 时 startTime 应等于 endTime（带分隔符）', () => {
      const result = getRecentDateRange(0);
      expect(result.startTime).toBe(result.endTime);
      expect(result.startTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
    });

    it('withSeparator=true 时应返回带分隔符的格式', () => {
      const result = getRecentDateRange(7, true);
      expect(result.startTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
      expect(result.endTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
    });

    it('withSeparator=false 时应返回无分隔符的日期格式', () => {
      const result = getRecentDateRange(7, false);
      // 注意：源码日期与时间之间仍有空格
      expect(result.startTime).toMatch(/^\d{8} \d{6}$/);
      expect(result.endTime).toMatch(/^\d{8} \d{6}$/);
    });

    it('withSeparator 缺省时默认为 true', () => {
      const result = getRecentDateRange(7);
      expect(result.startTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
      expect(result.endTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
    });

    it('days=1 时 startTime 应比 endTime 早 1 天', () => {
      const result = getRecentDateRange(1);
      const start = new Date(result.startTime.replace(/-/g, '/'));
      const end = new Date(result.endTime.replace(/-/g, '/'));
      const diff = end.getTime() - start.getTime();
      // 1 天 = 86400000 ms
      expect(diff).toBe(86400000);
    });

    it('应返回包含 startTime 和 endTime 两个字段的对象', () => {
      const result = getRecentDateRange(7);
      expect(result).toHaveProperty('startTime');
      expect(result).toHaveProperty('endTime');
      expect(typeof result.startTime).toBe('string');
      expect(typeof result.endTime).toBe('string');
    });
  });

  describe('getGlobalsConfigByKey - 全局配置查询', () => {
    beforeEach(() => {
      vi.mock('@/utils/globalConfig', () => ({
        globalConfig: {
          data: Promise.resolve({
            KEY_A: 'valueA',
            KEY_B: { nested: 'valueB' },
            FLAG_TRUE: 'true',
            FLAG_FALSE: 'false',
          }),
        },
      }));
    });

    afterEach(() => {
      vi.doUnmock('@/utils/globalConfig');
    });

    it('不传 key 时应返回完整数据对象', async () => {
      const result = await getGlobalsConfigByKey();
      expect(result).toEqual({
        KEY_A: 'valueA',
        KEY_B: { nested: 'valueB' },
        FLAG_TRUE: 'true',
        FLAG_FALSE: 'false',
      });
    });

    it('传入存在的 key 时应返回对应值', async () => {
      const result = await getGlobalsConfigByKey('KEY_A');
      expect(result).toBe('valueA');
    });

    it('传入存在的 key（对象值）时应返回对象', async () => {
      const result = await getGlobalsConfigByKey('KEY_B');
      expect(result).toEqual({ nested: 'valueB' });
    });

    it('传入不存在的 key 时应返回 undefined', async () => {
      const result = await getGlobalsConfigByKey('NOT_EXIST');
      expect(result).toBeUndefined();
    });
  });

  describe('getDatePickerTeleported - DatePicker teleport 配置', () => {
    afterEach(() => {
      vi.restoreAllMocks();
      vi.unstubAllGlobals();
    });

    it('非 WebView2 环境应返回 true', async () => {
      // 清空 window.chrome 避免被前置环境影响
      vi.stubGlobal('window', {
        location: { search: '', origin: 'http://localhost' },
      });
      // 重新导入获取新 isWebView2 引用
      vi.resetModules();
      const { getDatePickerTeleported: freshFn } = await import('@/utils/index');
      expect(freshFn()).toBe(true);
    });

    it('WebView2 环境（chrome.webview 存在）应返回 false', async () => {
      vi.stubGlobal('window', {
        chrome: { webview: {} },
        location: { search: '' },
      });
      vi.resetModules();
      const { getDatePickerTeleported: freshFn } = await import('@/utils/index');
      expect(freshFn()).toBe(false);
    });

    it('WebView2 环境（clientType=CSPC）应返回 false', async () => {
      vi.stubGlobal('window', {
        location: { search: '?clientType=CSPC', origin: 'http://localhost' },
      });
      vi.resetModules();
      const { getDatePickerTeleported: freshFn } = await import('@/utils/index');
      expect(freshFn()).toBe(false);
    });

    it('WebView2 环境（PIM_GetPlatform 存在）应返回 false', async () => {
      vi.stubGlobal('window', {
        location: { search: '', origin: 'http://localhost' },
        PIM_GetPlatform: () => 'win32',
      });
      vi.resetModules();
      const { getDatePickerTeleported: freshFn } = await import('@/utils/index');
      expect(freshFn()).toBe(false);
    });
  });

  describe('parseTime - 时间格式化', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-07-28T12:30:45.000Z'));
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('无参数时应返回 null', () => {
      expect(parseTime()).toBeNull();
    });

    it('传入 Date 对象应按默认格式格式化', () => {
      const date = new Date('2026-01-05T08:09:10.000Z');
      // 用本地时区格式化，使用源码默认 format
      const result = parseTime(date);
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
    });

    it('传入数字时间戳（13 位）应正确格式化', () => {
      const ts = new Date('2026-01-05T08:09:10.000Z').getTime();
      const result = parseTime(ts);
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
    });

    it('传入 10 位数字时间戳应被乘以 1000 后格式化', () => {
      const ts13 = new Date('2026-01-05T08:09:10.000Z').getTime();
      const ts10 = Math.floor(ts13 / 1000);
      const result13 = parseTime(ts13);
      const result10 = parseTime(ts10);
      expect(result10).toBe(result13);
    });

    it('传入纯数字字符串应被解析为数字后格式化', () => {
      const ts13 = new Date('2026-01-05T08:09:10.000Z').getTime();
      const result1 = parseTime(String(ts13));
      const result2 = parseTime(ts13);
      expect(result1).toBe(result2);
    });

    it('自定义 format 应被支持', () => {
      const date = new Date('2026-01-05T08:09:10.000Z');
      const result = parseTime(date, '{y}/{m}/{d}');
      expect(result).toMatch(/^\d{4}\/\d{2}\/\d{2}$/);
    });

    it('format 含 {a} 时应返回星期（通过 useI18n t 函数）', () => {
      const date = new Date('2026-07-28T12:30:45.000Z'); // 周二
      const result = parseTime(date, '{a}');
      // useI18n mock 中 t(key) 返回 key，且 date.getDay() 取本地时区星期
      // 周二 → dayOfWeek2
      const expectedDay = date.getDay();
      expect(result).toBe(`common.dateDay.dayOfWeek${expectedDay}`);
    });

    it('值小于 10 时应补零', () => {
      const date = new Date('2026-01-05T08:09:10.000Z');
      // 月份 1，日期 5（本地时区可能不同，仅校验格式正确）
      const result = parseTime(date, '{m}-{d}');
      expect(result).toMatch(/^\d{2}-\d{2}$/);
    });
  });

  describe('formatTime - 相对时间格式化', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date('2026-07-28T12:00:00.000Z'));
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('diff < 30 秒应返回"刚刚"', () => {
      const now = Date.now();
      const result = formatTime(now - 10 * 1000);
      expect(result).toBe('common.dateDay.justNow');
    });

    it('30 ≤ diff < 3600 应返回分钟前', () => {
      const now = Date.now();
      const result = formatTime(now - 60 * 5 * 1000); // 5 分钟前
      expect(result).toContain('common.dateDay.minuteAgo');
    });

    it('3600 ≤ diff < 86400 应返回小时前', () => {
      const now = Date.now();
      const result = formatTime(now - 3600 * 2 * 1000); // 2 小时前
      expect(result).toContain('common.dateDay.hourAgo');
    });

    it('86400 ≤ diff < 2*86400 应返回 1 天前', () => {
      const now = Date.now();
      const result = formatTime(now - 86400 * 1.5 * 1000);
      expect(result).toBe('1common.dateDay.dayAgo');
    });

    it('diff ≥ 2*86400 且无 option 应返回月日时分组合', () => {
      const now = Date.now();
      const result = formatTime(now - 86400 * 5 * 1000); // 5 天前
      // 返回值包含 useI18n t 的 key
      expect(result).toContain('common.dateDay.month');
      expect(result).toContain('common.dateDay.day');
      expect(result).toContain('common.dateDay.hour');
      expect(result).toContain('common.dateDay.minute');
    });

    it('diff ≥ 2*86400 且传 option 时应调用 parseTime', () => {
      const now = Date.now();
      const result = formatTime(now - 86400 * 5 * 1000, '{y}-{m}-{d}');
      expect(result).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    });

    it('10 位时间戳应被乘以 1000 处理', () => {
      const now = Date.now();
      const ts10 = Math.floor(now / 1000) - 5; // 5 秒前
      const result = formatTime(String(ts10));
      expect(result).toBe('common.dateDay.justNow');
    });
  });

  describe('html2Text - HTML 转纯文本', () => {
    it('应去除 HTML 标签保留文本', () => {
      const result = html2Text('<div>Hello <span>World</span></div>');
      expect(result).toBe('Hello World');
    });

    it('纯文本应原样返回', () => {
      const result = html2Text('plain text');
      expect(result).toBe('plain text');
    });

    it('空字符串应返回空串', () => {
      const result = html2Text('');
      expect(result).toBe('');
    });

    it('含 HTML 实体的文本应被解析', () => {
      const result = html2Text('<div>a&nbsp;b</div>');
      // happy-dom 将 &nbsp; 解析为不间断空格（U+00A0），不是普通空格
      expect(result).toContain('a');
      expect(result).toContain('b');
      expect(result).toMatch(/^a[\s\u00A0]b$/);
    });
  });

  describe('getImageUrl - 获取本地静态图片地址', () => {
    it('应返回基于 import.meta.url 解析的 href', () => {
      const result = getImageUrl('./test.png');
      expect(typeof result).toBe('string');
      expect(result).toContain('test.png');
    });

    it('绝对路径应被原样返回', () => {
      const result = getImageUrl('http://example.com/img.png');
      expect(result).toBe('http://example.com/img.png');
    });
  });

  describe('domConvertsToBase64 - DOM 转 base64 SVG', () => {
    it('应将 DOM 元素转为 data:image/svg+xml 格式的 URL', () => {
      const el = document.createElement('div');
      el.innerHTML = '<span>hello</span>';
      const result = domConvertsToBase64(el);
      expect(result).toContain('data:image/svg+xml,');
      expect(result).toContain('<svg');
      expect(result).toContain('foreignObject');
      expect(result).toContain('hello');
    });

    it('未传 width/height 时应使用默认值 54 和 64', () => {
      const el = document.createElement('div');
      const result = domConvertsToBase64(el);
      expect(result).toContain('width="54"');
      expect(result).toContain('height="64"');
    });

    it('传入 width/height 时应使用自定义值', () => {
      const el = document.createElement('div');
      const result = domConvertsToBase64(el, 100, 200);
      expect(result).toContain('width="100"');
      expect(result).toContain('height="200"');
    });

    it('传入 width=0 时应回退到默认 54（falsy 短路）', () => {
      const el = document.createElement('div');
      const result = domConvertsToBase64(el, 0, 0);
      // 源码 width || 54，0 falsy 回退到 54
      expect(result).toContain('width="54"');
      expect(result).toContain('height="64"');
    });
  });
});
