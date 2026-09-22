import { describe, it, expect, vi, beforeEach } from 'vitest'

// 使用 vi.hoisted 确保 mock 对象在 vi.mock 工厂函数被提升时也可访问
const { storeMock } = vi.hoisted(() => {
  const storeMock = {
    getters: {
      buttons: 'btn1,btn2,btn3'
    }
  }
  return { storeMock }
})

vi.mock('@/store', () => ({
  __esModule: true,
  default: storeMock,
  getters: storeMock.getters
}))

import Store, { getters } from '@/store'
import { hasBtnPermission } from '@/utils/permission'

describe('utils/permission.js - hasBtnPermission', () => {
  beforeEach(() => {
    localStorage.clear()
    // 重置 store.getters.buttons 为非空字符串
    Store.getters.buttons = 'btn1,btn2,btn3'
    vi.clearAllMocks()
  })

  it('Store.getters.buttons 有值时优先使用 store 中的按钮列表', () => {
    expect(hasBtnPermission('btn1')).toBe(true)
    expect(hasBtnPermission('btn2')).toBe(true)
    expect(hasBtnPermission('btn3')).toBe(true)
    expect(hasBtnPermission('btnX')).toBe(false)
  })

  it('store 中匹配到子串时返回 true（基于 indexOf 实现）', () => {
    // 'btn1' 是 'btn1,btn2,btn3' 的子串
    expect(hasBtnPermission('btn1')).toBe(true)
    // 'btn' 是子串，按现有实现也会返回 true
    expect(hasBtnPermission('btn')).toBe(true)
  })

  it('Store.getters.buttons 长度为 0 时回退到 localStorage', () => {
    Store.getters.buttons = ''
    localStorage.setItem('buttons', 'localBtn1,localBtn2')

    expect(hasBtnPermission('localBtn1')).toBe(true)
    expect(hasBtnPermission('localBtn2')).toBe(true)
    expect(hasBtnPermission('btn1')).toBe(false)
  })

  it('store.getters.buttons 为空字符串且 localStorage 也无值时返回 undefined', () => {
    Store.getters.buttons = ''
    // localStorage 也无 'buttons' 键
    expect(hasBtnPermission('anyBtn')).toBeUndefined()
  })

  it('store 和 localStorage 都无值时（myBtns 为 null），返回 undefined', () => {
    Store.getters.buttons = ''
    localStorage.removeItem('buttons')
    expect(hasBtnPermission('anyBtn')).toBeUndefined()
  })

  it('store 为非空但传入权限不在列表中返回 false', () => {
    Store.getters.buttons = 'btn1,btn2'
    expect(hasBtnPermission('notExist')).toBe(false)
  })

  it('store.getters.buttons 为空数组字符串时也走回退逻辑', () => {
    // 长度为 0 的字符串触发回退
    Store.getters.buttons = ''
    localStorage.setItem('buttons', 'fallback')
    expect(hasBtnPermission('fallback')).toBe(true)
  })

  it('Store mock 正确导出 getters', () => {
    expect(getters).toBe(Store.getters)
    expect(getters.buttons).toBe('btn1,btn2,btn3')
  })
})
