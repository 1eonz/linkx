import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// useTheme 的 themeService 是模块级单例,且 lockTheme 后无法解锁
// 通过 vi.resetModules() + 动态 import() 获取新实例,保证测试隔离
async function loadThemeService() {
  vi.resetModules();
  const mod = (await import('@/data/useTheme')) as { themeService: any };
  return mod.themeService;
}

describe('useTheme - 主题服务', () => {
  let originalDataset: DOMStringMap;

  beforeEach(() => {
    originalDataset = { ...document.documentElement.dataset };
    localStorage.clear();
  });

  afterEach(() => {
    // 还原 dataset
    for (const key of Object.keys(document.documentElement.dataset)) {
      delete document.documentElement.dataset[key];
    }
    Object.assign(document.documentElement.dataset, originalDataset);
    localStorage.clear();
    vi.restoreAllMocks();
    vi.resetModules();
  });

  describe('setTheme - 设置主题', () => {
    it('设置 light 主题', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme('light');
      expect(themeService.getCurrentTheme()).toBe('light');
      expect(document.documentElement.dataset.theme).toBe('light');
      expect(localStorage.getItem('theme')).toBe('light');
    });

    it('设置 dark 主题', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme('dark');
      expect(themeService.getCurrentTheme()).toBe('dark');
      expect(document.documentElement.dataset.theme).toBe('dark');
      expect(localStorage.getItem('theme')).toBe('dark');
    });

    it('设置主题会触发 onThemeChange 回调', async () => {
      const themeService = await loadThemeService();
      const cb = vi.fn();
      themeService.onThemeChange(cb);
      themeService.setTheme('dark');
      expect(cb).toHaveBeenCalledWith('dark');
    });

    it('normalizeTheme 容错: "ligth"（拼写错误）会被纠正为 "light"', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme('ligth');
      expect(themeService.getCurrentTheme()).toBe('light');
      expect(document.documentElement.dataset.theme).toBe('light');
      expect(localStorage.getItem('theme')).toBe('light');
    });

    it('normalizeTheme 容错: "LIGTH" 大小写也会被纠正', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme('LIGTH');
      expect(themeService.getCurrentTheme()).toBe('light');
    });

    it('normalizeTheme 容错: 非字符串输入会回退到 light', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme(123 as unknown as string);
      expect(themeService.getCurrentTheme()).toBe('light');
    });

    it('normalizeTheme 容错: null 输入会回退到 light', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme(null as unknown as string);
      expect(themeService.getCurrentTheme()).toBe('light');
    });

    it('normalizeTheme 容错: undefined 输入会回退到 light', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme(undefined as unknown as string);
      expect(themeService.getCurrentTheme()).toBe('light');
    });

    it('normalizeTheme 容错: 空字符串会回退到 light', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme('');
      expect(themeService.getCurrentTheme()).toBe('light');
    });

    it('normalizeTheme 容错: 空白字符串会回退到 light', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme('   ');
      expect(themeService.getCurrentTheme()).toBe('light');
    });

    it('normalizeTheme 正常值透传（不转换大小写）', async () => {
      const themeService = await loadThemeService();
      // 注意: normalizeTheme 对非 "ligth" 的值直接返回原值（含大小写）
      themeService.setTheme('Dark');
      expect(themeService.getCurrentTheme()).toBe('Dark');
    });
  });

  describe('getCurrentTheme / isLocked - 初始状态', () => {
    it('初始主题为 light', async () => {
      const themeService = await loadThemeService();
      expect(themeService.getCurrentTheme()).toBe('light');
    });

    it('初始锁定状态为 false', async () => {
      const themeService = await loadThemeService();
      expect(themeService.isLocked()).toBe(false);
    });
  });

  describe('toggleTheme - 切换主题', () => {
    it('light → dark', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme('light');
      const newTheme = themeService.toggleTheme();
      expect(newTheme).toBe('dark');
      expect(themeService.getCurrentTheme()).toBe('dark');
    });

    it('dark → light', async () => {
      const themeService = await loadThemeService();
      themeService.setTheme('dark');
      const newTheme = themeService.toggleTheme();
      expect(newTheme).toBe('light');
      expect(themeService.getCurrentTheme()).toBe('light');
    });

    it('切换主题会触发 onThemeChange 回调', async () => {
      const themeService = await loadThemeService();
      const cb = vi.fn();
      themeService.onThemeChange(cb);
      themeService.setTheme('light');
      cb.mockClear();
      themeService.toggleTheme();
      expect(cb).toHaveBeenCalledWith('dark');
    });
  });

  describe('lockTheme - 锁定主题', () => {
    it('锁定后 isLocked 返回 true', async () => {
      const themeService = await loadThemeService();
      themeService.lockTheme('dark');
      expect(themeService.isLocked()).toBe(true);
    });

    it('锁定后 setTheme 不再生效', async () => {
      const themeService = await loadThemeService();
      themeService.lockTheme('dark');
      themeService.setTheme('light');
      expect(themeService.getCurrentTheme()).toBe('dark');
    });

    it('锁定后 toggleTheme 不再生效,返回当前主题', async () => {
      const themeService = await loadThemeService();
      themeService.lockTheme('dark');
      const result = themeService.toggleTheme();
      expect(result).toBe('dark');
      expect(themeService.getCurrentTheme()).toBe('dark');
    });

    it('lockTheme 也会写入 dataset 和 localStorage', async () => {
      const themeService = await loadThemeService();
      themeService.lockTheme('dark');
      expect(document.documentElement.dataset.theme).toBe('dark');
      expect(localStorage.getItem('theme')).toBe('dark');
    });

    it('lockTheme 触发 onThemeChange 回调', async () => {
      const themeService = await loadThemeService();
      const cb = vi.fn();
      themeService.onThemeChange(cb);
      themeService.lockTheme('dark');
      expect(cb).toHaveBeenCalledWith('dark');
    });

    it('lockTheme 也走 normalizeTheme 容错', async () => {
      const themeService = await loadThemeService();
      themeService.lockTheme('ligth');
      expect(themeService.getCurrentTheme()).toBe('light');
    });
  });

  describe('onThemeChange - 监听主题变化', () => {
    it('多个回调都会被触发', async () => {
      const themeService = await loadThemeService();
      const cb1 = vi.fn();
      const cb2 = vi.fn();
      themeService.onThemeChange(cb1);
      themeService.onThemeChange(cb2);
      themeService.setTheme('dark');
      expect(cb1).toHaveBeenCalledWith('dark');
      expect(cb2).toHaveBeenCalledWith('dark');
    });

    it('回调会按注册顺序依次被调用', async () => {
      const themeService = await loadThemeService();
      const order: string[] = [];
      themeService.onThemeChange(() => order.push('first'));
      themeService.onThemeChange(() => order.push('second'));
      themeService.onThemeChange(() => order.push('third'));
      themeService.setTheme('dark');
      expect(order).toEqual(['first', 'second', 'third']);
    });

    it('每次 setTheme 都会触发回调', async () => {
      const themeService = await loadThemeService();
      const cb = vi.fn();
      themeService.onThemeChange(cb);
      themeService.setTheme('dark');
      themeService.setTheme('light');
      themeService.setTheme('dark');
      expect(cb).toHaveBeenCalledTimes(3);
    });
  });

  describe('init - 从 localStorage 初始化', () => {
    it('localStorage 无 theme 时使用 light 默认值', async () => {
      const themeService = await loadThemeService();
      localStorage.removeItem('theme');
      themeService.init();
      expect(themeService.getCurrentTheme()).toBe('light');
      expect(localStorage.getItem('theme')).toBe('light');
    });

    it('localStorage 有 dark 时使用 dark', async () => {
      const themeService = await loadThemeService();
      localStorage.setItem('theme', 'dark');
      themeService.init();
      expect(themeService.getCurrentTheme()).toBe('dark');
    });

    it('init 会同步写入 dataset', async () => {
      const themeService = await loadThemeService();
      localStorage.setItem('theme', 'dark');
      themeService.init();
      expect(document.documentElement.dataset.theme).toBe('dark');
    });

    it('init 会触发 onThemeChange 回调', async () => {
      const themeService = await loadThemeService();
      const cb = vi.fn();
      themeService.onThemeChange(cb);
      localStorage.setItem('theme', 'dark');
      themeService.init();
      expect(cb).toHaveBeenCalledWith('dark');
    });
  });

  describe('锁定与初始化交互', () => {
    it('锁定后 init 仍会变更主题（init 调用 setTheme,但锁定后 setTheme 无效）', async () => {
      const themeService = await loadThemeService();
      themeService.lockTheme('dark');
      localStorage.setItem('theme', 'light');
      themeService.init();
      // 锁定后 setTheme 不生效,主题仍为 dark
      expect(themeService.getCurrentTheme()).toBe('dark');
    });
  });
});
