import type { ComputedRef, Ref } from 'vue';
import { onBeforeUnmount, onMounted, ref, unref, watchEffect } from 'vue';

import { addUnit } from '@/utils';

interface DraggableConfig {
  callback?: (mouseStatus: string, options?: any) => void;
  draggable: boolean | ComputedRef<boolean> | Ref<boolean>;
  dragRef?: Ref<HTMLElement[]>;
  uid: string;
}

export const useDraggable = (config: DraggableConfig) => {
  const { callback, draggable, uid } = config;
  const transform = {
    offsetX: 0,
    offsetY: 0,
  };
  const dragRef = ref<Element[]>([]);
  let targetContainer: HTMLElement | null;

  const getTranslateXY = () => {
    const style = window.getComputedStyle(targetContainer!);
    const matrix = new WebKitCSSMatrix(style.transform);
    return [matrix.m41, matrix.m42];
  };

  const onMousedown = (e: MouseEvent) => {
    e.preventDefault();
    callback?.('mousedown', { transform });

    let { offsetX, offsetY } = transform;
    const targetTransform = targetContainer?.style.transform;
    if (!targetTransform || targetTransform === 'none') {
      offsetX = offsetY = 0;
    } else {
      [offsetX, offsetY] = getTranslateXY();
    }

    const downX = e.clientX;
    const downY = e.clientY;

    const targetRect = targetContainer!.getBoundingClientRect();
    const targetLeft = targetRect.left;
    const targetTop = targetRect.top;
    const targetWidth = targetRect.width;
    const targetHeight = targetRect.height;

    const clientWidth = document.documentElement.clientWidth;
    const clientHeight = document.documentElement.clientHeight;

    const minLeft = -targetLeft + offsetX;
    const minTop = -targetTop + offsetY;
    const maxLeft = clientWidth - targetLeft - targetWidth + offsetX;
    const maxTop = clientHeight - targetTop - targetHeight + offsetY;

    const onMousemove = (e: MouseEvent) => {
      const moveX = Math.min(Math.max(offsetX + e.clientX - downX, minLeft), maxLeft);
      const moveY = Math.min(Math.max(offsetY + e.clientY - downY, minTop), maxTop);
      transform.offsetX = moveX;
      transform.offsetY = moveY;

      targetContainer!.style.transform = `translate(${addUnit(moveX)}, ${addUnit(moveY)})`;
      callback?.('mousemove', { transform });
    };
    const onMouseup = () => {
      callback?.('mouseup', { transform });
      document.removeEventListener('mousemove', onMousemove);
      document.removeEventListener('mouseup', onMouseup);
    };
    document.addEventListener('mousemove', onMousemove);
    document.addEventListener('mouseup', onMouseup);
  };

  const onDraggable = () => {
    if (unref(dragRef) && targetContainer) {
      unref(dragRef).forEach((item) => {
        item.addEventListener('mousedown', onMousedown);
      });
    }
  };

  const offDraggable = () => {
    if (unref(dragRef) && targetContainer) {
      unref(dragRef).forEach((item) => {
        item.removeEventListener('mousedown', onMousedown);
      });
    }
  };

  const setDraggable = () => {
    if (unref(draggable)) {
      // 监听 弹窗是否打开 或者 关闭
      onDraggable();
    } else {
      offDraggable();
    }
  };

  const getDraggerRef = () => {
    targetContainer = document.getElementById(uid) as HTMLElement;
    const trans = targetContainer?.style.transform;
    if (trans) {
      const arr = getTranslateXY();
      Object.assign(transform, {
        offsetX: arr[0],
        offsetY: arr[1],
      });
    }

    const dragger = targetContainer?.getElementsByClassName?.('dragger') || [];
    const arr: Element[] = [];
    for (const i of dragger as any) {
      arr.push(i);
    }
    dragRef.value = arr;
    setDraggable();
  };

  getDraggerRef();

  watchEffect(setDraggable);

  onMounted(() => {
    getDraggerRef();
  });

  onBeforeUnmount(() => {
    offDraggable();
    targetContainer = null;
  });
};
