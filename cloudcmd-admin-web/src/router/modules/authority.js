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
const authorityRouter = {
  path: '/authority',
  component: Layout,
  alwaysShow: true,
  name: 'authority',
  meta: {
    title: '权限中心',
    icon: 'component'
  },
  children: [
    {
      path: 'role',
      component: () => import('@/views/authority/auth'),
      name: 'Role',
      meta: { title: language_type.menuBar.role }
    },
    {
      path: 'person',
      component: () => import('@/views/authority/person'),
      name: 'person',
      meta: { title: '权限管理' }
    },
    {
      path: 'userManage',
      component: () => import('@/views/authority/userManage'),
      name: 'userManage',
      meta: { title: '用户管理' }
    },
    {
      path: 'IMPermission',
      component: () => import('@/views/authority/imPermission'),
      name: 'IMPermission',
      meta: { title: '前台权限管理' }
    },
    {
      path: 'IMrole',
      component: () => import('@/views/authority/imRole'),
      name: 'IMrole',
      meta: { title: '前台角色管理' }
    },
    {
      path: 'IMperson',
      component: () => import('@/views/authority/imPerson'),
      name: 'IMperson',
      meta: { title: '前台用户管理' }
    },
    {
      path: 'adminPermission',
      component: () => import('@/views/authority/adminPermission/index'),
      name: 'adminPermission',
      meta: { title: '后台权限管理' }
    },
    {
      path: 'adminRole',
      component: () => import('@/views/authority/adminRole'),
      name: 'adminRole',
      meta: { title: '后台角色管理' }
    },
    {
      path: 'adminPerson',
      component: () => import('@/views/authority/adminPerson'),
      name: 'adminPerson',
      meta: { title: '后台用户管理' }
    },
    {
      path: 'customDepartment',
      component: () => import('@/views/authority/customDepartment'),
      name: 'customDepartment',
      meta: { title: '自定义组织管理' }
    },
    // {
    //   path: 'menu',
    //   component: () => import('@/views/permission/menu'),
    //   name: 'menu',
    //   meta: { title: 'xxxx' }
    //   // [MOVED TO srcDisabled/views/permission/]
    // }
    // {
    //   path: 'approval',
    //   component: () => import('@/views/authority/approval'),
    //   name: 'approval',
    //   meta: { title: '授权审批' }
    //   // [MOVED TO srcDisabled/views/authority/approval/]
    // },
  ]
}

export default authorityRouter
