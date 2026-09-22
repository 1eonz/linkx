import type { AppRouteModule } from '@/router/types';

export const routers: AppRouteModule[] = [
  {
    component: () => import('@/views/main/agent/index.vue'),
    meta: {
      title: '智能体管理',
    },
    name: 'Agent',
    path: '/layout/agent',
  },
  {
    component: () => import('@/views/main/queryStatistics/index.vue'),
    meta: {
      title: '查询统计',
    },
    name: 'QueryStatistics',
    path: '/layout/queryStatistics',
  },
  {
    component: () => import('@/views/main/globals/index.vue'),
    meta: {
      title: '全局变量',
    },
    name: 'baseDataGlobals',
    path: '/layout/globals',
  },
  {
    component: () => import('@/views/main/agentFile/index.vue'),
    meta: {
      title: '文件接口',
    },
    name: 'AgentFile',
    path: '/layout/agentFile',
  },
  // {
  //   component: () => import('@/views/main/applicationConfig/index.vue'),
  //   meta: {
  //     title: '应用配置',
  //   },
  //   name: 'ApplicationConfig',
  //   path: '/layout/applicationConfig',
  // },
  // {
  //   component: () => import('@/views/main/carouselManagement/index.vue'),
  //   meta: {
  //     title: '轮播图管理',
  //   },
  //   name: 'CarouselManagement',
  //   path: '/layout/carouselManagement',
  // },
];
