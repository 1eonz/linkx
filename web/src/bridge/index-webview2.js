/**
 * WebView2 环境 Bridge 入口
 * 动态加载 WeSpaceSDK.js
 */

import receiveObj from './receive';

console.log('[Bridge-WebView2] 开始初始化...');

// 初始化 SDK 并注册 handlers
const initBridge = async () => {
  // 动态导入 WebView2 SDK
  const module = await import('./WeSpaceSDK-cspc.js');
  const WeSpaceSDK = module.default;

  console.log('[Bridge-WebView2] SDK 加载完成');
  console.log('[Bridge-WebView2] 开始注册 receive handlers:', Object.keys(receiveObj || {}));

  // 注册 handlers
  for (const key in receiveObj) {
    const canRegister = WeSpaceSDK[key] && typeof WeSpaceSDK[key] === 'function';
    console.log('[Bridge-WebView2] handler:', key, '可注册:', canRegister);
    if (canRegister) {
      WeSpaceSDK[key](receiveObj[key]);
    }
  }

  // WebView2：宿主可能不会像 Browser/SETUP_CHANNEL 那样主动推一次可见性事件
  // 注意：不要在此处直接触发 receiveObj.onVisibleChange（可能早于 Vue/Store 初始化，线上会报错）
  // 由 main.ts 在 app.mount 后手动触发一次

  console.log('[Bridge-WebView2] 初始化完成');
  return WeSpaceSDK;
};

// 导出初始化 Promise
export const bridgeReady = initBridge();

// 默认导出
let WeSpaceSDK = {};
bridgeReady.then(sdk => {
  WeSpaceSDK = sdk;
  window.WeSpaceSDK = sdk
});

export default WeSpaceSDK;