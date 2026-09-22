import Store from '@/store'
import { Message } from 'element-ui'
import Router from '@/router'
import i18n from '@/locales'

const StrictObject = strictFunction()

function initLastTime() {
  global.lastRequestTime = new Date().getTime()
}

function strictFunction() {
  let lastTime = new Date().getTime()
  let currentTime = new Date().getTime()
  const timeOut = 60 * 60 * 1000 // 设置超时时间: 60分

  window.onload = () => {
    initLastTime()

    window.document.onmousedown = () => {
      initLastTime()
    }
  }

  function checkTimeout() {
    currentTime = new Date().getTime() // 更新当前时间
    lastTime = global.lastRequestTime

    if (currentTime - lastTime > timeOut) {
      // 判断是否超时
      console.log(i18n.t('index.statusTitle.hasTimedOut'), lastTime)
      clearInterval(global.tokenTimer)
      clearInterval(global.requestTimer)
      Store.dispatch('user/logout').then(() => {
        Router.push(`/login`)
      })
      Message({
        message: i18n.t('index.statusTitle.autoLogout'),
        type: 'warning'
      })
    }
  }

  /* 定时器 间隔30秒检测是否长时间未操作页面 */
  global.requestTimer = setInterval(checkTimeout, 30000)
}

export default {
  StrictObject
}
