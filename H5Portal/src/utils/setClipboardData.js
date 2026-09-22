/**
 * uni.setClipboardData 完全模拟工具函数
 * 支持直接调用 setClipboardData()，与 uniapp API 完全一致
 */

import { showToast, showSuccessToast, showFailToast } from 'vant';

// 全局剪贴板实例
let clipboardInstance = null;

class ClipboardImpl {
  constructor() {
    this.isSupported = this.checkClipboardSupport();
    this.fallbackEnabled = true;
    this.copyHistory = [];

    // 初始化时尝试获取权限
    this.initPermission();
  }

  /**
   * 检查剪贴板支持性
   */
  checkClipboardSupport() {
    // 检查现代 Clipboard API
    if (navigator.clipboard && window.isSecureContext) {
      return {
        modern: true,
        legacy: false,
        writeText: typeof navigator.clipboard.writeText === 'function',
      };
    }

    // 检查传统 document.execCommand 方法
    if (
      typeof document !== 'undefined' &&
      document.queryCommandSupported &&
      document.queryCommandSupported('copy')
    ) {
      return {
        modern: false,
        legacy: true,
        execCommand: true,
      };
    }

    // 检查 IE 特有的方法
    if (typeof window !== 'undefined' && window.clipboardData) {
      return {
        modern: false,
        legacy: true,
        ie: true,
      };
    }

    return {
      modern: false,
      legacy: false,
      supported: false,
    };
  }

  /**
   * 初始化权限
   */
  async initPermission() {
    if (!this.isSupported.modern) return;

    try {
      // 在某些浏览器中，需要先请求权限
      if (navigator.permissions && navigator.permissions.query) {
        const permissionStatus = await navigator.permissions.query({
          name: 'clipboard-write',
        });
        this.permissionGranted = permissionStatus.state === 'granted';
      }
    } catch (error) {
      console.warn('Clipboard permission check failed:', error);
      this.permissionGranted = false;
    }
  }

  /**
   * 使用现代 Clipboard API 复制文本
   */
  async copyWithModernAPI(text) {
    try {
      await navigator.clipboard.writeText(text);
      return { success: true, method: 'modern' };
    } catch (error) {
      console.error('Modern clipboard API failed:', error);

      // 根据错误类型提供更具体的错误信息
      if (error.name === 'NotAllowedError') {
        throw new Error('clipboard permission denied');
      } else if (error.name === 'NotFoundError') {
        throw new Error('no valid clipboard found');
      } else {
        throw new Error(`clipboard write failed: ${error.message}`);
      }
    }
  }

  /**
   * 使用传统 execCommand 方法复制文本
   */
  copyWithLegacyMethod(text) {
    try {
      // 创建临时文本域
      const textArea = document.createElement('textarea');
      textArea.value = text;

      // 设置样式使其不可见
      textArea.style.position = 'fixed';
      textArea.style.left = '-999999px';
      textArea.style.top = '-999999px';
      textArea.style.opacity = '0';
      textArea.style.pointerEvents = 'none';
      textArea.style.zIndex = '-1';

      document.body.appendChild(textArea);

      // 选中文本
      textArea.focus();
      textArea.select();

      // 兼容移动设备
      if (textArea.setSelectionRange) {
        textArea.setSelectionRange(0, text.length);
      }

      // 执行复制命令
      const successful = document.execCommand('copy');
      document.body.removeChild(textArea);

      if (successful) {
        return { success: true, method: 'legacy' };
      } else {
        throw new Error('execCommand copy failed');
      }
    } catch (error) {
      // 清理可能残留的元素
      const textAreas = document.querySelectorAll('textarea');
      textAreas.forEach((area) => {
        if (area.style.left === '-999999px') {
          document.body.removeChild(area);
        }
      });

      throw new Error(`legacy copy failed: ${error.message}`);
    }
  }

  /**
   * 使用 IE 特有方法复制文本
   */
  copyWithIE(text) {
    try {
      window.clipboardData.setData('Text', text);
      return { success: true, method: 'ie' };
    } catch (error) {
      throw new Error(`ie clipboard failed: ${error.message}`);
    }
  }

