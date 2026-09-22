import { mediaFunc } from '@/plugins/mspPlayer';
import { useCommunicationStore } from '@/store';

import { sdkCallbackPrint, triggerSDKMethods } from '../helper';

function mediaRegister() {
  const communicationStore = useCommunicationStore();

  init();

  function init() {
    // 查询扬声器和麦克风列表结果
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('查询扬声器和麦克风列表结果', data);
        handleMediaData(data);
      },
      eventName: 'OnGetSoundDeviceResult',
      eventType: 'MSPNotify',
    });

    // 查询扬声器音量列表结果
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('查询扬声器音量列表结果', data);
        handleMediaData(data);
      },
      eventName: 'OnGetVolumeResult',
      eventType: 'MSPNotify',
    });

    // 设置扬声器音量结果
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('设置扬声器音量结果', data);
        handleMediaData(data);
      },
      eventName: 'OnSetVolumeResult',
      eventType: 'MSPNotify',
    });

    // 静音扬声器结果
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('静音扬声器结果', data);
        handleMediaData(data);
      },
      eventName: 'OnMuteSpeakerResult',
      eventType: 'MSPNotify',
    });

    // 取消静音扬声器结果
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('取消静音扬声器结果', data);
        handleMediaData(data);
      },
      eventName: 'OnUnmuteSpeakerResult',
      eventType: 'MSPNotify',
    });

    // 静音麦克风结果
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('静音麦克风结果', data);
        handleMediaData(data);
      },
      eventName: 'OnMuteMicResult',
      eventType: 'MSPNotify',
    });

    // 取消静音麦克风结果
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('取消静音麦克风结果', data);
        handleMediaData(data);
      },
      eventName: 'OnUnmuteMicResult',
      eventType: 'MSPNotify',
    });

    getGlobalVoiceData();
  }

  function handleMediaData(message) {
    const { cid, Volume } = message.value;
    const { addCid2media, mediaData } = communicationStore;
    const data = mediaData[cid] || {
      muteMic: false,
      muteSpeaker: false,
      volume: 60,
    };
    switch (message.eventName) {
      case 'OnMuteMicResult': {
        // 组呼别人开始讲话通知
        if (data) {
          const media = {
            ...data,
            muteMic: true,
          };
          addCid2media({ id: cid, media });
        }
        break;
      }
      case 'OnMuteSpeakerResult': {
        if (data) {
          const media = {
            ...data,
            muteSpeaker: true,
            volume: 1,
          };
          addCid2media({ id: cid, media });
        }
        break;
      }
      case 'OnSetVolumeResult': {
        // 收到组呼请求
        if (data) {
          const media = {
            ...data,
            muteSpeaker: false,
            volume: Volume,
          };
          addCid2media({ id: cid, media });
        }
        break;
      }
      case 'OnUnmuteMicResult': {
        if (data) {
          const media = {
            ...data,
            muteMic: false,
          };
          addCid2media({ id: cid, media });
        }
        break;
      }
      case 'OnUnmuteSpeakerResult': {
        // 组呼空闲通知
        if (data) {
          const media = {
            ...data,
            muteSpeaker: false,
            volume: 50,
          };
          addCid2media({ id: cid, media });
        }
        break;
      }
      default: {
        console.log('error group message type!');
        break;
      }
    }
  }

  function getGlobalVoiceData() {
    // 获取本地扬声器和麦克风列表
    mediaFunc.getSoundDevice();

    // 查询扬声器音量
    mediaFunc.getVolume(-1);
  }
}

export default mediaRegister;
