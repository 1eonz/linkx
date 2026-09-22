import { createVNode, render } from 'vue';

import ConfirmDialog from './ConfirmDialog.vue';

// 卸载前延时，等待离场动画结束
const CLOSE_DELAY = 300;

/**
 * 函数式调用确认弹窗，用法与 vant 的 showConfirmDialog 对齐：
 *
 *   import { showConfirmDialog } from '@/components/ConfirmDialog';
 *
 *   showConfirmDialog({ message: '当前任务还未保存，是否保存？' })
 *     .then(() => {
 *       // 点击确认
 *     })
 *     .catch(() => {
 *       // 点击取消
 *     });
 *
 * @param {object} [options] 弹窗配置
 * @param {string} [options.message] 弹窗文案
 * @param {string} [options.confirmButtonText] 确认按钮文案，默认「保存」
 * @param {string} [options.cancelButtonText] 取消按钮文案，默认「取消」
 * @returns {Promise<void>} 确认 resolve，取消 reject（与 vant 行为一致）
 */
export function showConfirmDialog(options = {}) {
  return new Promise((resolve, reject) => {
    const container = document.createElement('div');
    document.body.appendChild(container);

    const props = {
      show: true,
      message: options.message || '',
      confirmButtonText: options.confirmButtonText || '保存',
      cancelButtonText: options.cancelButtonText || '取消',
      onConfirm: () => {
        close();
        resolve();
      },
      onCancel: () => {
        close();
        reject('cancel');
      },
    };

    // 先将 show 置 false 播放离场动画，动画结束后再卸载
    function close() {
      render(createVNode(ConfirmDialog, { ...props, show: false }), container);
      setTimeout(() => {
        render(null, container);
        container.remove();
      }, CLOSE_DELAY);
    }

    render(createVNode(ConfirmDialog, props), container);
  });
}
