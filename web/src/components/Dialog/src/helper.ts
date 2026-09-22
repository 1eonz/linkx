import { ref } from 'vue';

import { useCreateApp } from '@/hooks';

import DialogVue from './Dialog.vue';

const dialogMap = new Map();

type DialogConfig =
  | {
      cid: string; // 建议 组件名 + 特殊字符（id）
      content: object; // 组件
      data?: object; // props data
      draggable?: boolean; // 是否可拖拽
      fullScreenZIndex?: 'auto' | 'fixed' | number; // 全屏状态下的zIndex值
      offset?: string[]; // [left,top] [10px, 10px] 不传默认居中定位
      onClose?: () => void; // 关闭的回调事件
      onOpen?: () => void; // 弹窗打开回调事件
      shade?: boolean; // 是否显示遮罩
      zIndexDefault?: number; // 弹框默认z-index层级
    }
  | string;

export const zIndexSet = ref<string[]>([]);
let timer: Timeout;

/**
 * set dialog zIndex
 * @param cid 弹窗id
 * @param isClick 是否是点击弹窗,让弹窗居顶
 * @returns
 */
export const setZIndex = (cid: string, isClick?: boolean) => {
  if (isClick) {
    if (timer) return;
  } else {
    timer = setTimeout(() => {
      clearTimeout(timer);
    }, 500);
  }
  const index = zIndexSet.value.indexOf(cid);
  if (index !== -1) {
    zIndexSet.value.splice(index, 1);
  }
  zIndexSet.value.push(cid);
};

/**
 * 据目标字符串关闭对应弹窗
 * @param dialogMap map弹窗cid
 * @param target 目标字符串
 * @returns
 */
function closeDialogsContaining(
  dialogMap: Map<string, { close: (key: string) => void }>,
  target: string,
) {
  for (const key of dialogMap.keys()) {
    if (key.includes(target)) {
      const dialog = dialogMap.get(key);
      if (dialog) {
        dialog.close(key);
      }
    }
  }
}

/**
 * 弹窗规范-打开弹窗时需要关闭一些弹窗
 * @param {*} cid
 */
function dialogNormalize(cid: string) {
  const cidList = [
    'groupDetailCard',
    'createControlTask',
    'editPrivateCard',
    'create_dynamic_group',
    'trackCard',
    'realTimeTrackCard',
    'create_monitors_group',
    'chooseConferenceMember',
    'Setting',
  ];

  // 预警提醒弹窗不关闭以上弹窗
  if (cid.includes('remind')) {
    return;
  }

  // 处理 设置弹框与其子弹框的显示
  if (cid === 'navSetting') {
    closeDialogsContaining(dialogMap, 'Info');
  }

  cidList.forEach((item) => {
    if (cid.includes(item)) return;
    closeDialogsContaining(dialogMap, item);
  });
}

/**
 * 弹窗生成器
 * @param config
 * @returns Dialog
 */
export const Dialog = (config: DialogConfig) => {
  // 可以只传cid，即获取dialog
  if (typeof config === 'string') {
    return dialogMap.get(config);
  }

  const {
    cid,
    content,
    data,
    draggable = true,
    fullScreenZIndex,
    offset,
    onClose,
    onOpen,
    shade,
    zIndexDefault,
  } = config;
  const isFullScreen = ref<boolean>(false);
  const dialogOffset = ref(offset);

  // 避免重复打开
  if (dialogMap.has(cid)) {
    return;
  }

  dialogMap.set(cid, {});

  dialogNormalize(cid);

  // 实例化组件
  const instance = useCreateApp(DialogVue, {
    cid,
    content,
    draggable,
    fullScreenZIndex,
    isFullScreen,
    offset: dialogOffset,
    onOpen,
    propsData: data,
    shade,
    zIndexDefault,
  });

  // 挂载组件
  const el = document.createElement('div');
  document.body.append(el);
  instance.mount(el);

  // 卸载组件
  const unmount = () => {
    onClose?.();
    instance.unmount();
    el.remove();
    dialogMap.delete(cid);
  };

  /**
   * 弹窗全屏
   * 不传参数就取之前相反值
   * @param data 是否全屏
   */
  const fullScreen = (data?: boolean) => {
    if (data === undefined) {
      isFullScreen.value = !isFullScreen.value;
      return;
    }
    isFullScreen.value = data;
  };

  // set offset
  const setOffset = (opt: { left: string; top: string }) => {
    const { left, top } = opt;
    dialogOffset.value = [left, top];
  };

  // 弹窗方法
  const dialog = {
    close: unmount,
    fullScreen,
    setOffset,
  };

  setZIndex(cid);
  dialogMap.set(cid, dialog);

  return dialog;
};

// 获取所有的弹窗cid
export function getDialogCidList(): string[] {
  const keys: string[] = [];
  for (const key of dialogMap.keys()) {
    keys.push(key);
  }
  return keys;
}

// 关闭所有弹窗
export function closeAllDialog() {
  for (const key of dialogMap.keys()) {
    Dialog(key)?.close();
  }
}
