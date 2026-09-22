import { saveLogs } from '@/plugins/logs';

// sdk回调打印
export const sdkCallbackPrint = (name, data) => {
  console.log(name, JSON.stringify(data));
};

// 封装SDK调用统一入口
export const triggerSDKMethods = (module, methods, param): Promise<any> => {
  return new Promise((resolve) => {
    const b = param.callback;
    const nb = (data) => {
      const k = param.eventName || methods;
      {
        const d = { ...data };
        delete d.list; // 查询的list太大不宜存储
        saveLogs(k, d);
      }

      b?.(data);
      resolve(data);
    };
    param.callback = nb;

    {
      const p = { ...param };
      delete p.callback;
      if (methods === 'register') {
        saveLogs('register', param.eventName);
      } else {
        saveLogs(methods, p);
      }
    }

    try {
      window.cloudICP.dispatch[module][methods](param);
    } catch (error) {
      console.log(error);
    }
  });
};
