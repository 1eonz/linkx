import http from '@/utils/http';
const baseUrl = '/auth/v1';
export const login = (data) => http.post<any>(baseUrl + '/getToken', data);

export const uniLogin = (data) => http.post<any>(baseUrl +'/oauth/v2/login', data);

export const tokenLogin = (data) => http.post<any>(baseUrl + '/oauth/v2/dems/token', data);

export const logout = (params) => http.get<any>(baseUrl + '/logout', { params });

export const uniLogout = (data?) => http.post<any>(baseUrl + '/oauth/v2/logout', data);

export const refreshToken = (data) => http.post<any>(baseUrl + '/oauth/refresh', data);

export const refreshDemsToken = (data) => http.post<any>(baseUrl + '/oauth/dems/refresh', data);

export const keepalive = () => http.post<any>(`/linkx/desktop${baseUrl}/oauth/v2/keepalive`, {});

export const changePassword = (data) => http.post<any>(baseUrl + '/oauth/v2/changePwd', data);

export const CDClogin = (data) => http.post<any>('/pwi/login', data);

export const CDClogout = (data) => http.post<any>('/pwi/logout', data);

export const getRolePermissions = () => http.post<any>(`/linkx/desktop${baseUrl}/oauth/v2/permissions`, {});

export const getMenuList = (data) => http.post<any>('/linkx/desktop/admin/v1/menu/list', data);

// 登录提示消息
export const loginMessage = (data) => http.post<any>(baseUrl + '/oauth/v2/loginMessage', data);

export const bindResource = (data) => http.post<any>('/resource/bind/bindResource', data);

export const unbindResource = (data) => http.post<any>('/resource/bind/unbindResource', data);

// tokenLogin 一键登录接口
export const newTokenLogin = (data) => http.post<any>(`/linkx/desktop${baseUrl}/oauth/v2/tokenLogin`, data);
