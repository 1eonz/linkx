import { Dialog } from '@/components/Dialog';
import { Message } from '@/components/Message';
import { useEmitter, useI18n } from '@/hooks';
import { sdkCallbackPrint, triggerSDKMethods } from '@/plugins/mspPlayer/helper';
import { getConfer, useConferenceStore } from '@/store';

import confUtil from './confUtil';

let queryConfListByAttendeeCallback: any = null;
export const setQueryConfListByAttendeeCallback = (data) => {
  queryConfListByAttendeeCallback = data;
};

function confRegister() {
  const { t } = useI18n();
  const conferenceStore = useConferenceStore();

  init();

  function init() {
    // 创建音视频会议成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('创建音视频会议成功', data);
        const conferenceStatus = confUtil.conferenceMessage(data);
        handleConference(conferenceStatus);
        handleConferenceStatus(conferenceStatus);
        useEmitter().emit('OnCreateConfSuccess', conferenceStatus);
      },
      eventName: 'OnCreateConfSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 创建音视频会议失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('创建音视频会议失败', data);
        const conferenceStatus = confUtil.conferenceMessage(data);
        handleConference(conferenceStatus);
        handleConferenceStatus(conferenceStatus);

        const { confer } = conferenceStore;
        Message({
          message:
            confer?.status === 'ringing'
              ? t('videoConference.statusMsg.creteFailureInvited')
              : t('videoConference.statusMsg.creteFailure'),
          type: 'error',
        });
        Dialog('conferenceCard')?.close();
      },
      eventName: 'OnCreateConfFailure',
      eventType: 'PhoneConfNotify',
    });

    // 会议状态通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('会议状态通知', data);
        const conferenceStatus = confUtil.conferenceMessage(data);
        handleConferenceMember(conferenceStatus);
        handleConferenceSpeaker(data);
      },
      eventName: 'OnConfStatusNotify',
      eventType: 'PhoneConfNotify',
    });

    // 结束会议成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('结束会议成功', data);
        const conferenceStatus = confUtil.conferenceMessage(data);
        handleConference(conferenceStatus);
        handleConferenceStatus(conferenceStatus);
        useEmitter().emit('OnEndConfSuccess', conferenceStatus);
      },
      eventName: 'OnEndConfSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 结束会议失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('结束会议失败', data);
      },
      eventName: 'OnEndConfFailure',
      eventType: 'PhoneConfNotify',
    });

    // 订阅会议(加入会议)成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('订阅会议(加入会议)成功', data);
        useEmitter().emit('OnSubscribeConfSuccess');
      },
      eventName: 'OnSubscribeConfSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 订阅会议失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('订阅会议失败', data);
        useEmitter().emit('OnSubscribeConfFailure');
      },
      eventName: 'OnSubscribeConfFailure',
      eventType: 'PhoneConfNotify',
    });

    // 去订阅会议(挂断、离开会议)成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('去订阅会议(挂断、离开会议)成功', data);
        useEmitter().emit('OnUnsubscribeConfSuccess');
      },
      eventName: 'OnUnsubscribeConfSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 去订阅会议失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('去订阅会议失败', data);
      },
      eventName: 'OnUnsubscribeConfFailure',
      eventType: 'PhoneConfNotify',
    });

    // 会议呼出回铃事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('会议呼出回铃事件', data);
        data.isMain = true;
        const conferenceStatus = confUtil.conferenceMessage(data);
        handleConference(conferenceStatus);
      },
      eventName: 'OnConfDialOutRinging',
      eventType: 'PhoneConfNotify',
    });

    // 会议呼入振铃事件
    triggerSDKMethods('event', 'register', {
      callback: async (data) => {
        sdkCallbackPrint('会议呼入振铃事件', data);
        data.isMain = false;
        const conferenceStatus = confUtil.conferenceMessage(data);
        handleConference(conferenceStatus);
      },
      eventName: 'OnConfDialInRinging',
      eventType: 'PhoneConfNotify',
    });

    // 会议通话事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('会议通话事件', data);
        const conferenceStatus = confUtil.conferenceMessage(data);
        handleConference(conferenceStatus);
        handleConferenceStatus(conferenceStatus);
      },
      eventName: 'OnConfConnect',
      eventType: 'PhoneConfNotify',
    });

    // 会议释放事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('会议释放事件', data);
        const conferenceStatus = confUtil.conferenceMessage(data);
        handleConference(conferenceStatus);
        handleConferenceStatus(conferenceStatus);
        useEmitter().emit('OnEndConfSuccess', conferenceStatus);
      },
      eventName: 'OnConfRelease',
      eventType: 'PhoneConfNotify',
    });

    // 会议通话失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('会议通话失败', data);
        Message({
          message: t('videoConference.statusMsg.connectFailure'),
          type: 'error',
        });
      },
      eventName: 'OnConfFailure',
      eventType: 'PhoneConfNotify',
    });

    // 静音会议成员成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('静音会议成员成功', data);
      },
      eventName: 'OnMuteConfMemberSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 静音会议成员失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('静音会议成员失败', data);
        Message({
          message: t('videoConference.statusMsg.muteMemFailure'),
          type: 'error',
        });
      },
      eventName: 'OnMuteConfMemberFailure',
      eventType: 'PhoneConfNotify',
    });

    // 取消静音会议成员成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('取消静音会议成员成功', data);
      },
      eventName: 'OnUnmuteConfMemberSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 取消静音会议成员失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('取消静音会议成员失败', data);
        Message({
          message: t('videoConference.statusMsg.unmuteMemFailure'),
          type: 'error',
        });
      },
      eventName: 'OnUnmuteConfMemberFailure',
      eventType: 'PhoneConfNotify',
    });

    // 静音会议成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('静音会议成功', data);
      },
      eventName: 'OnMuteConfSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 静音会议失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('静音会议失败', data);
        Message({
          message: t('videoConference.statusMsg.muteFailure'),
          type: 'error',
        });
      },
      eventName: 'OnMuteConfFailure',
      eventType: 'PhoneConfNotify',
    });

    // 取消静音会议成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('取消静音会议成功', data);
      },
      eventName: 'OnUnmuteConfSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 取消静音会议失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('取消静音会议失败', data);
        Message({
          message: t('videoConference.statusMsg.unmuteFailure'),
          type: 'error',
        });
      },
      eventName: 'OnUnmuteConfFailure',
      eventType: 'PhoneConfNotify',
    });

    // 增加与会成员成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('增加与会成员成功', data);
        useEmitter().emit('OnAddConfMembersSuccess');
      },
      eventName: 'OnAddConfMembersSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 增加与会成员失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('增加与会成员失败', data);
        Message({
          message: t('videoConference.statusMsg.addFailure'),
          type: 'warning',
        });
      },
      eventName: 'OnAddConfMembersFailure',
      eventType: 'PhoneConfNotify',
    });

    // 删除成员成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('删除成员成功', data);
        useEmitter().emit('OnDelConfMembersSuccess', data.value);
      },
      eventName: 'OnDelConfMembersSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 删除成员失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('删除成员失败', data);
      },
      eventName: 'OnDelConfMembersFailure',
      eventType: 'PhoneConfNotify',
    });

    // 挂断成员成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('挂断成员成功', data);
      },
      eventName: 'OnHangupConfMemberSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 挂断成员失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('挂断成员失败', data);
        Message({
          message: t('videoConference.statusMsg.HangupFailure'),
          type: 'error',
        });
      },
      eventName: 'OnHangupConfMemberFailure',
      eventType: 'PhoneConfNotify',
    });

    // 重呼成员成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('重呼成员成功', data);
      },
      eventName: 'OnCallConfMemberSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 重呼成员失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('重呼成员失败', data);
        Message({
          message: t('videoConference.statusMsg.reCallFailure'),
          type: 'error',
        });
      },
      eventName: 'OnCallConfMemberFailure',
      eventType: 'PhoneConfNotify',
    });

    // 选看与会者成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('选看与会者成功', data);
      },
      eventName: 'OnWatchConfMemberSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 选看多画面成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('选看多画面成功', data);
        const member = data.value.member.split('/');
        conferenceStore.setBroadcastMember(member);
        useEmitter().emit('OnWatchMixPictureSuccess');
      },
      eventName: 'OnWatchMixPictureSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 查看调度员所在的会议信息结果
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('查看调度员所在的会议信息结果', data);
        queryConfListByAttendeeCallback?.(data);
      },
      eventName: 'OnQueryConfListByAttendeeResult',
      eventType: 'PhoneConfNotify',
    });

    // 广播多画面通知成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('广播多画面通知成功', data);
        const member = data.value.member.split('/');
        conferenceStore.setBroadcastMember(member);
        useEmitter().emit('OnBroadcastMixPictureSuccess');
      },
      eventName: 'OnBroadcastMixPictureSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 广播多画面通知失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('广播多画面通知失败', data);
        Message({
          message: t('videoConference.statusMsg.radioFailure'),
          type: 'error',
        });
      },
      eventName: 'OnBroadcastMixPictureFailure',
      eventType: 'PhoneConfNotify',
    });

    // 广播与会者成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('广播与会者成功', data);
      },
      eventName: 'OnBroadcastConfMemberSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 广播与会者失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('广播与会者失败', data);
        Message({
          message: t('广播与会者失败'),
          type: 'error',
        });
      },
      eventName: 'OnBroadcastConfMemberFailure',
      eventType: 'PhoneConfNotify',
    });

    // 取消广播与会者成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('取消广播与会者成功', data);
      },
      eventName: 'OnCancelBroadcastConfMemberSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 取消广播与会者失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('取消广播与会者失败', data);
        Message({
          message: t('取消广播与会者失败'),
          type: 'error',
        });
      },
      eventName: 'OnCancelBroadcastConfMemberFailure',
      eventType: 'PhoneConfNotify',
    });

    // 查询多画面信息结果
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('查询多画面信息结果', data);
        const modes: any = [];
        data.value.supportCPModes?.forEach((item) => {
          modes.push(item.mode.slice(3));
        });
        conferenceStore.setSupportCPModes(modes);
      },
      eventName: 'OnQueryContinuousPresenceInfoResult',
      eventType: 'PhoneConfNotify',
    });

    // 话权代理成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('话权代理成功', data);
        if (data.rsp === '0') {
          conferenceStore.updateConfProxy(data.value);
        }
      },
      eventName: 'OnConfProxyFloorSuccess',
      eventType: 'PhoneConfNotify',
    });

    // 话权代理失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('话权代理失败', data);
      },
      eventName: 'OnConfProxyFloorFailure',
      eventType: 'PhoneConfNotify',
    });
  }

  // 更新状态
  function handleConference(message) {
    const { status } = message;
    if (status === 'end' || status === 'failed') {
      message = getConfer();
    }
    conferenceStore.updateConferInfo(message);
  }

  // 更新状态
  function handleConferenceMember(message) {
    conferenceStore.updateConferMember(message.conferenceMember);
  }

  // 更新状态
  function handleConferenceStatus(message) {
    conferenceStore.updateConferenceStatus(message);
  }

  function handleConferenceSpeaker(message) {
    const data = {
      chair: message.value.confStatus.chair,
      confName: message.value.confStatus.confName,
      speaking: message.value.confStatus.speaking,
    };
    conferenceStore.updateConferenceSpeaker(data);
  }
}

export default confRegister;
