import { sdkCallbackPrint, triggerSDKMethods } from '../helper';

function queryRegister() {
  init();

  function init() {
    // 终端gis历史轨迹事件
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('查询扬声器和麦克风列表结果', data);
      },
      eventName: 'OnRecvGISTrackNotify',
      eventType: 'GisNotify',
    });
  }
}

export default queryRegister;
