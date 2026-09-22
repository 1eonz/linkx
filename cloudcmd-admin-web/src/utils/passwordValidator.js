/**
 * 密码验证工具函数
 */

// 特殊字符正则：`~!@#$%^&*()-_=+\|[{}];:'",<.>/? 和空格
// 注意：] 放在字符类开始位置，\ 和 - 需要转义
const SPECIAL_CHAR_PATTERN = /[\]`~!@#$%^&*()\-_=+\\|[{};:'",<.>\/\? ]/

/**
 * 判断是否包含中文
 * @param {string} str - 字符串
 * @returns {boolean}
 */
export function hasChinese(str) {
  return /[\u4E00-\u9FA5]/.test(str)
}

/**
 * 判断是否包含小写字母
 * @param {string} str - 字符串
 * @returns {boolean}
 */
export function hasLowercase(str) {
  return /[a-z]/.test(str)
}

/**
 * 判断是否包含大写字母
 * @param {string} str - 字符串
 * @returns {boolean}
 */
export function hasUppercase(str) {
  return /[A-Z]/.test(str)
}

/**
 * 判断是否包含数字
 * @param {string} str - 字符串
 * @returns {boolean}
 */
export function hasNumber(str) {
  return /[0-9]/.test(str)
}

/**
 * 判断是否包含特殊字符（含空格）
 * @param {string} str - 字符串
 * @returns {boolean}
 */
export function hasSpecialChar(str) {
  return SPECIAL_CHAR_PATTERN.test(str)
}

/**
 * 统计密码包含的字符类型数量
 * @param {string} str - 密码字符串
 * @returns {number} 字符类型数量（0-4）
 */
export function countCharTypes(str) {
  return [
    hasLowercase(str),
    hasUppercase(str),
    hasNumber(str),
    hasSpecialChar(str)
  ].filter(Boolean).length
}

/**
 * 简单模式验证（返回错误信息，null表示验证通过）
 * @param {string} value - 密码值
 * @param {function} i18n - 国际化函数（可选）
 * @returns {string|null} 错误信息或null
 */
export function validateSimpleMode(value, i18n) {
  // 非空检查
  if (!value || value === '') {
    return i18n ? i18n('index.pass.enterNewPassTip') : '请输入新密码'
  }

  // 不含中文
  if (hasChinese(value)) {
    return i18n ? i18n('index.pass.passRuledCannotContainChineseCharacters') : '密码不能含有中文'
  }

  return null
}

/**
 * 复杂模式验证（返回错误信息，null表示验证通过）
 * @param {string} value - 密码值
 * @param {string} username - 用户名
 * @param {function} i18n - 国际化函数（可选）
 * @returns {string|null} 错误信息或null
 */
export function validateComplexMode(value, username, i18n) {
  // 先执行简单模式验证
  const simpleError = validateSimpleMode(value, i18n)
  if (simpleError) {
    return simpleError
  }

  // 长度检查
  if (value.length < 8) {
    return i18n ? i18n('index.pass.passRuledLength') : '密码长度至少8位'
  }

  // 至少两种字符类型
  const typeCount = countCharTypes(value)
  if (typeCount < 2) {
    return i18n
      ? i18n('index.pass.passRuleAtLeastTwoTypes')
      : '密码必须包含小写字母、大写字母、数字、特殊字符中的至少两种'
  }

  // 不能和账号一样
  if (value === username) {
    return i18n ? i18n('index.pass.passCannotEqualToUsername') : '密码不能和账号一样'
  }

  return null
}

/**
 * 确认密码验证（返回错误信息，null表示验证通过）
 * @param {string} value - 确认密码值
 * @param {string} newPassword - 新密码
 * @param {function} i18n - 国际化函数（可选）
 * @returns {string|null} 错误信息或null
 */
export function validateRepeatPassword(value, newPassword, i18n) {
  // 非空检查
  if (!value || value === '') {
    return i18n ? i18n('index.pass.enterNewPassAgainTip') : '请再次输入新密码'
  }

  // 一致性检查
  if (value !== newPassword) {
    return i18n ? i18n('index.pass.passUnIdenticalTip') : '两次输入的密码不一致'
  }

  return null
}

/**
 * 创建 Element UI 表单验证器（简单模式）
 * @param {object} vm - Vue 组件实例
 * @returns {function} Element UI 验证函数
 */
export function createSimpleValidator(vm) {
  return (rule, value, callback) => {
    const error = validateSimpleMode(value, (key) => vm.$t(key))
    if (error) {
      callback(new Error(error))
    } else {
      callback()
    }
  }
}

/**
 * 创建 Element UI 表单验证器（复杂模式）
 * @param {object} vm - Vue 组件实例
 * @param {function} getUsername - 获取用户名的函数
 * @returns {function} Element UI 验证函数
 */
export function createComplexValidator(vm, getUsername) {
  return (rule, value, callback) => {
    const username = getUsername()
    const error = validateComplexMode(value, username, (key) => vm.$t(key))
    if (error) {
      callback(new Error(error))
    } else {
      callback()
    }
  }
}

/**
 * 创建 Element UI 表单验证器（确认密码）
 * @param {object} vm - Vue 组件实例
 * @param {function} getNewPassword - 获取新密码的函数
 * @returns {function} Element UI 验证函数
 */
export function createRepeatValidator(vm, getNewPassword) {
  return (rule, value, callback) => {
    const newPassword = getNewPassword()
    const error = validateRepeatPassword(value, newPassword, (key) => vm.$t(key))
    if (error) {
      callback(new Error(error))
    } else {
      callback()
    }
  }
}
