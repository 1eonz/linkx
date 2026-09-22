import { PIMHttpHeaders } from '@/enums/pim';
import http from '@/utils/http';

import { v4 as uuidv4 } from 'uuid';

const baseToken =
  'UElNMi1ERVNLVE9QLTEwMDpQQktERjJXaXRoSG1hY1NIQTI1Nl9BU0k3Q1hQMGl4b0xtVTFHOHVNbHRnPT1fMTAwMDBfRTRpazBVdXFSTHl1V3hla0ZPN01Ua3JwdDBSdFZ4RDJybVF2dVNxd2dBYz0=';

// 设备Id
function getDeviceId() {
  let deviceId = localStorage.getItem(PIMHttpHeaders.COMMID);
  if (!deviceId) {
    deviceId = uuidv4();
    localStorage.setItem(PIMHttpHeaders.COMMID, deviceId);
  }
  return deviceId;
}

export const getConfig = (isLogin?): any => {
  const Authorization = isLogin
    ? `Basic ${baseToken}`
    : localStorage.getItem(PIMHttpHeaders.ACCESSTOKEN);
  return {
    baseURL: '/pim',
    headers: {
      Authorization,
      'X-Comm-Id': getDeviceId(),
      'X-User-Id': localStorage.getItem(PIMHttpHeaders.USERID) || '123',
    },
  };
};

const request = (type) => {
  return (...arg) => {
    if (['delete', 'get'].includes(type)) {
      const [url, opt = {}] = arg;
      return http[type](url, {
        ...getConfig(),
        ...opt,
      });
    } else {
      const [url, data, opt = {}] = arg;
      return http[type](url, data, {
        ...getConfig(url.includes('user/login')),
        ...opt,
      });
    }
  };
};

export default {
  delete: request('delete'),
  get: request('get'),
  post: request('post'),
  put: request('put'),
};
