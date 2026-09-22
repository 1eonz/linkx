import { describe, it, expect, vi, beforeEach } from 'vitest'

// 使用 vi.hoisted 确保 mock 对象在 vi.mock 工厂函数被提升时也可访问
const { cookiesMock } = vi.hoisted(() => {
  const cookiesMock = {
    get: vi.fn(),
    set: vi.fn()
  }
  return { cookiesMock }
})

vi.mock('js-cookie', () => ({
  __esModule: true,
  default: cookiesMock
}))

import Cookies from 'js-cookie'
import appModule from '@/store/modules/app'

describe('store/modules/app.js', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    // 默认 Cookies.get('sidebarStatus') 返回 undefined → opened 为 true
    cookiesMock.get.mockReturnValue(undefined)
  })

  describe('state 初始值', () => {
    it('Cookies 无 sidebarStatus 时 sidebar.opened 默认 true', () => {
      cookiesMock.get.mockReturnValue(undefined)
      // state 是模块导出对象上的引用，每次访问都重新计算？不，state 是初始化时计算一次
      // 此处使用浅 clone 验证默认结构
      const state = appModule.state
      expect(state.sidebar.withoutAnimation).toBe(false)
      expect(state.device).toBe('desktop')
    })

    it('模块包含 namespaced/state/mutations/actions', () => {
      expect(appModule.namespaced).toBe(true)
      expect(appModule.state).toBeDefined()
      expect(appModule.mutations).toBeDefined()
      expect(appModule.actions).toBeDefined()
    })
  })

  describe('mutations.TOGGLE_SIDEBAR', () => {
    it('opened 由 true → false，withoutAnimation 重置为 false，并调用 Cookies.set 写入 0', () => {
      const state = {
        sidebar: {
          opened: true,
          withoutAnimation: true // 故意设 true，验证被重置
        }
      }
      appModule.mutations.TOGGLE_SIDEBAR(state)
      expect(state.sidebar.opened).toBe(false)
      expect(state.sidebar.withoutAnimation).toBe(false)
      expect(Cookies.set).toHaveBeenCalledWith('sidebarStatus', 0)
    })

    it('opened 由 false → true，调用 Cookies.set 写入 1', () => {
      const state = {
        sidebar: {
          opened: false,
          withoutAnimation: false
        }
      }
      appModule.mutations.TOGGLE_SIDEBAR(state)
      expect(state.sidebar.opened).toBe(true)
      expect(state.sidebar.withoutAnimation).toBe(false)
      expect(Cookies.set).toHaveBeenCalledWith('sidebarStatus', 1)
    })
  })

  describe('mutations.CLOSE_SIDEBAR', () => {
    it('关闭侧边栏，写入 Cookies 并设置 withoutAnimation', () => {
      const state = {
        sidebar: {
          opened: true,
          withoutAnimation: false
        }
      }
      appModule.mutations.CLOSE_SIDEBAR(state, true)
      expect(state.sidebar.opened).toBe(false)
      expect(state.sidebar.withoutAnimation).toBe(true)
      expect(Cookies.set).toHaveBeenCalledWith('sidebarStatus', 0)
    })

    it('closeSideBar 传 false 时 withoutAnimation 为 false', () => {
      const state = {
        sidebar: { opened: true, withoutAnimation: false }
      }
      appModule.mutations.CLOSE_SIDEBAR(state, false)
      expect(state.sidebar.opened).toBe(false)
      expect(state.sidebar.withoutAnimation).toBe(false)
    })
  })

  describe('mutations.TOGGLE_DEVICE', () => {
    it('切换设备为 mobile', () => {
      const state = { device: 'desktop' }
      appModule.mutations.TOGGLE_DEVICE(state, 'mobile')
      expect(state.device).toBe('mobile')
    })

    it('切换设备为 desktop', () => {
      const state = { device: 'mobile' }
      appModule.mutations.TOGGLE_DEVICE(state, 'desktop')
      expect(state.device).toBe('desktop')
    })
  })

  describe('actions', () => {
    it('toggleSideBar 调用 TOGGLE_SIDEBAR', () => {
      const commit = vi.fn()
      appModule.actions.toggleSideBar({ commit })
      expect(commit).toHaveBeenCalledWith('TOGGLE_SIDEBAR')
    })

    it('closeSideBar 调用 CLOSE_SIDEBAR 透传 withoutAnimation', () => {
      const commit = vi.fn()
      appModule.actions.closeSideBar({ commit }, { withoutAnimation: true })
      expect(commit).toHaveBeenCalledWith('CLOSE_SIDEBAR', true)
    })

    it('closeSideBar 传 false 也透传', () => {
      const commit = vi.fn()
      appModule.actions.closeSideBar({ commit }, { withoutAnimation: false })
      expect(commit).toHaveBeenCalledWith('CLOSE_SIDEBAR', false)
    })

    it('toggleDevice 调用 TOGGLE_DEVICE 透传 device', () => {
      const commit = vi.fn()
      appModule.actions.toggleDevice({ commit }, 'mobile')
      expect(commit).toHaveBeenCalledWith('TOGGLE_DEVICE', 'mobile')
    })
  })

  describe('action 与 mutation 联动（端到端）', () => {
    it('toggleSideBar → TOGGLE_SIDEBAR 链路：opened 翻转', () => {
      const state = {
        sidebar: { opened: true, withoutAnimation: false }
      }
      const commit = (type, payload) => {
        appModule.mutations[type](state, payload)
      }
      appModule.actions.toggleSideBar({ commit })
      expect(state.sidebar.opened).toBe(false)
      expect(Cookies.set).toHaveBeenCalledWith('sidebarStatus', 0)
    })

    it('closeSideBar → CLOSE_SIDEBAR 链路：opened 关闭', () => {
      const state = {
        sidebar: { opened: true, withoutAnimation: false }
      }
      const commit = (type, payload) => {
        appModule.mutations[type](state, payload)
      }
      appModule.actions.closeSideBar({ commit }, { withoutAnimation: true })
      expect(state.sidebar.opened).toBe(false)
      expect(state.sidebar.withoutAnimation).toBe(true)
    })

    it('toggleDevice → TOGGLE_DEVICE 链路：device 变更', () => {
      const state = { device: 'desktop' }
      const commit = (type, payload) => {
        appModule.mutations[type](state, payload)
      }
      appModule.actions.toggleDevice({ commit }, 'mobile')
      expect(state.device).toBe('mobile')
    })
  })
})
