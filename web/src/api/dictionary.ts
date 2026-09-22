import http from '@/utils/http';

// 根据字典类型获取所有字典配置项
export const getItemListByType = (data) =>
  http.post<any>('/base/dictionaryItem/getItemListByType', data);

// 根据参数获取字典配置值
export const nameByParam = (data) => http.post<any>('/base/dictionaryItem/nameByParam', data);

// 根据编号获取扩展信息列表
export const getExtendInfoPropertiesListByCode = (data) =>
  http.post<any>('/base/extendInfoProperties/getExtendInfoPropertiesListByCode', data);

// 根据编号和扩展信息名称获取扩展信息标签值
export const getExtendInfoPropertiesLabelByParam = (data) =>
  http.post<any>('/base/extendInfoProperties/getExtendInfoPropertiesLabelByParam', data);

// 根据名称获取全局配置值
export const getGlobalsValueByName = (data) =>
  http.post<any>('/base/globals/getGlobalsValueByName', data);

// 获取全局配置列表
export const getGlobalsList = () =>
  http.post<any>('/linkx/desktop/base/v1/globals/getGlobalsList', {});

// 获取所有类型的字典配置
export const getTypeList = () => http.post<any>('/base/dictionaryType/getTypeList', {});

// 获取所有扩展配置信息
export const getExtendInfoPropertiesList = (data) =>
  http.post<any>('/base/extendInfoProperties/getExtendInfoPropertiesList', data);

// 获取version
export const queryVersion = () => http.get<any>('/base/version');

// 获取系统配置
export const getSystemConfig = () =>
  http.get<any>('/linkx/desktop/admin/v1/system/config');
