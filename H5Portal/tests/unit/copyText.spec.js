import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

import { copyText } from '@/utils/copyText.js';

describe('utils/copyText.js - copyText', () => {
  let writeTextSpy;
  let execCommandSpy;
  let originalClipboard;
  let originalIsSecureContext;

  beforeEach(() => {
    // 保存原始值
    originalClipboard = navigator.clipboard;
    originalIsSecureContext = window.isSecureContext;

    // 默认模拟安全上下文 + Clipboard API 可用
    writeTextSpy = vi.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: writeTextSpy },
    });
    Object.defineProperty(window, 'isSecureContext', {
      configurable: true,
      value: true,
    });

    // mock execCommand（降级路径用）
    execCommandSpy = vi.fn().mockReturnValue(true);
    document.execCommand = execCommandSpy;
  });

  afterEach(() => {
    // 还原
    if (originalClipboard === undefined) {
      delete navigator.clipboard;
    } else {
      Object.defineProperty(navigator, 'clipboard', {
        configurable: true,
        value: originalClipboard,
      });
    }
    Object.defineProperty(window, 'isSecureContext', {
      configurable: true,
      value: originalIsSecureContext,
    });
    delete document.execCommand;
    vi.restoreAllMocks();
  });

  describe('纯文本复制', () => {
    it('应调用 navigator.clipboard.writeText 并提示成功', async () => {
      const showToast = vi.fn();
      await copyText('hello world', { showToast });

      expect(writeTextSpy).toHaveBeenCalledWith('hello world');
      expect(showToast).toHaveBeenCalledWith('复制成功');
    });

    it('未传入 showToast 时应使用 console.log 兜底', async () => {
      const logSpy = vi.spyOn(console, 'log').mockImplementation(() => {});
      await copyText('plain text');

      expect(writeTextSpy).toHaveBeenCalledWith('plain text');
      expect(logSpy).toHaveBeenCalledWith('复制成功');
    });
  });

  describe('HTML 转纯文本', () => {
    it('包含 < > 的字符串应被去除 HTML 标签', async () => {
      await copyText('<p>hello</p>');
      expect(writeTextSpy).toHaveBeenCalledWith('hello');
    });

    it('带属性的复杂 HTML 应正确提取纯文本', async () => {
      await copyText('<div class="x" data-id="1"><span>foo</span><span>bar</span></div>');
      expect(writeTextSpy).toHaveBeenCalledWith('foobar');
    });

    it('自闭合标签应被去除', async () => {
      await copyText('line1<br/>line2');
      expect(writeTextSpy).toHaveBeenCalledWith('line1line2');
    });

    it('仅包含 < 但不包含 > 的字符串应原样保留（不视为 HTML）', async () => {
      await copyText('a < b');
      expect(writeTextSpy).toHaveBeenCalledWith('a < b');
    });

    it('仅包含 > 但不包含 < 的字符串应原样保留', async () => {
      await copyText('a > b');
      expect(writeTextSpy).toHaveBeenCalledWith('a > b');
    });

    it('非字符串入参应被 extractText 处理为空字符串', async () => {
      await copyText(null);
      expect(writeTextSpy).toHaveBeenCalledWith('');
    });
  });

  describe('Clipboard API 降级路径', () => {
    it('writeText 抛错时应降级到 execCommand', async () => {
      writeTextSpy.mockRejectedValueOnce(new Error('permission denied'));
      const showToast = vi.fn();

      await copyText('text', { showToast });

      expect(writeTextSpy).toHaveBeenCalled();
      expect(execCommandSpy).toHaveBeenCalledWith('copy');
      expect(showToast).toHaveBeenCalledWith('复制成功');
    });

    it('无 navigator.clipboard 时应直接降级到 execCommand', async () => {
      Object.defineProperty(navigator, 'clipboard', {
        configurable: true,
        value: undefined,
      });
      const showToast = vi.fn();

      await copyText('fallback text', { showToast });

      expect(writeTextSpy).not.toHaveBeenCalled();
      expect(execCommandSpy).toHaveBeenCalledWith('copy');
      expect(showToast).toHaveBeenCalledWith('复制成功');
    });

    it('window.isSecureContext=false 时应直接降级到 execCommand', async () => {
      Object.defineProperty(window, 'isSecureContext', {
        configurable: true,
        value: false,
      });
      const showToast = vi.fn();

      await copyText('insecure text', { showToast });

      expect(writeTextSpy).not.toHaveBeenCalled();
      expect(execCommandSpy).toHaveBeenCalledWith('copy');
      expect(showToast).toHaveBeenCalledWith('复制成功');
    });

    it('execCommand 返回 false 时应提示失败', async () => {
      Object.defineProperty(navigator, 'clipboard', {
        configurable: true,
        value: undefined,
      });
      execCommandSpy.mockReturnValue(false);
      const showToast = vi.fn();
      const errorSpy = vi.spyOn(console, 'error').mockImplementation(() => {});

      await copyText('will fail', { showToast });

      expect(execCommandSpy).toHaveBeenCalledWith('copy');
      expect(showToast).toHaveBeenCalledWith('复制失败，请稍后再试');
      expect(errorSpy).toHaveBeenCalled();
    });

    it('降级路径应在 textarea 上设置 value 并移除元素', async () => {
      Object.defineProperty(navigator, 'clipboard', {
        configurable: true,
        value: undefined,
      });

      const appendSpy = vi.spyOn(document.body, 'appendChild');
      const removeSpy = vi.spyOn(document.body, 'removeChild');

      await copyText('textarea content');

      expect(appendSpy).toHaveBeenCalled();
      expect(removeSpy).toHaveBeenCalled();
      const textarea = appendSpy.mock.calls[0][0];
      expect(textarea.value).toBe('textarea content');
      expect(textarea.style.position).toBe('fixed');
    });
  });

  describe('options 参数处理', () => {
    it('未传 options 时应正常工作', async () => {
      await copyText('no options');
      expect(writeTextSpy).toHaveBeenCalledWith('no options');
    });

    it('options 为空对象时应正常工作', async () => {
      await copyText('empty options', {});
      expect(writeTextSpy).toHaveBeenCalledWith('empty options');
    });
  });
});
