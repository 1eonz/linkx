import { appConfig } from '@/config';

import { communicationStatus, notSupport } from './commStatus';

export default {
  // 语音点呼，主叫返回的消息处理
  audioMessage(message) {
    const tempAudioStatus = this.genCommStatus(message);
    const {
      CALL_DROPED,
      CALLING,
      CANCEL_CALLING,
      CONNECTING,
      NO_ANSWER,
      NO_USERDATA,
      NOT_FOUND,
      PEER_REJECT,
      POWER_OFF,
      RINGING,
      WAITING_ANSWER,
    } = communicationStatus();
    switch (message.eventName) {
      case 'OnCallConnect': {
        // 主动呼叫 通话中  计时开始
        if (message.rsp === '2003') {
          tempAudioStatus.status = CALLING;
          tempAudioStatus.isCalled = false;
          tempAudioStatus.beginTime = new Date();
          tempAudioStatus.belongId = tempAudioStatus.callee;
        } else if (message.rsp === '2007') {
          tempAudioStatus.status = CALLING;
          tempAudioStatus.isCalled = true;
          tempAudioStatus.belongId = tempAudioStatus.caller;
          tempAudioStatus.beginTime = new Date();
        }
        break;
      }
      case 'OnCallRelease': {
        // 发起的呼叫未等对端接通，主叫挂机取消,被叫收到消息；2、被呼超时,被呼收到；
        switch (message.rsp) {
          case '2009': {
            tempAudioStatus.status = CALL_DROPED;
            tempAudioStatus.endTime = new Date();
            tempAudioStatus.endFlag = 0;
            break;
          }
          case '2010': {
            tempAudioStatus.status = CANCEL_CALLING;
            tempAudioStatus.endFlag = 0;
            break;
          }
          case '2011': {
            tempAudioStatus.status = CANCEL_CALLING;
            tempAudioStatus.endFlag = 0;
            break;
          }
        }
        break;
      }
      case 'OnConnectProceeding': {
        // 语音呼入
        tempAudioStatus.status = CONNECTING;
        tempAudioStatus.isCalled = true;
        tempAudioStatus.belongId = tempAudioStatus.caller;
        break;
      }
      case 'OnDialEnd': {
        if (['2016', '2048', '2049', '2052', '3045'].includes(message.rsp)) {
          tempAudioStatus.status = notSupport(message.rsp);
        }
        tempAudioStatus.endFlag = 0;
        break;
      }
      case 'OnDialInRinging': {
        // 语音呼入
        tempAudioStatus.status = WAITING_ANSWER;
        tempAudioStatus.isCalled = true;
        tempAudioStatus.belongId = tempAudioStatus.caller;
        break;
      }
      case 'OnDialOutFailure': {
        // 主呼超时 无人接听  有提示音
        switch (message.rsp) {
          case '2013':
          case '2024': {
            // 有提示音
            tempAudioStatus.status = PEER_REJECT;

            break;
          }
          case '2017': {
            // 有提示音
            tempAudioStatus.status = NO_ANSWER;

            break;
          }
          case '2018': {
            // 有提示音
            tempAudioStatus.status = NOT_FOUND;

            break;
          }
          case '2023': {
            tempAudioStatus.status = NO_USERDATA;

            break;
          }
          case '2033': {
            // 有提示音
            tempAudioStatus.status = POWER_OFF;

            break;
          }
          default: {
            if (['2016', '2048', '2049', '2052'].includes(message.rsp)) {
              tempAudioStatus.status = notSupport(message.rsp);
              tempAudioStatus.endFlag = 0;
            }
          }
        }
        break;
      }
      case 'OnDialOutProceeding': {
        tempAudioStatus.status = CONNECTING;
        tempAudioStatus.isCalled = false;
        tempAudioStatus.belongId = tempAudioStatus.callee;
        break;
      }
      case 'OnDialOutRinging': {
        tempAudioStatus.status = RINGING;
        tempAudioStatus.isCalled = false;
        tempAudioStatus.belongId = tempAudioStatus.callee;
        break;
      }
      default: {
        break;
      }
    }

    return tempAudioStatus;
  },
  distributeMessage(message) {
    let groupStatus;
    groupStatus = this.genGroupCommStatus();
    const { CALL_DROPED, GROUP_ACCEPT, GROUP_IDLE, GROUP_REJECTED, GROUP_SPEAKING } =
      communicationStatus();
    switch (message.rsp) {
      case '4021': {
        // 收到组呼请求
        groupStatus.status = GROUP_SPEAKING;
        groupStatus.speaker = message.value.speaker;
        break;
      }
      case 'OnTalkingGroupCallFailure': {
        groupStatus.status = GROUP_REJECTED;
        groupStatus.endFlag = 0;
        break;
      }
      case 'OnTalkingGroupCallPTTFailure': {
        groupStatus.status = GROUP_REJECTED;
        groupStatus.endFlag = 0;
        break;
      }
      case 'OnTalkingGroupCallPTTIdle': {
        groupStatus.status = GROUP_IDLE;
        break;
      }
      case 'OnTalkingGroupCallPTTSuccess': {
        groupStatus.status = GROUP_ACCEPT;
        break;
      }
      case 'OnTalkingGroupCallRelease': {
        // 组呼空闲通知
        groupStatus.status = CALL_DROPED;
        groupStatus.endFlag = 0;
        break;
      }
      case 'ptt_ntf_snatch':
      case 'ptt_ntf_tx_begin': {
        // 组呼别人开始讲话通知 // 收到发起组呼请求的响应
        groupStatus.status = GROUP_SPEAKING;
        groupStatus.speaker = message.value.speaker;
        break;
      }
      default: {
        console.log('error group message type!');
        groupStatus = null;
        break;
      }
    }
    return groupStatus;
  },
  genCommStatus(message) {
    const { ORIGIN } = communicationStatus();
    const { callee, caller, calltype } = message.value;
    // 默认是初始状态
    const temp = {
      beginTime: new Date(), // 业务接通开始时间；格式："yyyy-MM-dd hh:mm:ss.S"；
      belongId: '',
      callee, // 被叫号码
      caller, // 主叫号码
      camera: '',
      commType: calltype,
      device: '', // 标识设备类型；
      endFlag: -1, // 通信连接结束标志； 0：标识结束；非0：后续还有消息；
      endTime: new Date(), // 业务结束时间点；格式："yyyy-MM-dd hh:mm:ss.S"；
      isCalled: false, // 标识被叫；
      status: ORIGIN, // 通信链路状态；
    };
    return temp;
  },
  generateStartComm(toUid) {
    const message = {
      value: {
        callee: appConfig.isdn,
        caller: toUid,
      },
    };
    const status = this.genCommStatus(message);
    status.isCalled = true;
    return status;
  },
  genGroupCommStatus() {
    const { ORIGIN } = communicationStatus();
    // 默认是初始状态
    const temp = {
      beginTime: new Date(), // 业务接通开始时间；格式："yyyy-MM-dd hh:mm:ss.S"；
      endFlag: -1, // 通信连接结束标志； 0：标识结束；非0：后续还有消息；
      endTime: new Date(), // 业务结束时间点；格式："yyyy-MM-dd hh:mm:ss.S"；
      from_uid: '', // 组呼通信中标识，信息发送者，显示讲话的人等；
      isCalled: false, // 标识被叫
      speaker: '',
      speakerName: '',
      status: ORIGIN, // 通信链路状态；
    };
    return temp;
  },
  // 组呼
  groupMessage(message) {
    let groupStatus;
    groupStatus = this.genGroupCommStatus();
    const { CALL_DROPED, GROUP_ACCEPT, GROUP_IDLE, GROUP_REJECTED, GROUP_SPEAKING } =
      communicationStatus();
    switch (message.eventName) {
      case 'OnGroupCallStatusNotify':
      case 'OnTalkingGroupCallPTTNotify': // 收到组呼请求
      case 'OnTalkingGroupCallPTTStart': // 收到发起组呼请求的响应
      case 'ptt_ntf_snatch': // 组呼别人开始讲话通知
      case 'ptt_ntf_tx_begin': {
        const { speaker } = message.value;
        groupStatus.speaker = speaker === '0' ? '' : speaker;
        if (message.rsp === '4028') {
          groupStatus.status = GROUP_REJECTED;
          groupStatus.endFlag = 0;
        } else {
          groupStatus.status = GROUP_SPEAKING;
        }
        break;
      }
      case 'OnTalkingGroupCallFailure': {
        groupStatus.status = GROUP_REJECTED;
        groupStatus.endFlag = 0;
        break;
      }
      case 'OnTalkingGroupCallPTTFailure': {
        break;
      }
      case 'OnTalkingGroupCallPTTIdle': {
        // 组呼空闲事件
        groupStatus.status = GROUP_IDLE;
        groupStatus.endFlag = 0;
        break;
      }
      case 'OnTalkingGroupCallPTTSuccess': {
        groupStatus.status = GROUP_ACCEPT;
        break;
      }
      case 'OnTalkingGroupCallRelease': {
        // 组呼释放事件
        groupStatus.status = CALL_DROPED;
        groupStatus.endFlag = 0;
        break;
      }
      default: {
        console.log('error group message type!');
        groupStatus = null;
        break;
      }
    }
    return groupStatus;
  },
  // 视频点呼，视频查看，消息处理
  videoMessage(message) {
    const tempVideoStatus = this.genCommStatus(message);
    const {
      CALL_DROPED,
      CALLING,
      CANCEL_CALLING,
      CONNECTING,
      END_OF_BEEP,
      NO_ANSWER,
      NOT_FOUND,
      PEER_REJECT,
      PEER_UNAVAILABLE,
      POWER_OFF,
      RINGING,
      WAITING_ANSWER,
    } = communicationStatus();
    switch (message.eventName) {
      case 'OnCallConnect': {
        // 主叫应答 开始计时
        switch (message.rsp) {
          case '2003': {
            tempVideoStatus.status = CALLING;
            tempVideoStatus.isCalled = false;
            tempVideoStatus.beginTime = new Date();
            tempVideoStatus.belongId = tempVideoStatus.callee;
            break;
          }
          case '2007': {
            tempVideoStatus.status = CALLING;
            tempVideoStatus.isCalled = true;
            tempVideoStatus.beginTime = new Date();
            tempVideoStatus.belongId = tempVideoStatus.caller;
            break;
          }
          case '3003': {
            tempVideoStatus.status = CALLING;
            tempVideoStatus.isCalled = false;
            tempVideoStatus.beginTime = new Date();
            tempVideoStatus.belongId = tempVideoStatus.callee;
            break;
          }
          case '3006': {
            tempVideoStatus.status = CALLING;
            tempVideoStatus.isCalled = true;
            tempVideoStatus.beginTime = new Date();
            tempVideoStatus.belongId = tempVideoStatus.caller;
            break;
          }
          case '3007': {
            tempVideoStatus.status = CALLING;
            tempVideoStatus.isCalled = true;
            tempVideoStatus.beginTime = new Date();
            tempVideoStatus.belongId = tempVideoStatus.caller;
            break;
          }
        }
        break;
      }
      case 'OnCallRelease': {
        // 发起的呼叫未等对端接通，主叫挂机取消,被叫收到消息；2、被呼超时,被呼收到；
        switch (message.rsp) {
          case '3008': {
            tempVideoStatus.status = CALL_DROPED;
            tempVideoStatus.endFlag = 0;
            break;
          }
          case '3009': {
            tempVideoStatus.status = CANCEL_CALLING;
            tempVideoStatus.endFlag = 0;
            break;
          }
          case '3010': {
            tempVideoStatus.status = CALL_DROPED;
            tempVideoStatus.endTime = new Date();
            tempVideoStatus.endFlag = 0;
            break;
          }
          default: {
            tempVideoStatus.status = CALL_DROPED;
            tempVideoStatus.endTime = new Date();
            tempVideoStatus.endFlag = 0;
            break;
          }
        }
        break;
      }
      case 'OnConnectProceeding': {
        // 视频呼入
        tempVideoStatus.status = CONNECTING;
        tempVideoStatus.isCalled = true;
        tempVideoStatus.belongId = tempVideoStatus.caller;
        break;
      }
      case 'OnDialEnd': {
        // 提示音结束l
        tempVideoStatus.status = END_OF_BEEP;
        if (['2016', '2048', '2049', '2052', '3045'].includes(message.rsp)) {
          tempVideoStatus.status = notSupport(message.rsp);
        }
        tempVideoStatus.endFlag = 0;
        break;
      }
      case 'OnDialInRinging': {
        // 等待接听 | 收到视频分发的请求;
        tempVideoStatus.status = WAITING_ANSWER;
        tempVideoStatus.isCalled = true;
        tempVideoStatus.belongId = tempVideoStatus.caller;
        tempVideoStatus.camera = message.camera;
        break;
      }
      case 'OnDialOutFailure': {
        // 对方忙
        if (!message.rsp || message.rsp === '') {
          tempVideoStatus.status = notSupport(message.rsp);
          tempVideoStatus.endFlag = 0;
        }
        switch (message.rsp) {
          case '3013': {
            // 有提示音
            tempVideoStatus.status = PEER_REJECT;

            break;
          }
          case '3015': {
            // 有提示音
            tempVideoStatus.status = NO_ANSWER;

            break;
          }
          case '3016': {
            // 有提示音
            tempVideoStatus.status = NOT_FOUND;

            break;
          }
          case '3021': {
            // 有提示音
            tempVideoStatus.status = POWER_OFF;

            break;
          }
          default: {
            tempVideoStatus.status = notSupport(message.rsp);
            tempVideoStatus.endFlag = 0;
          }
        }
        break;
      }
      case 'OnDialOutProceeding': {
        // 连接中
        tempVideoStatus.status = CONNECTING;
        tempVideoStatus.isCalled = false;
        tempVideoStatus.belongId = tempVideoStatus.callee;
        break;
      }
      case 'OnDialOutRinging': {
        // 振铃中
        tempVideoStatus.status = RINGING;
        tempVideoStatus.isCalled = false;
        tempVideoStatus.belongId = tempVideoStatus.callee;
        break;
      }
      default: {
        tempVideoStatus.status = PEER_UNAVAILABLE;
        tempVideoStatus.endFlag = 0;
        break;
      }
    }
    return tempVideoStatus;
  },
};
