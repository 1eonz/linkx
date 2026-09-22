import { useCommunicationStore } from '@/stores/communication.js';

export const sendNotification = async (message) => {
  console.info('sendNotification', message);
  try {
      const communicationStore = useCommunicationStore();
      const msgId = message.msgid;
      let glassesMsgIds = '';
      let glassesMsgIdsArr = [];
      if (msgId) {
        try {
          glassesMsgIds = await window.WeSpaceSDK.getStorage('glassesMsgIds') || '';
        } catch (e) {
          console.log('getStorage glassesMsgIds 失败，重置为空', e);
          glassesMsgIds = '';
        }
        glassesMsgIdsArr = glassesMsgIds.split(',').filter(Boolean);
        if (glassesMsgIdsArr.includes(String(msgId))) {
          return;
        }
      }

      const param = {
        url: message?.url ?? '',
        text: message?.text ?? '',
        contactId: message?.contactId ?? communicationStore.userInfo?.userid ?? '',
      };
      console.log('sendNotification param:', param);
      const showNotificationFunc = window?.WeSpaceSDK?.showNotification;
      if (showNotificationFunc && typeof showNotificationFunc === 'function') {
        const res = await window?.WeSpaceSDK?.showNotification(param);
        console.log('WeSpaceSDK.showNotification发送成功 res:', res);
        if (msgId) {
          if (glassesMsgIdsArr.length > 100) {
            glassesMsgIds = '';
          }
          if (glassesMsgIds) {
            glassesMsgIds += ',';
          }
          glassesMsgIds += msgId;
          try {
            await window.WeSpaceSDK.setStorage('glassesMsgIds', glassesMsgIds);
          } catch (e) {
            console.log('setStorage glassesMsgIds 失败', e);
          }
        }
      } else {
        console.log('showNotificationFunc方法不可用');
    }
  } catch (error) {
    console.log('智能眼镜消息发送失败', error);
  }
};
