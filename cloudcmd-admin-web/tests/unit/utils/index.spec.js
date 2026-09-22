import { describe, it, expect, beforeEach, vi } from 'vitest';

// mock 掉 auth，切断 @/utils/auth → @/utils/request → @/router → @/layout(.vue) 依赖链
vi.mock('@/utils/auth', () => ({
  getToken: () => 'mock-token',
}));

// mock 语言包（避免 require.context webpack API 在 Vite 下不可用）
vi.mock('@/locales/lang/cn', () => ({
  default: { message: { index: { dateDay: {
    dayOfWeek0: '星期日', dayOfWeek1: '星期一', dayOfWeek2: '星期二',
    dayOfWeek3: '星期三', dayOfWeek4: '星期四', dayOfWeek5: '星期五', dayOfWeek6: '星期六',
    month: '月', day: '日', hour: '时', minute: '分',
    justNow: '刚刚', minutesAgo: '分钟前', hoursBefore: '小时前', oneDayBefore: '一天前',
  } } } },
}));
vi.mock('@/locales/lang/en', () => ({
  default: { message: { index: { dateDay: {} } } },
}));

import {
  parseTime,
  format,
  param2Obj,
  treeDataTranslate,
  deepCopy,
  filterOrgList,
  getServiceFile,
  downloadJsonFile,
} from '@/utils/index';
import languageCn from '@/locales/lang/cn';

// index.js 顶层读取 localStorage.getItem('localLanguage')，默认走 cn 分支
beforeEach(() => {
  localStorage.setItem('localLanguage', 'cn');
});

describe('Utils:index - parseTime', () => {
  const d = new Date('2018-07-13 17:54:01');
  const languageType = languageCn.message.index;

  it('无参数应返回 null', () => {
    expect(parseTime()).toBeNull();
  });

  it('应使用默认格式 {y}-{m}-{d} {h}:{i}:{s}', () => {
    expect(parseTime(d)).toBe('2018-07-13 17:54:01');
  });

  it('应处理 Date 对象', () => {
    expect(parseTime(new Date('2020-01-01 00:00:00'))).toBe('2020-01-01 00:00:00');
  });

  it('应处理 13 位时间戳', () => {
    const ts = +d;
    expect(parseTime(ts)).toBe('2018-07-13 17:54:01');
  });

  it('应处理 10 位时间戳（秒）', () => {
    const ts = Math.floor(+d / 1000);
    expect(parseTime(ts)).toBe('2018-07-13 17:54:01');
  });

  it('应处理字符串形式的时间戳', () => {
    const ts = String(Math.floor(+d / 1000));
    expect(parseTime(ts)).toBe('2018-07-13 17:54:01');
  });

  it('应处理 13 位字符串时间戳（数字字符串会被 parseInt）', () => {
    const ts = String(+d);
    expect(parseTime(ts)).toBe('2018-07-13 17:54:01');
  });

  it('应支持自定义格式 {y}-{m}-{d} {h}:{i}', () => {
    expect(parseTime(d, '{y}-{m}-{d} {h}:{i}')).toBe('2018-07-13 17:54');
  });

  it('应支持自定义格式 {y}-{m}-{d}', () => {
    expect(parseTime(d, '{y}-{m}-{d}')).toBe('2018-07-13');
  });

  it('应支持自定义格式 {y}/{m}/{d} {h}-{i}', () => {
    expect(parseTime(d, '{y}/{m}/{d} {h}-{i}')).toBe('2018/07/13 17-54');
  });

  it('应支持只取年份 {y}', () => {
    expect(parseTime(d, '{y}')).toBe('2018');
  });

  it('应支持只取月份 {m}', () => {
    expect(parseTime(d, '{m}')).toBe('07');
  });

  it('应支持只取日 {d}', () => {
    expect(parseTime(d, '{d}')).toBe('13');
  });

  it('应支持只取小时 {h}', () => {
    expect(parseTime(d, '{h}')).toBe('17');
  });

  it('应支持只取分钟 {i}', () => {
    expect(parseTime(d, '{i}')).toBe('54');
  });

  it('应支持只取秒 {s}', () => {
    expect(parseTime(d, '{s}')).toBe('01');
  });

  it('应返回星期几 {a}', () => {
    // 2018-07-13 是星期五
    expect(parseTime(d, '{a}')).toBe(languageType.dateDay.dayOfWeek5);
  });

  it('星期日应返回 dayOfWeek0', () => {
    const sunday = new Date('2018-07-15 12:00:00'); // 星期日
    expect(parseTime(sunday, '{a}')).toBe(languageType.dateDay.dayOfWeek0);
  });

  it('星期一应返回 dayOfWeek1', () => {
    const monday = new Date('2018-07-16 12:00:00');
    expect(parseTime(monday, '{a}')).toBe(languageType.dateDay.dayOfWeek1);
  });

  it('个位数月份/日期应补零', () => {
    const d2 = new Date('2018-01-05 08:09:05');
    expect(parseTime(d2)).toBe('2018-01-05 08:09:05');
  });
});

