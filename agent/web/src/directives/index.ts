import type { App } from 'vue';

import setupClickOutside from './clickOutside';

export function setupGlobDirectives(app: App) {
  setupClickOutside(app);
}
