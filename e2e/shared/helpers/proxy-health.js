/**
 * 检测代理后端是否有 provider 连接（即宿主是否在线）。
 *
 * 无 provider 时，SDK 调用类测试应跳过——桩转发到代理会得到
 * "provider not connected" 错误。
 * 页面结构类测试不依赖 SDK，仍可运行。
 *
 * 用 page.request（Playwright APIRequestContext）发起，绕过浏览器 CORS。
 */
export async function hasProvider(page, healthPort = 8788) {
  try {
    const r = await page.request.get(`http://localhost:${healthPort}`);
    if (!r.ok()) return false;
    const j = await r.json();
    return !!j.providerConnected;
  } catch {
    return false;
  }
}

/**
 * 绑定端口的便捷封装，供各端 fixture 导出:
 *   export const hasProvider = bindHasProvider(8798)  // web
 *   export const hasProvider = bindHasProvider(8788)  // h5portal
 */
export const bindHasProvider = (healthPort) => (page) => hasProvider(page, healthPort);
