import http from '@/utils/http';

// 通过ID查询110警情详情
export const queryOrderDetailById = (params) => http.get<any>('/diaisi/order/byid', { params });

// 通过地理查询110警情详情
export const queryOrderByGeo = (data) => http.post<any>('/diaisi/order/bygeo', data);

// 分页查询任务列表
export const queryOrderListByPage = (data) => http.post<any>('/diaisi/order/paged', data);
