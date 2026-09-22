import type { App } from 'vue';

import * as ElementPlusIconsVue from '@element-plus/icons-vue';

const modules = import.meta.glob('./**/*.ts', { eager: true });

export function registerGlobComp(app: App) {
  // el plus icon
  for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
    app.component(key, component);
  }

  for (const path in modules) {
    const paths = path.split('/');
    if (paths.length === 3) {
      const mod = (modules[path] as any).default;
      if (mod && typeof mod === 'object' && mod.name) {
        app.use(mod);
      }
    }
  }
}
