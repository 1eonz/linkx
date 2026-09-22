import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import { eventBus } from '@/utils/eventBus';

describe('utils/eventBus.js - EventBus', () => {
  beforeEach(() => {
    // 每个用例前清空所有事件，避免相互污染
    eventBus.events = {};
  });

  afterEach(() => {
    eventBus.events = {};
  });

  describe('on / emit', () => {
    it('on 注册 handler 后 emit 触发该 handler', () => {
      const handler = vi.fn();
      eventBus.on('test', handler);

      eventBus.emit('test', { a: 1 });

      expect(handler).toHaveBeenCalledTimes(1);
      expect(handler).toHaveBeenCalledWith({ a: 1 });
    });

    it('emit 透传字符串参数', () => {
      const handler = vi.fn();
      eventBus.on('test', handler);

      eventBus.emit('test', 'hello');

      expect(handler).toHaveBeenCalledWith('hello');
    });

    it('emit 不传 data 时 handler 收到 undefined', () => {
      const handler = vi.fn();
      eventBus.on('test', handler);

      eventBus.emit('test');

      expect(handler).toHaveBeenCalledTimes(1);
      expect(handler).toHaveBeenCalledWith(undefined);
    });

    it('多个 handler 注册同一事件时 emit 应全部触发', () => {
      const handler1 = vi.fn();
      const handler2 = vi.fn();
      const handler3 = vi.fn();

      eventBus.on('test', handler1);
      eventBus.on('test', handler2);
      eventBus.on('test', handler3);

      eventBus.emit('test', 'data');

      expect(handler1).toHaveBeenCalledWith('data');
      expect(handler2).toHaveBeenCalledWith('data');
      expect(handler3).toHaveBeenCalledWith('data');
      expect(handler1).toHaveBeenCalledTimes(1);
      expect(handler2).toHaveBeenCalledTimes(1);
      expect(handler3).toHaveBeenCalledTimes(1);
    });

    it('不同事件互不干扰', () => {
      const handlerA = vi.fn();
      const handlerB = vi.fn();

      eventBus.on('eventA', handlerA);
      eventBus.on('eventB', handlerB);

      eventBus.emit('eventA', 1);

      expect(handlerA).toHaveBeenCalledWith(1);
      expect(handlerB).not.toHaveBeenCalled();
    });

    it('同一 handler 多次 on 注册后 emit 触发多次', () => {
      const handler = vi.fn();
      eventBus.on('test', handler);
      eventBus.on('test', handler);

      eventBus.emit('test');

      expect(handler).toHaveBeenCalledTimes(2);
    });

    it('无 handler 时 emit 不报错', () => {
      expect(() => eventBus.emit('not-registered', 'data')).not.toThrow();
    });

    it('事件名重复 emit 多次 handler 应触发多次', () => {
      const handler = vi.fn();
      eventBus.on('test', handler);

      eventBus.emit('test', 1);
      eventBus.emit('test', 2);
      eventBus.emit('test', 3);

      expect(handler).toHaveBeenCalledTimes(3);
      expect(handler).toHaveBeenNthCalledWith(1, 1);
      expect(handler).toHaveBeenNthCalledWith(2, 2);
      expect(handler).toHaveBeenNthCalledWith(3, 3);
    });
  });

  describe('off', () => {
    it('off 取消指定 handler 后 emit 不触发该 handler', () => {
      const handler = vi.fn();
      eventBus.on('test', handler);

      eventBus.off('test', handler);
      eventBus.emit('test', 'data');

      expect(handler).not.toHaveBeenCalled();
    });

    it('off 仅取消指定 handler，其他 handler 不受影响', () => {
      const handler1 = vi.fn();
      const handler2 = vi.fn();

      eventBus.on('test', handler1);
      eventBus.on('test', handler2);

      eventBus.off('test', handler1);
      eventBus.emit('test', 'data');

      expect(handler1).not.toHaveBeenCalled();
      expect(handler2).toHaveBeenCalledWith('data');
    });

    it('off 未注册的 handler 不报错', () => {
      const handler = vi.fn();
      expect(() => eventBus.off('test', handler)).not.toThrow();
    });

    it('off 不存在的事件名不报错', () => {
      expect(() => eventBus.off('not-exist', () => {})).not.toThrow();
    });

    it('off 同一 handler 注册多次时按引用一次性全部移除', () => {
      const handler = vi.fn();
      eventBus.on('test', handler);
      eventBus.on('test', handler);

      eventBus.off('test', handler);
      eventBus.emit('test');

      // off 使用 filter 按引用移除，同一引用全部被过滤掉
      expect(handler).toHaveBeenCalledTimes(0);
    });

    it('off 后再次 on 注册相同 handler 仍可触发', () => {
      const handler = vi.fn();
      eventBus.on('test', handler);
      eventBus.off('test', handler);

      eventBus.on('test', handler);
      eventBus.emit('test', 'data');

      expect(handler).toHaveBeenCalledWith('data');
      expect(handler).toHaveBeenCalledTimes(1);
    });
  });

  describe('eventBus 实例', () => {
    it('eventBus 应为单例对象', () => {
      expect(eventBus).toBeDefined();
      expect(eventBus.events).toBeDefined();
    });

    it('初始 events 应为空对象', () => {
      expect(eventBus.events).toEqual({});
    });

    it('on 注册后 events 中应存在对应事件数组', () => {
      const handler = vi.fn();
      eventBus.on('new-event', handler);

      expect(Array.isArray(eventBus.events['new-event'])).toBe(true);
      expect(eventBus.events['new-event']).toContain(handler);
    });

    it('off 全部移除后事件数组应为空数组', () => {
      const handler = vi.fn();
      eventBus.on('test', handler);
      eventBus.off('test', handler);

      expect(eventBus.events['test']).toEqual([]);
    });
  });
});
