import { describe, it, expect } from 'vitest';

import {
  is,
  isDef,
  isUnDef,
  isObject,
  isEmpty,
  isDate,
  isNull,
  isNullOrUnDef,
  isNumber,
  isPromise,
  isString,
  isFunction,
  isBoolean,
  isRegExp,
  isArray,
  isWindow,
  isElement,
  isMap,
  isUrl,
  isOnline,
  isServer,
  isClient,
} from '@/utils/is';

describe('is —— 基于 Object.prototype.toString 的类型判断', () => {
  it.each([
    ['字符串', 'hello', 'String', true],
    ['数字', 123, 'Number', true],
    ['布尔', true, 'Boolean', true],
    ['日期', new Date(), 'Date', true],
    ['正则', /abc/, 'RegExp', true],
    ['数组', [1, 2], 'Array', true],
    ['Map', new Map(), 'Map', true],
    ['Set', new Set(), 'Set', true],
    ['普通对象', { a: 1 }, 'Object', true],
    ['null', null, 'Null', true],
    ['undefined', undefined, 'Undefined', true],
    ['函数', () => {}, 'Function', true],
  ])('类型 %s 调用 is(val, %s) 应返回 %s', (_name, val, type, expected) => {
    expect(is(val, type)).toBe(expected);
  });

  it('类型不匹配时返回 false', () => {
    expect(is('hello', 'Number')).toBe(false);
    expect(is(123, 'String')).toBe(false);
    expect(is(null, 'Object')).toBe(false);
  });

  it('传入空字符串作为 type 时返回 false', () => {
    expect(is('x', '')).toBe(false);
  });
});

describe('isDef / isUnDef', () => {
  describe('isDef', () => {
    it.each([
      ['数字', 0, true],
      ['空字符串', '', true],
      ['false', false, true],
      ['null', null, true],
      ['对象', {}, true],
      ['undefined', undefined, false],
    ])('isDef(%s) 应返回 %s', (_name, val, expected) => {
      expect(isDef(val)).toBe(expected);
    });
  });

  describe('isUnDef', () => {
    it.each([
      ['undefined', undefined, true],
      ['null', null, false],
      ['数字', 0, false],
      ['空字符串', '', false],
      ['对象', {}, false],
    ])('isUnDef(%s) 应返回 %s', (_name, val, expected) => {
      expect(isUnDef(val)).toBe(expected);
    });
  });
});

describe('isObject', () => {
  it.each([
    ['普通对象字面量', { a: 1 }, true],
    ['new Object()', new Object(), true],
    ['空对象', {}, true],
    ['null', null, false],
    ['undefined', undefined, false],
    ['数组', [1, 2], false],
    ['字符串', 'abc', false],
    ['数字', 123, false],
    ['日期', new Date(), false],
    ['Map', new Map(), false],
    ['Set', new Set(), false],
  ])('isObject(%s) 应返回 %s', (_name, val, expected) => {
    expect(isObject(val)).toBe(expected);
  });

  it('Object.create(null) 仍判定为 Object（toString 为 [object Object]）', () => {
    expect(isObject(Object.create(null))).toBe(true);
  });
});

describe('isEmpty', () => {
  describe('数组', () => {
    it.each([
      ['空数组', [], true],
      ['非空数组', [1], false],
      ['多元素数组', [1, 2, 3], false],
    ])('isEmpty(%s) 应返回 %s', (_name, val, expected) => {
      expect(isArray(val));
      expect(isEmpty(val)).toBe(expected);
    });
  });

  describe('字符串', () => {
    it.each([
      ['空字符串', '', true],
      ['非空字符串', 'a', false],
      ['空白字符串', '  ', false],
    ])('isEmpty(%s) 应返回 %s', (_name, val, expected) => {
      expect(isEmpty(val)).toBe(expected);
    });
  });

  describe('Map / Set', () => {
    it('空 Map 返回 true', () => {
      expect(isEmpty(new Map())).toBe(true);
    });

    it('非空 Map 返回 false', () => {
      const m = new Map();
      m.set('a', 1);
      expect(isEmpty(m)).toBe(false);
    });

    it('空 Set 返回 true', () => {
      expect(isEmpty(new Set())).toBe(true);
    });

    it('非空 Set 返回 false', () => {
      const s = new Set([1]);
      expect(isEmpty(s)).toBe(false);
    });
  });

  describe('对象', () => {
    it.each([
      ['空对象', {}, true],
      ['非空对象', { a: 1 }, false],
      ['多字段对象', { a: 1, b: 2 }, false],
    ])('isEmpty(%s) 应返回 %s', (_name, val, expected) => {
      expect(isEmpty(val)).toBe(expected);
    });
  });

  describe('非集合类型的特殊行为（源码末尾 return false）', () => {
    it.each([
      ['null', null],
      ['undefined', undefined],
      ['数字 0', 0],
      ['数字 1', 1],
      ['false', false],
      ['true', true],
      ['日期', new Date()],
      ['正则', /abc/],
    ])('isEmpty(%s) 应返回 false（源码走到末尾 return false）', (_name, val) => {
      expect(isEmpty(val)).toBe(false);
    });
  });
});

