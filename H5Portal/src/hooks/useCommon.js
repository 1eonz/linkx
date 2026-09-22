import { getCurrentInstance } from 'vue';

import { useAiStore } from '@/stores/ai.js';
import { useApplicationStore } from '@/stores/application.js';
import { useGlobalStore } from '@/stores/global.js';
import { useUserStore } from '@/stores/user.js';

/**
 * @Func useCommon
 * @Desc 返回常用对象
 * @return {Object}
 * @Author Xingfei Xu
 * @Email 1824159241@qq.com
 */

export const useCommon = () => {
  const { appContext, proxy } = getCurrentInstance();

  const userStore = useUserStore();

  const globalStore = useGlobalStore();

  const applicationStore = useApplicationStore();

  const aiStore = useAiStore();

  const globalProperties = appContext.config.globalProperties;

  return {
    proxy,
    appContext,
    userStore,
    globalStore,
    applicationStore,
    aiStore,
    globalProperties,
    gProps: globalProperties,
    pageEvent:
      proxy.$mpType == 'page'
        ? proxy.getOpenerEventChannel()
        : { emit: () => console.error('No EventChannel') },
    containerStyle: globalStore.containerStyle,
  };
};
/**
 * 将 static 目录下的 png 图片转为 base64
 * @param {String} imgPath static目录下的相对路径，如 '/static/equipment/monitor.png'
 * @returns {Promise<String>} base64字符串
 */
export function pngToBase64(imgPath) {
  return new Promise((resolve, reject) => {
    if (!imgPath) {
      reject(new Error('pngToBase64: imgPath 不能为空'));
      return;
    }

    const raw = String(imgPath);
    // 已经是 dataURL，直接返回
    if (/^data:/i.test(raw)) {
      resolve(raw);
      return;
    }

    /**
     * 关键点：生产环境经常部署在子路径（并可能通过 <base href> 动态设置前缀）
     * - Vite 打包后的资源路径可能是 'assets/xxx.png'（相对）或 '/assets/xxx.png'（绝对）
     *
     * 但开发环境(Vite dev server)里，图片资源常以 `/src/...`、`/@fs/...` 这种“站点根绝对路径”提供，
     * 如果强行让它遵循 baseURI（尤其 baseURI 可能带子路径），会导致 dev 下 404。
     *
     * 策略：
     * - DEV：如果以 `/` 开头，保持原样交给 fetch（从站点根取）
     * - PROD：如果以 `/` 开头，去掉前导 `/`，让其遵循 document.baseURI（子路径部署可用）
     * - 其他相对路径：始终用 document.baseURI 解析
     */
    let url = raw;
    try {
      // http(s) 绝对地址保持不变；其他情况都按 baseURI 解析
      if (!/^https?:\/\//i.test(raw)) {
        if (raw.startsWith('/')) {
          // 开发环境：保留站点根绝对路径（/src、/@fs、/assets 等）
          if (import.meta.env?.DEV) {
            url = raw;
          } else {
            // 生产环境：让绝对路径也遵循 baseURI（适配子路径部署）
            url = new URL(raw.slice(1), document.baseURI).toString();
          }
        } else {
          url = new URL(raw, document.baseURI).toString();
        }
      }
    } catch {
      // 解析失败则回退到原始值（fetch 会按浏览器规则解析）
      url = raw;
    }

    fetch(url)
      .then(async (response) => {
        if (!response.ok) {
          throw new Error(`pngToBase64: 请求失败(${response.status})，url=${url}`);
        }
        const blob = await response.blob();
        // blob.type 可能为空字符串；优先使用 header 判断
        const contentType = response.headers?.get?.('content-type') || blob?.type || '';
        if (!String(contentType).startsWith('image/')) {
          throw new Error(`pngToBase64: 非图片响应(${contentType || 'unknown'})，url=${url}`);
        }
        return blob;
      })
      .then((blob) => {
        const reader = new FileReader();
        reader.onloadend = () => {
          resolve(reader.result); // base64字符串
        };
        reader.onerror = reject;
        reader.readAsDataURL(blob);
      })
      .catch(reject);
  });
}

/**
 * 移除 base64 字符串的前缀
 * @param {String} base64String 包含前缀的 base64 字符串，如 'data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAA...'
 * @returns {String} 移除前缀后的纯 base64 字符串
 */