describe('Utils:index - format', () => {
  it('应格式化为 yyyy-MM-dd HH:mm:ss', () => {
    expect(format(new Date('2018-07-13 17:54:01'))).toBe('2018-07-13 17:54:01');
  });

  it('应格式化另一个日期', () => {
    expect(format(new Date('2020-01-01 00:00:00'))).toBe('2020-01-01 00:00:00');
  });

  it('个位数月份/日期/时分秒应补零', () => {
    expect(format(new Date('2018-01-05 08:09:05'))).toBe('2018-01-05 08:09:05');
  });

  it('应支持两位数月份', () => {
    expect(format(new Date('2018-12-31 23:59:59'))).toBe('2018-12-31 23:59:59');
  });

  it('应支持时间戳输入', () => {
    const ts = +new Date('2018-07-13 17:54:01');
    expect(format(ts)).toBe('2018-07-13 17:54:01');
  });
});

describe('Utils:index - param2Obj', () => {
  it('应将 query 字符串转为对象', () => {
    expect(param2Obj('http://a.com?name=alice&age=18')).toEqual({
      name: 'alice',
      age: '18',
    });
  });

  it('应将 + 号转换为空格', () => {
    expect(param2Obj('http://a.com?name=alice+bob')).toEqual({
      name: 'alice bob',
    });
  });

  it('无 query 时应返回空对象', () => {
    expect(param2Obj('http://a.com')).toEqual({});
  });

  it('只有问号无参数时应返回空对象', () => {
    expect(param2Obj('http://a.com?')).toEqual({});
  });

  it('应处理多个参数', () => {
    const result = param2Obj('http://a.com?a=1&b=2&c=3');
    expect(result).toEqual({ a: '1', b: '2', c: '3' });
  });

  it('应处理 URL 编码的值', () => {
    const result = param2Obj('http://a.com?name=' + encodeURIComponent('中文'));
    expect(result).toEqual({ name: '中文' });
  });

  it('应处理空字符串值', () => {
    expect(param2Obj('http://a.com?key=')).toEqual({ key: '' });
  });

  it('应处理包含特殊字符的值', () => {
    expect(param2Obj('http://a.com?key=a+b+c')).toEqual({ key: 'a b c' });
  });
});

