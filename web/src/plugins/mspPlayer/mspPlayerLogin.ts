import { onMounted, onUnmounted, watchEffect } from 'vue';

import MessageBox from '@/components/MessageBox';
import { appConfig } from '@/config';
import { useDC, useI18n } from '@/hooks';
import { authFunc, initFunc, queryFunc } from '@/plugins/mspPlayer';
import { useCommunicationStore, useMainStore } from '@/store';

import commRegister from './commRegister';
import confRegister from './conf/confRegister';
import groupRegister from './group/groupRegister';
import mediaRegister from './media/mediaRegister';
import moduleRegister from './module/moduleRegister';
import queryRegister from './query/queryRegister';
import smsRegister from './sms/smsRegister';
import voiceRegister from './voice/voiceRegister';

export function mspLogin() {
  const { t } = useI18n();
  const mainStore = useMainStore();
  const communicationStore = useCommunicationStore();

  let unifiedLoginTime = 0;

  commRegister();

  watchEffect(() => {
    if (communicationStore.hasComm) {
      confRegister();
      groupRegister();
      mediaRegister();
      moduleRegister();
      smsRegister();
      voiceRegister();
      queryRegister();
    }
  });

  // 消息，如果有来绑定isdn，登出，再登录。
  useDC('RESOURCE_BIND', 'bindResource', async (message) => {
    if (Array.isArray(message) && message.length > 0) {
      const isdncode = localStorage.getItem('isdncode');
      message.forEach(async (item) => {
        if (
          item.executorId &&
          item.executorId === appConfig.userData.id &&
          item.isdn !== isdncode
        ) {
          // 弹提示框，要求重新登录，才可以恢复通信
          const config = {
            offset: ['40%', '35%'],
            text: t('login.bind.isdnBind'),
            type: 'ok',
          };

          await MessageBox(config);
          localStorage.setItem('isdncode', item.isdn);
          localStorage.setItem('isdnpass', item.isdnPass);
          // 通信登出再登录
          CBMLogout();
          CBMLogin();
        }
      });
    }
  });

  useDC('RESOURCE_BIND', 'unbindResource', async (message) => {
    if (Array.isArray(message) && message.length > 0) {
      const isdncode = localStorage.getItem('isdncode');
      message.forEach(async (item) => {
        // 如果isdn和executorId都对上了，那么就需要修改appConfig中的isdn字段，同时弹提示框。
        if (item.isdn === isdncode && item.executorId === appConfig.userData.id) {
          mainStore.changeHostIsdn({ hostIsdn: null });
          localStorage.removeItem('isdncode');
          localStorage.removeItem('isdnpass');
          // 弹框
          const config = {
            offset: ['40%', '35%'],
            text: t('login.bind.isdnUnBind'),
            type: 'ok',
          };
          await MessageBox(config);
        }
      });
    }
  });

  onMounted(() => {
    CBMLogin();
  });

  onUnmounted(() => {
    CBMLogout();
  });

  /**
   * 登入
   * 连云港这块逻辑和张家口不一样/连云港用的是多窗口
   */
  async function CBMLogin() {
    initFunc.sdkInit({
      callback: (data) => {
        const { rsp, status } = data;
        communicationStore.setSdkInitStatus(status);

        if (status === '1' && rsp === '0') {
          communicationStore.setHasComm(true);
          return;
        }

        initFunc.forceInitMSP();

        unifiedLogin();
      },
    });
  }

  // 统一登录
  let timer: any = null;
  async function unifiedLogin() {
    const user = localStorage.getItem('isdncode');
    const password = localStorage.getItem('isdnpass');
    const data = await authFunc.unifiedLogin({ password, user });

    // 用于判断登录的云指挥账号是都具有通信账号，如果有就要去获取通信状态，如果没有就不用获取通信状态
    let isCommunication = false;
    if (data.rsp === '0') {
      clearTimeout(timer);
      isCommunication = true;
      initMrs();
    } else {
      communicationStore.setCommunicationExDesc(t('resource.resourceMsg.communicationAbnormal'));
      if (data.rsp !== '101' && unifiedLoginTime <= 5) {
        unifiedLoginTime++;
        timer = setTimeout(() => {
          unifiedLogin();
        }, 2000);
      }
    }

    console.log(`isCommunicationStatus:  ${isCommunication}`);
    communicationStore.setHasComm(isCommunication);
  }

  // 统一登出
  function CBMLogout() {
    authFunc.unifiedLogout();
  }

  // 初始化mrs
  async function initMrs() {
    const r = await queryFunc.queryMRS();
    if (r.rsp === '0') {
      const ip = r.list[0]?.addripv4;
      initFunc.initMRS(ip);
    }
  }
}
