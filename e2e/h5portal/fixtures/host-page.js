import { createHostFixture } from '../../shared/fixtures/host-page.js';
import { bindHasProvider } from '../../shared/helpers/proxy-health.js';

/**
 * H5Portal 端 fixture
 * - stubQuery '?bridge=stub'（SDK 代理桥桩模式，与本地联调同一入口）
 * - gotoRouteMode 'router': H5Portal/src/main.js 会把 router 暴露到 window.__router
 * - 代理健康检查端口 8788（dev-bridge/h5portal 实例）
 */
export const { test, expect } = createHostFixture({
  stubQuery: '?bridge=stub',
  gotoRouteMode: 'router',
});

export const hasProvider = bindHasProvider(8788);
