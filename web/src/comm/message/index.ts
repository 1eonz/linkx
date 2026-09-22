import { updateMessageStatus } from '@/api/sms';
import { Dialog } from '@/components/Dialog';
import { appConfig } from '@/config';
import MessageDetailsCard from '@/pages/notification/messageDetailsCard.vue';
import { useCommunicationStoreWithOut } from '@/store';

/**
 * 打开短信弹窗
 * 两种打开路径：1.从短信列表（有chatId） 2.资源发送短信
 * 聊天窗口只能打开一个
 */
export function openMessageDetailsPopup(data) {
  const { chatId, msgBelongId, name, serviceAccounts, title } = data;
  const belongId = msgBelongId || serviceAccounts?.[0].account;
  const info = chatId
    ? data
    : {
        chatId: '',
        content: '',
        isRead: 1,
        msgBelongId: belongId,
        sendTime: '',
        title: title || `${name}(${belongId})`,
      };

  const cid = `MessageDetailsCard${belongId}`;
  Dialog({
    cid,
    content: MessageDetailsCard,
    data: { info, uid: cid },
  });
}

/**
 * 更新短信已读状态
 * @param chatId
 */
export async function updateMsgStatus(chatId) {
  const { code } = await updateMessageStatus({
    chatId,
    toIsdn: appConfig.isdn,
  });
  if (code === 0) {
    useCommunicationStoreWithOut().updateUnreadStatus(chatId);
  }
}
