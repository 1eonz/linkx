import MessageBox from '@/components/MessageBox';
import { useI18n } from '@/hooks';

import appConfig from '../config/appConfig';
import axiosFetchAdapter from './axiosFetchAdapter';

/**
 * axios 版本的 fetch
 * 保持与原 fetch.ts 完全一致的函数签名和行为
 *
 * @param url - 请求地址
 * @param data - 请求数据
 * @param type - 请求类型 'GET' | 'POST'
 * @param method - 请求方式（保留参数，兼容原签名）
 * @param timeout - 超时时间（毫秒），0 表示不超时
 * @returns Promise<{ code: number; msg: string; data?: T }>
 */
const axiosFetch = async function <T = any>(
  url = '',
  data: Record<string, any> = {},
  type: 'GET' | 'POST' = 'POST',
  method = 'fetch',
  timeout = 0,
) {
  const { t } = useI18n();
  const res = await axiosFetchAdapter<T>(url, data, appConfig.token, type, method, timeout);

  // 401 业务码弹窗提示（与原 fetch.ts 保持一致）
  if (res && res.code && res.code === 401) {
    const text = t('login.logon.kickoutByTokenTimeoutTip');
    MessageBox({
      offset: ['40%', '35%'],
      text,
      type: 'ok',
    });
  }

  return res;
};

export default axiosFetch;