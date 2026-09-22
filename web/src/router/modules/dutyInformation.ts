import type { AppRouteModule } from '@/router/types';

import menuBarEn from '@/locales/lang/en/homePage';
import menuBarCn from '@/locales/lang/zh-CN/homePage';

const lang = localStorage.getItem('localLanguage');
const language_type = lang === 'en' ? menuBarEn : menuBarCn;

const dutyInformationRouter: AppRouteModule = {
  component: () => import('@/pages/home/menuLayout.vue'),
  name: 'dutyInformation',
  path: '/dutyInformation',
  redirect: '/dutyInformation/index',
  children: [
    {
      meta: {
        icon: 'home_alarm',
        title: language_type.menus?.dutyInformation || '值班信息',
      },
      path: '/dutyInformation/index',
      name: 'dutyInformationIndex',
      component: () => import('@/pages/dutyInformation/index.vue'),
    },
  ],
};

export default dutyInformationRouter;