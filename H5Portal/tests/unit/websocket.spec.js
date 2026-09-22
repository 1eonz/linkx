import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import { Message, MyWebSocket } from '@/utils/websocket';

describe('utils/websocket.js - Message', () => {
  it('应正确设置 event 与 data 字段', () => {
    const msg = new Message('ping', { time: 1 });

    expect(msg.event).toBe('ping');
    expect(msg.data).toEqual({ time: 1 });
  });

  it('不传 data 时 data 应为 undefined', () => {
    const msg = new Message('ping');

    expect(msg.event).toBe('ping');
    expect(msg.data).toBeUndefined();
  });

  it('应支持 JSON 序列化为预期结构', () => {
    const msg = new Message('pong', { token: 'abc' });

    expect(JSON.parse(JSON.stringify(msg))).toEqual({
      event: 'pong',
      data: { token: 'abc' },
    });
  });

  it('支持任意类型 data（数字、字符串、数组）', () => {
    expect(new Message('a', 123).data).toBe(123);
    expect(new Message('a', 'str').data).toBe('str');
    expect(new Message('a', [1, 2]).data).toEqual([1, 2]);
  });
});

describe('utils/websocket.js - MyWebSocket', () => {
  let mockSocketTask;
  let onOpenCallback;
  let onMessageCallback;
  let onErrorCallback;
  let onCloseCallback;

  beforeEach(() => {
    vi.useFakeTimers();

    onOpenCallback = null;
    onMessageCallback = null;
    onErrorCallback = null;
    onCloseCallback = null;

    mockSocketTask = {
      onOpen: vi.fn((cb) => {
        onOpenCallback = cb;
      }),
      onMessage: vi.fn((cb) => {
        onMessageCallback = cb;
      }),
      onError: vi.fn((cb) => {
        onErrorCallback = cb;
      }),
      onClose: vi.fn((cb) => {
        onCloseCallback = cb;
      }),
      send: vi.fn(({ success }) => {
        success && success();
      }),
      close: vi.fn((opts) => {
        // 模拟真实 close：先 success 再 complete
        opts && opts.success && opts.success();
        opts && opts.complete && opts.complete();
      }),
    };

    vi.stubGlobal('uni', {
      connectSocket: vi.fn(() => mockSocketTask),
    });
  });

  afterEach(() => {
    vi.useRealTimers();
    vi.unstubAllGlobals();
  });

  describe('constructor', () => {
    it('onMessage 为函数时正常构造', () => {
      const onMessage = vi.fn();
      const ws = new MyWebSocket(onMessage);

      expect(ws.onMessage).toBe(onMessage);
      expect(ws.connected).toBe(false);
      expect(ws.closeFlag).toBe(true);
      expect(ws.logInfo).toBe(true);
      expect(ws.reconnectTimes).toBe(0);
    });

    it('onMessage 非函数时应抛出错误', () => {
      expect(() => new MyWebSocket(null)).toThrow('onMessage 应该是一个函数');
      expect(() => new MyWebSocket('not-fn')).toThrow('onMessage 应该是一个函数');
      expect(() => new MyWebSocket({})).toThrow('onMessage 应该是一个函数');
    });

    it('logInfo 参数默认为 true，传入 false 时关闭日志', () => {
      const ws = new MyWebSocket(vi.fn(), false);
      expect(ws.logInfo).toBe(false);
    });
  });

  describe('init', () => {
    it('options.url 非字符串时应 reject', async () => {
      const ws = new MyWebSocket(vi.fn());
      const promise = ws.init({ url: 123 });

      await expect(promise).rejects.toBe('options.url 应该是一个字符串');
    });

    it('options.url 缺失时应 reject', async () => {
      const ws = new MyWebSocket(vi.fn());
      const promise = ws.init({});

      await expect(promise).rejects.toBe('options.url 应该是一个字符串');
    });

    it('调用 uni.connectSocket 创建 socketTask', async () => {
      const ws = new MyWebSocket(vi.fn());
      const options = { url: 'wss://example.com/ws' };

      const promise = ws.init(options);
      onOpenCallback && onOpenCallback('open');
      await promise;

      expect(uni.connectSocket).toHaveBeenCalledWith(options);
      expect(ws.socketTask).toBe(mockSocketTask);
      expect(ws.connected).toBe(true);
      expect(ws.closeFlag).toBe(false);
      expect(ws.reconnectTimes).toBe(0);
    });

    it('onOpen 触发后 resolve promise', async () => {
      const ws = new MyWebSocket(vi.fn());
      const promise = ws.init({ url: 'wss://example.com/ws' });

      onOpenCallback({ hello: 'world' });
      await expect(promise).resolves.toEqual({ hello: 'world' });
    });

    it('onMessage 收到字符串消息时调用 onMessage 回调', async () => {
      const onMessage = vi.fn();
      const ws = new MyWebSocket(onMessage);

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      onMessageCallback({ data: JSON.stringify({ event: 'msg', data: 'hello' }) });

      expect(onMessage).toHaveBeenCalledWith({ event: 'msg', data: 'hello' });
    });

    it('onMessage 收到 ping/pong 事件时不调用 onMessage', async () => {
      const onMessage = vi.fn();
      const ws = new MyWebSocket(onMessage);

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      onMessageCallback({ data: JSON.stringify({ event: 'ping' }) });
      onMessageCallback({ data: JSON.stringify({ event: 'pong' }) });

      expect(onMessage).not.toHaveBeenCalled();
    });

    it('onMessage 收到非法 JSON 时不抛错', async () => {
      const onMessage = vi.fn();
      const ws = new MyWebSocket(onMessage);

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      expect(() => onMessageCallback({ data: 'not-json' })).not.toThrow();
      expect(onMessage).not.toHaveBeenCalled();
    });

    it('onError 触发后 connected 应为 false', async () => {
      const ws = new MyWebSocket(vi.fn());

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      expect(ws.connected).toBe(true);

      onErrorCallback({ errMsg: 'error' });

      expect(ws.connected).toBe(false);
    });

    it('onClose 触发后 connected 应为 false', async () => {
      const ws = new MyWebSocket(vi.fn());

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      // closeFlag 为 true 时 reconnect 直接返回，不会触发重连
      onCloseCallback({ code: 1000, reason: 'normal' });

      expect(ws.connected).toBe(false);
    });

    it('options.complete 非函数时会被赋默认空函数', async () => {
      const ws = new MyWebSocket(vi.fn());

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      expect(typeof ws.options.complete).toBe('function');
    });
  });

  describe('send', () => {
    it('未连接时 send 应 reject', async () => {
      const ws = new MyWebSocket(vi.fn());

      await expect(ws.send(new Message('ping'))).rejects.toBe('WebSocket 连接未开启');
    });

    it('非 Message 实例（且 verifyFormat=true）应 reject', async () => {
      const ws = new MyWebSocket(vi.fn());
      ws.connected = true;
      ws.socketTask = mockSocketTask;

      await expect(ws.send({ event: 'x' })).rejects.toBe('消息格式错误');
    });

    it('verifyFormat=false 时允许任意消息', async () => {
      const ws = new MyWebSocket(vi.fn());
      ws.connected = true;
      ws.socketTask = mockSocketTask;

      await ws.send('raw-string', false);

      expect(mockSocketTask.send).toHaveBeenCalled();
      expect(mockSocketTask.send.mock.calls[0][0].data).toBe(JSON.stringify('raw-string'));
    });

    it('连接正常时 send 应调用 socketTask.send 并传入 JSON', async () => {
      const ws = new MyWebSocket(vi.fn());
      ws.connected = true;
      ws.socketTask = mockSocketTask;

      const msg = new Message('event', { a: 1 });
      await ws.send(msg);

      expect(mockSocketTask.send).toHaveBeenCalledWith(
        expect.objectContaining({ data: JSON.stringify(msg) }),
      );
    });
  });

  describe('close', () => {
    it('未连接时 close 应 reject', async () => {
      const ws = new MyWebSocket(vi.fn());

      await expect(ws.close()).rejects.toBe('WebSocket 连接未开启');
      expect(ws.closeFlag).toBe(true);
    });

    it('连接后 close 应调用 socketTask.close', async () => {
      const ws = new MyWebSocket(vi.fn());

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      await ws.close();

      expect(mockSocketTask.close).toHaveBeenCalled();
      expect(ws.connected).toBe(false);
      expect(ws.socketTask).toBeNull();
      expect(ws.closeFlag).toBe(true);
    });

    it('close 后应清除心跳定时器（推进时间不再触发 send）', async () => {
      const ws = new MyWebSocket(vi.fn());

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      const callsBefore = mockSocketTask.send.mock.calls.length;

      await ws.close();

      // 推进多个心跳间隔时间，close 后定时器已被 clearInterval，不会再触发 send
      vi.advanceTimersByTime(ws.heartbeatIntervalTime * 3);

      expect(mockSocketTask.send.mock.calls.length).toBe(callsBefore);
    });
  });

  describe('heartbeat', () => {
    it('init 成功后启动心跳定时器', async () => {
      const ws = new MyWebSocket(vi.fn());

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      expect(ws.heartbeatTimer).not.toBeNull();
    });

    it('到达心跳间隔时触发一次 send', async () => {
      const ws = new MyWebSocket(vi.fn());

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      // init 时已 send 一次（heartbeat 内）
      const initialCalls = mockSocketTask.send.mock.calls.length;

      // 推进一个心跳间隔
      vi.advanceTimersByTime(ws.heartbeatIntervalTime);

      expect(mockSocketTask.send.mock.calls.length).toBe(initialCalls + 1);
    });

    it('断连后心跳定时器触发 reconnect', async () => {
      const ws = new MyWebSocket(vi.fn());

      const promise = ws.init({ url: 'wss://example.com/ws' });
      onOpenCallback('open');
      await promise;

      // 模拟断连
      ws.connected = false;
      const reconnectSpy = vi.spyOn(ws, 'reconnect');

      vi.advanceTimersByTime(ws.heartbeatIntervalTime);

      expect(reconnectSpy).toHaveBeenCalled();
    });
  });

  describe('reconnect', () => {
    it('closeFlag=true 时不应发起重连', () => {
      const ws = new MyWebSocket(vi.fn());
      ws.closeFlag = true;

      ws.reconnect();

      expect(ws.reconnectTimer).toBeNull();
    });

    it('reconnectTimes 达到上限时不应发起重连', () => {
      const ws = new MyWebSocket(vi.fn());
      ws.closeFlag = false;
      ws.reconnectTimes = ws.reconnectMaxTimes;

      ws.reconnect();

      expect(ws.reconnectTimer).toBeNull();
    });

    it('满足条件时在 reconnectDelayTime 后发起 init 重连', async () => {
      const ws = new MyWebSocket(vi.fn());
      ws.closeFlag = false;
      ws.connected = false;
      ws.options = { url: 'wss://example.com/ws' };

      const initSpy = vi.spyOn(ws, 'init').mockResolvedValue('open');
      ws.reconnect();

      expect(ws.reconnectTimer).not.toBeNull();

      // 推进重连延迟时间
      await vi.advanceTimersByTimeAsync(ws.reconnectDelayTime);

      expect(initSpy).toHaveBeenCalledWith(
        { url: 'wss://example.com/ws' },
        'reconnect',
      );
      expect(ws.reconnectTimes).toBe(1);
    });
  });
});
