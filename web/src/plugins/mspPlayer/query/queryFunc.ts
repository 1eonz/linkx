import { sdkCallbackPrint, triggerSDKMethods } from '@/plugins/mspPlayer/helper';

export const queryFunc = {
  // 查询解码器
  queryDecoder() {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询解码器', data);
      },
    };
    return triggerSDKMethods('query', 'queryDecoder', param);
  },
  // 查询终端历史轨迹
  queryGISTrack(data) {
    const param = {
      begintime: data.begintime,
      callback(data) {
        sdkCallbackPrint('查询终端历史轨迹', data);
      },
      endtime: data.endtime,
      limit: '1000',
      offset: data.offset,
      ueid: data.ueid,
    };
    return triggerSDKMethods('query', 'queryGISTrack', param);
  },
  // 查询MRS
  queryMRS() {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询MRS', data.rsp);
      },
    };
    return triggerSDKMethods('query', 'queryMRS', param);
  },
  // 查询用户属性
  queryUserInfo(isdn) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询用户属性', data);
      },
      isdn,
    };
    return triggerSDKMethods('query', 'queryUserInfo', param);
  },
  // 查询所有用户
  queryUserListV1(offset = 0) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('查询所有用户', data.rsp);
      },
      limit: 1000,
      offset,
    };
    return triggerSDKMethods('query', 'queryUserListV1', param);
  },
};
