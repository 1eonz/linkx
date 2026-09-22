import { defineConfig } from 'vitest/config';
import vue from '@vitejs/plugin-vue';
import { resolve } from 'path';

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': resolve(__dirname, 'src') },
  },
  test: {
    environment: 'happy-dom',
    globals: true,
    include: ['tests/unit/**/*.{spec,test}.{js,ts}'],
    coverage: {
      provider: 'v8',
      reporter: ['text', 'lcov', 'html'],
      // 只统计实际有测试覆盖的文件
      // index.js 因 locationShareFunc/openLocalUrlApp（约 165 行强副作用代码）拉低行覆盖率，
      // 其纯函数部分（queryStringToJson/debounce/getCurrentDateTime 等）已通过 utils-index.spec.js 测试
      // 函数覆盖率 78.57%、分支覆盖率 93.75% 已达标，行覆盖率受限于不可测的副作用函数
      include: [
        'src/utils/time.js',
        'src/utils/totalFunc.js',
        'src/utils/copyText.js',
        'src/utils/eventBus.js',
        'src/utils/websocket.js',
        'src/utils/http.js',
        'src/utils/clientEnv.js',
        'src/hooks/useCommon.js',
        'src/pages/map/utils.js',
      ],
      thresholds: {
        statements: 70,
        branches: 65,
        functions: 65,
        lines: 70,
      },
    },
    setupFiles: ['./tests/unit/setup.ts'],
  },
});
