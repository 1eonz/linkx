import { createRouter, createWebHistory } from 'vue-router';

import { getBaseUrlAll } from '@/utils';

const routes = [
  // TODO：节点加载页 默认首页改为/index
  {
    path: '/nodeLoading',
    name: 'NodeLoading',
    component: () => import('@/pages/nodeLoading.vue'),
  },
  {
    path: '/',
    name: 'Index',
    component: () => import('@/pages/index.vue'),
  },
  {
    path: '/pagesMain/xieTong',
    name: 'xieTong',
    component: () => import('@/pagesMain/xieTong.vue'),
  },
  {
    path: '/pagesMain/renWu',
    name: 'renWu',
    component: () => import('@/pagesMain/renWu.vue'),
  },
  {
    path: '/pagesMain/tongXunLu',
    name: 'tongXunLu',
    component: () => import('@/pagesMain/tongXunLu.vue'),
  },
  {
    path: '/pagesMain/yingYong',
    name: 'yingYong',
    component: () => import('@/pagesMain/yingYong.vue'),
  },

  {
    path: '/pagesMain/LocationApp',
    name: 'LocationApp',
    component: () => import('@/pagesMain/LocationApp.vue'),
  },
  {
    path: '/pagesMain/sdk/index',
    name: 'SDK',
    component: () => import('@/pagesMain/sdk/index.vue'),
  },

  {
    path: '/pages/application',
    name: 'application',
    component: () => import('@/pages/application.vue'),
  },
  {
    path: '/pages/commonAppsPage',
    name: 'commonAppsPage',
    component: () => import('@/pages/commonAppsPage.vue'),
  },
  {
    path: '/pages/equipment',
    name: 'equipment',
    component: () => import('@/pages/equipment.vue'),
  },
  {
    path: '/pages/weSpaceTest',
    name: 'weSpaceTest',
    component: () => import('@/pages/weSpaceTest.vue'),
  },
  // 雄安智能体页面
  {
    path: '/pages/aiAssistantXiongAn/index',
    name: 'aiAssistantIndexXiongAn',
    component: () => import('@/pages/aiAssistant_XiongAn/index.vue'),
  },
  {
    path: '/pages/aiAssistantXiongAn/agent',
    name: 'aiAssistantAgentXiongAn',
    component: () => import('@/pages/aiAssistant_XiongAn/agent.vue'),
  },
  {
    path: '/pages/aiAssistantXiongAn/applyPermissionForm',
    name: 'applyPermissionFormXiongAn',
    component: () => import('@/pages/aiAssistant_XiongAn/applyPermissionForm.vue'),
  },
  // 冀中智能体页面(智能体超市入口修改)
  {
    path: '/pages/aiAssistant/index',
    name: 'aiAssistantIndex',
    component: () => import('@/pages/aiAssistant/index.vue'),
  },
  {
    path: '/pages/aiAssistant/agent',
    name: 'aiAssistantAgent',
    component: () => import('@/pages/aiAssistant/agent.vue'),
  },
  {
    path: '/pages/aiAssistant/chatHistorys',
    name: 'aiAssistantChatHistorys',
    component: () => import('@/pages/aiAssistant/ChatHistorys.vue'),
  },
  {
    path: '/pages/aiAssistant/applyPermissionForm',
    name: 'applyPermissionForm',
    component: () => import('@/pages/aiAssistant/applyPermissionForm.vue'),
  },
  // 广铁加审批逻辑的路径（审批+历史记录走后端）
  {
    path: '/pages/aiAssistantNew/index',
    name: 'aiAssistantIndex',
    component: () => import('@/pages/aiAssistantNew/index.vue'),
  },
  {
    path: '/pages/aiAssistantNew/sessionHistorys',
    name: 'sessionHistorys',
    component: () => import('@/pages/aiAssistantNew/sessionHistorys.vue'),
  },
  {
    path: '/pages/aiAssistantNew/applyPermissionForm',
    name: 'applyPermissionForm',
    component: () => import('@/pages/aiAssistantNew/applyPermissionForm.vue'),
  },
  {
    path: '/pages/createGroup',
    name: 'createGroup',
    component: () => import('@/pages/createGroup.vue'),
  },
  {
    path: '/pages/policeCollaboration',
    name: 'policeCollaboration',
    component: () => import('@/pages/createGroup.vue'),
  },
  {
    path: '/pages/customGroup',
    name: 'customGroup',
    component: () => import('@/pages/customGroup/index.vue'),
  },
  {
    path: '/pages/functionGroup',
    name: 'functionGroup',
    component: () => import('@/pages/functionGroup/index.vue'),
  },
  {
    path: '/pages/dispatchGroup',
    name: 'dispatchGroup',
    component: () => import('@/pages/dispatchGroup/index.vue'),
  },
  {
    path: '/pages/departmentList',
    name: 'departmentList',
    component: () => import('@/pages/customGroup/departmentList.vue'),
  },
  {
    path: '/pages/followList',
    name: 'followList',
    component: () => import('@/pages/customGroup/followList.vue'),
  },
  {
    path: '/pages/myGroupList',
    name: 'myGroupList',
    component: () => import('@/pages/customGroup/myGroupList.vue'),
  },
  {
    path: '/pages/collaborativePositionList',
    name: 'collaborativePositionList',
    component: () => import('@/pages/customGroup/collaborativePositionList.vue'),
  },
  {
    path: '/pages/groupsList',
    name: 'groupsList',
    component: () => import('@/pages/groupsList.vue'),
  },
  {
    path: '/pages/archiveTable',
    name: 'archiveTable',
    component: () => import('@/pages/archiveTable.vue'),
  },
  {
    path: '/pages/archivedTable',
    name: 'archivedTable',
    component: () => import('@/pages/archivedTable.vue'),
  },
  {
    path: '/pages/myGroup',
    name: 'myGroup',
    component: () => import('@/pages/myGroup.vue'),
  },
  {
    path: '/pages/rating/submitRating',
    name: 'submitRating',
    component: () => import('@/pages/rating/submitRating.vue'),
  },
  {
    path: '/pages/rating/ratingList',
    name: 'ratingList',
    component: () => import('@/pages/rating/ratingList.vue'),
  },
  {
    path: '/pages/collaborativeGroup/collection',
    name: 'collaborativeGroupCollection',
    component: () => import('@/pages/collaborativeGroup/collection.vue'),
  },
  {
    path: '/pages/task/task',
    name: 'task',
    component: () => import('@/pages/task/task.vue'),
  },
  {
    path: '/pages/task/collect',
    name: 'collect',
    component: () => import('@/pages/task/collect.vue'),
  },
  {
    path: '/pages/task/taskDeal',
    name: 'taskDeal',
    component: () => import('@/pages/task/taskDeal.vue'),
  },
  {
    path: '/pages/task/taskTransfer',
    name: 'taskTransfer',
    component: () => import('@/pages/task/taskTransfer.vue'),
  },
  {
    path: '/pages/map',
    name: 'map',
    component: () => import('@/pages/map/index.vue'),
  },
  {
    path: '/pages/locationShare',
    name: 'locationShare',
    component: () => import('@/pages/locationShare/index.vue'),
  },
  {
    path: '/pages/webview/webview',
    name: 'webview',
    component: () => import('@/pages/webview.vue'),
  },
  {
    path: '/pages/resources',
    name: 'resources',
    component: () => import('@/pages/resources.vue'),
  },
  {
    path: '/pages/dutySchedule',
    name: 'dutySchedule',
    component: () => import('@/pages/dutySchedule/index.vue'),
  },
  {
    path: '/pagesMain/notifyList',
    name: 'notifyList',
    component: () => import('@/pagesMain/notifyList.vue'),
  },
];

const pathname = getBaseUrlAll();
const router = createRouter({
  history: createWebHistory(pathname),
  routes,
});

export default router;
