import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// 使用 vi.hoisted 创建可在 mock 工厂和测试间共享的 mock 对象
const {
  mockCommunicationStore,
  mockApplicationStore,
  mockHeartbeatApi,
  mockHeartbeatLocation,
  mockGetGlobalsConfigByKey,
} = vi.hoisted(() => ({
  mockCommunicationStore: {
    getGisInfo: vi.fn(),
  },
  mockApplicationStore: {
    userInfo: { userid: 'user-001', isdn: '13800000000', username: '张三' },
  },
  mockHeartbeatApi: vi.fn(),
  mockHeartbeatLocation: vi.fn(),
  mockGetGlobalsConfigByKey: vi.fn(),
}));

// 必须在 import 目标模块前 mock 其依赖（模块顶层会调用 useCommunicationStore）
vi.mock('@/stores/communication.js', () => ({
  useCommunicationStore: () => mockCommunicationStore,
}));
vi.mock('@/stores/application.js', () => ({
  useApplicationStore: () => mockApplicationStore,
}));
vi.mock('@/common/api/xietong', () => ({
  heartbeatApi: mockHeartbeatApi,
  heartbeatLocation: mockHeartbeatLocation,
}));
vi.mock('@/common/utils', () => ({
  getGlobalsConfigByKey: mockGetGlobalsConfigByKey,
}));

import {
  filterMonitor,
  checkLicenseStatus,
  clearTimerLocation,
  clearTimerXietong,
  testHeartXietong,
  heartbeatXietong,
  heartbeatAddress,
  testLocationUpdate,
} from '@/utils/totalFunc.js';

