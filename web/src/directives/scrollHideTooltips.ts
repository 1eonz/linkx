import type { App, Directive } from 'vue';

import { TooltipEnum } from '@/enums';
import { on } from '@/utils/domUtils';

/**
 * 解决列表出现滚动时 tooltips 错位
 */
const scrollHideTooltipsDirective: Directive = {
  mounted(el) {
    on(el, 'scroll', () => {
      const elList: HTMLCollectionOf<Element> = document.getElementsByClassName(
        TooltipEnum.POPPER_CLASS,
      );

      [...(elList as any)].forEach((tipItem: HTMLElement) => {
        if (!tipItem) return;

        tipItem.style.display = 'none';
        tipItem.style.zIndex = '-1';
      });
    });
  },
};

export function setupScrollHideTooltipsDirective(app: App) {
  app.directive('scrollHideTooltips', scrollHideTooltipsDirective);
}

export default setupScrollHideTooltipsDirective;
