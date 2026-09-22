import request from '@/utils/request'
import Vue from 'vue'

export function getAlarmRemindList(data) {
  return request({
    url: '/api/alarmRemind/selectPage',
    method: 'post',
    data
  })
}

export function updateAlarmRemind(data) {
  return request({
    url: '/api/alarmRemind/update',
    method: 'post',
    data
  })
}

export function createAlarmRemind(data) {
  return request({
    url: '/api/alarmRemind/create',
    method: 'post',
    data
  })
}

export function deleteAlarmRemind(id) {
  return request({
    url: '/api/alarmRemind/delete',
    method: 'post',
    params: { id }
  })
}

export const DictBus = new Vue()
