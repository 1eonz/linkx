import { appConfig } from '@/config';
import { saveLogs } from '@/plugins/logs';
import { sdkCallbackPrint, triggerSDKMethods } from '@/plugins/mspPlayer/helper';

export const initFunc = {
  // 强制初始化MSP
  forceInitMSP() {
    return triggerSDKMethods('device', 'forceInitMSP', {
      callback: (data) => {
        sdkCallbackPrint('强制初始化MSP', data);
      },
    });
  },
  // 获取MDC的IP
  getLocalIp() {
    return triggerSDKMethods('webSocket', 'getLocalIp', {
      callback: (data) => {
        sdkCallbackPrint('获取MDC的IP', data);
      },
    });
  },
  // 初始化录音录像服务器
  initMRS(mrsIp) {
    return triggerSDKMethods('device', 'initMRS', {
      callback: (data) => {
        sdkCallbackPrint('初始化录音录像服务器', data);
      },
      mrsIp,
    });
  },
  // 修改MDC的IP
  async modifyMdcIp(mdcip) {
    const res: any = await initFunc.getLocalIp();
    if (res.mdcip === mdcip) {
      return;
    }
    return triggerSDKMethods('webSocket', 'ModifyMdcIp', {
      callback: (data) => {
        sdkCallbackPrint('修改MDC的IP', data);
      },
      mdcip,
    });
  },
  // SDK初始化
  sdkInit({ callback }) {
    const sdkStatusNotify = (data) => {
      sdkCallbackPrint('SDK初始化', data);
      saveLogs('sdkStatusNotify', data);

      const { SDK_IP } = appConfig.settingData;
      if (SDK_IP) {
        initFunc.modifyMdcIp(SDK_IP);
      }
      callback(data);
    };
    const param = {
      // udcIP: appConfig.settingData.UDC_IP,
      // udcPort: appConfig.settingData.UDC_PORT,
      debugMode: import.meta.env.DEV ? 'false' : 'true',
      demo_wssflow_enable: false,
      mode: 'wssflow', // plugins 插件模式 window 弹窗模式 wssflow 推流模式
      ringFlag: '0',
      serverAddress: 'sdkserver.cloudicp.huawei.com',
      serverHttpPort: '8002',
      serverWSPort: '8002',
      ssl_enable: true,
      videoConfLocalWindow: false,
    };
    saveLogs('ICPSDK', param);
    // 将sdkStatusNotify拿出来是为了存储日志尽可能少占内存
    window.ICP = new window.ICPSDK({ ...param, sdkStatusNotify });
  },
};
