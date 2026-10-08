import { test as base, expect } from '@playwright/test';

/**
 * 创建分端 hostPage fixture（Playwright 消费侧工厂）。
 *
 * 本 fixture 是 SDK 代理桥（dev-bridge/）的自动化驱动封装:
 * 以 stub 模式（?bridge=stub）打开页面并等待桩连上代理 —— 与开发人员本地联调
 * 手动打开的 stub 页面完全等价，桥对驱动者无感知。
 *
 * 配置:
 *   stubQuery     stub 模式的 URL 查询串（默认 '?bridge=stub'；
 *                 web 端为 '?bridge=stub&clientType=CSPC' 以模拟 WebView2 宿主形态）
 *   readyTimeout  等待桩连上代理的超时（首次跑 Vite dev 可能很慢，默认 45s）
 *   gotoRouteMode 'router'（走入口暴露的 window.__router.push，避免整页刷新，H5Portal 用）
 *                 | 'url'（history 路由整页跳转，自动保留 stub 查询参数并等待桩重连，web 用）
 *
 * 返回 { test, expect, gotoRoute, waitForBridgeReady }，各端 fixtures/host-page.js 直接调用导出。
 */
export function createHostFixture({
  stubQuery = '?bridge=stub',
  readyTimeout = 45000,
  gotoRouteMode = 'router',
} = {}) {
  /**
   * 等待桩与代理建立连接。
   * stub-core.js 在 ws.onopen 时设置 window.__bridgeReady = true
   */
  async function waitForBridgeReady(page, timeout = readyTimeout) {
    await page.waitForFunction(() => window.__bridgeReady === true, { timeout });
  }

  async function gotoRoute(page, path) {
    if (gotoRouteMode === 'url') {
      // history 路由整页跳转: 保留 stub 查询参数（整刷后桩需要重新加载）
      const current = new URL(page.url());
      const query = current.searchParams.toString();
      await page.goto(query ? `${path}?${query}` : path);
      await waitForBridgeReady(page);
      return;
    }

    // router 模式: 通过入口暴露的 window.__router 切换，避免整页刷新
    // 冷启动期间 main.js 底部 DEV 块可能尚未执行（__router 未挂载），
    // 且应用自身的整页重定向（基座跳转/版本检查）可能覆盖 push 结果，
    // 因此: 先等 __router 就绪；push 后校验 URL，被覆盖则重试一次
    const waitForRouter = () =>
      page.waitForFunction(() => !!window.__router, { timeout: 10000 }).catch(() => {});
    const pushRoute = () =>
      page
        .evaluate(
          (p) => {
            const router = window.__router;
            if (router && typeof router.push === 'function') return router.push(p);
            history.pushState({}, '', p);
            dispatchEvent(new PopStateEvent('popstate'));
          },
          path,
        )
        .catch(() => {
          // 执行上下文被页面自身导航销毁，忽略，由 waitForURL 判定结果
        });

    await waitForRouter();
    let lastErr;
    for (let attempt = 0; attempt < 2; attempt++) {
      await pushRoute();
      try {
        await page.waitForURL(`**${path}*`, { timeout: 15000 });
        return;
      } catch (err) {
        lastErr = err;
        // push 被应用整页重定向覆盖: 等页面重载完成、__router 重新挂载后再试一次
        await waitForRouter();
      }
    }
    throw lastErr;
  }

  const test = base.extend({
    hostPage: async ({ page }, use) => {
      await page.goto('/' + stubQuery);
      await waitForBridgeReady(page);
      await use(page);
    },
    gotoRoute: async ({ hostPage }, use) => {
      await use(async (path) => {
        await gotoRoute(hostPage, path);
      });
    },
  });

  return { test, expect, gotoRoute, waitForBridgeReady };
}
