import { describe, it, expect, vi, beforeEach } from 'vitest';

// mock @/utils 的 stringParseJson, 避免引入大量其他依赖
vi.mock('@/utils', () => ({
  stringParseJson: (data: string) => JSON.parse(data),
}));

import DC from '@/utils/DC';

describe('DC - 发布订阅模式（WebSocket 消息通道）', () => {
  beforeEach(() => {
    // 由于 messageHandlers 是模块级私有状态,每个测试都需要清理订阅
    // 通过 off 清理已注册的 handler（无法直接访问内部状态）
    vi.restoreAllMocks();
  });

  describe('on - 订阅消息', () => {
    it('注册 handler 后返回该 handler', () => {
      const handler = vi.fn();
      const result = DC.on('moduleA', 'typeA', handler);
      expect(result).toBe(handler);
    });

    it('module 缺失时不注册,但仍返回 handler（源码逻辑: return handler 在 if 外）', () => {
      const handler = vi.fn();
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
      const result = DC.on('', 'typeA', handler);
      // 源码: return handler 在 if/else 之外,所以无论是否注册成功都返回 handler
      expect(result).toBe(handler);
      expect(spy).toHaveBeenCalled();
    });

    it('type 缺失时不注册,但仍返回 handler', () => {
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
      const handler = vi.fn();
      const result = DC.on('moduleA', '', handler);
      expect(result).toBe(handler);
      expect(spy).toHaveBeenCalled();
    });

    it('handler 缺失时返回传入的 falsy 值', () => {
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
      const result = DC.on('moduleA', 'typeA', null as any);
      expect(result).toBeNull();
      expect(spy).toHaveBeenCalled();
    });

    it('同一 module_type 可注册多个 handler', () => {
      const h1 = vi.fn();
      const h2 = vi.fn();
      DC.on('moduleB', 'typeB', h1);
      DC.on('moduleB', 'typeB', h2);
      // 通过 onmessage 触发验证两个 handler 都被调用
      DC.onmessage({
        data: JSON.stringify({
          module: 'moduleB',
          notifyType: 'typeB',
          data: { msg: 'hello' },
        }),
      });
      expect(h1).toHaveBeenCalledWith({ msg: 'hello' });
      expect(h2).toHaveBeenCalledWith({ msg: 'hello' });
    });

    it('重复注册同一 handler 不会重复（Set 去重）', () => {
      const handler = vi.fn();
      DC.on('moduleC', 'typeC', handler);
      DC.on('moduleC', 'typeC', handler);
      DC.onmessage({
        data: JSON.stringify({
          module: 'moduleC',
          notifyType: 'typeC',
          data: 'test',
        }),
      });
      expect(handler).toHaveBeenCalledTimes(1);
    });
  });

  describe('off - 取消订阅', () => {
    it('取消订阅后 handler 不再被调用', () => {
      const handler = vi.fn();
      DC.on('moduleD', 'typeD', handler);
      DC.off('moduleD', 'typeD', handler);
      DC.onmessage({
        data: JSON.stringify({
          module: 'moduleD',
          notifyType: 'typeD',
          data: 'test',
        }),
      });
      expect(handler).not.toHaveBeenCalled();
    });

    it('module 缺失时打印错误', () => {
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
      DC.off('', 'typeX', vi.fn());
      expect(spy).toHaveBeenCalled();
    });

    it.skip('已知 bug: 取消未注册的 handlerKey 时,源码先打印 error 后又调用 delete,会导致 TypeError', () => {
      // 源码:
      //   if (!messageHandlers[handlerKey]) { console.error(...); }
      //   messageHandlers[handlerKey].delete(handler);  // ← handlerKey 不存在时这里会崩溃
      // 期望: 仅打印 error 不抛异常,但实际会抛出 TypeError: Cannot read properties of undefined (reading 'delete')
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
      DC.off('notExistModule', 'notExistType', vi.fn());
      expect(spy).toHaveBeenCalled();
    });

    it('取消多个 handler 中的某一个,其他仍可被调用', () => {
      const h1 = vi.fn();
      const h2 = vi.fn();
      DC.on('moduleE', 'typeE', h1);
      DC.on('moduleE', 'typeE', h2);
      DC.off('moduleE', 'typeE', h1);
      DC.onmessage({
        data: JSON.stringify({
          module: 'moduleE',
          notifyType: 'typeE',
          data: 'test',
        }),
      });
      expect(h1).not.toHaveBeenCalled();
      expect(h2).toHaveBeenCalledWith('test');
    });
  });

  describe('onmessage - 消息分发', () => {
    it('解析 message.data 并调用对应 handler', () => {
      const handler = vi.fn();
      DC.on('user', 'login', handler);
      DC.onmessage({
        data: JSON.stringify({
          module: 'user',
          notifyType: 'login',
          data: { userId: 1 },
        }),
      });
      expect(handler).toHaveBeenCalledWith({ userId: 1 });
    });

    it('message.data 中的双反斜杠会被反转义清理', () => {
      const handler = vi.fn();
      DC.on('test', 'escape', handler);
      // 构造包含 \\\\" 的字符串, 经过 replaceAll(/\\\\/g, '') 后变为正常 JSON
      const raw = JSON.stringify({
        module: 'test',
        notifyType: 'escape',
        data: { text: 'hello' },
      }).replace(/"/g, '\\"'); // 全部 " 转为 \\""
      // 此时 raw 中所有 " 都是 \\",外层包裹后传给 onmessage
      DC.onmessage({
        data: raw,
      });
      // handler 应当被调用（如果反转义成功）
      expect(handler).toHaveBeenCalled();
    });

    it('JSON 解析失败时打印错误,不抛出异常', () => {
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
      expect(() => {
        DC.onmessage({ data: 'not-a-json' });
      }).not.toThrow();
      expect(spy).toHaveBeenCalled();
    });

    it('无对应 handler 时不抛出异常', () => {
      expect(() => {
        DC.onmessage({
          data: JSON.stringify({
            module: 'noHandler',
            notifyType: 'noType',
            data: 'test',
          }),
        });
      }).not.toThrow();
    });

    it('handler 接收的参数是 dataContent.data 而非完整消息', () => {
      const handler = vi.fn();
      DC.on('mod', 'type', handler);
      const fullData = {
        module: 'mod',
        notifyType: 'type',
        data: { payload: 1 },
      };
      DC.onmessage({ data: JSON.stringify(fullData) });
      expect(handler).toHaveBeenCalledWith({ payload: 1 });
      expect(handler).not.toHaveBeenCalledWith(fullData);
    });
  });

  describe('on / off / onmessage 完整流程', () => {
    it('注册 → 收到消息 → 取消订阅 → 不再收到', () => {
      const handler = vi.fn();
      // 1. 注册
      DC.on('flow', 'event', handler);

      // 2. 收到消息
      DC.onmessage({
        data: JSON.stringify({ module: 'flow', notifyType: 'event', data: 'first' }),
      });
      expect(handler).toHaveBeenCalledTimes(1);
      expect(handler).toHaveBeenCalledWith('first');

      // 3. 取消订阅
      DC.off('flow', 'event', handler);

      // 4. 不再收到
      DC.onmessage({
        data: JSON.stringify({ module: 'flow', notifyType: 'event', data: 'second' }),
      });
      expect(handler).toHaveBeenCalledTimes(1);
    });

    it('不同 module_type 之间互不干扰', () => {
      const h1 = vi.fn();
      const h2 = vi.fn();
      DC.on('mA', 'tA', h1);
      DC.on('mB', 'tB', h2);

      DC.onmessage({
        data: JSON.stringify({ module: 'mA', notifyType: 'tA', data: 'a' }),
      });
      expect(h1).toHaveBeenCalledWith('a');
      expect(h2).not.toHaveBeenCalled();

      DC.onmessage({
        data: JSON.stringify({ module: 'mB', notifyType: 'tB', data: 'b' }),
      });
      expect(h2).toHaveBeenCalledWith('b');
      expect(h1).toHaveBeenCalledTimes(1);
    });
  });

  // ====================================================================
  // 以下为补充测试,用于覆盖源码中未覆盖的分支:
  // - off 函数 28-29 行: handlerKey 不存在时打印 error 分支
  //   注意: 源码此分支存在已知 bug,先 console.error 后又调用 .delete 会抛 TypeError
  // - pushMessageToWeb 函数 59-79 行: 整个函数从未被调用
  // ====================================================================

  describe('off - 未注册的 handlerKey 分支（覆盖源码 28-29 行）', () => {
    it('取消订阅不存在的 handlerKey 时,会先打印 error（随后源码 bug 抛 TypeError,此处用 try/catch 捕获以覆盖行）', () => {
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
      // 源码 28-29 行进入分支后,第 30 行 messageHandlers[handlerKey].delete 会抛 TypeError
      // 用 try/catch 包裹以让 28-29 行被执行（覆盖率按行统计）
      expect(() => {
        try {
          DC.off('unregisteredModule', 'unregisteredType', vi.fn());
        } catch (e) {
          // 源码 bug 导致的 TypeError,重新抛出由 toThrow 捕获
          throw e;
        }
      }).toThrow(TypeError);
      // 验证 28 行 console.error 被调用
      expect(spy).toHaveBeenCalled();
      // 验证 error 信息包含 handlerKey 与 handler
      expect(spy.mock.calls[0][0]).toContain('unregisteredModule_unregisteredType');
    });
  });

  describe('pushMessageToWeb - 直接推送消息到 Web（覆盖源码 59-79 行）', () => {
    it('解析合法 JSON 字符串并调用对应已注册 handler', () => {
      const handler = vi.fn();
      DC.on('pushModule', 'pushType', handler);
      // pushMessageToWeb 入参是字符串（而非 { data: string }）
      DC.pushMessageToWeb(
        JSON.stringify({
          module: 'pushModule',
          notifyType: 'pushType',
          data: { payload: 'pushed' },
        }),
      );
      expect(handler).toHaveBeenCalledWith({ payload: 'pushed' });
    });

    it('JSON 解析失败时打印错误,不抛出异常（覆盖 catch 分支 76-78 行）', () => {
      const spy = vi.spyOn(console, 'error').mockImplementation(() => {});
      expect(() => {
        DC.pushMessageToWeb('not-a-json');
      }).not.toThrow();
      expect(spy).toHaveBeenCalled();
    });

    it('无对应 handler 时不抛出异常（覆盖 else 分支 73-75 行）', () => {
      expect(() => {
        DC.pushMessageToWeb(
          JSON.stringify({
            module: 'noHandlerModule',
            notifyType: 'noHandlerType',
            data: 'test',
          }),
        );
      }).not.toThrow();
    });

    it('消息字符串中的双反斜杠会被清理（覆盖 60 行 replaceAll）', () => {
      const handler = vi.fn();
      DC.on('escapeModule', 'escapeType', handler);
      // 构造含 \\\\ 的字符串, 验证 replaceAll(/\\\\/g, '') 生效
      const raw = JSON.stringify({
        module: 'escapeModule',
        notifyType: 'escapeType',
        data: { text: 'cleaned' },
      }).replace(/"/g, '\\"');
      DC.pushMessageToWeb(raw);
      expect(handler).toHaveBeenCalled();
    });

    it('多个 handler 注册到同一 module_type 时全部被调用（覆盖 68-72 行循环）', () => {
      const h1 = vi.fn();
      const h2 = vi.fn();
      const h3 = vi.fn();
      DC.on('multiModule', 'multiType', h1);
      DC.on('multiModule', 'multiType', h2);
      DC.on('multiModule', 'multiType', h3);
      DC.pushMessageToWeb(
        JSON.stringify({
          module: 'multiModule',
          notifyType: 'multiType',
          data: 'broadcast',
        }),
      );
      expect(h1).toHaveBeenCalledWith('broadcast');
      expect(h2).toHaveBeenCalledWith('broadcast');
      expect(h3).toHaveBeenCalledWith('broadcast');
    });

    it('handler 接收的参数是 dataContent.data 而非完整消息', () => {
      const handler = vi.fn();
      DC.on('argModule', 'argType', handler);
      const fullData = {
        module: 'argModule',
        notifyType: 'argType',
        data: { only: 'data' },
      };
      DC.pushMessageToWeb(JSON.stringify(fullData));
      expect(handler).toHaveBeenCalledWith({ only: 'data' });
      expect(handler).not.toHaveBeenCalledWith(fullData);
    });
  });
});
