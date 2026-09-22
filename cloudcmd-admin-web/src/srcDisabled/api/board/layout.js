import request from '@/utils/request'

// 查看看板布局 分页
export function lookDashboardsPage(params) {
  return request({
    url: `/api/v2/dashboard/dashboards/page`,
    method: 'get',
    params
  })
}

// 查看看板布局 id
export function lookDashboardsById(id) {
  return request({
    url: `/api/v2/dashboard/dashboards/${id}`,
    method: 'get'
  })
}

// 查看看板布局列表 status
export function lookDashboardsByStatus(id) {
  return request({
    url: `/api/v2/dashboard/dashboards/list/${id}`
  })
}

// 新增看板布局
export function addDashboards(data) {
  return request({
    url: `/api/v2/dashboard/dashboards`,
    method: 'post',
    data
  })
}

// 删除看板布局
export function delDashboards(id) {
  return request({
    url: `/api/v2/dashboard/dashboards/${id}`,
    method: 'delete'
  })
}

// 修改看板布局
export function updateDashboards(data, id) {
  return request({
    url: `/api/v2/dashboard/dashboards/${id}`,
    method: 'put',
    data
  })
}
