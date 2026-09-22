import { vi, describe, it, expect, beforeEach } from 'vitest';

// mock 语言包（避免 require.context webpack API 在 Vite 下不可用）
// dayOfWeek0=星期日, dayOfWeek1=星期一, ..., dayOfWeek5=星期五, dayOfWeek6=星期六
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
// mock @/utils/auth（切断 request → router → layout.vue 依赖链）
vi.mock('@/utils/auth', () => ({
  getToken: vi.fn(() => ''),
  setToken: vi.fn(),
  removeToken: vi.fn(),
}));

import { parseTime } from '@/utils/index.js';
import languageCn from '@/locales/lang/cn';

// 移除顶层 localStorage 访问，改为 beforeEach 中 mock
beforeEach(() => {
  localStorage.setItem('localLanguage', 'cn');
});

describe('Utils:parseTime', () => {
  const d = new Date('2018-07-13 17:54:01');
  const languageType = languageCn.message.index;

  it('应格式化 Date 对象', () => {
    expect(parseTime(d)).toBe('2018-07-13 17:54:01');
  });

  it('应处理 10 位时间戳', () => {
    expect(parseTime((d / 1000).toFixed(0))).toBe('2018-07-13 17:54:01');
  });

  it('应处理 new Date 实例', () => {
    expect(parseTime(new Date(d))).toBe('2018-07-13 17:54:01');
  });

  it('应支持自定义格式', () => {
    expect(parseTime(d, '{y}-{m}-{d} {h}:{i}')).toBe('2018-07-13 17:54');
    expect(parseTime(d, '{y}-{m}-{d}')).toBe('2018-07-13');
    expect(parseTime(d, '{y}/{m}/{d} {h}-{i}')).toBe('2018/07/13 17-54');
  });

  it('应返回星期几（2018-07-13 是星期五 → dayOfWeek5）', () => {
    expect(parseTime(d, '{a}')).toBe(`${languageType.dateDay.dayOfWeek5}`);
  });

  it('应返回正确的星期几（+2 天 = 2018-07-15 星期日 → dayOfWeek0）', () => {
    expect(parseTime(+d + 1000 * 60 * 60 * 24 * 2, '{a}')).toBe(
      `${languageType.dateDay.dayOfWeek0}`,
    );
  });

  it('空参数应返回 null', () => {
    expect(parseTime()).toBeNull();
  });
});
