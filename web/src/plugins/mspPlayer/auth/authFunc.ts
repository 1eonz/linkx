import { sdkCallbackPrint, triggerSDKMethods } from '@/plugins/mspPlayer/helper';

export const authFunc = {
  // 统一登录
  unifiedLogin({ password, user }) {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('统一登录', data);
      },
      force: 'true',
      password,
      user,
    };
    return triggerSDKMethods('auth', 'unifiedLogin', param);
  },
  // 统一登出
  unifiedLogout() {
    const param = {
      callback: (data) => {
        sdkCallbackPrint('统一登出', data);
      },
    };
    return triggerSDKMethods('auth', 'unifiedLogout', param);
  },
};
