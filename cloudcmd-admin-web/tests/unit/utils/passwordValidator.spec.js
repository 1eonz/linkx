import { describe, it, expect, vi } from 'vitest';
import {
  hasChinese,
  hasLowercase,
  hasUppercase,
  hasNumber,
  hasSpecialChar,
  countCharTypes,
  validateSimpleMode,
  validateComplexMode,
  validateRepeatPassword,
  createSimpleValidator,
  createComplexValidator,
  createRepeatValidator,
} from '@/utils/passwordValidator';

describe('Utils:passwordValidator - 字符类型检测', () => {
  describe('hasChinese', () => {
    it('包含中文应返回 true', () => {
      expect(hasChinese('abc中文123')).toBe(true);
      expect(hasChinese('密')).toBe(true);
    });

    it('不含中文应返回 false', () => {
      expect(hasChinese('abc123')).toBe(false);
      expect(hasChinese('ABC!@#')).toBe(false);
      expect(hasChinese('')).toBe(false);
    });
  });

  describe('hasLowercase', () => {
    it('包含小写字母应返回 true', () => {
      expect(hasLowercase('Abc')).toBe(true);
      expect(hasLowercase('a')).toBe(true);
    });

    it('不含小写字母应返回 false', () => {
      expect(hasLowercase('ABC123')).toBe(false);
      expect(hasLowercase('')).toBe(false);
    });
  });

  describe('hasUppercase', () => {
    it('包含大写字母应返回 true', () => {
      expect(hasUppercase('aBc')).toBe(true);
      expect(hasUppercase('Z')).toBe(true);
    });

    it('不含大写字母应返回 false', () => {
      expect(hasUppercase('abc123')).toBe(false);
      expect(hasUppercase('')).toBe(false);
    });
  });

  describe('hasNumber', () => {
    it('包含数字应返回 true', () => {
      expect(hasNumber('abc1')).toBe(true);
      expect(hasNumber('0')).toBe(true);
      expect(hasNumber('9')).toBe(true);
    });

    it('不含数字应返回 false', () => {
      expect(hasNumber('abcABC')).toBe(false);
      expect(hasNumber('')).toBe(false);
    });
  });

  describe('hasSpecialChar', () => {
    it('包含常见特殊字符应返回 true', () => {
      expect(hasSpecialChar('abc!')).toBe(true);
      expect(hasSpecialChar('abc@')).toBe(true);
      expect(hasSpecialChar('abc#')).toBe(true);
      expect(hasSpecialChar('abc$')).toBe(true);
      expect(hasSpecialChar('abc%')).toBe(true);
      expect(hasSpecialChar('abc^')).toBe(true);
      expect(hasSpecialChar('abc&')).toBe(true);
      expect(hasSpecialChar('abc*')).toBe(true);
      expect(hasSpecialChar('abc(')).toBe(true);
      expect(hasSpecialChar('abc)')).toBe(true);
      expect(hasSpecialChar('abc-')).toBe(true);
      expect(hasSpecialChar('abc_')).toBe(true);
      expect(hasSpecialChar('abc=')).toBe(true);
      expect(hasSpecialChar('abc+')).toBe(true);
      expect(hasSpecialChar('abc[')).toBe(true);
      expect(hasSpecialChar('abc]')).toBe(true);
      expect(hasSpecialChar('abc{')).toBe(true);
      expect(hasSpecialChar('abc}')).toBe(true);
      expect(hasSpecialChar('abc|')).toBe(true);
      expect(hasSpecialChar('abc\\')).toBe(true);
      expect(hasSpecialChar('abc;')).toBe(true);
      expect(hasSpecialChar('abc:')).toBe(true);
      expect(hasSpecialChar("abc'")).toBe(true);
      expect(hasSpecialChar('abc"')).toBe(true);
      expect(hasSpecialChar('abc<')).toBe(true);
      expect(hasSpecialChar('abc>')).toBe(true);
      expect(hasSpecialChar('abc,')).toBe(true);
      expect(hasSpecialChar('abc.')).toBe(true);
      expect(hasSpecialChar('abc/')).toBe(true);
      expect(hasSpecialChar('abc?')).toBe(true);
      expect(hasSpecialChar('abc`')).toBe(true);
      expect(hasSpecialChar('abc~')).toBe(true);
    });

    it('包含空格应返回 true', () => {
      expect(hasSpecialChar('ab c')).toBe(true);
      expect(hasSpecialChar(' ')).toBe(true);
    });

    it('不含特殊字符应返回 false', () => {
      expect(hasSpecialChar('abc123')).toBe(false);
      expect(hasSpecialChar('ABCabc')).toBe(false);
      expect(hasSpecialChar('')).toBe(false);
    });
  });
});

