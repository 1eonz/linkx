import http from '@/utils/http';

import { ListResponse } from '#/axios';

// 绑定任务和通讯组
export const bindCommunicationGroupAndMission = (data) =>
  http.post<any>('/mission/bindCommunicationGroupAndMission', data);

// missionId查询时间轴
export const queryMissionExecutionByMissionId = (data) =>
  http.post<any>('/mission/queryMissionExecutionByMissionId', data);

// 创建时间轴
export const createMissionExecution = (data) =>
  http.post<any>('/mission/createMissionExecution', data);

// 更新任务
export const updateMissionDetailByUpdateVO = (data) =>
  http.post<any>('/mission/updateMissionDetailByUpdateVO', data);

// 通过ID查询事件详情
export const queryEventDetailById = (data) => http.post<any>('/event/queryEventDetailById', data);

// 通过条件查询任务列表
export const queryMissionListByPage = (data) =>
  http.post<any>('/mission/queryMissionListByPage', data);

// 执行任务
export const completeTask = (data) => http.post<any>('/mission/completeTask', data);

// 通过Id查询任务详情
export const queryMissionDetailById = (data) =>
  http.post<any>('/mission/queryMissionDetailById', data);

// 创建任务
export const createMission = (data) => http.post<any>('/mission/createMission', data);

// 查询流程当前节点
export const queryWorkflowTaskListByInstanceId = (data) =>
  http.post<any>('/mission/queryWorkflowTaskListByInstanceId', data);

// 创建警情
export const createEvent = (data) => http.post<any>('/event/createEvent', data);

// 任务反馈
export const feedbackMission = (data) => http.post<any>('/mission/feedbackMission', data);

// 退单审核
export const checkBackMission = (data) => http.post<any>('/mission/checkBackMission', data);

// 分页查询警情列表接口
export const queryMissionAndEventListByPage = (data) =>
  http.post<ListResponse>('/mission/queryMissionAndEventListByPage', data);

// 分页查询事件列表接口
export const queryEventListByPage = (data) => http.post<any>('/event/queryEventListByPage', data);

// 作废任务
export const cancelMission = (data) => http.post<any>('/mission/cancelMission', data);

// 查询所有事件类型
export const queryTypeList = () => http.post<any>('/event/typeList', {});

// 查询重点人员接口
export const queryFocusPersonByIdCard = (data) =>
  http.post<any>('/mission/queryFocusPersonByIdCard', data);

// 通过号码查询报警次数
export const queryRelateEventListByPhoneNum = (data) =>
  http.post<any>('/event/queryRelateEventListByPhoneNum', data);

// 查询状态任务总数
export const countMissionAndEventListByStatus = (data) =>
  http.post<any>('/mission/countMissionAndEventListByStatus', data);

// 查询任务地图展示数据
export const queryMissionLocation = (data) => http.post<any>('/mission/queryMissionLocation', data);

// 任务处置
export const flowMissionProcess = (type, data) =>
  http.post<any>(`/flow/mission/process/${type}`, data);

// 任务处置过程
export const flowMissionProcessed = (flowId) => http.get<any>(`/flow/mission/process/${flowId}`);

// 任务分页列表
export const flowMissionPaged = (params) => http.get<any>('/flow/mission/paged', { params });

/**
 * @description 根据id查任务分页列表 人员统计type传1 组织传0
 * @param type
 * @param id
 * @returns
 */
export const flowMissionPagedById = (type, id, params) =>
  http.get<any>(`/flow/mission/paged/${type}/${id}`, { params });

// 任务按状态统计
export const flowMissionCount = () => http.get<any>('/flow/mission/count');

// 任务配置列表
export const flowConfigAll = (pageSize, pageNum) =>
  http.get<any>(`/flow/config/all?pageSize=${pageSize}&pageNum=${pageNum}`);

// 任务详情
export const flowMissionDetail = (params: { id: string }) =>
  http.get<any>('/flow/mission/detail', { params });

// 根据type获取任务配置
export const flowConfigByType = (params: { type: string }) =>
  http.get<any>('/flow/config/byType', { params });

// 地图任务数据查询
export const flowMissionByLocation = (params) =>
  http.get<any>('/flow/mission/byLocation', { params });
