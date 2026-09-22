/* eslint-disable unicorn/number-literal-case */
import type { SFCWithInstall } from '#/components';

import type { App } from 'vue';

import { useI18n } from '@/hooks';
import { isFunction, isNumber, isObject, isString } from '@/utils/is';
import { isWebView2 } from './env';
import { globalConfig } from './globalConfig';

// 生成UUID
export function generateUUID(): string {
  return 'xxxxxxxx-xxxx-4xxx-yxxx-xxxxxxxxxxxx'.replace(/[xy]/g, (c) => {
    const r = (Math.random() * 16) | 0;
    const v = c === 'x' ? r : (r & 0x3) | 0x8;
    return v.toString(16);
  });
}

// 将对象的方法转字符串
export function stringify(data) {
  if (!isObject(data) || !data) {
    return data;
  }

  const r = {};
  Object.keys(data).forEach((key) => {
    const v = data[key];
    r[key] = isFunction(v) ? v.toString() : v;
  });
  return JSON.stringify(r);
}

export function addUnit(value: number | string, defaultUnit = 'px') {
  if (!value) return '';
  if (isString(value)) {
    return value;
  } else if (isNumber(value)) {
    return `${value}${defaultUnit}`;
  }
}

export function parseTime(time, format = '{y}-{m}-{d} {h}:{i}:{s}') {
  if (arguments.length === 0) {
    return null;
  }
  let date;
  if (typeof time === 'object') {
    date = time;
  } else {
    if (typeof time === 'string' && /^\d+$/.test(time)) {
      time = Number.parseInt(time);
    }
    if (typeof time === 'number' && time.toString().length === 10) {
      time = time * 1000;
    }
    date = new Date(time);
  }
  const formatObj = {
    a: date.getDay(),
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    m: date.getMonth() + 1,
    s: date.getSeconds(),
    y: date.getFullYear(),
  };
  const time_str = format.replaceAll(/\{([ymdhisa])+\}/g, (result, key) => {
    const { t } = useI18n();
    let value = formatObj[key];
    // Note: getDay() returns 0 on Sunday
    if (key === 'a') {
      return [
        t('common.dateDay.dayOfWeek0'),
        t('common.dateDay.dayOfWeek1'),
        t('common.dateDay.dayOfWeek2'),
        t('common.dateDay.dayOfWeek3'),
        t('common.dateDay.dayOfWeek4'),
        t('common.dateDay.dayOfWeek5'),
        t('common.dateDay.dayOfWeek6'),
      ][value];
    }
    if (result.length > 0 && value < 10) {
      value = `0${value}`;
    }
    return value || 0;
  });
  return time_str;
}

export function formatTime(time, option) {
  const { t } = useI18n();
  time = `${time}`.length === 10 ? Number.parseInt(time) * 1000 : +time;
  const d = new Date(time);
  const now = Date.now();
  const diff = (now - d.getTime()) / 1000;

  if (diff < 30) {
    return t('common.dateDay.justNow');
  } else if (diff < 3600) {
    // less 1 hour
    return Math.ceil(diff / 60) + t('common.dateDay.minuteAgo');
  } else if (diff < 3600 * 24) {
    return Math.ceil(diff / 3600) + t('common.dateDay.hourAgo');
  } else if (diff < 3600 * 24 * 2) {
    return `1${t('common.dateDay.dayAgo')}`;
  }
  return option
    ? parseTime(time, option)
    : d.getMonth() +
        1 +
        t('common.dateDay.month') +
        d.getDate() +
        t('common.dateDay.day') +
        d.getHours() +
        t('common.dateDay.hour') +
        d.getMinutes() +
        t('common.dateDay.minute');
}

/**
 * @param {Sting} input value
 * @returns {number} output value
 * returns the byte length of an utf8 string
 */
export function byteLength(str) {
  let s = str.length;
  for (let i = str.length - 1; i >= 0; i--) {
    const code = str.charCodeAt(i);
    if (code > 0x7f && code <= 0x7_ff) s++;
    else if (code > 0x7_ff && code <= 0xff_ff) s += 2;
    if (code >= 0xdc_00 && code <= 0xdf_ff) i--;
  }
  return s;
}

export function cleanArray(actual) {
  const newArray: string[] = [];
  for (const element of actual) {
    if (element) {
      newArray.push(element);
    }
  }
  return newArray;
}

export function param(json) {
  if (!json) return '';

  const keys = Object.keys(json).map((key) => {
    if (json[key] === undefined) return '';
    return `${encodeURIComponent(key)}=${encodeURIComponent(json[key])}`;
  });
  return cleanArray(keys).join('&');
}

export function param2Obj(url) {
  const search = url.split('?')[1];
  if (!search) {
    return {};
  }
  return JSON.parse(
    `{"${decodeURIComponent(search)
      .replaceAll('"', String.raw`\"`)
      .replaceAll('&', '","')
      .replaceAll('=', '":"')
      .replaceAll('+', ' ')}"}`,
  );
}

export function html2Text(val) {
  const div = document.createElement('div');
  div.innerHTML = val;
  return div.textContent || div.innerText;
}

