import request from '@/utils/request'

// 创建可调用南向应用
export const createCallableApp = (data) => request({ url: '/third/v1/app/callable', method: 'post', data })

// 更新南向可调用应用信息
export const updateCallableApp = (callableId, data) => request({ url: `/third/v1/app/callable/${callableId}`, method: 'put', data })

// 更新南向可调用应用映射关系
export const updateCallableAppMapper = (callableId, data) => request({ url: `/third/v1/app/callable/${callableId}/mapper`, method: 'put', data })

// 删除南向可调用应用
export const deleteCallableApp = (callableId) => request({ url: `/third/v1/app/callable/${callableId}`, method: 'delete' })

// 获取南向可调用应用列表
export const getCallableAppList = (params) => request({ url: '/third/v1/app/callable', method: 'get', params })

// 获取南向可调用应用详情
export const getCallableAppDetail = (callableId) => request({ url: `/third/v1/app/callable/${callableId}`, method: 'get' })

// 获取南向可调用应用映射关系
export const getCallableAppTables = (callableId) => request({ url: `/third/v1/app/callable/${callableId}/tables`, method: 'get' })

// 获取事务数据任务标准件配置
export const getCallableAppTaskConfig = (callableId) => request({ url: `/third/v1/app/callable/${callableId}/task-config`, method: 'get' })

// 设置事务数据任务标准件配置
export const setCallableAppTaskConfig = (callableId, data) => request({ url: `/third/v1/app/callable/${callableId}/task-config`, method: 'post', data })
