import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import { getTimeRange } from '@/utils/time.js';

describe('utils/time.js - getTimeRange', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-07-27T10:00:00'));
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  describe('7days - 近 7 天', () => {
    it('应返回从 6 天前到今天的起止时间', () => {
      const result = getTimeRange('7days');
      expect(result.startTime).toBe('2026-07-21 00:00:00');
      expect(result.endTime).toBe('2026-07-27 23:59:59');
    });

    it('月初跨月时也应正确计算', () => {
      vi.setSystemTime(new Date('2026-07-02T08:00:00'));
      const result = getTimeRange('7days');
      // 7月2日 - 6天 = 6月26日
      expect(result.startTime).toBe('2026-06-26 00:00:00');
      expect(result.endTime).toBe('2026-07-02 23:59:59');
    });
  });

  describe('month - 本月', () => {
    it('应返回当月起止时间', () => {
      const result = getTimeRange('month');
      expect(result.startTime).toBe('2026-07-01 00:00:00');
      expect(result.endTime).toBe('2026-07-31 23:59:59');
    });

    it('2 月应正确处理平年 28 天', () => {
      vi.setSystemTime(new Date('2025-02-15T10:00:00'));
      const result = getTimeRange('month');
      expect(result.startTime).toBe('2025-02-01 00:00:00');
      expect(result.endTime).toBe('2025-02-28 23:59:59');
    });

    it('2 月应正确处理闰年 29 天', () => {
      vi.setSystemTime(new Date('2024-02-15T10:00:00'));
      const result = getTimeRange('month');
      expect(result.startTime).toBe('2024-02-01 00:00:00');
      expect(result.endTime).toBe('2024-02-29 23:59:59');
    });

    it('12 月应正确处理 31 天', () => {
      vi.setSystemTime(new Date('2026-12-15T10:00:00'));
      const result = getTimeRange('month');
      expect(result.startTime).toBe('2026-12-01 00:00:00');
      expect(result.endTime).toBe('2026-12-31 23:59:59');
    });
  });

  describe('year - 本年', () => {
    it('应返回当年起止时间', () => {
      const result = getTimeRange('year');
      expect(result.startTime).toBe('2026-01-01 00:00:00');
      expect(result.endTime).toBe('2026-12-31 23:59:59');
    });
  });

  describe('非法入参', () => {
    it('传入不支持的范围类型应抛错', () => {
      expect(() => getTimeRange('1day')).toThrow(/不支持的时间范围类型/);
      expect(() => getTimeRange('1day')).toThrow(/1day/);
    });

    it('传入 undefined 应抛错', () => {
      expect(() => getTimeRange(undefined)).toThrow(/不支持的时间范围类型/);
    });

    it('传入 null 应抛错', () => {
      expect(() => getTimeRange(null)).toThrow(/不支持的时间范围类型/);
    });

    it('传入空字符串应抛错', () => {
      expect(() => getTimeRange('')).toThrow(/不支持的时间范围类型/);
    });
  });

  describe('返回值结构', () => {
    it('返回对象应包含 startTime 和 endTime 两个字符串字段', () => {
      const result = getTimeRange('7days');
      expect(result).toHaveProperty('startTime');
      expect(result).toHaveProperty('endTime');
      expect(typeof result.startTime).toBe('string');
      expect(typeof result.endTime).toBe('string');
    });

    it('时间格式应为 YYYY-MM-DD HH:mm:ss', () => {
      const result = getTimeRange('month');
      expect(result.startTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
      expect(result.endTime).toMatch(/^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$/);
    });
  });
});
