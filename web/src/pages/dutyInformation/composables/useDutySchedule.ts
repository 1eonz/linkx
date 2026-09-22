import { computed, reactive, ref, shallowRef, watch } from 'vue';

import type {
  CalendarData,
  CalendarParams,
  DutyItem,
  // OrganizationItem as ApiOrganizationItem,
  PageParams,
  PageResponse,
} from '@/api/dutySchedule';
import {
  delBatchSchedule,
  getScheduleCalendar,
  getSchedulePage,
  getUserOrganizationTree,
} from '@/api/dutySchedule';

import type { OrganizationItem, OrgFilterMode, OtherSubMode, UserDepartment, UserInfo } from '../types';

import { DUTY_SCHEDULE_CONFIG } from '../constants';

import { usePIMStore } from '@/store/modules/pim';

import { debounce } from 'lodash-es';

// ─────────────────────────────────────────────────────────────
// Hook 实现
// ─────────────────────────────────────────────────────────────

export function useDutySchedule() {
  // 搜索参数
  const searchParams = reactive({
    userName: '',
    startDate: '',
    endDate: '',
    dutyType: '' as string | number,
  });

  // ─────────────────────────────────────────────────────────────
  // 用户信息相关
  // ─────────────────────────────────────────────────────────────

  // 用户信息
  const userInfo = ref<UserInfo | null>(null);

  // 用户主部门（isPrimary: true）
  const primaryDepartment = ref<UserDepartment | null>(null);

  // ─────────────────────────────────────────────────────────────
  // 组织筛选相关
  // ─────────────────────────────────────────────────────────────

  // 多组织列表
  const organizations = ref<OrganizationItem[]>([]);

  // 组织筛选模式
  const orgFilterMode = ref<OrgFilterMode>('self');

  // 选中的组织ID
  const selectedOrgId = ref<string>('');

  // 多组织模式下的子模式（用于区分按钮显示和传参）
  const otherSubMode = ref<OtherSubMode>('other');

  // 是否多组织模式
  const hasMultipleOrgs = computed(() => organizations.value.length > 0);

  // 组织加载状态
  const orgLoading = ref(false);

  // 初始化加载状态
  const initLoading = ref(false);

  // ─────────────────────────────────────────────────────────────
  // 数据相关
  // ─────────────────────────────────────────────────────────────

  // 日历数据
  const calendarData = shallowRef<CalendarData>({});

  // 列表数据
  const listData = ref<DutyItem[]>([]);
  const total = ref(0);

  // 分页参数
  const pagination = reactive({
    pageNum: 1,
    pageSize: 10,
  });

  // Loading 状态
  const calendarLoading = ref(false);
  const listLoading = ref(false);
  const deleteLoading = ref(false);

  // 当前请求的取消函数
  let abortCalendarFetch: (() => void) | null = null;
  let abortListFetch: (() => void) | null = null;

  // ─────────────────────────────────────────────────────────────
  // 数据权限相关
  // ─────────────────────────────────────────────────────────────

  // 数据权限范围（组织树筛选）
  const imOrgPrivsArr = ref<any[]>([]);

  // 用户菜单栏权限信息
  const userRoleAuth = ref<any>({});

  // 用户信息是否就绪
  const userReady = ref(false);

  // ─────────────────────────────────────────────────────────────
  // 用户信息方法
  // ─────────────────────────────────────────────────────────────

  /**
   * 获取用户信息（从 PIMStore）
   * 兼容 userDepartments 数组和 department 对象两种格式
   */
  function fetchUserInfo(): UserInfo | null {
    try {
      const PIMStore = usePIMStore();
      const user = PIMStore.user;

      if (!user || !user.userid) {
        console.warn('[值班信息] PIMStore.user 尚未初始化，当前值:', user);
        return null;
      }

      // 提取部门信息（兼容 userDepartments 数组和 department 对象）
      let depts: UserDepartment[] = [];
      if (Array.isArray(user.userDepartments) && user.userDepartments.length > 0) {
        // 优先使用 userDepartments 数组
        depts = user.userDepartments;
      } else if (user.department) {
        // 兼容 department 对象，转换为数组格式
        depts = [user.department as UserDepartment];
      }

      userInfo.value = {
        userid: user.userid,
        username: user.username,
        userDepartments: depts,
      };

      // 提取主部门（isPrimary: true）
      const primary = depts.find((d: UserDepartment) => d.isPrimary);

      if (primary) {
        primaryDepartment.value = primary;
      }

      console.log('[值班信息] 用户信息获取成功:', {
        userid: user.userid,
        userName: user.username,
        primaryDept: primary?.departmentName,
        primaryDeptId: primary?.departmentId,
        totalDepts: depts.length,
        deptSource: Array.isArray(user.userDepartments) ? 'userDepartments' : 'department',
      });

      return userInfo.value;
    } catch (err: any) {
      console.error('[值班信息] 获取用户信息异常:', err);
      return null;
    }
  }

  /**
   * 获取数据权限信息（从 PIMStore.userRoleAuth）
   */
  function fetchRoleAuth(): void {
    try {
      const PIMStore = usePIMStore();
      const roleAuth = PIMStore.userRoleAuth || {};
      console.log('值班信息=fetchRoleAuth', PIMStore.userRoleAuth);
      userRoleAuth.value = roleAuth;
      imOrgPrivsArr.value = roleAuth.imOrgPrivJson || [];

      console.log('[值班信息] 数据权限获取成功:', {
        hasRoleAuth: !!roleAuth,
        orgPrivCount: imOrgPrivsArr.value.length,
        iccPrivCount: (roleAuth.iccPrivJson || []).length,
      });
    } catch (err: any) {
      console.error('[值班信息] 获取数据权限异常:', err);
    }
  }

  /**
   * 设置 PIMStore.user 的响应式监听
   * 当用户信息变化时自动更新，并设置 userReady 状态
   */
  function setupUserWatch(): void {
    const PIMStore = usePIMStore();

    // 监听用户信息变化
    watch(
      () => PIMStore.user,
      (newUser) => {
        console.log(newUser, 'newUser')
        if (newUser && newUser.userid) {
          console.log('[值班信息] 监听到用户信息变化:', newUser.userid);
          userReady.value = true;
          fetchUserInfo();
        } else {
          userReady.value = false;
          console.log('[值班信息] 用户信息未就绪，等待中...');
        }
      },
      { immediate: true, deep: true }
    );

    // 监听权限信息变化
    watch(
      () => PIMStore.userRoleAuth,
      (newRoleAuth) => {
        if (newRoleAuth) {
          console.log('[值班信息] 监听到权限信息变化');
          fetchRoleAuth();
        }
      },
      { immediate: true, deep: true }
    );
  }

  // ─────────────────────────────────────────────────────────────
  // 组织筛选方法
  // ─────────────────────────────────────────────────────────────

  /**
   * 获取组织列表（支持树状数据）
   */
  async function fetchOrganizations(): Promise<OrganizationItem[]> {
    orgLoading.value = true;

    try {
      const response = await getUserOrganizationTree();
      const { data } = response as any;

      if (!data?.length) {
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

      console.log('[值班信息] 多组织列表获取成功:', {
        rootCount: orgList.length,
        totalNodes: countNodes(orgList),
        defaultId: primaryDeptId,
      });

      return orgList;
    } catch (error) {
      console.error('[值班信息] 获取组织列表失败:', error);
      organizations.value = [];
      return [];
    } finally {
      orgLoading.value = false;
    }
  }

  /**
   * 初始化
   */
  async function init(): Promise<boolean> {
    initLoading.value = true;

    try {
      console.log('[值班信息] 开始初始化...');

      // 步骤1: 获取用户信息
      const user = fetchUserInfo();
      if (!user) {
        console.warn('[值班信息] 用户信息未就绪，等待响应式更新...');
        return false;
      }

      // 步骤2: 获取数据权限
      fetchRoleAuth();
      // 步骤3: 获取多组织列表
      const orgs = await fetchOrganizations();

      // 步骤4: 设置默认模式
      if (orgs.length > 0) {
        // 多组织：默认选中"我的"
        orgFilterMode.value = 'other';
        otherSubMode.value = 'self';
        console.log('[值班信息] 多组织模式，默认选中"我的"');
      } else {
        // 单组织：默认"我的"
        orgFilterMode.value = 'self';
        console.log('[值班信息] 单组织模式，默认"我的"');
      }

      console.log('[值班信息] 初始化完成');
      return true;
    } catch (err: any) {
      console.error('[值班信息] 初始化异常:', err);
      return false;
    } finally {
      initLoading.value = false;
    }
  }

  /**
   * 切换组织筛选
   * @param mode 组织筛选模式
   * @param orgId 组织ID（仅 mode='other' 时有效）
   * @param subMode 多组织模式下的子模式（仅多组织模式有效）
   */
  function changeOrgFilter(mode: OrgFilterMode, orgId?: string, subMode?: OtherSubMode) {
    orgFilterMode.value = mode;
    // 更新 selectedOrgId：有值则设置，无值则清除
    if (orgId) {
      selectedOrgId.value = orgId;
    } else {
      selectedOrgId.value = ''; // 清除选中组织，避免树节点高亮
    }
    if (subMode) {
      otherSubMode.value = subMode;
    }
    console.log('[值班信息] 组织切换:', { mode, orgId, subMode });
  }

  // ─────────────────────────────────────────────────────────────
  // 数据请求方法
  // ─────────────────────────────────────────────────────────────

  /**
   * 构建请求参数（根据组织筛选模式）
   */
  function buildRequestParams<T extends CalendarParams>(baseParams: T): T {
    const params = { ...baseParams };

    // 根据组织筛选模式设置参数
    if (orgFilterMode.value === 'self') {
      // 单组织-我的：传 userId
      params.userId = userInfo.value?.userid;
    } else if (orgFilterMode.value === 'dept') {
      // 单组织-本组织：传用户主部门ID
      params.departmentId = primaryDepartment.value?.departmentId;
    } else if (orgFilterMode.value === 'other') {
      // 多组织模式：根据 otherSubMode 决定传参
      if (otherSubMode.value === 'self') {
        // 多组织-我的：传 userId
        params.userId = userInfo.value?.userid;
      } else if (otherSubMode.value === 'dept') {
        // 多组织-本组织：传用户主部门ID
        params.departmentId = primaryDepartment.value?.departmentId;
      } else {
        // 多组织-其他组织：传选中的部门ID
        params.departmentId = selectedOrgId.value;
      }
    }

    return params;
  }

  /**
   * 获取日历数据
   */
  async function fetchCalendarData(month: string): Promise<CalendarData> {
    // 取消之前的请求
    if (abortCalendarFetch) {
      abortCalendarFetch();
    }

    calendarLoading.value = true;

    const baseParams: CalendarParams = {
      month,
      ...searchParams,
    };

    const params = buildRequestParams(baseParams);

    // 临时需求：固定 dutyType = 0
    if (DUTY_SCHEDULE_CONFIG.FORCE_DUTY_TYPE_ZERO) {
      params.dutyType = DUTY_SCHEDULE_CONFIG.FIXED_DUTY_TYPE_VALUE;
    }

    try {
      const result = getScheduleCalendar(params);
      abortCalendarFetch = result.abortFetch;

      const { code, data } = await result;

      if (code === 0) {
        calendarData.value = data || {};
        return data || {};
      }
      return {};
    } catch (error) {
      console.error('获取日历数据失败:', error);
      return {};
    } finally {
      calendarLoading.value = false;
      abortCalendarFetch = null;
    }
  }

  /**
   * 获取列表数据
   */
  async function fetchListData(): Promise<PageResponse<DutyItem> | null> {
    // 取消之前的请求
    if (abortListFetch) {
      abortListFetch();
    }

    listLoading.value = true;

    const baseParams: PageParams = {
      month: '', // 列表模式不需要 month
      ...searchParams,
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
    };

    const params = buildRequestParams(baseParams);

    // 临时需求：固定 dutyType = 0
    if (DUTY_SCHEDULE_CONFIG.FORCE_DUTY_TYPE_ZERO) {
      params.dutyType = DUTY_SCHEDULE_CONFIG.FIXED_DUTY_TYPE_VALUE;
    }

    try {
      console.log(params, '值班信息-列表参数')
      const result = getSchedulePage(params);
      abortListFetch = result.abortFetch;

      const response = await result;
      console.log(response, '值班信息-列表返回')
      // 该接口直接返回分页数据，无 code/data 包裹
      // AxiosResult 有索引签名，可直接访问属性
      const records = (response as any).records;
      if (records) {
        listData.value = records;
        // 后端返回的 total 是字符串，需要转换为数字
        total.value = Number((response as any).total) || 0;
        return response as unknown as PageResponse<DutyItem>;
      }
      return null;
    } catch (error) {
      console.error('获取列表数据失败:', error);
      return null;
    } finally {
      listLoading.value = false;
      abortListFetch = null;
    }
  }

  /**
   * 防抖搜索
   */
  const debouncedSearch = debounce(fetchListData, 300);

  /**
   * 批量删除
   */
  async function deleteBatch(ids: string[]): Promise<boolean> {
    if (!ids.length) return false;

    deleteLoading.value = true;

    try {
      const { code } = await delBatchSchedule(ids);
      return code === 0;
    } catch (error) {
      console.error('删除失败:', error);
      return false;
    } finally {
      deleteLoading.value = false;
    }
  }

  /**
   * 重置搜索参数
   */
  function resetSearchParams() {
    searchParams.userName = '';
    searchParams.startDate = '';
    searchParams.endDate = '';
    searchParams.dutyType = '';
    pagination.pageNum = 1;
  }

  /**
   * 取消所有请求
   */
  function cancelAllRequests() {
    if (abortCalendarFetch) {
      abortCalendarFetch();
      abortCalendarFetch = null;
    }
    if (abortListFetch) {
      abortListFetch();
      abortListFetch = null;
    }
  }

  return {
    searchParams,
    calendarData,
    listData,
    total,
    pagination,
    calendarLoading,
    listLoading,
    deleteLoading,
    fetchCalendarData,
    fetchListData,
    debouncedSearch,
    deleteBatch,
    resetSearchParams,
    cancelAllRequests,
    // 用户信息
    userInfo,
    primaryDepartment,
    userReady,
    // 组织筛选
    organizations,
    orgFilterMode,
    selectedOrgId,
    otherSubMode,
    hasMultipleOrgs,
    orgLoading,
    initLoading,
    // 数据权限
    imOrgPrivsArr,
    userRoleAuth,
    // 方法
    init,
    changeOrgFilter,
    setupUserWatch,
  };
}