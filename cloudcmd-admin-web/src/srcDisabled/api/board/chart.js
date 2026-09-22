import request from '@/utils/request'

const localUrl = `http://127.0.0.1:4523/m1/4449071-4094989-default`
// 查看图表地址对象 分页
export function lookDashboardPageLocal() {
  return request({
    url: localUrl + `/api/v2/dashboard/charts/page`,
    method: 'get'
  })
}

// 新增图表地址对象
export function addDashboard(data) {
  return request({
    url: '/api/v2/dashboard/charts',
    method: 'post',
    data
  })
}

// 删除图表地址对象
export function delCharts(id) {
  return request({
    url: `/api/v2/dashboard/charts/${id}`,
    method: 'delete'
  })
}

// 查看图表地址对象 id
export function lookDashboardById(id) {
  return request({
    url: `/api/v2/dashboard/charts/${id}`
  })
}

// 更改图表地址对象
export function upDateDashboard(data, id) {
  return request({
    url: `/api/v2/dashboard/charts/${id}`,
    method: 'put',
    data
  })
}

// 查看图表地址对象 分页
export function lookChartsPage(params) {
  return request({
    url: `/api/v2/dashboard/charts/page`,
    method: 'get',
    params
  })
}

// 查看图表地址对象 列表
export function lookDashboardList(params) {
  return request({
    url: `/api/v2/dashboard/charts/list`,
    method: 'get',
    params
  })
}

// 获取图表导出类型
export function getChartExportType() {
  return request({
    url: `/api/v2/dashboard/charts/model`,
    method: 'get'
  })
}

