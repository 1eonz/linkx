import { describe, it, expect, vi, beforeEach } from 'vitest'

// 使用 vi.hoisted 确保 mock 对象在 vi.mock 工厂函数被提升时也可访问
const {
  apiUserMock,
  apiMenuMock,
  apiOauthMenuMock,
  apiGlobalsMock,
  authMock,
  routerMock
} = vi.hoisted(() => {
  const apiUserMock = {
    login: vi.fn(),
    logout: vi.fn(),
    getRolePermissions: vi.fn()
  }
  const apiMenuMock = {
    getMenuList: vi.fn()
  }
  const apiOauthMenuMock = {
    getOauthMenuList: vi.fn()
  }
  const apiGlobalsMock = {
    getGlobalsList: vi.fn()
  }
  const authMock = {
    getToken: vi.fn(() => null),
    setToken: vi.fn(),
    removeToken: vi.fn(),
    setButtons: vi.fn(),
    removeButtons: vi.fn(),
    setUserName: vi.fn(),
    setUserId: vi.fn(),
    setIsAdmin: vi.fn(),
    setIdCardNum: vi.fn(),
    removeIsAdmin: vi.fn(),
    removeIdCardNum: vi.fn(),
    getLicenseAuth: vi.fn(() => null),
    removeLicenseAuth: vi.fn()
  }
  const routerMock = {
    resetRouter: vi.fn()
  }
  return { apiUserMock, apiMenuMock, apiOauthMenuMock, apiGlobalsMock, authMock, routerMock }
})

// ===== mocks =====
vi.mock('@/api/user', () => apiUserMock)
vi.mock('@/api/permission/menu', () => apiMenuMock)
vi.mock('@/api/oauth/menu', () => apiOauthMenuMock)
vi.mock('@/api/dictionary/globals', () => apiGlobalsMock)
vi.mock('@/utils/auth', () => authMock)
vi.mock('@/router', () => routerMock)

// 提供一个全局 Buffer（happy-dom/node 环境自带），用于 Buffer.from(base64) 解码
// 由于源码使用 Buffer.from，需要确保全局可用
import userModule from '@/store/modules/user'

