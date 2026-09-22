import Layout from '@/layout'

/**
 * 二级菜单url → 组件 扁平映射表
 * url 是唯一标识，与父级菜单无关
 * 注释掉的条目表示路由已禁用，待恢复时取消注释即可
 */
const ROUTE_COMPONENT_MAP = {
  // ===== 系统配置 /baseData =====
  thirdParty: () => import('@/views/eventType/thirdParty'),
  globals: () => import('@/views/eventType/globals'),
  mapConfig: () => import('@/views/baseData/mapConfig/index'),
  layoutConfig: () => import('@/views/baseData/layoutConfig'),
  // dictionary: () => import('@/views/eventType/dictionary'),       // [已注释，待恢复] 字典配置
  // southBoundAccount: null,                                        // [路由不存在，待补充] 三方接入管理

  // ===== 权限中心 /authority =====
  IMPermission: () => import('@/views/authority/imPermission'),
  IMrole: () => import('@/views/authority/imRole'),
  IMperson: () => import('@/views/authority/imPerson'),
  adminPermission: () => import('@/views/authority/adminPermission/index'),
  adminRole: () => import('@/views/authority/adminRole'),
  adminPerson: () => import('@/views/authority/adminPerson'),
  // role: () => import('@/views/authority/auth'),                   // [已注释，待恢复] 角色管理
  // person: () => import('@/views/authority/person'),               // [已注释，待恢复] 权限管理

  // ===== H5管理（已移除，路由分散到各业务模块） =====
  // carousel 已迁移到 系统设置管理/APP H5设置/轮播图管理，不再作为顶级菜单
  // ArchivedTable 已迁移到 警信扩展信息管理/归档群组（path 仍为 ArchivedTable）
  ArchivedTable: () => import('@/views/h5/archivedTable'),
  GroupTags: () => import('@/views/h5/groupTags'),

  // ===== 协同岗管理 /collaboration =====
  collaboration: () => import('@/views/h5/collaboration'),
  quick: () => import('@/views/h5/quick'),

  // ===== 位置管理 /location =====
  location: () => import('@/views/location/index'),

  // ===== 三方警单 /policeReport =====
  dock: () => import('@/views/thirdInterface/policeReport/dock'),
  manage: () => import('@/views/thirdInterface/policeReport/manage'),
  typeManage: () => import('@/views/thirdInterface/policeReport/typeManage'),

  // ===== 排班管理 /scheduling =====
  dutyInformation: () => import('@/views/shiftScheduling/dutyInformation'),

  // ===== 预警管理 /notification =====
  alertPush: () => import('@/views/notification/alertPush/index.vue'),

  // ===== 看板配置 /board ===== (已注释，待恢复)
  // chart: () => import('@/views/board/chart'),
  // layout: () => import('@/views/board/layout'),
  // create: () => import('@/views/board/create'),
}

/**
 * 标准化url：去掉前导斜杠
 * @param {string} url
 * @returns {string}
 */
