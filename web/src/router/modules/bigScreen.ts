import type { AppRouteModule } from '@/router/types';

export const homeBigRouters: AppRouteModule[] = [
  {
    component: () => import('@/pages/bigScreen/communicateCenter/index.vue'),
    name: 'communicateCenter',
    path: '/communicateCenter',
  },
  {
    component: () => import('@/pages/bigScreen/screenView/index.vue'),
    name: 'screenView',
    path: '/screenView',
  },
  {
    component: () => import('@/pages/bigScreen/mapCenter/index.vue'),
    name: 'mapCenter',
    path: '/mapCenter',
  },
  {
    component: () => import('@/pages/bigScreen/monitorCenter/index.vue'),
    name: 'monitorCenter',
    path: '/monitorCenter',
  },
  {
    component: () => import('@/pages/planSafety/index.vue'),
    name: 'planSafety',
    path: '/planSafety',
  },
  {
    component: () => import('@/pages/bigScreen/mapCenter/index.vue'),
    name: 'planSpecial',
    path: '/planSpecial',
  },
  {
    component: () => import('@/pages/bigScreen/leadVehicle/index.vue'),
    name: 'leadVehicle',
    path: '/leadVehicle',
  },
];
