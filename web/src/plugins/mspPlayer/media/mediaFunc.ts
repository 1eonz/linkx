import { useCommunicationStoreWithOut } from '@/store';

import { sdkCallbackPrint, triggerSDKMethods } from '../helper';

export const mediaFunc = {
  // 查询扬声器和麦克风列表
  getSoundDevice() {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询扬声器和麦克风列表', data);
      },
    };
    return triggerSDKMethods('device', 'getSoundDevice', param);
  },
  // 查询扬声器音量
  getVolume(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询扬声器音量', data);
      },
      cid,
    };
    return triggerSDKMethods('device', 'getVolume', param);
  },
  // 静音麦克风
  muteMic(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('静音麦克风', data);
      },
      cid: grpid,
    };
    return triggerSDKMethods('device', 'muteMic', param);
  },
  // 静音扬声器
  muteSpeaker(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('静音扬声器', data);
      },
      cid,
    };
    return triggerSDKMethods('device', 'muteSpeaker', param);
  },
  // 查询本地摄像头能力
  queryCameraAbility() {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询本地摄像头能力', data);
      },
    };
    return triggerSDKMethods('device', 'queryCameraAbility', param);
  },
  setGroupVolume(grpid, volume) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('设置群组扬声器音量', data);
      },
      grpid,
      volume,
    };
    return triggerSDKMethods('device', 'setGroupVolume', param);
  },
  // 设置扬声器音量
  setVolume(cid, volume) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('设置扬声器音量', data);
        if (data.rsp === '0') {
          const { addCid2media, mediaData } = useCommunicationStoreWithOut();
          const data = mediaData[cid] || {};
          data.volume = volume;
          addCid2media({
            id: cid,
            media: {
              ...data,
              muteSpeaker: volume <= 1,
            },
          });
        }
      },
      cid,
      volume,
    };
    return triggerSDKMethods('device', 'setVolume', param);
  },
  // 停止播放放音
  stopPlayTone() {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('停止播放放音', data);
      },
    };
    return triggerSDKMethods('device', 'stopPlayTone', param);
  },
  // 取消静音麦克风
  unmuteMic(grpid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('取消静音麦克风', data);
      },
      cid: grpid,
    };
    return triggerSDKMethods('device', 'unmuteMic', param);
  },
  // 取消静音扬声器
  unmuteSpeaker(cid) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('取消静音扬声器', data);
      },
      cid,
    };
    return triggerSDKMethods('device', 'unmuteSpeaker', param);
  },
};