function normalizeUrl(url) {
  if (!url) return ''
  return url.replace(/^\//, '')
}

/**
 * 根据oauth菜单数据构建vue-router路由配置
 * @param {Array} menuData - 接口返回的菜单数组
 * @returns {Object} { routes, matchedInfo }
 *   routes: 可直接用于 router.addRoutes 的路由数组
 *   matchedInfo: 映射详情，用于调试打印
 */
export function buildRoutesFromOauthMenu(menuData) {
  if (!Array.isArray(menuData)) {
    console.warn('[menuRouteMapper] menuData is not an array:', menuData)
    return { routes: [], matchedInfo: { matched: [], placeholder: [], disabled: [], routeOnly: [] }}
  }

  const matchedInfo = {
    matched: [], // 映射成功的菜单
    placeholder: [], // 映射到占位页面的菜单
    disabled: [], // status=0 被过滤的菜单
    routeOnly: [], // 路由存在但接口未返回的菜单
  }

  // 收集接口中所有二级url
  const oauthChildUrls = new Set()

  const routes = []

  for (const parentMenu of menuData) {
    // 过滤 status=0 的一级菜单
    if (parentMenu.status === 0) {
      matchedInfo.disabled.push({
        name: parentMenu.name,
        url: parentMenu.url,
        reason: '一级菜单 status=0'
      })
      continue
    }

    // Dashboard 首页特殊处理
    if (normalizeUrl(parentMenu.url) === 'Dashboard') {
      matchedInfo.matched.push({
        name: parentMenu.name,
        url: parentMenu.url,
        routePath: '/dashboard',
        component: 'views/dashboard'
      })
      continue
    }

    // 一级菜单：构建Layout容器
    const parentPath = parentMenu.url
    const children = []

    if (Array.isArray(parentMenu.children) && parentMenu.children.length > 0) {
      // 按 sort 排序
      const sortedChildren = [...parentMenu.children].sort((a, b) => (a.sort || 0) - (b.sort || 0))

      for (const childMenu of sortedChildren) {
        const childUrl = normalizeUrl(childMenu.url)
        oauthChildUrls.add(childUrl)

        // 过滤 status=0 的二级菜单
        if (childMenu.status === 0) {
          matchedInfo.disabled.push({
            name: childMenu.name,
            url: childMenu.url,
            parentName: parentMenu.name,
            reason: '二级菜单 status=0'
          })
          continue
        }

        // 在映射表中查找组件
        if (ROUTE_COMPONENT_MAP[childUrl]) {
          // 匹配成功：使用真实组件
          children.push({
            path: childUrl,
            component: ROUTE_COMPONENT_MAP[childUrl],
            name: childUrl,
            meta: {
              title: childMenu.name,
              _oauthMenuId: childMenu.id,
              _matched: true
            }
          })
          matchedInfo.matched.push({
            name: childMenu.name,
            url: childMenu.url,
            routePath: `${parentPath}/${childUrl}`,
            component: 'real'
          })
        } else {
          // 未匹配：使用占位页面
          children.push({
            path: childUrl,
            component: () => import('@/views/placeholder/index'),
            name: `placeholder_${childUrl}`,
            meta: {
              title: childMenu.name,
              _oauthMenuId: childMenu.id,
              _matched: false,
              _placeholder: true
            }
          })
          matchedInfo.placeholder.push({
            name: childMenu.name,
            url: childMenu.url,
            normalizedUrl: childUrl,
            parentName: parentMenu.name,
            routePath: `${parentPath}/${childUrl}`,
            reason: '映射表中无此url对应的组件'
          })
        }
      }
    }

    // 只有一级菜单有子菜单时才生成路由
    if (children.length > 0) {
      routes.push({
        path: parentPath,
        component: Layout,
        alwaysShow: true,
        name: normalizeUrl(parentPath),
        meta: {
          title: parentMenu.name,
          icon: parentMenu.imgurl || 'component',
          _oauthMenuId: parentMenu.id
        },
        children
      })
    }
  }

  // 找出路由存在但接口未返回的菜单
  const allRouteUrls = Object.keys(ROUTE_COMPONENT_MAP)
  for (const url of allRouteUrls) {
    if (!oauthChildUrls.has(url)) {
      matchedInfo.routeOnly.push({
        url,
        reason: '路由映射表中存在，但接口未返回此url'
      })
    }
  }

  return { routes, matchedInfo }
}

/**
 * 打印映射结果到控制台（调试用）
 * @param {Object} matchedInfo
 */
export function printMatchedInfo(matchedInfo) {
  console.group('%c[OAuth菜单映射结果]', 'color: #409EFF; font-weight: bold;')

  console.group('%c✅ 映射成功', 'color: #67C23A;')
  console.table(matchedInfo.matched)
  console.groupEnd()

  console.group('%c⚠️ 占位页面（接口有但路由不存在）', 'color: #E6A23C;')
  if (matchedInfo.placeholder.length > 0) {
    console.table(matchedInfo.placeholder)
  } else {
    console.log('无')
  }
  console.groupEnd()

  console.group('%c🔵 路由独有（路由存在但接口未返回）', 'color: #909399;')
  if (matchedInfo.routeOnly.length > 0) {
    console.table(matchedInfo.routeOnly)
  } else {
    console.log('无')
  }
  console.groupEnd()

  console.group('%c🚫 已禁用（status=0）', 'color: #F56C6C;')
  if (matchedInfo.disabled.length > 0) {
    console.table(matchedInfo.disabled)
  } else {
    console.log('无')
  }
  console.groupEnd()

  console.groupEnd()
}
