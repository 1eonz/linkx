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
const dataRouter = {
  path: '/auth',
  component: Layout,
  redirect: '/auth/userLog',
  alwaysShow: true,
  name: 'Auth',
  meta: {
    title: language_type.menuBar.auth,
    icon: 'excel'
  },
  children: [
    {
      path: 'userLog',
      component: () => import('@/views/auth/userLog'),
      name: 'UserLog',
      meta: { title: language_type.menuBar.userLog }
    },
    {
      path: 'cookie',
      component: () => import('@/views/auth/cookie'),
      name: 'Cookie',
      meta: { title: language_type.menuBar.cookie }
    },
    // {
    //   path: 'equipmentLog',
    //   component: () => import('@/views/auth/equipmentLog'),
    //   name: 'EquipmentLog',
    //   meta: { title: language_type.menuBar.equipmentLog }
    // },
    // {
    //   path: 'iccLog',
    //   component: () => import('@/views/auth/iccLog'),
    //   name: 'IccLog',
    //   meta: { title: language_type.menuBar.iccLog }
    // },
  ]
}
export default dataRouter
