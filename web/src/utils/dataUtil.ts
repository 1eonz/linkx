/* eslint-disable unicorn/number-literal-case */

export function isNullOrUndefined(obj) {
  for (let i = 0; i < arguments.length; i++) {
    if (obj === null || obj === undefined) {
      return true;
    }
  }
  return false;
}

export function wordSizeOf(str) {
  let total = 0;
  let charCode;
  let i;
  let len;
  for (i = 0, len = str.length; i < len; i++) {
    charCode = str.charCodeAt(i);
    if (charCode <= 0x00_7f) {
      total += 1;
    } else if (charCode <= 0x07_ff) {
      total += 2;
    } else if (charCode <= 0xff_ff) {
      total += 3;
    } else {
      total += 4;
    }
  }
  return total;
}

export function checkReturnData(obj) {
  return obj?.code === 0;
}

export function getFileExtension(fileName) {
  return fileName.slice(Math.max(0, fileName.lastIndexOf('.') + 1)).toLowerCase();
}

export function formatTime(time) {
  const min = Math.floor(time / 60);
  const sec = Math.floor(time % 60);
  return `${min}:${sec < 10 ? `0${sec}` : sec}`;
}

export default {
  checkReturnData,
  formatTime,
  getFileExtension,
  isNullOrUndefined,
  wordSizeOf,
};
