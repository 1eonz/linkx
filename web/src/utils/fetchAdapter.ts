import { useSetInterval } from '@/hooks';

import appConfig from '../config/appConfig';
import { mapConfig } from '../config/index';
import { headerUtils } from './headerUtils';

function promise() {
  return new Promise((resolve) => {
    if (mapConfig.tokenRenovate) {
      resolve(true);
      return;
    }
    const clearTimer = useSetInterval(() => {
      if (mapConfig.tokenRenovate) {
        clearTimer?.();
        resolve(true);
      }
    }, 1000);
  });
}

export default async function fetchAdapter(
  url = '',
  data = {},
  token,
  type = 'GET',
  method = 'fetch',
  timeout = 0,
) {
  await promise();
  type = type.toUpperCase();
  if (type === 'GET') {
    let dataStr = ''; // 数据拼接字符串
    Object.keys(data).forEach((key) => {
      dataStr += `${key}=${data[key]}&`;
    });
    if (dataStr !== '') {
      dataStr = dataStr.slice(0, Math.max(0, dataStr.lastIndexOf('&')));
      url = `${url}?${dataStr}`;
    }
  }
  if (fetch && method === 'fetch') {
    const headers = {
      Accept: 'application/json;charset=UTF-8',
      'Content-Type': 'application/json',
      'TD-CloudCmd-Token': token,
      'X-Cloud-AgroupId': appConfig.actionId,
      'X-Cloud-FromContainerId': '',
      'X-Cloud-ResourceId': appConfig.resourceId,
      'X-Cloud-ResourceType': '',
      'X-Cloud-RoleId': appConfig.roleId,
      'X-CloudCmd-AppKey': mapConfig.ApiKey,
    };
    headers['X-CloudCmd-Signature'] = headerUtils?.getSignature(url, data, headers, type);
    const requestConfig: any = {
      cache: 'force-cache',
      dataType: 'json',
      headers,
      method: type,
      mode: 'cors',
    };

    if (type === 'POST') {
      Object.defineProperty(requestConfig, 'body', {
        value: JSON.stringify(data),
      });
    }

    let response;
    try {
      response = await fetch(url, requestConfig);
      const responseJson = await response.json();
      return responseJson;
    } catch {
      console.log(`请求错误:url:${url}${data}`);
      return {
        code: -100,
        msg: 'connect error',
      };
    }
  } else {
    return new Promise((resolve) => {
      const requestObj = new XMLHttpRequest();
      let sendData = '';
      let timeoutFlag = false; // 是否超时
      let timer: Timeout;

      if (type === 'POST') {
        sendData = JSON.stringify(data);
      }

      if (timeout > 0) {
        timer = setTimeout(() => {
          timeoutFlag = true;
          requestObj.abort(); // 请求中止
        }, timeout);
      }
      const ajaxHeaders = {
        Accept: 'application/json;charset=UTF-8',
        'Content-Type': 'application/json',
        'TD-CloudCmd-Token': token,
        'X-CloudCmd-AppKey': mapConfig.ApiKey,
      };
      requestObj.open(type, url, true);
      requestObj.setRequestHeader('Content-type', 'application/json');
      requestObj.setRequestHeader('Accept', 'application/json;charset=UTF-8');
      requestObj.setRequestHeader('TD-CloudCmd-Token', token);
      requestObj.setRequestHeader('X-CloudCmd-AppKey', mapConfig.ApiKey);
      requestObj.setRequestHeader(
        'X-CloudCmd-Signature',
        headerUtils.getSignature(url, data, ajaxHeaders, type),
      );
      requestObj.send(sendData);
      requestObj.addEventListener('readystatechange', () => {
        if (requestObj.readyState === 4) {
          if (timeout > 0) {
            if (timeoutFlag) {
              resolve({ code: -100, data: '', msg: '请求超时' });
            }
            clearTimeout(timer);
          }
          if (requestObj.status === 200) {
            let obj = requestObj.response;
            if (typeof obj !== 'object') {
              obj = JSON.parse(obj);
            }
            resolve(obj);
          } else {
            resolve({
              code: -100,
              msg: 'connect error',
            });
          }
        }
      });
    });
  }
}
