// ─────────────────────────────────────────────────────────────
// 用户相关类型
// ─────────────────────────────────────────────────────────────

/** 用户部门信息 */
export interface UserDepartment {
  departmentId: string;
  departmentCode: string;
  departmentName: string;
  isPrimary: boolean;
  fullPath?: string;
}

/** 用户信息 */
export interface UserInfo {
  userid: string;
  username: string;
  userDepartments?: UserDepartment[];
}

// ─────────────────────────────────────────────────────────────
// 组织相关类型
// ─────────────────────────────────────────────────────────────

/** 组织项（支持树状结构） */
export interface OrganizationItem {
  id: string;
  name: string;
  code?: string;
  isDefault?: boolean;
  hasPermission?: boolean;
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
