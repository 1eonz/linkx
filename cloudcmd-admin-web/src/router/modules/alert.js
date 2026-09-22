import Layout from '@/layout'
import menuBarCn from '@/locales/lang/cn'
import menuBarEn from '@/locales/lang/en'
const language = localStorage.getItem('localLanguage')
let language_type
if (language === 'en') {
  language_type = menuBarEn.message.index
} else {
  language_type = menuBarCn.message.index
}
const alertRouter = {
  path: '/notification',
  component: Layout,
  alwaysShow: true,
  name: 'notification',
  meta: {
    title: language_type.menuBar.notificationManagement,
    icon: 'component'
  },
  children: [
    {
      path: 'alertPush',
      component: () => import('@/views/notification/alertPush/index.vue'),
      name: 'AlertPush',
      meta: { title: language_type.menuBar.alertPush }
    },
  ]
}

export default alertRouter