describe('Utils:passwordValidator - countCharTypes', () => {
  it('空字符串应返回 0', () => {
    expect(countCharTypes('')).toBe(0);
  });

  it('仅小写字母应返回 1', () => {
    expect(countCharTypes('abcdef')).toBe(1);
  });

  it('仅大写字母应返回 1', () => {
    expect(countCharTypes('ABCDEF')).toBe(1);
  });

  it('仅数字应返回 1', () => {
    expect(countCharTypes('123456')).toBe(1);
  });

  it('仅特殊字符应返回 1', () => {
    expect(countCharTypes('!@#$%^')).toBe(1);
  });

  it('小写+大写应返回 2', () => {
    expect(countCharTypes('abcABC')).toBe(2);
  });

  it('小写+数字应返回 2', () => {
    expect(countCharTypes('abc123')).toBe(2);
  });

  it('小写+大写+数字应返回 3', () => {
    expect(countCharTypes('abcABC123')).toBe(3);
  });

  it('四种类型齐全应返回 4', () => {
    expect(countCharTypes('aB1!')).toBe(4);
  });
});

describe('Utils:passwordValidator - validateSimpleMode', () => {
  it('空值应返回错误信息（无 i18n）', () => {
    expect(validateSimpleMode('')).toBe('请输入新密码');
    expect(validateSimpleMode(null)).toBe('请输入新密码');
    expect(validateSimpleMode(undefined)).toBe('请输入新密码');
  });

  it('空值应使用 i18n 函数返回错误信息', () => {
    const i18n = vi.fn((key) => `i18n:${key}`);
    expect(validateSimpleMode('', i18n)).toBe('i18n:index.pass.enterNewPassTip');
    expect(i18n).toHaveBeenCalledWith('index.pass.enterNewPassTip');
  });

  it('包含中文应返回错误信息（无 i18n）', () => {
    expect(validateSimpleMode('abc中文')).toBe('密码不能含有中文');
  });

  it('包含中文应使用 i18n 函数返回错误信息', () => {
    const i18n = vi.fn((key) => `i18n:${key}`);
    expect(validateSimpleMode('abc中文', i18n)).toBe(
      'i18n:index.pass.passRuledCannotContainChineseCharacters',
    );
    expect(i18n).toHaveBeenCalledWith(
      'index.pass.passRuledCannotContainChineseCharacters',
    );
  });

  it('合法的简单密码应返回 null', () => {
    expect(validateSimpleMode('abc123')).toBeNull();
    expect(validateSimpleMode('ABCabc!@#')).toBeNull();
  });

  it('未传 i18n 时不应抛错', () => {
    expect(() => validateSimpleMode('合法密码123')).not.toThrow();
  });
});

describe('Utils:passwordValidator - validateComplexMode', () => {
  it('空值应返回错误信息', () => {
    expect(validateComplexMode('', 'user')).toBe('请输入新密码');
  });

  it('包含中文应返回错误信息', () => {
    expect(validateComplexMode('中文123', 'user')).toBe('密码不能含有中文');
  });

  it('长度小于 8 位应返回错误信息', () => {
    expect(validateComplexMode('aB1!', 'user')).toBe('密码长度至少8位');
  });

  it('长度等于 8 位但仅一种字符类型应返回错误信息', () => {
    expect(validateComplexMode('abcdefgh', 'user')).toBe(
      '密码必须包含小写字母、大写字母、数字、特殊字符中的至少两种',
    );
  });

  it('长度等于 8 位且两种字符类型应通过', () => {
    expect(validateComplexMode('abcdef12', 'user')).toBeNull();
  });

  it('密码等于账号应返回错误信息', () => {
    expect(validateComplexMode('abcdef12', 'abcdef12')).toBe(
      '密码不能和账号一样',
    );
  });

  it('密码不等于账号应通过', () => {
    expect(validateComplexMode('abcdef12', 'otheruser')).toBeNull();
  });

  it('满足所有规则的复杂密码应通过', () => {
    expect(validateComplexMode('aBc1234!', 'user')).toBeNull();
  });

  it('应支持 i18n 函数', () => {
    const i18n = vi.fn((key) => `i18n:${key}`);
    expect(validateComplexMode('', 'user', i18n)).toBe(
      'i18n:index.pass.enterNewPassTip',
    );
    expect(validateComplexMode('aB1!', 'user', i18n)).toBe(
      'i18n:index.pass.passRuledLength',
    );
    expect(validateComplexMode('abcdefgh', 'user', i18n)).toBe(
      'i18n:index.pass.passRuleAtLeastTwoTypes',
    );
    expect(validateComplexMode('abcdef12', 'abcdef12', i18n)).toBe(
      'i18n:index.pass.passCannotEqualToUsername',
    );
  });
});