describe('Utils:index - treeDataTranslate', () => {
  it('空数组应返回空数组', () => {
    expect(treeDataTranslate([])).toEqual([]);
  });

  it('扁平数据应转换为树形结构', () => {
    const data = [
      { id: 1, parentId: 0, name: 'root' },
      { id: 2, parentId: 1, name: 'child1' },
      { id: 3, parentId: 1, name: 'child2' },
    ];
    const result = treeDataTranslate(data);
    expect(result.length).toBe(1);
    expect(result[0].id).toBe(1);
    expect(result[0].children).toBeDefined();
    expect(result[0].children.length).toBe(2);
    expect(result[0].children[0].id).toBe(2);
    expect(result[0].children[1].id).toBe(3);
  });

  it('根节点的 _level 应为 1', () => {
    const data = [
      { id: 1, parentId: 0, name: 'root' },
      { id: 2, parentId: 1, name: 'child' },
    ];
    const result = treeDataTranslate(data);
    expect(result[0]._level).toBe(1);
  });

  it('子节点的 _level 应为父节点 + 1', () => {
    const data = [
      { id: 1, parentId: 0, name: 'root' },
      { id: 2, parentId: 1, name: 'child' },
      { id: 3, parentId: 2, name: 'grandchild' },
    ];
    const result = treeDataTranslate(data);
    expect(result[0]._level).toBe(1);
    expect(result[0].children[0]._level).toBe(2);
    expect(result[0].children[0].children[0]._level).toBe(3);
  });

  it('无子节点的项不应有 children 属性', () => {
    const data = [{ id: 1, parentId: 0, name: 'root' }];
    const result = treeDataTranslate(data);
    expect(result[0].children).toBeUndefined();
  });

  it('应支持自定义 id 和 parentId 字段名', () => {
    const data = [
      { key: 'a', parentKey: '', name: 'root' },
      { key: 'b', parentKey: 'a', name: 'child' },
    ];
    const result = treeDataTranslate(data, 'key', 'parentKey');
    expect(result.length).toBe(1);
    expect(result[0].key).toBe('a');
    expect(result[0].children[0].key).toBe('b');
  });

  it('应处理多个根节点', () => {
    const data = [
      { id: 1, parentId: 0, name: 'root1' },
      { id: 2, parentId: 0, name: 'root2' },
    ];
    const result = treeDataTranslate(data);
    expect(result.length).toBe(2);
  });

  it('应处理 id === parentId 的自环情况（不作为子节点）', () => {
    const data = [
      { id: 1, parentId: 1, name: 'self' },
    ];
    const result = treeDataTranslate(data);
    expect(result.length).toBe(1);
    expect(result[0].children).toBeUndefined();
  });

  it('会修改入参数据（添加 children 和 _level）', () => {
    const data = [
      { id: 1, parentId: 0, name: 'root' },
      { id: 2, parentId: 1, name: 'child' },
    ];
    treeDataTranslate(data);
    expect(data[0].children).toBeDefined();
    expect(data[0]._level).toBe(1);
    expect(data[1]._level).toBe(2);
  });
});

describe('Utils:index - deepCopy', () => {
  it('应深拷贝简单对象', () => {
    const obj = { a: 1, b: 'string', c: true };
    const copy = deepCopy(obj);
    expect(copy).toEqual(obj);
    expect(copy).not.toBe(obj);
  });

  it('应深拷贝嵌套对象', () => {
    const obj = { a: { b: { c: 1 } } };
    const copy = deepCopy(obj);
    expect(copy).toEqual(obj);
    expect(copy.a).not.toBe(obj.a);
    expect(copy.a.b).not.toBe(obj.a.b);
  });

  it('应深拷贝数组', () => {
    const arr = [1, 2, 3, { a: 1 }];
    const copy = deepCopy(arr);
    expect(copy).toEqual(arr);
    expect(copy).not.toBe(arr);
    expect(copy[3]).not.toBe(arr[3]);
  });

  it('修改拷贝不应影响原对象', () => {
    const obj = { a: { b: 1 } };
    const copy = deepCopy(obj);
    copy.a.b = 999;
    expect(obj.a.b).toBe(1);
  });

  it('null 应返回 null', () => {
    expect(deepCopy(null)).toBeNull();
  });

  it('应深拷贝包含数组的对象', () => {
    const obj = { list: [1, 2, 3], map: { a: 'b' } };
    const copy = deepCopy(obj);
    expect(copy).toEqual(obj);
    expect(copy.list).not.toBe(obj.list);
  });
});

