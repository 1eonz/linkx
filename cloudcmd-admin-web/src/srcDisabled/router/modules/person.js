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
const personRouter = {
  path: '/person',
  component: Layout,
  alwaysShow: true,
  meta: {
    title: language_type.menuBar.person,
    icon: 'user'
  },
  children: [
    {
      path: 'person',
      component: () => import('@/views/person/person'),
      name: 'Person',
      meta: { title: language_type.menuBar.person }
    },
    {
      path: 'organization',
      component: () => import('@/views/person/organization'),
      name: 'Organization',
      meta: { title: language_type.menuBar.organization }
    },
    // {
    //   path: 'role',
    //   component: () => import('@/views/person/role'),
    //   name: 'Role',
    //   meta: { title: language_type.menuBar.role }
    // },
    // {
    //   path: 'region',
    //   component: () => import('@/views/person/region'),
    //   name: 'Region',
    //   meta: { title: language_type.menuBar.region }
    // }
  ]
}

export default personRouter
