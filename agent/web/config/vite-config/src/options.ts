import type { PluginOption } from 'vite';

import vue from '@vitejs/plugin-vue';
import vueJsx from '@vitejs/plugin-vue-jsx';
import WindiCSS from 'vite-plugin-windicss';

import { htmlPlugin } from './plugins/html';
import { configSvgIconsPlugin } from './plugins/svgSprite';
import { unusedFilesPlugin } from './plugins/unusedFiles';
import { visualizerPlugin } from './plugins/visualizer';

export function createVitePlugins(viteEnv: ViteEnv, isBuild: boolean) {
  // const {} = viteEnv;

  const vitePlugins: (PluginOption | PluginOption[])[] = [
    // have to
    vue(),
    // have to
    vueJsx(),
  ];

  // vite-plugin-svg-icons
  vitePlugins.push(configSvgIconsPlugin(isBuild));

  vitePlugins.push(WindiCSS());

  vitePlugins.push(visualizerPlugin());

  vitePlugins.push(htmlPlugin(viteEnv));

  vitePlugins.push(unusedFilesPlugin());

  return vitePlugins;
}