describe('Utils:index - filterOrgList', () => {
  it('应过滤 status !== 0 的项', () => {
    const arr = [
      { id: 1, status: 0, name: 'a' },
      { id: 2, status: 1, name: 'b' },
      { id: 3, status: 0, name: 'c' },
    ];
    const result = filterOrgList(arr);
    expect(result.length).toBe(2);
    expect(result[0].id).toBe(1);
    expect(result[1].id).toBe(3);
  });

  it('全部 status === 0 应全部保留', () => {
    const arr = [
      { id: 1, status: 0 },
      { id: 2, status: 0 },
    ];
    const result = filterOrgList(arr);
    expect(result.length).toBe(2);
  });

  it('全部 status !== 0 应返回空数组', () => {
    const arr = [
      { id: 1, status: 1 },
      { id: 2, status: 2 },
    ];
    const result = filterOrgList(arr);
    expect(result.length).toBe(0);
  });

  it('应递归过滤子节点', () => {
    const arr = [
      {
        id: 1,
        status: 0,
        children: [
          { id: 2, status: 0 },
          { id: 3, status: 1 },
        ],
      },
      {
        id: 4,
        status: 0,
        children: [
          { id: 5, status: 1 },
        ],
      },
    ];
    const result = filterOrgList(arr);
    expect(result.length).toBe(2);
    expect(result[0].children.length).toBe(1);
    expect(result[0].children[0].id).toBe(2);
    expect(result[1].children.length).toBe(0);
  });

  it('没有 children 属性的项应直接保留（status === 0）', () => {
    const arr = [{ id: 1, status: 0, name: 'a' }];
    const result = filterOrgList(arr);
    expect(result.length).toBe(1);
    expect(result[0].children).toBeUndefined();
  });

  it('空数组应返回空数组', () => {
    expect(filterOrgList([])).toEqual([]);
  });

  it('会修改入参的 children 属性', () => {
    const arr = [
      {
        id: 1,
        status: 0,
        children: [
          { id: 2, status: 0 },
          { id: 3, status: 1 },
        ],
      },
    ];
    const result = filterOrgList(arr);
    // 入参的 children 也被替换为过滤后的数组
    expect(arr[0].children.length).toBe(1);
    expect(arr[0].children[0].id).toBe(2);
    expect(result[0].children).toBe(arr[0].children);
  });

  it('应递归过滤多层嵌套子节点', () => {
    const arr = [
      {
        id: 1,
        status: 0,
        children: [
          {
            id: 2,
            status: 0,
            children: [
              { id: 3, status: 0 },
              { id: 4, status: 1 },
            ],
          },
          { id: 5, status: 1 },
        ],
      },
    ];
    const result = filterOrgList(arr);
    expect(result.length).toBe(1);
    expect(result[0].children.length).toBe(1);
    expect(result[0].children[0].id).toBe(2);
    expect(result[0].children[0].children.length).toBe(1);
    expect(result[0].children[0].children[0].id).toBe(3);
  });
});

describe('Utils:index - getServiceFile', () => {
  function createFakeXHR(status = 200, response = 'data') {
    const calls = {
      open: [],
      setRequestHeader: [],
      send: [],
      responseType: null,
    };
    const xhr = {
      status,
      response,
      open: vi.fn((method, url, async) => {
        calls.open.push({ method, url, async });
      }),
      setRequestHeader: vi.fn((name, value) => {
        calls.setRequestHeader.push({ name, value });
      }),
      send: vi.fn(() => {
        calls.send.push(undefined);
        // 模拟同步触发 onload
        Promise.resolve().then(() => xhr.onload && xhr.onload());
      }),
      onload: null,
    };
    Object.defineProperty(xhr, 'responseType', {
      get: () => calls.responseType,
      set: (v) => {
        calls.responseType = v;
      },
      configurable: true,
    });
    return { xhr, calls };
  }

  it('应正确发起 GET 请求并携带 token（status 200 resolve）', async () => {
    const { xhr, calls } = createFakeXHR(200, 'file-content');
    const XHR = vi.fn(() => xhr);
    vi.stubGlobal('XMLHttpRequest', XHR);

    const promise = getServiceFile('/path/to/file', { responseType: 'blob' });
    await promise;

    expect(XHR).toHaveBeenCalledTimes(1);
    // open 调用参数
    expect(calls.open[0].method).toBe('GET');
    expect(calls.open[0].url).toContain('/path/to/file');
    expect(calls.open[0].async).toBe(true);
    // 设置 responseType
    expect(calls.responseType).toBe('blob');
    // 设置 Authorization header
    expect(calls.setRequestHeader[0]).toEqual({
      name: 'Authorization',
      value: 'token mock-token',
    });
    // 调用 send
    expect(calls.send.length).toBe(1);
  });

  it('status 200 时应 resolve(response)', async () => {
    const { xhr } = createFakeXHR(200, 'resolved-data');
    vi.stubGlobal('XMLHttpRequest', vi.fn(() => xhr));

    const result = await getServiceFile('/file');
    expect(result).toBe('resolved-data');
  });

  it('status 非 200 时应 reject', async () => {
    const { xhr } = createFakeXHR(404, 'not found');
    vi.stubGlobal('XMLHttpRequest', vi.fn(() => xhr));

    await expect(getServiceFile('/missing')).rejects.toBeUndefined();
  });

  it('未传 config 时不设置 responseType', async () => {
    const { xhr, calls } = createFakeXHR(200, 'data');
    vi.stubGlobal('XMLHttpRequest', vi.fn(() => xhr));

    await getServiceFile('/file');
    expect(calls.responseType).toBeNull();
  });

  it('config 为空对象时不设置 responseType', async () => {
    const { xhr, calls } = createFakeXHR(200, 'data');
    vi.stubGlobal('XMLHttpRequest', vi.fn(() => xhr));

    await getServiceFile('/file', {});
    expect(calls.responseType).toBeNull();
  });

  it('url 应拼接 VUE_APP_BASE_API', async () => {
    const { xhr, calls } = createFakeXHR(200, 'data');
    vi.stubGlobal('XMLHttpRequest', vi.fn(() => xhr));

    await getServiceFile('/api/file');
    // 源码: process.env.VUE_APP_BASE_API + src
    // 测试环境中 VUE_APP_BASE_API 通常为 undefined，会被字符串拼接为 'undefined'
    const expectedBase = String(process.env.VUE_APP_BASE_API);
    expect(calls.open[0].url).toBe(expectedBase + '/api/file');
  });
});

