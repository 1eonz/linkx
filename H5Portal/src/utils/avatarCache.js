// 头像缓存工具
// 用于缓存已加载的头像，避免重复加载

class AvatarCache {
  constructor() {
    this.cache = new Map();
    this.maxSize = 50; // 最大缓存数量
  }

  // 预加载头像
  async preloadAvatar(url) {
    if (!url) return false;

    // 如果已经缓存，直接返回
    if (this.cache.has(url)) {
      return true;
    }

    return new Promise((resolve) => {
      const img = new Image();

      img.onload = () => {
        this.cache.set(url, {
          loaded: true,
          timestamp: Date.now(),
          element: img,
        });
        console.log(`头像缓存成功: ${url}`);
        resolve(true);
      };

      img.onerror = () => {
        console.warn(`头像缓存失败: ${url}`);
        resolve(false);
      };

      // 设置超时
      setTimeout(() => {
        if (!img.complete) {
          console.warn(`头像加载超时: ${url}`);
          resolve(false);
        }
      }, 10000); // 10秒超时

      img.src = url;
    });
  }

  // 批量预加载头像
  async preloadAvatars(urls) {
    const promises = urls.map((url) => this.preloadAvatar(url));
    return Promise.all(promises);
  }

  // 检查头像是否已缓存
  isCached(url) {
    return this.cache.has(url) && this.cache.get(url).loaded;
  }

  // 清理过期缓存
  cleanExpiredCache(maxAge = 30 * 60 * 1000) {
    // 默认30分钟
    const now = Date.now();
    for (const [url, data] of this.cache.entries()) {
      if (now - data.timestamp > maxAge) {
        this.cache.delete(url);
      }
    }
  }

  // 清理缓存（保留最近使用的）
  cleanCache() {
    if (this.cache.size <= this.maxSize) return;

    // 按时间戳排序，删除最旧的
    const entries = Array.from(this.cache.entries()).sort(
      (a, b) => a[1].timestamp - b[1].timestamp,
    );

    const toDelete = entries.slice(0, entries.length - this.maxSize);
    toDelete.forEach(([url]) => this.cache.delete(url));
  }

  // 获取缓存统计信息
  getStats() {
    return {
      size: this.cache.size,
      maxSize: this.maxSize,
      urls: Array.from(this.cache.keys()),
    };
  }
}

// 创建全局实例
const avatarCache = new AvatarCache();

export default avatarCache;
