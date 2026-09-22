import http from '@/utils/http';

export const fenceCreate = (data) => http.post<any>('/resource/ccmd/fence/create', data);

export const fenceLists = (data) => http.post<any>('/resource/ccmd/fence/lists', data);

export const fenceModify = (data) => http.post<any>(`/resource/ccmd/fence/${data.id}`, data);

export const fenceDelete = (id) => http.post<any>(`/resource/ccmd/fence/delete/${id}`);

export const fenceDetails = (id) => http.get<any>(`/resource/ccmd/fence/${id}`);

export const fenceRecover = (data) => http.post<any>('/resource/ccmd/fence/recover', data);
