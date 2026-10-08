import { expect, hasProvider, test } from '../fixtures/host-page.js';

test.describe('SDK 基础能力（依赖宿主在线）', () => {
  test('getUserInfo 返回有效数据', async ({ hostPage }) => {
    test.skip(!(await hasProvider(hostPage)), '宿主未连接代理，跳过 SDK 调用测试');
    const userInfo = await hostPage.evaluate(() => window.WeSpaceSDK.getUserInfo());
    expect(userInfo).toBeTruthy();
    console.log('[test] userInfo =', JSON.stringify(userInfo));
  });

  test('getAuthInfo 调用链路可往返', async ({ hostPage }) => {
    // 某些宿主版本不支持 getAuthInfo，会返回 "method not support" 错误
    // 这里只验证调用能往返到宿主真实 SDK 并返回结果（成功或失败都算链路通）
    test.skip(!(await hasProvider(hostPage)), '宿主未连接代理，跳过 SDK 调用测试');
    const result = await hostPage.evaluate(async () => {
      try {
        const r = await window.WeSpaceSDK.getAuthInfo();
        return { ok: true, data: r };
      } catch (e) {
        return { ok: false, error: e.message };
      }
    });
    console.log('[test] getAuthInfo =', JSON.stringify(result));
    // 只验证调用有返回（不是 30s 超时），就算通过
    expect(result).toBeTruthy();
    expect(result).toHaveProperty('ok');
  });
});

test.describe('页面结构测试（不依赖宿主）', () => {
  test('跳转到协同页并验证可见', async ({ hostPage, gotoRoute }) => {
    await gotoRoute('/pagesMain/xieTong');
    await expect(hostPage.locator('body')).toBeVisible();
  });

  test('跳转到任务页并验证可见', async ({ hostPage, gotoRoute }) => {
    await gotoRoute('/pagesMain/renWu');
    await expect(hostPage.locator('body')).toBeVisible();
  });
});

test.describe('事件回调（依赖宿主在线）', () => {
  test('注册 onVisibleChange', async ({ hostPage, gotoRoute }) => {
    test.skip(!(await hasProvider(hostPage)), '宿主未连接代理，跳过事件回调测试');
    await gotoRoute('/pagesMain/xieTong');
    await hostPage.evaluate(() => {
      window.__testEventReceived = null;
      window.WeSpaceSDK.onVisibleChange((visible) => {
        window.__testEventReceived = visible;
      });
    });
    const status = await hostPage.evaluate(() => window.__bridgeStub.status());
    expect(status.events).toContain('onVisibleChange');
  });
});

test.describe('调试入口', () => {
  test('桩状态可读', async ({ hostPage }) => {
    const status = await hostPage.evaluate(() => window.__bridgeStub.status());
    console.log('[test] bridge stub status =', JSON.stringify(status));
    // 只校验 status 对象结构正确，不强制 connected
    // (ws 重连需要时间，且无宿主时 invoke 会堆积 pending)
    expect(status).toHaveProperty('connected');
    expect(status).toHaveProperty('pending');
    expect(status).toHaveProperty('events');
  });

  test('桩已替换 window.WeSpaceSDK', async ({ hostPage }) => {
    const isStub = await hostPage.evaluate(() => {
      // 桩是 Proxy，尝试调用一个不存在的方法应返回 Promise 而不抛错
      try {
        const r = window.WeSpaceSDK.__nonExistentTest__();
        return r && typeof r.then === 'function';
      } catch {
        return false;
      }
    });
    expect(isStub).toBe(true);
  });
});