describe('utils/totalFunc.js - 纯函数测试', () => {
  describe('filterMonitor', () => {
    it('空数组应返回空数组', () => {
      expect(filterMonitor([], { LINKXBS: '1', LINKXCCF: '1' })).toEqual([]);
    });

    it('应过滤掉 falsy 元素（null/undefined/0/false/空字符串）', () => {
      const list = [
        null,
        undefined,
        0,
        false,
        '',
        { name: 'A' },
        { name: 'B' },
      ];
      const result = filterMonitor(list, { LINKXBS: '1', LINKXCCF: '1' });
      expect(result).toHaveLength(2);
      expect(result[0].name).toBe('A');
      expect(result[1].name).toBe('B');
    });

    it('应按 name 字段去重，保留首个出现的', () => {
      const list = [
        { name: '设备调度', id: 1 },
        { name: '设备调度', id: 2 },
        { name: '设备调度', id: 3 },
      ];
      const result = filterMonitor(list, { LINKXBS: '1', LINKXCCF: '1' });
      expect(result).toHaveLength(1);
      expect(result[0].id).toBe(1);
    });

    it('name 缺失的项应被去重逻辑过滤掉', () => {
      const list = [
        { id: 1 }, // 无 name
        { name: '', id: 2 }, // name 为空字符串
        { name: 'A', id: 3 },
      ];
      const result = filterMonitor(list, { LINKXBS: '1', LINKXCCF: '1' });
      expect(result).toHaveLength(1);
      expect(result[0].name).toBe('A');
    });

    it('LINKXBS=1 且 LINKXCCF=1 时应保留"设备调度"', () => {
      const list = [
        { name: '设备调度', id: 1 },
        { name: '其他应用', id: 2 },
      ];
      const result = filterMonitor(list, { LINKXBS: '1', LINKXCCF: '1' });
      expect(result).toHaveLength(2);
    });

    it('LINKXBS !== "1" 时应过滤掉"设备调度"', () => {
      const list = [
        { name: '设备调度', id: 1 },
        { name: '其他应用', id: 2 },
      ];
      const result = filterMonitor(list, { LINKXBS: '0', LINKXCCF: '1' });
      expect(result).toHaveLength(1);
      expect(result[0].name).toBe('其他应用');
    });

    it('LINKXCCF !== "1" 时应过滤掉"设备调度"', () => {
      const list = [
        { name: '设备调度', id: 1 },
        { name: '其他应用', id: 2 },
      ];
      const result = filterMonitor(list, { LINKXBS: '1', LINKXCCF: '0' });
      expect(result).toHaveLength(1);
      expect(result[0].name).toBe('其他应用');
    });

    it('LINKXBS 和 LINKXCCF 同时不为 "1" 时应过滤掉"设备调度"', () => {
      const list = [
        { name: '设备调度', id: 1 },
        { name: '其他应用', id: 2 },
      ];
      const result = filterMonitor(list, { LINKXBS: '0', LINKXCCF: '0' });
      expect(result).toHaveLength(1);
      expect(result[0].name).toBe('其他应用');
    });

    it('license 缺失字段时（undefined）应过滤掉"设备调度"', () => {
      const list = [
        { name: '设备调度', id: 1 },
        { name: '其他应用', id: 2 },
      ];
      const result = filterMonitor(list, {});
      expect(result).toHaveLength(1);
      expect(result[0].name).toBe('其他应用');
    });

    it('综合场景：falsy 过滤 + name 去重 + license 过滤', () => {
      const list = [
        null,
        { name: '应用A', id: 1 },
        { name: '应用A', id: 2 }, // 重复，应被去重
        undefined,
        { name: '设备调度', id: 3 },
        { name: '应用B', id: 4 },
        { id: 5 }, // 无 name，应被去重逻辑过滤
      ];
      const result = filterMonitor(list, { LINKXBS: '0', LINKXCCF: '1' });
      expect(result).toHaveLength(2);
      expect(result[0].name).toBe('应用A');
      expect(result[1].name).toBe('应用B');
    });

    it('不应修改原数组', () => {
      const list = [{ name: 'A' }, { name: 'B' }];
      const snapshot = JSON.stringify(list);
      filterMonitor(list, { LINKXBS: '1', LINKXCCF: '1' });
      expect(JSON.stringify(list)).toBe(snapshot);
    });
  });

  describe('checkLicenseStatus', () => {
    it('当前实现直接 return，调用不应抛错', () => {
      expect(() => checkLicenseStatus({})).not.toThrow();
      expect(() => checkLicenseStatus({ status: '0', expireDate: '2026-12-31' })).not.toThrow();
      expect(() => checkLicenseStatus({ status: '3', expireDate: '2026-12-31' })).not.toThrow();
      expect(() => checkLicenseStatus({ status: '2', expireDate: '2026-12-31' })).not.toThrow();
      expect(() => checkLicenseStatus(null)).not.toThrow();
      expect(() => checkLicenseStatus(undefined)).not.toThrow();
    });

    it('当前实现返回 undefined', () => {
      expect(checkLicenseStatus({})).toBeUndefined();
      expect(checkLicenseStatus({ status: '2', expireDate: '2026-12-31' })).toBeUndefined();
    });
  });

  describe('clearTimerLocation', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('无定时器时应返回 undefined 且不抛错', () => {
      expect(clearTimerLocation()).toBeUndefined();
    });

    it('多次调用应幂等', () => {
      expect(() => {
        clearTimerLocation();
        clearTimerLocation();
        clearTimerLocation();
      }).not.toThrow();
    });
  });

  describe('clearTimerXietong', () => {
    beforeEach(() => {
      vi.useFakeTimers();
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('无定时器时应返回 undefined 且不抛错', () => {
      expect(clearTimerXietong()).toBeUndefined();
    });

    it('多次调用应幂等', () => {
      expect(() => {
        clearTimerXietong();
        clearTimerXietong();
        clearTimerXietong();
      }).not.toThrow();
    });
  });

  describe('testHeartXietong', () => {
    let messageBoxSpy;

    beforeEach(() => {
      vi.useFakeTimers();
      mockHeartbeatApi.mockReset();
      mockHeartbeatApi.mockResolvedValue(undefined);
      // 全局 MessageBox 在源码中以全局变量形式被调用，需要在 globalThis 上 mock
      messageBoxSpy = vi.fn().mockResolvedValue(undefined);
      globalThis.MessageBox = messageBoxSpy;
    });

    afterEach(() => {
      delete globalThis.MessageBox;
    });

    it('heartbeatApi 返回 falsy 时不应累加失败次数（业务反向逻辑：data 为假视为成功）', async () => {
      mockHeartbeatApi.mockResolvedValueOnce(null);
      await testHeartXietong();
      // 源码逻辑：data 为假时 failTimesXietong 重置为 0，不触发 MessageBox
      expect(mockHeartbeatApi).toHaveBeenCalledWith({ userId: 'user-001' });
      expect(messageBoxSpy).not.toHaveBeenCalled();
    });

    it('heartbeatApi 返回 truthy 时应累加失败次数（业务反向逻辑：data 为真视为失败）', async () => {
      mockHeartbeatApi.mockResolvedValueOnce({ code: 0 });
      // 先清理掉之前可能的失败累计（调用 clearTimerXietong 会重置 failTimesXietong）
      clearTimerXietong();
      await testHeartXietong();
      expect(mockHeartbeatApi).toHaveBeenCalled();
      expect(messageBoxSpy).not.toHaveBeenCalled(); // 仅 1 次未达阈值
    });

    it('连续 3 次"失败"后应调用 MessageBox 弹窗并清理定时器', async () => {
      // 清零（通过 clearTimerXietong 内部会重置 failTimesXietong = 0）
      clearTimerXietong();
      mockHeartbeatApi.mockResolvedValue({ code: 0 }); // truthy 视为失败
      // 触发 3 次
      await testHeartXietong();
      await testHeartXietong();
      await testHeartXietong();
      expect(messageBoxSpy).toHaveBeenCalledTimes(1);
      // 验证 MessageBox 调用参数
      expect(messageBoxSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          isLight: true,
          type: 'ok',
          text: expect.stringContaining('网络异常'),
        }),
      );
    });

    it('heartbeatApi 抛错时应累加失败次数', async () => {
      clearTimerXietong();
      mockHeartbeatApi.mockRejectedValueOnce(new Error('network'));
      await testHeartXietong();
      expect(messageBoxSpy).not.toHaveBeenCalled(); // 1 次未达阈值
    });

    it('连续 3 次抛错后应触发 MessageBox', async () => {
      clearTimerXietong();
      mockHeartbeatApi.mockRejectedValue(new Error('network'));
      await testHeartXietong();
      await testHeartXietong();
      await testHeartXietong();
      expect(messageBoxSpy).toHaveBeenCalledTimes(1);
    });

    it('userInfo 缺失时也应调用 heartbeatApi（传入 undefined）', async () => {
      const originalUserInfo = mockApplicationStore.userInfo;
      mockApplicationStore.userInfo = undefined;
      mockHeartbeatApi.mockResolvedValueOnce(null);
      try {
        await testHeartXietong();
        expect(mockHeartbeatApi).toHaveBeenCalledWith({ userId: undefined });
      } finally {
        mockApplicationStore.userInfo = originalUserInfo;
      }
    });
  });

  describe('heartbeatXietong', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      mockHeartbeatApi.mockReset();
      mockHeartbeatApi.mockResolvedValue(null);
      globalThis.MessageBox = vi.fn().mockResolvedValue(undefined);
    });

    afterEach(() => {
      clearTimerXietong();
      delete globalThis.MessageBox;
    });

    it('调用后应启动 10 秒间隔的定时器', async () => {
      const spy = vi.spyOn(globalThis, 'setInterval');
      await heartbeatXietong();
      expect(spy).toHaveBeenCalled();
      // 验证间隔为 10 秒（1000 * 10）
      const interval = spy.mock.calls[0][1];
      expect(interval).toBe(1000 * 10);
    });

    it('定时器触发时应调用 testHeartXietong（通过 heartbeatApi 间接验证）', async () => {
      await heartbeatXietong();
      mockHeartbeatApi.mockClear();
      // 推进 10 秒触发一次定时器
      await vi.advanceTimersByTimeAsync(10000);
      expect(mockHeartbeatApi).toHaveBeenCalled();
    });

    it('多次调用应先清理旧定时器再创建新定时器', async () => {
      const clearSpy = vi.spyOn(globalThis, 'clearInterval');
      await heartbeatXietong();
      const firstTimerId = clearSpy.mock.calls.length;
      await heartbeatXietong();
      // 第二次调用前应调用过 clearInterval（来自 clearTimerXietong）
      // 至少调用过 1 次（清理前一个）
      expect(clearSpy).toHaveBeenCalled();
      // 通过推进时间验证只产生新的定时器调用
      mockHeartbeatApi.mockClear();
      await vi.advanceTimersByTimeAsync(10000);
      expect(mockHeartbeatApi).toHaveBeenCalledTimes(1);
    });
  });

  describe('heartbeatAddress', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      mockGetGlobalsConfigByKey.mockReset();
      mockGetGlobalsConfigByKey.mockResolvedValue('5');
      mockHeartbeatLocation.mockReset();
      mockHeartbeatLocation.mockResolvedValue({});
      mockCommunicationStore.getGisInfo.mockReset();
      mockCommunicationStore.getGisInfo.mockResolvedValue({ longitude: 104.06, latitude: 30.57 });
    });

    afterEach(() => {
      clearTimerLocation();
    });

    it('应先调用 getGlobalsConfigByKey 获取上报间隔', async () => {
      await heartbeatAddress();
      expect(mockGetGlobalsConfigByKey).toHaveBeenCalledWith('LOCATION_REPORT_TIME');
    });

    it('应使用配置的时间间隔（秒）启动定时器', async () => {
      const spy = vi.spyOn(globalThis, 'setInterval');
      await heartbeatAddress();
      expect(spy).toHaveBeenCalled();
      // 5 秒 → 5000 ms
      expect(spy.mock.calls[0][1]).toBe(5000);
    });

    it('定时器触发时应调用 testLocationUpdate（通过 heartbeatLocation 验证）', async () => {
      await heartbeatAddress();
      await vi.advanceTimersByTimeAsync(5000);
      expect(mockHeartbeatLocation).toHaveBeenCalled();
    });

    it('配置返回非数字字符串时（NaN）应使用 NaN 作为间隔', async () => {
      mockGetGlobalsConfigByKey.mockResolvedValue('abc');
      const spy = vi.spyOn(globalThis, 'setInterval');
      await heartbeatAddress();
      expect(spy).toHaveBeenCalled();
      // Number('abc') * 1000 = NaN
      expect(spy.mock.calls[0][1]).toBeNaN();
    });
  });

  describe('testLocationUpdate', () => {
    beforeEach(() => {
      mockCommunicationStore.getGisInfo.mockReset();
      mockHeartbeatLocation.mockReset();
    });

    afterEach(() => {
      clearTimerLocation();
    });

    it('getGisInfo 返回位置时应拼接为 "经度,纬度" 并调用 heartbeatLocation', async () => {
      mockCommunicationStore.getGisInfo.mockResolvedValue({
        longitude: 104.0668,
        latitude: 30.5728,
      });
      mockHeartbeatLocation.mockResolvedValue({});

      await testLocationUpdate();

      expect(mockHeartbeatLocation).toHaveBeenCalledWith({
        userId: 'user-001',
        location: '104.0668,30.5728',
      });
    });

    it('getGisInfo 返回 null 时 location 应为空字符串', async () => {
      mockCommunicationStore.getGisInfo.mockResolvedValue(null);
      mockHeartbeatLocation.mockResolvedValue({});

      await testLocationUpdate();

      expect(mockHeartbeatLocation).toHaveBeenCalledWith({
        userId: 'user-001',
        location: '',
      });
    });

    it('getGisInfo 返回 undefined 时 location 应为空字符串', async () => {
      mockCommunicationStore.getGisInfo.mockResolvedValue(undefined);
      mockHeartbeatLocation.mockResolvedValue({});

      await testLocationUpdate();

      expect(mockHeartbeatLocation).toHaveBeenCalledWith({
        userId: 'user-001',
        location: '',
      });
    });

    it('userInfo.userid 缺失时 userId 应为 undefined', async () => {
      const originalUserInfo = mockApplicationStore.userInfo;
      // 源码: const { userInfo } = useApplicationStore(); userInfo.userid
      // userInfo 本身不能为 undefined（会 NPE），但 userid 可以缺失
      mockApplicationStore.userInfo = {};
      mockCommunicationStore.getGisInfo.mockResolvedValue({ longitude: 1, latitude: 2 });
      mockHeartbeatLocation.mockResolvedValue({});

      try {
        await testLocationUpdate();
        expect(mockHeartbeatLocation).toHaveBeenCalledWith({
          userId: undefined,
          location: '1,2',
        });
      } finally {
        mockApplicationStore.userInfo = originalUserInfo;
      }
    });
  });
});
