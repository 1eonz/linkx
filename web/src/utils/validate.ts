/* eslint-disable unicorn/number-literal-case */
/**
 * @description 表单校验规则
 */

// 用户账号
export function validRegisterUserName(param) {
  const patrn = /^([a-z0-9]){0,20}$/i;
  return patrn.exec(param);
}

// 用户密码
export function validRegisterPassword(param) {
  const pattern = new RegExp("[`~!@#$^&*()=|{}':;,[\\].<>《》/?！￥…（）—【】‘；：”“。，、？]");
  const patrn = /^([a-z0-9]){0,20}$/i;
  return patrn.test(param) ? true : pattern.test(param);
}

// 字节长度不超过32
export function validUtf8LongerThanLen(str, len = 32) {
  let totalLength = 0;
  let i = 0;
  let charCode;
  for (i = 0; i < str.length; i++) {
    charCode = str.charCodeAt(i);
    if (charCode < 0x00_7f) {
      totalLength = totalLength + 1;
    } else if (charCode >= 0x00_80 && charCode <= 0x07_ff) {
      totalLength += 2;
    } else if (charCode >= 0x08_00 && charCode <= 0xff_ff) {
      totalLength += 3;
    }
  }
  return totalLength > len;
}

// 是否有特殊字符
export function validHasSpecialCharacter(param) {
  const pattern = new RegExp("[`~!@#$^&*()=|{}':;,[\\].<>《》/?！￥…（）—【】‘；：”“。，、？]");
  return pattern.test(param);
}

export function isCardreg(param) {
  const patrn = /(^\d{15}$)|(^\d{18}$)|(^\d{17}([\dX])$)/i;
  return patrn.exec(param);
}

export function isPhonereg(param) {
  const patrn = /^((0\d{2,3}-\d{7,8})|(1[3-9]\d{9}))$/;
  return patrn.exec(param);
}

// 邮箱
export function isEmailreg(param) {
  const patrn = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return patrn.exec(param);
}
