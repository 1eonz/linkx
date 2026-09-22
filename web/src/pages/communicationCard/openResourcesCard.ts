import { Dialog } from '@/components/Dialog';
import MessageSendCard from '@/pages/communicationCard/sms/messageSendCard.vue';
import { openVideoPopup } from '@/pages/map/openVideoPopup';

// 摄像头视频弹窗
export function openMonitorCard(data) {
  let account;
  // 存在车载图传通信号则使用车载图传通信号
  if (data.account) {
    account = data.account;
  }
  // 存在摄像头通信号则使用摄像头通信号
  if (data.accounts) {
    account = data.accounts;
  }
  // 两者都未获取到，则使用设备默认编号
  if (!account) {
    account = data.code;
  }
  openVideoPopup({
    account,
    infoData: { ...data, resourceType: 'monitor' },
  });
}

// 打开群发短信弹窗
export function openMessageSendCard(checkedData) {
  Dialog({
    cid: 'MessageSend',
    content: MessageSendCard,
    data: {
      checkedData,
    },
  });
}

/**
 * 地图弹窗半双工点呼空格长按事件
 */
let voiceHalfCallKeyUpFunction: any = null;

// 保存事件
export function setVoiceHalfCallKeyUpFunction(fn) {
  voiceHalfCallKeyUpFunction = fn;
}

// 移除事件
export function removeVoiceHalfCallKeyUpFunction() {
  window.removeEventListener('keyup', voiceHalfCallKeyUpFunction as any);
  voiceHalfCallKeyUpFunction = null;
}
