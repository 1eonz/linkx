import { describe, it, expect, vi, beforeEach } from 'vitest'

// mock 依赖：queryDepartment 用于递归测试
vi.mock('@/api/h5/collaboration', () => ({
  queryDepartment: vi.fn()
}))

// 真实使用 secure / crypto（基于 Buffer/CryptoJS，happy-dom + node 环境可运行）
import {
  getToken,
  setToken,
  removeToken,
  getUserId,
  setUserId,
  setButtons,
  removeButtons,
  setUserName,
  getUserName,
  setIsAdmin,
  getIsAdmin,
  removeIsAdmin,
  setIdCardNum,
  getIdCardNum,
  removeIdCardNum,
  setLicenseAuth,
  getLicenseAuth,
  removeLicenseAuth,
  getAllNodeIdByDepartmentId
} from '@/utils/auth'
import { queryDepartment } from '@/api/h5/collaboration'

describe('utils/auth.js', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  describe('token 读写', () => {
    it('初始时 getToken 返回 null', () => {
      expect(getToken()).toBeNull()
    })

    it('setToken 后 getToken 能取到值', () => {
      setToken('abc-token-123')
      expect(getToken()).toBe('abc-token-123')
    })

    it('removeToken 后 getToken 返回 null', () => {
      setToken('will-be-removed')
      removeToken()
      expect(getToken()).toBeNull()
    })
  })

  describe('userId 读写', () => {
    it('setUserId 后 getUserId 能取到值', () => {
      setUserId('u-1001')
      expect(getUserId()).toBe('u-1001')
    })

    it('初始时 getUserId 返回 null', () => {
      expect(getUserId()).toBeNull()
    })
  })

  describe('buttons 读写', () => {
    it('setButtons 写入字符串', () => {
      setButtons('btn1,btn2,btn3')
      expect(localStorage.getItem('buttons')).toBe('btn1,btn2,btn3')
    })

    it('removeButtons 清空后 localStorage 无值', () => {
      setButtons('btn1,btn2')
      removeButtons()
      expect(localStorage.getItem('buttons')).toBeNull()
    })
  })

  describe('userName 读写', () => {
    it('setUserName / getUserName 配对', () => {
      setUserName('zhangsan')
      expect(getUserName()).toBe('zhangsan')
    })
  })

  describe('isAdmin 读写', () => {
    it('setIsAdmin 写入 true 后 getIsAdmin 返回 true（JSON.parse）', () => {
      setIsAdmin(true)
      expect(getIsAdmin()).toBe(true)
    })

    it('setIsAdmin 写入 false 后 getIsAdmin 返回 false', () => {
      setIsAdmin(false)
      expect(getIsAdmin()).toBe(false)
    })

    it('removeIsAdmin 后 localStorage 无值', () => {
      setIsAdmin(true)
      removeIsAdmin()
      expect(localStorage.getItem('is_admin')).toBeNull()
    })
  })

  describe('idCardNum 加密读写', () => {
    it('setIdCardNum 写入后 getIdCardNum 能解出原值', () => {
      setIdCardNum('110101199003071234')
      expect(getIdCardNum()).toBe('110101199003071234')
    })

    it('removeIdCardNum 后 localStorage 无值', () => {
      setIdCardNum('110101199003071234')
      removeIdCardNum()
      expect(localStorage.getItem('id_card_num')).toBeNull()
    })

    it('写入空值不会抛错，且 getIdCardNum 返回空串', () => {
      setIdCardNum('')
      expect(getIdCardNum()).toBe('')
    })
  })

  describe('licenseAuth 加密读写', () => {
    it('setLicenseAuth 写入对象后 getLicenseAuth 能解出原对象', () => {
      const obj = {
        groupCollaborationAuth: true,
        licenseState: 1,
        expireDate: '2025-12-31'
      }
      setLicenseAuth(obj)
      expect(getLicenseAuth()).toEqual(obj)
    })

    it('removeLicenseAuth 后 localStorage 无值', () => {
      setLicenseAuth({ licenseState: 0 })
      removeLicenseAuth()
      expect(localStorage.getItem('license_auth')).toBeNull()
    })
  })

  describe('getAllNodeIdByDepartmentId 递归', () => {
    it('多层嵌套返回时收集所有 id 并以逗号拼接', async () => {
      // 模拟：
      //   根 code = 'ROOT' -> [{id:2, code:'A'}, {id:3, code:'B'}]
      //   code 'A' -> [{id:4, code:'A1'}]
      //   code 'A1' -> []
      //   code 'B' -> [{id:5, code:null}]
      // 递归顺序（深度优先）：
      //   ROOT -> [id=2(code=A), id=3(code=B)]
      //   先 push id=2，再递归 A -> [id=4(code=A1)]  → push id=4，递归 A1 -> [] 返回
      //   回到外层 push id=3，再递归 B -> [id=5(code=null)] → push id=5，无 code 不递归
      //   最终顺序: 1,2,4,3,5
      queryDepartment
        .mockResolvedValueOnce({
          data: [
            { id: 2, code: 'A' },
            { id: 3, code: 'B' }
          ]
        })
        // 第 2 次调用：fetchAndCollectIds('A') → 返回 [{id:4, code:'A1'}]
        .mockResolvedValueOnce({ data: [{ id: 4, code: 'A1' }] })
        // 第 3 次调用：fetchAndCollectIds('A1') → 返回 []
        .mockResolvedValueOnce({ data: [] })
        // 第 4 次调用：fetchAndCollectIds('B') → 返回 [{id:5, code:null}]
        .mockResolvedValueOnce({ data: [{ id: 5, code: null }] })

      const result = await getAllNodeIdByDepartmentId(1, 'ROOT')
      // 深度优先递归顺序：1(ROOT) -> 2 -> 4(A子节点) -> 3 -> 5(B子节点)
      expect(result).toBe('1,2,4,3,5')
      // 调用次数 = 1(ROOT) + 1(A) + 1(A1) + 1(B) = 4 次
      expect(queryDepartment).toHaveBeenCalledTimes(4)
    })

    it('空子部门（首次查询返回空数组）只返回根 id', async () => {
      queryDepartment.mockResolvedValueOnce({ data: [] })
      const result = await getAllNodeIdByDepartmentId(99, 'ROOT')
      expect(result).toBe('99')
      expect(queryDepartment).toHaveBeenCalledTimes(1)
    })

    it('接口返回 data 为 null 时只返回根 id', async () => {
      queryDepartment.mockResolvedValueOnce({ data: null })
      const result = await getAllNodeIdByDepartmentId(99, 'ROOT')
      expect(result).toBe('99')
    })

    it('接口抛异常时被 catch，仍返回根 id（不影响最终返回）', async () => {
      queryDepartment.mockRejectedValueOnce(new Error('network error'))
      const result = await getAllNodeIdByDepartmentId(7, 'ROOT')
      expect(result).toBe('7')
    })

    it('部门对象缺少 code 字段时不递归，但仍收集 id', async () => {
      queryDepartment.mockResolvedValueOnce({
        data: [
          { id: 10 }, // 无 code
          { id: 11, code: 'C' }
        ]
      })
      queryDepartment.mockResolvedValueOnce({ data: [] })

      const result = await getAllNodeIdByDepartmentId(1, 'ROOT')
      expect(result).toBe('1,10,11')
    })
  })
})
