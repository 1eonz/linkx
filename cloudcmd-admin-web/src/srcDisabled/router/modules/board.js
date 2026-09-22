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
const boardRouter = {
  path: '/board',
  component: Layout,
  alwaysShow: true,
  name: 'Board',
  meta: {
    title: language_type.menuBar.boardConfig,
    icon: 'chart'
  },
  children: [
    {
      path: 'chart',
      component: () => import('@/views/board/chart'),
      name: 'Chart',
      meta: { title: language_type.menuBar.chartManagement }
    },
    {
      path: 'layout',
      component: () => import('@/views/board/layout'),
      name: 'Layout',
      meta: { title: language_type.menuBar.boardManagement }
    },
    {
      path: 'create',
      name: 'Create',
      meta: { title: language_type.menuBar.createChart }
    }
  ]
}

export default boardRouter
