/**
 * SDK 代理桥 —— Provider 协议主干（工具无关，与具体端和驱动工具无关）
 *
 * 职责: 运行在宿主 App 加载的页面里（真实环境），等待 SDK 就绪后，
 *      连接本地代理服务，把真实 SDK 的所有方法暴露给本地浏览器
 *      （开发联调的浏览器 或 Playwright 等自动化工具驱动的浏览器）调用。
 *      本模块只做"转发器"，不替换 SDK。
 *
 * 各端通过适配层调用 createProvider(config) 注入端配置:
 *   sdkName            SDK 全局变量名（默认 WeSpaceSDK，仅用于日志与默认 getSDK）
 *   proxyPort          代理服务 WS 端口（默认 8787，多端按 8x87/8x88 规律错开）
 *   getSDK             () => SDK 实例；默认 () => window[sdkName]，
 *                      模块单例型 SDK 的端（预留设计）需覆写
 *   bridgeEvents       [{ method }] 事件注册方法清单（按端维护，见各端适配层）
 *   storageChangeEvent { method } 参数化监听配置（如 onStorageChange(key, handler)），
 *                      null 表示该端 SDK 不支持
 *   sdkReadyTimeout    等待 SDK 就绪的轮询上限（毫秒，默认 10000）
 *
 * 触发条件: 由各端适配层以 import.meta.env.DEV 判断（URL 不带 ?bridge=stub
 *          时默认进入 provider 模式），生产 build 时自动剔除
 */
