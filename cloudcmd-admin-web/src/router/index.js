import Vue from 'vue'
import Router from 'vue-router'
import Layout from '@/layout'
// import dataRouter from '@/router/modules/dataImport' // [MOVED TO srcDisabled/router/modules/dataImport.js]
// import resourceRouter from '@/router/modules/resource' // [MOVED TO srcDisabled/router/modules/resource.js]
import dictionaryRouter from '@/router/modules/baseData'
// import executeControlRouter from '@/router/modules/executeControl' // [MOVED TO srcDisabled/router/modules/executeControl.js]
// import udcRouter from '@/router/modules/udc' // [MOVED TO srcDisabled/router/modules/udc.js]
// import permissionRouter from '@/router/modules/user' // [MOVED TO srcDisabled/router/modules/user.js]
// import personRouter from '@/router/modules/person' // [MOVED TO srcDisabled/router/modules/person.js]
// import authRouter from '@/router/modules/auth' // [MOVED TO srcDisabled/router/modules/auth.js]
// import boardRouter from '@/router/modules/board' // [MOVED TO srcDisabled/router/modules/board.js]
import collaborationRouter from '@/router/modules/collaboration'
import locationRouter from '@/router/modules/location'
import authorityRouter from '@/router/modules/authority' // 权限管理
import schedulingRouter from '@/router/modules/scheduling'
import alertRouter from '@/router/modules/alert' // 预警管理
import thirdInterfaceRouter from '@/router/modules/thirdInterface'
import policeExtendRouter from '@/router/modules/policeExtend'
// import nodeManageRouter from '@/router/modules/nodeManage' // 主线版本下车，入口屏蔽
import i18n from '@/locales'
import store from '../store'
import { getLicenseInfoUtil } from '@/utils/licenseUtils'
import { getGlobalsList } from '@/api/dictionary/globals'
import { getIsAdmin, getUserId } from '@/utils/auth'
import { menuListHasUrl } from '@/utils/permission'
const isProd = process.env.NODE_ENV === 'production'
const homePage = i18n.t('index.homePage')

export const routes = [
  {
    path: '/login',
    component: () => import('@/views/login/index'),
    hidden: true
  },
  {
    path: '/404',
    component: () => import('@/views/404'),
    hidden: true
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: homePage,
        component: () => import('@/views/dashboard'),
        meta: { title: homePage, icon: 'dashboard' }
      }
    ]
  },
  // permissionRouter,
  // udcRouter,
  // 404 page must be placed at the end !!!
  // { path: '*', redirect: '/404', hidden: true }
  { path: '*', redirect: '/', hidden: true }
]

Vue.use(Router)

const createRouter = () => {
  return new Router({
    mode: 'history', // require service support
    base: isProd ? '/linkx/admin/' : '/',
    scrollBehavior: () => ({ y: 0 }),
    routes
  })
}

const router = createRouter()

export function resetRouter() {
  const newRouter = createRouter()
  router.matcher = newRouter.matcher // reset router
}

function filterPaths(routes, paths) {
  console.log('filterPaths', routes, paths)
  // 创建一个新的数组以避免修改原数组
  const filteredPaths = [...paths]

  /**
   * 判断子路由 path 是否匹配 paths 中任一项
   * 支持两种匹配方式：
   * 1. 精确匹配：child.path === 'globals'，paths 含 'globals'
   * 2. 前缀匹配：child.path === 'layoutConfig'，paths 含 'layoutConfig/banner'
   *    （后端菜单 url 是 "父路由path/子标识" 形式时）
   */
  function isChildPathMatched(childPath, filteredPathsArr) {
    // 精确匹配
    if (filteredPathsArr.includes(childPath)) {
      return true
    }
    // 前缀匹配：paths 中存在 "childPath/xxx" 形式
    return filteredPathsArr.some(p => {
      if (typeof p !== 'string') return false
      const slashIdx = p.indexOf('/')
      if (slashIdx === -1) return false
      const prefix = p.substring(0, slashIdx)
      return prefix === childPath
    })
  }

  // 遍历 routes 数组
  for (const route of routes) {
    // 检查当前路由的 path 是否在 paths 中（带斜杠）
    if (filteredPaths.includes(route.path)) {
      // 如果路由有 children
      if (route.children && route.children.length > 0) {
        let hasChildInPaths = false
        // 检查是否有子路由的 path 在 paths 中
        for (const child of route.children) {
          // 子路由的完整路径需要拼接（注意：这里假设子路由的 path 是相对路径）
          const fullChildPath = `${route.path}/${child.path}`.replace(
            /\/\//g,
            '/'
          )
          if (
            filteredPaths.includes(fullChildPath) ||
            isChildPathMatched(child.path, filteredPaths)
          ) {
            hasChildInPaths = true
            break
          }
        }

        // 特殊适配：针对 collaboration 路由，如果没有匹配到子路由，自动添加 index
        // 只有当 route.path 是 /collaboration 且子路由 index 不在 paths 中时才处理
        if (!hasChildInPaths && route.path === '/collaboration') {
          const indexChild = route.children.find(child => child.path === 'index')
          if (indexChild) {
            hasChildInPaths = true
            // 使用唯一标识避免与其他路由的 index 混淆
            const collaborationIndexPath = '/collaboration/index'
            if (!filteredPaths.includes(collaborationIndexPath)) {
              filteredPaths.push(collaborationIndexPath)
            }
          }
        }

        // 如果没有子路由在 paths 中，则从 filteredPaths 中删除当前路由的 path
        if (!hasChildInPaths) {
          const index = filteredPaths.indexOf(route.path)
          if (index !== -1) {
            filteredPaths.splice(index, 1)
          }
        }
      } else {
        // 如果没有 children，保留当前路径（或者根据需求决定是否删除）
        // 这里按照题目要求：如果没有 children 数据在 paths 中就删除
        // 但当前路由本身在 paths 中，且没有 children，所以应该删除
        const index = filteredPaths.indexOf(route.path)
        if (index !== -1) {
          filteredPaths.splice(index, 1)
        }
      }
    }
  }

  // 特殊处理：如果 paths 中有 collaboration 或 /collaboration，确保 /collaboration/index 在结果中
  // 这样 filterMenus 过滤时能正确匹配到子路由
  if (filteredPaths.includes('collaboration') || filteredPaths.includes('/collaboration')) {
    const collaborationIndexPath = '/collaboration/index'
    if (!filteredPaths.includes(collaborationIndexPath)) {
      filteredPaths.push(collaborationIndexPath)
    }
  }

  return filteredPaths
}

