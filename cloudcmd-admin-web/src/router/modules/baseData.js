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

const baseDataRouter = {
  path: '/baseData',
  component: Layout,
  alwaysShow: true,
  name: 'BaseData',
  meta: {
    title: language_type.menuBar.baseData,
    icon: 'table',
  },
  children: [
    // {
    //   path: 'dictionary',
    //   component: () => import('@/views/eventType/dictionary'),
    //   name: 'Dictionary',
    //   meta: { title: language_type.menuBar.dictionary },
    //   // [MOVED TO srcDisabled/views/eventType/dictionary.vue]
    // },
    {
      path: 'globals',
      component: () => import('@/views/eventType/globals'),
      name: 'Globals',
      meta: { title: language_type.menuBar.globals },
    },
    // {
    //   path: 'tenantGlobals',
    //   component: () => import('@/views/eventType/tenantGlobals'),
    //   name: 'TenantGlobals',
    //   meta: { title: language_type.menuBar.tenantGlobals },
    //   // [MOVED TO srcDisabled/views/eventType/tenantGlobals.vue]
    // },
    // {
    //   path: 'extendInfoProperties',
    //   component: () => import('@/views/eventType/extendInfoProperties'),
    //   name: 'ExtendInfoProperties',
    //   meta: { title: language_type.menuBar.extendInfoProperties },
    //   // [MOVED TO srcDisabled/views/eventType/extendInfoProperties.vue]
    // },
    // {
    //   path: 'eventType',
    //   component: () => import('@/views/eventType/eventType'),
    //   name: 'EventType',
    //   meta: { title: language_type.menuBar.eventType },
    //   // [MOVED TO srcDisabled/views/eventType/eventType.vue]
    // },
    // {
    //   path: 'account',
    //   component: () => import('@/views/baseData/account'),
    //   name: 'Account',
    //   meta: { title: language_type.menuBar.account },
    //   // [MOVED TO srcDisabled/views/baseData/account.vue]
    // },
    // {
    //   path: 'mission',
    //   component: () => import('@/views/eventType/mission'),
    //   name: 'Mission',
    //   meta: { title: language_type.menuBar.alarm },
    //   // [MOVED TO srcDisabled/views/eventType/mission.vue]
    // },
    // {
    //   path: 'udcReport',
    //   component: () => import('@/views/udc/udcReport'),
    //   name: 'UdcReport',
    //   meta: { title: language_type.menuBar.udcReport },
    //   // [MOVED TO srcDisabled/views/udc/udcReport.vue]
    // },
    // {
    //   path: 'cappList',
    //   component: () => import('@/views/capp/cappList'),
    //   name: 'CappList',
    //   meta: { title: language_type.menuBar.capp },
    //   // [MOVED TO srcDisabled/views/capp/]
    // },
    {
      path: 'mapConfig',
      component: () => import('@/views/baseData/mapConfig/index'),
      name: 'MapConfig',
      meta: { title: language_type.menuBar.mapConfig },
    },
    {
      path: 'layoutConfig',
      component: () => import('@/views/baseData/layoutConfig'),
      name: 'Globals',
      meta: { title: language_type.menuBar.layoutConfig },
    },
    // {
    //   path: 'lawEnforcementHandbookManagement',
    //   component: () => import('@/views/baseData/guide/guideList'),
    //   name: 'LawEnforcementHandbookManagement',
    //   meta: { title: language_type.menuBar.law },
    //   // [MOVED TO srcDisabled/views/baseData/guide/]
    // },
    // {
    //   path: 'alarmRemind',
    //   component: () => import('@/views/alarmRemind/index'),
    //   name: 'AlarmRemind',
    //   meta: { title: language_type.menuBar.remindLevel },
    //   // [MOVED TO srcDisabled/views/alarmRemind/]
    // },
    // {
    //   // 调试代码，需要还原
    //   // path: 'account',
    //   path: 'warningManagement',
    //   component: () => import('@/views/warningManagement/index'),
    //   name: 'WarningManagement',
    //   meta: { title: language_type.menuBar.warningManagement },
    //   // [MOVED TO srcDisabled/views/warningManagement/]
    // },
    // {
    //   path: 'missionConfig',
    //   component: () => import('@/views/baseData/missionConfig/index'),
    //   name: 'MissionConfig',
    //   meta: { title: language_type.menuBar.missionConfig },
    //   // [MOVED TO srcDisabled/views/baseData/missionConfig/]
    // },
  ],
}
export default baseDataRouter
