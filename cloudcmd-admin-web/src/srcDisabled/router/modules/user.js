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
const permissionRouter = {
  path: '/user',
  component: Layout,
  alwaysShow: true,
  name: 'permission',
  meta: {
    title: language_type.menuBar.user,
    icon: 'example'
  },
  children: [
    {
      path: 'user',
      component: () => import('@/views/permission/user'),
      name: 'User',
      meta: { title: language_type.menuBar.user }
    },
    {
      path: 'role',
      component: () => import('@/views/permission/role'),
      name: 'Role',
      meta: { title: language_type.menuBar.role }
    },
    {
      path: 'menu',
      component: () => import('@/views/permission/menu'),
      name: 'Menu',
      meta: { title: language_type.menuBar.menu }
    },
    // {
    //   path: 'permission',
    //   component: () => import('@/views/permission/permission'),
    //   name: 'Permission',
    //   meta: { title: '权限管理' }
    // },
    {
      path: 'action',
      component: () => import('@/views/permission/action'),
      name: 'Action',
      meta: { title: language_type.menuBar.action }
    },
    {
      path: 'application',
      component: () => import('@/views/permission/application'),
      name: 'Application',
      meta: { title: language_type.menuBar.application }
    }
  ]
}

export default permissionRouter
