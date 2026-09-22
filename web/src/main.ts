import { catchGlobalError } from '@/plugins/logs/errorLog';

import App from './App.vue';
import { useCreateApp } from './hooks';

async function enableVConsoleByQuery() {
  if (typeof window === 'undefined') return;

  const queryString = window.location.search;
  if (!queryString) return;

  const urlParams = new URLSearchParams(queryString);
  if (urlParams.get('debug') !== 'true') return;

  const VConsole = await import('vconsole');
  new VConsole.default();
}

import { isWebView2 } from './utils/env';

// styles
import 'virtual:windi.css';
import 'element-plus/dist/index.css';
import './styles/theme/index.less';
import './styles/element-plus/index.less';
import './assets/font/index.css';

// Register icon sprite
import 'virtual:svg-icons-register';
import './utils/rem';
import 'dayjs/locale/zh-cn';

// 引入主题
import { themeService } from './data/useTheme';

const bootstrap = async () => {
  // Browser 宿主：必须最早挂载 message 监听，避免错过 SETUP_CHANNEL
  if (!isWebView2()) {
    await import('./bridge/index-browser.js');
  } else {
    // WebView2 宿主：debug 下先确保 vConsole 就绪
    await enableVConsoleByQuery();

    // WebView2 走统一入口并等待 ready
    const { bridgeReady } = await import('./bridge/index.js');
    await bridgeReady;
  }

  // 全局异常捕获
  catchGlobalError();

  const app = useCreateApp(App);

  // 初始化主题：WebView2环境下锁定为light主题
  if (isWebView2()) {
    themeService.lockTheme('light');
  } else {
    themeService.init();
    // 监听来自主项目的主题切换消息（仅非WebView2环境）
    window.addEventListener('message', (event) => {
      if (event.data && event.data.type === 'themeChange') {
        const { theme } = event.data;
        if (theme === 'light' || theme === 'dark') {
          themeService.setTheme(theme);
          console.log('收到主题切换消息:', theme);
        }
      }
    });
  }

  // 挂载
  app.mount('#app');

  // 预加载全局配置
  import('@/utils/globalConfig')
    .then(({ globalConfig }) => globalConfig.load())
    .catch(() => {});

  // WebView2：补一次初始可见性事件（等 Vue 应用已就绪后再触发，避免线上 undefined._s）
  if (isWebView2()) {
    try {
      const receiveObj = (await import('./bridge/receive')).default;
      if (typeof receiveObj?.onVisibleChange === 'function') {
        setTimeout(() => {
          // 第二个参数 true 表示主动触发，不刷新页面
          void receiveObj.onVisibleChange('true', true);
        }, 0);
      }
    } catch (e) {
      console.warn('[Bridge-WebView2] main.ts 手动触发 onVisibleChange 失败:', e);
    }
  }
};

bootstrap();
