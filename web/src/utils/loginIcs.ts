import { getGlobalsList } from '@/api/dictionary';
import { newTokenLogin } from '@/api/login';
import { getUserInfo } from '@/bridge/post.js';
import { appConfig } from '@/config';
import { initGlobalData } from '@/data';
import { themeService } from '@/data/useTheme';
import { useEmitter } from '@/hooks';
import { heartTest } from '@/pages/login/loginHandle';
import { usePIMStore, useRouterStore } from '@/store';
import { getDeviceId, LOGIN, setRefreshToken, setToken } from '@/utils/auth';
// import { httpAiService } from '@/utils/httpAi';
import { queryRoleDepartMentTreebyUserId } from '@/api/statics';
// import { addTasks, processTasks } from '@/pages/coordination/common';

// declare const PloLink: any;

// const plolink: any = new PloLink({
//   baseURL: 'https://10.28.15.61:30843', // 网关/服务地址
//   // baseURL: '', // 网关/服务地址
// });

// 获取im用户信息
export async function getImUser() {
  const PIMStore = usePIMStore();
  // openChat({ groupId: '26589782414850' });
  const res = await getUserInfo();
  console.log('🚀 ~ onVisibleChange getUserInfo ~', res);
  console.log(res, '==========IM用户信息========');
  if (res) {
    PIMStore.setUserMyInfo(res);
    loginICS(res);
  }
}

// 登录ICS服务
export async function loginICS(data) {
  const PIMStore = usePIMStore();
  const routerStore = useRouterStore();
  const res: any = await newTokenLogin({
    clientId: LOGIN.clientId,
    deviceId: getDeviceId(),
    token: data.aastoken || '',
  });
  console.log(res, '==========ICS登录信息======');
  if (res.code === 0 && res.data) {
    PIMStore.setSystemLimit(false);
    setToken(res.data.accessToken);
    setRefreshToken(res.data.refreshToken);
    PIMStore.setUserIcs(res.data);
    getAppConfig();
    // 心跳检测
    heartTest();
    getUserRoleAuth(data.userid);
    // 获取权限信息
    routerStore.getMenu();
    // 修改权限重新登录时重新获取roleAuth
    useEmitter().emit('getRoleAuthMitt');
  } else if (res.code === 182) {
    // 系统功能受限
    PIMStore.setSystemLimit(true);
  }
}

// 获取appConfig信息
async function getAppConfig() {
  const { code, data } = await getGlobalsList();
  if (code === 0) {
    appConfig.settingData = data;
    // 通知业务页面（如统计页）刷新基于 appConfig 的定时器等配置
    useEmitter().emit('APP_CONFIG_UPDATED', data);
    // 连接websocket
    initGlobalData();
    // 配置加载完成后，重新初始化 httpAiService
    // try {
    //   httpAiService.reinitialize();
    // } catch (error) {
    //   console.warn('AI 服务重新初始化失败:', error);
    // }
  }
}

// 获取用户菜单栏权限信息（协同群组，协同处置，协同统计）
async function getUserRoleAuth(userid) {
  const PIMStore = usePIMStore();
  const { code, data } = await queryRoleDepartMentTreebyUserId({
    userId: userid,
  });
  if (code === 0 && data) {
    PIMStore.setUserRoleAuth(data);
  }
}

// 接收到我被@的消息，处理新建任务
export async function reciveAtMe(data) {
  console.log(data, '==========接收到我被@的消息==========');

  // const { user } = usePIMStore();
  // // 使用空字符串让代理处理，或者使用环境变量中的baseURL
  // const baseURL = ''; // 或者 import.meta.env.VITE_BASE_API
  // await plolink.addTasks({ user, message: data, baseURL });
  // addTasks(data);
}

// 接收我发出的消息，做任务回复处理
export async function reciveMyMsg(data) {
  console.log(data);
  // const { user } = usePIMStore();
  // const baseURL = '';
  // await plolink.processTasks({ user, message: data, baseURL });
  // processTasks(data);
}

export async function plolinkStatic() {
  // 监听待办更新
  // window.addEventListener('linkXDataUpdate', (e) => {
  //   console.log('主项目收到待办更新', e.detail);
  // });
  // const { user } = usePIMStore();
  // window.LinkXEvents.startPushing(user);
}

// 主题切换
export async function changeTheme(data) {
  if (!data) return;

  // WebView2 新版可能返回颜色值（例如 "#152584FF"），这里统一映射到 light/dark
  let theme = data.theme || data.data.theme;
  console.log(data, '=======主题切换颜色======---data');
  if (typeof theme === 'string') {
    const t = theme.trim().toLowerCase();
    if (t === 'ligth') theme = 'light';
  }

  themeService.setTheme(theme);
  console.log({ ...data, theme }, '=======主题切换颜色======');
}
