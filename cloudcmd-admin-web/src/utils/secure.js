/**
 * 自定义 Base64 编码（避免使用 btoa）
 */
function base64Encode(str) {
    const encoder = new TextEncoder();
    const data = encoder.encode(str);
    return Buffer.from(data).toString('base64');
}

/**
 * 自定义 Base64 解码（避免使用 atob）
 */
function base64Decode(base64) {
    const buffer = Buffer.from(base64, 'base64');
    return new TextDecoder().decode(buffer);
}

/**
 * 加密身份证号
 * @param idCard 身份证号码
 * @returns 加密后的字符串
 */
export function encryptIdCard(idCard) {
    if (!idCard) return '';
    const reversed = idCard.split('').reverse().join('');
    return base64Encode(reversed);
}

/**
 * 解密身份证号
 * @param encrypted 加密后的字符串
 * @returns 原始身份证号码
 */
export function decryptIdCard(encrypted) {
    if (!encrypted) return '';
    const decoded = base64Decode(encrypted);
    return decoded.split('').reverse().join('');
}