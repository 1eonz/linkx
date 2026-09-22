import type { RouteRecordRaw } from 'vue-router';

import type { App } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';

import { useRouterGuard } from './routerGuard';

const routes: any[] = [
  {
    component: () => import('@/pages/login/index.vue'),
    meta: {
      icon: '',
      title: '',
    },
    name: 'Login',
    path: '/login',
  },
  {
    component: () => import('@/pages/iconView/index.vue'),
    meta: {
      icon: '',
      title: '',
    },
    name: 'IconView',
    path: '/iconView',
  },
  {
    meta: {
      icon: '',
      title: '',
    },
    path: '/',
    redirect: '/login',
  },
];

// app router
export const router = createRouter({
  history: createWebHistory(import.meta.env.VITE_PUBLIC_PATH),
  routes: routes as unknown as RouteRecordRaw[],
  scrollBehavior: () => ({ left: 0, top: 0 }),
  strict: true,
});

// config router
export const setupRouter = (app: App<Element>) => {
  app.use(router);
};

useRouterGuard(router);

export default router;
