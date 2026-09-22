/**
 * 分屏通信
 */

import { useCommunicateDispatchStoreWithOut } from '@/store';

const { addMonitorList, setProjectionCenter } = useCommunicateDispatchStoreWithOut();
let screenLabel = '';

/**
 * 获取当前屏幕信息
 */
async function screenDetails() {
  const { getScreenDetails } = window;

  if (getScreenDetails) {
    const { currentScreen } = await window.getScreenDetails();
    const { isPrimary, label } = currentScreen;

    screenLabel = label;

    const pro = getProjection();
    if (!pro.includes(label)) {
      pro.push(currentScreen.label);
      localStorage.setItem('projection', JSON.stringify(pro));
    }

    setProjectionCenter(pro.length > 1 && isPrimary);
  }
}

/**
 * 注册通信事件
 */
export const registerPostMessage = () => {
  screenDetails();

  window.addEventListener('message', (e) => {
    const { data, type } = e.data;

    // 一件播放 - 投屏
    if (type === 'projection') {
      setTimeout(() => {
        addMonitorList(data);
      }, 500);
    }
  });

  window.addEventListener('storage', (e) => {
    if (e.key === 'projection') {
      screenDetails();
    }
  });

  window.addEventListener('beforeunload', () => {
    if (screenLabel) {
      const pro = getProjection();
      localStorage.setItem('projection', JSON.stringify(pro.filter((i) => i && i !== screenLabel)));
    }
  });
};

/**
 * 获取屏幕
 * @returns []
 */
function getProjection() {
  return JSON.parse(localStorage.getItem('projection') || '[]');
}