/*
 * Merges two  objects,giving the last one precedence
 */
export function objectMerge(target, source) {
  if (typeof target !== 'object') {
    target = {};
  }
  if (Array.isArray(source)) {
    return [...source];
  }
  Object.keys(source).forEach((property) => {
    const sourceProperty = source[property];
    target[property] =
      typeof sourceProperty === 'object'
        ? objectMerge(target[property], sourceProperty)
        : sourceProperty;
  });
  return target;
}

export function toggleClass(element, className) {
  if (!element || !className) {
    return;
  }
  let classString = element.className;
  const nameIndex = classString.indexOf(className);
  if (nameIndex === -1) {
    classString += `${className}`;
  } else {
    classString =
      classString.slice(0, Math.max(0, nameIndex)) +
      classString.slice(nameIndex + className.length);
  }
  element.className = classString;
}

export function getTime(type) {
  return type === 'start'
    ? Date.now() - 3600 * 1000 * 24 * 90
    : new Date(new Date().toDateString());
}

export function uniqueArr(arr) {
  return [...new Set(arr)];
}

export function hasClass(ele, cls) {
  return !!new RegExp(String.raw`(\s|^)` + cls + String.raw`(\s|$)`).test(ele.className);
}

export function addClass(ele, cls) {
  if (!hasClass(ele, cls)) ele.className += ` ${cls}`;
}

export function removeClass(ele, cls) {
  if (hasClass(ele, cls)) {
    const reg = new RegExp(String.raw`(\s|^)` + cls + String.raw`(\s|$)`);
    ele.className = ele.className.replace(reg, ' ');
  }
}

export function getOffsetTop(e) {
  let offset = e.offsetTop;
  if (e.offsetParent) {
    offset += getOffsetTop(e.offsetParent);
  }
  return offset;
}

export function getOffsetLeft(e) {
  let offset = e.offsetLeft;
  if (e.offsetParent) {
    offset += getOffsetLeft(e.offsetParent);
  }
  return offset;
}

export function S4() {
  // eslint-disable-next-line unicorn/prefer-math-trunc
  return (((1 + Math.random()) * 0x1_00_00) | 0).toString(16).slice(1);
}

export function guid() {
  return `${S4() + S4()}-${S4()}-${S4()}-${S4()}-${S4()}${S4()}${S4()}`;
}

export function stringParseJson(data) {
  const dataTemp = data.replaceAll('\n', String.raw`\n`).replaceAll('\r', String.raw`\r`);
  return JSON.parse(dataTemp);
}

/**
 *
 * @param main 父组件
 * @param extra 子组件 { child1, child2 }
 * @returns main
 */
export const withInstall = <T, E extends Record<string, any>>(main: T, extra?: E) => {
  (main as SFCWithInstall<T>).install = (app: App): void => {
    for (const comp of [main, ...Object.values(extra ?? {})]) {
      app.component(comp.name, comp);
    }
  };

  if (extra) {
    for (const [key, comp] of Object.entries(extra)) {
      (main as any)[key] = comp;
    }
  }
  return main as E & SFCWithInstall<T>;
};

export function getIp(direct?: boolean): string {
  let ip = window.location.origin;
  const { VITE_PUBLIC_PATH } = import.meta.env;
  if (direct && VITE_PUBLIC_PATH) {
    ip = `${ip}/${VITE_PUBLIC_PATH.replaceAll('/', '')}`;
  }
  return ip;
}

export function getLocationOrigin() {
  let ip = location.origin;
  const { DEV, VITE_PROXY } = import.meta.env;
  if (DEV) {
    ip = VITE_PROXY;
  }
  return ip;
}

/**
 * @description 延迟执行（用于暂时中断）
 * @param {*} time
 * @return {*}
 */
export function delay(time) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(true);
    }, time);
  });
}

/**
 * 获取本地静态图片地址
 * @param path 图片路径
 * @returns url
 */
export function getImageUrl(path) {
  return new URL(path, import.meta.url).href;
}

// 使用 canvas 将图片转成 base64
export function imageToBase64(imageUrl) {
  return new Promise((resolve, reject) => {
    const canvas = document.createElement('canvas');
    const ctx: any = canvas.getContext('2d');
    const img = new Image();
    img.crossOrigin = 'Anonymous';

    img.addEventListener('load', () => {
      canvas.width = img.width;
      canvas.height = img.height;
      ctx.drawImage(img, 0, 0, img.width, img.height);

      const dataURL = canvas.toDataURL('image/png');
      resolve(dataURL);
    });

    img.onerror = function (error) {
      reject(error);
    };

    img.src = imageUrl;
  });
}

/**
 * 将dom转换成base64 url
 * @param el => 需要全都用行内样式
 * @returns
 */
export function domConvertsToBase64(el, width?: number, height?: number) {
  const XHTML = new XMLSerializer().serializeToString(el);
  const SVGDomElement = `<svg xmlns="http://www.w3.org/2000/svg"
  width="${width || 54}" height="${height || 64}" >
      <foreignObject height="100%" width="100%">${XHTML}</foreignObject>
  </svg>`;
  const src = `data:image/svg+xml,${SVGDomElement}`;
  return src;
}

