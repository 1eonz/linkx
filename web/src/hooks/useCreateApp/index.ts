import type { App, Component, ComponentOptions } from 'vue';
import { createApp } from 'vue';
import screenShort from 'vue-web-screen-shot';

import { registerGlobComp } from '@/components/register';
import { setupGlobDirectives } from '@/directives';
import { elementLocale, setupI18n } from '@/locales';
import { setupRouter } from '@/router/index2';
import { setupStore } from '@/store';
import VeeValidate from '@/utils/VeeValidate';

import ElementPlus from 'element-plus';

/**
 * 创建 vue 实例方法
 * @param comp 组件
 * @param options 组件参数
 * @returns app
 */
export const useCreateApp = (comp: Component, options?: ComponentOptions): App => {
  // 通过 createApp 创建的实例都需要再注册一遍全局组件
  const app = createApp(comp, options);

  // pinia store
  setupStore(app);

  // 注册全局组件
  registerGlobComp(app);

  // 自定义指令
  setupGlobDirectives(app);

  // 路由
  setupRouter(app);

  // 国际化
  setupI18n(app);

  // 表单校验
  app.use(VeeValidate);

  // 注册ElementPlus
  app.use(ElementPlus, {
    locale: elementLocale(),
  });

  // 使用截屏插件
  app.use(screenShort, {
    enableWebRtc: true,
    // 隐藏滚动条，防止页面挤压
    hiddenScrollBar: {
      color: '#000000',
      fillHeight: 40, // 对应您提到的40像素高度
      fillState: true,
      fillWidth: window.innerWidth,
      state: true,
    },
    // 防止页面滚动位置变化
    level: 9999,
    wrcWindowMode: true,
  });

  return app;
};
