import { useI18n } from '@/hooks';
import { isDef, isUnDef } from '@/utils/is';
import { validHasSpecialCharacter, validUtf8LongerThanLen } from '@/utils/validate';

/**
 * @description 自定义表单校验规则
 * https://vee-validate.logaretm.com/v4/guide/global-validators
 */
import { defineRule } from 'vee-validate';

function outRange(val, min, max) {
  const { t } = useI18n();

  if ((isDef(min) || isDef(max)) && (Number(val) < Number(min) || Number(val) > Number(max))) {
    return `${t('common.validate.outOfRange') + min}~${max}`;
  }

  return false;
}

export default {
  install() {
    const { t } = useI18n();

    // 最基础的输入框校验
    defineRule('basicValidate', (str) => {
      if (validHasSpecialCharacter(str)) {
        return t('common.validate.notSupportedSymbols');
      } else if (str.includes('  ')) {
        return t('common.validate.notSupportedSpaces');
      } else if (str.includes('%%')) {
        return t('common.validate.notSupportedPercent');
      }
      return true;
    });

    // 必填
    defineRule('required', (str) => {
      if (str === '' || isUnDef(str)) {
        return t('common.validate.notNull');
      }
      return true;
    });

    // 不超过32字节长度
    defineRule('maximum32Bytes', (str) => {
      if (validUtf8LongerThanLen(str, 32)) {
        return t('common.validate.rulesWords');
      }
      return true;
    });

    // 不超过64字符长度
    defineRule('maximum64Bytes', (str) => {
      // eslint-disable-next-line no-control-regex
      if (str.replaceAll(/[^\u0000-\u00FF]/g, 'AA').length > 64) {
        return t('common.validate.rulesWords64');
      }
      return true;
    });

    // 整数
    defineRule('number', (val, [min, max]) => {
      const reg = /(^\d*$)/;

      if (!reg.test(val)) {
        return t('common.validate.intNumber');
      }

      const range = outRange(val, min, max);
      if (range) {
        return range;
      }

      return true;
    });

    // 数字或者英文字符
    defineRule('numberAndStr', (val, [min, max]) => {
      const reg = /(^[a-z0-9]+$)/i;

      if (!reg.test(val)) {
        return t('common.validate.numberOrStr');
      }

      const range = outRange(val, min, max);
      if (range) {
        return range;
      }

      return true;
    });

    // 浮点数或者整数
    defineRule('floatOrInt', (val, [min, max]) => {
      const regInt = /^-?\d+$/;
      const regFloat = /^(-?\d+)(\.\d+)?$/;

      if (!regInt.test(val) && !regFloat.test(val)) {
        return t('common.validate.floatOrInt');
      }

      const range = outRange(val, min, max);
      if (range) {
        return range;
      }

      return true;
    });

    // 数字或者IP
    defineRule('numberOrIP', (val, [min, max]) => {
      const numberRegex = /^\d+$/; // 正整数
      const ipRegex =
        /^(?:(?:25[0-5]|2[0-4]\d|[01]?\d{1,2})\.){3}(?:25[0-5]|2[0-4]\d|[01]?\d{1,2})$/; // IPv4地址

      const reg = numberRegex.test(val) || ipRegex.test(val);
      if (!reg) {
        return t('common.validate.numberOrIP');
      }

      const range = outRange(val, min, max);
      if (range) {
        return range;
      }

      return true;
    });
  },
};
