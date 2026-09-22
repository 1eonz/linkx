import { vi, afterEach } from 'vitest';

// 全局 mock vant，避免 H5Portal/src/utils/index.js 导入时触发副作用
vi.mock('vant', () => ({
  Popup: { name: 'Popup' },
  showToast: vi.fn(),
  showConfirmDialog: vi.fn(),
  showDialog: vi.fn(),
}));

// 每个 case 后清理 mock，避免状态污染
afterEach(() => {
  vi.restoreAllMocks();
  vi.useRealTimers();
  localStorage.clear();
  sessionStorage.clear();
});
