import { onBeforeUnmount, onMounted } from 'vue';

const effect = new WeakMap();

// 监听元素窗口大小变化
export const useWatchSizeChange = (elementId, callback) => {
  // 记录下旧的宽高
  const oldSize = {
    height: 0,
    width: 0,
  };
  let resizeObserver: ResizeObserver;

  onMounted(() => {
    registerSizeChange();
  });

  onBeforeUnmount(() => {
    // 取消监听元素
    const el = document.getElementById(elementId);
    if (el) {
      resizeObserver.unobserve(el as Element);
      effect.delete(el);
    }
  });

  function registerSizeChange() {
    const el = document.getElementById(elementId);
    resizeObserver = new ResizeObserver((e) => {
      const { height, width } = e[0].contentRect;
      if (oldSize.width !== width && oldSize.height !== height) {
        Object.assign(oldSize, { height, width });
      }
      callback(oldSize);
    });
    // 监听元素
    if (el && !effect.has(el)) {
      resizeObserver.observe(el as Element);
      effect.set(el, '');
    }
  }
};
