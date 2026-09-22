import { onBeforeUnmount } from 'vue';

import mitt, { type Emitter } from 'mitt';

type E = Emitter<any>;

const emitter: E = mitt();

/**
 * 默认传参触发on事件
 * 需要触发emit可以useEmitter().emit('xx', data)
 * @param event
 * @param handler
 * @returns
 */
export const useEmitter = (event?, handler?): E => {
  if (event && handler) {
    emitter.on(event, handler);

    onBeforeUnmount(() => {
      emitter.off(event, handler);
    });
  }

  return emitter;
};
