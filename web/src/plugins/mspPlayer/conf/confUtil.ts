export default {
  // 视频会议消息处理
  conferenceMessage(message) {
    const conferenceStatus = this.genConferStatus();
    switch (message.eventName) {
      case 'OnConfConnect': {
        // 会议通话事件
        const { cid, confId, isVideo, wssUrl } = message.value;
        Object.assign(conferenceStatus, {
          belongId: conferenceStatus.caller,
          cid,
          conferenceId: confId,
          isCalled: true,
          isVideo,
          status: 'success',
          wssUrl,
        });
        break;
      }
      case 'OnConfDialInRinging': {
        // 会议呼入振铃事件
        const { caller, cid, confId, isVideo } = message.value;
        Object.assign(conferenceStatus, {
          belongId: conferenceStatus.caller,
          cid,
          conferenceId: confId,
          isCalled: true,
          isMain: false,
          isVideo,
          status: 'ringing',
          unifiedAccessCode: caller,
        });
        break;
      }
      case 'OnConfDialOutRinging': {
        // 会议呼出回铃事件
        const { callee, confId, isVideo } = message.value;
        Object.assign(conferenceStatus, {
          belongId: conferenceStatus.callee,
          conferenceId: confId,
          isCalled: false,
          isMain: true,
          isVideo,
          status: 'proceeding',
          unifiedAccessCode: callee,
        });
        break;
      }
      case 'OnConfFailure': {
        // 会议通话失败
        break;
      }
      case 'OnConfRelease': {
        // 会议释放事件
        const { confId, isVideo } = message.value;
        Object.assign(conferenceStatus, {
          belongId: conferenceStatus.caller,
          conferenceId: confId,
          isCalled: true,
          isVideo,
          status: 'end',
        });
        break;
      }
      case 'OnConfStatusNotify': {
        // 会议状态通知
        conferenceStatus.conferenceMember = message.value.confMembersStatus;
        break;
      }
      case 'OnCreateConfFailure': {
        // 创建音视频会议失败
        conferenceStatus.status = 'failed';
        conferenceStatus.conferenceId = message.value.confId;
        break;
      }
      case 'OnCreateConfSuccess': {
        // 创建音视频会议成功
        const { confId, isVideo, passcode, unifiedAccessCode } = message.value;
        Object.assign(conferenceStatus, {
          belongId: conferenceStatus.callee,
          conferenceId: confId,
          conferencePass: passcode,
          isCalled: false,
          isVideo,
          status: 'proceeding',
          unifiedAccessCode,
        });
        break;
      }
      case 'OnEndConfFailure': {
        // 结束会议失败
        break;
      }
      case 'OnEndConfSuccess': {
        // 结束会议成功
        conferenceStatus.status = 'end';
        conferenceStatus.conferenceId = message.value.confId;
        break;
      }
      case 'OnSubscribeConfFailure': {
        // 订阅会议失败
        break;
      }
      case 'OnSubscribeConfSuccess': {
        // 订阅会议成功
        break;
      }
      case 'OnUnsubscribeConfFailure': {
        // 去订阅会议失败
        break;
      }
      case 'OnUnsubscribeConfSuccess': {
        // 去订阅会议成功
        break;
      }
      default: {
        break;
      }
    }
    return conferenceStatus;
  },
  // 默认是初始状态
  genConferStatus() {
    const temp = {
      belongId: '',
      callee: '',
      caller: '',
      conferenceId: '',
      conferenceMember: [],
      conferencePass: '',
      isCalled: false,
      isVideo: false,
      status: '',
      unifiedAccessCode: '',
      updateTime: '',
    };
    return temp;
  },
};