export function removeBase64Prefix(base64String) {
  if (typeof base64String !== 'string') {
    console.warn('removeBase64Prefix: 参数必须是字符串类型');
    return base64String;
  }

  // 使用正则表达式匹配并移除各种常见的 base64 前缀
  return base64String.replace(/^data:([^;]+);base64,/, '');
}

/**
 * 深拷贝方法 - 支持数组、对象、基本数据类型等
 * @param {*} target 需要深拷贝的目标
 * @param {WeakMap} hash 用于处理循环引用的WeakMap
 * @returns {*} 深拷贝后的数据
 */
export function deepClone(target, hash = new WeakMap()) {
  // 处理null
  if (target === null) return null;

  // 处理基本数据类型
  if (typeof target !== 'object' && typeof target !== 'function') {
    return target;
  }

  // 处理日期对象
  if (target instanceof Date) {
    return new Date(target.getTime());
  }

  // 处理正则表达式
  if (target instanceof RegExp) {
    return new RegExp(target.source, target.flags);
  }

  // 处理函数
  if (typeof target === 'function') {
    // 对于普通函数，尝试克隆
    if (target.prototype) {
      // 构造函数
      const fnStr = target.toString();
      const paramReg = /\(([^)]*)\)/;
      const bodyReg = /(?<={)(.|\n)+(?=})/m;
      const param = paramReg.exec(fnStr);
      const body = bodyReg.exec(fnStr);
      if (param && body) {
        const newFn = new Function(...param[1].split(','), body[0]);
        return newFn;
      }
    } else {
      // 箭头函数或其他函数，直接返回原函数
      return target;
    }
  }

  // 处理循环引用
  if (hash.has(target)) {
    return hash.get(target);
  }

  // 处理数组
  if (Array.isArray(target)) {
    const clonedArray = [];
    hash.set(target, clonedArray);

    for (let i = 0; i < target.length; i++) {
      clonedArray[i] = deepClone(target[i], hash);
    }

    return clonedArray;
  }

  // 处理对象
  if (target instanceof Object) {
    // 处理特殊对象类型
    if (target instanceof Map) {
      const clonedMap = new Map();
      hash.set(target, clonedMap);

      target.forEach((value, key) => {
        clonedMap.set(deepClone(key, hash), deepClone(value, hash));
      });

      return clonedMap;
    }

    if (target instanceof Set) {
      const clonedSet = new Set();
      hash.set(target, clonedSet);

      target.forEach((value) => {
        clonedSet.add(deepClone(value, hash));
      });

      return clonedSet;
    }

    if (target instanceof Error) {
      const clonedError = new Error(target.message);
      clonedError.name = target.name;
      clonedError.stack = target.stack;
      return clonedError;
    }

    // 处理普通对象
    const clonedObject = {};
    hash.set(target, clonedObject);

    // 获取所有属性（包括不可枚举的）
    const allProps = Object.getOwnPropertyNames(target);
    const symbolProps = Object.getOwnPropertySymbols(target);

    // 处理普通属性
    allProps.forEach((prop) => {
      if (target.hasOwnProperty(prop)) {
        clonedObject[prop] = deepClone(target[prop], hash);
      }
    });

    // 处理Symbol属性
    symbolProps.forEach((symbol) => {
      clonedObject[symbol] = deepClone(target[symbol], hash);
    });

    return clonedObject;
  }

  // 处理其他类型（如Symbol等）
  return target;
}

/**
 * 深拷贝数组的便捷方法
 * @param {Array} array 需要深拷贝的数组
 * @returns {Array} 深拷贝后的数组
 */
export function deepCloneArray(array) {
  if (!Array.isArray(array)) {
    throw new Error('deepCloneArray: 参数必须是数组类型');
  }
  return deepClone(array);
}

/**
 * 深拷贝对象的便捷方法
 * @param {Object} obj 需要深拷贝的对象
 * @returns {Object} 深拷贝后的对象
 */
export function deepCloneObject(obj) {
  if (obj === null || typeof obj !== 'object' || Array.isArray(obj)) {
    throw new Error('deepCloneObject: 参数必须是对象类型');
  }
  return deepClone(obj);
}
