import { http } from '@/common/network/http.js';

const baseUrl = '/admin/auth/v1';
// const baseUrl = 'http://172.24.65.16:8081/';

/**
 * 获取组织列表（一键调度）
 * @param {Object} params - { imUserId: string }
 * @returns {Promise}
 */
export function getCustomDepartmentTree(params) {
  return http.get(baseUrl + '/custom-department/tree', params);
}

/**
 * 获取一级及子部门（一键调度）
 * @param {Object} params - { dutyType: number, dutyStartDate: string, dutyEndDate: string, departmentCustomId: string|number, imUserId: string }
 * @returns {Promise}
 */
export function getCustomDepartmentNode(params) {
  return http.get(baseUrl + '/custom-department/node', params);
}

/**
 * 获取人员信息（一键调度）
 * @param {number|string} id - 部门ID
 * @param {Object} params - { dutyType: number, dutyStartDate: string, dutyEndDate: string, imUserId: string }
 * @returns {Promise}
 */
export function getDutyScheduleUser(id, params) {
  return http.get(baseUrl + `/custom-department/node/${id}/duty-schedule-user`, params);
}
