import type { App, Directive } from 'vue';

/**
 * 解决群公告中的超链接点击
 */
const createHyperlinkClickDirective: Directive = {
  mounted(el, binding) {
    const text = binding.value || el.textContent;
    if (!text) return;

    const urlRegex =
      /(https?:\/\/|www\.)[a-zA-Z0-9-._~:/?#[\]@!$&'()*+,;=%]+(?![^\s<]*>|[^\s]*<\/)/gi;

    el.innerHTML = text.replaceAll(urlRegex, (url) => {
      // 补全协议（如果是www开头）
      const fullUrl = url.startsWith('www.') ? `https://${url}` : url;
      return `<a href="${fullUrl}" target="_blank" rel="noopener noreferrer">${url}</a>`;
    });
  },
};

export function setupHyperlinkClick(app: App) {
  app.directive('hyperlinkClick', createHyperlinkClickDirective);
}

export default setupHyperlinkClick;
