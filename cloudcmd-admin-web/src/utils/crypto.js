import CryptoJS from 'crypto-js'

// 🔐 请妥善保管此密钥，建议不要硬编码在代码中，可从环境变量读取
// 这是一个示例密钥，实际项目中请使用更安全的方式管理（比如构建时注入）
const SECRET_KEY = '8Jq4TvRz1WnXcY7bP9oK2mL6QvAsDfGhN1jUx3Zc5B6Y8tW0eRq==' // 长度建议 16/24/32 字符，对应 AES-128/192/256

/**
 * 加密 JSON 对象
 * @param {Object} jsonObj - 要加密的 JS 对象，如 { name: 'kooriookami', age: 25 }
 * @returns {string} 加密后的字符串（密文）
 */
export function encryptJson(jsonObj) {
  try {
    // 1. 将对象转为 JSON 字符串
    const jsonString = JSON.stringify(jsonObj)

    // 2. 使用 AES 加密
    const encrypted = CryptoJS.AES.encrypt(jsonString, SECRET_KEY).toString()

    // 3. 返回加密后的密文
    return encrypted
  } catch (error) {
    console.error('❌ 加密失败：', error)
    throw error
  }
}

/**
 * 解密字符串为 JSON 对象
 * @param {string} encryptedStr - 加密后的字符串（密文）
 * @returns {Object | null} 解密后的 JS 对象，如果失败返回 null
 */
export function decryptJson(encryptedStr) {
  try {
    // 1. 使用 AES 解密
    const decryptedBytes = CryptoJS.AES.decrypt(encryptedStr, SECRET_KEY)

    // 2. 转为 UTF-8 字符串
    const decryptedStr = decryptedBytes.toString(CryptoJS.enc.Utf8)

    if (!decryptedStr) {
      console.error('❌ 解密失败：可能密钥错误或数据已损坏')
      return null
    }

    // 3. 解析为 JS 对象
    const jsonObj = JSON.parse(decryptedStr)

    return jsonObj
  } catch (error) {
    console.error('❌ 解密失败：', error)
    return null
  }
}
