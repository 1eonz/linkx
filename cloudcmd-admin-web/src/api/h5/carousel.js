import request from '@/utils/request'

// 查询轮播图分页
export function getCarouselPage(params) {
  return request({
    url: 'api/content/carousel/page',
    method: 'get',
    params
  })
}

// 查询轮播图详情
export function getCarousel(id) {
  return request({
    url: `/api/content/carousel/get?id=${id}`,
    method: 'get',
  })
}
// 公众号选择
export function officialAccountsSelect(params) {
  return request({
    url: '/collaboration/v1/post/officialAccounts/page',
    method: 'get',
    params
  })
}

// 获取文章
export function getArticleList(params) {
  return request({
    url: '/collaboration/v1/post/articles/page',
    method: 'get',
    params
  })
}

// 新增轮播图
export function createCarousel(data) {
  return request({
    url: '/api/content/carousel/create',
    method: 'post',
    data
  })
}

// 修改轮播图
export function updateCarousel(data) {
  return request({
    url: `/api/content/carousel/update`,
    method: 'put',
    data
  })
}

// 删除轮播图
export function deleteCarousel(id) {
  return request({
    url: `/api/content/carousel/delete?id=${id}`,
    method: 'delete'
  })
}
