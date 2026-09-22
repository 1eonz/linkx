import { userLogin, userLogout, userRefreshToken } from '@/api/pim';
import { Message } from '@/components/Message';
import { appConfig } from '@/config';
import { PIMHttpHeaders } from '@/enums/pim';
// import { usePIM } from '@/hooks';
import { usePIMStateWithOut } from '@/store';

/**
 * 刷新token，过期时间小于一分钟刷新
 * @param data
 */
const refreshPIMToken = (data) => {
  const { accessToken, refreshToken, refreshTokenExpireIn, userId } = data;
  localStorage.setItem(PIMHttpHeaders.USERID, userId);
  localStorage.setItem(PIMHttpHeaders.ACCESSTOKEN, accessToken);

  setTimeout(
    async () => {
      const res = await userRefreshToken(
        {
          grantType: 'refresh_token',
          refreshToken,
        },
        {
          Authorization: `Basic ${refreshToken}`,
          'X-User-Id': userId,
        },
      );
      if (res.code === 0) {
        refreshPIMToken(res.data);
      }
    },
    refreshTokenExpireIn - 60 * 1000,
  );
};

// 登录PIM
export const loginPIM = async () => {
  const username = appConfig.userData?.idCardNum;
  if (!username) {
    Message({
      duration: 3000,
      message: '获取身份证号失败，无法完成IM登录！',
      type: 'error',
    });
    return;
  }

  const { code, data } = await userLogin({
    deviceType: '3',
    forceLogin: 1, // 1为强制登录，0非强制
    grantType: 'code',
    identityType: '3',
    loginType: '1',
    scope: 'all',
    state: '',
    username,
  });

  if (code === 0) {
    refreshPIMToken(data);

    const { updateUser } = usePIMStateWithOut();
    updateUser(true);

    // usePIM().init(`${appConfig.settingData.IM_ADDRESS_WS}/websocket/v1/webmessage`);
  }
};

// 登出PIM
export const logoutPIM = async () => {
  await userLogout();
};
