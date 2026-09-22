/**
 * Vite Plugin for fast creating SVG sprites.
 * https://github.com/anncwb/vite-plugin-svg-icons
 */

import { resolve } from 'path';

import { createSvgIconsPlugin } from 'vite-plugin-svg-icons';

export function configSvgIconsPlugin(isBuild: boolean) {
  const svgIconsPlugin = createSvgIconsPlugin({
    // 指定需要缓存的图标文件夹
    // eslint-disable-next-line n/prefer-global/process
    iconDirs: [resolve(process.cwd(), 'src/assets/svg')],
    svgoOptions: isBuild,
    // 指定symbolId格式
    symbolId: 'icon-[dir]-[name]',
  });
  return svgIconsPlugin;
}
