import type { App } from 'vue';

import setupClickOutside from './clickOutside';
import setupHyperlinkClick from './hyperlinkClick';
import setupScrollHideTooltipsDirective from './scrollHideTooltips';

export function setupGlobDirectives(app: App) {
  setupClickOutside(app);
  setupScrollHideTooltipsDirective(app);
  setupHyperlinkClick(app);
}
