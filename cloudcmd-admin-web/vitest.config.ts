import { defineConfig } from 'vitest/config';
import { resolve } from 'path';

export default defineConfig({
  resolve: {
    alias: [
      {
        find: '@/layout',
        replacement: resolve(__dirname, 'tests/unit/__mocks__/layout-stub.js'),
      },
      {
        find: /@\/views\/.*/,
        replacement: resolve(__dirname, 'tests/unit/__mocks__/view-stub.js'),
      },
      {
        find: /@\/(.*)/,
        replacement: resolve(__dirname, 'src/$1'),
      },
    ],
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['tests/unit/**/*.{spec,test}.{js,ts}'],
    // 跳过依赖 .vue 文件的测试（因 Vue 2.6 不兼容 @vitejs/plugin-vue2）
    exclude: ['node_modules', 'dist', 'tests/unit/components/**/*.spec.js'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      include: ['src/utils/**/*.{js,vue}'],
      exclude: [
        'src/utils/auth.js',
        'src/utils/request.js',
        'src/utils/createDialog.js',
        'src/utils/astrict.js',
        'src/utils/pageLoading.js',
        'src/utils/scrollTo.js',
      ],
      thresholds: {
        statements: 75,
        branches: 70,
        functions: 65,
        lines: 75,
      },
    },
    setupFiles: ['./tests/unit/setup.js'],
    server: {
      deps: {
        // 将 .vue 文件 inline 处理为空组件
        inline: [/\.vue$/],
      },
    },
  },
});
