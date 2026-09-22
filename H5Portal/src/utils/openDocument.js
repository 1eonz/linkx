/**
 * uni.openDocument() 完全模拟工具函数
 * 支持直接调用 openDocument()，与 uniapp API 完全一致
 */

import { showToast, showLoadingToast, closeToast, showConfirmDialog } from 'vant';

// 全局文档预览实例
let documentPreviewInstance = null;

class OpenDocumentImpl {
  constructor() {
    this.previewWindow = null;
    this.downloadLink = null;
    this.isMobile = /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
      navigator.userAgent,
    );
    this.isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent);
    this.isAndroid = /Android/.test(navigator.userAgent);
    this.history = [];

    // 支持的文档类型映射
    this.supportFormats = {
      // PDF
      pdf: { type: 'pdf', mime: 'application/pdf', name: 'PDF文档' },
      // Word
      doc: { type: 'word', mime: 'application/msword', name: 'Word文档' },
      docx: {
        type: 'word',
        mime: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        name: 'Word文档',
      },
      rtf: { type: 'word', mime: 'application/rtf', name: '富文本文档' },
      // Excel
      xls: {
        type: 'excel',
        mime: 'application/vnd.ms-excel',
        name: 'Excel表格',
      },
      xlsx: {
        type: 'excel',
        mime: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        name: 'Excel表格',
      },
      csv: { type: 'excel', mime: 'text/csv', name: 'CSV表格' },
      // PowerPoint
      ppt: {
        type: 'ppt',
        mime: 'application/vnd.ms-powerpoint',
        name: 'PowerPoint演示',
      },
      pptx: {
        type: 'ppt',
        mime: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        name: 'PowerPoint演示',
      },
      // 文本
      txt: { type: 'txt', mime: 'text/plain', name: '文本文档' },
      html: { type: 'html', mime: 'text/html', name: 'HTML文档' },
      htm: { type: 'html', mime: 'text/html', name: 'HTML文档' },
      xml: { type: 'xml', mime: 'text/xml', name: 'XML文档' },
      json: { type: 'json', mime: 'application/json', name: 'JSON数据' },
      md: { type: 'md', mime: 'text/markdown', name: 'Markdown文档' },
      // 图片
      jpg: { type: 'image', mime: 'image/jpeg', name: 'JPG图片' },
      jpeg: { type: 'image', mime: 'image/jpeg', name: 'JPEG图片' },
      png: { type: 'image', mime: 'image/png', name: 'PNG图片' },
      gif: { type: 'image', mime: 'image/gif', name: 'GIF图片' },
      bmp: { type: 'image', mime: 'image/bmp', name: 'BMP图片' },
      webp: { type: 'image', mime: 'image/webp', name: 'WebP图片' },
      svg: { type: 'image', mime: 'image/svg+xml', name: 'SVG图片' },
    };
  }

  /**
   * 获取文件扩展名
   */
  getFileExtension(filePath) {
    if (!filePath) return '';
    const parts = filePath.toLowerCase().split('.');
    return parts.length > 1 ? parts[parts.length - 1] : '';
  }

  /**
   * 根据文件路径获取文件信息
   */
  getFileInfo(filePath) {
    const ext = this.getFileExtension(filePath);
    return (
      this.supportFormats[ext] || {
        type: 'unknown',
        mime: 'application/octet-stream',
        name: '未知文件',
      }
    );
  }

  /**
   * 检查是否支持该文件格式
   */
  isSupported(filePath) {
    const fileInfo = this.getFileInfo(filePath);
    return fileInfo.type !== 'unknown';
  }

  /**
   * 从路径提取文件名
   */
  extractFileName(filePath) {
    if (!filePath) return 'document';
    // 移除查询参数和锚点
    const cleanPath = filePath.split('?')[0].split('#')[0];
    const parts = cleanPath.split('/');
    let fileName = parts[parts.length - 1];

    // 解码URL编码的文件名
    try {
      fileName = decodeURIComponent(fileName);
    } catch {
      // 如果解码失败，使用原始文件名
    }

    return fileName || 'document';
  }

  /**
   * 格式化文件大小
   */
  formatFileSize(bytes) {
    if (!bytes) return '';
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return Math.round((bytes / Math.pow(1024, i)) * 100) / 100 + ' ' + sizes[i];
  }

  /**
   * 检测文件是否可预览
   */
  canPreview(filePath) {
    const fileInfo = this.getFileInfo(filePath);
    const previewableTypes = ['pdf', 'image', 'txt', 'html', 'xml', 'json', 'md'];
    return previewableTypes.includes(fileInfo.type);
  }

  /**
   * 在新窗口打开文件
   */
  openInNewWindow(filePath) {
    try {
      let targetUrl = filePath;

      // 处理相对路径
      if (
        !filePath.startsWith('http://') &&
        !filePath.startsWith('https://') &&
        !filePath.startsWith('data:')
      ) {
        targetUrl = new URL(filePath, window.location.origin).href;
      }

      // 尝试在新窗口打开
      this.previewWindow = window.open(targetUrl, '_blank');

      if (!this.previewWindow) {
        throw new Error('Popup blocked or failed to open');
      }

      return true;
    } catch (error) {
      console.error('Open in new window failed:', error);
      return false;
    }
  }

  /**
   * 下载文件
   */
  downloadFile(filePath, fileName) {
    try {
      const link = document.createElement('a');
      link.href = filePath;
      link.download = fileName || this.extractFileName(filePath);
      link.style.display = 'none';

      // 处理跨域下载
      if (filePath.startsWith('http') && !filePath.includes(window.location.hostname)) {
        link.target = '_blank';
        delete link.download;
      }

      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);

      return true;
    } catch (error) {
      console.error('Download failed:', error);
      return false;
    }
  }

  /**
   * 预览图片
   */
  async previewImage(filePath) {
    return new Promise((resolve, reject) => {
      try {
        // 检查图片是否可以访问
        const img = new Image();
        img.onload = () => {
          this.createImagePreviewModal(filePath);
          resolve({ errMsg: 'openDocument:ok' });
        };
        img.onerror = () => {
          reject({ errMsg: 'openDocument:fail image load failed' });
        };
        img.src = filePath;
      } catch (error) {
        reject({ errMsg: `openDocument:fail ${error.message}` });
      }
    });
  }

  /**
   * 创建图片预览模态框
   */
  createImagePreviewModal(imageSrc) {
    // 移除已存在的预览框
    const existingModal = document.querySelector('.image-preview-modal');
    if (existingModal) {
      existingModal.remove();
    }

    const modal = document.createElement('div');
    modal.className = 'image-preview-modal';
    modal.innerHTML = `
      <div class="image-preview-backdrop">
        <div class="image-preview-container">
          <div class="image-preview-header">
            <h3>图片预览</h3>
            <div class="header-actions">
              <van-icon name="download" class="action-btn download-btn" title="下载" />
              <van-icon name="cross" class="action-btn close-btn" title="关闭" />
            </div>
          </div>
          <div class="image-preview-content">
            <img src="${imageSrc}" alt="预览图片" />
          </div>
        </div>
      </div>
    `;

    // 添加事件监听
    const backdrop = modal.querySelector('.image-preview-backdrop');
    const closeBtn = modal.querySelector('.close-btn');
    const downloadBtn = modal.querySelector('.download-btn');

    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) {
        modal.remove();
      }
    });

    closeBtn.addEventListener('click', () => {
      modal.remove();
    });

    downloadBtn.addEventListener('click', () => {
      this.downloadFile(imageSrc, this.extractFileName(imageSrc));
      showToast('开始下载');
    });

    // 添加样式
    this.addImagePreviewStyles();

    document.body.appendChild(modal);

    // 防止背景滚动
    document.body.style.overflow = 'hidden';
    modal.addEventListener(
      'click',
      () => {
        document.body.style.overflow = '';
      },
      { once: true },
    );
  }

  /**
   * 添加图片预览样式
   */
  addImagePreviewStyles() {
    if (document.getElementById('image-preview-styles')) return;

    const styles = document.createElement('style');
    styles.id = 'image-preview-styles';
    styles.textContent = `
      .image-preview-modal {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 9999;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }
      .image-preview-backdrop {
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.9);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
      }
      .image-preview-container {
        background: white;
        border-radius: 12px;
        max-width: 95vw;
        max-height: 95vh;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      }
      .image-preview-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px 20px;
        background: #f7f8fa;
        border-bottom: 1px solid #ebedf0;
      }
      .image-preview-header h3 {
        margin: 0;
        font-size: 16px;
        font-weight: 500;
        color: #323233;
      }
      .header-actions {
        display: flex;
        gap: 8px;
      }
      .action-btn {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.2s;
      }
      .download-btn {
        background: #1989fa;
        color: white;
      }
      .download-btn:hover {
        background: #007aff;
      }
      .close-btn {
        background: #969799;
        color: white;
      }
      .close-btn:hover {
        background: #646566;
      }
      .image-preview-content {
        flex: 1;
        overflow: auto;
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
        min-height: 200px;
      }
      .image-preview-content img {
        max-width: 100%;
        max-height: 100%;
        object-fit: contain;
        border-radius: 8px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
      }
    `;
    document.head.appendChild(styles);
  }

  /**
   * 预览文本文件
   */
  async previewTextFile(filePath, fileName) {
    showLoadingToast({
      message: '加载文档中...',
      forbidClick: true,
      duration: 0,
    });

    try {
      const response = await fetch(filePath);
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}: ${response.statusText}`);
      }

      const content = await response.text();
      this.createTextPreviewModal(content, fileName || this.extractFileName(filePath));

      closeToast();
      return { errMsg: 'openDocument:ok' };
    } catch (fetchError) {
      // fetch 失败，尝试作为 data URL 处理
      if (filePath.startsWith('data:text/')) {
        try {
          const content = decodeURIComponent(filePath.split(',')[1]);
          this.createTextPreviewModal(content, fileName || this.extractFileName(filePath));
          closeToast();
          return { errMsg: 'openDocument:ok' };
        } catch {
          closeToast();
          throw new Error('Failed to decode data URL');
        }
      }

      closeToast();
      throw fetchError;
    }
  }

  /**
   * 创建文本预览模态框
   */
  createTextPreviewModal(content, fileName) {
    // 移除已存在的模态框
    const existingModal = document.querySelector('.text-preview-modal');
    if (existingModal) {
      existingModal.remove();
    }

    const fileInfo = this.getFileInfo(fileName);
    const modal = document.createElement('div');
    modal.className = 'text-preview-modal';

    // 根据文件类型设置语法高亮
    const language = this.getHighlightLanguage(fileInfo.type);

    modal.innerHTML = `
      <div class="text-preview-backdrop">
        <div class="text-preview-container">
          <div class="text-preview-header">
            <div class="header-info">
              <h3>${fileName}</h3>
              <span class="file-type">${fileInfo.name}</span>
            </div>
            <div class="header-actions">
              <van-icon name="download" class="action-btn download-btn" title="下载" />
              <van-icon name="copy" class="action-btn copy-btn" title="复制" />
              <van-icon name="cross" class="action-btn close-btn" title="关闭" />
            </div>
          </div>
          <div class="text-preview-content">
            <div class="text-toolbar">
              <van-tag size="mini" type="primary">${language}</van-tag>
              <span class="char-count">${content.length} 字符</span>
            </div>
            <div class="text-container">
              <pre class="text-content ${language}"><code>${this.escapeHtml(content)}</code></pre>
            </div>
          </div>
        </div>
      </div>
    `;

    // 添加事件监听
    this.bindTextModalEvents(modal, content, fileName);

    // 添加样式
    this.addTextPreviewStyles();

    document.body.appendChild(modal);

    // 防止背景滚动
    document.body.style.overflow = 'hidden';
  }

  /**
   * 绑定文本模态框事件
   */
  bindTextModalEvents(modal, content, fileName) {
    const backdrop = modal.querySelector('.text-preview-backdrop');
    const closeBtn = modal.querySelector('.close-btn');
    const downloadBtn = modal.querySelector('.download-btn');
    const copyBtn = modal.querySelector('.copy-btn');
    const textContainer = modal.querySelector('.text-content');

    backdrop.addEventListener('click', (e) => {
      if (e.target === backdrop) {
        document.body.style.overflow = '';
        modal.remove();
      }
    });

    closeBtn.addEventListener('click', () => {
      document.body.style.overflow = '';
      modal.remove();
    });

    downloadBtn.addEventListener('click', () => {
      this.downloadTextFile(content, fileName);
    });

    copyBtn.addEventListener('click', async () => {
      try {
        await navigator.clipboard.writeText(content);
        showToast('已复制到剪贴板');
      } catch {
        // 降级方案
        const textArea = document.createElement('textarea');
        textArea.value = content;
        document.body.appendChild(textArea);
        textArea.select();
        document.execCommand('copy');
        document.body.removeChild(textArea);
        showToast('已复制到剪贴板');
      }
    });

    // 双击全选文本
    textContainer.addEventListener('dblclick', () => {
      const selection = window.getSelection();
      const range = document.createRange();
      range.selectNodeContents(textContainer);
      selection.removeAllRanges();
      selection.addRange(range);
      showToast('已选中全部文本');
    });
  }

  /**
   * 获取语法高亮语言
   */
  getHighlightLanguage(type) {
    const langMap = {
      json: 'json',
      xml: 'xml',
      html: 'html',
      md: 'markdown',
      js: 'javascript',
      css: 'css',
      txt: 'plaintext',
    };
    return langMap[type] || 'plaintext';
  }

  /**
   * 添加文本预览样式
   */
  addTextPreviewStyles() {
    if (document.getElementById('text-preview-styles')) return;

    const styles = document.createElement('style');
    styles.id = 'text-preview-styles';
    styles.textContent = `
      .text-preview-modal {
        position: fixed;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        z-index: 9999;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
      }
      .text-preview-backdrop {
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.5);
        display: flex;
        align-items: center;
        justify-content: center;
        padding: 20px;
      }
      .text-preview-container {
        background: white;
        border-radius: 12px;
        width: 100%;
        max-width: 900px;
        max-height: 90vh;
        display: flex;
        flex-direction: column;
        overflow: hidden;
        box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
      }
      .text-preview-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 16px 20px;
        background: #f7f8fa;
        border-bottom: 1px solid #ebedf0;
      }
      .header-info h3 {
        margin: 0 0 4px 0;
        font-size: 16px;
        font-weight: 500;
        color: #323233;
      }
      .file-type {
        font-size: 12px;
        color: #969799;
        background: #f0f0f0;
        padding: 2px 8px;
        border-radius: 4px;
      }
      .header-actions {
        display: flex;
        gap: 8px;
      }
      .action-btn {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
        cursor: pointer;
        transition: all 0.2s;
        background: #f0f0f0;
        color: #323233;
      }
      .action-btn:hover {
        background: #e0e0e0;
      }
      .download-btn:hover {
        background: #1989fa;
        color: white;
      }
      .copy-btn:hover {
        background: #07c160;
        color: white;
      }
      .close-btn:hover {
        background: #ee0a24;
        color: white;
      }
      .text-preview-content {
        flex: 1;
        display: flex;
        flex-direction: column;
        overflow: hidden;
      }
      .text-toolbar {
        display: flex;
        justify-content: space-between;
        align-items: center;
        padding: 12px 20px;
        background: #fafafa;
        border-bottom: 1px solid #ebedf0;
      }
      .char-count {
        font-size: 12px;
        color: #969799;
      }
      .text-container {
        flex: 1;
        overflow: auto;
        background: #fafafa;
      }
      .text-content {
        margin: 0;
        padding: 20px;
        font-family: 'Monaco', 'Menlo', 'Ubuntu Mono', monospace;
        font-size: 14px;
        line-height: 1.6;
        color: #333;
        white-space: pre-wrap;
        word-wrap: break-word;
        background: transparent;
      }
      .text-content.json { color: #24292e; }
      .text-content.xml { color: #0969da; }
      .text-content.html { color: #d73a49; }
      .text-content.markdown { color: #24292e; }
      .text-content.javascript { color: #f1e05a; }
      .text-content.css { color: #1572b6; }
    `;
    document.head.appendChild(styles);
  }

  /**
   * 下载文本文件
   */
  downloadTextFile(content, fileName) {
    try {
      const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = fileName;
      link.click();
      URL.revokeObjectURL(url);
      showToast('下载开始');
    } catch (error) {
      console.error('Download text file failed:', error);
      showToast('下载失败');
    }
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
   * 主要的 openDocument 方法 - 完全模拟 uni.openDocument
   */
  openDocument(options = {}) {
    return new Promise((resolve, reject) => {
      // 标准化参数
      const opts = {
        filePath: '',
        fileType: '',
        fileName: '',
        showMenu: true,
        ...options,
      };

      const { filePath, fileName, success, fail, complete } = opts;

      // 参数验证
      if (!filePath || typeof filePath !== 'string') {
        const error = {
          errMsg: 'openDocument:fail missing required argument filePath',
        };
        showToast('缺少文件路径');
        fail?.(error);
        complete?.(error);
        reject(error);
        return;
      }

      // 标准化文件路径
      const normalizedPath = filePath.trim();

      // 检查文件支持性
      if (!this.isSupported(normalizedPath)) {
        const error = { errMsg: 'openDocument:fail unsupported file type' };
        showToast('不支持的文档格式');
        fail?.(error);
        complete?.(error);
        reject(error);
        return;
      }

      // 显示加载提示
      showLoadingToast({
        message: '准备打开文档...',
        forbidClick: true,
        duration: 0,
      });

      try {
        const fileInfo = this.getFileInfo(normalizedPath);
        const finalFileName = fileName || this.extractFileName(normalizedPath);

        // 添加到历史记录
        this.addToHistory({
          path: normalizedPath,
          name: finalFileName,
          type: fileInfo.type,
          time: new Date().toISOString(),
        });

        // 根据文件类型和平台选择合适的预览方式
        this.handleFileByType(normalizedPath, finalFileName, fileInfo)
          .then(() => {
            closeToast();
            success?.({ errMsg: 'openDocument:ok' });
            complete?.({ errMsg: 'openDocument:ok' });
            resolve({ errMsg: 'openDocument:ok' });
          })
          .catch((error) => {
            closeToast();

            // 如果预览失败且不是用户取消，询问是否下载
            if (error.errMsg !== 'openDocument:fail user canceled') {
              this.handlePreviewFailure(normalizedPath, finalFileName, error)
                .then((downloadResult) => {
                  if (downloadResult) {
                    success?.({ errMsg: 'openDocument:ok' });
                    complete?.({ errMsg: 'openDocument:ok' });
                    resolve({ errMsg: 'openDocument:ok' });
                  } else {
                    fail?.(error);
                    complete?.(error);
                    reject(error);
                  }
                })
                .catch(() => {
                  fail?.(error);
                  complete?.(error);
                  reject(error);
                });
            } else {
              fail?.(error);
              complete?.(error);
              reject(error);
            }
          });
      } catch (error) {
        closeToast();
        const errorObj = {
          errMsg: `openDocument:fail ${error.message || 'unknown error'}`,
          error: error,
        };
        showToast('打开文档失败');
        fail?.(errorObj);
        complete?.(errorObj);
        reject(errorObj);
      }
    });
  }

  /**
   * 根据文件类型处理
   */
  async handleFileByType(filePath, fileName, fileInfo) {
    switch (fileInfo.type) {
      case 'image':
        return await this.previewImage(filePath);

      case 'txt':
      case 'html':
      case 'xml':
      case 'json':
      case 'md':
        return await this.previewTextFile(filePath, fileName);

      case 'pdf':
      case 'word':
      case 'excel':
      case 'ppt':
      default:
        // Office 文档和 PDF 尝试多种方式打开
        if (this.canPreview(filePath)) {
          if (this.isMobile) {
            // 移动端对于某些格式可能需要在新窗口尝试
            const opened = this.openInNewWindow(filePath, fileName);
            if (opened) {
              return { errMsg: 'openDocument:ok' };
            } else {
              throw { errMsg: 'openDocument:fail popup blocked' };
            }
          } else {
            // 桌面端直接新窗口打开
            const opened = this.openInNewWindow(filePath, fileName);
            if (opened) {
              return { errMsg: 'openDocument:ok' };
            } else {
              throw { errMsg: 'openDocument:fail popup blocked' };
            }
          }
        } else {
          throw { errMsg: 'openDocument:fail cannot preview' };
        }
    }
  }

  /**
   * 处理预览失败
   */
  async handlePreviewFailure(filePath, fileName, error) {
    return new Promise((resolve) => {
      showConfirmDialog({
        title: '预览失败',
        message: `无法预览此文档，是否下载查看？\n\n文件：${fileName}\n错误：${error.errMsg || '未知错误'}`,
        confirmButtonText: '下载',
        cancelButtonText: '取消',
        allowHtml: true,
      })
        .then(() => {
          const downloaded = this.downloadFile(filePath, fileName);
          resolve(downloaded);
        })
        .catch(() => {
          resolve(false);
        });
    });
  }

  /**
   * 添加到历史记录
   */
  addToHistory(record) {
    // 避免重复记录
    const existingIndex = this.history.findIndex((item) => item.path === record.path);
    if (existingIndex >= 0) {
      this.history.splice(existingIndex, 1);
    }

    this.history.unshift(record);

    // 限制历史记录数量
    if (this.history.length > 50) {
      this.history = this.history.slice(0, 50);
    }

    // 保存到本地存储
    try {
      localStorage.setItem('opendocument_history', JSON.stringify(this.history));
    } catch {
      console.warn('Failed to save history to localStorage');
    }
  }

  /**
   * 从本地存储加载历史记录
   */
  loadHistory() {
    try {
      const saved = localStorage.getItem('opendocument_history');
      if (saved) {
        this.history = JSON.parse(saved);
      }
    } catch {
      console.warn('Failed to load history from localStorage');
      this.history = [];
    }
  }

  /**
   * 清空历史记录
   */
  clearHistory() {
    this.history = [];
    localStorage.removeItem('opendocument_history');
  }

  /**
   * 销毁实例
   */
  destroy() {
    // 清理模态框
    const modals = document.querySelectorAll('.image-preview-modal, .text-preview-modal');
    modals.forEach((modal) => modal.remove());

    // 清理样式
    const styles = document.querySelectorAll('#image-preview-styles, #text-preview-styles');
    styles.forEach((style) => style.remove());

    // 恢复body样式
    document.body.style.overflow = '';

    this.previewWindow = null;
    this.downloadLink = null;
  }
}

// 初始化全局实例
function initDocumentPreview() {
  if (!documentPreviewInstance) {
    documentPreviewInstance = new OpenDocumentImpl();
    documentPreviewInstance.loadHistory();
  }
  return documentPreviewInstance;
}

// 主要的 openDocument 函数 - 直接调用的 API
function openDocument(options = {}) {
  initDocumentPreview();
  return documentPreviewInstance.openDocument(options);
}

// 工具函数集合
const openDocumentUtils = {
  // 获取支持的文件格式
  getSupportFormats() {
    initDocumentPreview();
    return documentPreviewInstance.supportFormats;
  },

  // 检查文件是否支持
  isSupported(filePath) {
    initDocumentPreview();
    return documentPreviewInstance.isSupported(filePath);
  },

  // 获取文件信息
  getFileInfo(filePath) {
    initDocumentPreview();
    return documentPreviewInstance.getFileInfo(filePath);
  },

  // 获取预览历史
  getHistory() {
    initDocumentPreview();
    return documentPreviewInstance.history;
  },

  // 清空预览历史
  clearHistory() {
    initDocumentPreview();
    documentPreviewInstance.clearHistory();
  },

  // 销毁实例
  destroy() {
    if (documentPreviewInstance) {
      documentPreviewInstance.destroy();
      documentPreviewInstance = null;
    }
  },
};

// 设置全局挂载点（可选）
if (typeof window !== 'undefined') {
  window.uni = window.uni || {};
  window.uni.openDocument = openDocument;
}

// 导出主要函数和工具
export { openDocument };
export { openDocumentUtils };
export default openDocument;

// CommonJS 导出（如果需要）
if (typeof module !== 'undefined' && module.exports) {
  module.exports = openDocument;
  module.exports.default = openDocument;
  module.exports.openDocumentUtils = openDocumentUtils;
}
