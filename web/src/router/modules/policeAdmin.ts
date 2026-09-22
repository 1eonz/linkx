import type { AppRouteModule } from '@/router/types';

import menuBarEn from '@/locales/lang/en/homePage';
import menuBarCn from '@/locales/lang/zh-CN/homePage';

const lang = localStorage.getItem('localLanguage');
const language_type = lang === 'en' ? menuBarEn : menuBarCn;

const policeAdminRouter: AppRouteModule = {
  component: () => import('@/pages/home/menuLayout.vue'),
  name: 'policeAdmin',
  path: '/policeAdmin',
  redirect: '/policeAdmin/alarmFrom',
  children: [
    {
      meta: {
        icon: 'home_alarm',
        title: language_type.menus.alarmRecord,
      },
      path: '/alarmRecord',
      name: 'alarmRecord',
      children: [
        {
          meta: {
            icon: '',
            title: language_type.menus.alarmList,
          },
          path: '/policeAdmin/alarmFrom',
          name: 'alarmFrom',
          component: () => import('@/pages/alarmRecord/alarmFrom.vue'),
        },
        {
          meta: {
            icon: '',
            title: language_type.menus.statisticsList,
          },
          path: '/policeAdmin/statisticsFrom',
          name: 'statisticsFrom ',
          component: () => import('@/pages/alarmRecord/statisticsFrom.vue'),
        },
      ],
    },
    {
      meta: {
        icon: 'service_status',
        title: language_type.menus.policeServiceAdmin,
      },
      path: '/policeServiceAdmin',
      name: 'policeServiceAdmin',
      children: [
        {
          meta: {
            icon: '',
            title: language_type.menus.electronicFence,
          },
          path: '/policeAdmin/electronicFence',
          name: 'electronicFence',
          component: () => import('@/pages/policeAdmin/electronicFence/index.vue'),
        },
        {
          meta: {
            icon: '',
            title: language_type.menus.serviceStatus,
          },
          path: '/policeAdmin/serviceStatus',
          name: 'serviceStatus',
          component: () => import('@/pages/policeAdmin/serviceStatus/index.vue'),
        },
        {
          meta: {
            icon: '',
            title: language_type.menus.trackPlay,
          },
          path: '/policeAdmin/trackPlay',
          name: 'trackPlay',
          component: () => import('@/pages/policeAdmin/trackPlay/index.vue'),
        },
      ],
    },
  ],
};

export default policeAdminRouter;
