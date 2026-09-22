import { useI18n } from '@/hooks';

const { t } = useI18n();
/**
 * 紧急事件状态--文字
 * @param messageData
 */
export function messageStatusText(messageData: any) {
  const { status } = messageData;
  switch (status) {
    case 0: {
      return t('message.status.pending');
    }
    case 1: {
      return t('message.status.processed');
    }
    case 2: {
      return t('videoControl.warningInformation.ignore');
    }
    default: {
      return t('videoControl.warningInformation.untreated');
    }
  }
}

/**
 * 紧急事件状态--颜色
 * @param messageData
 */
export function messageStatusColor(messageData: any) {
  const { status } = messageData;
  switch (status) {
    case 0: {
      return 'red';
    }
    case 1: {
      return 'gray';
    }
    case 2: {
      return 'gray';
    }
    default: {
      return 'red';
    }
  }
}
