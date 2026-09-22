/**
 * Vite 插件：动态设置 base 标签
 * 根据当前路径自动检测并设置正确的 base 路径
 * 与 getBaseUrlAll() 函数逻辑保持一致
 *
 * 关键：必须在所有资源加载之前设置 base 标签
 * 使用 document.write 确保在 HTML 解析阶段立即执行
 */
export function dynamicBase() {
  return {
    name: 'dynamic-base',
    transformIndexHtml(html) {
      // 检查 HTML 模板中是否已经包含 base 标签脚本
      // 如果已经包含（在 index.html 中），就不需要再次插入
      if (html.includes('[dynamic-base]') || html.includes('设置 base 路径')) {
        // 已经包含，直接返回
        return html;
      }

      // 如果没有包含，则插入（作为备用方案）
      const baseScript = `<script>
(function() {
  try {
    var pathname = location.pathname || "";
    var basePath = "/linkx/h5portal";
    var linkxIndex = pathname.indexOf("/linkx/h5portal");
    if (linkxIndex !== -1) {
      basePath = pathname.substring(0, linkxIndex + "/linkx/h5portal".length);
    }
    if (!basePath.endsWith("/")) {
      basePath += "/";
    }
    document.write('<base href="' + basePath + '">');
    if (typeof console !== 'undefined' && console.log) {
      console.log('[dynamic-base] 设置 base 路径:', basePath, '当前路径:', pathname);
    }
  } catch (e) {
    console.error('[dynamic-base] 设置 base 标签失败:', e);
    try {
      document.write('<base href="/linkx/h5portal/">');
    } catch (e2) {
      console.error('[dynamic-base] 写入默认 base 失败:', e2);
    }
  }
})();
</script>`;

      // 在 <head> 标签后立即插入
      if (html.includes('<head')) {
        return html.replace(/(<head[^>]*>)/i, `$1\n    ${baseScript}\n    `);
      } else {
        return html.replace(/(<html[^>]*>)/i, `$1\n  ${baseScript}\n  `);
      }
    },
  };
}
