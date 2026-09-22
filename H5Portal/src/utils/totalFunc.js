import { showToast } from 'vant';

import { heartbeatApi, heartbeatLocation } from '@/common/api/xietong';
import { getGlobalsConfigByKey } from '@/common/utils';
import { useApplicationStore } from '@/stores/application.js';
import { useCommunicationStore } from '@/stores/communication.js';

const communicationStore = useCommunicationStore();
let clearHeartTimerXietong = null; // 心跳定时器
let clearLocationTimer = null;
let updateLoactionTime = '';
let locationData = {};
let failTimesXietong = 0; // 检测协同岗心跳失败次数

// 位置上报默认间隔（秒）：当全局配置 LOCATION_REPORT_TIME 缺失或非法时兜底使用
const DEFAULT_LOCATION_REPORT_TIME = 30;

//协同岗的心跳检测
export async function heartbeatXietong() {
  await clearTimerXietong();

  clearHeartTimerXietong = setInterval(() => {
    testHeartXietong();
  }, 1000 * 10);
}

//位置信息的心跳检测

export async function heartbeatAddress() {
  await clearTimerLocation();
  updateLoactionTime = await getGlobalsConfigByKey('LOCATION_REPORT_TIME');

  // 兜底处理：接口返回为空或非正数时，使用默认间隔，避免 Number(undefined/''/'abc')*1000 → 0/NaN 导致 setInterval 失控疯狂上报
  const reportIntervalSeconds = Number(updateLoactionTime);
  const validInterval =
    Number.isFinite(reportIntervalSeconds) && reportIntervalSeconds > 0
      ? reportIntervalSeconds
      : DEFAULT_LOCATION_REPORT_TIME;

  clearLocationTimer = setInterval(
    () => {
      testLocationUpdate();
    },
    validInterval * 1000,
  );
}

export async function testLocationUpdate() {
  const { userInfo } = useApplicationStore();
  const locationMsg = await communicationStore.getGisInfo();
  const location = locationMsg ? locationMsg.longitude + ',' + locationMsg.latitude : '';
  console.log(locationMsg, '位置信息');
  locationData.userId = userInfo.userid;
  locationData.location = location;
  await heartbeatLocation(locationData);
}

export async function testHeartXietong() {
  const { userInfo } = useApplicationStore();
  try {
    const data = await heartbeatApi({ userId: userInfo?.userid });
    if (data) {
      failTimesXietong++;
    } else {
      failTimesXietong = 0;
    }
  } catch {
    failTimesXietong++;
  }

  if (failTimesXietong >= 3) {
    clearTimerXietong?.();
    await MessageBox({
      isLight: true,
      offset: ['40%', '35%'],
      text: '网络异常，功能不可用，请在网络恢复后重新登录',
      type: 'ok',
    });
  }
}

// 去掉心跳的定时位置上传
export function clearTimerLocation() {
  if (clearLocationTimer) {
    clearInterval(clearLocationTimer);
    clearLocationTimer = null;
  }
}

// 去掉心跳的定时任务协同岗
export function clearTimerXietong() {
  failTimesXietong = 0;
  if (clearHeartTimerXietong) {
    clearInterval(clearHeartTimerXietong);
    clearHeartTimerXietong = null;
  }
}

const judgeLicenseExpire = async (status, expireDate) => {
  const now = new Date();
  const expireDateObj = new Date(expireDate);
  const diffTime = expireDateObj.getTime() - now.getTime();
  const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
  const MSIP_LICENSE_EXPIRED_TIME = await getGlobalsConfigByKey('MSIP_LICENSE_EXPIRED_TIME');
  console.log(MSIP_LICENSE_EXPIRED_TIME, '====MSIP_LICENSE_EXPIRED_TIME');

  if (!MSIP_LICENSE_EXPIRED_TIME) return;
  if (diffDays >= Number(MSIP_LICENSE_EXPIRED_TIME)) return;
  // const title = '您的账户还有' + diffDays + '天到期，请及时联系相关人员进行续费，以免影响您的业务';
  // showToast(title);
};
export function checkLicenseStatus(licensePermissionsRes) {
  // 暂时不需要提示，过期直接系统受限就行
  return
  const { status, expireDate } = licensePermissionsRes;
  // 证书状态：0 未激活，1 激活，2 即将过期，3 已过期，4 失效可试用，5 失效
  let title = '';
  if (status === '0') {
    // title = 'license未激活';
    // showToast(title);
  } else if (status === '3' || status === '5') {
    // title = 'license已过期，请重新导入';
    // showToast(title);
  } else if (status === '2') {
    judgeLicenseExpire(status, expireDate);
  }
}

export function filterMonitor(list, license) {
  // 过滤
  const validList = list.filter(Boolean);
  //按name字段去重
  const uniqueList = [];
  const nameSet = new Set();
  for (const item of validList) {
    if (item?.name && !nameSet.has(item.name)) {
      nameSet.add(item.name);
      uniqueList.push(item);
    }
  }
  // 权限
  const { LINKXBS, LINKXCCF } = license;
  let filteredAppList = uniqueList;
  if (LINKXBS !== '1' || LINKXCCF !== '1') {
    filteredAppList = uniqueList.filter((item) => item.name !== '设备调度');
  }
  return filteredAppList;
}