describe('Utils:passwordValidator - validateRepeatPassword', () => {
  it('空值应返回错误信息', () => {
    expect(validateRepeatPassword('', 'newpass')).toBe('请再次输入新密码');
    expect(validateRepeatPassword(null, 'newpass')).toBe('请再次输入新密码');
    expect(validateRepeatPassword(undefined, 'newpass')).toBe('请再次输入新密码');
  });

  it('两次密码不一致应返回错误信息', () => {
    expect(validateRepeatPassword('pass1', 'pass2')).toBe('两次输入的密码不一致');
  });

  it('两次密码一致应返回 null', () => {
    expect(validateRepeatPassword('samepass', 'samepass')).toBeNull();
  });

  it('应支持 i18n 函数', () => {
    const i18n = vi.fn((key) => `i18n:${key}`);
    expect(validateRepeatPassword('', 'newpass', i18n)).toBe(
      'i18n:index.pass.enterNewPassAgainTip',
    );
    expect(validateRepeatPassword('a', 'b', i18n)).toBe(
      'i18n:index.pass.passUnIdenticalTip',
    );
    expect(i18n).toHaveBeenCalledWith('index.pass.passUnIdenticalTip');
  });

  it('两次密码均为空应通过（一致性检查）', () => {
    expect(validateRepeatPassword('', '')).toBe('请再次输入新密码');
  });
});

describe('Utils:passwordValidator - createSimpleValidator', () => {
  function createVm() {
    return {
      $t: vi.fn((key) => `i18n:${key}`),
    };
  }

  it('应返回一个验证器函数', () => {
    const vm = createVm();
    const validator = createSimpleValidator(vm);
    expect(typeof validator).toBe('function');
  });

  it('合法密码应调用 callback 无参数（验证通过）', () => {
    const vm = createVm();
    const validator = createSimpleValidator(vm);
    const callback = vi.fn();
    validator({}, 'abc123', callback);
    expect(callback).toHaveBeenCalledWith();
    expect(callback).toHaveBeenCalledTimes(1);
  });

  it('空值应 callback(new Error(错误信息))', () => {
    const vm = createVm();
    const validator = createSimpleValidator(vm);
    const callback = vi.fn();
    validator({}, '', callback);
    expect(callback).toHaveBeenCalledTimes(1);
    const error = callback.mock.calls[0][0];
    expect(error).toBeInstanceOf(Error);
    expect(error.message).toBe('i18n:index.pass.enterNewPassTip');
    expect(vm.$t).toHaveBeenCalledWith('index.pass.enterNewPassTip');
  });

  it('包含中文应 callback(new Error(错误信息))', () => {
    const vm = createVm();
    const validator = createSimpleValidator(vm);
    const callback = vi.fn();
    validator({}, 'abc中文', callback);
    const error = callback.mock.calls[0][0];
    expect(error).toBeInstanceOf(Error);
    expect(error.message).toBe('i18n:index.pass.passRuledCannotContainChineseCharacters');
    expect(vm.$t).toHaveBeenCalledWith('index.pass.passRuledCannotContainChineseCharacters');
  });

  it('rule 参数不影响验证逻辑', () => {
    const vm = createVm();
    const validator = createSimpleValidator(vm);
    const callback = vi.fn();
    validator({ required: true, trigger: 'blur' }, 'abc123', callback);
    expect(callback).toHaveBeenCalledWith();
  });
});

