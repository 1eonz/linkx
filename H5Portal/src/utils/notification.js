import { readNotification } from '@/common/api/collaborativeGroup.js';
import { useCommunicationStore } from '@/stores/communication.js';

/**
 * 处理通知点击的公共方法
 * 封装已读接口调用和url跳转逻辑，支持多种回调扩展
 * @param {Object} item - 通知数据 { notificationId, url, text, ... }
 * @param {Object} options - 配置项（全部可选）
 * @param {Function} [options.onClick] - 点击统一回调，最先执行（如埋点统计）
 * @param {Function} [options.beforeRead] - 调用已读接口前的回调
 * @param {Function} [options.afterRead] - 调用已读接口成功后的回调（如刷新列表）
 * @param {Function} [options.beforeOpenUrl] - 打开url前的回调
 * @param {Function} [options.afterOpenUrl] - 打开url后的回调（如关闭当前页）
 * @param {Function} [options.onNoUrl] - 无url时的回调（如显示弹框）
 */
export const handleNotificationClick = async (item, options = {}) => {
  if (!item) return;

  // 点击统一回调（最先执行）
  if (options.onClick) options.onClick(item);

  // 调用已读接口前回调
  if (options.beforeRead) options.beforeRead(item);

  // 调用已读接口（notificationId为空则不调用）
  if (item.notificationId) {
    try {
      await readNotification({ notificationId: item.notificationId });
      // 已读成功后回调（如刷新列表）
      if (options.afterRead) options.afterRead(item);
    } catch (err) {
      console.error('消息已读更新失败:', err);
    }
  }

  // 有url则打开url
  if (item.url) {
    if (options.beforeOpenUrl) options.beforeOpenUrl(item);
    const communicationStore = useCommunicationStore();
    await communicationStore.openUrl(item.url);
  } else if (options.onNoUrl) {
    // 无url时执行回调（如显示弹框）
    options.onNoUrl(item);
  }
  if (options.afterOpenUrl) options.afterOpenUrl(item);
};
