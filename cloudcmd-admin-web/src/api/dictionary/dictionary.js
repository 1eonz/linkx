import request from '@/utils/request'
import Vue from 'vue'
export function getDictionaryTypeList() {
  return request({
    url: '/api/dictionaryType/list',
    method: 'post'
  })
}

export function updateDictionaryType(data) {
  return request({
    url: '/api/dictionaryType/update',
    method: 'post',
    data
  })
}

export function deleteDictionaryType(id) {
  return request({
    url: '/api/dictionaryType/delete',
    method: 'post',
    params: { id }
  })
}

export function createDictionaryType(data) {
  return request({
    url: '/api/dictionaryType/create',
    method: 'post',
    data
  })
}

export function getDictionaryItemListByTypeCode(typeCode) {
  return request({
    url: '/api/dictionaryItem/getListByTypeCode',
    method: 'post',
    params: { typeCode }
  })
}

export function updateDictionaryItem(data) {
  return request({
    url: '/api/dictionaryItem/update',
    method: 'post',
    data
  })
}

export function deleteDictionaryItem(id) {
  return request({
    url: '/api/dictionaryItem/delete',
    method: 'post',
    params: { id }
  })
}

export function createDictionaryItem(data) {
  return request({
    url: '/api/dictionaryItem/create',
    method: 'post',
    data
  })
}

export const DictBus = new Vue()
