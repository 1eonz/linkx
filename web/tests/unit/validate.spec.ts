import { describe, it, expect } from 'vitest';
import {
  validRegisterUserName,
  validRegisterPassword,
  validUtf8LongerThanLen,
  validHasSpecialCharacter,
  isCardreg,
  isPhonereg,
  isEmailreg,
} from '@/utils/validate';

describe('validRegisterUserName - 用户名校验', () => {
  it('合法: 纯小写字母', () => {
    expect(validRegisterUserName('abc')).not.toBeNull();
  });

  it('合法: 纯数字', () => {
    expect(validRegisterUserName('123')).not.toBeNull();
  });

  it('合法: 字母数字混合', () => {
    expect(validRegisterUserName('abc123')).not.toBeNull();
  });

  it('合法: 大写字母 (正则带 i 标志)', () => {
    expect(validRegisterUserName('ABC123')).not.toBeNull();
  });

  it('合法边界: 20 位字符', () => {
    expect(validRegisterUserName('a'.repeat(20))).not.toBeNull();
  });

  it('非法边界: 21 位字符超过最大长度', () => {
    expect(validRegisterUserName('a'.repeat(21))).toBeNull();
  });

  it('非法: 包含特殊字符', () => {
    expect(validRegisterUserName('abc!')).toBeNull();
  });

  it('非法: 包含中文', () => {
    expect(validRegisterUserName('用户')).toBeNull();
  });

  it('合法边界: 空字符串 (量词为 0,20)', () => {
    expect(validRegisterUserName('')).not.toBeNull();
  });
});

describe('validRegisterPassword - 密码校验', () => {
  it('合法: 纯字母数字 (命中字母数字分支返回 true)', () => {
    expect(validRegisterPassword('abc123')).toBe(true);
  });

  it('合法: 字母数字达到 20 位上限', () => {
    expect(validRegisterPassword('a'.repeat(20))).toBe(true);
  });

  it('合法: 含特殊字符 (字母数字分支不命中,特殊字符分支命中)', () => {
    expect(validRegisterPassword('abc!@#')).toBe(true);
  });

  it('合法: 仅含特殊字符', () => {
    expect(validRegisterPassword('!@#')).toBe(true);
  });

  it('合法: 含中文特殊字符 “！￥”', () => {
    expect(validRegisterPassword('abc！￥')).toBe(true);
  });

  it('非法: 既非字母数字又无特殊字符 (仅中文)', () => {
    expect(validRegisterPassword('用户')).toBe(false);
  });

  it('非法: 字母数字超过 20 位且不含特殊字符', () => {
    expect(validRegisterPassword('a'.repeat(21))).toBe(false);
  });

  it('合法边界: 空字符串 (字母数字量词 0,20 命中返回 true)', () => {
    expect(validRegisterPassword('')).toBe(true);
  });
});

describe('validUtf8LongerThanLen - UTF-8 字节长度判断', () => {
  it('空字符串字节数为 0,不超过 32', () => {
    expect(validUtf8LongerThanLen('')).toBe(false);
  });

  it('纯英文 1 字节/字符: 32 个字符刚好 32 字节返回 false', () => {
    expect(validUtf8LongerThanLen('a'.repeat(32))).toBe(false);
  });

  it('纯英文: 33 个字符共 33 字节超过 32 返回 true', () => {
    expect(validUtf8LongerThanLen('a'.repeat(33))).toBe(true);
  });

  it('中文 3 字节/字符: 10 个中文共 30 字节不超过 32 返回 false', () => {
    expect(validUtf8LongerThanLen('中'.repeat(10))).toBe(false);
  });

  it('中文 3 字节/字符: 11 个中文共 33 字节超过 32 返回 true', () => {
    expect(validUtf8LongerThanLen('中'.repeat(11))).toBe(true);
  });

  it('自定义 len 参数: 5 个英文字符 5 字节, len=4 时返回 true', () => {
    expect(validUtf8LongerThanLen('abcde', 4)).toBe(true);
  });

  it('自定义 len 参数: 5 个英文字符 5 字节, len=5 时返回 false (不大于)', () => {
    expect(validUtf8LongerThanLen('abcde', 5)).toBe(false);
  });

  it('混合字符: 1 中文(3) + 29 英文(29) = 32 字节返回 false', () => {
    expect(validUtf8LongerThanLen('中' + 'a'.repeat(29))).toBe(false);
  });

  it('混合字符: 1 中文(3) + 30 英文(30) = 33 字节返回 true', () => {
    expect(validUtf8LongerThanLen('中' + 'a'.repeat(30))).toBe(true);
  });

  it('Emoji (代理对实现): charCodeAt 命中 0xFFFF 区间,按 3 字节计算', () => {
    // 注意: 源码基于 charCodeAt 实现,Emoji 由代理对组成,
    // 高位代理范围 0xD800-0xDBFF 落入 <= 0xFFFF 分支,每段按 3 字节计算。
    // '😀' 长度为 2,2*3 = 6 字节,len=5 时返回 true。
    expect(validUtf8LongerThanLen('😀', 5)).toBe(true);
  });
});

