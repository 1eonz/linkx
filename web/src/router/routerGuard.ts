import { appConfig } from '@/config';
import { usePermissions } from '@/hooks';
import { useRouterStore } from '@/store';
import { getToken } from '@/utils/auth';
import { isEmpty } from '@/utils/is';

import { homeRouters } from './modules';

const Home = () => import('@/pages/home/index.vue');

let menus: any = [];

function addRouter(router) {
  const routerStore = useRouterStore();
  const menuUrlList = routerStore.menuList.map((i) => i.url);

  const mapper = (data) => {
    const arr: any[] = [];
    data?.forEach((item) => {
      const { name } = item;
      if (name === 'mission' && !usePermissions('MISSION')) {
        return;
      }
      if (name === 'warningNotice' && !usePermissions('ALARM')) {
        return;
      }
      if (name === 'videoControl' && !usePermissions('CONTROL')) {
        return;
      }
      if (name === 'createControlTask' && !usePermissions('CONTROL')) {
        return;
      }
      item.children = mapper(item.children);
      arr.push(item);
    });
    return arr.length > 0 ? arr : null;
  };
  const hasPermissions = mapper(homeRouters) || [];

  // TODO:应该由后端加上菜单权限
  menus = hasPermissions.filter((i) =>
    [
      'coordination',
      'leadVehicle',
      'planSafety',
      'planSpecial',
      'statics',
      'dutyInformation',
      ...menuUrlList,
    ].includes(i.name),
  );
  router.addRoute({
    component: Home,
    meta: {
      icon: '',
      title: '',
    },
    name: 'Home',
    path: '/home',
    redirect: '/homeScreen',
    children: menus,
  });
}

export function useRouterGuard(router) {
  router.beforeEach(async (to, _, next) => {
    if (to.path === '/iconView') {
      next();
      return;
    }

    if (to.path === '/__devtools__/') {
      next();
      return;
    }

    const routerStore = useRouterStore();
    if (isEmpty(appConfig.settingData)) {
      await routerStore.getGlobalsOptions();
    }

    if (routerStore.menuList.length === 0 && to.path !== '/login') {
      const res = await routerStore.getMenu();
      if (!res) {
        next('/login');
        return;
      }

      addRouter(router);

      const index = menus.findIndex((i) => to.path.includes(i.path));
      if (index === -1) {
        next(menus[0].path);
      } else {
        next(to);
      }
      return;
    }

    const hasToken = getToken();
    const { path } = to;
    // white list
    if (hasToken) {
      const demsToken = localStorage.getItem('demsLoginToken');
      const demsVideo = localStorage.getItem('demsVideo');

      if (demsToken && demsVideo === '1' && path !== '/mrs' && path !== '/login') {
        next('/mrs');
        return;
      }

      if (path === '/index.html') {
        next('/');
      } else {
        next();
      }
    } else {
      if (path === '/login') {
        next();
      } else {
        next('/login');
      }
    }
  });
}
