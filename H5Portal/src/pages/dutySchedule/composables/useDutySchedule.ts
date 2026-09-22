import { ref, computed } from 'vue';
import { useCommunicationStore } from '@/stores/communication';
import { getDutyScheduleCalendar, getUserOrganizationTree } from '@/common/api/dutySchedule.js';
import { DUTY_SCHEDULE_CONFIG } from '../constants';
import type {
  DutyScheduleItem,
  DutyScheduleParams,
  DutyScheduleResponse,
  UserInfo,
  UserDepartment,
  OrganizationItem,
  OrgFilterMode,
  OtherSubMode,
} from '../types';

// ─────────────────────────────────────────────────────────────
// 工具函数
// ─────────────────────────────────────────────────────────────

/** 服务器时间缓存 */
let cachedServerTime: Date | null = null;
let serverTimeFetchPromise: Promise<Date | null> | null = null;

/**
 * 从服务器获取时间（通过请求响应头）
 * 使用 HEAD 请求，轻量级，只获取响应头
 */
async function fetchServerTime(): Promise<Date | null> {
  // 如果已经有缓存且在5分钟内，直接返回
  if (cachedServerTime) {
    const cacheAge = Date.now() - cachedServerTime.getTime();
    if (cacheAge < 5 * 60 * 1000) {
      return cachedServerTime;
    }
  }

  // 避免并发请求
  if (serverTimeFetchPromise) {
    return serverTimeFetchPromise;
  }

  serverTimeFetchPromise = (async () => {
    try {
      // 使用 HEAD 请求获取服务器时间，轻量级
      const baseUrl = window.location.origin;
      const response = await fetch(`${baseUrl}/favicon.ico`, {
        method: 'HEAD',
        cache: 'no-store',
      });

      const serverDateStr = response.headers.get('Date');
      if (serverDateStr) {
        const serverDate = new Date(serverDateStr);
        if (!isNaN(serverDate.getTime())) {
          cachedServerTime = serverDate;
          console.log('[排班] 服务器时间获取成功:', serverDate.toLocaleString());
          return serverDate;
        }
      }
    } catch (err) {
      console.warn('[排班] 获取服务器时间失败:', err);
    }
    return null;
  })();

  const result = await serverTimeFetchPromise;
  serverTimeFetchPromise = null;
  return result;
}

/**
 * 获取安全的当前日期（处理设备时间异常）
 * 策略：
 * 1. 检测本地时间是否异常（早于2026年）
 * 2. 如果异常，尝试从服务器获取时间
 * 3. 如果服务器时间获取失败，使用兜底时间
 */
function getSafeCurrentDate(): Date {
  const now = new Date();
  const minValidYear = 2026;

  if (now.getFullYear() < minValidYear) {
    console.warn('[排班] 检测到设备时间异常:', now.toLocaleString());
    // 返回兜底时间，后续会通过 init() 中的 fetchServerTime 校准
    return new Date(minValidYear, 0, 1);
  }
  return now;
}

/**
 * 校准日期（异步获取服务器时间并校准）
 * @returns 返回校准后的日期，如果服务器时间获取失败则返回本地时间
 */
async function calibrateDate(): Promise<Date> {
  const now = new Date();
  const minValidYear = 2026;

  if (now.getFullYear() >= minValidYear) {
    return now;
  }

  // 尝试从服务器获取时间
  const serverTime = await fetchServerTime();
  if (serverTime) {
    return serverTime;
  }

  // 服务器时间获取失败，使用兜底时间
  console.warn('[排班] 使用兜底时间:', minValidYear, '-01-01');
  return new Date(minValidYear, 0, 1);
}

/** 格式化月份为 "2026-03" 格式 */
export function formatMonth(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  return `${year}-${month}`;
}

