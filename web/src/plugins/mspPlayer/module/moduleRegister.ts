import MessageBox from '@/components/MessageBox';
import { useI18n } from '@/hooks';
import { sdkCallbackPrint, triggerSDKMethods } from '@/plugins/mspPlayer/helper';
import { useCommunicationStore } from '@/store';

function moduleRegister() {
  const { t } = useI18n();
  const communicationStore = useCommunicationStore();

  init();

  function init() {
    // 模块断连通知
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('模块断连通知', data);
        const { moduleType } = data;
        // 没有短信相关功能 暂不考虑
        if (Number(moduleType) === 1) return;
        const module = {
          1: t('communication.moduleConnection.SMSError'),
          2: t('communication.moduleConnection.SIPError'),
          3: t('communication.moduleConnection.networkError'),
          4: t('communication.moduleConnection.MSPError'),
          5: t('communication.moduleConnection.SDKSERVERError'),
        };
        MessageBox({
          offset: ['40%', '35%'],
          text: module[moduleType],
          type: 'ok',
        });
        communicationStore.deleteAllComm();
        communicationStore.setHasComm(false);
      },
      eventName: 'OnDisConnection',
      eventType: 'ModuleNotify',
    });

    // 模块连接成功
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('模块连接成功', data);
        communicationStore.setHasComm(true);
      },
      eventName: 'OnConnection',
      eventType: 'ModuleNotify',
    });

    // 用户被踢出
    triggerSDKMethods('event', 'register', {
      callback: (data) => {
        sdkCallbackPrint('用户被踢出', data);
      },
      eventName: 'OnDispatchKickOutNotifyEvent',
      eventType: 'ModuleNotify',
    });
  }
}

export default moduleRegister;
