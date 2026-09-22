import MessageBox from '@/components/MessageBox';
import { appConfig } from '@/config';
import { useDC } from '@/hooks';
import { getExecutorInfoByAccount } from '@/pages/resource/resourceHelper';
import { useCommunicationStore } from '@/store';

import { communicationStatus } from './commStatus';
import messageUtil from './messageUtil';
import storeCidIsdnRelation from './storeCidIsdnRelation';

function commRegister() {
  const communicationStore = useCommunicationStore();

  init();

  function init() {
    initAudioMessageHandler();
    initVideoMessageHandler();
    initMonitorMessageHandler();
    initMonitorUpMessageHandler();
    initGroupMessageHandler();
  }

  function initAudioMessageHandler() {
    useDC('acm', 'audio', async (message) => {
      // console.log("audio_web:" + JSON.stringify(message));
      // 发送成功的信令的response不与处理
      if (message.notify_message === 'p2pa_ntf_response' && message.result.ret === '0') {
        return;
      }
      if (!message.from_uid || !message.to_uid) {
        return;
      }
      const audioStat = messageUtil.audioMessage(message);
      handleMessage('voice', message, audioStat);
    });
  }

  function initVideoMessageHandler() {
    useDC('acm', 'video', async (message) => {
      // console.log("video:" + JSON.stringify(message));
      if (message.notify_message === 'p2pv_ntf_response' && message.result.ret === '0') {
        return;
      }
      const status = messageUtil.videoMessage(message);
      handleMessage('video', message, status);
    });
  }

  function initMonitorMessageHandler() {
    useDC('ms', 'monitor', async (message) => {
      // console.log("monitor:" + JSON.stringify(message));
      if (message.notify_message === 'p2pv_ntf_response' && message.result.ret === '0') {
        return;
      }
      const monitorStat = messageUtil.videoMessage(message); // 主叫
      handleMessage('monitor', message, monitorStat);
    });
  }

  function initMonitorUpMessageHandler() {
    useDC('acm', 'monitor', async (message) => {
      if (message.notify_message === 'p2pv_ntf_response' && message.result.ret === '0') {
        return;
      }
      const monitorStat = messageUtil.videoMessage(message); // 主叫
      handleMessage('monitor', message, monitorStat);
    });
  }

  function initGroupMessageHandler() {
    useDC('acm', 'group', async (message) => {
      if (message.notify_message === 'ptt_ntf_response' && message.result.ret === '0') {
        return;
      }
      const groupStat = messageUtil.groupMessage(message); // 主叫
      if (groupStat) {
        // 将群组中的speaker的isdn转换为rescId
        if (message.speaker && message.speaker !== '' && message.speaker !== '0') {
          const speakerInfo: any = await getExecutorInfoByAccount(message.speaker);
          groupStat.speaker = speakerInfo?.id;
          groupStat.speakerName = speakerInfo?.name;
        }
        handleMessage('group', message, groupStat);
      }
    });
  }

  function handleMessage(type, message, commuStatus) {
    let updateIsdn = '';
    const { NOT_SUPPORT, SEND_COMMAND_FAILED, SEND_COMMAND_TIMEOUT } = communicationStatus();
    if (type === 'group') {
      updateIsdn = message.to_uid;
      commuStatus.from_uid = message.from_uid;
    } else {
      const loginIsdn = appConfig.isdn;
      console.log(`loginIsdn is :::${loginIsdn}`);
      if (appConfig.isdn !== '' && message.from_uid === loginIsdn) {
        // 主叫
        updateIsdn = message.to_uid;
      } else if (appConfig.isdn !== '' && message.to_uid === loginIsdn) {
        // 语音被叫，点击接听、挂断，超时等消息处理
        updateIsdn = message.from_uid;
      }

      if (!updateIsdn || updateIsdn === '') {
        console.error('updateIsdn is null.');
        return;
      }
      // 如果是来电
      if (commuStatus.isCalled && message.dId && message.dId !== '') {
        storeCidIsdnRelation.add(message.dId, updateIsdn, type);
      }
      updateIsdn = storeCidIsdnRelation.getIsdnByCid(message.dId);
      if (!updateIsdn || updateIsdn === '') {
        console.log('get isdn from dIdisdnCache failed , and dId is ');
        return;
      }
    }
    // 如果没有权限、没有license和不支持等，用弹框提示
    if (
      commuStatus.status === NOT_SUPPORT.status ||
      commuStatus.status === SEND_COMMAND_TIMEOUT.status ||
      commuStatus.status === SEND_COMMAND_FAILED.status
    ) {
      MessageBox({ text: commuStatus.status.msg });
    }
    // 更新状态
    communicationStore.updateComm({
      isdn: updateIsdn,
      status: commuStatus,
      type,
    });
    if (commuStatus.endFlag === 0) {
      communicationStore.deleteComm({
        isdn: updateIsdn,
        status: commuStatus,
        type,
      });
      if (message.dId && message.dId !== '') {
        storeCidIsdnRelation.removeByCid(message.dId, type);
      }
    }
  }
}

export default commRegister;
