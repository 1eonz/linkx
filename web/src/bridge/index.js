/**
 * Bridge 统一入口（只做初始化副作用）
 * - Browser: 静态导入 `index-browser.js`，保证与旧版时序一致
 * - WebView2: 动态导入 `index-webview2.js`
 */

import { isWebView2 } from '../utils/env';

console.log('[Bridge] 检测环境:', isWebView2() ? 'WebView2' : 'Browser');

const initBridge = async () => {
  if (isWebView2()) {
    const m = await import('./index-webview2.js');
    if (m.bridgeReady) await m.bridgeReady;
    return;
  }

  // 浏览器环境：静态导入，确保 SDK 顶层 initBridge 立即执行
  await import('./index-browser.js');
};

export const bridgeReady = initBridge();
