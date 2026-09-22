import { saveMessage } from '@/api/sms';
import { Message } from '@/components/Message';
import { CategoryEnum } from '@/enums';
import { useEmitter } from '@/hooks';
import { getExecutorInfoByAccount, getInfoByAccount } from '@/pages/resource/resourceHelper';
import { groupFunc } from '@/plugins/mspPlayer';
import { sdkCallbackPrint, triggerSDKMethods } from '@/plugins/mspPlayer/helper';
import { delay } from '@/utils';
import { isEmpty } from '@/utils/is';

function smsRegister() {
  init();

  function init() {
    // 短信发送通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('短信发送通知', data);
        if (!sendError(data)) {
          handleSMS(data, true);
        }
      },
      eventName: 'OnSendDispSMSResult',
      eventType: 'MsNotify',
    });

    // 短信接收通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('短信接收通知', data);
        handleSMS(data, false);
      },
      eventName: 'OnRecvDispSMSNotify',
      eventType: 'MsNotify',
    });

    // 短信状态通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('短信状态通知', data);
      },
      eventName: 'OnDispSMSModuleNotify',
      eventType: 'MsNotify',
    });

    // 彩信发送通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('彩信发送通知', data);
        if (!sendError(data)) {
          handleSMS(data, true);
        }
      },
      eventName: 'OnSendDispMMSResult',
      eventType: 'MsNotify',
    });

    // 彩信接收通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('彩信接收通知', data);
        handleSMS(data, false);
      },
      eventName: 'OnRecvDispMMSNotify',
      eventType: 'MsNotify',
    });

    // 彩信状态通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('彩信状态通知', data);
      },
      eventName: 'OnDispMMSModuleNotify',
      eventType: 'MsNotify',
    });

    // 状态短信更新通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('状态短信更新通知', data);
      },
      eventName: 'OnRecvStateMsgNotify',
      eventType: 'MsNotify',
    });
  }

  // 短信收发
  async function handleSMS(data, isSend: boolean) {
    const { attach, attach_thumb, audiocontent, audioinfo, content, from, groupid, msgid, to } =
      data.value;

    const groupId = groupid || to; // 收消息取groupid，发消息取to
    const groupInfo = await groupFunc.queryGroupInfo(groupId);
    const isGroup = groupInfo.rsp === '0';

    const fromInfo = await getSenderInfo(from);
    const fileType = judgeType(attach, audiocontent, audioinfo);

    /**
     * 发送短信存
     * 接收短信时如果短信发送方不是icc则需要存后台
     */
    if (isSend || (!isSend && fromInfo.category !== CategoryEnum.seat)) {
      const toInfo = await getSenderInfo(to);
      let _attach = null;
      if (attach && fileType !== 'voice') {
        _attach = isSend ? attach.match(/file=([^ ]+)/)[1] : attach;
      }
      const param = {
        attach: _attach,
        attachThumb: fileType === 'voice' ? (audiocontent ?? null) : (attach_thumb ?? null),
        audioTime: audioinfo ? Number(audioinfo) : 0,
        chatId: null,
        content: fileType === 'voice' ? '' : content,
        fromIsdn: from,
        fromName: fromInfo.name || from,
        groupId: isGroup ? groupId : '',
        groupName: isGroup ? groupInfo.value.name : '',
        isGroup,
        isRead: 0,
        msgid,
        sendTime: Date.now(),
        status: 'success',
        toIsdn: isGroup ? '' : to,
        toName: isGroup ? '' : toInfo.name || to,
        type: fileType,
      };
      const { code } = await saveMessage(param); // 保存短信
      if (code !== 0) {
        throw new Error('save msg error');
      }
    }

    await delay(1000);

    useEmitter().emit('updateMsgList', {
      fromIsdn: from,
      groupId: isGroup ? groupId : '',
      isGroup,
      toIsdn: isGroup ? '' : to,
    });

    if (!isSend) {
      useEmitter().emit('newMsg', {
        content: fileType === 'voice' ? '' : content,
        groupId: isGroup ? groupId : '',
        isRead: 1,
        msgBelongId: isGroup ? groupId : from,
        title: `${fromInfo.name || from}(${isGroup ? groupId : from})`,
      });
    }
  }

  // 判断短彩信类型
  function judgeType(attach: string, audioContent, audioInfo) {
    if (audioContent && audioInfo) {
      return 'voice';
    }
    if (attach && attach.endsWith('.mp4')) {
      return 'video';
    }
    if (attach) return 'image';
    return 'text';
  }

  // 短彩信发送状态判断
  function sendError(data) {
    const { rsp } = data;
    if (rsp !== '0') {
      const { to } = data.value;
      const tips = {
        1: '业务不支持，如对方不支持加密短信或无权限等',
        2: `${to}VPN权限非法`,
        3: `${to}号码不存在`,
        5: `${to}号码被锁定`,
        6: `${to}无效msgid`,
      };
      Message(tips[rsp]);
    }
    return rsp !== '0';
  }

  // 获取消息发送者信息
  async function getSenderInfo(id) {
    const info = await getInfoByAccount(id);
    // 如果存在领用人则显示领用用人名称
    const executor: any = await getExecutorInfoByAccount(id);
    if (executor && !isEmpty(executor)) {
      info.name = executor.name;
    }
    return info;
  }
}

export default smsRegister;