describe('Utils:passwordValidator - createComplexValidator', () => {
  function createVm() {
    return {
      $t: vi.fn((key) => `i18n:${key}`),
    };
  }

  it('应返回一个验证器函数', () => {
    const vm = createVm();
    const validator = createComplexValidator(vm, () => 'user');
    expect(typeof validator).toBe('function');
  });

  it('合法密码应 callback 无参数（验证通过）', () => {
    const vm = createVm();
    const getUsername = vi.fn(() => 'user');
    const validator = createComplexValidator(vm, getUsername);
    const callback = vi.fn();
    validator({}, 'aBc1234!', callback);
    expect(callback).toHaveBeenCalledWith();
    expect(getUsername).toHaveBeenCalled();
  });

  it('空值应 callback(new Error)（先走简单模式非空校验）', () => {
    const vm = createVm();
    const validator = createComplexValidator(vm, () => 'user');
    const callback = vi.fn();
    validator({}, '', callback);
    const error = callback.mock.calls[0][0];
    expect(error).toBeInstanceOf(Error);
    expect(error.message).toBe('i18n:index.pass.enterNewPassTip');
  });

  it('长度小于 8 位应 callback(new Error)', () => {
    const vm = createVm();
    const validator = createComplexValidator(vm, () => 'user');
    const callback = vi.fn();
    validator({}, 'aB1!', callback);
    const error = callback.mock.calls[0][0];
    expect(error.message).toBe('i18n:index.pass.passRuledLength');
    expect(vm.$t).toHaveBeenCalledWith('index.pass.passRuledLength');
  });

  it('字符类型不足两种应 callback(new Error)', () => {
    const vm = createVm();
    const validator = createComplexValidator(vm, () => 'user');
    const callback = vi.fn();
    validator({}, 'abcdefgh', callback);
    const error = callback.mock.calls[0][0];
    expect(error.message).toBe('i18n:index.pass.passRuleAtLeastTwoTypes');
    expect(vm.$t).toHaveBeenCalledWith('index.pass.passRuleAtLeastTwoTypes');
  });

  it('密码等于账号应 callback(new Error)', () => {
    const vm = createVm();
    const validator = createComplexValidator(vm, () => 'abcdef12');
    const callback = vi.fn();
    validator({}, 'abcdef12', callback);
    const error = callback.mock.calls[0][0];
    expect(error.message).toBe('i18n:index.pass.passCannotEqualToUsername');
    expect(vm.$t).toHaveBeenCalledWith('index.pass.passCannotEqualToUsername');
  });

  it('getUsername 应在每次校验时被调用（动态获取）', () => {
    const vm = createVm();
    let current = 'user1';
    const getUsername = vi.fn(() => current);
    const validator = createComplexValidator(vm, getUsername);
    const callback = vi.fn();
    validator({}, 'user1pass', callback); // 与 user1 不一致，通过
    expect(callback).toHaveBeenCalledWith();
    current = 'user1pass';
    validator({}, 'user1pass', callback); // 第二次与用户名一致，失败
    const error = callback.mock.calls[1][0];
    expect(error.message).toBe('i18n:index.pass.passCannotEqualToUsername');
  });
});

describe('Utils:passwordValidator - createRepeatValidator', () => {
  function createVm() {
    return {
      $t: vi.fn((key) => `i18n:${key}`),
    };
  }

  it('应返回一个验证器函数', () => {
    const vm = createVm();
    const validator = createRepeatValidator(vm, () => 'newpass');
    expect(typeof validator).toBe('function');
  });

  it('两次密码一致应 callback 无参数（验证通过）', () => {
    const vm = createVm();
    const getNewPassword = vi.fn(() => 'samepass');
    const validator = createRepeatValidator(vm, getNewPassword);
    const callback = vi.fn();
    validator({}, 'samepass', callback);
    expect(callback).toHaveBeenCalledWith();
    expect(getNewPassword).toHaveBeenCalled();
  });

  it('空值应 callback(new Error)', () => {
    const vm = createVm();
    const validator = createRepeatValidator(vm, () => 'newpass');
    const callback = vi.fn();
    validator({}, '', callback);
    const error = callback.mock.calls[0][0];
    expect(error).toBeInstanceOf(Error);
    expect(error.message).toBe('i18n:index.pass.enterNewPassAgainTip');
    expect(vm.$t).toHaveBeenCalledWith('index.pass.enterNewPassAgainTip');
  });

  it('两次密码不一致应 callback(new Error)', () => {
    const vm = createVm();
    const validator = createRepeatValidator(vm, () => 'newpass');
    const callback = vi.fn();
    validator({}, 'different', callback);
    const error = callback.mock.calls[0][0];
    expect(error.message).toBe('i18n:index.pass.passUnIdenticalTip');
    expect(vm.$t).toHaveBeenCalledWith('index.pass.passUnIdenticalTip');
  });

  it('getNewPassword 应在每次校验时被调用（动态获取）', () => {
    const vm = createVm();
    let current = 'pass1';
    const getNewPassword = vi.fn(() => current);
    const validator = createRepeatValidator(vm, getNewPassword);
    const callback = vi.fn();
    validator({}, 'pass1', callback); // 一致，通过
    expect(callback).toHaveBeenCalledWith();
    current = 'pass2';
    validator({}, 'pass1', callback); // 不一致，失败
    const error = callback.mock.calls[1][0];
    expect(error.message).toBe('i18n:index.pass.passUnIdenticalTip');
  });
});
