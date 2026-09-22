import { createHtmlPlugin } from 'vite-plugin-html';

export const htmlPlugin = (env) => {
  return createHtmlPlugin({
    inject: {
      data: {
        ...env,
        injectScript: ``,
      },
    },
  });
};