export function createProvider({
  sdkName = 'WeSpaceSDK',
  proxyPort = 8787,
  getSDK,
  bridgeEvents = [],
  storageChangeEvent = null,
  sdkReadyTimeout = 10000,
} = {}) {
  const resolveSDK = getSDK || (() => window[sdkName]);

  // 自动从当前页面 URL 推导代理地址，避免 Mixed Content / localhost 跨源拦截
  // 宿主用 http://172.24.66.78:8001/ 加载时，代理应连 ws://172.24.66.78:<proxyPort>
  // 宿主用 http://localhost:8001/ 加载时，代理应连 ws://localhost:<proxyPort>
  const PROXY_URL = `ws://${location.hostname}:${proxyPort}`;

  // 参数化监听方法名（如 WeSpaceSDK 的 onStorageChange(key, handler)）
  const storageChangeMethod = storageChangeEvent ? storageChangeEvent.method : null;

  let ws = null;
  let connected = false;
  let eventBridgeInstalled = false;

  function log(...args) {
    console.log('[bridge provider]', ...args);
  }

  function safeSend(obj) {
    if (ws && ws.readyState === WebSocket.OPEN) {
      try {
        ws.send(JSON.stringify(obj));
      } catch (err) {
        log('send error:', err);
      }
    }
  }

  // 发送 invoke 应答；连接已断开时仅记日志，不向 onmessage 抛异常
  function safeSendResponse(id, result, error) {
    try {
      if (ws && ws.readyState === WebSocket.OPEN) {
        ws.send(
          JSON.stringify(error ? { type: 'response', id, error } : { type: 'response', id, result }),
        );
      }
    } catch (err) {
      log('send response error:', err);
    }
  }

  /**
   * 拦截 SDK 的回调注册 API。
   * 业务代码调用 sdk.onXxx(cb) 时，把 cb 包装一层:
   *   - 转发回调参数到代理（供浏览器侧的对应 handler 触发）
   *   - 同时仍执行业务的 cb
   *
   * 特殊处理（storageChangeMethod，如 onStorageChange(key, handler)）:
   *   参数化监听，按 key 存储。转发时带上 key 让浏览器侧能区分路由。
   */
  function installEventBridge() {
    if (eventBridgeInstalled) return;
    const sdk = resolveSDK();
    if (!sdk) return;

    const installed = [];

    // 1. 普通事件: (handler) => void
    bridgeEvents.forEach(({ method }) => {
      if (typeof sdk[method] !== 'function') return;
      if (sdk[method].__bridgeWrapped) return;

      const orig = sdk[method].bind(sdk);
      sdk[method] = function (cb) {
        const wrapped = function (...args) {
          // 1. 转发到代理
          safeSend({ type: 'event', event: method, payload: args });
          // 2. 仍然执行业务侧 handler
          if (typeof cb === 'function') {
            try {
              cb(...args);
            } catch (err) {
              log('event handler error:', method, err);
            }
          }
        };
        // 业务侧通过 this.xxxHandler 拿到的是 wrapped，
        // 原生 weSpace2WebCall 调用 wrapped 时会触发转发 + 业务 cb
        orig(wrapped);
      };
      sdk[method].__bridgeWrapped = true;
      installed.push(method);
    });

    // 2. 参数化监听: (key, handler) => void，转发时带上 key 让浏览器侧按 key 路由
    if (
      storageChangeMethod &&
      typeof sdk[storageChangeMethod] === 'function' &&
      !sdk[storageChangeMethod].__bridgeWrapped
    ) {
      const origStorage = sdk[storageChangeMethod].bind(sdk);
      sdk[storageChangeMethod] = function (key, handler) {
        const wrapped = function (value) {
          // 转发到代理，event 名固定为方法名，payload 带 value、storageKey 带 key
          safeSend({
            type: 'event',
            event: storageChangeMethod,
            payload: [value],
            storageKey: key,
          });
          // 仍然执行业务侧 handler
          if (typeof handler === 'function') {
            try {
              handler(value);
            } catch (err) {
              log('event handler error:', storageChangeMethod, key, err);
            }
          }
        };
        origStorage(key, wrapped);
      };
      sdk[storageChangeMethod].__bridgeWrapped = true;
      installed.push(storageChangeMethod);
    }

    // 3. removeStorageChange: 业务代码删除监听，无需特殊处理（直接走原始逻辑，
    //    handler 被 wrap 了，删除时只是从 storageChangeHandlers Map 里移除 wrapped）

    eventBridgeInstalled = true;
    log('event bridge installed for:', installed);
  }

  function connect() {
    if (ws) return;
    try {
      ws = new WebSocket(PROXY_URL);
    } catch (err) {
      log('connect failed, retry in 2s:', err);
      setTimeout(connect, 2000);
      return;
    }

    ws.onopen = () => {
      connected = true;
      log('connected to proxy, SDK ready:', !!resolveSDK());
      ws.send(JSON.stringify({ type: 'register', role: 'provider' }));
      installEventBridge();
    };

    ws.onclose = () => {
      connected = false;
      ws = null;
      log('disconnected, retry in 2s');
      setTimeout(connect, 2000);
    };

    ws.onerror = (e) => log('socket error:', e);

    ws.onmessage = async (ev) => {
      let msg;
      try {
        msg = JSON.parse(ev.data);
      } catch {
        return;
      }
      if (msg.type !== 'invoke') return;

      const { id, method, data } = msg;
      try {
        const sdk = resolveSDK();
        if (!sdk) throw new Error(`${sdkName} not ready`);
        if (typeof sdk[method] !== 'function') {
          throw new Error(`SDK.${method} is not a function`);
        }
        log('invoke', method, data);
        const result = await sdk[method](data);
        safeSendResponse(id, result);
      } catch (err) {
        log('invoke error', method, err.message);
        safeSendResponse(id, undefined, err.message);
      }
    };
  }

  // 等待 SDK 就绪（H5Portal 由 App.vue onMounted 挂 window；web 由 bridge/post.js 的 DEV 钩子挂出）
  function waitForSDK(retries = Math.max(1, Math.round(sdkReadyTimeout / 100))) {
    if (resolveSDK()) {
      connect();
    } else if (retries > 0) {
      setTimeout(() => waitForSDK(retries - 1), 100);
    } else {
      log(`SDK not found after ${sdkReadyTimeout}ms, give up`);
    }
  }

  // 调试入口
  window.__bridgeProvider = {
    status: () => ({ connected, hasSDK: !!resolveSDK() }),
    reconnect: () => {
      if (ws) {
        try {
          ws.close();
        } catch {
          /* ignore */
        }
      }
      ws = null;
      setTimeout(connect, 500);
    },
  };

  waitForSDK();
  log('provider module loaded, waiting for SDK...');
}
