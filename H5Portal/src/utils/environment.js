/**
 * 环境相关方法
 */

import { logJxVersion } from '@/utils/version.js';

/**
 * 判断是否为鸿蒙环境
 * @returns {boolean}
 */
export async function isHarmonyOS() {
    const versionInfo = await logJxVersion();
    const deviceType = versionInfo?.deviceType || '';

    return deviceType === 'ohos'
}