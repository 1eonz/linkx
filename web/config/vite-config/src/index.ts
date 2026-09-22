import type { ConfigEnv, UserConfig } from 'vite';

import { resolve } from 'path';

import gzip from 'rollup-plugin-gzip';
import { loadEnv } from 'vite';

import { createVitePlugins } from './options';
import { createProxy } from './proxy';
import { wrapperEnv } from './utils';
import basicSsl from '@vitejs/plugin-basic-ssl'; // 导入插件

// eslint-disable-next-line n/prefer-global/process
const pathResolve = (dir: string) => resolve(process.cwd(), '.', dir);

// https://vitejs.dev/config/
const defineConfig = ({ command, mode }: ConfigEnv): UserConfig => {
  // eslint-disable-next-line n/prefer-global/process
  const root = process.cwd();
  const env = loadEnv(mode, root);
  const viteEnv = wrapperEnv(env) as any;
  const { VITE_PORT, VITE_PROXY, VITE_PUBLIC_PATH } = viteEnv;
  const isBuild = command === 'build';

  const ret: any = {
    base: VITE_PUBLIC_PATH,
    build: {
      assetsDir: 'static',
      chunkSizeWarningLimit: 500,
      outDir: 'cloudcmd',
      rollupOptions: {
        output: {
          assetFileNames: 'static/[ext]/[name]-[hash].[ext]',
          chunkFileNames: 'static/js/[name]-[hash].js',
          entryFileNames: 'static/js/[name]-[hash].js',
          // 手动分割代码为更小的块
          manualChunks(id) {
            const _id = id.toString();
            if (_id.includes('node_modules')) {
              if (_id.includes('element-plus')) {
                return 'element-plus';
              }
              if (_id.includes('echarts')) {
                return 'echarts';
              }
              if (_id.includes('arcgis')) {
                return 'arcgis';
              }
              if (_id.includes('vue')) {
                return 'vue';
              }
              if (_id.includes('esri')) {
                return 'esri';
              }
              if (_id.split('/').includes('ol')) {
                return 'ol';
              }
              return id.toString().split('node_modules/')[1].split('/')[0].toString();
            }
          },
        },
        plugins: [
          // Nginx上需要配置 gzip_static on; # 开启 Gzip 静态文件支持
          gzip({
            gzipOptions: {
              level: 9, // 压缩级别，范围1-9
            },
          }),
        ],
      },
    },
    define: {
      __INTLIFY_PROD_DEVTOOLS__: false,
      __VUE_I18N_FULL_INSTALL__: true,
      __VUE_I18N_LEGACY_API__: true,
    },
    plugins: [basicSsl(), ...createVitePlugins(viteEnv, isBuild)],
    resolve: {
      alias: [
        // {
        //   find: 'vue-i18n',
        //   replacement: 'vue-i18n/dist/vue-i18n.cjs.js',
        // },
        // @/xxxx => src/xxxx
        {
          find: /@\//,
          replacement: `${pathResolve('src')}/`,
        },
        // #/xxxx => types/xxxx
        {
          find: /#\//,
          replacement: `${pathResolve('types')}/`,
        },
      ],
    },
    root,
    server: {
      https: true,
      // hot update
      // hmr: true,
      // Listening on all local IPs
      host: true,
      port: VITE_PORT,
      // Load proxy configuration from .env
      proxy: createProxy(VITE_PROXY),
    },
  };

  return ret;
};

export default defineConfig;
