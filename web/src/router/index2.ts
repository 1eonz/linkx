import type { RouteRecordRaw } from 'vue-router';

import type { App } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';

const Home = () => import('@/pages/home/index.vue');

// import { appConfig } from '@/config';

const routes: any[] = [
  {
    component: Home,
    meta: {
      icon: '',
      title: '',
    },
    name: 'Home',
    path: '/home',
    children: [
      {
        component: () => import('@/pages/statics/index.vue'),
        name: 'statics',
        path: '/statics',
      },
      {
        component: () => import('@/pages/dutyInformation/index.vue'),
        name: 'dutyInformation',
        path: '/dutyInformation',
      },
    ],
  },
  {
    meta: {
      icon: '',
      title: '',
    },
    path: '/',
    redirect: '/statics',
  },
];

export const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_PUBLIC_PATH),
  routes: routes as unknown as RouteRecordRaw[],
  scrollBehavior: () => ({ left: 0, top: 0 }),
  strict: true,
});

export const setupRouter = (app: App<Element>) => {
  app.use(router);
};

export default router;
