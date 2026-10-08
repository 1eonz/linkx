import { test, expect } from '../fixtures/test.js';

test.beforeEach(async ({ page, host, target }) => {
  await page.goto(`/?target=${target}`);
  await page.waitForFunction(() => (window as any).__sdkReady);
  if (target === 'web-bspc') await page.evaluate(() => (window as any).__e2eHost.connect());
});

test('original SDK receives user info through native transport', async ({ page, host, target }) => {
  const result = await page.evaluate(() => (window as any).WeSpaceSDK.getUserInfo());
  expect(result.userId).toBe('e2e-user');
  const call = await host.waitForCall('getUserInfo');
  if (target === 'web-cspc') expect((call.data as any).platform).toBe('win');
  if (target === 'h5portal') expect((call.data as any).platform).toBe('mobile');
});

test('concurrent same-method requests match distinct ids and repeated waits consume calls', async ({ page, host }) => {
  host.manual('getUserInfo');
  await page.evaluate(() => { const win = window as any; win.__requests = Promise.all([win.WeSpaceSDK.getUserInfo(), win.WeSpaceSDK.getUserInfo()]); });
  const first = await host.waitForCall('getUserInfo');
  const second = await host.waitForCall('getUserInfo');
  expect(first.id).not.toBe(second.id);
  await host.respond(second, { errorCode: 0, data: { userId: 'second' } });
  await host.respond(first, { errorCode: 0, data: { userId: 'first' } });
  expect(await page.evaluate(() => (window as any).__requests)).toEqual([{ userId: 'first' }, { userId: 'second' }]);
  await expect(host.waitForCall('getUserInfo', { timeoutMs: 20 })).rejects.toThrow('timeout');
});

test('native errors preserve observed SDK semantics', async ({ page, host, target }) => {
  host.on('getUserInfo', () => ({ errorCode: 403, errorMsg: 'access denied' }));
  const result = await page.evaluate(async () => {
    try { return { ok: true, value: await (window as any).WeSpaceSDK.getUserInfo() }; }
    catch (error: any) { return { ok: false, code: error.code, message: error.message }; }
  });
  // BSPC currently catches SDK errors; contract captures this upstream behavior.
  expect(result).toEqual(target === 'web-bspc' ? { ok: true, value: undefined } : { ok: false, code: 403, message: 'access denied' });
});

test('host event reaches original SDK listener and ACK is ignored', async ({ page, host, target }) => {
  await page.evaluate(() => { const win = window as any; win.WeSpaceSDK.onVisibleChange((data: unknown) => { win.__visibleResult = data; }); });
  await host.emit(page.mainFrame(), 'onVisibleChange', { visible: true });
  await expect.poll(() => page.evaluate(() => (window as any).__visibleResult)).toEqual(target === 'web-bspc' ? { visible: true } : JSON.stringify({ visible: true }));
  expect(host.calls).toHaveLength(0);
});

test('H5 and CSPC storage preserves SDK base64 and modern platform envelope', async ({ page, host, target }) => {
  test.skip(target === 'web-bspc', 'BSPC original SDK exposes no storage methods');
  expect(await page.evaluate(async () => { const sdk = (window as any).WeSpaceSDK; await sdk.setStorage('e2e-key', 'payload'); return sdk.getStorage('e2e-key'); })).toBe('payload');
  const write = await host.waitForCall('setStorage');
  expect((write.data as any).value).toBe(Buffer.from('payload').toString('base64'));
  expect(host.calls.filter(call => call.method === 'getVersion')).toHaveLength(1);
});

test('transport is injected into popup documents', async ({ page, context, host, target }) => {
  const popupPromise = context.waitForEvent('page');
  await page.evaluate(target => window.open(`/?target=${target}`), target);
  const popup = await popupPromise;
  await popup.waitForFunction(() => (window as any).__sdkReady);
  if (target === 'web-bspc') await popup.evaluate(() => (window as any).__e2eHost.connect());
  expect((await popup.evaluate(() => (window as any).WeSpaceSDK.getUserInfo())).userId).toBe('e2e-user');
  expect((await host.waitForCall('getUserInfo')).frame.page()).toBe(popup);
});
