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
const policeExtendRouter = {
  path: '/policeExtend',
  component: Layout,
  alwaysShow: true,
  name: 'policeExtend',
  meta: {
    title: '警信扩展信息管理',
    icon: 'component'
  },
  children: [
    {
      path: 'virtualUser',
      component: () => import('@/views/policeExtend/virtualUser'),
      name: 'virtualUser',
      meta: { title: '虚拟用户管理' }
    },
    {
      path: 'ArchivedTable',
      component: () => import('@/views/h5/archivedTable'),
      name: 'ArchivedTable',
      meta: { title: '归档群组' }
    },
  ]
}

export default policeExtendRouter
