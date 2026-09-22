import { vi, afterEach } from 'vitest';

// 每个 case 后清理 mock，避免状态污染
afterEach(() => {
  vi.restoreAllMocks();
  vi.useRealTimers();
  localStorage.clear();
  sessionStorage.clear();
});
