import { useEmitter, usePIM } from '@/hooks';
import { processTasks } from '@/pages/coordination/common';
import { modifyMessages, saveMessages } from '@/plugins/pim/indexDB';
import { usePIMStore } from '@/store';

import { cloneDeep } from 'lodash-es';

export const registerPimWssEvent = () => {
  const {
    getCooperationGroupHistoryMsg,
    handleMessageNotifications,
    handleUpdateUser,
    xietongChange,
  } = usePIMStore();

  // 接收文本消息通知
  usePIM().on('im', 'TEXT_MSG', (data) => {
    console.log('接收文本消息通知', data);
    handleMessageNotifications('TEXT_MSG', cloneDeep(data));
    saveMessages(data);
    processTasks(data);
  });

  // 接收彩信消息通知
  usePIM().on('im', 'MEDIA_MSG', (data) => {
    console.log('接收彩信消息通知', data);
    handleMessageNotifications('MEDIA_MSG', cloneDeep(data));
    saveMessages(data);
  });

  // 接收位置更新通知
  usePIM().on('im', 'LOCATION_SHARE', (data) => {
    console.log('接收位置更新通知', data);
  });

  // 接收已读回执消息通知
  usePIM().on('im', 'ACK_READ', (data) => {
    console.log('接收已读回执消息通知', data);
    handleMessageNotifications('ACK_READ', cloneDeep(data));
    modifyMessages(data, { read: true });
    // saveMessages(data, { notifyType: 'ACK_READ', read: true });
  });

  // 接收撤回消息通知
  usePIM().on('im', 'WITHDRAW_MSG', (data) => {
    console.log('接收撤回消息通知', data);
    handleMessageNotifications('WITHDRAW_MSG', cloneDeep(data));
  });

  // 接收合并转发消息通知
  usePIM().on('im', 'MERGE_FORWARD', (data) => {
    console.log('接收合并转发消息通知', data);
    const { forwardMsgList, user } = usePIMStore();
    if (Number(data.from) === user.id) {
      data.msg = forwardMsgList;
    }
    handleMessageNotifications('MERGE_FORWARD', cloneDeep(data));
    saveMessages(data);
  });

  // 用户踢出通知
  usePIM().on('auth', 'kickoff', (data) => {
    console.log('用户踢出通知', data);
  });

  // 群组事件
  usePIM().on('im', 'GROUP_EVENT', (data) => {
    console.log('IM群组信息变更通知', data);
    const { groupId, noticeDto, operationType } = data.msg.cmcontainer;
    const obj = {
      category: 2,
      notifyType: 'GROUP_EVENT',
      time: Date.now(),
      to: groupId,
      ...data,
    };
    handleMessageNotifications('groupNotice', cloneDeep(obj));

    if ([1, 2, 3, 4, 5, 6, 7, 11].includes(operationType)) {
      saveMessages(obj);
    }

    // 群公告变更消息发送
    if (noticeDto) {
      useEmitter().emit('groupNoticeChange', { content: noticeDto.content, groupId });
    }

    // 协同群组支撑人员变更
    if (operationType === 11) {
      getCooperationGroupHistoryMsg(obj);
    }
  });

  // 群组信息变更通知
  usePIM().on('group', 'groupNotice', () => {
    // console.log('群组信息变更通知', data);
  });

  // 用户信息变更通知
  usePIM().on('addressbook', 'update_user', (data) => {
    console.log('用户信息变更通知', data);
    handleUpdateUser(data.result, 'update_user');
  });

  // 用户聊天设置变更通知
  usePIM().on('addressbook', 'chatProfileNotice', (data) => {
    console.log('用户聊天设置变更通知', data);
    handleUpdateUser(data, 'chatProfileNotice');
  });

  // 接收卡片消息通知
  usePIM().on('im', 'NAME_CARD', (data) => {
    console.log('接收卡片消息通知', data);
    handleMessageNotifications('NAME_CARD', cloneDeep(data));
    saveMessages(data);
  });

  // 协同用户变更通知
  usePIM().on('addressbook', 'cooperation_user', (data) => {
    console.log('协同用户变更通知', data);
    xietongChange();
  });
};
