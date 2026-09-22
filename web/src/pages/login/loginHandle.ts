import { keepalive, refreshDemsToken, refreshToken } from '@/api/login';
import { heartbeatApi } from '@/api/xietong';
// import { Message } from '@/components/Message';
import MessageBox from '@/components/MessageBox';
import { useEmitter, useSetInterval } from '@/hooks';
// import { authFunc } from '@/plugins/mspPlayer';
import commOpt from '@/plugins/mspPlayer/commOpt';
import { useCommunicationStore, useMainStore, usePIMStore } from '@/store';
// import { getIp } from '@/utils';
import {
  getRefreshToken,
  LOGIN,
  // removeRefreshToken,
  // removeToken,
  setIsLockScreen,
  setRefreshToken,
  setToken,
} from '@/utils/auth';
import { loginICS } from '@/utils/loginIcs';

// TODO：异步加载问题 不能及时获取到appConfig先写死
const updatedTokenParams = {
  clientId: LOGIN.clientId,
  clientSecret: LOGIN.clientSecret,
  grantType: 'refresh_token',
  refreshToken: getRefreshToken(),
};
let clearHeartTimer: any = null; // 心跳定时器
let timerList: Array<any> = []; // 心跳定时器数组
let failTimes = 0; // 检测心跳失败次数
let clearHeartTimerXietong: any = null; // 心跳定时器
let failTimesXietong = 0; // 检测心跳失败次数
let clearTokenTimer: any = null;

// 重新刷新token
export async function refreshTokenMethod(text: string) {
  const { code, data } = await refreshToken(updatedTokenParams);
  if (code === 0) {
    setToken(data.accessToken);
    setRefreshToken(data.refreshToken);
  } else {
    invalidLoginOut(text);
  }
}

// 定时器 间隔30秒检测dems token是否失效
export function demsTokenInvalid(text: string) {
  const token = localStorage.getItem('demsLoginToken');
  const param = { ...updatedTokenParams, token };
  const demsTimingToken = async () => {
    const { data } = await refreshDemsToken(param);
    // data.isInvalid为1时dems token失效
    if (!data || data.isInvalid === 1) {
      console.log('dems token失效', data.isInvalid);
      clearTokenTimer?.();
      invalidLoginOut(text);
    }
  };
  clearTokenTimer = useSetInterval(demsTimingToken, 1 * 1000 * 30);
}

// 其他地方有人登录
export async function kickoutMethod(text: string) {
  const config = { offset: ['40%', '35%'], text, type: 'ok' };
  const res = await MessageBox(config);
  if (res) {
    loginOutMethod();
    useEmitter().emit('getReloginClickCancelState', false); // 点击确定
  } else {
    // 点击取消
    useEmitter().emit('getReloginClickCancelState', true);
  }
}
// 协同岗的心跳检测
export async function heartbeatXietong() {
  clearTimerXietong();

  const heartbeatInterval = 1000 * 10; // 10秒
  let heartbeatTimer: any = null;
  let lastHeartbeatTime = Date.now();
  let isRunning = true;

  const performHeartbeat = async () => {
    if (!isRunning) return;

    const now = Date.now();
    const timeSinceLastHeartbeat = now - lastHeartbeatTime;

    // 如果距离上次心跳超过10秒，执行心跳
    if (timeSinceLastHeartbeat >= heartbeatInterval) {
      await testHeartXietong();
      lastHeartbeatTime = now;
    }

    // 计算下次检查的时间
    const nextCheckDelay = Math.max(1000, heartbeatInterval - timeSinceLastHeartbeat);

    // 设置下次检查
    heartbeatTimer = setTimeout(() => {
      performHeartbeat();
    }, nextCheckDelay);
  };

  // 立即执行一次心跳
  await testHeartXietong();
  lastHeartbeatTime = Date.now();

  // 开始心跳循环
  heartbeatTimer = setTimeout(() => {
    performHeartbeat();
  }, heartbeatInterval);

  // 保存清除函数
  clearHeartTimerXietong = () => {
    isRunning = false;
    if (heartbeatTimer) {
      clearTimeout(heartbeatTimer);
      heartbeatTimer = null;
    }
  };
}
export async function testHeartXietong() {
  const { user } = usePIMStore();
  const { deleteAllComm } = useCommunicationStore();
  if (!user?.id) return;
  try {
    const { code } = await heartbeatApi({ userId: user?.id });
    if (code === 0) {
      failTimesXietong = 0;
    } else {
      failTimesXietong++;
    }
  } catch {
    failTimesXietong++;
  }

  if (failTimesXietong >= 3) {
    clearTimerXietong?.();
    // await MessageBox({
    //   isLight: true,
    //   offset: ['40%', '35%'],
    //   text: '网络异常，功能不可用',
    //   type: 'ok',
    // });

    // 挂断通信、关闭视频弹窗
    commOpt.hangUpAll();
    deleteAllComm();
    loginOutMethod();
  }
}

// 心跳检测
export function heartTest() {
  clearHeartTimer = useSetInterval(() => {
    testHeartTimes();
    timerList.push(clearHeartTimer);
  }, 1000 * 5);
}

// 心跳检测，连续3次调用失败，则认为客户端断线
export async function testHeartTimes() {
  const { setServerTime } = useMainStore();
  const { deleteAllComm } = useCommunicationStore();

  try {
    const { code, data } = await keepalive();
    if (code === 0) {
      failTimes = 0;
      setServerTime(data.time);
    } else if (code === 117 || code === 118) {
      // 心跳检测返回117或者118 则token失效 则提示重新登录
      console.error('Heartbeat test 117||118');
      // kickoutMethod(t('login.logon.refreshTokenExpired'))
    } else {
      console.error('Heartbeat test fail');
      failTimes++;
    }
  } catch {
    failTimes++;
  }

  if (failTimes >= 3) {
    // clearTimer?.();
    // await MessageBox({
    //   isLight: true,
    //   offset: ['40%', '35%'],
    //   text: '网络异常，功能不可用',
    //   type: 'ok',
    // });

    // 挂断通信、关闭视频弹窗
    commOpt.hangUpAll();
    deleteAllComm();
    loginOutMethod();
  }
}

// refreshtoken过期，清空缓存token，并退出重新登录
export async function invalidLoginOut(text: string) {
  console.log(text);

  // Message({
  //   duration: 5000,
  //   message: t(text),
  //   onClose: () => {
  //     loginOutMethod();
  //   },
  //   type: 'error',
  // });
}

// 去掉心跳的定时任务协同岗
export function clearTimerXietong() {
  failTimesXietong = 0;
  if (clearHeartTimerXietong) {
    clearHeartTimerXietong();
    clearHeartTimerXietong = null;
  }
}
// 去掉心跳的定时任务
export function clearTimer() {
  failTimes = 0;
  if (clearHeartTimer) {
    console.log('Clear heartbeat interval tasks');
    timerList.forEach((clear) => {
      clear?.();
    });
    timerList = [];
    clearHeartTimer = null;
  }
}

// 登出
export async function loginOut() {
  const { user } = usePIMStore();
  // 角色权限变更，重新走登录流程
  loginICS(user);
}

// 调用登出接口
export async function loginOutMethod() {
  // 清除定时任务
  clearTimer?.();
  clearTimerXietong?.();
  // await uniLogout();
  await loginOut();
}

// 长时间未操作 或者用户离线
export function lockScreenMethod() {
  // 用户10分钟内未操作，页面出提示然后退出登录
  setIsLockScreen(true);
  // 调用登出接口
  loginOutMethod();
}
