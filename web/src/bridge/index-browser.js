/**
 * 浏览器环境 Bridge 入口
 * 保持原有逻辑，静态导入 WeSpaceSDK-bspc.js
 */

import WeSpaceSDK from './WeSpaceSDK-bspc.js';
import receiveObj from './receive';
window.addEventListener('message', (event) => {
    console.log('[Bridge自检！！！！！！！] window message received. origin:', event.origin, 'data:', event.data, 'ports:',
      event.ports ? event.ports.length : 0);
  });
console.log('[Bridge-浏览器] 开始初始化...');

// 注册 handlers
for (const key in receiveObj) {
  const canRegister = WeSpaceSDK[key] && typeof WeSpaceSDK[key] === 'function';
  console.log('[Bridge-浏览器] handler:', key, '可注册:', canRegister);
  if (canRegister) {
    WeSpaceSDK[key](receiveObj[key]);
  }
}

console.log('[Bridge-浏览器] 初始化完成');

export default WeSpaceSDK;