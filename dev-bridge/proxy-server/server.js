import http from 'node:http';
import { pathToFileURL } from 'node:url';
import { WebSocketServer, WebSocket } from 'ws';

export const TARGET_PORTS = {
  h5portal: [8787, 8788],
  'web-bspc': [8887, 8888],
  'web-cspc': [8987, 8988],
};

export function createBridgeServer({ target = 'h5portal', wsPort, httpPort, host = '0.0.0.0', timeoutMs = 30000, token = '' } = {}) {
  if (!TARGET_PORTS[target]) throw new Error(`Unknown target: ${target}`);
  const sessions = new Map();
  const pending = new Map();
  let sequence = 0;
  const wss = new WebSocketServer({ host, port: wsPort ?? TARGET_PORTS[target][0], maxPayload: 2 * 1024 * 1024 });
  const send = (ws, message) => { if (ws?.readyState === WebSocket.OPEN) ws.send(JSON.stringify(message)); };
  const sessionFor = (id) => {
    if (!sessions.has(id)) sessions.set(id, { provider: null, stubs: new Set() });
    return sessions.get(id);
  };
  const status = (session) => {
    for (const stub of session.stubs) send(stub, { type: 'providerStatus', connected: session.provider?.readyState === WebSocket.OPEN, target });
  };
  const finish = (id, response) => {
    const call = pending.get(id);
    if (!call) return;
    clearTimeout(call.timer);
    pending.delete(id);
    send(call.stub, { type: 'response', id: call.originalId, ...response });
  };
  const failProvider = (provider, error) => {
    for (const [id, call] of pending) if (call.provider === provider) finish(id, { error });
  };
  wss.on('connection', (ws) => {
    let role;
    let session;
    let sessionId;
    const registrationTimer = setTimeout(() => ws.close(1008, 'registration required'), 5000);
    ws.on('message', (raw) => {
      let msg;
      try { msg = JSON.parse(raw.toString()); } catch { send(ws, { type: 'error', error: 'invalid JSON' }); return; }
      if (!msg || typeof msg !== 'object') return;
      if (msg.type === 'register') {
        if (role || !['provider', 'stub'].includes(msg.role) || msg.target !== target || (token && msg.token !== token) || typeof msg.session !== 'string' || !/^[\w.-]{1,100}$/.test(msg.session)) {
          ws.close(1008, 'invalid registration'); return;
        }
        clearTimeout(registrationTimer);
        role = msg.role; sessionId = msg.session; session = sessionFor(sessionId);
        if (role === 'provider') {
          const previous = session.provider;
          session.provider = ws;
          if (previous) { failProvider(previous, 'provider replaced'); previous.close(1012, 'provider replaced'); }
        } else session.stubs.add(ws);
        send(ws, { type: 'registered', role, target, session: sessionId });
        status(session);
        return;
      }
      if (!role) { ws.close(1008, 'registration required'); return; }
      if (role === 'stub' && msg.type === 'invoke') {
        if (typeof msg.id !== 'string' || typeof msg.method !== 'string' || !Array.isArray(msg.args) || msg.method.startsWith('__') || ['constructor', 'prototype', 'then'].includes(msg.method)) {
          send(ws, { type: 'response', id: msg.id, error: 'invalid invocation' }); return;
        }
        const provider = session.provider;
        if (provider?.readyState !== WebSocket.OPEN) { send(ws, { type: 'response', id: msg.id, error: 'provider not connected' }); return; }
        const id = `c${++sequence}`;
        const timer = setTimeout(() => finish(id, { error: `timeout: ${msg.method}` }), timeoutMs);
        pending.set(id, { timer, stub: ws, provider, originalId: msg.id });
        send(provider, { type: 'invoke', id, method: msg.method, args: msg.args });
      } else if (role === 'provider' && session.provider === ws && msg.type === 'response') {
        if (pending.get(msg.id)?.provider === ws) finish(msg.id, msg.error ? { error: String(msg.error), errorDetails: msg.errorDetails } : { result: msg.result });
      } else if (role === 'provider' && session.provider === ws && msg.type === 'event' && typeof msg.event === 'string' && Array.isArray(msg.payload)) {
        for (const stub of session.stubs) send(stub, { type: 'event', event: msg.event, payload: msg.payload, storageKey: msg.storageKey });
      } else if (role === 'stub' && msg.type === 'subscribe') {
        send(session.provider, { type: 'subscribe', event: msg.event, storageKey: msg.storageKey });
      } else if (role === 'stub' && msg.type === 'cancel') {
        for (const [id, call] of pending) if (call.stub === ws && call.originalId === msg.id) { clearTimeout(call.timer); pending.delete(id); }
      }
    });
    ws.on('close', () => {
      clearTimeout(registrationTimer);
      if (!session) return;
      session.stubs.delete(ws);
      if (session.provider === ws) { session.provider = null; status(session); }
      failProvider(ws, 'provider disconnected');
      for (const [id, call] of pending) if (call.stub === ws) { clearTimeout(call.timer); pending.delete(id); }
      if (!session.provider && !session.stubs.size) sessions.delete(sessionId);
    });
    ws.on('error', () => {});
  });
  const healthServer = http.createServer((req, res) => {
    const url = new URL(req.url, 'http://localhost');
    const id = url.searchParams.get('session') || 'local';
    const session = sessions.get(id);
    res.writeHead(url.pathname === '/health' || url.pathname === '/' ? 200 : 404, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
    res.end(JSON.stringify({ ok: true, target, session: id, providerConnected: !!session && session.provider?.readyState === WebSocket.OPEN, pendingCalls: pending.size, subscribers: session?.stubs.size || 0 }));
  });
  healthServer.listen(httpPort ?? TARGET_PORTS[target][1], host);
  return {
    wss, healthServer,
    close: async () => {
      for (const call of pending.values()) clearTimeout(call.timer);
      pending.clear();
      for (const client of wss.clients) client.terminate();
      await Promise.all([new Promise((resolve) => wss.close(resolve)), new Promise((resolve) => healthServer.close(resolve))]);
    },
  };
}

if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const target = process.argv[2] || process.env.BRIDGE_TARGET || 'h5portal';
  const bridge = createBridgeServer({ target, wsPort: process.env.BRIDGE_WS_PORT ? Number(process.env.BRIDGE_WS_PORT) : undefined, httpPort: process.env.BRIDGE_HTTP_PORT ? Number(process.env.BRIDGE_HTTP_PORT) : undefined, token: process.env.BRIDGE_TOKEN || '' });
  console.log(`[dev-bridge] ${target}: WS ${process.env.BRIDGE_WS_PORT || TARGET_PORTS[target][0]}, health ${process.env.BRIDGE_HTTP_PORT || TARGET_PORTS[target][1]}`);
  const shutdown = async () => { await bridge.close(); process.exit(0); };
  process.on('SIGINT', shutdown);
  process.on('SIGTERM', shutdown);
}
