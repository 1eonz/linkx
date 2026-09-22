/**
 * 环境判断工具函数
 * 无任何依赖，可安全在模块加载阶段使用
 */

/**
 * 判断是否为 WebView2 环境
 * 1. WebView2 环境下 window.chrome.webview 存在
 * 2. URL参数 clientType=CSPC 也视为 WebView2 环境
 * 3. 保底判断：PIM_GetPlatform 函数存在
 * @returns {boolean}
 */
export function isWebView2(): boolean {
  // 1. 原有判断：WebView2 环境下 window.chrome.webview 存在
  if (typeof window !== 'undefined' && window.chrome && (window.chrome as any).webview) {
    return true;
  }

  // 2. URL参数 clientType=CSPC 也视为 WebView2 环境
  try {
    if (typeof window !== 'undefined') {
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.get('clientType') === 'CSPC') {
        return true;
      }
    }
  } catch (e) {
    // URL解析失败时忽略
  }

  // 3. 保底判断：PIM_GetPlatform 函数存在
  if (typeof window !== 'undefined' && typeof (window as any).PIM_GetPlatform === 'function') {
    return true;
  }

  return false;
}

/**
 * 判断是否为 Windows 10 及以上版本
 * Windows 10 / Windows 11 的 userAgent 均为 Windows NT 10.0
 * @returns {boolean}
 */
export function isWindows10OrLater(): boolean {
  if (typeof navigator === 'undefined') {
    return false;
  }
  const match = navigator.userAgent.match(/Windows NT (\d+\.\d+)/);
  if (!match) {
    return false;
  }
  return parseFloat(match[1]) >= 10.0;
}