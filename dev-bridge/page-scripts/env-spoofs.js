/**
 * 环境伪造钩子（共享层，工具无关）
 *
 * 让本地浏览器（开发联调或自动化工具驱动）里的页面"看起来"运行在宿主 WebView 中，
 * 使 isWebView2() 等环境检测通过、SDK 模块顶层的
 * window.chrome.webview.addEventListener 不抛错。
 *
 * 各端适配层的 stub-config.js 按需组合导出给 createSdkStub({ envSpoofs })。
 */

/** 伪造 window.chrome.webview（普通浏览器自带 window.chrome 且不可重写，必须先 delete 再 define） */
export function spoofChromeWebview() {
  try {
    delete window.chrome;
  } catch {
    /* ignore */
  }
  try {
    Object.defineProperty(window, 'chrome', {
      value: {
        webview: {
          postMessage: () => {},
          addEventListener: () => {},
          removeEventListener: () => {},
        },
      },
      writable: true,
      configurable: true,
    });
  } catch {
    // 实在不行就直接赋值
    window.chrome = {
      webview: {
        postMessage: () => {},
        addEventListener: () => {},
        removeEventListener: () => {},
      },
    };
  }
}

/** 伪造 window.flutterNativeBridge（Browser/bspc 宿主通道） */
export function spoofFlutterNativeBridge() {
  window.flutterNativeBridge = { postMessage: () => {} };
}

/** 伪造 window.PIM_GetPlatform */
export function spoofPimGetPlatform(platform = 'win') {
  Object.defineProperty(window, 'PIM_GetPlatform', {
    value: () => platform,
    configurable: true,
  });
}

/** 伪造 window.jsBridge */
export function spoofJsBridge() {
  window.jsBridge = {
    invoke: () => {},
    receiveMessage: () => {},
    resolveMap: new Map(),
  };
}
