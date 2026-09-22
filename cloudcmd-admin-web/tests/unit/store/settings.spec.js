import { describe, it, expect, vi, beforeEach } from 'vitest'

// mock @/settings
vi.mock('@/settings', () => ({
  __esModule: true,
  default: {
    title: 'Test',
    showSettings: true,
    tagsView: true,
    fixedHeader: false,
    sidebarLogo: false,
    supportPinyinSearch: false
  }
}))

import settingsModule from '@/store/modules/settings'

describe('store/modules/settings.js', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('模块结构', () => {
    it('包含 namespaced/state/mutations/actions', () => {
      expect(settingsModule.namespaced).toBe(true)
      expect(settingsModule.state).toBeDefined()
      expect(settingsModule.mutations).toBeDefined()
      expect(settingsModule.actions).toBeDefined()
    })
  })

  describe('state 初始值', () => {
    it('从 mock 的 settings 读取 fixedHeader/sidebarLogo/showSettings', () => {
      // state 是模块加载时一次性计算的，mock 已生效
      expect(settingsModule.state.fixedHeader).toBe(false)
      expect(settingsModule.state.sidebarLogo).toBe(false)
      expect(settingsModule.state.showSettings).toBe(true)
    })

    it('systemName 默认为 localStorage 中的值，无值时为 "警务协同"', () => {
      // 此模块在 import 时已计算 state，由于 localStorage 未设置 SYSTEM_NAME
      // 这里验证默认行为
      expect(settingsModule.state.systemName).toBe('警务协同')
    })
  })

  describe('mutations.CHANGE_SETTING', () => {
    it('key 存在于 state 时更新对应字段', () => {
      const state = {
        showSettings: true,
        fixedHeader: false,
        sidebarLogo: false,
        systemName: '警务协同'
      }
      settingsModule.mutations.CHANGE_SETTING(state, { key: 'fixedHeader', value: true })
      expect(state.fixedHeader).toBe(true)
    })

    it('key 不存在时通过 hasOwnProperty 守卫，不写入新字段', () => {
      const state = {
        showSettings: true,
        fixedHeader: false,
        sidebarLogo: false,
        systemName: '警务协同'
      }
      settingsModule.mutations.CHANGE_SETTING(state, { key: 'notExistKey', value: 123 })
      expect(state).not.toHaveProperty('notExistKey')
    })

    it('key 为 systemName 时也可被更新（已存在字段）', () => {
      const state = {
        showSettings: true,
        fixedHeader: false,
        sidebarLogo: false,
        systemName: '警务协同'
      }
      settingsModule.mutations.CHANGE_SETTING(state, { key: 'systemName', value: '新系统名' })
      expect(state.systemName).toBe('新系统名')
    })
  })

  describe('mutations.SET_SYSTEM_NAME', () => {
    it('更新 state.systemName 并写入 localStorage', () => {
      const state = { systemName: '旧名称' }
      settingsModule.mutations.SET_SYSTEM_NAME(state, '新名称')
      expect(state.systemName).toBe('新名称')
      expect(localStorage.getItem('SYSTEM_NAME')).toBe('新名称')
    })

    it('传入空字符串也允许更新', () => {
      const state = { systemName: '旧名称' }
      settingsModule.mutations.SET_SYSTEM_NAME(state, '')
      expect(state.systemName).toBe('')
      expect(localStorage.getItem('SYSTEM_NAME')).toBe('')
    })
  })

  describe('actions', () => {
    it('changeSetting 调用 CHANGE_SETTING 透传 { key, value }', () => {
      const commit = vi.fn()
      settingsModule.actions.changeSetting({ commit }, { key: 'fixedHeader', value: true })
      expect(commit).toHaveBeenCalledWith('CHANGE_SETTING', { key: 'fixedHeader', value: true })
    })

    it('setSystemName 调用 SET_SYSTEM_NAME 透传 name', () => {
      const commit = vi.fn()
      settingsModule.actions.setSystemName({ commit }, '新系统名')
      expect(commit).toHaveBeenCalledWith('SET_SYSTEM_NAME', '新系统名')
    })
  })

  describe('action 与 mutation 联动', () => {
    it('changeSetting → CHANGE_SETTING：state 字段变更', () => {
      const state = {
        showSettings: true,
        fixedHeader: false,
        sidebarLogo: false,
        systemName: '警务协同'
      }
      const commit = (type, payload) => settingsModule.mutations[type](state, payload)
      settingsModule.actions.changeSetting({ commit }, { key: 'sidebarLogo', value: true })
      expect(state.sidebarLogo).toBe(true)
    })

    it('setSystemName → SET_SYSTEM_NAME：state + localStorage 同时变更', () => {
      const state = { systemName: '旧' }
      const commit = (type, payload) => settingsModule.mutations[type](state, payload)
      settingsModule.actions.setSystemName({ commit }, '协同平台')
      expect(state.systemName).toBe('协同平台')
      expect(localStorage.getItem('SYSTEM_NAME')).toBe('协同平台')
    })
  })
})
