import request from '@/utils/request'

// 列表查询
export function getAlarmList(data) {
  return request({
    url: '/api/alarmManage/list',
    method: 'post',
    data
  })
}

// 导出
export function exportAlarmData(data) {
  return request({
    url: '/api/alarmManage/exportAlarmData',
    method: 'post',
    responseType: 'blob',
    data
  })
}

// 永久删除
export function permanentlyDelete(data) {
  return request({
    url: '/api/alarmManage/delete',
    method: 'post',
    data
  })
}

// 恢复
export function recover(data) {
  return request({
    url: '/api/alarmManage/recover',
    method: 'post',
    data
  })
}
