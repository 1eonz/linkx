import { Message } from '@/components/Message';
import { appConfig } from '@/config';
import { CategoryEnum } from '@/enums';
import { useI18n } from '@/hooks';
import {
  getExecutorInfoByAccount,
  getHistoryCallInfo,
  getInfoByAccount,
} from '@/pages/resource/resourceHelper';
import { mediaFunc, voiceFunc } from '@/plugins/mspPlayer';
import { useCommunicationStore } from '@/store';

import { communicationStatus } from '../commStatus';
import { sdkCallbackPrint, triggerSDKMethods } from '../helper';
import messageUtil from '../messageUtil';
import storeCidIsdnRelation from '../storeCidIsdnRelation';

function voiceRegister() {
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  init();

  function init() {
    // 外呼呼出事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('外呼呼出事件', data);
        const { callee, calltype, cid } = data.value;
        if (data.rsp === '2005') {
          if (calltype === 'voice') {
            // 语音点呼
            storeCidIsdnRelation.add(cid, callee, 'voice');
            const audioStat = messageUtil.audioMessage(data);
            handleMessage('voice', data, audioStat);
          } else if (calltype === 'halfdial') {
            // 半双工点呼
            storeCidIsdnRelation.add(cid, callee, 'halfdial');
            const audioStat = messageUtil.audioMessage(data);
            handleMessage('halfdial', data, audioStat);
          }
        } else if (data.rsp === '3004') {
          if (calltype === 'video') {
            // 视频点呼
            storeCidIsdnRelation.add(cid, callee, 'video');
            const audioStat = messageUtil.videoMessage(data);
            handleMessage('video', data, audioStat);
          } else if (calltype === 'monitor') {
            // 视频查看
            storeCidIsdnRelation.add(cid, callee, 'monitor');
            const audioStat = messageUtil.videoMessage(data);
            handleMessage('monitor', data, audioStat);
          }
        }
      },
      eventName: 'OnDialOutProceeding',
      eventType: 'VoiceNotify',
    });

    // 呼出回铃事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('呼出回铃事件', data);
        const { calltype } = data.value;

        // 监控取消振铃
        if (calltype === 'monitor') {
          mediaFunc.stopPlayTone();
        }

        if (data.rsp === '2006') {
          if (calltype === 'voice') {
            // 语音点呼
            const audioStat = messageUtil.audioMessage(data);
            handleMessage('voice', data, audioStat);
          } else if (calltype === 'halfdial') {
            // 半双工点呼
            const audioStat = messageUtil.audioMessage(data);
            handleMessage('halfdial', data, audioStat);
          }
        } else if (data.rsp === '3005') {
          if (calltype === 'video') {
            // 视频点呼
            const audioStat = messageUtil.videoMessage(data);
            handleMessage('video', data, audioStat);
          } else if (calltype === 'monitor') {
            // 视频查看
            const audioStat = messageUtil.videoMessage(data);
            handleMessage('monitor', data, audioStat);
          }
        }
      },
      eventName: 'OnDialOutRinging',
      eventType: 'VoiceNotify',
    });

    // 呼出失败事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('呼出失败事件', data);
        const { calltype } = data.value;

        // 监控取消振铃 - 提示弹窗就行
        if (calltype === 'monitor') {
          mediaFunc.stopPlayTone();
        }

        const status = {
          2013: t('communication.communicationTips.dialOutFailure2013'),
          2016: t('communication.communicationTips.dialOutFailure2016'),
          2017: t('communication.communicationTips.dialOutFailure2017'),
          2018: t('communication.communicationTips.dialOutFailure2018'),
          2023: t('communication.communicationTips.dialOutFailure2023'),
          2024: t('communication.communicationTips.dialOutFailure2024'),
          2033: t('communication.communicationTips.dialOutFailure2033'),
          2048: t('communication.communicationTips.dialOutFailure2048'),
          2049: t('communication.communicationTips.dialOutFailure2049'),
          2052: t('communication.communicationTips.dialOutFailure2052'),
          3013: t('communication.communicationTips.dialOutFailure3013'),
          3014: t('communication.communicationTips.dialOutFailure3014'),
          3015: t('communication.communicationTips.dialOutFailure3015'),
          3016: t('communication.communicationTips.dialOutFailure3016'),
          3021: t('communication.communicationTips.dialOutFailure3021'),
          3044: t('communication.communicationTips.dialOutFailure3044'),
          3045: t('communication.communicationTips.dialOutFailure3045'),
          3048: t('communication.communicationTips.dialOutFailure3048'),
        };
        Message(status[data.rsp]);
        if (data.rsp.startsWith('2')) {
          data.eventName = 'OnDialEnd';
          if (calltype === 'voice') {
            // 语音点呼
            const audioStat = messageUtil.audioMessage(data);
            handleMessage('voice', data, audioStat);
          } else if (calltype === 'halfdial') {
            // 半双工点呼
            const audioStat = messageUtil.audioMessage(data);
            handleMessage('halfdial', data, audioStat);
          }
        } else if (data.rsp.startsWith('3')) {
          if (calltype === 'video') {
            // 视频点呼
            data.eventName = 'OnDialEnd';
            const audioStat = messageUtil.videoMessage(data);
            handleMessage('video', data, audioStat);
          } else if (calltype === 'monitor') {
            // 视频查看
            data.eventName = 'OnDialEnd';
            const audioStat = messageUtil.videoMessage(data);
            handleMessage('monitor', data, audioStat);
          }
        }
      },
      eventName: 'OnDialOutFailure',
      eventType: 'VoiceNotify',
    });

    // 用户状态变化通知事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('用户状态变化通知事件', data);
        const { CALLING } = communicationStatus();
        data.list.forEach((item) => {
          const { peerid, statusvalue } = item;
          // 半双工点呼发起
          if (statusvalue === '4040') {
            communicationStore.updateComm({
              isdn: peerid,
              status: { status: CALLING },
              type: 'halfdial',
              value: {},
            });
          }
          // 半双工点呼挂断
          if (statusvalue === '4041') {
            communicationStore.deleteComm({
              isdn: peerid,
              type: 'halfdial',
            });
          }
        });
      },
      eventName: 'OnUserStatusNotify',
      eventType: 'VoiceNotify',
    });

    // 呼入振铃事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('呼入振铃事件', data);
        // 取主叫 caller
        const { caller, calltype, cid, src } = data.value;
        if (data.rsp === '2002') {
          // 语音点呼
          storeCidIsdnRelation.add(cid, caller, 'voice');
          const audioStat = messageUtil.audioMessage(data);
          // 判断是否当前正在通话
          const isAnswer = Boolean(isAlreadyVoiceForAnswer());
          if (isAnswer) {
            handleMessage('voice', data, audioStat, true);
          } else {
            handleMessage('voice', data, audioStat);
          }
        } else if (data.rsp === '3002' && calltype === 'video') {
          // 视频点呼
          storeCidIsdnRelation.add(cid, caller, 'video');
          const audioStat = messageUtil.videoMessage(data);
          handleMessage('video', data, audioStat);
        } else if (data.rsp === '3002' && calltype === 'monitor') {
          // 视频点查看
          storeCidIsdnRelation.add(cid, caller, 'monitor');
          const audioStat = messageUtil.videoMessage(data);
          handleMessage('monitor', data, audioStat);
        } else if (data.rsp === '3011' && calltype === 'dispatch') {
          // 视频分享-视频回传
          const calltype = 'monitor';
          Object.assign(data.value, {
            calltype,
            dispatch: true,
          });
          storeCidIsdnRelation.add(cid, src === '-1' ? caller : src, calltype);
          const audioStat = messageUtil.videoMessage(data);
          handleMessage(calltype, data, audioStat);
        }
      },
      eventName: 'OnDialInRinging',
      eventType: 'VoiceNotify',
    });

    // 呼入应答事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('呼入应答事件', data);
        // 取主叫 caller
        const { caller, calltype, cid, src } = data.value;
        if (data.rsp === '2040') {
          // 语音点呼
          storeCidIsdnRelation.add(cid, caller, 'voice');
          const audioStat = messageUtil.audioMessage(data);
          handleMessage('voice', data, audioStat);
        } else if (data.rsp === '3040' && calltype === 'video') {
          // 视频点呼
          storeCidIsdnRelation.add(cid, caller, 'video');
          const audioStat = messageUtil.videoMessage(data);
          handleMessage('video', data, audioStat);
        } else if (data.rsp === '3040' && calltype === 'monitor') {
          // 视频查看
          storeCidIsdnRelation.add(cid, caller, 'monitor');
          const audioStat = messageUtil.videoMessage(data);
          handleMessage('monitor', data, audioStat);
        } else if (data.rsp === '3040' && calltype === 'dispatch') {
          // 视频查看
          const calltype = 'monitor';
          Object.assign(data.value, {
            calltype,
            dispatch: true,
          });
          storeCidIsdnRelation.add(cid, src === '-1' ? caller : src, calltype);
          const audioStat = messageUtil.videoMessage(data);
          handleMessage(calltype, data, audioStat);
        }
      },
      eventName: 'OnConnectProceeding',
      eventType: 'VoiceNotify',
    });

    // 保持成功事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('语音保持成功', data);
        if (data.rsp === '0') {
          // 保持成功
          const audioStat = messageUtil.audioMessage(data);
          handleMessage('hold', data, audioStat);
        }
      },
      eventName: 'OnHoldSuccess',
      eventType: 'VoiceNotify',
    });

    // 取消保持成功事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('语音取消保持成功', data);
        if (data.rsp === '0') {
          // 取消保持删除掉保持的状态即可
          communicationStore.deleteComm({
            isdn: data.value.peerid,
            type: 'hold',
          });
        }
      },
      eventName: 'OnUnholdSuccess',
      eventType: 'VoiceNotify',
    });

    // 半双工抢权成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('半双工抢权成功', data);
      },
      eventName: 'OnHalfDialSuccess',
      eventType: 'VoiceNotify',
    });

    // 半双工抢权失败
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('半双工抢权失败', data);
      },
      eventName: 'OnHalfDialFailure',
      eventType: 'VoiceNotify',
    });

    // 开始接收半双工语音
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('开始接收半双工语音', data);
      },
      eventName: 'OnStartRecvHalfDial',
      eventType: 'VoiceNotify',
    });

    // 停止接收半双工语音
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('停止接收半双工语音', data);
      },
      eventName: 'OnStopRecvHalfDial',
      eventType: 'VoiceNotify',
    });

    // 通话事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('通话事件', data);
        const { calltype, cid } = data.value;
        if (data.rsp.startsWith('2') && calltype === 'voice') {
          // 语音点呼
          const audioStat = messageUtil.audioMessage(data);
          handleMessage('voice', data, audioStat);
        } else if (data.rsp.startsWith('3') && calltype === 'video') {
          // 视频点呼
          const audioStat = messageUtil.videoMessage(data);
          handleMessage('video', data, audioStat);
        } else if (data.rsp.startsWith('3') && calltype === 'monitor') {
          // 视频查看
          const { callee, cid } = data.value;
          const id = storeCidIsdnRelation.getIsdnByCid(cid);
          if (!id) {
            storeCidIsdnRelation.add(cid, callee, 'monitor');
          }
          const audioStat = messageUtil.videoMessage(data);
          handleMessage('monitor', data, audioStat);
        } else if (data.rsp.startsWith('3') && calltype === 'dispatch') {
          // 视频查看
          const audioStat = messageUtil.videoMessage(data);
          const calltype = 'monitor';
          Object.assign(data.value, {
            calltype,
            dispatch: true,
          });
          handleMessage(calltype, data, audioStat);
        } else if (calltype === 'halfdial') {
          // 半双工
          // 默认接听是抢权状态，接听后需要调用释放，不然pdt没法抢权说话
          const audioStat = messageUtil.videoMessage(data);
          handleMessage('halfdial', data, audioStat);
          setTimeout(() => {
            voiceFunc.voiceReleaseHalfDial(audioStat.belongId, cid);
          }, 0);
        }
      },
      eventName: 'OnCallConnect',
      eventType: 'VoiceNotify',
    });

    // 通话挂机事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('通话挂机事件', data);
        const { calltype } = data.value;
        if (data.rsp.startsWith('2') && calltype === 'voice') {
          // 语音点呼
          const audioStat = messageUtil.audioMessage(data);
          handleMessage('voice', data, audioStat);
        } else if (data.rsp.startsWith('3') && calltype === 'video') {
          // 视频点呼
          const audioStat = messageUtil.videoMessage(data);
          handleMessage('video', data, audioStat);
        } else if (data.rsp.startsWith('3') && calltype === 'monitor') {
          // 视频点呼
          const audioStat = messageUtil.videoMessage(data);
          handleMessage('monitor', data, audioStat);
        } else if (data.rsp.startsWith('3') && calltype === 'dispatch') {
          // 视频分发
          const calltype = 'monitor';
          Object.assign(data.value, {
            calltype,
            dispatch: true,
          });
          const audioStat = messageUtil.videoMessage(data);
          handleMessage(calltype, data, audioStat);
        }
      },
      eventName: 'OnCallRelease',
      eventType: 'VoiceNotify',
    });

    // 视频分发成功通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('视频分发成功通知', data);
        Message(t('communication.communicationTips.videoDistributionSucceeded'));
      },
      eventName: 'OnVideoDispatchSuccess',
      eventType: 'VideoNotify',
    });

    // 视频分发失败通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('视频分发失败通知', data);
        Message(t('communication.communicationTips.videoDistributionFailure'));
      },
      eventName: 'OnVideoDispatchFailure',
      eventType: 'VideoNotify',
    });

    // 视频分发状态通知
    triggerSDKMethods('event', 'register', {
      callback: async (data) => {
        sdkCallbackPrint('视频分发状态通知', data);
        const { caller, peerid, src } = data.value;

        const personInfo = await getInfoByAccount(peerid);
        // 如果存在领用人则显示领用用人名称
        const executor: any = await getExecutorInfoByAccount(peerid);
        personInfo.name = executor?.name;

        const msgStatus = {
          isdn: peerid,
          personInfo,
          status: data.rsp,
        };

        // 事件码为4021：振铃中,4022：正在通话,4023：空闲
        if (data.rsp === '4022') {
          communicationStore.updateDistributeStatus({
            isdn: src === '-1' ? caller : src,
            peerId: peerid,
            status: msgStatus,
          });
        } else if (data.rsp === '4023') {
          communicationStore.deleteDistributeStatus({
            isdn: src === '-1' ? caller : src,
            peerId: peerid,
            status: msgStatus,
          });
        }
      },
      eventName: 'OnVideoDispatchStatusNotify',
      eventType: 'VideoNotify',
    });
  }

  function isAlreadyVoiceForAnswer() {
    const { comm } = useCommunicationStore();
    const keys = Object.keys(comm);
    let isWorking;
    const { CALLING } = communicationStatus();

    keys.forEach((key) => {
      if (key === 'updateInfo') return;
      const _comm = comm[key];
      const target = _comm.voice;
      if (target?.status.status === CALLING.status) {
        isWorking = {
          commType: target.commType,
          id: key,
        };
      }
    });
    return isWorking;
  }

  function handleMessage(type, message, commStatus, isHold?) {
    const { callee, caller, cid, dispatch, src } = message.value;
    const loginIsdn = appConfig.isdn;
    const { NOT_SUPPORT, SEND_COMMAND_FAILED, SEND_COMMAND_TIMEOUT } = communicationStatus();
    let updateIsdn = '';
    saveHistoryCall(message);

    if (dispatch) {
      // 视频分享
      updateIsdn = src === '-1' ? caller : src;
    } else if (loginIsdn !== '' && caller === loginIsdn) {
      // 主叫
      updateIsdn = callee;
    } else if (loginIsdn !== '' && callee === loginIsdn) {
      // 语音被叫，点击接听、挂断，超时等消息处理
      updateIsdn = caller;
    }

    if (!updateIsdn) {
      updateIsdn = storeCidIsdnRelation.getIsdnByCid(cid);
    }

    // 如果是来电
    if (commStatus.isCalled && cid && cid !== '') {
      storeCidIsdnRelation.add(cid, updateIsdn, type);
    }

    // 如果没有权限、没有license和不支持等，用弹框提示
    if (
      commStatus.status.status === NOT_SUPPORT.status ||
      commStatus.status.status === SEND_COMMAND_TIMEOUT.status ||
      commStatus.status.status === SEND_COMMAND_FAILED.status
    ) {
      Message({
        message: commStatus.status.msg,
        type: 'warning',
      });
    }

    if (commStatus.endFlag === 0) {
      const did = storeCidIsdnRelation.getCidByIsdn(updateIsdn, type);
      if (!did || (did && did === cid)) {
        communicationStore.deleteComm({
          isdn: updateIsdn,
          status: commStatus,
          type,
        });
      }

      if (cid) {
        storeCidIsdnRelation.removeByCid(cid, type);
      }
    } else {
      // 更新状态
      if (isHold) {
        communicationStore.updateComm({
          isdn: updateIsdn,
          status: commStatus,
          type,
          value: { ...message.value, isHold: true },
        });
      } else {
        communicationStore.updateComm({
          isdn: updateIsdn,
          status: commStatus,
          type,
          value: message.value,
        });
      }
    }
  }

  // 来电去电记录
  async function saveHistoryCall(message) {
    const { getAllCallRecordPage, saveCallRecords, setTemporarilyDialList, temporarilyDialList } =
      communicationStore;
    const { eventName } = message;
    const { callee, calltype, cid } = message.value;
    const caller = message.value.caller;

    const index = temporarilyDialList.findIndex((i: any) => i.cid === cid);
    const loginIsdn = appConfig.isdn;
    const isCalled = loginIsdn === callee; // 是否是被呼叫者
    const belongId = isCalled ? caller : callee;

    if (index === -1) {
      setTemporarilyDialList([
        ...temporarilyDialList,
        {
          belongId,
          belongName: '',
          callee,
          calleeName: '',
          caller,
          callerName: '', // 主呼
          callType: calltype,
          cid,
          endTime: '',
          isCalled,
          isConnect: false,
          isRead: false,
          startTime: Date.now(),
          status: 'ringing',
        },
      ]);
    } else {
      const target = temporarilyDialList[index];
      switch (eventName) {
        case 'OnCallConnect': {
          Object.assign(target, {
            isConnect: true,
            startTime: Date.now(),
            status: 'connect',
          });
          break;
        }
        case 'OnCallRelease':
        case 'OnDialEnd': {
          Object.assign(target, {
            endTime: Date.now(),
            status: 'release',
          });
          break;
        }
      }
      setTemporarilyDialList(temporarilyDialList);

      // icc被呼：主呼调度员(icc)不存，其余icc存
      // icc主呼：icc存
      const { calleeInfo, callerInfo } = await getHistoryCallInfo(caller, callee);
      if (['OnCallRelease', 'OnDialEnd'].includes(eventName)) {
        if (isCalled && callerInfo.category === CategoryEnum.seat) {
          return;
        }
        Object.assign(target, {
          belongName: (isCalled ? callerInfo : calleeInfo).name || belongId,
          calleeName: calleeInfo.name,
          callerName: callerInfo.name, // 主呼
        });
        await saveCallRecords(target);
        await getAllCallRecordPage();
      }
    }
  }
}

export default voiceRegister;
