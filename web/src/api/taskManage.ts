// web/src/api/taskManage.ts
import http from '@/utils/http';

// 获取可调用南向应用列表
export const getCallebleApps = async (params: any) => http.get<any>('/linkx/desktop/third/v1/app/callable', {params})

// 获取可调用南向应用数据表列表
export const getCallebleTables = async (callableId: any) => http.get<any>(`/linkx/desktop/third/v1/app/callable/${callableId}/tables`)

// 查询可调用南向应用的数据
export const getCallebleTablesData = async (callableId: any, params: any) => http.get<any>(`/linkx/desktop/third/v1/app/callable/${callableId}/data`, params)

// 转换事务为任务
export const convertToTask = async (callableId: any, data: any) => http.post<any>(`/linkx/desktop/third/v1/app/callable/${callableId}/data/tasks`, data)

// 获取事务转任务状态
export const getTaskStatus = async (callableId: any, params: any) => http.get<any>(`/linkx/desktop/third/v1/app/callable/${callableId}/data/tasks/status`, { params: params })

// 派发任务
export const dispatchTask = async (callableId: any, taskData: any) => http.post<any>(`/linkx/desktop/third/v1/app/callable/${callableId}/data/tasks`, taskData)

// 获取任务详情
export const getTaskDetail = async (taskId: any) => http.get<any>(`/linkx/desktop/third/v1/tasks/${taskId}`)

// 获取任务操作记录
export const getProcessList = async (taskNumber: any) => http.get<any>(`/linkx/desktop/collaboration/v1/tasks/${taskNumber}/processes/list`)
// 获取任务文件列表
export const getFileList = async (taskNumber: any) => http.get<any>(`/linkx/desktop/collaboration/v1/tasks/${taskNumber}/attachment`)

// 分页查询任务处理记录（新接口 /collaboration/v1/tasks/{taskNumber}/processes）
export const getProcessListPage = async (
  taskNumber: string,
  params: { pageNum?: number; pageSize?: number } = {}
) => http.get<any>(`/linkx/desktop/collaboration/v1/tasks/${taskNumber}/processes`, { params })

// 获取任务列名称
export const getColumns = async (callableId: any, params: any) => http.get<any>(`/linkx/desktop/third/v1/app/callable/${callableId}/data/columnInfo`, { params})

// 获取南向应用任务派发模板配置
export const getCallableAppTaskConfig = async (callableId: any) => http.get<any>(`/linkx/desktop/third/v1/app/callable/${callableId}/task-config`)

export const getUserList = (params: any, config?: { abort?: AbortSignal }) =>
  http.get<any>(`/linkx/desktop/collaboration/v1/post/queryUserByPage`, { params, abort: config?.abort })

// ============ 任务标准件模块（对接后端 TasksController 真实接口） ============
// 字段对齐接口文档：根路径 /collaboration/v1/tasks（前缀 /linkx/desktop 由网关拼接）
// 鉴权：需登录（access token），未登录返回 access token invalid

// 接口一：查询 PC 展示的任务标准件模块列表
// GET /collaboration/v1/tasks/config/modules
// 出参：R<List<TasksConfigVO>>
export const getTaskStandardModules = () =>
  http.get<any>('/linkx/desktop/collaboration/v1/tasks/config/modules');

// 接口二：通过模块名称分页查询任务列表
// GET /collaboration/v1/tasks/config/tasks/page
// 出参：R<Page<TasksVO>>，分页结构 { total, pages, current, size, records }
// 备注：scope != 1 且未传 idCard 时由后端取当前登录人身份证号；任务按 update_time DESC 排序
export const getTaskList = (params: {
  module: string;          // 必填，任务标准件模块名称
  pageNum?: number;        // 默认 1
  pageSize?: number;       // 默认 10
  name?: string;           // 任务名称关键字（模糊匹配）
  content?: string;       // 任务内容关键字（模糊匹配）
  scope?: number;          // 范围：1全部、2创建人、3执行人、4创建人+执行人
  idCard?: string;         // scope != 1 时用于过滤；未传取当前登录人身份证号
}) =>
  http.get<any>('/linkx/desktop/collaboration/v1/tasks/config/tasks/page', { params });