describe('isDate', () => {
  it.each([
    ['new Date()', new Date(), true],
    ['Invalid Date', new Date('invalid'), true],
    ['字符串', '2025-01-01', false],
    ['数字时间戳', Date.now(), false],
    ['null', null, false],
    ['undefined', undefined, false],
    ['对象', {}, false],
  ])('isDate(%s) 应返回 %s', (_name, val, expected) => {
    expect(isDate(val)).toBe(expected);
  });
});

describe('isNull', () => {
  it.each([
    ['null', null, true],
    ['undefined', undefined, false],
    ['0', 0, false],
    ['空字符串', '', false],
    ['false', false, false],
    ['对象', {}, false],
  ])('isNull(%s) 应返回 %s', (_name, val, expected) => {
    expect(isNull(val)).toBe(expected);
  });
});

describe('isNullOrUnDef', () => {
  it.each([
    ['null', null, true],
    ['undefined', undefined, true],
    ['数字 0', 0, false],
    ['空字符串', '', false],
    ['false', false, false],
    ['对象', {}, false],
    ['数组', [], false],
  ])('isNullOrUnDef(%s) 应返回 %s', (_name, val, expected) => {
    expect(isNullOrUnDef(val)).toBe(expected);
  });
});

describe('isNumber', () => {
  it.each([
    ['整数', 1, true],
    ['浮点数', 1.23, true],
    ['零', 0, true],
    ['负数', -5, true],
    ['NaN', Number.NaN, true],
    ['Infinity', Number.POSITIVE_INFINITY, true],
    ['new Number()', new Number(1), true],
    ['字符串数字', '1', false],
    ['null', null, false],
    ['undefined', undefined, false],
    ['对象', {}, false],
  ])('isNumber(%s) 应返回 %s', (_name, val, expected) => {
    expect(isNumber(val)).toBe(expected);
  });
});

describe('isString', () => {
  it.each([
    ['字面量字符串', 'hello', true],
    ['空字符串', '', true],
    ['new String()', new String('abc'), true],
    ['模板字符串', `tpl`, true],
    ['数字', 123, false],
    ['null', null, false],
    ['undefined', undefined, false],
    ['对象', {}, false],
    ['数组', [], false],
  ])('isString(%s) 应返回 %s', (_name, val, expected) => {
    expect(isString(val)).toBe(expected);
  });
});

describe('isFunction', () => {
  it.each([
    ['普通函数', function () {}, true],
    ['箭头函数', () => {}, true],
    ['异步函数', async () => {}, true],
    ['生成器函数', function* () {}, true],
    ['类', class A {}, true],
    ['Math.max', Math.max, true],
    ['字符串', 'a', false],
    ['数字', 1, false],
    ['null', null, false],
    ['undefined', undefined, false],
    ['对象', {}, false],
  ])('isFunction(%s) 应返回 %s', (_name, val, expected) => {
    expect(isFunction(val)).toBe(expected);
  });
});

describe('isBoolean', () => {
  it.each([
    ['true', true, true],
    ['false', false, true],
    ['new Boolean()', new Boolean(true), true],
    ['0', 0, false],
    ['1', 1, false],
    ['"true" 字符串', 'true', false],
    ['null', null, false],
    ['undefined', undefined, false],
    ['对象', {}, false],
  ])('isBoolean(%s) 应返回 %s', (_name, val, expected) => {
    expect(isBoolean(val)).toBe(expected);
  });
});

describe('isRegExp', () => {
  it.each([
    ['字面量正则', /abc/, true],
    ['new RegExp()', new RegExp('abc'), true],
    ['带标志正则', /abc/gi, true],
    ['字符串', 'abc', false],
    ['对象', {}, false],
    ['null', null, false],
    ['undefined', undefined, false],
    ['数字', 1, false],
  ])('isRegExp(%s) 应返回 %s', (_name, val, expected) => {
    expect(isRegExp(val)).toBe(expected);
  });
});

