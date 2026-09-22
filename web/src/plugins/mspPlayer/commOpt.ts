import type { MonitorVideoOpt, VideoDialOpt } from './video/videoFunc';

import { queryEquipmentsByParam } from '@/api/equipment';
import { Message } from '@/components/Message';
import MessageBox from '@/components/MessageBox';
import { appConfig } from '@/config';
import { CategoryEnum, VideoCallEnum } from '@/enums';
import { useI18n } from '@/hooks';
import { smsFunc, videoFunc, voiceFunc } from '@/plugins/mspPlayer';
import { useCommunicationStore, useMonitorStore } from '@/store';

import { communicationStatus } from './commStatus';
import messageUtil from './messageUtil';
import storeCidIsdnRelation from './storeCidIsdnRelation';

export default {
  // 接听
  async answer(type, fromUid, toUid) {
    const { t } = useI18n();
    if (!type || !fromUid || !toUid) {
      return '-1';
    }
    // 判断通信是否有准备好
    if (!this.isCommReady()) {
      return '-1';
    }
    // 判断是否已经通话
    const temp = this.isAlreadyCommForAnswer(type);
    if (temp) {
      const msgRes = await MessageBox({
        offset: ['40%', '30%'],
        text: t('communication.communicationTips.closeOngoingCall'),
      });
      if (!msgRes) {
        return '-1';
      }
      // 关闭原来的通话
      const typeStr = temp.commType;
      this.hangUp(typeStr, toUid, temp.id);
      // 500ms 延迟是因为通信时间挂断可能并没有正真挂断
      await new Promise((resolve) => {
        setTimeout(() => {
          resolve(true);
        }, 500);
      });
    }
    const dId = storeCidIsdnRelation.getCidByIsdn(fromUid, type);
    switch (type) {
      case 'halfdial': {
        voiceFunc.voiceCloseHalfDial(dId);
        break;
      }
      case 'monitor': {
        await videoFunc.videoAnswer(VideoCallEnum.MONITOR_CONTAINER + fromUid, dId);
        break;
      }
      case 'video': {
        await videoFunc.videoAnswer(VideoCallEnum.CONTAINER + fromUid, dId);
        break;
      }
      case 'voice': {
        voiceFunc.voiceAnswer(dId);
        break;
      }
      default: {
        break;
      }
    }
    return '0';
  },
  // 挂断当前通话并接听
  async answerAndReject(type, fromUid, toUid) {
    if (!type || !fromUid || !toUid) {
      return '-1';
    }
    // 判断通信是否有准备好
    if (!this.isCommReady()) {
      return '-1';
    }
    // 判断是否已经通话
    const temp = this.isAlreadyRejectForAnswer();
    if (temp) {
      // 关闭原来的通话
      const typeStr = temp.commType;
      this.hangUp(typeStr, toUid, temp.id);
      // 500ms 延迟是因为通信时间挂断可能并没有正真挂断
      await new Promise((resolve) => {
        setTimeout(() => {
          resolve(true);
        }, 500);
      });
    }
    const dId = storeCidIsdnRelation.getCidByIsdn(fromUid, type);
    voiceFunc.voiceAnswer(dId);
    return '0';
  },
  /**
   * 语音点呼
   * @param toUid 对端isdn
   */
  async audioCall(toUid: string, dialType: string): Promise<string> {
    // 判断通信是否有准备好
    if (!this.isCommReady(toUid)) {
      return '-1';
    }
    // 判断是否已经通话
    if (this.isAlreadyCommForSend('voice', toUid)) {
      return '-1';
    }

    const { updateComm } = useCommunicationStore();
    const status = messageUtil.generateStartComm(toUid);
    updateComm({
      isdn: toUid,
      status,
      type: 'voice',
    });
    // AUTO_ANSWER,记录仪自动接听开关 0关闭
    let answerMode: any = '1';
    if (appConfig.settingData.AUTO_ANSWER === '1') {
      const { data } = await queryEquipmentsByParam({ account: toUid });
      data.forEach((item) => {
        if (item.category === CategoryEnum.recorder) {
          answerMode = '0';
        }
      });
    }

    const res: any = await voiceFunc.voiceDial(toUid, dialType, answerMode);
    return res.rsp;
  },
  // 挂断、拒接、取消
  hangUp(type, toUid, fromUid) {
    if (!type || !fromUid || !toUid) {
      return '-1';
    }
    const { comm, deleteComm } = useCommunicationStore();
    const saveId = fromUid;
    const commStatus = comm[saveId];

    if (commStatus?.[type]) {
      const cid = storeCidIsdnRelation.getCidByIsdn(saveId, type);
      const status = commStatus[type].status.status;
      const {
        CALLING,
        CONNECTING,
        NO_ANSWER,
        NOT_FOUND,
        ORIGIN,
        PEER_REJECT,
        POWER_OFF,
        RINGING,
        WAITING_ANSWER,
      } = communicationStatus();

      switch (status) {
        case CALLING.status: {
          // 挂断
          console.log(saveId, '挂断---cid', cid);
          switch (type) {
            case 'halfdial': {
              voiceFunc.voiceCloseHalfDial(cid);
              break;
            }
            case 'monitor':
            case 'video': {
              videoFunc.videoRelease(cid);
              break;
            }
            case 'voice': {
              voiceFunc.voiceRelease(cid);
              break;
            }
            default: {
              break;
            }
          }

          break;
        }
        case CONNECTING.status:
        case NO_ANSWER.status:
        case NOT_FOUND.status:
        case ORIGIN.status:
        case PEER_REJECT.status:
        case POWER_OFF.status:
        case RINGING.status: {
          // 取消
          console.log(saveId, '取消---cid', cid);
          switch (type) {
            case 'halfdial': {
              voiceFunc.voiceCloseHalfDial(cid);
              break;
            }
            case 'hold':
            case 'voice': {
              voiceFunc.voiceRelease(cid);
              break;
            }
            case 'monitor':
            case 'video': {
              videoFunc.videoRelease(cid);
              break;
            }
            default: {
              break;
            }
          }

          break;
        }
        case WAITING_ANSWER.status: {
          // 拒接
          console.log(saveId, '拒接---cid', cid);
          const callInfo = {
            cid,
            fromUid,
            toUid,
            type,
          };
          switch (type) {
            case 'hold':
            case 'voice': {
              voiceFunc.voiceReject(callInfo);
              break;
            }
            case 'monitor':
            case 'video': {
              videoFunc.videoReject(callInfo);
              break;
            }
            default: {
              break;
            }
          }

          break;
        }
        // No default
      }
    }

    storeCidIsdnRelation.removeByIsdn(saveId, type);
    // 界面恢复初始状态
    deleteComm({
      isdn: saveId,
      status: {},
      type,
    });
  },
  // 挂断所有通信
  hangUpAll() {
    const { clearMonitorDrawerData } = useMonitorStore();
    clearMonitorDrawerData();

    const { comm } = useCommunicationStore();
    if (!comm) return;
    Object.keys(comm).forEach((key) => {
      if (key !== 'updateInfo') {
        const target = comm[key];
        const type = Object.keys(target)[0];
        this.hangUp(type, appConfig.isdn, key);
      }
    });
  },
  // 语音点呼或者视频点呼，是否有正在呼叫中的
  isAlreadyCalling() {
    const { t } = useI18n();
    const { comm } = useCommunicationStore();
    let isCalling = false;
    const keys = Object.keys(comm);
    const { RINGING } = communicationStatus();

    keys.forEach((key) => {
      if (key === 'updateInfo') return;
      const _comm = comm[key];
      const target = _comm.voice || _comm.video;
      if (target?.status.status === RINGING.status) {
        isCalling = true;
      }
    });

    if (isCalling) {
      Message(t('communication.communicationTips.pointCallWarnTwo'));
    }
    return isCalling;
  },
  isAlreadyCommForAnswer(type) {
    const { comm } = useCommunicationStore();
    const keys = Object.keys(comm);
    let isWorking;
    const { CALLING } = communicationStatus();

    keys.forEach((key) => {
      if (key === 'updateInfo') return;
      // 同一个CDC不能发起两路点呼(只能进行一路语音点呼或者一路视频点呼)
      if (['video', 'voice'].includes(type)) {
        if (key === 'updateInfo') {
          return;
        }
        const _comm = comm[key];
        const target = _comm.voice || _comm.video;
        if (target?.status.status === CALLING.status) {
          isWorking = {
            commType: target.commType,
            id: key,
          };
        }
      }
    });
    return isWorking;
  },
  /**
   * 是否已经有点呼
   * @param type
   * @param commId
   * @returns
   */
  isAlreadyCommForSend(type, commId) {
    if (type !== 'monitor' && this.isAlreadyCalling()) {
      return true;
    }

    const { t } = useI18n();
    const { comm } = useCommunicationStore();
    const _comm = comm[commId];
    let isAnswer = false;
    if (_comm) {
      // 同一设备是否已经正在进行相同操作
      if (_comm[type]) {
        isAnswer = true;
      }
      // 同一设备视频点呼和视频查看只能同时进行一种
      if (type === 'video' && _comm.monitor) {
        isAnswer = true;
      }
      if (type === 'monitor' && _comm.video) {
        isAnswer = true;
      }
    }
    if (this.isAlreadyCommForAnswer(type)) {
      isAnswer = true;
    }
    if (isAnswer) {
      Message(t('communication.communicationTips.pointCallWarnTwo'));
    }
    return isAnswer;
  },
  isAlreadyRejectForAnswer() {
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
  },
  // 判断通信是否可用
  isCommReady(toUid?: any) {
    const { t } = useI18n();
    const { hasComm } = useCommunicationStore();

    if (!appConfig.isdn || appConfig.isdn === 'null') {
      Message(t('communication.communicationTips.communicationAccountNotAssociated'));
      return false;
    }

    if (toUid !== undefined && !toUid) {
      Message(t('communication.communicationTips.communicationAccountNotAssociated'));
      return false;
    }

    if (!hasComm) {
      Message(t('communication.communicationTips.communicationServiceNotAvailable'));
      return false;
    }

    return true;
  },
  /**
   * 视频查看
   * @param toUid 对端isdn
   * @param type 0表示摄像头，1表示终端
   **/
  async monitorCall(opt: MonitorVideoOpt): Promise<string> {
    const { toUid, type } = opt;
    const { updateComm } = useCommunicationStore();

    if (!this.isCommReady(toUid)) {
      return '-1';
    }

    const status = messageUtil.generateStartComm(toUid);
    if (type === '0') {
      status.device = 'camera';
    }
    updateComm({
      isdn: toUid,
      status,
      type: 'monitor',
    });

    const res: any = await videoFunc.monitorVideo(opt);
    return res?.rsp;
  },
  // 发送彩信
  sendMMS(dest, content, attach, attachThumb?, audioContent?, audioInfo?) {
    // const { t } = useI18n();
    // if (content === '' && !attach) {
    //   Message(t('communication.communicationTips.msgNotEmpty'));
    //   return '-1';
    // }
    // 判断通信是否有准备好
    if (!this.isCommReady(dest)) {
      return '-1';
    }
    return smsFunc.sendDispMMS(
      dest,
      content,
      attach,
      attachThumb || '',
      audioContent || '',
      audioInfo || '',
    );
  },
  // 发送短信
  sendSMS(dest, content) {
    const { t } = useI18n();
    if (content === '') {
      Message(t('communication.communicationTips.msgNotEmpty'));
      return '-1';
    }
    // 判断通信是否有准备好
    if (!this.isCommReady(dest)) {
      return '-1';
    }
    return smsFunc.sendDispSMS(dest, content);
  },
  /**
   * 视频点呼
   * @param toUid 对端isdn
   * @param container 渲染元素
   */
  async videoCall(opt: { fromUid: string } & VideoDialOpt): Promise<string> {
    const { toUid } = opt;

    // 判断通信是否有准备好
    if (!this.isCommReady(toUid)) {
      return '-1';
    }

    // 判断是否已经通话
    if (this.isAlreadyCommForSend('video', toUid)) {
      return '-1';
    }

    if (this.isAlreadyCalling()) {
      return '-1';
    }

    const { updateComm } = useCommunicationStore();
    const status = messageUtil.generateStartComm(toUid);
    updateComm({
      isdn: toUid,
      status,
      type: 'video',
    });

    const res: any = await videoFunc.videoDial({
      container: opt.container,
      toUid,
    });
    return res.rsp;
  },
  /**
   * 半双工点呼
   * @param toUid
   */
  async voiceHalfCall(toUid): Promise<string> {
    // 判断通信是否有准备好
    if (!this.isCommReady(toUid)) {
      return '-1';
    }
    // 判断是否已经通话
    if (this.isAlreadyCommForSend('voice', toUid)) {
      return '-1';
    }

    const { updateComm } = useCommunicationStore();
    const status = messageUtil.generateStartComm(toUid);
    updateComm({
      isdn: toUid,
      status,
      type: 'halfdial',
    });
    const res: any = await voiceFunc.voiceHalfDial(toUid);
    return res.rsp;
  },
  // 保持当前通话并接听
  async voiceHold(type, fromUid, toUid) {
    if (!type || !fromUid || !toUid) {
      return '-1';
    }
    // 判断是否已经通话
    const temp = this.isAlreadyRejectForAnswer();
    if (temp) {
      // 保持原来的通话
      const typeStr = temp.commType;
      const holdDId = storeCidIsdnRelation.getCidByIsdn(temp.id, typeStr);
      voiceFunc.voiceHold(holdDId);
      // 500ms 延迟是因为通信时间保持可能并没有正真保持
      await new Promise((resolve) => {
        setTimeout(() => {
          resolve(true);
        }, 500);
      });
    }
    // 保持后与接入通话
    const dId = storeCidIsdnRelation.getCidByIsdn(fromUid, type);
    voiceFunc.voiceAnswer(dId);
    return '0';
  },
  // 取消保持
  async voiceUnhold(dId) {
    if (!dId) {
      return '-1';
    }
    const res = await voiceFunc.voiceUnhold(dId);
    return res.rsp;
  },
};
