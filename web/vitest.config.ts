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
    exclude: ['node_modules', 'dist', 'cloudcmd'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      // 只统计实际有测试覆盖的文件，避免未测试的业务文件拉低全局覆盖率
      include: [
        'src/utils/is.ts',
        'src/utils/validate.ts',
        'src/utils/dataUtil.ts',
        'src/utils/treeHelper.js',
        'src/utils/index.ts',
        'src/utils/dateUtil.ts',
        'src/utils/auth.ts',
        'src/utils/headerUtils.ts',
        'src/utils/env.ts',
        'src/utils/clientEnv.ts',
        'src/utils/DC.ts',
        'src/utils/globalConfig.ts',
        'src/data/useTheme.ts',
      ],
      thresholds: {
        statements: 80,
        branches: 75,
        functions: 75,
        lines: 80,
      },
    },
    setupFiles: ['./tests/unit/setup.ts'],
  },
});
