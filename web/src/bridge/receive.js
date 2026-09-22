import { changeTheme, getImUser } from '@/utils/loginIcs.ts';

import { closeChatUI, getUserInfo } from './post';
import { isWebView2 } from '@/utils/env';

/**
 *
 * @name 页面显示/隐藏切换
 * @param {string} data 状态
 * @param {boolean} isManual 是否为主动触发（默认 false，SDK 触发）
 * @returns {void}
 */
async function onVisibleChange(data, isManual = false) {
  const visible = String(isWebView2() ? data : data.data) === 'true';
  console.log('🚀 ~ onVisibleChange ~', visible, data);
  
  // 非主动触发 + WebView2 环境：强制刷新页面
  if (!isManual && isWebView2() && visible) {
    console.log('[onVisibleChange] SDK 触发，WebView2 环境自动刷新页面');
    window.location.reload();
    return
  }
  if (visible) {
    // openChatUI({ groupId: '0' })
    console.log('🚀 ~ userInfo ~', data);
    getImUser();
  } else closeChatUI();
}

/**
 * @name 接收我发消息
 * @param {string} data 消息内容
 * @returns {void}
 */
function onSendChatMsg(data) {
  console.log('🚀 ~ onSendChatMsg ~', data);
  // reciveMyMsg(data.data);
}

/**
 * @name 接收我被@ 的消息
 * @param {string} data 消息内容
 * @returns {void}
 */
function onReceiveChatMsgFilterAtMe(data) {
  console.log('🚀 ~ onReceiveChatMsgFilterAtMe ~', data);
  // reciveAtMe(data.data);
}

/**
 * @name 接收主题切换消息
 * @param {string} data 消息内容
 * @returns {void}
 */
function onThemeChanged(data) {
  changeTheme(data);
  console.log('🚀 ~ onThemeChanged ~', data);
}

/**
 * @name 接收选人组件选择结果
 * @param {string} data 选择结果
 * @returns {void}
 */
function onSelectedMembers(data) {
  console.log('🚀 ~ onSelectedMembers ~', data);
}

export default {
  onReceiveChatMsgFilterAtMe,
  onSelectedMembers,
  onSendChatMsg,
  onThemeChanged,
  onVisibleChange,
};
