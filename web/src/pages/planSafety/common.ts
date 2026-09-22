import { appConfig } from '@/config';
import { useI18n } from '@/hooks';

const { t } = useI18n();

/**
 * 根据code取字典配置
 * @param code
 * @returns
 */
function getDictionaryOptions(code) {
  return (appConfig.dictionaryList[code] || []).map((item) => {
    return {
      label: item.name,
      value: item.value,
    };
  });
}

export const planTypeOptions = () => getDictionaryOptions('1001');

export const planLevelOptions = () => getDictionaryOptions('1002');

export function getStatusText(val) {
  switch (val) {
    case 0: {
      return t('planSafety.tabs.notStart');
    }
    case 1: {
      return t('planSafety.tabs.guaranteed');
    }
    case 2: {
      return t('planSafety.tabs.end');
    }
    default: {
      return t('planSafety.tabs.notStart');
    }
  }
}

export function getStatusColor(val) {
  switch (val) {
    case 0: {
      return 'red';
    }
    case 1: {
      return 'orange';
    }
    case 2: {
      return 'gray';
    }
    default: {
      return 'red';
    }
  }
}

export function getSupportType(val) {
  switch (val) {
    case '0': {
      return t('planSafety.significantSecurity');
    }
    case '1': {
      return t('planSafety.vipSecurity');
    }
    default: {
      return t('resource.detailType.noYet');
    }
  }
}

export function getSupportLevel(val) {
  switch (val) {
    case '0': {
      return t('planSafety.level.particularlyImportant');
    }
    case '1': {
      return t('planSafety.level.important');
    }
    case '2': {
      return t('planSafety.level.larger');
    }
    case '3': {
      return t('planSafety.level.normal');
    }
    default: {
      return t('resource.detailType.noYet');
    }
  }
}
export function getLevelColor(val) {
  switch (val) {
    case '0': {
      return 'red';
    }
    case '1': {
      return 'orange';
    }
    case '2': {
      return 'yellow';
    }
    case '3': {
      return 'blue';
    }
    default: {
      return 'red';
    }
  }
}