  /**
   * 降级方案：提示用户手动复制
   */
  showManualCopyFallback(text) {
    return new Promise((resolve, reject) => {
      // 创建手动复制的模态框
      this.createManualCopyModal(text, resolve, reject);
    });
  }

  /**
   * 创建手动复制模态框
   */
  createManualCopyModal(text, resolve, reject) {
    // 移除已存在的模态框
    const existingModal = document.querySelector('.manual-copy-modal');
    if (existingModal) {
      existingModal.remove();
    }

    const modal = document.createElement('div');
    modal.className = 'manual-copy-modal';
    modal.innerHTML = `
      <div class="manual-copy-backdrop">
        <div class="manual-copy-container">
          <div class="manual-copy-header">
            <h3>复制内容</h3>
            <van-icon name="cross" class="close-btn" />
          </div>
          <div class="manual-copy-content">
            <p class="instruction">请手动复制以下内容：</p>
            <div class="copy-area">
              <textarea readonly>${this.escapeHtml(text)}</textarea>
              <van-button 
                type="primary" 
                size="small" 
                class="copy-btn"
                onclick="this.previousElementSibling.select()"
              >
                选中文本
              </van-button>
            </div>
            <div class="tips">
              <van-icon name="info-o" />
              <span>提示：选中上方文本框的内容，然后使用 Ctrl+C (Windows) 或 Cmd+C (Mac) 复制</span>
            </div>
          </div>
          <div class="manual-copy-footer">
            <van-button type="default" @click="cancelCopy">取消</van-button>
            <van-button type="primary" @click="confirmCopy">我已复制</van-button>
          </div>
        </div>
      </div>
    `;

    // 绑定事件
    this.bindManualCopyEvents(modal, resolve, reject);

    // 添加样式
    this.addManualCopyStyles();

    document.body.appendChild(modal);
    document.body.style.overflow = 'hidden';
  }