// 根据License过滤菜单
async function filterRoutesByLicense(routes) {
  // const { licenseAuth } = store.state.user
  const licenseAuth = await getLicenseInfoUtil()

  // 群组协同license权限
  if (licenseAuth?.groupCollaborationAuth) {
    // 归档群组已迁移到警信扩展信息管理
    const policeExtendExcludeRouteNameArr = ['ArchivedTable']
    policeExtendRouter.children = policeExtendRouter.children.filter(
      item => !policeExtendExcludeRouteNameArr.includes(item.name)
    )
    if (policeExtendRouter.children.length === 0) {
      routes = routes.filter(item => item.name !== 'policeExtend')
    }
    // 筛选位置管理
    const locationExcludeRouteNameArr = ['Location']
    locationRouter.children = locationRouter.children.filter(
      item => !locationExcludeRouteNameArr.includes(item.name)
    )
    if (locationRouter.children.length === 0) {
      routes = routes.filter(item => item.name !== 'location')
    }
  }
  // 业务协同license权限
  // 轮播图管理已迁移到 系统设置管理/APP H5设置 下，不再单独屏蔽路由（保留板块列表可用）
  // 但若需要业务协同license失效时屏蔽整个 layoutConfig，可在此扩展
  return routes
}

async function filterRoutesByGlobalConfig(routes) {
  const { code, data } = await getGlobalsList()
  const isAdmin = getIsAdmin()
  if (code === 0) {
    const DUTY_SCHEDULE_ENABLE = data?.find(
      item => item?.name === 'DUTY_SCHEDULE_ENABLE'
    )?.value
    if (!isAdmin || DUTY_SCHEDULE_ENABLE !== '1') {
      const schedulingRouterArr = ['dutyInformation']
      schedulingRouter.children = schedulingRouter.children.filter(
        item => !schedulingRouterArr.includes(item.name)
      )
      if (schedulingRouter.children.length === 0) {
        routes = routes.filter(item => item.name !== 'scheduling')
      }
    }
  }
  return routes
}
// 根据isAdmin过滤菜单
async function filterRoutesByPermissions(routes) {
  const isAdmin = getIsAdmin()
  const withAdminRoutePaths = ['layoutConfig']
  if (!isAdmin) {
    // 特殊例外：layoutConfig（App H5设置）原本仅管理员可见
    // 但当用户拥有 "layoutConfig/banner"（轮播图管理）菜单权限时，应保留该路由
    // 路由内部的二级 Tab 可见性由 AppH5Config.vue 根据菜单权限控制
    // 注：菜单数据可能存在嵌套，需递归查找
    const userMenu = store.state.user.menu || []
    const hasCarouselMenu = menuListHasUrl(userMenu, 'layoutConfig/banner')
    const adminPathsToHide = hasCarouselMenu
      ? withAdminRoutePaths.filter(p => p !== 'layoutConfig')
      : withAdminRoutePaths
    routes = routes.map(item => {
      if (item.children) {
        item.children = item.children.filter(child => {
          return !adminPathsToHide.includes(child.path)
        })
      }
      return item
    })
  }
  // 用户管理菜单：仅 isAdmin 为 true 且 userId 为 1 时显示
  const userId = getUserId()
  const showUserManage = isAdmin && userId === '1'
  if (!showUserManage) {
    routes = routes.map(item => {
      if (item.children) {
        item.children = item.children.filter(
          child => child.name !== 'userManage'
        )
      }
      return item
    })
  }

  // 生产环境下：隐藏权限中心的6个路由（本地开发时始终展示）
  const isProd = process.env.NODE_ENV === 'production'
  if (isProd) {
    const hiddenPaths = [
      'IMPermission',
      'IMrole',
      'IMperson',
      'adminPermission',
      'adminRole',
      'adminPerson'
    ]
    routes = routes.map(item => {
      if (item.children) {
        item.children = item.children.filter(
          child => !hiddenPaths.includes(child.path)
        )
      }
      return item
    })
  }

  return routes
}
// 根据后台权限动态添加路由
export async function addRouterByPermissions(menu, menuPermissions) {
  let routes = [
    // personRouter, // [MOVED TO srcDisabled/router/modules/person.js]
    // resourceRouter, // [MOVED TO srcDisabled/router/modules/resource.js]
    dictionaryRouter,
    // authRouter,//日志与会话 [MOVED TO srcDisabled/router/modules/auth.js]
    // dataRouter, // [MOVED TO srcDisabled/router/modules/dataImport.js]
    // executeControlRouter, // [MOVED TO srcDisabled/router/modules/executeControl.js]
    // boardRouter, // [MOVED TO srcDisabled/router/modules/board.js]
    // h5Router 已移除：子菜单迁移到 dictionaryRouter(policeExtend/archivedTable) 与 dictionaryRouter(layoutConfig)
    collaborationRouter,
    authorityRouter,
    locationRouter,
    schedulingRouter,
    alertRouter,
    policeExtendRouter,
    thirdInterfaceRouter,
    // nodeManageRouter
  ]
  let paths = []
  // 注：原 H5 父菜单 ID (1522392406668869814, 1522392406668869816) 已移除
  // 子菜单迁移后由后端权限配置中新的父菜单 ID 控制
  const newMenuPermissions = menuPermissions

  const mapper = data => {
    console.log(data, newMenuPermissions, 'data, menuPermissions')
    data.forEach(item => {
      if (newMenuPermissions.includes(item.id)) {
        paths.push(item.url)
        if (item.children) {
          mapper(item.children)
        }
      }
    })
  }
  mapper(menu)
  // 过滤没有子目录的菜单
  paths = filterPaths(routes, paths)
  console.log(paths, 'paths')
  // 过滤没有子目录的菜单
  const { globals, permissions } = store.state.user
  console.log('globals', globals, 'permissions', permissions)
  // 边缘网关、车辆控制、自定义图层、权限（菜单、功能、应用）---开关
  localStorage.removeItem('hiddenGateWay')
  localStorage.removeItem('hiddenVehicle')

  const { hostname, protocol } = window.location
  let supersetHttps = ''
  let supersetHttp = ''
  const hidePaths = []
  globals.forEach(item => {
    if (item.name === 'EDGEGATEWAY_BREAKER' && item.value === '0') {
      localStorage.setItem('hiddenGateWay', true)
      hidePaths.push('gateway')
    } else if (item.name === 'CAR_BREAKER' && item.value === '0') {
      localStorage.setItem('hiddenVehicle', true)
      hidePaths.push('vehicle')
    } else if (item.name === 'CUSTOMIZED_LAYER' && item.value === '0') {
      hidePaths.push('layer')
    } else if (item.name === 'APPLICATION_BREAKER' && item.value === '0') {
      // ...
    } else if (item.name === 'SUPERSET_SERVER_URL') {
      supersetHttps = item.value.replace('ip', hostname)
    } else if (item.name === 'SUPERSET_SERVER_URL_HTTP') {
      supersetHttp = item.value.replace('ip', hostname)
    } else if (item.name === 'CONFIG_DEVICE_GB') {
      sessionStorage.gbId = item.value
    }
  })

  // superset
  const supersetUrl = protocol === 'https:' ? supersetHttps : supersetHttp
  sessionStorage.supersetUrl = supersetUrl

  const filterMenus = data => {
    return data
      .filter(({ path }) => {
        if (hidePaths.includes(path)) {
          return false
        }
        if (permissions.type === 0) {
          return true
        }
        // 特殊处理：collaboration 的 index 子路由
        // 如果 paths 中包含 /collaboration/index，则允许 path 为 index 的子路由
        if (path === 'index' && paths.includes('/collaboration/index')) {
          return true
        }
        // 精确匹配
        if (paths.includes(path)) {
          return true
        }
        // 前缀匹配：后端菜单 url 可能是 "layoutConfig/banner" 这种形式
        // 当前端路由 path 是 "layoutConfig" 时，应视为匹配
        // 这样父级菜单（/baseData）下只要有任一子菜单前缀匹配，父级就会保留
        return paths.some(p => {
          if (typeof p !== 'string') return false
          const slashIdx = p.indexOf('/')
          if (slashIdx === -1) return false
          const prefix = p.substring(0, slashIdx)
          return prefix === path
        })
      })
      .map(item => {
        item = Object.assign({}, item)
        if (item.children) {
          item.children = filterMenus(item.children)
        }
        return item
      })
  }
  routes = await filterRoutesByLicense(routes)
  routes = await filterRoutesByGlobalConfig(routes)
  routes = await filterRoutesByPermissions(routes)
  const list = filterMenus(routes)
  store.commit('user/SET_PERMISSIONS_MENU', list)
  router.addRoutes(list)
}

export default router
