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
  path: '/importData',
  component: Layout,
  // redirect: '/importData',
  alwaysShow: true,
  name: 'Event',
  meta: {
    title: language_type.menuBar.dataSynchronizationAndImport,
    icon: 'documentation'
  },
  children: [
    {
      path: 'dataImport',
      component: () => import('@/views/dataImport/dataImport'),
      name: 'DataImport',
      meta: { title: language_type.menuBar.dataImport }
    },
    {
      path: 'filesystem',
      component: () => import('@/views/dataImport/filesystem'),
      name: 'Filesystem',
      meta: { title: language_type.menuBar.filesystem }
    },
    // 三方数据同步
    // {
    //   path: 'dataSync',
    //   component: () => import('@/views/dataImport/dataSync'),
    //   name: 'DataSync',
    //   meta: { title: language_type.menuBar.tripartiteDataSync }
    // }
  ]
}
export default dataRouter
