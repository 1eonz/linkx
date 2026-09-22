// 排班项
export interface DutyScheduleItem {
  id: string;
  userId: string;
  userName: string;
  departmentId: string;
  departmentName: string;
  dutyDate: string; // "2026-03-26"
  dutyStartTime: string; // "11:10:00"
  dutyEndTime: string; // "11:15:00"
  dutyContent: string;
  importBatch: string;
  gmtCreated: string;
  gmtModified: string;
  postName?: string; // 协同岗名称，有值表示协同岗，无值表示非协同岗
  dutyType?: string | number; // 排班类型
  dutyTypeName: string;
}

// API 响应
export interface DutyScheduleResponse {
  code: number;
  msg: string;
  data: Record<string, DutyScheduleItem[]>;
}

// 请求参数
export interface DutyScheduleParams {
  userId?: string;        // "我的"模式
  departmentId?: string;  // "本组织/多组织"模式
  userName?: string;
  month: string; // "2026-03"
  dutyType?: string | number; // 排班类型
}

// 日期单元格数据
export interface DateCell {
  date: Date;
  day: number;
  month: number;
  year: number;
  isCurrentMonth: boolean;
  isToday: boolean;
  isSelected: boolean;
  hasSchedule: boolean;
  dateStr: string; // "2026-03-26"
}

// ─────────────────────────────────────────────────────────────
// 用户信息相关（从 communicationStore 获取）
// ─────────────────────────────────────────────────────────────

/** 用户部门信息 */
export interface UserDepartment {
  departmentId: string;
  departmentCode: string;
  departmentName: string;
  isPrimary: boolean;      // 是否主部门（用户当前所在部门）
  fullPath?: string;
}

/** 用户信息 */
export interface UserInfo {
  userid: string;
  username: string;
  userDepartments: UserDepartment[];
}

// ─────────────────────────────────────────────────────────────
// 多组织相关
// ─────────────────────────────────────────────────────────────

/** 组织项（支持树状结构） */
export interface OrganizationItem {
  id: string;              // departmentId
  name: string;            // departmentName
  code?: string;           // departmentCode
  isDefault?: boolean;     // 是否用户默认部门（前端计算）
  hasPermission?: boolean; // 是否有权限（true=可选中，false/不存在=禁用选中但可展开）
  // 树状结构字段
  parentId?: string;
  children?: OrganizationItem[];
  fullPath?: string;
  fullPathName?: string;
  sort?: number;
}

/** 组织筛选模式 */
export type OrgFilterMode = 'self' | 'dept' | 'other';

/** 多组织模式下的子模式（用于区分按钮显示和传参） */
export type OtherSubMode = 'self' | 'dept' | 'other';

/** 组织筛选状态 */
export interface OrgFilterState {
  mode: OrgFilterMode;
  orgId?: string;
  orgName?: string;
}

// ─────────────────────────────────────────────────────────────
// 工具函数
// ─────────────────────────────────────────────────────────────

/**
 * 递归查找树节点
 */
export function findNodeById(tree: OrganizationItem[], id: string): OrganizationItem | null {
  for (const node of tree) {
    if (node.id === id) return node;
    if (node.children?.length) {
      const found = findNodeById(node.children, id);
      if (found) return found;
    }
  }
  return null;
}

/**
 * 统计树节点数量
 */
export function countTreeNodes(tree: OrganizationItem[]): number {
  let count = 0;
  for (const node of tree) {
    count++;
    if (node.children?.length) {
      count += countTreeNodes(node.children);
    }
  }
  return count;
}