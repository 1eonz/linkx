import http from '@/utils/http';

// 查询组织
export const queryOrganizationById = (data) =>
  http.post<any>('/resource/organization/queryOrganizationById', data);

// 查询子组织
export const queryLowerOrganization = (data) =>
  http.post<any>(
    '/resource/organization/queryLowerOrganizationToCommandCenterByOrganizationId',
    data,
  );

// 查询组织与人员
export const queryEoResourcesByLike = (data) =>
  http.post<any>('/resource/common/queryEoResourcesByLike', data);

// 通过 isdn 查询是装备还是人员或者摄像头
export const queryServiceAccountTypeByAccount = (params) =>
  http.get<any>('/resource/serviceAccount/queryServiceAccountTypeByAccount', {
    params,
  });
