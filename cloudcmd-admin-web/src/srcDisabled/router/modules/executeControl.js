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
const executeControlRouter = {
  path: '/executeControl',
  component: Layout,
  alwaysShow: true,
  meta: {
    title: language_type.menuBar.controlRouter,
    icon: 'component'
  },
  children: [
    {
      path: 'task',
      component: () => import('@/views/executeControl/task'),
      name: 'Task',
      meta: { title: language_type.menuBar.controlTask }
    },
    {
      path: 'image',
      component: () => import('@/views/executeControl/image'),
      name: 'Image',
      meta: { title: language_type.menuBar.controlImage }
    }
  ]
}

export default executeControlRouter
