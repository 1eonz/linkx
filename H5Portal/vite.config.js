import { VantResolver } from '@vant/auto-import-resolver';
import legacy from '@vitejs/plugin-legacy';
import vue from '@vitejs/plugin-vue';
// import configApi from "./config/env.js";
import { resolve } from 'path';
//vant
import AutoImport from 'unplugin-auto-import/vite';
import Components from 'unplugin-vue-components/vite';
import { defineConfig } from 'vite';

//vant
import { dynamicBase } from './vite-plugin-dynamic-base.js';

// gzip 压缩插件 (使用 vite-plugin-compression2 兼容 Vite 7.x)
import { compression } from 'vite-plugin-compression2';

// console 前缀插件
import { consolePrefixPlugin } from './vite-plugin-console-prefix.js';

// 获取当前环境，如果没有设置则默认为开发环境
// const currentEnv =  import.meta.env?.UNI_CUSTOM_SCRIPT || "dev";

export default defineConfig({
  base: './',
  plugins: [
    dynamicBase(), // 动态设置 base 标签
    vue(),
    // console 前缀插件 - 生产环境为 console 添加统一前缀
    consolePrefixPlugin({
      enabled: true,
      include: /\.(js|ts|vue)$/,
      exclude: /node_modules/,
    }),
    //vant
    AutoImport({
      resolvers: [VantResolver()],
    }),
    Components({
      resolvers: [VantResolver()],
    }),
    //vant
    legacy({
      targets: ['chrome 52', 'ie >= 11'], // 指定兼容的浏览器版本
      additionalLegacyPolyfills: ['regenerator-runtime/runtime'], // 针对 IE11 的额外 Polyfill
      polyfills: [
        'es.symbol',
        'es.promise',
        'es.array.filter',
        'es.object.define-properties',
        'es.object.keys',
        // 其他需要的 Polyfill
      ],
      renderLegacyChunks: true,
    }),
    // gzip 压缩 - 生产环境启用 (vite-plugin-compression2)
    compression({
      algorithms: ['gzip', 'brotli'], // 同时生成 gzip 和 brotli 格式
      threshold: 10240, // 大于 10KB 的文件才压缩
      deleteOriginalAssets: false, // 保留原文件
    }),
  ],
  resolve: {
    alias: {
      // 设置 @ 指向 src 目录
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    host: '0.0.0.0',
    port: 8001,
    proxy: {
      '/linkx/h5portal/api': {
        target: `http://10.28.15.61:30280`,
        changeOrigin: true,
        secure: false,
      },
      '/linkx/h5portal/admin': {
        target: `http://10.28.15.61:30280`,
        changeOrigin: true,
        secure: false,
      },
      '/linkx/h5portal/base': {
        target: `http://10.28.15.61:30280`,
        changeOrigin: true,
        secure: false,
      },
      '/linkx/h5portal/oauth': {
        target: `http://10.28.15.61:30280`,
        changeOrigin: true,
        secure: false,
      },
      '/linkx/h5portal/collaboration': {
        target: `http://10.28.15.61:30280`,
        changeOrigin: true,
        secure: false,
      },
      '/linkx/h5portal/tasks': {
        target: `http://10.28.15.61:30280`,
        changeOrigin: true,
        secure: false,
      },
      '/linkx/h5portal/XA-ics-agent': {
        target: `http://10.28.15.61:30280`,
        changeOrigin: true,
        secure: false,
      },
      '/cagent': {
        target: `http://10.28.15.61:30280`,
        ws: true,
      },
    },
  },
  build: {
    target: 'es2015', // 指定构建目标为 ES2015
    outDir: 'unpackage/dist/build/web',
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (id.includes('node_modules')) {
            // Vue 核心
            if (id.includes('vue') || id.includes('pinia')) {
              return 'vue-stack';
            }
            // Vant UI 组件库
            if (id.includes('vant')) {
              return 'vant';
            }
            // ECharts 图表库 - 仅在需要图表的页面加载
            if (id.includes('echarts')) {
              return 'echarts';
            }
            // OpenLayers 地图库 - 非常大，单独分包
            if (id.includes('/ol/') || id.includes('ol/ol.css')) {
              return 'openlayers';
            }
            // Markdown 解析 - 仅在 AI 助手页面加载
            if (id.includes('marked')) {
              return 'marked';
            }
            // 拖拽排序 - 仅在应用管理页面加载
            if (id.includes('sortablejs')) {
              return 'sortablejs';
            }
            // 通用工具库
            if (
              id.includes('axios') ||
              id.includes('query-string') ||
              id.includes('destr') ||
              id.includes('regenerator-runtime')
            ) {
              return 'utils';
            }
            // 其他第三方库
            return 'vendor';
          }
          // OpenLayers 源码也单独分包
          if (id.includes('/plugins/map/src/OpenLayers')) {
            return 'openlayers';
          }
        },
      },
    },
  },
  optimizeDeps: {
    force: true, // 强制重新预构建依赖
    include: ['core-js/stable'], // 明确包含 core-js/stable
  },
  // define: {
  //   "process.env": process.env,
  //   "process.env.config": configApi[currentEnv],
  // },
});
