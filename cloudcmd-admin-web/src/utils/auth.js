import { encryptIdCard, decryptIdCard } from '@/utils/secure'
import { encryptJson, decryptJson } from '@/utils/crypto'
import { queryDepartment } from '@/api/h5/collaboration'

const TokenKey = 'vue_admin_template_token'
const Buttons = 'buttons'
const UserName = 'back_username'

const IsAdmin = 'is_admin'
const IdCardNum = 'id_card_num'
const UserId = 'back_user_id'
const licenseAuthKey = 'license_auth'

export function getToken() {
  return localStorage.getItem(TokenKey)
}

export function setToken(token) {
  localStorage.setItem(TokenKey, token)
}

export function getUserId() {
  return localStorage.getItem(UserId)
}

export function setUserId(userId) {
  localStorage.setItem(UserId, userId)
}

export function removeToken() {
  return localStorage.removeItem(TokenKey)
}

export function setButtons(buttons) {
  localStorage.setItem(Buttons, buttons)
}

export function removeButtons() {
  return localStorage.removeItem(Buttons)
}

export function setUserName(username) {
  localStorage.setItem(UserName, username)
}

export function getUserName() {
  return localStorage.getItem(UserName)
}

export function setIsAdmin(isAdmin) {
  localStorage.setItem(IsAdmin, isAdmin)
}

export function setIdCardNum(idCardNum) {
  localStorage.setItem(IdCardNum, encryptIdCard(idCardNum))
}

export function getIsAdmin() {
  // 需要将字符串转换成boolean
  return JSON.parse(localStorage.getItem(IsAdmin))
}

export function getIdCardNum() {
  return decryptIdCard(localStorage.getItem(IdCardNum))
}

export function removeIsAdmin() {
  return localStorage.removeItem(IsAdmin)
}

export function removeIdCardNum() {
  return localStorage.removeItem(IdCardNum)
}

export function getLicenseAuth() {
  return JSON.parse(decryptJson(localStorage.getItem(licenseAuthKey)))
}

export function setLicenseAuth(obj) {
  localStorage.setItem(licenseAuthKey, encryptJson(JSON.stringify(obj)))
}

export function removeLicenseAuth() {
  return localStorage.removeItem(licenseAuthKey)
}

export const getAllNodeIdByDepartmentId = async (departmentId, departmentCode) => {
  // 用于存储所有 id
  const allIds = [departmentId]
  /**
   * 内部递归函数：根据 code 查询部门，并收集 id，然后递归查询子部门
   * @param {string} code - 当前部门编码
   */
  async function fetchAndCollectIds(code) {
    try {
      // 调用接口，获取当前 code 对应的部门列表（子部门）
      const res = await queryDepartment({ parentCode: code }) // 返回 Promise<any[]>
      const departments = res?.data
      if (!departments || departments.length === 0) {
        return // 没有子部门，直接返回
      }

      for (const dept of departments) {
        const { id, code: currentCode } = dept

        if (id) {
          allIds.push(id) // 收集当前节点的 id
        }

        if (currentCode) {
          await fetchAndCollectIds(currentCode) // 递归查询下一层子部门
        }
      }
    } catch (error) {
      console.error(`查询部门失败，code = ${code}，错误：`, error)
      // 出错时可以选择继续，或者根据需求处理
    }
  }

  // 从传入的 departmentCode 开始递归查询
  await fetchAndCollectIds(departmentCode)

  // 将所有 id 用逗号拼接成字符串返回
  return allIds.join(',')
}
