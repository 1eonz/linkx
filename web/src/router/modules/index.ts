import type { AppRouteModule } from '@/router/types';

export const homeRouters: AppRouteModule[] = [
  {
    component: () => import('@/pages/statics/index.vue'),
    name: 'statics',
    path: '/statics',
    // path: '/statics/:tabId?/:bigActiveName?',
  },
  {
    component: () => import('@/pages/coordination/index.vue'),
    name: 'coordination',
    path: '/coordination',
  },
  {
    component: () => import('@/pages/dutyInformation/index.vue'),
    name: 'dutyInformation',
    path: '/dutyInformation',
  },
];
