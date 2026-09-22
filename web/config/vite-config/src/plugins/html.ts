import { createHtmlPlugin } from 'vite-plugin-html';

export const htmlPlugin = (env) => {
  return createHtmlPlugin({
    inject: {
      data: {
        ...env,
        injectScript: `<script src="/libs/huaweiSDK/ICPSDK_ALL.js"></script> <script src="/libs/huaweiSDK/msp-yuv-player.umd.js"></script>`,
      },
    },
  });
};
