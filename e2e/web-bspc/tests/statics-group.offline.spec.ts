import { test, expect } from '../../shared/fixtures/test.js';
import { installStaticsScenario, webGroupId, webUser } from '../fixtures/statics-scenario.js';

test.use({ target: 'web-bspc', mode: 'offline' });

test('BSPC statics custom group uses selection result and browser SDK createGroup', async ({ page, host, scenario }) => {
  installStaticsScenario(scenario, host);
  await page.addInitScript(() => localStorage.setItem('ICP-X-Token', 'e2e-web-token'));
  await page.goto('/statics');
  await page.evaluate(() => (window as any).__e2eHost.connect());
  await host.emit(page.mainFrame(), 'onVisibleChange', { data: 'true' });
  await expect.poll(() => host.calls.some(call => call.method === 'getUserInfo')).toBe(true);
  const quick = page.getByText('自定义建群', { exact: true }).first();
  await expect(quick).toBeVisible({ timeout: 30000 });
  await quick.click();
  const selection = await host.waitForCall('openSelectMemberUI');
  expect(selection.data).toMatchObject({ type: 'cooperated' });
  const create = await host.waitForCall('createGroup');
  expect(create.data).toMatchObject({ addMembers: [webUser.userid, '900002'], groupType: '3' });
  const chat = await host.waitForCall('openChat');
  expect(chat.data).toMatchObject({ groupId: webGroupId });
});

test('BSPC statics group dialog rejects API response without opening chat', async ({ page, host, scenario }) => {
  installStaticsScenario(scenario, host, { rejectCreate: true });
  host.manual('createGroup');
  await page.addInitScript(() => localStorage.setItem('ICP-X-Token', 'e2e-web-token'));
  await page.goto('/statics');
  await page.evaluate(() => (window as any).__e2eHost.connect());
  await host.emit(page.mainFrame(), 'onVisibleChange', { data: 'true' });
  await page.getByText('自定义建群', { exact: true }).first().click();
  const selection = await host.waitForCall('openSelectMemberUI');
  const create = await host.waitForCall('createGroup');
  await host.respond(create, { errorCode: 500, errorMsg: 'E2E creation rejected' });
  expect(host.calls.some(call => call.method === 'openChat')).toBe(false);
});
