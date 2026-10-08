/**
 * SDK 代理桥 —— Stub 协议主干（工具无关，与具体端和驱动工具无关）
 *
 * 职责: 运行在本地浏览器加载的页面里，把 SDK 替换为桩，
 *      所有 SDK 方法调用通过 WebSocket 转发给代理 → 宿主真实 SDK。
 *      本模块不感知浏览器由谁驱动: 开发联调时是开发人员手动打开的浏览器，
 *      自动化测试时是 Playwright 等工具驱动的浏览器 —— 对桩完全等价。
 *
 * 各端通过适配层调用 createSdkStub(config) 注入端配置:
 *   sdkName               SDK 全局变量名（默认 WeSpaceSDK，仅用于日志与默认桩安装）
 *   proxyPort             代理服务 WS 端口（默认 8787，多端按 8x87/8x88 规律错开）
 *   installStub           (stub) => void 桩安装方式；默认 defineProperty getter/setter
 *                         锁定 window[sdkName]（适配 App.vue onMounted 覆盖场景），
 *                         模块单例型 SDK 的端（预留设计）覆写为挂到自己的全局占位
 *   eventRegisterMethods  Set<string> 事件注册方法白名单（业务代码调这些方法注册回调，
 *                         桩端把 cb 存到本地，等待 provider 转发事件触发）
 *   hasStorageChangeEvent 是否支持参数化监听 onStorageChange(key, handler) /
 *                         removeStorageChange(key)
 *   envSpoofs             (() => void)[] 环境伪造钩子（chrome.webview /
 *                         PIM_GetPlatform 等，按端启用，见 page-scripts/env-spoofs.js）
 *   invokeRetry           provider 未上线时的等待/重试策略——
 *                         新代理（推送 providerStatus）: 调用门控等待，总窗口 =
 *                         count × intervalMs，窗口内宿主连上即自动放行；
 *                         旧代理（无状态推送）: 按原有盲重试 count 次
 *   callTimeoutMs         单次调用超时（默认 30000，等待 provider 的时间不计入）
 *
 * 触发条件: 由各端适配层以 import.meta.env.DEV + ?bridge=stub 判断，
 *          生产 build 时整段被 Vite tree-shake
 */
