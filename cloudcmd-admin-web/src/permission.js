import router, { addRouterByPermissions } from './router'
// import store from './store'
// import { Message } from 'element-ui'
import NProgress from 'nprogress' // progress bar
import 'nprogress/nprogress.css' // progress bar style
import { getToken } from '@/utils/auth' // get token from cookie
import getPageTitle from '@/utils/get-page-title'
import store from './store'
import { buildRoutesFromOauthMenu, printMatchedInfo } from '@/utils/menuRouteMapper'

NProgress.configure({ showSpinner: false }) // NProgress Configuration

router.beforeEach(beforeEach)

const whiteList = ['/login', '/index.html'] // no redirect whitelist

// oauth菜单是否已加载
let oauthMenuLoaded = false

async function beforeEach(to, from, next) {
  // start progress bar
  NProgress.start()

  const { menu } = store.state.user
  if (!whiteList.includes(to.path) && menu.length === 0) {
    const res = await store.dispatch('user/getMenu')
    if (!res) {
      return
    }
    const { menu, permissions } = store.state.user
    await addRouterByPermissions(menu, permissions.menus)
    next(to.redirectedFrom)

    // 加载oauth菜单：生产环境下等待完成（用于路由过滤），开发环境下并行运行（不影响本地调试）
    if (!oauthMenuLoaded) {
      const isProd = process.env.NODE_ENV === 'production'
      if (isProd) {
        oauthMenuLoaded = true
        // await loadOauthMenu()
      } else {
        loadOauthMenu()
      }
    }
  }

  // set page title
  document.title = getPageTitle(to.meta.title)

  // determine whether the user has logged in
  const hasToken = getToken()

  if (hasToken) {
    if (whiteList.includes(to.redirectedFrom || to.path)) {
      // if is logged in, redirect to the home page
      next({ path: '/' })
      NProgress.done()
    } else {
      next()
    }
  } else {
    if (to.path === '/login') {
      // in the free login whitelist, go directly
      next()
    } else {
      // other pages that do not have permission to access are redirected to the login page.
      next(`/login?redirect=${to.path}`)
      NProgress.done()
    }
  }
}

/**
 * 加载oauth菜单并构建路由（并行运行，不影响现有菜单显示）
 * 当前阶段仅打印结果，待接口调试完毕后可切换
 */
async function loadOauthMenu() {
  try {
    const menuData = await store.dispatch('user/getOauthMenu')
    if (!menuData || menuData.length === 0) {
      console.warn('[OAuth菜单] 未获取到菜单数据')
      return
    }

    const { routes, matchedInfo } = buildRoutesFromOauthMenu(menuData)

    // 打印映射结果
    printMatchedInfo(matchedInfo)

    // 打印构建的路由结构
    console.group('%c[OAuth菜单] 构建的路由结构', 'color: #409EFF; font-weight: bold;')
    console.log(JSON.stringify(routes, (key, val) => {
      // 组件函数无法序列化，用标记替代
      if (key === 'component' && typeof val === 'function') return '[component function]'
      return val
    }, 2))
    console.groupEnd()

    // 生产环境下实际生效：将oauth菜单数据存入store供路由过滤使用
    const isProd = process.env.NODE_ENV === 'production'
    if (isProd) {
      store.commit('user/SET_OAUTH_MENU_LOADED', true)
    }

    oauthMenuLoaded = true
  } catch (err) {
    console.error('[OAuth菜单] 加载失败:', err)
  }
}

router.afterEach(() => {
  // finish progress bar
  NProgress.done()
})

export default router
