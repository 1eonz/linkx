import { http } from '@/common/network/http.js';

const adminUrl = '/admin/';
const baseUrl = '/collaboration/v1';

// ─────────────────────────────────────────────────────────────
// 值班信息相关接口
// ─────────────────────────────────────────────────────────────

/**
 * 获取排班日历数据
 * @param {Object} params - 查询参数
 * @param {string} params.month - 月份，格式 "2026-03"
 * @param {string} [params.userId] - 用户ID（"我的"模式）
 * @param {string} [params.departmentId] - 部门ID（"本组织/多组织"模式）
 * @param {string} [params.userName] - 搜索用户名
 * @returns {Promise<Object>} - 返回日期为键的排班数据 { "2026-03-26": [...], ... }
 */
export const getDutyScheduleCalendar = (params) =>
  http.get(`${baseUrl}/duty/schedule/calendar`, params);

/**
 * 获取用户多组织列表
 * @param {Object} [params] - 查询参数
 * @returns {Promise<Array>} - 返回组织列表
 */
export const getUserOrganizations = (params) =>
  http.get(`${baseUrl}/user/organizations`, params);

/**
 * 获取用户组织树9
 * @returns {Promise<Array>} - 返回组织树
 */
export const getUserOrganizationTree = () =>
  http.get(`${baseUrl}/organization/user/tree`);