/** 格式化日期为 "2026-03-26" 格式 */
export function formatDateStr(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

// ─────────────────────────────────────────────────────────────
// Hook 实现
// ─────────────────────────────────────────────────────────────

export function useDutySchedule() {
  // ─────────────────────────────────────────────────────────────
  // 状态定义
  // ─────────────────────────────────────────────────────────────

  // 当前选中的日期
  const selectedDate = ref<Date>(getSafeCurrentDate());

  // 当前显示的月份
  const currentMonth = ref<Date>(getSafeCurrentDate());

  // 搜索的用户名
  const searchUserName = ref<string>('');

  // ─────────────────────────────────────────────────────────────
  // 用户信息相关
  // ─────────────────────────────────────────────────────────────

  // 用户信息
  const userInfo = ref<UserInfo | null>(null);

  // 用户主部门（isPrimary: true）
  const primaryDepartment = ref<UserDepartment | null>(null);

  // ─────────────────────────────────────────────────────────────
  // 多组织相关
  // ─────────────────────────────────────────────────────────────

  // 多组织列表
  const organizations = ref<OrganizationItem[]>([]);

  // 组织筛选模式
  const orgFilterMode = ref<OrgFilterMode>('self');

  // 选中的组织ID
  const selectedOrgId = ref<string>('');

  // 多组织模式下的子模式（用于区分按钮显示和传参）
  const otherSubMode = ref<OtherSubMode>('other');

  // ─────────────────────────────────────────────────────────────
  // 加载状态
  // ─────────────────────────────────────────────────────────────

  // 数据加载状态
  const loading = ref<boolean>(false);

  // 初始化加载状态
  const initLoading = ref<boolean>(false);

  // 多组织加载状态
  const orgLoading = ref<boolean>(false);

  // 错误信息
  const error = ref<string>('');

  // ─────────────────────────────────────────────────────────────
  // 计算属性
  // ─────────────────────────────────────────────────────────────

  // 是否支持多组织（>1个部门）
  const hasMultipleOrgs = computed(() => organizations.value.length > 0);

  // 排班数据缓存 Map<月份字符串, Map<日期字符串, DutyScheduleItem[]>>
  const scheduleCache = ref<Map<string, Map<string, DutyScheduleItem[]>>>(new Map());

  // 当前月份的排班数据
  const currentScheduleData = computed(() => {
    const monthKey = formatMonth(currentMonth.value);
    return scheduleCache.value.get(monthKey) || new Map();
  });

  // 选中日期的排班列表
  const selectedDateSchedule = computed<DutyScheduleItem[]>(() => {
    const dateKey = formatDateStr(selectedDate.value);
    return currentScheduleData.value.get(dateKey) || [];
  });

  // ─────────────────────────────────────────────────────────────
  // 核心方法
  // ─────────────────────────────────────────────────────────────

  /**
   * 步骤1: 获取用户信息
   */
  async function fetchUserInfo(): Promise<UserInfo | null> {
    try {
      const communicationStore = useCommunicationStore();

      // 使用 ensureUserInfoWithDept 确保获取完整信息
      const user = await communicationStore.ensureUserInfoWithDept(2, 400);

      if (!user || !user.userid) {
        console.error('[排班] 获取用户信息失败');
        return null;
      }

      userInfo.value = user;

      // 提取主部门（isPrimary: true）
      const depts = Array.isArray(user.userDepartments) ? user.userDepartments : [];
      const primary = depts.find((d) => d.isPrimary);

      if (primary) {
        primaryDepartment.value = primary;
      }

      console.log('[排班] 用户信息获取成功:', {
        userId: user.userid,
        userName: user.username,
        primaryDept: primary?.departmentName,
        primaryDeptId: primary?.departmentId,
        totalDepts: depts.length,
      });

      return user;
    } catch (err: any) {
      console.error('[排班] 获取用户信息异常:', err);
      return null;
    }
  }

  /**
   * 步骤2: 获取多组织列表（支持树状数据）
   */
  async function fetchOrganizations(): Promise<OrganizationItem[]> {
    orgLoading.value = true;

    try {
      const response = await getUserOrganizationTree();
      console.log('[排班] 组织树接口返回:', response);

      // 接口直接返回数组，不是 { code, data } 格式
      const data = Array.isArray(response) ? response : [];
      if (data.length === 0) {
        organizations.value = [];
        return [];
      }

      // 根据用户的 userDepartments 标记默认部门
      const primaryDeptId = primaryDepartment.value?.departmentId;

      // 递归标记默认部门
      function markDefaultOrg(orgs: any[]): OrganizationItem[] {
        return orgs.map((org: any) => ({
          id: org.id,
          name: org.name,
          code: org.code,
          isDefault: org.id === primaryDeptId,
          hasPermission: org.hasPermission,
          parentId: org.parentId,
          children: org.children?.length ? markDefaultOrg(org.children) : undefined,
          fullPath: org.fullPath,
          fullPathName: org.fullPathName,
          sort: org.sort,
        }));
      }

      const orgList = markDefaultOrg(data);
      organizations.value = orgList;

      // 统计总节点数
      function countNodes(tree: OrganizationItem[]): number {
        let count = 0;
        for (const node of tree) {
          count++;
          if (node.children?.length) {
            count += countNodes(node.children);
          }
        }
        return count;
      }

      console.log('[排班] 多组织列表获取成功:', {
        rootCount: orgList.length,
        totalNodes: countNodes(orgList),
        defaultId: primaryDeptId,
      });

      return orgList;
    } catch (err: any) {
      console.error('[排班] 获取多组织失败:', err);
      organizations.value = [];
      return [];
    } finally {
      orgLoading.value = false;
    }
  }

  /**
   * 步骤3: 获取排班数据（根据模式传参）
   */
  async function fetchSchedule(
    month: Date,
    mode?: OrgFilterMode,
    orgId?: string,
    userName?: string,
    subMode?: OtherSubMode,
  ): Promise<Map<string, DutyScheduleItem[]> | null> {
    // 使用传入的模式或当前模式
    const currentMode = mode || orgFilterMode.value;
    const currentOrgId = orgId || selectedOrgId.value;
    const currentSubMode = subMode || otherSubMode.value;

    const monthStr = formatMonth(month);
    loading.value = true;
    error.value = '';

    try {
      const params: DutyScheduleParams = {
        month: monthStr,
      };

      // 根据模式设置参数
      if (currentMode === 'self') {
        // 单组织-我的：传 userId
        params.userId = userInfo.value?.userid;
      } else if (currentMode === 'dept') {
        // 单组织-本组织：传用户主部门ID
        params.departmentId = primaryDepartment.value?.departmentId;
      } else if (currentMode === 'other') {
        // 多组织模式：根据 otherSubMode 决定传参
        if (currentSubMode === 'self') {
          // 多组织-我的：传 userId
          params.userId = userInfo.value?.userid;
        } else if (currentSubMode === 'dept') {
          // 多组织-本组织：传用户主部门ID
          params.departmentId = primaryDepartment.value?.departmentId;
        } else {
          // 多组织-其他组织：传选中的部门ID
          params.departmentId = currentOrgId;
        }
      }

      // 搜索用户名
      if (userName) {
        params.userName = userName;
      }

      // 临时需求：固定 dutyType = 0
      if (DUTY_SCHEDULE_CONFIG.FORCE_DUTY_TYPE_ZERO) {
        params.dutyType = DUTY_SCHEDULE_CONFIG.FIXED_DUTY_TYPE_VALUE;
      }

      console.log('[排班] 请求参数:', { mode: currentMode, subMode: currentSubMode, params });

      const response = await getDutyScheduleCalendar(params);

      // 解析响应数据
      const data = response || {};
      const monthData = new Map<string, DutyScheduleItem[]>();

      // 遍历日期键
      Object.keys(data).forEach((dateKey) => {
        const items = data[dateKey];
        if (Array.isArray(items)) {
          monthData.set(dateKey, items);
        }
      });

      // 更新缓存
      scheduleCache.value.set(monthStr, monthData);

      console.log('[排班] 数据获取成功:', {
        month: monthStr,
        datesCount: monthData.size,
        totalItems: Array.from(monthData.values()).reduce((sum, arr) => sum + arr.length, 0),
      });

      return monthData;
    } catch (err: any) {
      console.error('[排班] 获取数据失败:', err);
      error.value = err?.message || '获取排班数据失败';
      return null;
    } finally {
      loading.value = false;
    }
  }

  /**
   * 初始化（链式调用）
   */
  async function init(): Promise<boolean> {
    initLoading.value = true;

    try {
      console.log('[排班] 开始初始化...');

      // 步骤0: 校准日期（处理华为手机时间异常）
      const calibratedDate = await calibrateDate();
      if (calibratedDate.getFullYear() !== new Date().getFullYear()) {
        console.log('[排班] 日期已校准:', calibratedDate.toLocaleString());
        selectedDate.value = calibratedDate;
        currentMonth.value = new Date(calibratedDate.getFullYear(), calibratedDate.getMonth(), 1);
      }

      // 步骤1: 获取用户信息
      const user = await fetchUserInfo();
      if (!user) {
        console.error('[排班] 初始化失败：无法获取用户信息');
        return false;
      }

      // 步骤2: 获取多组织列表（依赖用户信息判断默认部门）
      const orgs = await fetchOrganizations();

      // 步骤3: 设置默认模式
      if (orgs.length > 0) {
        // 多组织：默认选中"我的"
        orgFilterMode.value = 'other';
        otherSubMode.value = 'self';
        console.log('[排班] 多组织模式，默认选中"我的"');
      } else {
        // 单组织：默认"我的"
        orgFilterMode.value = 'self';
        console.log('[排班] 单组织模式，默认"我的"');
      }

      // 步骤4: 加载排班数据
      await fetchSchedule(currentMonth.value);

      console.log('[排班] 初始化完成');
      return true;
    } catch (err: any) {
      console.error('[排班] 初始化异常:', err);
      return false;
    } finally {
      initLoading.value = false;
    }
  }

  /**
   * 切换组织模式
   */
  async function changeOrgFilter(mode: OrgFilterMode, orgId?: string, subMode?: OtherSubMode): Promise<void> {
    const oldMode = orgFilterMode.value;
    const oldOrgId = selectedOrgId.value;

    orgFilterMode.value = mode;
    if (orgId) {
      selectedOrgId.value = orgId;
    }
    if (subMode) {
      otherSubMode.value = subMode;
    }

    console.log('[排班] 组织切换:', { from: oldMode, to: mode, orgId, subMode });

    // 清除当前月份缓存
    const monthStr = formatMonth(currentMonth.value);
    scheduleCache.value.delete(monthStr);

    // 重新加载数据
    await fetchSchedule(currentMonth.value, mode, orgId, searchUserName.value, subMode);
  }

  /**
   * 刷新当前月份数据（强制请求，不使用缓存）
   */
  async function refreshCurrentMonth(): Promise<void> {
    const monthStr = formatMonth(currentMonth.value);
    // 清除缓存
    scheduleCache.value.delete(monthStr);
    await fetchSchedule(currentMonth.value, undefined, undefined, searchUserName.value);
  }

  /**
   * 切换月份
   */
  async function changeMonth(delta: number, preserveSelectedDate?: Date): Promise<void> {
    // 关键修复：只使用年月，日期设为1号，避免日期溢出问题
    const year = currentMonth.value.getFullYear();
    const month = currentMonth.value.getMonth() + delta;
    const newMonth = new Date(year, month, 1);
    currentMonth.value = newMonth;

    // 请求最新数据
    await fetchSchedule(newMonth, undefined, undefined, searchUserName.value);

    // 如果指定了要保留的选中日期，则使用它
    if (preserveSelectedDate) {
      selectedDate.value = preserveSelectedDate;
      return;
    }

    // 如果切换到当前月，选中今天；否则选中1号
    const today = new Date();
    if (newMonth.getFullYear() === today.getFullYear() && newMonth.getMonth() === today.getMonth()) {
      selectedDate.value = today;
    } else {
      selectedDate.value = new Date(newMonth.getFullYear(), newMonth.getMonth(), 1);
    }
  }

  /**
   * 跳转到指定月份
   */
  async function goToMonth(year: number, month: number, preserveSelectedDate?: Date): Promise<void> {
    const newMonth = new Date(year, month - 1, 1);
    currentMonth.value = newMonth;
    await fetchSchedule(newMonth, undefined, undefined, searchUserName.value);

    // 如果指定了要保留的选中日期，则使用它
    if (preserveSelectedDate) {
      selectedDate.value = preserveSelectedDate;
      return;
    }

    // 如果是当前月，选中今天；否则选中1号
    const today = new Date();
    if (year === today.getFullYear() && month === today.getMonth() + 1) {
      selectedDate.value = today;
    } else {
      selectedDate.value = new Date(year, month - 1, 1);
    }
  }

  /**
   * 选择日期
   */
  function selectDate(date: Date): void {
    console.log('[排班] 选择日期:', formatDateStr(date));
    selectedDate.value = date;
  }

  /**
   * 搜索用户（防抖在外部处理）
   */
  async function searchByUserName(userName: string): Promise<void> {
    searchUserName.value = userName;
    // 清除当前月份缓存
    const monthStr = formatMonth(currentMonth.value);
    scheduleCache.value.delete(monthStr);
    // 重新请求
    await fetchSchedule(currentMonth.value, undefined, undefined, userName);
  }

  /**
   * 清除搜索
   */
  async function clearSearch(): Promise<void> {
    searchUserName.value = '';
    const monthStr = formatMonth(currentMonth.value);
    scheduleCache.value.delete(monthStr);
    await fetchSchedule(currentMonth.value);
  }

  /**
   * 检查某日期是否有排班
   */
  function hasScheduleOnDate(date: Date): boolean {
    const dateKey = formatDateStr(date);
    const items = currentScheduleData.value.get(dateKey);
    return items ? items.length > 0 : false;
  }

  return {
    // ─────────────────────────────────────────────────────────────
    // 状态
    // ─────────────────────────────────────────────────────────────
    selectedDate,
    currentMonth,
    searchUserName,
    userInfo,
    primaryDepartment,
    organizations,
    orgFilterMode,
    selectedOrgId,
    otherSubMode,
    scheduleCache,
    currentScheduleData,
    selectedDateSchedule,
    loading,
    initLoading,
    orgLoading,
    error,
    hasMultipleOrgs,

    // ─────────────────────────────────────────────────────────────
    // 方法
    // ─────────────────────────────────────────────────────────────
    fetchUserInfo,
    fetchOrganizations,
    fetchSchedule,
    init,
    changeOrgFilter,
    refreshCurrentMonth,
    changeMonth,
    goToMonth,
    selectDate,
    searchByUserName,
    clearSearch,
    hasScheduleOnDate,

    // ─────────────────────────────────────────────────────────────
    // 工具函数
    // ─────────────────────────────────────────────────────────────
    formatMonth,
    formatDateStr,
  };
}
