import type { AppRouteModule } from '@/router/types';

import menuBarEn from '@/locales/lang/en/homePage';
import menuBarCn from '@/locales/lang/zh-CN/homePage';

const lang = localStorage.getItem('localLanguage');
const languageType = lang === 'en' ? menuBarEn : menuBarCn;

const policeTaskRouter: AppRouteModule = {
  component: () => import('@/pages/home/menuLayout.vue'),
  name: 'policeTask',
  path: '/policeTask',
  children: [
    // {
    //   meta: {
    //     title: '警情列表',
    //     icon: 'home_warning',
    //   },
    //   path: '/policeTask/order',
    //   name: 'warningList',
    //   component: () => import('@/pages/order/index.vue'),
    // },
    {
      meta: {
        icon: 'home_mission',
        title: languageType.menus.missionList,
      },
      path: '/policeTask/mission',
      name: 'mission',
      component: () => import('@/pages/mission/index.vue'),
    },
    {
      meta: {
        icon: 'deploy_control',
        title: languageType.menus.createControl,
      },
      path: '/policeTask/createControlTask',
      name: 'createControlTask',
      component: () => import('@/pages/videoControl/controlTask/createControlTask.vue'),
      props: { isMenu: true },
    },
    {
      meta: {
        icon: 'home_control',
        title: languageType.menus.videoControl,
      },
      path: '/videoPatrolControl',
      name: 'videoPatrolControl',
      children: [
        {
          meta: {
            icon: '',
            title: languageType.menus.warningNotice,
          },
          path: '/policeTask/warningNotice',
          name: 'warningNotice',
          component: () => import('@/pages/videoControl/warningNotice/index.vue'),
        },
        {
          meta: {
            icon: '',
            title: languageType.menus.controlList,
          },
          path: '/policeTask/videoControl',
          name: 'videoControl',
          component: () => import('@/pages/videoControl/controlTask/index.vue'),
        },
      ],
    },
    {
      meta: {
        icon: 'home_emergency',
        title: languageType.menus.emergencyMessages,
      },
      path: '/policeTask/emergency',
      name: 'emergency',
      component: () => import('@/pages/messages/index.vue'),
    },
  ],
};

export default policeTaskRouter;
