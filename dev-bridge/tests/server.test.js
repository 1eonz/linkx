import assert from 'node:assert/strict';
import { afterEach, test } from 'node:test';
import { WebSocket } from 'ws';
import { createBridgeServer } from '../proxy-server/server.js';

const servers = [];

async function start(timeoutMs = 500) {
  const bridge = createBridgeServer({ target: 'h5portal', wsPort: 0, httpPort: 0, host: '127.0.0.1', timeoutMs });
  servers.push(bridge);
  await waitFor(() => bridge.wss.address() && bridge.healthServer.address());
  return {
    bridge,
    connect: async (role, session) => {
      const ws = new WebSocket(`ws://127.0.0.1:${bridge.wss.address().port}`);
      await new Promise((resolve, reject) => { ws.once('open', resolve); ws.once('error', reject); });
      const messages = [];
      ws.on('message', (raw) => messages.push(JSON.parse(raw.toString())));
      ws.send(JSON.stringify({ type: 'register', role, target: 'h5portal', session }));
      await waitFor(() => messages.some((message) => message.type === 'registered'));
      return { ws, messages };
    },
  };
}

async function waitFor(check, timeoutMs = 1000) {
  const started = Date.now();
  while (!check()) {
    if (Date.now() - started > timeoutMs) throw new Error('Timed out waiting for bridge message');
    await new Promise((resolve) => setTimeout(resolve, 5));
  }
}

afterEach(async () => {
  await Promise.all(servers.splice(0).map((server) => server.close()));
});

test('routes full argument arrays and replies only to the requesting stub', async () => {
  const { bridge, connect } = await start();
  const healthPort = () => bridge.healthServer.address().port;
  const [provider, stubA, stubB] = await Promise.all([connect('provider', 'run-a'), connect('stub', 'run-a'), connect('stub', 'run-b')]);
  stubA.ws.send(JSON.stringify({ type: 'invoke', id: 'request-a', method: 'setStorage', args: ['key', 'value'] }));
  await waitFor(() => provider.messages.some((message) => message.type === 'invoke'));
  const call = provider.messages.find((message) => message.type === 'invoke');
  assert.deepEqual(call.args, ['key', 'value']);
  provider.ws.send(JSON.stringify({ type: 'response', id: call.id, result: { ok: true } }));
  await waitFor(() => stubA.messages.some((message) => message.type === 'response'));
  assert.deepEqual(stubA.messages.find((message) => message.type === 'response'), { type: 'response', id: 'request-a', result: { ok: true } });
  assert.equal(stubB.messages.some((message) => message.type === 'response'), false);
  const response = await fetch(`http://127.0.0.1:${healthPort()}/health?session=run-a`).then((res) => res.json());
  assert.equal(response.providerConnected, true);
  assert.equal(response.target, 'h5portal');
  provider.ws.close(); stubA.ws.close(); stubB.ws.close();
});

test('cleans calls and fails them when provider disconnects', async () => {
  const { connect } = await start();
  const [provider, stub] = await Promise.all([connect('provider', 'run'), connect('stub', 'run')]);
  stub.ws.send(JSON.stringify({ type: 'invoke', id: 'waiting', method: 'getUserInfo', args: [] }));
  await waitFor(() => provider.messages.some((message) => message.type === 'invoke'));
  provider.ws.close();
  await waitFor(() => stub.messages.some((message) => message.type === 'response'));
  assert.equal(stub.messages.find((message) => message.type === 'response').error, 'provider disconnected');
  stub.ws.close();
});

test('rejects registrations for another target and replaces providers without stale ownership', async () => {
  const { bridge, connect } = await start();
  const oldProvider = await connect('provider', 'run');
  const replacement = await connect('provider', 'run');
  await waitFor(() => oldProvider.ws.readyState === WebSocket.CLOSED);
  const status = await fetch(`http://127.0.0.1:${bridge.healthServer.address().port}/health?session=run`).then((res) => res.json());
  assert.equal(status.providerConnected, true);
  replacement.ws.close();
});
