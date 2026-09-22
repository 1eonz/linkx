import * as Pinia from 'pinia';
import { createApp } from 'vue';

import App from './App.vue';
import './style.css';
import router from './router';

import 'vant/lib/index.css';
import 'vant/es/toast/style'; //确保引入dialog后showToast能展示
// import MyContainer from '@/components/my-container/my-container.vue'
import 'core-js/stable';
import 'regenerator-runtime/runtime';

// 在开启 vConsole 的同时，用日志方式记录 WebSocket 收发，方便在移动端调试
function enableWsConsoleLog() {
  if (typeof window === 'undefined') return;
  if (window.__WS_CONSOLE_PATCHED__) return;
  const OriginalWebSocket = window.WebSocket;
  if (!OriginalWebSocket) return;

  const log = (...args) => console.log('[WS]', ...args);
  window.WebSocket = function (url, protocols) {
    const ws = new OriginalWebSocket(url, protocols);
    log('connect', url);
    ws.addEventListener('open', () => log('open', url));
    ws.addEventListener('message', (evt) => log('message', evt.data));
    ws.addEventListener('error', (err) => log('error', err));
    ws.addEventListener('close', (evt) => log('close', evt.code, evt.reason));

    const origSend = ws.send;
    ws.send = function (data) {
      log('send', data);
      return origSend.call(ws, data);
    };
    return ws;
  };
  window.WebSocket.prototype = OriginalWebSocket.prototype;
  window.__WS_CONSOLE_PATCHED__ = true;
  log('WebSocket console hook enabled');
}

if (!Array.prototype.at) {
  Array.prototype.at = function (index) {
    if (index < 0) {
      index = this.length + index;
    }
    if (index < 0 || index >= this.length) {
      return undefined;
    }
    return this[index];
  };
}

function createConsole(isCreate) {
  if (isCreate) {
    import('vconsole').then((VConsole) => {
      new VConsole.default();
    });
  }
}
// createConsole(true); //测试时使用
// enableWsConsoleLog(); // 测试场景默认开启 WS 日志
// ✅ 判断当前是否为浏览器环境（H5）
if (typeof window !== 'undefined') {
  const queryString = window.location.search;
  if (queryString) {
    const urlParams = new URLSearchParams(queryString);
    if (urlParams.get('debug') === 'true') {
      //import.meta.env.DEV
      createConsole(true);
      enableWsConsoleLog();
    }
  }
}
// 开发环境默认打开
if (import.meta.env && import.meta.env.DEV) {
  enableWsConsoleLog();
}

const app = createApp(App);
app.use(router);
app.use(Pinia.createPinia());
// app.component("my-container",MyContainer)
app.mount('#app');
