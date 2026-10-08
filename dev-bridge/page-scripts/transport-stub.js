import { assertJsonValue, bridgeConfig } from './config.js';

// Keeps the vendor SDK and its callback registration path intact, replacing only its
// message transport. Used by web-bspc and web-cspc, whose SDKs are module singletons.
export function installTransportStub({ target, getBridge = () => window.jsBridge, callTimeoutMs = 30000 } = {}) {
  const config = bridgeConfig(target);
  const pending = new Map();
  const subscriptions = [];
  let connection;
  let ready = false;
  let sequence = 0;
  let stopped = false;
  let reconnectTimer;
  const NativeWebSocket = window.WebSocket;
  const send = (message) => { if (ready && connection?.readyState === 1) connection.send(JSON.stringify(message)); };
  const flush = () => { if (ready) { subscriptions.forEach(send); for (const call of pending.values()) if (!call.sent) { call.sent = true; send(call.message); } } };
  const finish = (id, error, result, details) => {
    const call = pending.get(id); if (!call) return;
    clearTimeout(call.timer); pending.delete(id);
    error ? call.reject(Object.assign(new Error(error), details || {})) : call.resolve(result);
  };
  function connect() {
    if (stopped) return;
    connection = new NativeWebSocket(config.url);
    connection.onopen = () => connection.send(JSON.stringify({ type: 'register', role: 'stub', target, session: config.session, token: config.token }));
    connection.onmessage = ({ data }) => {
      let msg; try { msg = JSON.parse(data); } catch { return; }
      if (msg.type === 'registered') ready = true;
      if (msg.type === 'providerStatus') { ready = !!msg.connected; flush(); }
      if (msg.type === 'response') finish(msg.id, msg.error, msg.result, msg.errorDetails);
      if (msg.type === 'event') {
        // These SDKs route host events through the same dispatcher they use in the real host.
        const bridge = getBridge();
        const receiveMethod = target === 'h5portal' ? (msg.event === 'onUserStatusChange' ? 'onStatusChange' : msg.event) : msg.event;
        try { bridge?.receiveMessage(`event-${Date.now()}-${sequence++}`, receiveMethod, JSON.stringify(msg.payload?.[0])); }
        catch (error) { console.error('[dev-bridge] host event delivery failed', receiveMethod, error); }
      }
    };
    connection.onclose = ({ code }) => {
      ready = false;
      for (const [id] of pending) finish(id, 'bridge disconnected');
      if (!stopped && code !== 1008) reconnectTimer = setTimeout(connect, 1000);
    };
    connection.onerror = () => {};
  }
  const bridge = getBridge();
  if (!bridge || typeof bridge.invoke !== 'function') throw new Error(`${target} SDK transport is not initialized`);
  const originalInvoke = bridge.invoke;
  bridge.invoke = function (method, ...args) {
    try { assertJsonValue(args, new Set(), 'arguments'); } catch (error) { return Promise.reject(error); }
    return new Promise((resolve, reject) => {
      const id = `r${++sequence}`;
      const timer = setTimeout(() => { send({ type: 'cancel', id }); finish(id, `bridge timeout: ${method}`); }, callTimeoutMs);
      pending.set(id, { resolve, reject, timer, sent: false, message: { type: 'invoke', id, method, args } }); flush();
    });
  };
  const api = {
    getStatus: () => ({ role: 'stub', target, session: config.session, connected: ready, providerConnected: ready }),
    dispose: () => { stopped = true; clearTimeout(reconnectTimer); connection?.close(); for (const [id] of pending) finish(id, 'bridge disposed'); bridge.invoke = originalInvoke; },
    subscribe: (event, storageKey) => { const message = { type: 'subscribe', event, storageKey }; subscriptions.push(message); send(message); },
  };
  window.__DEV_BRIDGE__ = api;
  window.__bridgeStub = api;
  connect();
  return api;
}
