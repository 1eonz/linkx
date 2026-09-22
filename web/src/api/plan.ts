import http from '@/utils/http';

// 新建预案
export const addAlternative = (data) =>
  http.post<any>('/resource/alternative/addAlternative', data);

// 预案列表
export const queryAlternativeByPage = (data) =>
  http.post<any>('/resource/alternative/queryAlternativeByPage', data);

// 删除预案列表
export const deleteAlternativeById = (data) =>
  http.post<any>('/resource/alternative/deleteAlternativeById', data);

// 修改预案列表
export const updateAlternative = (data) =>
  http.post<any>('/resource/alternative/updateAlternative', data);

// 新增专项保障
export const createPlanGroup = (data) =>
  http.post<any>('/resource/planGroup/createPlanGroup', data);

// 更新专项保障
export const updatePlanGroup = (data) =>
  http.post<any>('/resource/planGroup/updatePlanGroup', data);

// 删除专项保障
export const deletePlanGroup = (data) =>
  http.post<any>('/resource/planGroup/deletePlanGroup', data);

// 查询专项保障详情
export const queryPlanGroupById = (data) =>
  http.post<any>('/resource/planGroup/queryPlanGroupById', data);

// 查询专项保障组成员
export const querySupportGroup = (data) =>
  http.post<any>('/resource/planGroup/supportGroupState', data);

// 查询专项保障列表
export const queryPersonGroupListByParam = (data) =>
  http.post<any>('/resource/planGroup/queryPersonGroupListByParam', data);

// 置顶专项保障
export const stickPlanGroup = (data) => http.post<any>('/resource/planGroup/stickPlanGroup', data);

// 收藏专项保障
export const collectPlanGroup = (data) =>
  http.post<any>('/resource/planGroup/collectPlanGroup', data);

// 圈层防控
export const queryCircleLayerEquipment = (data) =>
  http.post<any>('/resource/planGroup/circleLayerEquipment', data);
