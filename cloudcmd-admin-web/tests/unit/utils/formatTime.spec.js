import { vi, describe, it, expect, beforeEach } from 'vitest';

// mock 语言包（避免 require.context webpack API 在 Vite 下不可用）
vi.mock('@/locales/lang/cn', () => ({
  default: { message: { index: { dateDay: {
    dayOfWeek0: '星期日', dayOfWeek1: '星期一', dayOfWeek2: '星期二',
    dayOfWeek3: '星期三', dayOfWeek4: '星期四', dayOfWeek5: '星期五', dayOfWeek6: '星期六',
    month: '月', day: '日', hour: '时', minute: '分',
    justNow: '刚刚', minutesAgo: '分钟前', hoursBefore: '小时前', oneDayBefore: '一天前',
  } } } },
}));
vi.mock('@/locales/lang/en', () => ({
  default: { message: { index: { dateDay: {} } } },
}));
vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => ''),
  setToken: vi.fn(),
  removeToken: vi.fn(),
}));

import { formatTime } from '@/utils/index.js';
import languageCn from '@/locales/lang/cn';

beforeEach(() => {
  localStorage.setItem('localLanguage', 'cn');
});

describe('Utils:formatTime', () => {
  const d = new Date('2018-07-13 17:54:01');
  const retrofit = 5 * 1000;
  const languageType = languageCn.message.index;

  it('应处理 10 位时间戳', () => {
    expect(formatTime((d / 1000).toFixed(0))).toBe(
      `7${languageType.dateDay.month}13${languageType.dateDay.day}17${languageType.dateDay.hour}54${languageType.dateDay.minute}`,
    );
  });

  it('应返回"刚刚"（1 秒内）', () => {
    expect(formatTime(+new Date() - 1)).toBe(languageType.dateDay.justNow);
  });

  it('应返回"2 分钟前"', () => {
    expect(formatTime(+new Date() - 60 * 2 * 1000 + retrofit)).toBe(
      `2${languageType.dateDay.minutesAgo}`,
    );
  });

  it('应返回"2 小时前"', () => {
    expect(formatTime(+new Date() - 60 * 60 * 2 * 1000 + retrofit)).toBe(
      `2${languageType.dateDay.hoursBefore}`,
    );
  });

  it('应返回"1 天前"', () => {
    expect(formatTime(+new Date() - 60 * 60 * 24 * 1 * 1000)).toBe(
      languageType.dateDay.oneDayBefore,
    );
  });

  it('超过一天应返回格式化日期', () => {
    expect(formatTime(d)).toBe(
      `7${languageType.dateDay.month}13${languageType.dateDay.day}17${languageType.dateDay.hour}54${languageType.dateDay.minute}`,
    );
  });

  it('应支持自定义格式', () => {
    expect(formatTime(d, '{y}-{m}-{d} {h}:{i}')).toBe('2018-07-13 17:54');
    expect(formatTime(d, '{y}-{m}-{d}')).toBe('2018-07-13');
    expect(formatTime(d, '{y}/{m}/{d} {h}-{i}')).toBe('2018/07/13 17-54');
  });
});
