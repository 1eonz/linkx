import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
// 导入值班信息列表
export function uploadDutyInformationFile(file) {
  return request({
    url: `${baseUrl}/duty/schedule/import`,
    method: 'post',
    data: file
  })
}

// 导出值班信息列表
export function exportDutyInformationTemplate() {
  return request({
    url: `${baseUrl}/duty/schedule/template`,
    method: 'get',
    responseType: 'blob'
  })
}
// 获取日历模式列表
export function getScheduleCalendar(params) {
  return request({
    // url: 'http://172.24.65.22:9084/collaboration/duty/schedule/calendar',
    url: `${baseUrl}/duty/schedule/calendar`,
    method: 'get',
    params
  })
}
// 获取列表
export function getSchedulePage(params) {
  return request({
    // url: 'http://172.24.65.22:9084/collaboration/duty/schedule/page',
    url: `${baseUrl}/duty/schedule/page`,
    method: 'get',
    params
  })
}

// 批量删除排班信息
export function delBatchSchedule(data) {
  return request({
    url: `${baseUrl}/duty/schedule/deleteBatch`,
    method: 'delete',
    data
  })
}
