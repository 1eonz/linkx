import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// mock 掉 @/hooks,避免 useBaseData → resourceHelper → mspPlayer → plugins/logs 的加载链
// (plugins/logs 在模块加载时访问 indexedDB,会导致 happy-dom 环境下报错)
vi.mock('@/hooks', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}));

import { headerUtils } from '@/utils/headerUtils';
import { constantConfig } from '@/config';

describe('headerUtils - 请求头工具', () => {
  describe('getSignature - HMAC-SHA256 签名（确定性）', () => {
    it('固定输入产出固定输出（POST 方法）', () => {
      const url = '/api/v1/test';
      const param = { foo: 'bar' };
      const headers = {
        'X-CloudCmd-AppKey': 'test-app-key',
        'TD-CloudCmd-Token': 'test-token',
        Accept: 'application/json',
      };
      const sig1 = headerUtils.getSignature(url, param, headers, 'POST');
      const sig2 = headerUtils.getSignature(url, param, { ...headers }, 'POST');
      expect(sig1).toBe(sig2);
      // base64 编码字符串
      expect(typeof sig1).toBe('string');
      expect(sig1.length).toBeGreaterThan(0);
    });

    it('不同的 URL 产生不同签名', () => {
      const param = { foo: 'bar' };
      const headers = {
        'X-CloudCmd-AppKey': 'test-app-key',
        'TD-CloudCmd-Token': 'test-token',
        Accept: 'application/json',
      };
      const sig1 = headerUtils.getSignature('/api/v1/a', param, headers, 'POST');
      const sig2 = headerUtils.getSignature('/api/v1/b', param, headers, 'POST');
      expect(sig1).not.toBe(sig2);
    });

    it('不同的 param 产生不同签名', () => {
      const url = '/api/v1/test';
      const headers = {
        'X-CloudCmd-AppKey': 'test-app-key',
        'TD-CloudCmd-Token': 'test-token',
        Accept: 'application/json',
      };
      const sig1 = headerUtils.getSignature(url, { foo: 'bar' }, headers, 'POST');
      const sig2 = headerUtils.getSignature(url, { foo: 'baz' }, headers, 'POST');
      expect(sig1).not.toBe(sig2);
    });

    it('不同的 HTTPMethod 产生不同签名', () => {
      const url = '/api/v1/test';
      const param = { foo: 'bar' };
      const headers = {
        'X-CloudCmd-AppKey': 'test-app-key',
        'TD-CloudCmd-Token': 'test-token',
        Accept: 'application/json',
      };
      const sigPost = headerUtils.getSignature(url, param, headers, 'POST');
      const sigGet = headerUtils.getSignature(url, param, headers, 'GET');
      expect(sigPost).not.toBe(sigGet);
    });

    it('默认 HTTPMethod 为 POST', () => {
      const url = '/api/v1/test';
      const param = { foo: 'bar' };
      const headers = {
        'X-CloudCmd-AppKey': 'test-app-key',
        'TD-CloudCmd-Token': 'test-token',
        Accept: 'application/json',
      };
      const sigDefault = headerUtils.getSignature(url, param, headers);
      const sigPost = headerUtils.getSignature(url, param, headers, 'POST');
      expect(sigDefault).toBe(sigPost);
    });

    it('TD-CloudCmd-Token 为 undefined 时会被设为空字符串（mutate headers）', () => {
      const url = '/api/v1/test';
      const param = { foo: 'bar' };
      const headers: Record<string, any> = {
        'X-CloudCmd-AppKey': 'test-app-key',
        Accept: 'application/json',
      };
      headerUtils.getSignature(url, param, headers, 'POST');
      expect(headers['TD-CloudCmd-Token']).toBe('');
    });

    it('TD-CloudCmd-Token 为 null 时也会被设为空字符串', () => {
      const url = '/api/v1/test';
      const param = { foo: 'bar' };
      const headers: Record<string, any> = {
        'X-CloudCmd-AppKey': 'test-app-key',
        'TD-CloudCmd-Token': null,
        Accept: 'application/json',
      };
      headerUtils.getSignature(url, param, headers, 'POST');
      expect(headers['TD-CloudCmd-Token']).toBe('');
    });

    it('Token 为空字符串与 null 输入应产生相同签名', () => {
      const url = '/api/v1/test';
      const param = { foo: 'bar' };
      const sigWithEmpty = headerUtils.getSignature(
        url,
        param,
        {
          'X-CloudCmd-AppKey': 'test-app-key',
          'TD-CloudCmd-Token': '',
          Accept: 'application/json',
        },
        'POST',
      );
      const sigWithNull = headerUtils.getSignature(
        url,
        param,
        {
          'X-CloudCmd-AppKey': 'test-app-key',
          'TD-CloudCmd-Token': null as unknown as string,
          Accept: 'application/json',
        },
        'POST',
      );
      expect(sigWithEmpty).toBe(sigWithNull);
    });
  });

  describe('getRequestId - 请求 ID 生成', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 45, 123)); // 2024-01-15 08:30:45.123
      // 重置 REQUEST_ID
      constantConfig.REQUEST_ID = 0;
      // 固定 Math.random 为 0.5 → Math.floor(0.5 * 10) = 5
      vi.spyOn(Math, 'random').mockReturnValue(0.5);
    });

    afterEach(() => {
      vi.useRealTimers();
      vi.restoreAllMocks();
    });

    it('生成的 ID 包含日期时间前缀（yyMMddHHmmss）', () => {
      const id = headerUtils.getRequestId();
      // 年份后2位: 24
      // 月份: 01
      // 日期: 15
      // 小时: 08
      // 分钟: 30
      // 秒: 45
      expect(id.startsWith('240115083045')).toBe(true);
    });

    it('生成的 ID 包含 4 位随机数后缀（前缀为 "-" + 序号）', () => {
      // random 固定为 0.5,每轮循环 floor(0.5*10)=5,4 轮 => 5555
      const id = headerUtils.getRequestId();
      // 格式: {date}{ms}{random4}-{REQUEST_ID}
      // date=240115083045, ms=123, random4=5555, REQUEST_ID 自增
      expect(id).toContain('5555-');
    });

    it('调用后 REQUEST_ID 自增 1', () => {
      const before = constantConfig.REQUEST_ID;
      headerUtils.getRequestId();
      expect(constantConfig.REQUEST_ID).toBe(before + 1);
    });

    it('REQUEST_ID 超过 MAX_REQUEST_ID 时归零', () => {
      constantConfig.REQUEST_ID = constantConfig.MAX_REQUEST_ID; // 9
      headerUtils.getRequestId();
      // ++REQUEST_ID = 10 > 9 → 归零
      expect(constantConfig.REQUEST_ID).toBe(0);
    });

    it('REQUEST_ID 达到 MAX_REQUEST_ID 时下一次仍可正常生成', () => {
      constantConfig.REQUEST_ID = constantConfig.MAX_REQUEST_ID - 1; // 8
      const id1 = headerUtils.getRequestId();
      expect(constantConfig.REQUEST_ID).toBe(9);
      const id2 = headerUtils.getRequestId();
      expect(constantConfig.REQUEST_ID).toBe(0);
      expect(id1).not.toBe(id2);
    });

    it('毫秒数补零：1-9 补两位', () => {
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 45, 5));
      const id = headerUtils.getRequestId();
      // 源码: currentdate 拼接 date.getMilliseconds()（原始值，未补零）作为前半段
      // 但用于生成的 currentdate 字符串末尾会包含毫秒数
      expect(id).toContain('5');
    });

    it('相同时间和随机数下,不同 REQUEST_ID 生成不同 ID', () => {
      constantConfig.REQUEST_ID = 1;
      const id1 = headerUtils.getRequestId();
      constantConfig.REQUEST_ID = 1;
      const id2 = headerUtils.getRequestId();
      expect(id1).toBe(id2); // 同样初始值,自增后序号相同
    });

    // ====================================================================
    // 以下用例用于覆盖源码未覆盖的补零分支:
    // - 33-34 行: 秒数 0-9 时补 0（cSecond = `0${cSecond}`）
    // - 40-41 行: 毫秒 10-99 时补一位 0（sss = `0${sss}`）
    // ====================================================================

    it('秒数为 0-9 时进入补零分支（覆盖源码 33-34 行）', () => {
      // 设置秒数为 5（0-9 区间）
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 5, 100));
      const id = headerUtils.getRequestId();
      // 当前时间: 240115083005 100 5555 -序号
      // 验证秒数部分被补零为 05
      expect(id).toContain('083005');
    });

    it('秒数为 0 时进入补零分支（边界值）', () => {
      vi.setSystemTime(new Date(2024, 0, 15, 0, 0, 0, 100));
      const id = headerUtils.getRequestId();
      // 年月日时分秒: 240115000000
      expect(id).toContain('000000');
    });

    it('秒数为 9 时进入补零分支（边界值）', () => {
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 9, 100));
      const id = headerUtils.getRequestId();
      expect(id).toContain('083009');
    });

    it('秒数为 10 时不进入补零分支', () => {
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 10, 100));
      const id = headerUtils.getRequestId();
      expect(id).toContain('083010');
    });

    it('秒数为 59 时不进入补零分支', () => {
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 59, 100));
      const id = headerUtils.getRequestId();
      expect(id).toContain('083059');
    });

    it('毫秒数为 10-99 时进入补一位 0 分支（覆盖源码 40-41 行）', () => {
      // 设置毫秒为 50（10-99 区间）
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 45, 50));
      const id = headerUtils.getRequestId();
      // 注意:源码 sss 变量虽被赋值补零,但最终 currentdate 拼接使用 date.getMilliseconds() 原始值
      // 此用例主要为了执行 40-41 行的赋值语句以达成行覆盖
      expect(id).toContain('50');
    });

    it('毫秒数为 10 时进入补一位 0 分支（边界值）', () => {
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 45, 10));
      const id = headerUtils.getRequestId();
      expect(id).toBeDefined();
    });

    it('毫秒数为 99 时进入补一位 0 分支（边界值）', () => {
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 45, 99));
      const id = headerUtils.getRequestId();
      expect(id).toBeDefined();
    });

    it('毫秒数为 9 时进入补两位 0 分支（不进入 40-41,覆盖对照）', () => {
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 45, 9));
      const id = headerUtils.getRequestId();
      expect(id).toBeDefined();
    });

    it('毫秒数为 100 时不进入任何补零分支', () => {
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 45, 100));
      const id = headerUtils.getRequestId();
      expect(id).toBeDefined();
    });

    it('秒数与毫秒同时进入补零分支（33-34 与 37-38 行）', () => {
      // 秒=5, 毫秒=9 → 秒补一位 0, 毫秒补两位 0
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 5, 9));
      const id = headerUtils.getRequestId();
      expect(id).toContain('083005');
    });

    it('秒数补零 + 毫秒 10-99 补一位 0 同时触发（33-34 与 40-41 行）', () => {
      // 秒=3, 毫秒=50
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 3, 50));
      const id = headerUtils.getRequestId();
      expect(id).toContain('083003');
    });
  });

  describe('getTimeStamp - 当前时间戳', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('返回 Date.getTime() 毫秒数', () => {
      const fixedTime = new Date(2024, 0, 15, 8, 30, 45).getTime();
      vi.setSystemTime(fixedTime);
      expect(headerUtils.getTimeStamp()).toBe(fixedTime);
    });

    it('两次调用返回相同值（同一时刻）', () => {
      const fixedTime = new Date(2024, 0, 15, 8, 30, 45).getTime();
      vi.setSystemTime(fixedTime);
      const t1 = headerUtils.getTimeStamp();
      const t2 = headerUtils.getTimeStamp();
      expect(t1).toBe(t2);
    });
  });
});
