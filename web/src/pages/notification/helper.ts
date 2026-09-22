import { useI18n } from '@/hooks';

import dayjs from 'dayjs';

export function showTime(time) {
  const { t } = useI18n();
  const target = dayjs(time).format('YYYY-MM-DD');
  const today = dayjs().format('YYYY-MM-DD');

  // 今天
  if (target === today) {
    return dayjs(time).format('HH:mm');
  }

  // 昨天
  const yesterday = dayjs().subtract(1, 'day').format('YYYY-MM-DD');
  if (target === yesterday) {
    return t('common.yesterday');
  }

  return dayjs(time).format('MM-DD');
}
