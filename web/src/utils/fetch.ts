import MessageBox from '@/components/MessageBox';
import { useI18n } from '@/hooks';

import appConfig from '../config/appConfig';
import fetchAdapter from './fetchAdapter';

const fetch = async function (url = '', data = {}, type = 'POST', method = 'fetch', timeout = 0) {
  const { t } = useI18n();
  const res = await fetchAdapter(url, data, appConfig.token, type, method, timeout);
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

export default fetch;