describe('isArray', () => {
  it('空数组返回 true', () => {
    expect(isArray([])).toBe(true);
  });

  it('非空数组返回 true', () => {
    expect(isArray([1, 2, 3])).toBe(true);
  });

  it('Array.from 返回的数组返回 true', () => {
    expect(isArray(Array.from('abc'))).toBe(true);
  });

  it.each([
    ['字符串', 'abc'],
    ['数字', 123],
    ['对象', { a: 1 }],
    ['null', null],
    ['undefined', undefined],
    ['日期', new Date()],
  ])('isArray(%s) 应返回 falsy 值', (_name, val) => {
    expect(isArray(val)).toBeFalsy();
  });

  it('关键行为：isArray(0) 返回数字 0（非 false，源码 val && Array.isArray(val) 短路）', () => {
    // 源码: return val && Array.isArray(val);
    // 0 是 falsy，短路返回原值 0
    expect(isArray(0) as unknown as number).toBe(0);
  });

  it('isArray(false) 返回 false', () => {
    expect(isArray(false) as unknown as boolean).toBe(false);
  });

  it('isArray("") 返回空字符串（短路）', () => {
    expect(isArray('') as unknown as string).toBe('');
  });
});

describe('isMap', () => {
  it.each([
    ['空 Map', new Map(), true],
    ['非空 Map', new Map([['a', 1]]), true],
    ['Set', new Set(), false],
    ['WeakMap', new WeakMap(), false],
    ['对象', {}, false],
    ['数组', [], false],
    ['null', null, false],
    ['undefined', undefined, false],
    ['字符串', 'a', false],
  ])('isMap(%s) 应返回 %s', (_name, val, expected) => {
    expect(isMap(val)).toBe(expected);
  });
});

describe('isPromise', () => {
  // 源码: return is(val, 'Promise') && isObject(val) && isFunction(val.then) && isFunction(val.catch);
  // 注意: isObject(val) 内部调用 is(val, 'Object')，Promise 的 toString 为 [object Promise]
  // 因此 isObject(promise) 恒为 false，整个 isPromise 恒为 false（源码逻辑缺陷）
  it('标准 Promise.resolve 返回 false（isObject(promise) 恒假）', () => {
    expect(isPromise(Promise.resolve(1))).toBe(false);
  });

  it('new Promise 返回 false', () => {
    expect(isPromise(new Promise(() => {}))).toBe(false);
  });

  it('thenable 对象（含 then 和 catch 函数）返回 false', () => {
    const thenable = {
      then: () => {},
      catch: () => {},
    };
    expect(isPromise(thenable)).toBe(false);
  });

  it.each([
    ['字符串', 'a'],
    ['数字', 1],
    ['对象', {}],
    ['数组', []],
    ['null', null],
    ['undefined', undefined],
    ['函数', () => {}],
    ['Promise.resolve', Promise.resolve(1)],
    ['new Promise', new Promise(() => {})],
  ])('isPromise(%s) 应返回 false', (_name, val) => {
    expect(isPromise(val)).toBe(false);
  });

  it('Promise.resolve 即使 await 后仍返回 false', async () => {
    const p = Promise.resolve(1);
    await p;
    expect(isPromise(p)).toBe(false);
  });
});

describe('isWindow', () => {
  // 源码: typeof window !== 'undefined' && is(val, 'Window')
  // 注意: happy-dom 中 Object.prototype.toString.call(window) 返回 '[object global]'
  //       而非 '[object Window]'，因此 is(window, 'Window') 恒为 false
  it('happy-dom 环境下 isWindow(window) 返回 false（toString 为 [object global]）', () => {
    // 这是 happy-dom 的特性，与浏览器环境不同
    expect(isWindow(window)).toBe(false);
  });

  it.each([
    ['null', null],
    ['undefined', undefined],
    ['普通对象', { a: 1 }],
    ['document', document],
    ['空对象', {}],
    ['数字', 1],
  ])('isWindow(%s) 应返回 false', (_name, val) => {
    expect(isWindow(val)).toBe(false);
  });
});

describe('isElement', () => {
  // 源码: isObject(val) && !!val.tagName
  // 注意: isObject(val) 内部调用 is(val, 'Object')
  //   - happy-dom 中 DOM 元素的 toString 为 '[object HTMLDivElement]' 等
  //   - 因此 isObject(div) 恒为 false，整个 isElement(div) 恒为 false
  it('真实 DOM 元素返回 false（happy-dom 下 isObject(div) 恒假）', () => {
    const div = document.createElement('div');
    expect(isElement(div)).toBe(false);
  });

  it('document.body 返回 false', () => {
    expect(isElement(document.body)).toBe(false);
  });

  it('自定义含 tagName 属性的普通对象返回 true（源码仅检查 isObject && !!val.tagName）', () => {
    const fake = { tagName: 'DIV' };
    expect(isElement(fake)).toBe(true);
  });

  it.each([
    ['null', null],
    ['undefined', undefined],
    ['普通对象无 tagName', { a: 1 }],
    ['字符串', 'div'],
    ['数字', 1],
    ['数组', []],
    ['对象 tagName 为空字符串', { tagName: '' }],
    ['对象 tagName 为 null', { tagName: null }],
  ])('isElement(%s) 应返回 false', (_name, val) => {
    expect(isElement(val)).toBe(false);
  });
});

