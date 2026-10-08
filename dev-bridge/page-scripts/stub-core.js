import { assertJsonValue, bridgeConfig } from './config.js';

export function createSdkStub({ target = 'h5portal', installStub, eventRegisterMethods = new Set(), hasStorageChangeEvent = false, envSpoofs = [], callTimeoutMs = 30000 } = {}) {
  const config = bridgeConfig(target);
  const pending = new Map();
  const handlers = new Map();
  const storage = new Map();
  const subscriptions = new Map();
  const NativeWebSocket = window.WebSocket;
  let socket;
  let stopped = false;
  let providerConnected = false;
  let registered = false;
  let sequence = 0;
  let reconnectTimer;
  envSpoofs.forEach((spoof) => spoof());
  const send = (message) => { if (socket?.readyState === 1 && registered) socket.send(JSON.stringify(message)); };
  const updateReady = () => { window.__bridgeReady = registered && providerConnected; };
  const subscribe = (event, storageKey) => {
    const message = { type: 'subscribe', event, storageKey };
    subscriptions.set(`${event}:${storageKey || ''}`, message);
    if (providerConnected) send(message);
  };
  const flush = () => {
    if (!registered || !providerConnected) return;
    for (const subscription of subscriptions.values()) send(subscription);
    for (const call of pending.values()) if (!call.sent) { call.sent = true; send(call.message); }
  };
  const finish = (id, error, result, details) => {
    const call = pending.get(id);
    if (!call) return;
    clearTimeout(call.timer); pending.delete(id);
    error ? call.reject(Object.assign(new Error(error), details || {})) : call.resolve(result);
  };
  function connect() {
    if (stopped) return;
    socket = new NativeWebSocket(config.url);
    socket.onopen = () => socket.send(JSON.stringify({ type: 'register', role: 'stub', target, session: config.session, token: config.token }));
    socket.onmessage = ({ data }) => {
      let message;
      try { message = JSON.parse(data); } catch { return; }
      if (message.type === 'registered') { registered = true; updateReady(); }
      if (message.type === 'providerStatus') { providerConnected = !!message.connected; updateReady(); flush(); }
      if (message.type === 'response') finish(message.id, message.error, message.result, message.errorDetails);
      if (message.type === 'event') {
        const callbacks = message.event === 'onStorageChange' ? storage.get(message.storageKey) : handlers.get(message.event);
        for (const callback of [...(callbacks || [])]) {
          try { Promise.resolve(callback(...message.payload)).catch((error) => console.error('[dev-bridge] event callback', error)); }
          catch (error) { console.error('[dev-bridge] event callback', error); }
        }
      }
    };
    socket.onclose = ({ code }) => {
      registered = false; providerConnected = false; updateReady();
      for (const [id] of pending) finish(id, 'bridge disconnected');
      if (!stopped && code !== 1008) reconnectTimer = setTimeout(connect, 1000);
    };
    socket.onerror = () => {};
  }
  const invoke = (method, args) => {
    if (stopped) return Promise.reject(new Error('bridge stopped'));
    try { assertJsonValue(args, new Set(), 'arguments'); } catch (error) { return Promise.reject(error); }
    return new Promise((resolve, reject) => {
      const id = `r${++sequence}`;
      const timer = setTimeout(() => { send({ type: 'cancel', id }); finish(id, `bridge timeout: ${method}`); }, callTimeoutMs);
      pending.set(id, { resolve, reject, timer, sent: false, message: { type: 'invoke', id, method, args } });
      flush();
    });
  };
  const stub = new Proxy({ storageChangeHandlers: new Map() }, {
    get(object, method) {
      if (method === 'then' || typeof method === 'symbol') return undefined;
      if (method in object) return object[method];
      if (hasStorageChangeEvent && method === 'onStorageChange') return (key, callback) => {
        if (typeof callback !== 'function') throw new Error('onStorageChange requires a callback');
        storage.set(key, new Set([callback])); subscribe(method, key);
      };
      if (hasStorageChangeEvent && method === 'removeStorageChange') return (key) => { storage.delete(key); subscriptions.delete(`onStorageChange:${key}`); };
      if (eventRegisterMethods.has(method)) return (callback) => {
        handlers.set(method, new Set(typeof callback === 'function' ? [callback] : [])); subscribe(method);
      };
      return (...args) => invoke(method, args);
    },
  });
  if (installStub) installStub(stub);
  else Object.defineProperty(window, 'WeSpaceSDK', { configurable: true, get: () => stub, set: () => {} });
  window.__DEV_BRIDGE__ = {
    sdk: stub,
    getStatus: () => ({ role: 'stub', target, session: config.session, connected: registered, providerConnected }),
    dispose: () => { stopped = true; clearTimeout(reconnectTimer); socket?.close(); for (const [id] of pending) finish(id, 'bridge disposed'); },
  };
  window.__bridgeStub = window.__DEV_BRIDGE__;
  connect();
  return stub;
}
