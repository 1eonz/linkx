import type { SFCWithInstall } from '#/components';

import type { App } from 'vue';

import { isNumber, isString } from './is';

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

export function S4() {
  // eslint-disable-next-line unicorn/prefer-math-trunc
  return (((1 + Math.random()) * 0x1_00_00) | 0).toString(16).slice(1);
}

export function guid() {
  return `${S4() + S4()}-${S4()}-${S4()}-${S4()}-${S4()}${S4()}${S4()}`;
}

export function addUnit(value: number | string, defaultUnit = 'px') {
  if (!value) return '';
  if (isString(value)) {
    return value;
  } else if (isNumber(value)) {
    return `${value}${defaultUnit}`;
  }
}
/**
 * 处理图片地址
 * @param url 图片地址
 * @returns
 */
export function displayImage(url: string) {
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  const { DEV, VITE_PROXY } = import.meta.env;
  const baseUrl = '/agent/admin';
  return url ? `${DEV ? VITE_PROXY + baseUrl + url : baseUrl + url}` : '';
}
