import Layout from '@/layout'
const language = localStorage.getItem('localLanguage')

const udcRouter = {
  path: '/udc',
  component: Layout,
  alwaysShow: true,
  name: 'Event',
  meta: {
    title: language === 'cn' ? 'udc相关' : 'be related to UDC',
    icon: 'nested'
  },
  children: [
    {
      path: 'udcReport',
      component: () => import('@/views/udc/udcReport'),
      name: 'UdcReport',
      meta: { title: language === 'cn' ? 'udc上报' : 'Udc report  ' }
    }
    // },
    // {
    //   path: 'udcSync',
    //   component: () => import('@/views/udc/udcSync'),
    //   name: 'UdcSync',
    //   meta: { title:language === 'cn' ?  'udc同步'  : 'Udc synchronization '}
    // }
  ]
}
export default udcRouter
