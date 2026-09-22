import { ref } from 'vue';

const themes = [
  { label: '浅色', name: 'light' },
  { label: '深色', name: 'dark' },
];

const normalizeTheme = (input: unknown): string => {
  if (typeof input !== 'string') return 'light';
  const t = input.trim();
  if (!t) return 'light';
  const lower = t.toLowerCase();
  if (lower === 'ligth') return 'light';
  return t;
};

export function useTheme() {
  const currentTheme = ref(normalizeTheme(localStorage.getItem('theme') || 'light'));

  // 应用主题
  const applyTheme = (themeName) => {
    const normalized = normalizeTheme(themeName);
    document.documentElement.dataset.theme = normalized;
    localStorage.setItem('theme', normalized);
    currentTheme.value = normalized;
  };

  // 初始化主题
  const initTheme = () => {
    applyTheme(currentTheme.value);
  };

  // 切换主题
  const toggleTheme = (themeName) => {
    applyTheme(themeName);
  };

  // 轮换主题
  const cycleThemes = () => {
    const currentIndex = themes.findIndex((t) => t.name === currentTheme.value);
    const nextIndex = (currentIndex + 1) % themes.length;
    applyTheme(themes[nextIndex].name);
  };

  return {
    currentTheme,
    cycleThemes,
    initTheme,
    themes,
    toggleTheme,
  };
}
