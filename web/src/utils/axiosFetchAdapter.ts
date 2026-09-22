import axios, { AxiosError, AxiosRequestConfig } from 'axios';

import appConfig from '../config/appConfig';
import { mapConfig } from '../config/index';
import { useSetInterval } from '@/hooks';
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

/**
 * 构建查询字符串（保持与原 fetchAdapter 一致的不编码行为）
 */
function buildQueryString(data: Record<string, any>): string {
  let dataStr = '';
  Object.keys(data).forEach((key) => {
    dataStr += `${key}=${data[key]}&`;
  });
  if (dataStr !== '') {
    dataStr = dataStr.slice(0, Math.max(0, dataStr.lastIndexOf('&')));
  }
  return dataStr;
}

/**
 * 构建请求头（与原 fetchAdapter 保持一致）
 */
function buildHeaders(token: string, url: string, data: any, method: string) {
  const headers: Record<string, string> = {
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
  headers['X-CloudCmd-Signature'] = headerUtils?.getSignature(url, data, headers, method);
  return headers;
}

interface FetchResult<T = any> {
  code: number;
  msg: string;
  data?: T;
}

/**
 * axios 版本的 fetchAdapter
 * 保持与原 fetchAdapter 完全一致的函数签名和行为
 */
export default async function axiosFetchAdapter<T = any>(
  url = '',
  data: Record<string, any> = {},
  token?: string,
  type: 'GET' | 'POST' = 'GET',
  method = 'fetch',
  timeout = 0,
): Promise<FetchResult<T>> {
  // 等待 token 刷新完成
  await promise();
  console.log(method);

  type = type.toUpperCase() as 'GET' | 'POST';

  let requestUrl = url;
  let requestBody: any = undefined;
  const headers = buildHeaders(token || '', url, data, type);

  if (type === 'GET') {
    const queryString = buildQueryString(data);
    if (queryString) {
      requestUrl = `${url}?${queryString}`;
    }
  } else if (type === 'POST') {
    requestBody = data;
  }

  const config: AxiosRequestConfig = {
    url: requestUrl,
    method: type,
    headers,
    data: requestBody,
    timeout: timeout > 0 ? timeout : undefined,
    // 不让 axios 对非 2xx 自动 reject，保持与原 fetchAdapter 一致
    validateStatus: () => true,
    // 禁用 axios 默认的参数序列化，我们手动拼接了 query
    paramsSerializer: {
      encode: (param) => String(param),
    },
  };

  try {
    const response = await axios.request<FetchResult<T>>(config);

    // 如果响应体已经是 { code, msg, data } 结构，直接返回
    const result = response.data;
    if (result && typeof result.code !== 'undefined') {
      return result;
    }

    // 否则包装成统一结构
    return {
      code: response.status === 200 ? 0 : -100,
      msg: 'success',
      data: result as any,
    } as FetchResult<T>;
  } catch (error) {
    const axiosError = error as AxiosError;
    console.log(`请求错误:url:${url}`, axiosError.message);

    // 超时错误
    if (axiosError.code === 'ECONNABORTED') {
      return {
        code: -100,
        msg: '请求超时',
      };
    }

    // 其他错误
    return {
      code: -100,
      msg: 'connect error',
    };
  }
}