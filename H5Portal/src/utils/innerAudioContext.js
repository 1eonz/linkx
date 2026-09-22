/**
 * 完全模拟 uniapp createInnerAudioContext API 的工具函数
 * 与 uniapp 保持完全一致的接口和使用方式
 */

class InnerAudioContext {
  constructor() {
    // 内部音频对象
    this._audio = null;
    this._destroyed = false;

    // 属性定义 - 与 uniapp 完全一致
    this.src = '';
    this.autoplay = false;
    this.loop = false;
    this.volume = 1;
    this.startTime = 0;
    this.currentTime = 0;
    this.duration = 0;
    this.paused = true;
    this.buffered = 0;

    // 事件监听器存储
    this._events = {
      play: [],
      pause: [],
      ended: [],
      error: [],
      timeupdate: [],
      canplay: [],
      loadstart: [],
      loadedmetadata: [],
      waiting: [],
      seeking: [],
      seeked: [],
      stop: [], // 注意：uniapp 官方文档没有 stop 事件，但我们可以支持
    };

    // 初始化
    this._init();
  }

  /**
   * 初始化音频对象
   */
  _init() {
    if (typeof window === 'undefined') return;

    this._audio = new Audio();
    this._bindEvents();
    this._syncProperties();
  }

  /**
   * 同步属性到音频对象
   */
  _syncProperties() {
    if (!this._audio || this._destroyed) return;

    this._audio.src = this.src;
    this._audio.autoplay = this.autoplay;
    this._audio.loop = this.loop;
    this._audio.volume = Math.max(0, Math.min(1, this.volume));
    this._audio.currentTime = this.startTime;
  }

  /**
   * 绑定音频事件
   */
  _bindEvents() {
    if (!this._audio) return;

    // 播放事件
    this._audio.addEventListener('play', () => {
      this.paused = false;
      this._triggerEvent('play', {});
    });

    // 暂停事件
    this._audio.addEventListener('pause', () => {
      this.paused = true;
      this._triggerEvent('pause', {});
    });

    // 结束事件
    this._audio.addEventListener('ended', () => {
      this.paused = true;
      this.currentTime = 0;
      this._triggerEvent('ended', {});
    });

    // 错误事件
    this._audio.addEventListener('error', (e) => {
      this.paused = true;
      const errorData = {
        errMsg: this._getErrorMessage(e.target.error?.code || -1),
      };
      this._triggerEvent('error', errorData);
    });

    // 加载开始
    this._audio.addEventListener('loadstart', () => {
      this._triggerEvent('loadstart', {});
    });

    // 元数据加载完成
    this._audio.addEventListener('loadedmetadata', () => {
      this.duration = this._audio.duration || 0;
      this._triggerEvent('loadedmetadata', {});
    });

    // 可以播放
    this._audio.addEventListener('canplay', () => {
      this.duration = this._audio.duration || 0;
      this._triggerEvent('canplay', {});
    });

    // 等待数据
    this._audio.addEventListener('waiting', () => {
      this._triggerEvent('waiting', {});
    });

    // 寻找开始
    this._audio.addEventListener('seeking', () => {
      this._triggerEvent('seeking', {});
    });

    // 寻找完成
    this._audio.addEventListener('seeked', () => {
      this.currentTime = this._audio.currentTime;
      this._triggerEvent('seeked', {});
    });

    // 时间更新
    this._audio.addEventListener('timeupdate', () => {
      this.currentTime = this._audio.currentTime;
      this.duration = this._audio.duration || 0;

      // 计算缓冲进度
      if (this._audio.buffered.length > 0) {
        this.buffered = this._audio.buffered.end(this._audio.buffered.length - 1);
      }

      this._triggerEvent('timeupdate', {
        currentTime: this.currentTime,
        duration: this.duration,
      });
    });
  }

  /**
   * 获取错误信息
   */
  _getErrorMessage(code) {
    const errorMap = {
      1: '用户终止了音频播放',
      2: '网络错误',
      3: '解码错误',
      4: '音频格式不支持',
      MEDIA_ERR_ABORTED: '用户终止了音频播放',
      MEDIA_ERR_NETWORK: '网络错误',
      MEDIA_ERR_DECODE: '解码错误',
      MEDIA_ERR_SRC_NOT_SUPPORTED: '音频格式不支持',
      '-1': '未知错误',
    };

    return errorMap[code] || errorMap[code.toString()] || '未知错误';
  }

  /**
   * 触发事件
   */
  _triggerEvent(eventName, data) {
    if (this._events[eventName]) {
      this._events[eventName].forEach((callback) => {
        try {
          callback(data || {});
        } catch (error) {
          console.error(`AudioContext event [${eventName}] callback error:`, error);
        }
      });
    }
  }

