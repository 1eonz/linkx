import { onBeforeUnmount } from 'vue';

// 简单的事件总线实现
class EventEmitter {
  constructor() {
    this.events = {};
  }

  on(event, handler) {
    if (!this.events[event]) {
      this.events[event] = [];
    }
    this.events[event].push(handler);
  }

  off(event, handler) {
    if (this.events[event]) {
      this.events[event] = this.events[event].filter((h) => h !== handler);
    }
  }

  emit(event, ...args) {
    if (this.events[event]) {
      this.events[event].forEach((handler) => handler(...args));
    }
  }
}

const emitter = new EventEmitter();

/**
 * 事件发射器 hook
 * @param {string} event 事件名称
 * @param {Function} handler 事件处理函数
 * @returns {EventEmitter} 事件发射器实例
 */
export const useEmitter = (event, handler) => {
  if (event && handler) {
    emitter.on(event, handler);

    onBeforeUnmount(() => {
      emitter.off(event, handler);
    });
  }

  return emitter;
};
