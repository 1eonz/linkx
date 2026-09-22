import { getLicenseInfo } from '@/api/license/index.js'
import { setLicenseAuth } from '@/utils/auth'
export async function getLicenseInfoUtil() {
  const licenseAuth = {
    groupCollaborationAuth: false, // 群组协同lecense权限
    taskCollaborationAuth: false, // 任务协同lecense权限
    businessCollaborationAuth: false, // 业务协同lecense权限
    AICollaborationAuth: false, // AI协同lecense权限
    northboundDataInterface: false, // 北向数据接口lecense权限
    licenseState: 999,
    expireDate: ''
  }
  try {
    const licenseData = await getLicenseInfo()
    if (licenseData.code === 0) {
      const authData = licenseData?.data
      if ([1, 2, 4].includes(+authData.status)) {
        if (authData?.LINKXBS === '1' && authData?.LINKXGCF === '0') {
          licenseAuth.groupCollaborationAuth = true
        }
        if (authData?.LINKXBS === '1' && authData?.LINKXTCF === '0') {
          licenseAuth.taskCollaborationAuth = true
        }
        if (authData?.LINKXBS === '1' && authData?.LINKXBCF === '0') {
          licenseAuth.businessCollaborationAuth = true
        }
        if (authData?.LINKXBS === '1' && authData?.LINKXACF === '0') {
          licenseAuth.AICollaborationAuth = true
        }
        if (authData?.LINKXBS === '1' && authData?.LINKXNDI === '0') {
          licenseAuth.northboundDataInterface = true
        }
      }
      licenseAuth.licenseState = authData?.status
      licenseAuth.expireDate = authData?.expireDate
    } else {
      console.log('LicenseInfo获取失败')
    }
  } catch (error) {
    console.log('LicenseInfo获取失败', error)
  }
  setLicenseAuth(licenseAuth)
  return licenseAuth
}

