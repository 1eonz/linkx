import { delVideoPollingGroup } from '@/api/monitor';
import { Message } from '@/components/Message';
import MessageBox from '@/components/MessageBox';
import { appConfig } from '@/config';
import { useI18n } from '@/hooks';

const { t } = useI18n();

export async function deleteGroup(id): Promise<boolean> {
  const text = t('resource.poll.removePoll');
  const res = await MessageBox({ text });
  if (res) {
    const { code, msg } = await delVideoPollingGroup({
      groupId: id,
      isdn: appConfig.isdn,
    });
    if (code === 0) {
      Message(t('resource.poll.pollDeleteSuccess'));
      return true;
    } else {
      Message({
        message: msg || t('resource.poll.pollDeleteFailed'),
        type: 'error',
      });
    }
  }
  return false;
}
