import languageCn from '@/locales/lang/cn'
import languageEn from '@/locales/lang/en'
import { getToken } from '@/utils/auth'

const language = localStorage.getItem('localLanguage')
let language_type
if (language === 'en') {
  language_type = languageEn.message.index
} else {
  language_type = languageCn.message.index
}

export function parseTime(time, cFormat) {
  if (arguments.length === 0) {
    return null
  }
  const format = cFormat || '{y}-{m}-{d} {h}:{i}:{s}'
  let date
  if (typeof time === 'object') {
    date = time
  } else {
    if (typeof time === 'string' && /^[0-9]+$/.test(time)) {
      time = parseInt(time)
    }
    if (typeof time === 'number' && time.toString().length === 10) {
      time = time * 1000
    }
    date = new Date(time)
  }
  const formatObj = {
    y: date.getFullYear(),
    m: date.getMonth() + 1,
    d: date.getDate(),
    h: date.getHours(),
    i: date.getMinutes(),
    s: date.getSeconds(),
    a: date.getDay()
  }
  const time_str = format.replace(/{([ymdhisa])+}/g, (result, key) => {
    const value = formatObj[key]
    // Note: getDay() returns 0 on Sunday
    if (key === 'a') {
      const dayOfWeek0 = language_type.dateDay.dayOfWeek0
      const dayOfWeek1 = language_type.dateDay.dayOfWeek1
      const dayOfWeek2 = language_type.dateDay.dayOfWeek2
      const dayOfWeek3 = language_type.dateDay.dayOfWeek3
      const dayOfWeek4 = language_type.dateDay.dayOfWeek4
      const dayOfWeek5 = language_type.dateDay.dayOfWeek5
      const dayOfWeek6 = language_type.dateDay.dayOfWeek6
      return [
        dayOfWeek0,
        dayOfWeek1,
        dayOfWeek2,
        dayOfWeek3,
        dayOfWeek4,
        dayOfWeek5,
        dayOfWeek6
      ][value]
    }
    return value.toString().padStart(2, '0')
  })
  return time_str
}

/**
 * @param {number} time
 * @param {string} option
 * @returns {string}
 */
export function formatTime(time, option) {
  if (('' + time).length === 10) {
    time = parseInt(time) * 1000
  } else {
    time = +time
  }
  const d = new Date(time)
  const now = Date.now()

  const diff = (now - d) / 1000

  if (diff < 30) {
    return language_type.dateDay.justNow
  } else if (diff < 3600) {
    // less 1 hour
    return Math.ceil(diff / 60) + language_type.dateDay.minutesAgo
  } else if (diff < 3600 * 24) {
    return Math.ceil(diff / 3600) + language_type.dateDay.hoursBefore
  } else if (diff < 3600 * 24 * 2) {
    return language_type.dateDay.oneDayBefore
  }
  if (option) {
    return parseTime(time, option)
  } else {
    return (
      d.getMonth() +
      1 +
      language_type.dateDay.month +
      d.getDate() +
      language_type.dateDay.day +
      d.getHours() +
      language_type.dateDay.hour +
      d.getMinutes() +
      language_type.dateDay.minute
    )
  }
}

export function format(date) {
  const time = new Date(date)
  const y = time.getFullYear()
  const m = time.getMonth() + 1
  const d = time.getDate()
  const h = time.getHours()
  const mm = time.getMinutes()
  const s = time.getSeconds()
  const timeChange = (m) => {
    return m < 10 ? '0' + m : m
  }
  return y + '-' + timeChange(m) + '-' + timeChange(d) + ' ' + timeChange(h) + ':' + timeChange(mm) + ':' + timeChange(s)
}

/**
 * @param {string} url
 * @returns {Object}
 */
export function param2Obj(url) {
  const search = url.split('?')[1]
  if (!search) {
    return {}
  }
  return JSON.parse(
    '{"' +
      decodeURIComponent(search)
        .replace(/"/g, '\\"')
        .replace(/&/g, '","')
        .replace(/=/g, '":"')
        .replace(/\+/g, ' ') +
      '"}'
  )
}

/**
 * 树形数据转换
 * @param {*} data
 * @param {*} id
 * @param {*} pid
 */
export function treeDataTranslate(data, id = 'id', pid = 'parentId') {
  const res = []
  const temp = {}
  for (let i = 0; i < data.length; i++) {
    temp[data[i][id]] = data[i]
  }
  for (let k = 0; k < data.length; k++) {
    if (temp[data[k][pid]] && data[k][id] !== data[k][pid]) {
      if (!temp[data[k][pid]]['children']) {
        temp[data[k][pid]]['children'] = []
      }
      if (!temp[data[k][pid]]['_level']) {
        temp[data[k][pid]]['_level'] = 1
      }
      data[k]['_level'] = temp[data[k][pid]]._level + 1
      temp[data[k][pid]]['children'].push(data[k])
    } else {
      res.push(data[k])
    }
  }
  return res
}

/**
 * 深拷贝
 * @param {*} obj
 * @returns
 */
export function deepCopy(obj) {
  return JSON.parse(JSON.stringify(obj))
}

/**
 * 过滤树形数据
 * @param {*} Array
 * @returns
 */

export function filterOrgList(arr) {
  const newArr = arr.filter(item => item.status === 0)
  return newArr.map(item => {
    if (item.children) {
      item.children = filterOrgList(item.children)
    }
    return item
  })
}

/**
 * 获取服务器文件
 * @param {*} src
 */
export function getServiceFile(src, config) {
  return new Promise((resolve, reject) => {
    const { responseType } = config || {}
    const token = getToken()
    const url = process.env.VUE_APP_BASE_API + src
    const xhr = new XMLHttpRequest()

    xhr.open('GET', url, true)
    if (responseType) {
      xhr.responseType = responseType
    }
    xhr.setRequestHeader('Authorization', 'token ' + token)
    xhr.onload = () => {
      if (xhr.status === 200) {
        resolve(xhr.response)
      } else {
        reject()
      }
    }
    xhr.send()
  })
}

// 传入data转成json下载
export function downloadJsonFile(data, filename) {
  // 将JSON数据转换为字符串
  const jsonString = JSON.stringify(data)

  // 创建一个新的Blob对象
  const blob = new Blob([jsonString], { type: 'application/json' })

  // 创建一个URL对象
  const url = URL.createObjectURL(blob)

  // 创建一个链接并设置下载属性
  const a = document.createElement('a')
  a.href = url
  a.download = filename + '.json'
  a.click()

  // 释放URL对象
  URL.revokeObjectURL(url)
}
