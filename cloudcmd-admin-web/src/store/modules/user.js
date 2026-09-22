import { login, logout, getRolePermissions } from '@/api/user'
import {
  getToken,
  setToken,
  removeToken,
  setButtons,
  removeButtons,
  setUserName,
  setUserId,
  setIsAdmin,
  setIdCardNum,
  removeIsAdmin,
  removeIdCardNum,
  getLicenseAuth,
  removeLicenseAuth
} from '@/utils/auth'
import { resetRouter } from '@/router'
import { getMenuList } from '@/api/permission/menu'
import { getOauthMenuList } from '@/api/oauth/menu'
import { getGlobalsList } from '@/api/dictionary/globals'

const getDefaultState = () => {
  return {
    token: getToken(),
    name: '',
    avatar: '',
    buttons: [],
    type: null,
    hasChildOrgPriv: null,
    menu: [],
    permissions: {
      menus: [],
      actions: [],
    },
    permissionsMenu: [],
    oauthMenu: [],       // oauth菜单数据（新逻辑，并行运行）
    oauthMenuLoaded: false, // oauth菜单是否已加载完成
    globals: [],
    userInfo: null,
    licenseAuth: getLicenseAuth() || {
      groupCollaborationAuth: true, // 群组协同lecense权限
      taskCollaborationAuth: true, // 任务协同lecense权限
      businessCollaborationAuth: true, // 业务协同lecense权限
      AICollaborationAuth: true, // AI协同lecense权限
      northboundDataInterface: true, // 北向数据接口lecense权限
      licenseState: 999, // 0 未激活，1 激活，2 即将过期，3 已过期，4 失效可试用，5 失效
    },
  }
}

const state = getDefaultState()

const mutations = {
  RESET_STATE: (state) => {
    Object.assign(state, getDefaultState())
  },
  SET_TOKEN: (state, token) => {
    state.token = token
  },
  SET_NAME: (state, name) => {
    state.name = name
  },
  SET_AVATAR: (state, avatar) => {
    state.avatar = avatar
  },
  SET_BUTTONS: (state, buttons) => {
    state.buttons = buttons
  },
  SET_TYPE: (state, type) => {
    state.type = type
  },
  SET_PRIV: (state, priv) => {
    state.hasChildOrgPriv = priv
  },
  SET_MENU: (state, data) => {
    state.menu = data
  },
  SET_PERMISSIONS: (state, data) => {
    state.permissions = data
  },
  SET_PERMISSIONS_MENU: (state, data) => {
    state.permissionsMenu = data
  },
  SET_OAUTH_MENU: (state, data) => {
    state.oauthMenu = data
  },
  SET_OAUTH_MENU_LOADED: (state, loaded) => {
    state.oauthMenuLoaded = loaded
  },
  SET_GLOBALS: (state, data) => {
    state.globals = data
  },
  SET_USER_INFO: (state, userInfo) => {
    state.userInfo = userInfo
  },
}

const actions = {
  // user login
  login({ commit, state }, userInfo) {
    return new Promise((resolve, reject) => {
      login(userInfo)
        .then(async (result) => {
          // 登录成功或者密码即将过期情况都算登录 add fsj
          if (result.code === 0 || result.code === 121) {
            // license 提示
            const licenseWarning = result.headers['x-cloudcmd-license-warning']
            if (licenseWarning) {
              const msg = Buffer.from(licenseWarning, 'base64')
                .toString()
                .toString()
              result.msg = result.code === 121 ? result.msg : msg
            }
            const data = result.data
            console.log('login result:', data)
            commit('SET_TOKEN', data.accessToken)
            commit('SET_NAME', data.userName)
            setToken(data.accessToken)
            setUserName(data.userName)
            setUserId(data.userId)
            // 存储身份证号
            setIdCardNum(data.idCardNum)
            // 是否超级管理员
            setIsAdmin(data.isAdmin)
            resolve(result)
          } else {
            reject(result)
          }
        })
        .catch((error) => {
          reject(error)
        })
    })
  },
  // user logout
  logout({ commit, state }) {
    return new Promise((resolve, reject) => {
      logout(state.token)
        .then(() => {
          removeToken() // must remove  token  first
          removeButtons()
          resetRouter()
          commit('RESET_STATE')
          commit('SET_BUTTONS', [])
          // 清楚本缓存 身份证 是否Admin
          removeIsAdmin()
          removeIdCardNum()
          // 清楚本缓存end
          removeLicenseAuth()
          resolve()
        })
        .catch((error) => {
          reject(error)
        })
    })
  },
  // remove token
  resetToken({ commit }) {
    return new Promise((resolve) => {
      removeToken() // must remove  token  first
      removeButtons()
      resetRouter()
      commit('RESET_STATE')
      commit('SET_BUTTONS', [])
      resolve()
    })
  },
  setUserInfo({ commit }, userInfo) {
    commit('SET_USER_INFO', userInfo)
  },
  setType({ commit }, data) {
    commit('SET_TYPE', data)
  },
  setPriv({ commit }, priv) {
    commit('SET_PRIV', priv)
  },
  getMenu({ commit, dispatch }) {
    return new Promise(async (resolve) => {
      try {
        await dispatch('getGlobals')
        await dispatch('getPermissions')

        const { code, data = [] } = await getMenuList({ applicationId: '' })
        if (code === 0) {
          commit('SET_MENU', data)
          resolve(true)
        }
        resolve(false)
      } catch (err) {
        resolve(false)
      }
    })
  },
  // oauth菜单获取（新逻辑，与现有getMenu并行运行，不影响现有菜单）
  getOauthMenu({ commit }) {
    return new Promise(async (resolve) => {
      try {
        const { code, data = [] } = await getOauthMenuList()
        if (code === 0) {
          commit('SET_OAUTH_MENU', data)
          console.log('[OAuth菜单] 原始数据:', data)
          resolve(data)
        } else {
          console.warn('[OAuth菜单] 接口返回code不为0:', code)
          resolve([])
        }
      } catch (err) {
        console.error('[OAuth菜单] 请求失败:', err)
        resolve([])
      }
    })
  },
  getPermissions({ commit }) {
    return new Promise(async (resolve, reject) => {
      const { code, data } = await getRolePermissions()
      if (code === 0) {
        const buttonAuthList = []
        data.actions?.forEach((item) => {
          buttonAuthList.push(item)
        })
        setButtons(buttonAuthList)
        commit('SET_BUTTONS', buttonAuthList)
        commit('SET_PERMISSIONS', data)
        resolve()
      } else {
        reject()
      }
    })
  },
  getGlobals({ commit }) {
    return new Promise(async (resolve, reject) => {
      const { code, data } = await getGlobalsList()
      if (code === 0) {
        commit('SET_GLOBALS', data)
        resolve()
      } else {
        reject()
      }
    })
  },
}

export default {
  namespaced: true,
  state,
  mutations,
  actions,
}
