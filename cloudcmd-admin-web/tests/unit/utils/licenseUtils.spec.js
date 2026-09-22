import { describe, it, expect, vi, beforeEach } from 'vitest'

// mock API
vi.mock('@/api/license/index.js', () => ({
  getLicenseInfo: vi.fn()
}))

// mock auth.setLicenseAuth，便于断言
vi.mock('@/utils/auth', () => ({
  setLicenseAuth: vi.fn()
}))

import { getLicenseInfo } from '@/api/license/index.js'
import { setLicenseAuth } from '@/utils/auth'
import { getLicenseInfoUtil } from '@/utils/licenseUtils'

describe('utils/licenseUtils.js - getLicenseInfoUtil', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('code=0 且 status 命中 [1,2,4] 时，根据 5 个权限位设置对应字段', async () => {
    // LINKXBS=1 + LINKXGCF=0 → 群组协同 true
    // LINKXBS=1 + LINKXTCF=0 → 任务协同 true
    // LINKXBS=1 + LINKXBCF=0 → 业务协同 true
    // LINKXBS=1 + LINKXACF=0 → AI协同 true
    // LINKXBS=1 + LINKXNDI=0 → 北向接口 true
    getLicenseInfo.mockResolvedValueOnce({
      code: 0,
      data: {
        status: 1,
        LINKXBS: '1',
        LINKXGCF: '0',
        LINKXTCF: '0',
        LINKXBCF: '0',
        LINKXACF: '0',
        LINKXNDI: '0',
        expireDate: '2025-12-31'
      }
    })

    const result = await getLicenseInfoUtil()

    expect(result).toEqual({
      groupCollaborationAuth: true,
      taskCollaborationAuth: true,
      businessCollaborationAuth: true,
      AICollaborationAuth: true,
      northboundDataInterface: true,
      licenseState: 1,
      expireDate: '2025-12-31'
    })

    expect(setLicenseAuth).toHaveBeenCalledTimes(1)
    expect(setLicenseAuth).toHaveBeenCalledWith(result)
  })

  it('LINKXBS=0 时所有协同权限均为 false（不满足 LINKXBS=1 条件）', async () => {
    getLicenseInfo.mockResolvedValueOnce({
      code: 0,
      data: {
        status: 2,
        LINKXBS: '0',
        LINKXGCF: '0',
        LINKXTCF: '0',
        LINKXBCF: '0',
        LINKXACF: '0',
        LINKXNDI: '0',
        expireDate: ''
      }
    })

    const result = await getLicenseInfoUtil()

    expect(result.groupCollaborationAuth).toBe(false)
    expect(result.taskCollaborationAuth).toBe(false)
    expect(result.businessCollaborationAuth).toBe(false)
    expect(result.AICollaborationAuth).toBe(false)
    expect(result.northboundDataInterface).toBe(false)
    expect(result.licenseState).toBe(2)
    expect(result.expireDate).toBe('')
  })

  it('status 不在 [1,2,4] 内时（如 3），权限位全部保持默认 false', async () => {
    getLicenseInfo.mockResolvedValueOnce({
      code: 0,
      data: {
        status: 3, // 不命中
        LINKXBS: '1',
        LINKXGCF: '0',
        LINKXTCF: '0',
        LINKXBCF: '0',
        LINKXACF: '0',
        LINKXNDI: '0',
        expireDate: '2024-01-01'
      }
    })

    const result = await getLicenseInfoUtil()

    expect(result.groupCollaborationAuth).toBe(false)
    expect(result.taskCollaborationAuth).toBe(false)
    expect(result.businessCollaborationAuth).toBe(false)
    expect(result.AICollaborationAuth).toBe(false)
    expect(result.northboundDataInterface).toBe(false)
    expect(result.licenseState).toBe(3)
    expect(result.expireDate).toBe('2024-01-01')
  })

  it('code=0 但 authData 缺失（data 为 undefined）时，应抛出后由 try/catch 兜底，仍写默认值', async () => {
    getLicenseInfo.mockResolvedValueOnce({ code: 0 }) // 无 data 字段

    const result = await getLicenseInfoUtil()

    // 由于访问 authData.status 会抛错，进入 catch 分支
    // 但最终仍调用 setLicenseAuth 并返回默认 licenseAuth
    expect(result.groupCollaborationAuth).toBe(false)
    expect(result.taskCollaborationAuth).toBe(false)
    expect(result.businessCollaborationAuth).toBe(false)
    expect(result.AICollaborationAuth).toBe(false)
    expect(result.northboundDataInterface).toBe(false)
    expect(result.licenseState).toBe(999)
    expect(result.expireDate).toBe('')
    expect(setLicenseAuth).toHaveBeenCalledTimes(1)
  })

  it('code 不为 0 时走 else 分支，返回默认值', async () => {
    getLicenseInfo.mockResolvedValueOnce({ code: 1, msg: 'failed' })

    const result = await getLicenseInfoUtil()

    expect(result.licenseState).toBe(999)
    expect(result.groupCollaborationAuth).toBe(false)
    expect(setLicenseAuth).toHaveBeenCalledWith(result)
  })

  it('getLicenseInfo 抛错时被 catch，仍返回默认 licenseAuth', async () => {
    getLicenseInfo.mockRejectedValueOnce(new Error('network error'))

    const result = await getLicenseInfoUtil()

    expect(result.licenseState).toBe(999)
    expect(result.expireDate).toBe('')
    expect(result.groupCollaborationAuth).toBe(false)
    expect(setLicenseAuth).toHaveBeenCalledTimes(1)
  })

  it('单个权限位（LINKXGCF=1）不满足条件，群组协同应保持 false', async () => {
    getLicenseInfo.mockResolvedValueOnce({
      code: 0,
      data: {
        status: 4,
        LINKXBS: '1',
        LINKXGCF: '1', // 不为 '0'，不满足
        LINKXTCF: '0',
        LINKXBCF: '0',
        LINKXACF: '0',
        LINKXNDI: '0',
        expireDate: '2026-06-30'
      }
    })

    const result = await getLicenseInfoUtil()

    expect(result.groupCollaborationAuth).toBe(false)
    expect(result.taskCollaborationAuth).toBe(true)
    expect(result.businessCollaborationAuth).toBe(true)
    expect(result.AICollaborationAuth).toBe(true)
    expect(result.northboundDataInterface).toBe(true)
    expect(result.licenseState).toBe(4)
    expect(result.expireDate).toBe('2026-06-30')
  })

  it('status=2 即将过期时仍按权限位规则计算', async () => {
    getLicenseInfo.mockResolvedValueOnce({
      code: 0,
      data: {
        status: 2,
        LINKXBS: '1',
        LINKXGCF: '0',
        LINKXTCF: '1',
        LINKXBCF: '1',
        LINKXACF: '1',
        LINKXNDI: '1',
        expireDate: '2025-11-01'
      }
    })

    const result = await getLicenseInfoUtil()

    expect(result.groupCollaborationAuth).toBe(true)
    expect(result.taskCollaborationAuth).toBe(false)
    expect(result.businessCollaborationAuth).toBe(false)
    expect(result.AICollaborationAuth).toBe(false)
    expect(result.northboundDataInterface).toBe(false)
    expect(result.licenseState).toBe(2)
  })

  it('status=4 失效可试用场景权限位计算', async () => {
    getLicenseInfo.mockResolvedValueOnce({
      code: 0,
      data: {
        status: 4,
        LINKXBS: '1',
        LINKXGCF: '0',
        LINKXTCF: '0',
        LINKXBCF: '0',
        LINKXACF: '0',
        LINKXNDI: '0',
        expireDate: ''
      }
    })

    const result = await getLicenseInfoUtil()

    expect(result.groupCollaborationAuth).toBe(true)
    expect(result.taskCollaborationAuth).toBe(true)
    expect(result.businessCollaborationAuth).toBe(true)
    expect(result.AICollaborationAuth).toBe(true)
    expect(result.northboundDataInterface).toBe(true)
    expect(result.licenseState).toBe(4)
  })
})
