import { assertJsonValue, bridgeConfig } from './config.js';

// Observe handler slots rather than replacing vendor SDK registration methods.
export function observeSdkEvents(sdk, events, send) {
  const restore = [];
  const latest = new Map();
  for (const { method, handlerField } of events) {
    if (!handlerField || !(handlerField in sdk)) continue;
    const original = Object.getOwnPropertyDescriptor(sdk, handlerField);
    if (!original?.configurable || original.get || original.set) continue;
    let callback = sdk[handlerField];
    const relay = (...payload) => {
      latest.set(method, payload);
      send({ type: 'event', event: method, payload });
      return typeof callback === 'function' ? callback(...payload) : undefined;
    };
    Object.defineProperty(sdk, handlerField, { configurable: true, enumerable: original.enumerable, get: () => relay, set: (value) => { callback = value; } });
    restore.push(() => Object.defineProperty(sdk, handlerField, { ...original, value: callback }));
  }
  // Storage handlers are keyed properties in these SDKs, despite being stored on a Map.
  const storageHandlers = sdk.storageChangeHandlers;
  if (storageHandlers) {
    const originalReceive = window.jsBridge?.receiveMessage;
    if (originalReceive) {
      const receive = function (id, method, data) {
        if (method === 'onStorageChange' && !this.resolveMap?.[id]) {
          try {
            const parsed = JSON.parse(data);
            send({ type: 'event', event: 'onStorageChange', storageKey: parsed.key, payload: [atob(parsed.value)] });
          } catch (error) { console.error('[dev-bridge] malformed storage event', error); }
        }
        return originalReceive.call(this, id, method, data);
      };
      window.jsBridge.receiveMessage = receive;
      restore.push(() => { if (window.jsBridge.receiveMessage === receive) window.jsBridge.receiveMessage = originalReceive; });
    }
  }
  return { latest, dispose: () => restore.reverse().forEach((fn) => fn()) };
}

export function createProvider({ target = 'h5portal', getSDK = () => window.WeSpaceSDK, bridgeEvents = [] } = {}) {
  const config = bridgeConfig(target);
  const sdk = getSDK();
  if (!sdk) throw new Error('Provider requires an initialized real SDK');
  let socket;
  let registered = false;
  let stopped = false;
  let reconnectTimer;
  const NativeWebSocket = window.WebSocket;
  const send = (message, connection = socket) => {
    if (connection?.readyState !== 1) return;
    try { assertJsonValue(message); connection.send(JSON.stringify(message)); }
    catch (error) { console.error('[dev-bridge] unsupported event/result', error); }
  };
  const events = observeSdkEvents(sdk, bridgeEvents, send);
  function connect() {
    if (stopped) return;
    const connection = new NativeWebSocket(config.url);
    socket = connection;
    connection.onopen = () => connection.send(JSON.stringify({ type: 'register', role: 'provider', target, session: config.session, token: config.token }));
    connection.onmessage = async ({ data }) => {
      let message;
      try { message = JSON.parse(data); } catch { return; }
      if (message.type === 'registered') { registered = true; return; }
      if (message.type === 'subscribe') {
        if (message.event === 'onVisibleChange' && events.latest.has(message.event)) send({ type: 'event', event: message.event, payload: events.latest.get(message.event) }, connection);
        // subscribeFileShare also asks the native host to start delivering events.
        if (message.event === 'subscribeFileShare' && typeof sdk.subscribeFileShare === 'function') {
          const slot = bridgeEvents.find((item) => item.method === message.event)?.handlerField;
          const callback = slot ? sdk[slot] : undefined;
          // The observer getter is already the relay: avoid storing it as its own callback.
          if (slot) void window.jsBridge?.invoke('subscribeFileShare');
          else if (callback) sdk.subscribeFileShare(callback);
        }
        return;
      }
      if (message.type !== 'invoke') return;
      try {
        if (typeof message.method !== 'string' || message.method.startsWith('__') || ['constructor', 'prototype', 'then'].includes(message.method) || !Array.isArray(message.args)) throw new Error('Invalid invocation');
        if (!Object.prototype.hasOwnProperty.call(sdk, message.method) || typeof sdk[message.method] !== 'function') throw new Error(`SDK.${message.method} is unavailable for ${target}`);
        if (bridgeEvents.some((item) => item.method === message.method)) throw new Error('Event subscriptions must use subscribe messages');
        const result = await sdk[message.method](...message.args);
        assertJsonValue(result, new Set(), 'result');
        send({ type: 'response', id: message.id, result }, connection);
      } catch (error) {
        send({ type: 'response', id: message.id, error: error.message || String(error), errorDetails: { name: error.name || 'Error', code: error.code } }, connection);
      }
    };
    connection.onclose = ({ code, reason }) => {
      if (socket !== connection) return;
      registered = false;
      // Replacement is deliberate; reconnecting would replace the new provider again.
      if (!stopped && code !== 1008 && String(reason) !== 'provider replaced') reconnectTimer = setTimeout(connect, 1000);
    };
    connection.onerror = () => {};
  }
  window.__DEV_BRIDGE__ = {
    getStatus: () => ({ role: 'provider', target, session: config.session, connected: registered, providerConnected: registered }),
    dispose: () => { stopped = true; clearTimeout(reconnectTimer); socket?.close(); events.dispose(); },
  };
  window.__bridgeProvider = window.__DEV_BRIDGE__;
  connect();
  return window.__DEV_BRIDGE__;
}
