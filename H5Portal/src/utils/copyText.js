// 从 HTML 字符串中提取纯文本
function extractText(html) {
  if (typeof html !== 'string') return '';
  if (html.includes('<') && html.includes('>')) {
    return html.replace(/<[^>]*>/g, '');
  }
  return html;
}

// 降级复制（textarea + execCommand）
function fallbackCopyTextToClipboard(text, showToastFn) {
  const textArea = document.createElement('textarea');
  textArea.value = text;
  textArea.style.position = 'fixed';
  textArea.style.top = '-9999px';
  textArea.style.left = '-9999px';
  document.body.appendChild(textArea);
  textArea.focus();
  textArea.select();

  try {
    const successful = document.execCommand('copy');
    if (!successful) throw new Error('复制命令执行失败');
    showToastFn?.('复制成功');
  } catch (err) {
    console.error('复制失败:', err);
    showToastFn?.('复制失败，请稍后再试');
  } finally {
    document.body.removeChild(textArea);
  }
}

// 复制文本到剪贴板（支持 HTML 自动转纯文本）
export async function copyText(htmlOrText, options = {}) {
  const textToCopy = extractText(htmlOrText);
  const { showToast } = options;

  // 提示函数
  const toast = showToast || console.log;

  // 优先使用 Clipboard API
  if (navigator.clipboard && window.isSecureContext) {
    try {
      await navigator.clipboard.writeText(textToCopy);
      toast('复制成功');
    } catch (err) {
      console.error('Clipboard API 失败，使用降级方案', err);
      fallbackCopyTextToClipboard(textToCopy, toast);
    }
  } else {
    fallbackCopyTextToClipboard(textToCopy, toast);
  }
}