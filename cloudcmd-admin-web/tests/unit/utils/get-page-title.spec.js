import { vi, describe, it, expect, beforeEach } from 'vitest';

// mock @/locales（vue-i18n）
vi.mock('@/locales', () => ({
  default: {
    t: (key, params) => `页面标题-${params?.title || 'default'}`,
  },
}));

// mock @/store（Vuex）
vi.mock('@/store', () => ({
  default: {
    state: {
      settings: {
        systemName: 'TestSystem',
      },
    },
  },
}));

import getPageTitle from '@/utils/get-page-title';

describe('utils/get-page-title', () => {
  it('传入 pageTitle 时应拼接 "pageTitle - title"', () => {
    expect(getPageTitle('首页')).toBe('首页 - 页面标题-TestSystem');
  });

  it('未传入 pageTitle 时应只返回 title', () => {
    expect(getPageTitle()).toBe('页面标题-TestSystem');
  });

  it('传入空字符串时应只返回 title', () => {
    expect(getPageTitle('')).toBe('页面标题-TestSystem');
  });

  it('传入 null 时应只返回 title', () => {
    expect(getPageTitle(null)).toBe('页面标题-TestSystem');
  });
});
