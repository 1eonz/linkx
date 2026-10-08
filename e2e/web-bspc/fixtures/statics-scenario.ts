import type { Scenario } from '../../shared/scenarios/scenario.js';
import type { OfflineHost } from '../../shared/hosts/offline-host.js';

export const webUser = {
  userid: '900001', id: '900001', username: 'E2E Operator', aastoken: 'bspc-token',
  idCard: 'E2E-ID-900001', cooperationUser: { userId: '900001', userName: 'E2E Post' },
  cooperationUsers: [{ userId: '900001', userName: 'E2E Post' }],
  department: { departmentId: 'e2e-dept', departmentCode: 'E2E', departmentName: 'E2E Department', fullPath: 'E2E' },
};
export const webLabel = { id: 'e2e-label', name: 'E2E Collaboration', type: 0, color: '#3268c8', icon: 'fa fa-users' };
export const webGroupId = '900099';
const ok = (data: unknown) => ({ code: 0, msg: 'success', data });
const path = (suffix: string) => new RegExp(`${suffix.replaceAll('/', '\\/')}$`);

export function installStaticsScenario(scenario: Scenario, host: OfflineHost, options: { rejectCreate?: boolean } = {}) {
  host.on('getUserInfo', () => ({ errorCode: 0, data: webUser }));
  host.on('getVersion', () => ({ errorCode: 0, data: { versionCode: 900, versionName: 'e2e', deviceType: 'Windows' } }));
  host.on('getCurrentTheme', () => ({ errorCode: 0, data: { theme: 'light' } }));
  host.on('getTheme', () => ({ errorCode: 0, data: { theme: 'light' } }));
  host.on('setBadge', () => ({ errorCode: 0, data: true }));
  host.on('getUserInfoByUserId', () => ({ errorCode: 0, data: webUser }));
  host.on('openSelectMemberUI', () => ({ errorCode: 0, data: { data: { contactList: [{ id: '900002', name: 'E2E Member' }] } } }));
  host.on('createGroup', () => ({ errorCode: 0, data: { groupId: webGroupId } }));
  for (const method of ['sms', 'switchTab', 'close', 'openChat', 'selectMembers']) host.manual(method);
  scenario.json('POST', path('/auth/v1/oauth/v2/dems/tokenLogin'), ok({ accessToken: 'e2e-web-token', refreshToken: 'e2e-refresh', userId: webUser.userid, id: webUser.userid }));
  scenario.json('POST', path('/linkx/desktop/auth/v1/oauth/v2/tokenLogin'), ok({ accessToken: 'e2e-web-token', refreshToken: 'e2e-refresh', userId: webUser.userid, id: webUser.userid }));
  scenario.json('POST', path('/linkx/desktop/auth/v1/oauth/v2/permissions'), ok({ type: 1, menus: ['e2e-statics'] }));
  scenario.json('GET', path('/linkx/desktop/auth/v1/role/byUserId'), ok([{ id: 'e2e-role', name: 'E2E Role' }]));
  scenario.json('POST', path('/linkx/desktop/auth/v1/oauth/v2/keepalive'), ok(true));
  scenario.json('POST', path('/linkx/desktop/admin/v1/menu/list'), ok([{ id: 'e2e-statics', url: 'statics', name: 'statics', children: [] }]));
  scenario.json('POST', path('/linkx/desktop/base/v1/globals/getGlobalsList'), ok({ STATION_NAME: 'E2E Station', ONE_KEY_CREATE_GROUP_SIGN: 'true', MULTIPLE_COLLABORATION: 'false', eICSFeBC: '1', eICSCDF: '1', eICSCF: '1', eICSANF: '1', eICSTDF: '1', eICSLEMF: '1', eICSSSF: '1', eICSVCF: '1' }));
  scenario.json('GET', /\/linkx\/desktop\/admin\/v1\/{1,2}msip\/license\/info$/, ok({ status: '1', LINKXBS: '1', LINKXGCF: '1', LINKXBCF: '1' }));
  scenario.json('GET', path('/linkx/desktop/admin/v1/system/config'), ok([{ key: 'CREAT_GROUP_CONFIG', value: JSON.stringify([{ type: '1', name: '自定义建群', enable: 'true' }, { type: '2', name: '一键建群', enable: 'true' }, { type: '3', name: '职能建群', enable: 'true' }]) }]));
  scenario.json('GET', path('/linkx/desktop/admin/v1/content/app/info/apps'), ok({ records: [], total: 0 }));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/attendance/getSwitchStatus'), ok({ bondedStatus: false, switchStatus: false }));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/post/queryUserByIdCard'), ok({ userDepartments: [{ isPrimary: true, departmentCode: 'E2E', departmentName: 'E2E Department' }] }));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/post/queryByUserId'), ok([{ userId: webUser.userid, name: 'E2E Post', groupIds: [] }]));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/label/list'), ok([webLabel]));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/base/version'), ok({ linkx: { serviceVersion: 'e2e' } }));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/attendance/heartbeat/900001'), ok({}));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/attendance/getLastNum'), ok(0));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/unattended/page'), ok({ records: [], total: 0 }));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/tasks/expired/page'), ok({ records: [], total: 0 }));
  scenario.json('GET', path('/linkx/desktop/third/v1/app/callable'), ok({ records: [], total: 0 }));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/tasks/config/modules'), ok({ records: [], total: 0 }));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/tasks/statistics'), ok({}));
  scenario.json('GET', path('/linkx/desktop/collaboration/v1/post/queryDepartment'), ok([]));
  scenario.json('GET', path('/linkx/desktop/dashboard/v1/group/rating/tag'), ok([]));
  scenario.json('GET', path('/linkx/desktop/dashboard/v1/group/rating/coopUser'), ok([]));
  scenario.websocket('/linkx/desktop/cagent', socket => socket.close({ code: 1000, reason: 'offline scenario' }));
  scenario.route('POST', path('/linkx/desktop/collaboration/v1/label/createGroup'), async (route, state) => {
    const request = route.request().postDataJSON();
    state.set('createdGroupRequest', request);
    await route.fulfill({ json: options.rejectCreate ? { code: 500, msg: 'E2E creation rejected', data: null } : ok(webGroupId) });
  });
}
