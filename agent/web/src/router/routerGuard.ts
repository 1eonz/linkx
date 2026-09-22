export function useRouterGuard(router) {
  router.beforeEach((to, _, next) => {
    if (to.path === '/iconView') {
      next();
      return;
    }

    next();
  });
}
