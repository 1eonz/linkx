import { Message } from '@/components/Message';
import { useI18n } from '@/hooks';
import { queryFunc } from '@/plugins/mspPlayer';
import commOpt from '@/plugins/mspPlayer/commOpt';
import { sdkCallbackPrint, triggerSDKMethods } from '@/plugins/mspPlayer/helper';
import { useConferenceStoreWithOut } from '@/store';
import { delay } from '@/utils';

import { setQueryConfListByAttendeeCallback } from './confRegister';

// 设置是否是摄像头用户
async function getIsCamera(data) {
  const ret: any = [];
  for (const item of data) {
    const res = await queryFunc.queryUserInfo(item.number);
    ret.push({ ...item, isCamera: String(res?.value?.category === '1') });
  }
  return ret;
}

// 获取会议窗口宽高
function getConferenceDomWH() {
  const el: any = document.querySelector('#conferenceVideoContainer');
  return {
    height: el?.offsetHeight,
    width: el?.offsetWidth,
  };
}

export const confFunc = {
  // 音频被动入会
  acceptAudioConf(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('音频被动入会', data);
      },
      cid,
    };
    return triggerSDKMethods('conf', 'acceptAudioConf', param);
  },
  // 视频被动入会
  acceptVideoConf(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('视频被动入会', data);
      },
      cid,
      windowInfo: {
        buttonIDs: 0,
        mode: 'wssflow',
        posX: 400,
        posY: 400,
        showToolbar: 1,
        ...getConferenceDomWH(),
      },
    };
    return triggerSDKMethods('conf', 'acceptVideoConf', param);
  },
  // 增加与会成员
  async addConfMembers(confId, memberInfos) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('增加与会成员', data);
      },
      confId,
      memberInfos: await getIsCamera(memberInfos),
    };
    return triggerSDKMethods('conf', 'addConfMembers', param);
  },
  // 广播与会者
  broadcastConfMember(confId, number) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('广播与会者', data);
      },
      confId,
      number,
    };
    return triggerSDKMethods('conf', 'broadcastConfMember', param);
  },
  // 广播多画面
  broadcastMixPicture(confId, memberInfos, flexType) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('广播多画面', data);
      },
      confId,
      memberInfos,
      mixPictureType: flexType,
    };
    return triggerSDKMethods('conf', 'broadcastMixPicture', param);
  },
  // 重呼成员
  async callConfMember(confId, memberInfo) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('重呼成员', data);
      },
      confId,
      memberInfo: await getIsCamera([memberInfo])[0],
    };
    return triggerSDKMethods('conf', 'callConfMember', param);
  },
  // 取消与会者
  cancelBroadcastConfMember(confId, number) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('取消与会者', data);
      },
      confId,
      number,
    };
    return triggerSDKMethods('conf', 'cancelBroadcastConfMember', param);
  },
  // 发起音视频会议
  async createConf(memberInfos, isVideo) {
    // 出现绿屏原因：sharpType的宽高要和以下的宽高一致
    const isCommReady = commOpt.isCommReady();
    const { t } = useI18n();

    if (!isCommReady) {
      return;
    }

    const param = {
      callback: (data) => {
        sdkCallbackPrint('发起音视频会议', data);
        if (data?.rsp === '-6') {
          Message({
            message: t('communication.communicationTips.communicationError'),
            type: 'error',
          });
        }
      },
      isVideo,
      memberInfos: await getIsCamera(memberInfos),
      windowInfo: {
        buttonIDs: 0,
        mode: 'wssflow',
        posX: 400,
        posY: 400,
        showToolbar: 1,
        ...getConferenceDomWH(),
      },
    };
    return triggerSDKMethods('conf', 'createConf', param);
  },
  // 删除成员
  delConfMember(confId, memberInfo) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('删除成员', data);
      },
      confId,
      memberInfo,
    };
    return triggerSDKMethods('conf', 'delConfMember', param);
  },
  // 结束会议
  endConf(confId) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('结束会议', data);
      },
      confId,
    };
    return triggerSDKMethods('conf', 'endConf', param);
  },
  // 离开音频会议
  exitAudioConf(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('离开音频会议', data);
        useConferenceStoreWithOut().clearConferData();
      },
      cid,
    };
    return triggerSDKMethods('conf', 'exitAudioConf', param);
  },
  // 离开视频会议
  exitVideoConf(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('离开视频会议', data);
        useConferenceStoreWithOut().clearConferData();
      },
      cid,
    };
    return triggerSDKMethods('conf', 'exitVideoConf', param);
  },
  // 挂断成员
  hangupConfMember(confId, memberInfo) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('挂断成员', data);
      },
      confId,
      memberInfo,
    };
    return triggerSDKMethods('conf', 'hangupConfMember', param);
  },
  // 主动加入音视频会议
  joinConf(confId, passcode, unifiedAccessCode, isVideo) {
    const { t } = useI18n();
    const isCommReady = commOpt.isCommReady();
    if (!isCommReady) {
      return;
    }

    const param = {
      callback: (data) => {
        sdkCallbackPrint('主动加入音视频会议', data);
        if (data.rsp !== '0' && data.rsp !== '-2') {
          Message({
            message: t('videoConference.statusMsg.joinFailure'),
            type: 'error',
          });
        } else if (data.rsp === '-2') {
          Message({
            message: t('videoConference.statusMsg.errorID'),
            type: 'error',
          });
        }
      },
      confId,
      fmt: '1080P',
      isVideo,
      passcode,
      unifiedAccessCode,
      windowInfo: {
        buttonIDs: 0,
        mode: 'wssflow',
        posX: 400,
        posY: 400,
        showToolbar: 1,
        ...getConferenceDomWH(),
      },
    };
    return triggerSDKMethods('conf', 'joinConf', param);
  },
  // 静音会议
  muteConf(confId, isMute) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('静音会议', data);
      },
      confId,
      isMute,
    };
    return triggerSDKMethods('conf', 'muteConf', param);
  },
  // 静音会议成员
  muteConfMember(confId, isMute, number) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('静音会议成员', data);
      },
      confId,
      isMute,
      number,
    };
    return triggerSDKMethods('conf', 'muteConfMember', param);
  },
  // 申请话权代理
  proxyFloor(confId, member, operation) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('申请话权代理', data);
      },
      confId, // 会议id
      member, // 群组号码
      operation, // 操作：0表示抢权， 1表示放权
    };
    return triggerSDKMethods('conf', 'proxyFloor', param);
  },
  // 查看调度员所在的会议信息
  queryConfListByAttendee(): Promise<any> {
    return new Promise((resolve) => {
      const param = {
        callback: (data) => {
          sdkCallbackPrint('查看调度员所在的会议信息', data);
          setQueryConfListByAttendeeCallback((d) => {
            resolve(d);
          });
        },
      };
      triggerSDKMethods('conf', 'queryConfListByAttendee', param);
    });
  },
  // 查询多画面信息
  queryContinuousPresenceInfo(confId) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询多画面信息', data);
      },
      confId,
    };
    return triggerSDKMethods('conf', 'queryContinuousPresenceInfo', param);
  },
  // 音频拒接入会
  rejectAudioConf(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('音频拒接入会', data);
        useConferenceStoreWithOut().clearConferData();
      },
      cid,
    };
    return triggerSDKMethods('conf', 'rejectAudioConf', param);
  },
  // 视频拒接入会
  rejectVideoConf(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('视频拒接入会', data);
        useConferenceStoreWithOut().clearConferData();
      },
      cid,
    };
    return triggerSDKMethods('conf', 'rejectVideoConf', param);
  },
  // 设置主席名字
  setChairmanName(name) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('设置主席名字', data);
      },
      name,
    };
    return triggerSDKMethods('conf', 'setChairmanName', param);
  },
  // 选看与会者
  watchConfMember(confId, number) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('选看与会者', data);
      },
      confId,
      number,
    };
    return triggerSDKMethods('conf', 'watchConfMember', param);
  },
  // 选看多画面
  watchMixPicture(confId, memberInfos, flexType) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('选看多画面', data);
      },
      confId,
      memberInfos,
      mixPictureType: flexType,
    };
    return triggerSDKMethods('conf', 'watchMixPicture', param);
  },
};

// 异常情况导致上次的会议未关闭
export async function makeSureEndConf() {
  // 如果还在会议里但是查不出来，就把错误抛给SDK侧解决
  const res = await confFunc.queryConfListByAttendee();
  const arr = res.value?.confInfos || [];
  if (arr.length > 0) {
    for (const { confId } of arr) {
      await confFunc.endConf(confId);
    }
    await delay(500);
  }
}
