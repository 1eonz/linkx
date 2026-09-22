/**
 * clearTimeout替代setInterval，返回clear方法清除定时器
 * @param f 副作用函数
 * @param time 时间
 * @param immediate 立即执行副作用函数
 * @returns clear func
 */
export const useSetInterval = (f, time, immediate) => {
  if (immediate) {
    f?.();
  }

  let timer;

  const func = () => {
    timer = setTimeout(() => {
      f();
      if (timer !== null) {
        func();
      }
    }, time);
  };
  func();

  const clear = () => {
    clearTimeout(timer);
    timer = null;
  };
  return clear;
};
