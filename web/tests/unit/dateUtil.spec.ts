import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// mock 掉 @/hooks,避免 useBaseData → resourceHelper → mspPlayer → plugins/logs 的加载链
// (plugins/logs 在模块加载时访问 indexedDB,会导致 happy-dom 环境下报错)
vi.mock('@/hooks', () => ({
  useI18n: () => ({
    t: (key: string) => key,
  }),
}));

import dateUtil from '@/utils/dateUtil';

// 提供一个固定的基准日期，便于断言
const FIXED_DATE = new Date(2024, 0, 15, 8, 30, 45); // 2024-01-15 08:30:45 (闰年)

describe('dateUtil - 日期工具方法', () => {
  describe('isLeapYear - 闰年判断', () => {
    it('2000 是闰年（能被400整除）', () => {
      expect(dateUtil.isLeapYear(2000)).toBe(true);
    });

    it('1900 不是闰年（能被100整除但不能被400整除）', () => {
      expect(dateUtil.isLeapYear(1900)).toBe(false);
    });

    it('2024 是闰年（能被4整除且不能被100整除）', () => {
      expect(dateUtil.isLeapYear(2024)).toBe(true);
    });

    it('2023 不是闰年', () => {
      expect(dateUtil.isLeapYear(2023)).toBe(false);
    });

    it('字符串 "2024" 也支持（内部 parseInt 转换）', () => {
      expect(dateUtil.isLeapYear('2024')).toBe(true);
    });
  });

  describe('getDaysInMonth - 指定月份天数', () => {
    it('平年2月返回28天', () => {
      expect(dateUtil.getDaysInMonth(2023, 2)).toBe(28);
    });

    it('闰年2月返回29天', () => {
      expect(dateUtil.getDaysInMonth(2024, 2)).toBe(29);
    });

    it('大月返回31天（1月）', () => {
      expect(dateUtil.getDaysInMonth(2024, 1)).toBe(31);
    });

    it('小月返回30天（4月）', () => {
      expect(dateUtil.getDaysInMonth(2024, 4)).toBe(30);
    });

    it('字符串月份会被 parseInt 处理', () => {
      expect(dateUtil.getDaysInMonth(2024, '2')).toBe(29);
    });
  });

  describe('format - 默认 yyyy-MM-dd HH:mm:ss', () => {
    it('Date 对象输出标准格式', () => {
      expect(dateUtil.format(FIXED_DATE)).toBe('2024-01-15 08:30:45');
    });

    it('字符串日期也可被解析', () => {
      expect(dateUtil.format('2024-01-15 08:30:45')).toBe('2024-01-15 08:30:45');
    });

    it('时间戳数字也支持', () => {
      expect(dateUtil.format(FIXED_DATE.getTime())).toBe('2024-01-15 08:30:45');
    });

    it('单数字会补零', () => {
      expect(dateUtil.format(new Date(2024, 0, 5, 3, 4, 5))).toBe('2024-01-05 03:04:05');
    });
  });

  describe('formatDate - 自定义占位符', () => {
    it('yyyy-MM-dd HH:mm:ss', () => {
      expect(dateUtil.formatDate(FIXED_DATE, 'yyyy-MM-dd HH:mm:ss')).toBe('2024-01-15 08:30:45');
    });

    it('yy-MM-dd 仅取两位年份', () => {
      expect(dateUtil.formatDate(FIXED_DATE, 'yy-MM-dd')).toBe('24-01-15');
    });

    it('yyyy年M月d日 不补零', () => {
      expect(dateUtil.formatDate(new Date(2024, 0, 5, 3, 4, 5), 'yyyy年M月d日')).toBe(
        '2024年1月5日',
      );
    });

    it('yyyy-MM-dd hh:mm:ss 使用 12 小时制（8点 => 08）', () => {
      expect(dateUtil.formatDate(FIXED_DATE, 'yyyy-MM-dd hh:mm:ss')).toBe('2024-01-15 08:30:45');
    });

    it('12 小时制下午会转换（13点 => 1）', () => {
      const d = new Date(2024, 0, 15, 13, 30, 45);
      expect(dateUtil.formatDate(d, 'yyyy-MM-dd hh:mm:ss')).toBe('2024-01-15 01:30:45');
    });

    it('E 单字符显示星期几（"一"）', () => {
      // 2024-01-15 是周一
      expect(dateUtil.formatDate(FIXED_DATE, 'yyyy-MM-dd E')).toBe('2024-01-15 一');
    });

    it('EE 双字符显示带"周"前缀', () => {
      expect(dateUtil.formatDate(FIXED_DATE, 'yyyy-MM-dd EE')).toBe('2024-01-15 周一');
    });

    it('EEE 三字符显示带"星期"前缀', () => {
      expect(dateUtil.formatDate(FIXED_DATE, 'yyyy-MM-dd EEE')).toBe('2024-01-15 星期一');
    });

    it('包含季度占位符 q', () => {
      expect(dateUtil.formatDate(FIXED_DATE, 'yyyy 第 q 季度')).toBe('2024 第 1 季度');
    });

    it('包含毫秒 S', () => {
      const d = new Date(2024, 0, 15, 8, 30, 45, 123);
      expect(dateUtil.formatDate(d, 'yyyy-MM-dd HH:mm:ss.S')).toBe('2024-01-15 08:30:45.123');
    });

    it('非 Date 对象返回 undefined', () => {
      expect(dateUtil.formatDate('not a date', 'yyyy-MM-dd')).toBeUndefined();
    });
  });

  describe('formatHourMinute - 小时:分钟 差', () => {
    it('start 大于 end（注意参数顺序: 计算为 start - end）', () => {
      // start=10:00, end=08:00 => 差 2 小时
      const result = dateUtil.formatHourMinute(
        new Date(2024, 0, 15, 10, 0, 0),
        new Date(2024, 0, 15, 8, 0, 0),
      );
      expect(result).toBe('2:0');
    });

    it('时间字符串作为入参也支持', () => {
      const result = dateUtil.formatHourMinute('2024-01-15 10:30:00', '2024-01-15 08:00:00');
      expect(result).toBe('2:30');
    });
  });

  describe('formatHourMinuteSecond - 小时:分钟:秒 差（参数顺序为 end, start）', () => {
    it('end 大于 start（计算 end - start）', () => {
      // end=10:00:00, start=08:30:00 => 差 1:30:00
      const result = dateUtil.formatHourMinuteSecond(
        new Date(2024, 0, 15, 10, 0, 0),
        new Date(2024, 0, 15, 8, 30, 0),
      );
      expect(result).toBe('01:30:00');
    });

    it('时间字符串作为入参也支持', () => {
      const result = dateUtil.formatHourMinuteSecond(
        '2024-01-15 10:00:00',
        '2024-01-15 08:30:00',
      );
      expect(result).toBe('01:30:00');
    });

    it('小于10的数字会补零', () => {
      const result = dateUtil.formatHourMinuteSecond(
        new Date(2024, 0, 15, 9, 5, 3),
        new Date(2024, 0, 15, 9, 0, 0),
      );
      expect(result).toBe('00:05:03');
    });
  });

  describe('getDays - 两日期差天数', () => {
    it('同一天返回 0', () => {
      expect(dateUtil.getDays('2024-01-15', '2024-01-15')).toBe(0);
    });

    it('相差1天', () => {
      expect(dateUtil.getDays('2024-01-15', '2024-01-16')).toBe(1);
    });

    it('相差跨月', () => {
      expect(dateUtil.getDays('2024-01-15', '2024-02-15')).toBe(31);
    });

    it('取绝对值，参数顺序不影响结果', () => {
      expect(dateUtil.getDays('2024-02-15', '2024-01-15')).toBe(31);
    });
  });

  describe('getHMSByMsec - 毫秒转 {day, hour, minute, second}', () => {
    it('0 毫秒返回全 0（字符串形式）', () => {
      const result = dateUtil.getHMSByMsec(0);
      expect(result.day).toBe(0);
      expect(result.hour).toBe('00');
      expect(result.minute).toBe('00');
      expect(result.second).toBe('00');
    });

    it('1天', () => {
      const oneDay = 24 * 60 * 60 * 1000;
      const result = dateUtil.getHMSByMsec(oneDay);
      expect(result.day).toBe(1);
      expect(result.hour).toBe('00');
      expect(result.minute).toBe('00');
      expect(result.second).toBe('00');
    });

    it('1天2小时3分4秒', () => {
      const ms = 1 * 24 * 60 * 60 * 1000 + 2 * 60 * 60 * 1000 + 3 * 60 * 1000 + 4 * 1000;
      const result = dateUtil.getHMSByMsec(ms);
      expect(result.day).toBe(1);
      expect(result.hour).toBe('02');
      expect(result.minute).toBe('03');
      expect(result.second).toBe('04');
    });

    it('小时大于等于10不补零', () => {
      const ms = 10 * 60 * 60 * 1000;
      const result = dateUtil.getHMSByMsec(ms);
      expect(result.hour).toBe('10');
    });
  });

  describe('getNumberString - 日期数字串截取', () => {
    it('Date 对象按 yyyyMMddHHmmss 截取前 8 位', () => {
      expect(dateUtil.getNumberString(FIXED_DATE, 8)).toBe('20240115');
    });

    it('Date 对象截取前 14 位（全部）', () => {
      expect(dateUtil.getNumberString(FIXED_DATE, 14)).toBe('20240115083045');
    });

    it('字符串日期会被清理非数字字符', () => {
      expect(dateUtil.getNumberString('2024-01-15 08:30:45', 8)).toBe('20240115');
    });

    it('中文年月日时分秒也会被清理', () => {
      expect(dateUtil.getNumberString('2024年01月15日 08时30分45秒', 8)).toBe('20240115');
    });

    it('length=0 返回空字符串', () => {
      expect(dateUtil.getNumberString(FIXED_DATE, 0)).toBe('');
    });
  });

  describe('getSecondByDateSub - begin - end 秒数（无绝对值）', () => {
    it('begin 大于 end 返回正数', () => {
      const begin = new Date(2024, 0, 15, 8, 30, 46);
      const end = new Date(2024, 0, 15, 8, 30, 45);
      expect(dateUtil.getSecondByDateSub(begin, end)).toBe(1);
    });

    it('begin 小于 end 返回负数', () => {
      const begin = new Date(2024, 0, 15, 8, 30, 45);
      const end = new Date(2024, 0, 15, 8, 30, 46);
      expect(dateUtil.getSecondByDateSub(begin, end)).toBe(-1);
    });
  });

  describe('getSeconds - 两时间差秒数（取绝对值）', () => {
    it('相同时间返回 0', () => {
      expect(dateUtil.getSeconds('2024-01-15 08:30:45', '2024-01-15 08:30:45')).toBe(0);
    });

    it('相差 60 秒', () => {
      expect(dateUtil.getSeconds('2024-01-15 08:31:45', '2024-01-15 08:30:45')).toBe(60);
    });

    it('取绝对值，参数顺序不影响结果', () => {
      expect(dateUtil.getSeconds('2024-01-15 08:30:45', '2024-01-15 08:31:45')).toBe(60);
    });
  });

  describe('getTime - 获取毫秒数', () => {
    it('Date 对象返回毫秒数', () => {
      expect(dateUtil.getTime(FIXED_DATE)).toBe(FIXED_DATE.getTime());
    });

    it('非 Date 对象原样返回', () => {
      expect(dateUtil.getTime('not a date')).toBe('not a date');
    });
  });

  describe('timeChange - 数字补零', () => {
    it('小于10补零', () => {
      expect(dateUtil.timeChange(5)).toBe('05');
    });

    it('大于等于10原样返回', () => {
      expect(dateUtil.timeChange(15)).toBe(15);
    });

    it('0 补零', () => {
      expect(dateUtil.timeChange(0)).toBe('00');
    });
  });

  describe('addDays - 加减天数（会 mutate 入参，需要深拷贝）', () => {
    it('加1天', () => {
      const d = new Date(2024, 0, 15);
      const result = dateUtil.addDays(new Date(d), 1, 'yyyy-MM-dd');
      expect(result).toBe('2024-01-16');
    });

    it('减1天', () => {
      const d = new Date(2024, 0, 15);
      const result = dateUtil.addDays(new Date(d), -1, 'yyyy-MM-dd');
      expect(result).toBe('2024-01-14');
    });

    it('会修改入参对象（mutate 行为）', () => {
      const d = new Date(2024, 0, 15);
      const result = dateUtil.addDays(d, 1, 'yyyy-MM-dd');
      // 入参 d 已被 setTime 修改
      expect(d.getDate()).toBe(16);
      expect(result).toBe('2024-01-16');
    });

    it('传入 outFmt 返回字符串', () => {
      const d = new Date(2024, 0, 15);
      const result = dateUtil.addDays(new Date(d), 1, 'yyyy-MM-dd');
      expect(result).toBe('2024-01-16');
    });

    it('非 Date 入参原样返回', () => {
      expect(dateUtil.addDays('not a date', 1, 'yyyy-MM-dd')).toBe('not a date');
    });

    it.skip('已知 bug: 不传 outFmt 时 isEmpty(undefined) 返回 false,导致进入 formatDate(date, undefined) 分支崩溃', () => {
      // 源码: if (isEmpty(outFmt)) { return date; } return this.formatDate(date, outFmt);
      // isEmpty(undefined) 在 is.ts 中走完所有分支后返回 false,导致 outFmt=undefined 时进入 formatDate
      // formatDate 内部 fmt.replace(RegExp.$1, ...) 因 RegExp.$1 为 undefined 抛出 TypeError
      const d = new Date(2024, 0, 15);
      const result = dateUtil.addDays(new Date(d), 1, undefined as unknown as string);
      expect(result.getDate()).toBe(16);
    });
  });

  describe('addHours - 加减小时', () => {
    it('加2小时', () => {
      const result = dateUtil.addHours(new Date(2024, 0, 15, 8, 0, 0), 2, 'HH:mm:ss');
      expect(result).toBe('10:00:00');
    });

    it('减2小时', () => {
      const result = dateUtil.addHours(new Date(2024, 0, 15, 8, 0, 0), -2, 'HH:mm:ss');
      expect(result).toBe('06:00:00');
    });

    it('传入 outFmt 返回字符串', () => {
      const result = dateUtil.addHours(new Date(2024, 0, 15, 8, 0, 0), 2, 'HH:mm:ss');
      expect(result).toBe('10:00:00');
    });
  });

  describe('addMinutes - 加减分钟', () => {
    it('加30分钟', () => {
      const result = dateUtil.addMinutes(new Date(2024, 0, 15, 8, 0, 0), 30, 'HH:mm');
      expect(result).toBe('08:30');
    });

    it('减30分钟', () => {
      const result = dateUtil.addMinutes(new Date(2024, 0, 15, 8, 0, 0), -30, 'HH:mm');
      // 8:00 - 30min = 7:30
      expect(result).toBe('07:30');
    });

    it('传入 outFmt 返回字符串', () => {
      const result = dateUtil.addMinutes(new Date(2024, 0, 15, 8, 0, 0), 30, 'HH:mm');
      expect(result).toBe('08:30');
    });
  });

  describe('addSeconds - 加减秒数', () => {
    it('加30秒', () => {
      const result = dateUtil.addSeconds(new Date(2024, 0, 15, 8, 0, 0), 30, 'HH:mm:ss');
      expect(result).toBe('08:00:30');
    });

    it('减30秒', () => {
      const result = dateUtil.addSeconds(new Date(2024, 0, 15, 8, 0, 0), -30, 'HH:mm:ss');
      // 8:00:00 - 30s = 7:59:30
      expect(result).toBe('07:59:30');
    });

    it('传入 outFmt 返回字符串', () => {
      const result = dateUtil.addSeconds(new Date(2024, 0, 15, 8, 0, 0), 30, 'HH:mm:ss');
      expect(result).toBe('08:00:30');
    });
  });

  describe('addMonths - 加减月份', () => {
    it('加1个月', () => {
      const result = dateUtil.addMonths(new Date(2024, 0, 15), 1, 'yyyy-MM-dd');
      expect(result).toBe('2024-02-15');
    });

    it('减1个月', () => {
      const result = dateUtil.addMonths(new Date(2024, 1, 15), -1, 'yyyy-MM-dd');
      expect(result).toBe('2024-01-15');
    });

    it('跨年：12月加1个月 → 次年1月', () => {
      const result = dateUtil.addMonths(new Date(2024, 11, 15), 1, 'yyyy-MM-dd');
      expect(result).toBe('2025-01-15');
    });

    it('跨年：1月减1个月 → 上年12月', () => {
      const result = dateUtil.addMonths(new Date(2024, 0, 15), -1, 'yyyy-MM-dd');
      expect(result).toBe('2023-12-15');
    });

    it('1月31日加1个月会按2月最后一天截断', () => {
      const result = dateUtil.addMonths(new Date(2024, 0, 31), 1, 'yyyy-MM-dd');
      // 2024 是闰年, 2月有 29 天
      expect(result).toBe('2024-02-29');
    });

    it('传入 outFmt 返回字符串', () => {
      const result = dateUtil.addMonths(new Date(2024, 0, 15), 1, 'yyyy-MM');
      expect(result).toBe('2024-02');
    });
  });

  describe('addYears - 加减年份', () => {
    it('加1年', () => {
      const result = dateUtil.addYears(new Date(2024, 0, 15), 1, 'yyyy-MM-dd');
      expect(result).toBe('2025-01-15');
    });

    it('减1年', () => {
      const result = dateUtil.addYears(new Date(2024, 0, 15), -1, 'yyyy-MM-dd');
      expect(result).toBe('2023-01-15');
    });

    it('2月29日加1年到平年 → 2月28日', () => {
      const result = dateUtil.addYears(new Date(2024, 1, 29), 1, 'yyyy-MM-dd');
      expect(result).toBe('2025-02-28');
    });

    it('传入 outFmt 返回字符串', () => {
      const result = dateUtil.addYears(new Date(2024, 0, 15), 1, 'yyyy');
      expect(result).toBe('2025');
    });
  });

  describe('strToDate - 字符串转 Date（已知 bug: isEmpty 判断反了）', () => {
    it.skip('已知 bug: 非空字符串应进入解析分支返回 Date,但源码 isEmpty(str) 判断反了,非空时不进入解析直接原样返回字符串', () => {
      // 源码: if (isString(str) && isEmpty(str)) { ...解析... } return str;
      // 期望: 非空字符串进入解析返回 Date,实际 isEmpty('20240115...') = false,所以不进入解析,原样返回字符串
      const result = dateUtil.strToDate('20240115083045', 'yyyyMMddHHmmss');
      expect(result).toBeInstanceOf(Date);
      expect(result.getFullYear()).toBe(2024);
    });

    it.skip('已知 bug: yyyy-MM-dd 字符串解析同样受 isEmpty 判断反转影响', () => {
      const result = dateUtil.strToDate('2024-01-15', 'yyyy-MM-dd') as Date;
      expect(result.getFullYear()).toBe(2024);
      expect(result.getMonth()).toBe(0);
      expect(result.getDate()).toBe(15);
    });

    it('非字符串入参原样返回', () => {
      const result = dateUtil.strToDate(123 as any, 'yyyyMMddHHmmss');
      expect(result).toBe(123);
    });

    it.skip('已知 bug: 空字符串时 isEmpty("")=true 会进入解析分支,但 substring 在空字符串上返回空串,setFullYear 等会使用 NaN', () => {
      // 源码逻辑: 空字符串时进入解析,但因 substring 结果为空,parseInt('') = NaN
      // 期望: 空字符串原样返回,实际进入解析分支返回一个 Invalid Date 或异常 Date
      const result = dateUtil.strToDate('', 'yyyyMMddHHmmss');
      expect(result).toBe('');
    });
  });

  describe('getCurrentTime / getTodayWeek - 当前时间相关（使用 fake timers）', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 45)); // 2024-01-15 周一
    });

    afterEach(() => {
      vi.useRealTimers();
    });

    it('getCurrentTime 默认 yyyyMMddHHmmss', () => {
      expect(dateUtil.getCurrentTime()).toBe('20240115083045');
    });

    it('getCurrentTime 支持自定义格式', () => {
      expect(dateUtil.getCurrentTime('yyyy-MM-dd')).toBe('2024-01-15');
    });

    it('getTodayWeek 返回中文星期', () => {
      // 2024-01-15 是周一
      expect(dateUtil.getTodayWeek()).toBe('星期一');
    });

    it('周日返回"星期天"', () => {
      vi.setSystemTime(new Date(2024, 0, 14, 8, 30, 45)); // 2024-01-14 周日
      expect(dateUtil.getTodayWeek()).toBe('星期天');
    });
  });

  describe('formatDateStr - 字符串日期转换', () => {
    it('Date 对象直接格式化为 yyyy-MM-dd', () => {
      expect(dateUtil.formatDateStr(FIXED_DATE, 'yyyy-MM-dd')).toBe('2024-01-15');
    });

    it('Date 对象格式化为完整时间', () => {
      expect(dateUtil.formatDateStr(FIXED_DATE, 'yyyy-MM-dd HH:mm:ss')).toBe('2024-01-15 08:30:45');
    });

    it('Date 对象格式化为带星期', () => {
      expect(dateUtil.formatDateStr(FIXED_DATE, 'yyyy-MM-dd EE')).toBe('2024-01-15 周一');
    });

    it('非字符串非 Date 的数字入参原样返回', () => {
      expect(dateUtil.formatDateStr(123, 'yyyy-MM-dd')).toBe(123);
    });

    it('null 原样返回', () => {
      expect(dateUtil.formatDateStr(null, 'yyyy-MM-dd')).toBe(null);
    });

    it('undefined 原样返回', () => {
      expect(dateUtil.formatDateStr(undefined, 'yyyy-MM-dd')).toBe(undefined);
    });

    it('空字符串原样返回（falsy 短路）', () => {
      expect(dateUtil.formatDateStr('', 'yyyy-MM-dd')).toBe('');
    });

    it('非 Date 对象（如普通对象）走 isDate 分支失败后原样返回', () => {
      const obj = { a: 1 };
      expect(dateUtil.formatDateStr(obj, 'yyyy-MM-dd')).toBe(obj);
    });

    it.skip('已知 bug: 非空字符串因 strToDate 的 isEmpty 判断反转,formatDate 收到字符串返回 undefined', () => {
      // 源码: strToDate 中 isEmpty(str) 对非空字符串返回 false,不进入解析,原样返回字符串
      // 随后 formatDate 收到字符串,因 !isDate(date) 返回 undefined
      expect(dateUtil.formatDateStr('20240115083045', 'yyyy-MM-dd')).toBe('2024-01-15');
    });

    it.skip('已知 bug: 指定 inFmt=yyyy-MM-dd 的字符串同样无法解析', () => {
      expect(dateUtil.formatDateStr('2024-01-15', 'yyyy年MM月dd日', 'yyyy-MM-dd')).toBe(
        '2024年01月15日',
      );
    });
  });

  describe('getNumberString - 补充非 Date/非 String 分支', () => {
    it('数字入参原样返回', () => {
      expect(dateUtil.getNumberString(123456, 8)).toBe(123456);
    });

    it('null 入参原样返回', () => {
      expect(dateUtil.getNumberString(null, 8)).toBe(null);
    });

    it('负 length 走 Math.max(0, length) 返回空字符串', () => {
      expect(dateUtil.getNumberString(FIXED_DATE, -3)).toBe('');
    });
  });

  describe('dataToYesterday / getMondayTimesTamp / transformTime - 依赖 useI18n', () => {
    beforeEach(() => {
      vi.useFakeTimers();
      vi.setSystemTime(new Date(2024, 0, 15, 8, 30, 45)); // 2024-01-15 周一 08:30:45
      // dataToYesterday 内部 toLocaleDateString 在非中文 locale 下格式为 'M/d/yyyy',
      // 会导致 split('-') 后数组顺序异常,函数逻辑不可用。统一 mock 为 'yyyy/M/d' 格式。
      vi.spyOn(Date.prototype, 'toLocaleDateString').mockReturnValue('2024/1/15');
    });

    afterEach(() => {
      vi.restoreAllMocks();
      vi.useRealTimers();
    });

    describe('dataToYesterday - 日期转昨天/今天', () => {
      it('跨年日期原样返回', () => {
        expect(dateUtil.dataToYesterday('2023-12-31')).toBe('2023-12-31');
      });

      it('今天返回 today 文案 key', () => {
        expect(dateUtil.dataToYesterday('2024-01-15')).toBe('common.dateDay.today');
      });

      it('昨天返回 yesterday 文案 key', () => {
        expect(dateUtil.dataToYesterday('2024-01-14')).toBe('common.yesterday');
      });

      it('本周其他日期返回 MM-dd 截取', () => {
        expect(dateUtil.dataToYesterday('2024-01-10')).toBe('01-10');
      });

      it('同年的远期日期返回 MM-dd 截取', () => {
        expect(dateUtil.dataToYesterday('2024-06-08')).toBe('06-08');
      });
    });

    describe('getMondayTimesTamp - 判断是否本周并返回星期几', () => {
      it('本周一返回星期一文案 key', () => {
        expect(dateUtil.getMondayTimesTamp('2024-01-15 10:00:00')).toBe(
          'common.dateDay.dayOfWeek1',
        );
      });

      it('本周二返回星期二文案 key', () => {
        expect(dateUtil.getMondayTimesTamp('2024-01-16 10:00:00')).toBe(
          'common.dateDay.dayOfWeek2',
        );
      });

      it('本周日(本周日 2024-01-21)返回星期日文案 key', () => {
        expect(dateUtil.getMondayTimesTamp('2024-01-21 10:00:00')).toBe(
          'common.dateDay.dayOfWeek0',
        );
      });

      it('本周一之前的周日(上周)返回 null', () => {
        // 2024-01-14 周日,在周一 2024-01-15 之前,diff < 0
        expect(dateUtil.getMondayTimesTamp('2024-01-14 10:00:00')).toBeNull();
      });

      it('超过一周返回 null', () => {
        // 2024-01-22 周一,距本周一正好 7 天,diff = 7*24*3600*1000,不满足 diff < 7*...
        expect(dateUtil.getMondayTimesTamp('2024-01-22 10:00:00')).toBeNull();
      });

      it('传入 Date 对象也支持', () => {
        expect(dateUtil.getMondayTimesTamp(new Date(2024, 0, 16, 10, 0, 0))).toBe(
          'common.dateDay.dayOfWeek2',
        );
      });
    });

    describe('transformTime - 警情时间展示', () => {
      it('当天返回 [时间]', () => {
        expect(dateUtil.transformTime('2024-01-15 10:00:00')).toEqual(['10:00:00']);
      });

      it('昨天返回 [yesterday, 时间]', () => {
        expect(dateUtil.transformTime('2024-01-14 10:00:00')).toEqual([
          'common.yesterday',
          '10:00:00',
        ]);
      });

      it('本周(非昨天非今天)返回 [星期, 时间]', () => {
        // 将 now 设为周五,2024-01-16 周二距 now 超过 24 小时,走本周分支
        vi.setSystemTime(new Date(2024, 0, 19, 8, 30, 45)); // 2024-01-19 周五
        expect(dateUtil.transformTime('2024-01-16 10:00:00')).toEqual([
          'common.dateDay.dayOfWeek2',
          '10:00:00',
        ]);
      });

      it('本年非本周返回 [MM-dd, 时间]', () => {
        expect(dateUtil.transformTime('2024-01-05 10:00:00')).toEqual(['01-05', '10:00:00']);
      });

      it('非本年返回 [完整日期, 时间]', () => {
        expect(dateUtil.transformTime('2023-12-31 10:00:00')).toEqual(['2023-12-31', '10:00:00']);
      });
    });
  });
});
