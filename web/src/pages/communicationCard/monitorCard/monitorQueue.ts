/**
 * 用于解决同时播放多个监控 没画面或者画面卡住的问题
 */
import { useSetInterval } from '@/hooks';

const queueList: any[] = [];
let clearTimer: any;

export function setQueueList(fn) {
  const index = queueList.indexOf(fn);
  if (index !== -1) {
    queueList.splice(index, 1);
  }
  queueList.push(fn);

  if (!clearTimer) {
    runTimer();
  }
}

function runTimer() {
  clearTimer = useSetInterval(() => {
    if (queueList.length === 0) {
      clearTimer?.();
      clearTimer = null;
      return;
    }
    const func = queueList.shift();
    func();
  }, 700);
}
