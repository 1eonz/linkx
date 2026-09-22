import http from '@/utils/http';

// 查询看板发布列表
// status 0-未发布 1-已发布
export const getDashboardsList = (status = 1) =>
  http.get<any>(`/v2/dashboard/dashboards/list/${status}`);

// 查看看板详情
export const getDashboardsDetail = (id) => http.get<any>(`/v2/dashboard/dashboards/${id}`);

// 看板数据导出
export const getChartsExport = (id) =>
  http.get<any>(`/v2/dashboard/charts/export/${id}`, {
    responseType: 'blob',
  });

// 看板数据导出-新
export const getChartsExportNew = (id, data) =>
  http.get<any>(`/v2/dashboard/charts/export/new/${id}`, {
    data,
    responseType: 'blob',
  });
