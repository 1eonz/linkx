import axios from 'axios'
import Store from '@/store'
import Router from '@/router'
import { Message } from 'element-ui'
import { getToken } from '@/utils/auth'
import { CLIENT_TYPE, getBrowserInfo, getOSInfo, getScreenInfo } from '@/utils/clientEnv'
import menuBarCn from '@/locales/lang/cn'
import menuBarEn from '@/locales/lang/en'
import { closeAllDialogs } from '@/utils/createDialog'

const language = localStorage.getItem('localLanguage')
let language_type
if (language === 'en') {
  language_type = menuBarEn.message.index
} else {
  language_type = menuBarCn.message.index
}
// create an axios instance
const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API, // url = base url + request url
  // withCredentials: true, // send cookies when cross-domain requests
  timeout: 60000, // request timeout
})

axios.defaults.headers = {
  applicationId: '1289822833455460000',
  'X-CloudCmd-AppKey': 'CDC-2000',
}

let msgFlag = true

// request interceptor
service.interceptors.request.use(
  (config) => {
    /**
     * 应用ID不进行程序判断就全部默认传递
     */
    config.headers['applicationId'] = '1289822833455460000' // 添加应用id
    config.headers['X-CloudCmd-AppKey'] = 'CDC-2000'
    if (localStorage.getItem('localLanguage') === 'en') {
      config.headers['Accept-Language'] = 'en-us, en;q=1, en;q=0,'
    } else {
      config.headers['Accept-Language'] = 'zh-cn, zh;q=1, en;q=0,'
    }
    // do something before request is sent
    const token = getToken()
    if (token !== undefined && token !== null) {
      // let each request carry token
      // ['X-Token'] is a custom headers key
      // please modify it according to the actual situation
      config.headers['Authorization'] = 'token ' + token
    }

    // 设置客户端环境信息header
    config.headers['X-Browser'] = getBrowserInfo()
    config.headers['X-OS'] = getOSInfo()
    config.headers['X-Screen'] = getScreenInfo()
    config.headers['X-Client-Type'] = CLIENT_TYPE.ADMIN

    return config
  },
  (error) => {
    // do something with request error
    console.log(error) // for debug
    return Promise.reject(error)
  }
)

// response interceptor
service.interceptors.response.use(
  /**
   * If you want to get http information such as headers or status
   * Please return  response => response
   */

  /**
   * Determine the request status by custom code
   * Here is just an example
   * You can also judge the status by HTTP Status Code
   */
  (response) => {
    const res = response.data
    res.headers = response.headers
    return res
  },
  (error) => {
    const { response } = JSON.parse(JSON.stringify(error))
    const resCode = response?.status
    if (resCode === 401) {
      if (msgFlag) {
        msgFlag = !msgFlag
        // 关闭所有打开的弹窗
        closeAllDialogs()
        Message({
          message: language_type.messageText.loginExpiredAgin,
          type: 'warning',
          duration: 5 * 1000,
        })
        Store.dispatch('user/resetToken').then(() => {
          Router.push(`/login`)
        })
        clearInterval(global.tokenTimer)
      }
    } else if (resCode === 403) {
      Message({
        message: response.data.msg,
        type: 'error',
        duration: 5 * 1000,
      })
    } else {
      if (msgFlag) {
        Message({
          message: response.data.msg || language_type.messageText.requestFailed,
          type: 'error',
          duration: 5 * 1000,
        })
      }
    }
    return {}
  }
)

export default service
