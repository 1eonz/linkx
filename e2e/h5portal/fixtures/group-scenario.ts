import type { Scenario } from '../../shared/scenarios/scenario.js';
import type { OfflineHost } from '../../shared/hosts/offline-host.js';

export const testUser = {
  userid: '900001', userId: '900001', username: 'E2E Operator',
  aastoken: 'offline-host-token', idCard: 'E2E-ID-900001',
  userDepartments: [{ isPrimary: true, departmentId: 'e2e-dept', departmentCode: 'E2E', departmentName: 'E2E Department', fullPath: 'E2E' }],
  department: { departmentId: 'e2e-dept', departmentCode: 'E2E', departmentName: 'E2E Department', fullPath: 'E2E' },
};
export const label = { id: 'e2e-label', name: 'E2E Collaboration', type: 0, color: '#3268c8', icon: 'fa fa-users' };
export const groupId = '900099';
export const success = (data: unknown) => ({ code: 0, msg: 'success', data });

export function installGroupScenario(scenario: Scenario, host: OfflineHost, options: { license?: boolean; rejectCreate?: boolean } = {}) {
  host.on('getUserInfo', () => ({ errorCode: 0, data: testUser }));
  host.on('getVersion', () => ({ errorCode: 0, data: { versionCode: 900, versionName: 'e2e', deviceType: 'Android' } }));
  host.on('getStatusBarHeight', () => ({ errorCode: 0, data: 0 }));
  host.on('getGisInfo', () => ({ errorCode: 0, data: { longitude: 116.4, latitude: 39.9 } }));
  for (const method of ['sms', 'switchTab', 'close']) host.on(method, () => ({ errorCode: 0, data: true }));
  const path = (suffix: string) => new RegExp(`${suffix.replaceAll('/', '\\/')}$`);
  scenario.json('GET', path('/admin/v1/msip/license/info'), success({ LINKXBS: '1', LINKXGCF: '1', LINKXACF: options.license === false ? '0' : '1' }));
  scenario.json('GET', path('/admin/v1/role/h5permissions'), success([{ url: 'copilotStatistics' }]));
  scenario.json('POST', path('/base/v1/globals/getGlobalsList'), success({ MULTIPLE_COLLABORATION: 'false' }));
  scenario.json('GET', path('/collaboration/v1/label/list'), success([label, { ...label, id: 'restricted-label', name: '人员核查' }]));
  scenario.route('POST', path('/collaboration/v1/label/createGroup'), async (route, state) => {
    const request = route.request().postDataJSON();
    state.set('createdGroupRequest', request);
    if (options.rejectCreate) await route.fulfill({ json: { code: 500, msg: 'E2E creation rejected', data: null } });
    else { state.set('createdGroupId', groupId); await route.fulfill({ json: success(groupId) }); }
  });
}
