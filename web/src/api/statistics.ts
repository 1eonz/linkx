import http from '@/utils/http';

// 话务统计
export const queryTrafficMeasurement = (data: any) =>
  http.post<any>('/statistics/record/statistics/queryRecordStatistics', data);

// 组织话务统计
export const queryOrganizeTrafficStatistics = (data?: any) =>
  http.post<any>('/statistics/record/statistics/queryAccountUsage', data);

// 警情统计
export const queryCountMission = (data?: any) =>
  http.post<any>('/statistics/mission/countMission', data);

// 预警统计
export const queryCountAlarm = (data?: any) =>
  http.post<any>('/statistics/alarm/queryCountAlarm', data);

// 警情分布区域
export const queryPoliceDistribution = (data?: any) =>
  http.post<any>('/statistics/mission/countMissionGroupByOrganization', data);

// 设备统计
export const queryCountEquipment = (data?: any) =>
  http.post<any>('/statistics/equipment/countEquipment', data);

// 抓拍统计
export const querySnapStatistics = (data?: any) =>
  http.post<any>('/statistics/suspecttask/countSnapshot', data);

// 重点人员与记录仪抓拍统计
export const queryCountSnapshotAndKeyPersonnel = (data?: any) =>
  http.post<any>('/statistics/suspecttask/countSnapshotAndKeyPersonnel', data);

// 证据文件统计
export const queryEvidenceDocuments = (data?: any) =>
  http.post<any>('/statistics/data/evidence/statistics', data);

// 终端使用统计
export const queryTerminalStatistics = (data?: any) =>
  http.post<any>('/statistics/data/countTerminal', data);

// 组织统计
export const queryOrgStatistics = (data?: any) =>
  http.post<any>('/statistics/data/countOrganization', data);

// 组织人员统计
export const queryOrgPersonnelStatisticss = (data?: any) =>
  http.post<any>('/statistics/data/countOrganizationAndUser', data);

// 二级域
export const querySecondLevelDomain = (data?: any) =>
  http.post<any>('/statistics/data/countRecorderBySecondLevelDomain', data);