describe('validHasSpecialCharacter - 特殊字符检测', () => {
  it('合法: 纯字母数字无特殊字符返回 false', () => {
    expect(validHasSpecialCharacter('abc123')).toBe(false);
  });

  it('合法: 空字符串无特殊字符返回 false', () => {
    expect(validHasSpecialCharacter('')).toBe(false);
  });

  it('检测到英文特殊字符 `!` 返回 true', () => {
    expect(validHasSpecialCharacter('abc!')).toBe(true);
  });

  it('检测到英文特殊字符 `@` 返回 true', () => {
    expect(validHasSpecialCharacter('a@b')).toBe(true);
  });

  it('检测到中文特殊字符 “！” 返回 true', () => {
    expect(validHasSpecialCharacter('abc！')).toBe(true);
  });

  it('检测到中文特殊字符 “￥” 返回 true', () => {
    expect(validHasSpecialCharacter('a￥b')).toBe(true);
  });

  it('检测到书名号 《》 返回 true', () => {
    expect(validHasSpecialCharacter('《abc》')).toBe(true);
  });

  it('中文普通字符不算特殊字符返回 false', () => {
    expect(validHasSpecialCharacter('用户')).toBe(false);
  });
});

describe('isCardreg - 身份证校验', () => {
  it('合法: 15 位纯数字', () => {
    expect(isCardreg('123456789012345')).not.toBeNull();
  });

  it('合法: 18 位纯数字', () => {
    expect(isCardreg('123456789012345678')).not.toBeNull();
  });

  it('合法: 18 位末尾为大写 X', () => {
    expect(isCardreg('12345678901234567X')).not.toBeNull();
  });

  it('合法: 18 位末尾为小写 x (正则带 i 标志)', () => {
    expect(isCardreg('12345678901234567x')).not.toBeNull();
  });

  it('非法: 18 位末尾为非数字非 X 字符', () => {
    expect(isCardreg('12345678901234567Y')).toBeNull();
  });

  it('非法: 长度 16 位不匹配', () => {
    expect(isCardreg('1234567890123456')).toBeNull();
  });

  it('非法: 长度 17 位纯数字不匹配', () => {
    expect(isCardreg('12345678901234567')).toBeNull();
  });

  it('非法: 包含字母(非末位 X)', () => {
    expect(isCardreg('123456A8901234567')).toBeNull();
  });

  it('非法: 空字符串', () => {
    expect(isCardreg('')).toBeNull();
  });

  it('非法: 19 位超长', () => {
    expect(isCardreg('1234567890123456789')).toBeNull();
  });

  it('非法: 含特殊字符', () => {
    expect(isCardreg('123456789012345-')).toBeNull();
  });
});

describe('isPhonereg - 手机号/座机号校验', () => {
  it('合法: 手机号 13 段', () => {
    expect(isPhonereg('13812345678')).not.toBeNull();
  });

  it('合法: 手机号 15 段', () => {
    expect(isPhonereg('15912345678')).not.toBeNull();
  });

  it('合法: 手机号 19 段', () => {
    expect(isPhonereg('19912345678')).not.toBeNull();
  });

  it('合法: 座机号 010-12345678', () => {
    expect(isPhonereg('010-12345678')).not.toBeNull();
  });

  it('合法: 座机号 0755-87654321 (区号 4 位)', () => {
    expect(isPhonereg('0755-87654321')).not.toBeNull();
  });

  it('合法: 座机号 021-1234567 (电话 7 位)', () => {
    expect(isPhonereg('021-1234567')).not.toBeNull();
  });

  it('非法: 手机号 12 段 (1 后必须 3-9)', () => {
    expect(isPhonereg('12812345678')).toBeNull();
  });

  it('非法: 手机号 11 位但第二位为 2', () => {
    expect(isPhonereg('12212345678')).toBeNull();
  });

  it('非法: 手机号 10 位', () => {
    expect(isPhonereg('1381234567')).toBeNull();
  });

  it('非法: 座机号缺横线', () => {
    expect(isPhonereg('01012345678')).toBeNull();
  });

  it('非法: 座机号区号仅 1 位', () => {
    expect(isPhonereg('0-12345678')).toBeNull();
  });

  it('非法: 座机号电话仅 6 位', () => {
    expect(isPhonereg('010-123456')).toBeNull();
  });

  it('非法: 空字符串', () => {
    expect(isPhonereg('')).toBeNull();
  });
});

describe('isEmailreg - 邮箱校验', () => {
  it('合法: 标准邮箱', () => {
    expect(isEmailreg('user@example.com')).not.toBeNull();
  });

  it('合法: 含子域', () => {
    expect(isEmailreg('user@mail.example.com')).not.toBeNull();
  });

  it('合法: 用户名含点', () => {
    expect(isEmailreg('john.doe@example.com')).not.toBeNull();
  });

  it('合法: 用户名含加号', () => {
    expect(isEmailreg('user+tag@example.com')).not.toBeNull();
  });

  it('合法: 短域名后缀', () => {
    expect(isEmailreg('a@b.cn')).not.toBeNull();
  });

  it('非法: 缺少 @', () => {
    expect(isEmailreg('userexample.com')).toBeNull();
  });

  it('非法: 缺少域名点', () => {
    expect(isEmailreg('user@example')).toBeNull();
  });

  it('非法: 缺少用户名', () => {
    expect(isEmailreg('@example.com')).toBeNull();
  });

  it('非法: 含空格', () => {
    expect(isEmailreg('user @example.com')).toBeNull();
  });

  it('非法: 多个 @ 符号', () => {
    expect(isEmailreg('user@@example.com')).toBeNull();
  });

  it('非法: 空字符串', () => {
    expect(isEmailreg('')).toBeNull();
  });
});
