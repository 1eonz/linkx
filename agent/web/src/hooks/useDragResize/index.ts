import { ComputedRef, onBeforeMount, onMounted, Ref, unref } from 'vue';

import { isEmpty, isString } from '@/utils/is';

type Change = {
  e?: number;
  n?: number;
  s?: number;
  w?: number;
};

type UseDragResizeProps = {
  callback?: (change: Change) => void;
  canDrag?: boolean | ComputedRef<boolean> | Ref<boolean>;
  maxH?: number;
  maxW?: number;
  minH?: number;
  minW?: number;
  outsideUid?: string;
  resize?: 'x' | 'y'; // 调整宽或者高，不传都可以调整
  uid: string;
};

// 拖拽调整div大小
export const useDragResize = (config: UseDragResizeProps) => {
  const {
    callback,
    canDrag = true,
    maxH = 1080,
    maxW = 1920,
    minH = 0,
    minW = 0,
    outsideUid,
    resize,
    uid,
  } = config;
  let resizeAble = false;
  let downDir = '';
  let moveDir = '';
  // 鼠标按下时坐标
  let downX = 0;
  let downY = 0;
  let container: HTMLElement | null;
  let changeOffset: any = {};

  const handleResize = () => {
    if (isEmpty(changeOffset)) {
      return;
    }

    const { n, w } = changeOffset;
    if (n || w) {
      const el = document.getElementById(outsideUid || uid) as HTMLElement;
      el.style.position = 'fixed';

      const style = window.getComputedStyle(el);

      const matrix = new WebKitCSSMatrix(style.transform);
      const x = matrix.m41;
      const y = matrix.m42;

      el.style.transform = `translate(${x - (w || 0)}px, ${y - (n || 0)}px)`;
    }

    callback?.(changeOffset);
  };

  // 鼠标位于div方位
  const getDirection = (e: MouseEvent): string => {
    let dir = '';
    if (!container) {
      return dir;
    }

    const { x, y } = e;
    const { bottom, left, right, top } = container.getBoundingClientRect();
    const offset = 10;

    // 北
    if (y < top + offset) {
      dir += 'n';
    }

    // 南
    if (y > bottom - offset) {
      dir += 's';
    }

    // 西
    if (x < left + offset) {
      dir += 'w';
    }

    // 东
    if (x > right - offset) {
      dir += 'e';
    }

    return dir;
  };

  const mousemove = (e: MouseEvent, dir?) => {
    if (!unref(canDrag)) return;
    if (!dir) {
      dir = getDirection(e);
      moveDir = dir;
    }

    const style = container?.style;
    if (!style) return;

    style.cursor =
      !dir ||
      (resize === 'y' && ['e', 'w'].includes(dir)) ||
      (resize === 'x' && ['n', 's'].includes(dir))
        ? 'default'
        : `${dir}-resize`;

    if (!resizeAble) return;

    const pointX = e.clientX;
    const pointY = e.clientY;

    const x = container?.offsetWidth || 0;
    const y = container?.offsetHeight || 0;

    changeOffset = {};

    // 东
    if (downDir.includes('e')) {
      let offset = x + (pointX - downX);
      if (offset < minW) {
        offset = minW;
      } else if (offset > maxW) {
        offset = maxW;
      }
      style.width = `${offset}px`;
      changeOffset.e = pointX - downX;
      downX = pointX;
    }

    // 南
    if (downDir.includes('s')) {
      let offset = y + (pointY - downY);
      if (offset < minH) {
        offset = minH;
      } else if (offset > maxH) {
        offset = maxH;
      }
      offset = Math.max(minH, y + (pointY - downY));
      style.height = `${offset}px`;
      changeOffset.s = pointY - downY;
      downY = pointY;
    }

    // 西
    if (downDir.includes('w')) {
      let offset = x + (downX - pointX);
      if (offset < minW) {
        offset = minW;
      } else if (offset > maxW) {
        offset = maxW;
      }
      style.width = `${offset}px`;
      changeOffset.w = downX - pointX;
      downX = pointX;
    }

    // 北
    if (downDir.includes('n')) {
      let offset = y + (downY - pointY);
      if (offset < minH) {
        offset = minH;
      } else if (offset > maxH) {
        offset = maxH;
      }
      style.height = `${offset}px`;
      changeOffset.n = downY - pointY;
      downY = pointY;
    }

    handleResize();
  };

  const mouseup = () => {
    if (resizeAble) {
      handleResize();
    }
    resizeAble = false;
  };

  const mousedown = (e) => {
    const targetClass = e.target?.className;
    if (isString(targetClass) && targetClass.includes('dragger')) {
      resizeAble = false;
      return;
    }
    downDir = getDirection(e);
    if (downDir) {
      if (
        (resize === 'y' && ['e', 'w'].includes(downDir)) ||
        (resize === 'x' && ['n', 's'].includes(downDir))
      ) {
        return;
      }
      resizeAble = true;
      downX = e.clientX;
      downY = e.clientY;
    }
  };

  const windowMousemove = (e: MouseEvent) => {
    mousemove(e, moveDir);
  };

  const removeMouseEvent = () => {
    container?.removeEventListener('mousemove', mousemove);
    container?.removeEventListener('mouseup', mouseup);
    container?.removeEventListener('mousedown', mousedown);
    document.removeEventListener('mousemove', windowMousemove);
    document.removeEventListener('mouseup', mouseup);
  };

  const registerMouseEvent = () => {
    container?.addEventListener('mousemove', mousemove);
    container?.addEventListener('mouseup', mouseup);
    container?.addEventListener('mousedown', mousedown);
    document.addEventListener('mousemove', windowMousemove);
    document.addEventListener('mouseup', mouseup);
  };

  onMounted(() => {
    container = document.getElementById(uid) as HTMLElement;
    registerMouseEvent();
  });

  onBeforeMount(() => {
    removeMouseEvent();
    container = null;
  });
};
