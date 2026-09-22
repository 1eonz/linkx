import { Message } from '@/components/Message';
import { useI18n } from '@/hooks';
import { useCommunicationStore } from '@/store';

import { sdkCallbackPrint, triggerSDKMethods } from '../helper';
import storeCidIsdnRelation from '../storeCidIsdnRelation';

export const voiceFunc = {
  // 开启环境侦听
  voiceAmbienceListen(toUid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('开启环境侦听', data);
      },
      to: toUid,
    };
    return triggerSDKMethods('voice', 'ambienceListen', param);
  },
  // 接听
  voiceAnswer(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('接听', data);
      },
      cid,
    };
    return triggerSDKMethods('voice', 'answer', param);
  },
  // 点呼强拆
  voiceBreakOff(toUid) {
    const { t } = useI18n();
    const param = {
      callback: (data) => {
        sdkCallbackPrint('点呼强拆', data);
        if (data.rsp === '0') {
          Message(t('communication.communicationFunction.forcedDemolitionSuccess'));
        } else {
          Message(t('communication.communicationFunction.forcedDemolitionFailed'));
        }
      },
      to: toUid,
    };
    return triggerSDKMethods('voice', 'breakOff', param);
  },
  // 点呼人工转接取消
  voiceCancelTransfer(toUid, speaker) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('点呼人工转接取消', data);
      },
      speaker,
      to: toUid,
    };
    return triggerSDKMethods('voice', 'cancelTransfer', param);
  },
  // 半双工点呼挂断
  voiceCloseHalfDial(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('半双工点呼挂断', data);
      },
      cid,
    };
    return triggerSDKMethods('voice', 'closeHalfDial', param);
  },
  // 语音点呼
  voiceDial(toUid, dialType, answerMode: '0' | '1') {
    const { t } = useI18n();
    const param = {
      // Answer-Mode,接听模式0表示自动接听，1表示手动接听
      'Answer-Mode': answerMode,
      callback: (data) => {
        sdkCallbackPrint('语音点呼', data);
        const { rsp } = data;
        if (rsp !== '0') {
          const { deleteComm } = useCommunicationStore();
          deleteComm({
            isdn: toUid,
            status: {},
            type: 'voice',
          });
          storeCidIsdnRelation.removeByIsdn(toUid, 'voice');
          if (rsp === '-6') {
            Message({
              message: t('communication.communicationTips.communicationError'),
              type: 'error',
            });
          } else if (rsp === '-4') {
            Message(t('communication.communicationTips.pointCallWarnTwo'));
          }
        }
      },
      to: toUid,
    };
    return triggerSDKMethods('voice', dialType, param);
  },
  // 发起PSTN/PLMN 电话呼叫
  voiceDialout(toUid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('发起PSTN/PLMN 电话呼叫', data);
      },
      to: toUid,
    };
    return triggerSDKMethods('voice', 'dialout', param);
  },
  // 开启/关闭缜密侦听
  voiceDiscreetListen(toUid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('开启/关闭缜密侦听', data);
      },
      opType: 'start',
      to: toUid,
    };
    return triggerSDKMethods('voice', 'discreetListen', param);
  },
  // 半双工点呼发起/抢话
  voiceHalfDial(toUid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('半双工点呼发起/抢话', data);
        const { comm, updateComm } = useCommunicationStore();
        if (comm[toUid]?.halfdial) {
          updateComm({
            isdn: toUid,
            type: 'halfdial',
            value: {
              halfDialStatus: '1',
            },
          });
        }
      },
      to: toUid,
    };
    return triggerSDKMethods('voice', 'halfDial', param);
  },
  // 保持
  voiceHold(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('保持', data);
      },
      cid,
    };
    return triggerSDKMethods('voice', 'hold', param);
  },
  // 语音抢话
  voiceIntercept(toUid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('语音抢话', data);
      },
      to: toUid,
    };
    return triggerSDKMethods('voice', 'intercept', param);
  },
  // 拒接
  voiceReject(params) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('拒接', data);
        const { rejectDial } = useCommunicationStore();
        rejectDial(params);
      },
      cid: params.cid,
    };
    return triggerSDKMethods('voice', 'reject', param);
  },
  // 语音挂断
  voiceRelease(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('语音挂断', data);
      },
      cid,
    };
    return triggerSDKMethods('voice', 'release', param);
  },
  // 挂断PSTN/PLMN 电话呼叫
  voiceReleaseDialout(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('挂断PSTN/PLMN 电话呼叫', data);
      },
      cid,
    };
    return triggerSDKMethods('voice', 'releaseDialout', param);
  },
  // 半双工点呼释放
  voiceReleaseHalfDial(to_uid, cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('半双工点呼释放', data);
        const { updateComm } = useCommunicationStore();
        updateComm({
          isdn: to_uid,
          type: 'halfdial',
          value: {
            halfDialStatus: '0',
          },
        });
      },
      cid,
    };
    return triggerSDKMethods('voice', 'releaseHalfDial', param);
  },
  // 订阅用户
  voiceSubscribeUserStatus(reslist) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('订阅用户', data);
      },
      reslist,
    };
    return triggerSDKMethods('voice', 'subscribeUserStatus', param);
  },
  // 点呼人工转接
  voiceTransfer(toUid, speaker) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('点呼人工转接', data);
      },
      speaker,
      to: toUid,
    };
    return triggerSDKMethods('voice', 'transfer', param);
  },
  // 取消保持
  voiceUnhold(cid) {
    const { t } = useI18n();
    const param = {
      callback: (data) => {
        sdkCallbackPrint('取消保持', data);
        if (data.rsp === '-7') {
          Message(t('communication.communicationTips.unHoldFailure'));
        }
      },
      cid,
    };
    return triggerSDKMethods('voice', 'unhold', param);
  },
  // 取消订阅用户
  voiceUnsubscribeUserStatus(reslist) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('取消订阅用户', data);
      },
      reslist,
    };
    return triggerSDKMethods('voice', 'unsubscribeUserStatus', param);
  },
};