export function createSdkStub({
  sdkName = 'WeSpaceSDK',
  proxyPort = 8787,
  installStub,
  eventRegisterMethods = new Set(),
  hasStorageChangeEvent = false,
  envSpoofs = [],
  invokeRetry = { count: 3, intervalMs: 500 },
  callTimeoutMs = 30000,
} = {}) {
  // 自动从当前页面 URL 推导代理地址，与 provider 保持一致
  const PROXY_URL = `ws://${location.hostname}:${proxyPort}`;

  // 保存原生 WebSocket 引用，避免被业务的 WS console hook 干扰
  // (hook 会替换 window.WebSocket，导致 readyState 检查异常，invoke 调用堆积超时)
  const NativeWebSocket = window.WebSocket;

  const pending = new Map(); // callId -> { resolve, reject }
  const eventHandlers = new Map(); // event -> Set<Function>
  const storageChangeHandlers = new Map(); // storageKey -> Set<Function>
  let ws = null;
  // provider 在线状态（由代理推送/快照更新）:
  //   providerStatusKnown —— 是否收到过 providerStatus（新代理）；旧代理无此消息，保持旧盲重试行为
  //   providerOnline     —— 宿主端 provider 当前是否已连接代理
  let providerStatusKnown = false;
  let providerOnline = false;

  function log(...args) {
    console.log('[bridge stub]', ...args);
  }

  // 环境伪造钩子（chrome.webview / PIM_GetPlatform 等，按端配置）
  envSpoofs.forEach((spoof) => {
    try {
      spoof();
    } catch (err) {
      log('env spoof error:', err);
    }
  });

  function connect() {
    if (ws) return;
    // 重连后状态未知，等待代理对 subscribe 的状态快照重新确认
    providerStatusKnown = false;
    providerOnline = false;
    try {
      ws = new NativeWebSocket(PROXY_URL);
    } catch {
      setTimeout(connect, 1000);
      return;
    }

    ws.onopen = () => {
      log('connected to proxy');
      window.__bridgeReady = true;
      ws.send(JSON.stringify({ type: 'subscribe' }));
    };
    ws.onclose = () => {
      window.__bridgeReady = false;
      ws = null;
      log('disconnected, retry in 500ms');
      setTimeout(connect, 500);
    };
    ws.onerror = () => {
      /* onclose 会处理 */
    };

    ws.onmessage = (ev) => {
      let msg;
      try {
        msg = JSON.parse(ev.data);
      } catch {
        return;
      }

      // 0. provider 在线状态（新代理在 subscribe 应答快照 + 上下线时推送）
      if (msg.type === 'providerStatus') {
        const was = providerOnline;
        providerStatusKnown = true;
        providerOnline = !!msg.connected;
        if (was !== providerOnline) {
          log('provider status:', providerOnline ? 'online' : 'offline');
        }
        return;
      }

      // 1. SDK 调用结果
      if (msg.type === 'response' && msg.id) {
        const p = pending.get(msg.id);
        if (!p) return;
        pending.delete(msg.id);
        msg.error ? p.reject(new Error(msg.error)) : p.resolve(msg.result);
        return;
      }
      // 2. 宿主端推送的事件
      if (msg.type === 'event' && msg.event) {
        // 参数化监听特殊处理: 按 storageKey 路由到对应 handler
        if (msg.event === 'onStorageChange' && msg.storageKey) {
          const key = msg.storageKey;
          const keySet = storageChangeHandlers.get(key);
          if (keySet) {
            keySet.forEach((cb) => {
              try {
                cb(...(msg.payload || []));
              } catch (err) {
                log('storage event handler error:', key, err);
              }
            });
          }
          return;
        }

        // 普通事件
        const set = eventHandlers.get(msg.event);
        if (set) {
          set.forEach((cb) => {
            try {
              cb(...(msg.payload || []));
            } catch (err) {
              log('event handler error:', err);
            }
          });
        }
      }
    };
  }

  // 等待 provider 上线（宿主未连接代理时不白发请求，等代理的状态推送）
  function waitProviderOnline(timeoutMs) {
    if (providerOnline) return Promise.resolve();
    return new Promise((resolve, reject) => {
      const deadline = Date.now() + timeoutMs;
      const timer = setInterval(() => {
        if (providerOnline) {
          clearInterval(timer);
          resolve();
        } else if (Date.now() >= deadline) {
          clearInterval(timer);
          reject(
            new Error(`provider not connected (waited ${Math.round(timeoutMs / 1000)}s)`),
          );
        }
      }, 200);
    });
  }

  // 转发 SDK 调用到代理
  // 新代理: 宿主未上线时先门控等待（invokeRetry 总窗口内自动放行，先开页面后开宿主不报错）
  // 旧代理（无状态推送）: 保持原有 "provider not connected" 盲重试
  function invoke(method, data, retryCount = invokeRetry.count) {
    return new Promise((resolve, reject) => {
      const id = `r${Math.random().toString(36).slice(2, 10)}`;

      pending.set(id, {
        resolve,
        reject: (err) => {
          // 仅旧代理需要盲重试；新代理由 waitProviderOnline 门控，这里只兜底极端竞态
          if (
            retryCount > 0 &&
            !providerStatusKnown &&
            err.message.includes('provider not connected')
          ) {
            log('provider not connected, retry', invokeRetry.count - retryCount + 1, method);
            setTimeout(() => {
              invoke(method, data, retryCount - 1).then(resolve).catch(reject);
            }, invokeRetry.intervalMs);
            return;
          }
          reject(err);
        },
      });

      const trySend = () => {
        if (ws && ws.readyState === NativeWebSocket.OPEN) {
          try {
            ws.send(JSON.stringify({ type: 'invoke', id, method, data }));
          } catch {
            setTimeout(trySend, 100);
          }
        } else {
          setTimeout(trySend, 100);
        }
      };

      // 门控: 已知宿主未上线 → 等待状态推送（窗口 = invokeRetry.count × intervalMs）
      const gate =
        providerStatusKnown && !providerOnline
          ? waitProviderOnline(invokeRetry.count * invokeRetry.intervalMs)
          : Promise.resolve();

      gate
        .then(() => {
          trySend();
          // 超时保护（等待 provider 的时间不计入）
          setTimeout(() => {
            if (pending.has(id)) {
              pending.get(id).reject(new Error(`proxy timeout: ${method}`));
              pending.delete(id);
            }
          }, callTimeoutMs);
        })
        .catch((err) => {
          pending.delete(id);
          reject(err);
        });
    });
  }

  /**
   * 构造桩 SDK：用 Proxy 拦截所有方法。
   * - 事件注册类方法: 把 cb 存到本地，等待 provider 转发事件触发
   * - onStorageChange(key, handler): 参数化事件监听（按端启用）
   * - removeStorageChange(key): 删除对应 key 的 handler
   * - 普通方法: 转发到代理
   */
  const stub = new Proxy(
    {
      // 占位的 storageChangeHandlers，避免业务代码直接访问 SDK 属性时报错
      storageChangeHandlers: new Map(),
    },
    {
      get(_, prop) {
        if (prop === 'then' || prop === Symbol.toPrimitive) return undefined;

        // onStorageChange(key, handler): 参数化事件监听
        if (hasStorageChangeEvent && prop === 'onStorageChange') {
          return (key, handler) => {
            if (typeof key === 'string' && typeof handler === 'function') {
              if (!storageChangeHandlers.has(key)) {
                storageChangeHandlers.set(key, new Set());
              }
              storageChangeHandlers.get(key).add(handler);
              log('subscribe storage event', key);
            }
            return undefined;
          };
        }

        // removeStorageChange(key): 删除监听
        if (hasStorageChangeEvent && prop === 'removeStorageChange') {
          return (key) => {
            storageChangeHandlers.delete(key);
            log('unsubscribe storage event', key);
            return undefined;
          };
        }

        return (...args) => {
          if (eventRegisterMethods.has(prop)) {
            const cb = args[0];
            if (typeof cb === 'function') {
              if (!eventHandlers.has(prop)) eventHandlers.set(prop, new Set());
              eventHandlers.get(prop).add(cb);
              log('subscribe event', prop);
            }
            return Promise.resolve(undefined);
          }

          const data = args[0];
          log('invoke', prop);
          return invoke(prop, data);
        };
      },
      set() {
        // 拦截外部对 SDK 属性的赋值（如 storageChangeHandlers 之类）
        return true;
      },
    },
  );

  /**
   * 默认桩安装: defineProperty getter/setter 锁定 window[sdkName]。
   * App.vue onMounted 会执行 window.WeSpaceSDK = WeSpaceSDK，
   * 这里用 getter/setter 拦截：setter 静默丢弃赋值，getter 始终返回桩。
   * 不能用 writable: false —— 严格模式下赋值会抛错，导致 App.vue onMounted 后续代码不执行。
   */
  function defaultInstallStub() {
    try {
      Object.defineProperty(window, sdkName, {
        get() {
          return stub;
        },
        set() {
          // 静默吞掉赋值，保持桩不被覆盖
        },
        configurable: true,
      });
    } catch {
      window[sdkName] = stub;
    }
    log('stub installed and locked on window.' + sdkName);
  }

  // 调试入口
  window.__bridgeStub = {
    status: () => ({
      connected: ws && ws.readyState === NativeWebSocket.OPEN,
      providerOnline,
      pending: pending.size,
      events: [...eventHandlers.keys()],
      storageKeys: [...storageChangeHandlers.keys()],
    }),
    ready: () => !!window.__bridgeReady,
  };

  if (typeof installStub === 'function') {
    installStub(stub);
    log('stub installed via custom installer');
  } else {
    defaultInstallStub();
  }

  connect();
  log('stub installed, waiting for proxy connection...');
}
