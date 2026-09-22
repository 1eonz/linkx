/**
 * 用户信息缓存 Store
 * 用于缓存用户头像等信息，避免重复请求
 */
import { defineStore } from 'pinia';
import { ref } from 'vue';

import { getUserInfo as getCollaborationUserInfo } from '@/common/api/collaborativeGroup.js';
import { transformImageUrl } from '@/utils/imgUrlParse.js';
import { imageToBase64 } from '@/utils/index.js';

export const useUserCacheStore = defineStore('userCache', () => {
  // 用户信息缓存 Map: userId -> userInfo
  const userCacheMap = ref(new Map());
  
  // 头像 base64 缓存 Map: userId -> base64String
  const avatarCacheMap = ref(new Map());
  
  // 正在加载的用户ID集合（避免重复请求）
  const loadingUserIds = ref(new Set());

  /**
   * 批量获取用户信息并缓存
   * @param {string[]} userIds - 用户ID数组
   * @param {boolean} preloadAvatars - 是否预加载头像为base64
   */
  async function batchFetchUsers(userIds, preloadAvatars = true) {
    if (!userIds || userIds.length === 0) return;
    
    // 过滤掉已缓存和正在加载的用户
    const needFetchIds = userIds.filter(
      (id) => id && !userCacheMap.value.has(id) && !loadingUserIds.value.has(id)
    );
    
    if (needFetchIds.length === 0) return;
    
    // 标记为正在加载
    needFetchIds.forEach((id) => loadingUserIds.value.add(id));
    
    try {
      console.log('[UserCache] 批量获取用户信息:', needFetchIds);
      const userInfos = await getCollaborationUserInfo({ userIds: needFetchIds });
      
      // 处理返回结果
      const results = userInfos?.results || userInfos || [];
      const users = Array.isArray(results) ? results : [results];
      
      users.forEach((user) => {
        if (user && user.id) {
          userCacheMap.value.set(String(user.id), user);
        }
      });
      
      console.log('[UserCache] 缓存用户数量:', userCacheMap.value.size);
      
      // 预加载头像为 base64
      if (preloadAvatars) {
        await batchPreloadAvatars(needFetchIds);
      }
    } catch (error) {
      console.error('[UserCache] 批量获取用户信息失败:', error);
    } finally {
      // 移除加载标记
      needFetchIds.forEach((id) => loadingUserIds.value.delete(id));
    }
  }

  /**
   * 批量预加载头像为 base64
   * @param {string[]} userIds - 用户ID数组
   */
  async function batchPreloadAvatars(userIds) {
    const promises = userIds.map(async (userId) => {
      const user = userCacheMap.value.get(String(userId));
      if (!user || !user.avatar) return;
      
      // 已经缓存过
      if (avatarCacheMap.value.has(String(userId))) return;
      
      try {
        // avatar 是文件ID，拼接完整URL
        const avatarUrl = transformImageUrl(`/admin-api${user.avatar}`);
        const base64 = await imageToBase64(avatarUrl);
        avatarCacheMap.value.set(String(userId), base64);
        console.log('[UserCache] 头像预加载成功:', userId);
      } catch (error) {
        console.warn('[UserCache] 头像预加载失败:', userId, error);
      }
    });
    
    await Promise.all(promises);
  }

  /**
   * 获取用户信息（优先从缓存）
   * @param {string} userId - 用户ID
   * @returns {Object|null} 用户信息
   */
  function getUser(userId) {
    if (!userId) return null;
    return userCacheMap.value.get(String(userId)) || null;
  }

  /**
   * 获取用户头像 base64（优先从缓存）
   * @param {string} userId - 用户ID
   * @returns {string} 头像 base64 或空字符串
   */
  function getAvatar(userId) {
    if (!userId) return '';
    return avatarCacheMap.value.get(String(userId)) || '';
  }

  /**
   * 获取用户头像（如果没有则异步加载）
   * @param {string} userId - 用户ID
   * @returns {Promise<string>} 头像 base64 或空字符串
   */
  async function fetchAvatar(userId) {
    if (!userId) return '';
    
    // 优先从缓存获取
    const cachedAvatar = avatarCacheMap.value.get(String(userId));
    if (cachedAvatar) return cachedAvatar;
    
    // 检查用户信息缓存
    const user = userCacheMap.value.get(String(userId));
    if (user && user.avatar) {
      try {
        const avatarUrl = transformImageUrl(`/admin-api${user.avatar}`);
        const base64 = await imageToBase64(avatarUrl);
        avatarCacheMap.value.set(String(userId), base64);
        return base64;
      } catch (error) {
        console.warn('[UserCache] 获取头像失败:', userId, error);
        return '';
      }
    }
    
    // 没有缓存，批量获取
    await batchFetchUsers([userId], true);
    return avatarCacheMap.value.get(String(userId)) || '';
  }

  /**
   * 清空缓存
   */
  function clearCache() {
    userCacheMap.value.clear();
    avatarCacheMap.value.clear();
    loadingUserIds.value.clear();
  }

  return {
    userCacheMap,
    avatarCacheMap,
    batchFetchUsers,
    batchPreloadAvatars,
    getUser,
    getAvatar,
    fetchAvatar,
    clearCache,
  };
});