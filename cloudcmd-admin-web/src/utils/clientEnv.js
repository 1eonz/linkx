/**
 * 客户端环境信息工具模块
 * 提供浏览器、操作系统、屏幕尺寸等信息的解析与缓存
 * 各端（web/H5/admin）应引用此模块，js-sdk 因打包为静态文件需内联副本并标注同步约定
 */

// 客户端类型枚举：1=PC浏览器 2=PC WebView2 3=App H5 4=Admin后台 5=RESTful开放接口 6=JS-SDK开放接口
export const CLIENT_TYPE = {
  PC: '1',
  CS: '2',
  H5: '3',
  ADMIN: '4',
  RESTFUL: '5',
  JS_SDK: '6',
};

// 模块加载时计算一次并缓存，会话内浏览器/OS/屏幕不变
const _cachedBrowserInfo = _parseBrowserInfo();
const _cachedOSInfo = _parseOSInfo();
const _cachedScreenInfo = _parseScreenInfo();

// 从UA解析浏览器信息
function _parseBrowserInfo() {
  const ua = navigator.userAgent;
  const edgeMatch = ua.match(/Edg\/(\d+)/);
  if (edgeMatch) return `Edge ${edgeMatch[1]}`;
  const chromeMatch = ua.match(/Chrome\/(\d+)/);
  if (chromeMatch) return `Chrome ${chromeMatch[1]}`;
  const criosMatch = ua.match(/CriOS\/(\d+)/);
  if (criosMatch) return `Chrome ${criosMatch[1]}`;
  const fxiosMatch = ua.match(/FxiOS\/(\d+)/);
  if (fxiosMatch) return `Firefox ${fxiosMatch[1]}`;
  const safariMatch = ua.match(/Safari\/(\d+)/);
  if (safariMatch) return `Safari ${safariMatch[1]}`;
  const firefoxMatch = ua.match(/Firefox\/(\d+)/);
  if (firefoxMatch) return `Firefox ${firefoxMatch[1]}`;
  return '';
}

// 从UA解析操作系统信息
function _parseOSInfo() {
  const ua = navigator.userAgent;
  const winMatch = ua.match(/Windows NT (\d+\.?\d*)/);
  if (winMatch) {
    const ver = winMatch[1];
    const map = { '10.0': '10', '6.3': '8.1', '6.2': '8', '6.1': '7' };
    return `Windows ${map[ver] || ver}`;
  }
  const macMatch = ua.match(/Mac OS X (\d+[._]\d+)/);
  if (macMatch) return `macOS ${macMatch[1].replace('_', '.')}`;
  const androidMatch = ua.match(/Android (\d+\.?\d*)/);
  if (androidMatch) return `Android ${androidMatch[1]}`;
  const iosMatch = ua.match(/iPhone OS (\d+_\d+)/);
  if (iosMatch) return `iOS ${iosMatch[1].replace('_', '.')}`;
  return '';
}

// 获取屏幕尺寸信息
function _parseScreenInfo() {
  if (typeof window === 'undefined') return '';
  return `${window.screen.width}x${window.screen.height}`;
}

// 获取缓存的浏览器信息
export function getBrowserInfo() {
  return _cachedBrowserInfo;
}

// 获取缓存的操作系统信息
export function getOSInfo() {
  return _cachedOSInfo;
}

// 获取缓存的屏幕尺寸信息
export function getScreenInfo() {
  return _cachedScreenInfo;
}