  /**
   * 监听事件 - 与 uniapp 完全一致的直接调用方法
   */
  onPlay(callback) {
    if (typeof callback === 'function') {
      this._events.play.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 监听暂停事件 - 与 uniapp 完全一致的直接调用方法
   */
  onPause(callback) {
    if (typeof callback === 'function') {
      this._events.pause.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 监听结束事件 - 与 uniapp 完全一致的直接调用方法
   */
  onEnded(callback) {
    if (typeof callback === 'function') {
      this._events.ended.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 监听错误事件 - 与 uniapp 完全一致的直接调用方法
   */
  onError(callback) {
    if (typeof callback === 'function') {
      this._events.error.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 监听时间更新事件 - 与 uniapp 完全一致的直接调用方法
   */
  onTimeUpdate(callback) {
    if (typeof callback === 'function') {
      this._events.timeupdate.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 监听可以播放事件 - 与 uniapp 完全一致的直接调用方法
   */
  onCanplay(callback) {
    if (typeof callback === 'function') {
      this._events.canplay.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 监听加载开始事件 - 与 uniapp 完全一致的直接调用方法
   */
  onLoadStart(callback) {
    if (typeof callback === 'function') {
      this._events.loadstart.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 监听元数据加载完成事件 - 与 uniapp 完全一致的直接调用方法
   */
  onLoadedMetadata(callback) {
    if (typeof callback === 'function') {
      this._events.loadedmetadata.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 监听等待数据事件 - 与 uniapp 完全一致的直接调用方法
   */
  onWaiting(callback) {
    if (typeof callback === 'function') {
      this._events.waiting.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 监听寻找开始事件 - 与 uniapp 完全一致的直接调用方法
   */
  onSeeking(callback) {
    if (typeof callback === 'function') {
      this._events.seeking.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 监听寻找完成事件 - 与 uniapp 完全一致的直接调用方法
   */
  onSeeked(callback) {
    if (typeof callback === 'function') {
      this._events.seeked.push(callback);
      this._ensureAudioInit();
    }
    return this;
  }

  /**
   * 确保音频已初始化
   */
  _ensureAudioInit() {
    if (!this._audio && !this._destroyed) {
      this._init();
    }
  }

  /**
   * 播放 - 与 uniapp 完全一致的方法
   */
  play() {
    if (this._destroyed) {
      console.warn('InnerAudioContext: Cannot play destroyed instance');
      return Promise.reject(new Error('Instance destroyed'));
    }

    this._ensureAudioInit();

    if (!this.src) {
      const error = { errMsg: 'src 为空' };
      this._triggerEvent('error', error);
      return Promise.reject(error);
    }

    return new Promise((resolve, reject) => {
      try {
        this._syncProperties();
        const playPromise = this._audio.play();

        if (playPromise !== undefined) {
          playPromise
            .then(() => {
              resolve();
            })
            .catch((error) => {
              const errorData = {
                errMsg: `play failed: ${error.message}`,
              };
              this._triggerEvent('error', errorData);
              reject(errorData);
            });
        } else {
          resolve();
        }
      } catch (error) {
        const errorData = {
          errMsg: `play failed: ${error.message}`,
        };
        this._triggerEvent('error', errorData);
        reject(errorData);
      }
    });
  }

  /**
   * 暂停 - 与 uniapp 完全一致的方法
   */
  pause() {
    if (this._audio && !this._destroyed) {
      this._audio.pause();
    }
    return this;
  }

  /**
   * 停止 - 注意：uniapp 官方 API 中没有 stop 方法，但很多开发者需要
   * 这里我们实现一个兼容版本
   */
  stop() {
    if (this._audio && !this._destroyed) {
      this._audio.pause();
      this._audio.currentTime = 0;
      this.currentTime = 0;
      this.paused = true;
      this._triggerEvent('stop', {});
    }
    return this;
  }

  /**
   * 跳转到指定位置 - 与 uniapp 完全一致的方法
   */
  seek(position) {
    if (this._audio && !this._destroyed && typeof position === 'number' && position >= 0) {
      this._audio.currentTime = position;
      this.currentTime = position;
    }
    return this;
  }

  /**
   * 销毁实例 - 扩展方法，用于清理资源
   */
  destroy() {
    if (this._destroyed) return this;

    this.stop();

    // 清空所有事件监听器
    Object.keys(this._events).forEach((eventName) => {
      this._events[eventName] = [];
    });

    if (this._audio) {
      this._audio.src = '';
      this._audio = null;
    }

    this._destroyed = true;
    return this;
  }

  /**
   * 移除事件监听 - 扩展方法
   */
  off(eventName, callback) {
    if (!this._events[eventName]) return this;

    if (callback) {
      const index = this._events[eventName].indexOf(callback);
      if (index > -1) {
        this._events[eventName].splice(index, 1);
      }
    } else {
      // 不传回调则移除该事件的所有监听器
      this._events[eventName] = [];
    }

    return this;
  }
}

/**
 * 创建音频上下文 - 与 uniapp 完全一致的主函数
 */
function createInnerAudioContext() {
  return new InnerAudioContext();
}

// 导出 - 支持多种导入方式
export { createInnerAudioContext, InnerAudioContext };
export default { createInnerAudioContext };
