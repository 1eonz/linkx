import { Dialog } from '@/components/Dialog';

import MessageBoxVue from './src/MessageBox.vue';

let msgId = 0;

/**
 * @description 消息弹框
 * @param {string} text 提示信息
 * @param {array} offset 定位
 * @param {string} confirmText 确定按钮文字
 * @param {string} cancelText 取消按钮文字
 * @param {string} iconName 图标名
 * @param {string} title 提示标题
 * @param {string} tip 建议信息
 * @return Promise
 */
function MessageBox(config) {
  msgId++;
  const MSGBOX = 'MessageBox';
  const { cancelText, confirmText, iconName, offset, text, tip, title, zIndexDefault } = config;
  const cid = MSGBOX + (msgId - 1);
  const dialog = Dialog(cid);

  dialog?.close(cid);

  return new Promise((resolve, _) => {
    Dialog({
      cid: MSGBOX + msgId,
      content: MessageBoxVue,
      data: {
        cancelText,
        confirmText,
        iconName,
        onCancel: () => {
          resolve(false);
        },
        onConfirm: () => {
          config.onConfirm?.();
          resolve(true);
        },
        text,
        tip,
        title,
      },
      offset,
      shade: true,
      zIndexDefault,
    });
  });
}

export default MessageBox;
