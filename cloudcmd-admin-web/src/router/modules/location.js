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
const locationRouter = {
  path: '/location',
  component: Layout,
  alwaysShow: true,
  name: 'location',
  meta: {
    title: '位置管理',
    icon: 'component'
  },
  children: [
    {
      path: 'location',
      component: () => import('@/views/location/index'),
      name: 'Location',
      meta: { title: '位置信息' }
    },
  ]
}

export default locationRouter