  /**
   * 绑定手动复制模态框事件
   */
  bindManualCopyEvents(modal, resolve, reject) {
    const backdrop = modal.querySelector('.manual-copy-backdrop');
    const closeBtn = modal.querySelector('.close-btn');
    const cancelBtn = modal.querySelector('.cancelCopy');
    const confirmBtn = modal.querySelector('.confirmCopy');
    const textarea = modal.querySelector('textarea');

    const closeModal = () => {
      document.body.style.overflow = '';
      modal.remove();
      reject({ errMsg: 'setClipboardData:fail user canceled' });
    };

    const confirmCopy = () => {
      document.body.style.overflow = '';
      modal.remove();
      resolve({ errMsg: 'setClipboardData:ok' });
    };

    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) {
        closeModal();
      }
    });

    closeBtn.addEventListener('click', closeModal);
    cancelBtn.addEventListener('click', closeModal);
    confirmBtn.addEventListener('click', confirmCopy);

    // 自动选中文本
    setTimeout(() => {
      textarea.select();
    }, 100);
  }

  /**
   * 添加手动复制样式
   */
  addManualCopyStyles() {
    if (document.getElementById('manual-copy-styles')) return;

    const styles = document.createElement('style');
    styles.id = 'manual-copy-styles';
    styles.textContent = `
      .manual-copy-modal {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 9999;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }
      .manual-copy-backdrop {
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
      }
      .manual-copy-container {
        background: white;
        border-radius: 12px;
        width: 100%;
        max-width: 500px;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      }
      .manual-copy-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px 20px;
        background: #f7f8fa;
        border-bottom: 1px solid #ebedf0;
      }
      .manual-copy-header h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
        color: #323233;
      }
      .close-btn {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        color: #969799;
        background: #f0f0f0;
      }
      .close-btn:hover {
        background: #e0e0e0;
      }
      .manual-copy-content {
        padding: 20px;
        flex: 1;
      }
      .instruction {
        margin: 0 0 12px 0;
        color: #323233;
        font-size: 14px;
      }
      .copy-area {
        position: relative;
        margin-bottom: 16px;
      }
      .copy-area textarea {
        width: 100%;
        min-height: 120px;
        padding: 12px;
        border: 1px solid #ebedf0;
        border-radius: 6px;
        resize: vertical;
        font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
        font-size: 13px;
        line-height: 1.5;
        background: #fafafa;
      }
      .copy-btn {
        position: absolute;
        top: 8px;
        right: 8px;
      }
      .tips {
        display: flex;
        align-items: flex-start;
        gap: 8px;
        padding: 12px;
        background: #f0f8ff;
        border-radius: 6px;
        font-size: 12px;
        color: #1989fa;
        line-height: 1.4;
      }
      .tips .van-icon {
        margin-top: 1px;
        flex-shrink: 0;
      }
      .manual-copy-footer {
        display: flex;
        justify-content: flex-end;
        gap: 12px;
        padding: 16px 20px;
        background: #fafafa;
        border-top: 1px solid #ebedf0;
      }
    `;
    document.head.appendChild(styles);
  }

  /**
   * HTML 转义
   */
  escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
  }

  /**
   * 复制到剪贴板的主要方法
   */
  async copyToClipboard(text) {
    if (!text || typeof text !== 'string') {
      throw new Error('invalid data');
    }

    // 记录复制历史
    this.addToHistory(text);

    // 根据支持情况选择复制方法
    if (this.isSupported.modern && this.isSupported.writeText) {
      // 优先使用现代 Clipboard API
      return await this.copyWithModernAPI(text);
    } else if (this.isSupported.legacy) {
      if (this.isSupported.ie) {
        // IE 浏览器
        return this.copyWithIE(text);
      } else if (this.isSupported.execCommand) {
        // 传统浏览器使用 execCommand
        return this.copyWithLegacyMethod(text);
      }
    }

    // 如果没有任何 API 支持，抛出错误
    throw new Error('clipboard not supported');
  }

  /**
   * 添加到复制历史
   */
  addToHistory(text) {
    const record = {
      text: text.substring(0, 100) + (text.length > 100 ? '...' : ''), // 只保存前100个字符
      time: new Date().toISOString(),
      length: text.length,
    };

    this.copyHistory.unshift(record);

    // 限制历史记录数量
    if (this.copyHistory.length > 20) {
      this.copyHistory = this.copyHistory.slice(0, 20);
    }

    // 保存到本地存储
    try {
      localStorage.setItem('clipboard_history', JSON.stringify(this.copyHistory));
    } catch {
      console.warn('Failed to save clipboard history');
    }
  }

  /**
   * 从本地存储加载历史记录
   */
  loadHistory() {
    try {
      const saved = localStorage.getItem('clipboard_history');
      if (saved) {
        this.copyHistory = JSON.parse(saved);
      }
    } catch {
      console.warn('Failed to load clipboard history');
      this.copyHistory = [];
    }
  }

  /**
   * 清空复制历史
   */
  clearHistory() {
    this.copyHistory = [];
    localStorage.removeItem('clipboard_history');
  }

  /**
   * 主要的 setClipboardData 方法 - 完全模拟 uni.setClipboardData
   */
  setClipboardData(options = {}) {
    return new Promise((resolve, reject) => {
      // 标准化参数
      const opts = {
        data: '',
        success: () => {},
        fail: () => {},
        complete: () => {},
        ...options,
      };

      const { data, success, fail, complete } = opts;

      // 参数验证
      if (!data || typeof data !== 'string') {
        const error = {
          errMsg: 'setClipboardData:fail missing or invalid data',
        };
        showFailToast('复制失败：数据无效');
        fail(error);
        complete(error);
        reject(error);
        return;
      }

      // 显示复制中提示
      const loadingToast = showToast({
        message: '复制中...',
        type: 'loading',
        forbidClick: true,
        duration: 0,
      });

      // 尝试复制文本
      this.copyToClipboard(data)
        .then(() => {
          // 清除加载提示
          if (loadingToast) {
            loadingToast.close();
          }

          // 显示成功提示
          showSuccessToast('复制成功');

          // 触发成功回调
          const successResult = { errMsg: 'setClipboardData:ok' };
          success(successResult);
          complete(successResult);
          resolve(successResult);
        })
        .catch((error) => {
          // 清除加载提示
          if (loadingToast) {
            loadingToast.close();
          }

          console.error('Copy to clipboard failed:', error);

          // 判断是否启用降级方案
          if (this.fallbackEnabled && error.message !== 'setClipboardData:fail user canceled') {
            // 使用降级方案：手动复制
            this.showManualCopyFallback(data)
              .then((fallbackResult) => {
                success(fallbackResult);
                complete(fallbackResult);
                resolve(fallbackResult);
              })
              .catch(() => {
                // 降级方案也失败了
                const finalError = {
                  errMsg: `setClipboardData:fail ${error.message}`,
                  originalError: error,
                };
                showFailToast('复制失败');
                fail(finalError);
                complete(finalError);
                reject(finalError);
              });
          } else {
            // 不使用降级方案，直接返回错误
            const finalError = {
              errMsg: `setClipboardData:fail ${error.message}`,
              originalError: error,
            };
            showFailToast('复制失败');
            fail(finalError);
            complete(finalError);
            reject(finalError);
          }
        });
    });
  }

  /**
   * 获取复制历史
   */
  getHistory() {
    this.loadHistory();
    return this.copyHistory;
  }

  /**
   * 检查剪贴板支持性
   */
  checkSupport() {
    return {
      supported: this.isSupported.modern || this.isSupported.legacy,
      modern: this.isSupported.modern,
      legacy: this.isSupported.legacy,
      details: this.isSupported,
    };
  }

  /**
   * 启用/禁用降级方案
   */
  setFallbackEnabled(enabled) {
    this.fallbackEnabled = !!enabled;
  }

  /**
   * 销毁实例
   */
  destroy() {
    // 清理模态框
    const modals = document.querySelectorAll('.manual-copy-modal');
    modals.forEach((modal) => modal.remove());

    // 清理样式
    const styles = document.querySelectorAll('#manual-copy-styles');
    styles.forEach((style) => style.remove());

    // 恢复body样式
    document.body.style.overflow = '';

    this.copyHistory = [];
  }
}

