import { saveLogs } from './index';

/**
 * 捕获全局的JavaScript错误
 *
 * js调用
 * 资源加载
 * http请求
 */
export const catchGlobalError = () => {
  // 同步异常
  window.addEventListener('error', (e: any) => {
    if (e.error?.stack) {
      saveLogs('error', e.error.stack);
    }
  });

  // 异步异常
  window.addEventListener('unhandledrejection', (e: any) => {
    saveLogs('error', e.reason.stack);
  });
};
