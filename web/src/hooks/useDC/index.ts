import { onBeforeUnmount } from 'vue';

import DC from '@/utils/DC';

type Noop = (message: any) => void;

export const useDC = (module: string, type: string, handler: Noop) => {
  DC.on(module, type, handler);

  onBeforeUnmount(() => {
    DC.off(module, type, handler);
  });
};
