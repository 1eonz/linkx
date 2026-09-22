import { markRaw } from 'vue';

import { useI18n } from '@/hooks';

export const communicationStatus = () => {
  const { t } = useI18n();

  return {
    CALL_DROPED: markRaw({
      msg: t('communication.communicationStatus.audioWasDropped'),
      status: 'audio was dropped',
    }),
    // 通话中
    CALLING: markRaw({
      msg: t('communication.communicationStatus.calling'),
      status: 'calling',
    }),
    CANCEL_CALLING: markRaw({
      msg: t('communication.communicationStatus.cancelCalling'),
      status: 'cancel calling',
    }),
    // 连接中
    CONNECTING: markRaw({
      msg: t('communication.communicationStatus.connecting'),
      status: 'connecting',
    }),
    // 提示音结束
    END_OF_BEEP: markRaw({
      msg: t('communication.communicationStatus.endBeep'),
      status: 'end of beep',
    }),
    FAILED: markRaw({
      msg: t('communication.communicationStatus.failed'),
      status: 'failed',
    }),
    GROUP_ACCEPT: markRaw({
      msg: t('communication.communicationStatus.groupAccept'),
      status: 'accept',
    }),
    GROUP_IDLE: markRaw({
      msg: t('communication.communicationStatus.groupIdle'),
      status: 'group idle',
    }),
    // 组呼通信状态
    GROUP_RECEIVED: markRaw({
      msg: t('communication.communicationStatus.groupReceived'),
      status: 'received',
    }),
    GROUP_REJECTED: markRaw({
      msg: t('communication.communicationStatus.groupRejected'),
      status: 'group rejected',
    }),
    GROUP_SNATCH: markRaw({
      msg: t('communication.communicationStatus.groupReject'),
      status: 'reject',
    }),
    GROUP_SPEAKING: markRaw({
      msg: t('communication.communicationStatus.groupBegin'),
      status: 'begin',
    }),
    // 无应答
    NO_ANSWER: markRaw({
      msg: t('communication.communicationStatus.noAnswer'),
      status: 'no answer',
    }),
    NO_LICENCE: markRaw({
      msg: t('communication.communicationStatus.nolicence'),
      status: 'nolicence',
    }),
    // 不支持
    NO_PERMISSION: markRaw({
      msg: t('communication.communicationStatus.nopermission'),
      status: 'nopermission',
    }),
    NO_USERDATA: markRaw({
      msg: t('communication.communicationStatus.nouserdata'),
      status: 'nouserdata',
    }),
    NOT_FOUND: markRaw({
      msg: t('communication.communicationStatus.notfound'),
      status: 'notfound',
    }),
    NOT_SUPPORT: markRaw({
      msg: t('communication.communicationStatus.notSupport'),
      status: 'not support',
    }),
    ORIGIN: markRaw({
      msg: t('communication.communicationStatus.unknown'),
      status: 'init',
    }),
    OTHER: markRaw({
      msg: t('communication.communicationStatus.other'),
      status: 'other',
    }),
    // 对端不存在
    PEER_NOT_EXIST: markRaw({
      msg: t('communication.communicationStatus.peerNotExist'),
      status: 'peer not exist',
    }),
    PEER_REJECT: markRaw({
      msg: t('communication.communicationStatus.peerReject'),
      status: 'peer reject',
    }),
    // 对方暂时无法接通
    PEER_UNAVAILABLE: markRaw({
      msg: t('communication.communicationStatus.peerUnavailable'),
      status: 'peer unavailable',
    }),
    POWER_OFF: markRaw({
      msg: t('communication.communicationStatus.poweroff'),
      status: 'poweroff',
    }),
    // 振铃中
    RINGING: markRaw({
      msg: t('communication.communicationStatus.ringing'),
      status: 'ringing',
    }),
    SEND_COMMAND_FAILED: markRaw({
      msg: t('communication.communicationStatus.sendCommandFailed'),
      status: 'send command failed.',
    }),
    // 消息发送失败
    SEND_COMMAND_TIMEOUT: markRaw({
      msg: t('communication.communicationStatus.sendCommandTimeout'),
      status: 'send command timeout.',
    }),
    STACK_ERROR: markRaw({
      msg: t('communication.communicationStatus.stackerror'),
      status: 'stackerror',
    }),
    // 无法连接
    UNABLE_CONNECT: markRaw({
      msg: t('communication.communicationStatus.unableConnect'),
      status: 'unable connect',
    }),
    WAITING_ANSWER: markRaw({
      msg: t('communication.communicationStatus.incoming'),
      status: 'incoming',
    }),
  };
};

export function notSupport(message) {
  const { t } = useI18n();
  const { NOT_SUPPORT } = communicationStatus();
  switch (message) {
    case '2016':
    case '2048':
    case '2049':
    case '3014':
    case '3045': {
      NOT_SUPPORT.msg = t('communication.communicationStatus.nopermission');
      break;
    }
    case '2052':
    case '3048': {
      NOT_SUPPORT.msg = t('communication.communicationStatus.other');
      break;
    }
    case '3044': {
      NOT_SUPPORT.msg = t('communication.communicationStatus.nolicence');
      break;
    }
    default: {
      NOT_SUPPORT.msg = t('communication.communicationStatus.nopermission');
      break;
    }
  }
  return NOT_SUPPORT;
}
