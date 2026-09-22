import type { ListResponse } from '#/axios';
import http from '@/utils/http';
const baseUrl = '/linkx/desktop/collaboration/v1';
// ─────────────────────────────────────────────────────────────
// 类型定义
// ─────────────────────────────────────────────────────────────

/** 日历查询参数 */
export interface CalendarParams {
  month: string; // YYYY-MM格式
  startDate?: string; // 开始日期 YYYY-MM-DD
  endDate?: string; // 结束日期 YYYY-MM-DD
  userName?: string; // 人员名称
  userId?: string; // 用户ID（"我的"模式）
  departmentId?: string; // 部门ID（"本组织/多组织"模式）
  dutyType?: string | number; // 排班类型
}

/** 列表查询参数 */
export interface PageParams extends CalendarParams {
  pageNum: number;
  pageSize: number;
}

/** 值班信息项 */
export interface DutyItem {
  id: string;
  userId: string;
  userName: string;
  departmentName: string;
  dutyStartDate: string; // YYYY-MM-DD
  dutyEndDate: string; // YYYY-MM-DD
  dutyStartTime: string; // HH:mm:ss
  dutyEndTime: string; // HH:mm:ss
  dutyContent: string;
  gmtCreated: string;
  postName?: string; // 协同岗名称，有值表示协同岗，无值表示非协同岗
  dutyTypeName?: string; // 排班类型名称，空表示没有类型
}

/** 日历数据返回格式 */
export interface CalendarData {
  [date: string]: DutyItem[]; // key为YYYY-MM-DD
}

/** 分页响应（直接返回，无 code/data 包裹） */
export interface PageResponse<T> extends Omit<ListResponse, 'records'> {
  records: T[];
}

// ─────────────────────────────────────────────────────────────
// API 接口
// ─────────────────────────────────────────────────────────────

/** 日历模式查询 */
export const getScheduleCalendar = (params: CalendarParams) => {
  return http.get<CalendarData>(`${baseUrl}/duty/schedule/calendar`, { params });
};

/** 列表模式分页查询 */
export const getSchedulePage = (params: PageParams) => {
  return http.get<PageResponse<DutyItem>>(`${baseUrl}/duty/schedule/page`, { params });
};

/** 导入值班信息 */
export const uploadDutyInformationFile = (file: File) => {
  const formData = new FormData();
  formData.append('file', file);
  return http.post<{ success: boolean; errorList?: { row: number; msg: string }[] }>(
    `${baseUrl}/duty/schedule/import`,
    formData,
    {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    },
  );
};

/** 模版下载 */
export const exportDutyInformationTemplate = () => {
  return http.get<Blob>(`${baseUrl}/duty/schedule/template`, {
    responseType: 'blob',
  });
};

/** 批量删除 */
export const delBatchSchedule = (ids: string[]) => {
  return http.delete<{ success: boolean }>(`${baseUrl}/duty/schedule/deleteBatch`, {
    data: { ids },
  });
};

/** 获取用户组织列表 */
export const getUserOrganizations = () => {
  return http.get<OrganizationItem[]>(`${baseUrl}/user/organizations`);
};

/** 组织项 */
export interface OrganizationItem {
  id: string;
  name: string;
  code?: string;
}

/** 获取用户组织树 */
export const getUserOrganizationTree = () => {
  return http.get<OrganizationItem[]>(`${baseUrl}/organization/user/tree`);
};

/** 获取值班类型列表（分页） */
export const getDutyTypes = (params: { pageNum: number; pageSize: number; name?: string }) => {
  return http.get<any>('http://172.24.65.22:9084/collaboration/v1/duty/type/page', { params });
  // return http.get<any>(`${baseUrl}/duty/type/page`, { params });
};

/** 获取所有值班类型列表 */
export const getAllDutyTypes = () => {
  return http.get<any>('http://172.24.65.22:9084/collaboration/v1/duty/type/all');
  // return http.get<any>(`${baseUrl}/duty/type/all`);
};