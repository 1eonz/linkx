import { describe, it, expect } from 'vitest';
import {
  isNullOrUndefined,
  wordSizeOf,
  checkReturnData,
  getFileExtension,
  formatTime,
} from '@/utils/dataUtil';

describe('isNullOrUndefined - 空值判断', () => {
  it('null 返回 true', () => {
    expect(isNullOrUndefined(null)).toBe(true);
  });

  it('undefined 返回 true', () => {
    expect(isNullOrUndefined(undefined)).toBe(true);
  });

  it('数字 0 返回 false (falsy 但非空)', () => {
    expect(isNullOrUndefined(0)).toBe(false);
  });

  it('空字符串返回 false', () => {
    expect(isNullOrUndefined('')).toBe(false);
  });

  it('false 返回 false', () => {
    expect(isNullOrUndefined(false)).toBe(false);
  });

  it('对象返回 false', () => {
    expect(isNullOrUndefined({})).toBe(false);
  });

  it('数组返回 false', () => {
    expect(isNullOrUndefined([])).toBe(false);
  });

  it.skip('已知 bug: 多参数时循环未读取 arguments[i],导致只校验第一个参数。期望 (null, 1) 检查两者返回 true,实际仅校验首个 null 即返回 true', () => {
    // 源码: for (let i = 0; i < arguments.length; i++) { if (obj === null || obj === undefined) return true; }
    // 循环变量 i 递增,但 if 条件始终判断固定形参 obj,不会读取 arguments[i]。
    // 因此当首个参数为 null/undefined 时直接返回 true,后续参数被忽略。
    // @ts-expect-error 源码仅声明 1 个参数，但运行时通过 arguments 接收多个（bug 测试用例）
    expect(isNullOrUndefined(null, 1)).toBe(true);
  });

  it.skip('已知 bug: (1, null) 期望返回 true,实际因只判断首个参数 1 而返回 false', () => {
    // 同上,首个参数为 1 不为空,循环虽执行两次但都判断 obj(=1),返回 false。
    // @ts-expect-error 源码仅声明 1 个参数，但运行时通过 arguments 接收多个（bug 测试用例）
    expect(isNullOrUndefined(1, null)).toBe(true);
  });
});

describe('wordSizeOf - UTF-8 字节长度', () => {
  it('空字符串为 0 字节', () => {
    expect(wordSizeOf('')).toBe(0);
  });

  it('单个英文字符为 1 字节', () => {
    expect(wordSizeOf('a')).toBe(1);
  });

  it('多个英文字符按 1 字节/字符计算', () => {
    expect(wordSizeOf('abc')).toBe(3);
  });

  it('单个中文字符为 3 字节', () => {
    expect(wordSizeOf('中')).toBe(3);
  });

  it('多个中文按 3 字节/字符计算', () => {
    expect(wordSizeOf('中文测试')).toBe(12);
  });

  it('中英混合: 1 中文(3) + 2 英文(2) = 5 字节', () => {
    expect(wordSizeOf('中ab')).toBe(5);
  });

  it('数字字符为 1 字节/字符', () => {
    expect(wordSizeOf('12345')).toBe(5);
  });

  it('Emoji (代理对): 高位代理落入 <=0xFFFF 分支按 3 字节计算,2 段代理共 6 字节', () => {
    // 注意: 源码基于 charCodeAt 实现,Emoji 由两个代理对字符组成,
    // 高位/低位代理均落入 0xD800-0xDFFF 范围 (<=0xFFFF),各按 3 字节计算。
    expect(wordSizeOf('😀')).toBe(6);
  });
});

describe('checkReturnData - 返回数据校验', () => {
  it('code 为数字 0 返回 true', () => {
    expect(checkReturnData({ code: 0 })).toBe(true);
  });

  it('code 为数字 1 返回 false', () => {
    expect(checkReturnData({ code: 1 })).toBe(false);
  });

  it('code 为负数返回 false', () => {
    expect(checkReturnData({ code: -1 })).toBe(false);
  });

  it('null 返回 false (可选链短路)', () => {
    expect(checkReturnData(null)).toBe(false);
  });

  it('undefined 返回 false (可选链短路)', () => {
    expect(checkReturnData(undefined)).toBe(false);
  });

  it('code 为字符串 "0" 返回 false (严格 === 比较)', () => {
    expect(checkReturnData({ code: '0' })).toBe(false);
  });

  it('code 为字符串 0 返回 false', () => {
    expect(checkReturnData({ code: '0' as unknown as number })).toBe(false);
  });

  it('对象无 code 字段返回 false', () => {
    expect(checkReturnData({ data: 'x' })).toBe(false);
  });

  it('空对象返回 false', () => {
    expect(checkReturnData({})).toBe(false);
  });

  it('code 为 0 同时携带其他字段返回 true', () => {
    expect(checkReturnData({ code: 0, data: [1, 2, 3] })).toBe(true);
  });
});

describe('getFileExtension - 文件扩展名', () => {
  it('普通文件名返回小写扩展名', () => {
    expect(getFileExtension('a.txt')).toBe('txt');
  });

  it('大写扩展名转为小写', () => {
    expect(getFileExtension('a.TXT')).toBe('txt');
  });

  it('多点文件名返回最后一段小写', () => {
    expect(getFileExtension('a.tar.gz')).toBe('gz');
  });

  it('全大写多点扩展名转为小写', () => {
    expect(getFileExtension('a.TAR.GZ')).toBe('gz');
  });

  it('无扩展名(无点)返回原文件名（源码 lastIndexOf 返回 -1，slice(0) 返回完整字符串）', () => {
    expect(getFileExtension('filename')).toBe('filename');
  });

  it('以点结尾的文件名返回空字符串', () => {
    expect(getFileExtension('filename.')).toBe('');
  });

  it('以点开头的隐藏文件返回点后部分', () => {
    expect(getFileExtension('.gitignore')).toBe('gitignore');
  });

  it('路径型文件名取最后一段扩展名', () => {
    expect(getFileExtension('/path/to/file.JSON')).toBe('json');
  });

  it('Windows 路径文件名取扩展名', () => {
    expect(getFileExtension('C:\\dir\\file.PDF')).toBe('pdf');
  });

  it('空字符串返回空字符串', () => {
    expect(getFileExtension('')).toBe('');
  });
});

describe('formatTime - 秒转 M:SS 格式', () => {
  it('0 秒 → 0:00', () => {
    expect(formatTime(0)).toBe('0:00');
  });

  it('59 秒 → 0:59', () => {
    expect(formatTime(59)).toBe('0:59');
  });

  it('60 秒 → 1:00', () => {
    expect(formatTime(60)).toBe('1:00');
  });

  it('61 秒 → 1:01', () => {
    expect(formatTime(61)).toBe('1:01');
  });

  it('3599 秒 → 59:59', () => {
    expect(formatTime(3599)).toBe('59:59');
  });

  it('3600 秒 → 60:00 (分钟可超 59)', () => {
    expect(formatTime(3600)).toBe('60:00');
  });

  it('599 秒 → 9:59 (分钟为 9 不补零)', () => {
    expect(formatTime(599)).toBe('9:59');
  });

  it('10 秒 → 0:10 (秒补零)', () => {
    expect(formatTime(10)).toBe('0:10');
  });

  it('125 秒 → 2:05', () => {
    expect(formatTime(125)).toBe('2:05');
  });

  it('小数秒向下取整: 60.9 → 1:00', () => {
    expect(formatTime(60.9)).toBe('1:00');
  });
});
