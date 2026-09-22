import request from '@/utils/request'

// capp列表
export function getCappList(data) {
  return request({
    url: '/api/cappJump/list',
    method: 'post',
    data
  })
}

// 新增capp
export function addCapp(data) {
  return request({
    url: '/api/cappJump/add',
    method: 'post',
    data
  })
}

// 修改capp
export function updateCapp(data) {
  return request({
    url: '/api/cappJump/update',
    method: 'post',
    data
  })
}

// 删除capp
export function deleteCapp(data) {
  return request({
    url: `/api/cappJump/delete?id=${data.id}`,
    method: 'post',
    data
  })
}

// 启用/禁用
export function updateSystemApp(data) {
  return request({
    url: '/api/cappJump/updateSystemApplication',
    method: 'post',
    data
  })
}

// 收藏
export function updateFavorite(data) {
  return request({
    url: '/api/cappJump/updateFavorite',
    method: 'post',
    data
  })
}