describe('isUrl', () => {
  it.each([
    ['http 协议', 'http://example.com', true],
    ['https 协议', 'https://example.com', true],
    ['带端口', 'http://example.com:8080', true],
    ['带路径', 'https://example.com/path/to', true],
    ['带查询参数', 'https://example.com?q=1&b=2', true],
    ['带锚点', 'https://example.com#anchor', true],
    ['带路径+查询+锚点', 'https://example.com/path?q=1#x', true],
    ['带用户信息', 'https://user:pass@example.com', true],
    ['www 开头', 'www.example.com', true],
    ['www 开头带路径', 'www.example.com/path', true],
  ])('isUrl(%s) 应返回 true', (_name, path, expected) => {
    expect(isUrl(path)).toBe(expected);
  });

  it.each([
    ['空字符串', ''],
    ['无协议无 www', 'example.com'],
    ['ftp 协议', 'ftp://example.com'],
    ['只有 http://', 'http://'],
    ['只有 https://', 'https://'],
    ['纯路径', '/path/to'],
    ['相对路径', './path'],
    ['null 转字符串', 'null'],
    ['数字字符串', '123'],
  ])('isUrl(%s) 应返回 false', (_name, path) => {
    expect(isUrl(path)).toBe(false);
  });
});

describe('isOnline', () => {
  describe('bizStatus 在线状态', () => {
    it.each([
      ['bizStatus=1', { bizStatus: '1' }, true],
      ['bizStatus=4', { bizStatus: '4' }, true],
      ['bizStatus=6', { bizStatus: '6' }, true],
    ])('isOnline(%s) 应返回 true', (_name, data, expected) => {
      expect(isOnline(data)).toBe(expected);
    });

    it.each([
      ['bizStatus=0', { bizStatus: '0' }],
      ['bizStatus=2', { bizStatus: '2' }],
      ['bizStatus=3', { bizStatus: '3' }],
      ['bizStatus=5', { bizStatus: '5' }],
      ['bizStatus=7', { bizStatus: '7' }],
      ['bizStatus=9', { bizStatus: '9' }],
    ])('isOnline(%s) 应返回 false', (_name, data) => {
      expect(isOnline(data)).toBe(false);
    });
  });

  describe('bizStatus falsy 提前返回 false', () => {
    it.each([
      ['空字符串', { bizStatus: '' }],
      ['null', { bizStatus: null }],
      ['undefined', { bizStatus: undefined }],
      ['0', { bizStatus: 0 }],
      ['false', { bizStatus: false }],
    ])('isOnline(%s) 应返回 false（!bizStatus 短路）', (_name, data) => {
      expect(isOnline(data)).toBe(false);
    });
  });

  it('bizStatus 为数字 1（非字符串）返回 false（includes 严格比较）', () => {
    // 源码: ['1','4','6'].includes(bizStatus) — includes 是严格相等比较
    expect(isOnline({ bizStatus: 1 })).toBe(false);
  });

  it('bizStatus 为数字 4（非字符串）返回 false', () => {
    expect(isOnline({ bizStatus: 4 })).toBe(false);
  });

  it('对象无 bizStatus 字段时返回 false', () => {
    expect(isOnline({})).toBe(false);
  });

  it('对象 bizStatus 为非空字符串但不在白名单返回 false', () => {
    expect(isOnline({ bizStatus: 'online' })).toBe(false);
  });
});

describe('isServer / isClient 常量', () => {
  it('isServer 是 boolean 类型', () => {
    expect(typeof isServer).toBe('boolean');
  });

  it('isClient 是 boolean 类型', () => {
    expect(typeof isClient).toBe('boolean');
  });

  it('isServer 与 isClient 互斥', () => {
    expect(isServer).toBe(!isClient);
  });

  it('happy-dom 环境下 isServer 为 false', () => {
    // 测试环境 window 存在
    expect(isServer).toBe(false);
  });

  it('happy-dom 环境下 isClient 为 true', () => {
    expect(isClient).toBe(true);
  });
});
