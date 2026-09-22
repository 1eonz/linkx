import { useI18n } from '@/hooks';

import { ElMessage } from 'element-plus';

let msgTextList: string[] = [];

export const Message = (config) => {
  const { t } = useI18n();
  const param: any = {
    duration: 1000,
    offset: 90,
  };

  if (typeof config === 'string') {
    // 确保结果是翻译后的
    param.message = t(config);
  } else if (typeof config === 'number') {
    param.message = config;
  } else {
    Object.assign(param, config);
  }

  if (msgTextList.includes(param.message)) {
    return;
  }

  msgTextList.push(param.message);
  setTimeout(() => {
    msgTextList = msgTextList.filter((i) => i !== param.message);
  }, param.duration);

  ElMessage(param);
};
