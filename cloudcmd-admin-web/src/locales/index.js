import Vue from 'vue'
import VueI18n from 'vue-i18n'
import ElementLocale from 'element-ui/lib/locale'
import en from './lang/en.js'
import cn from './lang/cn.js'

const messages = {
  en: en.message,
  cn: cn.message
}

Vue.use(VueI18n)

const navLang = ['zh-CN', 'zh', 'zh-HK'].includes(navigator.language)
  ? 'cn'
  : 'en'
const lang = localStorage.getItem('localLanguage') || navLang
const i18n = new VueI18n({
  locale: lang,
  fallbackLocale: 'cn',
  messages
})
localStorage.setItem('localLanguage', lang)

ElementLocale.i18n((key, value) => i18n.t(key, value))

export default i18n
