import { createApp, h } from 'vue';

import CustomToast from '@/components/my-toast/index.vue';

// 用于挂载的单例 DOM 节点和 Vue 应用
let toastNode = null;
let toastApp = null;

/**
 * 全局方法：showCustomToast
 * @param {string} message - 要显示的提示文字
 * @param {number} duration - 显示时间（毫秒），默认 2000ms
 */
export function showCustomToast(message, duration = 2000) {
  // 如果已存在 Toast，先卸载
  if (toastApp) {
    toastApp.unmount();
    if (toastNode && toastNode.parentNode) {
      toastNode.parentNode.removeChild(toastNode);
    }
  }

  // 创建挂载节点
  toastNode = document.createElement('div');
  document.body.appendChild(toastNode);

  // 创建一个极简的 Vue 应用，仅渲染 CustomToast 组件
  toastApp = createApp({
    render() {
      return h(CustomToast, {
        message,
        duration,
      });
    },
  });

  // 挂载到 DOM
  toastApp.mount(toastNode);
}
