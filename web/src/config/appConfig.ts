import { getToken } from '@/utils/auth';

const config: any = {
  actionId: '',
  bindResourceData: {},
  config: {},
  dictionary: {},
  dictionaryList: {},
  isdn: '',
  isdnPass: '',
  isHK: import.meta.env.VITE_MODE_TYPE === 'hk',
  isLyg: import.meta.env.VITE_MODE_TYPE === 'lyg',
  isReload: false,
  logSwitch: 0,
  mapConfig: {},
  remindSwitch: 1,
  resourceId: '',
  settingData: {},
  token: getToken(), // 直接从cookie中获取
  userCode: '',
  userData: {
    categoryId: '',
    code: '', // 警员号
    commandCenterId: '',
    id: '',
    name: '',
    organizationId: '',
    organizationName: '',
  },
};

export default config;
