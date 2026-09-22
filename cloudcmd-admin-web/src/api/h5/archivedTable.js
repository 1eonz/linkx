import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
// 查询群组管理分页
export function getArchivePage(params) {
  return request({
    url: `${baseUrl}/groups/archive/list`,
    method: 'get',
    params
  })
}

// 批量下载归档文件
export function getArchiveDownload(data) {
  return request({
    url: `${baseUrl}/groups/archive/download`,
    method: 'post',
    data:data.groupIds,
    responseType: 'blob', // 指定响应类型
    headers: { 'Content-Type': 'application/json' }
  })
}

// 删除群组管理
export function deleteArchive(data) {
  return request({
    url: `${baseUrl}/groups/archive/delete`,
    method: 'post',
    data:data.groupIds,
    headers: { 'Content-Type': 'application/json' }
  })
}
// 同步数据
export function pullHistoryGroup(params) {
  return request({
    url: `${baseUrl}/im/pull/history/group`,
    method: 'post',
    params
  })
}