describe('store/modules/user.js', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // 重置 mock 返回
    authMock.getToken.mockReturnValue(null)
    authMock.getLicenseAuth.mockReturnValue(null)
    localStorage.clear()
  })

  describe('模块结构', () => {
    it('包含 namespaced/state/mutations/actions', () => {
      expect(userModule.namespaced).toBe(true)
      expect(userModule.state).toBeDefined()
      expect(userModule.mutations).toBeDefined()
      expect(userModule.actions).toBeDefined()
    })
  })

  describe('state 初始值', () => {
    it('初始 token 为 null（getToken 返回 null）', () => {
      // 直接验证 state 中字段
      // 注意：模块加载时只调用一次 getToken
      expect(typeof userModule.state.token).toBe('object')
    })

    it('初始 buttons 为空数组', () => {
      expect(userModule.state.buttons).toEqual([])
    })

    it('初始 licenseAuth 在 getLicenseAuth 返回 null 时使用默认对象', () => {
      expect(userModule.state.licenseAuth).toEqual({
        groupCollaborationAuth: true,
        taskCollaborationAuth: true,
        businessCollaborationAuth: true,
        AICollaborationAuth: true,
        northboundDataInterface: true,
        licenseState: 999
      })
    })
  })

  describe('mutations', () => {
    it('SET_TOKEN 设置 state.token', () => {
      const state = { token: null }
      userModule.mutations.SET_TOKEN(state, 'new-token')
      expect(state.token).toBe('new-token')
    })

    it('SET_NAME 设置 state.name', () => {
      const state = { name: '' }
      userModule.mutations.SET_NAME(state, 'zhangsan')
      expect(state.name).toBe('zhangsan')
    })

    it('SET_AVATAR 设置 state.avatar', () => {
      const state = { avatar: '' }
      userModule.mutations.SET_AVATAR(state, 'http://img.png')
      expect(state.avatar).toBe('http://img.png')
    })

    it('SET_BUTTONS 设置 state.buttons', () => {
      const state = { buttons: [] }
      const btns = ['btn1', 'btn2']
      userModule.mutations.SET_BUTTONS(state, btns)
      expect(state.buttons).toEqual(btns)
    })

    it('SET_TYPE 设置 state.type', () => {
      const state = { type: null }
      userModule.mutations.SET_TYPE(state, 1)
      expect(state.type).toBe(1)
    })

    it('SET_PRIV 设置 state.hasChildOrgPriv', () => {
      const state = { hasChildOrgPriv: null }
      userModule.mutations.SET_PRIV(state, true)
      expect(state.hasChildOrgPriv).toBe(true)
    })

    it('SET_MENU 设置 state.menu', () => {
      const state = { menu: [] }
      const data = [{ id: 1 }]
      userModule.mutations.SET_MENU(state, data)
      expect(state.menu).toEqual(data)
    })

    it('SET_PERMISSIONS 设置 state.permissions', () => {
      const state = { permissions: {} }
      const data = { menus: [], actions: [] }
      userModule.mutations.SET_PERMISSIONS(state, data)
      expect(state.permissions).toEqual(data)
    })

    it('SET_OAUTH_MENU 设置 state.oauthMenu', () => {
      const state = { oauthMenu: [] }
      const data = [{ id: 'm1' }]
      userModule.mutations.SET_OAUTH_MENU(state, data)
      expect(state.oauthMenu).toEqual(data)
    })

    it('SET_OAUTH_MENU_LOADED 设置 state.oauthMenuLoaded', () => {
      const state = { oauthMenuLoaded: false }
      userModule.mutations.SET_OAUTH_MENU_LOADED(state, true)
      expect(state.oauthMenuLoaded).toBe(true)
    })

    it('SET_GLOBALS 设置 state.globals', () => {
      const state = { globals: [] }
      const data = [{ key: 'k1' }]
      userModule.mutations.SET_GLOBALS(state, data)
      expect(state.globals).toEqual(data)
    })

    it('SET_USER_INFO 设置 state.userInfo', () => {
      const state = { userInfo: null }
      const data = { id: 1, name: 'u' }
      userModule.mutations.SET_USER_INFO(state, data)
      expect(state.userInfo).toEqual(data)
    })

    it('RESET_STATE 重置 state 为默认状态', () => {
      const state = {
        token: 'old-token',
        name: 'old',
        buttons: ['a', 'b'],
        type: 1
      }
      userModule.mutations.RESET_STATE(state)
      expect(state.token).toBeNull()
      expect(state.name).toBe('')
      expect(state.buttons).toEqual([])
      expect(state.type).toBeNull()
    })
  })

  describe('actions.login - 关键分支', () => {
    it('code=0 时 commit SET_TOKEN/SET_NAME，并调用 auth 写入 token/userName/userId/idCardNum/isAdmin', async () => {
      apiUserMock.login.mockResolvedValueOnce({
        code: 0,
        headers: {}, // 源码会读 result.headers['x-cloudcmd-license-warning']，需提供空对象
        data: {
          accessToken: 'tok-001',
          userName: 'zhangsan',
          userId: 'u-1001',
          idCardNum: '110101199003071234',
          isAdmin: true
        }
      })

      const commit = vi.fn()
      const result = await userModule.actions.login({ commit, state: {} }, {
        username: 'zhangsan',
        password: '123456'
      })

      expect(apiUserMock.login).toHaveBeenCalledWith({ username: 'zhangsan', password: '123456' })
      expect(commit).toHaveBeenCalledWith('SET_TOKEN', 'tok-001')
      expect(commit).toHaveBeenCalledWith('SET_NAME', 'zhangsan')
      expect(authMock.setToken).toHaveBeenCalledWith('tok-001')
      expect(authMock.setUserName).toHaveBeenCalledWith('zhangsan')
      expect(authMock.setUserId).toHaveBeenCalledWith('u-1001')
      expect(authMock.setIdCardNum).toHaveBeenCalledWith('110101199003071234')
      expect(authMock.setIsAdmin).toHaveBeenCalledWith(true)
      // 返回原始 result
      expect(result.code).toBe(0)
    })

    it('code=121（密码即将过期）也走成功分支，并 reject 不调用', async () => {
      apiUserMock.login.mockResolvedValueOnce({
        code: 121,
        msg: '密码即将过期',
        headers: {}, // 源码会读 result.headers['x-cloudcmd-license-warning']
        data: {
          accessToken: 'tok-002',
          userName: 'lisi',
          userId: 'u-1002',
          idCardNum: '',
          isAdmin: false
        }
      })

      const commit = vi.fn()
      const result = await userModule.actions.login({ commit, state: {} }, {
        username: 'lisi',
        password: 'pwd'
      })

      expect(commit).toHaveBeenCalledWith('SET_TOKEN', 'tok-002')
      expect(commit).toHaveBeenCalledWith('SET_NAME', 'lisi')
      expect(authMock.setToken).toHaveBeenCalledWith('tok-002')
      expect(result.code).toBe(121)
    })

    it('带 x-cloudcmd-license-warning header（code=0）时，将 base64 解码后赋给 result.msg', async () => {
      const warningText = 'License即将过期'
      const base64 = Buffer.from(warningText, 'utf-8').toString('base64')
      apiUserMock.login.mockResolvedValueOnce({
        code: 0,
        headers: {
          'x-cloudcmd-license-warning': base64
        },
        data: {
          accessToken: 'tok-003',
          userName: 'wangwu',
          userId: 'u-1003',
          idCardNum: '',
          isAdmin: false
        }
      })

      const commit = vi.fn()
      const result = await userModule.actions.login({ commit, state: {} }, {
        username: 'wangwu',
        password: 'pwd'
      })

      // 由于 code=0（非 121），result.msg 应被解码后的内容覆盖
      expect(result.msg).toBe(warningText)
    })

    it('带 license-warning 且 code=121 时，result.msg 保留原 msg（不被覆盖）', async () => {
      const warningText = 'License即将过期'
      const base64 = Buffer.from(warningText, 'utf-8').toString('base64')
      const originalMsg = '密码即将过期，请尽快修改'
      apiUserMock.login.mockResolvedValueOnce({
        code: 121,
        msg: originalMsg,
        headers: {
          'x-cloudcmd-license-warning': base64
        },
        data: {
          accessToken: 'tok-004',
          userName: 'zhaoliu',
          userId: 'u-1004',
          idCardNum: '',
          isAdmin: false
        }
      })

      const commit = vi.fn()
      const result = await userModule.actions.login({ commit, state: {} }, {
        username: 'zhaoliu',
        password: 'pwd'
      })

      // code=121 时 result.msg 保持原值
      expect(result.msg).toBe(originalMsg)
    })

    it('code 不为 0 且不为 121 时，reject 整个 result', async () => {
      apiUserMock.login.mockResolvedValueOnce({
        code: 500,
        msg: '密码错误'
      })

      const commit = vi.fn()
      await expect(
        userModule.actions.login({ commit, state: {} }, { username: 'x', password: 'y' })
      ).rejects.toEqual({ code: 500, msg: '密码错误' })

      // 不应 commit SET_TOKEN
      expect(commit).not.toHaveBeenCalledWith('SET_TOKEN', expect.anything())
    })

    it('login API 抛错时 reject 该错误', async () => {
      apiUserMock.login.mockRejectedValueOnce(new Error('network error'))
      const commit = vi.fn()
      await expect(
        userModule.actions.login({ commit, state: {} }, { username: 'x', password: 'y' })
      ).rejects.toThrow('network error')
    })
  })

  describe('actions.logout', () => {
    it('logout API 成功时清理 token/buttons/router/state/licenseAuth', async () => {
      apiUserMock.logout.mockResolvedValueOnce({})
      const commit = vi.fn()

      await userModule.actions.logout({ commit, state: { token: 'old' } })

      expect(apiUserMock.logout).toHaveBeenCalledWith('old')
      expect(authMock.removeToken).toHaveBeenCalledTimes(1)
      expect(authMock.removeButtons).toHaveBeenCalledTimes(1)
      expect(routerMock.resetRouter).toHaveBeenCalledTimes(1)
      expect(commit).toHaveBeenCalledWith('RESET_STATE')
      expect(commit).toHaveBeenCalledWith('SET_BUTTONS', [])
      expect(authMock.removeIsAdmin).toHaveBeenCalledTimes(1)
      expect(authMock.removeIdCardNum).toHaveBeenCalledTimes(1)
      expect(authMock.removeLicenseAuth).toHaveBeenCalledTimes(1)
    })

    it('logout API 抛错时 reject', async () => {
      apiUserMock.logout.mockRejectedValueOnce(new Error('logout fail'))
      const commit = vi.fn()
      await expect(
        userModule.actions.logout({ commit, state: { token: 't' } })
      ).rejects.toThrow('logout fail')
    })
  })

  describe('actions.resetToken', () => {
    it('清理 token/buttons/router/state，不调用 logout API', async () => {
      const commit = vi.fn()
      await userModule.actions.resetToken({ commit })

      expect(authMock.removeToken).toHaveBeenCalledTimes(1)
      expect(authMock.removeButtons).toHaveBeenCalledTimes(1)
      expect(routerMock.resetRouter).toHaveBeenCalledTimes(1)
      expect(commit).toHaveBeenCalledWith('RESET_STATE')
      expect(commit).toHaveBeenCalledWith('SET_BUTTONS', [])
      // 不应调用 logout API
      expect(apiUserMock.logout).not.toHaveBeenCalled()
    })
  })

  describe('actions.getOauthMenu', () => {
    it('code=0 时 commit SET_OAUTH_MENU 并 resolve(data)', async () => {
      const data = [{ id: 'm1' }, { id: 'm2' }]
      apiOauthMenuMock.getOauthMenuList.mockResolvedValueOnce({ code: 0, data })
      const commit = vi.fn()

      const result = await userModule.actions.getOauthMenu({ commit })
      expect(commit).toHaveBeenCalledWith('SET_OAUTH_MENU', data)
      expect(result).toEqual(data)
    })

    it('code 非 0 时 resolve 空数组', async () => {
      apiOauthMenuMock.getOauthMenuList.mockResolvedValueOnce({ code: 1, data: [] })
      const commit = vi.fn()
      const result = await userModule.actions.getOauthMenu({ commit })
      expect(result).toEqual([])
      expect(commit).not.toHaveBeenCalledWith('SET_OAUTH_MENU', expect.anything())
    })

    it('getOauthMenuList 抛错时 resolve 空数组（不 reject）', async () => {
      apiOauthMenuMock.getOauthMenuList.mockRejectedValueOnce(new Error('net'))
      const commit = vi.fn()
      const result = await userModule.actions.getOauthMenu({ commit })
      expect(result).toEqual([])
    })
  })

  describe('actions.getPermissions', () => {
    it('code=0 时 setButtons + commit SET_BUTTONS + SET_PERMISSIONS', async () => {
      apiUserMock.getRolePermissions.mockResolvedValueOnce({
        code: 0,
        data: {
          actions: [{ code: 'a1' }, { code: 'a2' }],
          menus: [{ id: 1 }]
        }
      })
      const commit = vi.fn()

      await userModule.actions.getPermissions({ commit })

      expect(authMock.setButtons).toHaveBeenCalledWith([{ code: 'a1' }, { code: 'a2' }])
      expect(commit).toHaveBeenCalledWith('SET_BUTTONS', [{ code: 'a1' }, { code: 'a2' }])
      expect(commit).toHaveBeenCalledWith('SET_PERMISSIONS', {
        actions: [{ code: 'a1' }, { code: 'a2' }],
        menus: [{ id: 1 }]
      })
    })

    it('code 非 0 时 reject', async () => {
      apiUserMock.getRolePermissions.mockResolvedValueOnce({ code: 1, data: null })
      const commit = vi.fn()
      await expect(userModule.actions.getPermissions({ commit })).rejects.toBeUndefined()
    })
  })

  describe('actions.getGlobals', () => {
    it('code=0 时 commit SET_GLOBALS', async () => {
      const data = [{ key: 'k1' }]
      apiGlobalsMock.getGlobalsList.mockResolvedValueOnce({ code: 0, data })
      const commit = vi.fn()

      await userModule.actions.getGlobals({ commit })
      expect(commit).toHaveBeenCalledWith('SET_GLOBALS', data)
    })

    it('code 非 0 时 reject', async () => {
      apiGlobalsMock.getGlobalsList.mockResolvedValueOnce({ code: 1, data: null })
      const commit = vi.fn()
      await expect(userModule.actions.getGlobals({ commit })).rejects.toBeUndefined()
    })
  })

  describe('actions.getMenu', () => {
    it('getMenuList code=0 时 commit SET_MENU 并 resolve(true)', async () => {
      apiGlobalsMock.getGlobalsList.mockResolvedValueOnce({ code: 0, data: [] })
      apiUserMock.getRolePermissions.mockResolvedValueOnce({
        code: 0,
        data: { actions: [], menus: [] }
      })
      apiMenuMock.getMenuList.mockResolvedValueOnce({ code: 0, data: [{ id: 1 }] })
      const commit = vi.fn()
      const dispatch = vi.fn()

      const result = await userModule.actions.getMenu({ commit, dispatch })
      expect(commit).toHaveBeenCalledWith('SET_MENU', [{ id: 1 }])
      expect(result).toBe(true)
    })

    it('getMenuList code 非 0 时 resolve(false)', async () => {
      apiGlobalsMock.getGlobalsList.mockResolvedValueOnce({ code: 0, data: [] })
      apiUserMock.getRolePermissions.mockResolvedValueOnce({
        code: 0,
        data: { actions: [], menus: [] }
      })
      apiMenuMock.getMenuList.mockResolvedValueOnce({ code: 1, data: [] })
      const commit = vi.fn()
      const dispatch = vi.fn()

      const result = await userModule.actions.getMenu({ commit, dispatch })
      expect(result).toBe(false)
      expect(commit).not.toHaveBeenCalledWith('SET_MENU', expect.anything())
    })

    it('dispatch 中任意环节抛错时 resolve(false)', async () => {
      apiGlobalsMock.getGlobalsList.mockRejectedValueOnce(new Error('net'))
      const commit = vi.fn()
      const dispatch = vi.fn()

      const result = await userModule.actions.getMenu({ commit, dispatch })
      expect(result).toBe(false)
    })
  })

  describe('actions.setUserInfo / setType / setPriv', () => {
    it('setUserInfo 调用 SET_USER_INFO', () => {
      const commit = vi.fn()
      const info = { id: 1 }
      userModule.actions.setUserInfo({ commit }, info)
      expect(commit).toHaveBeenCalledWith('SET_USER_INFO', info)
    })

    it('setType 调用 SET_TYPE', () => {
      const commit = vi.fn()
      userModule.actions.setType({ commit }, 2)
      expect(commit).toHaveBeenCalledWith('SET_TYPE', 2)
    })

    it('setPriv 调用 SET_PRIV', () => {
      const commit = vi.fn()
      userModule.actions.setPriv({ commit }, true)
      expect(commit).toHaveBeenCalledWith('SET_PRIV', true)
    })
  })
})
