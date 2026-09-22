import type { RouteRecordRaw } from 'vue-router';

import type { App } from 'vue';
import { createRouter, createWebHistory } from 'vue-router';

import { routers } from './modules';
import { useRouterGuard } from './routerGuard';

const routes: any[] = [
  {
    component: () => import('@/views/login/index.vue'),
    name: 'Login',
    path: '/login',
  },
  {
    component: () => import('@/views/layout/index.vue'),
    name: 'Layout',
    path: '/layout',
    redirect: '/layout/agent',
    children: routers,
  },
  {
    path: '/',
    redirect: '/layout',
  },
];

// app router
export const router = createRouter({
  // use dev base (/) locally, sub-path when built
  history: createWebHistory(import.meta.env.BASE_URL || '/'),
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
