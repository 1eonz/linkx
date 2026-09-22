import request from '@/utils/request'
const baseUrl = '/collaboration/v1'
export function labelList(params) {
  return request({
    url: `${baseUrl}/label/list`,
    method: 'get',
    params
  })
}

export function labelSave(data) {
  return request({
    url: `${baseUrl}/label/save`,
    method: 'post',
    data
  })
}

export function labelUpdate(data) {
  return request({
    url: `${baseUrl}/label/update`,
    method: 'post',
    data
  })
}

export function labelDetail(id, idCard) {
  let url = `${baseUrl}/label/detail/${id}`;
  if (idCard) {
    url = `${baseUrl}/label/detail/${id}?idCard=${idCard}`;
  }
  return request({
    url: url,
    method: 'get'
  })
}

export function labelDelete(id) {
  return request({
    url: `${baseUrl}/label/delete/${id}`,
    method: 'delete'
  })
}

// 标签管理批量删除
export function labelBatchDelete(ids) {
  return request({
    url: `${baseUrl}/label/delete/list`,
    method: 'delete',
    data: ids
  })
}


export function bindLabelUser(data) {
  return request({
    url: `${baseUrl}/label/binding/user`,
    method: 'post',
    data
  })
}

