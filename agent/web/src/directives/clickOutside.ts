import type { App, ComponentPublicInstance, Directive, DirectiveBinding } from 'vue';

import { on } from '@/utils/domUtils';
import { isServer } from '@/utils/is';

type DocumentHandler = <T extends MouseEvent>(mouseup: T, mousedown: T) => void;

type FlushList = Map<
  HTMLElement,
  {
    bindingFn: (...args: unknown[]) => unknown;
    documentHandler: DocumentHandler;
  }
>;

const nodeList: FlushList = new Map();

let startClick: MouseEvent;

if (!isServer) {
  on(document, 'mousedown', (e: MouseEvent) => (startClick = e));
  on(document, 'mouseup', (e: MouseEvent) => {
    for (const { documentHandler } of nodeList.values()) {
      documentHandler(e, startClick);
    }
  });
}

function createDocumentHandler(el: HTMLElement, binding: DirectiveBinding): DocumentHandler {
  let excludes: HTMLElement[] = [];
  if (Array.isArray(binding.arg)) {
    excludes = binding.arg;
  } else {
    // due to current implementation on binding type is wrong the type casting is necessary here
    excludes.push(binding.arg as unknown as HTMLElement);
  }
  return function (mouseup, mousedown) {
    if (!mousedown) return;

    const popperRef = (
      binding.instance as ComponentPublicInstance<{
        popperRef: Nullable<HTMLElement>;
      }>
    ).popperRef;
    const mouseUpTarget = mouseup.target as Node;
    const mouseDownTarget = mousedown.target as Node;
    const isBound = !binding || !binding.instance;
    const isTargetExists = !mouseUpTarget || !mouseDownTarget;
    const isContainedByEl = el.contains(mouseUpTarget) || el.contains(mouseDownTarget);
    const isSelf = el === mouseUpTarget;

    const isTargetExcluded =
      excludes.some((item) => item?.contains(mouseUpTarget)) ||
      excludes.includes(mouseDownTarget as HTMLElement);
    const isContainedByPopper =
      popperRef && (popperRef.contains(mouseUpTarget) || popperRef.contains(mouseDownTarget));
    if (
      isBound ||
      isTargetExists ||
      isContainedByEl ||
      isSelf ||
      isTargetExcluded ||
      isContainedByPopper
    ) {
      return;
    }
    binding.value();
  };
}

const ClickOutside: Directive = {
  beforeMount(el, binding) {
    nodeList.set(el, {
      bindingFn: binding.value,
      documentHandler: createDocumentHandler(el, binding),
    });
  },
  unmounted(el) {
    nodeList.delete(el);
  },
  updated(el, binding) {
    nodeList.set(el, {
      bindingFn: binding.value,
      documentHandler: createDocumentHandler(el, binding),
    });
  },
};

export function setupClickOutside(app: App) {
  app.directive('clickOutside', ClickOutside);
}

export default setupClickOutside;