// 初始化全局实例
function initClipboard() {
  if (!clipboardInstance) {
    clipboardInstance = new ClipboardImpl();
    clipboardInstance.loadHistory();
  }
  return clipboardInstance;
}

// 主要的 setClipboardData 函数 - 直接调用的 API
function setClipboardData(options = {}) {
  initClipboard();
  return clipboardInstance.setClipboardData(options);
}

// 工具函数集合
const clipboardUtils = {
  // 获取剪贴板支持信息
  checkSupport() {
    initClipboard();
    return clipboardInstance.checkSupport();
  },

  // 获取复制历史
  getHistory() {
    initClipboard();
    return clipboardInstance.getHistory();
  },

  // 清空复制历史
  clearHistory() {
    initClipboard();
    clipboardInstance.clearHistory();
  },

  // 启用/禁用降级方案
  setFallbackEnabled(enabled) {
    initClipboard();
    clipboardInstance.setFallbackEnabled(enabled);
  },

  // 直接复制文本（简化版本）
  copy(text, showToast = true) {
    initClipboard();
    return clipboardInstance.setClipboardData({
      data: text,
      success: showToast ? () => showSuccessToast('复制成功') : () => {},
      fail: showToast ? () => showFailToast('复制失败') : () => {},
    });
  },

  // 销毁实例
  destroy() {
    if (clipboardInstance) {
      clipboardInstance.destroy();
      clipboardInstance = null;
    }
  },
};

// 设置全局挂载点（可选）
if (typeof window !== 'undefined') {
  window.uni = window.uni || {};
  window.uni.setClipboardData = setClipboardData;
}

// 导出主要函数和工具
export { setClipboardData };
export { clipboardUtils };
export default setClipboardData;

// CommonJS 导出（如果需要）
if (typeof module !== 'undefined' && module.exports) {
  module.exports = setClipboardData;
  module.exports.default = setClipboardData;
  module.exports.clipboardUtils = clipboardUtils;
}
