/**
 * H5Portal 端 SDK 代理桥 Stub 适配层（薄配置，不含协议逻辑）
 *
 * 由 H5Portal/src/main.js 在所有其他 import 之前静态 import:
 *   - WeSpaceSDK.js 模块顶部立即执行 window.chrome.webview.addEventListener，
 *     必须先伪造 chrome.webview，否则 import App 时就会抛错
 *   - App.vue onMounted 会执行 window.WeSpaceSDK = WeSpaceSDK 覆盖桩，
 *     stub-core 默认桩带 defineProperty setter 锁定
 *
 * 使用者: 本地联调时开发人员手动打开浏览器访问 /?bridge=stub；
 *        自动化测试时由 Playwright（e2e/ 工程）驱动同一页面。桩对驱动者无感知。
 *
 * 触发条件: DEV 环境下，URL 带 ?bridge=stub 时进入桩模式
 * 生产 build 时整段被 Vite tree-shake
 */
import { createSdkStub } from '../page-scripts/stub-core.js';
import { envSpoofs, EVENT_REGISTER_METHODS } from './stub-config.js';

if (import.meta.env && import.meta.env.DEV) {
  const __bridgeRole = new URLSearchParams(
    typeof location !== 'undefined' ? location.search : '',
  ).get('bridge');
  if (__bridgeRole === 'stub') {
    createSdkStub({
      sdkName: 'WeSpaceSDK',
      proxyPort: 8787,
      eventRegisterMethods: EVENT_REGISTER_METHODS,
      hasStorageChangeEvent: true,
      envSpoofs,
    });
  }
}
