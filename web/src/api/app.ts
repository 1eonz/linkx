import http from '@/utils/http';

// ─────────────────────────────────────────────────────────────
// 类型定义
// ─────────────────────────────────────────────────────────────

/** 应用信息 */
export interface AppInfo {
  id: number;
  name: string;
  icon?: string;
  url?: string;
  status: number; // 0: 启用, 1: 禁用
  sort?: number;
  [key: string]: any;
}

/** 获取全部应用响应 */
export interface GetAppPageResponse {
  records: AppInfo[];
  total: number;
}

// ─────────────────────────────────────────────────────────────
// API 接口
// ─────────────────────────────────────────────────────────────

/**
 * 获取全部应用列表
 * @param params 分页参数
 */
export const getAppPage = (params: { scope: number; pageNum: number; pageSize: number }) => {
  return http.get<GetAppPageResponse>('/linkx/desktop/admin/v1/content/app/info/apps', { params });
};

/**
 * 值班信息应用 ID（与 H5 项目保持一致）
 */
export const DUTY_INFORMATION_APP_ID = 25;
