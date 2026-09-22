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
const thirdInterfaceRouter = {
  path: '/thirdParty',
  component: Layout,
  alwaysShow: true,
  name: 'thirdInterface',
  meta: {
    title: '三方对接',
    icon: 'component'
  },
  children: [
    {
      path: 'app',
      component: () => import('@/views/thirdInterface/app'),
      name: 'app',
      meta: { title: '应用管理' }
    },
    {
      path: 'southInterface',
      component: () => import('@/views/thirdInterface/southInterface'),
      name: 'SouthInterface',
      meta: { title: '南向对接' }
    },
    {
      path: 'policeReport',
      component: () => import('@/views/thirdInterface/policeReport'),
      name: 'PoliceReport',
      meta: { title: '警单平台' }
    },
    {
      path: 'unifiedComm',
      component: () => import('@/views/thirdInterface/unifiedComm'),
      name: 'UnifiedComm',
      meta: { title: '通信服务管理' }
    },
    {
      path: 'agentInterface',
      component: () => import('@/views/thirdInterface/agentInterface'),
      name: 'AgentInterface',
      meta: { title: 'AI智能体对接（南向）' }
    },
    {
      path: 'thirdParty',
      component: () => import('@/views/eventType/thirdParty'),
      name: 'ThirdParty',
      meta: { title: language_type.menuBar.thirdParty },
    },
  ]
}

export default thirdInterfaceRouter
