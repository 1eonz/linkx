import { test, expect } from '../../shared/fixtures/test.js';
import { installStaticsScenario, webLabel, webGroupId, webUser } from '../fixtures/statics-scenario.js';

test.use({ target: 'web-cspc', mode: 'offline' });

test('CSPC statics group uses selectMembers direct result and sms conversion', async ({ page, host, scenario }) => {
  installStaticsScenario(scenario, host);
  host.manual('createGroup');
  await page.addInitScript(() => localStorage.setItem('ICP-X-Token', 'e2e-web-token'));
  await page.goto('/statics?clientType=CSPC');
  await expect(page.getByText('自定义建群', { exact: true }).first()).toBeVisible({ timeout: 30000 });
  await page.getByText('自定义建群', { exact: true }).first().click();
  const select = await host.waitForCall('selectMembers');
  expect(select.data).toMatchObject({ maxNumber: 49, mode: '0' });
  await host.respond(select, { errorCode: 0, data: [{ userId: '900002', userName: 'E2E Member' }] });
  const create = await host.waitForCall('createGroup');
  expect(create.data).toMatchObject({ addMembers: `${webUser.userid},900002`, groupType: '3' });
  await host.respond(create, { errorCode: 0, data: { groupId: webGroupId } });
  const sms = await host.waitForCall('sms');
  expect(sms.data).toMatchObject({ id: webGroupId, category: '2' });
});

test('CSPC custom group cancellation does not call create API', async ({ page, host, scenario }) => {
  installStaticsScenario(scenario, host);
  host.manual('createGroup');
  await page.addInitScript(() => localStorage.setItem('ICP-X-Token', 'e2e-web-token'));
  await page.goto('/statics?clientType=CSPC');
  await page.getByText('自定义建群', { exact: true }).first().click();
  const select = await host.waitForCall('selectMembers');
  await host.respond(select, { errorCode: 0, data: [] });
  await page.waitForTimeout(300);
  expect(scenario.state.has('createdGroupRequest')).toBe(false);
  expect(host.calls.some(call => call.method === 'sms')).toBe(false);
});
