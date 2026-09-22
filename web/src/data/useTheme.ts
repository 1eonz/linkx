class ThemeService {
  private currentTheme: string = 'light';
  private themeChangeCallbacks: Array<(theme: string) => void> = [];
  private locked: boolean = false; // 主题锁定标志

  private notifyThemeChange(theme: string) {
    this.themeChangeCallbacks.forEach((callback) => callback(theme));
  }

  private normalizeTheme(input: unknown): string {
    if (typeof input !== 'string') return 'light';
    const t = input.trim();
    if (!t) return 'light';

    const lower = t.toLowerCase();
    // 历史拼写错误
    if (lower === 'ligth') return 'light';

    // 正常值透传（light/dark）
    return t;
  }

  // 获取当前主题
  getCurrentTheme(): string {
    return this.currentTheme;
  }

  // 获取锁定状态
  isLocked(): boolean {
    return this.locked;
  }

  // 初始化主题
  init() {
    const savedTheme = localStorage.getItem('theme') || 'light';
    this.setTheme(savedTheme);
  }

  // 锁定主题（WebView2环境下使用）
  lockTheme(theme: string) {
    const normalized = this.normalizeTheme(theme);
    this.currentTheme = normalized;
    document.documentElement.dataset.theme = normalized;
    localStorage.setItem('theme', normalized);
    this.locked = true;
    this.notifyThemeChange(normalized);
  }

  // 监听主题变化
  onThemeChange(callback: (theme: string) => void) {
    this.themeChangeCallbacks.push(callback);
  }

  // 设置主题
  setTheme(theme: string) {
    // 锁定后不可变更
    if (this.locked) return;

    const normalized = this.normalizeTheme(theme);
    this.currentTheme = normalized;
    document.documentElement.dataset.theme = normalized;
    localStorage.setItem('theme', normalized);
    this.notifyThemeChange(normalized);
  }

  // 切换主题
  toggleTheme() {
    // 锁定后不可变更
    if (this.locked) return this.currentTheme;

    const newTheme = this.currentTheme === 'light' ? 'dark' : 'light';
    this.setTheme(newTheme);
    return newTheme;
  }
}

export const themeService = new ThemeService();
