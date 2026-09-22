import { onMounted, onUnmounted } from 'vue';
import { getVersionCode } from '@/utils/clientEnv';

// 获取警信版本号
let cachedVersionCode = null;
export async function getVersionInfo() {
  if (cachedVersionCode !== null) return cachedVersionCode;
  if (typeof window === 'undefined' || !window.WeSpaceSDK?.getVersion) return '';
  try {
    const info = await window.WeSpaceSDK.getVersion();
    cachedVersionCode = info?.versionCode || '';
    return cachedVersionCode;
  } catch (error) {
    console.error('[clientEnv] SDK getVersion 失败:', error);
    return '';
  }
}
/**
 * 等待警信SDK的onCooperationGroupCreate通知，并与建群接口返回的groupId进行校验
 * @param {number} timeout 超时时间（毫秒），默认5000
 */
export function useGroupCreateNotify(timeout = 5000) {
  // 提前到达时缓存的groupId
  let preReceivedGroupId = null;
  // 建群接口返回的groupId，用于校验
  let createdGroupId = null;
  // 等待Promise的resolve
  let resolveNotify = null;
  // 超时定时器
  let timeoutTimer = null;

  const waitForNotify = (expectedGroupId) => {
    // 旧版本（versionCode < 843）不支持 onCooperationGroupCreate 通知，延迟1秒后返回
    const versionCode = getVersionCode();
    if (!versionCode || versionCode < 843) {
      return new Promise((resolve) => setTimeout(resolve, 1000));
    }

    createdGroupId = expectedGroupId;
    // 检查事件是否已提前到达且groupId匹配
    if (preReceivedGroupId !== null && preReceivedGroupId === expectedGroupId) {
      preReceivedGroupId = null;
      return Promise.resolve();
    }
    return new Promise((resolve, reject) => {
      resolveNotify = resolve;
      timeoutTimer = setTimeout(() => {
        resolveNotify = null;
        reject(new Error('等待警信SDK通知超时'));
      }, timeout);
    });
  };

  const onNotifyReceived = (groupId) => {
    if (resolveNotify) {
      // 已有人在等待，校验groupId是否一致
      if (createdGroupId !== null && groupId === createdGroupId) {
        clearTimeout(timeoutTimer);
        timeoutTimer = null;
        resolveNotify();
        resolveNotify = null;
      }
      // groupId不匹配则忽略，继续等待
    } else {
      // 还没人等待，缓存groupId
      preReceivedGroupId = groupId;
    }
  };

  onMounted(() => {
    window.WeSpaceSDK?.onCooperationGroupCreate((data) => {
      console.info('建群后收到警信的通知', data);
      const { groupId } = data;
      onNotifyReceived(groupId);
    });
  });

  onUnmounted(() => {
    if (timeoutTimer) {
      clearTimeout(timeoutTimer);
      timeoutTimer = null;
    }
    resolveNotify = null;
    preReceivedGroupId = null;
    createdGroupId = null;
  });

  return {
    waitForNotify,
  };
}
