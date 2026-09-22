import Vue from 'vue'

import 'normalize.css/normalize.css' // A modern alternative to CSS resets

import ElementUI from 'element-ui'
import 'element-ui/lib/theme-chalk/index.css'
import locale from 'element-ui/lib/locale/lang/zh-CN' // lang i18n

import i18n from './locales' // 国际化
import '@/styles/index.scss' // global css

import App from './App'
import store from './store'
import router from './permission'
import strict from './utils/astrict'

import '@/icons' // icon
import JsonViewer from 'vue-json-viewer'

import VEasy from 'v-easy-components'
import 'v-easy-components/lib/theme-chalk/index.css'
import { hasBtnPermission } from './utils/permission'
import waves from '@/directive/waves'
import loadMore from '@/directive/loadmore'
import components from './components'

/**
 * If you don't want to use mock-server
 * you want to use MockJs for mock api
 * you can execute: mockXHR()
 *
 * Currently MockJs will be used in the production environment,
 * please remove it before going online ! ! !
 */
if (process.env.NODE_ENV !== 'production') {
  const { mockXHR } = require('../mock')
  mockXHR()
}

// set ElementUI lang to EN
Vue.use(ElementUI, { locale })
// 如果想要中文版 element-ui，按如下方式声明
// Vue.use(ElementUI)
Vue.use(VEasy)
Vue.use(waves)
Vue.use(loadMore)
Vue.use(components)
Vue.use(JsonViewer)

Vue.config.productionTip = false
Vue.prototype.hasPerm = hasBtnPermission
Vue.prototype.eventBus = new Vue()

// 点击事件 全局 防抖处理
const on = Vue.prototype.$on
Vue.prototype.$on = function(event, func) {
  let timer
  let newFunc = func
  if (event === 'click') {
    newFunc = function() {
      clearTimeout(timer)
      timer = setTimeout(function() {
        func.apply(this, arguments)
      }, 500)
    }
  }
  on.call(this, event, newFunc)
}

new Vue({
  el: '#app',
  router,
  store,
  i18n,
  strict,
  render: h => h(App)
})
