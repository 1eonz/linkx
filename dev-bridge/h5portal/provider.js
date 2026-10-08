/**
 * H5Portal 端 SDK 代理桥 Provider 适配层（薄配置，不含协议逻辑）
 *
 * 运行在宿主 App 加载的 H5Portal 页面里（真实环境）。
 * H5Portal 的 WeSpaceSDK 由 App.vue onMounted 挂到 window.WeSpaceSDK，
 * provider-core 默认 getSDK 即取 window[sdkName]。
 *
 * 触发条件: DEV 环境下，URL 显式带 ?bridge=provider 时进入 provider 模式
 * （由 H5Portal/src/App.vue 在 SDK 挂载后动态 import，适配层内部再判一次角色，
 *   生产 build 时被 Vite tree-shake 剔除）
 */
import { createProvider } from '../page-scripts/provider-core.js';
import { BRIDGE_EVENTS } from './bridge-events.js';

if (import.meta.env && import.meta.env.DEV) {
  const __bridgeRole = new URLSearchParams(
    typeof location !== 'undefined' ? location.search : '',
  ).get('bridge');
  if (__bridgeRole === 'provider') {
    createProvider({
      target: 'h5portal',
      bridgeEvents: BRIDGE_EVENTS,
    });
  }
}
