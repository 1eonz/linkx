import http from '@/utils/http';

// 设置超速告警阈值
export const setRateLimiting = (data) => http.post<any>('/resource/rule/rate_limiting', data);

// 获取超速告警设置值
export const getRateLimiting = () => http.get<any>('/resource/rule/rate_limiting');

// 获取超速列表
export const queryOverSpeedList = () => http.get<any>('/resource/equipment/overspeed/list');
