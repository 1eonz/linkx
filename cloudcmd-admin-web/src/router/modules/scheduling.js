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
const schedulingRouter = {
  path: '/scheduling',
  component: Layout,
  alwaysShow: true,
  name: 'scheduling',
  meta: {
    title: '排班管理',
    icon: 'component'
  },
  children: [
    {
      path: 'dutyType',
      component: () => import('@/views/shiftScheduling/dutyType'),
      name: 'dutyType',
      meta: { title: '排班类型管理' }
    },
    {
      path: 'dutyInformation',
      component: () => import('@/views/shiftScheduling/dutyInformation'),
      name: 'dutyInformation',
      meta: { title: '排班信息' }
    },
    // {
    //   path: 'shiftScheduling',
    //   component: () => import('@/views/shiftScheduling/collaborativeLayoffs'),
    //   name: 'shiftScheduling',
    //   meta: { title: '协同上下岗' }
    //   // [MOVED TO srcDisabled/views/shiftScheduling/collaborativeLayoffs/]
    // },
  ]
}

export default schedulingRouter
