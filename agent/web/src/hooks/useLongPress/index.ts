import { onBeforeUnmount, onMounted } from 'vue';
/**
 * 长按键盘按键
 * @param {*} keyCode 按键Ascii码值
 * @param {*} callback 回调函数
 * @param {*} delay
 */
export const useLongPress = (keyCode: number, callback: any, delay = 100) => {
  let timer: any = null;
  // 按下
  const startLongPress = (event: any) => {
    if (event.keyCode !== keyCode) return;
    event.preventDefault();
    if (!timer) {
      timer = setTimeout(() => {
        callback(true);
        timer = null;
      }, delay);
    }
  };
  // 释放
  const endLongPress = (event: any) => {
    if (event.keyCode !== keyCode) return;
    event.preventDefault();
    if (timer) {
      callback(false);
      clearTimeout(timer);
      timer = null;
    }
  };

  onMounted(() => {
    window.addEventListener('keydown', startLongPress);
    window.addEventListener('keyup', endLongPress);
  });

  onBeforeUnmount(() => {
    window.removeEventListener('keydown', startLongPress);
    window.removeEventListener('keyup', endLongPress);
  });
};
