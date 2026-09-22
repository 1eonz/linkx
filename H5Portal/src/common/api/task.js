import { http } from '@/common/network/http.js';

const baseUrl = '/collaboration/v1/'; //"/task-api/"
// 获取全部任务
export const getTaskPage = (params) => http.get(baseUrl + 'tasks/page', params);
// 获取任务详情
export const getTaskDetailByTaskNumber = (params) =>
  http.get(baseUrl + 'tasks/' + params.taskNumber);
// 更改任务状态
export const updateTaskStatusByTaskNumber = (params) =>
  http.request(baseUrl + `tasks/${params.taskNumber}/processes`, params, 'put');
// 收藏任务列表
export const getCollectTaskList = (params) => http.get(baseUrl + 'tasks/favorite/page', params);
// 收藏任务
export const collectTask = (params) =>
  http.post(baseUrl + `tasks/${params.taskNumber}/favorite`, params);
// 取消收藏
export const cancleCollectTask = (params) =>
  http.request(baseUrl + `tasks/${params.taskNumber}/favorite`, params, 'delete');
// 按任务类型统计总数
export const getTaskStatusTotal = (params) =>
  http.get(baseUrl + 'tasks/statistics/total/count', params);
// 获取首页完成类型统计
export const getTaskStatusCount = (params) =>
  http.get(baseUrl + 'tasks/statistics/status/count', params);
// 获取首页任务类型统计
export const getTaskTypeCount = (params) =>
  http.get(baseUrl + 'tasks/statistics/type/count', params);
// 获取当前用户的任务的所有业务类型
export const getTaskTypeList = (params) => http.get(baseUrl + 'tasks/statistics/type/list', params);
// 按任务类型统计
export const getTaskTypeStatistics = (params) =>
  http.get(baseUrl + 'tasks/statistics/type', params);
// 按任务完成度统计
// export const getTaskCompletenessStatistics = (params) => http.get(baseUrl+"tasks/statistics/completeness", params);
export const getTaskCompletenessStatistics = (params) =>
  http.get(baseUrl + 'tasks/statistics/status/completeness', params);
// 任务发起趋势统计
export const getTaskInitiateTrendStatistics = (params) =>
  http.get(baseUrl + 'tasks/statistics/date', params);
// 任务量分析
export const getTaskStatisticsMineCount = (params) =>
  http.get(baseUrl + 'tasks/statistics/mine/count', params);
// 任务发起趋势统计
export const getTaskStatisticsMineCompleteness = (params) =>
  http.get(baseUrl + 'tasks/statistics/mine/completeness', params);
// 任务平均完成时间分析
export const getTaskStatisticsMineAvgDuration = (params) =>
  http.get(baseUrl + 'tasks/statistics/mine/avg/duration', params);
// 获取待处理任务总数
export const getMineTodoCount = (params) =>
  http.get(baseUrl + 'tasks/statistics/mine/todo/count', params);
// 查询离线地图列表
export const selectListMap = (data) =>
   http.post(  '/admin/v1/map/selectListMap', data);
// 获取任务详情
export const getColumns = (params) => 
  http.get('/third/v1/app/callable/data/columnInfo', params);
// 上传任务附件
// options.onProgress: 进度回调 (progress: number 0-100) => void
// options.signal: AbortSignal,用于取消上传
export const uploadAttachment = (formData, options = {}) => {
  const { onProgress, signal } = options;
  return http.upFile(
    baseUrl + `tasks/upload/attachment`,
    formData,
    { 'Content-Type': ' multipart/form-data' },
    {
      onUploadProgress: (progressEvent) => {
        if (progressEvent.lengthComputable && onProgress) {
          const progress = Math.round((progressEvent.loaded / progressEvent.total) * 100);
          onProgress(progress);
        }
      },
      signal,
    },
  );
};
  
// 获取任务附件 { taskNumber: 任务编号, id: 附件id }
export const getAttachment = (params) => 
  http.get(baseUrl + `tasks/${params.taskNumber}/attachment`);
// 删除任务附件 { taskNumber: 任务编号, id: 附件id, filePath: 附件路径 }
export const deleteAttachment = (params) => 
  http.request(baseUrl + `tasks/${params.taskNumber}/v2/attachment`, { id: params.id, filePath: params.filePath }, 'delete');
// 查询任务处置列表
export const getProcessList = (params) =>
  http.get(baseUrl + `tasks/${params.taskNumber}/processes/list`);
// 更新任务状态 { taskNumber: 任务编号, status: 任务状态, attachments: 附件列表 }
export const updateTaskStatus = (data) => 
  http.post(baseUrl + `tasks/${data.taskNumber}/attachment`, data);

