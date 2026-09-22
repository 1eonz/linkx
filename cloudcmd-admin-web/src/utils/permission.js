import Store from '@/store'

export function hasBtnPermission(permission) {
  let myBtns = []
  if (Store.getters.buttons.length !== 0) {
    myBtns = Store.getters.buttons
  } else {
    myBtns = localStorage.getItem('buttons')
  }
  if (myBtns) {
    return myBtns.indexOf(permission) > -1
  }
}

/**
 * 递归查找菜单列表中是否存在指定 url 的菜单项
 * 用于判断用户是否拥有某个菜单权限
 * @param {Array} menuList - 菜单列表
 * @param {String} targetUrl - 目标 url（如 'layoutConfig/banner'）
 * @returns {Boolean}
 */
export function menuListHasUrl(menuList, targetUrl) {
  if (!Array.isArray(menuList) || !targetUrl) return false
  const normalizedTarget = targetUrl.replace(/^\//, '')
  for (const item of menuList) {
    const itemUrl = (item.url || '').replace(/^\//, '')
    if (itemUrl === normalizedTarget) return true
    if (item.children && item.children.length > 0) {
      if (menuListHasUrl(item.children, targetUrl)) return true
    }
  }
  return false
}
