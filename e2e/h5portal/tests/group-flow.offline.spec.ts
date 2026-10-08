import { test, expect } from '../../shared/fixtures/test.js';
import { installGroupScenario, label, groupId, testUser } from '../fixtures/group-scenario.js';

test('H5 label group: matching host notification opens chat and closes page', async ({ page, host, scenario }) => {
  installGroupScenario(scenario, host);
  await page.goto('pages/createGroup');
  await expect(page.getByText(label.name, { exact: true })).toBeVisible();
  const confirm = page.getByRole('button', { name: '确定', exact: true });
  await expect(confirm).toBeDisabled();
  await page.getByText(label.name, { exact: true }).click();
  await expect(confirm).toBeEnabled();
  await confirm.click();
  await expect.poll(() => scenario.state.get('createdGroupId')).toBe(groupId);
  expect(scenario.state.get('createdGroupRequest')).toMatchObject({ ids: [label.id], ownerId: testUser.userid, departmentId: 'e2e-dept', idCard: testUser.idCard, location: '116.4,39.9' });
  await expect(page.getByText('创建中...', { exact: true }).first()).toBeVisible();
  expect(host.calls.filter(call => call.method === 'sms')).toHaveLength(0);
  await host.emit(page.mainFrame(), 'onCooperationGroupCreate', { groupId: 'another-group' });
  await expect(page.getByText('创建中...', { exact: true }).first()).toBeVisible();
  expect(host.calls.filter(call => call.method === 'sms')).toHaveLength(0);
  await host.emit(page.mainFrame(), 'onCooperationGroupCreate', { groupId });
  const chat = await host.waitForCall('sms');
  expect(chat.data).toMatchObject({ id: groupId, category: 2 });
  expect((await host.waitForCall('switchTab')).data).toMatchObject({ appId: 'ITEM_SESSION_PAGE' });
  await host.waitForCall('close');
  await expect(page.locator('.loading-mask')).toHaveCount(0);
});

test('H5 label group: cancelling selection disables submit without creating', async ({ page, host, scenario }) => {
  installGroupScenario(scenario, host);
  await page.goto('pages/createGroup');
  await page.getByText(label.name, { exact: true }).click();
  await expect(page.getByRole('button', { name: '确定', exact: true })).toBeEnabled();
  await page.getByRole('button', { name: '取消选中', exact: true }).click();
  await expect(page.getByRole('button', { name: '确定', exact: true })).toBeDisabled();
  expect(scenario.state.has('createdGroupRequest')).toBe(false);
  expect(host.calls.some(call => call.method === 'sms')).toBe(false);
});

test('H5 label group: API error restores submit and keeps page open', async ({ page, host, scenario }) => {
  installGroupScenario(scenario, host, { rejectCreate: true });
  await page.goto('pages/createGroup');
  await page.getByText(label.name, { exact: true }).click();
  await page.getByRole('button', { name: '确定', exact: true }).click();
  await expect(page.getByText('E2E creation rejected', { exact: true })).toBeVisible();
  await expect(page.getByRole('button', { name: '确定', exact: true })).toBeEnabled();
  expect(host.calls.some(call => ['sms', 'close', 'switchTab'].includes(call.method))).toBe(false);
});

test('H5 label group: personnel-check label is hidden without license', async ({ page, host, scenario }) => {
  installGroupScenario(scenario, host, { license: false });
  await page.goto('pages/createGroup');
  await expect(page.getByText(label.name, { exact: true })).toBeVisible();
  await expect(page.getByText('人员核查', { exact: true })).toHaveCount(0);
  expect(scenario.state.has('createdGroupRequest')).toBe(false);
});
