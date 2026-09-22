import { showToast } from 'vant';

import { initData } from './x-utils/index.js';

import { login } from '@/common/api/user.js';
import { useUserStore } from '@/stores/user.js';

const _devLogin = (phone) => {
  login({
    phone,
    pwd: 123456,
  }).then((res) => {
    const { setToken, setUserInfo } = useUserStore();
    setToken(res.token);
    setUserInfo().then(() => {
      // uni.showToast({
      //     title: '开发账号登陆',
      //     icon: 'success'
      // })
      showToast('开发账号登陆');
    });
  });
};

// #ifdef H5
// 调用方式 window._devLogin
if (import.meta.env.MODE == 'development') {
  window.initData = initData;
  window._devLogin = (phone = 13315511111) => _devLogin(phone);
}

// #endif

// #ifdef MP
// 调用方式 wx._devLogin
if (uni.getSystemInfoSync().platform == 'devtools') {
  wx._devLogin = (phone = 13315511111) => _devLogin(phone);
}

// #endif
