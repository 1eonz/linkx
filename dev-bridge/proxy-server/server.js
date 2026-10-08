/**
 * SDK 代理桥 —— 代理服务（工具无关）
 *
 * 职责: 转发 stub ↔ provider 的 SDK 调用与事件，是本地浏览器（联调/自动化）
 *      与宿主真实 SDK 之间的唯一中转。
 *
 * 端口（默认，可用环境变量覆盖）:
 *   - h5portal: WS 8787 / HTTP 健康检查 8788
 *   - 新端接入按 8x87/8x88 规律错开端口段（见 README 多端并行说明）
 *
 * 环境变量:
 *   BRIDGE_WS_PORT   WebSocket 服务端口（默认 8787）
 *   BRIDGE_HTTP_PORT HTTP 健康检查端口（默认 8788）
 */
import http from 'node:http';
import { WebSocketServer } from 'ws';

const WS_PORT = process.env.BRIDGE_WS_PORT || 8787;
const HTTP_PORT = process.env.BRIDGE_HTTP_PORT || 8788;

let provider = null;
const pendingCalls = new Map();
const eventSubscribers = new Set();
let callIdSeq = 0;

// 显式监听 0.0.0.0（IPv4）确保 localhost / 127.0.0.1 / 局域网 IP 都能连上
// 默认 ws 库只监听 :: (IPv6 任意地址)，浏览器 WebSocket 连 localhost 经常解析到 IPv4 而失败
const wss = new WebSocketServer({ host: '0.0.0.0', port: WS_PORT });
console.log(`[bridge proxy] WebSocket server listening on 0.0.0.0:${WS_PORT}`);

// 广播 provider 在线状态（stub 端据此门控调用: 宿主未连时不白发请求，
// 先开页面后开宿主的场景下，挂起的调用会在 provider 上线时自动放行）
function broadcastProviderStatus() {
  const payload = JSON.stringify({ type: 'providerStatus', connected: !!provider });
  wss.clients.forEach((c) => {
    if (c.readyState === c.OPEN) {
      try {
        c.send(payload);
      } catch {
        /* ignore */
      }
    }
  });
}

wss.on('connection', (ws, req) => {
  const role = req.headers['x-role'] || 'unknown';
  console.log(`[bridge proxy] ${role} connected from ${req.socket.remoteAddress}`);

  ws.on('message', (raw) => {
    let msg;
    try {
      msg = JSON.parse(raw.toString());
    } catch {
      return ws.send(JSON.stringify({ type: 'error', error: 'invalid json' }));
    }

    // 1. provider 注册自己
    if (msg.type === 'register' && msg.role === 'provider') {
      if (provider) {
        try {
          provider.close();
        } catch {
          /* ignore */
        }
      }
      provider = ws;
      console.log('[bridge proxy] provider registered, SDK ready');
      ws.on('close', () => {
        if (provider === ws) {
          provider = null;
          broadcastProviderStatus();
        }
        for (const [, p] of pendingCalls) p.reject(new Error('provider disconnected'));
        pendingCalls.clear();
        console.log('[bridge proxy] provider disconnected, all pending calls failed');
      });
      broadcastProviderStatus();
      return;
    }

    // 2. stub 发起 SDK 调用 → 转发给 provider
    if (msg.type === 'invoke') {
      if (!provider || provider.readyState !== ws.OPEN) {
        return ws.send(
          JSON.stringify({
            type: 'response',
            id: msg.id,
            error: 'provider not connected',
          }),
        );
      }
      const callId = `c${++callIdSeq}`;
      const timer = setTimeout(() => {
        if (pendingCalls.has(callId)) {
          pendingCalls.get(callId).reject(new Error(`timeout: ${msg.method}`));
          pendingCalls.delete(callId);
        }
      }, 30000);

      pendingCalls.set(callId, {
        method: msg.method,
        stubWs: ws,
        resolve: (result) => {
          clearTimeout(timer);
          if (ws.readyState === ws.OPEN) {
            ws.send(JSON.stringify({ type: 'response', id: msg.id, result }));
          }
        },
        reject: (err) => {
          clearTimeout(timer);
          if (ws.readyState === ws.OPEN) {
            ws.send(
              JSON.stringify({
                type: 'response',
                id: msg.id,
                error: err.message,
              }),
            );
          }
        },
      });

      provider.send(
        JSON.stringify({ type: 'invoke', id: callId, method: msg.method, data: msg.data }),
      );
      console.log(`[bridge proxy] invoke ${msg.method} -> ${callId}`);
      return;
    }

    // 3. provider 返回结果 → 转回 stub
    if (msg.type === 'response' && msg.id) {
      const p = pendingCalls.get(msg.id);
      if (!p) return;
      pendingCalls.delete(msg.id);
      if (msg.error) {
        p.reject(new Error(msg.error));
        console.log(`[bridge proxy] response ${msg.id} ERROR: ${msg.error}`);
      } else {
        p.resolve(msg.result);
        console.log(`[bridge proxy] response ${msg.id} OK (${p.method})`);
      }
      return;
    }

    // 4. provider 推送事件 → 广播给所有订阅的 stub
    if (msg.type === 'event') {
      const payload = JSON.stringify({
        type: 'event',
        event: msg.event,
        payload: msg.payload,
        // onStorageChange 特有: 带 storageKey 让 stub 按 key 路由
        storageKey: msg.storageKey,
      });
      let count = 0;
      eventSubscribers.forEach((c) => {
        if (c.readyState === ws.OPEN) {
          c.send(payload);
          count++;
        }
      });
      console.log(`[bridge proxy] event ${msg.event} broadcast to ${count} stubs`);
      return;
    }

    // 5. stub 订阅事件（附带当前 provider 状态快照，供新接入的 stub 门控）
    if (msg.type === 'subscribe') {
      eventSubscribers.add(ws);
      ws.on('close', () => eventSubscribers.delete(ws));
      ws.send(JSON.stringify({ type: 'providerStatus', connected: !!provider }));
      console.log('[bridge proxy] stub subscribed events');
      return;
    }
  });

  ws.on('error', (err) => console.error('[bridge proxy] socket error:', err.message));
});

// HTTP 健康检查
const httpServer = http.createServer((req, res) => {
  // CORS 头，方便浏览器端 fetch 检测代理状态
  res.writeHead(200, {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
  });
  if (req.method === 'OPTIONS') {
    res.end();
    return;
  }
  res.end(
    JSON.stringify({
      ok: true,
      providerConnected: !!provider,
      pendingCalls: pendingCalls.size,
      subscribers: eventSubscribers.size,
      uptime: process.uptime(),
    }),
  );
});
httpServer.listen(HTTP_PORT, '0.0.0.0', () =>
  console.log(`[bridge proxy] HTTP health check on 0.0.0.0:${HTTP_PORT}`),
);

function shutdown() {
  console.log('[bridge proxy] shutting down...');
  wss.clients.forEach((c) => {
    try {
      c.close();
    } catch {
      /* ignore */
    }
  });
  wss.close();
  httpServer.close();
  process.exit(0);
}
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
