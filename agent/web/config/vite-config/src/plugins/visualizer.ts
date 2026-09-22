import { visualizer } from 'rollup-plugin-visualizer';

// build visual
export const visualizerPlugin = (): any => {
  return visualizer({
    brotliSize: true,
    filename: 'buildVisual.html',
    gzipSize: true,
    open: true,
  });
};
