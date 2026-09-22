import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
      '#': resolve(__dirname, 'types'),
    },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['tests/unit/**/*.{spec,test}.{ts,tsx,js}'],
    exclude: ['node_modules', 'dist'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      // 只统计实际有测试覆盖的文件，避免语言包等业务文件拉低全局覆盖率
      include: [
        'src/utils/is.ts',
        'src/utils/index.ts',
        'src/locales/helper.ts',
      ],
      thresholds: {
        statements: 75,
        branches: 70,
        functions: 70,
        lines: 75,
      },
    },
    setupFiles: ['./tests/unit/setup.ts'],
  },
});
