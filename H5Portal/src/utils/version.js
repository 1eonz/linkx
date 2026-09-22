/**
 * 获取并打印警信版本信息
 * @returns {Promise<{version: string, deviceType: string} | null>}
 */
export async function logJxVersion() {
  try {
    const versionInfo = await window.WeSpaceSDK?.getVersion();
    if (versionInfo) {
      console.log(`警信版本：${versionInfo?.version}，环境：${versionInfo?.deviceType}`);
    }
    return versionInfo;
  } catch (error) {
    console.error('获取警信版本信息失败:', error);
    return null;
  }
}