describe('Utils:index - downloadJsonFile', () => {
  function createFakeAnchor() {
    const calls = {
      href: null,
      download: null,
      click: 0,
    };
    const anchor = {
      set href(v) {
        calls.href = v;
      },
      get href() {
        return calls.href;
      },
      set download(v) {
        calls.download = v;
      },
      get download() {
        return calls.download;
      },
      click: vi.fn(() => {
        calls.click += 1;
      }),
    };
    return { anchor, calls };
  }

  it('应创建 a 标签并触发下载', () => {
    const { anchor, calls } = createFakeAnchor();
    const createElement = vi.fn(() => anchor);
    const revokeObjectURL = vi.fn();
    const createObjectURL = vi.fn(() => 'blob:fake-url');
    vi.stubGlobal('document', {
      ...document,
      createElement,
    });
    vi.stubGlobal('URL', {
      createObjectURL,
      revokeObjectURL,
    });

    downloadJsonFile({ a: 1 }, 'myfile');

    expect(createElement).toHaveBeenCalledWith('a');
    expect(calls.href).toBe('blob:fake-url');
    expect(calls.download).toBe('myfile.json');
    expect(calls.click).toBe(1);
    expect(createObjectURL).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:fake-url');
  });

  it('应将 data 序列化为 JSON 字符串并放入 Blob', () => {
    const { anchor } = createFakeAnchor();
    const createElement = vi.fn(() => anchor);
    const revokeObjectURL = vi.fn();
    const createObjectURL = vi.fn(() => 'blob:fake-url-2');
    vi.stubGlobal('document', {
      ...document,
      createElement,
    });
    vi.stubGlobal('URL', {
      createObjectURL,
      revokeObjectURL,
    });

    const data = { name: 'test', list: [1, 2, 3] };
    downloadJsonFile(data, 'data');

    // createObjectURL 接收的参数应为 Blob
    const blobArg = createObjectURL.mock.calls[0][0];
    expect(blobArg).toBeInstanceOf(Blob);
    expect(blobArg.type).toBe('application/json');
  });

  it('应支持数组数据', () => {
    const { anchor } = createFakeAnchor();
    const createElement = vi.fn(() => anchor);
    const revokeObjectURL = vi.fn();
    const createObjectURL = vi.fn(() => 'blob:fake-url-3');
    vi.stubGlobal('document', {
      ...document,
      createElement,
    });
    vi.stubGlobal('URL', {
      createObjectURL,
      revokeObjectURL,
    });

    downloadJsonFile([1, 2, 3], 'arrayfile');

    const blobArg = createObjectURL.mock.calls[0][0];
    expect(blobArg).toBeInstanceOf(Blob);
  });

  it('应在下载完成后释放 URL 对象', () => {
    const { anchor } = createFakeAnchor();
    const createElement = vi.fn(() => anchor);
    const revokeObjectURL = vi.fn();
    const createObjectURL = vi.fn(() => 'blob:release-test');
    vi.stubGlobal('document', {
      ...document,
      createElement,
    });
    vi.stubGlobal('URL', {
      createObjectURL,
      revokeObjectURL,
    });

    downloadJsonFile({}, 'release');

    expect(revokeObjectURL).toHaveBeenCalledTimes(1);
    expect(revokeObjectURL).toHaveBeenCalledWith('blob:release-test');
  });
});