/**
 * RGB转换成16进制颜色
 * @param rgb (r,g,b)
 * @returns hex
 */
export function colorRGBtoHex(color) {
  const rgb = color.split(',');
  const r = Number.parseInt(rgb[0].split('(')[1]);
  const g = Number.parseInt(rgb[1]);
  const b = Number.parseInt(rgb[2].split(')')[0]);
  const hex = `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`;
  return hex;
}

/**
 * 16进制转换成RGB颜色
 * @param hex
 * @returns grb
 */
export function hexToRgb(hex, value?: boolean): any {
  const color = hex.replace('#', '');
  const red = Number.parseInt(color.slice(0, 2), 16);
  const green = Number.parseInt(color.slice(2, 4), 16);
  const blue = Number.parseInt(color.slice(4, 6), 16);
  if (value) {
    return [red, green, blue];
  }
  return `rgb(${red}, ${green}, ${blue})`;
}

// 获取最近几天日期
export function getRecentSomeDays(count) {
  const end = new Date();
  const start = new Date();
  start.setDate(end.getDate() - (count - 1)); // 包含今天共7天
  // 格式化为 'YYYY-MM-DD' 字符串
  const format = (date: Date) =>
    `${date.getFullYear()}-${(date.getMonth() + 1)
      .toString()
      .padStart(2, '0')}-${date.getDate().toString().padStart(2, '0')}`;
  return [format(start), format(end)];
}

// 获取最近几天日期区间

/**
 * 获取最近 N 天的日期区间（起始时间 ~ 当前时间）
 * @param {number} days - 天数
 * @param {boolean} withSeparator - 格式化是否带分隔符
 * @returns {object} { start: 起始时间, end: 结束时间 }
 */
export function getRecentDateRange(
  days: number,
  withSeparator = true,
): { endTime: string; startTime: string } {
  // 参数验证
  if (!isNumber(days) || days < 0) {
    throw new Error('days 参数必须是非负数');
  }

  const now = new Date(); // 当前时间（结束时间）
  const oneDayMs = 24 * 60 * 60 * 1000; // 1天的毫秒数：86400000

  // 计算起始时间：当前时间 - N 天的毫秒数
  const startDate = new Date(now.getTime() - days * oneDayMs);

  // 格式化起始时间和结束时间
  const endTime = formatDateTimeString(now, withSeparator);
  const startTime = formatDateTimeString(startDate, withSeparator);

  return { endTime, startTime };
}
function formatDateTimeString(date: Date, withSeparator = true): string {
  // 补零函数：单个数字前补 0（如 5 → '05'）
  const padZero = (num: number): string => num.toString().padStart(2, '0');

  const year = date.getFullYear(); // 年（4位）
  const month = padZero(date.getMonth() + 1); // 月（0-11 → 1-12，补零）
  const day = padZero(date.getDate()); // 日（1-31，补零）
  const hour = padZero(date.getHours()); // 时（0-23，补零）
  const minute = padZero(date.getMinutes()); // 分（0-59，补零）
  const second = padZero(date.getSeconds()); // 秒（0-59，补零）

  // 根据是否带分隔符返回不同格式
  if (withSeparator) {
    return `${year}-${month}-${day} ${hour}:${minute}:${second}`;
  }
  return `${year}${month}${day} ${hour}${minute}${second}`;
}

/**
 * 检查是否为会话模式
 * @param configData 全局配置数据对象
 */
export function checkIsSessionMode(configData?: any): boolean {
  if (!configData) return false;
  const aiAgentInteraction = configData.AI_AGENT_INTERACTION || '';
  return aiAgentInteraction === 'session';
}

/**
 * 检查配置开关是否开启
 * @param options 配置选项
 * @param options.configData 全局配置数据对象
 * @param options.configKey 配置项key，默认为 'SHOW_331_FEATURE'
 * @param options.defaultValue 配置不存在时的默认值，默认为 false
 */
export function checkConfigSwitch(options: {
  configData: any;
  configKey?: string;
  defaultValue?: boolean;
}): boolean {
  const { configData, configKey = 'SHOW_331_FEATURE', defaultValue = false } = options;
  if (!configData) return defaultValue;
  const switchValue = configData[configKey];
  if (switchValue === undefined) return defaultValue;
  return switchValue === 'true' || switchValue === true;
}

// 重新导出环境判断函数，保持兼容性
export { isWebView2 };

// 根据key获取对应的全局配置
export function getGlobalsConfigByKey(key?: string): Promise<any> {
  return globalConfig.data.then((data: any) => (key ? data?.[key] : data));
}

/**
 * 获取 DatePicker 的 teleported 属性值
 * WebView2 环境下需要禁用 teleport，避免弹出层焦点丢失导致闪烁
 * @returns {boolean} Browser 环境返回 true，WebView2 环境返回 false
 */
export function getDatePickerTeleported(): boolean {
  return !isWebView2();
}
